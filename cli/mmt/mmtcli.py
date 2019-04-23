import os
import re

from cli.mmt import MMT_HOME_DIR, MMT_LIB_DIR, MMT_BIN_DIR, MMT_JAR, MMT_PLUGINS_JARS
from cli.utils import osutils


def __get_java_version():
    try:
        stdout, stderr = osutils.shell_exec(['java', '-version'])
        java_output = stdout + '\n' + stderr

        for line in java_output.split('\n'):
            tokens = line.split()
            if 'version' in tokens:
                version = tokens[tokens.index('version') + 1]
                version = version.strip('"')

                if version.startswith('1.'):
                    version = version[2:]

                version = re.match('^[0-9]+', version)
                return int(version.group())

        return None
    except OSError:
        return None


__java_version = __get_java_version()
assert __java_version is not None, 'missing Java executable, please check INSTALL.md'
assert __java_version > 7, 'wrong version of Java: required Java 8 or higher'


def __mmt_env():
    llp = (MMT_LIB_DIR + os.pathsep + os.environ['LD_LIBRARY_PATH']) if 'LD_LIBRARY_PATH' in os.environ else MMT_LIB_DIR
    return dict(os.environ, LD_LIBRARY_PATH=llp, LC_ALL='C.UTF-8', LANG='C.UTF-8')


if 'MMT_HOME' not in os.environ:
    os.environ['MMT_HOME'] = MMT_HOME_DIR


# - ModernMT CLI functions ---------------------------------------------------------------------------------------------


def mmt_java(main_class, args=None, *,
             java_ops=None, remote_debug=False, max_heap_mb=None, server=False, logs_path=None):
    java_ops = java_ops or []
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

    classpath = ':'.join([MMT_JAR] + MMT_PLUGINS_JARS)
    java_cmd = ['java'] + java_ops + \
               ['-cp', classpath, '-Dmmt.home=' + MMT_HOME_DIR, '-Djava.library.path=' + MMT_LIB_DIR, main_class]

    if args is not None:
        java_cmd += args

    return java_cmd


def mmt_tmsclean(src_lang, tgt_lang, in_path, out_path, out_format=None, filters=None):
    args = ['-s', src_lang, '-t', tgt_lang, '--input', in_path, '--output', out_path]
    if out_format is not None:
        args += ['--output-format', out_format]
    if filters is not None and len(filters) > 0:
        args += ['--filters'] + filters

    extended_heap_mb = int(osutils.mem_size() * 90 / 100)

    java_ops = ['-DentityExpansionLimit=0', '-DtotalEntitySizeLimit=0', '-Djdk.xml.totalEntitySizeLimit=0']
    command = mmt_java('eu.modernmt.cli.CleaningPipelineMain', args, max_heap_mb=extended_heap_mb, java_ops=java_ops)
    osutils.shell_exec(command, env=__mmt_env())


def mmt_preprocess(src_lang, tgt_lang, in_paths, out_path, dev_path=None, test_path=None, partition_size=None):
    args = ['-s', src_lang, '-t', tgt_lang, '--output', out_path, '--input']
    if isinstance(in_paths, str):
        in_paths = [in_paths]

    args += in_paths

    if partition_size is not None:
        args += ['--size', str(partition_size)]
    if dev_path is not None:
        args += ['--dev', dev_path]
    if test_path is not None:
        args += ['--test', test_path]

    command = mmt_java('eu.modernmt.cli.TrainingPipelineMain', args)
    osutils.shell_exec(command, env=__mmt_env())


def mmt_dedup(src_lang, tgt_lang, in_path, out_path, length_threshold=None):
    args = ['-s', src_lang, '-t', tgt_lang, '--input', in_path, '--output', out_path]
    if length_threshold is not None and length_threshold > 0:
        args += ['-l', length_threshold]

    command = mmt_java('eu.modernmt.cli.DeduplicationMain', args)
    osutils.shell_exec(command, env=__mmt_env())


# - Fastalign CLI functions --------------------------------------------------------------------------------------------


def fastalign_build(src_lang, tgt_lang, in_path, out_model, iterations=None,
                    case_sensitive=True, favor_diagonal=True, log=None):
    os.makedirs(out_model, exist_ok=True)
    out_model = os.path.join(out_model, '%s__%s.fam' % (src_lang, tgt_lang))

    if log is None:
        log = osutils.DEVNULL

    command = [os.path.join(MMT_BIN_DIR, 'fa_build'), '-s', src_lang, '-t', tgt_lang, '-i', in_path, '-m', out_model]

    if iterations is not None:
        command.extend(['-I', str(iterations)])
    if not case_sensitive:
        command.append('--case-insensitive')
    if not favor_diagonal:
        command.append('--no-favor-diagonal')

    osutils.shell_exec(command, stdout=log, stderr=log, env=__mmt_env())


def fastalign_score(src_lang, tgt_lang, model_path, in_path, out_path=None):
    model_path = os.path.join(model_path, '%s__%s.fam' % (src_lang, tgt_lang))

    command = [os.path.join(MMT_BIN_DIR, 'fa_score'), '-s', src_lang, '-t', tgt_lang,
               '-m', model_path, '-i', in_path, '-o', out_path or in_path]
    stdout, _ = osutils.shell_exec(command, env=__mmt_env())

    result = dict()
    for line in stdout.splitlines(keepends=False):
        key, value = line.split('=', maxsplit=1)
        result[key] = float(value)

    return result['good_avg'], result['good_std_dev'], result['bad_avg'], result['bad_std_dev']
