import os

import logging
import torch
import torch.cuda.random as random

from nmmt import SubwordTextProcessor, NMTEngine
from nmmt.internal_utils import log_timed_action


class NMTDecoder:
    def __init__(self, model_path, gpu_id=None, random_seed=None):
        self._logger = logging.getLogger('nmmt.NMTDecoder')

        if gpu_id is not None:
            torch.cuda.set_device(gpu_id)

        if random_seed is not None:
            torch.manual_seed(random_seed)
            random.manual_seed_all(random_seed)

        using_cuda = gpu_id is not None

        self._text_processor = SubwordTextProcessor.load_from_file(os.path.join(model_path, 'model.bpe'))
        with log_timed_action(self._logger, 'Loading model from checkpoint'):
            self._engine = NMTEngine.load_from_checkpoint(os.path.join(model_path, 'model.pt'), using_cuda=using_cuda)

        # Public-editable options
        self.beam_size = 5
        self.max_sent_length = 160
        self.replace_unk = False
        self.tuning_epochs = 5

    def translate(self, text, suggestions=None, n_best=1):
        # (1) Process text and suggestions
        processed_text = self._text_processor.encode_line(text, is_source=True)
        processed_suggestions = None

        if self.tuning_epochs > 0 and suggestions is not None and len(suggestions) > 0:
            processed_suggestions = [], []

            for suggestion in suggestions:
                processed_suggestions[0].append(self._text_processor.encode_line(suggestion.source, is_source=True))
                processed_suggestions[1].append(self._text_processor.encode_line(suggestion.target, is_source=False))

        # (2) Tune engine if suggestions provided
        if processed_suggestions is not None:
            with log_timed_action(self._logger, 'Tuning engine'):
                self._engine.tune(*processed_suggestions, epochs=self.tuning_epochs)

        # (3) Translate
        pred_batch, pred_score = self._engine.translate(processed_text,
                                                        n_best=n_best, beam_size=self.beam_size,
                                                        max_sent_length=self.max_sent_length,
                                                        replace_unk=self.replace_unk)

        # (4) Reset engine if needed
        if processed_suggestions is not None:
            with log_timed_action(self._logger, 'Restoring model initial state'):
                self._engine.reset_model()

        return self._text_processor.decode_tokens(pred_batch[0])
