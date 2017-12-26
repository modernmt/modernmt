import codecs
import os
from collections import Counter
from collections import defaultdict

import re

import copy

import sys
from multiprocessing import Pipe, Process


class _BPE:
    def __init__(self, codes, separator):
        self.separator = separator
        self.bpe_codes = codes

        self._cache = {}
        self._bpe_codes_reverse = dict([(pair[0] + pair[1], pair) for pair, _ in self.bpe_codes.iteritems()])

    # Learning
    # ------------------------------------------------------------------------------------------------------------------

    @staticmethod
    def learn_from_terms(terms, symbols, min_frequency, separator):
        codes = []

        vocab = dict([(tuple(a[:-1]) + (a[-1] + '</w>',), b) for (a, b) in terms.items()])
        sorted_vocab = sorted(vocab.items(), key=lambda x: x[1], reverse=True)

        stats, indices = _BPE._get_pair_statistics(sorted_vocab)
        big_stats = copy.deepcopy(stats)
        # threshold is inspired by Zipfian assumption, but should only affect speed
        threshold = max(stats.values()) / 10
        for i in xrange(symbols):
            if stats:
                most_frequent = max(stats, key=lambda x: (stats[x], x))

            # we probably missed the best pair because of pruning; go back to full statistics
            if not stats or (i and stats[most_frequent] < threshold):
                _BPE._prune_stats(stats, big_stats, threshold)
                stats = copy.deepcopy(big_stats)
                most_frequent = max(stats, key=lambda x: (stats[x], x))
                # threshold is inspired by Zipfian assumption, but should only affect speed
                threshold = stats[most_frequent] * i / (i + 10000.0)
                _BPE._prune_stats(stats, big_stats, threshold)

            if stats[most_frequent] < min_frequency:
                # No pair has frequency >= min_frequency. Stopping
                break

            codes.append(most_frequent)
            changes = _BPE._replace_pair(most_frequent, sorted_vocab, indices)
            _BPE._update_pair_statistics(most_frequent, changes, stats, indices)
            stats[most_frequent] = 0

            if not i % 100:
                _BPE._prune_stats(stats, big_stats, threshold)

        # some hacking to deal with duplicates (only consider first instance)
        codes = dict([(code, i) for (i, code) in reversed(list(enumerate(codes)))])

        return _BPE(codes, separator=separator)

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

    # Applying
    # ------------------------------------------------------------------------------------------------------------------

    def apply(self, tokens, vocabulary=None):
        output = []
        for word in tokens:
            new_word = [out for out in self._encode(word, vocabulary)]

            for item in new_word[:-1]:
                output.append(item + self.separator)
            output.append(new_word[-1])

        return output

    def _encode(self, _word, vocabulary=None):
        if _word in self._cache:
            return self._cache[_word]

        word = tuple(_word[:-1]) + (_word[-1] + '</w>',)

        pairs = self._get_pairs(word)

        if not pairs:
            return _word

        while True:
            bigram = min(pairs, key=lambda pair: self.bpe_codes.get(pair, float('inf')))
            if bigram not in self.bpe_codes:
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

        if vocabulary is not None:
            word = self._check_vocab_and_split(word, vocabulary)

        self._cache[_word] = word
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

    def _check_vocab_and_split(self, orig, vocabulary):
        """Check for each segment in word if it is in-vocabulary,
        and segment OOV segments into smaller units by reversing the BPE merge operations"""

        out = []

        for segment in orig[:-1]:
            if segment + self.separator in vocabulary:
                out.append(segment)
            else:
                for item in self._recursive_split(segment, vocabulary, False):
                    out.append(item)

        segment = orig[-1]
        if segment in vocabulary:
            out.append(segment)
        else:
            for item in self._recursive_split(segment, vocabulary, True):
                out.append(item)

        return out

    def _recursive_split(self, segment, vocabulary, final=False):
        """Recursively split segment into smaller units (by reversing BPE merges)
        until all units are either in-vocabulary, or cannot be split futher."""

        try:
            if final:
                left, right = self._bpe_codes_reverse[segment + '</w>']
                right = right[:-4]
            else:
                left, right = self._bpe_codes_reverse[segment]
        except:
            yield segment
            return

        if left + self.separator in vocabulary:
            yield left
        else:
            for item in self._recursive_split(left, vocabulary, False):
                yield item

        if (final and right in vocabulary) or (not final and right + self.separator in vocabulary):
            yield right
        else:
            for item in self._recursive_split(right, vocabulary, final):
                yield item


class SubwordTextProcessor:
    @staticmethod
    def load_from_file(path):
        with codecs.open(path, mode='r', encoding='utf-8') as inp:
            separator = inp.readline().rstrip('\n')

            source_codes = dict()
            for i in xrange(int(inp.readline().rstrip('\n'))):
                left, right, i = inp.readline().rstrip('\n').split()
                source_codes[(left, right)] = int(i)

            source_terms = set()
            for i in xrange(int(inp.readline().rstrip('\n'))):
                source_terms.add(inp.readline().rstrip('\n'))

            target_codes = dict()
            for i in xrange(int(inp.readline().rstrip('\n'))):
                left, right, i = inp.readline().rstrip('\n').split()
                target_codes[(left, right)] = int(i)

            target_terms = set()
            for i in xrange(int(inp.readline().rstrip('\n'))):
                target_terms.add(inp.readline().rstrip('\n'))

            return SubwordTextProcessor(source_codes=source_codes, source_terms=source_terms,
                                        target_codes=(target_codes if len(target_codes) > 0 else None),
                                        target_terms=target_terms, separator=separator)

    def __init__(self, source_codes, source_terms, target_codes, target_terms, separator):
        self._separator = separator
        self._source_bpe = _BPE(source_codes, separator)
        self._source_terms = source_terms
        self._target_bpe = _BPE(target_codes, separator) if target_codes is not None else None
        self._target_terms = target_terms

    def get_source_terms(self):
        return list(self._source_terms)

    def get_target_terms(self):
        return list(self._target_terms)

    def save_to_file(self, path):
        parent_folder = os.path.abspath(os.path.join(path, os.pardir))
        if not os.path.isdir(parent_folder):
            os.makedirs(parent_folder)

        source_codes = self._source_bpe.bpe_codes
        target_codes = self._target_bpe.bpe_codes if self._target_bpe is not None else dict()

        with codecs.open(path, mode='w', encoding='utf-8') as out:
            out.write(u'%s\n' % self._separator)

            out.write(u'%d\n' % len(source_codes))
            for (left, right), i in source_codes.iteritems():
                out.write(u'%s %s %d\n' % (left, right, i))

            out.write(u'%d\n' % len(self._source_terms))
            for term in self._source_terms:
                out.write(u'%s\n' % term)

            out.write(u'%d\n' % len(target_codes))
            for (left, right), i in target_codes.iteritems():
                out.write(u'%s %s %d\n' % (left, right, i))

            out.write(u'%d\n' % len(self._target_terms))
            for term in self._target_terms:
                out.write(u'%s\n' % term)

    def encode_line(self, line, is_source):
        if isinstance(line, str):
            line = line.decode('utf-8')

        if isinstance(line, unicode):
            line = line.strip().split()

        bpe = self._source_bpe if is_source or (self._target_bpe is None) else self._target_bpe
        return bpe.apply(line, vocabulary=self._source_terms if is_source else self._target_terms)

    def decode_tokens(self, tokens):
        return u' '.join(tokens).replace(self._separator + u' ', u'')

    def get_words_indexes(self, bpe_tokens):
        indexes = []
        i = 0

        for tok in bpe_tokens:
            indexes.append(i)
            if not tok.endswith(self._separator):
                i += 1

        return indexes

    class Builder:
        def __init__(self, symbols, max_vocabulary_size=None, vocab_pruning_threshold=None,
                     min_frequency=2, separator='@@'):
            self._symbols = symbols
            self._max_vocabulary_size = max_vocabulary_size
            self._vocab_pruning_threshold = vocab_pruning_threshold
            self._min_frequency = min_frequency
            self._separator = separator

            self._dictionaries = (Counter(), Counter())

        def build(self, data_sources):
            """
            It builds a new processor from a collection of data sources.
            A data source object must support __enter__ and __exit__ method to open and close the data stream.
            The data source object myst also be iterable returning a pair of strings: source and target.

            :param data_sources: a collection of data source objects
            :return: and instance of Dictionary
            """

            # Create dictionaries and alphabets
            for data_source in data_sources:
                with data_source as stream:
                    for source, target in stream:
                        self._add_line(source, is_source=True)
                        self._add_line(target, is_source=False)

            # Create BPEs
            source_parent_conn, source_child_conn = Pipe()
            source_worker = Process(target=SubwordTextProcessor.Builder._build_bpe,
                                    args=(source_child_conn, self._dictionaries[0], self._symbols, self._min_frequency,
                                          self._separator, self._vocab_pruning_threshold, self._max_vocabulary_size))

            target_parent_conn, target_child_conn = Pipe()
            target_worker = Process(target=SubwordTextProcessor.Builder._build_bpe,
                                    args=(target_child_conn, self._dictionaries[1], self._symbols, self._min_frequency,
                                          self._separator, self._vocab_pruning_threshold, self._max_vocabulary_size))

            source_worker.start()
            target_worker.start()

            source_subwords, source_codes = source_parent_conn.recv()
            target_subwords, target_codes = target_parent_conn.recv()

            source_worker.join()
            target_worker.join()

            # Cleanup
            for counter in self._dictionaries:
                counter.clear()

            return SubwordTextProcessor(source_codes, source_subwords, target_codes, target_subwords, self._separator)

        def _add_line(self, line, is_source=True):
            if isinstance(line, str):
                line = line.decode('utf-8')

            if isinstance(line, unicode):
                line = line.strip().split()

            dictionary = self._dictionaries[0 if is_source else 1]

            for word in line:
                dictionary[word] += 1

        @staticmethod
        def _build_bpe(connection, dictionary, symbols, min_frequency, separator,
                       vocab_pruning_threshold, max_vocabulary_size):
            bpe = _BPE.learn_from_terms(dictionary, symbols, min_frequency, separator)
            subwords = SubwordTextProcessor.Builder._collect_subwords(dictionary, bpe,
                                                                      vocab_pruning_threshold=vocab_pruning_threshold,
                                                                      max_vocabulary_size=max_vocabulary_size)
            connection.send((subwords, bpe.bpe_codes))
            connection.close()

        @staticmethod
        def _collect_subwords(terms, bpe, vocab_pruning_threshold, max_vocabulary_size):
            vocab = Counter()

            for word, count in terms.iteritems():
                for subword in bpe.apply([word]):
                    vocab[subword] += count

            # Prune rare terms
            if vocab_pruning_threshold is not None:
                total = sum(vocab.values())
                counter = 0
                threshold = 0

                for w, c in vocab.most_common():
                    counter += c
                    if counter >= total * vocab_pruning_threshold:
                        threshold = c
                        break

                for w, c in vocab.items():
                    if c < threshold:
                        del vocab[w]

            # Reduce vocabulary
            if max_vocabulary_size is not None and len(vocab) > max_vocabulary_size:
                entries = vocab.most_common()[:max_vocabulary_size]
                vocab = set(x for x, _ in entries)
            else:
                vocab = set(vocab.keys())

            return vocab


def bpe_main(model_path, is_source):
    processor = SubwordTextProcessor.load_from_file(model_path)

    while 1:
        line = sys.stdin.readline()
        if not line:
            break
        print (u' '.join(processor.encode_line(line, is_source))).encode('utf-8')


if __name__ == '__main__':
    bpe_main(sys.argv[1], True if sys.argv[2] == "source" else False)
