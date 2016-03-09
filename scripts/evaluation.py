import HTMLParser
import json as js
import os
import random
import time

import requests

from scripts.libs import multithread, shell
from scripts.mt import ParallelCorpus

DEFAULT_GOOGLE_KEY = 'AIzaSyBl9WAoivTkEfRdBBSCs4CruwnGL_aV74c'


class TranslateError(Exception):
    def __init__(self, *args, **kwargs):
        super(TranslateError, self).__init__(*args, **kwargs)


class Translator:
    def __init__(self, source_lang, target_lang, threads=1):
        self.source_lang = source_lang
        self.target_lang = target_lang
        self._threads = threads

    def name(self):
        return None

    def _before_translate(self, corpus):
        pass

    def _get_translation(self, line, corpus):
        pass

    def _after_translate(self, corpus):
        pass

    def translate(self, corpora, output):
        pool = multithread.Pool(self._threads)

        try:
            translations = []

            for corpus in corpora:
                self._before_translate(corpus)

                with open(corpus.get_file(self.source_lang)) as source:
                    output_path = os.path.join(output, corpus.name + '.' + self.target_lang)

                    for line in source:
                        translation = pool.apply_async(self._get_translation, (line, corpus))
                        translations.append((translation, output_path))

                self._after_translate(corpus)

            elapsed_time = 0
            translation_count = 0

            path = None
            stream = None

            for translation_job, output_path in translations:
                translation, elapsed = translation_job.get()

                if output_path != path:
                    if stream is not None:
                        stream.close()

                    stream = open(output_path, 'wb')
                    path = output_path

                stream.write(translation.encode('utf-8'))
                stream.write('\n')

                elapsed_time += elapsed
                translation_count += 1

            if stream is not None:
                stream.close()

            return ParallelCorpus.list(output), (elapsed_time / translation_count)
        finally:
            pool.terminate()


class HumanEvaluationFileOutputter:
    def __init__(self, separator='\t'):
        self.separator = separator

    def write(self, input_file, output_file, lang):
        line_id = 0

        with open(output_file, 'wb') as out:
            with open(input_file) as inp:
                for line in inp:
                    line = line.replace(self.separator, ' ')
                    lid = str(line_id)
                    line_id += 1
                    out.write(self.separator.join([lid, lang, line]))


class Score:
    def __init__(self):
        pass

    def name(self):
        pass

    def calculate(self, corpora, references):
        pass


class BingTranslator(Translator):
    def __init__(self, source_lang, target_lang, key=None):
        Translator.__init__(self, source_lang, target_lang)

    def name(self):
        return 'Bing Translator'

    def translate(self, document_path, output_path):
        raise TranslateError('Coming in next MMT release')


class MMTTranslator(Translator):
    def __init__(self, server, use_sessions=True):
        Translator.__init__(self, server.engine.source_lang, server.engine.target_lang, threads=100)
        self._server = server
        self._sessions = {}
        self._context = None  # redundant with the session, stored just for the case of _use_sessions=False
        self._use_sessions = use_sessions

    def name(self):
        return 'MMT'

    def translate(self, corpora, output):
        result = Translator.translate(self, corpora, output)

        for _, session in self._sessions.iteritems():
            self._server.api.close_session(session)

        return result

    def _before_translate(self, corpus):
        corpus_file = corpus.get_file(self.source_lang)
        self._context = self._server.api.get_context_f(corpus_file)
        if self._use_sessions:
            self._sessions[corpus_file] = self._server.api.create_session(self._context)['id']

    def _get_translation(self, line, corpus):
        corpus_file = corpus.get_file(self.source_lang)

        try:
            # use per-session context (not passed) if _use_sessions
            # pass context here (and do not pass session) otherwise
            sess = self._sessions[corpus_file] if self._use_sessions else None
            ctxt = None if self._use_sessions else self._context

            translation = self._server.api.translate(line, session=sess, context=ctxt, processing=True)
        except Exception as e:
            raise TranslateError(e.message)

        text = translation['translation']
        took = float(translation['took']) / 1000.

        return text, took


class GoogleTranslate(Translator):
    def __init__(self, source_lang, target_lang, key=None):
        Translator.__init__(self, source_lang, target_lang, threads=10)
        self._key = key if key is not None else DEFAULT_GOOGLE_KEY
        self._html = HTMLParser.HTMLParser()

    def name(self):
        return 'Google Translate'

    def _get_translation(self, line, corpus):
        url = 'https://www.googleapis.com/language/translate/v2'

        data = {
            'source': self.source_lang,
            'target': self.target_lang,
            'q': line,
            'key': self._key,
            'userip': '.'.join(map(str, (random.randint(0, 200) for _ in range(4))))
        }

        headers = {
            'X-HTTP-Method-Override': 'GET'
        }

        begin = time.time()
        r = requests.post(url, data=data, headers=headers)
        elapsed = time.time() - begin

        json = r.json()

        if r.status_code != requests.codes.ok:
            message = json['error']['message']
            raise TranslateError('Google Translate query failed with code ' + str(r.status_code) + ': ' + message)

        text = self._html.unescape(json['data']['translations'][0]['translatedText'])

        return text, elapsed


class BLEUScore(Score):
    def __init__(self):
        Score.__init__(self)

    def name(self):
        return 'BLEU'

    def calculate(self, document, reference):
        script = os.path.abspath(os.path.join(__file__, os.pardir, 'opt', 'multi-bleu.perl'))
        command = ['perl', script, reference]

        with open(document) as input_stream:
            stdout, _ = shell.execute(command, stdin=input_stream)

        return float(stdout)


class MatecatScore(Score):
    def __init__(self):
        Score.__init__(self)

    def name(self):
        return 'Matecat'

    @staticmethod
    def _get_score(sentences, references):
        url = 'http://api.mymemory.translated.net/computeMatch.php'

        data = {
            'sentences': sentences,
            'reference_sentences': references
        }

        r = requests.post(url, data=js.dumps(data), headers={'Content-type': 'application/json'})
        body = r.json()

        if r.status_code != requests.codes.ok:
            raise TranslateError('Matecat Score service not available (' + str(r.status_code) + '): ' + body['error'])

        return body

    @staticmethod
    def _read_lines(document, reference, limit=30):
        reference_lines = []
        document_lines = []

        for i in range(0, limit):
            reference_line = reference.readline()
            document_line = document.readline()

            if (not reference_line) or (not document_line):
                break

            reference_lines.append(reference_line)
            document_lines.append(document_line)

        return (document_lines, reference_lines) if len(reference_lines) > 0 else (None, None)

    def calculate(self, document, reference):
        scores = []

        with open(reference) as reference_input:
            with open(document) as document_input:
                while True:
                    document_lines, reference_lines = self._read_lines(document_input, reference_input)

                    if document_lines is None:
                        break

                    scores += self._get_score(document_lines, reference_lines)

        return reduce(lambda x, y: x + y, scores) / len(scores)
