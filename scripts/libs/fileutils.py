import os
import errno
import shutil

__author__ = 'Davide Caroselli'


def makedirs(name, mode=0777, exist_ok=False):
    try:
        os.makedirs(name, mode)
    except OSError as exception:
        if not exist_ok or exception.errno != errno.EEXIST:
            raise


def merge(srcs, dest, buffer_size=10 * 1024 * 1024, delimiter=None):
    with open(dest, 'wb') as blob:
        for src in srcs:
            with open(src, 'rb') as source:
                shutil.copyfileobj(source, blob, buffer_size)
            if delimiter is not None:
                blob.write(delimiter)
