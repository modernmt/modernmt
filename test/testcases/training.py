import os

from testcases import ModernMTTestCase, TEST_RESOURCES

TRAIN_FOLDER = os.path.join(TEST_RESOURCES, 'training', 'data')


class TrainingTest(ModernMTTestCase):
    def test_train_example(self):
        self.mmt.create()
        self.mmt.start()

        result = self.mmt.api.translate('en', 'it', 'hello world')

        self.assertIn('translation', result)
        self.assertGreater(len(result['translation']), 0)
