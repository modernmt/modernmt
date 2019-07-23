import argparse
import os
import sys
import tempfile

from cli import ensure_node_running, ensure_node_has_api, CLIArgsException
from cli.mmt.engine import EngineNode, Engine
from cli.mmt.fileformats import XLIFFFileFormat
from cli.mmt.translation import ModernMTTranslate


class Translator(object):
    def __init__(self, engine):
        self._engine = engine

    def run(self, in_stream, out_stream, threads=None):
        raise NotImplementedError


class XLIFFTranslator(Translator):
    def __init__(self, engine):
        Translator.__init__(self, engine)

    def run(self, in_stream, out_stream, threads=None):
        temp_file = None

        try:
            with tempfile.NamedTemporaryFile('w', encoding='utf-8', delete=False) as temp_stream:
                temp_file = temp_stream.name
                temp_stream.write(in_stream.read())

            xliff = XLIFFFileFormat(temp_file, self._engine.target_lang)

            def generator():
                with xliff.reader() as reader:
                    for src_line, _ in reader:
                        yield src_line

            with xliff.writer() as writer:
                self._engine.translate_batch(generator(), lambda r: writer.write(None, r), threads=threads)

            with open(temp_file, 'r', encoding='utf-8') as result:
                out_stream.write(result.read())
        finally:
            if temp_file is not None and os.path.exists(temp_file):
                os.remove(temp_file)


class BatchTranslator(Translator):
    def __init__(self, engine):
        Translator.__init__(self, engine)

    def run(self, in_stream, out_stream, threads=None):
        self._engine.translate_stream(in_stream, out_stream, threads=threads)


class InteractiveTranslator(Translator):
    def __init__(self, engine):
        Translator.__init__(self, engine)

        print('\nModernMT Translate command line')

        if isinstance(engine, ModernMTTranslate) and engine.context_vector:
            norm = sum([e['score'] for e in engine.context_vector])
            print('>> Context:', ', '.join(
                ['%s %.f%%' % (self._memory_to_string(score['memory']), round(score['score'] * 100 / norm))
                 for score in engine.context_vector]))
        else:
            print('>> No context provided.')

        print(flush=True)

    @staticmethod
    def _memory_to_string(memory):
        if isinstance(memory, int):
            return '[' + str(memory) + ']'
        else:
            return memory['name']

    def run(self, in_stream, out_stream, threads=None):
        try:
            while 1:
                out_stream.write('> ')
                line = in_stream.readline()
                if not line:
                    break

                line = line.strip()
                if len(line) == 0:
                    continue

                translation = self._engine.translate_text(line)
                out_stream.write(translation)
                out_stream.write('\n')
                out_stream.flush()
        except KeyboardInterrupt:
            pass


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Translate text with ModernMT', prog='mmt translate')
    parser.add_argument('text', metavar='TEXT', help='text to be translated (optional)', default=None, nargs='?')
    parser.add_argument('-s', '--source', dest='source_lang', metavar='SOURCE_LANGUAGE', default=None,
                        help='the source language (ISO 639-1). Can be omitted if engine is monolingual.')
    parser.add_argument('-t', '--target', dest='target_lang', metavar='TARGET_LANGUAGE', default=None,
                        help='the target language (ISO 639-1). Can be omitted if engine is monolingual.')

    # Context arguments
    parser.add_argument('--context', metavar='CONTEXT', dest='context',
                        help='A string to be used as translation context')
    parser.add_argument('--context-file', metavar='CONTEXT_FILE', dest='context_file',
                        help='A local file to be used as translation context')
    parser.add_argument('--context-vector', metavar='CONTEXT_VECTOR', dest='context_vector',
                        help='The context vector with format: <document 1>:<score 1>[,<document N>:<score N>]')

    # Mixed arguments
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')
    parser.add_argument('--batch', action='store_true', dest='batch', default=False,
                        help='if set, the script will read the whole stdin before send translations to MMT.'
                             'This can be used to execute translation in parallel for a faster translation. ')
    parser.add_argument('--threads', dest='threads', default=None, type=int,
                        help='number of concurrent translation requests.')
    parser.add_argument('--xliff', dest='is_xliff', action='store_true', default=False,
                        help='if set, the input is a XLIFF file.')

    args = parser.parse_args(argv)

    engine = Engine(args.engine)

    if args.source_lang is None or args.target_lang is None:
        if len(engine.languages) > 1:
            raise CLIArgsException(parser,
                                   'Missing language. Options "-s" and "-t" are mandatory for multilingual engines.')
        args.source_lang, args.target_lang = engine.languages[0]

    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)

    engine = Engine(args.engine)
    node = EngineNode(engine)
    ensure_node_running(node)
    ensure_node_has_api(node)

    mmt = ModernMTTranslate(node, args.source_lang, args.target_lang, context_string=args.context,
                            context_file=args.context_file, context_vector=args.context_vector)

    if args.text is not None:
        print(mmt.translate_text(args.text.strip()))
    else:
        if args.is_xliff:
            translator = XLIFFTranslator(mmt)
        elif args.batch:
            translator = BatchTranslator(mmt)
        else:
            translator = InteractiveTranslator(mmt)

        try:
            translator.run(sys.stdin, sys.stdout, threads=args.threads)
        except KeyboardInterrupt:
            pass  # exit
