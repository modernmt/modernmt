import glob
import os

__author__ = 'Davide Caroselli'

__self_dir = os.path.dirname(os.path.realpath(__file__))

PYOPT_DIR = os.path.join(__self_dir, 'opt')
MMT_ROOT = os.path.abspath(os.path.join(__self_dir, os.pardir))
ENGINES_DIR = os.path.join(MMT_ROOT, 'engines')
RUNTIME_DIR = os.path.join(MMT_ROOT, 'runtime')
BUILD_DIR = os.path.join(MMT_ROOT, 'build')
VENDOR_DIR = os.path.join(MMT_ROOT, 'vendor')

PLUGINS_DIR = os.path.join(BUILD_DIR, 'plugins')
LIB_DIR = os.path.join(BUILD_DIR, 'lib')
BIN_DIR = os.path.join(BUILD_DIR, 'bin')


def mmt_jar(pattern):
    jars = [f for f in glob.glob(pattern)]
    jars.sort(key=lambda e: os.path.getmtime(e))
    return jars[-1]


MMT_JAR = mmt_jar(os.path.join(BUILD_DIR, 'mmt-*.jar'))

# Environment setup
os.environ['LD_LIBRARY_PATH'] = os.pathsep.join(
    [LIB_DIR] + [x for x in os.environ['LD_LIBRARY_PATH'].split(os.pathsep)
                 if len(x) > 0]) if 'LD_LIBRARY_PATH' in os.environ else LIB_DIR
os.environ['LC_ALL'] = 'en_US.UTF-8'
os.environ['LANG'] = 'en_US.UTF-8'


def mmt_javamain(main_class, args=None, remote_debug=False, max_heap_mb=None, server=False, logs_path=None):
    classpath = [MMT_JAR]

    if os.path.isdir(PLUGINS_DIR):
        for filename in os.listdir(PLUGINS_DIR):
            if filename.endswith('.jar'):
                classpath.append(os.path.join(PLUGINS_DIR, filename))

    classpath = ':'.join(classpath)

    java_ops = []
    if remote_debug:
        java_ops.append('-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005')

    if server:
        java_ops.append('-server')

        if max_heap_mb is not None:
            java_ops.append('-Xms' + str(max_heap_mb) + 'm')
            java_ops.append('-Xmx' + str(max_heap_mb) + 'm')

        if logs_path is not None:
            java_ops += ['-XX:ErrorFile=' + os.path.join(logs_path, 'hs_err_pid%p.log')]

            java_ops += ['-XX:+PrintGCDateStamps', '-verbose:gc', '-XX:+PrintGCDetails',
                         '-Xloggc:' + os.path.join(logs_path, 'gc.log')]

            java_ops += ['-XX:+HeapDumpOnOutOfMemoryError', '-XX:HeapDumpPath=' + logs_path]

        java_ops += ['-XX:+CMSClassUnloadingEnabled', '-XX:+UseConcMarkSweepGC', '-XX:+CMSParallelRemarkEnabled',
                     '-XX:+UseCMSInitiatingOccupancyOnly', '-XX:CMSInitiatingOccupancyFraction=70',
                     '-XX:+ScavengeBeforeFullGC', '-XX:+CMSScavengeBeforeRemark', '-XX:+CMSClassUnloadingEnabled',
                     '-XX:+ExplicitGCInvokesConcurrentAndUnloadsClasses']
    else:
        if max_heap_mb is not None:
            java_ops.append('-Xmx' + str(max_heap_mb) + 'm')

    java_cmd = ['java'] + java_ops

    command = java_cmd + ['-cp', classpath, '-Dmmt.home=' + MMT_ROOT, '-Djava.library.path=' + LIB_DIR, main_class]
    if args is not None:
        command += args

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
