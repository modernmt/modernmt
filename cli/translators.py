import Queue
import copy
import os
import random
import re
import threading
import time
from multiprocessing.dummy import Pool
from xml.etree import ElementTree

import requests

from cli.libs import nvidia_smi
from cli.mmt.cluster import ApiException, ClusterNode
from cli.mmt.processing import XMLEncoder

def  map_language(lang):
    fields = lang.split('-')
    if fields[0] == "zh" and len(fields) > 1:
        if fields[1] == "CN" or fields[1] == "TW":
            return lang
    return fields[0]

class TranslateError(Exception):
    def __init__(self, *args, **kwargs):
        super(TranslateError, self).__init__(*args, **kwargs)


class TranslateEngine(object):
    def __init__(self, source_lang, target_lang):
        self.source_lang = source_lang
        self.target_lang = target_lang

    @property
    def name(self):
        raise NotImplementedError

    def _get_default_threads(self):
        raise NotImplementedError

    def translate_text(self, text):
        raise NotImplementedError

    def translate_batch(self, generator, consumer, threads=None):
        pool = Pool(threads if threads is not None else self._get_default_threads())
        jobs = Queue.Queue()

        raise_error = []

        def _consumer_thread_run():
            while True:
                job = jobs.get(block=True)

                if job is None:
                    break

                try:
                    translation = job.get()
                    consumer(translation)
                except Exception as e:
                    raise_error.append(e)
                    break

        consumer_thread = threading.Thread(target=_consumer_thread_run)
        consumer_thread.start()

        try:
            count = 0
            for line in generator:
                count += 1
                _job = pool.apply_async(self.translate_text, (line,))
                jobs.put(_job, block=True)
            return count
        finally:
            jobs.put(None, block=True)
            consumer_thread.join()
            pool.terminate()

            if len(raise_error) > 0:
                raise raise_error[0]

    def translate_stream(self, input_stream, output_stream, threads=None):
        def generator():
            for line in input_stream:
                yield line.rstrip('\n')

        def consumer(line):
            output_stream.write(line.encode('utf-8'))
            output_stream.write('\n')

        return self.translate_batch(generator(), consumer, threads=threads)

    def translate_file(self, input_file, output_file, threads=None):
        with open(input_file) as input_stream:
            with open(output_file, 'w') as output_stream:
                return self.translate_stream(input_stream, output_stream, threads=threads)

    def translate_corpora(self, corpora, output_folder, threads=None):
        count = 0
        for corpus in corpora:
            input_file = corpus.get_file(self.source_lang)
            output_file = os.path.join(output_folder, corpus.name + '.' + self.target_lang)

            count += self.translate_file(input_file, output_file, threads=threads)
        return count


class MMTTranslator(TranslateEngine):
    def __init__(self, node, source_lang, target_lang, priority=None,
                 context_vector=None, context_file=None, context_string=None):
        TranslateEngine.__init__(self, source_lang, target_lang)
        self._api = node.api
        self._priority = ClusterNode.Api.PRIORITY_BACKGROUND if priority is None else priority
        self._context = None

        if context_vector is None:
            if context_file is not None:
                self._context = self._api.get_context_f(self.source_lang, self.target_lang, context_file)
            elif context_string is not None:
                self._context = self._api.get_context_s(self.source_lang, self.target_lang, context_string)
        else:
            self._context = self._parse_context_vector(context_vector)

    def _get_default_threads(self):
        executors = max(len(nvidia_smi.list_gpus()), 1)
        cluster_info = self._api.info()['cluster']
        node_count = max(len(cluster_info['nodes']), 1)

        return executors * node_count

    @property
    def context_vector(self):
        return [x.copy() for x in self._context] if self._context is not None else None

    @staticmethod
    def _parse_context_vector(text):
        context = []

        try:
            for score in text.split(','):
                _id, value = score.split(':', 2)
                value = float(value)

                context.append({
                    'memory': int(_id),
                    'score': value
                })
        except ValueError:
            raise ValueError('invalid context weights map: ' + text)

        return context

    @property
    def name(self):
        return 'ModernMT'

    def translate_text(self, text):
        try:
            text = text.decode('utf-8')

            if len(text) > 4096:
                text = text[:4096]

            translation = self._api.translate(self.source_lang, self.target_lang, text,
                                              context=self._context, priority=self._priority)
        except requests.exceptions.ConnectionError:
            raise TranslateError('Unable to connect to MMT. '
                                 'Please check if engine is running on port %d.' % self._api.port)
        except ApiException as e:
            raise TranslateError(e.message)

        return translation['translation']

    def translate_file(self, input_file, output_file, threads=None):
        try:
            self._context = self._api.get_context_f(self.source_lang, self.target_lang, input_file)
            return super(MMTTranslator, self).translate_file(input_file, output_file, threads=threads)
        except requests.exceptions.ConnectionError:
            raise TranslateError('Unable to connect to MMT. '
                                 'Please check if engine is running on port %d.' % self._api.port)
        except ApiException as e:
            raise TranslateError(e.message)
        finally:
            self._context = None


class GoogleRateLimitError(TranslateError):
    def __init__(self, *args, **kwargs):
        super(GoogleRateLimitError, self).__init__(*args, **kwargs)


class GoogleServerError(TranslateError):
    def __init__(self, *args, **kwargs):
        super(GoogleServerError, self).__init__(*args, **kwargs)


class GoogleTranslate(TranslateEngine):
    DEFAULT_GOOGLE_KEY = 'AIzaSyBl9WAoivTkEfRdBBSCs4CruwnGL_aV74c'

    def __init__(self, source_lang, target_lang, key=None):
        TranslateEngine.__init__(self, source_lang, target_lang)
        self._key = key if key is not None else self.DEFAULT_GOOGLE_KEY
        self._delay = 0
        self._xml_encoder = XMLEncoder()

        self._url = 'https://translation.googleapis.com/language/translate/v2'

    @property
    def name(self):
        return 'Google Translate'

    def _get_default_threads(self):
        return 5

    @staticmethod
    def _pack_error(request):
        json = request.json()

        if request.status_code == 403:
            for error in json['error']['errors']:
                if error['reason'] == 'dailyLimitExceeded':
                    return TranslateError('Google Translate free quota is over. Please use option --gt-key'
                                          ' to specify your GT API key.')
                elif error['reason'] == 'userRateLimitExceeded':
                    return GoogleRateLimitError('Google Translate rate limit exceeded')
        elif 500 <= request.status_code < 600:
            return GoogleServerError('Google Translate server error (%d): %s' %
                                     (request.status_code, json['error']['message']))

        return TranslateError('Google Translate error (%d): %s' % (request.status_code, json['error']['message']))

    def _increment_delay(self):
        if self._delay < 0.002:
            self._delay = 0.05
        else:
            self._delay = min(1, self._delay * 1.05)

    def _decrement_delay(self):
        self._delay *= 0.95

        if self._delay < 0.002:
            self._delay = 0

    def translate_text(self, text):
        data = {
            'model': 'nmt',
            'source': map_language(self.source_lang),
            'target': map_language(self.target_lang),
            'q': text,
            'key': self._key,
            'userip': '.'.join(map(str, (random.randint(0, 200) for _ in range(4))))
        }

        headers = {
            'X-HTTP-Method-Override': 'GET'
        }

        rate_limit_reached = False
        server_error_count = 0

        while True:
            if self._delay > 0:
                delay = self._delay * random.uniform(0.5, 1)
                time.sleep(delay)

            r = requests.post(self._url, data=data, headers=headers)

            if r.status_code != requests.codes.ok:
                e = self._pack_error(r)
                if isinstance(e, GoogleRateLimitError):
                    rate_limit_reached = True
                    self._increment_delay()
                elif isinstance(e, GoogleServerError):
                    server_error_count += 1

                    if server_error_count < 10:
                        time.sleep(1.)
                    else:
                        raise e
                else:
                    raise e
            else:
                break

        if not rate_limit_reached and self._delay > 0:
            self._decrement_delay()

        translation = r.json()['data']['translations'][0]['translatedText']
        translation = self._xml_encoder.encode_string(translation)

        return translation


class Translator(object):
    def __init__(self, engine):
        self._engine = engine

    def run(self, in_stream, out_stream, threads=None):
        raise NotImplementedError


class BatchTranslator(Translator):
    def __init__(self, engine):
        Translator.__init__(self, engine)

    def run(self, in_stream, out_stream, threads=None):
        self._engine.translate_stream(in_stream, out_stream, threads=threads)


class InteractiveTranslator(Translator):
    def __init__(self, engine):
        Translator.__init__(self, engine)

        print '\nModernMT Translate command line'

        if isinstance(engine, MMTTranslator) and engine.context_vector:
            norm = sum([e['score'] for e in engine.context_vector])
            print '>> Context:', ', '.join(
                ['%s %.f%%' % (self._memory_to_string(score['memory']), round(score['score'] * 100 / norm))
                 for score in engine.context_vector])
        else:
            print '>> No context provided.'

        print

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
                out_stream.write(translation.encode('utf-8'))
                out_stream.write('\n')
                out_stream.flush()
        except KeyboardInterrupt:
            pass


class XLIFFTranslator(Translator):
    NAMESPACES = {
        'xlf': 'urn:oasis:names:tc:xliff:document:1.2',
        'sdl': 'http://sdl.com/FileTypes/SdlXliff/1.0',
        'mq': 'MQXliff'
    }
    DEFAULT_NAMESPACE = 'urn:oasis:names:tc:xliff:document:1.2'
    SDL_NAMESPACE = 'http://sdl.com/FileTypes/SdlXliff/1.0'

    class TransUnit(object):
        @staticmethod
        def parse(tu, target_lang):
            entries = []
            ns = XLIFFTranslator.NAMESPACES

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

            return XLIFFTranslator.TransUnit(entries)

        def __init__(self, entries):
            self._entries = entries

        def __iter__(self):
            return (x for x in self._entries)

    def __init__(self, engine):
        Translator.__init__(self, engine)
        self._units = None
        self._index = None

        for namespace, uri in self.NAMESPACES.iteritems():
            if namespace == 'xlf':
                namespace = ''
            ElementTree.register_namespace(namespace, uri)

    def run(self, in_stream, out_stream, threads=None):
        self._units = []
        self._index = 0

        # Parse XLIFF
        xliff = ElementTree.fromstring(in_stream.read())

        for tu in xliff.findall('.//xlf:trans-unit', self.NAMESPACES):
            trans_unit = XLIFFTranslator.TransUnit.parse(tu, self._engine.target_lang)

            for source_tag, target_tag in trans_unit:
                if self._skip_source_tag(tu, source_tag):
                    continue

                source_content, placeholders = self._get_source_content(source_tag)
                if source_content is None:
                    continue

                self._units.append((source_tag, target_tag))

        # Translate XLIFF
        def generator():
            for st, tt in self._units:
                content, _ = self._get_source_content(st)
                yield content

        self._engine.translate_batch(generator(), self._append_translation, threads=threads)

        xliff_str = ElementTree.tostring(xliff, encoding='UTF-8', method='xml')
        out_stream.write(xliff_str)

    def _skip_source_tag(self, tu, source_tag):
        _id = source_tag.attrib['mid']
        match = tu.find('.//sdl:seg[@id="%s"][@percent="100"]' % _id, self.NAMESPACES)
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

    def _append_translation(self, content):
        source_tag, target_tag = self._units[self._index]
        self._index += 1

        source_text, placeholders = self._get_source_content(source_tag)
        trailing_match = re.search(r'\s*$', source_text)
        trailing_space = trailing_match.group() if trailing_match is not None else ''

        content = u'<content xmlns="%s">%s</content>' % (self.NAMESPACES['xlf'], content + trailing_space)
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
        for child in list(target_tag):
            target_tag.remove(child)

        # Replace target content
        target_tag.text = content.text
        for child in list(content):
            content.remove(child)
            target_tag.append(child)
