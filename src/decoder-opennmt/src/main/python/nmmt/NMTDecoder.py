import os

import logging
import torch
import torch.cuda.random as random

from nmmt import SubwordTextProcessor, NMTEngine
from nmmt.internal_utils import log_timed_action


class UnsupportedLanguageException(Exception):
    def __init__(self, source_language, target_language):
        self.message = "No engine and text processors found for " + source_language + " -> " + target_language + "."


class IllegalStateException(Exception):
    def __init__(self, source_language, target_language):
        self.message = "Error: illegal internal state for direction " + source_language + " -> " + target_language + "."


class NMTDecoder:
    def __init__(self, model_path, gpu_id=None, random_seed=None):

        self._logger = logging.getLogger('nmmt.NMTDecoder')

        if gpu_id is not None:
            torch.cuda.set_device(gpu_id)

        if random_seed is not None:
            torch.manual_seed(random_seed)
            random.manual_seed_all(random_seed)

        using_cuda = gpu_id is not None

        # map <direction -> TextProcessor>    (the direction is a string <src>__<trg>)
        self._text_processors = {}
        # map <direction -> NMTEngine>    (the direction is a string <src>__<trg>)
        self._engines = {}

        with open(os.path.join(model_path, 'model.map'), "r") as model_map_file:
            model_map_lines = model_map_file.readlines()
            for line in model_map_lines:
                # read from model.map file the translation directions and the corresponding the model;
                direction, model_name = line.strip().split("=")
                direction = direction.strip()
                model_name = model_name.strip()

                # use the directions and models to create and store the text processors and engines
                tp_model = model_name + '.bpe'
                engine_model = model_name + '.pt'
                self._text_processors[direction] = SubwordTextProcessor.load_from_file(
                    os.path.join(model_path, tp_model))
                with log_timed_action(self._logger, 'Loading model from checkpoint'):
                    self._engines[direction] = NMTEngine.load_from_checkpoint(os.path.join(model_path, engine_model),
                                                                              using_cuda=using_cuda)

        # Public-editable options
        self.beam_size = 5
        self.max_sent_length = 160
        self.replace_unk = False
        self.tuning_epochs = 5

    def translate(self, source_lang, target_lang, text, suggestions=None, n_best=1):
        # (0) Get textProcessor and nmtEngine for current direction
        direction = source_lang + '__' + target_lang
        if direction not in self._text_processors.keys() and direction not in self._engines.keys():
            raise UnsupportedLanguageException(source_lang, target_lang)
        elif direction not in self._text_processors.keys() or direction not in self._engines.keys():
            raise IllegalStateException(source_lang, target_lang)

        text_processor = self._text_processors[direction]
        engine = self._engines[direction]

        # (1) Process text and suggestions
        processed_text = text_processor.encode_line(text, is_source=True)
        processed_suggestions = None

        if self.tuning_epochs > 0 and suggestions is not None and len(suggestions) > 0:
            processed_suggestions = [], []

            for suggestion in suggestions:
                processed_suggestions[0].append(text_processor.encode_line(suggestion.source, is_source=True))
                processed_suggestions[1].append(text_processor.encode_line(suggestion.target, is_source=False))

        # (2) Tune engine if suggestions provided
        if processed_suggestions is not None:
            msg = 'Tuning engine on %d suggestions (%d epochs)' % (len(processed_suggestions[0]), self.tuning_epochs)

            with log_timed_action(self._logger, msg, log_start=False):
                engine.tune(*processed_suggestions, epochs=self.tuning_epochs)

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