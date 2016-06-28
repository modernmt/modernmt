import os
import xml.sax
from multiprocessing import Lock
from operator import attrgetter

from cli.libs import fileutils

__author__ = 'Davide Caroselli'


class ParallelCorpus:
    @staticmethod
    def list(root='.'):
        name2langs = {}

        for filename in os.listdir(root):
            filepath = os.path.join(root, filename)

            if os.path.isfile(filepath):
                filename, extension = os.path.splitext(filename)
                extension = extension[1:]

                if len(filename) == 0:
                    continue

                if extension.lower() == 'tmx':
                    name2langs[filename] = None
                else:
                    langs = name2langs[filename] if filename in name2langs else None
                    if langs is None:
                        langs = []
                        name2langs[filename] = langs
                    langs.append(extension)

        corpora = [ParallelCorpus.__build(name, root, langs) for name, langs in name2langs.iteritems()]

        return sorted(corpora, key=attrgetter('name'))

    @staticmethod
    def __build(name, root, langs=None):
        return TMXParallelCorpus(name, root) if langs is None else FileParallelCorpus(name, root, langs)

    @staticmethod
    def splitlist(source_lang, target_lang, monolingual_is_target=True, roots=None):
        if roots is None:
            roots = ['.']
        elif not type(roots) is list:
            roots = [roots]

        monolingual_corpora = []
        bilingual_corpora = []

        monolingual_lang = target_lang if monolingual_is_target else source_lang

        for directory in roots:
            corpora = ParallelCorpus.list(directory)

            for corpus in corpora:
                if len(corpus.langs) == 1:
                    if monolingual_lang in corpus.langs:
                        monolingual_corpora.append(corpus)
                elif len(corpus.langs) > 1:
                    if source_lang in corpus.langs and target_lang in corpus.langs:
                        bilingual_corpora.append(corpus)

        return bilingual_corpora, monolingual_corpora

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

    def get_basename(self):
        raise NotImplementedError('Abstract method')

    def get_file(self, lang):
        raise NotImplementedError('Abstract method')

    def count_lines(self):
        raise NotImplementedError('Abstract method')

    def __str__(self):
        return self.name + '(' + ','.join(self.langs) + ')'

    def __repr__(self):
        return self.__str__()


class FileParallelCorpus(ParallelCorpus):
    def __init__(self, name, root, langs=None):
        ParallelCorpus.__init__(self, name, root, langs)

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
                    self._lines_count = fileutils.linecount(self.get_file(self.langs[0]))

        return self._lines_count


class _TMXContentReader(xml.sax.handler.ContentHandler):
    langs = []

    def startElement(self, name, attrs):
        if name == 'tuv':
            lang = str(attrs['xml:lang']) if 'xml:lang' in attrs else None

            if lang is not None:
                idx = lang.find('-')
                if idx > 0:
                    lang = lang[:idx]

                if lang not in self.langs:
                    self.langs.append(lang)

                    if len(self.langs) > 1:
                        raise StopIteration


class TMXParallelCorpus(ParallelCorpus):
    @staticmethod
    def __get_langs(name, root):
        handler = _TMXContentReader()

        try:
            parser = xml.sax.make_parser()
            parser.setContentHandler(handler)

            with open(os.path.join(root, name + '.tmx')) as f:
                parser.parse(f)
        except StopIteration:
            pass

        return handler.langs

    def __init__(self, name, root):
        ParallelCorpus.__init__(self, name, root, self.__get_langs(name, root))

    def get_basename(self):
        return os.path.join(self.root, self.name)

    def get_file(self, lang):
        raise NotImplementedError('Cannot read lang file for TMX')

    def count_lines(self):
        raise NotImplementedError('Count lines not supported for TMX')

    def __str__(self):
        return self.name + '[' + ','.join(self.langs) + ']'

