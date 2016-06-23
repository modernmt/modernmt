import multiprocessing
import os

import scripts
from moses import MosesFeature, Moses
from scripts.libs import fileutils, shell, multithread
from scripts.mt import ParallelCorpus

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

        self._cleaner_script = os.path.join(Moses.bin_path, 'scripts', 'clean-corpus-n-ratio.perl')

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

        self._pool_exec(self._clean_file,
                        [(corpus, ParallelCorpus(corpus.name, dest_folder, corpus.langs), langs) for corpus in corpora])
        return ParallelCorpus.list(dest_folder)

    def _clean_file(self, source, dest, langs):
        if not os.path.isdir(dest.root):
            fileutils.makedirs(dest.root, exist_ok=True)

        source = os.path.splitext(source.get_file(langs[0]))[0]
        output = os.path.splitext(dest.get_file(langs[0]))[0]

        command = ['perl', self._cleaner_script, '-ratio', str(self._ratio), source, langs[0], langs[1], output,
                   str(self._min), str(self._max)]
        shell.execute(command, stdout=shell.DEVNULL, stderr=shell.DEVNULL)


class WordAligner:
    available_types = ['FastAlign', 'mgizapp']

    def __init__(self):
        pass

    @staticmethod
    def instantiate(type_name):
        if type_name == 'FastAlign':
            return FastAlign()
        elif type_name == 'mgizapp':
            return MGizaPP()
        else:
            raise NameError('Invalid Word Aligner type: ' + str(type_name))

    def align(self, corpus, langs, model_dir, working_dir='.', log_file=None):
        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)


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

        self._symal_bin = os.path.join(Moses.bin_path, 'bin', 'symal')
        self._symal2mam_bin = os.path.join(Moses.bin_path, 'bin', 'symal2mam')
        self._mttbuild_bin = os.path.join(Moses.bin_path, 'bin', 'mtt-build')
        self._mmlexbuild_bin = os.path.join(Moses.bin_path, 'bin', 'mmlex-build')

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
            merged_corpus = ParallelCorpus(os.path.basename(mct_base), working_dir, langs)

            fileutils.merge([corpus.get_file(l1) for corpus in corpora], merged_corpus.get_file(l1))
            fileutils.merge([corpus.get_file(l2) for corpus in corpora], merged_corpus.get_file(l2))
            with open(dmp_file, 'w') as dmp:
                for corpus in corpora:
                    dmp.write(str(corpus.name) + ' ' + str(corpus.count_lines()) + '\n')

            # Create alignments in 'bal' file and symmetrize
            bal_file = aligner.align(merged_corpus, langs, self._model, working_dir, log_file)

            symal_file = os.path.join(working_dir, 'alignments.' + langs_suffix + '.symal')
            symal_command = [self._symal_bin, '-a=g', '-d=yes', '-f=yes', '-b=yes']
            with open(bal_file) as stdin:
                with open(symal_file, 'w') as stdout:
                    shell.execute(symal_command, stdin=stdin, stdout=stdout, stderr=log)

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

        self._align_bin = os.path.join(scripts.BIN_DIR, 'fastalign-maurobuild', 'fast_align')

    def align(self, corpus, langs, model_dir, working_dir='.', log_file=None):
        WordAligner.align(self, corpus, langs, working_dir, log_file)

        l1 = langs[0]
        l2 = langs[1]
        corpus_name = 'corpus'
        langs_suffix = l1 + '-' + l2

        fwd_file = os.path.join(working_dir, corpus_name + '.' + langs_suffix + '.fwd')
        bwd_file = os.path.join(working_dir, corpus_name + '.' + langs_suffix + '.bwd')
        bal_file = os.path.join(working_dir, corpus_name + '.' + langs_suffix + '.bal')
        aligned_file_path = os.path.join(working_dir, corpus_name + '.' + langs_suffix + '.aligned')

        corpus_l1 = corpus.get_file(l1)
        corpus_l2 = corpus.get_file(l2)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'a')

            with open(corpus_l1) as source_corpus, \
                    open(corpus_l2) as target_corpus, \
                    open(aligned_file_path, 'w') as aligned_file:
                for x, y in zip(source_corpus, target_corpus):
                    aligned_file.write(x.strip() + ' ||| ' + y.strip() + '\n')

            cpus = multiprocessing.cpu_count()

            # Forward alignments
            fwd_model = os.path.join(model_dir, 'model.align.fwd')
            command = [self._align_bin, '-d', '-v', '-o', '-n', str(cpus), '-B', '-p', fwd_model, '-i',
                       aligned_file_path]
            with open(fwd_file, 'w') as stdout:
                shell.execute(command, stdout=stdout, stderr=log)

            # Backward alignments
            bwd_model = os.path.join(model_dir, 'model.align.bwd')
            command = [self._align_bin, '-d', '-v', '-o', '-n', str(cpus), '-B', '-p', bwd_model, '-r', '-i',
                       aligned_file_path]
            with open(bwd_file, 'w') as stdout:
                shell.execute(command, stdout=stdout, stderr=log)

        finally:
            if log_file is not None:
                log.close()

        encoder = _FastAlignBALEncoder(corpus, langs, fwd_file, bwd_file)
        encoder.encode(bal_file)

        return bal_file

class _FastAlignBALEncoder:
    def __init__(self, corpus, langs, fwd, bwd):
        self._corpus_l1 = corpus.get_file(langs[0])
        self._corpus_l2 = corpus.get_file(langs[1])
        self._fwd = fwd
        self._bwd = bwd

    def encode(self, output):
        files = []

        try:
            files.append(open(self._corpus_l1))
            files.append(open(self._corpus_l2))
            files.append(open(self._fwd))
            files.append(open(self._bwd))
            files.append(open(output, 'w'))

            c1, c2, fwd, bwd, bal = files

            for t1 in c1:
                t1 = t1.strip().split()
                t2 = c2.readline().strip().split()
                a1 = self._alnvec(len(t1), bwd.readline().split(), 0)
                a2 = self._alnvec(len(t2), fwd.readline().split(), 1)

                print >> bal, 1
                print >> bal, len(t2), ' '.join(t2), '#', ' '.join(["%d" % x for x in a2])
                print >> bal, len(t1), ' '.join(t1), '#', ' '.join(["%d" % x for x in a1])
        finally:
            for f in files:
                f.close()

    @staticmethod
    def _alnvec(slen, alinks, mode):
        d = dict([(int(x[mode]), int(x[(mode + 1) % 2]) + 1) for x in [y.split('-') for y in alinks]])
        return [d.get(i, 0) for i in xrange(slen)]


class MGizaPP(WordAligner):
    __mgiza_config_template = 'adbackoff 0\ncompactadtable 1\ncompactalignmentformat 0\n' \
                              'coocurrencefile {coocurrencefile}\ncorpusfile {corpusfile}\ncountcutoff 1e-06\n' \
                              'countcutoffal 1e-05\ncountincreasecutoff 1e-06\ncountincreasecutoffal 1e-05\n' \
                              'countoutputprefix\nd\ndeficientdistortionforemptyword 0\ndepm4 76\ndepm5 68\n' \
                              'dictionary\ndopeggingyn 0\ndumpcount 0\ndumpcountusingwordstring 0\n' \
                              'emalignmentdependencies 2\nemalsmooth 0.2\nemprobforempty 0.4\nemsmoothhmm 2\n' \
                              'hmmdumpfrequency 0\nhmmiterations 5\nlog 0\nm1 5\nm2 0\nm3 3\nm4 3\nm5 0\nm5p0 -1\n' \
                              'm6 0\nmanlexfactor1 0\nmanlexfactor2 0\nmanlexmaxmultiplicity 20\nmaxfertility 10\n' \
                              'maxsentencelength 101\nmh 5\nmincountincrease 1e-07\nml 101\nmodel1dumpfrequency 1\n' \
                              'model1iterations 5\nmodel23smoothfactor 0\nmodel2dumpfrequency 0\nmodel2iterations 0\n' \
                              'model345dumpfrequency 0\nmodel3dumpfrequency 0\nmodel3iterations 3\n' \
                              'model4iterations 3\nmodel4smoothfactor 0.4\nmodel5iterations 0\n' \
                              'model5smoothfactor 0.1\nmodel6iterations 0\nnbestalignments 0\nncpus {ncpus}\n' \
                              'nodumps 1\nnofiledumpsyn 1\nnoiterationsmodel1 5\nnoiterationsmodel2 0\n' \
                              'noiterationsmodel3 3\nnoiterationsmodel4 3\nnoiterationsmodel5 0\n' \
                              'noiterationsmodel6 0\nnsmooth 4\nnsmoothgeneral 0\n' \
                              'numberofiterationsforhmmalignmentmodel 5\nonlyaldumps 1\n' \
                              'outputfileprefix {outputfileprefix}\noutputpath\np 0\np0 0.999\npeggedcutoff 0.03\n' \
                              'pegging 0\npreviousa\npreviousd\npreviousd4\npreviousd42\nprevioushmm\npreviousn\n' \
                              'previousp0\nprevioust\nprobcutoff 1e-07\nprobsmooth 1e-07\nreadtableprefix\n' \
                              'restart 0\nsourcevocabularyfile {sourcevocabularyfile}\nt1 1\nt2 0\nt2to3 0\nt3 0\n' \
                              't345 0\ntargetvocabularyfile {targetvocabularyfile}\ntc\ntestcorpusfile\nth 0\n' \
                              'transferdumpfrequency 0\nv 0\nverbose 0\nverbosesentence -10'

    def __init__(self):
        WordAligner.__init__(self)

        self._mgiza_bin = os.path.join(scripts.BIN_DIR, 'mgizapp-master_a036__1e18', 'mgiza')
        self._plain2snt_bin = os.path.join(scripts.BIN_DIR, 'mgizapp-master_a036__1e18', 'plain2snt')
        self._snt2cooc_bin = os.path.join(scripts.BIN_DIR, 'mgizapp-master_a036__1e18', 'snt2cooc')
        self._giza2bal_bin = os.path.join(scripts.BIN_DIR, 'mgizapp-master_a036__1e18', 'giza2bal.pl')
        self._merge_bin = os.path.join(scripts.BIN_DIR, 'mgizapp-master_a036__1e18', 'merge_alignment.py')

    def align(self, corpus, langs, model_dir, working_dir='.', log_file=None):
        WordAligner.align(self, corpus, langs, working_dir, log_file)

        l1 = langs[0]
        l2 = langs[1]

        corpus_name = 'corpus'

        vcb1_file = os.path.join(working_dir, corpus_name + '.' + l1 + '.vcb')
        vcb2_file = os.path.join(working_dir, corpus_name + '.' + l2 + '.vcb')
        snt12_file = os.path.join(working_dir, corpus_name + '.' + l1 + '_' + l2 + '.snt')
        snt21_file = os.path.join(working_dir, corpus_name + '.' + l2 + '_' + l1 + '.snt')
        cooc12_file = os.path.join(working_dir, corpus_name + '.' + l1 + '_' + l2 + '.cooc')
        cooc21_file = os.path.join(working_dir, corpus_name + '.' + l2 + '_' + l1 + '.cooc')
        fwdc_file = os.path.join(working_dir, 'fwd.config')
        bwdc_file = os.path.join(working_dir, 'bwd.config')
        fwddict_file = os.path.join(working_dir, corpus_name + '.fwd.dict')
        bwddict_file = os.path.join(working_dir, corpus_name + '.bwd.dict')
        fwd_file = os.path.join(working_dir, corpus_name + '.fwd')
        bwd_file = os.path.join(working_dir, corpus_name + '.bwd')
        bal_file = os.path.join(working_dir, corpus_name + '.bal')

        corpus_l1 = corpus.get_file(l1)
        corpus_l2 = corpus.get_file(l2)

        log = shell.DEVNULL

        try:
            ncpus = max(2, multiprocessing.cpu_count())

            if log_file is not None:
                log = open(log_file, 'a')

            # Translate the corpora into GIZA format
            command = [self._plain2snt_bin, corpus_l1, corpus_l2, '-vcb1', vcb1_file, '-vcb2', vcb2_file, '-snt1',
                       snt12_file, '-snt2', snt21_file]
            shell.execute(command, stdout=log, stderr=log)

            # Create the cooccurence
            command = [self._snt2cooc_bin, cooc12_file, vcb1_file, vcb2_file, snt12_file]
            shell.execute(command, stdout=log, stderr=log)

            command = [self._snt2cooc_bin, cooc21_file, vcb2_file, vcb1_file, snt21_file]
            shell.execute(command, stdout=log, stderr=log)

            # Forward alignments
            with open(fwdc_file, 'w') as config:
                config.write(self.__mgiza_config_template.format(
                    coocurrencefile=cooc12_file,
                    corpusfile=snt12_file,
                    outputfileprefix=fwddict_file,
                    sourcevocabularyfile=vcb1_file,
                    targetvocabularyfile=vcb2_file,
                    ncpus=ncpus
                ))
            command = [self._mgiza_bin, fwdc_file]
            shell.execute(command, stdout=log, stderr=log)

            parts = [fwddict_file + '.A3.final.part{part:03d}'.format(part=part) for part in range(0, ncpus)]
            command = ['python', self._merge_bin] + parts
            with open(fwd_file, 'w') as stdout:
                shell.execute(command, stdout=stdout, stderr=log)

            # Backward alignments
            with open(bwdc_file, 'w') as config:
                config.write(self.__mgiza_config_template.format(
                    coocurrencefile=cooc21_file,
                    corpusfile=snt21_file,
                    outputfileprefix=bwddict_file,
                    sourcevocabularyfile=vcb2_file,
                    targetvocabularyfile=vcb1_file,
                    ncpus=ncpus
                ))
            command = [self._mgiza_bin, bwdc_file]
            shell.execute(command, stdout=log, stderr=log)

            parts = [bwddict_file + '.A3.final.part{part:03d}'.format(part=part) for part in range(0, ncpus)]
            command = ['python', self._merge_bin] + parts
            with open(bwd_file, 'w') as stdout:
                shell.execute(command, stdout=stdout, stderr=log)

            # Create BAL file
            command = [self._giza2bal_bin, '-i', bwd_file, '-d', fwd_file]
            with open(bal_file, 'w') as stdout:
                shell.execute(command, stdout=stdout, stderr=log)
        finally:
            if log_file is not None:
                log.close()

        return bal_file
