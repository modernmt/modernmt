import logging
import os
import shutil
import time
from xml.dom import minidom

import cli
from cli import IllegalArgumentException
from cli.libs import fileutils
from cli.libs import shell
from cli.mt import BilingualCorpus
from cli.mt.contextanalysis import ContextAnalyzer
from cli.mt.lm import InterpolatedLM
from cli.mt.moses import Moses, MosesFeature
from cli.mt.phrasetable import SuffixArraysPhraseTable, LexicalReordering, FastAlign
from cli.mt.processing import TrainingPreprocessor, TMCleaner

__author__ = 'Davide Caroselli'


class _DomainMapBuilder:
    def __init__(self, model, source_lang, target_lang):
        self._model = model
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._java_mainclass = 'eu.modernmt.cli.DomainMapMain'

    def generate(self, bilingual_corpora, monolingual_corpora, output, log=None):
        if log is None:
            log = shell.DEVNULL

        fileutils.makedirs(self._model, exist_ok=True)

        args = ['--db', os.path.join(self._model, 'domains.db'), '-s', self._source_lang, '-t', self._target_lang, '-c']

        source_paths = set([corpus.get_folder() for corpus in bilingual_corpora])
        for source_path in source_paths:
            args.append(source_path)

        command = cli.mmt_javamain(self._java_mainclass, args)
        stdout, _ = shell.execute(command, stderr=log)

        domains = {}

        for domain, name in [line.rstrip('\n').split('\t', 2) for line in stdout.splitlines()]:
            domains[name] = domain

        bilingual_corpora = [corpus.symlink(output, name=domains[corpus.name]) for corpus in bilingual_corpora]
        monolingual_corpora = [corpus.symlink(output) for corpus in monolingual_corpora]

        return bilingual_corpora, monolingual_corpora

    @staticmethod
    def _load_map(filepath):
        domains = {}

        for domain, name in [line.rstrip('\n').split('\t', 2) for line in open(filepath)]:
            domains[name] = domain

        return domains


class _builder_logger:
    def __init__(self, steps_count, log_file, line_len=70):
        self.stream = open(log_file, 'wb')

        logging.basicConfig(format='%(asctime)-15s [%(levelname)s] - %(message)s',
                            level=logging.DEBUG, stream=self.stream)

        self._logger = logging.getLogger('EngineBuilder')
        self._line_len = line_len
        self._engine_name = None

        self._steps_count = steps_count
        self._current_step_num = 0
        self._current_step_name = None

        self._step_start_time = 0
        self._start_time = 0

    def start(self, engine, bilingual_corpora, monolingual_corpora):
        self._start_time = time.time()
        self._engine_name = engine.name if engine.name != 'default' else None

        self._logger.log(logging.INFO, 'Training started: engine=%s, bilingual=%d, monolingual=%d, langpair=%s-%s' %
                         (engine.name, len(bilingual_corpora), len(monolingual_corpora),
                          engine.source_lang, engine.target_lang))

        print '\n=========== TRAINING STARTED ===========\n'
        print 'ENGINE:  %s' % engine.name
        print 'BILINGUAL CORPORA: %d documents' % len(bilingual_corpora)
        print 'MONOLINGUAL CORPORA: %d documents' % len(monolingual_corpora)
        print 'LANGS:   %s > %s' % (engine.source_lang, engine.target_lang)
        print

    def step(self, step):
        self._current_step_name = step
        self._current_step_num += 1
        return self

    def completed(self):
        self._logger.log(logging.INFO,
                         'Training completed in %s' % self._pretty_print_time(time.time() - self._start_time))

        print '\n=========== TRAINING SUCCESS ===========\n'
        print 'You can now start, stop or check the status of the server with command:'
        print '\t./mmt start|stop|status ' + ('' if self._engine_name is None else '-e %s' % self._engine_name)
        print

    def error(self):
        self._logger.exception('Unexpected exception')

    def close(self):
        self.stream.close()

    def __enter__(self):
        self._logger.log(logging.INFO, 'Training step "%s" (%d/%d) started' %
                         (self._current_step_name, self._current_step_num, self._steps_count))

        message = 'INFO: (%d of %d) %s... ' % (self._current_step_num, self._steps_count, self._current_step_name)
        print message.ljust(self._line_len),

        self._step_start_time = time.time()
        return self

    def __exit__(self, *_):
        elapsed_time = time.time() - self._step_start_time
        self._logger.log(logging.INFO, 'Training step "%s" completed in %s' %
                         (self._current_step_name, self._pretty_print_time(elapsed_time)))
        print 'DONE (in %s)' % self._pretty_print_time(elapsed_time)

    @staticmethod
    def _pretty_print_time(elapsed):
        elapsed = int(elapsed)
        parts = []

        if elapsed > 86400:  # days
            d = int(elapsed / 86400)
            elapsed -= d * 86400
            parts.append('%dd' % d)
        if elapsed > 3600:  # hours
            h = int(elapsed / 3600)
            elapsed -= h * 3600
            parts.append('%dh' % h)
        if elapsed > 60:  # minutes
            m = int(elapsed / 60)
            elapsed -= m * 60
            parts.append('%dm' % m)
        parts.append('%ds' % elapsed)

        return ' '.join(parts)


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

        logger = _builder_logger(len(steps) + 1, self._engine.get_logfile('training'))

        try:
            logger.start(self._engine, bilingual_corpora, monolingual_corpora)

            unprocessed_bicorpora = bilingual_corpora
            unprocessed_monocorpora = monolingual_corpora

            # TM draft-translations cleanup
            if 'tm_cleanup' in steps:
                with logger.step('TMs clean-up') as _:
                    unprocessed_bicorpora = self._engine.cleaner.clean(
                        unprocessed_bicorpora, self._get_tempdir('clean_tms'), log=logger.stream)

            cleaned_bicorpora = unprocessed_bicorpora
            processed_bicorpora = unprocessed_bicorpora
            processed_monocorpora = unprocessed_monocorpora

            # Preprocessing
            if 'preprocess' in steps:
                with logger.step('Corpora preprocessing') as _:
                    unprocessed_bicorpora, unprocessed_monocorpora = self._engine.db.generate(
                        unprocessed_bicorpora, unprocessed_monocorpora, self._get_tempdir('training_corpora'),
                        log=logger.stream)

                    processed_bicorpora, processed_monocorpora = self._engine.training_preprocessor.process(
                        unprocessed_bicorpora + unprocessed_monocorpora, self._get_tempdir('preprocessed'),
                        (self._engine.data_path if split_trainingset else None), log=logger.stream)

                    cleaned_bicorpora = self._engine.training_preprocessor.clean(
                        processed_bicorpora, self._get_tempdir('clean_corpora'))

            # Training Context Analyzer
            if 'context_analyzer' in steps:
                with logger.step('Context Analyzer training') as _:
                    self._engine.analyzer.create_index(unprocessed_bicorpora, log=logger.stream)

            # Aligner
            if 'aligner' in steps:
                with logger.step('Aligner training') as _:
                    working_dir = self._get_tempdir('aligner')
                    self._engine.aligner.build(cleaned_bicorpora, working_dir, log=logger.stream)

            # Training Translation Model
            if 'tm' in steps:
                with logger.step('Translation Model training') as _:
                    working_dir = self._get_tempdir('tm')
                    self._engine.pt.train(cleaned_bicorpora, self._engine.aligner, working_dir, log=logger.stream)

            # Training Adaptive Language Model
            if 'lm' in steps:
                with logger.step('Language Model training') as _:
                    working_dir = self._get_tempdir('lm')
                    self._engine.lm.train(processed_bicorpora + processed_monocorpora, target_lang,
                                          working_dir, log=logger.stream)

            # Writing config file
            with logger.step('Writing config files') as _:
                self._engine.write_configs()

            logger.completed()
        except:
            logger.error()
            raise
        finally:
            logger.close()
            if not debug:
                self._engine.clear_tempdir('training')


class _EngineConfig(object):
    @staticmethod
    def from_file(file):
        node_el = minidom.parse(file).documentElement
        engine_el = node_el.getElementsByTagName('engine')[0]

        name = engine_el.getAttribute('name')
        source_lang = engine_el.getAttribute('source-language')
        target_lang = engine_el.getAttribute('target-language')

        return _EngineConfig(name, source_lang, target_lang)

    def __init__(self, name, source_lang, target_lang):
        self.name = name
        self.source_lang = source_lang
        self.target_lang = target_lang

    def store(self, file):
        xml_template = '''<node xsi:schemaLocation="http://www.modernmt.eu/schema/config mmt-config-1.0.xsd"
      xmlns="http://www.modernmt.eu/schema/config"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <engine name="%s" source-language="%s" target-language="%s" />
</node>'''

        with open(file, 'wb') as out:
            out.write(xml_template % (self.name, self.source_lang, self.target_lang))


class MMTEngine(object):
    training_steps = ['tm_cleanup', 'preprocess', 'context_analyzer', 'aligner', 'tm', 'lm']

    @staticmethod
    def _get_path(name):
        return os.path.join(cli.ENGINES_DIR, name)

    @staticmethod
    def _get_config_path(name):
        return os.path.join(cli.ENGINES_DIR, name, 'engine.xconf')

    @staticmethod
    def list():
        return sorted([MMTEngine.load(name) for name in os.listdir(cli.ENGINES_DIR)
                       if os.path.isfile(MMTEngine._get_config_path(name))], key=lambda x: x.name)

    @staticmethod
    def load(name):
        config = _EngineConfig.from_file(MMTEngine._get_config_path(name))
        return MMTEngine(config.name, config.source_lang, config.target_lang, config)

    def __init__(self, name, source_lang, target_lang, config=None):
        self.name = name if name is not None else 'default'
        self.source_lang = source_lang
        self.target_lang = target_lang

        self._config_file = self._get_config_path(self.name)
        self._config = _EngineConfig(name, source_lang, target_lang) if config is None else config

        self.path = self._get_path(self.name)
        self.data_path = os.path.join(self.path, 'data')
        self.models_path = os.path.join(self.path, 'models')
        self.runtime_path = os.path.join(cli.RUNTIME_DIR, self.name)
        self._logs_path = os.path.join(self.runtime_path, 'logs')
        self._temp_path = os.path.join(self.runtime_path, 'tmp')

        self.builder = _MMTEngineBuilder(self)

        self._db_path = os.path.join(self.models_path, 'db')
        self._vocabulary_model = os.path.join(self.models_path, 'vocabulary')
        self._aligner_model = os.path.join(self.models_path, 'align')
        self._context_index = os.path.join(self.models_path, 'context')
        self._moses_path = os.path.join(self.models_path, 'decoder')
        self._lm_model = os.path.join(self._moses_path, 'lm')
        self._pt_model = os.path.join(self._moses_path, 'sapt')

        self.analyzer = ContextAnalyzer(self._context_index, self.source_lang, self.target_lang)
        self.cleaner = TMCleaner(self.source_lang, self.target_lang)
        self.pt = SuffixArraysPhraseTable(self._pt_model, (self.source_lang, self.target_lang))
        self.pt.set_reordering_model('DM0')
        self.aligner = FastAlign(self._aligner_model, self.source_lang, self.target_lang)
        self.lm = InterpolatedLM(self._lm_model)
        self.training_preprocessor = TrainingPreprocessor(self.source_lang, self.target_lang, self._vocabulary_model)
        self.db = _DomainMapBuilder(self._db_path, self.source_lang, self.target_lang)
        self.moses = Moses(self._moses_path)
        self.moses.add_feature(MosesFeature('UnknownWordPenalty'))
        self.moses.add_feature(MosesFeature('WordPenalty'))
        self.moses.add_feature(MosesFeature('Distortion'))
        self.moses.add_feature(MosesFeature('PhrasePenalty'))
        self.moses.add_feature(self.pt, 'Sapt')
        self.moses.add_feature(LexicalReordering(), 'DM0')
        self.moses.add_feature(self.lm, 'InterpolatedLM')

    def exists(self):
        return os.path.isfile(self._config_file)

    def _on_fields_injected(self, injector):
        self.analyzer = injector.inject(self.analyzer)
        self.pt = injector.inject(self.pt)
        self.aligner = injector.inject(self.aligner)
        self.lm = injector.inject(self.lm)
        self.training_preprocessor = injector.inject(self.training_preprocessor)
        self.moses = injector.inject(self.moses)

    def write_configs(self):
        self.moses.create_configs()
        self._config.store(self._config_file)

    def get_logfile(self, name, ensure=True):
        if ensure and not os.path.isdir(self._logs_path):
            fileutils.makedirs(self._logs_path, exist_ok=True)

        logfile = os.path.join(self._logs_path, name + '.log')

        if ensure and os.path.isfile(logfile):
            os.remove(logfile)

        return logfile

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
