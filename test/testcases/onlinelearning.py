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

    # def test_add_with_all_language_combinations(self):
    #     memories = {}
    #
    #     for source, target in [('en', 'de'), ('en', 'fr'), ('en', 'it'), ('en', 'es'), ('en', 'zh')]:
    #         corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.%s__%s.cpt' % (source, target)))
    #         memory = self.mmt.api.create_memory('Memory.%s__%s' % (source, target))
    #
    #         job = None
    #         with corpus.reader() as reader:
    #             for s, t, sentence, translation in reader:
    #                 job = self.mmt.api.append_to_memory(s, t, memory['id'], sentence, translation)
    #
    #         if job is not None:
    #             self.mmt.wait_job(job)
    #
    #         memories['%s_%s' % (source, target)] = memory
    #
    #     self._verify_index_integrity(memories)

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

        self.assertEqual(2, len(ctx_source))
        self.assertEqual(2, len(ctx_target))
        self.assertInContent(ctx_source, u'This is en__fr example one')
        self.assertInContent(ctx_source, u'This is en__fr example two')
        self.assertInContent(ctx_target, u'C\'est en__fr exemple un')
        self.assertInContent(ctx_target, u'C\'est en__fr exemple deux')

        self.assertEqual(2, len(mem_data))
        self.assertInParallelContent(mem_data, u'This is en__fr example one', u'C\'est en__fr exemple un')
        self.assertInParallelContent(mem_data, u'This is en__fr example two', u'C\'est en__fr exemple deux')

        # en__it
        memory = memories['en_it']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'it')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'it', 'en')
        mem_data = tm_content.get_content(memory['id'], 'en', 'it')

        self.assertEqual(4, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertInContent(ctx_source, u'This is en__it example one')
        self.assertInContent(ctx_source, u'This is en__it example two')
        self.assertInContent(ctx_source, u'This is en__it example three')
        self.assertInContent(ctx_source, u'This is en__it example four')

        self.assertEqual(4, len(mem_data))
        self.assertInParallelContent(mem_data, u'This is en__it example one', u'Questo è un esempio en__it uno')
        self.assertInParallelContent(mem_data, u'This is en__it example two', u'Questo è un esempio en__it due')
        self.assertInParallelContent(mem_data, u'This is en__it example three', u'Questo è un esempio en__it tre')
        self.assertInParallelContent(mem_data, u'This is en__it example four', u'Questo è un esempio en__it quattro')

        # en__es
        memory = memories['en_es']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'es')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'es', 'en')
        es_mem_data = tm_content.get_content(memory['id'], 'en', 'es-ES')
        mx_mem_data = tm_content.get_content(memory['id'], 'en', 'es-MX')

        self.assertEqual(4, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertInContent(ctx_source, u'This is en__es example one')
        self.assertInContent(ctx_source, u'This is en__es example two')
        self.assertInContent(ctx_source, u'This is en__es example three')
        self.assertInContent(ctx_source, u'This is en__es example four')

        self.assertEqual(2, len(es_mem_data))
        self.assertEqual(2, len(mx_mem_data))
        self.assertInParallelContent(es_mem_data, u'This is en__es example one', u'Esto es ejemplo en__es uno')
        self.assertInParallelContent(es_mem_data, u'This is en__es example two', u'Esto es ejemplo en__es dos')
        self.assertInParallelContent(mx_mem_data, u'This is en__es example three', u'Esto es ejemplo en__es tres')
        self.assertInParallelContent(mx_mem_data, u'This is en__es example four', u'Esto es ejemplo en__es cuatro')

        # en__zh
        memory = memories['en_zh']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'zh')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'zh', 'en')
        cn_mem_data = tm_content.get_content(memory['id'], 'en', 'zh-CN')
        tw_mem_data = tm_content.get_content(memory['id'], 'en', 'zh-TW')
        mem_data = tm_content.get_content(memory['id'], 'en', 'zh')

        self.assertEqual(4, len(ctx_source))
        self.assertEqual(4, len(ctx_target))

        self.assertInContent(ctx_source, u'The en__zh example one')
        self.assertInContent(ctx_source, u'This is en__zh example two')
        self.assertInContent(ctx_source, u'This is en__zh example three')
        self.assertInContent(ctx_source, u'This is en__zh example four')
        self.assertInContent(ctx_target, u'en__zh例子之一')
        self.assertInContent(ctx_target, u'这是en__zh例子二')
        self.assertInContent(ctx_target, u'這是en__zh例子三')
        self.assertInContent(ctx_target, u'這是en__zh例子四')

        self.assertEqual(2, len(cn_mem_data))
        self.assertEqual(2, len(tw_mem_data))
        self.assertEqual(0, len(mem_data))
        self.assertInParallelContent(cn_mem_data, u'The en__zh example one', u'en__zh例子之一')
        self.assertInParallelContent(cn_mem_data, u'This is en__zh example two', u'这是en__zh例子二')
        self.assertInParallelContent(tw_mem_data, u'This is en__zh example three', u'這是en__zh例子三')
        self.assertInParallelContent(tw_mem_data, u'This is en__zh example four', u'這是en__zh例子四')


class OnlineLearningChannelsTest(_OnlineLearningTest):
    def setUp(self):
        super(OnlineLearningChannelsTest, self).setUp()

    def _prepare_partial(self, context=True, memory=True):
        self.mmt.add_contributions('en', 'it', [
            (u'This is en__it example one', u'Questo è un esempio en__it uno'),
            (u'This is en__it example two', u'Questo è un esempio en__it due')])
        self.mmt.stop()

        os.rename(self.mmt.context_analyzer.path, self.mmt.context_analyzer.path + '.bak')
        os.rename(self.mmt.memory.path, self.mmt.memory.path + '.bak')

        self.mmt.start()
        self.mmt.add_contributions('en', 'it', [
            (u'This is en__it example three', u'Questo è un esempio en__it tre'),
            (u'This is en__it example four', u'Questo è un esempio en__it quattro')], memory=1)
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

        self.assertEqual(4, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertEqual(4, len(mem_data))

        self.assertInContent(ctx_source, u'This is en__it example one')
        self.assertInContent(ctx_source, u'This is en__it example two')
        self.assertInContent(ctx_source, u'This is en__it example three')
        self.assertInContent(ctx_source, u'This is en__it example four')
        self.assertInParallelContent(mem_data, u'This is en__it example one', u'Questo è un esempio en__it uno')
        self.assertInParallelContent(mem_data, u'This is en__it example two', u'Questo è un esempio en__it due')
        self.assertInParallelContent(mem_data, u'This is en__it example three', u'Questo è un esempio en__it tre')
        self.assertInParallelContent(mem_data, u'This is en__it example four', u'Questo è un esempio en__it quattro')

    # Tests

    def test_single_contribution(self):
        self.mmt.add_contributions('en', 'it', [(u'Hello world', u'Ciao mondo')])

        ctx_source = self.mmt.context_analyzer.get_content(1, 'en', 'it')
        ctx_target = self.mmt.context_analyzer.get_content(1, 'it', 'en')
        mem_data = self.mmt.memory.dump().get_content(1, 'en', 'it')

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(0, 0))

        self.assertEqual(1, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertEqual(1, len(mem_data))

        self.assertInContent(ctx_source, u'Hello world')
        self.assertInParallelContent(mem_data, u'Hello world', u'Ciao mondo')

    def test_upload_domain(self):
        corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.en__it.cpt'))
        self.mmt.import_corpus(compact=corpus.path)

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(3, 0))
        self._verify_index_integrity()

    def test_updating_from_scratch_all(self):
        corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.en__it.cpt'))
        self.mmt.import_corpus(compact=corpus.path)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(3, 0))

        self.mmt.stop()
        shutil.rmtree(self.mmt.memory.path)
        shutil.rmtree(self.mmt.context_analyzer.path)
        self.mmt.start()

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(3, 0))
        self._verify_index_integrity()

    def test_updating_from_scratch_context(self):
        corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.en__it.cpt'))
        self.mmt.import_corpus(compact=corpus.path)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(3, 0))

        self.mmt.stop()
        shutil.rmtree(self.mmt.context_analyzer.path)
        self.mmt.start()

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(3, 0))
        self._verify_index_integrity()

    def test_updating_from_scratch_memory(self):
        corpus = CompactCorpus(os.path.join(RES_FOLDER, 'Memory.en__it.cpt'))
        self.mmt.import_corpus(compact=corpus.path)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(3, 0))

        self.mmt.stop()
        shutil.rmtree(self.mmt.memory.path)
        self.mmt.start()

        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(3, 0))
        self._verify_index_integrity()

    def test_updating_partial_all(self):
        self._prepare_partial()
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(0, 3))
        self._verify_index_integrity()

    def test_updating_partial_context(self):
        self._prepare_partial(context=True, memory=False)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(0, 3))
        self._verify_index_integrity()

    def test_updating_partial_memory(self):
        self._prepare_partial(context=False, memory=True)
        self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(0, 3))
        self._verify_index_integrity()
