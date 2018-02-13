import os
import shutil
import subprocess

import sys

import time

MMT_HOME = os.path.abspath(os.path.join(__file__, os.pardir, os.pardir, os.pardir))
sys.path.insert(0, MMT_HOME)

from cli.mmt.cluster import ClusterNode
from cli.libs.shell import ShellError

os.environ['LD_LIBRARY_PATH'] = os.path.join(MMT_HOME, 'build', 'lib')
os.environ['LC_ALL'] = 'en_US.UTF-8'
os.environ['LANG'] = 'en_US.UTF-8'


class ContextAnalyzer:
    def __init__(self, path):
        self._storage_path = os.path.join(path, 'storage')

    def get_domains(self):
        domains = set()

        for name in os.listdir(self._storage_path):
            if name != 'index':
                domains.add(int(name.split('_')[0]))

        return domains

    def get_content(self, memory_id, source, target):
        source = source.replace('-', '_')
        target = target.replace('-', '_')
        file_path = os.path.join(self._storage_path, '%d_%s__%s' % (memory_id, source, target))

        result = []
        if os.path.isfile(file_path):
            with open(file_path, 'rb') as stream:
                for line in stream:
                    result.append(line.strip().decode('utf-8'))

        return result


class Memory:
    def __init__(self, path):
        self._path = path


class ModernMT:
    def __init__(self, engine='default'):
        self.engines_path = os.path.join(MMT_HOME, 'engines')

        self._engine = engine
        self._models_path = os.path.join(self.engines_path, self._engine, 'models')

        self.api = ClusterNode.Api(port=8045)
        self.context_analyzer = ContextAnalyzer(os.path.join(self._models_path, 'context'))
        self.memory = Memory(os.path.join(self._models_path, 'decoder', 'memory'))

    @staticmethod
    def _exe(cmd, stdin=None, stdout=subprocess.PIPE, stderr=subprocess.PIPE):
        process = subprocess.Popen(cmd, stdin=stdin, stdout=stdout, stderr=stderr, shell=True, cwd=MMT_HOME)

        return_code = process.wait()
        if return_code != 0:
            raise ShellError(cmd, return_code, process.stdout.read() + '\n' + process.stderr.read())

    def start(self):
        self._exe('./mmt stop -e ' + self._engine)

        runtime = os.path.join(MMT_HOME, 'runtime', self._engine)
        shutil.rmtree(runtime, ignore_errors=True)

        self._exe('./mmt start -e ' + self._engine)

    def stop(self):
        self._exe('./mmt stop -e ' + self._engine)

    def restart(self):
        self.stop()
        self.start()

    def import_corpus(self, tmx=None, compact=None):
        content_path = tmx if tmx is not None else compact
        name, _ = os.path.splitext(content_path)
        _, name = os.path.split(name)

        memory = self.api.create_memory(name)
        memory_id = memory['id']

        job = self.api.import_into_memory(memory_id, tmx=tmx, compact=compact)

        return job, memory

    def wait_job(self, job, callback=None, refresh_rate_in_seconds=1):
        if callback is not None:
            callback(job)

        while job['progress'] != 1.0:
            time.sleep(refresh_rate_in_seconds)
            job = self.api.get_import_job(job['id'])

            if callback is not None:
                callback(job)



