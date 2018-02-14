# coding=utf-8
import os
import shutil
import tarfile
import unittest

from commons import ModernMT, CompactCorpus

RES_FOLDER = os.path.abspath(os.path.join(__file__, os.pardir, 'res', 'onlinelearning'))


class OnlineLearningLanguageTest(unittest.TestCase):
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
        self._delete_engine()

    # Assertion

    def assertInContent(self, content, element):
        element = ''.join(element.split())
        content = [''.join(line.split()) for line in content]

        self.assertIn(element, content)

    def assertInParallel(self, content, sentence, translation):
        sentence = ''.join(sentence.split())
        translation = ''.join(translation.split())
        content = [(''.join(s.split()), ''.join(t.split())) for s, t in content]

        self.assertIn((sentence, translation), content)

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
        self.assertInParallel(mem_data, u'This is en__fr example one', u'C\'est en__fr exemple un')
        self.assertInParallel(mem_data, u'This is en__fr example two', u'C\'est en__fr exemple deux')

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
        self.assertInParallel(mem_data, u'This is en__it example one', u'Questo è un esempio en__it uno')
        self.assertInParallel(mem_data, u'This is en__it example two', u'Questo è un esempio en__it due')
        self.assertInParallel(mem_data, u'This is en__it example three', u'Questo è un esempio en__it tre')
        self.assertInParallel(mem_data, u'This is en__it example four', u'Questo è un esempio en__it quattro')

        # en__pt
        memory = memories['en_pt']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'pt')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'pt', 'en')
        mem_data = tm_content.get_content(memory['id'], 'en', 'pt')

        self.assertEqual(0, len(ctx_source))
        self.assertEqual(4, len(ctx_target))
        self.assertInContent(ctx_target, u'Este é en__pt exemplo um')
        self.assertInContent(ctx_target, u'Este é en__pt exemplo dois')
        self.assertInContent(ctx_target, u'Este é en__pt exemplo três')
        self.assertInContent(ctx_target, u'Este é en__pt exemplo quatro')

        self.assertEqual(4, len(mem_data))
        self.assertInParallel(mem_data, u'This is en__pt example one', u'Este é en__pt exemplo um')
        self.assertInParallel(mem_data, u'This is en__pt example two', u'Este é en__pt exemplo dois')
        self.assertInParallel(mem_data, u'This is en__pt example three', u'Este é en__pt exemplo três')
        self.assertInParallel(mem_data, u'This is en__pt example four', u'Este é en__pt exemplo quatro')

        # en__pt-PT
        memory = memories['en_pt']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'pt-PT')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'pt-PT', 'en')
        mem_data = tm_content.get_content(memory['id'], 'en', 'pt-PT')

        self.assertEqual(2, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertInContent(ctx_source, u'This is en__pt example one')
        self.assertInContent(ctx_source, u'This is en__pt example three')

        self.assertEqual(2, len(mem_data))
        self.assertInParallel(mem_data, u'This is en__pt example one', u'Este é en__pt exemplo um')
        self.assertInParallel(mem_data, u'This is en__pt example three', u'Este é en__pt exemplo três')

        # en__pt-BR
        memory = memories['en_pt']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'pt-BR')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'pt-BR', 'en')
        mem_data = tm_content.get_content(memory['id'], 'en', 'pt-BR')

        self.assertEqual(3, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertInContent(ctx_source, u'This is en__pt example one')
        self.assertInContent(ctx_source, u'This is en__pt example two')
        self.assertInContent(ctx_source, u'This is en__pt example four')

        self.assertEqual(3, len(mem_data))
        self.assertInParallel(mem_data, u'This is en__pt example one', u'Este é en__pt exemplo um')
        self.assertInParallel(mem_data, u'This is en__pt example two', u'Este é en__pt exemplo dois')
        self.assertInParallel(mem_data, u'This is en__pt example four', u'Este é en__pt exemplo quatro')

        # en__zh
        memory = memories['en_zh']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'zh')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'zh', 'en')
        mem_data = tm_content.get_content(memory['id'], 'en', 'zh')

        self.assertEqual(0, len(ctx_source))
        self.assertEqual(0, len(ctx_target))
        self.assertEqual(0, len(mem_data))

        # en__zh-TW
        memory = memories['en_zh']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'zh-TW')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'zh-TW', 'en')
        mem_data = tm_content.get_content(memory['id'], 'en', 'zh-TW')

        self.assertEqual(2, len(ctx_source))
        self.assertEqual(2, len(ctx_target))

        self.assertInContent(ctx_source, u'The en__zh example one')
        self.assertInContent(ctx_source, u'This is en__zh example three')
        self.assertInContent(ctx_target, u'en__zh例子之一')
        self.assertInContent(ctx_target, u'這是en__zh例子三')

        self.assertEqual(2, len(mem_data))
        self.assertInParallel(mem_data, u'The en__zh example one', u'en__zh例子之一')
        self.assertInParallel(mem_data, u'This is en__zh example three', u'這是en__zh例子三')

        # en__zh-CN
        memory = memories['en_zh']
        ctx_source = self.mmt.context_analyzer.get_content(memory['id'], 'en', 'zh-CN')
        ctx_target = self.mmt.context_analyzer.get_content(memory['id'], 'zh-CN', 'en')
        mem_data = tm_content.get_content(memory['id'], 'en', 'zh-CN')

        self.assertEqual(2, len(ctx_source))
        self.assertEqual(2, len(ctx_target))

        self.assertInContent(ctx_source, u'The en__zh example one')
        self.assertInContent(ctx_source, u'This is en__zh example two')
        self.assertInContent(ctx_target, u'en__zh例子之一')
        self.assertInContent(ctx_target, u'这是en__zh例子二')

        self.assertEqual(2, len(mem_data))
        self.assertInParallel(mem_data, u'The en__zh example one', u'en__zh例子之一')
        self.assertInParallel(mem_data, u'This is en__zh example two', u'这是en__zh例子二')


        # class OnlineLearningSequenceTest(unittest.TestCase):
        #     mmt = ModernMT('OnlineLearningTest')
        #
        #     _engine_tar = os.path.join(RES_FOLDER, 'engine.tar.gz')
        #
        #     def _delete_engine(self):
        #         shutil.rmtree(os.path.join(self.mmt.engines_path, 'OnlineLearningTest'), ignore_errors=True)
        #
        #     def setUp(self):
        #         self._delete_engine()
        #
        #         tar = tarfile.open(self._engine_tar, 'r:gz')
        #         tar.extractall(self.mmt.engines_path)
        #         tar.close()
        #
        #         self.mmt.start()
        #         self.mmt.api.create_memory('TestMemory')
        #
        #     def tearDown(self):
        #         self.mmt.stop()
        #         self._delete_engine()
        #
        #     # Tests
        #
        #     def test_single_contribution(self):
        #         self.assertEqual(self.mmt.get_channels(), ModernMT.Channels(0, 0))
        #         self.mmt.api.append_to_memory('en', 'it', 1, 'hello Mike!', 'ciao Mike!')
        # assert_equals(mmt_api_count_domains(), 3)
        #
        # mmt_stop()
        # new_size = mmt_engine_size()
        # assert_size_increased(new_size, init_size)
        #
        # mmt_start()
        # assert_equals(mmt_stream_status(), StreamsStatus(0, 1))
        # sleep(2)
        # mmt_stop()
        #
        # assert_equals(mmt_engine_size(), new_size)

        # def test_upload_domain():
        #     copy_engine('_test_base', 'default')
        #     init_size = mmt_engine_size()
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
        #     assert_equals(mmt_api_translate('Hello world!'), 'Mondo Hello!')
        #     mmt_api_import(os.path.join(RES, 'NewDomain.tmx'))
        #     assert_equals(mmt_api_translate('Hello world!'), 'Ciao mondo!')
        #     assert_equals(mmt_api_count_domains(), 4)
        #
        #     mmt_stop()
        #     new_size = mmt_engine_size()
        #     assert_size_increased(new_size, init_size)
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(4, 0))
        #     sleep(5)
        #     mmt_stop()
        #
        #     assert_equals(mmt_engine_size(), new_size)
        #
        # def test_updating_from_scratch_all():
        #     copy_engine('_test_4C_2TM', 'default')
        #     copy_engine_model('context', '_test_base', 'default')
        #     copy_engine_model('ilm', '_test_base', 'default')
        #     copy_engine_model('sapt', '_test_base', 'default')
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
        #     sleep(5)
        #     assert_equals(mmt_api_count_domains(), 5)
        #     mmt_stop()
        #
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
        #     sleep(5)
        #     mmt_stop()
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #
        # def test_updating_from_scratch_context():
        #     copy_engine('_test_4C_2TM', 'default')
        #     copy_engine_model('context', '_test_base', 'default')
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
        #     sleep(5)
        #     assert_equals(mmt_api_count_domains(), 5)
        #     mmt_stop()
        #
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
        #     sleep(5)
        #     mmt_stop()
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #
        # def test_updating_from_scratch_ilm():
        #     copy_engine('_test_4C_2TM', 'default')
        #     copy_engine_model('ilm', '_test_base', 'default')
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
        #     sleep(5)
        #     assert_equals(mmt_api_count_domains(), 5)
        #     mmt_stop()
        #
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
        #     sleep(5)
        #     mmt_stop()
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #
        # def test_updating_from_scratch_sapt():
        #     copy_engine('_test_4C_2TM', 'default')
        #     copy_engine_model('sapt', '_test_base', 'default')
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
        #     sleep(5)
        #     assert_equals(mmt_api_count_domains(), 5)
        #     mmt_stop()
        #
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
        #     sleep(5)
        #     mmt_stop()
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #
        # def test_updating_from_partial_all():
        #     copy_engine('_test_4C_2TM', 'default')
        #     copy_engine_model('context', '_test_2C_1TM', 'default')
        #     copy_engine_model('ilm', '_test_2C_1TM', 'default')
        #     copy_engine_model('sapt', '_test_2C_1TM', 'default')
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(4, 2))
        #     sleep(5)
        #     assert_equals(mmt_api_count_domains(), 5)
        #     mmt_stop()
        #
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
        #     sleep(5)
        #     mmt_stop()
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #
        # def test_updating_from_partial_context():
        #     copy_engine('_test_4C_2TM', 'default')
        #     copy_engine_model('context', '_test_2C_1TM', 'default')
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(4, 2))
        #     sleep(5)
        #     assert_equals(mmt_api_count_domains(), 5)
        #     mmt_stop()
        #
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
        #     sleep(5)
        #     mmt_stop()
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #
        # def test_updating_from_partial_ilm():
        #     copy_engine('_test_4C_2TM', 'default')
        #     copy_engine_model('ilm', '_test_2C_1TM', 'default')
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(4, 2))
        #     sleep(5)
        #     assert_equals(mmt_api_count_domains(), 5)
        #     mmt_stop()
        #
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
        #     sleep(5)
        #     mmt_stop()
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #
        # def test_updating_from_partial_sapt():
        #     copy_engine('_test_4C_2TM', 'default')
        #     copy_engine_model('sapt', '_test_2C_1TM', 'default')
        #
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(4, 2))
        #     sleep(5)
        #     assert_equals(mmt_api_count_domains(), 5)
        #     mmt_stop()
        #
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
        #     mmt_start()
        #     assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
        #     sleep(5)
        #     mmt_stop()
        #     assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
