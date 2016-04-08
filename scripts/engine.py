import json as js
import logging
import multiprocessing
import os
import shutil
import signal
import subprocess
import sys
import tempfile
import time
from ConfigParser import ConfigParser

import requests

import scripts
from scripts import mmt_javamain
from scripts.evaluation import MMTTranslator, GoogleTranslate, BLEUScore, MatecatScore, TranslateError, \
    HumanEvaluationFileOutputter
from scripts.libs import fileutils, daemon, shell
from scripts.mt import ParallelCorpus
from scripts.mt.contextanalysis import ContextAnalyzer
from scripts.mt.lm import LanguageModel
from scripts.mt.moses import Moses, MosesFeature, LexicalReordering
from scripts.mt.phrasetable import WordAligner, SuffixArraysPhraseTable
from scripts.mt.processing import Preprocessor, TrainingPreprocessor, TMCleaner, XMLEncoder

__author__ = 'Davide Caroselli'

DEFAULT_MMT_API_PORT = 8045
DEFAULT_MMT_CLUSTER_PORTS = [5016, 5017]

logger = logging.getLogger()


# ==============================
# Base classes
# ==============================

class IllegalStateException(Exception):
    def __init__(self, error):
        self.message = error


class IllegalArgumentException(Exception):
    def __init__(self, error):
        self.message = error


class _MMTRuntimeComponent:
    def __init__(self, engine_name, component_name):
        self.__engine = engine_name
        self.__component = component_name

        self.__runtime_path = os.path.join(scripts.RUNTIME_DIR, engine_name, component_name)
        self.__logs_path = os.path.join(self.__runtime_path, 'logs')
        self.__temp_path = os.path.join(self.__runtime_path, 'tmp')

    def _get_runtimedir(self, ensure=True):
        if ensure and not os.path.isdir(self.__logs_path):
            fileutils.makedirs(self.__runtime_path, exist_ok=True)
        return self.__runtime_path

    def _get_logfile(self, name, ensure=True):
        if ensure and not os.path.isdir(self.__logs_path):
            fileutils.makedirs(self.__logs_path, exist_ok=True)

        return os.path.join(self.__logs_path, name + '.log')

    def _get_tempdir(self, name, ensure=True):
        if ensure and not os.path.isdir(self.__temp_path):
            fileutils.makedirs(self.__temp_path, exist_ok=True)

        folder = os.path.join(self.__temp_path, name)

        if ensure:
            shutil.rmtree(folder, ignore_errors=True)
            os.makedirs(folder)

        return folder

    def _clear_tempdir(self):
        shutil.rmtree(self.__temp_path, ignore_errors=True)


class _ProcessMonitor:
    def __init__(self, pidfile):
        self._pidfile = pidfile
        self._process = None
        self._stop_requested = False

    def _get_pid(self):
        pid = 0

        if os.path.isfile(self._pidfile):
            with open(self._pidfile) as pid_file:
                pid = int(pid_file.read())

        return pid

    def _set_pid(self, pid):
        parent_dir = os.path.abspath(os.path.join(self._pidfile, os.pardir))
        if not os.path.isdir(parent_dir):
            fileutils.makedirs(parent_dir, exist_ok=True)

        with open(self._pidfile, 'w') as pid_file:
            pid_file.write(str(pid))

    def is_running(self):
        pid = self._get_pid()

        if pid == 0:
            return False

        return daemon.is_running(pid)

    def start(self, daemonize=True):
        if self.is_running():
            raise IllegalStateException('process is already running')

        i_am_a_daemon = daemon.daemonize() if daemonize else True

        if i_am_a_daemon:
            self._set_pid(os.getpid())

            signal.signal(signal.SIGINT, self._kill_handler)
            signal.signal(signal.SIGTERM, self._kill_handler)

            code = 1

            while not self._stop_requested and code > 0 and code != -signal.SIGINT and code != -signal.SIGTERM:
                self._process = self._start_process()
                self._process.wait()
                code = self._process.returncode
        else:
            success = False
            for _ in range(0, 5):
                time.sleep(1)

                success = self._check_process_status()
                if success:
                    break

            return success

    def _start_process(self):
        pass

    def _check_process_status(self):
        return self.is_running()

    def _kill_handler(self, sign, _):
        self._stop_requested = True

        self._process.send_signal(sign)
        self._process.wait()

        exit(0)

    # after this amount of seconds, there is no excuse for a process to still be there.
    SIGTERM_TIMEOUT = 60 * 5

    def stop(self):
        pid = self._get_pid()

        if not self.is_running():
            raise IllegalStateException('process is not running')

        try:
            os.kill(pid, signal.SIGTERM)
            daemon.wait(pid, _ProcessMonitor.SIGTERM_TIMEOUT)
        except daemon.TimeoutExpired:
            # firin mah lazer!!1
            os.kill(pid, signal.SIGKILL)
            daemon.wait(pid)


# ==============================
# Engine
# ==============================

class _MMTEngineBuilderLogger:
    def __init__(self, count, line_len=70):
        self.line_len = line_len
        self.count = count
        self._current_step = 0
        self._step = None

    def start(self, engine, bilingual_corpora, monolingual_corpora):
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
        print '\t./mmt start|stop|status'
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


class _MMTEngineBuilder(_MMTRuntimeComponent):
    def __init__(self, engine):
        _MMTRuntimeComponent.__init__(self, engine.name, 'training')
        self._engine = engine

    def build(self, roots, debug=False, steps=None, split_trainingset=True):
        source_lang = self._engine.source_lang
        target_lang = self._engine.target_lang

        bilingual_corpora, monolingual_corpora = ParallelCorpus.splitlist(source_lang, target_lang, roots=roots)

        if len(bilingual_corpora) == 0:
            raise IllegalArgumentException('empty corpora provided')

        if steps is None:
            steps = self._engine.training_steps
        else:
            unknown_steps = [step for step in steps if step not in self._engine.training_steps]
            if len(unknown_steps) > 0:
                raise IllegalArgumentException('Unknown training steps: ' + str(unknown_steps))

        cmdlogger = _MMTEngineBuilderLogger(len(steps) + 1)
        cmdlogger.start(self._engine, bilingual_corpora, monolingual_corpora)

        shutil.rmtree(self._engine.path, ignore_errors=True)
        os.makedirs(self._engine.path)

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
                    log_file = self._get_logfile('context')
                    self._engine.analyzer.create_index(unprocessed_bicorpora, source_lang, log_file=log_file)

            # Training Adaptive Language Model (on the target side of all bilingual corpora)
            if 'lm' in steps:
                with cmdlogger.step('Language Model training') as _:
                    working_dir = self._get_tempdir('lm')
                    log_file = self._get_logfile('lm')
                    self._engine.lm.train(processed_bicorpora + processed_mocorpora, target_lang, working_dir, log_file)

            # Training Translation Model
            if 'tm' in steps:
                with cmdlogger.step('Translation Model training') as _:
                    working_dir = self._get_tempdir('tm')
                    log_file = self._get_logfile('tm')
                    self._engine.pt.train(processed_bicorpora, self._engine.aligner, working_dir, log_file)

            # Writing config file
            with cmdlogger.step('Writing config files') as _:
                self._engine.moses.create_ini()
                self._engine.write_config()

            cmdlogger.completed()
        except Exception as e:
            logger.exception(e)
            raise
        finally:
            if not debug:
                self._clear_tempdir()


class MMTEngine:
    injector_section = 'engine'
    injectable_fields = {
        'lm_type': ('LM implementation', (basestring, LanguageModel.available_types), 'MultiplexedLM'),
        'aligner_type': ('Aligner implementation',
                         (basestring, WordAligner.available_types), WordAligner.available_types[0]),
    }

    training_steps = ['tm_cleanup', 'preprocess', 'context_analyzer', 'lm', 'tm']

    def __init__(self, langs=None, name=None):
        self.name = name if name is not None else 'default'
        self.source_lang = langs[0] if langs is not None else None
        self.target_lang = langs[1] if langs is not None else None

        self._lm_type = None  # Injected
        self._aligner_type = None  # Injected

        self._config = None

        self.path = os.path.join(scripts.ENGINES_DIR, self.name)

        self.data_path = os.path.join(self.path, 'data')
        self.models_path = os.path.join(self.path, 'models')

        self._config_file = os.path.join(self.path, 'engine.ini')
        self._pt_model = os.path.join(self.models_path, 'phrase_tables')
        self._lm_model = os.path.join(self.models_path, 'lm', 'target.lm')
        self._context_index = os.path.join(self.models_path, 'context', 'index')
        self._moses_ini_file = os.path.join(self.models_path, 'moses.ini')

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


# ==============================
# MMT Components
# ==============================


class _TuningProcessLogger:
    def __init__(self, count, line_len=70):
        self.line_len = line_len
        self.count = count
        self._current_step = 0
        self._step = None
        self._api_port = None

    def start(self, server, corpora):
        engine = server.engine
        self._api_port = server.api_port

        print '\n============ TUNING STARTED ============\n'
        print 'ENGINE:  %s' % engine.name
        print 'CORPORA: %s (%d documents)' % (corpora[0].root, len(corpora))
        print 'LANGS:   %s > %s' % (engine.source_lang, engine.target_lang)
        print
        sys.stdout.flush()

    def step(self, step):
        self._step = step
        self._current_step += 1
        return self

    def completed(self, bleu):
        print '\n============ TUNING SUCCESS ============\n'
        print '\nFinal BLEU: %.2f\n' % (bleu * 100.)
        print 'You can try the API with:'
        print '\tcurl "http://localhost:{port}/translate?q=hello+world&context=computer"'.format(port=self._api_port)
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


class MMTServerApi:
    DEFAULT_TIMEOUT = 60 * 60  # sec

    def __init__(self, port):
        self.port = port
        self._url_template = 'http://localhost:{port}/{endpoint}'

        logging.getLogger('requests').setLevel(1000)
        logging.getLogger('urllib3').setLevel(1000)

    def _unpack(self, r):
        if r.status_code != requests.codes.ok:
            raise Exception('HTTP request failed with code ' + str(r.status_code) + ': ' + r.url)

        return r.json()

    def _get(self, endpoint, params=None):
        url = self._url_template.format(port=self.port, endpoint=endpoint)
        r = requests.get(url, params=params, timeout=MMTServerApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _delete(self, endpoint):
        url = self._url_template.format(port=self.port, endpoint=endpoint)
        r = requests.delete(url, timeout=MMTServerApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _put(self, endpoint, json=None):
        url = self._url_template.format(port=self.port, endpoint=endpoint)

        data = headers = None
        if json is not None:
            data = js.dumps(json)
            headers = {'Content-type': 'application/json'}

        r = requests.put(url, data=data, headers=headers, timeout=MMTServerApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _post(self, endpoint, json=None):
        url = self._url_template.format(port=self.port, endpoint=endpoint)

        data = headers = None
        if json is not None:
            data = js.dumps(json)
            headers = {'Content-type': 'application/json'}

        r = requests.post(url, data=data, headers=headers, timeout=MMTServerApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def stats(self):
        return self._get('_stat')

    def update_features(self, features):
        return self._put('decoder/features', json=features)

    def get_features(self):
        return self._get('decoder/features')

    def get_context_f(self, document, limit=None):
        params = {'local_file': document}
        if limit is not None:
            params['limit'] = limit
        return self._get('context', params=params)

    def get_context_s(self, text, limit=None):
        params = {'text': text}
        if limit is not None:
            params['limit'] = limit
        return self._get('context', params=params)

    def create_session(self, context):
        return self._post('sessions', json=context)

    def close_session(self, session):
        return self._delete('sessions/' + str(session))

    def translate(self, source, session=None, context=None, processing=True, nbest=None):
        p = {'q': source, 'processing': (1 if processing else 0)}
        if session is not None:
            p['session'] = session
        if nbest is not None:
            p['nbest'] = nbest
        if context is not None:
            p['context_array'] = js.dumps(context)

        return self._get('translate', params=p)


class _MMTDistributedComponent(_MMTRuntimeComponent, _ProcessMonitor):
    def __init__(self, component, engine, cluster_ports=None):
        _MMTRuntimeComponent.__init__(self, engine.name, component)
        _ProcessMonitor.__init__(self, os.path.join(self._get_runtimedir(), 'process.pid'))

        self.engine = engine
        self.cluster_ports = cluster_ports if cluster_ports is not None else DEFAULT_MMT_CLUSTER_PORTS


class MMTServer(_MMTDistributedComponent):
    def __init__(self, engine, api_port=None, cluster_ports=None):
        _MMTDistributedComponent.__init__(self, 'master', engine, cluster_ports)

        self.api_port = api_port if api_port is not None else DEFAULT_MMT_API_PORT
        self.api = MMTServerApi(api_port)

        self._mert_script = os.path.join(Moses.bin_path, 'scripts', 'mert-moses.pl')
        self._mert_i_script = os.path.join(scripts.MMT_ROOT, 'scripts', 'mertinterface.py')
        self.log_file = self._get_logfile('process', ensure=False)

    def _start_process(self):
        args = ['-e', self.engine.name, '-a', str(self.api_port), '-p', str(self.cluster_ports[0]),
                str(self.cluster_ports[1])]

        env = os.environ.copy()
        env['LD_LIBRARY_PATH'] = scripts.LIB_DIR
        command = mmt_javamain('eu.modernmt.cli.MasterNodeMain', args)

        self._get_logfile(self.log_file, ensure=True)
        log = open(self.log_file, 'wa')
        return subprocess.Popen(command, stderr=log, shell=False, env=env)

    def _check_process_status(self):
        try:
            self.api.stats()
            return True
        except:
            return False

    def tune(self, corpora=None, tokenize=True, debug=False, context_enabled=True):
        if corpora is None:
            corpora = ParallelCorpus.list(os.path.join(self.engine.data_path, TrainingPreprocessor.DEV_FOLDER_NAME))

        if len(corpora) == 0:
            raise IllegalArgumentException('empty corpora')

        if not self.is_running():
            raise IllegalStateException('No MMT Server running, start the engine first')

        target_lang = self.engine.target_lang
        source_lang = self.engine.source_lang

        cmdlogger = _TuningProcessLogger(4 if tokenize else 3)
        cmdlogger.start(self, corpora)

        working_dir = self._get_tempdir('tuning')
        mert_wd = os.path.join(working_dir, 'mert')

        try:
            original_corpora = corpora

            # Tokenization
            tokenized_corpora = original_corpora

            if tokenize:
                tokenizer_output = os.path.join(working_dir, 'tokenized_corpora')
                fileutils.makedirs(tokenizer_output, exist_ok=True)

                with cmdlogger.step('Corpus tokenization') as _:
                    tokenized_corpora = self.engine.preprocessor.process(corpora, tokenizer_output, print_tags=False,
                                                                         print_placeholders=True,
                                                                         original_spacing=False)

            # Create merged corpus
            with cmdlogger.step('Merging corpus') as _:
                source_merged_corpus = os.path.join(working_dir, 'corpus.' + source_lang)
                with open(source_merged_corpus, 'wb') as out:
                    original_root = original_corpora[0].root

                    for corpus in tokenized_corpora:
                        tokenized = corpus.get_file(source_lang)
                        original = os.path.join(original_root, corpus.name + '.' + source_lang)
                        out.write(tokenized + ':' + original + '\n')

                target_merged_corpus = os.path.join(working_dir, 'corpus.' + target_lang)
                fileutils.merge([corpus.get_file(target_lang) for corpus in tokenized_corpora], target_merged_corpus)

            # Run MERT algorithm
            with cmdlogger.step('Tuning') as _:
                # Start MERT
                decoder_flags = ['--port', str(self.api_port)]

                if not context_enabled:
                    decoder_flags.append('--skip-context-analysis')
                    decoder_flags.append('1')

                fileutils.makedirs(mert_wd, exist_ok=True)

                with tempfile.NamedTemporaryFile() as runtime_moses_ini:
                    command = [self._mert_script, source_merged_corpus, target_merged_corpus,
                               self._mert_i_script, runtime_moses_ini.name, '--threads',
                               str(multiprocessing.cpu_count()), '--mertdir', os.path.join(Moses.bin_path, 'bin'),
                               '--mertargs', '\'--binary --sctype BLEU\'', '--working-dir', mert_wd, '--nbest', '100',
                               '--decoder-flags', '"' + ' '.join(decoder_flags) + '"', '--nonorm', '--closest',
                               '--no-filter-phrase-table']

                    with open(self._get_logfile('mert'), 'wb') as log:
                        shell.execute(' '.join(command), stdout=log, stderr=log)

            # Read optimized configuration
            with cmdlogger.step('Applying changes') as _:
                bleu_score = 0
                weights = {}
                found_weights = False

                with open(os.path.join(mert_wd, 'moses.ini')) as moses_ini:
                    for line in moses_ini:
                        line = line.strip()

                        if len(line) == 0:
                            continue
                        elif found_weights:
                            tokens = line.split()
                            weights[tokens[0].rstrip('=')] = [float(val) for val in tokens[1:]]
                        elif line.startswith('# BLEU'):
                            bleu_score = float(line.split()[2])
                        elif line == '[weight]':
                            found_weights = True

                _ = self.api.update_features(weights)

            cmdlogger.completed(bleu_score)
        except Exception as e:
            logger.exception(e)
            raise
        finally:
            if not debug:
                self._clear_tempdir()

    def evaluate(self, corpora, google_key=None, heval_output=None, use_sessions=True):
        if len(corpora) == 0:
            raise IllegalArgumentException('empty corpora')

        if not self.is_running():
            raise IllegalStateException('No MMT Server running, start the engine first')

        he_outputter = None
        if heval_output is not None:
            fileutils.makedirs(heval_output, exist_ok=True)
            he_outputter = HumanEvaluationFileOutputter()

        target_lang = self.engine.target_lang
        source_lang = self.engine.source_lang
        xmlencoder = XMLEncoder()

        translators = [
            GoogleTranslate(source_lang, target_lang, key=google_key),
            # BingTranslator(source_lang, target_lang),
            MMTTranslator(self, use_sessions)
        ]

        working_dir = self._get_tempdir('evaluate')

        translations = []

        # Process references
        corpora_path = os.path.join(working_dir, 'corpora')
        corpora = xmlencoder.encode(corpora, corpora_path)

        reference = os.path.join(working_dir, 'reference.' + target_lang)
        fileutils.merge([corpus.get_file(target_lang) for corpus in corpora], reference)
        source = os.path.join(working_dir, 'source.' + source_lang)
        fileutils.merge([corpus.get_file(source_lang) for corpus in corpora], source)

        # Translate
        for translator in translators:
            tid = translator.name().replace(' ', '_')

            translations_path = os.path.join(working_dir, 'translations', tid + '.raw')
            xmltranslations_path = os.path.join(working_dir, 'translations', tid)
            fileutils.makedirs(translations_path, exist_ok=True)

            try:
                translated, mtt = translator.translate(corpora, translations_path)
                translated = xmlencoder.encode(translated, xmltranslations_path)
                translations.append((translator, translated, mtt))
            except TranslateError as e:
                translations.append((translator, e))
            except Exception as e:
                translations.append((translator, TranslateError('Unexcepted ERROR: ' + str(e.message))))

        if he_outputter is not None:
            he_output = os.path.join(heval_output, 'reference.' + target_lang)
            he_outputter.write(reference, he_output, target_lang)
            he_output = os.path.join(heval_output, 'source.' + source_lang)
            he_outputter.write(source, he_output, source_lang)

        # Scoring
        scores = {}

        for translation in translations:
            if len(translation) > 2:
                translator, translated, mtt = translation
                tid = translator.name().replace(' ', '_')

                translated_merged = os.path.join(working_dir, tid + '.' + target_lang)
                fileutils.merge([corpus.get_file(target_lang) for corpus in translated], translated_merged)

                if he_outputter is not None:
                    he_output = os.path.join(heval_output, tid + '.' + target_lang)
                    he_outputter.write(translated_merged, he_output, target_lang)

                scores[translator.name()] = {
                    'bleu': BLEUScore().calculate(translated_merged, reference),
                    'matecat': MatecatScore().calculate(translated_merged, reference),
                    '_mtt': mtt
                }
            else:
                translator, e = translation
                scores[translator.name()] = str(e.message)

        return scores


class MMTWorker(_MMTDistributedComponent):
    def __init__(self, engine, cluster_ports=None, master=None):
        _MMTDistributedComponent.__init__(self, 'slave', engine, cluster_ports)

        self._master = master
        self.log_file = self._get_logfile('process', ensure=False)
        self._status_file = os.path.join(self._get_runtimedir(), 'process.status')

    def _get_status(self):
        if os.path.isfile(self._status_file):
            with open(self._status_file) as content:
                status = content.read()
            return status.strip().lower()

        return None

    def wait_modelsync(self):
        status = self._get_status()

        while status is None:
            time.sleep(1)
            status = self._get_status()

        return status == 'ready'

    def _start_process(self):
        args = ['-e', self.engine.name, '-p', str(self.cluster_ports[0]), str(self.cluster_ports[1]), '--status-file',
                self._status_file]

        if self._master is not None:
            for key, value in self._master.iteritems():
                if value is not None:
                    args.append('--master-' + key)
                    args.append(str(value))

        env = os.environ.copy()
        env['LD_LIBRARY_PATH'] = scripts.LIB_DIR
        command = mmt_javamain('eu.modernmt.cli.SlaveNodeMain', args)

        self._get_logfile(self.log_file, ensure=True)
        log = open(self.log_file, 'wa')

        if os.path.isfile(self._status_file):
            os.remove(self._status_file)

        return subprocess.Popen(command, stderr=log, shell=False, env=env)
