import os
import time

from cli.mmt.fileformats import CompactFileFormat
from testcases import ModernMTTestCase, TEST_RESOURCES


class __PrivacyTest(ModernMTTestCase):
    mmt_engine_archive = os.path.join(TEST_RESOURCES, 'stub_en_it_engine.tar.gz')

    USER_1 = '00000000-0000-0000-0000-000000000001'
    USER_2 = '00000000-0000-0000-0000-000000000002'

    CORPUS_A = CompactFileFormat('en', 'it', os.path.join(TEST_RESOURCES, 'privacy', 'Memory-A.cfc'))
    CORPUS_B = CompactFileFormat('en', 'it', os.path.join(TEST_RESOURCES, 'privacy', 'Memory-B.cfc'))
    CORPUS_C = CompactFileFormat('en', 'it', os.path.join(TEST_RESOURCES, 'privacy', 'Memory-C.cfc'))

    def _setup_with_memories(self):
        a = self.mmt.api.create_memory('A')
        b = self.mmt.api.create_memory('B', owner=self.USER_1)
        c = self.mmt.api.create_memory('C', owner=self.USER_2)

        self.assertNotIn('owner', a)
        self.assertEqual(self.USER_1, b['owner'])
        self.assertEqual(self.USER_2, c['owner'])

        self.mmt.api.import_into_memory(compact=self.CORPUS_A.file_path, memory=a['id'])
        self.mmt.api.import_into_memory(compact=self.CORPUS_B.file_path, memory=b['id'])
        import_job = self.mmt.api.import_into_memory(compact=self.CORPUS_C.file_path, memory=c['id'])

        self.mmt.wait_import_job(import_job)

    def _setup_with_contributions(self):
        a = self.mmt.api.create_memory('A')
        b = self.mmt.api.create_memory('B', owner=self.USER_1)
        c = self.mmt.api.create_memory('C', owner=self.USER_2)

        self.assertNotIn('owner', a)
        self.assertEqual(self.USER_1, b['owner'])
        self.assertEqual(self.USER_2, c['owner'])

        import_job = None

        with self.CORPUS_A.reader() as reader:
            for src, tgt in reader:
                import_job = self.mmt.api.append_to_memory('en', 'it', a['id'], src, tgt)

        with self.CORPUS_B.reader() as reader:
            for src, tgt in reader:
                import_job = self.mmt.api.append_to_memory('en', 'it', b['id'], src, tgt)

        with self.CORPUS_C.reader() as reader:
            for src, tgt in reader:
                import_job = self.mmt.api.append_to_memory('en', 'it', c['id'], src, tgt)

        self.mmt.wait_import_job(import_job)


class ContextAnalyzerPrivacyTest(__PrivacyTest):
    def _get_context(self, text, user=None):
        ctx = self.mmt.api.get_context_s('en', 'it', text, limit=100, user=user)
        return [e['memory']['id'] for e in ctx]

    def _run(self):
        public_context = self._get_context('This is an example')
        self.assertEqual(1, len(public_context))
        self.assertIn(1, public_context)

        user1_context = self._get_context('This is an example', user=self.USER_1)
        self.assertEqual(2, len(user1_context))
        self.assertIn(1, user1_context)
        self.assertIn(2, user1_context)

        user2_context = self._get_context('This is an example', user=self.USER_2)
        self.assertEqual(2, len(user2_context))
        self.assertIn(1, user2_context)
        self.assertIn(3, user2_context)

    def test_memory_import(self):
        self._setup_with_memories()
        time.sleep(3)  # wait Context Analyzer indexing timer
        self._run()

    def test_contributions(self):
        self._setup_with_contributions()
        time.sleep(3)  # wait indexing
        self._run()


class TranslationMemoryPrivacyTest(__PrivacyTest):
    def _translate(self, text, user=None, context=None):
        with self.mmt.log_listener().listen_for_translation() as result:
            context = [{'memory': k, 'score': v} for k, v in context.items()] if context is not None else None
            _ = self.mmt.api.translate('en', 'it', text, user=user, context=context)['translation']
        return result.translation

    def _run(self):
        # Empty context
        translation = self._translate('This is an example')
        suggestions = [s.sentence for s in translation.suggestions]

        self.assertEqual('This is an example', translation.sentence)
        self.assertEqual(0, len(suggestions))

        # Wrong context
        translation = self._translate('This is an example', context={100: 1})
        suggestions = [s.sentence for s in translation.suggestions]

        self.assertEqual('This is an example', translation.sentence)
        self.assertEqual(0, len(suggestions))

        # Context with memory B
        translation = self._translate('This is an example', context={2: 1})
        suggestions = [s.sentence for s in translation.suggestions]

        self.assertEqual('This is an example', translation.sentence)
        self.assertEqual(1, len(suggestions))
        self.assertIn('This is an example B', suggestions)

        # Context with memory C
        translation = self._translate('This is an example', context={3: 1})
        suggestions = [s.sentence for s in translation.suggestions]

        self.assertEqual('This is an example', translation.sentence)
        self.assertEqual(1, len(suggestions))
        self.assertIn('This is an example C', suggestions)

        # Context with all memories
        translation = self._translate('This is an example', context={1: 1, 2: 1, 3: 1})
        suggestions = [s.sentence for s in translation.suggestions]

        self.assertEqual('This is an example', translation.sentence)
        self.assertEqual(3, len(suggestions))
        self.assertIn('This is an example A', suggestions)
        self.assertIn('This is an example B', suggestions)
        self.assertIn('This is an example C', suggestions)

    def test_memory_import(self):
        self._setup_with_memories()
        self._run()

    def test_contributions(self):
        self._setup_with_contributions()
        self._run()
