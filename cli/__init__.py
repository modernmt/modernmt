import glob
import os


def mmt_jar(pattern):
    jars = [f for f in glob.glob(pattern)]
    jars.sort(key=lambda e: os.path.getmtime(e))
    return jars[-1] if len(jars) > 0 else None


__this_dir = os.path.dirname(os.path.realpath(__file__))

MMT_HOME_DIR = os.path.abspath(os.path.join(__this_dir, os.pardir))
MMT_OPT_DIR = os.path.join(__this_dir, 'opt')
MMT_ENGINES_DIR = os.path.join(MMT_HOME_DIR, 'engines')
MMT_RUNTIME_DIR = os.path.join(MMT_HOME_DIR, 'runtime')
MMT_BUILD_DIR = os.path.join(MMT_HOME_DIR, 'build')
MMT_VENDOR_DIR = os.path.join(MMT_HOME_DIR, 'vendor')

MMT_PLUGINS_DIR = os.path.join(MMT_BUILD_DIR, 'plugins')
MMT_LIB_DIR = os.path.join(MMT_BUILD_DIR, 'lib')
MMT_BIN_DIR = os.path.join(MMT_BUILD_DIR, 'bin')
MMT_RES_DIR = os.path.join(MMT_BUILD_DIR, 'res')

MMT_JAR = mmt_jar(os.path.join(MMT_BUILD_DIR, 'mmt-*.jar'))

# Environment setup
os.environ['LD_LIBRARY_PATH'] = os.pathsep.join(
    [MMT_LIB_DIR] + [x for x in os.environ['LD_LIBRARY_PATH'].split(os.pathsep)
                     if len(x) > 0]) if 'LD_LIBRARY_PATH' in os.environ else MMT_LIB_DIR
os.environ['LC_ALL'] = 'en_US.UTF-8'
os.environ['LANG'] = 'en_US.UTF-8'
