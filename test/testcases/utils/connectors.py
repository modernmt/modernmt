import os
import re
import shutil
import subprocess
import time

from cli import mmt
from cli.mmt.engine import Engine, EngineNode
from cli.utils import osutils
from cli.utils.osutils import ShellError
from testcases.utils import Namespace


class ModernMTConnector(object):
    def __init__(self, engine_name):
        self.engine = Engine(engine_name)
        self.api = EngineNode.RestApi(port=8045)
        self._mmt_script = os.path.join(mmt.MMT_HOME_DIR, 'mmt')

    def log_listener(self):
        return _LogListener(os.path.join(self.engine.logs_path, 'node.log'))

    def cli(self, *args, stdin=None, stdout=subprocess.PIPE, stderr=subprocess.PIPE):
        return osutils.shell_exec([self._mmt_script] + list(args), stdin=stdin, stdout=stdout, stderr=stderr)

    def delete(self):
        self.stop()
        shutil.rmtree(self.engine.path, ignore_errors=True)
        shutil.rmtree(self.engine.runtime_path, ignore_errors=True)

    def start(self):
        self.cli('start', '-e', self.engine.name, '-v', '2')

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

    def wait_import_job(self, import_job, refresh_rate_in_seconds=1):
        while import_job['progress'] != 1.0:
            time.sleep(refresh_rate_in_seconds)
            import_job = self.api.get_import_job(import_job['id'])


class _LogListener(object):
    def __init__(self, log_file):
        self.log_file = log_file
        self._log_start_re = re.compile(r'^([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}) ([A-Z]+) ')

    def get_log_start(self):
        with open(self.log_file, 'r', encoding='utf-8') as f:
            f.seek(0, 2)
            return f.tell()

    def log_lines_iter(self, start=None):
        with open(self.log_file, 'r', encoding='utf-8') as f:
            if start is not None:
                f.seek(start)

            log_line = None

            for line in f:
                start_match = self._log_start_re.match(line)

                if start_match:
                    if log_line is not None:
                        yield log_line

                    log_line = line
                else:
                    log_line += line

    def listen_for_translation(self):
        this = self

        class __Listener:
            def __init__(self):
                self.translation = None
                self._start = None

            def __enter__(self):
                self._start = this.get_log_start()
                return self

            def __exit__(self, exc_type, exc_val, exc_tb):
                for line in this.log_lines_iter(start=self._start):
                    if 'Translation received from neural decoder:' not in line:
                        continue

                    lines = line.strip().splitlines(keepends=False)

                    sentence = lines[1][lines[1].index(' = ') + 3:]
                    translation = lines[2][lines[2].index(' = ') + 3:]
                    suggestions = [self._parse_suggestion(x) for x in lines[4:-1]]

                    self.translation = Namespace(sentence=sentence, translation=translation, suggestions=suggestions)

                    break

            @staticmethod
            def _parse_suggestion(line):
                def extract(start, end):
                    start = line.index(start) + len(start)
                    end = line.index(end, start)
                    return line[start:end]

                return Namespace(
                    memory=int(extract('ScoreEntry{memory=', ', language=')),
                    language=extract(', language=', ', sentence='),
                    sentence=extract(', sentence=[', '], translation=[').replace(', ', ' '),
                    translation=extract('], translation=[', '], score=').replace(', ', ' '),
                    score=float(extract('], score=', '}'))
                )

        return __Listener()
