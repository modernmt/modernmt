import os

import shutil

import cli
from cli.libs import fileutils, shell
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

    def build(self, corpora, working_dir='.', log=None):
        raise NotImplementedError('Abstract method')

    def align(self, corpora, output_folder, log=None):
        raise NotImplementedError('Abstract method')


class FastAlign(WordAligner):
    def __init__(self, model, source_lang, target_lang):
        WordAligner.__init__(self, model, source_lang, target_lang)

        self._build_bin = os.path.join(cli.BIN_DIR, 'fa_build')
        self._align_bin = os.path.join(cli.BIN_DIR, 'fa_align')

    def build(self, corpora, working_dir='.', log=None):
        if log is None:
            log = shell.DEVNULL

        shutil.rmtree(self._model, ignore_errors=True)
        fileutils.makedirs(self._model, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)

        merged_corpus = BilingualCorpus.make_parallel('merge', working_dir, (self._source_lang, self._target_lang))

        fileutils.merge([corpus.get_file(self._source_lang) for corpus in corpora],
                        merged_corpus.get_file(self._source_lang))
        fileutils.merge([corpus.get_file(self._target_lang) for corpus in corpora],
                        merged_corpus.get_file(self._target_lang))

        command = [self._build_bin,
                   '-s', merged_corpus.get_file(self._source_lang), '-t', merged_corpus.get_file(self._target_lang),
                   '-m', self._model, '-I', '4']
        shell.execute(command, stdout=log, stderr=log)

    def align(self, corpora, output_folder, log=None):
        if log is None:
            log = shell.DEVNULL

        root = set([corpus.get_folder() for corpus in corpora])

        if len(root) != 1:
            raise Exception('Aligner corpora must share the same folder: found  ' + str(root))

        root = root.pop()

        command = [self._align_bin, '--model', self._model,
                   '--input', root, '--output', output_folder,
                   '--source', self._source_lang, '--target', self._target_lang,
                   '--strategy', '1']
        shell.execute(command, stderr=log, stdout=log)


class LexicalReordering(MosesFeature):
    def __init__(self):
        MosesFeature.__init__(self, 'LexicalReordering')

    def get_iniline(self, base_path):
        return 'input-factor=0 output-factor=0 type=hier-mslr-bidirectional-fe-allff'


class SuffixArraysPhraseTable(MosesFeature):
    injector_section = 'sapt'
    injectable_fields = {
        'sample': ('number of samples for phrase table', int, 1000),
    }

    def __init__(self, model, langs):
        MosesFeature.__init__(self, 'SAPT')

        self._sample = None  # Injected

        self._model = model
        self._reordering_model_feature = None
        self._source_lang = langs[0]
        self._target_lang = langs[1]

        self._build_bin = os.path.join(cli.BIN_DIR, 'sapt_build')

    def _get_model_basename(self):
        return os.path.join(self._model, 'model')

    def set_reordering_model(self, name):
        self._reordering_model_feature = name

    def get_iniline(self, base_path):
        result = 'path={model} input-factor=0 output-factor=0 sample-limit={sample}'.format(
            model=self.get_relpath(base_path, self._model), sample=self._sample
        )

        if self._reordering_model_feature is not None:
            result += ' lr-func={name}'.format(name=self._reordering_model_feature)

        return result

    def train(self, corpora, aligner, working_dir='.', log=None):
        if log is None:
            log = shell.DEVNULL

        shutil.rmtree(self._model, ignore_errors=True)
        fileutils.makedirs(self._model, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)

        train_corpora = []  # Prepare training folder
        for corpus in corpora:
            dest_corpus = BilingualCorpus.make_parallel(corpus.name, working_dir,
                                                        (self._source_lang, self._target_lang))
            source_file = corpus.get_file(self._source_lang)
            target_file = corpus.get_file(self._target_lang)

            os.symlink(source_file, dest_corpus.get_file(self._source_lang))
            os.symlink(target_file, dest_corpus.get_file(self._target_lang))

            train_corpora.append(dest_corpus)

        # Align corpora
        aligner.align(train_corpora, working_dir, log=log)

        # Build models
        command = [self._build_bin, '--input', working_dir, '--model', self._model,
                   '-s', self._source_lang, '-t', self._target_lang]
        shell.execute(command, stdout=log, stderr=log)
