import argparse
import glob
import json
import os
import shutil
import time
from collections import defaultdict

import requests

from cli import StatefulActivity, ensure_engine_exists, ensure_node_running, ensure_node_has_api, activitystep, \
    CLIArgsException
from cli.mmt import bleu
from cli.mmt.engine import Engine, EngineNode
from cli.mmt.fileformats import ParallelFileFormat
from cli.mmt.processing import XMLEncoder
from cli.mmt.translation import GoogleTranslate, ModernMTTranslate, TranslateError
from cli.utils import osutils


class Score(object):
    @property
    def name(self):
        raise NotImplementedError

    def calculate(self, reference, translation):
        raise NotImplementedError


class BLEUScore(Score):
    @property
    def name(self):
        return 'BLEU Score'

    def calculate(self, reference, translation):
        with open(reference, 'r', encoding='utf-8') as ref, open(translation, 'r', encoding='utf-8') as hyp:
            return bleu.corpus_bleu(ref, hyp, tokenize=True)


class MatecatScore(Score):
    @property
    def name(self):
        return 'Matecat Post-Editing Score'

    @staticmethod
    def _get_score(sentences, references):
        url = 'http://api.mymemory.translated.net/computeMatch.php'
        data = {
            'sentences': sentences,
            'reference_sentences': references
        }

        r = requests.post(url, data=json.dumps(data), headers={'Content-type': 'application/json'}, timeout=60)
        body = r.json()

        if r.status_code != requests.codes.ok:
            err = body['error'] if isinstance(body, dict) and 'error' in body else 'unknown'
            raise requests.RequestException('Matecat Score service not available (%d): %s' % (r.status_code, err))

        return body

    def calculate(self, reference, translation):
        with open(reference, 'r', encoding='utf-8') as _r:
            references = _r.readlines()
        with open(translation, 'r', encoding='utf-8') as _t:
            translations = _t.readlines()

        def chunks(l, n):
            for i in range(0, len(l), n):
                yield l[i:i + n]

        scores = []
        for ref_lines, tr_lines in zip(chunks(references, 30), chunks(translations, 30)):
            scores.extend(self._get_score(tr_lines, ref_lines))

        return (sum(scores) / len(scores)) * 100.


class _EvaluationEntry:
    def __init__(self, engine):
        self.id = engine.name.replace(' ', '_')
        self.name = engine.name
        self.engine = engine
        self.translations_path = None
        self.file = None
        self.time = float('inf')
        self.error = None
        self.scores = defaultdict(float)

    def __repr__(self):
        return repr(self.__dict__)

    def __str__(self):
        return str(self.__dict__)


class EvaluateActivity(StatefulActivity):
    def __init__(self, mmt_node, args, extra_argv=None, wdir=None, log_file=None, start_step=None, delete_on_exit=True):
        super().__init__(args, extra_argv, wdir, log_file, start_step, delete_on_exit)

        gt = GoogleTranslate(args.src_lang, args.tgt_lang, key=args.google_key)
        mmt = ModernMTTranslate(mmt_node, args.src_lang, args.tgt_lang, priority='background',
                                context_string=args.context, context_file=args.context_file,
                                context_vector=args.context_vector)

        self.state.scores = [MatecatScore(), BLEUScore()]
        self.state.corpora = ParallelFileFormat.list(args.src_lang, args.tgt_lang, args.test_set)
        self.state.test_set_lines = sum([osutils.lc(c.src_file) for c in self.state.corpora])
        self.state.entries = [_EvaluationEntry(engine) for engine in [gt, mmt]]

        for entry in self.state.entries:
            self._steps.insert(0, activitystep('Translating with %s' % entry.name)(self._translate(entry)))

        for score in self.state.scores:
            self._steps.append(activitystep('Scoring with %s' % score.name)(self._score(score)))

    def _save_state(self):
        pass

    @staticmethod
    def _translate(entry):
        def internal(self):
            entry.translations_path = self.wdir('translations', entry.id)

            try:
                begin_time = time.time()
                for corpus in self.state.corpora:
                    out_file = os.path.join(entry.translations_path, corpus.name + '.' + self.args.tgt_lang)
                    entry.engine.translate_file(corpus.src_file, out_file)

                entry.time = time.time() - begin_time
            except TranslateError as e:
                entry.error = e
            except Exception as e:
                entry.error = TranslateError('Unexpected ERROR: ' + str(e))

        return internal

    @activitystep('Preparing data for scoring')
    def prepare(self):
        def _list(path, lang):
            return sorted(glob.glob(os.path.join(path, '*.' + lang)))

        def _export_he_file(src, dest_folder):
            filename = os.path.basename(src)
            dest = os.path.join(dest_folder, filename)
            lang = os.path.splitext(filename)[1][1:]

            with open(src, 'r', encoding='utf-8') as src_in, open(dest, 'w', encoding='utf-8') as dest_out:
                for i, line in enumerate(src_in):
                    line = XMLEncoder.encode(line)
                    dest_out.write('%d\t%s\t%s' % (i, lang, line.replace('\t', ' ')))

        self.state.source_file = os.path.join(self._wdir, 'source.' + self.args.src_lang)
        self.state.reference_file = os.path.join(self._wdir, 'reference.' + self.args.tgt_lang)

        osutils.cat(_list(self.args.test_set, self.args.src_lang), self.state.source_file)
        osutils.cat(_list(self.args.test_set, self.args.tgt_lang), self.state.reference_file)

        for entry in self.state.entries:
            if entry.error is not None:
                continue

            entry.file = os.path.join(self._wdir, entry.id + '.' + self.args.tgt_lang)
            osutils.cat(_list(entry.translations_path, self.args.tgt_lang), entry.file)

        if self.args.human_eval_path is not None:
            if not os.path.isdir(self.args.human_eval_path):
                os.makedirs(self.args.human_eval_path)

            _export_he_file(self.state.source_file, self.args.human_eval_path)
            _export_he_file(self.state.reference_file, self.args.human_eval_path)

            for entry in self.state.entries:
                if entry.file is not None:
                    _export_he_file(entry.file, self.args.human_eval_path)

    @staticmethod
    def _score(score):
        def internal(self):
            for entry in self.state.entries:
                if entry.error is not None:
                    continue

                try:
                    entry.scores[score.name] = score.calculate(self.state.reference_file, entry.file)
                except Exception as e:
                    entry.scores[score.name] = str(e)

        return internal

    def run(self):
        print()
        print('============== EVALUATION ==============', end='\n\n')
        print('Testing on %d lines:' % self.state.test_set_lines, end='\n\n')

        super().run()

        print()
        print('=============== RESULTS ================', end='\n\n')

        for score in self.state.scores:
            print(score.name + ':')

            for i, entry in enumerate(sorted(self.state.entries,
                                             key=lambda x: x.scores[score.name]
                                             if isinstance(x.scores[score.name], float) else -1, reverse=True)):
                if entry.error is None:
                    score_value = entry.scores[score.name]

                    if isinstance(score_value, float):
                        score_value = '%.1f' % score_value
                        if i == 0:
                            score_value += ' (Winner)'
                else:
                    score_value = str(entry.error)

                print('  %s: %s' % (entry.name.ljust(20), score_value))
            print()

        print('Translation Speed:')
        for entry in sorted(self.state.entries, key=lambda x: x.time):
            if entry.error is None:
                text = '%.2fs per sentence' % (entry.time / self.state.test_set_lines)
            else:
                text = str(entry.error)

            print('  %s: %s' % (entry.name.ljust(20), text))
        print()

        if self.args.human_eval_path is not None:
            print('Files for Human Evaluation are available here: %s' % os.path.abspath(self.args.human_eval_path))
            print()


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Evaluate a ModernMT engine', prog='mmt evaluate')
    parser.add_argument('-s', '--source', dest='src_lang', metavar='SOURCE_LANGUAGE', default=None,
                        help='the source language (ISO 639-1). Can be omitted if engine is monolingual.')
    parser.add_argument('-t', '--target', dest='tgt_lang', metavar='TARGET_LANGUAGE', default=None,
                        help='the target language (ISO 639-1). Can be omitted if engine is monolingual.')
    parser.add_argument('--path', dest='test_set', metavar='CORPORA', default=None,
                        help='the path to the test corpora (default is the automatically extracted sample)')

    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')
    parser.add_argument('--gt-key', dest='google_key', metavar='GT_API_KEY', default=None,
                        help='A custom Google Translate API Key to use during evaluation')
    parser.add_argument('--human-eval', dest='human_eval_path', metavar='OUTPUT', default=None,
                        help='the output folder for the tab-spaced files needed to setup a Human Evaluation benchmark')
    parser.add_argument('-d', '--debug', action='store_true', dest='debug',
                        help='if debug is set, prevents temporary files to be removed after execution')

    # Context arguments
    parser.add_argument('--context', metavar='CONTEXT', dest='context',
                        help='A string to be used as translation context')
    parser.add_argument('--context-file', metavar='CONTEXT_FILE', dest='context_file',
                        help='A local file to be used as translation context')
    parser.add_argument('--context-vector', metavar='CONTEXT_VECTOR', dest='context_vector',
                        help='The context vector with format: <document 1>:<score 1>[,<document N>:<score N>]')

    args = parser.parse_args(argv)

    engine = Engine(args.engine)
    if args.src_lang is None or args.tgt_lang is None:
        if len(engine.languages) > 1:
            raise CLIArgsException(parser,
                                   'Missing language. Options "-s" and "-t" are mandatory for multilingual engines.')
        args.src_lang, args.tgt_lang = engine.languages[0]

    if args.test_set is None:
        args.test_set = engine.get_test_path(args.src_lang, args.tgt_lang)

    if len(ParallelFileFormat.list(args.src_lang, args.tgt_lang, args.test_set)) == 0:
        raise CLIArgsException(parser, 'No parallel corpora found in path: ' + args.test_set)

    return args


def main(argv=None):
    args = parse_args(argv)

    engine = Engine(args.engine)
    ensure_engine_exists(engine)

    node = EngineNode(engine)
    ensure_node_running(node)
    ensure_node_has_api(node)

    wdir = engine.get_tempdir('evaluate')
    shutil.rmtree(wdir, ignore_errors=True)
    os.makedirs(wdir)

    activity = EvaluateActivity(node, args, wdir=wdir, delete_on_exit=not args.debug)
    activity.run()
