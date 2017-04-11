import errno
import os
import resource
import time

import signal


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


class TimeoutExpired(Exception):
    pass


def is_running(pid):
    if pid < 0:
        return False
    try:
        os.kill(pid, 0)
    except OSError as err:
        if err.errno == errno.ESRCH:
            # ESRCH == No such process
            return False
        elif err.errno == errno.EPERM:
            # EPERM clearly means there's a process to deny access to
            return True
        else:
            # According to "man 2 kill" possible error values are
            # (EINVAL, EPERM, ESRCH)
            raise
    else:
        return True


def kill(pid, timeout=2, ignore_errors=False):
    if pid is None or pid == 0:
        return

    try:
        os.kill(pid, signal.SIGTERM)
        wait(pid, timeout)
    except TimeoutExpired:
        os.kill(pid, signal.SIGKILL)
        wait(pid)
    except OSError:
        if not ignore_errors:
            raise



def wait(pid, timeout=None):
    def check_timeout(_delay):
        if timeout is not None:
            if time.time() >= stop_at:
                raise TimeoutExpired
        time.sleep(_delay)
        return min(_delay * 2, 0.04)

    if timeout is not None:
        waitcall = lambda: os.waitpid(pid, os.WNOHANG)
        stop_at = time.time() + timeout
    else:
        waitcall = lambda: os.waitpid(pid, 0)

    delay = 0.0001
    while 1:
        try:
            retpid, status = waitcall()
        except OSError, err:
            if err.errno == errno.EINTR:
                delay = check_timeout(delay)
                continue
            elif err.errno == errno.ECHILD:
                # This has two meanings:
                # - pid is not a child of os.getpid() in which case
                #   we keep polling until it's gone
                # - pid never existed in the first place
                # In both cases we'll eventually return None as we
                # can't determine its exit status code.
                while 1:
                    if is_running(pid):
                        delay = check_timeout(delay)
                    else:
                        return
            else:
                raise
        else:
            if retpid == 0:
                # WNOHANG was used, pid is still running
                delay = check_timeout(delay)
                continue
            # process exited due to a signal; return the integer of
            # that signal
            if os.WIFSIGNALED(status):
                return os.WTERMSIG(status)
            # process exited using exit(2) system call; return the
            # integer exit(2) system call has been called with
            elif os.WIFEXITED(status):
                return os.WEXITSTATUS(status)
            else:
                # should never happen
                raise RuntimeError("unknown process exit status")
