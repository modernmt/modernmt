import multiprocessing
import os

import scripts
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


class CorpusCleaner:
    injector_section = 'cleaner'
    injectable_fields = {
        'ratio': ('parallel sentence length ratio', float, 3),
        'min': ('min acceptable number of words per sentence', int, 1),
        'max': ('max acceptable number of words per sentence', int, 80),
    }

    def __init__(self):
        self._ratio = None  # Injected
        self._min = None  # Injected
        self._max = None  # Injected

        self._cleaner_script = os.path.join(scripts.BIN_DIR, 'cleaner-mosesofficial', 'clean-corpus-n-ratio.perl')

    def batch_clean(self, corpora, dest_folder, langs=None):
        if langs is None and len(corpora) > 0:
            langs = (corpora[0].langs[0], corpora[0].langs[1])

        _pool_exec(self.clean_corpus,
                   [(corpus, ParallelCorpus(corpus.name, dest_folder, corpus.langs), langs) for corpus in corpora])
        return ParallelCorpus.list(dest_folder)

    def clean_corpus(self, source, dest, langs):
        if not os.path.isdir(dest.root):
            fileutils.makedirs(dest.root, exist_ok=True)

        source = os.path.splitext(source.get_file(langs[0]))[0]
        output = os.path.splitext(dest.get_file(langs[0]))[0]

        command = ['perl', self._cleaner_script, '-ratio', str(self._ratio), source, langs[0], langs[1], output,
                   str(self._min), str(self._max)]
        shell.execute(command, stdout=shell.DEVNULL, stderr=shell.DEVNULL)


class Preprocessor:
    DEV_FOLDER_NAME = 'dev'
    TEST_FOLDER_NAME = 'test'

    def __init__(self):
        self._java_mainclass = 'eu.modernmt.cli.PreprocessorMain'

    def process(self, source, target, input_paths, output_path, data_path=None):
        args = ['-s', source, '-t', target, '--output', output_path, '--input']

        for root in input_paths:
            args.append(root)

        if data_path is not None:
            args.append('--dev')
            args.append(os.path.join(data_path, Preprocessor.DEV_FOLDER_NAME))
            args.append('--test')
            args.append(os.path.join(data_path, Preprocessor.TEST_FOLDER_NAME))

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdin=shell.DEVNULL, stdout=shell.DEVNULL, stderr=shell.DEVNULL)

        return ParallelCorpus.splitlist(source, target, roots=output_path)


class Tokenizer:
    def __init__(self):
        self._java_mainclass = 'eu.modernmt.cli.TokenizerMain'

    def _get_tokenizer_command(self, lang, print_tags, print_placeholders):
        args = ['--lang', lang]
        if not print_tags:
            args.append('--no-tags')
        if print_placeholders:
            args.append('--print-placeholders')

        return mmt_javamain(self._java_mainclass, args)

    def tokenize(self, sentence, lang, print_tags=True, print_placeholders=False):
        command = self._get_tokenizer_command(lang, print_tags, print_placeholders)
        out, _ = shell.execute(command, sentence)

        return out.strip()

    def batch_tokenize(self, corpora, dest_folder, print_tags=True, print_placeholders=False):
        for corpus in corpora:
            for lang in corpus.langs:
                source = corpus.get_file(lang)
                dest = ParallelCorpus(corpus.name, dest_folder, [lang]).get_file(lang)

                self.tokenize_file(source, dest, lang, print_tags, print_placeholders)

        return ParallelCorpus.list(dest_folder)

    # noinspection PyTypeChecker
    def tokenize_file(self, source, dest, lang, print_tags=True, print_placeholders=False):
        command = self._get_tokenizer_command(lang, print_tags, print_placeholders)

        parent_dir = os.path.abspath(os.path.join(dest, os.pardir))
        if not os.path.isdir(parent_dir):
            fileutils.makedirs(parent_dir, exist_ok=True)

        with open(source) as input_stream:
            with open(dest, 'w') as output_stream:
                shell.execute(command, stdin=input_stream, stdout=output_stream, stderr=shell.DEVNULL)
