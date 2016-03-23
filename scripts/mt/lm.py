import os
from multiprocessing import cpu_count

import scripts
from moses import MosesFeature
from scripts.libs import fileutils, shell

__author__ = 'Davide Caroselli'


class LanguageModel(MosesFeature):
    available_types = ['AdaptiveIRSTLM', 'MultiplexedIRSTLM', 'StaticIRSTLM', 'KenLM']

    injector_section = 'lm'
    injectable_fields = {
        'order': ('LM order (N-grams length)', int, 5),
    }

    @staticmethod
    def instantiate(type_name, *model):
        if type_name == 'KenLM':
            return KenLM(*model)
        elif type_name == 'AdaptiveIRSTLM':
            return AdaptiveIRSTLM(*model)
        elif type_name == 'MultiplexedIRSTLM':
            return MultiplexedIRSTLM(*model)
        elif type_name == 'StaticIRSTLM':
            return StaticIRSTLM(*model)
        else:
            raise NameError('Invalid Language Model type: ' + type_name)

    def __init__(self, model, classname):
        MosesFeature.__init__(self, classname)

        self._order = None  # Injected
        self._model = model

    def train(self, corpora, lang, working_dir='.', log_file=None):
        if os.path.isfile(self._model):
            raise Exception('Model already exists at ' + self._model)

        parent_dir = os.path.abspath(os.path.join(self._model, os.pardir))
        if not os.path.isdir(parent_dir):
            fileutils.makedirs(parent_dir, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)


class KenLM(LanguageModel):
    def __init__(self, model):
        LanguageModel.__init__(self, model, 'KENLM')

        self._lmplz_bin = os.path.join(scripts.BIN_DIR, 'kenlm-stable', 'bin', 'lmplz')
        self._bbinary_bin = os.path.join(scripts.BIN_DIR, 'kenlm-stable', 'bin', 'build_binary')

    def train(self, corpora, lang, working_dir='.', log_file=None):
        LanguageModel.train(self, corpora, lang, working_dir, log_file)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'w')

            # Collapse all corpora into a single text file
            merged_corpus = os.path.join(working_dir, 'merge')
            fileutils.merge([corpus.get_file(lang) for corpus in corpora], merged_corpus)

            # Create language model in ARPA format
            arpa_file = os.path.join(working_dir, 'lm.arpa')
            arpa_command = [self._lmplz_bin, '--discount_fallback', '--prune', '0', '0', '1', '-o', str(self._order)]
            with open(merged_corpus) as stdin:
                with open(arpa_file, 'w') as stdout:
                    shell.execute(arpa_command, stdin=stdin, stdout=stdout, stderr=log)

            # Binarize ARPA file
            binarize_command = [self._bbinary_bin, arpa_file, self._model]
            shell.execute(binarize_command, stdout=log, stderr=log)
        finally:
            if log_file is not None:
                log.close()

    def get_iniline(self):
        return 'factor=0 order={order} path={model}'.format(order=self._order, model=self.get_relpath(self._model))


class StaticIRSTLM(LanguageModel):
    def __init__(self, model):
        LanguageModel.__init__(self, model, 'IRSTLM')

        self._model_dir = os.path.abspath(os.path.join(model, os.pardir))

        self._irstlm_dir = os.path.join(scripts.BIN_DIR, 'irstlm-adaptivelm-v0.6')
        self._addbound_bin = os.path.join(self._irstlm_dir, 'scripts', 'add-start-end.sh')
        self._buildlm_bin = os.path.join(self._irstlm_dir, 'scripts', 'build-lm.sh')
        self._compilelm_bin = os.path.join(self._irstlm_dir, 'bin', 'compile-lm')

    def train(self, corpora, lang, working_dir='.', log_file=None):
        LanguageModel.train(self, corpora, lang, working_dir, log_file)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'w')

            # Collapse all corpora into a single text file
            merged_corpus = os.path.join(working_dir, 'merge')
            fileutils.merge([corpus.get_file(lang) for corpus in corpora], merged_corpus)
            input_se = os.path.join(working_dir, 'static_input.se')
            temp = os.path.join(working_dir, 'temp')
            arpa_file = os.path.join(working_dir, 'static_lm.arpa')

            # Add start and end symbols
            with open(merged_corpus) as stdin:
                with open(input_se, 'w') as stdout:
                    shell.execute([self._addbound_bin], stdin=stdin, stdout=stdout, stderr=log)

            # Creating lm in ARPA format
            command = [self._buildlm_bin, '-i', input_se, '-k', str(cpu_count()), '-o', arpa_file, '-n',
                       str(self._order), '-s', 'witten-bell', '-t', temp, '-l', '/dev/stdout', '-irstlm',
                       self._irstlm_dir, '--PruneSingletons']
            shell.execute(command, stderr=log)

            # Create binary lm
            command = [self._compilelm_bin, arpa_file + '.gz', self._model]
            shell.execute(command, stderr=log)

        finally:
            if log_file is not None:
                log.close()

    def get_iniline(self):
        return 'factor=0 path={model} dub=10000000'.format(model=self.get_relpath(self._model))


class AdaptiveIRSTLM(LanguageModel):
    injector_section = 'adaptivelm'
    injectable_fields = {
        'order': ('LM order (N-grams length)', int, 2),
        'limit': ('Limit the LMs to be queried at runtime', int, 1),
    }

    def __init__(self, model):
        LanguageModel.__init__(self, model, 'IRSTLM')

        self._limit = None  # Injected

        self._model_dir = os.path.abspath(os.path.join(model, os.pardir))

        self._irstlm_dir = os.path.join(scripts.BIN_DIR, 'irstlm-adaptivelm-v0.6')
        self._addbound_bin = os.path.join(self._irstlm_dir, 'scripts', 'add-start-end.sh')
        self._buildlm_bin = os.path.join(self._irstlm_dir, 'scripts', 'build-lm.sh')
        self._compilelm_bin = os.path.join(self._irstlm_dir, 'bin', 'compile-lm')

    def train(self, corpora, lang, working_dir='.', log_file=None):
        LanguageModel.train(self, corpora, lang, working_dir, log_file)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'w')

            lmconfig_content = ['LMINTERPOLATION ' + str(len(corpora)) + ' MAP']
            w = 1. / len(corpora)

            models_folder = os.path.dirname(self._model)

            for corpus in corpora:
                cfile = corpus.get_file(lang)
                lm = corpus.name + '.alm'

                lmconfig_content.append('{weight} {name} {lm}'.format(weight=str(w), name=corpus.name, lm=lm))

                self._train_lm(cfile, os.path.join(models_folder, lm), working_dir, log)

            with open(self._model, 'w') as model:
                for line in lmconfig_content:
                    model.write(line)
                    model.write('\n')
        finally:
            if log_file is not None:
                log.close()

    def _train_lm(self, source, dest, working_dir, log):
        input_se = os.path.join(working_dir, 'input.se')
        temp = os.path.join(working_dir, 'temp')
        arpa_file = os.path.join(working_dir, 'arpa')

        # Add start and end symbols
        with open(source) as stdin:
            with open(input_se, 'w') as stdout:
                shell.execute([self._addbound_bin], stdin=stdin, stdout=stdout, stderr=log)

        # Creating lm in ARPA format
        command = [self._buildlm_bin, '-i', input_se, '-k', str(cpu_count()), '-o', arpa_file, '-n', str(self._order),
                   '-s', 'witten-bell', '-t', temp, '-l', '/dev/stdout', '-irstlm', self._irstlm_dir]
        shell.execute(command, stderr=log)

        # Create binary lm
        command = [self._compilelm_bin, arpa_file + '.gz', dest]
        shell.execute(command, stderr=log)

    def get_iniline(self):
        return 'factor=0 path={model} dub=10000000 weight_normalization=yes weight_limit={limit}'.format(
            model=self.get_relpath(self._model), limit=self._limit)


class MultiplexedIRSTLM(AdaptiveIRSTLM):
    injector_section = 'muxlm'
    injectable_fields = {
        'order': ('LM order (N-grams length)', int, 5),
        #'limit': ('Limit the LMs to be queried at runtime', int, 1),
        'alpha': ('Adaptive LM weight fraction from [0,1), the rest is assigned to background LM', float, 0.5),
        'function': ('interpolation function', str, 'interpolate-linear'),
    }

    def __init__(self, model, static_lm):
        AdaptiveIRSTLM.__init__(self, model)
        LanguageModel.__init__(self, model, 'MUXLM')  # or simply: self.classname = 'MUXLM'

        # Injected
        self._function = None
        self._alpha = None
        #self._limit = None

        self._static_lm = static_lm

    def train(self, corpora, lang, working_dir='.', log_file=None):
        # note: this is very similar to AdaptiveIRSTLM train(), can we unite some stuff?

        LanguageModel.train(self, corpora, lang, working_dir, log_file)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'w')

            lmconfig_content = ['[muxlm]']
            w = 1. / len(corpora)

            models_folder = os.path.dirname(self._model)

            # also need background LM here.
            # TODO: rename member _model -> model? (we are accessing private-marked members here)
            bglm_model = self._static_lm._model
            bglm_order = self._static_lm._order
            lmconfig_content.append('KENLM name=BackgroundLM factor=0 order={order} path={path}'.format(order=bglm_order, path=bglm_model))

            for corpus in corpora:
                cfile = corpus.get_file(lang)
                lm = corpus.name + '.alm'
                dest = os.path.join(models_folder, lm)

                lmconfig_content.append('IRSTLM name={name} factor=0 order={order} path={dest}'.format(name=corpus.name, order=self._order, dest=dest))

                self._train_lm(cfile, dest, working_dir, log)

            with open(self._model, 'w') as model:
                for line in lmconfig_content:
                    model.write(line)
                    model.write('\n')
        finally:
            if log_file is not None:
                log.close()

    # def _train_lm() inherited

    def get_iniline(self):
        return 'factor=0 path={model} background-lm=BackgroundLM alpha={alpha} function={function}'.format(
            model=self.get_relpath(self._model), alpha=self._alpha, function=self._function)
