import ConfigParser
import logging
import math
import os
# noinspection PyUnresolvedReferences
import time

import numpy as np
import tensorflow as tf
# noinspection PyUnresolvedReferences
from tensor2tensor import problems as problems_lib  # pylint: disable=unused-import
from tensor2tensor.data_generators import text_encoder
from tensor2tensor.layers import common_layers
from tensor2tensor.utils import registry, optimize

# noinspection PyUnresolvedReferences
import t2t  # pylint: disable=unused-import
from nmmt import Translation, TranslationRequest, TranslationResponse


class ModelConfig(object):
    __custom_values = {'True': True, 'False': False, 'None': None}

    @staticmethod
    def load(model_path):
        config = ConfigParser.ConfigParser()
        config.read(os.path.join(model_path, 'model.conf'))
        return ModelConfig(model_path, config)

    def __init__(self, basepath, config_parser):
        self._basepath = basepath
        self._config = config_parser

    def _parse(self, value):
        if value in self.__custom_values:
            value = self.__custom_values[value]
        else:
            try:
                number = float(value)
                value = number if '.' in value else int(value)
            except ValueError:
                pass  # value is a string

        return value

    @property
    def settings(self):
        settings = TransformerDecoder.Settings()

        if self._config.has_section('settings'):
            for name, value in self._config.items('settings'):
                if not hasattr(settings, name):
                    raise ValueError('Invalid option "%s"' % name)
                setattr(settings, name, self._parse(value))

        return settings

    @property
    def checkpoints(self):
        result = []
        for name, value in self._config.items('models'):
            if not os.path.isabs(value):
                value = os.path.join(self._basepath, value)

            result.append((name, value))
        return result


class TransformerDecoder(object):
    class Settings(object):
        def __init__(self):
            self.memory_suggestions_limit = None  # Ignore
            self.memory_query_min_results = None  # Ignore
            self.tuning_max_epochs = 5
            self.tuning_max_learning_rate = .0002
            self.tuning_max_batch_size = None

        def __str__(self):
            return str(self.__dict__)

    def __init__(self, gpu, checkpoints, settings=None):
        self._logger = logging.getLogger('TransformerDecoder')
        self._settings = settings if settings is not None else TransformerDecoder.Settings()
        self._checkpoints = checkpoints
        self._checkpoint = None
        self._nn_needs_reset = True

        with tf.device('/device:GPU:0' if gpu is not None else '/cpu:0'):
            self._restorer = checkpoints.restorer()

            # Prepare features for feeding into the model.
            self._ph_infer_inputs = tf.placeholder(dtype=tf.int32)
            self._ph_train_inputs = tf.reshape(tf.placeholder(dtype=tf.int32), shape=[-1, -1, 1, 1])
            self._ph_train_targets = tf.reshape(tf.placeholder(dtype=tf.int32), shape=[-1, -1, 1, 1])
            self._ph_learning_rate = tf.placeholder(tf.float32, [], name='learning_rate')

            # Prepare the model for training
            self._model = registry.model('transformer')(self._checkpoints.hparams, tf.estimator.ModeKeys.TRAIN)

            _, losses = self._model({
                "inputs": self._ph_train_inputs,
                "targets": self._ph_train_targets
            })

            self._loss = losses['training']
            self._train_op = optimize.optimize(self._loss, self._ph_learning_rate, self._model.hparams,
                                               use_tpu=common_layers.is_on_tpu())

            tf.get_variable_scope().reuse_variables()

            # Prepare the model for infer
            self._attention_mats_op = [
                self._model.attention_weights[
                    'transformer/body/decoder/layer_%i/encdec_attention/multihead_attention/dot_product_attention' % i]
                for i in xrange(self._model.hparams.num_hidden_layers)
            ]

            infer_inputs = tf.reshape(self._ph_infer_inputs, [1, -1, 1, 1]),  # Make it 4D.
            infer_out = self._model.infer({
                "inputs": infer_inputs
            }, beam_size=4, top_beams=1, alpha=0.6, decode_length=100)

            self._predictions_op = {
                "outputs": infer_out["outputs"],
                "inputs": infer_inputs,
            }

        session_config = tf.ConfigProto(allow_soft_placement=True)
        session_config.gpu_options.allow_growth = True
        if gpu is not None:
            session_config.gpu_options.force_gpu_compatible = True
            session_config.gpu_options.visible_device_list = str(gpu)

        self._session = tf.Session(config=session_config)

        # Init model
        self._warmup()

    def translate(self, source_lang, target_lang, text, suggestions=None,
                  tuning_epochs=None, tuning_learning_rate=None, forced_translation=None):
        checkpoint = self._checkpoints[source_lang, target_lang]

        # (1) Reset model (if necessary)
        begin = time.time()
        self._reset_model(checkpoint)
        reset_time = time.time() - begin

        # (2) Tune engine if suggestions provided
        begin = time.time()
        if suggestions is not None and len(suggestions) > 0:
            self._tune(suggestions, epochs=tuning_epochs, learning_rate=tuning_learning_rate)
        tune_time = time.time() - begin

        # (3) Translate and compute word alignment
        begin = time.time()
        result = self._decode(source_lang, target_lang, text, output_text=forced_translation)
        decode_time = time.time() - begin

        self._logger.info('reset_time = %.3f, tune_time = %.3f, decode_time = %.3f'
                          % (reset_time, tune_time, decode_time))

        return result

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
        tuning_epochs = int(self._settings.tuning_max_epochs * average_score)

        # Empirically defined function to make the learning rate dependent to the quality of the suggestions
        # lr = max_lr * sqrt(average_score)
        # hence lr = max_lr only with perfect suggestions
        # and lr = 0, when the average_score is exactly 0.0
        tuning_learning_rate = self._settings.tuning_max_learning_rate * math.sqrt(average_score)

        return tuning_epochs, tuning_learning_rate

    def _reset_model(self, checkpoint):
        if self._nn_needs_reset or checkpoint != self._checkpoint:
            self._restorer.restore(self._session, checkpoint)
            self._checkpoint = checkpoint
            self._nn_needs_reset = False

    def _tune(self, suggestions, epochs=None, learning_rate=None):
        # Set tuning parameters
        if epochs is None or learning_rate is None:
            _epochs, _learning_rate = self._estimate_tuning_parameters(suggestions)

            epochs = epochs if epochs is not None else _epochs
            learning_rate = learning_rate if learning_rate is not None else _learning_rate

        if learning_rate > 0. and epochs > 0:
            batch_src = [self._text_encode(s.segment)[0] for s in suggestions]
            batch_tgt = [self._text_encode(s.translation)[0] for s in suggestions]

            batch_src, batch_tgt = self._pack_batch(batch_src, batch_tgt, self._settings.tuning_max_batch_size)

            for _ in xrange(epochs):
                self._session.run(self._train_op, {
                    self._ph_train_inputs: batch_src,
                    self._ph_train_targets: batch_tgt,
                    self._ph_learning_rate: learning_rate
                })

            self._nn_needs_reset = True

    def _decode(self, source_lang, target_lang, text, output_text=None):
        # decode
        inputs, input_indexes = self._text_encode(text)
        if output_text is None:
            results = self._session.run(self._predictions_op, {
                self._ph_infer_inputs: inputs
            })
            outputs = self._save_until_eos(results['outputs'])
            raw_output, output_indexes = self._text_decode(outputs)
        else:
            outputs, output_indexes = self._text_encode(output_text)
            raw_output = output_text

        # align
        results = self._session.run(self._attention_mats_op, {
            self._ph_infer_inputs: inputs,
            self._ph_train_inputs: np.reshape(inputs, [1, -1, 1, 1]),
            self._ph_train_targets: np.reshape(outputs, [1, -1, 1, 1]),
        })

        alignment = self._make_alignment(input_indexes, output_indexes, results)

        return Translation(text=raw_output, alignment=alignment)

    def _warmup(self):
        random_checkpoint = self._checkpoints[None]
        self._reset_model(random_checkpoint)

        self._session.run(self._predictions_op, {
            self._ph_infer_inputs: [text_encoder.EOS_ID]
        })

    def _text_encode(self, text):
        encoded, indexes = self._checkpoint.encoder.encode_with_indexes(text)
        encoded.append(text_encoder.EOS_ID)
        return encoded, indexes

    def _text_decode(self, hyp):
        encoded, indexes = self._checkpoint.decoder.decode_with_indexes(hyp)
        return encoded, indexes

    @staticmethod
    def _pack_batch(batch_src, batch_tgt, max_size=None):
        src_lengths = [len(x) for x in batch_src]
        tgt_lengths = [len(x) for x in batch_tgt]
        src_max_length = max(src_lengths)
        tgt_max_length = max(tgt_lengths)

        if max_size is not None:
            while (src_max_length * len(src_lengths) + tgt_max_length * len(tgt_lengths)) > max_size:
                src_lengths.pop()
                tgt_lengths.pop()
                src_max_length = max(src_lengths)
                tgt_max_length = max(tgt_lengths)

            if len(src_lengths) < len(batch_src):
                batch_src = batch_src[:len(src_lengths)]
            if len(tgt_lengths) < len(batch_tgt):
                batch_tgt = batch_tgt[:len(tgt_lengths)]

        def _pack(batch, max_length):
            for e in batch:
                length = len(e)

                if length < max_length:
                    for i in xrange(max_length - length):
                        e.append(text_encoder.PAD_ID)

            return [[[[w]] for w in l] for l in batch]

        return _pack(batch_src, src_max_length), _pack(batch_tgt, tgt_max_length)

    @staticmethod
    def _save_until_eos(hyp):
        """Strips everything after the first <EOS> token, which is normally 1."""
        hyp = hyp.flatten()
        try:
            index = list(hyp).index(text_encoder.EOS_ID)
            return hyp[0:index]
        except ValueError:
            # No EOS_ID: return the array as-is.
            return hyp

    @staticmethod
    def _make_alignment(input_indexes, output_indexes, attention_matrix):
        attention_matrix = np.asarray(attention_matrix)

        # resulting shape (layers, batch, heads, output, input);
        # last two dimensions truncated to the size of trg_sub_tokens and src_sub_tokens
        reduced_attention_matrix = attention_matrix[:, :, :, :len(output_indexes), :len(input_indexes)]
        # get average over layers and heads; resulting shape (batch, output, input)
        average_encdec_atts_mats = reduced_attention_matrix.mean((0, 2))
        # get first batch only; resulting shape (output, input)
        alignment_matrix = average_encdec_atts_mats[0]

        # get indexes of the best aligned source for each output; resulting shape (output)
        # best_indexes = alignment_matrix.argmax(1)
        # sub_alignment = [(best_indexes[index], index) for index in xrange(len(best_indexes))]

        # get indexes of the best aligned output for each input; resulting shape (input)
        best_indexes = alignment_matrix.argmax(0)
        sub_alignment = [(index, best_indexes[index]) for index in xrange(len(best_indexes))]

        if not sub_alignment:
            return []

        return sorted(set([(input_indexes[al[0]], output_indexes[al[1]]) for al in sub_alignment]))

    def serve_forever(self, stdin, stdout):
        try:
            while True:
                line = stdin.readline()
                if not line:
                    break

                request = TranslationRequest.from_json_string(line)
                translation = self.translate(request.source_lang, request.target_lang, request.query,
                                             suggestions=request.suggestions,
                                             forced_translation=request.forced_translation)
                response = TranslationResponse.to_json_string(translation)

                stdout.write(response + '\n')
                stdout.flush()
        except KeyboardInterrupt:
            pass  # ignore and exit
        except BaseException as e:
            response = TranslationResponse.to_json_string(e)
            stdout.write(response + '\n')
            stdout.flush()

            raise
