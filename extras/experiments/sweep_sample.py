#!/usr/bin/env python

import sys
import os
from os.path import dirname, realpath
sys.path.append(dirname(dirname(dirname(realpath(__file__)))))

from cli.evaluation import Evaluator
from cli.mt import BilingualCorpus
from cli.mt.processing import TrainingPreprocessor
from cli.engine import ConfiguredClusterNode
import argparse


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

    evaluator = Evaluator(node, google_key='1234', use_sessions=True)

    corpora = BilingualCorpus.list(args.corpora_path) if args.corpora_path is not None \
        else BilingualCorpus.list(os.path.join(node.engine.data_path, TrainingPreprocessor.TEST_FOLDER_NAME))

    lines = 0
    for corpus in corpora:
        lines += corpus.count_lines()

    # end copy-paste

    print('sample bleu')

    for sample in samples:
        node.set('suffixarrays', 'sample', sample)
        node.apply_configs()

        scores = evaluator.evaluate(corpora=corpora, heval_output=None,
                                    debug=False)

        engine_scores = [r for r in scores if r.id == 'MMT'][0]

        if engine_scores.error:
            raise RuntimeError(engine_scores.error)

        bleu = engine_scores.bleu
        print(sample, '%.2f' % (bleu * 100))


if __name__ == '__main__':
    main_sweep(sys.argv[1:])
