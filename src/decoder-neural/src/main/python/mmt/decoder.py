import configparser
import logging
import math
import os
import time

import fairseq
import numpy as np
import torch
from fairseq.models.transformer import TransformerModel
from fairseq.sequence_generator import SequenceGenerator
from mmt import textencoder
from mmt.alignment import make_alignment
from mmt.tuning import Tuner, TuningOptions


class Translation(object):
    def __init__(self, text, alignment=None, score=None):
        self.text = text
        self.alignment = alignment
        self.score = score


class Suggestion(object):
    def __init__(self, source_lang, target_lang, segment, translation, score):
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.segment = segment
        self.translation = translation
        self.score = score


class ModelConfig(object):
    __custom_values = {'True': True, 'False': False, 'None': None}

    @classmethod
    def load(cls, model_path):
        config = configparser.ConfigParser()
        config.read(os.path.join(model_path, 'model.conf'))
        return cls(model_path, config)

    def __init__(self, base_path, config_parser):
        self._base_path = base_path
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
    def tuning(self):
        ops = TuningOptions()

        if self._config.has_section('settings'):
            for name, value in self._config.items('settings'):
                if not hasattr(ops, name):
                    raise ValueError('Invalid option "%s"' % name)
                setattr(ops, name, self._parse(value))

        return ops

    @property
    def checkpoints(self):
        def normalize_lang(lang):
            if '-' in lang:
                a, b = lang.split('-')
                return '%s-%s' % (a.lower(), b.upper())
            else:
                return lang.lower()

        result = []

        for name, value in self._config.items('models'):
            if not os.path.isabs(value):
                value = os.path.join(self._base_path, value)

            source, target = name.split('__')
            name = '%s__%s' % (normalize_lang(source), normalize_lang(target))

            result.append((name, value))

        return result


class MMTDecoder(object):
    @classmethod
    def _create_model(cls, checkpoints, device, beam_size, use_fp16):
        model = TransformerModel.build_model(checkpoints.args, checkpoints.task)

        # Custom make_generation_fast_
        eval_fn, train_fn = model.eval, model.train
        model.eval = lambda: None

        model.make_generation_fast_(
            beamable_mm_beam_size=None if beam_size == 0 else beam_size,
            need_attn=True,  # --print-alignment
        )

        model.eval, model.train = eval_fn, train_fn

        if device is not None:
            torch.cuda.set_device(device)
            model = model.cuda(device)

        if use_fp16:
            model.half()

        return model

    @classmethod
    def _create_translator(cls, checkpoints, beam_size):
        return SequenceGenerator(
            checkpoints.task.target_dictionary, beam_size=beam_size,
            max_len_a=0, max_len_b=1, min_len=1,
            stop_early=True, normalize_scores=True,
            len_penalty=1, unk_penalty=0,
            sampling=False, sampling_topk=-1, temperature=1,
            diverse_beam_groups=1, diverse_beam_strength=0.5
        )

    @classmethod
    def _create_tuner(cls, checkpoints, model, tuning_ops, device):
        return Tuner(checkpoints.args, checkpoints.task, model, tuning_ops=tuning_ops, device=device)

    def __init__(self, checkpoints, device=None, beam_size=5, use_fp16=False, tuning_ops=None):
        torch.manual_seed(checkpoints.args.seed)

        self._checkpoints = checkpoints
        self._device = device
        self._model = self._create_model(checkpoints, device=device, beam_size=beam_size, use_fp16=use_fp16)
        self._translator = self._create_translator(checkpoints, beam_size)
        self._tuner = self._create_tuner(checkpoints, self._model, tuning_ops, device)
        self._max_positions = fairseq.utils.resolve_max_positions(
            checkpoints.task.max_positions(),
            self._model.max_positions(),
        )

        self._logger = logging.getLogger('Transformer')

        self._nn_needs_reset = True
        self._checkpoint = None

    # - High level functions -------------------------------------------------------------------------------------------

    def test(self):
        test_batch, _, _ = self._make_batch([])

        begin = time.time()
        self._translator.max_len_b = 1
        self._translator.generate([self._model], test_batch)
        test_time = time.time() - begin

        self._logger.info('test_time = %.3f' % test_time)

    def translate(self, source_lang, target_lang, batch, suggestions=None,
                  tuning_epochs=None, tuning_learning_rate=None, forced_translation=None):
        # (1) Reset model (if necessary)
        begin = time.time()
        self._reset_model(source_lang, target_lang)
        reset_time = time.time() - begin

        # (2) Tune engine if suggestions provided
        begin = time.time()
        if suggestions is not None and len(suggestions) > 0:
            self._tune(suggestions, epochs=tuning_epochs, learning_rate=tuning_learning_rate)
        tune_time = time.time() - begin

        # (3) Translate and compute word alignment
        begin = time.time()
        if forced_translation is not None:
            result = self._force_decode(target_lang, batch, forced_translation)
        else:
            result = self._decode(source_lang, target_lang, batch)

        decode_time = time.time() - begin

        self._logger.info('reset_time = %.3f, tune_time = %.3f, decode_time = %.3f'
                          % (reset_time, tune_time, decode_time))

        return result

    # - Low level functions --------------------------------------------------------------------------------------------

    def _reset_model(self, source_lang, target_lang):
        checkpoint = self._checkpoints.load(source_lang, target_lang)

        if self._nn_needs_reset or checkpoint != self._checkpoint:
            self._model.load_state_dict(checkpoint.state, strict=True)
            self._checkpoint = checkpoint
            self._nn_needs_reset = False

    def _tune(self, suggestions, epochs=None, learning_rate=None):
        # Set tuning parameters
        if epochs is None or learning_rate is None:
            _epochs, _learning_rate = self._tuner.estimate_tuning_parameters(suggestions)

            epochs = epochs if epochs is not None else _epochs
            learning_rate = learning_rate if learning_rate is not None else _learning_rate

        # Run tuning
        if learning_rate > 0. and epochs > 0:
            sub_dict = self._checkpoint.subword_dictionary

            if self._checkpoint.multilingual_target:
                src_samples = [sub_dict.language_tag(e.target_lang) + ' ' + e.segment for e in suggestions]
            else:
                src_samples = [e.segment for e in suggestions]
            tgt_samples = [e.translation for e in suggestions]

            dataset = self._tuner.dataset(src_samples, tgt_samples, sub_dict)
            self._tuner.tune(dataset, num_iterations=epochs, lr=learning_rate)
            self._nn_needs_reset = True

    def _decode(self, source_lang, target_lang, batch):
        prefix_lang = target_lang if self._checkpoint.multilingual_target else None
        batch, input_indexes, sentence_len = self._make_batch(batch, prefix_lang=prefix_lang)

        # Compute translation
        self._translator.max_len_b = self._checkpoint.decode_length(source_lang, target_lang, sentence_len)
        translations = self._translator.generate([self._model], batch)

        # Decode translation
        sub_dict = self._checkpoint.subword_dictionary

        results = []
        for i, hypo in enumerate(translations):
            hypo = hypo[0]  # (top-1 best nbest)
            hypo_score = math.exp(hypo['score'])
            hypo_tokens = hypo['tokens']
            hypo_indexes = sub_dict.indexes_of(hypo_tokens)
            hypo_str = sub_dict.string(hypo_tokens)
            hypo_attention = np.asarray(hypo['attention'].data.cpu())

            # Make alignment
            if len(hypo_indexes) > 0:
                hypo_alignment = make_alignment(input_indexes[i], hypo_indexes, hypo_attention,
                                                prefix_lang=prefix_lang is not None)
            else:
                hypo_alignment = []

            results.append(Translation(hypo_str, alignment=hypo_alignment, score=hypo_score))

        return results

    def _force_decode(self, target_lang, segments, translations):
        prefix_lang = target_lang if self._checkpoint.multilingual_target else None

        batch = self._make_batch_forced_translation(segments, translations, prefix_lang=prefix_lang)

        src_tokens = batch['src_tokens']
        tgt_tokens = batch['trg_tokens']
        src_indexes = batch['src_indexes']
        tgt_indexes = batch['trg_indexes']
        src_lengths = batch['src_lengths']

        if self._device is not None:
            src_tokens = src_tokens.cuda(self._device)
            src_lengths = src_lengths.cuda(self._device)
            tgt_tokens = tgt_tokens.cuda(self._device)

        self._model.eval()
        _, attn = self._model(src_tokens, src_lengths, tgt_tokens)
        if type(attn) is dict:
            attn = attn['attn']

        results = []
        for i, hypo_attention in enumerate(attn):  # for each entry of the original batch
            hypo_attention = hypo_attention.transpose(0, 1).cpu()
            hypo_attention = hypo_attention[hypo_attention.size(0) - (len(src_indexes[i]) + 1):,
                             hypo_attention.size(1) - (len(tgt_indexes[i]) + 1):]

            # Make alignment
            hypo_alignment = make_alignment(src_indexes[i], tgt_indexes[i], hypo_attention.data.numpy(),
                                            prefix_lang=prefix_lang is not None)

            results.append(Translation(translations[i], alignment=hypo_alignment))

        return results

    def _make_batch(self, lines, prefix_lang=None):
        sub_dict = self._checkpoint.subword_dictionary

        # Add language prefix if multilingual target
        if prefix_lang is not None:
            lines = [sub_dict.language_tag(prefix_lang) + ' ' + text for text in lines]

        # Prepare batch
        if len(lines) > 0:
            tokens = [
                sub_dict.encode_line(text, line_tokenizer=sub_dict.tokenize, add_if_not_exist=False).long()
                for text in lines
            ]
            input_indexes = [sub_dict.indexes_of(el) for el in tokens]
            lengths = torch.LongTensor([t.numel() for t in tokens])
        else:
            input_indexes = [[]]
            tokens = torch.LongTensor([[textencoder.EOS_ID]])
            lengths = torch.LongTensor([1])

        max_length = torch.max(lengths)

        # Apply padding
        if len(lines) > 1:
            tokens = [torch.nn.functional.pad(el, (max_length - el.size(0), 0), value=sub_dict.pad()) for el in tokens]

        # Reshape tokens tensor
        if len(lines) > 1:
            tokens = torch.cat(tokens)
        else:
            tokens = tokens[0]

        tokens = tokens.reshape([max(1, len(lines)), max_length])

        if self._device is not None:
            tokens = tokens.cuda(self._device)
            lengths = lengths.cuda(self._device)

        batch = {'net_input': {
            'src_tokens': tokens,
            'src_lengths': lengths
        }}

        return batch, input_indexes, max_length

    def _make_batch_forced_translation(self, segments, translations, prefix_lang=None):
        sub_dict = self._checkpoint.subword_dictionary

        # Add language prefix if multilingual target
        if prefix_lang is not None:
            segments = [sub_dict.language_tag(prefix_lang) + ' ' + text for text in segments]

        # Prepare batch
        if len(segments) > 0:
            src_tokens = [
                sub_dict.encode_line(text, line_tokenizer=sub_dict.tokenize, add_if_not_exist=False).long()
                for text in segments
            ]
            src_indexes = [sub_dict.indexes_of(el) for el in src_tokens]
            src_lengths = torch.LongTensor([t.numel() for t in src_tokens])
            trg_tokens = [
                sub_dict.encode_line(text, line_tokenizer=sub_dict.tokenize, add_if_not_exist=False).long()
                for text in translations
            ]
            trg_indexes = [sub_dict.indexes_of(el) for el in trg_tokens]
            trg_tokens = [torch.cat((text[-1:], text[:-1])) for text in trg_tokens]

            trg_lengths = torch.LongTensor([t.numel() for t in trg_tokens])
        else:
            src_tokens = torch.LongTensor([[textencoder.EOS_ID]])
            src_indexes = [[]]
            src_lengths = torch.LongTensor([1])
            trg_tokens = torch.LongTensor([[textencoder.EOS_ID]])
            trg_indexes = [[]]
            trg_lengths = torch.LongTensor([1])

        max_src_length = torch.max(src_lengths)
        max_trg_length = torch.max(trg_lengths)

        # Apply padding
        if len(segments) > 1:
            src_tokens = [torch.nn.functional.pad(el, (max_src_length - el.size(0), 0), value=sub_dict.pad())
                          for el in src_tokens]
            trg_tokens = [torch.nn.functional.pad(el, (max_trg_length - el.size(0), 0), value=sub_dict.pad())
                          for el in trg_tokens]

        # Reshape tokens tensor
        if len(segments) > 1:
            src_tokens = torch.cat(src_tokens)
            trg_tokens = torch.cat(trg_tokens)
        else:
            src_tokens = src_tokens[0]
            trg_tokens = trg_tokens[0]

        src_tokens = src_tokens.reshape([max(1, len(segments)), max_src_length])
        trg_tokens = trg_tokens.reshape([max(1, len(translations)), max_trg_length])

        if self._device is not None:
            src_tokens = src_tokens.cuda(self._device)
            trg_tokens = trg_tokens.cuda(self._device)

        batch = {
            'src_tokens': src_tokens,
            'trg_tokens': trg_tokens,
            'src_indexes': src_indexes,
            'trg_indexes': trg_indexes,
            'src_lengths': src_lengths
        }

        return batch
