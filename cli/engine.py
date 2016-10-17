import os
import shutil
import sys
import time
from ConfigParser import ConfigParser

import cli
from cli import IllegalArgumentException, IllegalStateException
from cli.libs import fileutils
from cli.libs import shell
from cli.mt import BilingualCorpus
from cli.mt.contextanalysis import ContextAnalyzer
from cli.mt.lm import LanguageModel
from cli.mt.moses import Moses, MosesFeature, LexicalReordering
from cli.mt.phrasetable import WordAligner, SuffixArraysPhraseTable
from cli.mt.processing import TrainingPreprocessor, TMCleaner

__author__ = 'Davide Caroselli'


class _DomainMapBuilder:
    def __init__(self, model, source_lang, target_lang):
        self._model = model
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._java_mainclass = 'eu.modernmt.cli.DomainMapMain'

    def generate(self, bilingual_corpora, monolingual_corpora, output, log_file=None):
        fileutils.makedirs(self._model, exist_ok=True)

        args = ['--db', os.path.join(self._model, 'domains.db'), '-l', self._source_lang, '-c']

        source_paths = set([corpus.get_folder() for corpus in bilingual_corpora])
        for source_path in source_paths:
            args.append(source_path)

        command = cli.mmt_javamain(self._java_mainclass, args)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'w')

            stdout, _ = shell.execute(command, stderr=log)

            domains = {}

            for domain, name in [line.rstrip('\n').split('\t', 2) for line in stdout.splitlines()]:
                domains[name] = domain

            return self._make_training_folder(bilingual_corpora, monolingual_corpora, domains, output)
        finally:
            if log_file is not None:
                log.close()

    def _make_training_folder(self, bilingual_corpora, monolingual_corpora, domains, folder):
        for corpus in bilingual_corpora:
            dest_corpus = BilingualCorpus.make_parallel(domains[corpus.name], folder, corpus.langs)

            for lang in corpus.langs:
                os.symlink(corpus.get_file(lang), dest_corpus.get_file(lang))

        for corpus in monolingual_corpora:
            dest_corpus = BilingualCorpus.make_parallel(corpus.name, folder, corpus.langs)

            for lang in corpus.langs:
                os.symlink(corpus.get_file(lang), dest_corpus.get_file(lang))

        return BilingualCorpus.splitlist(self._source_lang, self._target_lang, roots=folder)

    @staticmethod
    def _load_map(filepath):
        domains = {}

        for domain, name in [line.rstrip('\n').split('\t', 2) for line in open(filepath)]:
            domains[name] = domain

        return domains


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

        bilingual_corpora, monolingual_corpora = BilingualCorpus.splitlist(source_lang, target_lang, roots=roots)

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
            unprocessed_bicorpora = bilingual_corpora
            unprocessed_monocorpora = monolingual_corpora

            # TM draft-translations cleanup
            if 'tm_cleanup' in steps:
                with cmdlogger.step('TMs clean-up') as _:
                    unprocessed_bicorpora = self._engine.cleaner.clean(
                        unprocessed_bicorpora, self._get_tempdir('clean_tms')
                    )

            cleaned_bicorpora = unprocessed_bicorpora
            processed_bicorpora = unprocessed_bicorpora
            processed_monocorpora = unprocessed_monocorpora

            # Preprocessing
            if 'preprocess' in steps:
                with cmdlogger.step('Corpora preprocessing') as _:
                    unprocessed_bicorpora, unprocessed_monocorpora = self._engine.db.generate(
                        unprocessed_bicorpora, unprocessed_monocorpora, self._get_tempdir('training_corpora')
                    )

                    processed_bicorpora, processed_monocorpora = self._engine.training_preprocessor.process(
                        unprocessed_bicorpora + unprocessed_monocorpora, self._get_tempdir('preprocessed'),
                        (self._engine.data_path if split_trainingset else None)
                    )

                    cleaned_bicorpora = self._engine.training_preprocessor.clean(
                        processed_bicorpora, self._get_tempdir('clean_corpora')
                    )

            # Training Context Analyzer
            if 'context_analyzer' in steps:
                with cmdlogger.step('Context Analyzer training') as _:
                    log_file = self._engine.get_logfile('training.context')
                    self._engine.analyzer.create_index(unprocessed_bicorpora, source_lang, log_file=log_file)

            # Aligner
            if 'aligner' in steps:
                with cmdlogger.step('Aligner training') as _:
                    log_file = self._engine.get_logfile('training.aligner')
                    working_dir = self._get_tempdir('aligner')

                    self._engine.aligner.build(cleaned_bicorpora, working_dir, log_file)

            # Training Translation Model
            if 'tm' in steps:
                with cmdlogger.step('Translation Model training') as _:
                    working_dir = self._get_tempdir('tm')
                    log_file = self._engine.get_logfile('training.tm')
                    self._engine.pt.train(cleaned_bicorpora, self._engine.aligner, working_dir, log_file)

            # Training Adaptive Language Model
            if 'lm' in steps:
                with cmdlogger.step('Language Model training') as _:
                    working_dir = self._get_tempdir('lm')
                    log_file = self._engine.get_logfile('training.lm')
                    self._engine.lm.train(processed_bicorpora + processed_monocorpora, target_lang,
                                          working_dir, log_file)

            # Writing config file
            with cmdlogger.step('Writing config files') as _:
                self._engine.write_configs()

            cmdlogger.completed()
        finally:
            if not debug:
                self._engine.clear_tempdir('training')


class MMTEngine(object):
    injector_section = 'engine'
    injectable_fields = {
        'lm_type': ('LM implementation', (basestring, LanguageModel.available_types), None),
        'aligner_type': ('Aligner implementation',
                         (basestring, WordAligner.available_types), None),
    }

    training_steps = ['tm_cleanup', 'preprocess', 'context_analyzer', 'aligner', 'tm', 'lm']

    @staticmethod
    def list():
        return sorted([MMTEngine(name=name) for name in os.listdir(cli.ENGINES_DIR)
                       if os.path.isdir(os.path.join(cli.ENGINES_DIR, name))], key=lambda x: x.name)

    def __init__(self, langs=None, name=None):
        self.name = name if name is not None else 'default'
        self.source_lang = langs[0] if langs is not None else None
        self.target_lang = langs[1] if langs is not None else None

        self._lm_type = None  # Injected
        self._aligner_type = None  # Injected

        self._config = None

        self.path = os.path.join(cli.ENGINES_DIR, self.name)

        self.data_path = os.path.join(self.path, 'data')
        self.models_path = os.path.join(self.path, 'models')

        self._config_file = os.path.join(self.path, 'engine.ini')
        self._vocabulary_model = os.path.join(self.models_path, 'vocabulary')
        self._pt_model = os.path.join(self.models_path, 'sapt')
        self._aligner_model = os.path.join(self.models_path, 'align')
        self._lm_model = os.path.join(self.models_path, 'lm')
        self._context_index = os.path.join(self.models_path, 'context')
        self._moses_ini_file = os.path.join(self.models_path, 'moses.ini')
        self._db_path = os.path.join(self.models_path, 'db')

        self._runtime_path = os.path.join(cli.RUNTIME_DIR, self.name)
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

        if self._lm_type is None:
            self._lm_type = LanguageModel.available_types[0]
        if self._aligner_type is None:
            self._aligner_type = WordAligner.available_types[0]

        self.analyzer = injector.inject(ContextAnalyzer(self._context_index))
        self.cleaner = TMCleaner(self.source_lang, self.target_lang)

        self.pt = injector.inject(SuffixArraysPhraseTable(self._pt_model, (self.source_lang, self.target_lang)))
        self.aligner = injector.inject(
            WordAligner.instantiate(self._aligner_type, self._aligner_model, self.source_lang, self.target_lang)
        )
        self.lm = injector.inject(LanguageModel.instantiate(self._lm_type, self._lm_model))
        self.training_preprocessor = injector.inject(
            TrainingPreprocessor(self.source_lang, self.target_lang, self._vocabulary_model)
        )

        self.db = _DomainMapBuilder(self._db_path, self.source_lang, self.target_lang)

        self.moses = injector.inject(Moses(self._moses_ini_file))
        self.moses.add_feature(MosesFeature('UnknownWordPenalty'))
        self.moses.add_feature(MosesFeature('WordPenalty'))
        self.moses.add_feature(MosesFeature('Distortion'))
        self.moses.add_feature(MosesFeature('PhrasePenalty'))
        self.moses.add_feature(self.pt, 'Sapt')
        # self.moses.add_feature(LexicalReordering(), 'DM0')
        self.moses.add_feature(self.lm, 'InterpolatedLM')

        self._optimal_weights = {
            'InterpolatedLM': [0.261806],
            'Distortion0': [0.161326],
            'WordPenalty0': [-0.163892],
            'PhrasePenalty0': [-0.189044],
            'Sapt': [0.0632762, 0.105652, 0.00365665, 0.0513473],
        }

        if self._config is None:
            self._config = injector.to_config()
            self._config.set(self.injector_section, 'source_lang', self.source_lang)
            self._config.set(self.injector_section, 'target_lang', self.target_lang)

    @property
    def config(self):
        if self._config is None and os.path.isfile(self._config_file):
            self._config = ConfigParser()
            self._config.optionxform = str  # make ConfigParser() case sensitive (avoid lowercasing Moses feature weight names in write())
            self._config.read(self._config_file)
        return self._config

    def set_config_option(self, section, option, value=None):
        """
        Set engine configuration option in the config dictionary.
        * use dependency.Injector with read_config() and inject() to affect MoseeFeatures, so an up-to-date 'moses.ini' gets written to disk in write_configs()
        * call write_configs() to write 'engine.ini' (and 'moses.ini') to disk
        * call ClusterNode.restart() for values to take effect
        """
        assert (MMTEngine.config_option_exists(section, option))
        # coerce all types to str -- because they are parsed back in "ConfigParser.py", line 663, in _interpolate
        self.config.set(section, option, str(value))

    @staticmethod
    def config_option_exists(section, option):
        """check if section and option indeed exist"""
        from cli import dependency  # cannot be at the top to avoid circular imports
        for clazz in dependency.injectable_components:
            if not hasattr(clazz, 'injectable_fields') or not hasattr(clazz, 'injector_section'):
                continue
            if clazz.injector_section == section and option in clazz.injectable_fields:
                return True
        return False

    def write_configs(self):
        """write engine.ini and moses.ini"""
        self.moses.create_ini()
        self.write_engine_config()

    def write_engine_config(self):
        # set default weights if not already in config
        if self._optimal_weights is not None and not 'weights' in self._config.sections():
            self._config.add_section('weights')
            for name, weights in self._optimal_weights.iteritems():
                self._config.set('weights', name, ' '.join([str(w) for w in weights]))
        # end "set default weights"

        with open(self._config_file, 'wb') as out:
            self._config.write(out)

    def backup_engine_config(self):
        shutil.copy(self._config_file, self._config_file + '.bak')

    def restore_engine_config(self):
        shutil.move(self._config_file + '.bak', self._config_file)

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
