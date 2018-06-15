# coding=utf-8
import os
import tarfile
import unittest

import time

from commons import ModernMT, CompactCorpus

RES_FOLDER = os.path.abspath(os.path.join(__file__, os.pardir, 'res', 'privacy'))


class _PrivacyTest(unittest.TestCase):
    USER_1 = '00000000-0000-0000-0000-000000000001'
    USER_2 = '00000000-0000-0000-0000-000000000002'

    mmt = ModernMT('PrivacyTest')
    _engine_tar = os.path.join(RES_FOLDER, 'engine.tar.gz')

    def setUp(self):
        self.mmt.delete_engine()

        tar = tarfile.open(self._engine_tar, 'r:gz')
        tar.extractall(os.path.abspath(os.path.join(self.mmt.engine_path, os.pardir)))
        tar.close()

        self.mmt.start(verbosity=2)

    def tearDown(self):
        self.mmt.stop()
        self.mmt.delete_engine()

    def _setup_with_memories(self):
        a = self.mmt.api.create_memory('A')
        b = self.mmt.api.create_memory('B', owner=self.USER_1)
        c = self.mmt.api.create_memory('B', owner=self.USER_2)

        self.assertNotIn('owner', a)
        self.assertEqual(self.USER_1, b['owner'])
        self.assertEqual(self.USER_2, c['owner'])

        self.mmt.import_corpus(compact=CompactCorpus(os.path.join(RES_FOLDER, 'Memory.A.cpt')).path, memory=a['id'])
        self.mmt.import_corpus(compact=CompactCorpus(os.path.join(RES_FOLDER, 'Memory.B.cpt')).path, memory=b['id'])
        self.mmt.import_corpus(compact=CompactCorpus(os.path.join(RES_FOLDER, 'Memory.C.cpt')).path, memory=c['id'])

    def _setup_with_contributions(self):
        a = self.mmt.api.create_memory('A')
        b = self.mmt.api.create_memory('B', owner=self.USER_1)
        c = self.mmt.api.create_memory('B', owner=self.USER_2)

        self.assertNotIn('owner', a)
        self.assertEqual(self.USER_1, b['owner'])
        self.assertEqual(self.USER_2, c['owner'])

        with CompactCorpus(os.path.join(RES_FOLDER, 'Memory.A.cpt')).reader() as reader:
            for s, t, sentence, translation in reader:
                self.mmt.add_contributions(s, t, [(sentence, translation)], memory=1)

        with CompactCorpus(os.path.join(RES_FOLDER, 'Memory.B.cpt')).reader() as reader:
            for s, t, sentence, translation in reader:
                self.mmt.add_contributions(s, t, [(sentence, translation)], memory=2)

        with CompactCorpus(os.path.join(RES_FOLDER, 'Memory.C.cpt')).reader() as reader:
            for s, t, sentence, translation in reader:
                self.mmt.add_contributions(s, t, [(sentence, translation)], memory=3)


class ContextAnalyzerPrivacyTest(_PrivacyTest):
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
        time.sleep(12)  # wait indexing
        self._run()

    def test_contributions(self):
        self._setup_with_contributions()
        time.sleep(12)  # wait indexing
        self._run()


class TranslationMemoryPrivacyTest(_PrivacyTest):
    def _listen_for_translation(self):
        class __Listener:
            def __init__(self, path):
                self._path = path

            def __enter__(self):
                with open(self._path, 'rb') as f:
                    f.seek(0, 2)
                    self._start = f.tell()
                return self

            def __exit__(self, exc_type, exc_val, exc_tb):
                with open(self._path, 'rb') as f:
                    f.seek(self._start)

                    begin = False
                    lines = []

                    for line in f:
                        line = line.strip()
                        if begin:
                            if line == ']':
                                break
                            else:
                                lines.append(line)
                        elif line == 'suggestions = [':
                            begin = True

                    self.suggestions = []
                    for line in lines:
                        self.suggestions.append(self._parse_suggestion(line))

            @staticmethod
            def _parse_suggestion(line):
                line = line.replace(' ', '').replace('ScoreEntry{memory=', '').rstrip('}')
                memory, line = line.split(',', 1)
                _, line = line.split('[', 1)
                s, line = line.split(']', 1)
                _, line = line.split('[', 1)
                t, line = line.split(']', 1)

                return int(memory), s.replace(',', ' ').decode('utf-8'), t.replace(',', ' ').decode('utf-8')

        return __Listener(os.path.join(self.mmt.engine_runtime_path, 'logs', 'node.log'))

    def _translate(self, text, user=None, context=None):
        context = [{'memory': k, 'score': v} for k, v in context.iteritems()] if context is not None else None
        return self.mmt.api.translate('en', 'it', text, user=user, context=context)['translation']

    def _run(self):
        # Public
        with self._listen_for_translation() as translation:
            self._translate('This is an example')

        self.assertEqual(1, len(translation.suggestions))
        self.assertIn((1, u'This is an example A', u'Questo è un esempio A'), translation.suggestions)

        # User 1
        with self._listen_for_translation() as translation:
            self._translate('This is an example', user=self.USER_1)

        self.assertEqual(2, len(translation.suggestions))
        self.assertIn((1, u'This is an example A', u'Questo è un esempio A'), translation.suggestions)
        self.assertIn((2, u'This is an example B', u'Questo è un esempio B'), translation.suggestions)

        with self._listen_for_translation() as translation:
            self._translate('This is an example', user=self.USER_1, context={3: 1})

        self.assertEqual(3, len(translation.suggestions))
        self.assertIn((1, u'This is an example A', u'Questo è un esempio A'), translation.suggestions)
        self.assertIn((2, u'This is an example B', u'Questo è un esempio B'), translation.suggestions)
        self.assertIn((3, u'This is an example C', u'Questo è un esempio C'), translation.suggestions)

        # User 2
        with self._listen_for_translation() as translation:
            self._translate('This is an example', user=self.USER_2)

        self.assertEqual(2, len(translation.suggestions))
        self.assertIn((1, u'This is an example A', u'Questo è un esempio A'), translation.suggestions)
        self.assertIn((3, u'This is an example C', u'Questo è un esempio C'), translation.suggestions)

        with self._listen_for_translation() as translation:
            self._translate('This is an example', user=self.USER_2, context={2: 1})

        self.assertEqual(3, len(translation.suggestions))
        self.assertIn((1, u'This is an example A', u'Questo è un esempio A'), translation.suggestions)
        self.assertIn((2, u'This is an example B', u'Questo è un esempio B'), translation.suggestions)
        self.assertIn((3, u'This is an example C', u'Questo è un esempio C'), translation.suggestions)

    def test_memory_import(self):
        self._setup_with_memories()
        self._run()

    def test_contributions(self):
        self._setup_with_contributions()
        self._run()
