import logging
import torch
import torch.nn as nn

from nmmt.NMTEngineTrainer import NMTEngineTrainer
from nmmt.internal_utils import opts_object, log_timed_action
from onmt import Models, Translator, Constants, Dataset


class _Translator(Translator):
    def __init__(self, using_cuda, src_dict, trg_dict, model):
        # Super constructor MUST NOT be invoked
        # super(_Translator, self).__init__(None)
        self.opt = opts_object()
        self.opt.batch_size = 32
        self.opt.cuda = using_cuda
        self.tt = torch.cuda if using_cuda else torch
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
            self.tuning_max_epochs = 5

    @staticmethod
    def load_from_checkpoint(checkpoint_path, using_cuda):
        checkpoint = torch.load(checkpoint_path, map_location=lambda storage, loc: storage)

        model_opt = NMTEngine.Parameters()
        model_opt.__dict__.update(checkpoint['opt'])

        src_dict = checkpoint['dicts']['src']
        trg_dict = checkpoint['dicts']['tgt']

        encoder = Models.Encoder(model_opt, src_dict)
        decoder = Models.Decoder(model_opt, trg_dict)

        model = Models.NMTModel(encoder, decoder)
        model.load_state_dict(checkpoint['model'])

        generator = nn.Sequential(nn.Linear(model_opt.rnn_size, trg_dict.size()), nn.LogSoftmax())
        generator.load_state_dict(checkpoint['generator'])

        if using_cuda:
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

        return NMTEngine(model_opt, src_dict, trg_dict, model, optim, checkpoint, using_cuda)

    def __init__(self, params, src_dict, trg_dict, model, optim, checkpoint, using_cuda):
        self._logger = logging.getLogger('ommt.NMTEngine')
        self._log_level = logging.INFO
        self._model_loaded = False

        self._model_params = params
        self._src_dict = src_dict
        self._trg_dict = trg_dict
        self._model = model
        self._optim = optim
        self._checkpoint = checkpoint
        self._using_cuda = using_cuda

        self._translator = None  # lazy load
        self._tuner = None  # lazy load

    def get_tuning_options(self):
        return self._model_params.tuning_max_epochs, self._model_params.tuning_max_learning_rate

    def set_tuning_options(self, max_epochs, max_learning_rate):
        self._model_params.tuning_max_epochs = max_epochs
        self._model_params.tuning_max_learning_rate = max_learning_rate

    def _ensure_model_loaded(self):
        if not self._model_loaded:
            self.reset_model()
            self._model_loaded = True

    def reset_model(self):
        model_state_dict = {k: v for k, v in sorted(self._checkpoint['model'].items()) if 'generator' not in k}
        model_state_dict.update({"generator." + k: v for k, v in sorted(self._checkpoint['generator'].items())})
        self._model.load_state_dict(model_state_dict)

        self._model.encoder.rnn.dropout = 0.
        self._model.decoder.dropout = nn.Dropout(0.)
        self._model.decoder.rnn.dropout = nn.Dropout(0.)

        self._optim.set_parameters(self._model.parameters())
        self._optim.optimizer.load_state_dict(self._checkpoint['optim'].optimizer.state_dict())

    def tune(self, suggestions, epochs=None, learning_rate=None):
        self._ensure_model_loaded()

        if self._tuner is None:
            self._tuner = NMTEngineTrainer(self._model, self._optim, self._src_dict, self._trg_dict,
                                           model_params=self._model_params, gpu_ids=([0] if self._using_cuda else None))
            self._tuner.start_epoch = 1
            self._tuner.min_perplexity_decrement = -1.
            self._tuner.set_log_level(logging.NOTSET)

        # Set tuning parameters
        if epochs is None or learning_rate is None:
            _epochs, _learning_rate = self._estimate_tuning_parameters(suggestions)

            self._tuner.min_epochs = self._tuner.max_epochs = epochs if epochs is not None else _epochs
            self._tuner.learning_rate = learning_rate if learning_rate is not None else _learning_rate

        # Convert words to indexes [suggestions]
        tuning_src_batch, tuning_trg_batch = [], []

        for source, target, _ in suggestions:
            tuning_src_batch.append(self._src_dict.convertToIdxTensor(source, Constants.UNK_WORD))
            tuning_trg_batch.append(self._trg_dict.convertToIdxTensor(target, Constants.UNK_WORD,
                                                                      Constants.BOS_WORD, Constants.EOS_WORD))

        # Prepare data for training on the tuningBatch
        tuning_dataset = Dataset(tuning_src_batch, tuning_trg_batch, 32, self._using_cuda)

        # Run tuning
        log_message = 'Tuning on %d suggestions (epochs = %d epochs, learning_rate = %.2f )' % (
            len(suggestions), self._tuner.min_epochs, self._tuner.learning_rate)

        with log_timed_action(self._logger, log_message, log_start=False):
            self._tuner.train_model(tuning_dataset, save_epochs=0)

    def _estimate_tuning_parameters(self, suggestions):
        # TODO: it should return an actual learning_rate and epochs based on the quality of the suggestions
        return self.get_tuning_options()

    def translate(self, text, beam_size=5, max_sent_length=160, replace_unk=False, n_best=1):
        self._ensure_model_loaded()

        if self._translator is None:
            self._translator = _Translator(self._using_cuda, self._src_dict, self._trg_dict, self._model)

        self._translator.opt.replace_unk = replace_unk
        self._translator.opt.beam_size = beam_size
        self._translator.opt.max_sent_length = max_sent_length
        self._translator.opt.n_best = n_best

        pred_batch, pred_score, _ = self._translator.translate([text], None)

        return pred_batch[0], pred_score[0]
