import os
import shutil
import subprocess

from cli import mmt
from cli.mmt.engine import Engine, EngineNode
from cli.utils import osutils
from cli.utils.osutils import ShellError


class ModernMTConnector(object):
    def __init__(self, engine_name):
        self.engine = Engine(engine_name)
        self.api = EngineNode.RestApi(port=8045)
        self._mmt_script = os.path.join(mmt.MMT_HOME_DIR, 'mmt')

    def cli(self, *args, stdin=None, stdout=subprocess.PIPE, stderr=subprocess.PIPE):
        return osutils.shell_exec([self._mmt_script] + list(args), stdin=stdin, stdout=stdout, stderr=stderr)

    def delete(self):
        self.stop()
        shutil.rmtree(self.engine.path, ignore_errors=True)
        shutil.rmtree(self.engine.runtime_path, ignore_errors=True)

    def start(self):
        self.cli('start', '-e', self.engine.name)

    def stop(self):
        try:
            self.cli('stop', '-e', self.engine.name)
        except ShellError:
            pass

    def restart(self):
        self.start()
        self.stop()

    def create(self, src_lang='en', tgt_lang='it', path=None, arch='transformer_mmt_tiny', epochs=10):
        if path is None:
            path = os.path.join(mmt.MMT_HOME_DIR, 'examples', 'data', 'train')

        self.cli('create', src_lang, tgt_lang, path, '-e', self.engine.name, '-y',
                 '-a', arch, '--debug', '--no-test', '--max-epoch', str(epochs))
