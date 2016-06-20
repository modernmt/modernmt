import multiprocessing
import os
import re
from HTMLParser import HTMLParser

from scripts import mmt_javamain
from scripts.libs import multithread, fileutils, shell
from scripts.mt import ParallelCorpus

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


class TMCleaner:
    def __init__(self):
        self._java_mainclass = 'eu.modernmt.cli.CleaningPipelineMain'

    def clean(self, source, target, input_paths, output_path):
        args = ['-s', source, '-t', target, '--output', output_path, '--input']

        for root in input_paths:
            args.append(root)

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdin=shell.DEVNULL, stdout=shell.DEVNULL, stderr=shell.DEVNULL)

        return ParallelCorpus.splitlist(source, target, roots=output_path)[0]


class TrainingPreprocessor:
    DEV_FOLDER_NAME = 'dev'
    TEST_FOLDER_NAME = 'test'

    def __init__(self):
        self._java_mainclass = 'eu.modernmt.cli.TrainingPipelineMain'

    def process(self, source, target, input_paths, output_path, data_path=None):
        args = ['-s', source, '-t', target, '--output', output_path, '--input']

        for root in input_paths:
            args.append(root)

        if data_path is not None:
            args.append('--dev')
            args.append(os.path.join(data_path, TrainingPreprocessor.DEV_FOLDER_NAME))
            args.append('--test')
            args.append(os.path.join(data_path, TrainingPreprocessor.TEST_FOLDER_NAME))

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdin=shell.DEVNULL, stdout=shell.DEVNULL, stderr=shell.DEVNULL)

        return ParallelCorpus.splitlist(source, target, roots=output_path)


class Preprocessor:
    def __init__(self):
        self._java_mainclass = 'eu.modernmt.cli.PreprocessorMain'

    def __get_command(self, lang, print_tags, print_placeholders, original_spacing):
        args = ['--lang', lang]
        if original_spacing:
            args.append('--original-spacing')
        if not print_tags:
            args.append('--no-tags')
        if print_placeholders:
            args.append('--print-placeholders')

        return mmt_javamain(self._java_mainclass, args)

    def process(self, corpora, dest_folder, print_tags=True, print_placeholders=False, original_spacing=False):
        for corpus in corpora:
            for lang in corpus.langs:
                source = corpus.get_file(lang)
                dest = ParallelCorpus(corpus.name, dest_folder, [lang]).get_file(lang)

                self.__process_file(source, dest, lang, print_tags, print_placeholders, original_spacing)

        return ParallelCorpus.list(dest_folder)

    # noinspection PyTypeChecker
    def __process_file(self, source, dest, lang, print_tags=True, print_placeholders=False, original_spacing=False):
        command = self.__get_command(lang, print_tags, print_placeholders, original_spacing)

        parent_dir = os.path.abspath(os.path.join(dest, os.pardir))
        if not os.path.isdir(parent_dir):
            fileutils.makedirs(parent_dir, exist_ok=True)

        with open(source) as input_stream:
            with open(dest, 'w') as output_stream:
                shell.execute(command, stdin=input_stream, stdout=output_stream, stderr=shell.DEVNULL)


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
                dest = ParallelCorpus(corpus.name, dest_folder, [lang]).get_file(lang)

                self.encode_file(source, dest, delete_nl=True)

        return ParallelCorpus.list(dest_folder)

    def encode_file(self, source, dest, delete_nl=False):
        with open(dest, 'wb') as outstream:
            with open(source) as instream:
                for line in instream:
                    encoded = self.encode_string(line.decode('utf-8'))
                    encoded = encoded.rstrip('\r\n')

                    if delete_nl:
                        encoded = encoded.replace('\n', ' ').replace('\r', '')

                    encoded += '\n'
                    outstream.write(encoded.encode('utf-8'))

    def __escape(self, string):
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

