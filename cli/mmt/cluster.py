import json as js
import logging
import os
import time

import requests

import cli
from cli import mmt_javamain, IllegalArgumentException
from cli.libs.daemon import DaemonController
from cli.mmt import TMXCorpus, FileParallelCorpus
from cli.mmt.engine import Engine

__author__ = 'Davide Caroselli'


class ApiException(Exception):
    def __init__(self, *args, **kwargs):
        super(ApiException, self).__init__(*args, **kwargs)


class ClusterNode(DaemonController):
    class State(object):
        def __init__(self, props=None):
            self.status = props['status']
            self.api_port = props['api']['port'] if 'api' in props else None
            self.api_root = props['api']['root'] if 'api' in props and 'root' in props['api'] else None
            self.cluster_port = props['cluster_port']
            self.datastream_host = props['datastream']['host'] if 'datastream' in props else None
            self.datastream_port = props['datastream']['port'] if 'datastream' in props else None
            self.database_host = props['database']['host'] if 'database' in props else None
            self.database_port = props['database']['port'] if 'database' in props else None
            self.embedded_services = [p for p in props['embedded_services']] if 'embedded_services' in props else None

    class Api(object):
        DEFAULT_TIMEOUT = 60 * 60  # sec

        PRIORITY_HIGH = 'high'
        PRIORITY_NORMAL = 'normal'
        PRIORITY_BACKGROUND = 'background'

        def __init__(self, host=None, port=None, root=None):
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
            params = {'local_file': document, 'source': source, 'targets': target}
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
            return None if len(result) != 1 else result.values()[0]

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

        def import_into_memory(self, memory, tmx=None, compact=None,
                               source_file=None, target_file=None, source_lang=None, target_lang=None):
            if tmx is not None:
                params = {
                    'content_type': 'tmx',
                    'local_file': tmx
                }
            elif compact is not None:
                params = {
                    'content_type': 'compact',
                    'local_file': compact
                }
            else:
                params = {
                    'content_type': 'parallel',
                    'source': source_lang,
                    'target': target_lang,
                    'source_local_file': source_file,
                    'target_local_file': target_file
                }

            return self._post('memories/' + str(memory) + '/corpus', params=params)

        def get_import_job(self, jid):
            return self._get('memories/imports/' + str(jid))

        def get_all_memories(self):
            return self._get('memories')

        def rename_memory(self, mid, name):
            return self._put('memories/' + str(mid), params={'name': name})

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
        'READY': 1000,
        'SHUTDOWN': 1100,
        'TERMINATED': 1200,
        'ERROR': 9999,
    }

    # This method creates a new ClusterNode object for an already existing engine
    # (and therefore with an already existing node.status file)
    @staticmethod
    def connect(engine_name, silent=False):
        engine = None

        try:
            # Load the already created engine
            engine = Engine.load(engine_name)
        except IllegalArgumentException:
            if not silent:
                raise

        # create a clusterNode and load its node.status file
        return ClusterNode(engine) if engine is not None else None

    def __init__(self, engine):
        super(ClusterNode, self).__init__(os.path.join(engine.runtime_path, 'node.pid'), sigterm_timeout=10)

        self._status_file = os.path.join(engine.runtime_path, 'node.properties')
        self._log_file = engine.get_logfile('node', ensure=False)
        self._scorer_script = os.path.join(cli.PYOPT_DIR, 'mmt-bleu.perl')

        self.engine = engine
        self._api = None

    def start(self, api_port=None, cluster_port=None, datastream_port=None,
              db_port=None, leader=None, verbosity=None, remote_debug=False, log_file=None):
        if log_file is not None:
            self._log_file = log_file

        if not os.path.isdir(self.engine.runtime_path):
            os.makedirs(self.engine.runtime_path)

        args = ['-e', self.engine.name,
                '--status-file', self._status_file,
                '--log-file', self._log_file]

        if cluster_port is not None:
            args.append('--cluster-port')
            args.append(str(cluster_port))

        if api_port is not None:
            args.append('--api-port')
            args.append(str(api_port))

        if datastream_port is not None:
            args.append('--datastream-port')
            args.append(str(datastream_port))

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
        command = mmt_javamain('eu.modernmt.cli.ClusterNodeMain', args,
                               logs_path=logs_folder, remote_debug=remote_debug,
                               max_heap_mb=heap_mb, server=True)

        if os.path.isfile(self._status_file):
            os.remove(self._status_file)

        if not super(ClusterNode, self)._start(command):
            raise Exception('failed to start node, check log file for more details: %s' % self._log_file)

    def stop(self):
        if self.running:
            super(ClusterNode, self)._stop(children=self.state.embedded_services)

        if os.path.isfile(self._status_file):
            os.remove(self._status_file)

    @property
    def api(self):
        if self._api is None:
            state = self.state

            if state is not None and state.api_port is not None:
                self._api = self.Api(port=state.api_port, root=state.api_root)

        return self._api

    @property
    def state(self):
        if os.path.isfile(self._status_file) and self.running:
            with open(self._status_file) as properties_file:
                properties = js.loads(properties_file.read())
                return ClusterNode.State(properties)
        return None

    def wait(self, status):
        target_code = ClusterNode.STATUS[status]

        while True:
            state = self.state

            current_status = 'NONE'
            if state is not None:
                current_status = self.state.status

                if current_status not in ClusterNode.STATUS:
                    current_status = 'NONE'
                elif current_status == 'ERROR':
                    raise Exception('failed to start node, check log file for more details: ' + self._log_file)

            current_code = ClusterNode.STATUS[current_status]

            if current_code < target_code:
                time.sleep(1)
            else:
                break

    def new_memory(self, name):
        return self.api.create_memory(name)

    def delete_memory(self, mid):
        return self.api.delete_memory(mid)

    def import_corpus(self, memory_id, corpus, callback=None, refresh_rate_in_seconds=1):
        if type(corpus) == TMXCorpus:
            job = self.api.import_into_memory(memory_id, tmx=corpus.get_tmx())
        elif type(corpus) == FileParallelCorpus:
            source_lang, target_lang = corpus.langs
            job = self.api.import_into_memory(memory_id,
                                              source_file=corpus.get_file(source_lang),
                                              target_file=corpus.get_file(target_lang),
                                              source_lang=source_lang,
                                              target_lang=target_lang)
        else:
            raise IllegalArgumentException('Invalid corpus type: ' + str(type(corpus)))

        if callback is not None:
            callback(job)

        while job['progress'] != 1.0:
            time.sleep(refresh_rate_in_seconds)
            job = self.api.get_import_job(job['id'])

            if callback is not None:
                callback(job)

    def get_memory_id_by_name(self, name):
        try:
            return int(name)
        except ValueError:
            memories = self.api.get_all_memories()
            ids = [m['id'] for m in memories if m['name'] == name]

            if len(ids) == 0:
                raise IllegalArgumentException('unable to find memory "%s"' % name)
            elif len(ids) > 1:
                raise IllegalArgumentException(
                    'ambiguous memory name "%s", choose one of the following ids: %s' % (name, str(ids)))
            else:
                return ids[0]

    def append_to_memory(self, memory, source_lang, target_lang, sentence, translation):
        memory = self.get_memory_id_by_name(memory)
        return self.api.append_to_memory(source_lang, target_lang, memory, sentence, translation)

    def rename_memory(self, memory, name):
        memory = self.get_memory_id_by_name(memory)
        return self.api.rename_memory(memory, name)
