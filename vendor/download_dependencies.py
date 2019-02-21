import argparse
import os
import shutil
import sys
import tarfile
import tempfile

import requests

try:
    sys.path.insert(0, os.path.abspath(os.path.join(__file__, os.pardir, os.pardir)))
    from cli.libs.progressbar import Progressbar, UndefinedProgressbar
except ImportError:
    raise

try:
    from requests.packages.urllib3.exceptions import InsecureRequestWarning

    # Suppress InsecureRequestWarning
    requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
except ImportError:
    pass

CASSANDRA_VERSION = '3.11.3'
CASSANDRA_FILE_SIZE = 37317433
KAFKA_VERSION = '1.1.0'
KAFKA_SCALA_VERSION = '2.11'
KAFKA_FILE_SIZE = 56969154
KAFKA_DOWNLOAD_URL = 'https://archive.apache.org/dist/kafka/1.1.0/kafka_2.11-1.1.0.tgz'

vendor_dir = os.path.dirname(os.path.realpath(__file__))
mmt_home = os.path.join(vendor_dir, os.path.pardir)
mmt_build_dir = os.path.join(mmt_home, 'build')
mmt_install_res = os.path.join(mmt_build_dir, 'res')


def untar(filename, destination):
    if filename.endswith('.tar.gz') or filename.endswith('.tgz'):
        tar = tarfile.open(filename, 'r:gz')
    elif filename.endswith('.tar'):
        tar = tarfile.open(filename, 'r:')
    else:
        raise Exception('Unknown file type (supported .tar.gz or .tar): ' + filename)

    folder = tar.getnames()[0]
    if '/' in folder:
        folder = folder[:folder.index('/')]

    tar.extractall(destination)
    tar.close()

    return os.path.join(destination, folder)


class ApacheDownloader(object):
    def __init__(self):
        pass

    def download(self, name, apache_path, destination_folder, expected_file_size=None):
        return self.download_from_mirrors(name, self._get_mirrors(apache_path), destination_folder, expected_file_size)

    def download_from_mirrors(self, name, mirrors, destination_folder, expected_file_size=None):
        work_directory = tempfile.mkdtemp()

        progressbar = Progressbar('Downloading ' + name) \
            if expected_file_size is not None else UndefinedProgressbar('Downloading ' + name)
        progressbar.start()

        try:
            def _callback(length):
                progress = min(1., length / float(expected_file_size))
                progressbar.set_progress(progress * 0.8)

            tar_file = self._download_from_mirrors(mirrors, work_directory,
                                                   callback=None if expected_file_size is None else _callback)

            folder = untar(tar_file, work_directory)
            progressbar.set_progress(0.9)

            shutil.rmtree(destination_folder, ignore_errors=True)
            shutil.move(folder, destination_folder)

            progressbar.complete()
        except Exception as e:
            progressbar.abort(e.message)
            raise
        finally:
            shutil.rmtree(work_directory, ignore_errors=True)

    @staticmethod
    def _get_mirrors(apache_path):
        current_attempt = 0
        attempts_limit = 3
        while True:
            try:
                r = requests.get('https://www.apache.org/dyn/closer.cgi', params={
                    'path': apache_path,
                    'as_json': '1',
                }, timeout=30, verify=False)

                if r.status_code == requests.codes.ok:
                    break
            except requests.exceptions.Timeout:
                current_attempt += 1

                if current_attempt >= 3:
                    raise Exception('HTTP request failed: https://www.apache.org/dyn/closer.cgi\n' +
                                    'Timeout limit exceeded for ' + str(attempts_limit) + 'times')

        if r.status_code != requests.codes.ok:
            raise Exception('HTTP request failed with code ' + str(r.status_code) + ': ' + r.url)

        content = r.json()
        path_info = content['path_info']
        preferred = content['preferred']
        mirrors = content['http']

        # handle preferred mirrors (?)
        if preferred in mirrors:
            del mirrors[mirrors.index(preferred)]
        mirrors.insert(0, preferred)

        return ['%s/%s' % (mirror, path_info) for mirror in mirrors]

    @staticmethod
    def _download_from_mirrors(mirrors, output_folder, callback=None):
        for mirror in mirrors:
            try:
                r = requests.get(mirror, timeout=10, stream=True)
            except requests.exceptions.Timeout:
                continue

            if r.status_code == 200:
                length = 0

                output_file = os.path.join(output_folder, os.path.basename(mirror))

                with open(output_file, 'wb') as f:
                    for chunk in r:
                        length += len(chunk)
                        f.write(chunk)

                        if callback is not None:
                            callback(length)
                return output_file

        raise Exception('Failed to download from Apache repository')


# =========================================================================================
# Main functions
# =========================================================================================

def download_cassandra():
    cassandra_home = os.path.join(vendor_dir, 'cassandra-' + CASSANDRA_VERSION)
    cassandra_apache_path = '/cassandra/' + CASSANDRA_VERSION + '/apache-cassandra-' + CASSANDRA_VERSION + '-bin.tar.gz'

    downloader = ApacheDownloader()
    downloader.download('Cassandra', cassandra_apache_path, cassandra_home, expected_file_size=CASSANDRA_FILE_SIZE)

    # Update cassandra-env.sh to make it accept a new JMX_PORT as an environment variable;
    # this is necessary in order to let separate Cassandra instances run on the same machine
    conf_path = os.path.join(cassandra_home, 'conf')
    cassandra_env = os.path.join(conf_path, 'cassandra-env.sh')
    cassandra_env_bak = os.path.join(conf_path, 'cassandra-env.sh.bak')
    shutil.copyfile(cassandra_env, cassandra_env_bak)
    with open(cassandra_env) as f:
        content = f.read()
    content = content.replace('JMX_PORT="7199"', 'JMX_PORT="7199"\n'
                                                 'if [ "x$CASSANDRA_JMX_PORT" != "x" ]; then\n'
                                                 '    JMX_PORT=$CASSANDRA_JMX_PORT;'
                                                 '\nfi')
    with open(cassandra_env, 'w') as f:
        f.write(content)


def download_kafka():
    kafka_home = os.path.join(vendor_dir, 'kafka-' + KAFKA_VERSION)

    downloader = ApacheDownloader()
    downloader.download_from_mirrors('Kafka', [KAFKA_DOWNLOAD_URL], kafka_home, expected_file_size=KAFKA_FILE_SIZE)


def copy_opennlp():
    opennlp_home = os.path.join(vendor_dir, 'opennlp')
    assert os.path.exists(opennlp_home)

    opennlp_res = os.path.join(mmt_install_res, 'opennlp')
    if not os.path.exists(opennlp_res):
        os.makedirs(opennlp_res)

    files = [f for f in os.listdir(opennlp_home) if f.endswith('.bin')]

    progressbar = Progressbar('Downloading OpenNLP')
    progressbar.start()

    count = 0
    try:
        for filename in files:
            shutil.copyfile(os.path.join(opennlp_home, filename), os.path.join(opennlp_res, filename))
            count += 1
            progressbar.set_progress(count / float(len(files)))
    finally:
        progressbar.complete()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Download all dependencies needed by ModernMT system.')
    parser.add_argument('--skip-kafka', dest='skip_kafka', action='store_true', default=False,
                        help='skip Apache Kafka download')
    parser.add_argument('--skip-cassandra', dest='skip_cassandra', action='store_true', default=False,
                        help='skip Apache Cassandra download')

    args = parser.parse_args()

    if not args.skip_cassandra:
        download_cassandra()
    if not args.skip_kafka:
        download_kafka()
    copy_opennlp()
