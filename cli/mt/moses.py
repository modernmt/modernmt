import os

import cli

__author__ = 'Davide Caroselli'


class MosesFeature:
    def __init__(self, classname):
        self.classname = classname

    def get_relpath(self, path):
        path = os.path.abspath(path)
        root = os.path.abspath(cli.ENGINES_DIR)
        path = path.replace(root, '').lstrip(os.sep)

        path = os.sep.join(path.split(os.sep)[1:])

        return '${ENGINE_PATH}' + path

    def get_iniline(self):
        return None


class LexicalReordering(MosesFeature):
    def __init__(self):
        MosesFeature.__init__(self, 'LexicalReordering')

    def get_iniline(self):
        return 'input-factor=0 output-factor=0 type=hier-mslr-bidirectional-fe-allff'


class Moses:
    injector_section = 'moses'
    injectable_fields = {
        'stack_size': ('search algorithm stack size', int, 1000),
        'cube_pruning_pop_limit': ('pop limit of cube pruning algorithm', int, 1000),
        'distortion_limit': ('distortion limit', int, 6),
    }

    def __init__(self, ini_file):
        self._stack_size = None  # Injected
        self._cube_pruning_pop_limit = None  # Injected
        self._distortion_limit = None  # Injected

        self._ini_file = ini_file

        self._server_process = None
        self._server_port = None
        self._server_proxy = None
        self._server_log_file = None

        self._features = []

    def add_feature(self, feature, name=None):
        self._features.append((feature, name))

    def __get_iniline(self, feature, name):
        custom = feature.get_iniline()
        line = feature.classname

        if name is not None:
            line += ' name=' + name

        if custom is not None:
            line += ' ' + custom

        return line

    def create_ini(self):
        lines = ['[input-factors]', '0', '', '[search-algorithm]', '1', '', '[stack]', str(self._stack_size), '',
                 '[cube-pruning-pop-limit]', str(self._cube_pruning_pop_limit), '', '[mapping]', '0 T 0', '',
                 '[distortion-limit]', str(self._distortion_limit), '', '[threads]', '${DECODER_THREADS}', '',
                 '[verbose]', '0', '', '[feature]']

        for feature in self._features:
            lines.append(self.__get_iniline(*feature))
        lines.append('')

        with open(self._ini_file, 'wb') as out:
            out.write('\n'.join(lines))
