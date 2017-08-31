import os

import logging
import torch
import torch.cuda.random as random

from nmmt import SubwordTextProcessor, NMTEngine
from nmmt.internal_utils import log_timed_action


class UnsupportedLanguageException(BaseException):
    def __init__(self, source_language, target_language):
        self.message = "No engine and text processors found for %s -> %s." % (source_language, target_language)


class ModelFileNotFoundException(BaseException):
    def __init__(self, path):
        self.message = "Decoder model file not found: %s" % path


class _EngineData:
    @staticmethod
    def load(model_name, base_path='.', using_cuda=True):
        tp_model_file = os.path.join(base_path, model_name + '.bpe')
        engine_model_file = os.path.join(base_path, model_name + '.pt')

        if not os.path.isfile(tp_model_file):
            raise ModelFileNotFoundException(tp_model_file)
        if not os.path.isfile(engine_model_file):
            raise ModelFileNotFoundException(engine_model_file)

        text_processor = SubwordTextProcessor.load_from_file(tp_model_file)
        engine = NMTEngine.load_from_checkpoint(engine_model_file, using_cuda=using_cuda)

        return _EngineData(engine, text_processor)

    def __init__(self, engine, text_processor):
        self.engine = engine
        self.text_processor = text_processor


class NMTDecoder:
    def __init__(self, model_path, gpu_id=None, random_seed=None):
        self._logger = logging.getLogger('nmmt.NMTDecoder')

        if gpu_id is not None:
            torch.cuda.set_device(gpu_id)

        if random_seed is not None:
            torch.manual_seed(random_seed)
            random.manual_seed_all(random_seed)

        using_cuda = gpu_id is not None

        # map languageDirection -> _EngineData (direction is a string <src>__<trg>)
        self._engines_data = {}

        # create and put in its map a TextProcessor and a NMTEngine for each line in model.map
        with open(os.path.join(model_path, 'model.map'), "r") as model_map_file:
            model_map_lines = model_map_file.readlines()
            for line in model_map_lines:
                direction, model_name = map(str.strip, line.split("="))

                with log_timed_action(self._logger, 'Loading "%s" model from checkpoint' % direction):
                    self._engines_data[direction] = _EngineData.load(model_name,
                                                                     base_path=model_path,
                                                                     using_cuda=using_cuda)

        # Public-editable options
        self.beam_size = 5
        self.max_sent_length = 160
        self.replace_unk = False

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

        # (4) Reset engine if needed
        if processed_suggestions is not None:
            with log_timed_action(self._logger, 'Restoring model initial state', log_start=False):
                engine.reset_model()

        return text_processor.decode_tokens(pred_batch[0])
