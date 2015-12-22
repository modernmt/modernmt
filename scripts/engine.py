import BaseHTTPServer
import errno
import json
import logging
import os
import random
import shutil
import signal
import time
import urllib
import urllib2
import urlparse
from ConfigParser import ConfigParser

import requests

import scripts
from scripts.libs import fileutils, daemon, shell
from scripts.mt.contextanalysis import ContextAnalyzer
from scripts.mt.lm import LanguageModel
from scripts.mt.moses import Moses, MosesFeature, LexicalReordering
from scripts.mt.phrasetable import WordAligner, SuffixArraysPhraseTable
from scripts.mt.processing import Tokenizer, Detokenizer, CorpusCleaner

__author__ = 'Davide Caroselli'

logger = logging.getLogger()


class _Api:
    def __init__(self, port):
        self.port = port

    def get(self, endpoint, params=None):
        url = 'http://localhost:{port}/{endpoint}'.format(port=self.port, endpoint=endpoint)

        if params is not None:
            url += '?' + urllib.urlencode(params)

        raw_text = urllib2.urlopen(url).read()
        return json.loads(raw_text)


class _CommandLogger:
    def __init__(self, step, logger_=None):
        self._step = step
        self._logger = logger_ if logger_ is not None else logger

    def __enter__(self):
        self._logger.info('%s is taking place...', self._step)
        self._start_time = time.time()

        return self

    def __exit__(self, *_):
        self._end_time = time.time()
        self._logger.info('%s finished, took %ds', self._step, self._end_time - self._start_time)


# noinspection PyBroadException
class MMTEngine:
    injector_section = 'engine'
    injectable_fields = {
        'lm_type': ('LM implementation', (basestring, LanguageModel.available_types), LanguageModel.available_types[0]),
        'aligner_type': (
            'Aligner implementation', (basestring, WordAligner.available_types), WordAligner.available_types[0]),
    }

    training_steps = ['tokenize', 'clean', 'context_analyzer', 'lm', 'tm']

    def __init__(self, langs=None, name=None):
        self._name = name if name is not None else 'default'
        self._source_lang = langs[0] if langs is not None else None
        self._target_lang = langs[1] if langs is not None else None

        self._lm_type = None  # Injected
        self._aligner_type = None  # Injected

        self._config = None

        self._mert_script = os.path.join(Moses.bin_path, 'scripts', 'mert-moses.pl')
        self._mert_i_script = os.path.join(scripts.MMT_ROOT, 'pymmt', 'mertinterface.py')

        self._root_path = os.path.join(scripts.ENGINES_DIR, self._name)
        self._data_path = os.path.join(self._root_path, 'data')
        self._logs_path = os.path.join(self._root_path, 'logs')
        self._temp_path = os.path.join(self._root_path, 'temp')
        self._runtime_path = os.path.join(self._root_path, 'runtime')

        self._config_file = os.path.join(self._root_path, 'engine.ini')
        self._server_pid_file = os.path.join(self._runtime_path, 'pid')
        self._pt_model = os.path.join(self._data_path, 'phrase_tables')
        self._lm_model = os.path.join(self._data_path, 'lm', 'target.lm')
        self._context_index = os.path.join(self._data_path, 'context', 'index')
        self._moses_ini_file = os.path.join(self._data_path, 'moses.ini')

        self.server = None

    def exists(self):
        return os.path.isfile(self._config_file)

    def _on_fields_injected(self, injector):
        if self._target_lang is None or self._source_lang is None:
            config = self.config

            if config is not None:
                self._target_lang = config.get(self.injector_section, 'target_lang')
                self._source_lang = config.get(self.injector_section, 'source_lang')

        if self._target_lang is None or self._source_lang is None:
            raise Exception('Engine target language or source language must be specified')

        self._tokenizer = injector.inject(Tokenizer())
        self._detokenizer = injector.inject(Detokenizer())
        self._cleaner = injector.inject(CorpusCleaner())

        self._analyzer = injector.inject(ContextAnalyzer())

        self._pt = injector.inject(SuffixArraysPhraseTable(self._pt_model, (self._source_lang, self._target_lang)))
        self._aligner = injector.inject(WordAligner.instantiate(self._aligner_type))
        self._lm = injector.inject(LanguageModel.instantiate(self._lm_type, self._lm_model))

        self._moses = injector.inject(Moses(self._moses_ini_file))
        self._moses.add_feature(MosesFeature('UnknownWordPenalty'))
        self._moses.add_feature(MosesFeature('WordPenalty'))
        self._moses.add_feature(MosesFeature('Distortion'))
        self._moses.add_feature(MosesFeature('PhrasePenalty'))
        self._moses.add_feature(self._pt)
        self._moses.add_feature(LexicalReordering())
        self._moses.add_feature(self._lm)

        if self._config is None:
            self._config = injector.to_config()
            self._config.set(self.injector_section, 'source_lang', self._source_lang)
            self._config.set(self.injector_section, 'target_lang', self._target_lang)

    @property
    def config(self):
        if self._config is None and os.path.isfile(self._config_file):
            self._config = ConfigParser()
            self._config.read(self._config_file)
        return self._config

    def _get_tempdir(self, name, ensure=True):
        if ensure and not os.path.isdir(self._temp_path):
            fileutils.makedirs(self._temp_path, exist_ok=True)

        folder = os.path.join(self._temp_path, name)

        if ensure:
            shutil.rmtree(folder, ignore_errors=True)
            os.makedirs(folder)

        return folder

    def _get_logfile(self, name, ensure=True):
        if ensure and not os.path.isdir(self._logs_path):
            fileutils.makedirs(self._logs_path, exist_ok=True)

        return os.path.join(self._logs_path, name + '.log')

    def build(self, corpora, debug=False, steps=None):
        if len(corpora) == 0:
            raise Exception('Empty corpora')

        if steps is None:
            steps = self.training_steps
        else:
            unknown_steps = [step for step in steps if step not in self.training_steps]
            if len(unknown_steps) > 0:
                raise Exception('Unknown training steps: ' + str(unknown_steps))

        logger.info("MMT training started. ENGINE = %s, CORPORA = %s (%d documents), LANGS = %s > %s", self._name,
                    corpora[0].root, len(corpora), self._source_lang, self._target_lang)

        shutil.rmtree(self._root_path, ignore_errors=True)
        os.makedirs(self._root_path)

        try:
            original_corpora = corpora

            # Tokenization
            tokenized_corpora = original_corpora

            if 'tokenize' in steps:
                with _CommandLogger('Tokenization process') as _:
                    tokenizer_output = self._get_tempdir('tokenizer')
                    tokenized_corpora = self._tokenizer.batch_tokenize(corpora, tokenizer_output)

            # Cleaning
            cleaned_corpora = tokenized_corpora

            if 'clean' in steps:
                with _CommandLogger('Corpora cleaning process') as _:
                    cleaner_output = self._get_tempdir('cleaner')
                    cleaned_corpora = self._cleaner.batch_clean(tokenized_corpora, cleaner_output)

            # Training Context Analyzer
            if 'context_analyzer' in steps:
                with _CommandLogger('Context Analyzer training') as _:
                    log_file = self._get_logfile('build.context')
                    self._analyzer.create_index(self._context_index, original_corpora[0].root, log_file)

            # Training Language Model
            if 'lm' in steps:
                with _CommandLogger('Language Model training') as _:
                    working_dir = self._get_tempdir('lm')
                    log_file = self._get_logfile('build.lm')
                    self._lm.train(tokenized_corpora, self._target_lang, working_dir, log_file)

            # Training Translation Model
            if 'tm' in steps:
                with _CommandLogger('Translation Model training') as _:
                    working_dir = self._get_tempdir('tm')
                    log_file = self._get_logfile('build.tm')
                    self._pt.train(cleaned_corpora, self._aligner, working_dir, log_file)

            # Writing config file
            logger.info("Writing '%s' file...", self._config_file)
            with open(self._config_file, 'wb') as out:
                self._config.write(out)

            logger.info("Writing Moses INI template file...")
            with open(self._moses_ini_file, 'wb') as out:
                out.write(self._moses.create_ini())

            logger.info("MMT training process finished with SUCCESS")
        except Exception as e:
            logger.exception(e)
            raise
        finally:
            if not debug:
                shutil.rmtree(self._temp_path, ignore_errors=True)

    def tune(self, corpora, port, tokenize=True, debug=False, context_enabled=True):
        if len(corpora) == 0:
            raise Exception('Empty corpora')

        logger.info("MMT tuning started. ENGINE = %s, CORPORA = %s (%d documents), LANGS = %s > %s", self._name,
                    corpora[0].root, len(corpora), self._source_lang, self._target_lang)

        working_dir = self._get_tempdir('tuning')

        try:
            original_corpora = corpora

            # Tokenization
            tokenized_corpora = original_corpora

            if tokenize:
                tokenizer_output = os.path.join(working_dir, 'tokenized_corpora')
                fileutils.makedirs(tokenizer_output, exist_ok=True)

                with _CommandLogger('Tokenization process') as _:
                    tokenized_corpora = self._tokenizer.batch_tokenize(corpora, tokenizer_output)

            # Create merged corpus
            source_merged_corpus = os.path.join(working_dir, 'corpus.' + self._source_lang)
            with open(source_merged_corpus, 'wb') as out:
                original_root = original_corpora[0].root

                for corpus in tokenized_corpora:
                    tokenized = corpus.get_file(self._source_lang)
                    original = os.path.join(original_root, corpus.name + '.' + self._source_lang)
                    out.write(tokenized + ':' + original + '\n')

            target_merged_corpus = os.path.join(working_dir, 'corpus.' + self._target_lang)
            fileutils.merge([corpus.get_file(self._target_lang) for corpus in tokenized_corpora], target_merged_corpus)

            # Run MERT algorithm
            with _CommandLogger('MERT tuning') as _:
                decoder_flags = ['--port', str(port)]

                if not context_enabled:
                    decoder_flags.append('--skip-context-analysis')
                    decoder_flags.append('1')

                mert_wd = os.path.join(working_dir, 'mert')
                fileutils.makedirs(mert_wd, exist_ok=True)

                command = [self._mert_script, source_merged_corpus, target_merged_corpus,
                           self._mert_i_script, self._moses_ini_file, '--mertdir', os.path.join(Moses.bin_path, 'bin'),
                           '--mertargs', '\'--binary --sctype BLEU\'', '--working-dir', mert_wd,
                           '--nbest', '100', '--decoder-flags', '"' + ' '.join(decoder_flags) + '"', '--nonorm',
                           '--closest', '--no-filter-phrase-table']

                with open(self._get_logfile('mert'), 'wb') as log:
                    shell.execute(' '.join(command), stdout=log, stderr=log)

            # Read optimized configuration
            bleu_score = 0
            weights = {}
            found_weights = False

            with open(os.path.join(working_dir, 'moses.ini')) as moses_ini:
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

            with _CommandLogger('Applying changes') as _:
                _ = _Api(port).get('weights/set', {'w': json.dumps(weights)})

            logger.info("MMT tuning process finished with BLEU of " + str(bleu_score))
        except Exception as e:
            logger.exception(e)
            raise
        finally:
            if not debug:
                shutil.rmtree(self._temp_path, ignore_errors=True)

    def start_server(self, ports, context_enabled=True, daemonize=True):
        if self.is_server_running():
            raise Exception('Engine server is already running')

        analyzer_port, moses_port, server_port = ports
        self.server = MMTEngineServer(analyzer_port, moses_port, server_port, self)
        self.server.set_context_enabled(context_enabled)

        i_am_a_daemon = daemon.daemonize() if daemonize else True

        if i_am_a_daemon:
            self.server.start()
        else:
            success = False
            for _ in range(0, 5):
                logging.getLogger('requests').setLevel(1000)
                logging.getLogger('urllib3').setLevel(1000)

                try:
                    response = requests.get('http://localhost:{port}/'.format(port=server_port))
                    if response.status_code == 200:
                        success = True
                        break
                except:
                    pass

                time.sleep(1)

            return success

    def _get_server_pid(self):
        pid = 0

        if os.path.isfile(self._server_pid_file):
            with open(self._server_pid_file) as pid_file:
                pid = int(pid_file.read())

        return pid

    def _set_server_pid(self, pid):
        parent_dir = os.path.abspath(os.path.join(self._server_pid_file, os.pardir))
        if not os.path.isdir(parent_dir):
            fileutils.makedirs(parent_dir, exist_ok=True)

        with open(self._server_pid_file, 'w') as pid_file:
            pid_file.write(str(pid))

    def is_server_running(self):
        pid = self._get_server_pid()

        if pid == 0:
            return False

        try:
            os.kill(pid, 0)
        except OSError as err:
            if err.errno == errno.ESRCH:
                # ESRCH == No such process
                return False
            elif err.errno == errno.EPERM:
                # EPERM clearly means there's a process to deny access to
                return True
            else:
                # According to "man 2 kill" possible error values are
                # (EINVAL, EPERM, ESRCH)
                raise
        else:
            return True

    def stop_server(self):
        if not self.is_server_running():
            raise Exception('Server is not running')

        os.kill(self._get_server_pid(), signal.SIGTERM)