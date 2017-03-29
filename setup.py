#!/usr/bin/env python
import os
import shutil
import tarfile
import tempfile

import requests
import sys

try:
    from requests.packages.urllib3.exceptions import InsecureRequestWarning

    # Suppress InsecureRequestWarning
    requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
except ImportError:
    pass

__author__ = 'Davide Caroselli'


def apache_download(path, outfile):
    r = requests.get('https://www.apache.org/dyn/closer.cgi', params={
        'path': path,
        'as_json': '1',
    }, timeout=60, verify=False)

    if r.status_code != requests.codes.ok:
        raise Exception('HTTP request failed with code ' + str(r.status_code) + ': ' + r.url)

    content = r.json()

    path_info = content['path_info']
    preferred = content['preferred']

    mirrors = content['http']
    if preferred in mirrors:
        del mirrors[mirrors.index(preferred)]
    mirrors.insert(0, preferred)

    for mirror in mirrors:
        r = requests.get(mirror + '/' + path_info, stream=True)

        if r.status_code == 200:
            with open(outfile, 'wb') as f:
                for chunk in r:
                    f.write(chunk)

            return

    raise Exception('Failed to download ' + path)


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


def install_apache_lib(path, output, name):
    print 'Installing ' + name + '...',
    wdir = tempfile.mkdtemp()

    try:
        tardest = os.path.join(wdir, os.path.basename(path))
        apache_download(path, tardest)

        folder = untar(tardest, wdir)

        shutil.rmtree(output, ignore_errors=True)
        shutil.move(folder, output)
    finally:
        shutil.rmtree(wdir, ignore_errors=True)

    print 'DONE'


def configure_cassandra(cassandra_home):
    print 'Configuring cassandra...',

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

    print 'DONE'

if __name__ == '__main__':
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # reopen stdout in unbuffered mode

    install_apache_lib('/cassandra/3.10/apache-cassandra-3.10-bin.tar.gz', 'vendor/cassandra-3.10', name='cassandra-3.10')
    configure_cassandra('vendor/cassandra-3.10')
    install_apache_lib('/kafka/0.10.0.1/kafka_2.11-0.10.0.1.tgz', 'vendor/kafka-0.10.0.1', name='kafka-0.10.0.1')
