import logging
import os
import subprocess

__author__ = 'Davide Caroselli'

DEVNULL = open(os.devnull, 'wb')
logger = logging.getLogger()


class ShellError(Exception):
    def __init__(self, command, errno, message=None):
        self.command = command
        self.errno = errno
        self.message = message

    def __str__(self):
        string = "Command '%s' failed with exit code %d" % (self.command, self.errno)
        if self.message is not None:
            string += ': ' + repr(self.message)
        return string

    def __repr__(self):
        return self.__str__()


def execute(cmd, stdin=None, stdout=subprocess.PIPE, stderr=subprocess.PIPE, background=False, env=None):
    str_cmd = cmd if isinstance(cmd, basestring) else ' '.join(cmd)
    logger.debug('Shell exec: %s', str_cmd)

    message = None
    if background:
        if stdout == subprocess.PIPE:
            stdout = DEVNULL
        if stderr == subprocess.PIPE:
            stderr = DEVNULL
    elif stdin is not None and isinstance(stdin, basestring):
        message = stdin
        stdin = subprocess.PIPE

    process = subprocess.Popen(cmd, stdin=stdin, stdout=stdout, stderr=stderr,
                               shell=(True if isinstance(cmd, basestring) else False), env=env)

    stdout_dump = None
    stderr_dump = None
    returncode = 0

    if message is not None or stdout == subprocess.PIPE or stderr == subprocess.PIPE:
        stdout_dump, stderr_dump = process.communicate(message)
        returncode = process.returncode
    elif not background:
        returncode = process.wait()

    if returncode != 0:
        raise ShellError(str_cmd, returncode, stderr_dump)
    else:
        return stdout_dump, stderr_dump


def execute_pipe(cmds, stdin=None, stdout=subprocess.PIPE, stderr=subprocess.PIPE, background=False):
    str_cmd = ' | '.join([cmd if isinstance(cmd, basestring) else ' '.join(cmd) for cmd in cmds])
    logger.debug('Shell exec: %s', str_cmd)

    if background:
        if stdout == subprocess.PIPE:
            stdout = DEVNULL
        if stderr == subprocess.PIPE:
            stderr = DEVNULL

    processes = []
    for i in range(0, len(cmds)):
        cmd = cmds[i]

        if i == 0:
            process = subprocess.Popen(cmd, stdin=stdin, stdout=subprocess.PIPE, stderr=stderr)
        elif 0 < i < len(cmds) - 1:
            process = subprocess.Popen(cmd, stdin=processes[i - 1].stdout, stdout=subprocess.PIPE, stderr=stderr)
        else:
            process = subprocess.Popen(cmd, stdin=processes[i - 1].stdout, stdout=stdout, stderr=stderr)

        processes.append(process)

    for i in range(0, len(cmds) - 1):
        processes[i].stdout.close()

    stdout_dump = None
    stderr_dump = None
    returncode = 0

    if stdout == subprocess.PIPE or stderr == subprocess.PIPE or not background:
        last_process = processes[-1]
        stdout_dump, stderr_dump = last_process.communicate()

        for i in range(0, len(cmds) - 1):
            _code = processes[i].wait()
            returncode = returncode if returncode > 0 else _code

        returncode = returncode if returncode > 0 else last_process.returncode

    if returncode != 0:
        raise ShellError(str_cmd, returncode, stderr_dump)
    else:
        return stdout_dump, stderr_dump
