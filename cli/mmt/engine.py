import inspect
import json
import os
import shutil
from xml.dom import minidom

import logging

import time

import cli
from cli import IllegalArgumentException, CorpusNotFoundInFolderException, mmt_javamain
from cli.libs import fileutils, shell
from cli.mmt import BilingualCorpus
from cli.mmt.processing import TrainingPreprocessor, TMCleaner

__author__ = 'Davide Caroselli'


class ContextAnalyzer:
    def __init__(self, index, source_lang, target_lang):
        self._index = index
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._java_mainclass = 'eu.modernmt.cli.ContextAnalyzerMain'

    def create_index(self, corpora, log=None):
        if log is None:
            log = shell.DEVNULL

        source_paths = set()

        for corpus in corpora:
            source_paths.add(corpus.get_folder())

        shutil.rmtree(self._index, ignore_errors=True)
        fileutils.makedirs(self._index, exist_ok=True)

        args = ['-s', self._source_lang, '-t', self._target_lang, '-i', self._index, '-c']
        for source_path in source_paths:
            args.append(source_path)

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdout=log, stderr=log)


class FastAlign:
    def __init__(self, model, source_lang, target_lang):
        # FastAlign only supports base languages, without regions
        self._model = os.path.join(model, '%s__%s.mdl' % (source_lang.split('-')[0], target_lang.split('-')[0]))
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._build_bin = os.path.join(cli.BIN_DIR, 'fa_build')
        self._align_bin = os.path.join(cli.BIN_DIR, 'fa_align')
        self._export_bin = os.path.join(cli.BIN_DIR, 'fa_export')

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

    def export(self, path, log=None):
        if log is None:
            log = shell.DEVNULL

        command = [self._export_bin, '--model', self._model, '--output', path]
        shell.execute(command, stderr=log, stdout=log)


class JsonDatabase:
    class Memory:
        def __init__(self, _id, name, corpus=None):
            self.id = _id
            self.name = name
            self.corpus = corpus

        def to_json(self):
            return {'id': self.id, 'name': self.name}

    def __init__(self, path):
        self._path = path
        self._json_file = os.path.join(path, 'baseline_memories.json')
        self.memories = []

    def insert(self, bilingual_corpora):
        for i in xrange(len(bilingual_corpora)):
            corpus = bilingual_corpora[i]
            self.memories.append(
                JsonDatabase.Memory((i + 1), name=corpus.name, corpus=corpus)
            )

        # create the necessary folders if they don't already exist
        if not os.path.isdir(self._path):
            os.makedirs(self._path)

        # create the json file and stores memories inside it
        with open(self._json_file, 'w') as out:
            json.dump([memory.to_json() for memory in self.memories], out)

        return self.memories


class Engine(object):
    @staticmethod
    def _get_config_path(name):
        return os.path.join(cli.ENGINES_DIR, name, 'engine.xconf')

    @staticmethod
    def list():
        return sorted([name for name in os.listdir(cli.ENGINES_DIR)
                       if os.path.isfile(Engine._get_config_path(name))])

    # This method loads an already created engine using its name.
    # The method figures the configuration file path from the engine name,
    # parses the source language and target language from the configuration file
    # and creates and return a new engine with that name, source target and language target
    @staticmethod
    def load(name):
        # figure the configuration file path from the engine name
        config_path = Engine._get_config_path(name)

        if not os.path.isfile(config_path):
            raise IllegalArgumentException("Engine '%s' not found" % name)

        # parse the source language and target language from the configuration file
        engine_el = minidom.parse(config_path).documentElement.getElementsByTagName("engine")[0]
        engine_type = engine_el.getAttribute('type')
        source_lang = engine_el.getAttribute('source-language')
        target_lang = engine_el.getAttribute('target-language')

        # create and return a new engine with that name, source target and language target

        if engine_type == 'neural':
            from cli.mmt.neural import NeuralEngine
            return NeuralEngine(name, source_lang, target_lang, bpe_symbols=None)
        else:
            from cli.mmt.phrasebased import PhraseBasedEngine
            return PhraseBasedEngine(name, source_lang, target_lang)

    def __init__(self, name, source_lang, target_lang):
        # properties
        self.name = name if name is not None else 'default'
        self.source_lang = source_lang
        self.target_lang = target_lang

        # base paths
        self.config_path = self._get_config_path(self.name)
        self.path = os.path.join(cli.ENGINES_DIR, name)
        self.data_path = os.path.join(self.path, 'data')
        self.models_path = os.path.join(self.path, 'models')
        self.runtime_path = os.path.join(cli.RUNTIME_DIR, self.name)
        self.logs_path = os.path.join(self.runtime_path, 'logs')
        self.temp_path = os.path.join(self.runtime_path, 'tmp')
        self.vocabulary_path = None  # Sub-classes must override this if needed

        # common models
        self.cleaner = TMCleaner(self.source_lang, self.target_lang)
        self.training_preprocessor = TrainingPreprocessor(self.source_lang, self.target_lang)
        self.aligner = FastAlign(os.path.join(self.models_path, 'aligner'), self.source_lang, self.target_lang)
        self.db = JsonDatabase(os.path.join(self.models_path, 'db'))
        self.analyzer = ContextAnalyzer(os.path.join(self.models_path, 'context'), self.source_lang,
                                        self.target_lang)

    def exists(self):
        return os.path.isfile(self.config_path)

    def get_logfile(self, name, ensure=True, append=False):
        if ensure and not os.path.isdir(self.logs_path):
            fileutils.makedirs(self.logs_path, exist_ok=True)

        logfile = os.path.join(self.logs_path, name + '.log')

        if not append and ensure and os.path.isfile(logfile):
            os.remove(logfile)

        return logfile

    def get_tempdir(self, name, ensure=True):
        if ensure and not os.path.isdir(self.temp_path):
            fileutils.makedirs(self.temp_path, exist_ok=True)

        folder = os.path.join(self.temp_path, name)

        if ensure:
            shutil.rmtree(folder, ignore_errors=True)
            os.makedirs(folder)

        return folder

    def get_tempfile(self, name, ensure=True):
        if ensure and not os.path.isdir(self.temp_path):
            fileutils.makedirs(self.temp_path, exist_ok=True)
        return os.path.join(self.temp_path, name)

    def clear_tempdir(self, subdir=None):
        path = os.path.join(self.temp_path, subdir) if subdir is not None else self.temp_path
        shutil.rmtree(path, ignore_errors=True)

    def type(self):
        raise NotImplementedError('abstract method')


class EngineBuilder:
    _MB = (1024 * 1024)
    _GB = (1024 * 1024 * 1024)

    class Listener:
        def __init__(self):
            pass

        def on_hw_constraint_violated(self, message):
            pass

        def on_training_begin(self, steps, engine, bilingual_corpora, monolingual_corpora):
            pass

        def on_step_begin(self, step, name):
            pass

        def on_step_end(self, step, name):
            pass

        def on_training_end(self, engine):
            pass

    class Step:
        def __init__(self, name, optional=True, hidden=False):
            self._name = name
            self._optional = optional
            self._hidden = hidden

        def __call__(self, *_args, **_kwargs):
            class _:
                def __init__(self, f, name, optional, hidden):
                    self.id = f.__name__.strip('_')
                    self.name = name

                    self._optional = optional
                    self._hidden = hidden
                    self._f = f

                def is_optional(self):
                    return self._optional

                def is_hidden(self):
                    return self._hidden

                def __call__(self, *args, **kwargs):
                    names, _, _, _ = inspect.getargspec(self._f)

                    if 'delete_on_exit' not in names:
                        del kwargs['delete_on_exit']
                    if 'log' not in names:
                        del kwargs['log']
                    if 'skip' not in names:
                        del kwargs['skip']

                    self._f(*args, **kwargs)

            return _(_args[0], self._name, self._optional, self._hidden)

    class __Args(object):
        def __init__(self):
            pass

        def __getattr__(self, item):
            return self.__dict__[item] if item in self.__dict__ else None

        def __setattr__(self, key, value):
            self.__dict__[key] = value

    class __Schedule:
        def __init__(self, plan, filtered_steps=None):
            self._plan = plan
            self._passed_steps = []

            all_steps = self.all_steps()

            if filtered_steps is not None:
                self._scheduled_steps = filtered_steps

                unknown_steps = [step for step in self._scheduled_steps if step not in all_steps]
                if len(unknown_steps) > 0:
                    raise IllegalArgumentException('Unknown training steps: ' + str(unknown_steps))
            else:
                self._scheduled_steps = all_steps

        def __len__(self):
            return len(self._scheduled_steps)

        def __iter__(self):
            class _:
                def __init__(self, plan):
                    self._plan = plan
                    self._idx = 0

                def next(self):
                    if self._idx < len(self._plan):
                        self._idx += 1
                        return self._plan[self._idx - 1]
                    else:
                        raise StopIteration

            return _([el for el in self._plan if el.id in self._scheduled_steps or not el.is_optional()])

        def visible_steps(self):
            return [x.id for x in self._plan if x.id in self._scheduled_steps and not x.is_hidden()]

        def all_steps(self):
            return [e.id for e in self._plan]

        def store(self, path):
            with open(path, 'w') as json_file:
                json.dump(self._passed_steps, json_file)

        def load(self, path):
            try:
                with open(path) as json_file:
                    self._passed_steps = json.load(json_file)
            except IOError:
                self._passed_steps = []

        def step_completed(self, step):
            self._passed_steps.append(step)

        def is_completed(self, step):
            return step in self._passed_steps

    # an EngineBuilder is initialized with several command line arguments
    # of the mmt create method:
    # - the engine
    # - the path where to find the corpora to employ in training (roots)
    # - the debug boolean flag (debug)
    # - the steps to perform (steps) (if None, perform all)
    # - the set splitting boolean flag (split_trainingset): if it is set to false,
    #   MMT will not extract dev and test sets out of the provided training corpora
    def __init__(self, engine, roots, debug=False, steps=None, split_trainingset=True, max_training_words=None):
        self._engine = engine
        self._roots = roots
        self._delete_on_exit = not debug
        self._split_trainingset = split_trainingset
        self._max_training_words = max_training_words

        self._temp_dir = None

        self._schedule = EngineBuilder.__Schedule(self._build_schedule(), steps)

    def _build_schedule(self):
        return [self._clean_tms] + \
               ([self._reduce_train] if self._max_training_words > 0 else []) + \
               [self._create_db, self._preprocess, self._train_context, self._train_aligner, self._write_config]

    def _get_tempdir(self, name, delete_if_exists=False):
        path = os.path.join(self._temp_dir, name)
        if delete_if_exists:
            shutil.rmtree(path, ignore_errors=True)
        if not os.path.isdir(path):
            fileutils.makedirs(path, exist_ok=True)
        return path

    # ~~~~~~~~~~~~~~~~~~~~~~~~~~ Engine creation management ~~~~~~~~~~~~~~~~~~~~~~~~~~

    # This method launches the initialization of a new engine from scratch
    # Used when "resume" is False.
    def build(self, listener=None):
        self._build(resume=False, listener=listener)

    # Launch re-activation of a previous not successful engine training process.
    # Used when "resume" is True.
    def resume(self, listener=None):
        self._build(resume=True, listener=listener)

    # Depending on the resume value, either train a new engine from scratch
    # or resume a not successfully finished training process
    def _build(self, resume, listener):
        self._temp_dir = self._engine.get_tempdir('training', ensure=(not resume))

        checkpoint_path = os.path.join(self._temp_dir, 'checkpoint.json')
        if resume:
            self._schedule.load(checkpoint_path)
        else:
            self._schedule.store(checkpoint_path)

        source_lang = self._engine.source_lang
        target_lang = self._engine.target_lang

        # separate bilingual and monolingual corpora in separate lists, reading them from roots
        bilingual_corpora, monolingual_corpora = BilingualCorpus.splitlist(source_lang, target_lang,
                                                                           roots=self._roots)

        # if no bilingual corpora are found, it is not possible to train the translation system
        if len(bilingual_corpora) == 0:
            raise CorpusNotFoundInFolderException(
                'Could not find %s-%s corpora in path %s' %
                (source_lang.upper(), target_lang.upper(), ', '.join(self._roots)))

        # if no old engines (i.e. engine folders) can be found, create a new one from scratch
        # if we are not trying to resume an old one, create from scratch anyway
        if not os.path.isdir(self._engine.path) or not resume:
            shutil.rmtree(self._engine.path, ignore_errors=True)
            os.makedirs(self._engine.path)

        # Create a new logger for the building activities,
        log_file = self._engine.get_logfile('training', append=resume)
        log_stream = open(log_file, 'ab' if resume else 'wb')
        logging.basicConfig(format='%(asctime)-15s [%(levelname)s] - %(message)s',
                            level=logging.DEBUG, stream=log_stream)
        logger = logging.getLogger('EngineBuilder')

        # Start the engine building (training) phases
        try:
            logger.log(logging.INFO, 'Training started: engine=%s, bilingual=%d, monolingual=%d, langpair=%s-%s' %
                       (self._engine.name, len(bilingual_corpora), len(monolingual_corpora),
                        self._engine.source_lang, self._engine.target_lang))

            if listener:
                listener.on_training_begin(self._schedule.visible_steps(), self._engine, bilingual_corpora,
                                           monolingual_corpora)

            # Check if all requirements are fulfilled before actual engine training
            try:
                self._check_constraints()
            except EngineBuilder.HWConstraintViolated as e:
                if listener:
                    listener.on_hw_constraint_violated(e.cause)

            args = EngineBuilder.__Args()
            args.bilingual_corpora = bilingual_corpora
            args.monolingual_corpora = monolingual_corpora

            # ~~~~~~~~~~~~~~~~~~~~~ RUN ALL STEPS ~~~~~~~~~~~~~~~~~~~~~
            # Note: if resume is true, a step is only run if it was not in the previous attempt

            step_index = 1

            for method in self._schedule:
                skip = self._schedule.is_completed(method.id)

                if listener and not method.is_hidden():
                    listener.on_step_begin(method.id, method.name)

                logger.log(logging.INFO, 'Training step "%s" (%d/%d) started' %
                           (method.id, step_index, len(self._schedule)))

                start_time = time.time()
                method(self, args, skip=skip, log=log_stream, delete_on_exit=self._delete_on_exit)
                elapsed_time = time.time() - start_time

                if listener and not method.is_hidden():
                    listener.on_step_end(method.id, method.name)

                logger.log(logging.INFO, 'Training step "%s" completed in %d s' %
                           (method.id, int(elapsed_time)))

                self._schedule.step_completed(method.id)
                self._schedule.store(checkpoint_path)

                step_index += 1

            if listener:
                listener.on_training_end(self._engine)

            if self._delete_on_exit:
                self._engine.clear_tempdir('training')
        except:
            logger.exception('Unexpected exception')
            raise
        finally:
            log_stream.close()

    class HWConstraintViolated(Exception):
        def __init__(self, cause):
            self.cause = cause

    # This method checks if memory and disk space requirements
    # for engine initialization are fulfilled.
    # It is therefore called just before starting the engine training.
    def _check_constraints(self):
        raise NotImplementedError('abstract method')

    # ~~~~~~~~~~~~~~~~~~~~~ Training step functions ~~~~~~~~~~~~~~~~~~~~~

    @Step('Corpora cleaning')
    def _clean_tms(self, args, skip=False, log=None):
        folder = self._get_tempdir('clean_corpora')

        if skip:
            args.bilingual_corpora = BilingualCorpus.list(folder)
        else:
            args.bilingual_corpora = self._engine.cleaner.clean(args.bilingual_corpora, folder, log=log)

    @Step('Reducing training corpora')
    def _reduce_train(self, args, skip=False, log=None):
        folder = self._get_tempdir('reduced_corpora')

        if skip:
            args.bilingual_corpora = BilingualCorpus.list(folder)
        else:
            args.bilingual_corpora = self._engine.training_preprocessor.reduce(
                args.bilingual_corpora, folder, self._max_training_words, log=log)

    @Step('Database create', optional=False, hidden=True)
    def _create_db(self, args, skip=False):
        training_folder = self._get_tempdir('training_corpora')

        if skip:
            b, m = BilingualCorpus.splitlist(self._engine.source_lang, self._engine.target_lang, roots=training_folder)
        else:
            memories = self._engine.db.insert(args.bilingual_corpora)

            b = [memory.corpus.symlink(training_folder, name=str(memory.id)) for memory in memories]
            m = [corpus.symlink(training_folder) for corpus in args.monolingual_corpora]

        args.bilingual_corpora = b
        args.monolingual_corpora = m

    @Step('Corpora pre-processing')
    def _preprocess(self, args, skip=False, log=None):
        preprocessed_folder = self._get_tempdir('preprocessed_corpora')

        if skip:
            processed_bicorpora, processed_monocorpora = BilingualCorpus.splitlist(self._engine.source_lang,
                                                                                   self._engine.target_lang,
                                                                                   roots=preprocessed_folder)
        else:
            corpora = args.bilingual_corpora + args.monolingual_corpora
            if not corpora:
                raise CorpusNotFoundInFolderException("Could not find any valid %s -> %s segments in your input." %
                                                      (self._engine.source_lang, self._engine.target_lang))

            processed_bicorpora, processed_monocorpora = self._engine.training_preprocessor.process(
                corpora,
                preprocessed_folder,
                data_path=(self._engine.data_path if self._split_trainingset else None),
                vb_path=self._engine.vocabulary_path,
                log=log)

        args.processed_bilingual_corpora = processed_bicorpora
        args.processed_monolingual_corpora = processed_monocorpora

    @Step('Context Analyzer training')
    def _train_context(self, args, skip=False, log=None):
        if not skip:
            self._engine.analyzer.create_index(args.bilingual_corpora, log=log)

    @Step('Aligner training')
    def _train_aligner(self, args, skip=False, log=None, delete_on_exit=False):
        if not skip:
            corpora = filter(None, [args.processed_bilingual_corpora, args.bilingual_corpora])[0]

            working_dir = self._get_tempdir('aligner', delete_if_exists=True)
            self._engine.aligner.build(corpora, working_dir, log=log)

            if delete_on_exit:
                shutil.rmtree(working_dir, ignore_errors=True)

    @Step('Writing config', optional=False, hidden=True)
    def _write_config(self, _):
        xml_template = \
            '<node xsi:schemaLocation="http://www.modernmt.eu/schema/config mmt-config-1.0.xsd"\n' \
            '      xmlns="http://www.modernmt.eu/schema/config"\n' \
            '      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n' \
            '   <engine source-language="%s" target-language="%s" type="%s" />\n' \
            '</node>'

        with open(self._engine.config_path, 'wb') as out:
            out.write(xml_template % (self._engine.source_lang, self._engine.target_lang, self._engine.type()))
