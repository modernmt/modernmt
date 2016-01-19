import os
import stat
import subprocess

__author__ = 'Davide Caroselli'


class SSHException(Exception):
    def __init__(self, host, pem, error):
        self.message = error
        self.pem = pem
        self.host = host


def verify(host, pem=None):
    # Verify PEM file
    if pem is not None:
        permissions = os.stat(pem).st_mode

        if (permissions & (stat.S_IRWXG | stat.S_IRWXO)) > 0:
            raise SSHException(host, pem, 'Permissions %s for \'%s\' are too open. '
                                          'Your private key files MUST NOT be accessible by others.'
                               % (oct(permissions & 0777), pem))

    # Verify host
    command = ['ssh-keygen', '-F', host]

    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    _, _ = process.communicate()

    if process.returncode != 0:
        raise SSHException(host, pem, 'The authenticity of host \'%s\' can\'t be established. '
                                      'Please try to connect to the host with \'ssh\' command before retrying.' % host)
