import os
from collections import defaultdict

import torch
from fairseq import tasks
from torch.serialization import default_restore_location

from mmt import SubwordDictionary


def _resize_embeddings(tensor, size):
    embed_size = tensor.shape[1]
    stub_embeds_count = size - tensor.shape[0]

    padding = torch.zeros((stub_embeds_count, embed_size), dtype=tensor.dtype)
    tensor = torch.cat((tensor, padding), 0)

    return tensor


class UnsupportedLanguageException(KeyError):
    def __init__(self, source_language, target_language, *args: object) -> None:
        super().__init__("unsupported language: %s > %s" % (source_language, target_language), *args)


class Checkpoint(object):
    def __init__(self, task, model_state, decode_stats, multilingual_target=False):
        self.task = task
        self._decode_stats = decode_stats
        self._state = model_state
        self._multilingual_target = multilingual_target

    @property
    def multilingual_target(self):
        return self._multilingual_target

    @property
    def state(self):
        return self._state

    @property
    def subword_dictionary(self):
        return self.task.source_dictionary

    def decode_length(self, source_lang, target_lang, source_length):
        lang_key = '%s__%s' % (source_lang, target_lang)
        avg, std_dev = self._decode_stats[lang_key]

        return int(source_length * (avg + 4 * std_dev)) + 20

    def __eq__(self, o):
        if isinstance(self, o.__class__):
            return self.task.args.data[0] == o.task.args.data[0]
        return False

    def __ne__(self, o):
        return not (self == o)

    def __hash__(self):
        return hash(self.task.args.data[0])

    def __str__(self):
        return self.__class__.__name__ + '@' + self.task.args.data[0]

    def __repr__(self):
        return str(self)


class CheckpointRegistry(object):
    class Builder(object):
        def __init__(self):
            self._checkpoints_names = set()
            self._checkpoints_by_path = defaultdict(list)
            self._max_vocab_size = 0

        @property
        def embeddings_size(self):
            return self._max_vocab_size

        def register(self, name, checkpoint_path):
            if name in self._checkpoints_names:
                raise ValueError('Checkpoint with name "%s" already registered' % name)

            self._checkpoints_names.add(name)
            self._checkpoints_by_path[checkpoint_path].append(name)

            dict_path = os.path.join(checkpoint_path, 'model.vcb')
            self._max_vocab_size = max(self._max_vocab_size, SubwordDictionary.size_of(dict_path))

            return self

        def build(self, device=None):
            checkpoints = {}

            for path, keys in self._checkpoints_by_path.items():
                args, model_state, decode_stats = self._load(path, self._max_vocab_size)
                task = tasks.setup_task(args)
                task.source_dictionary.force_length(self._max_vocab_size)

                target_languages = set([key.split('__', 1)[1] for key in keys])

                checkpoint = Checkpoint(task, model_state, decode_stats, multilingual_target=len(target_languages) > 1)

                for key in keys:
                    checkpoints[key] = checkpoint

            return CheckpointRegistry(checkpoints, device=device)

        @classmethod
        def _load(cls, checkpoint_path, embeddings_size=None):
            model_pt_path = os.path.join(checkpoint_path, 'model.pt')
            if not os.path.isfile(model_pt_path):
                raise IOError('Model file not found: {}'.format(model_pt_path))

            # Load model from file
            model_pt = torch.load(model_pt_path, map_location=lambda s, l: default_restore_location(s, 'cpu'))
            args, model_state, decode_stats = model_pt['args'], model_pt['model'], model_pt['decode_stats']

            # Resize embeddings
            if embeddings_size is not None and model_state['encoder.embed_tokens.weight'].shape[0] < embeddings_size:
                model_state['encoder.embed_tokens.weight'] = \
                    _resize_embeddings(model_state['encoder.embed_tokens.weight'], embeddings_size)
                model_state['decoder.embed_tokens.weight'] = \
                    _resize_embeddings(model_state['decoder.embed_tokens.weight'], embeddings_size)

            args.data = [checkpoint_path]
            return args, model_state, decode_stats

    def __init__(self, checkpoints, device=None) -> None:
        self._checkpoints = checkpoints
        self._device = device

        sample = None
        for sample in checkpoints.values():
            break

        self.task = sample.task
        self.args = sample.task.args

    def load(self, source_lang, target_lang):
        key = '%s__%s' % (source_lang, target_lang)

        if key in self._checkpoints:
            return self._checkpoints[key]
        else:
            raise UnsupportedLanguageException(source_lang, target_lang)

    def __len__(self):
        return len(self._checkpoints)

    def __str__(self):
        return str(self._checkpoints)

    def __repr__(self):
        return self.__class__.__name__ + str(self._checkpoints)
