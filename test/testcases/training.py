# coding=utf-8
import os
import unittest

from cli.libs.shell import ShellError
from commons import ModernMT, ApiException

RES_FOLDER = os.path.abspath(os.path.join(__file__, os.pardir, 'res', 'training'))
DEV_FOLDER = os.path.join(RES_FOLDER, 'dev')
TRAIN_FOLDER = os.path.join(RES_FOLDER, 'train')


class TrainingTest(unittest.TestCase):
    mmt = ModernMT('TrainingTest')

    def tearDown(self):
        self.mmt.delete_engine()

    @staticmethod
    def _get_tmx_content(path):
        with open(path, 'rb') as stream:
            return ' '.join(stream.read().decode('utf-8').split())

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

    def assertTranslateMatch(self, source, target, sentence, chars):
        translation = self.mmt.translate(source, target, sentence)

        for c in chars:
            if c in translation:
                return

        raise self.failureException(u'Translation "%s" does not contain any of %s' % (translation, repr(chars)))

    def assertTranslateFail(self, source, target, sentence):
        try:
            self.mmt.translate(source, target, sentence)
            raise self.failureException('Invalid translation request: %s %s' % (source, target))
        except ApiException as e:
            self.assertIn('HTTP request failed with code 400', e.message)

    # Tests

    def test_chinese_conversion(self):
        try:
            self.mmt.create('en zh %s --debug -s clean_tms' % TRAIN_FOLDER)
        except ShellError:
            pass

        training_folder = os.path.join(self.mmt.engine_runtime_path, 'tmp', 'training')

        clean_tmx = self._get_tmx_content(os.path.join(training_folder, 'clean_corpora', 'Memory.en__zh.tmx'))
        original_tmx = self._get_tmx_content(os.path.join(TRAIN_FOLDER, 'Memory.en__zh.tmx'))

        self.assertNotEqual(clean_tmx, original_tmx)

        clean_tmx = clean_tmx.replace(u'<tuv xml:lang="zh-TW"> <seg>這是en__zh例子四</seg> </tuv>',
                                      u'<tuv xml:lang="zh"> <seg>這是en__zh例子四</seg> </tuv>')

        self.assertEqual(clean_tmx, original_tmx)

    def test_train_chinese(self):
        self.mmt.create('en zh %s --neural --debug --no-split --validation-corpora %s' % (TRAIN_FOLDER, DEV_FOLDER))

        tm_content = self.mmt.memory.dump()

        self.assertEqual({1}, self.mmt.context_analyzer.get_domains())
        self.assertEqual({1}, tm_content.get_domains())

        ctx_source = self.mmt.context_analyzer.get_content(1, 'en', 'zh')
        ctx_target = self.mmt.context_analyzer.get_content(1, 'zh', 'en')
        mem_data = tm_content.get_content(1, 'en', 'zh')

        self.assertEqual(4, len(ctx_source))
        self.assertEqual(0, len(ctx_target))

        self.assertInContent(ctx_source, u'The en__zh example one')
        self.assertInContent(ctx_source, u'This is en__zh example two')
        self.assertInContent(ctx_source, u'This is en__zh example three')
        self.assertInContent(ctx_source, u'This is en__zh example four')

        self.assertEqual(4, len(mem_data))
        self.assertInParallelContent(mem_data, u'The en__zh example one', u'en__zh例子之一')
        self.assertInParallelContent(mem_data, u'This is en__zh example two', u'这是en__zh例子二')
        self.assertInParallelContent(mem_data, u'This is en__zh example three', u'這是en__zh例子三')
        self.assertInParallelContent(mem_data, u'This is en__zh example four', u'這是en__zh例子四')

        self.mmt.start()

        self.assertTranslateMatch('en', 'zh', u'This is example', {u'这', u'這', u'是', u'例', u'子'})
        self.assertTranslateFail('en', 'zh-CN', u'This is example')
        self.assertTranslateFail('en', 'zh-TW', u'This is example')

        self.mmt.add_contributions('en', 'zh', [(u'The en__zh example five', u'en__zh例子五')], 1)

        ctx_source = self.mmt.context_analyzer.get_content(1, 'en', 'zh')
        mem_data = self.mmt.memory.dump().get_content(1, 'en', 'zh')

        self.assertInContent(ctx_source, u'The en__zh example five')
        self.assertInParallelContent(mem_data, u'The en__zh example five', u'en__zh例子五')

    def test_train_simplified_chinese(self):
        self.mmt.create('en zh-CN %s --neural --debug --no-split --validation-corpora %s' % (TRAIN_FOLDER, DEV_FOLDER))

        tm_content = self.mmt.memory.dump()

        self.assertEqual({1}, self.mmt.context_analyzer.get_domains())
        self.assertEqual({1}, tm_content.get_domains())

        ctx_source = self.mmt.context_analyzer.get_content(1, 'en', 'zh-CN')
        ctx_target = self.mmt.context_analyzer.get_content(1, 'zh-CN', 'en')
        mem_data = tm_content.get_content(1, 'en', 'zh-CN')

        self.assertEqual(2, len(ctx_source))
        self.assertEqual(0, len(ctx_target))

        self.assertInContent(ctx_source, u'The en__zh example one')
        self.assertInContent(ctx_source, u'This is en__zh example two')

        self.assertEqual(2, len(mem_data))
        self.assertInParallelContent(mem_data, u'The en__zh example one', u'en__zh例子之一')
        self.assertInParallelContent(mem_data, u'This is en__zh example two', u'这是en__zh例子二')

        self.mmt.start()

        self.assertTranslateMatch('en', 'zh-CN', u'This is example', {u'这', u'是', u'例', u'子'})
        self.assertTranslateFail('en', 'zh', u'This is example')
        self.assertTranslateFail('en', 'zh-TW', u'This is example')

        self.mmt.add_contributions('en', 'zh', [(u'The en__zh example five', u'en__zh例子五')], 1)

        ctx_source = self.mmt.context_analyzer.get_content(1, 'en', 'zh-CN')
        mem_data = self.mmt.memory.dump().get_content(1, 'en', 'zh-CN')

        self.assertInContent(ctx_source, u'The en__zh example five')
        self.assertInParallelContent(mem_data, u'The en__zh example five', u'en__zh例子五')

    def test_train_traditional_chinese(self):
        self.mmt.create('en zh-TW %s --neural --debug --no-split --validation-corpora %s' % (TRAIN_FOLDER, DEV_FOLDER))

        tm_content = self.mmt.memory.dump()

        self.assertEqual({1}, self.mmt.context_analyzer.get_domains())
        self.assertEqual({1}, tm_content.get_domains())

        ctx_source = self.mmt.context_analyzer.get_content(1, 'en', 'zh-TW')
        ctx_target = self.mmt.context_analyzer.get_content(1, 'zh-TW', 'en')
        mem_data = tm_content.get_content(1, 'en', 'zh-TW')

        self.assertEqual(3, len(ctx_source))
        self.assertEqual(0, len(ctx_target))

        self.assertInContent(ctx_source, u'The en__zh example one')
        self.assertInContent(ctx_source, u'This is en__zh example three')
        self.assertInContent(ctx_source, u'This is en__zh example four')

        self.assertEqual(3, len(mem_data))
        self.assertInParallelContent(mem_data, u'The en__zh example one', u'en__zh例子之一')
        self.assertInParallelContent(mem_data, u'This is en__zh example three', u'這是en__zh例子三')
        self.assertInParallelContent(mem_data, u'This is en__zh example four', u'這是en__zh例子四')

        self.mmt.start()

        self.assertTranslateMatch('en', 'zh-TW', u'This is example', {u'這', u'是', u'例', u'子'})
        self.assertTranslateFail('en', 'zh-CN', u'This is example')
        self.assertTranslateFail('en', 'zh', u'This is example')

        self.mmt.add_contributions('en', 'zh', [(u'The en__zh example five', u'en__zh例子五')], 1)

        ctx_source = self.mmt.context_analyzer.get_content(1, 'en', 'zh-TW')
        mem_data = self.mmt.memory.dump().get_content(1, 'en', 'zh-TW')

        self.assertInContent(ctx_source, u'The en__zh example five')
        self.assertInParallelContent(mem_data, u'The en__zh example five', u'en__zh例子五')