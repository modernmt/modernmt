import os
import shutil
import time

from cli.mmt.engine import ApiException
from cli.mmt.fileformats import CompactFileFormat
from testcases import ModernMTTestCase, TEST_RESOURCES


class __OnlineLearningTest(ModernMTTestCase):
    mmt_engine_archive = os.path.join(TEST_RESOURCES, 'multilingual_echo_engine.tar.gz')

    CORPUS_DE = CompactFileFormat('en', 'de', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__de.cfc'))
    CORPUS_ES = CompactFileFormat('en', 'es', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__es.cfc'))
    CORPUS_FR = CompactFileFormat('en', 'fr', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__fr.cfc'))
    CORPUS_IT = CompactFileFormat('en', 'it', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__it.cfc'))
    CORPUS_ZH = CompactFileFormat('en', 'zh', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__zh.cfc'))

    ALL_CORPORA = [CORPUS_DE, CORPUS_ES, CORPUS_FR, CORPUS_IT, CORPUS_ZH]
    ACCEPTED_CORPORA = [CORPUS_ES, CORPUS_FR, CORPUS_IT, CORPUS_ZH]

    # Assertion

    def assertInContent(self, content, element):
        element = ''.join(element.split())
        content = [''.join(line.split()) for line in content]

        self.assertIn(element, content)

    def assertInParallelContent(self, content, sentence, translation):
        sentence = ''.join(sentence.split())
        translation = ''.join(translation.split())
        content = [(''.join(s.split()), ''.join(t.split())) for s, t in content]

        self.assertIn((sentence, translation), content)


class OnlineLearningLanguageTest(__OnlineLearningTest):
    def _get_context(self, tgt_lang):
        context = self.mmt.api.get_context_s('en', tgt_lang, 'This is example')
        return [e['memory']['id'] for e in context] if context is not None else None

    def _translate(self, memories, tgt_lang):
        context = [{'memory': int(m['id']), 'score': 1} for m in memories.values()]
        result = self.mmt.api.translate('en', tgt_lang, 'This is example', context=context)
        return result['translation']

    def test_import_with_all_language_combinations(self):
        memories = {}

        for corpus in self.ALL_CORPORA:
            memory = self.mmt.api.create_memory(corpus.name)
            job = self.mmt.api.import_into_memory(memory['id'], compact=corpus.file_path)
            self.mmt.wait_import_job(job)

            memories[corpus.name] = memory

        self._verify_index_integrity(memories)

    def test_add_with_all_language_combinations(self):
        memories = {}

        for corpus in self.ALL_CORPORA:
            memory = self.mmt.api.create_memory(corpus.name)

            job = None
            with corpus.reader_with_metadata() as reader:
                for tuid, src_lang, tgt_lang, src_line, tgt_line in reader:
                    job = self.mmt.api.append_to_memory(src_lang, tgt_lang, memory['id'], src_line, tgt_line, tuid=tuid)

            if job is not None:
                self.mmt.wait_import_job(job)

            memories[corpus.name] = memory

        self._verify_index_integrity(memories)

    def _verify_index_integrity(self, memories):
        time.sleep(3)  # wait Context Analyzer indexing timer

        # Verify Context Analyzer index
        for corpus in self.ACCEPTED_CORPORA:
            memory = memories[corpus.name]
            context = self._get_context(corpus.tgt_lang)

            self.assertEqual(1, len(context))
            self.assertIn(memory['id'], context)

        context = self._get_context('de')
        self.assertIsNone(context)  # 'de' is not supported but added to the model

        # Verify Memory index
        es_translation = self._translate(memories, 'es')
        fr_translation = self._translate(memories, 'fr')
        it_translation = self._translate(memories, 'it')
        zh_translation = self._translate(memories, 'zh')

        self.assertIn('Esto es ejemplo', es_translation)
        self.assertIn('C\'est', fr_translation)
        self.assertIn('Questo è un esempio', it_translation)
        self.assertIn('这是', zh_translation)

        try:
            self._translate(memories, 'de')
            self.fail('German should not be enabled for this engine')
        except ApiException as e:
            self.assertIn('UnsupportedLanguageException', e.cause)

        # Dump engine content
        self.mmt.stop()

        context_analyzer = self.mmt.dump_context_analyzer()
        translation_memory = self.mmt.dump_translation_memory()

        # Verify Context Analyzer content
        self.assertEqual(5, len(context_analyzer))

        for corpus in self.ALL_CORPORA:
            memory = memories[corpus.name]
            memory_id = int(memory['id'])

            self.assertIn(memory_id, context_analyzer)
            content = context_analyzer[memory_id]

            with corpus.reader_with_metadata() as reader:
                for tuid, src_lang, tgt_lang, src_line, tgt_line in reader:
                    self.assertIn((src_lang, tgt_lang, src_line), content)
                    self.assertIn((tgt_lang, src_lang, tgt_line), content)

        # Verify Memory content
        self.assertEqual(5, len(translation_memory))

        for corpus in self.ALL_CORPORA:
            memory = memories[corpus.name]
            memory_id = int(memory['id'])

            self.assertIn(memory_id, translation_memory)
            content = translation_memory[memory_id]

            with corpus.reader_with_metadata() as reader:
                for tuid, src_lang, tgt_lang, src_line, tgt_line in reader:
                    self.assertIn((tuid, src_lang, tgt_lang, src_line, tgt_line), content)


class OnlineLearningChannelsTest(__OnlineLearningTest):
    CONTENT = [('tuid-%d' % i, 'This is en__it example %d' % i, u'Questo è un esempio en__it %d' % i)
               for i in range(1, 9)]

    def _setup(self, context='full', memory='full'):
        def load(entries):
            job = None
            for tuid, segment, translation in entries:
                job = self.mmt.api.append_to_memory('en', 'it', 1, segment, translation, tuid=tuid)
            self.mmt.wait_import_job(job)

        def restore_model(lvl, path, path_bak):
            if lvl == 'full':
                shutil.rmtree(path_bak)
            else:
                shutil.rmtree(path)
                if lvl == 'partial':
                    os.rename(path_bak, path)

        assert context in ['full', 'partial', 'empty']
        assert memory in ['full', 'partial', 'empty']

        context_path = os.path.join(self.mmt.engine.models_path, 'context')
        context_path_bak = context_path + '.bak'
        memory_path = os.path.join(self.mmt.engine.models_path, 'decoder', 'memory')
        memory_path_bak = memory_path + '.bak'

        self.mmt.api.create_memory('test')
        load(self.CONTENT[:4])
        self.mmt.stop()

        os.rename(context_path, context_path_bak)
        os.rename(memory_path, memory_path_bak)

        self.mmt.start()
        load(self.CONTENT[4:])
        self.mmt.stop()

        restore_model(context, context_path, context_path_bak)
        restore_model(memory, memory_path, memory_path_bak)

    def _verify_index_integrity(self):
        # Dump engine content
        self.mmt.stop()

        context_analyzer = self.mmt.dump_context_analyzer()
        translation_memory = self.mmt.dump_translation_memory()

        # Verify Context Analyzer content
        self.assertEqual(1, len(context_analyzer))
        self.assertIn(1, context_analyzer)
        content = context_analyzer[1]

        for _, src_line, tgt_line in self.CONTENT:
            self.assertIn(('en', 'it', src_line), content)
            self.assertIn(('it', 'en', tgt_line), content)

        # Verify Memory content
        self.assertEqual(1, len(translation_memory))
        self.assertIn(1, translation_memory)
        content = translation_memory[1]

        for tuid, src_line, tgt_line in self.CONTENT:
            self.assertIn((tuid, 'en', 'it', src_line, tgt_line), content)

    # Tests

    def test_single_contribution(self):
        self.mmt.api.create_memory('test')
        job = self.mmt.api.append_to_memory('en', 'it', 1, 'Hello world', 'Ciao mondo')
        self.mmt.wait_import_job(job)

        self.assertEqual({0: 0, 1: 0}, self.mmt.get_channels())

        self.mmt.stop()

        context_analyzer = self.mmt.dump_context_analyzer()
        translation_memory = self.mmt.dump_translation_memory()

        # Verify Context Analyzer content
        self.assertEqual(1, len(context_analyzer))
        self.assertIn(1, context_analyzer)
        self.assertIn(('en', 'it', 'Hello world'), context_analyzer[1])
        self.assertIn(('it', 'en', 'Ciao mondo'), context_analyzer[1])

        # Verify Memory content
        self.assertEqual(1, len(translation_memory))
        self.assertIn(1, translation_memory)
        self.assertIn((None, 'en', 'it', 'Hello world', 'Ciao mondo'), translation_memory[1])

    def test_upload_memory(self):
        self.mmt.api.create_memory('test')
        job = self.mmt.api.import_into_memory(1, compact=self.CORPUS_IT.file_path)
        self.mmt.wait_import_job(job)

        self.assertEqual({0: 7, 1: 0}, self.mmt.get_channels())
        self.mmt.stop()

        context_analyzer = self.mmt.dump_context_analyzer()
        translation_memory = self.mmt.dump_translation_memory()

        self.assertIn(1, context_analyzer)
        self.assertEqual(16, len(context_analyzer[1]))
        self.assertIn(1, translation_memory)
        self.assertEqual(8, len(translation_memory[1]))

    def test_updating_from_scratch_all(self):
        self._setup(context='empty', memory='empty')

        self.mmt.start()
        self.assertEqual({0: 0, 1: 7}, self.mmt.get_channels())
        self._verify_index_integrity()

    def test_updating_from_scratch_context(self):
        self._setup(context='empty', memory='full')

        self.mmt.start()
        self.assertEqual({0: 0, 1: 7}, self.mmt.get_channels())
        self._verify_index_integrity()

    def test_updating_from_scratch_memory(self):
        self._setup(context='full', memory='empty')

        self.mmt.start()
        self.assertEqual({0: 0, 1: 7}, self.mmt.get_channels())
        self._verify_index_integrity()

    def test_updating_partial_all(self):
        self._setup(context='partial', memory='partial')

        self.mmt.start()
        self.assertEqual({0: 0, 1: 7}, self.mmt.get_channels())
        self._verify_index_integrity()

    def test_updating_partial_context(self):
        self._setup(context='partial', memory='full')

        self.mmt.start()
        self.assertEqual({0: 0, 1: 7}, self.mmt.get_channels())
        self._verify_index_integrity()

    def test_updating_partial_memory(self):
        self._setup(context='full', memory='partial')

        self.mmt.start()
        self.assertEqual({0: 0, 1: 7}, self.mmt.get_channels())
        self._verify_index_integrity()


class OnlineLearningUpdateTest(__OnlineLearningTest):
    def _setup(self):
        memory_a = self.mmt.api.create_memory('Memory A')
        self.mmt.api.append_to_memory('en', 'it', memory_a['id'], 'Test A1', 'Prova A1', tuid='tuid-001')
        self.mmt.api.append_to_memory('en', 'it', memory_a['id'], 'Test A2', 'Prova A2', tuid='tuid-002')
        self.mmt.api.append_to_memory('en', 'fr', memory_a['id'], 'Test A1', 'Essai A1', tuid='tuid-001')

        memory_b = self.mmt.api.create_memory('Memory B')
        self.mmt.api.append_to_memory('en', 'it', memory_b['id'], 'Test B1', 'Prova B1', tuid='tuid-001')
        self.mmt.api.append_to_memory('en', 'it', memory_b['id'], 'Test B2', 'Prova B2', tuid='tuid-002')
        job = self.mmt.api.append_to_memory('en', 'fr', memory_b['id'], 'Test B1', 'Essai B1', tuid='tuid-001')

        self.mmt.wait_import_job(job)

        return memory_a['id'], memory_b['id']

    def test_add(self):
        memory_a, memory_b = self._setup()

        self.mmt.stop()

        translation_memory = self.mmt.dump_translation_memory()

        self.assertEqual(2, len(translation_memory))
        self.assertIn(memory_a, translation_memory)
        self.assertIn(memory_b, translation_memory)

        content_a = translation_memory[memory_a]
        self.assertIn(('tuid-001', 'en', 'it', 'Test A1', 'Prova A1'), content_a)
        self.assertIn(('tuid-002', 'en', 'it', 'Test A2', 'Prova A2'), content_a)
        self.assertIn(('tuid-001', 'en', 'fr', 'Test A1', 'Essai A1'), content_a)

        content_b = translation_memory[memory_b]
        self.assertIn(('tuid-001', 'en', 'it', 'Test B1', 'Prova B1'), content_b)
        self.assertIn(('tuid-002', 'en', 'it', 'Test B2', 'Prova B2'), content_b)
        self.assertIn(('tuid-001', 'en', 'fr', 'Test B1', 'Essai B1'), content_b)

    def test_replace_by_match(self):
        memory_a, memory_b = self._setup()

        self.mmt.api.replace_in_memory('en', 'it', memory_a, 'NEW Test A1', 'NEW Prova A1', 'Test A1', 'Prova A1')
        self.mmt.api.replace_in_memory('en', 'it', memory_a, 'NEW Test A2', 'NEW Prova A2', 'Test A2', 'Prova A2')
        self.mmt.api.replace_in_memory('en', 'fr', memory_a, 'NEW Test A1', 'NEW Essai A1', 'Test A1', 'Essai A1')

        self.mmt.api.replace_in_memory('en', 'it', memory_b, 'NEW Test B1', 'NEW Prova B1', 'Test B1', 'Prova B1')
        self.mmt.api.replace_in_memory('en', 'it', memory_b, 'NEW Test B2', 'NEW Prova B2', 'Test B2', 'Prova B2')
        job = self.mmt.api.replace_in_memory('en', 'fr', memory_b, 'NEW Test B1', 'NEW Essai B1', 'Test B1', 'Essai B1')

        self.mmt.wait_import_job(job)

        self.mmt.stop()

        translation_memory = self.mmt.dump_translation_memory()

        self.assertEqual(2, len(translation_memory))
        self.assertIn(memory_a, translation_memory)
        self.assertIn(memory_b, translation_memory)

        content_a = translation_memory[memory_a]
        self.assertIn((None, 'en', 'it', 'NEW Test A1', 'NEW Prova A1'), content_a)
        self.assertIn((None, 'en', 'it', 'NEW Test A2', 'NEW Prova A2'), content_a)
        self.assertIn((None, 'en', 'fr', 'NEW Test A1', 'NEW Essai A1'), content_a)

        content_b = translation_memory[memory_b]
        self.assertIn((None, 'en', 'it', 'NEW Test B1', 'NEW Prova B1'), content_b)
        self.assertIn((None, 'en', 'it', 'NEW Test B2', 'NEW Prova B2'), content_b)
        self.assertIn((None, 'en', 'fr', 'NEW Test B1', 'NEW Essai B1'), content_b)

    def test_replace_by_tuid(self):
        memory_a, memory_b = self._setup()

        self.mmt.api.replace_in_memory('en', 'it', memory_a, 'NEW Test A1', 'NEW Prova A1', tuid='tuid-001')
        self.mmt.api.replace_in_memory('en', 'it', memory_a, 'NEW Test A2', 'NEW Prova A2', tuid='tuid-002')
        self.mmt.api.replace_in_memory('en', 'fr', memory_a, 'NEW Test A1', 'NEW Essai A1', tuid='tuid-001')

        self.mmt.api.replace_in_memory('en', 'it', memory_b, 'NEW Test B1', 'NEW Prova B1', tuid='tuid-001')
        self.mmt.api.replace_in_memory('en', 'it', memory_b, 'NEW Test B2', 'NEW Prova B2', tuid='tuid-002')
        job = self.mmt.api.replace_in_memory('en', 'fr', memory_b, 'NEW Test B1', 'NEW Essai B1', tuid='tuid-001')

        self.mmt.wait_import_job(job)

        self.mmt.stop()

        translation_memory = self.mmt.dump_translation_memory()

        self.assertEqual(2, len(translation_memory))
        self.assertIn(memory_a, translation_memory)
        self.assertIn(memory_b, translation_memory)

        content_a = translation_memory[memory_a]
        self.assertIn(('tuid-001', 'en', 'it', 'NEW Test A1', 'NEW Prova A1'), content_a)
        self.assertIn(('tuid-002', 'en', 'it', 'NEW Test A2', 'NEW Prova A2'), content_a)
        self.assertIn(('tuid-001', 'en', 'fr', 'NEW Test A1', 'NEW Essai A1'), content_a)

        content_b = translation_memory[memory_b]
        self.assertIn(('tuid-001', 'en', 'it', 'NEW Test B1', 'NEW Prova B1'), content_b)
        self.assertIn(('tuid-002', 'en', 'it', 'NEW Test B2', 'NEW Prova B2'), content_b)
        self.assertIn(('tuid-001', 'en', 'fr', 'NEW Test B1', 'NEW Essai B1'), content_b)