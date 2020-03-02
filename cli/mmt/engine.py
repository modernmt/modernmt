import json as js
import logging
import os
import time
import requests
from xml.dom import minidom

from cli import mmt
from cli.mmt.mmtcli import mmt_java
from cli.utils.daemon import Daemon


class Engine(object):
    @classmethod
    def list(cls):
        engines = [cls(name)
                   for name in os.listdir(mmt.MMT_ENGINES_DIR)
                   if os.path.isdir(os.path.join(mmt.MMT_ENGINES_DIR, name))]

        return sorted([engine for engine in engines if engine.exists()], key=lambda e: e.name)

    @classmethod
    def get_languages_from_config(cls, config_file):
        def _get_child(root, child_name):
            elements = root.getElementsByTagName(child_name)
            return elements[0] if len(elements) > 0 else None

        languages = []

        config_root = minidom.parse(config_file).documentElement
        engine_el = _get_child(config_root, 'engine')
        lang_el = _get_child(engine_el, 'languages')

        if lang_el is not None:
            for pair_el in lang_el.getElementsByTagName('pair'):
                source_lang = pair_el.getAttribute('source')
                target_lang = pair_el.getAttribute('target')
                languages.append((source_lang, target_lang))
        else:
            source_lang = engine_el.getAttribute('source-language')
            target_lang = engine_el.getAttribute('target-language')
            languages.append((source_lang, target_lang))

        return languages

    def __init__(self, name) -> None:
        if os.sep in name:
            raise ValueError('Invalid engine name: "%s"' % name)

        self.name = name

        self.config_path = os.path.join(mmt.MMT_ENGINES_DIR, name, 'engine.xconf')
        self.path = os.path.join(mmt.MMT_ENGINES_DIR, name)
        self.test_data_path = os.path.join(self.path, 'test_data')
        self.models_path = os.path.join(self.path, 'models')
        self.runtime_path = os.path.join(mmt.MMT_RUNTIME_DIR, self.name)
        self.logs_path = os.path.join(self.runtime_path, 'logs')
        self.temp_path = os.path.join(self.runtime_path, 'tmp')

        if self.exists():
            self.languages = self.get_languages_from_config(self.config_path)
        else:
            self.languages = []

    def get_test_path(self, src_lang, tgt_lang):
        if src_lang > tgt_lang:
            src_lang, tgt_lang = tgt_lang, src_lang
        return os.path.join(self.test_data_path, '%s__%s' % (src_lang, tgt_lang))

    def exists(self):
        return os.path.isfile(self.config_path)

    def get_logfile(self, name, ensure=True, append=False):
        if ensure and not os.path.isdir(self.logs_path):
            os.makedirs(self.logs_path, exist_ok=True)

        logfile = os.path.join(self.logs_path, name + '.log')

        if not append and ensure and os.path.isfile(logfile):
            os.remove(logfile)

        return logfile

    def get_tempdir(self, name, ensure=True):
        if ensure and not os.path.isdir(self.temp_path):
            os.makedirs(self.temp_path, exist_ok=True)

        folder = os.path.join(self.temp_path, name)

        if ensure and not os.path.isdir(folder):
            os.makedirs(folder)

        return folder


class ApiException(Exception):
    def __init__(self, cause) -> None:
        super().__init__()
        self.cause = cause

    def __repr__(self):
        return '%s: %s' % (self.__class__.__name__, self.cause)

    def __str__(self):
        return self.cause


class _RestApi(object):
    DEFAULT_TIMEOUT = 60 * 60  # sec

    PRIORITY_HIGH = 'high'
    PRIORITY_NORMAL = 'normal'
    PRIORITY_BACKGROUND = 'background'

    def __init__(self, host=None, port=None, root=None) -> None:
        self.port = port
        self.host = host if host is not None else "localhost"
        self.root = self._normalize_root(root)

        if root is None:
            self.base_path = 'http://%s:%d' % (self.host, self.port)
        else:
            self.base_path = 'http://%s:%d%s' % (self.host, self.port, self.root)

        self._url_template = self.base_path + "/{endpoint}"

        logging.getLogger('requests').setLevel(1000)
        logging.getLogger('urllib3').setLevel(1000)

    @staticmethod
    def _normalize_root(root):
        if root is None or len(root.strip()) == 0:
            return None

        root = root.strip()
        if root[0] != '/':
            root = '/' + root
        if root[-1] == '/':
            root = root[:-1]

        return root.strip()

    @staticmethod
    def _unpack(r):
        if r.status_code != requests.codes.ok:
            msg = r.text
            try:
                error = r.json()['error']
                msg = '(%s) %s' % (error['type'], error['message'])
            except KeyError:
                pass
            except ValueError:
                pass

            raise ApiException('HTTP request "%s" failed with code %d: %s' % (r.url, r.status_code, msg))

        content = r.json()
        return content['data'] if 'data' in content else None

    def _get(self, endpoint, params=None):
        url = self._url_template.format(endpoint=endpoint)
        r = requests.get(url, params=params, timeout=self.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _delete(self, endpoint):
        url = self._url_template.format(endpoint=endpoint)
        r = requests.delete(url, timeout=self.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _put(self, endpoint, json=None, params=None):
        url = self._url_template.format(endpoint=endpoint)

        data = headers = None
        if json is not None:
            data = js.dumps(json)
            headers = {'Content-type': 'application/json'}
        elif params is not None:
            data = params

        r = requests.put(url, data=data, headers=headers, timeout=self.DEFAULT_TIMEOUT)
        return self._unpack(r)

    def _post(self, endpoint, json=None, params=None):
        url = self._url_template.format(endpoint=endpoint)

        data = headers = None
        if json is not None:
            data = js.dumps(json)
            headers = {'Content-type': 'application/json'}
        elif params is not None:
            data = params

        r = requests.post(url, data=data, headers=headers, timeout=self.DEFAULT_TIMEOUT)
        return self._unpack(r)

    @staticmethod
    def _encode_context(context):
        scores = [(e['memory'], e['score']) for e in context if 'memory' in e]
        scores = [(m['id'] if isinstance(m, dict) else m, s) for m, s in scores]
        return ','.join(['%d:%f' % e for e in scores])

    def info(self):
        return self._get('')

    def update_features(self, features):
        return self._put('decoder/features', json=features)

    def get_features(self):
        return self._get('decoder/features')

    def get_context_f(self, source, target, document, limit=None, user=None):
        params = {'local_file': os.path.abspath(document), 'source': source, 'targets': target}
        if limit is not None:
            params['limit'] = limit
        if user is not None:
            params['user'] = user
        return self._unpack_context(self._get('context-vector', params=params))

    def get_context_s(self, source, target, text, limit=None, user=None):
        params = {'text': text, 'source': source, 'targets': target}
        if limit is not None:
            params['limit'] = limit
        if user is not None:
            params['user'] = user
        return self._unpack_context(self._get('context-vector', params=params))

    @staticmethod
    def _unpack_context(data):
        result = data['vectors']
        return None if len(result) != 1 else list(result.values())[0]

    def health_check(self):
        return self._get('_health')

    def translate(self, source, target, text, context=None, nbest=None, verbose=False, priority=None, user=None):
        p = {'q': text, 'source': source, 'target': target}
        if nbest is not None:
            p['nbest'] = nbest
        if context is not None and len(context) > 0:
            p['context_vector'] = self._encode_context(context)
        if verbose:
            p['verbose'] = 'true'
        if priority is not None:
            p['priority'] = priority
        if user is not None:
            p['user'] = user

        return self._get('translate', params=p)

    def create_memory(self, name, owner=None):
        params = {'name': name}
        if owner is not None:
            params['owner'] = owner
        return self._post('memories', params=params)

    def delete_memory(self, memory_id):
        return self._delete('memories/' + str(memory_id))

    def append_to_memory(self, source, target, memory, sentence, translation):
        params = {'sentence': sentence, 'translation': translation, 'source': source, 'target': target}
        return self._post('memories/' + str(memory) + '/corpus', params=params)

    def replace_in_memory(self, source, target, memory, sentence, translation, old_sentence, old_translation):
        params = {'sentence': sentence, 'translation': translation, 'source': source, 'target': target,
                  'old_sentence': old_sentence, 'old_translation': old_translation}
        return self._put('memories/' + str(memory) + '/corpus', params=params)

    def import_into_memory(self, memory, tmx=None, compact=None,
                           source_file=None, target_file=None, source_lang=None, target_lang=None):
        if tmx is not None:
            params = {
                'content_type': 'tmx',
                'local_file': os.path.abspath(tmx)
            }
        elif compact is not None:
            params = {
                'content_type': 'compact',
                'local_file': os.path.abspath(compact)
            }
        else:
            params = {
                'content_type': 'parallel',
                'source': source_lang,
                'target': target_lang,
                'source_local_file': os.path.abspath(source_file),
                'target_local_file': os.path.abspath(target_file)
            }

        return self._post('memories/' + str(memory) + '/corpus', params=params)

    def get_import_job(self, jid):
        return self._get('memories/imports/' + str(jid))

    def get_all_memories(self):
        return self._get('memories')

    def rename_memory(self, mid, name):
        return self._put('memories/' + str(mid), params={'name': name})


class _State(object):
    def __init__(self, props=None):
        self.status = props['status']
        self.api_port = props['api']['port'] if 'api' in props else None
        self.api_root = props['api']['root'] if 'api' in props and 'root' in props['api'] else None
        self.cluster_port = props['cluster_port']
        self.binlog_host = props['binlog']['host'] if 'binlog' in props else None
        self.binlog_port = props['binlog']['port'] if 'binlog' in props else None
        self.database_host = props['database']['host'] if 'database' in props else None
        self.database_port = props['database']['port'] if 'database' in props else None
        self.embedded_services = [p for p in props['embedded_services']] if 'embedded_services' in props else None


class EngineNode(Daemon):
    STATUS = {
        'NONE': 0,
        'CREATED': 100,
        'JOINING': 200,
        'JOINED': 300,
        'SYNCHRONIZING': 400,
        'SYNCHRONIZED': 500,
        'LOADING': 600,
        'LOADED': 700,
        'UPDATING': 800,
        'UPDATED': 900,
        'RUNNING': 1000,
        'SHUTDOWN': 1100,
        'TERMINATED': 1200,
        'ERROR': 9999,
    }

    RestApi = _RestApi

    State = _State

    def __init__(self, engine):
        super().__init__(os.path.join(engine.runtime_path, 'node.pid'))

        self.engine = engine

        self._status_file = os.path.join(engine.runtime_path, 'node.properties')
        self._log_file = engine.get_logfile('node', ensure=False)
        self._api = None

    def start(self, api_port=None, cluster_port=None, binlog_port=None,
              db_port=None, leader=None, verbosity=None, remote_debug=False, log_file=None):
        if log_file is not None:
            self._log_file = log_file

        if not os.path.isdir(self.engine.runtime_path):
            os.makedirs(self.engine.runtime_path)

        args = ['-e', self.engine.name, '--status-file', self._status_file, '--log-file', self._log_file]

        if cluster_port is not None:
            args.append('--cluster-port')
            args.append(str(cluster_port))

        if api_port is not None:
            args.append('--api-port')
            args.append(str(api_port))

        if binlog_port is not None:
            args.append('--binlog-port')
            args.append(str(binlog_port))

        if db_port is not None:
            args.append('--db-port')
            args.append(str(db_port))

        if verbosity is not None:
            args.append('-v')
            args.append(str(verbosity))

        if leader is not None:
            args.append('--leader')
            args.append(leader)

        # read memory size
        mem_bytes = os.sysconf('SC_PAGE_SIZE') * os.sysconf('SC_PHYS_PAGES')  # e.g. 4015976448
        mem_mb = mem_bytes / (1024. ** 2)  # e.g. 3.74

        heap_mb = max(min(mem_mb / 4, 16 * 1024), 1024)
        heap_mb = int(heap_mb / 1024) * 1024

        logs_folder = os.path.abspath(os.path.join(self._log_file, os.pardir))
        command = mmt_java('eu.modernmt.cli.ClusterNodeMain', args, logs_path=logs_folder, remote_debug=remote_debug,
                           max_heap_mb=heap_mb, server=True)

        if os.path.isfile(self._status_file):
            os.remove(self._status_file)

        if not super()._start(command):
            raise Exception('failed to start node, check log file for more details: %s' % self._log_file)

    def stop(self, force=False):
        if self.running:
            super()._stop(children=self.state.embedded_services, timeout=10 if force else None)

        if os.path.isfile(self._status_file):
            os.remove(self._status_file)

    @property
    def api(self):
        if self._api is None:
            state = self.state

            if state is not None and state.api_port is not None:
                self._api = self.RestApi(port=state.api_port, root=state.api_root)

        return self._api

    @property
    def state(self):
        if os.path.isfile(self._status_file) and self.running:
            with open(self._status_file) as properties_file:
                properties = js.loads(properties_file.read())
                return self.State(properties)
        return None

    def wait(self, status):
        target_code = self.STATUS[status]

        while True:
            state = self.state

            current_status = 'NONE'
            if state is not None:
                current_status = self.state.status

                if current_status not in self.STATUS:
                    current_status = 'NONE'
                elif current_status == 'ERROR':
                    raise Exception('failed to start node, check log file for more details: ' + self._log_file)

            current_code = self.STATUS[current_status]

            if current_code < target_code:
                time.sleep(1)
            else:
                break
