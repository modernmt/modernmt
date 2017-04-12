import os

import shutil

import cli
from cli.libs import fileutils, shell
from moses import MosesFeature

__author__ = 'Davide Caroselli'


class LanguageModel(MosesFeature):
    available_types = ['InterpolatedLM', 'KenLM']

    injector_section = 'lm'
    injectable_fields = {
        'order': ('LM order (N-grams length)', int, 5),
    }

    @staticmethod
    def instantiate(type_name, *model):
        if type_name == 'KenLM':
            return KenLM(*model)
        elif type_name == 'InterpolatedLM':
            return InterpolatedLM(*model)
        else:
            raise NameError('Invalid Language Model type: ' + type_name)

    def __init__(self, model, classname):
        MosesFeature.__init__(self, classname)

        self._order = None  # Injected
        self._model = model

    def train(self, corpora, lang, working_dir='.', log=None):
        raise NotImplemented


class KenLM(LanguageModel):
    def __init__(self, model):
        LanguageModel.__init__(self, model, 'KENLM')

        self.prune = True
        self._lmplz_bin = os.path.join(cli.BIN_DIR, 'lmplz')
        self._bbinary_bin = os.path.join(cli.BIN_DIR, 'build_binary')

    def train(self, corpora, lang, working_dir='.', log=None):
        if log is None:
            log = shell.DEVNULL

        shutil.rmtree(self._model, ignore_errors=True)
        fileutils.makedirs(self._model, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)

        # Collapse all corpora into a single text file
        merged_corpus = os.path.join(working_dir, 'merge')
        fileutils.merge([corpus.get_file(lang) for corpus in corpora], merged_corpus)

        # Create language model in ARPA format
        arpa_file = os.path.join(working_dir, 'lm.arpa')
        arpa_command = [self._lmplz_bin, '--discount_fallback', '-o', str(self._order),
                        '-S', str(self.get_mem_percent()) + '%', '-T', working_dir]
        if self._order > 2 and self.prune:
            arpa_command += ['--prune', '0', '0', '1']

        with open(merged_corpus) as stdin:
            with open(arpa_file, 'w') as stdout:
                shell.execute(arpa_command, stdin=stdin, stdout=stdout, stderr=log)

        # Binarize ARPA file
        binarize_command = [self._bbinary_bin, arpa_file, self._model]
        shell.execute(binarize_command, stdout=log, stderr=log)

    @staticmethod
    def get_mem_percent():
        """:returns percentage of MemTotal (hardware memory) to use in `lmplz`."""
        # Simple heuristic: use 80% of *available* memory (instead of MemTotal as is the lmplz default) - avoids
        # crashing on machines with other jobs running.
        # This may evict some disk caches (is not too nice to other programs using mmapped
        # files unless you lower the 80%).

        mi = fileutils.meminfo()
        total = float(mi['MemTotal'])
        available = fileutils.free()
        return int(80.0 * available / total)

    def get_iniline(self, base_path):
        return 'factor=0 order={order} path={model}'.format(order=self._order,
                                                            model=self.get_relpath(base_path, self._model))


class InterpolatedLM(LanguageModel):
    injector_section = 'lm'
    injectable_fields = {
    }

    def __init__(self, model):
        LanguageModel.__init__(self, model, 'MMTILM')

        self._create_alm_bin = os.path.join(cli.BIN_DIR, 'create_alm')
        self._create_slm_bin = os.path.join(cli.BIN_DIR, 'create_slm')

        self.prune = True

    def train(self, corpora, lang, working_dir='.', log=None):
        if log is None:
            log = shell.DEVNULL

        bicorpora = []
        for corpus in corpora:
            if len(corpus.langs) > 1:
                bicorpora.append(corpus)

        shutil.rmtree(self._model, ignore_errors=True)
        fileutils.makedirs(self._model, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)

        # Train static LM
        static_lm_model = os.path.join(self._model, 'background.slm')
        static_lm_wdir = os.path.join(working_dir, 'slm.temp')

        fileutils.makedirs(static_lm_wdir, exist_ok=True)

        merged_corpus = os.path.join(working_dir, 'merged_corpus')
        fileutils.merge([corpus.get_file(lang) for corpus in corpora], merged_corpus)

        command = [self._create_slm_bin, '--discount-fallback', '-o', str(self._order),
                   '--model', static_lm_model,
                   '-T', static_lm_wdir]
        if self._order > 2 and self.prune:
            command += ['--prune', '0', '0', '1']

        with open(merged_corpus) as stdin:
            shell.execute(command, stdin=stdin, stdout=log, stderr=log)

        # Create AdaptiveLM training folder
        alm_train_folder = os.path.join(working_dir, 'alm_train')
        fileutils.makedirs(alm_train_folder, exist_ok=True)

        for corpus in bicorpora:
            os.symlink(corpus.get_file(lang), os.path.join(alm_train_folder, corpus.name + '.' + lang))

        # Train adaptive LM
        adaptive_lm_model = os.path.join(self._model, 'foreground.alm')
        fileutils.makedirs(adaptive_lm_model, exist_ok=True)

        command = [self._create_alm_bin, '-m', adaptive_lm_model, '-i', alm_train_folder, '-b', '50000000']
        shell.execute(command, stdout=log, stderr=log)

    def get_iniline(self, base_path):
        return 'path={model}'.format(model=self.get_relpath(base_path, self._model))
