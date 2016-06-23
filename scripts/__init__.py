import os

__author__ = 'Davide Caroselli'

__self_dir = os.path.dirname(os.path.realpath(__file__))

MMT_VERSION = '0.13'

MMT_ROOT = os.path.abspath(os.path.join(__self_dir, os.pardir))
ENGINES_DIR = os.path.join(MMT_ROOT, 'engines')
RUNTIME_DIR = os.path.join(MMT_ROOT, 'runtime')
BUILD_DIR = os.path.join(MMT_ROOT, 'build')

OPT_DIR = os.path.join(MMT_ROOT, 'opt')
LIB_DIR = os.path.join(BUILD_DIR, 'lib')
BIN_DIR = os.path.join(OPT_DIR, 'bin')

MMT_JAR = os.path.join(BUILD_DIR, 'mmt-' + MMT_VERSION + ".jar")


def mmt_javamain(main_class, args=None, hserr_path=None, remote_debug=False):
    command = ['java', '-cp', MMT_JAR, '-Dmmt.home=' + MMT_ROOT, '-Djava.library.path=' + LIB_DIR, main_class]

    if remote_debug:
        command.insert(1, '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005')

    if hserr_path is not None:
        command.insert(1, '-XX:ErrorFile=' + os.path.join(hserr_path, 'hs_err_pid%p.log'))

    if args is not None:
        command += args

    return command


class IllegalStateException(Exception):
    def __init__(self, error):
        self.message = error


class IllegalArgumentException(Exception):
    def __init__(self, error):
        self.message = error
