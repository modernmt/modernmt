import os
from glob import glob
from pathlib import Path


def __mmt_jar():
    jars = [f for f in glob(os.path.join(MMT_BUILD_DIR, 'mmt-*.jar'))]
    jars.sort(key=lambda x: os.path.getmtime(x))
    return jars[-1] if len(jars) > 0 else None


__this_dir = os.path.dirname(os.path.realpath(__file__))

MMT_HOME_DIR = os.path.abspath(os.path.join(__this_dir, os.pardir, os.pardir))
MMT_ENGINES_DIR = os.path.join(MMT_HOME_DIR, 'engines')
MMT_RUNTIME_DIR = os.path.join(MMT_HOME_DIR, 'runtime')
MMT_BUILD_DIR = os.path.join(MMT_HOME_DIR, 'build')
MMT_VENDOR_DIR = os.path.join(MMT_HOME_DIR, 'vendor')

MMT_PLUGINS_DIR = os.path.join(MMT_BUILD_DIR, 'plugins')
MMT_LIB_DIR = os.path.join(MMT_BUILD_DIR, 'lib')
MMT_BIN_DIR = os.path.join(MMT_BUILD_DIR, 'bin')
MMT_RES_DIR = os.path.join(MMT_BUILD_DIR, 'res')

MMT_JAR = __mmt_jar()
MMT_PLUGINS_JARS = [f for f in glob(os.path.join(MMT_PLUGINS_DIR, '*.jar'))] if os.path.isdir(MMT_PLUGINS_DIR) else []
MMT_FAIRSEQ_USER_DIR = os.path.join(MMT_JAR, 'mmt') if MMT_JAR is not None else None


def collect_parallel_files(src_lang, tgt_lang, paths):
    src_suffix, tgt_suffix = '.' + src_lang, '.' + tgt_lang
    all_src_files, all_tgt_files = [], []

    if isinstance(paths, str):
        paths = [paths]

    for path in paths:
        src_files = sorted(
            [os.path.abspath(os.path.join(path, f)) for f in os.listdir(path) if f.endswith(src_suffix)])
        tgt_files = sorted(
            [os.path.abspath(os.path.join(path, f)) for f in os.listdir(path) if f.endswith(tgt_suffix)])

        if len(src_files) == 0:
            raise IOError("no valid files found in %s" % path)

        if len(src_files) != len(tgt_files):
            raise IOError(
                "files are not parallel: src_files = %d, tgt_files = %d " % (len(src_files), len(tgt_files)))

        for src_file, tgt_file in zip(src_files, tgt_files):
            if src_file[:src_file.rfind('.')] != tgt_file[:tgt_file.rfind('.')]:
                raise IOError("invalid parallel file: %s" % src_file)

        all_src_files.extend(src_files)
        all_tgt_files.extend(tgt_files)

    return all_src_files, all_tgt_files


def collect_parallel_files_with_factor(src_lang, tgt_lang, paths):
    src_suffix, tgt_suffix, factor_suffix = '.' + src_lang, '.' + tgt_lang, '.factor'
    all_src_files, all_tgt_files, all_factor_files = [], [], []

    if isinstance(paths, str):
        paths = [paths]

    for path in paths:

        src_files = sorted([os.path.abspath(os.path.join(path, f)) for f in os.listdir(path) if f.endswith(src_suffix)])
        tgt_files = sorted([os.path.abspath(os.path.join(path, f)) for f in os.listdir(path) if f.endswith(tgt_suffix)])
        factor_files = []

        for sf in src_files:
            f = os.path.abspath(os.path.join(path, Path(sf).stem + factor_suffix))
            factor_files.append(f if os.path.exists(f) else None)

        if len(src_files) == 0:
            raise IOError("no valid files found in %s" % path)

        if len(src_files) != len(tgt_files):
            raise IOError(
                "files are not parallel: src_files = %d, tgt_files = %d " % (len(src_files), len(tgt_files)))

        if len(src_files) != len(factor_files):
            raise IOError(
                "files are not parallel: src_files = %d, factor_files = %d " % (len(src_files), len(factor_files)))

        for src_file, tgt_file, factor_file in zip(src_files, tgt_files, factor_files):
            if src_file[:src_file.rfind('.')] != tgt_file[:tgt_file.rfind('.')]:
                raise IOError("invalid parallel file: %s" % src_file)

            if factor_file is not None:
                if src_file[:src_file.rfind('.')] != factor_file[:factor_file.rfind('.')]:
                    raise IOError("invalid parallel file: %s" % src_file)

        all_src_files.extend(src_files)
        all_tgt_files.extend(tgt_files)
        all_factor_files.extend(factor_files)

        print('collect_parallel_files_with_factor all_src_files:{}'.format(all_src_files))
        print('collect_parallel_files_with_factor all_tgt_files:{}'.format(all_tgt_files))
        print('collect_parallel_files_with_factor all_factor_files:{}'.format(all_factor_files))

    return all_src_files, all_tgt_files, all_factor_files
