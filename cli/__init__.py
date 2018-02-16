import os

__author__ = 'Davide Caroselli'

__self_dir = os.path.dirname(os.path.realpath(__file__))

MMT_VERSION = '2.4'

PYOPT_DIR = os.path.join(__self_dir, 'opt')
MMT_ROOT = os.path.abspath(os.path.join(__self_dir, os.pardir))
ENGINES_DIR = os.path.join(MMT_ROOT, 'engines')
RUNTIME_DIR = os.path.join(MMT_ROOT, 'runtime')
BUILD_DIR = os.path.join(MMT_ROOT, 'build')
VENDOR_DIR = os.path.join(MMT_ROOT, 'vendor')

LIB_DIR = os.path.join(BUILD_DIR, 'lib')
BIN_DIR = os.path.join(BUILD_DIR, 'bin')

MMT_JAR = os.path.join(BUILD_DIR, 'mmt-' + MMT_VERSION + '.jar')

# Environment setup
os.environ['LD_LIBRARY_PATH'] = LIB_DIR
os.environ['LC_ALL'] = 'en_US.UTF-8'
os.environ['LANG'] = 'en_US.UTF-8'


def mmt_javamain(main_class, args=None, hserr_path=None, remote_debug=False, max_heap_mb=None):
    command = ['java', '-cp', MMT_JAR, '-Dmmt.home=' + MMT_ROOT, '-Djava.library.path=' + LIB_DIR, main_class]

    if remote_debug:
        command.insert(1, '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005')

    if hserr_path is not None:
        command.insert(1, '-XX:ErrorFile=' + os.path.join(hserr_path, 'hs_err_pid%p.log'))

    if args is not None:
        command += args

    if max_heap_mb is not None:
        command.insert(1, '-Xmx' + str(max_heap_mb) + 'm')

    return command


class IllegalStateException(Exception):
    def __init__(self, error):
        super(Exception, self).__init__(error)
        self.message = error


class IllegalArgumentException(Exception):
    def __init__(self, error):
        super(Exception, self).__init__(error)
        self.message = error


class CorpusNotFoundInFolderException(IllegalArgumentException):
    def __init__(self, error):
        super(Exception, self).__init__(error)
        self.message = error
