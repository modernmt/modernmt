import collections
import logging
import multiprocessing
import os
import re
import tempfile
from itertools import chain

import cachetools
import torch
from fairseq.data import Dictionary

PAD = "<PAD>_"
EOS = "<EOS>_"
UNK = "<UNK>_"
RESERVED_TOKENS = ["<Lua_Heritage>", PAD, EOS, UNK]
PAD_ID = RESERVED_TOKENS.index(PAD)  # Normally 1
EOS_ID = RESERVED_TOKENS.index(EOS)  # Normally 2
UNK_ID = RESERVED_TOKENS.index(UNK)  # Normally 3

_UNESCAPE_REGEX = re.compile(r"\\u|\\\\|\\([0-9]+);")
_ESCAPE_CHARS = set(u"\\_u;0123456789")


def _escape_token(token, alphabet=None):
    """Escape away underscores and OOV characters and append '_'.

    This allows the token to be expressed as the concatenation of a list
    of subtokens from the vocabulary. The underscore acts as a sentinel
    which allows us to invertibly concatenate multiple such lists.

    Args:
      token: A unicode string to be escaped.
      alphabet: A set of all characters in the vocabulary's alphabet.

    Returns:
      escaped_token: An escaped unicode string.

    Raises:
      ValueError: If the provided token is not unicode.
    """
    if not isinstance(token, str):
        raise ValueError("Expected string type for token, got %s" % type(token))

    token = token.replace("\\", "\\\\").replace("_", "\\u")

    if alphabet is not None:
        chars = [c if c in alphabet and c != u"\n" else r"\%d;" % ord(c) for c in token]
        token = ''.join(chars)

    return token + "_"


def _unescape_token(escaped_token):
    """Inverse of _escape_token().

    Args:
      escaped_token: a unicode string

    Returns:
      token: a unicode string
    """

    def match(m):
        if m.group(1) is None:
            return "_" if m.group(0) == "\\u" else "\\"

        try:
            return chr(int(m.group(1)))
        except (ValueError, OverflowError) as _:
            return "\u3013"  # Unicode for undefined character.

    trimmed = escaped_token[:-1] if escaped_token.endswith("_") else escaped_token
    return _UNESCAPE_REGEX.sub(match, trimmed)


def _collect_counts_from_file(filename):
    counter = collections.Counter()

    with open(filename, 'r', encoding='utf-8') as f:
        for line in f.readlines():
            for word in line.strip().split():
                counter[word] += 1

    return counter


def _build_from_token_counts(args):
    counter, min_count, count_threshold = args
    dictionary = SubwordDictionary.build_from_token_counts(counter, min_count,
                                                           num_iterations=2, count_threshold=count_threshold)
    return min_count, len(dictionary)


class _SubwordDictionaryFactory(object):
    __INITIAL_MAX_SIZE = 16e3

    def __init__(self, target_size, vocab_threads=1, custom_tokens=None, padding_factor=8, count_threshold=None):
        self._approx_vocab_size = target_size
        self._vocab_threads = vocab_threads
        self._custom_tokens = [_escape_token(t) for t in custom_tokens] if custom_tokens is not None else []
        self._padding_factor = padding_factor
        self._count_threshold = count_threshold

        self._logger = logging.getLogger("SubwordDictionary::Factory")

    def build(self, files, tmp_path=None):
        if tmp_path is None:
            tmp_path = tempfile.gettempdir()
        else:
            os.makedirs(tmp_path, exist_ok=True)

        token_counts_file = os.path.join(tmp_path, 'token_counts.dict')
        if os.path.isfile(token_counts_file):
            token_counts = self._load_token_counts(token_counts_file)
        else:
            token_counts = self._collect_token_counts(files)
            self._save_token_counts(token_counts, token_counts_file)

        target_size = self._approx_vocab_size

        # Searching the minimum max_size
        max_size = self.__INITIAL_MAX_SIZE

        while True:
            max_size, success = self._run_max_size_attempt(max_size, token_counts)

            if success:
                break

        min_size = 1 if max_size == self.__INITIAL_MAX_SIZE else int(max_size / 2)

        self._logger.info("Generating vocab file: min = %d, max = %d" % (min_size, max_size))
        ret = self._build_to_target_size(target_size, token_counts, min_size, max_size)

        # Pad to padding factor if necessary
        if self._padding_factor > 1 and len(ret) % self._padding_factor > 0:
            ret.force_length(len(ret) + (self._padding_factor - len(ret) % self._padding_factor))

        return ret

    def _collect_token_counts(self, files):
        self._logger.info("Collecting counts BEGIN")
        pool = multiprocessing.Pool(processes=min(os.cpu_count() or 1, 16))

        try:
            counts_array = pool.map(_collect_counts_from_file, files)

            counts = counts_array[0]
            for c in counts_array[1:]:
                counts.update(c)

            self._logger.info("Collecting counts END")
            return counts
        finally:
            pool.terminate()

    @staticmethod
    def _load_token_counts(filename):
        token_counts = {}

        with open(filename, 'r', encoding='utf-8') as f:
            for line in f:
                count, token = line.strip().split(maxsplit=1)
                token_counts[token] = int(count)

        return token_counts

    @staticmethod
    def _save_token_counts(token_counts, filename):
        with open(filename, 'w', encoding='utf-8') as f:
            for token, count in token_counts.items():
                f.write("%d %s\n" % (count, token))

    def _run_max_size_attempt(self, max_size, token_counts):
        pool = multiprocessing.Pool(processes=self._vocab_threads)

        try:
            max_size_candidates = [int(max_size * 2 ** x) for x in range(self._vocab_threads)]

            self._logger.info("Vocabulary max_size attempt with candidates = %s" % str(max_size_candidates))

            results = pool.map(_build_from_token_counts,
                               [(token_counts, x, self._count_threshold) for x in max_size_candidates])

            for _max_size, vocab_size in results:
                if vocab_size <= self._approx_vocab_size:
                    return _max_size, True

            last_max_size, last_vocab_size = results[-1]
            self._logger.info("Failed to identify Vocabulary max_size with last_max_size = %d, last_vocab_size = %d" %
                              (last_max_size, last_vocab_size))

            return last_max_size * 2, False
        finally:
            pool.terminate()

    def _build_to_target_size(self, target_size, token_counts, min_val, max_val, num_iterations=4):
        """Builds a SubwordTextEncoder that has `vocab_size` near `target_size`.

        Uses simple recursive binary search to find a minimum token count that most
        closely matches the `target_size`.

        Args:
          target_size: Desired vocab_size to approximate.
          token_counts: A dictionary of token counts, mapping string to int.
          min_val: An integer; lower bound for the minimum token count.
          max_val: An integer; upper bound for the minimum token count.
          num_iterations: An integer; how many iterations of refinement.

        Returns:
          A SubwordTextEncoder instance.

        Raises:
          ValueError: If `min_val` is greater than `max_val`.
        """
        if min_val > max_val:
            raise ValueError("Lower bound for the minimum token count "
                             "is greater than the upper bound.")
        if target_size < 1:
            raise ValueError("Target size must be positive.")

        reserved_tokens = RESERVED_TOKENS + self._custom_tokens

        def bisect(_min_val, _max_val):
            """Bisection to find the right size."""
            present_count = (_max_val + _min_val) // 2
            self._logger.info("Trying min_count %d" % present_count)
            subtokenizer = SubwordDictionary.build_from_token_counts(
                token_counts, present_count, num_iterations,
                reserved_tokens=reserved_tokens, count_threshold=self._count_threshold)

            # Being within 1% of the target size is ok.
            is_ok = abs(len(subtokenizer) - target_size) * 100 < target_size
            # If min_val == max_val, we can't do any better than this.
            if is_ok or _min_val >= _max_val or present_count < 2:
                return subtokenizer

            if len(subtokenizer) > target_size:
                other_subtokenizer = bisect(present_count + 1, _max_val)
            else:
                other_subtokenizer = bisect(_min_val, present_count - 1)

            if other_subtokenizer is None:
                return subtokenizer

            if abs(len(other_subtokenizer) - target_size) < abs(len(subtokenizer) - target_size):
                return other_subtokenizer
            return subtokenizer

        return bisect(min_val, max_val)


class SubwordDictionary(Dictionary):
    class Factory(_SubwordDictionaryFactory):
        pass

    @classmethod
    def build_from_token_counts(cls, token_counts, min_count, num_iterations=4,
                                reserved_tokens=None, count_threshold=None):
        """Train a SubwordTextEncoder based on a dictionary of word counts.

        Args:
          token_counts: a dictionary of Unicode strings to int.
          min_count: an integer - discard subtokens with lower counts.
          num_iterations: an integer.  how many iterations of refinement.
          reserved_tokens: List of reserved tokens. The global variable
            `RESERVED_TOKENS` must be a prefix of `reserved_tokens`. If this
            argument is `None`, it will use `RESERVED_TOKENS`.
          count_threshold: if specified, skip all tokens with count < count_threshold
            but still uses them for alphabet generation

        Raises:
          ValueError: if reserved is not 0 or len(RESERVED_TOKENS). In this case, it
            is not clear what the space is being reserved for, or when it will be
            filled in.
        """
        logger = logging.getLogger("SubwordDictionary::build_from_token_counts")

        if reserved_tokens is None:
            reserved_tokens = RESERVED_TOKENS
        else:
            # There is not complete freedom in replacing RESERVED_TOKENS.
            for default, proposed in zip(RESERVED_TOKENS, reserved_tokens):
                if default != proposed:
                    raise ValueError("RESERVED_TOKENS must be a prefix of "
                                     "reserved_tokens.")

        sd = SubwordDictionary()

        # Initialize the alphabet. Note, this must include reserved tokens or it can
        # result in encoding failures.
        alphabet_tokens = chain(token_counts.keys(), reserved_tokens)
        sd._init_alphabet_from_tokens(alphabet_tokens)

        # Bootstrap the initial list of subtokens with the characters from the
        # alphabet plus the escaping characters.
        sd._init_subtokens_from_list(list(sd._alphabet) + reserved_tokens)

        # We build iteratively.  On each iteration, we segment all the words,
        # then count the resulting potential subtokens, keeping the ones
        # with high enough counts for our new vocabulary.
        if min_count < 1:
            min_count = 1

        for i in range(num_iterations):
            logger.info("iteration {0} BEGIN".format(i + 1))

            # Collect all substrings of the encoded token that break along current
            # subtoken boundaries.
            subtoken_counts = collections.defaultdict(int)
            for token, count in token_counts.items():
                if count_threshold is not None and count < count_threshold:
                    continue

                escaped_token = _escape_token(token, sd._alphabet)
                subtokens = sd._subtokens_of_escaped(escaped_token)
                start = 0
                for subtoken in subtokens:
                    last_position = len(escaped_token) + 1

                    for end in range(start + 1, last_position):
                        new_subtoken = escaped_token[start:end]
                        subtoken_counts[new_subtoken] += count
                    start += len(subtoken)

            # Array of sets of candidate subtoken strings, by length.
            len_to_subtoken_strings = []
            for subtoken_string, count in subtoken_counts.items():
                lsub = len(subtoken_string)
                if count >= min_count:
                    while len(len_to_subtoken_strings) <= lsub:
                        len_to_subtoken_strings.append(set())
                    len_to_subtoken_strings[lsub].add(subtoken_string)

            # Consider the candidates longest to shortest, so that if we accept
            # a longer subtoken string, we can decrement the counts of its prefixes.
            new_subtoken_strings = []
            for lsub in range(len(len_to_subtoken_strings) - 1, 0, -1):
                subtoken_strings = len_to_subtoken_strings[lsub]
                for subtoken_string in subtoken_strings:
                    count = subtoken_counts[subtoken_string]
                    if count >= min_count:
                        # Exclude alphabet tokens here, as they must be included later,
                        # explicitly, regardless of count.
                        if subtoken_string not in sd._alphabet:
                            new_subtoken_strings.append((count, subtoken_string))
                        for l in range(1, lsub):
                            subtoken_counts[subtoken_string[:l]] -= count

            # Include the alphabet explicitly to guarantee all strings are encodable.
            new_subtoken_strings.extend((subtoken_counts.get(a, 0), a) for a in sd._alphabet)
            new_subtoken_strings.sort(reverse=True)

            # Reinitialize to the candidate vocabulary.
            new_subtoken_strings = [subtoken for _, subtoken in new_subtoken_strings]
            if reserved_tokens:
                new_subtoken_strings = reserved_tokens + new_subtoken_strings

            sd._init_subtokens_from_list(new_subtoken_strings)
            logger.info("iteration %d END, vocab_size = %d" % (i + 1, len(sd)))

        return sd

    def __init__(self, subtokens=None):
        # super().__init__()  - DO NOT CALL

        self.unk_word, self.pad_word, self.eos_word = UNK, PAD, EOS
        self.symbols = []
        self.indices = {}
        self.count = None

        self.pad_index = RESERVED_TOKENS.index(PAD)
        self.eos_index = RESERVED_TOKENS.index(EOS)
        self.unk_index = RESERVED_TOKENS.index(UNK)
        self.nspecial = len(RESERVED_TOKENS)

        self._cache = cachetools.LRUCache(maxsize=2 ** 20)
        self._max_subtoken_len = 0
        self._alphabet = set()

        if subtokens is not None and len(subtokens) > 0:
            self._init_subtokens_from_list(subtokens)
            self._init_alphabet_from_tokens(subtokens)

    def _init_subtokens_from_list(self, subtokens):
        self.symbols = subtokens
        self.indices = {s: i for i, s in enumerate(subtokens) if s}

        # we remember the maximum length of any subtoken to avoid having to
        # check arbitrarily long strings.
        self._max_subtoken_len = max([len(s) for s in subtokens])

    def _init_alphabet_from_tokens(self, tokens):
        # Include all characters from all tokens in the alphabet to guarantee that
        # any token can be encoded. Additionally, include all escaping characters.
        self._alphabet = {c for token in tokens for c in token}
        self._alphabet |= _ESCAPE_CHARS

    def force_length(self, new_length):
        count = new_length - len(self)
        if count < 0:
            raise ValueError('new length (%d) must be greater than current length (%d)' % (new_length, len(self)))

        for i in range(count):
            self.symbols.append('')

    def __getitem__(self, idx):
        if idx < len(self.symbols):
            return self.symbols[idx]
        raise ValueError("invalid id %d" % idx)

    def index(self, sym):
        if sym in self.indices:
            return self.indices[sym]
        raise ValueError("unknown symbol '%s'" % sym)

    def add_symbol(self, word, n=1):
        raise NotImplementedError

    def update(self, new_dict):
        raise NotImplementedError

    def finalize(self, threshold=-1, nwords=-1, padding_factor=8):
        raise NotImplementedError

    @classmethod
    def language_tag(cls, lang):
        return '[[%s]]' % lang

    @classmethod
    def size_of(cls, f):
        if isinstance(f, str):
            with open(f, 'r', encoding='utf-8') as fd:
                return cls.size_of(fd)

        size = 0
        for _ in f:
            size += 1
        return size

    @classmethod
    def load(cls, f, ignore_utf_errors=False):
        if isinstance(f, str):
            try:
                if not ignore_utf_errors:
                    with open(f, 'r', encoding='utf-8') as fd:
                        return cls.load(fd)
                else:
                    with open(f, 'r', encoding='utf-8', errors='ignore') as fd:
                        return cls.load(fd)
            except FileNotFoundError as e:
                raise e
            except Exception:
                raise Exception("Incorrect encoding detected in {}, please "
                                "rebuild the dataset".format(f))

        def unpack(s):
            # Some vocab files wrap words in single quotes, but others don't
            if (s.startswith("'") and s.endswith("'")) or (s.startswith("\"") and s.endswith("\"")):
                return s[1:-1]
            else:
                return s

        return cls(subtokens=[unpack(line.strip()) for line in f])

    def save(self, f):
        if isinstance(f, str):
            with open(f, 'w', encoding='utf-8') as fd:
                return self.save(fd)

        for symbol in self.symbols:
            print("'{}'".format(symbol), file=f)

    def indexes_of(self, subtoken_ids):
        indexes = []
        i = 0

        for j in range(len(subtoken_ids)):
            ### handle "empty" sub_tokens like UNK
            if self[subtoken_ids[j]] == '':
                subtoken_ids[j] = UNK_ID

            _id = subtoken_ids[j]
            if _id == self.eos():
                break
            elif _id != self.pad():
                if j > 0:
                    if self[_id] == '_':
                        x = subtoken_ids[j - 1]
                        if self[x].endswith('_'):
                            continue
                        else:
                            indexes.append(i)
                            i += 1
                    elif self[_id].endswith('_'):
                        indexes.append(i)
                        i += 1
                    else:
                        indexes.append(i)
                else:
                    if self[_id] == '_':
                        continue
                    elif self[_id].endswith('_'):
                        indexes.append(i)
                        i += 1
                    else:
                        indexes.append(i)
        return indexes

    def string(self, tensor, bpe_symbol=None, escape_unk=False):
        if torch.is_tensor(tensor) and tensor.dim() == 2:
            return '\n'.join(self.string(t) for t in tensor)

        concatenated = "".join(self.tokens(tensor))
        ret = []
        for t in concatenated.split("_"):
            if t:
                unescaped = _unescape_token(t + "_")
                if unescaped:
                    ret.append(unescaped)

        return ' '.join(ret)

    def tokens(self, subtoken_ids):
        subtokens = []
        for i in subtoken_ids:
            if i == self.eos():
                break
            elif i != self.pad():
                subtokens.append(self[i])

        return subtokens

    def tokenize(self, raw_text):
        ret = []
        for token in raw_text.strip().split():
            ret.extend(self._subtokens_of(token))
        return ret

    @cachetools.cachedmethod(cache=lambda self: self._cache, key=lambda token: token)
    def _subtokens_of(self, token):
        return self._subtokens_of_escaped(_escape_token(token, self._alphabet))

    def _subtokens_of_escaped(self, escaped_token):
        # NOTE: This algorithm is greedy; it won't necessarily produce the "best"
        # list of subtokens.
        ret = []
        start = 0
        token_len = len(escaped_token)
        while start < token_len:
            for end in range(
                    min(token_len, start + self._max_subtoken_len), start, -1):
                subtoken = escaped_token[start:end]
                if subtoken in self.indices:
                    ret.append(subtoken)
                    start = end
                    break
            else:  # Did not break
                # If there is no possible encoding of the escaped token then one of the
                # characters in the token is not in the alphabet.
                return [self.unk_string()]

        return ret
