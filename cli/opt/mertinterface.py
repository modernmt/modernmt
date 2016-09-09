#!/usr/bin/env python
import argparse
import collections
import os
import sys
import time

sys.path.insert(0, os.path.abspath(os.path.join(__file__, os.pardir, os.pardir, os.pardir)))

from cli.cluster import MMTApi
from cli.libs import multithread
from cli.mt.processing import Tokenizer

__author__ = 'Davide Caroselli'


def _sorted_features_list(features=None):
    if features is None:
        features = Api.get_features()

    l = [str(f) for f, _ in features.iteritems()]
    l.sort()
    return l


class _DocumentTranslator:
    def __init__(self, corpus, nbest, nbest_file, workers=100):
        self.corpus = corpus
        self.nbest = nbest
        self.nbest_file = nbest_file

        self.weights = None
        self.skip_context = False
        self._line_id = 0

        self._pool = multithread.Pool(workers)
        self._features = None

        self._tokenizer = Tokenizer()

    def set_skipcontext(self, skip_context):
        self.skip_context = skip_context

    def set_weights(self, raw):
        if raw is None:
            self.weights = None
            return

        self.weights = {}
        array = []

        for token in raw.split():
            if token[-1:] == '=':
                self.weights[token.rstrip('=')] = array = []
            else:
                array.append(float(token))

    def _get_translation(self, line, nbest, session):
        translation = Api.translate(line, session=session, nbest=nbest)

        # tokenize
        translation['translation'] = self._tokenizer.process(translation['translation'])
        for hyp in translation['nbest']:
            hyp['translation'] = self._tokenizer.process(hyp['translation'])

        return translation

    def _print(self, translation, nbest_out):
        print translation['translation'].encode('utf-8')
        sys.stdout.flush()

        for hyp in translation['nbest']:
            scores = []

            for feature in self._features:
                if feature in hyp['scores']:
                    scores.append(feature + '=')
                    for s in hyp['scores'][feature]:
                        scores.append(str(s))

            nbest_out.write(str(self._line_id))
            nbest_out.write(' ||| ')
            nbest_out.write(hyp['translation'].encode('utf-8'))
            nbest_out.write(' ||| ')
            nbest_out.write(' '.join(scores))
            nbest_out.write(' ||| ')
            nbest_out.write(str(hyp['totalScore']))
            nbest_out.write('\n')

    def run(self):
        self._line_id = 0

        try:
            if self.weights is not None:
                Api.update_features(self.weights)
            time.sleep(1)

            self._features = _sorted_features_list()

            translations = []
            sessions = []

            # Enqueue translations requests
            with open(self.corpus) as source:
                for line in source:
                    corpus_path = line.strip()

                    session = None

                    if not self.skip_context:
                        context = Api.get_context_f(corpus_path)
                        session = Api.create_session(context)['id']
                        sessions.append(session)

                    with open(corpus_path) as doc:
                        for docline in doc:
                            translation = self._pool.apply_async(self._get_translation, (docline, self.nbest, session))
                            translations.append(translation)

            # Collection and outputting results
            with open(self.nbest_file, 'ab') as nbest_out:
                for translation_job in translations:
                    translation = translation_job.get()
                    self._print(translation, nbest_out)
                    self._line_id += 1

            # Closing sessions
            for session in sessions:
                try:
                    Api.close_session(session)
                except:
                    # ignore it
                    pass
        finally:
            self._pool.terminate()


def show_weighs():
    features_map = Api.get_features()
    features = _sorted_features_list(features_map)

    for feature in features:
        weights = features_map[feature]
        if weights is not None and isinstance(weights, collections.Iterable):
            print feature + '=', ' '.join([str(w) for w in weights])
        else:
            print feature, 'UNTUNEABLE'


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='MMT Server wrapper script for \'mert-moses.pl\' script.')
    parser.add_argument('--host', '-H', dest='host', help 'MMT engine host(:port)', default='localhost')
    parser.add_argument('--port', '-p', dest='port', type=int, help='MMT engine port')
    parser.add_argument('--skip-context-analysis', dest='context_analysis', type=int,
                        help='if present, skip context analysis')
    parser.add_argument('-show-weights', dest='show_weights', action='store_true')
    parser.add_argument('-weight-overwrite', dest='weights', nargs='?')

    parser.add_argument('-n-best-list', dest='nbest_list', nargs=3)
    parser.add_argument('-input-file', dest='input_file')

    realargv = []
    for arg in sys.argv:
        if arg.startswith('--'):
            realargv += arg.split()
        else:
            realargv.append(arg)

    args, _ = parser.parse_known_args(realargv)

    if len(sys.argv) == 1 or args.port is None:
        parser.print_help()
        exit(1)

    Api = MMTApi(host=args.host, port=args.port)

    if args.show_weights:
        # Show weights
        show_weighs()
    else:
        translator = _DocumentTranslator(args.input_file, int(args.nbest_list[1]), args.nbest_list[0])
        translator.set_weights(args.weights)

        if args.context_analysis is not None:
            translator.set_skipcontext(True)

        translator.run()
