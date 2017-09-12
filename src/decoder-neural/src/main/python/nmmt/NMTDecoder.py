import logging
import os

from nmmt.NMTEngine import NMTEngine
from nmmt.internal_utils import log_timed_action
from nmmt.torch_utils import torch_setup


class UnsupportedLanguageException(BaseException):
    def __init__(self, source_language, target_language):
        self.message = "No engine and text processors found for %s -> %s." % (source_language, target_language)


class Suggestion:
    def __init__(self, source, target, score):
        self.source = source
        self.target = target
        self.score = score


class NMTDecoder:
    def __init__(self, model_path, gpu_id=None, random_seed=None):
        torch_setup(gpus=[gpu_id] if gpu_id is not None else None, random_seed=random_seed)

        self._logger = logging.getLogger('nmmt.NMTDecoder')
        self._engines = {}

        # create and put in its map a TextProcessor and a NMTEngine for each line in model.conf
        with open(os.path.join(model_path, 'model.conf'), "r") as model_map_file:
            model_map_lines = model_map_file.readlines()
            for line in model_map_lines:
                if not line.startswith('model.'):
                    continue

                direction, model_name = map(str.strip, line.split("="))
                direction = direction[6:]

                with log_timed_action(self._logger, 'Loading "%s" model from checkpoint' % direction):
                    model_file = os.path.join(model_path, model_name)
                    self._engines[direction] = NMTEngine.load_from_checkpoint(model_file)

        # Public-editable options
        self.beam_size = 5
        self.max_sent_length = 160

    def get_engine(self, source_lang, target_lang):
        direction = source_lang + '__' + target_lang
        if direction not in self._engines:
            return None
        return self._engines[direction]

    def translate(self, source_lang, target_lang, text, suggestions=None, n_best=1,
                  tuning_epochs=None, tuning_learning_rate=None):
        # (0) Get NMTEngine for current direction; if it does not exist, raise an exception
        engine = self.get_engine(source_lang, target_lang)

        reset_model = False

        # (1) Tune engine if suggestions provided
        if suggestions is not None and len(suggestions) > 0:
            engine.tune(suggestions, epochs=tuning_epochs, learning_rate=tuning_learning_rate)
            reset_model = True

        # (2) Translate
        result = engine.translate(text, n_best=n_best, beam_size=self.beam_size, max_sent_length=self.max_sent_length)

        # (3) Reset model if needed
        if reset_model:
            engine.reset_model()

        return result
