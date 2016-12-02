import os
import shutil
import xml.sax
from multiprocessing import Lock
from operator import attrgetter

from cli.libs import fileutils

__author__ = 'Davide Caroselli'


class _CorpusBuilder:
    def __init__(self, name, tmx=None):
        self.name = name
        self.tmx = tmx
        self.lang2file = {}

    def add(self, lang, f):
        self.lang2file[lang] = f

    def build(self):
        return _TMXCorpus(self.name, self.tmx) if self.tmx is not None else _FileParallelCorpus(self.name,
                                                                                                self.lang2file)


def _parse_lang(lang):
    if lang is None:
        return None

    idx = lang.find('-')
    if idx > 0:
        if idx > 3:
            return None
        else:
            lang = lang[:idx]

    return lang.lower()


class BilingualCorpus:
    @staticmethod
    def list(root='.'):
        corpus_map = {}

        root = os.path.abspath(root)

        for filename in os.listdir(root):
            filepath = os.path.join(root, filename)

            if os.path.isfile(filepath):
                name, extension = os.path.splitext(filename)
                extension = extension[1:]

                if len(name) == 0:
                    continue

                if extension.lower() == 'tmx':
                    corpus_map[name] = _CorpusBuilder(name, tmx=filepath)
                else:
                    lang = _parse_lang(extension)

                    if lang is None:
                        continue

                    if name in corpus_map:
                        builder = corpus_map[name]
                    else:
                        builder = _CorpusBuilder(name)
                        corpus_map[name] = builder

                    builder.add(lang, filepath)

        return sorted([builder.build() for _, builder in corpus_map.iteritems()], key=attrgetter('name'))

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
            corpora = BilingualCorpus.list(directory)

            for corpus in corpora:
                if len(corpus.langs) == 1:
                    if monolingual_lang in corpus.langs:
                        monolingual_corpora.append(corpus)
                elif len(corpus.langs) > 1:
                    if source_lang in corpus.langs and target_lang in corpus.langs:
                        bilingual_corpora.append(corpus)

        return bilingual_corpora, monolingual_corpora

    @staticmethod
    def make_parallel(name, folder, langs):
        folder = os.path.abspath(folder)
        lang2file = {}

        for lang in langs:
            lang2file[lang] = os.path.join(folder, name + '.' + lang.lower())

        return _FileParallelCorpus(name, lang2file)

    def __init__(self, name, langs=None):
        self.name = name
        self.langs = langs if langs is not None else []

    def get_file(self, lang):
        raise NotImplementedError('Abstract method')

    def count_lines(self):
        raise NotImplementedError('Abstract method')

    def get_folder(self):
        raise NotImplementedError('Abstract method')

    def copy(self, folder, suffixes=None):
        raise NotImplementedError('Abstract method')

    def symlink(self, folder, name=None):
        raise NotImplementedError('Abstract method')

    def __str__(self):
        return self.name + '(' + ','.join(self.langs) + ')'

    def __repr__(self):
        return self.__str__()


class _FileParallelCorpus(BilingualCorpus):
    def __init__(self, name, lang2file):
        BilingualCorpus.__init__(self, name, lang2file.keys())

        self._lang2file = lang2file

        files = lang2file.values()

        self._root = os.path.abspath(os.path.join(files[0], os.pardir)) if len(files) > 0 else None
        self._lines_count = -1
        self._lock = Lock()

    def get_file(self, lang):
        return self._lang2file[lang] if lang in self._lang2file else None

    def get_folder(self):
        return self._root

    def count_lines(self):
        if self._lines_count < 0 < len(self.langs):
            with self._lock:
                if self._lines_count < 0 < len(self.langs):
                    self._lines_count = fileutils.linecount(self.get_file(self.langs[0]))

        return self._lines_count

    def copy(self, folder, suffixes=None):
        for lang, file in self._lang2file.iteritems():
            suffix = suffixes[lang] if suffixes else ''
            shutil.copy(file, os.path.join(folder, os.path.basename(file) + suffix))

    def symlink(self, folder, name=None):
        link = BilingualCorpus.make_parallel(name if name is not None else self.name, folder, self.langs)

        for lang in self.langs:
            os.symlink(self.get_file(lang), link.get_file(lang))

        return link


class _TMXContentReader(xml.sax.handler.ContentHandler):
    langs = []

    def startElement(self, name, attrs):
        if name == 'tuv':
            lang = _parse_lang(str(attrs['xml:lang']) if 'xml:lang' in attrs else None)

            if lang is not None and lang not in self.langs:
                self.langs.append(lang)

                if len(self.langs) > 1:
                    raise StopIteration


class _TMXCorpus(BilingualCorpus):
    @staticmethod
    def __get_langs(tmx_file):
        handler = _TMXContentReader()

        try:
            parser = xml.sax.make_parser()
            parser.setFeature(xml.sax.handler.feature_validation, False)
            parser.setFeature(xml.sax.handler.feature_external_ges, False)
            parser.setFeature(xml.sax.handler.feature_external_pes, False)
            parser.setContentHandler(handler)

            with open(tmx_file) as f:
                parser.parse(f)
        except StopIteration:
            pass

        return handler.langs

    def __init__(self, name, f, langs=None):
        BilingualCorpus.__init__(self, name, langs if langs is not None else self.__get_langs(f))
        self._tmx_file = f
        self._root = os.path.dirname(f)

    def get_folder(self):
        return self._root

    def get_file(self, lang):
        raise NotImplementedError('Cannot read lang file for TMX')

    def count_lines(self):
        raise NotImplementedError('Count lines not supported for TMX')

    def copy(self, folder, suffixes=None):
        suffix = suffixes['tmx'] if suffixes else ''
        shutil.copy(self._tmx_file, os.path.join(folder, os.path.basename(self._tmx_file) + suffix))

    def symlink(self, folder, name=None):
        if name is None:
            name = self.name

        link = os.path.join(folder, name + '.tmx')
        os.symlink(self._tmx_file, link)

        return _TMXCorpus(name, link, langs=self.langs)

    def __str__(self):
        return self.name + '[' + ','.join(self.langs) + ']'
