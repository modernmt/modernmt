import multiprocessing
import os
from collections import defaultdict

import tensorflow as tf
from tensor2tensor.data_generators import problem, text_problems, text_encoder
from tensor2tensor.data_generators import translate
from tensor2tensor.data_generators.text_encoder import SubwordTextEncoder
from tensor2tensor.data_generators.text_problems import VocabType
from tensor2tensor.utils import registry

ENV_MMT_PROBLEM_SOURCE_LANG = 'MMT_PROBLEM_SOURCE_LANG'
ENV_MMT_PROBLEM_TARGET_LANG = 'MMT_PROBLEM_TARGET_LANG'
ENV_MMT_PROBLEM_BPE = 'MMT_PROBLEM_BPE'
ENV_MMT_PROBLEM_TRAIN_PATH = 'MMT_PROBLEM_TRAIN_PATH'
ENV_MMT_PROBLEM_DEV_PATH = 'MMT_PROBLEM_DEV_PATH'


def set_translate_mmt_problem_variables(source_lang, target_lang, train_path, dev_path, bpe_size=2 ** 15):
    os.environ[ENV_MMT_PROBLEM_SOURCE_LANG] = source_lang
    os.environ[ENV_MMT_PROBLEM_TARGET_LANG] = target_lang
    os.environ[ENV_MMT_PROBLEM_BPE] = str(bpe_size)
    os.environ[ENV_MMT_PROBLEM_TRAIN_PATH] = train_path
    os.environ[ENV_MMT_PROBLEM_DEV_PATH] = dev_path


def _env_get_int(name):
    value = os.environ[name]
    if value is None:
        raise ValueError('Missing value for "%s"' % name)
    try:
        return int(value)
    except ValueError:
        raise ValueError('Invalid value for "%s": %s' % (name, value))


def _env_get_string(name):
    value = os.environ[name]
    if value is None or len(value) == 0:
        raise ValueError('Missing value for "%s"' % name)
    return value


def _env_get_folder(name):
    value = os.environ[name]
    if value is None or not os.path.isdir(value):
        raise ValueError('Folder not found: %s' % name)
    return value


def _build_from_token_counts(args):
    token_counts, max_size, iterations, vocab_filepath, reserved_tokens = args

    encoder = SubwordTextEncoder()
    encoder.build_from_token_counts(token_counts, max_size, num_iterations=iterations)

    if vocab_filepath is not None:
        encoder.store_to_file(vocab_filepath)

    return max_size, encoder.vocab_size


class ModernMTSubwordTextEncoder(SubwordTextEncoder):
    def __init__(self, filename=None):
        super(ModernMTSubwordTextEncoder, self).__init__(filename=filename)

    def encode(self, raw_text):
        return self._tokens_to_subtoken_ids(text_encoder.native_to_unicode(raw_text).split(u' '))

    def encode_with_indexes(self, raw_text):
        tokens = text_encoder.native_to_unicode(raw_text).split(u' ')
        subtokens = self._tokens_to_subtoken_strings(tokens)
        subtoken_ids = [self._subtoken_string_to_id[subtoken] for subtoken in subtokens]

        return subtoken_ids, self._get_indexes(subtokens)

    def _tokens_to_subtoken_strings(self, tokens):
        ret = []
        for token in tokens:
            ret.extend(self._token_to_subtoken_strings(token))
        return ret

    def _token_to_subtoken_strings(self, token):
        cache_location = hash(token) % self._cache_size
        cache_key, cache_value = self._cache[cache_location]
        if cache_key == token:
            return cache_value
        ret = self._escaped_token_to_subtoken_strings(text_encoder._escape_token(token, self._alphabet))
        self._cache[cache_location] = (token, ret)
        return ret

    def decode(self, subtoken_ids):
        return text_encoder.unicode_to_native(u' '.join(self._subtoken_ids_to_tokens(subtoken_ids)))

    def decode_with_indexes(self, subtoken_ids):
        subtokens = [self._subtoken_id_to_subtoken_string(subtoken_id) for subtoken_id in subtoken_ids]
        tokens = self._subtoken_strings_to_tokens(subtokens)
        raw_text = text_encoder.unicode_to_native(u' '.join(tokens))

        return raw_text, self._get_indexes(subtokens)

    @staticmethod
    def _subtoken_strings_to_tokens(subtokens):
        concatenated = ''.join(subtokens)
        split = concatenated.split("_")
        ret = []
        for t in split:
            if t:
                unescaped = text_encoder._unescape_token(t + "_")
                if unescaped:
                    ret.append(unescaped)
        return ret

    @staticmethod
    def _get_indexes(subtokens):
        indexes = []
        i = 0
        final = True
        for subtoken in subtokens:
            if subtoken == "_" and final:  # handle the subtoken containing only the EndOfToken. associate it with the same previous token (if final)
                i = i - 1 if i > 0 else 0

            indexes.append(i)

            if subtoken.endswith('_'):
                i += 1
                final = True
            else:
                final = False

        return indexes


class SubwordTextEncoderBuilder(object):
    __INITIAL_MAX_SIZE = 2e3

    def __init__(self, target_size, threads=1, custom_tokens=None):
        self._approx_vocab_size = target_size
        self._threads = threads
        self._reserved_tokens = text_encoder.RESERVED_TOKENS + custom_tokens if custom_tokens is not None else None

    def _run_max_size_attempt(self, max_size, token_counts):
        pool = multiprocessing.Pool(processes=self._threads)

        try:
            max_size_candidates = [int(max_size * 2 ** x) for x in range(self._threads)]

            tf.logging.info("Vocabulary max_size attempt with candidates = %s" % str(max_size_candidates))

            results = pool.map(_build_from_token_counts,
                               [(token_counts, x, 2, None, self._reserved_tokens) for x in max_size_candidates])

            for _max_size, vocab_size in results:
                if vocab_size <= self._approx_vocab_size:
                    return _max_size, True

            last_max_size, last_vocab_size = results[-1]
            tf.logging.info("Failed to identify Vocabulary max_size with last_max_size = %d, last_vocab_size = %d" %
                            (last_max_size, last_vocab_size))

            return last_max_size * 2, False
        finally:
            pool.terminate()

    def build(self, token_counts, vocab_filepath):
        target_size = self._approx_vocab_size

        # Searching the minimum max_size
        max_size = self.__INITIAL_MAX_SIZE

        while True:
            max_size, success = self._run_max_size_attempt(max_size, token_counts)

            if success:
                break

        min_size = 1 if max_size == self.__INITIAL_MAX_SIZE else int(max_size / 2)

        # Generating Vocabulary file
        tf.logging.info("Generating vocab file: %s (min = %d, max = %d)" % (vocab_filepath, min_size, max_size))

        encoder = SubwordTextEncoder.build_to_target_size(target_size, token_counts, min_size, max_size,
                                                          reserved_tokens=self._reserved_tokens)

        if vocab_filepath is not None:
            encoder.store_to_file(vocab_filepath)

        return ModernMTSubwordTextEncoder(vocab_filepath)


@registry.register_problem(name='translate_mmt')
class TranslateModernMT(translate.TranslateProblem):
    @property
    def source_language(self):
        return _env_get_string(ENV_MMT_PROBLEM_SOURCE_LANG)

    @property
    def target_language(self):
        return _env_get_string(ENV_MMT_PROBLEM_TARGET_LANG)

    @property
    def vocab_type(self):
        return VocabType.SUBWORD

    @property
    def approx_vocab_size(self):
        return _env_get_int(ENV_MMT_PROBLEM_BPE)

    @property
    def vocab_filename(self):
        return "model.vcb"

    def source_data_files(self, dataset_split):
        train = dataset_split == problem.DatasetSplit.TRAIN
        folder = _env_get_folder(ENV_MMT_PROBLEM_TRAIN_PATH if train else ENV_MMT_PROBLEM_DEV_PATH)

        source_lang, target_lang = self.source_language, self.target_language
        dataset = {}

        for entry in os.listdir(folder):
            name, lang = os.path.splitext(entry)
            lang = lang[1:]

            ex_source_file, ex_target_file = dataset[name] if name in dataset else (None, None)

            if lang == source_lang:
                dataset[name] = (os.path.join(folder, entry), ex_target_file)
            elif lang == target_lang:
                dataset[name] = (ex_source_file, os.path.join(folder, entry))

        return [(src, tgt) for src, tgt in dataset.itervalues() if src is not None and tgt is not None]

    def generate_samples(self, data_dir, tmp_dir, dataset_split):
        datasets = self.source_data_files(dataset_split)

        src_files = [src for src, tgt in datasets]
        tgt_files = [tgt for src, tgt in datasets]

        base_path = os.path.abspath(os.path.join(src_files[0], os.pardir))
        tf.logging.info("Generating samples from: %s/* (%d corpora)" % (base_path, len(src_files)))

        def iterator():
            log_every_n = 100 if len(src_files) < 10000 else 1000
            i = 0

            for src_file, tgt_file in zip(src_files, tgt_files):
                i += 1

                if i % log_every_n == 0:
                    tf.logging.info("Generating samples: %d corpora out of %d" % (i + 1, len(src_files)))

                for src_line, tgt_line in zip(
                        text_problems.txt_line_iterator(src_file), text_problems.txt_line_iterator(tgt_file)):
                    yield {"inputs": src_line, "targets": tgt_line}

        return iterator()

    def get_or_create_vocab(self, data_dir, tmp_dir, force_get=False):
        if self.vocab_type != VocabType.SUBWORD:
            raise ValueError('Unsupported VocabType: %s' % self.vocab_type)

        vocab_filepath = os.path.join(data_dir, self.vocab_filename)

        if force_get or tf.gfile.Exists(vocab_filepath):
            tf.logging.info('Found vocab file: %s', vocab_filepath)
            return ModernMTSubwordTextEncoder(vocab_filepath)

        # Vocabulary file does not exist: generate vocabulary
        # --------------------------------------------------------------------------------------------------------------

        # Load token counts file if present (or generate if missing)
        tokens_filepath = os.path.join(tmp_dir, 'token_counts.dict')

        if tf.gfile.Exists(tokens_filepath):
            tf.logging.info('Found token counts file: %s', tokens_filepath)
            token_counts = self._load_token_counts(tokens_filepath)
        else:
            tf.logging.info('Generating token counts file: %s', tokens_filepath)
            token_counts = defaultdict(int)

            for item in self.generate_text_for_vocab(data_dir, tmp_dir):
                for tok in text_encoder.native_to_unicode(item).split(u' '):
                    token_counts[tok] += 1

            self._save_token_counts(token_counts, tokens_filepath)

        # Build subword
        builder = SubwordTextEncoderBuilder(self.approx_vocab_size, custom_tokens=self._make_reserved_tokens())
        return builder.build(token_counts, vocab_filepath)

    def _make_reserved_tokens(self):
        return [('${DNT%d}_' % i) for i in range(10)]

    @staticmethod
    def _load_token_counts(filepath):
        token_counts = {}

        with tf.gfile.GFile(filepath, mode='rb') as tokens_file:
            for line in tokens_file:
                line = text_encoder.native_to_unicode(line.rstrip('\n'))
                count, token = line.split(u' ', 1)

                token_counts[token] = int(count)

        return token_counts

    @staticmethod
    def _save_token_counts(token_counts, filepath):
        with tf.gfile.Open(filepath, 'wb') as f:
            for token, count in token_counts.iteritems():
                f.write(str(count) + ' ' + text_encoder.unicode_to_native(token) + '\n')
