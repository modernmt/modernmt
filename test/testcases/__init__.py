import os
import tarfile
import unittest

from testcases.utils.connectors import ModernMTConnector

__this_dir = os.path.dirname(os.path.realpath(__file__))
TEST_RESOURCES = os.path.abspath(os.path.join(__this_dir, 'res'))


class ModernMTTestCase(unittest.TestCase):
    mmt_engine_archive = None

    def setUp(self):
        self.mmt = ModernMTConnector(self.__class__.__name__)

        if self.mmt_engine_archive is not None:
            self.mmt.delete()

            os.makedirs(self.mmt.engine.path)

            tar = tarfile.open(self.mmt_engine_archive, 'r:gz')
            tar.extractall(self.mmt.engine.path)
            tar.close()

            self.mmt.start()

    def tearDown(self):
        self.mmt.stop()
        pass
        # self.mmt.delete()

    def assertInContent(self, content, element):
        element = ''.join(element.split())
        content = [''.join(line.split()) for line in content]

        self.assertIn(element, content)

    def assertInParallelContent(self, content, sentence, translation):
        sentence = ''.join(sentence.split())
        translation = ''.join(translation.split())
        content = [(''.join(s.split()), ''.join(t.split())) for s, t in content]

        self.assertIn((sentence, translation), content)
