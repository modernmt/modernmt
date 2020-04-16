import os
import re
import tempfile
import unittest

from cli.mmt.mmtcli import mmt_java
from cli.mmt.processing import XMLEncoder
from cli.utils import osutils
from testcases import TEST_RESOURCES


class _XTag(object):
    VALID_NAME_REGEX = re.compile(r'^tag[0-9]+$')

    @staticmethod
    def from_text(text):
        if text == '<!--':
            return _XTag(text, '--', 'O')
        if text == '-->':
            return _XTag(text, '--', 'C')

        name_offset = 1

        if text[1] == '!':  # DTD
            xml_type = 'O'
            name_offset = 2
        elif text[1] == '/':
            xml_type = 'C'
            name_offset = 2
        elif text[-2] == '/':
            xml_type = 'E'
        else:
            xml_type = 'O'

        name = next(XMLEncoder.TAG_NAME_REGEX.finditer(text, name_offset)).group()
        return _XTag(text, name, xml_type) if _XTag.VALID_NAME_REGEX.match(name) else None

    def __init__(self, text, name, xml_type):
        self._text = text
        self._name = name
        self._type = xml_type

    @property
    def name(self):
        return self._name

    @property
    def type(self):
        return self._type

    def __eq__(self, o: object) -> bool:
        if not isinstance(o, type(self)): return NotImplemented
        return self._text == o._text

    def __hash__(self) -> int:
        return hash(self._text)

    def __str__(self) -> str:
        return self._text

    def __repr__(self) -> str:
        return self._text


class _Reader(object):
    def __init__(self, src_file, tgt_file, alg_file) -> None:
        self._src_file = src_file
        self._tgt_file = tgt_file
        self._alg_file = alg_file
        self._src_stream = None
        self._tgt_stream = None
        self._alg_stream = None

    def __enter__(self):
        self._src_stream = open(self._src_file, 'r', encoding='utf-8')
        self._tgt_stream = open(self._tgt_file, 'r', encoding='utf-8')
        self._alg_stream = open(self._alg_file, 'r', encoding='utf-8')
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self._src_stream.close()
        self._tgt_stream.close()
        self._alg_stream.close()

    def __iter__(self):
        for src_line, tgt_line, alg_line in zip(self._src_stream, self._tgt_stream, self._alg_stream):
            yield src_line.rstrip('\n'), tgt_line.rstrip('\n'), alg_line.rstrip('\n')


class TagProjectionTest(unittest.TestCase):
    @staticmethod
    def _extract_tags(line):
        tags = [_XTag.from_text(x.group()) for x in XMLEncoder.TAG_REGEX.finditer(line)]
        return [tag for tag in tags if tag is not None]

    @staticmethod
    def __validate_tags(tags):
        stack = []

        for tag in tags:
            if tag.type == 'O':
                stack.append(tag)
            elif tag.type == 'C':
                if len(stack) > 0:
                    o_tag = stack.pop()
                    if o_tag.name != tag.name:
                        return False

        return True

    def __test(self, src, tgt, filename):
        src_file = os.path.join(TEST_RESOURCES, 'tag_projection_dataset', filename + '.' + src)
        tgt_file = os.path.join(TEST_RESOURCES, 'tag_projection_dataset', filename + '.' + tgt)
        alg_file = os.path.join(TEST_RESOURCES, 'tag_projection_dataset', filename + '.alg')

        self.assertTrue(os.path.isfile(src_file))
        self.assertTrue(os.path.isfile(tgt_file))
        self.assertTrue(os.path.isfile(alg_file))

        java_cmd = mmt_java('eu.modernmt.processing.tags.cli.XMLProjectorTestMain', [src_file, tgt_file, alg_file])

        with tempfile.NamedTemporaryFile() as out_stream:
            osutils.shell_exec(java_cmd, stdout=out_stream)
            out_stream.flush()

            with _Reader(src_file, out_stream.name, alg_file) as reader:
                for src_line, tgt_line, alg_line in reader:
                    src_line, tgt_line = src_line.rstrip(), tgt_line.rstrip()
                    src_tags, tgt_tags = self._extract_tags(src_line), self._extract_tags(tgt_line)

                    if set(src_tags) != set(tgt_tags):
                        self.fail('Not all tags were projected:\n\t%s\n\t%s\n\t%s' % (src_line, tgt_line, alg_line))

                    if not self.__validate_tags(tgt_tags):
                        self.fail('Invalid tag projection:\n\t%s\n\t%s\n\t%s' % (src_line, tgt_line, alg_line))

    def test_project_en_de(self):
        self.__test('en', 'de', 'corpus_en_de')

    def test_project_en_es(self):
        self.__test('en', 'es', 'corpus_en_es')

    def test_project_en_fr(self):
        self.__test('en', 'fr', 'corpus_en_fr')

    def test_project_en_it(self):
        self.__test('en', 'it', 'corpus_en_it')

    def test_project_en_ja(self):
        self.__test('en', 'ja', 'corpus_en_ja')

    def test_project_en_zh(self):
        self.__test('en', 'zh', 'corpus_en_zh')

    def test_project_extra_en_fr(self):
        self.__test('en', 'fr', 'extra_en_fr')
