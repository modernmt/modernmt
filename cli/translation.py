import Queue
import sys
import threading
from xml.etree import ElementTree

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
    DEFAULT_NAMESPACE = 'urn:oasis:names:tc:xliff:document:1.2'
    SDL_NAMESPACE = 'http://sdl.com/FileTypes/SdlXliff/1.0'

    def __init__(self, node, context_string=None, context_file=None, context_vector=None):
        Translator.__init__(self, node, context_string, context_file, context_vector)
        self._content = []
        self._pool = multithread.Pool(100)

        ElementTree.register_namespace('', self.DEFAULT_NAMESPACE)
        ElementTree.register_namespace('sdl', self.SDL_NAMESPACE)

    def execute(self, line):
        self._content.append(line)

    def _translate_transunit(self, tu, _=None):

        # if there is a sdl match to 
        if self._has_sdl_match(tu):
            return None

        source_tag = tu.find('{' + self.DEFAULT_NAMESPACE + '}source')
        seg_source_tag = tu.find('{' + self.DEFAULT_NAMESPACE + '}seg-source')
        target_tag = tu.find('{' + self.DEFAULT_NAMESPACE + '}target')

        # if this XLIFF file uses <seg-source> nodes, find its <mrk> children that have an ID and that are segments,
        # translate them and put the translation in the corresponding <mrk> children in <target>
        if seg_source_tag is not None:
            source_mrk_tags = seg_source_tag.findall('.//{' + self.DEFAULT_NAMESPACE + '}mrk[@mid]')

            # for each <mrk> element, find the corresponding <mrk> in <target>
            for source_mrk_tag in source_mrk_tags:
                mrk_tag_id = str(source_mrk_tag.attrib["mid"])
                target_mrk_tag = target_tag.find('.//{' + self.DEFAULT_NAMESPACE + '}mrk[@mid="' + mrk_tag_id + '"]')

                # if no <mrk> node with the right type and ID exists in <target>, raise an exception
                if target_mrk_tag is None:
                    raise IllegalArgumentException(
                        "<seg-source><mrk> tag with mid %s has no corresponding <target><mrk> tag" % mrk_tag_id)

                # extract the text to translate:

                # if there are more <mrk> descendants under this <mrk>, ignore this <mrk>;
                # the <mrk> descendants will be handled one by one by following iterations
                if len(source_mrk_tag.findall('.//{' + self.DEFAULT_NAMESPACE + '}mrk')) > 0:
                    continue

                # get the content of the mrk tag to translate, translate it,
                # parse the translation result as an XML element and append it under the target mrk tag
                source_text = self._get_string_content_of(source_mrk_tag)
                if source_text:
                    translation = self._translate(source_text)
                    self._append_xmlstring_to(translation['translation'], target_mrk_tag)

        # else just use the <source> and <target> nodes directly
        else:
            if source_tag is not None and target_tag is not None and source_tag.text:
                translation = self._translate(source_tag.text)
                target_tag.text = translation['translation']

        return None

    # This method checks if the passed trans-unit has an SDL match content and must therefore be skipped
    def _has_sdl_match(self, tu):
        match = tu.find('.//{' + self.SDL_NAMESPACE + '}seg[@percent="100"]')
        return True if match is not None else False

    # This method takes an ElementTree element and gets its content (text + subelements) as a string
    def _get_string_content_of(self, element):
        s = element.text or ""
        for sub_element in element:
            s += ElementTree.tostring(sub_element).replace('xmlns="' + self.DEFAULT_NAMESPACE + '" ', '')
        s += element.tail or ""
        return s

    # This method parses an xml string and appends its elements and text to the passed XML Element
    def _append_xmlstring_to(self, string, element_to_fill):
        parseable = "<container>" + string + "</container>"
        xml = ElementTree.fromstring(parseable.encode('utf-8'))

        element_to_fill.text = xml.text
        for xml_el in list(xml):
            element_to_fill.append(xml_el)
        element_to_fill.tail = xml.tail

    def flush(self):
        xliff = ElementTree.fromstring('\n'.join(self._content))
        jobs = []

        for tu in xliff.findall('.//{' + self.DEFAULT_NAMESPACE + '}trans-unit'):
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
