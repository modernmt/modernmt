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

PLUGINS_DIR = os.path.join(BUILD_DIR, 'plugins')
LIB_DIR = os.path.join(BUILD_DIR, 'lib')
BIN_DIR = os.path.join(BUILD_DIR, 'bin')

MMT_JAR = os.path.join(BUILD_DIR, 'mmt-' + MMT_VERSION + '.jar')

# Environment setup
os.environ['LD_LIBRARY_PATH'] = os.pathsep.join(
    [LIB_DIR] + [x for x in os.environ['LD_LIBRARY_PATH'].split(os.pathsep)
                 if len(x) > 0]) if 'LD_LIBRARY_PATH' in os.environ else LIB_DIR
os.environ['LC_ALL'] = 'en_US.UTF-8'
os.environ['LANG'] = 'en_US.UTF-8'


def mmt_javamain(main_class, args=None, hserr_path=None, remote_debug=False, max_heap_mb=None):
    classpath = [MMT_JAR]

    if os.path.isdir(PLUGINS_DIR):
        for filename in os.listdir(PLUGINS_DIR):
            if filename.endswith('.jar'):
                classpath.append(os.path.join(PLUGINS_DIR, filename))

    classpath = ':'.join(classpath)

    command = ['java', '-cp', classpath, '-Dmmt.home=' + MMT_ROOT, '-Djava.library.path=' + LIB_DIR, main_class]

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
