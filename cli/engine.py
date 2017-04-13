import json
import logging
import os
import shutil
import time
from xml.dom import minidom

import cli
from cli import IllegalArgumentException
from cli.libs import fileutils
from cli.mt import BilingualCorpus
from cli.mt.phrasebased import ContextAnalyzer, Moses, FastAlign
from cli.mt.processing import TrainingPreprocessor, TMCleaner

__author__ = 'Davide Caroselli'


class _JsonDatabase:
    class Domain:
        def __init__(self, _id, name, corpus=None):
            self.id = _id
            self.name = name
            self.corpus = corpus

        def to_json(self):
            return {'id': self.id, 'name': self.name}

    def __init__(self, path):
        self._path = path
        self._json_file = os.path.join(path, 'baseline_domains.json')
        self.domains = []

    def insert(self, bilingual_corpora):
        for i in xrange(len(bilingual_corpora)):
            corpus = bilingual_corpora[i]
            self.domains.append(
                _JsonDatabase.Domain((i + 1), name=corpus.name, corpus=corpus)
            )

        # create the necessary folders if they don't already exist
        if not os.path.isdir(self._path):
            os.makedirs(self._path)

        # create the json file and stores the domains inside it
        with open(self._json_file, 'w') as out:
            json.dump([domain.to_json() for domain in self.domains], out)

        return self.domains


################################################################################################
# This private class manages the logging activities of the engine builder
class _builder_logger:
    # a new logger for the engine builder is initialized with
    # - the amount of steps to log data on
    # - the log filepath,
    # - the max length of a line
    # Initialization also creates the log_file
    # (after destroying the already existing one, if necessary)
    # and opens with the file a communication stream that won't be closed
    # until the end of the training process.
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

    # this method is typically called when the MMTEngineBuilder starts it build method,
    # so it starts the training process of an engine.
    # It mainly writes the training start data on the logfile (an on stdout)
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

    # this method just updates the logger internal information on the current step.
    # Writing information on a completed step is actually handled by __enter__ method.
    def step(self, step):
        self._current_step_name = step
        self._current_step_num += 1
        return self

    # this method is typically called when the MMTEngineBuilder,
    # in its build method, completes the whole training process.
    def completed(self):
        self._logger.log(logging.INFO,
                         'Training completed in %s' % self._pretty_print_time(time.time() - self._start_time))

        print '\n=========== TRAINING SUCCESS ===========\n'
        print 'You can now start, stop or check the status of the server with command:'
        print '\t./mmt start|stop|status ' + ('' if self._engine_name is None else '-e %s' % self._engine_name)
        print

    # this method logs unexpected errors or problems
    # in any phase of the engine building process.
    def error(self):
        self._logger.exception('Unexpected exception')

    # this method closes the stream of the logger with the logfile.
    # It is mainly used when the builder has completed its build activity,
    # or in case of unexpected problems.
    def close(self):
        self.stream.close()

    # this method is employed to write on the logfile
    # that a new training step has been started the building process.
    def __enter__(self):
        self._logger.log(logging.INFO, 'Training step "%s" (%d/%d) started' %
                         (self._current_step_name, self._current_step_num, self._steps_count))

        message = 'INFO: (%d of %d) %s... ' % (self._current_step_num, self._steps_count, self._current_step_name)
        print message.ljust(self._line_len),

        self._step_start_time = time.time()
        return self

    # this method is employed to write on the logfile
    # that a new training step has been completed the building process.
    def __exit__(self, *_):
        elapsed_time = time.time() - self._step_start_time
        self._logger.log(logging.INFO, 'Training step "%s" completed in %s' %
                         (self._current_step_name, self._pretty_print_time(elapsed_time)))
        print 'DONE (in %s)' % self._pretty_print_time(elapsed_time)

    # this private method figures the best way to write time amounts on the log
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


################################################################################################

# An MMTEngineBuilder manages the initialization of a new engine.
# The builder is created when mmt.py receives a create request from command line.
# At that point an Engine has **already** been instantiated
# with some of the command line arguments (name, source language, target language).
# The builder, on the other hand, handles the whole engine training process.
class MMTEngineBuilder:
    __MB = (1024 * 1024)
    __GB = (1024 * 1024 * 1024)

    # the default training steps to perform during engine build
    DEFAULT_TRAINING_STEPS = {
        'tm_cleanup': 'TMs clean-up',
        'preprocess': 'Corpora pre-processing',
        'context_analyzer': 'Context Analyzer training',
        'aligner': 'Aligner training',
        'tm': 'Translation Model training',
        'lm': 'Language Model training'
    }

    # an MMTEngineBuilder is initialized with several command line arguments
    # of the mmt create method:
    # - the engine name (engine)
    # - the path where to find the corpora to employ in training (roots)
    # - the debug boolean flag (debug)
    # - the steps to perform (steps) (if None, perform all)
    # - the set splitting boolean flag (split_trainingset): if it is set to false,
    #   MMT will not extract dev and test sets out of the provided training corpora
    def __init__(self, name, source_lang, target_lang,
                 roots, debug=False, steps=None, split_trainingset=True):

        # the builder already prepares a basic, "empty" engine
        self._engine = MMTEngine(name, source_lang, target_lang)
        self._roots = roots
        self._debug = debug
        self._split_trainingset = split_trainingset

        self._temp_dir = None
        self._checkpoint_path = None

        self._scheduled_steps = None
        self._passed_steps = None

        # if no steps are passed, all training steps must be performed.
        # else, only perform the passed training steps (if they are all legal)
        if steps is None:
            self._scheduled_steps = self.DEFAULT_TRAINING_STEPS
        else:
            unknown_steps = [step for step in steps if step not in self.DEFAULT_TRAINING_STEPS]
            if len(unknown_steps) > 0:
                raise IllegalArgumentException('Unknown training steps: ' + str(unknown_steps))
            self._scheduled_steps = steps
        print

    # use an injector (with args already loaded) to inject parameters in the engine
    def _on_fields_injected(self, injector):
        self._engine = injector.inject(self._engine)

    # Create a temp directory where to store temporary training data.
    # Delete old tempdir unless delete_if_exists is False
    # NOTE: in the end all temporary directories will be destroyed
    # unless debug is True or the training process is not successful
    def _get_tempdir(self, name, delete_if_exists=False):
        path = os.path.join(self._temp_dir, name)
        if delete_if_exists:
            shutil.rmtree(path, ignore_errors=True)
        if not os.path.isdir(path):
            fileutils.makedirs(path, exist_ok=True)
        return path

    # ~~~~~~~~~~~~~~~~~~~~~~~~~~ Checkpoint Management ~~~~~~~~~~~~~~~~~~~~~~~~~~~

    # Overwrite the checkpoint passed steps list with thw local list.
    # Mainly used after completing a step, in order to mark that that step was passed.
    def save_checkpoint(self):
        with open(self._checkpoint_path, 'w') as json_file:
            json.dump(self._passed_steps, json_file)

    # load the checkpoint file passed steps list in the local list.
    # Used at the beginning of a resume task.
    def load_checkpoint(self):
        try:
            with open(self._checkpoint_path) as json_file:
                self._passed_steps = json.load(json_file)
        except IOError:
            raise cli.IllegalStateException("Engine creation can not be resumed. "
                                            "Checkpoint file " + self._checkpoint_path + " not found")

    # Store a new passed step in the local list without updating the checkpoint file.
    # Generally followed by save() to update the checkpoint file too.
    def completed_step(self, step):
        self._passed_steps.append(step)
        return self

    # ~~~~~~~~~~~~~~~~~~~~~~~~~~ Engine creation management ~~~~~~~~~~~~~~~~~~~~~~~~~~

    # This method launches the initialization of a new engine from scratch
    # Used when "resume" is False.
    def build(self):
        self._build(resume=False)

    # Launch re-activation of a previous not successful engine training process.
    # Used when "resume" is True.
    def resume(self):
        self._build(resume=True)

    # Depending on the resume value, either train a new engine from scratch
    # or resume a not successfully finished training process
    def _build(self, resume=False):

        self._temp_dir = self._engine.get_tempdir('training', ensure=(not resume))
        self._checkpoint_path = os.path.join(self._temp_dir, 'checkpoint.json')
        self._passed_steps = []

        if resume:
            self.load_checkpoint()
        else:
            self.save_checkpoint()

        # initialize thee checkpoint manager
        source_lang = self._engine.source_lang
        target_lang = self._engine.target_lang

        # separate bilingual and monolingual corpora in separate lists, reading them from roots
        bilingual_corpora, monolingual_corpora = BilingualCorpus.splitlist(source_lang, target_lang, roots=self._roots)
        # if no bilingual corpora are found, it is not possible to train the translation system
        if len(bilingual_corpora) == 0:
            raise IllegalArgumentException(
                'you project does not include %s-%s data.' % (source_lang.upper(), target_lang.upper()))

        # if no old engines (i.e. engine folders) can be found, create a new one from scratch
        # if we are not trying to resume an old one, create from scratch anyway
        if not os.path.isdir(self._engine.path) or not resume:
            shutil.rmtree(self._engine.path, ignore_errors=True)
            os.makedirs(self._engine.path)

        # Check if all requirements are fulfilled before launching engine training
        self._check_constraints()

        # Create a new logger for the building activities,
        # passing it the amount of steps to perform (plus a non user-decidable step)
        # and the name of the log file to create
        logger = _builder_logger(len(self._scheduled_steps) + 1, self._engine.get_logfile('training'))
        delete_on_exit = not self._debug
        # Start the engine building (training) phases
        try:
            # tell the logger that the engine training has started
            logger.start(self._engine, bilingual_corpora, monolingual_corpora)

            # ~~~~~~~~~~~~~~~~~~~~~ RUN ALL STEPS ~~~~~~~~~~~~~~~~~~~~~
            # Note: if resume is true, a step is only run if it was not in the previous attempt

            # run tm_cleanup step on the bilingual_corpora if required.
            # Obtain cleaned bicorpora
            cleaned_bicorpora = self._run_step('tm_cleanup',
                                               self._step_tm_cleanup,
                                               logger=logger,
                                               values=[bilingual_corpora],
                                               delete_on_exit=delete_on_exit)

            # run __db_map step (always: user can't skip it)
            # on the cleaned bicorpora and the original monocorpora;
            # obtain base bicorpora and base monocorpora
            base_bicorpora, base_monocorpora = self._run_step('__db_map',
                                                              self._step_init,
                                                              forced=True,
                                                              values=[cleaned_bicorpora, monolingual_corpora],
                                                              delete_on_exit=delete_on_exit)

            # run preprocess step if required.
            # Return processed bi and mono corpora and cleaned bicorpora
            processed_bicorpora, processed_monocorpora, cleaned_bicorpora = \
                self._run_step('preprocess',
                               self._step_preprocess,
                               logger=logger,
                               values=[base_bicorpora, base_monocorpora, base_bicorpora],
                               delete_on_exit=delete_on_exit)

            # run context_analyzer step base_bicorpora if required.
            _ = self._run_step('context_analyzer',
                               self._step_context_analyzer,
                               logger=logger,
                               values=[base_bicorpora],
                               delete_on_exit=delete_on_exit)

            # run aligner step cleaned_bicorpora if required.
            _ = self._run_step('aligner',
                               self._step_aligner,
                               logger=logger,
                               values=[cleaned_bicorpora],
                               delete_on_exit=delete_on_exit)

            # run tm step cleaned_bicorpora if required.
            _ = self._run_step('tm',
                               self._step_tm,
                               logger=logger,
                               values=[cleaned_bicorpora],
                               delete_on_exit=delete_on_exit)

            # run lm step on the joint list of processed_bicorpora and processed_monocorpora
            _ = self._run_step('lm',
                               self._step_lm,
                               logger=logger,
                               values=[processed_bicorpora + processed_monocorpora],
                               delete_on_exit=delete_on_exit)

            # Writing config file
            with logger.step('Writing config files') as _:
                self._engine.write_configs()

            # tell the logger that the engine training has completed
            logger.completed()

            # if this is not debug mode, then the training temporary folder must be deleted
            if not self._debug:
                self._engine.clear_tempdir('training')
        except:
            logger.error()
            raise
        finally:
            logger.close()

    # This method checks if memory and disk space requirements
    # for engine initialization are fullfilled.
    # It is therefore called just before starting the engine training.
    def _check_constraints(self):
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

    # This method runs a training step, receiving as parameters:
    #   - the step name
    #   - the step function
    #   - the attributes to pass to the step function
    # Moreover, it is possible to ask not to log data about this step execution,
    # and it is possible to force the execution even if the user didn't ask for this step.
    # (these options are important when running a non user-decidable, "hidden" step)
    def _run_step(self, step_name, step_function, values, logger=None, forced=False, delete_on_exit=True):
        step_description = self.DEFAULT_TRAINING_STEPS[step_name] if step_name in self.DEFAULT_TRAINING_STEPS else None

        # if this step is not forced AND if it the user asked not to perform it,
        # return the passed parameters with no variations
        if not forced and step_name not in self._scheduled_steps:
            return values

        # else, if the step (even if it is scheduled) must not be run
        # because it has already been passed in a previous
        skip = step_name in self._passed_steps

        # Now we can launch the step function.
        # (Note: if it is launched with skip == true, the function will immediately return)

        # if logger is none, launch the step_function without logging any messages
        if logger is None:
            result = step_function(*values, skip=skip, logger=logger, delete_on_exit=delete_on_exit)
        # else, launch the step_function logging the step description
        else:
            with logger.step(step_description) as _:
                result = step_function(*values, skip=skip, logger=logger, delete_on_exit=delete_on_exit)

        # moreover if skip was false, mark the step as completed in the checkpoint manager!
        if not skip:
            self.completed_step(step_name).save_checkpoint()

        return result

    # ~~~~~~~~~~~~~~~~~~~~~ Training step functions ~~~~~~~~~~~~~~~~~~~~~

    # This step function performs cleaning of the translation models
    def _step_tm_cleanup(self, corpora, skip=False, logger=None, delete_on_exit=True):
        # the folder where tm_cleanup results are to be stored
        folder = self._get_tempdir('clean_tms')

        # if skip is true, then we are in resume mode, so use the already existing results
        if skip:
            clean_tms = BilingualCorpus.list(folder)
        # else perform the cleaning on the corpora and use the clean corpora
        else:
            clean_tms = self._engine.cleaner.clean(corpora, folder, log=logger.stream)

        return clean_tms

    # This step function performs the domain mapping and IT IS NOT USER-DECIDABLE
    def _step_init(self, bilingual_corpora, monolingual_corpora, skip=False, logger=None, delete_on_exit=False):
        training_folder = self._get_tempdir('training_corpora')

        # if skip is true, then we are in resume mode, so return the already existing results
        if skip:
            bilingual_corpora, monolingual_corpora = BilingualCorpus.splitlist(self._engine.source_lang, self._engine.target_lang, roots=training_folder)
        # else perform the baseline domains extraction and domain mapping, and return its result
        else:
            domains = self._engine.db.insert(bilingual_corpora)

            bilingual_corpora = [domain.corpus.symlink(training_folder, name=str(domain.id)) for domain in domains]
            monolingual_corpora = [corpus.symlink(training_folder) for corpus in monolingual_corpora]

        return bilingual_corpora, monolingual_corpora

    # This step function performs the preprocessing of the domain-mapped corpora
    def _step_preprocess(self, bilingual_corpora, monolingual_corpora, _, skip=False, logger=None, delete_on_exit=False):
        preprocessed_folder = self._get_tempdir('preprocessed')
        cleaned_folder = self._get_tempdir('clean_corpora')

        # if skip is true, then we are in resume mode, so return the already existing results
        if skip:
            processed_bicorpora, processed_monocorpora = BilingualCorpus.splitlist(self._engine.source_lang,
                                                                                   self._engine.target_lang,
                                                                                   roots=preprocessed_folder)
            cleaned_bicorpora = BilingualCorpus.list(cleaned_folder)
        else:
            processed_bicorpora, processed_monocorpora = self._engine.training_preprocessor.process(
                bilingual_corpora + monolingual_corpora,
                preprocessed_folder,
                (self._engine.data_path if self._split_trainingset else None),
                log=logger.stream)
            cleaned_bicorpora = self._engine.training_preprocessor.clean(
                processed_bicorpora, cleaned_folder)

        return processed_bicorpora, processed_monocorpora, cleaned_bicorpora

    # This step function performs the context analyzer training with the base corpora
    def _step_context_analyzer(self, corpora, skip=False, logger=None, delete_on_exit=False):
        # if skip is true, then there's nothing to do because the models exist already
        if not skip:
            self._engine.analyzer.create_index(corpora, log=logger.stream)

    # This step function performs the aligner training with the cleaned corpora
    def _step_aligner(self, corpora, skip=False, logger=None, delete_on_exit=False):
        # if skip is true, then there's nothing to do because the models exist already
        if not skip:
            working_dir = self._get_tempdir('aligner', delete_if_exists=True)
            self._engine.aligner.build(corpora, working_dir, log=logger.stream)

            if delete_on_exit:
                shutil.rmtree(working_dir, ignore_errors=True)

    # This step function performs the Translation Model training with the cleaned corpora
    def _step_tm(self, corpora, skip=False, logger=None, delete_on_exit=False):
        # if skip is true, then there's nothing to do because the models exist already
        if not skip:
            working_dir = self._get_tempdir('tm', delete_if_exists=True)
            self._engine.moses.pt.train(corpora, self._engine.aligner, working_dir, log=logger.stream)

            if delete_on_exit:
                shutil.rmtree(working_dir, ignore_errors=True)

    # This step function performs the Language Model training with the preprocessed corpora
    def _step_lm(self, corpora, skip=False, logger=None, delete_on_exit=False):
        # if skip is true, then there's nothing to do because the models exist already
        if not skip:
            working_dir = self._get_tempdir('lm', delete_if_exists=True)
            self._engine.moses.lm.train(corpora, self._engine.target_lang, working_dir, log=logger.stream)

            if delete_on_exit:
                shutil.rmtree(working_dir, ignore_errors=True)


############################################################################################


class MMTEngine(object):
    @staticmethod
    def _get_path(name):
        return os.path.join(cli.ENGINES_DIR, name)

    @staticmethod
    def _get_config_path(name):
        return os.path.join(cli.ENGINES_DIR, name, 'engine.xconf')

    @staticmethod
    def list():
        return sorted([name for name in os.listdir(cli.ENGINES_DIR)
                       if os.path.isfile(MMTEngine._get_config_path(name))])

    # This method loads an already created engine using its name.
    # The method figures the configuration file path from the engine name,
    # parses the source language and target language from the configuration file
    # and creates and return a new engine with that name, source target and language target
    @staticmethod
    def load(name):
        # figure the configuration file path from the engine name
        config_path = MMTEngine._get_config_path(name)

        if not os.path.isfile(config_path):
            raise IllegalArgumentException("Engine '%s' not found" % name)

        # parse the source language and target language from the configuration file
        engine_el = minidom.parse(config_path).documentElement.getElementsByTagName("engine")[0]
        source_lang = engine_el.getAttribute('source-language')
        target_lang = engine_el.getAttribute('target-language')

        # create and return a new engine with that name, source target and language target
        return MMTEngine(name, source_lang, target_lang)

    # This method instantiates a new Engine object
    # starting from its name, source language and target language.
    # It calculates and stores in instance viariables
    # all the relevant paths that an engine should store,
    # and creates reference to object that will be used during training,
    # such as ContextAnalyzer, TMCleaner etc.
    # It also creates a Moses object and writes its features
    def __init__(self, name, source_lang, target_lang):
        self.name = name if name is not None else 'default'
        self.source_lang = source_lang
        self.target_lang = target_lang
        self._config_file = self._get_config_path(self.name)
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
        self._db_model = os.path.join(self.models_path, 'db')

        self.analyzer = ContextAnalyzer(self._context_index, self.source_lang, self.target_lang)
        self.cleaner = TMCleaner(self.source_lang, self.target_lang)
        self.aligner = FastAlign(self._aligner_model, self.source_lang, self.target_lang)
        self.training_preprocessor = TrainingPreprocessor(self.source_lang, self.target_lang, self._vocabulary_model)
        self.db = _JsonDatabase(self._db_model)
        self.moses = Moses(self._moses_path, self.source_lang, self.target_lang)

    def builder(self, roots, debug=False, steps=None, split_trainingset=True):
        return MMTEngineBuilder(self, roots, debug, steps, split_trainingset)

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

        xml_template = '''<node xsi:schemaLocation="http://www.modernmt.eu/schema/config mmt-config-1.0.xsd"
      xmlns="http://www.modernmt.eu/schema/config"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <engine source-language="%s" target-language="%s" />
</node>'''
        with open(self._config_file, 'wb') as out:
            out.write(xml_template % (self.source_lang, self.target_lang))

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
