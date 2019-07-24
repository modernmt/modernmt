import os
import time

from testcases import ModernMTTestCase, TEST_RESOURCES

TRAIN_FOLDER = os.path.join(TEST_RESOURCES, 'training', 'data')


class HealthCheckTest(ModernMTTestCase):
    mmt_engine_archive = os.path.join(TEST_RESOURCES, 'stub_en_it_engine.tar.gz')

    def test_train_example(self):
        self.mmt.api.health_check()

        result = self.mmt.api.translate('en', 'it', 'hello world')
        self.assertIn('translation', result)
        self.assertGreater(len(result['translation']), 0)

        self.mmt.api.health_check()
        time.sleep(6)
        self.mmt.api.health_check()


class TrainingTest(ModernMTTestCase):
    def test_train_example(self):
        self.mmt.create()
        self.mmt.start()

        self.mmt.api.health_check()

        result = self.mmt.api.translate('en', 'it', 'hello world')
        self.assertIn('translation', result)
        self.assertGreater(len(result['translation']), 0)

        self.mmt.api.health_check()
        time.sleep(6)
        self.mmt.api.health_check()
