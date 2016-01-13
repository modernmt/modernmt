import os

import scripts

__author__ = 'Davide Caroselli'


class MosesFeature:
    def __init__(self, name):
        self.name = name

    def get_relpath(self, path):
        path = os.path.abspath(path)
        root = os.path.abspath(scripts.ENGINES_DIR)
        path = path.replace(root, '').lstrip(os.sep)

        path = os.sep.join(path.split(os.sep)[1:])

        return '${ENGINE_PATH}' + path

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
        'threads': ('decoder threads', int, None)
    }

    bin_path = os.path.join(scripts.BIN_DIR, 'moses-mmt-dev_4a82__6baa')

    def __init__(self, ini_file):
        self._stack_size = None  # Injected
        self._cube_pruning_pop_limit = None  # Injected
        self._distortion_limit = None  # Injected
        self._threads = None  # Injected

        self._moses_bin = os.path.join(Moses.bin_path, 'bin', 'moses')
        self._ini_file = ini_file

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
                 '[distortion-limit]', str(self._distortion_limit), '', '[threads]', '${DECODER_THREADS}', '', '[v]',
                 '0', '', '[feature]']

        for feature in self._features:
            lines.append(feature.get_iniline())
        lines.append('')

        if weights is not None:
            lines.append('[weight]')

            for feature, w in weights.iteritems():
                lines.append(' '.join([feature + '='] + [str(el) for el in w]))

        lines.append('')

        with open(self._ini_file, 'wb') as out:
            out.write('\n'.join(lines))
