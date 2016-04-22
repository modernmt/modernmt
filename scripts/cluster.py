import json as js
import logging
import os
import shutil
import signal
import subprocess
import time

import requests

import scripts
from scripts import mmt_javamain, IllegalStateException
from scripts.libs import fileutils, daemon

__author__ = 'Davide Caroselli'

DEFAULT_MMT_API_PORT = 8045
DEFAULT_MMT_CLUSTER_PORT = 5016


class MMTApi:
    DEFAULT_TIMEOUT = 60 * 60  # sec

    def __init__(self, port):
        self.port = port
        self._url_template = 'http://localhost:{port}/{endpoint}'

        logging.getLogger('requests').setLevel(1000)
        logging.getLogger('urllib3').setLevel(1000)

    @staticmethod
    def _unpack(r):
        if r.status_code != requests.codes.ok:
            raise Exception('HTTP request failed with code ' + str(r.status_code) + ': ' + r.url)

        return r.json()

    def _get(self, endpoint, params=None):
        url = self._url_template.format(port=self.port, endpoint=endpoint)
        r = requests.get(url, params=params, timeout=MMTApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _delete(self, endpoint):
        url = self._url_template.format(port=self.port, endpoint=endpoint)
        r = requests.delete(url, timeout=MMTApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _put(self, endpoint, json=None):
        url = self._url_template.format(port=self.port, endpoint=endpoint)

        data = headers = None
        if json is not None:
            data = js.dumps(json)
            headers = {'Content-type': 'application/json'}

        r = requests.put(url, data=data, headers=headers, timeout=MMTApi.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _post(self, endpoint, json=None):
        url = self._url_template.format(port=self.port, endpoint=endpoint)

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

    def translate(self, source, session=None, context=None, processing=True, nbest=None):
        p = {'q': source, 'processing': (1 if processing else 0)}
        if session is not None:
            p['session'] = session
        if nbest is not None:
            p['nbest'] = nbest
        if context is not None:
            p['context_array'] = js.dumps(context)

        return self._get('translate', params=p)


class MMTMember:
    __SIGTERM_TIMEOUT = 10  # after this amount of seconds, there is no excuse for a process to still be there.

    STATUS = {
        'NONE': 0,
        'JOINED': 100,
        'SYNCHRONIZED': 200,
        'READY': 300,
        'ERROR': 9999,
    }

    def __init__(self, engine, rest=True, api_port=None, cluster_port=None, sibling=None, verbosity=None):
        self.engine = engine
        self.api = MMTApi(api_port)

        self._runtime_path = os.path.join(scripts.RUNTIME_DIR, engine.name, 'member')
        self._logs_path = os.path.join(self._runtime_path, 'logs')
        self._temp_path = os.path.join(self._runtime_path, 'tmp')

        self._pidfile = os.path.join(self._runtime_path, 'process.pid')
        self._process = None
        self._stop_requested = False

        self._cluster_port = cluster_port if cluster_port is not None else DEFAULT_MMT_CLUSTER_PORT
        self._api_port = api_port if api_port is not None else DEFAULT_MMT_API_PORT
        self._start_rest_server = rest
        self._sibling = sibling
        self._verbosity = verbosity
        self._status_file = os.path.join(self._runtime_path, 'process.status')
        self._log_file = os.path.join(self._logs_path, 'process.log')

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

    def _kill_handler(self, sign, _):
        self._stop_requested = True

        self._process.send_signal(sign)
        self._process.wait()

        exit(0)

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
                success = self.is_running()
                if success:
                    break

                time.sleep(1)

            if not success:
                raise Exception('failed to start member, check log file for more details: ' + self._log_file)

    def _start_process(self):
        if not os.path.isdir(self._runtime_path):
            fileutils.makedirs(self._runtime_path, exist_ok=True)
        if not os.path.isdir(self._logs_path):
            fileutils.makedirs(self._logs_path, exist_ok=True)
        if not os.path.isdir(self._temp_path):
            fileutils.makedirs(self._temp_path, exist_ok=True)

        args = ['-e', self.engine.name, '-p', str(self._cluster_port), '--status-file', self._status_file]

        if self._start_rest_server:
            args.append('-a')
            args.append(str(self._api_port))

        if self._verbosity is not None:
            args.append('-v')
            args.append(str(self._verbosity))

        if self._sibling is not None:
            for key, value in self._sibling.iteritems():
                if value is not None:
                    args.append('--member-' + key)
                    args.append(str(value))

        env = os.environ.copy()
        env['LD_LIBRARY_PATH'] = scripts.LIB_DIR
        command = mmt_javamain('eu.modernmt.cli.MemberMain', args)

        log = open(self._log_file, 'wa')

        if os.path.isfile(self._status_file):
            os.remove(self._status_file)

        return subprocess.Popen(command, stderr=log, shell=False, env=env)

    def _get_status(self):
        if os.path.isfile(self._status_file):
            with open(self._status_file) as content:
                status = content.read()
            status = status.strip().upper()

            return MMTMember.STATUS[status] if status in MMTMember.STATUS else MMTMember.STATUS['NONE']

        return MMTMember.STATUS['NONE']

    def wait(self, status):
        status = MMTMember.STATUS[status]
        current = self._get_status()

        while current < status:
            time.sleep(1)
            current = self._get_status()

        if current == MMTMember.STATUS['ERROR']:
            raise Exception('failed to start member, check log file for more details: ' + self._log_file)

    def stop(self):
        pid = self._get_pid()

        if not self.is_running():
            raise IllegalStateException('process is not running')

        try:
            os.kill(pid, signal.SIGTERM)
            daemon.wait(pid, MMTMember.__SIGTERM_TIMEOUT)
        except daemon.TimeoutExpired:
            os.kill(pid, signal.SIGKILL)
            daemon.wait(pid)

    def get_tempdir(self, name, ensure=True):
        if ensure and not os.path.isdir(self._temp_path):
            fileutils.makedirs(self._temp_path, exist_ok=True)

        folder = os.path.join(self._temp_path, name)

        if ensure:
            shutil.rmtree(folder, ignore_errors=True)
            os.makedirs(folder)

        return folder
