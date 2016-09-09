import json as js
import logging
import multiprocessing
import os
import signal
import subprocess
import tempfile
import time

import requests

import cli
from cli import mmt_javamain, IllegalStateException, IllegalArgumentException
from cli.libs import fileutils, daemon, shell
from cli.mt import BilingualCorpus
from cli.mt.processing import TrainingPreprocessor, Tokenizer

__author__ = 'Davide Caroselli'

DEFAULT_MMT_API_PORT = 8045
DEFAULT_MMT_CLUSTER_PORTS = [5016, 5017]

class MMTApi:
    DEFAULT_TIMEOUT = 60 * 60  # sec

    def __init__(self, host="localhost", port=None):
        if host[:7] == "http://":
            host = host[7:]
        if host.find(":") != -1:
            host, port = host.split(":")
        port = port if port else DEFAULT_MMT_API_PORT
        self.port = int(port)
        self.host = host
        self._url_template = 'http://{host}:{port}/{endpoint}'

        logging.getLogger('requests').setLevel(1000)
        logging.getLogger('urllib3').setLevel(1000)

    @staticmethod
    def _unpack(r):
        if r.status_code != requests.codes.ok:
            raise Exception('HTTP request failed with code ' + str(r.status_code) + ': ' + r.url)
        content = r.json()

        return content['data'] if 'data' in content else None

    def _get(self, endpoint, params=None):
        url = self._url_template.format(host=self.host, port=self.port, endpoint=endpoint)
        r = requests.get(url, params=params, timeout=MMTApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _delete(self, endpoint):
        url = self._url_template.format(host=self.host, port=self.port, endpoint=endpoint)
        r = requests.delete(url, timeout=MMTApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _put(self, endpoint, json=None):
        url = self._url_template.format(host=self.host, port=self.port, endpoint=endpoint)

        data = headers = None
        if json is not None:
            data = js.dumps(json)
            headers = {'Content-type': 'application/json'}

        r = requests.put(url, data=data, headers=headers, timeout=MMTApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _post(self, endpoint, json=None):
        url = self._url_template.format(host=self.host, port=self.port, endpoint=endpoint)

        data = headers = None
        if json is not None:
            data = js.dumps(json)
            headers = {'Content-type': 'application/json'}

        r = requests.post(url, data=data, headers=headers, timeout=MMTApi.DEFAULT_TIMEOUT)
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

    def translate(self, source, session=None, context=None, nbest=None):
        p = {'q': source}
        if session is not None:
            p['session'] = session
        if nbest is not None:
            p['nbest'] = nbest
        if context is not None:
            p['context_array'] = js.dumps(context)

        return self._get('translate', params=p)


class _tuning_logger:
    def __init__(self, count, line_len=70):
        self.line_len = line_len
        self.count = count
        self._current_step = 0
        self._step = None
        self._api_port = None

    def start(self, node, corpora):
        engine = node.engine
        self._api_port = node.api.port

        print '\n============ TUNING STARTED ============\n'
        print 'ENGINE:  %s' % engine.name
        print 'CORPORA: %s (%d documents)' % (corpora[0].get_folder(), len(corpora))
        print 'LANGS:   %s > %s' % (engine.source_lang, engine.target_lang)
        print

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

    def __enter__(self):
        message = 'INFO: (%d of %d) %s... ' % (self._current_step, self.count, self._step)
        print message.ljust(self.line_len),

        self._start_time = time.time()
        return self

    def __exit__(self, *_):
        self._end_time = time.time()
        print 'DONE (in %ds)' % int(self._end_time - self._start_time)


class ClusterNode(object):
    __SIGTERM_TIMEOUT = 10  # after this amount of seconds, there is no excuse for a process to still be there.
    __LOG_FILENAME = 'node'

    STATUS = {
        'NONE': 0,
        'JOINED': 100,
        'SYNCHRONIZED': 200,
        'LOADED': 300,
        'READY': 400,
        'ERROR': 9999,
    }

    def __init__(self, engine, rest=True, host="localhost", api_port=None, cluster_ports=None,
                 sibling=None, verbosity=None):
        self.engine = engine
        self.api = MMTApi(host=host, port=api_port)

        self._pidfile = os.path.join(engine.get_runtime_path(), 'node.pid')

        self._cluster_ports = cluster_ports if cluster_ports is not None else DEFAULT_MMT_CLUSTER_PORTS
        self._api_port = api_port if api_port is not None else DEFAULT_MMT_API_PORT
        self._start_rest_server = rest
        self._sibling = sibling
        self._verbosity = verbosity
        self._status_file = os.path.join(engine.get_runtime_path(), 'node.status')
        self._log_file = engine.get_logfile(ClusterNode.__LOG_FILENAME, ensure=False)

        self._mert_script = os.path.join(cli.PYOPT_DIR, 'mert-moses.perl')
        self._mert_i_script = os.path.join(cli.PYOPT_DIR, 'mertinterface.py')

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

    def start(self):
        if self.is_running():
            raise IllegalStateException('process is already running')

        success = False
        process = self._start_process()
        pid = process.pid

        if pid > 0:
            self._set_pid(pid)

            for _ in range(0, 5):
                success = self.is_running()
                if success:
                    break

                time.sleep(1)

        if not success:
            raise Exception('failed to start node, check log file for more details: ' + self._log_file)

    def _start_process(self):
        if not os.path.isdir(self.engine.get_runtime_path()):
            fileutils.makedirs(self.engine.get_runtime_path(), exist_ok=True)
        self._log_file = self.engine.get_logfile(ClusterNode.__LOG_FILENAME, ensure=True)

        args = ['-e', self.engine.name, '-p', str(self._cluster_ports[0]), str(self._cluster_ports[1]),
                '--status-file', self._status_file]

        if self._start_rest_server:
            args.append('-a')
            args.append(str(self._api_port))

        if self._verbosity is not None:
            args.append('-v')
            args.append(str(self._verbosity))

        if self._sibling is not None:
            args.append('--member')
            args.append(str(self._sibling))

        command = mmt_javamain('eu.modernmt.cli.ClusterNodeMain', args,
                               hserr_path=os.path.abspath(os.path.join(self._log_file, os.pardir)))

        log = open(self._log_file, 'wa')

        if os.path.isfile(self._status_file):
            os.remove(self._status_file)

        return subprocess.Popen(command, stdout=open(os.devnull), stderr=log, shell=False)

    def get_state(self):
        state = None

        if os.path.isfile(self._status_file) and self.is_running():
            state = {}

            with open(self._status_file) as properties:
                for line in properties:
                    line = line.strip()

                    if not line.startswith('#'):
                        (key, value) = line.split('=', 2)
                        state[key] = value

        return state

    def _get_status(self):
        status = 'NONE'
        state = self.get_state()
        if state is not None and 'status' in state:
            status = state['status']

        return ClusterNode.STATUS[status] if status in ClusterNode.STATUS else ClusterNode.STATUS['NONE']

    def wait(self, status):
        status = ClusterNode.STATUS[status]
        current = self._get_status()

        while current < status:
            time.sleep(1)
            current = self._get_status() if self.is_running() else ClusterNode.STATUS['ERROR']

        if current == ClusterNode.STATUS['ERROR']:
            raise Exception('failed to start node, check log file for more details: ' + self._log_file)

    def stop(self):
        pid = self._get_pid()

        if not self.is_running():
            raise IllegalStateException('process is not running')

        try:
            os.kill(pid, signal.SIGTERM)
            daemon.wait(pid, ClusterNode.__SIGTERM_TIMEOUT)
        except daemon.TimeoutExpired:
            os.kill(pid, signal.SIGKILL)
            daemon.wait(pid)

    def restart(self):
        # ensure engine is stopped
        if self.is_running():
            self.stop()

        # start engine again
        self.start()
        self.wait('READY')

    def tune(self, corpora=None, debug=False, context_enabled=True):
        if corpora is None:
            corpora = BilingualCorpus.list(os.path.join(self.engine.data_path, TrainingPreprocessor.DEV_FOLDER_NAME))

        if len(corpora) == 0:
            raise IllegalArgumentException('empty corpora')

        if not self.is_running():
            raise IllegalStateException('No MMT Server running, start the engine first')

        tokenizer = Tokenizer()

        target_lang = self.engine.target_lang
        source_lang = self.engine.source_lang

        source_corpora = [BilingualCorpus.make_parallel(corpus.name, corpus.get_folder(), [source_lang])
                          for corpus in corpora]
        reference_corpora = [BilingualCorpus.make_parallel(corpus.name, corpus.get_folder(), [target_lang])
                             for corpus in corpora]

        cmdlogger = _tuning_logger(4)
        cmdlogger.start(self, corpora)

        working_dir = self.engine.get_tempdir('tuning')
        mert_wd = os.path.join(working_dir, 'mert')

        try:
            # Tokenization
            tokenized_output = os.path.join(working_dir, 'reference_corpora')
            fileutils.makedirs(tokenized_output, exist_ok=True)

            with cmdlogger.step('Corpora tokenization') as _:
                reference_corpora = tokenizer.process_corpora(reference_corpora, tokenized_output)

            # Create merged corpus
            with cmdlogger.step('Merging corpus') as _:
                # source
                source_merged_corpus = os.path.join(working_dir, 'corpus.' + source_lang)

                with open(source_merged_corpus, 'wb') as out:
                    for corpus in source_corpora:
                        out.write(corpus.get_file(source_lang) + '\n')

                # target
                target_merged_corpus = os.path.join(working_dir, 'corpus.' + target_lang)
                fileutils.merge([corpus.get_file(target_lang) for corpus in reference_corpora], target_merged_corpus)

            # Run MERT algorithm
            with cmdlogger.step('Tuning') as _:
                # Start MERT
                decoder_flags = ['--port', str(self.api.port)]

                if not context_enabled:
                    decoder_flags.append('--skip-context-analysis')
                    decoder_flags.append('1')

                fileutils.makedirs(mert_wd, exist_ok=True)

                with tempfile.NamedTemporaryFile() as runtime_moses_ini:
                    command = [self._mert_script, source_merged_corpus, target_merged_corpus,
                               self._mert_i_script, runtime_moses_ini.name, '--threads',
                               str(multiprocessing.cpu_count()), '--mertdir', os.path.join(cli.BIN_DIR, 'mert'),
                               '--mertargs', '\'--binary --sctype BLEU\'', '--working-dir', mert_wd, '--nbest', '100',
                               '--decoder-flags', '"' + ' '.join(decoder_flags) + '"', '--nonorm', '--closest',
                               '--no-filter-phrase-table']

                    with open(self.engine.get_logfile('mert'), 'wb') as log:
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
        finally:
            if not debug:
                self.engine.clear_tempdir()
