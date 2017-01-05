import os

__author__ = 'Davide Caroselli'


class MosesFeature:
    def __init__(self, classname):
        self.classname = classname

    def get_relpath(self, base_path, path):
        path = os.path.abspath(path)
        base_path = os.path.abspath(base_path)

        path = path.replace(base_path, '').lstrip(os.sep)

        return '${DECODER_PATH}' + path

    def get_iniline(self, base_path):
        return None


class Moses:
    injector_section = 'moses'
    injectable_fields = {
        'stack_size': ('search algorithm stack size', int, 1000),
        'cube_pruning_pop_limit': ('pop limit of cube pruning algorithm', int, 1000),
        'distortion_limit': ('distortion limit', int, 6),
    }

    def __init__(self, model_path):
        self._path = model_path

        self._stack_size = None  # Injected
        self._cube_pruning_pop_limit = None  # Injected
        self._distortion_limit = None  # Injected

        self._ini_file = os.path.join(self._path, 'moses.ini')
        self._weights_file = os.path.join(self._path, 'weights.dat')

        self._server_process = None
        self._server_port = None
        self._server_proxy = None
        self._server_log_file = None

        self._features = []

        self._optimal_weights = {
            'InterpolatedLM': [0.0883718],
            'Sapt': [0.0277399, 0.0391562, 0.00424704, 0.0121731],
            'DM0': [0.0153337, 0.0181129, 0.0423417, 0.0203163, 0.261833, 0.126704, 0.0670114, 0.0300892],
            'Distortion0': [0.0335557],
            'WordPenalty0': [-0.0750738],
            'PhrasePenalty0': [-0.13794],
        }

    def add_feature(self, feature, name=None):
        self._features.append((feature, name))

    def __get_iniline(self, feature, name):
        custom = feature.get_iniline(self._path)
        line = feature.classname

        if name is not None:
            line += ' name=' + name

        if custom is not None:
            line += ' ' + custom

        return line

    def create_configs(self):
        self.__create_ini()
        self.__store_default_weights(self._optimal_weights)

    def __create_ini(self):
        lines = ['[input-factors]', '0', '', '[search-algorithm]', '1', '', '[stack]', str(self._stack_size), '',
                 '[cube-pruning-pop-limit]', str(self._cube_pruning_pop_limit), '', '[mapping]', '0 T 0', '',
                 '[distortion-limit]', str(self._distortion_limit), '', '[threads]', '${DECODER_THREADS}', '',
                 '[verbose]', '0', '', '[feature]']

        for feature in self._features:
            lines.append(self.__get_iniline(*feature))
        lines.append('')

        with open(self._ini_file, 'wb') as out:
            out.write('\n'.join(lines))

    def __store_default_weights(self, weights):
        lines = [('%s = %s\n' % (key, ' '.join([str(v) for v in value]))) for key, value in weights.iteritems()]

        with open(self._weights_file, 'wb') as out:
            out.writelines(lines)
