import errno
import os
import shutil
import socket
import subprocess
from contextlib import contextmanager

import select

__author__ = 'Davide Caroselli'


def makedirs(name, mode=0777, exist_ok=False):
    try:
        os.makedirs(name, mode)
    except OSError as exception:
        if not exist_ok or exception.errno != errno.EEXIST:
            raise


def linecount(f):
    with open(f) as stream:
        count = 0
        for _, line in enumerate(stream):
            count += 1

    return count


def wordcount(f):
    wc = 0

    with open(f, 'r+') as stream:
        for _ in stream.read().split():
            wc += 1

    return wc


def df(f=None):
    if f is None:
        f = '.'

    output = subprocess.Popen(['df', f], stdout=subprocess.PIPE).communicate()[0]
    _, size, used, available, _, _ = output.split('\n')[1].split()

    return int(size) * 1024, int(used) * 1024, int(available) * 1024


def du(f=None):
    if f is None:
        f = '.'

    total_size = 0

    for dirpath, dirnames, filenames in os.walk(f):
        for f in filenames:
            fp = os.path.join(dirpath, f)
            total_size += os.path.getsize(fp)

    return int(total_size)


def meminfo():
    """
    Returns the memory usage from /proc/meminfo as a dict str -> int
    Numbers are in **bytes** (converted from /proc/meminfo which has kB)
    """
    with open('/proc/meminfo') as mi:
        info_lines = [l.split()[0:2] for l in mi.readlines()]
        info = {key.rstrip(':'): int(val) * 1024 for key, val in info_lines}
    return info


def free():
    """real available RAM in bytes (excluding disk cache)"""
    mi = meminfo()
    return mi['MemFree'] + mi['Buffers'] + mi['Cached']


def merge(srcs, dest, buffer_size=10 * 1024 * 1024, delimiter=None):
    with open(dest, 'wb') as blob:
        for src in srcs:
            with open(src, 'rb') as source:
                shutil.copyfileobj(source, blob, buffer_size)
            if delimiter is not None:
                blob.write(delimiter)


@contextmanager
def chdir(path):
    """Context Manager for chdir() that returns to the previous working directory afterwards"""
    wd = os.getcwd()
    os.chdir(path)
    yield
    os.chdir(wd)


def netcat(host, port, content, timeout=60, max_size=4096):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((host, int(port)))
    s.sendall(content.encode())
    s.shutdown(socket.SHUT_WR)

    result = None

    ready = select.select([s], [], [], timeout)
    if ready[0]:
        s.setblocking(False)
        data = s.recv(max_size)
        if data:
            result = str(data)

    s.close()

    return result
