import argparse
import os
import shutil

from cli import CLIArgsException, StatefulActivity, activitystep
from cli.mmt.mmtcli import mmt_preprocess


class TokenizeActivity(StatefulActivity):
    DEFAULT_VALIDATION_LINES = 3000

    def __init__(self, args, extra_argv=None, wdir=None, log_file=None, start_step=None, delete_on_exit=True):
        super().__init__(args, extra_argv, wdir, log_file, start_step, delete_on_exit)

        self._langs = [tuple(lp.split(':')) for lp in args.lang_pairs.split(',')]
        self._mono_pairs = [tuple(lp.split(':')) for lp in set([('%s:%s' % tuple(sorted(p))) for p in self._langs])]

    @activitystep('Tokenize corpora')
    def tokenize(self):
        self.state.tokenized_corpora = self.args.output_path

        train_paths, dev_paths = [], []

        partition_size = self.DEFAULT_VALIDATION_LINES
        if len(self._langs) > 1:
            partition_size = int((2 * self.DEFAULT_VALIDATION_LINES) / len(self._langs))

        for src_lang, tgt_lang in self._mono_pairs:
            lang_dir = '%s__%s' % (src_lang, tgt_lang)

            train_path = os.path.join(self.state.tokenized_corpora, lang_dir, 'train')
            raw_dev_path = os.path.join(self.state.tokenized_corpora, lang_dir, '_dev')
            dev_path = os.path.join(self.state.tokenized_corpora, lang_dir, 'dev')

            test_path = os.path.join(self.args.test_dir, lang_dir) if self.args.test_dir is not None else None

            mmt_preprocess(src_lang, tgt_lang, self.args.input_paths, train_path,
                           dev_path=raw_dev_path, test_path=test_path, partition_size=partition_size)
            mmt_preprocess(src_lang, tgt_lang, raw_dev_path, dev_path)

            shutil.rmtree(raw_dev_path)

            train_paths.append(train_path)
            dev_paths.append(dev_path)


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Generate archives for neural training', prog='mmt datagen')
    parser.add_argument('lang_pairs', metavar='LANGUAGE_PAIRS',
                        help='the language pair list encoded as <ls1>:<t1>[,<lsn>:<ltn>] (i.e. en:it,it:en,en:fr)')
    parser.add_argument('output_path', metavar='OUTPUT', help='the destination folder')
    parser.add_argument('input_paths', nargs='+', metavar='INPUT_PATHS', help='the paths to the training corpora')
    parser.add_argument('-w', '--working-dir', metavar='WORKING_DIR', dest='wdir', default=None,
                        help='the working directory for temporary files (default is os temp folder)')
    parser.add_argument('-d', '--debug', action='store_true', dest='debug', default=False,
                        help='prevents temporary files to be removed after execution')
    parser.add_argument('-T', '--threads', dest='threads', default=2, type=int,
                        help='the number of threads used to find the bounds for vocabulary creation (default is 2)')
    parser.add_argument('--log', dest='log_file', default=None, help='detailed log file')
    parser.add_argument('--test', metavar='TEST_SET_DIR', dest='test_dir', default=None,
                        help='optional directory where to store a small subset of training data for testing')


    args = parser.parse_args(argv)
    if args.debug and args.wdir is None:
        raise CLIArgsException(parser, '"--debug" options requires explicit working dir with "--working-dir"')
    return args


def main(argv=None):
    args = parse_args(argv)
    activity = TokenizeActivity(args, wdir=args.wdir, log_file=args.log_file, delete_on_exit=not args.debug)
    activity.run()
