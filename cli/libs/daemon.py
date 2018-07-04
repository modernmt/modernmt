import errno
import os
import subprocess
import time

import signal


class _TimeoutExpired(Exception):
    pass


class DaemonController(object):
    def __init__(self, pid_file, sigterm_timeout=1):
        self._pid_file = pid_file
        self._sigterm_timeout = sigterm_timeout

    @property
    def running(self):
        return self.__is_running(self.pid)

    def _start(self, command):
        dev_null = open(os.devnull, 'w')

        process = subprocess.Popen(command, stdout=dev_null, stderr=dev_null, shell=False)
        pid = process.pid

        if pid > 0:
            self.__set_pid(pid)

            time.sleep(1)
            return process.poll() is None

        return False

    def _stop(self, children=None, timeout_children=2):
        pid = self.pid

        if self.__is_running(pid):
            self.__kill(pid, timeout=self._sigterm_timeout)

            if children is not None:
                for child_pid in children:
                    self.__kill(child_pid, timeout=timeout_children)

        if os.path.isfile(self._pid_file):
            os.remove(self._pid_file)

    @property
    def pid(self):
        pid = 0
        if os.path.isfile(self._pid_file):
            with open(self._pid_file) as pid_file:
                pid = int(pid_file.read())

        return pid

    # Private utilities

    def __set_pid(self, pid):
        parent_dir = os.path.abspath(os.path.join(self._pid_file, os.pardir))
        if not os.path.isdir(parent_dir):
            try:
                os.makedirs(parent_dir)
            except OSError as exception:
                if exception.errno != errno.EEXIST:
                    raise

        with open(self._pid_file, 'w') as pid_file:
            pid_file.write(str(pid))

    @staticmethod
    def __is_running(pid):
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

    def __kill(self, pid, timeout=2, ignore_errors=False):
        if pid is None or pid == 0:
            return

        try:
            os.kill(pid, signal.SIGTERM)
            self.__wait(pid, timeout)
        except _TimeoutExpired:
            os.kill(pid, signal.SIGKILL)
            self.__wait(pid)
        except OSError:
            if not ignore_errors:
                raise

    def __wait(self, pid, timeout=None):
        def check_timeout(_delay):
            if timeout is not None:
                if time.time() >= stop_at:
                    raise _TimeoutExpired
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
                        if self.__is_running(pid):
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
                    raise RuntimeError('unknown process exit status')
