import json as js
import os
import time

import requests

import cli
from cli import IllegalArgumentException
from cli.libs import osutils
from cli.mmt.processing import XMLEncoder
from cli.translators import GoogleTranslate, MMTTranslator, TranslateError


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
                    line = self.separator.join([lid, lang, line.decode('utf-8')])
                    out.write(line.encode('utf-8'))


class Score(object):
    def __init__(self):
        pass

    def name(self):
        raise NotImplementedError

    def calculate(self, corpora, references):
        raise NotImplementedError

    def __eq__(self, o):
        return super(Score, self).__eq__(o)

    def __hash__(self):
        return hash(self.name())


class BLEUScore(Score):
    def __init__(self):
        Score.__init__(self)

    def name(self):
        return 'BLEU Score'

    def calculate(self, document, reference):
        script = os.path.join(cli.PYOPT_DIR, 'mmt-bleu.perl')
        command = ['perl', script, reference]

        with open(document) as input_stream:
            stdout, _ = osutils.shell_exec(command, stdin=input_stream)

        return float(stdout)


class CharCutScore(Score):
    def __init__(self):
        Score.__init__(self)

    def name(self):
        return 'CharCut Accuracy Score'

    def calculate(self, document, reference):
        script = os.path.join(cli.PYOPT_DIR, 'charcut.py')
        command = ['python', script, '-c', '/dev/stdin', '-r', reference]

        with open(document) as input_stream:
            stdout, _ = osutils.shell_exec(command, stdin=input_stream)

        return 1.0 - float(stdout)


class MatecatScore(Score):
    DEFAULT_TIMEOUT = 60  # secs

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

        r = requests.post(url, data=js.dumps(data), headers={'Content-type': 'application/json'},
                          timeout=MatecatScore.DEFAULT_TIMEOUT)
        body = r.json()

        if r.status_code != requests.codes.ok:
            err = body['error'] if isinstance(body, dict) and 'error' in body else 'unknown'
            raise requests.RequestException('Matecat Score service not available (%d): %s' % (r.status_code, err))

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


class _StepLogger:
    def __init__(self, line_len=70):
        self._step = None
        self._line_len = line_len

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


class Evaluator:
    class _Entry:
        def __init__(self, translator):
            self.id = translator.name.replace(' ', '_')
            self.translator = translator
            self.translation_file = None
            self.translation_time = None
            self.error = None
            self.scores = {}

    def __init__(self, node, source_lang, target_lang, google_key=None):
        self._engine = node.engine
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._translators = [GoogleTranslate(self._source_lang, self._target_lang, key=google_key),
                             MMTTranslator(node, self._source_lang, self._target_lang)]
        self._scorers = [MatecatScore(), BLEUScore()]

        try:
            import regex
            self._scorers.append(CharCutScore())
        except ImportError:
            pass

    def evaluate(self, corpora, heval_output=None, debug=False):
        corpora = [corpus for corpus in corpora
                   if self._source_lang in corpus.langs and self._target_lang in corpus.langs]
        if len(corpora) == 0:
            raise IllegalArgumentException(
                'No %s > %s corpora found into specified path' % (self._source_lang, self._target_lang))

        print '\n============== EVALUATION ==============\n'
        print 'Testing on %d lines:\n' % sum([corpus.count_lines() for corpus in corpora])

        if heval_output is not None:
            osutils.makedirs(heval_output, exist_ok=True)

        step_logger = _StepLogger()
        human_eval_outputter = HumanEvaluationFileOutputter() if heval_output is not None else None

        working_dir = self._engine.get_tempdir('evaluation')

        try:
            # Process references
            with step_logger.step('Preparing corpora') as _:
                source = os.path.join(working_dir, 'source.' + self._source_lang)
                osutils.concat([corpus.get_file(self._source_lang) for corpus in corpora], source)

                reference = os.path.join(working_dir, 'reference.' + self._target_lang)
                osutils.concat([corpus.get_file(self._target_lang) for corpus in corpora], reference + '.tmp')
                XMLEncoder().encode_file(reference + '.tmp', reference)
                os.remove(reference + '.tmp')

                if human_eval_outputter is not None:
                    human_eval_outputter.write(lang=self._target_lang, input_file=reference,
                                               output_file=os.path.join(heval_output,
                                                                        'reference.' + self._target_lang))
                    human_eval_outputter.write(lang=self._source_lang, input_file=source,
                                               output_file=os.path.join(heval_output, 'source.' + self._source_lang))

                total_line_count = osutils.lc(reference)

            # Translate
            entries = []
            for translator in self._translators:
                with step_logger.step('Translating with %s' % translator.name) as _:
                    entry = self._translate_with(translator, corpora, working_dir, total_line_count)
                    entries.append(entry)

                    if entry.error is None and human_eval_outputter is not None:
                        human_eval_file = os.path.join(heval_output, os.path.basename(entry.translation_file))
                        human_eval_outputter.write(lang=self._target_lang, input_file=entry.translation_file,
                                                   output_file=human_eval_file)

            # Scoring
            for scorer in self._scorers:
                with step_logger.step('Calculating %s' % scorer.name()) as _:
                    for entry in entries:
                        if entry.error is not None:
                            continue
                        try:
                            entry.scores[scorer] = scorer.calculate(entry.translation_file, reference)
                        except Exception as e:
                            entry.scores[scorer] = str(e)

            # Print results
            print '\n=============== RESULTS ================\n'

            for scorer in self._scorers:
                print scorer.name() + ':'

                for i, entry in enumerate(sorted(entries, key=lambda x: x.scores[scorer] if x.error is None else 0,
                                                 reverse=True)):
                    if entry.error is None:
                        value = entry.scores[scorer]
                        if isinstance(value, basestring):
                            text = value
                        else:
                            text = '%.2f' % (value * 100)
                            if i == 0:
                                text += ' (Winner)'
                    else:
                        text = str(entry.error)

                    print '  %s: %s' % (entry.translator.name.ljust(20), text)
                print

            print 'Translation Speed:'
            for entry in sorted(entries, key=lambda x: x.translation_time if x.error is None else float('inf')):
                if entry.error is None:
                    text = '%.2fs per sentence' % entry.translation_time
                else:
                    text = str(entry.error)

                print '  %s: %s' % (entry.translator.name.ljust(20), text)
            print
        finally:
            if not debug:
                self._engine.clear_tempdir('evaluation')

    def _translate_with(self, translator, corpora, working_dir, expected_segments):
        result = self._Entry(translator)

        translations_path = os.path.join(working_dir, 'translations', result.id)
        osutils.makedirs(translations_path, exist_ok=True)

        try:
            begin_time = time.time()
            segments_count = translator.translate_corpora(corpora, translations_path)
            result.translation_time = (time.time() - begin_time) / float(segments_count)

            if expected_segments != segments_count:
                raise TranslateError('Invalid line count for translator %s: expected %d, found %d.'
                                     % (translator.name, expected_segments, segments_count))

            result.translation_file = os.path.join(working_dir, result.id + '.' + self._target_lang)
            osutils.concat([
                os.path.join(translations_path, corpus.name + '.' + self._target_lang) for corpus in corpora
            ], result.translation_file)
        except TranslateError as e:
            result.error = e
        except Exception as e:
            result.error = TranslateError('Unexpected ERROR: ' + str(e.message))

        return result
