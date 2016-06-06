import os
import shutil
import sys
import time
from ConfigParser import ConfigParser

import scripts
from scripts import IllegalArgumentException, IllegalStateException
from scripts.libs import fileutils
from scripts.mt import ParallelCorpus
from scripts.mt.contextanalysis import ContextAnalyzer
from scripts.mt.lm import LanguageModel
from scripts.mt.moses import Moses, MosesFeature, LexicalReordering
from scripts.mt.phrasetable import WordAligner, SuffixArraysPhraseTable
from scripts.mt.processing import Preprocessor, TrainingPreprocessor, TMCleaner

__author__ = 'Davide Caroselli'


class _builder_logger:
    def __init__(self, count, line_len=70):
        self.line_len = line_len
        self.count = count
        self._current_step = 0
        self._step = None
        self._engine_name = None

    def start(self, engine, bilingual_corpora, monolingual_corpora):
        self._engine_name = engine.name if engine.name != 'default' else None
        print '\n=========== TRAINING STARTED ===========\n'
        print 'ENGINE:  %s' % engine.name
        print 'BILINGUAL CORPORA: %d documents' % len(bilingual_corpora)
        print 'MONOLINGUAL CORPORA: %d documents' % len(monolingual_corpora)
        print 'LANGS:   %s > %s' % (engine.source_lang, engine.target_lang)
        print
        sys.stdout.flush()

    def step(self, step):
        self._step = step
        self._current_step += 1
        return self

    def completed(self):
        print '\n=========== TRAINING SUCCESS ===========\n'
        print 'You can now start, stop or check the status of the server with command:'
        print '\t./mmt start|stop|status ' + ('' if self._engine_name is None else '-e %s' % self._engine_name)
        print
        sys.stdout.flush()

    def __enter__(self):
        message = 'INFO: (%d of %d) %s... ' % (self._current_step, self.count, self._step)
        print message.ljust(self.line_len),
        sys.stdout.flush()

        self._start_time = time.time()
        return self

    def __exit__(self, *_):
        self._end_time = time.time()
        print 'DONE (in %ds)' % int(self._end_time - self._start_time)
        sys.stdout.flush()


class _MMTEngineBuilder:
    __MB = (1024 * 1024)
    __GB = (1024 * 1024 * 1024)

    def __init__(self, engine):
        self._engine = engine
        self._temp_dir = None

    def _get_tempdir(self, name):
        path = os.path.join(self._temp_dir, name)
        if not os.path.isdir(path):
            fileutils.makedirs(path, exist_ok=True)
        return path

    def build(self, roots, debug=False, steps=None, split_trainingset=True):
        self._temp_dir = self._engine.get_tempdir('training', ensure=True)

        source_lang = self._engine.source_lang
        target_lang = self._engine.target_lang

        bilingual_corpora, monolingual_corpora = ParallelCorpus.splitlist(source_lang, target_lang, roots=roots)

        if len(bilingual_corpora) == 0:
            raise IllegalArgumentException(
                'you project does not include %s-%s data.' % (source_lang.upper(), target_lang.upper()))

        if steps is None:
            steps = self._engine.training_steps
        else:
            unknown_steps = [step for step in steps if step not in self._engine.training_steps]
            if len(unknown_steps) > 0:
                raise IllegalArgumentException('Unknown training steps: ' + str(unknown_steps))

        cmdlogger = _builder_logger(len(steps) + 1)
        cmdlogger.start(self._engine, bilingual_corpora, monolingual_corpora)

        shutil.rmtree(self._engine.path, ignore_errors=True)
        os.makedirs(self._engine.path)

        # Check disk space constraints
        free_space_on_disk = fileutils.df(self._engine.path)[2]
        corpus_size_on_disk = 0
        for root in roots:
            corpus_size_on_disk += fileutils.du(root)
        free_memory = fileutils.free()

        recommended_mem = self.__GB * corpus_size_on_disk / (350 * self.__MB)  # 1G RAM every 350M on disk
        recommended_disk = 10 * corpus_size_on_disk

        if free_memory < recommended_mem or free_space_on_disk < recommended_disk:
            if free_memory < recommended_mem:
                print '> WARNING: more than %.fG of RAM recommended, only %.fG available' % \
                      (recommended_mem / self.__GB, free_memory / self.__GB)
            if free_space_on_disk < recommended_disk:
                print '> WARNING: more than %.fG of storage recommended, only %.fG available' % \
                      (recommended_disk / self.__GB, free_space_on_disk / self.__GB)
            print

        try:
            corpora_roots = roots

            unprocessed_bicorpora = bilingual_corpora
            unprocessed_mocorpora = monolingual_corpora

            # TM cleanup
            if 'tm_cleanup' in steps:
                with cmdlogger.step('TMs clean-up') as _:
                    cleaned_output = self._get_tempdir('clean_tms')
                    self._engine.cleaner.clean(source_lang, target_lang, roots, cleaned_output)

                    for corpus in monolingual_corpora:
                        cfile = corpus.get_file(target_lang)
                        link = os.path.join(cleaned_output, os.path.basename(cfile))
                        os.symlink(cfile, link)

                    corpora_roots = [cleaned_output]
                    unprocessed_bicorpora, unprocessed_mocorpora = ParallelCorpus.splitlist(source_lang, target_lang,
                                                                                            roots=corpora_roots)

            # Preprocessing
            processed_bicorpora = unprocessed_bicorpora
            processed_mocorpora = unprocessed_mocorpora

            if 'preprocess' in steps:
                with cmdlogger.step('Corpora preprocessing') as _:
                    preprocessor_output = self._get_tempdir('preprocessed')
                    processed_bicorpora, processed_mocorpora = self._engine.training_preprocessor.process(
                        source_lang, target_lang, corpora_roots, preprocessor_output,
                        (self._engine.data_path if split_trainingset else None)
                    )

            # Training Context Analyzer
            if 'context_analyzer' in steps:
                with cmdlogger.step('Context Analyzer training') as _:
                    log_file = self._engine.get_logfile('training.context')
                    self._engine.analyzer.create_index(unprocessed_bicorpora, source_lang, log_file=log_file)

            # Training Adaptive Language Model (on the target side of all bilingual corpora)
            if 'lm' in steps:
                with cmdlogger.step('Language Model training') as _:
                    working_dir = self._get_tempdir('lm')
                    log_file = self._engine.get_logfile('training.lm')
                    self._engine.lm.train(processed_bicorpora + processed_mocorpora, target_lang, working_dir, log_file)

            # Training Translation Model
            if 'tm' in steps:
                with cmdlogger.step('Translation Model training') as _:
                    working_dir = self._get_tempdir('tm')
                    log_file = self._engine.get_logfile('training.tm')
                    self._engine.pt.train(processed_bicorpora, self._engine.aligner, working_dir, log_file)

            # Writing config file
            with cmdlogger.step('Writing config files') as _:
                self._engine.moses.create_ini()
                self._engine.write_config()

            cmdlogger.completed()
        finally:
            if not debug:
                self._engine.clear_tempdir('training')


class MMTEngine:
    injector_section = 'engine'
    injectable_fields = {
        'lm_type': ('LM implementation', (basestring, LanguageModel.available_types), 'MultiplexedLM'),
        'aligner_type': ('Aligner implementation',
                         (basestring, WordAligner.available_types), WordAligner.available_types[0]),
        'enable_tag_projection': ('Enable Tag Projection, this may take some time during engine startup.', bool, False)
    }

    training_steps = ['tm_cleanup', 'preprocess', 'context_analyzer', 'lm', 'tm']

    @staticmethod
    def list():
        return sorted([MMTEngine(name=name) for name in os.listdir(scripts.ENGINES_DIR)
                       if os.path.isdir(os.path.join(scripts.ENGINES_DIR, name))], key=lambda x: x.name)

    def __init__(self, langs=None, name=None):
        self.name = name if name is not None else 'default'
        self.source_lang = langs[0] if langs is not None else None
        self.target_lang = langs[1] if langs is not None else None

        self._lm_type = None  # Injected
        self._aligner_type = None  # Injected
        self._enable_tag_projection = None  # Injected

        self._config = None

        self.path = os.path.join(scripts.ENGINES_DIR, self.name)

        self.data_path = os.path.join(self.path, 'data')
        self.models_path = os.path.join(self.path, 'models')

        self._config_file = os.path.join(self.path, 'engine.ini')
        self._pt_model = os.path.join(self.models_path, 'phrase_tables')
        self._lm_model = os.path.join(self.models_path, 'lm', 'target.lm')
        self._context_index = os.path.join(self.models_path, 'context', 'index')
        self._moses_ini_file = os.path.join(self.models_path, 'moses.ini')

        self._runtime_path = os.path.join(scripts.RUNTIME_DIR, self.name)
        self._logs_path = os.path.join(self._runtime_path, 'logs')
        self._temp_path = os.path.join(self._runtime_path, 'tmp')

        self.builder = _MMTEngineBuilder(self)

        self._optimal_weights = None

    def exists(self):
        return os.path.isfile(self._config_file)

    def _on_fields_injected(self, injector):
        if self.target_lang is None or self.source_lang is None:
            config = self.config

            if config is not None:
                self.target_lang = config.get(self.injector_section, 'target_lang')
                self.source_lang = config.get(self.injector_section, 'source_lang')

        if self.target_lang is None or self.source_lang is None:
            raise IllegalStateException('Engine target language or source language must be specified')

        self.training_preprocessor = TrainingPreprocessor()
        self.preprocessor = Preprocessor()

        self.analyzer = injector.inject(ContextAnalyzer(self._context_index))
        self.cleaner = TMCleaner()

        self.pt = injector.inject(SuffixArraysPhraseTable(self._pt_model, (self.source_lang, self.target_lang)))
        self.aligner = injector.inject(WordAligner.instantiate(self._aligner_type))
        self.lm = injector.inject(LanguageModel.instantiate(self._lm_type, self._lm_model))

        self.moses = injector.inject(Moses(self._moses_ini_file))
        self.moses.add_feature(MosesFeature('UnknownWordPenalty'))
        self.moses.add_feature(MosesFeature('WordPenalty'))
        self.moses.add_feature(MosesFeature('Distortion'))
        self.moses.add_feature(MosesFeature('PhrasePenalty'))
        self.moses.add_feature(self.pt, 'PT0')
        self.moses.add_feature(LexicalReordering(), 'DM0')
        self.moses.add_feature(self.lm, 'MuxLM')

        self._optimal_weights = {
            'MuxLM': [0.03],
            'DM0': [0.0281009, 0.0254415, 0.0229716, 0.0334702, 0.0440066, 0.0106037, 0.163133, 0.179085],
            'Distortion0': [0.00517499],
            'WordPenalty0': [-0.124562],
            'PhrasePenalty0': [-0.165601],
            'PT0': [8.95681E-4, 0.0163699, 0.102362, 0.0310822, 0.0160701],
        }

        if self._config is None:
            self._config = injector.to_config()
            self._config.set(self.injector_section, 'source_lang', self.source_lang)
            self._config.set(self.injector_section, 'target_lang', self.target_lang)

    @property
    def config(self):
        if self._config is None and os.path.isfile(self._config_file):
            self._config = ConfigParser()
            self._config.read(self._config_file)
        return self._config

    def write_config(self):
        with open(self._config_file, 'wb') as out:
            self._config.write(out)

            if self._optimal_weights is not None and len(self._optimal_weights) > 0:
                out.write('[weights]\n')

                for name, weights in self._optimal_weights.iteritems():
                    out.write(name)
                    out.write(' = ')
                    out.write(' '.join([str(w) for w in weights]))
                    out.write('\n')

    def get_logfile(self, name, ensure=True):
        if ensure and not os.path.isdir(self._logs_path):
            fileutils.makedirs(self._logs_path, exist_ok=True)

        logfile = os.path.join(self._logs_path, name + '.log')

        if ensure and os.path.isfile(logfile):
            os.remove(logfile)

        return logfile

    def get_runtime_path(self):
        return self._runtime_path

    def get_tempdir(self, name, ensure=True):
        if ensure and not os.path.isdir(self._temp_path):
            fileutils.makedirs(self._temp_path, exist_ok=True)

        folder = os.path.join(self._temp_path, name)

        if ensure:
            shutil.rmtree(folder, ignore_errors=True)
            os.makedirs(folder)

        return folder

    def clear_tempdir(self, subdir=None):
        path = os.path.join(self._temp_path, subdir) if subdir is not None else self._temp_path
        shutil.rmtree(path, ignore_errors=True)
