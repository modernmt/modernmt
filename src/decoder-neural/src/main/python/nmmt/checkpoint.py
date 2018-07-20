import json
import os
from collections import defaultdict

import numpy as np
import tensorflow as tf
from tensor2tensor.utils import trainer_lib
from tensorflow.contrib.training import HParams
from tensorflow.python.ops import control_flow_ops, state_ops

from nmmt import UnsupportedLanguageException


class SymbolModalityShard(object):
    @staticmethod
    def match(name):
        return 'symbol_modality' in name

    @staticmethod
    def concat(shards):
        tensors = [shard.tensor for shard in sorted(shards, key=lambda x: x.index)]
        return SymbolModalityShard(shards[0].key, 0, np.concatenate(tensors, 0))

    @staticmethod
    def parse(name, tensor):
        shard_number = None
        components = []

        for split in name.split('/'):
            if split.startswith('symbol_modality_'):
                parts = split.split('_')
                parts[2] = '{vocab_size}'
                split = '_'.join(parts)
            elif split.startswith('weights_'):
                shard_number = int(split.split('_')[1])
                split = 'weights_{shard}'

            components.append(split)

        return SymbolModalityShard('/'.join(components), shard_number, tensor)

    def __init__(self, key, index, tensor):
        self.key = key
        self.index = index
        self.tensor = tensor

    def __str__(self):
        return '%s(%d)' % (self.key, self.index)

    def __repr__(self):
        return 'SymbolModalityShard(%d)' % self.index

    def make_key(self, size):
        return self.key.replace('{vocab_size}', str(size)).replace('{shard}', str(self.index))

    def pad_to(self, size):
        num_pads = size - int(self.tensor.shape[0])
        pad_size = int(self.tensor[0].shape[0])
        dtype = self.tensor[0].dtype

        padding = np.zeros(shape=(num_pads, pad_size), dtype=dtype)

        self.tensor = np.concatenate((self.tensor, padding), 0)

    def slice(self, num_slices):
        vocab_size = int(self.tensor.shape[0])
        shard_sizes = []
        last_shard = 0

        for i in xrange(num_slices - 1):
            shard_size = (vocab_size // num_slices) + (1 if i < vocab_size % num_slices else 0)
            shard_sizes.append(shard_size + last_shard)
            last_shard += shard_size

        return [SymbolModalityShard(self.key, i, t) for i, t in enumerate(np.split(self.tensor, shard_sizes, 0))]


class Checkpoint(object):
    def __init__(self, hparams, symbols=None):
        self.hparams = hparams
        self._path = tf.train.get_checkpoint_state(hparams.data_dir).model_checkpoint_path

        if symbols is not None and symbols > self.encoder.vocab_size:
            self._variables = self._load_variables(self._path, expand_to=symbols)

            self.problem_hparams.input_modality['inputs'] = \
                self._make_modality(self.problem_hparams, self.problem_hparams.input_modality['inputs'], symbols)
            self.problem_hparams.target_modality = \
                self._make_modality(self.problem_hparams, self.problem_hparams.target_modality, symbols)
        else:
            self._variables = self._load_variables(self._path)

    @staticmethod
    def _load_variables(checkpoint_path, expand_to=None):
        reader = tf.contrib.framework.load_checkpoint(checkpoint_path)
        var_list = tf.contrib.framework.list_variables(checkpoint_path)
        var_values = {}

        symbol_modalities = defaultdict(list)

        for name, shape in var_list:
            tensor = reader.get_tensor(name)

            if expand_to is not None and SymbolModalityShard.match(name):
                shard = SymbolModalityShard.parse(name, tensor)
                symbol_modalities[shard.key].append(shard)
            else:
                var_values[name] = tensor

        # Reshape symbol modalities
        if len(symbol_modalities) > 0:
            for key, shards in symbol_modalities.iteritems():
                num_shards = len(shards)

                shard = SymbolModalityShard.concat(shards)
                shard.pad_to(expand_to)

                for shard in shard.slice(num_shards):
                    var_values[shard.make_key(expand_to)] = shard.tensor

        return var_values

    @staticmethod
    def _make_modality(hparams, modality, vocab_size):
        cls = type(modality)
        return ('symbol', vocab_size) if cls == tuple else cls(hparams, vocab_size)

    def __eq__(self, o):
        if isinstance(self, o.__class__):
            return self._path == o._path
        return False

    def __ne__(self, o):
        return not (self == o)

    def __hash__(self):
        return hash(self._path)

    @property
    def problem_hparams(self):
        return self.hparams.problem_hparams

    @property
    def encoder(self):
        return self.problem_hparams.vocabulary['inputs']

    @property
    def decoder(self):
        return self.problem_hparams.vocabulary['targets']

    def variables(self):
        for name, value in self._variables.iteritems():
            yield name, value


class CheckpointPool(object):
    class Builder(object):
        def __init__(self):
            self._checkpoints_by_name = {}

        def register(self, name, checkpoint_path):
            # Normalize name (it can be lowercased by ConfigParser)
            source, target = name.split('__')
            name = '%s__%s' % (self._normalize_lang(source), self._normalize_lang(target))

            if name in self._checkpoints_by_name:
                raise ValueError('Checkpoint with name "%s" already registered' % name)

            self._checkpoints_by_name[name] = self._load_hparams(checkpoint_path)

            return self

        def build(self):
            max_symbols = max([self._get_symbols(hparams) for hparams in self._checkpoints_by_name.itervalues()])

            return CheckpointPool({
                name: Checkpoint(hparams, symbols=max_symbols)
                for name, hparams in self._checkpoints_by_name.iteritems()
            })

        @staticmethod
        def _load_hparams(path):
            with open(os.path.join(path, 'hparams.json'), 'rb') as json_file:
                hparams_dict = {
                    k.encode('utf-8'): v.encode('utf-8') if type(v) == unicode else v
                    for k, v in json.load(json_file).iteritems()
                }

                hparams = HParams(**hparams_dict)
                hparams.set_hparam('data_dir', path)

            trainer_lib.add_problem_hparams(hparams, 'translate_mmt')

            # Removing dropout from HParams even on TRAIN mode
            for key in hparams.values():
                if key.endswith("dropout"):
                    setattr(hparams, key, 0.0)

            return hparams

        @staticmethod
        def _get_symbols(hparams):
            return hparams.problem_hparams.input_modality['inputs'][1]

        @staticmethod
        def _target_langs(keys):
            return set([key.split('__', 1)[1] for key in keys])

        @staticmethod
        def _normalize_lang(lang):
            if '-' in lang:
                a, b = lang.split('-')
                return '%s-%s' % (a.lower(), b.upper())
            else:
                return lang.lower()

    def __init__(self, checkpoint_map):
        self._checkpoints = checkpoint_map
        self._len = len(set(checkpoint_map.values()))

    def __len__(self):
        return self._len

    def __getitem__(self, item):
        if item is None:
            for c in self._checkpoints.itervalues():
                return c
            raise ValueError('No checkpoint registered')

        source_lang, target_lang = item

        if source_lang is None or target_lang is None:
            raise ValueError(item)

        key = '%s__%s' % (source_lang, target_lang)

        if key in self._checkpoints:
            return self._checkpoints[key]
        else:
            raise UnsupportedLanguageException(source_lang, target_lang)

    @property
    def hparams(self):
        for checkpoint in self._checkpoints.itervalues():
            return checkpoint.hparams
        raise ValueError('No checkpoint registered')

    def restorer(self):
        return CheckpointRestorer(self._checkpoints.values())


class CheckpointRestorer(object):
    def __init__(self, checkpoints):
        self._checkpoints = checkpoints
        self._feeds = {}
        self._model_assign_op = None  # Lazy load

    def _init(self):
        global_variables = {var.name: var for var in tf.global_variables()}

        placeholders = {}
        to_model_assign_ops = []

        for name, _ in self._checkpoints[0].variables():
            global_name = name + ':0'

            if global_name in global_variables:
                global_variable = global_variables[global_name]
                placeholder = tf.placeholder(global_variable.dtype)
                placeholders[name] = placeholder

                to_model_assign_ops.append(state_ops.assign(global_variable, placeholder))

        self._model_assign_op = control_flow_ops.group(*to_model_assign_ops)

        for checkpoint in self._checkpoints:
            self._feeds[checkpoint] = {
                placeholders[key]: value for key, value in checkpoint.variables() if key in placeholders
            }

    def restore(self, session, checkpoint):
        # Lazy loading
        if self._model_assign_op is None:
            self._init()

        session.run(self._model_assign_op, feed_dict=self._feeds[checkpoint])
