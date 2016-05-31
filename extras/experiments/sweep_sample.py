#!/usr/bin/env python

import sys
import os
from os.path import dirname, realpath
sys.path.append(dirname(dirname(dirname(realpath(__file__)))))

from scripts import dependency
from scripts.cluster import ClusterNode, DEFAULT_MMT_API_PORT
from scripts.engine import MMTEngine
from scripts.evaluation import Evaluator
from scripts.mt import ParallelCorpus
from scripts.mt.processing import TrainingPreprocessor
import argparse


class ConfiguredEngine(MMTEngine):
    """
    MMTEngine with calls to ad-hoc reconfigure,
    for tests of several different parameter settings.
    """
    def __init__(self, engine_name=None):
        super(ConfiguredEngine, self).__init__(name=engine_name)
        self._injector = dependency.Injector()
        self._injector.inject(self)
        self._injector.read_config(self.config)  # dummy config access to make it load

    def set(self, section, option, value=None):
        """Only sets values on the engine.config, until write_configs() is called.
        After that, values are valid on the engine itself as well."""

        assert(self.config_option_exists(section, option))

        # coerce all types to str -- because they are parsed back in "ConfigParser.py", line 663, in _interpolate
        self.config.set(section, option, str(value))

    def config_option_exists(self, section, option):
        """check if section and option indeed exist"""
        for clazz in dependency.injectable_components:
            if not hasattr(clazz, 'injectable_fields') or not hasattr(clazz, 'injector_section'):
                continue
            if clazz.injector_section == section and option in clazz.injectable_fields:
                return True
        return False

    def write_configs(self):
        """write engine.ini and moses.ini"""
        self._injector.read_config(self.config)  # so injector params get updated
        self._injector.inject(self)  # so engine instance itself gets updated (goes to moses.ini)
        super(ConfiguredEngine, self).write_configs()  # write engine.ini and moses.ini


class ConfiguredClusterNode(ClusterNode):
    """
    Local ClusterNode with calls to ad-hoc reconfigure,
    for tests of several different parameter settings.
    """
    def __init__(self, engine_name=None):
        super(ConfiguredClusterNode, self).__init__(engine=ConfiguredEngine(engine_name), api_port=DEFAULT_MMT_API_PORT)

    def set(self, section, option, value=None):
        self.engine.set(section, option, value)

    def write_configs(self):
        """Write config to disk without affecting the running node."""
        self.engine.write_configs()

    def apply_configs(self):
        self.write_configs()
        self.restart()

    def restart(self):
        # ensure engine is stopped
        if self.is_running():
            self.stop()

        # start engine again (load up with new config)
        self.start()
        self.wait('READY')


def main_sweep(argv):
    parser = argparse.ArgumentParser(description='Sweep SA sample size and measure BLEU scores at various settings.')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default=None)
    parser.add_argument('--path', dest='corpora_path', metavar='CORPORA', default=None,
                        help='the path to the test corpora (default is the automatically splitted sample)')
    args = parser.parse_args(argv)

    samples = [int(e) for e in '10 20 50 70 80 90 100 110 120 150 200 350 500 800 1000 2000 5000'.split()]

    node = ConfiguredClusterNode(args.engine)

    # more or less copy-pasted from mmt evaluate:

    evaluator = Evaluator(node.engine, node)

    corpora = ParallelCorpus.list(args.corpora_path) if args.corpora_path is not None \
        else ParallelCorpus.list(os.path.join(node.engine.data_path, TrainingPreprocessor.TEST_FOLDER_NAME))

    lines = 0
    for corpus in corpora:
        lines += corpus.count_lines()

    # end copy-paste

    print('sample bleu')

    for sample in samples:
        node.set('suffixarrays', 'sample', sample)
        node.apply_configs()

        scores = evaluator.evaluate(corpora=corpora, google_key='1234', heval_output=None,
                                    use_sessions=True, debug=False)

        engine_scores = scores['MMT']

        if isinstance(engine_scores, str):
            raise RuntimeError(engine_scores)

        bleu = engine_scores['bleu']
        print(sample, '%.2f' % (bleu * 100))


if __name__ == '__main__':
    main_sweep(sys.argv[1:])
