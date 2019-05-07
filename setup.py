import os
import shutil
import sys
import tarfile
import tempfile

from cli import mmt
from cli.utils import osutils
from cli.utils.progressbar import Progressbar, UndefinedProgressbar

CASSANDRA_VERSION = '3.11.4'
CASSANDRA_FILE_SIZE = 41235177
KAFKA_VERSION = '1.1.0'
KAFKA_SCALA_VERSION = '2.11'
KAFKA_FILE_SIZE = 56969154
KAFKA_DOWNLOAD_URL = 'https://archive.apache.org/dist/kafka/1.1.0/kafka_2.11-1.1.0.tgz'


def chown(folder, uid, gid):
    os.chown(folder, uid, gid)

    for root, dirs, files in os.walk(folder):
        for d in dirs:
            os.chown(os.path.join(root, d), uid, gid)
        for f in files:
            os.chown(os.path.join(root, f), uid, gid)


def get_owner(folder):
    stat_info = os.stat(folder)
    return stat_info.st_uid, stat_info.st_gid


class ApacheDownloader(object):
    def __init__(self):
        try:
            import requests.packages.urllib3 as urllib3
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

            import logging
            logging.getLogger("requests.packages.urllib3").setLevel(logging.CRITICAL)
        except ImportError:
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

            folder = self._untar(tar_file, work_directory)
            progressbar.set_progress(0.9)

            shutil.rmtree(destination_folder, ignore_errors=True)
            shutil.move(folder, destination_folder)

            chown(destination_folder, *get_owner(mmt.MMT_VENDOR_DIR))

            progressbar.complete()
        except Exception as e:
            progressbar.abort(str(e))
            raise
        finally:
            shutil.rmtree(work_directory, ignore_errors=True)

    @staticmethod
    def _untar(filename, destination):
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

    @staticmethod
    def _get_mirrors(apache_path):
        import requests

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
        import requests

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


# - Main functions -----------------------------------------------------------------------------------------------------

def install_cassandra():
    cassandra_home = os.path.join(mmt.MMT_VENDOR_DIR, 'cassandra-' + CASSANDRA_VERSION)
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


def install_kafka():
    kafka_home = os.path.join(mmt.MMT_VENDOR_DIR, 'kafka-' + KAFKA_VERSION)

    downloader = ApacheDownloader()
    downloader.download_from_mirrors('Kafka', [KAFKA_DOWNLOAD_URL], kafka_home, expected_file_size=KAFKA_FILE_SIZE)


def copy_opennlp_resources():
    opennlp_home = os.path.join(mmt.MMT_VENDOR_DIR, 'opennlp')
    assert os.path.exists(opennlp_home)

    opennlp_res = os.path.join(mmt.MMT_RES_DIR, 'opennlp')
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

    chown(mmt.MMT_RES_DIR, *get_owner(mmt.MMT_BUILD_DIR))


def pip_install():
    requirements_txt = os.path.join(mmt.MMT_HOME_DIR, 'requirements.txt')
    osutils.shell_exec(['pip3', 'install', '-r', requirements_txt], stderr=sys.stderr, stdout=sys.stdout)


def main(argv=None):
    import argparse

    parser = argparse.ArgumentParser(description='Download all dependencies needed by ModernMT system.')
    parser.add_argument('--skip-pip', dest='skip_pip', action='store_true', default=False,
                        help='skip pip dependencies')
    parser.add_argument('--skip-kafka', dest='skip_kafka', action='store_true', default=False,
                        help='skip Apache Kafka download')
    parser.add_argument('--skip-cassandra', dest='skip_cassandra', action='store_true', default=False,
                        help='skip Apache Cassandra download')

    args = parser.parse_args(argv)

    if not args.skip_pip:
        pip_install()

    if not args.skip_cassandra:
        install_cassandra()

    if not args.skip_kafka:
        install_kafka()

    copy_opennlp_resources()


if __name__ == '__main__':
    main()
