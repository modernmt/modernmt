import HTMLParser
import json as js
import multiprocessing
import os
import random
import time

import requests

from scripts.libs import multithread, shell

DEFAULT_GOOGLE_KEY = 'AIzaSyBl9WAoivTkEfRdBBSCs4CruwnGL_aV74c'


class TranslateError(Exception):
    def __init__(self, *args, **kwargs):
        super(TranslateError, self).__init__(*args, **kwargs)


class Translator:
    def __init__(self, source_lang, target_lang, threads=1):
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.threads = threads

    def name(self):
        return None

    def __get_timed_translation(self, line, _=None):
        begin = time.time()
        text = self._get_translation(line)
        elapsed = time.time() - begin

        return text, elapsed

    def _before_translate(self, document_path, output_path):
        pass

    def _get_translation(self, line):
        pass

    def _after_translate(self, document_path, output_path):
        pass

    def translate(self, document_path, output_path):
        pool = multithread.Pool(self.threads)

        try:
            jobs = []
            elapsed_total = 0
            translation_count = 0

            self._before_translate(document_path, output_path)

            with open(document_path) as source:
                for line in source:
                    result = pool.apply_async(self.__get_timed_translation, (line, None))
                    jobs.append(result)

            with open(output_path, 'wb') as output:
                for job in jobs:
                    translation, elpased = job.get()

                    output.write(translation.encode('utf-8'))
                    output.write('\n')

                    elapsed_total += elpased
                    translation_count += 1

            return elapsed_total, translation_count
        finally:
            pool.terminate()
            self._after_translate(document_path, output_path)


class HumanEvaluationFileOutputter:
    def __init__(self, separator='\t'):
        self.separator = separator

    def write(self, input_file, output_file, lang):
        line_id = 0

        with open(output_file, 'wb') as out:
            with open(input_file) as inp:
                for line in inp:
                    line = line.lstrip(self.separator)
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
    def __init__(self, server):
        Translator.__init__(self, server.engine.source_lang, server.engine.target_lang,
                            threads=(multiprocessing.cpu_count() * 2))
        self._server = server
        self._session = None

    def name(self):
        return 'MMT'

    def _before_translate(self, document_path, output_path):
        context = self._server.api.get_context_f(document_path)
        self._session = self._server.api.create_session(context)['id']

    def _get_translation(self, line):
        try:
            translation = self._server.api.translate(line, session=self._session, processing=True)
        except Exception as e:
            raise TranslateError(e.message)

        return translation['translation']

    def _after_translate(self, document_path, output_path):
        self._server.api.close_session(self._session)


class GoogleTranslate(Translator):
    def __init__(self, source_lang, target_lang, key=None):
        Translator.__init__(self, source_lang, target_lang, threads=10)
        self._key = key if key is not None else DEFAULT_GOOGLE_KEY
        self._html = HTMLParser.HTMLParser()

    def name(self):
        return 'Google Translate'

    def _get_translation(self, line):
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

        r = requests.post(url, data=data, headers=headers)
        json = r.json()

        if r.status_code != requests.codes.ok:
            message = json['error']['message']
            raise TranslateError('Google Translate query failed with code ' + str(r.status_code) + ': ' + message)

        return self._html.unescape(json['data']['translations'][0]['translatedText'])


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
