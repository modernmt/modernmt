# coding=utf-8
import os
import shutil
import tarfile
import unittest

from commons import ModernMT, CompactCorpus

RES_FOLDER = os.path.abspath(os.path.join(__file__, os.pardir, 'res', 'onlinelearning'))


class _OnlineLearningTest(unittest.TestCase):
    """
    Content of engine.xconf:

    <engine type="neural">
        <languages>
            <pair source="en" target="fr" />
            <pair source="fr" target="en" />

            <pair source="en" target="it" />

            <pair source="en" target="es-ES" />
            <pair source="en" target="es-MX" />

            <pair source="en" target="zh-TW" />
            <pair source="en" target="zh-CN" />
            <pair source="zh" target="en" />

            <rules>
                <rule lang="zh" from="zh-HK" to="zh-TW" />
                <rule lang="zh" from="*" to="zh-CN" />

                <rule lang="es" from="es" to="es-ES" />
                <rule lang="es" from="*" to="es-MX" />
            </rules>
        </languages>
    </engine>
    """
    mmt = ModernMT('OnlineLearningTest')
    _engine_tar = os.path.join(RES_FOLDER, 'engine.tar.gz')

    def setUp(self):
        self.mmt.delete_engine()

        tar = tarfile.open(self._engine_tar, 'r:gz')
        tar.extractall(os.path.abspath(os.path.join(self.mmt.engine_path, os.pardir)))
        tar.close()

        self.mmt.start()

    def tearDown(self):
        self.mmt.stop()
        self.mmt.delete_engine()

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


class OnlineLearningLanguageTest(_OnlineLearningTest):
    def test_import_with_all_language_combinations(self):
        memories = {}

        for source, target in [('en', 'de'), ('en', 'fr'), ('en', 'it'), ('en', 'es'), ('en', 'zh')]:
            corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.%s__%s.cpt' % (source, target)))
            memory = self.mmt.import_corpus(compact=corpus.path)

            memories['%s_%s' % (source, target)] = memory

        self._verify_index_integrity(memories)

    def test_add_with_all_language_combinations(self):
        memories = {}

        for source, target in [('en', 'de'), ('en', 'fr'), ('en', 'it'), ('en', 'es'), ('en', 'zh')]:
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
        tm_content = self.mmt.memory.dump()

        self.assertEqual({2, 3, 4, 5}, self.mmt.context_analyzer.get_domains())
        self.assertEqual({2, 3, 4, 5}, tm_content.get_domains())

        # en__de
        memory = memories['en_de']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'de')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'de', 'en')
        mem_data = tm_content.get_content(memory['id'], 'de', 'en')

        self.assertEqual(0, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertEqual(0, len(mem_data))

        # en__fr
        memory = memories['en_fr']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'fr')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'fr', 'en')
        mem_data = tm_content.get_content(memory['id'], 'en', 'fr')

        self.assertEqual(8, len(ctx_source))
        self.assertEqual(8, len(ctx_target))

        for i in range(1, 9):
            self.assertInContent(ctx_source, u'This is en__fr example ' + (u'O' * i))
        for i in range(1, 9):
            self.assertInContent(ctx_target, u'C\'est en__fr exemple ' + (u'O' * i))

        self.assertEqual(8, len(mem_data))

        for i in range(1, 9):
            self.assertInParallelContent(mem_data,
                                         u'This is en__fr example ' + (u'O' * i),
                                         u'C\'est en__fr exemple ' + (u'O' * i))

        # en__it
        memory = memories['en_it']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'it')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'it', 'en')
        mem_data = tm_content.get_content(memory['id'], 'en', 'it')

        self.assertEqual(8, len(ctx_source))
        self.assertEqual(8, len(ctx_target))

        for i in range(1, 9):
            self.assertInContent(ctx_source, u'This is en__it example ' + (u'O' * i))
        for i in range(1, 9):
            self.assertInContent(ctx_target, u'Questo è un esempio en__it ' + (u'O' * i))

        self.assertEqual(8, len(mem_data))

        for i in range(1, 9):
            self.assertInParallelContent(mem_data,
                                         u'This is en__it example ' + (u'O' * i),
                                         u'Questo è un esempio en__it ' + (u'O' * i))

        # en__es
        memory = memories['en_es']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'es')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'es', 'en')
        es_mem_data = tm_content.get_content(memory['id'], 'en', 'es-ES')
        mx_mem_data = tm_content.get_content(memory['id'], 'en', 'es-MX')

        self.assertEqual(10, len(ctx_source))
        self.assertEqual(10, len(ctx_target))

        for i in range(1, 11):
            self.assertInContent(ctx_source, u'This is en__es example ' + (u'O' * i))
        for i in range(1, 11):
            self.assertInContent(ctx_target, u'Esto es ejemplo en__es ' + (u'O' * i))

        self.assertEqual(4, len(es_mem_data))

        for i in [1, 2, 6, 7]:
            self.assertInParallelContent(es_mem_data,
                                         u'This is en__es example ' + (u'O' * i),
                                         u'Esto es ejemplo en__es ' + (u'O' * i))

        self.assertEqual(6, len(mx_mem_data))

        for i in [3, 4, 5, 8, 9, 10]:
            self.assertInParallelContent(mx_mem_data,
                                         u'This is en__es example ' + (u'O' * i),
                                         u'Esto es ejemplo en__es ' + (u'O' * i))

        # en__zh
        memory = memories['en_zh']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'zh')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'zh', 'en')
        cn_mem_data = tm_content.get_content(memory['id'], 'en', 'zh-CN')
        tw_mem_data = tm_content.get_content(memory['id'], 'en', 'zh-TW')
        mem_data = tm_content.get_content(memory['id'], 'en', 'zh')

        self.assertEqual(12, len(ctx_source))
        self.assertEqual(12, len(ctx_target))

        for i in range(1, 13):
            self.assertInContent(ctx_source, u'The en__zh example ' + (u'O' * i))
        for i in range(1, 13):
            self.assertInContent(ctx_target, u'这是en__zh例子' + (u'O' * i))

        self.assertEqual(6, len(cn_mem_data))
        self.assertEqual(6, len(tw_mem_data))
        self.assertEqual(0, len(mem_data))

        for i in [1, 2, 3, 7, 8, 9]:
            self.assertInParallelContent(cn_mem_data,
                                         u'The en__zh example ' + (u'O' * i),
                                         u'这是en__zh例子' + (u'O' * i))
        for i in [4, 5, 6, 10, 11, 12]:
            self.assertInParallelContent(tw_mem_data,
                                         u'The en__zh example ' + (u'O' * i),
                                         u'这是en__zh例子' + (u'O' * i))


class OnlineLearningChannelsTest(_OnlineLearningTest):
    def setUp(self):
        super(OnlineLearningChannelsTest, self).setUp()

    def _prepare_partial(self, context=True, memory=True):
        self.mmt.add_contributions('en', 'it', [
            (u'This is en__it example O', u'Questo è un esempio en__it O'),
            (u'This is en__it example OO', u'Questo è un esempio en__it OO'),
            (u'This is en__it example OOO', u'Questo è un esempio en__it OOO'),
            (u'This is en__it example OOOO', u'Questo è un esempio en__it OOOO')])
        self.mmt.stop()

        os.rename(self.mmt.context_analyzer.path, self.mmt.context_analyzer.path + '.bak')
        os.rename(self.mmt.memory.path, self.mmt.memory.path + '.bak')

        self.mmt.start()
        self.mmt.add_contributions('en', 'it', [
            (u'This is en__it example OOOOO', u'Questo è un esempio en__it OOOOO'),
            (u'This is en__it example OOOOOO', u'Questo è un esempio en__it OOOOOO'),
            (u'This is en__it example OOOOOOO', u'Questo è un esempio en__it OOOOOOO'),
            (u'This is en__it example OOOOOOOO', u'Questo è un esempio en__it OOOOOOOO')], memory=1)
        self.mmt.stop()

        if context:
            shutil.rmtree(self.mmt.context_analyzer.path)
            os.rename(self.mmt.context_analyzer.path + '.bak', self.mmt.context_analyzer.path)

        if memory:
            shutil.rmtree(self.mmt.memory.path)
            os.rename(self.mmt.memory.path + '.bak', self.mmt.memory.path)

        self.mmt.start()

    def _verify_index_integrity(self):
        ctx_source = self.mmt.context_analyzer.get_content(1, 'en', 'it')
        ctx_target = self.mmt.context_analyzer.get_content(1, 'it', 'en')
        mem_data = self.mmt.memory.dump().get_content(1, 'en', 'it')

        self.assertEqual(8, len(ctx_source))
        self.assertEqual(8, len(ctx_target))

        for i in range(1, 9):
            self.assertInContent(ctx_source, u'This is en__it example ' + (u'O' * i))
        for i in range(1, 9):
            self.assertInContent(ctx_target, u'Questo è un esempio en__it ' + (u'O' * i))

        self.assertEqual(8, len(mem_data))

        for i in range(1, 9):
            self.assertInParallelContent(mem_data,
                                         u'This is en__it example ' + (u'O' * i),
                                         u'Questo è un esempio en__it ' + (u'O' * i))

    # Tests

    def test_single_contribution(self):
        self.mmt.add_contributions('en', 'it', [(u'Hello world', u'Ciao mondo')])

        ctx_source = self.mmt.context_analyzer.get_content(1, 'en', 'it')
        ctx_target = self.mmt.context_analyzer.get_content(1, 'it', 'en')
        mem_data = self.mmt.memory.dump().get_content(1, 'en', 'it')

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(0, 0))

        self.assertEqual(1, len(ctx_source))
        self.assertEqual(1, len(ctx_target))
        self.assertEqual(1, len(mem_data))

        self.assertInContent(ctx_source, u'Hello world')
        self.assertInContent(ctx_target, u'Ciao mondo')
        self.assertInParallelContent(mem_data, u'Hello world', u'Ciao mondo')

    def test_upload_domain(self):
        corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.en__it.cpt'))
        self.mmt.import_corpus(compact=corpus.path)

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(7, 0))
        self._verify_index_integrity()

    def test_updating_from_scratch_all(self):
        corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.en__it.cpt'))
        self.mmt.import_corpus(compact=corpus.path)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(7, 0))

        self.mmt.stop()
        shutil.rmtree(self.mmt.memory.path)
        shutil.rmtree(self.mmt.context_analyzer.path)
        self.mmt.start()

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(7, 0))
        self._verify_index_integrity()

    def test_updating_from_scratch_context(self):
        corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.en__it.cpt'))
        self.mmt.import_corpus(compact=corpus.path)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(7, 0))

        self.mmt.stop()
        shutil.rmtree(self.mmt.context_analyzer.path)
        self.mmt.start()

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(7, 0))
        self._verify_index_integrity()

    def test_updating_from_scratch_memory(self):
        corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.en__it.cpt'))
        self.mmt.import_corpus(compact=corpus.path)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(7, 0))

        self.mmt.stop()
        shutil.rmtree(self.mmt.memory.path)
        self.mmt.start()

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(7, 0))
        self._verify_index_integrity()

    def test_updating_partial_all(self):
        self._prepare_partial()
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(0, 7))
        self._verify_index_integrity()

    def test_updating_partial_context(self):
        self._prepare_partial(context=True, memory=False)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(0, 7))
        self._verify_index_integrity()

    def test_updating_partial_memory(self):
        self._prepare_partial(context=False, memory=True)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(0, 7))
        self._verify_index_integrity()
