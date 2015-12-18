import json
import os
import subprocess
import xmlrpclib
import time
import scripts
from scripts.libs import shell

__author__ = 'Davide Caroselli'


class MosesFeature:
    def __init__(self, name):
        self.name = name

    def get_iniline(self):
        return self.name


class LexicalReordering(MosesFeature):
    def __init__(self):
        MosesFeature.__init__(self, 'LexicalReordering')

    def get_iniline(self):
        return self.name + ' name=DM0 input-factor=0 output-factor=0 type=hier-mslr-bidirectional-fe-allff'


class Moses:
    injector_section = 'moses'
    injectable_fields = {
        'stack_size': ('search algorithm stack size', int, 5000),
        'cube_pruning_pop_limit': ('pop limit of cube pruning algorithm', int, 5000),
        'distortion_limit': ('distortion limit', int, 6),
    }

    bin_path = os.path.join(scripts.BIN_DIR, 'moses-mmt-dev_4a82__6baa')

    def __init__(self, ini_file):
        self._stack_size = None  # Injected
        self._cube_pruning_pop_limit = None  # Injected
        self._distortion_limit = None  # Injected

        self._moses_bin = os.path.join(Moses.bin_path, 'bin', 'moses')
        self._server_ini_file = ini_file

        self._server_process = None
        self._server_port = None
        self._server_proxy = None
        self._server_log_file = None

        self._features = []

    def add_feature(self, feature):
        self._features.append(feature)

    def get_features(self):
        return self._features[:]

    def create_ini(self, weights=None):
        lines = ['[input-factors]', '0', '', '[search-algorithm]', '1', '', '[stack]', str(self._stack_size), '',
                 '[cube-pruning-pop-limit]', str(self._cube_pruning_pop_limit), '', '[mapping]', '0 T 0', '',
                 '[distortion-limit]', str(self._distortion_limit), '', '[v]', '0', '', '[feature]']

        for feature in self._features:
            lines.append(feature.get_iniline())
        lines.append('')

        if weights is not None:
            lines.append('[weight]')

            for feature, w in weights.iteritems():
                lines.append(' '.join([feature + '='] + [str(el) for el in w]))

        lines.append('')
        return '\n'.join(lines)

    def restart_server(self):
        port = self._server_port
        log_file = self._server_log_file

        self.stop_server()
        self.start_server(port, log_file)

    def start_server(self, port, log_file=None):
        self._server_log_file = log_file

        log = shell.DEVNULL if log_file is None else open(log_file, 'w')

        command = [self._moses_bin, '-f', self._server_ini_file, '--server', '--server-port', str(port)]
        self._server_process = subprocess.Popen(command, stdout=log, stderr=log)

        # TODO: We should check if the process is ready
        time.sleep(3)

        self._server_port = port
        self._server_proxy = xmlrpclib.ServerProxy('http://localhost:{port}/RPC2'.format(port=port))

    def stop_server(self):
        if self._server_process is not None:
            self._server_process.terminate()
            self._server_process.wait()

            self._server_process = None
            self._server_port = None
            self._server_proxy = None

    def translate_document(self, document, output, context=None):
        session = None

        if context is not None:
            session = self.open_session(context)

        with open(document) as source:
            with open(output, 'wb') as dest:
                for line in source:
                    translation = self.translate(line, session=session)['text']
                    dest.write(translation.encode('utf-8'))
                    dest.write('\n')

        if context is not None:
            self.close_session(session)

    def open_session(self, context):
        result = self._server_proxy.translate({
            'text': '',
            'context-weights': ':'.join([k + ',' + str(v) for k, v in context.iteritems()]),
            'session-id': 1
        })

        return int(result['session-id'])

    def close_session(self, session):
        self._server_proxy.close_session({'session-id': session})

    def _escape(self, sentence):
        return sentence.replace('|', '&#124;')

    def translate(self, sentence, context=None, session=None, nbest=None):
        params = {'text': self._escape(sentence)}

        if context is not None:
            params['context-weights'] = ':'.join([k + ',' + str(v) for k, v in context.iteritems()])
        if session is not None:
            params['session-id'] = session
        if nbest is not None:
            params['add-score-breakdown'] = 'true'
            params['nbest-distinct'] = 'true'
            params['nbest'] = nbest

        translation = self._server_proxy.translate(params)
        result = {
            'text': translation['text']
        }

        if 'nbest' in translation:
            result['nbest'] = nbest_list = []

            for element in translation['nbest']:
                nbest_list.append({
                    'text': element['hyp'],
                    'score': element['totalScore'],
                    'fvals': element['fvals']
                })

        return result

    def get_feature_weights(self):
        if self._server_ini_file is None:
            return None

        command = [self._moses_bin, '-f', self._server_ini_file, '-show-weights']
        stdout, _ = shell.execute(command)

        features = []

        for line in stdout.splitlines():
            tokens = line.split()
            feature = {'name': tokens[0].rstrip('=')}

            weights = tokens[1:]
            if not (len(weights) == 1 and weights[0] == 'UNTUNEABLE'):
                feature['weights'] = [float(weight) for weight in weights]

            features.append(feature)

        return features

    def set_feature_weights(self, weights):
        with open(self._server_ini_file, 'wb') as out:
            out.write(self.create_ini(weights))

        self.restart_server()
