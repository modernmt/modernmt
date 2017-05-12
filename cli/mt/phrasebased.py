import os
import shutil

import cli
from cli import mmt_javamain
from cli.libs import fileutils
from cli.libs import shell
from cli.mt import BilingualCorpus

__author__ = 'Davide Caroselli'


class ContextAnalyzer:
    def __init__(self, index, source_lang, target_lang):
        self._index = index
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._java_mainclass = 'eu.modernmt.cli.ContextAnalyzerMain'

    def create_index(self, corpora, log=None):
        if log is None:
            log = shell.DEVNULL

        source_paths = set()

        for corpus in corpora:
            source_paths.add(corpus.get_folder())

        shutil.rmtree(self._index, ignore_errors=True)
        fileutils.makedirs(self._index, exist_ok=True)

        args = ['-s', self._source_lang, '-t', self._target_lang, '-i', self._index, '-c']
        for source_path in source_paths:
            args.append(source_path)

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdout=log, stderr=log)


class MosesFeature:
    def __init__(self, classname):
        self.classname = classname

    def get_relpath(self, base_path, path):
        path = os.path.abspath(path)
        base_path = os.path.abspath(base_path)

        path = path.replace(base_path, '').lstrip(os.sep)

        return '${DECODER_PATH}' + path

    def get_iniline(self, base_path):
        return None


class InterpolatedLM(MosesFeature):
    def __init__(self, model, order=5, prune=True):
        MosesFeature.__init__(self, 'MMTILM')

        self._create_alm_bin = os.path.join(cli.BIN_DIR, 'create_alm')
        self._create_slm_bin = os.path.join(cli.BIN_DIR, 'create_slm')

        self._model = model
        self._prune = prune
        self._order = order

        self._quantization = 8
        self._compression = 32

    def train(self, corpora, lang, working_dir='.', log=None):
        if log is None:
            log = shell.DEVNULL

        bicorpora = []
        for corpus in corpora:
            if len(corpus.langs) > 1:
                bicorpora.append(corpus)

        shutil.rmtree(self._model, ignore_errors=True)
        fileutils.makedirs(self._model, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)

        # Train static LM
        static_lm_model = os.path.join(self._model, 'background.slm')
        static_lm_wdir = os.path.join(working_dir, 'slm.temp')

        fileutils.makedirs(static_lm_wdir, exist_ok=True)

        merged_corpus = os.path.join(working_dir, 'merged_corpus')
        fileutils.merge([corpus.get_file(lang) for corpus in corpora], merged_corpus)

        command = [self._create_slm_bin, '--discount-fallback', '-o', str(self._order),
                   '-a', str(self._compression), '-q', str(self._quantization), '--type', 'trie',
                   '--model', static_lm_model,
                   '-T', static_lm_wdir]
        if self._order > 2 and self._prune:
            command += ['--prune', '0', '1', '2']

        with open(merged_corpus) as stdin:
            shell.execute(command, stdin=stdin, stdout=log, stderr=log)

        # Create AdaptiveLM training folder
        alm_train_folder = os.path.join(working_dir, 'alm_train')
        fileutils.makedirs(alm_train_folder, exist_ok=True)

        for corpus in bicorpora:
            os.symlink(corpus.get_file(lang), os.path.join(alm_train_folder, corpus.name + '.' + lang))

        # Train adaptive LM
        adaptive_lm_model = os.path.join(self._model, 'foreground.alm')
        fileutils.makedirs(adaptive_lm_model, exist_ok=True)

        command = [self._create_alm_bin, '-m', adaptive_lm_model, '-i', alm_train_folder, '-b', '50000000']
        shell.execute(command, stdout=log, stderr=log)

    def get_iniline(self, base_path):
        return 'path={model} static-type=trie quantization={q} compression={c}'.format(
            model=self.get_relpath(base_path, self._model), q=self._quantization, c=self._compression
        )


class FastAlign:
    def __init__(self, model, source_lang, target_lang):
        self._model = model
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._build_bin = os.path.join(cli.BIN_DIR, 'fa_build')
        self._align_bin = os.path.join(cli.BIN_DIR, 'fa_align')
        self._export_bin = os.path.join(cli.BIN_DIR, 'fa_export')

    def build(self, corpora, working_dir='.', log=None):
        if log is None:
            log = shell.DEVNULL

        shutil.rmtree(self._model, ignore_errors=True)
        fileutils.makedirs(self._model, exist_ok=True)

        if not os.path.isdir(working_dir):
            fileutils.makedirs(working_dir, exist_ok=True)

        merged_corpus = BilingualCorpus.make_parallel('merge', working_dir, (self._source_lang, self._target_lang))

        fileutils.merge([corpus.get_file(self._source_lang) for corpus in corpora],
                        merged_corpus.get_file(self._source_lang))
        fileutils.merge([corpus.get_file(self._target_lang) for corpus in corpora],
                        merged_corpus.get_file(self._target_lang))

        command = [self._build_bin,
                   '-s', merged_corpus.get_file(self._source_lang), '-t', merged_corpus.get_file(self._target_lang),
                   '-m', self._model, '-I', '4']
        shell.execute(command, stdout=log, stderr=log)

    def align(self, corpora, output_folder, log=None):
        if log is None:
            log = shell.DEVNULL

        root = set([corpus.get_folder() for corpus in corpora])

        if len(root) != 1:
            raise Exception('Aligner corpora must share the same folder: found  ' + str(root))

        root = root.pop()

        command = [self._align_bin, '--model', self._model,
                   '--input', root, '--output', output_folder,
                   '--source', self._source_lang, '--target', self._target_lang,
                   '--strategy', '1']
        shell.execute(command, stderr=log, stdout=log)

    def export(self, path, log=None):
        if log is None:
            log = shell.DEVNULL

        command = [self._export_bin, '--model', self._model, '--output', path]
        shell.execute(command, stderr=log, stdout=log)


class LexicalReordering(MosesFeature):
    def __init__(self):
        MosesFeature.__init__(self, 'LexicalReordering')

    def get_iniline(self, base_path):
        return 'input-factor=0 output-factor=0 type=hier-mslr-bidirectional-fe-allff'


class SuffixArraysPhraseTable(MosesFeature):
    def __init__(self, model, source_lang, target_lang, sample=1000):
        MosesFeature.__init__(self, 'SAPT')

        self._sample = sample

        self._model = model
        self._reordering_model_feature = None
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._build_bin = os.path.join(cli.BIN_DIR, 'sapt_build')

    def _get_model_basename(self):
        return os.path.join(self._model, 'model')

    def set_reordering_model(self, name):
        self._reordering_model_feature = name

    def get_iniline(self, base_path):
        result = 'path={model} input-factor=0 output-factor=0 sample-limit={sample}'.format(
            model=self.get_relpath(base_path, self._model), sample=self._sample
        )

        if self._reordering_model_feature is not None:
            result += ' lr-func={name}'.format(name=self._reordering_model_feature)

        return result

    def train(self, corpora, aligner, working_dir='.', log=None):
        if log is None:
            log = shell.DEVNULL

        shutil.rmtree(self._model, ignore_errors=True)
        fileutils.makedirs(self._model, exist_ok=True)

        train_corpora_path = os.path.join(working_dir, 'corpora')
        lex_model_path = os.path.join(working_dir, 'model.tlex')

        if not os.path.isdir(train_corpora_path):
            fileutils.makedirs(train_corpora_path, exist_ok=True)

        train_corpora = []  # Prepare training folder
        for corpus in corpora:
            dest_corpus = BilingualCorpus.make_parallel(corpus.name, train_corpora_path,
                                                        (self._source_lang, self._target_lang))
            source_file = corpus.get_file(self._source_lang)
            target_file = corpus.get_file(self._target_lang)

            os.symlink(source_file, dest_corpus.get_file(self._source_lang))
            os.symlink(target_file, dest_corpus.get_file(self._target_lang))

            train_corpora.append(dest_corpus)

        # Align corpora
        aligner.align(train_corpora, train_corpora_path, log=log)
        aligner.export(lex_model_path)

        # Build models
        command = [self._build_bin, '--lex', lex_model_path, '--input', train_corpora_path, '--model', self._model,
                   '-s', self._source_lang, '-t', self._target_lang]
        shell.execute(command, stdout=log, stderr=log)


class Moses:
    def __init__(self, model_path, source_lang, target_lang,
                 stack_size=1000, cube_pruning_pop_limit=1000, distortion_limit=6):
        self._path = model_path

        self._stack_size = stack_size
        self._cube_pruning_pop_limit = cube_pruning_pop_limit
        self._distortion_limit = distortion_limit

        self._ini_file = os.path.join(self._path, 'moses.ini')
        self._weights_file = os.path.join(self._path, 'weights.dat')

        self.lm = InterpolatedLM(os.path.join(model_path, 'lm'))
        self.pt = SuffixArraysPhraseTable(os.path.join(model_path, 'sapt'), source_lang, target_lang)
        self.pt.set_reordering_model('DM0')

        self._features = []
        self.add_feature(self.lm, 'InterpolatedLM')
        self.add_feature(self.pt, 'Sapt')
        self.add_feature(LexicalReordering(), 'DM0')
        self.add_feature(MosesFeature('Distortion'))
        self.add_feature(MosesFeature('WordPenalty'))
        self.add_feature(MosesFeature('PhrasePenalty'))
        self.add_feature(MosesFeature('UnknownWordPenalty'))

        self._optimal_weights = {
            'InterpolatedLM': [0.0883718],
            'Sapt': [0.0277399, 0.0391562, 0.00424704, 0.0121731],
            'DM0': [0.0153337, 0.0181129, 0.0423417, 0.0203163, 0.261833, 0.126704, 0.0670114, 0.0300892],
            'Distortion0': [0.0335557],
            'WordPenalty0': [-0.0750738],
            'PhrasePenalty0': [-0.13794],
        }

    def add_feature(self, feature, name=None):
        self._features.append((feature, name))

    def __get_iniline(self, feature, name=None):
        custom = feature.get_iniline(self._path)
        line = feature.classname

        if name is not None:
            line += ' name=' + name

        if custom is not None:
            line += ' ' + custom

        return line

    def create_configs(self):
        self.__create_ini()
        self.__store_default_weights(self._optimal_weights)

    def __create_ini(self):
        lines = ['[input-factors]', '0', '', '[search-algorithm]', '1', '', '[stack]', str(self._stack_size), '',
                 '[cube-pruning-pop-limit]', str(self._cube_pruning_pop_limit), '', '[mapping]', '0 T 0', '',
                 '[distortion-limit]', str(self._distortion_limit), '', '[threads]', '${DECODER_THREADS}', '',
                 '[verbose]', '0', '', '[feature]']

        for feature in self._features:
            lines.append(self.__get_iniline(*feature))
        lines.append('')

        with open(self._ini_file, 'wb') as out:
            out.write('\n'.join(lines))

    def __store_default_weights(self, weights):
        lines = [('%s = %s\n' % (key, ' '.join([str(v) for v in value]))) for key, value in weights.iteritems()]

        with open(self._weights_file, 'wb') as out:
            out.writelines(lines)
