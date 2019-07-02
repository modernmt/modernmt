import argparse
import math
import multiprocessing
import os
import pickle
import shutil
from collections import Counter
from itertools import islice

from cli import CLIArgsException, StatefulActivity, activitystep
from cli.mmt import collect_parallel_files, MMT_FAIRSEQ_USER_DIR
from cli.mmt.mmtcli import mmt_preprocess
from cli.utils import osutils
from mmt.textencoder import SubwordDictionary


def _pool_initializer(vocab_path):
    global bpe_vocab
    bpe_vocab = SubwordDictionary.load(vocab_path)


def _apply_bpe(entry):
    global bpe_vocab

    src_line, tgt_line = entry
    src_line, tgt_line = src_line.strip(), tgt_line.strip()
    src_tokens = bpe_vocab.tokenize(src_line)
    tgt_tokens = bpe_vocab.tokenize(tgt_line)

    if len(src_tokens) == 0 or len(tgt_tokens) == 0:
        return None, None, 0, 0
    else:
        return ' '.join(src_tokens) + '\n', ' '.join(tgt_tokens) + '\n', len(src_tokens), len(tgt_tokens)


class _Sequence(object):
    def __init__(self) -> None:
        self._sum = 0
        self._sum2 = 0
        self._count = 0
        self._counter = Counter()

    def add(self, value):
        self._sum += value
        self._sum2 += value * value
        self._count += 1

        value = int(value * 10) / 10.
        self._counter[value] += 1

    @property
    def modal_value(self):
        return self._counter.most_common(1)[0][0]

    @property
    def avg(self):
        return self._sum / self._count

    @property
    def std_dev(self):
        return math.sqrt((self._sum2 / self._count) - (self.avg * self.avg))

    def __len__(self):
        return self._count


class DatagenActivity(StatefulActivity):
    DEFAULT_VALIDATION_LINES = 3000

    def __init__(self, args, extra_argv=None, wdir=None, log_file=None, start_step=None, delete_on_exit=True):
        super().__init__(args, extra_argv, wdir, log_file, start_step, delete_on_exit)

        self._langs = [tuple(lp.split(':')) for lp in args.lang_pairs.split(',')]
        self._mono_pairs = [tuple(lp.split(':')) for lp in set([('%s:%s' % tuple(sorted(p))) for p in self._langs])]
        self._target_langs = set([p[1] for p in self._langs])

    @activitystep('Tokenize corpora')
    def tokenize(self):
        self.state.tokenized_corpora = self.wdir('tokenized_corpora')

        train_paths, dev_paths = [], []

        partition_size = self.DEFAULT_VALIDATION_LINES
        if len(self._langs) > 1:
            partition_size = int((2 * self.DEFAULT_VALIDATION_LINES) / len(self._langs))

        for src_lang, tgt_lang in self._mono_pairs:
            lang_dir = '%s__%s' % (src_lang, tgt_lang)

            train_path = os.path.join(self.state.tokenized_corpora, lang_dir, 'train')
            raw_dev_path = os.path.join(self.state.tokenized_corpora, lang_dir, '_dev')
            dev_path = os.path.join(self.state.tokenized_corpora, lang_dir, 'dev')

            test_path = os.path.join(self.args.test_dir, lang_dir) if self.args.test_dir is not None else None

            mmt_preprocess(src_lang, tgt_lang, self.args.input_paths, train_path,
                           dev_path=raw_dev_path, test_path=test_path, partition_size=partition_size)
            mmt_preprocess(src_lang, tgt_lang, raw_dev_path, dev_path)

            shutil.rmtree(raw_dev_path)

            train_paths.append(train_path)
            dev_paths.append(dev_path)

    @activitystep('Training BPE model')
    def bpe_create(self):
        os.makedirs(self.args.output_path, exist_ok=True)
        self.state.vocab = os.path.join(self.args.output_path, 'model.vcb')

        # Copy existing model if specified
        if self.args.vocabulary_path is not None:
            shutil.copyfile(self.args.vocabulary_path, self.state.vocab)
            return

        # Create custom tokens list
        custom_tokens = [('${DNT%d}' % i) for i in range(10)]

        if len(self._target_langs) > 1:
            custom_tokens = [SubwordDictionary.language_tag(l) for l in self._target_langs] + custom_tokens

        # Collect all training files
        all_files = []

        for src_lang, tgt_lang in self._mono_pairs:
            lang_dir = '%s__%s' % (src_lang, tgt_lang)
            train_path = os.path.join(self.state.tokenized_corpora, lang_dir, 'train')
            dev_path = os.path.join(self.state.tokenized_corpora, lang_dir, 'dev')

            all_src, all_tgt = collect_parallel_files(src_lang, tgt_lang, [train_path, dev_path])

            all_files.extend(all_src)
            all_files.extend(all_tgt)

        # Build SubwordDictionary
        builder = SubwordDictionary.Factory(self.args.voc_size,
                                            vocab_threads=self.args.threads,
                                            custom_tokens=custom_tokens,
                                            padding_factor=8,
                                            count_threshold=self.args.count_threshold)
        dictionary = builder.build(all_files, tmp_path=self.wdir('bpe_temp'))
        dictionary.save(self.state.vocab)

    def _bpe_encode_files(self, pool, src_lang, tgt_lang,
                          in_src_files, in_tgt_files, out_src_file_obj, out_tgt_file_obj):
        src_prefix, tgt_prefix = None, None
        if len(self._target_langs) > 1:
            src_prefix = SubwordDictionary.language_tag(tgt_lang) + '_ '
            tgt_prefix = SubwordDictionary.language_tag(src_lang) + '_ '

        batch_size = (multiprocessing.cpu_count() or 1) * 100
        bidirectional = ((src_lang, tgt_lang) in self._langs) and ((tgt_lang, src_lang) in self._langs)

        fwd_seq, bwd_seq = _Sequence(), _Sequence()

        for in_src_file, in_tgt_file in zip(in_src_files, in_tgt_files):
            with open(in_src_file, 'r', encoding='utf-8') as in_src_file_obj, \
                    open(in_tgt_file, 'r', encoding='utf-8') as in_tgt_file_obj:
                for batch in iter(lambda: tuple(islice(zip(in_src_file_obj, in_tgt_file_obj), batch_size)), ()):
                    for src_line, tgt_line, src_len, tgt_len in pool.map(_apply_bpe, batch):
                        if src_line is None or tgt_line is None:
                            continue

                        s2t_rate, t2s_rate = tgt_len / src_len, src_len / tgt_len
                        fwd_seq.add(s2t_rate)
                        bwd_seq.add(t2s_rate)

                        if src_prefix is not None:
                            out_src_file_obj.write(src_prefix)
                        out_src_file_obj.write(src_line)
                        out_tgt_file_obj.write(tgt_line)

                        if bidirectional:
                            if tgt_prefix is not None:
                                out_src_file_obj.write(tgt_prefix)
                            out_src_file_obj.write(tgt_line)
                            out_tgt_file_obj.write(src_line)

        return fwd_seq, bwd_seq

    @activitystep('Encoding training corpora')
    def bpe_encode(self):
        self.state.encoded_corpora = out_dir = self.wdir('encoded_corpora')

        train_sl, train_tl = os.path.join(out_dir, 'train.sl'), os.path.join(out_dir, 'train.tl')
        dev_sl, dev_tl = os.path.join(out_dir, 'dev.sl'), os.path.join(out_dir, 'dev.tl')

        covered_langs = set()
        decode_lengths = {}

        with open(train_sl, 'w', encoding='utf-8') as train_sl_obj, \
                open(train_tl, 'w', encoding='utf-8') as train_tl_obj, \
                open(dev_sl, 'w', encoding='utf-8') as dev_sl_obj, \
                open(dev_tl, 'w', encoding='utf-8') as dev_tl_obj:
            with multiprocessing.Pool(initializer=_pool_initializer, initargs=(self.state.vocab,)) as pool:
                for src_lang, tgt_lang in self._langs:
                    lang_dir = '%s__%s' % tuple(sorted([src_lang, tgt_lang]))

                    if lang_dir in covered_langs:
                        continue
                    covered_langs.add(lang_dir)

                    train_path = os.path.join(self.state.tokenized_corpora, lang_dir, 'train')
                    dev_path = os.path.join(self.state.tokenized_corpora, lang_dir, 'dev')

                    train_src_files, train_tgt_files = collect_parallel_files(src_lang, tgt_lang, train_path)
                    dev_src_files, dev_tgt_files = collect_parallel_files(src_lang, tgt_lang, dev_path)

                    fwd_seq, bwd_seq = self._bpe_encode_files(pool, src_lang, tgt_lang,
                                                              train_src_files, train_tgt_files,
                                                              train_sl_obj, train_tl_obj)
                    self._bpe_encode_files(pool, src_lang, tgt_lang,
                                           dev_src_files, dev_tgt_files, dev_sl_obj, dev_tl_obj)

                    decode_lengths['%s__%s' % (src_lang, tgt_lang)] = (fwd_seq.modal_value, fwd_seq.std_dev)
                    decode_lengths['%s__%s' % (tgt_lang, src_lang)] = (bwd_seq.modal_value, bwd_seq.std_dev)

        os.makedirs(self.args.output_path, exist_ok=True)
        with open(os.path.join(self.args.output_path, 'decode_lengths.bin'), 'wb') as f:
            pickle.dump(decode_lengths, f)

    @activitystep('Generating binary data')
    def datagen(self):
        os.makedirs(self.args.output_path, exist_ok=True)

        train_pref = os.path.join(self.state.encoded_corpora, 'train')
        valid_pref = os.path.join(self.state.encoded_corpora, 'dev')
        cmd = ['fairseq-preprocess', '--source-lang', 'sl', '--target-lang', 'tl', '--user-dir', MMT_FAIRSEQ_USER_DIR,
               '--task', 'mmt_translation', '--trainpref', train_pref, '--validpref', valid_pref,
               '--destdir', self.args.output_path, '--workers', str(multiprocessing.cpu_count()),
               '--srcdict', self.state.vocab, '--joined-dictionary', '--dataset-impl', 'mmap']

        osutils.shell_exec(cmd, stdout=self.log_fobj, stderr=self.log_fobj)


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Generate archives for neural training', prog='mmt datagen')
    parser.add_argument('lang_pairs', metavar='LANGUAGE_PAIRS',
                        help='the language pair list encoded as <ls1>:<t1>[,<lsn>:<ltn>] (i.e. en:it,it:en,en:fr)')
    parser.add_argument('output_path', metavar='OUTPUT', help='the destination folder')
    parser.add_argument('input_paths', nargs='+', metavar='INPUT_PATHS', help='the paths to the training corpora')
    parser.add_argument('-w', '--working-dir', metavar='WORKING_DIR', dest='wdir', default=None,
                        help='the working directory for temporary files (default is os temp folder)')
    parser.add_argument('-d', '--debug', action='store_true', dest='debug', default=False,
                        help='prevents temporary files to be removed after execution')
    parser.add_argument('-s', '--voc-size', dest='voc_size', default=32768, type=int,
                        help='the vocabulary size to use (default is 32768)')
    parser.add_argument('-T', '--threads', dest='threads', default=2, type=int,
                        help='the number of threads used to find the bounds for vocabulary creation (default is 2)')
    parser.add_argument('--count-threshold', dest='count_threshold', default=None, type=int,
                        help='all tokens with a count less than this threshold will be used '
                             'only for alphabet generation in vocabulary creation, useful for very large corpus')
    parser.add_argument('--vocabulary', metavar='VOCABULARY_PATH', dest='vocabulary_path', default=None,
                        help='use the specified bpe vocabulary model instead of re-train a new one from scratch')
    parser.add_argument('--log', dest='log_file', default=None, help='detailed log file')
    parser.add_argument('--test', metavar='TEST_SET_DIR', dest='test_dir', default=None,
                        help='optional directory where to store a small subset of training data for testing')

    args = parser.parse_args(argv)
    if args.debug and args.wdir is None:
        raise CLIArgsException(parser, '"--debug" options requires explicit working dir with "--working-dir"')
    return args


def main(argv=None):
    args = parse_args(argv)
    activity = DatagenActivity(args, wdir=args.wdir, log_file=args.log_file, delete_on_exit=not args.debug)
    activity.run()
