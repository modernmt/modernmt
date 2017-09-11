import logging
import math
import os

import torch
import torch.nn as nn

from nmmt.internal_utils import opts_object, log_timed_action
from nmmt.torch_utils import torch_is_multi_gpu, torch_is_using_cuda
from onmt import Models, Translator, Constants, Dataset


class _Translator(Translator):
    def __init__(self, src_dict, trg_dict, model):
        # Super constructor MUST NOT be invoked
        # super(_Translator, self).__init__(None)
        self.opt = opts_object()
        self.opt.batch_size = 32
        self.opt.cuda = torch_is_using_cuda()
        self.tt = torch.cuda if self.opt.cuda else torch
        self.beam_accum = None
        self.src_dict = src_dict
        self.tgt_dict = trg_dict
        self._type = 'text'
        self.model = model


class NMTEngine:
    class Parameters:
        def __init__(self):
            self.layers = 2  # Number of layers in the LSTM encoder/decoder
            self.rnn_size = 500  # Size of hidden states
            self.rnn_type = 'LSTM'  # The gate type used in the RNNs
            self.word_vec_size = 500  # Word embedding sizes
            self.input_feed = 1  # Feed the context vector at each time step as additional input to the decoder
            self.brnn = True  # Use a bidirectional encoder
            self.brnn_merge = 'sum'  # Merge action for the bidirectional hidden states: [concat|sum]
            self.context_gate = None  # Type of context gate to use [source|target|both] or None.
            self.dropout = 0.3  # Dropout probability; applied between LSTM stacks.

            # Optimization options -------------------------------------------------------------------------------------
            self.optim = 'sgd'  # Optimization method. [sgd|adagrad|adadelta|adam]
            self.max_grad_norm = 5  # If norm(gradient vector) > max_grad_norm, re-normalize
            self.learning_rate = 1.0
            self.learning_rate_decay = 0.9
            self.start_decay_at = 10

            # Tuning options -------------------------------------------------------------------------------------------
            self.tuning_max_learning_rate = 0.2
            self.tuning_max_epochs = 10

    @staticmethod
    def load_from_checkpoint(checkpoint_path):
        # Metadata
        model_opt = NMTEngine.Parameters()

        if os.path.isfile(checkpoint_path + '.meta'):
            with open(checkpoint_path + '.meta', 'rb') as metadata_file:
                for line in metadata_file:
                    key, value = (x.strip() for x in line.split('=', 1))

                    if value == 'True':
                        value = True
                    elif value == 'False':
                        value = False
                    else:
                        try:
                            number = float(value)
                            value = number if '.' in value else int(value)
                        except ValueError:
                            pass  # value is a string

                    model_opt.__dict__[key] = value

        # Data
        checkpoint = torch.load(checkpoint_path + '.dat', map_location=lambda storage, loc: storage)

        src_dict = checkpoint['dicts']['src']
        trg_dict = checkpoint['dicts']['tgt']

        encoder = Models.Encoder(model_opt, src_dict)
        decoder = Models.Decoder(model_opt, trg_dict)

        model = Models.NMTModel(encoder, decoder)
        model.load_state_dict(checkpoint['model'])

        generator = nn.Sequential(nn.Linear(model_opt.rnn_size, trg_dict.size()), nn.LogSoftmax())
        generator.load_state_dict(checkpoint['generator'])

        if torch_is_using_cuda():
            model.cuda()
            generator.cuda()
        else:
            model.cpu()
            generator.cpu()

        model.generator = generator
        model.eval()

        optim = checkpoint['optim']
        optim.set_parameters(model.parameters())
        optim.optimizer.load_state_dict(checkpoint['optim'].optimizer.state_dict())

        return NMTEngine(src_dict, trg_dict, model, optim, model_opt, checkpoint)

    def __init__(self, src_dict, trg_dict, model, optimizer, parameters=None, checkpoint=None):
        self._logger = logging.getLogger('nmmt.NMTEngine')
        self._log_level = logging.INFO
        self._model_loaded = False

        self.src_dict = src_dict
        self.trg_dict = trg_dict
        self.model = model
        self.optimizer = optimizer
        self.parameters = parameters if parameters is not None else NMTEngine.Parameters()
        self.checkpoint = checkpoint

        self._translator = None  # lazy load
        self._tuner = None  # lazy load

    def _reset_model(self):
        model_state_dict = {k: v for k, v in sorted(self.checkpoint['model'].items()) if 'generator' not in k}
        model_state_dict.update({"generator." + k: v for k, v in sorted(self.checkpoint['generator'].items())})
        self.model.load_state_dict(model_state_dict)

        self.model.encoder.rnn.dropout = 0.
        self.model.decoder.dropout = nn.Dropout(0.)
        self.model.decoder.rnn.dropout = nn.Dropout(0.)

        self.optimizer.set_parameters(self.model.parameters())
        self.optimizer.optimizer.load_state_dict(self.checkpoint['optim'].optimizer.state_dict())

    def _ensure_model_loaded(self):
        if not self._model_loaded:
            self._reset_model()
            self._model_loaded = True

    def tune(self, suggestions, epochs=None, learning_rate=None):
        # Set tuning parameters
        if epochs is None or learning_rate is None:
            _epochs, _learning_rate = self._estimate_tuning_parameters(suggestions)

            epochs = epochs if epochs is not None else _epochs
            learning_rate = learning_rate if learning_rate is not None else _learning_rate

        if learning_rate > 0. or epochs > 0:
            if self._tuner is None:
                from nmmt.NMTEngineTrainer import NMTEngineTrainer
                self._tuner = NMTEngineTrainer(self)

                self._tuner.start_epoch = 1
                self._tuner.min_perplexity_decrement = -1.
                self._tuner.set_log_level(logging.NOTSET)

            self._tuner.min_epochs = self._tuner.max_epochs = epochs
            self.optimizer.lr = learning_rate

            # Reset model
            with log_timed_action(self._logger, 'Restoring model initial state', log_start=False):
                self._reset_model()

            # Convert words to indexes [suggestions]
            tuning_src_batch, tuning_trg_batch = [], []

            for source, target, _ in suggestions:
                tuning_src_batch.append(self.src_dict.convertToIdxTensor(source, Constants.UNK_WORD))
                tuning_trg_batch.append(self.trg_dict.convertToIdxTensor(target, Constants.UNK_WORD,
                                                                         Constants.BOS_WORD, Constants.EOS_WORD))

            # Prepare data for training on the tuningBatch
            tuning_dataset = Dataset(tuning_src_batch, tuning_trg_batch, 32, torch_is_using_cuda())

            # Run tuning
            log_message = 'Tuning on %d suggestions (epochs = %d, learning_rate = %.3f )' % (
                len(suggestions), epochs, learning_rate)

            with log_timed_action(self._logger, log_message, log_start=False):
                self._tuner.train_model(tuning_dataset, save_epochs=0)

    def _estimate_tuning_parameters(self, suggestions):
        # it returns an actual learning_rate and epochs based on the quality of the suggestions
        # it is assured that at least one suggestion is provided (hence, len(suggestions) > 0)
        average_score = 0.0
        for source, target, score in suggestions:
            average_score += score
        average_score /= len(suggestions)

        # Empirically defined function to make the number of epochs dependent to the quality of the suggestions
        # epochs = max_epochs * average_score + 1
        # where max_epochs is the maximum number of epochs allowed;
        # hence epochs = max_epochs only with perfect suggestions
        # and epochs = 0, when the average_score is close to 0.0 (<1/max_epochs)
        tuning_epochs = int(self.parameters.tuning_max_epochs * average_score)

        # Empirically defined function to make the learning rate dependent to the quality of the suggestions
        # lr = max_lr * sqrt(average_score)
        # hence lr = max_lr only with perfect suggestions
        # and lr = 0, when the average_score is exactly 0.0
        tuning_learning_rate = self.parameters.tuning_max_learning_rate * math.sqrt(average_score)

        return tuning_epochs, tuning_learning_rate

    def translate(self, text, beam_size=5, max_sent_length=160, replace_unk=False, n_best=1):
        self._ensure_model_loaded()

        if self._translator is None:
            self._translator = _Translator(self.src_dict, self.trg_dict, self.model)

        self._translator.opt.replace_unk = replace_unk
        self._translator.opt.beam_size = beam_size
        self._translator.opt.max_sent_length = max_sent_length
        self._translator.opt.n_best = n_best

        pred_batch, pred_score, _ = self._translator.translate([text], None)

        return pred_batch[0], pred_score[0]

    def save(self, path, store_data=True, store_parameters=True, epoch=None):
        if store_parameters:
            with open(path + '.meta', 'wb') as metadata_file:
                for key, value in self.parameters.__dict__.iteritems():
                    if value is not None:
                        metadata_file.write('%s = %s\n' % (key, str(value)))

        if store_data:
            is_multi_gpu = torch_is_multi_gpu()

            model = self.model.module if is_multi_gpu else self.model
            generator = self.model.generator.module if is_multi_gpu else self.model.generator

            model_state_dict = {k: v for k, v in model.state_dict().items() if 'generator' not in k}
            generator_state_dict = generator.state_dict()

            if epoch is None:
                epoch = self.checkpoint['epoch'] if self.checkpoint else 0

            checkpoint = {
                'model': model_state_dict,
                'generator': generator_state_dict,
                'dicts': {'src': self.src_dict, 'tgt': self.trg_dict},
                'epoch': epoch,
                'optim': self.optimizer
            }

            torch.save(checkpoint, path + '.dat')
