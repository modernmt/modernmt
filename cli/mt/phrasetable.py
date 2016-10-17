import multiprocessing
import os

import cli
from cli.libs import fileutils, shell, multithread
from cli.mt import BilingualCorpus
from moses import MosesFeature

__author__ = 'Davide Caroselli'


class WordAligner:
    available_types = ['FastAlign']

    def __init__(self, model, source_lang, target_lang):
        self._model = model
        self._source_lang = source_lang
        self._target_lang = target_lang

    @staticmethod
    def instantiate(type_name, model, source_lang, target_lang):
        if type_name == 'FastAlign':
            return FastAlign(model, source_lang, target_lang)
        else:
            raise NameError('Invalid Word Aligner type: ' + str(type_name))

    def build(self, corpora, working_dir='.', log_file=None):
        raise NotImplementedError('Abstract method')

    def align(self, corpus, output):
        raise NotImplementedError('Abstract method')


class FastAlign(WordAligner):
    def __init__(self, model, source_lang, target_lang):
        WordAligner.__init__(self, model, source_lang, target_lang)

        self._build_bin = os.path.join(cli.BIN_DIR, 'fa_build')
        self._align_bin = os.path.join(cli.BIN_DIR, 'fa_align')

    def build(self, corpora, working_dir='.', log_file=None):
        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)
        if not os.path.isdir(self._model):
            fileutils.makedirs(self._model, exist_ok=True)

        merged_corpus = BilingualCorpus.make_parallel('merge', working_dir, (self._source_lang, self._target_lang))

        fileutils.merge([corpus.get_file(self._source_lang) for corpus in corpora],
                        merged_corpus.get_file(self._source_lang))
        fileutils.merge([corpus.get_file(self._target_lang) for corpus in corpora],
                        merged_corpus.get_file(self._target_lang))

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'a')

            # Train model
            command = [self._build_bin,
                       '-s', merged_corpus.get_file(self._source_lang), '-t', merged_corpus.get_file(self._target_lang),
                       '-m', self._model, '-I', '4']
            shell.execute(command, stderr=log)
        finally:
            if log_file is not None:
                log.close()

    def align(self, corpus, output):
        command = [self._align_bin, '-s', corpus.get_file(self._source_lang), '-t', corpus.get_file(self._target_lang),
                   '-m', self._model, '-a', '1']
        with open(output, 'w') as stdout:
            shell.execute(command, stdout=stdout)


class SuffixArraysPhraseTable(MosesFeature):
    injector_section = 'sapt'
    injectable_fields = {
        'sample': ('number of samples for phrase table', int, 1000),
    }

    def __init__(self, model, langs):
        MosesFeature.__init__(self, 'SAPT')

        self._sample = None  # Injected

        self._model = model
        self._source_lang = langs[0]
        self._target_lang = langs[1]

        self._build_bin = os.path.join(cli.BIN_DIR, 'sapt_build')

    def _get_model_basename(self):
        return os.path.join(self._model, 'model')

    def get_iniline(self):
        return 'path={model} input-factor=0 output-factor=0 sample-limit={sample}'.format(
            model=self.get_relpath(self._model), sample=self._sample
        )

    def train(self, corpora, aligner, working_dir='.', log_file=None):
        if os.path.isdir(self._model) and len(os.listdir(self._model)) > 0:
            raise Exception('Model already exists at ' + self._model)

        if not os.path.isdir(self._model):
            fileutils.makedirs(self._model, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'a')

            # Prepare training folder
            for corpus in corpora:
                dest_corpus = BilingualCorpus.make_parallel(corpus.name, working_dir,
                                                            (self._source_lang, self._target_lang))
                source_file = corpus.get_file(self._source_lang)
                target_file = corpus.get_file(self._target_lang)

                os.symlink(source_file, dest_corpus.get_file(self._source_lang))
                os.symlink(target_file, dest_corpus.get_file(self._target_lang))

                aligner.align(corpus, os.path.join(working_dir, corpus.name + '.align'))

            # Build models
            command = [self._build_bin, '--input', working_dir, '--model', self._model,
                       '-s', self._source_lang, '-t', self._target_lang]
            shell.execute(command, stdout=log, stderr=log)
        finally:
            if log_file is not None:
                log.close()
