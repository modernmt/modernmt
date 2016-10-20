import sys

import requests

from cli import IllegalArgumentException, IllegalStateException
from cli.libs import multithread

__author__ = 'Davide Caroselli'


class Translator:
    def __init__(self, node, context_string=None, context_file=None, context_weights=None,
                 print_nbest=False, nbest_file=None):
        self._api = node.api
        self._print_nbest = print_nbest

        self._nbest_out = None
        self._nbest_file = None

        if print_nbest:
            self._nbest_out = sys.stdout

            if nbest_file is not None:
                self._nbest_out = self._nbest_file = open(nbest_file, 'wb')

        self._context = None

        if context_string is not None:
            self._context = self._api.get_context_s(context_string)
        elif context_file is not None:
            self._context = self._api.get_context_f(context_file)
        elif context_weights is not None:
            self._context = self.__parse_context_map(context_weights)

        if self._context is not None:
            self._session = self._api.create_session(self._context)['id']
        else:
            self._session = None

    @staticmethod
    def __parse_context_map(text):
        context = []

        try:
            for score in text.split(','):
                name, value = score.split(':', 2)
                value = float(value)

                context.append({
                    'id': name,
                    'score': value
                })
        except ValueError:
            raise IllegalArgumentException('invalid context weights map: ' + text)

        return context

    @staticmethod
    def _encode_translation(translation):
        return translation['translation'].encode('utf-8')

    @staticmethod
    def _encode_nbest(nbest):
        scores = []

        for name, values in nbest['scores'].iteritems():
            scores.append(name + '= ' + ' '.join([str(f) for f in values]))

        return [
            nbest['translation'],
            ' '.join(scores),
            str(nbest['totalScore'])
        ]

    def _translate(self, line, _=None):
        return self._api.translate(line, session=self._session, nbest=self._print_nbest)

    def execute(self, line):
        pass

    def flush(self):
        pass

    def close(self):
        if self._nbest_file is not None:
            self._nbest_file.close()

        try:
            if self._session is not None:
                self._api.close_session(self._session)
        except:
            # Nothing to do
            pass


class BatchTranslator(Translator):
    def __init__(self, node, context_string=None, context_file=None, context_weights=None,
                 print_nbest=False, nbest_file=None):
        Translator.__init__(self, node, context_string, context_file, context_weights, print_nbest, nbest_file)
        self._pool = multithread.Pool(100)
        self._jobs = []

    def execute(self, line):
        result = self._pool.apply_async(self._translate, (line, None))
        self._jobs.append(result)

    def flush(self):
        line_id = 0

        for job in self._jobs:
            translation = job.get()

            print self._encode_translation(translation)

            if self._print_nbest is not None:
                for nbest in translation['nbest']:
                    parts = [str(line_id)] + self._encode_nbest(nbest)
                    self._nbest_out.write((u' ||| '.join(parts)).encode('utf-8'))
                    self._nbest_out.write('\n')

            line_id += 1

    def close(self):
        Translator.close(self)
        self._pool.terminate()


class InteractiveTranslator(Translator):
    def __init__(self, node, context_string=None, context_file=None, context_weights=None,
                 print_nbest=False, nbest_file=None):
        Translator.__init__(self, node, context_string, context_file, context_weights, print_nbest, nbest_file)

        print '\nModernMT Translate command line'

        if self._context:
            norm = sum([e['score'] for e in self._context])
            print '>> Context:', ', '.join(
                ['%s %.f%%' % (score['domain']['name'], round(score['score'] * 100 / norm)) for score in self._context]
            )
        else:
            print '>> No context provided.'

        print

    def execute(self, line):
        if len(line) == 0:
            return

        try:
            translation = self._translate(line)

            if self._print_nbest is not None:
                for nbest in translation['nbest']:
                    self._nbest_out.write((u' ||| '.join(self._encode_nbest(nbest))).encode('utf-8'))
                    self._nbest_out.write('\n')

            print '>>', self._encode_translation(translation)
        except requests.exceptions.ConnectionError:
            raise IllegalStateException('connection problem: MMT server not running, start it with "./mmt start"')
        except requests.exceptions.HTTPError as e:
            raise Exception('HTTP ERROR: ' + e.message)
