import codecs
import copy
import re
from collections import Counter, defaultdict


class BPEEncoderBuilder:
    class VocabularyBuilder:
        def __init__(self):
            self.counter = Counter()

        def add_line(self, line):
            if isinstance(line, str):
                line = line.decode('utf-8')

            if isinstance(line, unicode):
                line = line.strip().split()

            for word in line:
                self.counter[word] += 1

        def build(self):
            return self.counter

    def __init__(self, model):
        self.model = model

    @staticmethod
    def _get_pair_statistics(vocab):
        """Count frequency of all symbol pairs, and create index"""

        # data structure of pair frequencies
        stats = defaultdict(int)

        # index from pairs to words
        indices = defaultdict(lambda: defaultdict(int))

        for i, (word, freq) in enumerate(vocab):
            prev_char = word[0]
            for char in word[1:]:
                stats[prev_char, char] += freq
                indices[prev_char, char][i] += 1
                prev_char = char

        return stats, indices

    @staticmethod
    def _prune_stats(stats, big_stats, threshold):
        """Prune statistics dict for efficiency of max()

        The frequency of a symbol pair never increases, so pruning is generally safe
        (until we the most frequent pair is less frequent than a pair we previously pruned)
        big_stats keeps full statistics for when we need to access pruned items
        """
        for item, freq in stats.items():
            if freq < threshold:
                del stats[item]
                if freq < 0:
                    big_stats[item] += freq
                else:
                    big_stats[item] = freq

    @staticmethod
    def _replace_pair(pair, vocab, indices):
        """Replace all occurrences of a symbol pair ('A', 'B') with a new symbol 'AB'"""
        first, second = pair
        pair_str = ''.join(pair)
        pair_str = pair_str.replace('\\', '\\\\')
        changes = []
        pattern = re.compile(r'(?<!\S)' + re.escape(first + ' ' + second) + r'(?!\S)')

        for j, freq in indices[pair].iteritems():
            if freq < 1:
                continue
            word, freq = vocab[j]
            new_word = ' '.join(word)
            new_word = pattern.sub(pair_str, new_word)
            new_word = tuple(new_word.split())

            vocab[j] = (new_word, freq)
            changes.append((j, new_word, word, freq))

        return changes

    @staticmethod
    def _update_pair_statistics(pair, changed, stats, indices):
        """Minimally update the indices and frequency of symbol pairs

        if we merge a pair of symbols, only pairs that overlap with occurrences
        of this pair are affected, and need to be updated.
        """
        stats[pair] = 0
        indices[pair] = defaultdict(int)
        first, second = pair
        new_pair = first + second
        for j, word, old_word, freq in changed:

            # find all instances of pair, and update frequency/indices around it
            i = 0
            while True:
                # find first symbol
                try:
                    i = old_word.index(first, i)
                except ValueError:
                    break
                # if first symbol is followed by second symbol, we've found an occurrence of pair (old_word[i:i+2])
                if i < len(old_word) - 1 and old_word[i + 1] == second:
                    # assuming a symbol sequence "A B C", if "B C" is merged, reduce the frequency of "A B"
                    if i:
                        prev = old_word[i - 1:i + 1]
                        stats[prev] -= freq
                        indices[prev][j] -= 1
                    if i < len(old_word) - 2:
                        # assuming a symbol sequence "A B C B", if "B C" is merged, reduce the frequency of "C B".
                        # however, skip this if the sequence is A B C B C, because the frequency of "C B" will be
                        # reduced by the previous code block
                        if old_word[i + 2] != first or i >= len(old_word) - 3 or old_word[i + 3] != second:
                            nex = old_word[i + 1:i + 3]
                            stats[nex] -= freq
                            indices[nex][j] -= 1
                    i += 2
                else:
                    i += 1

            i = 0

            while True:
                try:
                    # find new pair
                    i = word.index(new_pair, i)
                except ValueError:
                    break
                # assuming a symbol sequence "A BC D", if "B C" is merged, increase the frequency of "A BC"
                if i:
                    prev = word[i - 1:i + 1]
                    stats[prev] += freq
                    indices[prev][j] += 1
                # assuming a symbol sequence "A BC B", if "B C" is merged, increase the frequency of "BC B"
                # however, if the sequence is A BC BC, skip this step because the count of "BC BC" will be
                # incremented by the previous code block
                if i < len(word) - 1 and word[i + 1] != new_pair:
                    nex = word[i:i + 2]
                    stats[nex] += freq
                    indices[nex][j] += 1
                i += 1

    def learn(self, vocab, symbols=50000, min_frequency=2):
        with codecs.open(self.model, 'w', 'utf-8') as outfile:
            # version 0.2 changes the handling of the end-of-word token ('</w>');
            # version numbering allows backward compatibility
            outfile.write('#version: 0.2\n')

            vocab = dict([(tuple(a[:-1]) + (a[-1] + '</w>',), b) for (a, b) in vocab.items()])
            sorted_vocab = sorted(vocab.items(), key=lambda x: x[1], reverse=True)

            stats, indices = self._get_pair_statistics(sorted_vocab)
            big_stats = copy.deepcopy(stats)
            # threshold is inspired by Zipfian assumption, but should only affect speed
            threshold = max(stats.values()) / 10
            for i in range(symbols):
                if stats:
                    most_frequent = max(stats, key=lambda x: (stats[x], x))

                # we probably missed the best pair because of pruning; go back to full statistics
                if not stats or (i and stats[most_frequent] < threshold):
                    self._prune_stats(stats, big_stats, threshold)
                    stats = copy.deepcopy(big_stats)
                    most_frequent = max(stats, key=lambda x: (stats[x], x))
                    # threshold is inspired by Zipfian assumption, but should only affect speed
                    threshold = stats[most_frequent] * i / (i + 10000.0)
                    self._prune_stats(stats, big_stats, threshold)

                if stats[most_frequent] < min_frequency:
                    # No pair has frequency >= min_frequency. Stopping
                    break

                outfile.write(u'{0} {1}\n'.format(*most_frequent))
                changes = self._replace_pair(most_frequent, sorted_vocab, indices)
                self._update_pair_statistics(most_frequent, changes, stats, indices)
                stats[most_frequent] = 0

                if not i % 100:
                    self._prune_stats(stats, big_stats, threshold)

        return BPEEncoder(self.model)


class BPEEncoder:
    def __init__(self, model, separator='@@'):
        self.separator = separator
        self._cache = {}

        with codecs.open(model, encoding='utf-8') as codes:
            # check version information
            first_line = codes.readline()
            if first_line.startswith('#version:'):
                self._version = tuple([int(x) for x in re.sub(r'(\.0+)*$', '', first_line.split()[-1]).split('.')])
            else:
                self._version = (0, 1)
                codes.seek(0)

            self._bpe_codes = [tuple(item.split()) for item in codes]
            # some hacking to deal with duplicates (only consider first instance)
            self._bpe_codes = dict([(code, i) for (i, code) in reversed(list(enumerate(self._bpe_codes)))])

            self._bpe_codes_reverse = dict([(pair[0] + pair[1], pair) for pair, i in self._bpe_codes.items()])

    def encode_line(self, line):
        if isinstance(line, str):
            line = line.decode('utf-8')

        if isinstance(line, unicode):
            line = line.strip().split()

        output = []
        for word in line:
            new_word = [out for out in self._encode(word)]

            for item in new_word[:-1]:
                output.append(item + self.separator)
            output.append(new_word[-1])

        return output

    def decode_line(self, tokens):
        return ' '.join(tokens).replace(self.separator + ' ', '')

    def _encode(self, orig):
        if orig in self._cache:
            return self._cache[orig]

        if self._version == (0, 1):
            word = tuple(orig) + ('</w>',)
        elif self._version == (0, 2):  # more consistent handling of word-final segments
            word = tuple(orig[:-1]) + (orig[-1] + '</w>',)
        else:
            raise NotImplementedError

        pairs = self._get_pairs(word)

        if not pairs:
            return orig

        while True:
            bigram = min(pairs, key=lambda pair: self._bpe_codes.get(pair, float('inf')))
            if bigram not in self._bpe_codes:
                break
            first, second = bigram
            new_word = []
            i = 0
            while i < len(word):
                try:
                    j = word.index(first, i)
                    new_word.extend(word[i:j])
                    i = j
                except:
                    new_word.extend(word[i:])
                    break

                if word[i] == first and i < len(word) - 1 and word[i + 1] == second:
                    new_word.append(first + second)
                    i += 2
                else:
                    new_word.append(word[i])
                    i += 1
            new_word = tuple(new_word)
            word = new_word
            if len(word) == 1:
                break
            else:
                pairs = self._get_pairs(word)

        # don't print end-of-word symbols
        if word[-1] == '</w>':
            word = word[:-1]
        elif word[-1].endswith('</w>'):
            word = word[:-1] + (word[-1].replace('</w>', ''),)

        self._cache[orig] = word

        return word

    @staticmethod
    def _get_pairs(word):
        """Return set of symbol pairs in a word.

        word is represented as tuple of symbols (symbols being variable-length strings)
        """
        pairs = set()
        prev_char = word[0]
        for char in word[1:]:
            pairs.add((prev_char, char))
            prev_char = char

        return pairs
