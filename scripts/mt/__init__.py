import os
from multiprocessing import Lock
from operator import attrgetter

__author__ = 'Davide Caroselli'


class ParallelCorpus:
    @staticmethod
    def list(root='.'):
        name2corpus = {}

        for filename in os.listdir(root):
            filepath = os.path.join(root, filename)
            if os.path.isfile(filepath):
                filename, extension = os.path.splitext(filename)
                extension = extension[1:]

                if filename in name2corpus:
                    corpus = name2corpus[filename]
                else:
                    corpus = (filename, root, [])
                    name2corpus[filename] = corpus

                corpus[2].append(extension)

        return sorted([ParallelCorpus(*corpus) for _, corpus in name2corpus.iteritems()], key=attrgetter('name'))

    @staticmethod
    def filter(corpora, lang):
        result = []

        for corpus in corpora:
            result.append(ParallelCorpus(corpus.name, corpus.root, [lang]))

        return result

    def __init__(self, name, root, langs=None):
        self.name = name
        self.langs = langs if langs is not None else []
        self.root = os.path.abspath(root)

        self._lines_count = -1
        self._lock = Lock()

    def get_basename(self):
        return os.path.join(self.root, self.name)

    def get_file(self, lang):
        return self.get_basename() + '.' + lang

    def count_lines(self):
        if self._lines_count < 0 < len(self.langs):
            with self._lock:
                if self._lines_count < 0 < len(self.langs):
                    with open(self.get_file(self.langs[0])) as document:
                        count = 0
                        for _, _ in enumerate(document):
                            count += 1
                        self._lines_count = count

        return self._lines_count

    def __str__(self):
        return self.name + '(' + ','.join(self.langs) + ')'

    def __repr__(self):
        return self.__str__()