import os
import shutil
import subprocess

import sys
import tempfile

import time
from collections import defaultdict
from distutils.dir_util import copy_tree

MMT_HOME = os.path.abspath(os.path.join(__file__, os.pardir, os.pardir, os.pardir))
sys.path.insert(0, MMT_HOME)

from cli.mmt.cluster import ClusterNode, ApiException
from cli.libs.shell import ShellError
from cli import MMT_JAR

os.environ['LD_LIBRARY_PATH'] = os.path.join(MMT_HOME, 'build', 'lib')
os.environ['LC_ALL'] = 'en_US.UTF-8'
os.environ['LANG'] = 'en_US.UTF-8'


def _exe(cmd, stdin=None, stdout=subprocess.PIPE, stderr=subprocess.PIPE):
    process = subprocess.Popen(cmd, stdin=stdin, stdout=stdout, stderr=stderr, shell=True, cwd=MMT_HOME)

    return_code = process.wait()
    if return_code != 0:
        raise ShellError(cmd, return_code, process.stdout.read() + '\n' + process.stderr.read())

    return process


class ContextAnalyzer(object):
    def __init__(self, path):
        self.path = path
        self._storage_path = os.path.join(path, 'storage')

    def get_domains(self):
        domains = set()

        for name in os.listdir(self._storage_path):
            if name != 'index':
                domains.add(int(name.split('_')[0]))

        return domains

    def get_content(self, memory_id, source, target):
        file_path = os.path.join(self._storage_path, '%d_%s_%s' % (memory_id, source, target))

        result = []
        if os.path.isfile(file_path):
            with open(file_path, 'rb') as stream:
                for line in stream:
                    result.append(line.strip().decode('utf-8'))

        return result


class Memory(object):
    class Content(object):
        def __init__(self, dump):
            self._data = defaultdict(lambda: defaultdict(list))

            for line in dump.splitlines():
                memory, source, target, sentence, translation = line.strip().split('\t')
                self._data[int(memory)]['%s__%s' % (source, target)].append((sentence, translation))

        def get_domains(self):
            return set([int(e) for e in self._data.keys()])

        def get_content(self, memory_id, source, target):
            return self._data[memory_id]['%s__%s' % (source, target)]

    def __init__(self, path):
        self.path = path

    def dump(self):
        tmp_folder = tempfile.mkdtemp()

        try:
            copy_tree(self.path, tmp_folder)
            os.unlink(os.path.join(tmp_folder, 'write.lock'))

            process = _exe('java -cp %s eu.modernmt.decoder.neural.memory.lucene.utils.Dump %s' % (MMT_JAR, tmp_folder))
            return self.Content(process.stdout.read().decode('utf-8'))
        finally:
            shutil.rmtree(tmp_folder)


class ModernMT(object):
    class Channels(object):
        @staticmethod
        def from_dict(d):
            return ModernMT.Channels(d['0'], d['1'])

        def __init__(self, memory_upload, contributions):
            self.memory_upload = memory_upload
            self.contributions = contributions

        def __str__(self):
            return str(self.__dict__)

        def __repr__(self):
            return repr(self.__dict__)

        def __eq__(self, other):
            if isinstance(other, ModernMT.Channels):
                return self.__dict__ == other.__dict__

            return NotImplemented

    def __init__(self, engine='default'):
        self.engine_path = os.path.join(MMT_HOME, 'engines', engine)
        self.engine_runtime_path = os.path.join(MMT_HOME, 'runtime', engine)
        self.engine_name = engine

        self._models_path = os.path.join(self.engine_path, 'models')

        self.api = ClusterNode.Api(port=8045)
        self.context_analyzer = ContextAnalyzer(os.path.join(self._models_path, 'context'))
        self.memory = Memory(os.path.join(self._models_path, 'decoder', 'memory'))

    def backup(self, runtime=False, engine=False):
        if runtime:
            shutil.rmtree(self.engine_runtime_path + '_BAK', ignore_errors=True)
            shutil.copytree(self.engine_runtime_path, self.engine_runtime_path + '_BAK')
        if engine:
            shutil.rmtree(self.engine_path + '_BAK', ignore_errors=True)
            shutil.copytree(self.engine_path, self.engine_path + '_BAK')

    def start(self):
        _exe('./mmt stop -e ' + self.engine_name)

        runtime = os.path.join(MMT_HOME, 'runtime', self.engine_name)
        shutil.rmtree(runtime, ignore_errors=True)

        _exe('./mmt start -e ' + self.engine_name)

    def stop(self):
        _exe('./mmt stop -e ' + self.engine_name)

    def create(self, args):
        _exe('./mmt create -e %s %s' % (self.engine_name, args))

    def delete_engine(self):
        shutil.rmtree(self.engine_path, ignore_errors=True)
        shutil.rmtree(self.engine_runtime_path, ignore_errors=True)

    def restart(self):
        self.stop()
        self.start()

    def translate(self, source, target, sentence):
        return self.api.translate(source, target, sentence)['translation']

    def add_contributions(self, source, target, entries, memory=None, wait=True):
        if memory is None:
            memory = self.api.create_memory('TestMemory')['id']

        job = None
        for sentence, translation in entries:
            job = self.api.append_to_memory(source, target, memory, sentence, translation)

        if wait:
            self.wait_job(job)

    def import_corpus(self, tmx=None, compact=None, wait=True):
        content_path = tmx if tmx is not None else compact
        name, _ = os.path.splitext(content_path)
        _, name = os.path.split(name)

        memory = self.api.create_memory(name)
        memory_id = memory['id']

        job = self.api.import_into_memory(memory_id, tmx=tmx, compact=compact)

        if wait:
            self.wait_job(job)

        return memory

    def wait_job(self, job, callback=None, refresh_rate_in_seconds=1):
        if callback is not None:
            callback(job)

        while job['progress'] != 1.0:
            time.sleep(refresh_rate_in_seconds)
            job = self.api.get_import_job(job['id'])

            if callback is not None:
                callback(job)

    def get_channels(self):
        info = self.api.info()
        return self.Channels.from_dict(info['cluster']['nodes'][0]['channels'])
