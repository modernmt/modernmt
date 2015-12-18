#!/usr/bin/python
import argparse
import json
import os
import sys
import tempfile
import urllib
import urllib2

__author__ = 'Davide Caroselli'


class ApiClass:
    def __init__(self, port):
        self.port = port

    def get(self, endpoint, params=None):
        url = 'http://localhost:{port}/{endpoint}'.format(port=self.port, endpoint=endpoint)

        if params is not None:
            url += '?' + urllib.urlencode(params)

        raw_text = urllib2.urlopen(url).read()
        return json.loads(raw_text)


Api = None


def show_weighs():
    features = Api.get('features')

    for feature in features:
        if 'weights' in feature:
            print feature['name'] + '=', ' '.join([str(w) for w in feature['weights']])
        else:
            print feature['name'], 'UNTUNEABLE'


def _parse_weights(raw):
    result = {}
    array = []

    for token in raw.split():
        if token[-1:] == '=':
            result[token.rstrip('=')] = array = []
        else:
            array.append(float(token))

    return result


def _translate_document(document, tokenized_document, nbest, nbest_file, skip_context, line_id):
    session = None

    if not skip_context:
        session = Api.get('session/open', {
            'document': document
        })['session-id']

    with open(nbest_file, 'ab') as nbest_out:
        with open(tokenized_document) as doc:
            for line in doc:
                translation = Api.get('translate', {
                    'text': line,
                    'processing': 'false',
                    'nbest': nbest
                })

                if session is not None:
                    translation['session'] = session

                print translation['text'].encode('utf-8')
                sys.stdout.flush()

                for nbest_element in translation['nbest']:
                    nbest_out.write(str(line_id))
                    nbest_out.write(' ||| ')
                    nbest_out.write(nbest_element['text'].encode('utf-8'))
                    nbest_out.write(' ||| ')
                    nbest_out.write(nbest_element['fvals'])
                    nbest_out.write(' ||| ')
                    nbest_out.write(str(nbest_element['score']))
                    nbest_out.write('\n')

                line_id += 1

    if session is not None:
        _ = Api.get('session/close', {
            'session-id': session
        })

    return line_id


def translate_merged_corpus(corpus, nbest, nbest_file, weights, skip_context):
    if weights is not None:
        weights = _parse_weights(weights)
        _ = Api.get('weights/set', {
            'w': json.dumps(weights)
        })

    line_id = 0

    with open(corpus) as source:
        for line in source:
            tokenized, original = line.strip().split(':')
            line_id = _translate_document(original, tokenized, nbest, nbest_file, skip_context, line_id)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='MMT engine wrapper for \'mert-moses.pl\' script.')
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

    Api = ApiClass(args.port)

    if args.show_weights:
        show_weighs()
    else:
        skip_context = args.context_analysis is not None
        translate_merged_corpus(args.input_file, int(args.nbest_list[1]), args.nbest_list[0], args.weights,
                                skip_context)
