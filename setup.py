#!/usr/bin/env python
import os
import shutil
import tarfile
import tempfile

import requests
import sys

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
        os.rename(folder, output)
    finally:
        shutil.rmtree(wdir, ignore_errors=True)

    print 'DONE'


if __name__ == '__main__':
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # reopen stdout in unbuffered mode

    # install_apache_lib('/cassandra/3.7/apache-cassandra-3.7-bin.tar.gz', 'vendor/cassandra-3.7', name='cassandra-3.7')
    install_apache_lib('/kafka/0.10.0.1/kafka_2.11-0.10.0.1.tgz', 'vendor/kafka-0.10.0.1', name='kafka-0.10.0.1')
