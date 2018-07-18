import os
import re
from HTMLParser import HTMLParser

from cli import mmt_javamain
from cli.libs import osutils
from cli.mmt import BilingualCorpus

__author__ = 'Davide Caroselli'


class Tokenizer:
    def __init__(self, source_lang, target_lang, print_tags=False, print_placeholders=True):
        self._source_lang = source_lang
        self._target_lang = target_lang
        self._print_tags = print_tags
        self._print_placeholders = print_placeholders
        self._java_main = 'eu.modernmt.cli.PreprocessorMain'

    def process_corpora(self, corpora, output_folder):
        osutils.makedirs(output_folder, exist_ok=True)

        for corpus in corpora:
            output_corpus = BilingualCorpus.make_parallel(corpus.name, output_folder, corpus.langs)

            for lang in corpus.langs:
                input_path = corpus.get_file(lang)
                output_path = output_corpus.get_file(lang)

                self.process_file(input_path, output_path, lang)

        return BilingualCorpus.list(self._source_lang, self._target_lang, output_folder)

    def process_file(self, input_path, output_path, lang):
        if lang == self._source_lang:
            args = ['-s', self._source_lang, '-t', self._target_lang]
        elif lang == self._target_lang:
            args = ['-s', self._target_lang, '-t', self._source_lang]
        else:
            raise ValueError('Unsupported language "%s"' % lang)

        if not self._print_tags:
            args.append('--no-tags')
        if self._print_placeholders:
            args.append('--print-placeholders')

        command = mmt_javamain(self._java_main, args=args)

        with open(input_path) as input_stream:
            with open(output_path, 'w') as output_stream:
                osutils.shell_exec(command, stdin=input_stream, stdout=output_stream)


class XMLEncoder:
    __TAG_NAME = '([a-zA-Z]|_|:)([a-zA-Z]|[0-9]|\\.|-|_|:|)*'
    __TAG_REGEX = re.compile('(<(' + __TAG_NAME + ')[^>]*/?>)|(<!(' + __TAG_NAME + ')[^>]*[^/]>)|(</(' +
                             __TAG_NAME + ')[^>]*>)|(<!--)|(-->)')
    __HTML = HTMLParser()

    def __init__(self):
        pass

    @staticmethod
    def is_xml_tag(string):
        return XMLEncoder.__TAG_REGEX.match(string) is not None

    def encode(self, corpora, dest_folder):
        if not os.path.isdir(dest_folder):
            osutils.makedirs(dest_folder, exist_ok=True)

        out_corpus = []
        for corpus in corpora:
            for lang in corpus.langs:
                source = corpus.get_file(lang)
                dest_file = BilingualCorpus.make_parallel(corpus.name, dest_folder, [lang]).get_file(lang)

                self.encode_file(source, dest_file, delete_nl=True)

            out_corpus.append(BilingualCorpus.make_parallel(corpus.name, dest_folder, corpus.langs))

        return out_corpus

    def encode_file(self, source, dest_file, delete_nl=False):
        with open(dest_file, 'wb') as outstream:
            with open(source) as instream:
                for line in instream:
                    encoded = self.encode_string(line.decode('utf-8'))
                    encoded = encoded.rstrip('\r\n')

                    if delete_nl:
                        encoded = encoded.replace('\n', ' ').replace('\r', '')

                    encoded += '\n'
                    outstream.write(encoded.encode('utf-8'))

    @staticmethod
    def escape(string):
        escaped = XMLEncoder.__HTML.unescape(string)
        return escaped \
            .replace('&', '&amp;') \
            .replace('<', '&lt;') \
            .replace('>', '&gt;')

    @staticmethod
    def unescape(string):
        return XMLEncoder.__HTML.unescape(string)

    def encode_string(self, string):
        result = []
        index = 0

        for match in XMLEncoder.__TAG_REGEX.finditer(string):
            start = match.start()
            end = match.end()

            result.append(self.escape(string[index:start]))
            result.append(match.group())

            index = end

        if index < len(string):
            result.append(self.escape(string[index:]))

        return ''.join(result)

    def decode_string(self, string):
        result = []
        index = 0

        for match in self.__TAG_REGEX.finditer(string):
            start = match.start()
            end = match.end()

            if index != start:
                result.append(self.__HTML.unescape(string[index:start]).strip())

            index = end

        if index < len(string):
            result.append(self.__HTML.unescape(string[index:]).strip())

        return ' '.join(result)
