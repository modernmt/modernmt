import os
import re
import shutil

from cli.libs import osutils

__author__ = 'Davide Caroselli'


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
    def list(source_lang, target_lang, roots):
        if isinstance(roots, basestring):
            roots = [roots]

        corpora = []

        for root in roots:
            root = os.path.abspath(root)

            for filename in os.listdir(root):
                filepath = os.path.join(root, filename)

                if not os.path.isfile(filepath):
                    continue

                name, extension = os.path.splitext(filename)
                extension = extension[1:]

                if len(name) == 0:
                    continue

                if extension.lower() == 'tmx':
                    corpora.append(TMXCorpus(name, filepath))
                elif extension == source_lang:
                    pair_file = os.path.join(root, name + '.' + target_lang)

                    if os.path.isfile(pair_file):
                        corpora.append(FileParallelCorpus(name, source_lang, target_lang, filepath, pair_file))

        return sorted(corpora, key=lambda x: x.name)

    @staticmethod
    def make_parallel(name, folder, langs):
        folder = os.path.abspath(folder)

        source_lang, target_lang = langs
        source_file = os.path.join(folder, name + '.' + source_lang)
        target_file = os.path.join(folder, name + '.' + target_lang)

        return FileParallelCorpus(name, source_lang, target_lang, source_file, target_file)

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
    @staticmethod
    def from_files(source_file, target_file):
        name = os.path.basename(os.path.splitext(source_file)[0])
        source_lang = os.path.splitext(source_file)[1][1:]
        target_lang = os.path.splitext(target_file)[1][1:]

        return FileParallelCorpus(name, source_lang, target_lang, source_file, target_file)

    def __init__(self, name, source_lang, target_lang, source_file, target_file):
        BilingualCorpus.__init__(self, name, [source_lang, target_lang])

        self._lang2file = {source_lang: source_file, target_lang: target_file}

        files = self._lang2file.values()

        self._root = os.path.abspath(os.path.join(files[0], os.pardir)) if len(files) > 0 else None
        self._lines_count = -1

    def get_file(self, lang):
        return self._lang2file[lang] if lang in self._lang2file else None

    def get_folder(self):
        return self._root

    def count_lines(self):
        if self._lines_count < 0 < len(self.langs):
            self._lines_count = osutils.lc(self.get_file(self.langs[0]))

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
    @staticmethod
    def from_file(tmx_file):
        name = os.path.basename(os.path.splitext(tmx_file)[0])
        return TMXCorpus(name, tmx_file)

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
