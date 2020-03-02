import copy
import glob
import os
import re
from xml.etree import ElementTree


class FileFormat(object):
    def reader(self):
        raise NotImplementedError

    def writer(self, append=False):
        raise NotImplementedError


class DevNullFileFormat(FileFormat):
    class NullWriter(object):
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            pass

        def write(self, src_line, tgt_line):
            pass

    class NullReader(object):
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            pass

        def __iter__(self):
            return self

        def __next__(self):
            raise StopIteration

    def reader(self):
        return self.NullReader()

    def writer(self, append=False):
        return self.NullWriter()


class ParallelFileFormat(FileFormat):
    class Reader(object):
        def __init__(self, src_file, tgt_file) -> None:
            self._src_file = src_file
            self._tgt_file = tgt_file
            self._src_stream = None
            self._tgt_stream = None

        def __enter__(self):
            self._src_stream = open(self._src_file, 'r', encoding='utf-8')
            self._tgt_stream = open(self._tgt_file, 'r', encoding='utf-8')
            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            self._src_stream.close()
            self._tgt_stream.close()

        def __iter__(self):
            for src_line, tgt_line in zip(self._src_stream, self._tgt_stream):
                yield src_line.rstrip('\n'), tgt_line.rstrip('\n')

    class Writer(object):
        def __init__(self, src_file, tgt_file, append=False) -> None:
            self._src_file = src_file
            self._tgt_file = tgt_file
            self._src_stream = None
            self._tgt_stream = None
            self._append = append

        def __enter__(self):
            mode = 'a' if self._append else 'w'
            self._src_stream = open(self._src_file, mode, encoding='utf-8')
            self._tgt_stream = open(self._tgt_file, mode, encoding='utf-8')
            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            self._src_stream.close()
            self._tgt_stream.close()

        def write(self, src_line, tgt_line):
            src_line, tgt_line = src_line.rstrip('\n').replace('\n', ' '), tgt_line.rstrip('\n').replace('\n', ' ')
            self._src_stream.write(src_line + '\n')
            self._tgt_stream.write(tgt_line + '\n')

    @classmethod
    def from_path(cls, src_lang, tgt_lang, name, path):
        src_file = os.path.join(path, name + '.' + src_lang)
        tgt_file = os.path.join(path, name + '.' + tgt_lang)

        return cls(src_lang, tgt_lang, src_file, tgt_file)

    @classmethod
    def list(cls, src_lang, tgt_lang, path):
        result = []

        for src_file in glob.glob(os.path.join(path, '*.' + src_lang)):
            tgt_file = os.path.splitext(src_file)[0] + '.' + tgt_lang

            if os.path.isfile(tgt_file):
                result.append(cls(src_lang, tgt_lang, src_file, tgt_file))

        return result

    def __init__(self, src_lang, tgt_lang, src_file, tgt_file) -> None:
        self._src_lang = src_lang
        self._tgt_lang = tgt_lang
        self._src_file = src_file
        self._tgt_file = tgt_file
        self._name = os.path.splitext(os.path.basename(src_file))[0]

    @property
    def name(self):
        return self._name

    @property
    def src_lang(self):
        return self._src_lang

    @property
    def tgt_lang(self):
        return self._tgt_lang

    @property
    def src_file(self):
        return self._src_file

    @property
    def tgt_file(self):
        return self._tgt_file

    def reader(self):
        return self.Reader(self._src_file, self._tgt_file)

    def writer(self, append=False):
        return self.Writer(self._src_file, self._tgt_file, append=append)


class CompactFileFormat(FileFormat):
    class Reader(object):
        def __init__(self, file_path, include_lang=False) -> None:
            self._file_path = file_path
            self._stream = None
            self._include_lang = include_lang

        def __enter__(self):
            self._stream = open(self._file_path, 'r', encoding='utf-8')
            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            self._stream.close()

        def __iter__(self):
            for src_line in self._stream:
                src_line = src_line.rstrip('\n')
                tgt_line = self._stream.readline().rstrip('\n')
                meta = self._stream.readline().strip()

                if self._include_lang:
                    src_lang, tgt_lang = meta.split(',')[1].split()
                    yield src_lang, tgt_lang, src_line, tgt_line
                else:
                    yield src_line, tgt_line

    class Writer(object):
        def __init__(self, src_lang, tgt_lang, file_path, append=False) -> None:
            self._src_lang = src_lang
            self._tgt_lang = tgt_lang
            self._file_path = file_path
            self._stream = None
            self._append = append

        def __enter__(self):
            mode = 'a' if self._append else 'w'
            self._stream = open(self._file_path, mode, encoding='utf-8')
            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            self._stream.close()

        def write(self, src_line, tgt_line):
            src_line, tgt_line = src_line.rstrip('\n').replace('\n', ' '), tgt_line.rstrip('\n').replace('\n', ' ')
            self._stream.write(src_line + '\n')
            self._stream.write(tgt_line + '\n')
            self._stream.write('0,%s %s\n' % (self._src_lang, self._tgt_lang))

    def __init__(self, src_lang, tgt_lang, file_path) -> None:
        self._src_lang = src_lang
        self._tgt_lang = tgt_lang
        self._file_path = file_path
        self._name = os.path.splitext(os.path.basename(file_path))[0]

    @property
    def name(self):
        return self._name

    @property
    def src_lang(self):
        return self._src_lang

    @property
    def tgt_lang(self):
        return self._tgt_lang

    @property
    def file_path(self):
        return self._file_path

    def reader(self):
        return self.Reader(self._file_path, include_lang=False)

    def reader_with_languages(self):
        return self.Reader(self._file_path, include_lang=True)

    def writer(self, append=False):
        return self.Writer(self._src_lang, self._tgt_lang, self._file_path, append=append)


class XLIFFFileFormat(FileFormat):
    NAMESPACES = {
        'xlf': 'urn:oasis:names:tc:xliff:document:1.2',
        'sdl': 'http://sdl.com/FileTypes/SdlXliff/1.0',
        'mq': 'MQXliff'
    }
    DEFAULT_NAMESPACE = 'urn:oasis:names:tc:xliff:document:1.2'
    SDL_NAMESPACE = 'http://sdl.com/FileTypes/SdlXliff/1.0'

    class TransUnit(object):
        @classmethod
        def parse(cls, tu, target_lang):
            entries = []
            ns = XLIFFFileFormat.NAMESPACES

            # Target part
            target_tag = tu.find('xlf:target', ns)
            if target_tag is None:
                target_tag = ElementTree.Element('target', attrib={
                    'xml:lang': target_lang
                })
                tu.append(target_tag)

            # Source part
            source_tag = tu.find('xlf:seg-source', ns)
            if source_tag is None:
                source_tag = tu.find('xlf:source', ns)

            segments = source_tag.findall('.//xlf:mrk[@mtype="seg"]', ns)

            if segments is None or len(segments) == 0:
                entries.append((source_tag, target_tag))
            else:
                for source_segment in segments:
                    mid = source_segment.get('mid')
                    if mid is None:
                        raise ValueError('Invalid XLIFF, missing "mid" for <mrk>')

                    target_segment = target_tag.find('.//xlf:mrk[@mtype="seg"][@mid="%s"]' % mid, ns)
                    if target_segment is None:
                        raise ValueError('Invalid XLIFF, unable to locate <mrk> element for "mid" %s '
                                         'in <target> element' % mid)

                    entries.append((source_segment, target_segment))

            return cls(entries)

        def __init__(self, entries):
            self._entries = entries

        def __iter__(self):
            for entry in self._entries:
                yield entry

    @classmethod
    def _skip_source_tag(cls, tu, source_tag):
        if 'mid' in source_tag.attrib:
            _id = source_tag.attrib['mid']
            match = tu.find('.//sdl:seg[@id="%s"][@percent="100"]' % _id, cls.NAMESPACES)
            return True if match is not None else False
        else:
            return True

    @classmethod
    def _get_tag_name(cls, e):
        return e.tag if '}' not in e.tag else e.tag.split('}', 1)[1]

    @classmethod
    def _get_source_content(cls, element):
        if element is None:
            return None, None

        def _navigate(el, placeholders):
            for child in list(el):
                name = cls._get_tag_name(child)
                if name in ['ph', 'bpt', 'ept', 'it']:
                    clone = copy.deepcopy(child)
                    clone.tail = None
                    placeholders.append(clone)

                    child.text = None
                    child.attrib = {'id': str(len(placeholders))}
                else:
                    _navigate(child, placeholders)

            return el, placeholders

        content, _placeholders = _navigate(copy.deepcopy(element), [])
        content = ElementTree.tostring(content, encoding='utf-8', method='xml').decode('utf-8')
        content = content[content.find('>') + 1:]
        content = content[:content.rfind('</%s>' % cls._get_tag_name(element))]
        return (content, _placeholders) if len(content) > 0 else (None, None)

    def __init__(self, file_path, tgt_lang) -> None:
        self._file_path = file_path
        self._output_file = file_path
        self._units = []

        for namespace, uri in self.NAMESPACES.items():
            if namespace == 'xlf':
                namespace = ''
            ElementTree.register_namespace(namespace, uri)

        with open(file_path, 'r', encoding='utf-8') as in_stream:
            self._xliff = ElementTree.fromstring(in_stream.read())

        for tu in self._xliff.findall('.//xlf:trans-unit', self.NAMESPACES):
            trans_unit = self.TransUnit.parse(tu, tgt_lang)

            for source_tag, target_tag in trans_unit:
                if self._skip_source_tag(tu, source_tag):
                    continue

                source_content, placeholders = self._get_source_content(source_tag)
                if source_content is None:
                    continue

                self._units.append((source_tag, target_tag))

    def write_to(self, output_file):
        self._output_file = output_file

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        pass

    def __iter__(self):
        for st, tt in self._units:
            src_content, _ = self._get_source_content(st)
            tgt_content, _ = self._get_source_content(tt)
            yield src_content, tgt_content

    def reader(self):
        return self

    def writer(self, append=False):
        class _Writer(object):
            def __init__(self, xliff, units, output_file, get_content_f):
                self._index = 0
                self._xliff = xliff
                self._units = units
                self._xlf_ns = XLIFFFileFormat.NAMESPACES['xlf']
                self._get_content_f = get_content_f
                self._output_file = output_file
                self._output_stream = None

            def __enter__(self):
                self._output_stream = open(self._output_file, 'w', encoding='utf-8')
                return self

            def __exit__(self, exc_type, exc_val, exc_tb):
                xliff_str = ElementTree.tostring(self._xliff, encoding='utf-8', method='xml').decode('utf-8')
                self._output_stream.write(xliff_str)
                self._output_stream.write('\n')
                self._output_stream.close()

            def write(self, _, content):
                source_tag, target_tag = self._units[self._index]
                self._index += 1

                source_text, placeholders = self._get_content_f(source_tag)
                trailing_match = re.search(r'\s*$', source_text)
                trailing_space = trailing_match.group() if trailing_match is not None else ''

                content = u'<content xmlns="%s">%s</content>' % (self._xlf_ns, content.rstrip() + trailing_space)
                content = ElementTree.fromstring(content)

                # Replace placeholders
                parent_map = dict((c, p) for p in content.getiterator() for c in p)

                for i, source in enumerate(placeholders):
                    target = content.find('.//%s[@id="%d"]' % (source.tag, i + 1))
                    if target is not None:
                        source.tail = target.tail

                        parent = parent_map[target]
                        parent_index = list(parent).index(target)
                        parent.remove(target)
                        parent.insert(parent_index, source)

                # Clear target element
                for child in list(target_tag):
                    target_tag.remove(child)

                # Replace target content
                target_tag.text = content.text
                for child in list(content):
                    content.remove(child)
                    target_tag.append(child)

        return _Writer(self._xliff, self._units, self._output_file, self._get_source_content)
