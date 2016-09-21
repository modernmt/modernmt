import multiprocessing
import os

import cli
from cli.libs import fileutils, shell, multithread
from cli.mt import BilingualCorpus
from moses import MosesFeature

__author__ = 'Davide Caroselli'


class _CorpusCleaner:
    injector_section = 'cleaner'
    injectable_fields = {
        'ratio': ('parallel sentence length ratio', float, 3),
        'min': ('min acceptable number of words per sentence', int, 1),
        'max': ('max acceptable number of words per sentence', int, 80),
    }

    def __init__(self):
        self._ratio = 3
        self._min = 1
        self._max = 80

        # TODO: this can be a python native implementation
        self._cleaner_script = os.path.join(cli.PYOPT_DIR, 'clean-corpus-n-ratio.perl')

    @staticmethod
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

    def clean(self, corpora, dest_folder, langs=None):
        if langs is None and len(corpora) > 0:
            langs = (corpora[0].langs[0], corpora[0].langs[1])

        self._pool_exec(self._clean_file, [(corpus, dest_folder, langs) for corpus in corpora])
        return BilingualCorpus.list(dest_folder)

    def _clean_file(self, source, dest_folder, langs):
        if not os.path.isdir(dest_folder):
            fileutils.makedirs(dest_folder, exist_ok=True)

        input_folder = os.path.join(source.get_folder(), source.name)
        output_folder = os.path.join(dest_folder, source.name)

        command = ['perl', self._cleaner_script, '-ratio', str(self._ratio), input_folder, langs[0], langs[1],
                   output_folder, str(self._min), str(self._max)]
        shell.execute(command, stdout=shell.DEVNULL, stderr=shell.DEVNULL)


class WordAligner:
    available_types = ['FastAlign']

    def __init__(self):
        pass

    @staticmethod
    def instantiate(type_name):
        if type_name == 'FastAlign':
            return FastAlign()
        else:
            raise NameError('Invalid Word Aligner type: ' + str(type_name))

    def align(self, corpus, langs, model_dir, working_dir='.', log_file=None):
        raise NotImplementedError('Abstract method')


class SuffixArraysPhraseTable(MosesFeature):
    injector_section = 'suffixarrays'
    injectable_fields = {
        'sample': ('number of samples for phrase table', int, 1000),
        'method': ('sampling method for suffix array phrase table', str, 'ranked3'),
    }

    def __init__(self, model, langs):
        MosesFeature.__init__(self, 'Mmsapt')

        self._sample = None  # Injected
        self._method = None  # Injected

        self._model = model
        self._source_lang = langs[0]
        self._target_lang = langs[1]

        self._cleaner = _CorpusCleaner()

        self._symal2mam_bin = os.path.join(cli.BIN_DIR, 'symal2mam')
        self._mttbuild_bin = os.path.join(cli.BIN_DIR, 'mtt-build')
        self._mmlexbuild_bin = os.path.join(cli.BIN_DIR, 'mmlex-build')

    def _get_model_basename(self):
        return os.path.join(self._model, 'model')

    def get_iniline(self):
        template = 'path={model}. L1={source_lang} L2={target_lang} output-factor=0 sample={sample} method={method} ' \
                   'pfwd=g pbwd=g logcnt=0 coh=0 prov=0 rare=0 unal=0 smooth=.01 lexalpha=0 lr-func=DM0 bias-loglevel=0'

        return template.format(model=self.get_relpath(self._get_model_basename()),
                               source_lang=self._source_lang, target_lang=self._target_lang,
                               sample=self._sample, method=self._method)

    def train(self, corpora, aligner, working_dir='.', log_file=None):
        if os.path.isdir(self._model) and len(os.listdir(self._model)) > 0:
            raise Exception('Model already exists at ' + self._model)

        if not os.path.isdir(self._model):
            fileutils.makedirs(self._model, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)

        l1 = self._source_lang
        l2 = self._target_lang
        langs = (l1, l2)
        langs_suffix = l1 + '-' + l2

        mct_base = self._get_model_basename()
        dmp_file = mct_base + '.dmp'
        mam_file = mct_base + '.' + langs_suffix + '.mam'
        lex_file = mct_base + '.' + langs_suffix + '.lex'

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'a')

            # Clean corpus for training
            clean_output = os.path.join(working_dir, 'clean_corpora')
            fileutils.makedirs(clean_output, exist_ok=True)
            corpora = self._cleaner.clean(corpora, clean_output, (self._source_lang, self._target_lang))

            # Create merged corpus and domains list file (dmp)
            merged_corpus = BilingualCorpus.make_parallel(os.path.basename(mct_base), working_dir, langs)

            fileutils.merge([corpus.get_file(l1) for corpus in corpora], merged_corpus.get_file(l1))
            fileutils.merge([corpus.get_file(l2) for corpus in corpora], merged_corpus.get_file(l2))
            with open(dmp_file, 'w') as dmp:
                for corpus in corpora:
                    dmp.write(str(corpus.name) + ' ' + str(corpus.count_lines()) + '\n')

            # Create Aligner models and align corpus
            symal_file = aligner.align(merged_corpus, langs, self._model, working_dir, log_file)

            # Execute mtt-build
            mttbuild_command = self._get_mttbuild_command(mct_base, dmp_file, l1)
            with open(merged_corpus.get_file(l1)) as stdin:
                shell.execute(mttbuild_command, stdin=stdin, stdout=log, stderr=log)

            mttbuild_command = self._get_mttbuild_command(mct_base, dmp_file, l2)
            with open(merged_corpus.get_file(l2)) as stdin:
                shell.execute(mttbuild_command, stdin=stdin, stdout=log, stderr=log)

            # Create 'mam' file
            mam_command = [self._symal2mam_bin, mam_file]
            with open(symal_file) as stdin:
                shell.execute(mam_command, stdin=stdin, stdout=log, stderr=log)

            # Create 'lex' file
            lex_command = [self._mmlexbuild_bin, mct_base + '.', l1, l2, '-o', lex_file]
            shell.execute(lex_command, stdout=log, stderr=log)
        finally:
            if log_file is not None:
                log.close()

    def _get_mttbuild_command(self, mct_base, dmp_file, lang):
        output = mct_base + '.' + lang
        return [self._mttbuild_bin, '-i', '-o', output, '-m', dmp_file, '-g']


class FastAlign(WordAligner):
    def __init__(self):
        WordAligner.__init__(self)

        self._train_bin = os.path.join(cli.BIN_DIR, 'fa_build')
        self._align_bin = os.path.join(cli.BIN_DIR, 'fa_align')

    def align(self, corpus, langs, model_dir, working_dir='.', log_file=None):
        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)

        symal_file = os.path.join(working_dir, 'alignments.symal')

        corpus_l1 = corpus.get_file(langs[0])
        corpus_l2 = corpus.get_file(langs[1])

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'a')

            # Train model
            command = [self._train_bin, '-s', corpus_l1, '-t', corpus_l2, '-m', model_dir, '-I', '4']
            shell.execute(command, stderr=log)

            # Align
            command = [self._align_bin, '-s', corpus_l1, '-t', corpus_l2, '-m', model_dir, '-a', '1']
            with open(symal_file, 'w') as stdout:
                shell.execute(command, stdout=stdout, stderr=log)
        finally:
            if log_file is not None:
                log.close()

        return symal_file
