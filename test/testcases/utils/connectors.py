import os
import re
import shutil
import subprocess
import time
from collections import defaultdict

from cli import mmt
from cli.mmt.engine import Engine, EngineNode
from cli.mmt.mmtcli import mmt_java
from cli.utils import osutils, network
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

    def dump_context_analyzer(self):
        return _ContextAnalyzerContent(os.path.join(self.engine.models_path, 'context'))

    def dump_translation_memory(self):
        return _MemoryContent(os.path.join(self.engine.models_path, 'decoder', 'memory'))

    def get_channels(self):
        node = self.api.info()['cluster']['nodes'].pop()
        return {int(k): v for k, v in node['channels'].items()}


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


class _ContextAnalyzerContent(object):
    def __init__(self, model_path) -> None:
        std_out, _ = osutils.shell_exec(['java', '-cp', mmt.MMT_JAR, 'eu.modernmt.context.lucene.storage.utils.Dump',
                                         os.path.join(model_path, 'storage')])

        self._content_by_memory = defaultdict(set)

        for line in std_out.splitlines(keepends=False):
            memory, src_lang, tgt_lang, line = line.strip().split('\t', maxsplit=3)
            self._content_by_memory[int(memory)].add('%s\t%s\t%s' % (src_lang, tgt_lang, line))

    def __len__(self):
        return len(self._content_by_memory)

    def __contains__(self, memory):
        return memory in self._content_by_memory

    def __getitem__(self, item):
        class __Content(object):
            def __init__(self, entries):
                self._entries = entries

            def __len__(self):
                return len(self._entries)

            def __contains__(self, _item):
                src_lang, tgt_lang, line = _item
                _key = '%s\t%s\t%s' % (self._norm_lang(src_lang), self._norm_lang(tgt_lang), line)
                return _key in self._entries

            @staticmethod
            def _norm_lang(lang):
                if '-' in lang:
                    lang = lang[:lang.index('-')]
                return lang.lower()

        if item in self._content_by_memory:
            return __Content(self._content_by_memory[item])
        else:
            raise KeyError(item)


class _MemoryContent(object):
    class Entry(object):
        def __init__(self, src_lang, tgt_lang, src_line, tgt_line):
            self._src_lang = src_lang
            self._tgt_lang = tgt_lang
            self._src_line = src_line
            self._tgt_line = tgt_line

            if src_lang < tgt_lang:
                self._id = '%s %s %s %s' % (src_lang, tgt_lang, src_line.replace(' ', ''), tgt_line.replace(' ', ''))
            else:  # reversed
                self._id = '%s %s %s %s' % (tgt_lang, src_lang, tgt_line.replace(' ', ''), src_line.replace(' ', ''))

        @property
        def src_lang(self):
            return self._src_lang

        @property
        def tgt_lang(self):
            return self._tgt_lang

        @property
        def src_line(self):
            return self._src_line

        @property
        def tgt_line(self):
            return self._tgt_line

        def __eq__(self, o: object) -> bool:
            return (type(o) == type(self)) and (self._id == o._id)

        def __str__(self) -> str:
            return '%s\t%s\t%s\t%s' % (self._src_lang, self._tgt_lang, self._src_line, self._tgt_line)

        def __repr__(self) -> str:
            return str(self)

        def __hash__(self) -> int:
            return hash(self._id)

    def __init__(self, model_path, main_class=None) -> None:
        if main_class is None:
            main_class = 'eu.modernmt.decoder.neural.memory.lucene.utils.Dump'
        cmd = ['java', '-cp', mmt.MMT_JAR, main_class, model_path]
        std_out, _ = osutils.shell_exec(cmd)

        self._content_by_memory = defaultdict(set)

        for line in std_out.splitlines(keepends=False):
            memory, src_lang, tgt_lang, src_line, tgt_line = line.strip().split('\t')
            self._content_by_memory[int(memory)].add(self.Entry(src_lang, tgt_lang, src_line, tgt_line))

    def __len__(self):
        return len(self._content_by_memory)

    def __contains__(self, memory):
        return memory in self._content_by_memory

    def __getitem__(self, item):
        class __Memory(object):
            def __init__(self, entries):
                self._entries = entries

            def __len__(self):
                return len(self._entries)

            def __iter__(self):
                for entry in self._entries:
                    yield entry

            def __contains__(self, _item):
                src_lang, tgt_lang, src_line, tgt_line = _item
                return _MemoryContent.Entry(src_lang, tgt_lang, src_line, tgt_line) in self._entries

        if item in self._content_by_memory:
            return __Memory(self._content_by_memory[item])
        else:
            raise KeyError(item)


class BackupDaemonConnector(object):
    def __init__(self, engine_name):
        self.engine = Engine(engine_name)
        self._process = None

    def start(self):
        command = mmt_java('eu.modernmt.cli.BackupDaemonMain', args=['-e', self.engine.name, '-i', '3600', '-l', '1'])
        env = dict(os.environ, MMT_Q_HOST=network.get_ip())
        self._process = osutils.shell_exec(command, background=True, env=env)

    def stop(self):
        if self._process is not None:
            self._process.terminate()
            self._process.wait()
            self._process = None

    def delete(self):
        shutil.rmtree(self.engine.path, ignore_errors=True)
        shutil.rmtree(self.engine.runtime_path, ignore_errors=True)

    def dump_translation_memory(self):
        return _MemoryContent(os.path.join(self.engine.models_path, 'backup'),
                              main_class='eu.modernmt.backup.model.utils.Dump')
