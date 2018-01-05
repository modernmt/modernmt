import copy
import logging
import math
import os

import torch
import torch.nn as nn

from nmmt.models import Translation
from nmmt.IDataset import DatasetWrapper
from nmmt.SubwordTextProcessor import SubwordTextProcessor
from nmmt.internal_utils import opts_object, log_timed_action
from nmmt.torch_utils import torch_is_multi_gpu, torch_is_using_cuda, torch_get_gpus
from onmt import Models, Translator, Constants, Dataset, Optim


class _Translator(Translator):
    def __init__(self, src_dict, trg_dict, model):
        # Super constructor MUST NOT be invoked
        # super(_Translator, self).__init__(None)
        self.opt = opts_object()
        self.opt.alignment = True
        self.opt.batch_size = 32
        self.opt.cuda = torch_is_using_cuda()
        self.tt = torch.cuda if self.opt.cuda else torch
        self.beam_accum = None
        self.src_dict = src_dict
        self.tgt_dict = trg_dict
        self._type = 'text'
        self.model = model


class ModelFileNotFoundException(BaseException):
    def __init__(self, path):
        self.message = "Model file not found: %s" % path


class NMTEngine(object):
    COLD = 0
    WARM = 1
    HOT = 2

    class Metadata:
        __custom_values = {'True': True, 'False': False, 'None': None}

        def __init__(self):
            self.layers = 2  # Number of layers in the LST decoder/encoder
            self.encoder_layers = None  # Number of layers in the LSTM encoder only
            self.decoder_layers = None  # Number of layers in the LSTM decoder only
            self.rnn_size = 500  # Size of hidden states
            self.rnn_type = 'LSTM'  # The gate type used in the RNNs
            self.word_vec_size = 500  # Word embedding sizes
            self.input_feed = 1  # Feed the context vector at each time step as additional input to the decoder
            self.brnn = True  # Use a bidirectional encoder
            self.brnn_merge = 'sum'  # Merge action for the bidirectional hidden states: [concat|sum]
            self.context_gate = None  # Type of context gate to use [source|target|both] or None.
            self.dropout = 0.3  # Dropout probability; applied between LSTM stacks.

            # Tuning options -------------------------------------------------------------------------------------------
            self.tuning_optimizer = 'sgd'  # Optimization method. [sgd|adagrad|adadelta|adam]
            self.tuning_max_grad_norm = 5  # If norm(gradient vector) > max_grad_norm, re-normalize
            self.tuning_max_learning_rate = 0.2
            self.tuning_max_epochs = 10

        def __str__(self):
            return str(self.__dict__)

        def __repr__(self):
            return str(self.__dict__)

        def load_from_file(self, path):
            with open(path, 'rb') as metadata_stream:
                for line in metadata_stream:
                    key, value = (x.strip() for x in line.split('=', 1))

                    if key not in self.__dict__:
                        continue

                    if value in self.__custom_values:
                        value = self.__custom_values[value]
                    else:
                        try:
                            number = float(value)
                            value = number if '.' in value else int(value)
                        except ValueError:
                            pass  # value is a string

                    self.__dict__[key] = value

        def save_to_file(self, path):
            with open(path, 'wb') as metadata_file:
                for key, value in self.__dict__.iteritems():
                    if value is not None:
                        metadata_file.write('%s = %s\n' % (key, str(value)))

    @staticmethod
    def new_instance(src_dict, trg_dict, processor, metadata=None, init_value=0.1):
        if metadata is None:
            metadata = NMTEngine.Metadata()

        def _new_instance_initializer(model, generator):
            for p in model.parameters():
                p.data.uniform_(-init_value, init_value)
            for p in generator.parameters():
                p.data.uniform_(-init_value, init_value)

        return NMTEngine(src_dict, trg_dict, _new_instance_initializer, processor, metadata=metadata)

    @staticmethod
    def load_from_checkpoint(checkpoint_path):
        metadata_file = checkpoint_path + '.meta'
        processor_file = checkpoint_path + '.bpe'
        data_file = checkpoint_path + '.dat'
        dict_file = checkpoint_path + '.vcb'

        if not os.path.isfile(processor_file):
            raise ModelFileNotFoundException(processor_file)
        if not os.path.isfile(data_file):
            raise ModelFileNotFoundException(data_file)
        if not os.path.isfile(dict_file):
            raise ModelFileNotFoundException(dict_file)

        # Metadata
        metadata = NMTEngine.Metadata()

        if os.path.isfile(metadata_file):
            metadata.load_from_file(metadata_file)

        # Processor
        processor = SubwordTextProcessor.load_from_file(processor_file)

        dictionary = torch.load(dict_file, map_location=lambda storage, loc: storage)
        src_dict = dictionary['src']
        trg_dict = dictionary['tgt']

        def _checkpoint_initializer(model, generator):
            checkpoint = torch.load(data_file, map_location=lambda storage, loc: storage)

            model.load_state_dict(checkpoint['model'])
            generator.load_state_dict(checkpoint['generator'])

        return NMTEngine(src_dict, trg_dict, _checkpoint_initializer, processor, metadata=metadata)

    def __init__(self, src_dict, trg_dict, initializer, processor, metadata=None):
        self._logger = logging.getLogger('nmmt.NMTEngine')
        self._log_level = logging.INFO
        self._model_loaded = False
        self._running_state = self.COLD

        self.src_dict = src_dict
        self.trg_dict = trg_dict
        self.model = None
        self._model_init_state = None
        self.processor = processor
        self.metadata = metadata if metadata is not None else NMTEngine.Metadata()

        self._translator = None  # lazy load
        self._tuner = None  # lazy load

        self._initializer = initializer

    def __load(self):
        encoder = Models.Encoder(self.metadata, self.src_dict)
        decoder = Models.Decoder(self.metadata, self.trg_dict)
        model = Models.NMTModel(encoder, decoder)

        generator = nn.Sequential(nn.Linear(self.metadata.rnn_size, self.trg_dict.size()), nn.LogSoftmax(dim=1))

        model.cpu()
        generator.cpu()

        self._initializer(model, generator)

        model.generator = generator
        model.eval()

        self.model = model

        # Compute initial state
        model_state_dict, generator_state_dict = self._get_state_dicts()

        self._model_init_state = {k: v for k, v in sorted(model_state_dict.items()) if 'generator' not in k}
        self._model_init_state.update({"generator." + k: v for k, v in sorted(generator_state_dict.items())})

        self._model_loaded = False

    def __unload(self):
        del self.model
        del self._model_init_state
        del self._translator
        self.model = None
        self._model_init_state = None
        self._translator = None

        self._model_loaded = False

    def __gpu(self):
        model = self.model
        generator = self.model.generator

        if torch_is_using_cuda():
            model.cuda()
            generator.cuda()

            if torch_is_multi_gpu():
                model = nn.DataParallel(model, device_ids=torch_get_gpus(), dim=1)
                generator = nn.DataParallel(generator, device_ids=torch_get_gpus(), dim=0)

            self.model = model
            self.model.generator = generator

    def __cpu(self):
        self.model.cpu()
        self.model.generator.cpu()

    def _is_data_parallel(self):
        return isinstance(self.model, nn.DataParallel) or isinstance(self.model.generator, nn.DataParallel)

    def reset_model(self):
        with log_timed_action(self._logger, 'Restoring model initial state', log_start=False):
            self.model.load_state_dict(self._model_init_state)

            self.model.encoder.rnn.dropout = 0.
            self.model.decoder.dropout = nn.Dropout(0.)
            self.model.decoder.rnn.dropout = nn.Dropout(0.)

            self._model_loaded = True

    def _ensure_model_loaded(self):
        if not self._model_loaded:
            self.reset_model()

    def count_parameters(self):
        return sum([p.nelement() for p in self.model.parameters()])

    def tune(self, suggestions, epochs=None, learning_rate=None):
        # Set tuning parameters
        if epochs is None or learning_rate is None:
            _epochs, _learning_rate = self._estimate_tuning_parameters(suggestions)

            epochs = epochs if epochs is not None else _epochs
            learning_rate = learning_rate if learning_rate is not None else _learning_rate

        if learning_rate > 0. and epochs > 0:
            if self._tuner is None:
                from nmmt.NMTEngineTrainer import NMTEngineTrainer

                optimizer = Optim(self.metadata.tuning_optimizer, 1., max_grad_norm=self.metadata.tuning_max_grad_norm)

                tuner_opts = NMTEngineTrainer.Options()
                tuner_opts.log_level = logging.NOTSET

                self._tuner = NMTEngineTrainer(self, options=tuner_opts, optimizer=optimizer)

            self._tuner.opts.step_limit = epochs
            self._tuner.reset_learning_rate(learning_rate)

            # Process suggestions
            tuning_src_batch, tuning_trg_batch = [], []

            for suggestion in suggestions:
                source = self.processor.encode_line(suggestion.source, is_source=True)
                source = self.src_dict.convertToIdxTensor(source, Constants.UNK_WORD)

                target = self.processor.encode_line(suggestion.target, is_source=False)
                target = self.trg_dict.convertToIdxTensor(target, Constants.UNK_WORD, Constants.BOS_WORD,
                                                          Constants.EOS_WORD)

                tuning_src_batch.append(source)
                tuning_trg_batch.append(target)

            tuning_set = Dataset(tuning_src_batch, tuning_trg_batch, len(tuning_src_batch), torch_is_using_cuda())
            tuning_set = DatasetWrapper(tuning_set)

            # Run tuning
            log_message = 'Tuning on %d suggestions (epochs = %d, learning_rate = %.3f )' % (
                len(suggestions), self._tuner.opts.step_limit, self._tuner.optimizer.lr)
            with log_timed_action(self._logger, log_message, log_start=False):
                self._tuner.train_model(tuning_set)

    def _estimate_tuning_parameters(self, suggestions):
        # it returns an actual learning_rate and epochs based on the quality of the suggestions
        # it is assured that at least one suggestion is provided (hence, len(suggestions) > 0)
        average_score = 0.0
        for suggestion in suggestions:
            average_score += suggestion.score
        average_score /= len(suggestions)

        # Empirically defined function to make the number of epochs dependent to the quality of the suggestions
        # epochs = max_epochs * average_score + 1
        # where max_epochs is the maximum number of epochs allowed;
        # hence epochs = max_epochs only with perfect suggestions
        # and epochs = 0, when the average_score is close to 0.0 (<1/max_epochs)
        tuning_epochs = int(self.metadata.tuning_max_epochs * average_score)

        # Empirically defined function to make the learning rate dependent to the quality of the suggestions
        # lr = max_lr * sqrt(average_score)
        # hence lr = max_lr only with perfect suggestions
        # and lr = 0, when the average_score is exactly 0.0
        tuning_learning_rate = self.metadata.tuning_max_learning_rate * math.sqrt(average_score)

        return tuning_epochs, tuning_learning_rate

    def translate(self, text, beam_size=5, max_sent_length=160, replace_unk=False, n_best=1):
        self._ensure_model_loaded()

        self.model.eval()

        if self._translator is None:
            self._translator = _Translator(self.src_dict, self.trg_dict, self.model)

        self._translator.opt.replace_unk = replace_unk
        self._translator.opt.beam_size = max(beam_size, n_best)
        self._translator.opt.max_sent_length = max_sent_length
        self._translator.opt.n_best = n_best

        src_bpe_tokens = self.processor.encode_line(text, is_source=True)
        pred_batch, _, _, align_batch = self._translator.translate([src_bpe_tokens], None)

        translations = []
        for trg_bpe_tokens, bpe_alignment in zip(pred_batch[0], align_batch[0]):
            src_indexes = self.processor.get_words_indexes(src_bpe_tokens)
            trg_indexes = self.processor.get_words_indexes(trg_bpe_tokens)

            translation = Translation(text=self.processor.decode_tokens(trg_bpe_tokens),
                                      alignment=self._make_alignment(src_indexes, trg_indexes, bpe_alignment))

            translations.append(translation)

        return translations

    @staticmethod
    def _make_alignment(src_indexes, trg_indexes, bpe_alignment):
        if not bpe_alignment:
            return []

        return sorted(set([(src_indexes[al[0]], trg_indexes[al[1]]) for al in bpe_alignment]))

    def save(self, path, store_data=True, store_metadata=True, store_processor=True):
        if store_metadata:
            self.metadata.save_to_file(path + '.meta')

        if store_processor:
            self.processor.save_to_file(path + '.bpe')

        if store_data:
            model_state_dict, generator_state_dict = self._get_state_dicts()

            checkpoint = {
                'model': model_state_dict,
                'generator': generator_state_dict,
            }
            torch.save(checkpoint, path + '.dat')

            dictionary = {
                'src': self.src_dict, 'tgt': self.trg_dict,
            }
            torch.save(dictionary, path + '.vcb')

    def _get_state_dicts(self):
        if self._is_data_parallel():
            model = self.model.module
            generator = self.model.generator.module
        else:
            model = self.model
            generator = self.model.generator

        model_state_dict = {k: v for k, v in model.state_dict().items() if 'generator' not in k}
        generator_state_dict = generator.state_dict()

        return copy.deepcopy(model_state_dict), copy.deepcopy(generator_state_dict)

    @property
    def running_state(self):
        return self._running_state

    @running_state.setter
    def running_state(self, value):
        if not (value == self.COLD or value == self.WARM or value == self.HOT):
            raise ValueError('Invalid Value %d' % value)

        if self._running_state == self.COLD:
            if value == self.WARM:
                self.__load()
            elif value == self.HOT:
                self.__load()
                self.__gpu()
        elif self._running_state == self.WARM:
            if value == self.COLD:
                self.__unload()
            elif value == self.HOT:
                self.__gpu()
        elif self._running_state == self.HOT:
            if value == self.COLD:
                self.__unload()
            elif value == self.WARM:
                self.__cpu()

        self._running_state = value
