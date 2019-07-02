import os
import tarfile
import time

from cli.mmt.fileformats import CompactFileFormat
from testcases import ModernMTTestCase, TEST_RESOURCES
from testcases.utils.connectors import BackupDaemonConnector


class __BackupTest(ModernMTTestCase):
    mmt_engine_archive = os.path.join(TEST_RESOURCES, 'multilingual_echo_engine.tar.gz')
    backup_daemon_archive = os.path.join(TEST_RESOURCES, 'backup_daemon.tar.gz')

    CORPUS_DE = CompactFileFormat('en', 'de', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__de.cfc'))
    CORPUS_ES = CompactFileFormat('en', 'es', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__es.cfc'))
    CORPUS_FR = CompactFileFormat('en', 'fr', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__fr.cfc'))
    CORPUS_IT = CompactFileFormat('en', 'it', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__it.cfc'))
    CORPUS_ZH = CompactFileFormat('en', 'zh', os.path.join(TEST_RESOURCES, 'onlinelearning', 'Memory.en__zh.cfc'))

    ALL_CORPORA = [CORPUS_DE, CORPUS_ES, CORPUS_FR, CORPUS_IT, CORPUS_ZH]
    FINAL_CORPORA = [CORPUS_DE, CORPUS_ES, CORPUS_IT, CORPUS_ZH]

    def setUp(self):
        super().setUp()

        self.backup_daemon = BackupDaemonConnector(self.__class__.__name__ + '_backup')

        if self.backup_daemon_archive is not None:
            self.backup_daemon.delete()

            os.makedirs(self.backup_daemon.engine.path)

            tar = tarfile.open(self.backup_daemon_archive, 'r:gz')
            tar.extractall(self.backup_daemon.engine.path)
            tar.close()

            self.backup_daemon.start()

    def tearDown(self):
        super().tearDown()
        self.backup_daemon.stop()
        self.backup_daemon.delete()

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

    # Utils

    @staticmethod
    def _update_of(tgt_line):
        if int(tgt_line[-1]) % 2 == 0:
            return tgt_line + ' - UPDATE'
        else:
            return None


class BackupTest(__BackupTest):
    def _send_updates(self, memories):
        for corpus in self.ALL_CORPORA:
            memory = memories[corpus.name]
            memory_id = int(memory['id'])

            job = None
            with corpus.reader_with_languages() as reader:
                for src_lang, tgt_lang, src_line, tgt_line in reader:
                    updated_tgt_line = self._update_of(tgt_line)
                    if updated_tgt_line is not None:
                        job = self.mmt.api.replace_in_memory(src_lang, tgt_lang, memory_id,
                                                             src_line, updated_tgt_line, src_line, tgt_line)

            if job is not None:
                self.mmt.wait_import_job(job)

        fr_memory = memories['Memory.en__fr']
        self.mmt.api.delete_memory(fr_memory['id'])

    # Tests

    def test_backup_import_with_all_language_combinations(self):
        memories = {}

        for corpus in self.ALL_CORPORA:
            memory = self.mmt.api.create_memory(corpus.name)
            job = self.mmt.api.import_into_memory(memory['id'], compact=corpus.file_path)
            self.mmt.wait_import_job(job)

            memories[corpus.name] = memory
        self._send_updates(memories)

        self._verify_index_integrity(memories)

    def test_backup_add_with_all_language_combinations(self):
        memories = {}

        for corpus in self.ALL_CORPORA:
            memory = self.mmt.api.create_memory(corpus.name)

            job = None
            with corpus.reader_with_languages() as reader:
                for src_lang, tgt_lang, src_line, tgt_line in reader:
                    job = self.mmt.api.append_to_memory(src_lang, tgt_lang, memory['id'], src_line, tgt_line)

            if job is not None:
                self.mmt.wait_import_job(job)

            memories[corpus.name] = memory
        self._send_updates(memories)

        self._verify_index_integrity(memories)

    def _verify_index_integrity(self, memories):
        time.sleep(10)  # wait to ensure backup engine has completed

        # Dump engine content
        self.mmt.stop()
        self.backup_daemon.stop()

        backup_memory = self.backup_daemon.dump_translation_memory()

        # Verify Memory content
        self.assertEqual(4, len(backup_memory))

        for corpus in self.FINAL_CORPORA:
            memory = memories[corpus.name]
            memory_id = int(memory['id'])

            self.assertIn(memory_id, backup_memory)
            content = backup_memory[memory_id]

            with corpus.reader_with_languages() as reader:
                for src_lang, tgt_lang, src_line, tgt_line in reader:
                    updated_tgt_line = self._update_of(tgt_line)
                    self.assertIn((src_lang, tgt_lang, src_line, updated_tgt_line or tgt_line), content)
