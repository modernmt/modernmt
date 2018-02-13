# coding=utf-8
import os
import shutil
import tarfile
import unittest

from commons import ModernMT, CompactCorpus

RES_FOLDER = os.path.abspath(os.path.join(__file__, os.pardir, 'res', 'onlinelearning'))


class OnlineLearningTest(unittest.TestCase):
    mmt = ModernMT('OnlineLearningTest')

    _engine_tar = os.path.join(RES_FOLDER, 'engine.tar.gz')

    def _delete_engine(self):
        shutil.rmtree(os.path.join(self.mmt.engines_path, 'OnlineLearningTest'), ignore_errors=True)

    def setUp(self):
        self._delete_engine()

        tar = tarfile.open(self._engine_tar, 'r:gz')
        tar.extractall(self.mmt.engines_path)
        tar.close()

        self.mmt.start()

    def tearDown(self):
        self.mmt.stop()
        # self._delete_engine()

    # Assertion

    def assertContentContains(self, content, element):
        element = ''.join(element.split())
        content = [''.join(line.split()) for line in content]

        self.assertIn(element, content)

    # Tests

    def test_import_with_all_language_combinations(self):
        memories = {}

        for source, target in [('en', 'de'), ('en', 'fr'), ('en', 'it'), ('en', 'pt'), ('en', 'zh')]:
            corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.%s__%s.cpt' % (source, target)))
            job, memory = self.mmt.import_corpus(compact=corpus.path)

            self.mmt.wait_job(job)

            memories['%s_%s' % (source, target)] = memory

        self._verify_index_integrity(memories)

    def test_add_with_all_language_combinations(self):
        memories = {}

        for source, target in [('en', 'de'), ('en', 'fr'), ('en', 'it'), ('en', 'pt'), ('en', 'zh')]:
            corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.%s__%s.cpt' % (source, target)))
            memory = self.mmt.api.create_memory('Memory.%s__%s' % (source, target))

            job = None
            with corpus.reader() as reader:
                for s, t, sentence, translation in reader:
                    job = self.mmt.api.append_to_memory(s, t, memory['id'], sentence, translation)

            if job is not None:
                self.mmt.wait_job(job)

            memories['%s_%s' % (source, target)] = memory

        self._verify_index_integrity(memories)

    def _verify_index_integrity(self, memories):
        # ----------------------
        # Check Context Analyzer
        # ----------------------
        self.assertEqual({2, 3, 4, 5}, self.mmt.context_analyzer.get_domains())

        # en__de
        memory = memories['en_de']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'de')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'de', 'en')

        self.assertEqual(0, len(ctx_source))
        self.assertEqual(0, len(ctx_target))

        # en__fr
        memory = memories['en_fr']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'fr')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'fr', 'en')

        self.assertEqual(2, len(ctx_source))
        self.assertEqual(2, len(ctx_target))
        self.assertContentContains(ctx_source, u'This is en__fr example one')
        self.assertContentContains(ctx_source, u'This is en__fr example two')
        self.assertContentContains(ctx_target, u'C\'est en__fr exemple un')
        self.assertContentContains(ctx_target, u'C\'est en__fr exemple deux')

        # en__it
        memory = memories['en_it']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'it')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'it', 'en')

        self.assertEqual(4, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertContentContains(ctx_source, u'This is en__it example one')
        self.assertContentContains(ctx_source, u'This is en__it example two')
        self.assertContentContains(ctx_source, u'This is en__it example three')
        self.assertContentContains(ctx_source, u'This is en__it example four')

        # en__pt
        memory = memories['en_pt']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'pt')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'pt', 'en')

        self.assertEqual(0, len(ctx_source))
        self.assertEqual(4, len(ctx_target))
        self.assertContentContains(ctx_target, u'Este é en__pt exemplo um')
        self.assertContentContains(ctx_target, u'Este é en__pt exemplo dois')
        self.assertContentContains(ctx_target, u'Este é en__pt exemplo três')
        self.assertContentContains(ctx_target, u'Este é en__pt exemplo quatro')

        # en__pt-PT
        memory = memories['en_pt']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'pt-PT')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'pt-PT', 'en')

        self.assertEqual(2, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertContentContains(ctx_source, u'This is en__pt example one')
        self.assertContentContains(ctx_source, u'This is en__pt example three')

        # en__pt-BR
        memory = memories['en_pt']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'pt-BR')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'pt-BR', 'en')

        self.assertEqual(3, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertContentContains(ctx_source, u'This is en__pt example one')
        self.assertContentContains(ctx_source, u'This is en__pt example two')
        self.assertContentContains(ctx_source, u'This is en__pt example four')

        # en__zh
        memory = memories['en_zh']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'zh')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'zh', 'en')

        self.assertEqual(0, len(ctx_source))
        self.assertEqual(0, len(ctx_target))

        # en__zh-TW
        memory = memories['en_zh']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'zh-TW')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'zh-TW', 'en')

        self.assertEqual(2, len(ctx_source))
        self.assertEqual(2, len(ctx_target))

        self.assertContentContains(ctx_source, u'The en__zh example one')
        self.assertContentContains(ctx_source, u'This is en__zh example three')
        self.assertContentContains(ctx_target, u'en__zh例子之一')
        self.assertContentContains(ctx_target, u'這是en__zh例子三')

        # en__zh-CN
        memory = memories['en_zh']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'zh-CN')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'zh-CN', 'en')

        self.assertEqual(2, len(ctx_source))
        self.assertEqual(2, len(ctx_target))

        self.assertContentContains(ctx_source, u'The en__zh example one')
        self.assertContentContains(ctx_source, u'This is en__zh example two')
        self.assertContentContains(ctx_target, u'en__zh例子之一')
        self.assertContentContains(ctx_target, u'这是en__zh例子二')
