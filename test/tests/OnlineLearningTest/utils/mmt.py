import os
import shutil
import subprocess

import sys

import time

MMT_HOME = os.path.abspath(os.path.join(__file__, os.pardir, os.pardir, os.pardir, os.pardir, os.pardir))

sys.path.insert(0, MMT_HOME)
os.environ['LD_LIBRARY_PATH'] = os.path.join(MMT_HOME, 'build', 'lib')
os.environ['LC_ALL'] = 'en_US.UTF-8'
os.environ['LANG'] = 'en_US.UTF-8'

from cli.mmt.cluster import MMTApi
from cli.libs.shell import ShellError

_api = MMTApi(port=8045)


def _log(message, nl=True):
    sys.stderr.write(message)
    if nl:
        sys.stderr.write('\n')
    sys.stderr.flush()


def _exe(cmd, stdin=None, stdout=subprocess.PIPE, stderr=subprocess.PIPE):
    process = subprocess.Popen(cmd, stdin=stdin, stdout=stdout, stderr=stderr, shell=True, cwd=MMT_HOME)

    returncode = process.wait()
    if returncode != 0:
        raise ShellError(cmd, returncode, process.stdout.read() + '\n' + process.stderr.read())


def mmt_create_example():
    _log('Creating example engine...', nl=False)
    _exe('./mmt create en it examples/data/train/')
    _log('DONE')


def mmt_start(engine='default'):
    _log('Starting mmt (%s)...' % engine, nl=False)
    _exe('./mmt stop -e ' + engine)

    runtime = os.path.join(MMT_HOME, 'runtime', engine)
    shutil.rmtree(runtime)

    _exe('./mmt start -e ' + engine)
    _log('DONE')


def mmt_stop(engine='default'):
    _log('Stopping mmt (%s)...' % engine, nl=False)
    _exe('./mmt stop -e ' + engine)
    _log('DONE')


def mmt_restart(engine='default'):
    mmt_stop(engine)
    mmt_start(engine)


def mmt_api_translate(line):
    return _api.translate(line)['translation']


def mmt_api_append(domain, source, target):
    _api.append_to_domain(domain, source, target)
    time.sleep(5)


def mmt_api_import(tmx, name=None):
    if name is None:
        name = os.path.basename(os.path.splitext(tmx)[0])
    domain = _api.create_domain(name)

    job = _api.import_into_domain(domain['id'], tmx)

    while job['progress'] != 1.0:
        time.sleep(1)
        job = _api.get_import_job(job['id'])

    time.sleep(5)


def mmt_api_count_domains():
    return len(_api.get_all_domains())


class EngineSize:
    def __init__(self, ngrams, context_model_size, sapt_model_size, sapt_prefixes, sapt_target_words):
        self._ngrams = ngrams
        self._context_model_size = context_model_size
        self._sapt_model_size = sapt_model_size
        self._sapt_prefixes = sapt_prefixes
        self._sapt_target_words = sapt_target_words

    def __eq__(self, other):
        if isinstance(other, self.__class__):
            return self.__dict__ == other.__dict__
        else:
            return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def gt(self, o):
        return self._ngrams >= o._ngrams and \
               self._context_model_size >= o._context_model_size and \
               self._sapt_model_size >= o._sapt_model_size and \
               self._sapt_prefixes >= o._sapt_prefixes and \
               self._sapt_target_words >= o._sapt_target_words

    def __str__(self):
        return '{ilm=%d, context=%d, sapt=tg:%d pr:%d sz:%d}' % (
            self._ngrams, self._context_model_size, self._sapt_target_words, self._sapt_prefixes, self._sapt_model_size)

    def __repr__(self):
        return self.__str__()


def _get_folder_size(start_path='.', ext=None):
    total_size = 0
    for dirpath, dirnames, filenames in os.walk(start_path):
        for f in filenames:
            if ext is not None and not f.endswith('.' + ext):
                continue
            fp = os.path.join(dirpath, f)
            total_size += os.path.getsize(fp)
    return total_size


def _get_ngrams(engine):
    cmd = ['build/bin/dump_alm', '-m', 'engines/%s/models/decoder/lm/foreground.alm/' % engine]
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=MMT_HOME)

    ngrams = 0

    while True:
        line = process.stdout.readline()
        if line != '':
            if line.startswith('domain'):
                ngrams += int(line.split()[5])
        else:
            break

    returncode = process.wait()
    if returncode != 0:
        raise ShellError(' '.join(cmd), returncode, process.stderr.read())

    return ngrams


def _get_sapt_data(engine):
    cmd = ['build/bin/sapt_dump_index', '-m', 'engines/%s/models/decoder/sapt/' % engine]
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=MMT_HOME)

    prefixes = 0
    target_words = 0

    while True:
        line = process.stdout.readline()
        if line != '':
            if line.startswith('SOURCE'):
                prefixes += int(line.split()[6])
            elif line.startswith('TARGET'):
                target_words += int(line.split()[4])
        else:
            break

    returncode = process.wait()
    if returncode != 0:
        raise ShellError(' '.join(cmd), returncode, process.stderr.read())

    return prefixes, target_words


def mmt_engine_size(engine='default'):
    _log('Getting engine size (%s)...' % engine, nl=False)
    models = os.path.join(MMT_HOME, 'engines', engine, 'models')
    context_size = _get_folder_size(os.path.join(models, 'context', 'storage'))
    sapt_size = _get_folder_size(os.path.join(models, 'decoder', 'sapt', 'storage'))
    sapt_prefixes, sapt_target_words = _get_sapt_data(engine)
    ngrams = _get_ngrams(engine)
    _log('DONE')

    return EngineSize(ngrams, context_size, sapt_size, sapt_prefixes, sapt_target_words)


class StreamsStatus:
    def __init__(self, domain_upload, contributions):
        self._domain_upload = domain_upload
        self._contributions = contributions

    def __eq__(self, other):
        if isinstance(other, self.__class__):
            return self.__dict__ == other.__dict__
        else:
            return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __str__(self):
        return 'DM=%d, C=%d' % (self._domain_upload, self._contributions)


def mmt_stream_status(engine='default'):
    log_file = os.path.join(MMT_HOME, 'runtime', engine, 'logs', 'node.log')

    domain_upload = None
    contributions = None

    with open(log_file) as stream:
        for line in stream:
            if 'eu.modernmt.cluster.kafka.KafkaDataManager' in line and 'Channel' in line:
                tokens = line.split('Channel')[-1].strip().split()
                value = int(tokens[-1])

                if 'domain-upload-stream' in tokens[0]:
                    domain_upload = value
                elif 'contributions-stream' in tokens[0]:
                    contributions = value

    return StreamsStatus(domain_upload, contributions)
