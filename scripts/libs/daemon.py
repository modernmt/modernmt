import os
import resource

__author__ = 'Davide Caroselli'


def daemonize():
    try:
        pid = os.fork()
    except OSError as e:
        raise e

    if pid == 0:
        os.setsid()

        try:
            pid = os.fork()
        except OSError as e:
            raise e

        if pid == 0:
            os.chdir('/')
            os.umask(0)
        else:
            os._exit(0)
    else:
        return False

    maxfd = resource.getrlimit(resource.RLIMIT_NOFILE)[1]
    if maxfd == resource.RLIM_INFINITY:
        maxfd = 1024

    for fd in range(0, maxfd):
        try:
            os.close(fd)
        except OSError:
            pass

    os.open(os.devnull, os.O_RDWR)
    os.dup2(0, 1)
    os.dup2(0, 2)

    return True
