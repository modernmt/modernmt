import Queue
import sys
import threading
from xml.etree import ElementTree

import copy
import requests

from cli import IllegalArgumentException, IllegalStateException
from cli.libs import multithread
from cli.mmt.cluster import ClusterNode

__author__ = 'Davide Caroselli'


class Translator:
    def __init__(self, node, context_string=None, context_file=None, context_vector=None,
                 print_nbest=None, nbest_file=None):
        self.source_lang = node.engine.source_lang
        self.target_lang = node.engine.target_lang
        self._priority = ClusterNode.Api.PRIORITY_BACKGROUND
        self._api = node.api
        self._print_nbest = print_nbest

        self._nbest_out = None
        self._nbest_file = None

        if print_nbest:
            self._nbest_out = sys.stdout

            if nbest_file is not None:
                self._nbest_out = self._nbest_file = open(nbest_file, 'wb')

        self._context = None

        if context_string is not None:
            self._context = self._api.get_context_s(self.source_lang, self.target_lang, context_string)
        elif context_file is not None:
            self._context = self._api.get_context_f(self.source_lang, self.target_lang, context_file)
        elif context_vector is not None:
            self._context = self.__parse_context_vector(context_vector)

    @staticmethod
    def __parse_context_vector(text):
        context = []

        try:
            for score in text.split(','):
                id, value = score.split(':', 2)
                value = float(value)

                context.append({
                    'memory': int(id),
                    'score': value
                })
        except ValueError:
            raise IllegalArgumentException('invalid context weights map: ' + text)

        return context

    @staticmethod
    def _encode_translation(translation):
        return translation['translation'].encode('utf-8')

    @staticmethod
    def _encode_nbest(nbest):
        scores = []

        for name, values in nbest['scores'].iteritems():
            scores.append(name + '= ' + ' '.join([str(f) for f in values]))

        return [
            nbest['translation'],
            ' '.join(scores),
            str(nbest['totalScore'])
        ]

    def _translate(self, line, _=None):
        return self._api.translate(self.source_lang, self.target_lang, line, context=self._context,
                                   nbest=self._print_nbest, priority=self._priority)

    def execute(self, line):
        pass

    def flush(self):
        pass

    def close(self):
        if self._nbest_file is not None:
            self._nbest_file.close()


class BatchTranslator(Translator):
    def __init__(self, node, context_string=None, context_file=None, context_vector=None,
                 print_nbest=False, nbest_file=None, pool_size=100):
        Translator.__init__(self, node, context_string, context_file, context_vector, print_nbest, nbest_file)
        self._pool = multithread.Pool(pool_size)
        self._jobs = Queue.Queue(pool_size)
        self._line_id = 0
        self._printer_thread = threading.Thread(target=self._threaded_print)

        self._printer_thread.start()

    def execute(self, line):
        result = self._pool.apply_async(self._translate, (line, None))
        self._jobs.put(result, block=True)

    def flush(self):
        self._jobs.put(None, block=True)

    def _threaded_print(self):
        while True:
            job = self._jobs.get(block=True)

            if job is None:
                break

            translation = job.get()

            print self._encode_translation(translation)
            if self._print_nbest is not None:
                for nbest in translation['nbest']:
                    parts = [str(self._line_id)] + self._encode_nbest(nbest)
                    self._nbest_out.write((u' ||| '.join(parts)).encode('utf-8'))
                    self._nbest_out.write('\n')

            self._line_id += 1

    def close(self):
        self._printer_thread.join()
        Translator.close(self)
        self._pool.terminate()


class XLIFFTranslator(Translator):
    NAMESPACES = {
        'xlf': 'urn:oasis:names:tc:xliff:document:1.2',
        'sdl': 'http://sdl.com/FileTypes/SdlXliff/1.0',
        'mq': 'MQXliff'
    }
    DEFAULT_NAMESPACE = 'urn:oasis:names:tc:xliff:document:1.2'
    SDL_NAMESPACE = 'http://sdl.com/FileTypes/SdlXliff/1.0'

    def __init__(self, node, context_string=None, context_file=None, context_vector=None):
        Translator.__init__(self, node, context_string, context_file, context_vector)
        self._target_lang = node.engine.target_lang
        self._content = []
        self._pool = multithread.Pool(100)

        for namespace, uri in self.NAMESPACES.iteritems():
            if namespace == 'xlf':
                namespace = ''
            ElementTree.register_namespace(namespace, uri)

    def execute(self, line):
        self._content.append(line)

    def _translate_transunit(self, tu, _=None):
        # if there is a 100% sdl match
        if self._skip_translation_unit(tu):
            return None

        ns = self.NAMESPACES

        source_tag = tu.find('xlf:seg-source', ns)
        if source_tag is None:
            source_tag = tu.find('xlf:source', ns)
        target_tag = tu.find('xlf:target', ns)

        source_content, placeholders = self._get_source_content(source_tag)

        if source_content is None:
            return None

        if target_tag is None:
            target_tag = ElementTree.Element('target', attrib={
                'xml:lang': self._target_lang
            })
            tu.append(target_tag)

        translation = self._translate(source_content)
        self._append_translation(translation['translation'], target_tag, placeholders)

        return None

    def _skip_translation_unit(self, tu):
        match = tu.find('.//sdl:seg[@percent="100"]', self.NAMESPACES)
        return True if match is not None else False

    @staticmethod
    def _get_tag_name(e):
        return e.tag if '}' not in e.tag else e.tag.split('}', 1)[1]

    @staticmethod
    def _get_source_content(element):
        if element is None:
            return None, None

        def _navigate(el, placeholders):
            for child in list(el):
                name = XLIFFTranslator._get_tag_name(child)
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
        content = ElementTree.tostring(content, encoding='utf-8', method='xml')
        content = content[content.find('>') + 1:]
        content = content[:content.rfind('</%s>' % XLIFFTranslator._get_tag_name(element))]
        return (content, _placeholders) if len(content) > 0 else (None, None)

    def _append_translation(self, content, element, placeholders):
        content = u'<content xmlns="%s">%s</content>' % (self.NAMESPACES['xlf'], content)
        content = ElementTree.fromstring(content.encode('utf-8'))

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
        for child in list(element):
            element.remove(child)

        # Replace target content
        element.text = content.text
        element.tail = content.tail
        for child in list(content):
            content.remove(child)
            element.append(child)

    def flush(self):
        xliff = ElementTree.fromstring('\n'.join(self._content))
        jobs = []

        for tu in xliff.findall('.//xlf:trans-unit', self.NAMESPACES):
            job = self._pool.apply_async(self._translate_transunit, (tu, None))
            jobs.append(job)

        for job in jobs:
            _ = job.get()

        print ElementTree.tostring(xliff, encoding='UTF-8', method='xml')

    def close(self):
        Translator.close(self)
        self._pool.terminate()


class InteractiveTranslator(Translator):
    def __init__(self, node, context_string=None, context_file=None, context_vector=None,
                 print_nbest=False, nbest_file=None):
        Translator.__init__(self, node, context_string, context_file, context_vector, print_nbest, nbest_file)
        self._priority = ClusterNode.Api.PRIORITY_NORMAL

        print '\nModernMT Translate command line'

        if self._context:
            norm = sum([e['score'] for e in self._context])
            print '>> Context:', ', '.join(
                ['%s %.f%%' % (self._memory_to_string(score['memory']), round(score['score'] * 100 / norm)) for score in
                 self._context]
            )
        else:
            print '>> No context provided.'

        print

    @staticmethod
    def _memory_to_string(memory):
        if isinstance(memory, int):
            return '[' + str(memory) + ']'
        else:
            return memory['name']

    def execute(self, line):
        if len(line) == 0:
            return

        try:
            translation = self._translate(line)

            if self._print_nbest is not None:
                for nbest in translation['nbest']:
                    self._nbest_out.write((u' ||| '.join(self._encode_nbest(nbest))).encode('utf-8'))
                    self._nbest_out.write('\n')

            print '>>', self._encode_translation(translation)
        except requests.exceptions.ConnectionError:
            raise IllegalStateException('connection problem: MMT server not running, start it with "./mmt start"')
        except requests.exceptions.HTTPError as e:
            raise Exception('HTTP ERROR: ' + e.message)