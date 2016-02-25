import HTMLParser
import multiprocessing
import os
import json as js
import requests

from scripts.libs import multithread, shell


DEFAULT_GOOGLE_KEY = 'AIzaSyCJGuxxF9fn2ntT5KNNLAamc9GyQ3m2Sfk'


class TranslateError(Exception):
    def __init__(self, *args, **kwargs):
        super(TranslateError, self).__init__(*args, **kwargs)


class Translator:
    def __init__(self, source_lang, target_lang):
        self.source_lang = source_lang
        self.target_lang = target_lang

    def name(self):
        return None

    def translate(self, document_path, output_path):
        pass


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
    def __init__(self, server):
        Translator.__init__(self, server.engine.source_lang, server.engine.target_lang)
        self._server = server

    def name(self):
        return 'MMT'

    def _get_translation(self, line, session):
        try:
            translation = self._server.api.translate(line, session=session, processing=True)
        except Exception as e:
            raise TranslateError(e.message)

        return translation['translation']

    def translate(self, document_path, output_path):
        context = self._server.api.get_context_f(document_path)
        session = self._server.api.create_session(context)['id']

        pool = multithread.Pool(multiprocessing.cpu_count() * 2)

        try:
            jobs = []

            with open(document_path) as source:
                for line in source:
                    result = pool.apply_async(self._get_translation, (line, session))
                    jobs.append(result)

            with open(output_path, 'wb') as output:
                for job in jobs:
                    translation = job.get()
                    output.write(translation.encode('utf-8'))
                    output.write('\n')

            self._server.api.close_session(session)
        finally:
            pool.terminate()


class GoogleTranslate(Translator):
    def __init__(self, source_lang, target_lang, key=None):
        Translator.__init__(self, source_lang, target_lang)
        self._key = key if key is not None else DEFAULT_GOOGLE_KEY

    def name(self):
        return 'Google Translate'

    def _get_translation(self, line, _):
        url = 'https://www.googleapis.com/language/translate/v2'

        data = {
            'source': self.source_lang,
            'target': self.target_lang,
            'q': line,
            'key': self._key
        }

        headers = {
            'X-HTTP-Method-Override': 'GET'
        }

        r = requests.post(url, data=data, headers=headers)
        json = r.json()

        if r.status_code != requests.codes.ok:
            message = json['error']['message']
            raise TranslateError('Google Translate query failed with code ' + str(r.status_code) + ': ' + message)

        return json['data']['translations'][0]['translatedText']

    def translate(self, document_path, output_path):
        pool = multithread.Pool(5)

        try:
            jobs = []

            with open(document_path) as source:
                for line in source:
                    result = pool.apply_async(self._get_translation, (line, None))
                    jobs.append(result)

            html = HTMLParser.HTMLParser()

            with open(output_path, 'wb') as output:
                for job in jobs:
                    translation = job.get()
                    output.write(html.unescape(translation).encode('utf-8'))
                    output.write('\n')
        finally:
            pool.terminate()


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
    def _read_lines(document, reference, limit=11):
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
