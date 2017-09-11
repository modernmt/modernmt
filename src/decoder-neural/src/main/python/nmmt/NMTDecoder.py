import logging
import os

from nmmt.NMTEngine import NMTEngine
from nmmt.SubwordTextProcessor import SubwordTextProcessor
from nmmt.internal_utils import log_timed_action
from nmmt.torch_utils import torch_setup


class UnsupportedLanguageException(BaseException):
    def __init__(self, source_language, target_language):
        self.message = "No engine and text processors found for %s -> %s." % (source_language, target_language)


class ModelFileNotFoundException(BaseException):
    def __init__(self, path):
        self.message = "Decoder model file not found: %s" % path


class Suggestion:
    def __init__(self, source, target, score):
        self.source = source
        self.target = target
        self.score = score


class _EngineData:
    @staticmethod
    def load(model_name, base_path='.'):
        model_file = os.path.join(base_path, model_name)
        tp_model_file = model_file + '.bpe'

        if not os.path.isfile(tp_model_file):
            raise ModelFileNotFoundException(tp_model_file)
        if not os.path.isfile(model_file + '.dat'):
            raise ModelFileNotFoundException(model_file + '.dat')

        text_processor = SubwordTextProcessor.load_from_file(tp_model_file)
        engine = NMTEngine.load_from_checkpoint(model_file)

        return _EngineData(engine, text_processor)

    def __init__(self, engine, text_processor):
        self.engine = engine
        self.text_processor = text_processor


class NMTDecoder:
    def __init__(self, model_path, gpu_id=None, random_seed=None):
        torch_setup(gpus=[gpu_id] if gpu_id is not None else None, random_seed=random_seed)

        self._logger = logging.getLogger('nmmt.NMTDecoder')
        self._engines_data = {}

        # create and put in its map a TextProcessor and a NMTEngine for each line in model.conf
        with open(os.path.join(model_path, 'model.conf'), "r") as model_map_file:
            model_map_lines = model_map_file.readlines()
            for line in model_map_lines:
                if not line.startswith('model.'):
                    continue

                direction, model_name = map(str.strip, line.split("="))
                direction = direction[6:]

                with log_timed_action(self._logger, 'Loading "%s" model from checkpoint' % direction):
                    self._engines_data[direction] = _EngineData.load(model_name, base_path=model_path)

        # Public-editable options
        self.beam_size = 5
        self.max_sent_length = 160
        self.replace_unk = False

    def get_engine(self, source_lang, target_lang):
        direction = source_lang + '__' + target_lang
        if direction not in self._engines_data:
            return None
        return self._engines_data[direction].engine

    def translate(self, source_lang, target_lang, text, suggestions=None, n_best=1,
                  tuning_epochs=None, tuning_learning_rate=None):
        # (0) Get TextProcessor and NMTEngine for current direction; if it does not exist, raise an exception
        direction = source_lang + '__' + target_lang
        if direction not in self._engines_data:
            raise UnsupportedLanguageException(source_lang, target_lang)

        engine_data = self._engines_data[direction]
        text_processor = engine_data.text_processor
        engine = engine_data.engine

        # (1) Process text and suggestions
        processed_text = text_processor.encode_line(text, is_source=True)
        processed_suggestions = None

        if suggestions is not None and len(suggestions) > 0:
            processed_suggestions = []

            for suggestion in suggestions:
                e = (text_processor.encode_line(suggestion.source, is_source=True),
                     text_processor.encode_line(suggestion.target, is_source=False),
                     suggestion.score)

                processed_suggestions.append(e)

        # (2) Tune engine if suggestions provided
        if processed_suggestions is not None:
            engine.tune(processed_suggestions, epochs=tuning_epochs, learning_rate=tuning_learning_rate)

        # (3) Translate
        pred_batch, pred_score = engine.translate(processed_text,
                                                  n_best=n_best, beam_size=self.beam_size,
                                                  max_sent_length=self.max_sent_length,
                                                  replace_unk=self.replace_unk)

        return text_processor.decode_tokens(pred_batch[0])
