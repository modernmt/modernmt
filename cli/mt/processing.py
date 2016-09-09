import multiprocessing
import os
import re
from HTMLParser import HTMLParser

from cli import mmt_javamain
from cli.libs import multithread, fileutils, shell
from cli.mt import BilingualCorpus

__author__ = 'Davide Caroselli'


def _pool_exec(function, jobs):
    if len(jobs) < 1:
        return

    workers = min(multiprocessing.cpu_count(), len(jobs))
    pool = multithread.Pool(workers)

    try:
        aync_jobs = [pool.apply_async(function, job) for job in jobs]
        return [job.get() for job in aync_jobs]
    finally:
        pool.terminate()


class Tokenizer:
    __wspace = re.compile('\s+')

    def __init__(self):
        self._xml = XMLEncoder()

    def process_corpora(self, corpora, dest_folder):
        fileutils.makedirs(dest_folder, exist_ok=True)

        for corpus in corpora:
            for lang in corpus.langs:
                source = corpus.get_file(lang)
                dest = BilingualCorpus.make_parallel(corpus.name, dest_folder, [lang])

                self.process_file(source, dest, lang)

        return BilingualCorpus.list(dest_folder)

    def process_file(self, source, dest, lang):
        with open(source) as input_stream:
            with open(dest.get_file(lang), 'w') as output_stream:
                for line in input_stream:
                    output_stream.write(self.process(line.decode('utf-8')).encode('utf-8'))
                    output_stream.write('\n')

    def process(self, string):
        string = string.strip()
        string = self._xml.decode_string(string)

        chars = []

        for i in xrange(0, len(string)):
            p = None if i == 0 else string[i - 1]
            c = string[i]
            s = None if i == (len(string) - 1) else string[i + 1]

            p_is_digit = p is not None and p.isdigit()
            s_is_digit = s is not None and s.isdigit()

            if not c.isspace() and not c.isalnum() and not p_is_digit and not s_is_digit:
                chars.append(' ')
                chars.append(c)
                chars.append(' ')
            else:
                chars.append(c)

        string = ''.join(chars)
        string = self.__wspace.sub(' ', string)

        return string.strip()


class TMCleaner:
    def __init__(self):
        self._java_mainclass = 'eu.modernmt.cli.CleaningPipelineMain'

    def clean(self, source, target, input_paths, output_path):
        args = ['-s', source, '-t', target, '--output', output_path, '--input']

        for root in input_paths:
            args.append(root)

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdin=shell.DEVNULL, stdout=shell.DEVNULL, stderr=shell.DEVNULL)

        return BilingualCorpus.splitlist(source, target, roots=output_path)[0]


class TrainingPreprocessor:
    DEV_FOLDER_NAME = 'dev'
    TEST_FOLDER_NAME = 'test'

    def __init__(self, vocabulary_path):
        self._java_mainclass = 'eu.modernmt.cli.TrainingPipelineMain'
        self._vocabulary_path = vocabulary_path

    def process(self, source, target, input_paths, output_path, data_path=None):
        args = ['-s', source, '-t', target, '-v', self._vocabulary_path, '--output', output_path, '--input']

        for root in input_paths:
            args.append(root)

        if data_path is not None:
            args.append('--dev')
            args.append(os.path.join(data_path, TrainingPreprocessor.DEV_FOLDER_NAME))
            args.append('--test')
            args.append(os.path.join(data_path, TrainingPreprocessor.TEST_FOLDER_NAME))

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdin=shell.DEVNULL, stdout=shell.DEVNULL, stderr=shell.DEVNULL)

        return BilingualCorpus.splitlist(source, target, roots=output_path)


class XMLEncoder:
    __TAG_NAME = '([a-zA-Z]|_|:)([a-zA-Z]|[0-9]|\\.|-|_|:|)*'
    __TAG_REGEX = re.compile('(<(' + __TAG_NAME + ')[^>]*/?>)|(<!(' + __TAG_NAME + ')[^>]*[^/]>)|(</(' +
                             __TAG_NAME + ')[^>]*>)|(<!--)|(-->)')
    __HTML = HTMLParser()

    def __init__(self):
        pass

    def encode(self, corpora, dest_folder):
        if not os.path.isdir(dest_folder):
            fileutils.makedirs(dest_folder, exist_ok=True)

        for corpus in corpora:
            for lang in corpus.langs:
                source = corpus.get_file(lang)
                dest_file = BilingualCorpus.make_parallel(corpus.name, dest_folder, [lang]).get_file(lang)

                self.encode_file(source, dest_file, delete_nl=True)

        return BilingualCorpus.list(dest_folder)

    def encode_file(self, source, dest_file, delete_nl=False):
        with open(dest_file, 'wb') as outstream:
            with open(source) as instream:
                for line in instream:
                    encoded = self.encode_string(line.decode('utf-8'))
                    encoded = encoded.rstrip('\r\n')

                    if delete_nl:
                        encoded = encoded.replace('\n', ' ').replace('\r', '')

                    encoded += '\n'
                    outstream.write(encoded.encode('utf-8'))

    @staticmethod
    def __escape(string):
        escaped = XMLEncoder.__HTML.unescape(string)
        return escaped \
            .replace('&', '&amp;') \
            .replace('"', '&quot;') \
            .replace('\'', '&apos;') \
            .replace('<', '&lt;') \
            .replace('>', '&gt;')

    def encode_string(self, string):
        result = []
        index = 0

        for match in XMLEncoder.__TAG_REGEX.finditer(string):
            start = match.start()
            end = match.end()

            result.append(self.__escape(string[index:start]))
            result.append(match.group())

            index = end

        if index < len(string):
            result.append(self.__escape(string[index:]))

        return ''.join(result)

    def decode_string(self, string):
        result = []
        index = 0

        for match in self.__TAG_REGEX.finditer(string):
            start = match.start()
            end = match.end()

            if index != start:
                result.append(self.__HTML.unescape(string[index:start]).strip())

            index = end

        if index < len(string):
            result.append(self.__HTML.unescape(string[index:]).strip())

        return ' '.join(result)
