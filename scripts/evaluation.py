import json as js
import os
import random
import time

from datetime import datetime
import requests

from scripts import IllegalArgumentException
from scripts.libs import multithread, shell, fileutils
from scripts.mt import ParallelCorpus
from scripts.mt.processing import XMLEncoder

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
        """
        Translate the given corpora in parallel processing fashion.
        :param corpora: list of ParallelCorpus
        :param output:  path to output directory
        :return: ([ParallelCorpus, ...], time_per_sentence, parallelism)
        """
        pool = multithread.Pool(self._threads)

        try:
            translations = []
            start_time = datetime.now()

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

            end_time = datetime.now()
            total_time = end_time - start_time

            return ParallelCorpus.list(output), (elapsed_time / translation_count), (
                elapsed_time / total_time.total_seconds())
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
    def __init__(self, node, use_sessions=True):
        Translator.__init__(self, node.engine.source_lang, node.engine.target_lang, threads=100)
        self._api = node.api
        self._sessions = {}
        self._contexts = {}  # redundant with the sessions, stored just for the case of _use_sessions=False
        self._use_sessions = use_sessions

    def name(self):
        return 'MMT'

    def translate(self, corpora, output):
        result = Translator.translate(self, corpora, output)

        for _, session in self._sessions.iteritems():
            self._api.close_session(session)

        return result

    def _before_translate(self, corpus):
        try:
            corpus_file = corpus.get_file(self.source_lang)
            context = self._api.get_context_f(corpus_file)
            self._contexts[corpus_file] = context
            if self._use_sessions:
                self._sessions[corpus_file] = self._api.create_session(context)['id']
        except requests.exceptions.ConnectionError:
            raise TranslateError('Unable to connect to MMT. '
                                 'Please check if engine is running on port %d.' % self._api.port)
        except Exception as e:
            raise TranslateError(e.message)

    def _get_translation(self, line, corpus):
        corpus_file = corpus.get_file(self.source_lang)

        try:
            # use per-session context (not passed) if _use_sessions
            # pass context here (and do not pass session) otherwise
            sess = self._sessions[corpus_file] if self._use_sessions else None
            ctxt = None if self._use_sessions else self._contexts[corpus_file]

            translation = self._api.translate(line, session=sess, context=ctxt, processing=True)
        except requests.exceptions.ConnectionError:
            raise TranslateError('Unable to connect to MMT. '
                                 'Please check if engine is running on port %d.' % self._api.port)
        except Exception as e:
            raise TranslateError(e.message)

        text = translation['translation']
        decoding_time = float(translation['decodingTime']) / 1000.

        return text, decoding_time


class GoogleTranslate(Translator):
    def __init__(self, source_lang, target_lang, key=None):
        Translator.__init__(self, source_lang, target_lang, threads=10)
        self._key = key if key is not None else DEFAULT_GOOGLE_KEY

    def name(self):
        return 'Google Translate'

    @staticmethod
    def _pack_error(request):
        json = request.json()

        daily_limit = False

        if request.status_code == 403:
            for error in json['error']['errors']:
                if error['reason'] == 'dailyLimitExceeded':
                    daily_limit = True
                    break

        if daily_limit:
            return TranslateError('Google Translate free quota is over. Please use option --gt-key'
                                  ' to specify your GT API key.')
        else:
            return TranslateError('Google Translate error (%d): %s' % (request.status_code, json['error']['message']))

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

        if r.status_code != requests.codes.ok:
            raise self._pack_error(r)

        text = r.json()['data']['translations'][0]['translatedText']
        return text, elapsed


class BLEUScore(Score):
    def __init__(self):
        Score.__init__(self)

    def name(self):
        return 'BLEU Score'

    def calculate(self, document, reference):
        script = os.path.abspath(os.path.join(__file__, os.pardir, 'opt', 'mmt-bleu.perl'))
        command = ['perl', script, reference]

        with open(document) as input_stream:
            stdout, _ = shell.execute(command, stdin=input_stream)

        return float(stdout)


class MatecatScore(Score):
    def __init__(self):
        Score.__init__(self)

    def name(self):
        return 'Matecat Post-Editing Score'

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


class _evaluate_logger:
    def __init__(self, line_len=70):
        self._step = None
        self._line_len = line_len

    def start(self, corpora):
        lines = 0
        for corpus in corpora:
            lines += corpus.count_lines()

        print '\n============== EVALUATION ==============\n'
        print 'Testing on %d lines:\n' % lines

    def step(self, step):
        self._step = step
        return self

    def __enter__(self):
        message = '%s... ' % self._step
        print message.ljust(self._line_len),

        self._start_time = time.time()
        return self

    def __exit__(self, *_):
        self._end_time = time.time()
        print 'DONE (in %ds)' % int(self._end_time - self._start_time)

    def completed(self, results, scorers):
        print '\n=============== RESULTS ================\n'

        for scorer, field in scorers:
            scores = sorted(results, key=lambda r: getattr(r, field) if r.error is None else 0, reverse=True)

            print scorer.name() + ':'
            for i in range(0, len(scores)):
                result = scores[i]

                if result.error is None:
                    text = '%.2f' % (getattr(result, field) * 100)
                    if i == 0:
                        text += ' (Winner)'
                else:
                    text = str(result.error)

                print '  %s: %s' % (result.translator.name().ljust(20), text)
            print

        sorted_by_mtt = sorted(results, key=lambda r: r.mtt if r.error is None else float('inf'), reverse=False)

        print 'Translation Speed:'
        for i in range(0, len(sorted_by_mtt)):
            result = sorted_by_mtt[i]

            if result.error is None:
                text = '%.2fs per sentence (parallelism %.1fx)' % (result.mtt, result.parallelism)
            else:
                text = str(result.error)

            print '  %s: %s' % (result.translator.name().ljust(20), text)
        print


class _EvaluationResult:
    def __init__(self, translator):
        self.id = translator.name().replace(' ', '_')
        self.translator = translator

        self.merge = None
        self.translated_corpora = None
        self.mtt = None
        self.parallelism = None
        self.error = None
        self.bleu = None
        self.pes = None


class Evaluator:
    def __init__(self, node, google_key=None, use_sessions=True):
        self._engine = node.engine
        self._node = node

        self._heval_outputter = HumanEvaluationFileOutputter()
        self._xmlencoder = XMLEncoder()
        self._translators = [
            GoogleTranslate(self._engine.source_lang, self._engine.target_lang, key=google_key),
            # BingTranslator(source_lang, target_lang),
            MMTTranslator(self._node, use_sessions)
        ]

    def evaluate(self, corpora, heval_output=None, debug=False):
        if len(corpora) == 0:
            raise IllegalArgumentException('empty corpora')
        if heval_output is not None:
            fileutils.makedirs(heval_output, exist_ok=True)

        target_lang = self._engine.target_lang
        source_lang = self._engine.source_lang

        logger = _evaluate_logger()
        logger.start(corpora)

        working_dir = self._engine.get_tempdir('evaluation')

        try:
            results = []

            # Process references
            with logger.step('Preparing corpora') as _:
                corpora_path = os.path.join(working_dir, 'corpora')
                corpora = self._xmlencoder.encode(corpora, corpora_path)

                reference = os.path.join(working_dir, 'reference.' + target_lang)
                source = os.path.join(working_dir, 'source.' + source_lang)
                fileutils.merge([corpus.get_file(target_lang) for corpus in corpora], reference)
                fileutils.merge([corpus.get_file(source_lang) for corpus in corpora], source)

                if heval_output is not None:
                    self._heval_outputter.write(lang=target_lang, input_file=reference,
                                                output_file=os.path.join(heval_output, 'reference.' + target_lang))
                    self._heval_outputter.write(lang=source_lang, input_file=source,
                                                output_file=os.path.join(heval_output, 'source.' + source_lang))

            # Translate
            for translator in self._translators:
                name = translator.name()

                with logger.step('Translating with %s' % name) as _:
                    result = _EvaluationResult(translator)
                    results.append(result)

                    translations_path = os.path.join(working_dir, 'translations', result.id + '.raw')
                    xmltranslations_path = os.path.join(working_dir, 'translations', result.id)
                    fileutils.makedirs(translations_path, exist_ok=True)

                    try:
                        translated, mtt, parallelism = translator.translate(corpora, translations_path)
                        filename = result.id + '.' + target_lang

                        result.mtt = mtt
                        result.parallelism = parallelism
                        result.translated_corpora = self._xmlencoder.encode(translated, xmltranslations_path)
                        result.merge = os.path.join(working_dir, filename)

                        fileutils.merge([corpus.get_file(target_lang)
                                         for corpus in result.translated_corpora], result.merge)

                        if heval_output is not None:
                            self._heval_outputter.write(lang=target_lang, input_file=result.merge,
                                                        output_file=os.path.join(heval_output, filename))
                    except TranslateError as e:
                        result.error = e
                    except Exception as e:
                        result.error = TranslateError('Unexpected ERROR: ' + str(e.message))

            # Scoring
            scorers = [(MatecatScore(), 'pes'), (BLEUScore(), 'bleu')]

            for scorer, field in scorers:
                with logger.step('Calculating %s' % scorer.name()) as _:
                    for result in results:
                        if result.error is not None:
                            continue
                        setattr(result, field, scorer.calculate(result.merge, reference))

            logger.completed(results, scorers)
        finally:
            if not debug:
                self._engine.clear_tempdir('evaluation')
