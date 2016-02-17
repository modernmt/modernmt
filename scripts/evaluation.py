import multiprocessing

import requests

from scripts.libs import multithread


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


class MMTTranslator(Translator):
    def __init__(self, server):
        Translator.__init__(self, server.engine.source_lang, server.engine.target_lang)
        self._server = server

    def name(self):
        return 'ModernMT'

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
    def __init__(self, source_lang, target_lang, key='AIzaSyCJGuxxF9fn2ntT5KNNLAamc9GyQ3m2Sfk'):
        Translator.__init__(self, source_lang, target_lang)
        self._key = key

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

            with open(output_path, 'wb') as output:
                for job in jobs:
                    translation = job.get()
                    output.write(translation.encode('utf-8'))
                    output.write('\n')
        finally:
            pool.terminate()


class BLEUScore(Score):
    def __init__(self, lowercase=False):
        Score.__init__(self)

    def name(self):
        return 'BLEU'

    def calculate(self, corpora, references):
        pass
