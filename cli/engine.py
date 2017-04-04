import logging
import os
import shutil
import time
from xml.dom import minidom
import json

import cli
from cli import IllegalArgumentException
from cli.libs import fileutils
from cli.mt import BilingualCorpus
from cli.mt.contextanalysis import ContextAnalyzer
from cli.mt.lm import InterpolatedLM
from cli.mt.moses import Moses, MosesFeature
from cli.mt.phrasetable import SuffixArraysPhraseTable, LexicalReordering, FastAlign
from cli.mt.processing import TrainingPreprocessor, TMCleaner

__author__ = 'Davide Caroselli'


# This class manages the Map <domainID - domainName> during the MMT creation.
class BaselineDatabase:
    def __init__(self, db_path):
        self.path = db_path
        self._json_path = os.path.join(db_path, "baseline_domains.json")

    # this method generates the initial domainID-domainName map during MMT Create
    def generate(self, bilingual_corpora, monolingual_corpora, output):
        # create a domains dictionary, reading the domains as the bilingual corpora names;
        # also create an inverted domains dictionary
        domains = []
        inverted_domains = {}
        for i in xrange(len(bilingual_corpora)):
            domain = {}
            domain_id = str(i + 1)
            domain_name = bilingual_corpora[i].name
            domain["id"] = domain_id
            domain["name"] = domain_name
            domains.append(domain)
            inverted_domains[domain_name] = domain_id

        # create the necessary folders if they don't already exist
        if not os.path.isdir(self.path):
            os.makedirs(self.path)

        # creates the json file and stores the domains inside it
        with open(self._json_path, 'w') as json_file:
            json.dump(domains, json_file)

        bilingual_corpora = [corpus.symlink(output, name=inverted_domains[corpus.name]) for corpus in bilingual_corpora]
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

    def __init__(self, engine, roots, debug=False, steps=None, split_trainingset=True):
        self._engine = engine
        self._roots = roots
        self._debug = debug
        self._split_trainingset = split_trainingset

        if steps is None:
            self._steps = self._engine.training_steps
        else:
            unknown_steps = [step for step in steps if step not in self._engine.training_steps]
            if len(unknown_steps) > 0:
                raise IllegalArgumentException('Unknown training steps: ' + str(unknown_steps))
            self._steps = steps

        self._temp_dir = None
        self._checkpoint_manager = None

    def _get_tempdir(self, name, delete_if_exists=False):
        path = os.path.join(self._temp_dir, name)
        if delete_if_exists:
            shutil.rmtree(path, ignore_errors=True)
        if not os.path.isdir(path):
            fileutils.makedirs(path, exist_ok=True)
        return path

    def build(self):
        self._build(resume=False)

    def resume(self):
        self._build(resume=True)

    def _build(self, resume=False):
        # Initializing parameters
        self._temp_dir = self._engine.get_tempdir('training', ensure=(not resume))
        self._init_checkpoint_manager(resume)

        source_lang = self._engine.source_lang
        target_lang = self._engine.target_lang

        bilingual_corpora, monolingual_corpora = BilingualCorpus.splitlist(source_lang, target_lang, roots=self._roots)
        if len(bilingual_corpora) == 0:
            raise IllegalArgumentException(
                'you project does not include %s-%s data.' % (source_lang.upper(), target_lang.upper()))

        if not os.path.isdir(self._engine.path) or not resume:
            shutil.rmtree(self._engine.path, ignore_errors=True)
            os.makedirs(self._engine.path)

        # Checking constraints
        self._check_constraints()

        # Start building
        logger = _builder_logger(len(self._steps) + 1, self._engine.get_logfile('training'))

        try:
            logger.start(self._engine, bilingual_corpora, monolingual_corpora)

            cleaned_bicorpora = self._run_step('tm_cleanup', self._step_tm_cleanup, logger=logger,
                                               values=[bilingual_corpora])
            base_bicorpora, base_monocorpora = self._run_step('__db_map', self._step_init, forced=True,
                                                              values=[cleaned_bicorpora, monolingual_corpora])
            processed_bicorpora, processed_monocorpora, cleaned_bicorpora =\
                self._run_step('preprocess', self._step_preprocess, logger=logger,
                               values=[base_bicorpora, base_monocorpora, base_bicorpora])
            _ = self._run_step('context_analyzer', self._step_context_analyzer, logger=logger, values=[base_bicorpora])
            _ = self._run_step('aligner', self._step_aligner, logger=logger, values=[cleaned_bicorpora])
            _ = self._run_step('tm', self._step_tm, logger=logger, values=[cleaned_bicorpora])
            _ = self._run_step('lm', self._step_lm, logger=logger, values=[processed_bicorpora + processed_monocorpora])

            # Writing config file
            with logger.step('Writing config files') as _:
                self._engine.write_configs()

            logger.completed()

            if not self._debug:
                self._engine.clear_tempdir('training')
        except:
            logger.error()
            raise
        finally:
            logger.close()

    def _init_checkpoint_manager(self, resume=False):
        checkpoint_file = os.path.join(self._temp_dir, 'checkpoint.json')

        if resume:
            self._checkpoint_manager = _CheckpointManager.load_from_file(checkpoint_file)
        else:
            self._checkpoint_manager = _CheckpointManager.create_for_engine(checkpoint_file)

    def _check_constraints(self):
        # Check disk space constraints
        free_space_on_disk = fileutils.df(self._engine.path)[2]
        corpus_size_on_disk = 0
        for root in self._roots:
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

    def _run_step(self, step, func, values, logger=None, forced=False):
        step_description = self._engine.training_steps[step] if step in self._engine.training_steps else None

        if not forced and step not in self._engine.training_steps:
            return values

        skip = not self._checkpoint_manager.to_be_run(step)

        if logger is None:
            result = func(*values, skip=skip, logger=logger)
        else:
            with logger.step(step_description) as _:
                result = func(*values, skip=skip, logger=logger)

        if not skip:
            self._checkpoint_manager.completed(step).save()

        return result

    # Training steps

    def _step_init(self, bilingual_corpora, monolingual_corpora, skip=False, logger=None):
        training_folder = self._get_tempdir('training_corpora')

        if skip:
            return BilingualCorpus.splitlist(self._engine.source_lang, self._engine.target_lang, roots=training_folder)
        else:
            return self._engine.db.generate(bilingual_corpora, monolingual_corpora, training_folder)

    def _step_tm_cleanup(self, corpora, skip=False, logger=None):
        folder = self._get_tempdir('clean_tms')

        if skip:
            return BilingualCorpus.list(folder)
        else:
            return self._engine.cleaner.clean(corpora, folder, log=logger.stream)

    def _step_preprocess(self, bilingual_corpora, monolingual_corpora, _, skip=False, logger=None):
        preprocessed_folder = self._get_tempdir('preprocessed')
        cleaned_folder = self._get_tempdir('clean_corpora')

        if skip:
            processed_bicorpora, processed_monocorpora = BilingualCorpus.splitlist(self._engine.source_lang,
                                                                                   self._engine.target_lang,
                                                                                   roots=preprocessed_folder)
            cleaned_bicorpora = BilingualCorpus.list(cleaned_folder)
        else:
            processed_bicorpora, processed_monocorpora = self._engine.training_preprocessor.process(
                bilingual_corpora + monolingual_corpora, preprocessed_folder,
                (self._engine.data_path if self._split_trainingset else None), log=logger.stream)
            cleaned_bicorpora = self._engine.training_preprocessor.clean(processed_bicorpora, cleaned_folder)

        return processed_bicorpora, processed_monocorpora, cleaned_bicorpora

    def _step_context_analyzer(self, corpora, skip=False, logger=None):
        if not skip:
            self._engine.analyzer.create_index(corpora, log=logger.stream)

    def _step_aligner(self, corpora, skip=False, logger=None):
        if not skip:
            working_dir = self._get_tempdir('aligner', delete_if_exists=True)
            self._engine.aligner.build(corpora, working_dir, log=logger.stream)

    def _step_tm(self, corpora, skip=False, logger=None):
        if not skip:
            working_dir = self._get_tempdir('tm', delete_if_exists=True)
            self._engine.pt.train(corpora, self._engine.aligner, working_dir, log=logger.stream)

    def _step_lm(self, corpora, skip=False, logger=None):
        if not skip:
            working_dir = self._get_tempdir('lm', delete_if_exists=True)
            self._engine.lm.train(corpora, self._engine.target_lang, working_dir, log=logger.stream)


# This private class belongs to the MMTEngineBuilder
# and handles the management of checkpoints that make it possible
# to resume an engine creation if it was unexpectedly interrupted
class _CheckpointManager:

    # this method is used to create a checkpointManager when resume is TRUE,
    # so the checkpointManager is initialized with the path of the checkpoints file to read.
    # Both options and passed checkpoints are read from such file
    @staticmethod
    def load_from_file(file_path):
        try:
            with open(file_path) as json_file:
                passed_steps = json.load(json_file)
        except IOError:
            raise cli.IllegalStateException("Engine creation can not be resumed. "
                                            "Checkpoint file " + file_path + " not found")

        return _CheckpointManager(file_path, passed_steps)

    # this method is used to create a checkpointManager when resume is FALSE,
    # so the checkpointManager is initialized
    # with the path of the checkpoints file to create and fill from scratch
    @staticmethod
    def create_for_engine(file_path):
        manager = _CheckpointManager(file_path, [])
        manager.save()
        return manager

    def __init__(self, file_path, passed_steps):
        self._file_path = file_path
        self._passed_steps = passed_steps

    # This method stores a new checkpoint in the checkpoints file
    def save(self):
        with open(self._file_path, 'w') as json_file:
            json.dump(self._passed_steps, json_file)

    # This method stores a new checkpoint in the checkpoints list
    def completed(self, step):
        self._passed_steps.append(step)
        return self

    def to_be_run(self, step):
        return step not in self._passed_steps


class EngineConfig(object):
    @staticmethod
    # read engine.xconf file and import its configuration
    def from_file(name, file):
        # get "node" element and its children elements "engine", "kafka", "database"
        node_el = minidom.parse(file).documentElement
        engine_el = EngineConfig._get_element_if_exists(node_el, 'engine')
        datastream_el = EngineConfig._get_element_if_exists(node_el, 'datastream')
        db_el = EngineConfig._get_element_if_exists(node_el, 'db')
        # get attributes from the various elements
        source_lang = EngineConfig._get_attribute_if_exists(engine_el, 'source-language')
        target_lang = EngineConfig._get_attribute_if_exists(engine_el, 'target-language')
        datastream_enabled = EngineConfig._get_attribute_if_exists(datastream_el, 'enabled')
        db_enabled = EngineConfig._get_attribute_if_exists(db_el, 'enabled')

        # Parsing boolean from string:
        # if None or "true" (or uppercase variations) then the value is True; else it is False
        datastream_enabled = datastream_enabled.lower() == 'true' if datastream_enabled else True
        db_enabled = db_enabled.lower() == 'true' if db_enabled else True

        # all configuration elements are put into an EngineConfig object
        config = EngineConfig(name, source_lang, target_lang, datastream_enabled, db_enabled)

        network = node_el.getElementsByTagName('network')
        network = network[0] if len(network) > 0 else None

        if network is None:
            return config

        api = network.getElementsByTagName('api')
        api = api[0] if len(api) > 0 else None

        if api is None:
            return config

        if api.hasAttribute('root'):
            config.apiRoot = api.getAttribute('root')

        return config

    @staticmethod
    def _get_element_if_exists(parent, child_name):
        children = parent.getElementsByTagName(child_name)
        if len(children) is 0:
            return None
        else:
            return children[0]

    @staticmethod
    def _get_attribute_if_exists(node, attribute_name):
        if node is None:
            return None
        else:
            return node.getAttribute(attribute_name)

    def __init__(self, name, source_lang, target_lang, datastream_enabled=True, db_enabled=True):
        self.name = name
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.datastream_enabled = datastream_enabled
        self.db_enabled = db_enabled
        self.apiRoot = None

    def store(self, file):
        xml_template = '''<node xsi:schemaLocation="http://www.modernmt.eu/schema/config mmt-config-1.0.xsd"
      xmlns="http://www.modernmt.eu/schema/config"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <engine source-language="%s" target-language="%s" />
</node>'''

        with open(file, 'wb') as out:
            out.write(xml_template % (self.source_lang, self.target_lang))


class MMTEngine(object):
    training_steps = {
        'tm_cleanup': 'TMs clean-up',
        'preprocess': 'Corpora pre-processing',
        'context_analyzer': 'Context Analyzer training',
        'aligner': 'Aligner training',
        'tm': 'Translation Model training',
        'lm': 'Language Model training'
    }

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
        config_path = MMTEngine._get_config_path(name)
        if not os.path.isfile(config_path):
            raise IllegalArgumentException("Engine '%s' not found" % name)

        config = EngineConfig.from_file(name, MMTEngine._get_config_path(name))
        return MMTEngine(config.name, config.source_lang, config.target_lang, config)

    def __init__(self, name, source_lang, target_lang, config=None):
        self.name = name if name is not None else 'default'
        self.source_lang = source_lang
        self.target_lang = target_lang

        self._config_file = self._get_config_path(self.name)
        self.config = EngineConfig(self.name, source_lang, target_lang) if config is None else config

        self.path = self._get_path(self.name)
        self.data_path = os.path.join(self.path, 'data')
        self.models_path = os.path.join(self.path, 'models')

        self.runtime_path = os.path.join(cli.RUNTIME_DIR, self.name)
        self._logs_path = os.path.join(self.runtime_path, 'logs')
        self._temp_path = os.path.join(self.runtime_path, 'tmp')
        self._temp_path = os.path.join(self.runtime_path, 'tmp')

        self._vocabulary_model = os.path.join(self.models_path, 'vocabulary')
        self._aligner_model = os.path.join(self.models_path, 'align')
        self._context_index = os.path.join(self.models_path, 'context')
        self._moses_path = os.path.join(self.models_path, 'decoder')
        self._lm_model = os.path.join(self._moses_path, 'lm')
        self._pt_model = os.path.join(self._moses_path, 'sapt')
        self._db_model = os.path.join(self.models_path, 'db')

        self.analyzer = ContextAnalyzer(self._context_index, self.source_lang, self.target_lang)
        self.cleaner = TMCleaner(self.source_lang, self.target_lang)
        self.pt = SuffixArraysPhraseTable(self._pt_model, (self.source_lang, self.target_lang))
        self.pt.set_reordering_model('DM0')
        self.aligner = FastAlign(self._aligner_model, self.source_lang, self.target_lang)
        self.lm = InterpolatedLM(self._lm_model)
        self.training_preprocessor = TrainingPreprocessor(self.source_lang, self.target_lang, self._vocabulary_model)
        self.db = BaselineDatabase(self._db_model)
        self.moses = Moses(self._moses_path)
        self.moses.add_feature(MosesFeature('UnknownWordPenalty'))
        self.moses.add_feature(MosesFeature('WordPenalty'))
        self.moses.add_feature(MosesFeature('Distortion'))
        self.moses.add_feature(MosesFeature('PhrasePenalty'))
        self.moses.add_feature(self.pt, 'Sapt')
        self.moses.add_feature(LexicalReordering(), 'DM0')
        self.moses.add_feature(self.lm, 'InterpolatedLM')

    def builder(self, roots, debug=False, steps=None, split_trainingset=True):
        return _MMTEngineBuilder(self, roots, debug, steps, split_trainingset)

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
        self.config.store(self._config_file)

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

    def get_tempfile(self, name, ensure=True):
        if ensure and not os.path.isdir(self._temp_path):
            fileutils.makedirs(self._temp_path, exist_ok=True)
        return os.path.join(self._temp_path, name)

    def clear_tempdir(self, subdir=None):
        path = os.path.join(self._temp_path, subdir) if subdir is not None else self._temp_path
        shutil.rmtree(path, ignore_errors=True)
