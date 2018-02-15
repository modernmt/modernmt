import os
import re
import shutil
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
        return TMXCorpus(self.name, self.tmx) if self.tmx is not None else FileParallelCorpus(self.name,
                                                                                              self.lang2file)


def _parse_lang(lang):
    def _looks_like_script_code(string):
        return len(string) == 4 and re.match('[A-Z][a-z]{3}', string)

    def _looks_like_geo_code_3166(string):
        return len(string) == 2 and re.match('[A-Z]{2}', string)

    def _looks_like_geo_code_numeric(string):
        return len(string) == 3 and re.match('[0-9]{3}', string)

    def _looks_like_language(string):
        return re.match('[a-z]{2,3}', string) is not None

    parts = lang.split('-')

    language = None
    script = None
    region = None

    for i in range(len(parts)):
        part = parts[i]

        if i == 0:
            if _looks_like_language(part):
                language = part
            else:
                raise ValueError(lang)
        else:
            if script is None and region is None and _looks_like_script_code(part):
                script = part
            elif region is None and (_looks_like_geo_code_3166(part) or _looks_like_geo_code_numeric(part)):
                region = part
            else:
                raise ValueError(lang)

    if region is None:
        return language
    else:
        return '%s-%s' % (language, region)


class BilingualCorpus(object):
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
                if isinstance(corpus, TMXCorpus):
                    bilingual_corpora.append(corpus)
                elif len(corpus.langs) > 1:
                    if source_lang in corpus.langs and target_lang in corpus.langs:
                        bilingual_corpora.append(corpus)
                elif len(corpus.langs) == 1:
                    if monolingual_lang in corpus.langs:
                        monolingual_corpora.append(corpus)

        return bilingual_corpora, monolingual_corpora

    @staticmethod
    def make_parallel(name, folder, langs):
        folder = os.path.abspath(folder)
        lang2file = {}

        for lang in langs:
            lang2file[lang] = os.path.join(folder, name + '.' + lang)

        return FileParallelCorpus(name, lang2file)

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

    def reader(self, langs=None):
        raise NotImplementedError('Abstract method')

    def writer(self, langs=None):
        raise NotImplementedError('Abstract method')

    def __str__(self):
        return self.name + '(' + ','.join(self.langs) + ')'

    def __repr__(self):
        return self.__str__()


class FileParallelCorpus(BilingualCorpus):
    def __init__(self, name, lang2file):
        BilingualCorpus.__init__(self, name, lang2file.keys())

        self._lang2file = lang2file

        files = lang2file.values()

        self._root = os.path.abspath(os.path.join(files[0], os.pardir)) if len(files) > 0 else None
        self._lines_count = -1

    def get_file(self, lang):
        return self._lang2file[lang] if lang in self._lang2file else None

    def get_folder(self):
        return self._root

    def count_lines(self):
        if self._lines_count < 0 < len(self.langs):
            self._lines_count = fileutils.linecount(self.get_file(self.langs[0]))

        return self._lines_count

    def copy(self, folder, suffixes=None):
        for lang, f in self._lang2file.iteritems():
            suffix = suffixes[lang] if suffixes else ''
            shutil.copy(f, os.path.join(folder, os.path.basename(f) + suffix))

    def symlink(self, folder, name=None):
        link = BilingualCorpus.make_parallel(name if name is not None else self.name, folder, self.langs)

        for lang in self.langs:
            os.symlink(self.get_file(lang), link.get_file(lang))

        return link

    def writer(self, langs=None):
        if langs is None:
            langs = self.langs

        class __w:
            def __init__(self, files):
                self._files = [open(f, 'wb') for f in files]

            def __enter__(self):
                return self

            def __exit__(self, exc_type, exc_val, exc_tb):
                for f in self._files:
                    f.close()

            def writelines(self, *args):
                for f, line in zip(self._files, args):
                    f.write(line)

        return __w([self.get_file(l) for l in langs])

    def reader(self, langs=None):
        if langs is None:
            langs = self.langs

        class __r:
            def __init__(self, filenames):
                self._filenames = filenames
                self._files = None

            def __enter__(self):
                self._files = [open(f, 'rb') for f in self._filenames]
                return self

            def __exit__(self, exc_type, exc_val, exc_tb):
                for f in self._files:
                    f.close()

            def __iter__(self):
                return self

            def next(self):
                lines = [f.readline() for f in self._files]

                empty = True
                for line in lines:
                    if line:
                        empty = False
                        break
                if empty:
                    raise StopIteration

                for line in lines:
                    if not line:
                        raise Exception('Files are not parallel')

                return lines

        return __r([self.get_file(l) for l in langs])


class TMXCorpus(BilingualCorpus):
    def __init__(self, name, f):
        BilingualCorpus.__init__(self, name, None)
        self._tmx_file = f
        self._root = os.path.dirname(f)

    def get_tmx(self):
        return self._tmx_file

    def get_folder(self):
        return self._root

    def get_file(self, lang):
        raise NotImplementedError('Cannot read lang file for TMX')

    def count_lines(self):
        raise NotImplementedError('Count lines not supported for TMX')

    def writer(self, langs=None):
        raise NotImplementedError()

    def reader(self, langs=None):
        raise NotImplementedError()

    def copy(self, folder, suffixes=None):
        suffix = suffixes['tmx'] if suffixes else ''
        shutil.copy(self._tmx_file, os.path.join(folder, os.path.basename(self._tmx_file) + suffix))

    def symlink(self, folder, name=None):
        if name is None:
            name = self.name

        link = os.path.join(folder, name + '.tmx')
        os.symlink(self._tmx_file, link)

        return TMXCorpus(name, link)

    def __str__(self):
        return self.name + '[' + ','.join(self.langs) + ']'
