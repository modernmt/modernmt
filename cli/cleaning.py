import argparse
import glob
import math
import multiprocessing
import os

from cli import CLIArgsException, StatefulActivity, activitystep
from cli.mmt.fileformats import ParallelFileFormat, DevNullFileFormat
from cli.mmt.mmtcli import mmt_tmsclean, mmt_dedup, mmt_preprocess, fastalign_build, fastalign_score


def _filtered_corpus(stats, src_lang, tgt_lang, name, in_path, score_path):
    good_avg, good_std_dev, bad_avg, bad_std_dev = stats

    src_file = os.path.join(in_path, name + '.' + src_lang)
    tgt_file = os.path.join(in_path, name + '.' + tgt_lang)
    score_file = os.path.join(score_path, name + '.score')

    if not os.path.isfile(score_file):
        if os.path.getsize(src_file) > 0 or os.path.getsize(tgt_file) > 0:
            raise IOError('File not found: %s' % score_file)
        else:
            return

    with open(src_file, 'r', encoding='utf-8') as src_file_obj, \
            open(tgt_file, 'r', encoding='utf-8') as tgt_file_obj, \
            open(score_file, 'r', encoding='utf-8') as score_file_obj:
        for src_line, tgt_line, score_line in zip(src_file_obj, tgt_file_obj, score_file_obj):
            score = float(score_line.strip())

            if math.isnan(score):
                accept = False
            elif score >= good_avg:
                accept = True
            elif score <= bad_avg:
                accept = False
            else:
                good_distance = (good_avg - score) / good_std_dev
                bad_distance = (score - bad_avg) / bad_std_dev

                accept = good_distance < bad_distance

            yield src_line, tgt_line, accept


def _apply_filter(stats, src_lang, tgt_lang, name, in_path, score_path, out_path, trash_path=None):
    out_file = ParallelFileFormat.from_path(src_lang, tgt_lang, name, out_path)
    trash_file = ParallelFileFormat.from_path(src_lang, tgt_lang, name, trash_path) \
        if trash_path is not None else DevNullFileFormat()

    with out_file.writer() as out_writer, trash_file.writer() as trash_writer:
        for src_line, tgt_line, accept in _filtered_corpus(stats, src_lang, tgt_lang, name, in_path, score_path):
            if accept:
                out_writer.write(src_line, tgt_line)
            else:
                trash_writer.write(src_line, tgt_line)


class CleaningActivity(StatefulActivity):
    def __init__(self, args, extra_argv=None, wdir=None, log_file=None, start_step=None, delete_on_exit=True):
        super().__init__(args, extra_argv, wdir, log_file, start_step, delete_on_exit)

    @activitystep('Cleaning corpora')
    def clean(self):
        self.state.clean_corpora = self.wdir('clean_corpora')
        mmt_tmsclean(self.args.src_lang, self.args.tgt_lang,
                     self.args.input_path, self.state.clean_corpora, out_format='parallel')

    @activitystep('De-duplicate corpora')
    def dedup(self):
        in_path = self.state.clean_corpora or self.args.input_path
        self.state.dedup_corpora = self.wdir('dedup_corpora')

        mmt_dedup(self.args.src_lang, self.args.tgt_lang, in_path, self.state.dedup_corpora,
                  sort=self.args.dedup_sort)

    @activitystep('Preprocess corpora')
    def preprocess(self):
        # Pre-process training corpora
        in_path = self.state.dedup_corpora or self.state.clean_corpora or self.args.input_path
        self.state.preprocessed_corpora = self.wdir('preprocessed_corpora')
        mmt_preprocess(self.args.src_lang, self.args.tgt_lang, in_path, self.state.preprocessed_corpora)

    @activitystep('Creating aligner filter')
    def make_filter(self):
        self.state.aligner = fa_model = self.wdir('aligner')
        fastalign_build(self.args.src_lang, self.args.tgt_lang, self.state.preprocessed_corpora, fa_model,
                        iterations=4, case_sensitive=False, favor_diagonal=False, log=self.log_fobj)

    @activitystep('Scoring corpora')
    def score(self):
        good_avg, good_std_dev, bad_avg, bad_std_dev = fastalign_score(
            self.args.src_lang, self.args.tgt_lang, self.state.aligner, self.state.preprocessed_corpora)
        self.state.filter_stats = (good_avg, good_std_dev, bad_avg, bad_std_dev)

    @activitystep('Apply aligner-based filter')
    def apply_filter(self):
        self._logger.info('Applying aligner filter with: '
                          'good_avg = %f, good_std_dev = %f, bad_avg = %f, bad_std_dev = %f' % self.state.filter_stats)

        in_path = self.state.dedup_corpora or self.state.clean_corpora or self.args.input_path
        trash_path = self.wdir('trash_bin') if self.args.debug else None

        os.makedirs(self.args.output_path, exist_ok=True)

        filenames = [os.path.splitext(os.path.basename(f))[0]
                     for f in glob.glob(os.path.join(in_path, '*.' + self.args.src_lang))]

        with multiprocessing.Pool(min(16, multiprocessing.cpu_count())) as pool:
            jobs = []

            for name in filenames:
                args = (self.state.filter_stats, self.args.src_lang, self.args.tgt_lang, name,
                        in_path, self.state.preprocessed_corpora, self.args.output_path, trash_path)
                job = pool.apply_async(_apply_filter, args)
                jobs.append(job)

            for job in jobs:
                job.get()


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Clean parallel corpora before training', prog='mmt clean')
    parser.add_argument('src_lang', metavar='SOURCE_LANGUAGE', help='the source language (ISO 639-1)')
    parser.add_argument('tgt_lang', metavar='TARGET_LANGUAGE', help='the target language (ISO 639-1)')
    parser.add_argument('input_path', metavar='INPUT', help='the path to the corpora to clean')
    parser.add_argument('output_path', metavar='OUTPUT', help='the destination folder')
    parser.add_argument('--dedup-sort', metavar='SUBSTRING', dest='dedup_sort', default=None, nargs='+',
                        help='list of substrings to use to sort corpora during deduplication')
    parser.add_argument('-w', '--working-dir', metavar='WORKING_DIR', dest='wdir', default=None,
                        help='the working directory for temporary files (default is os temp folder)')
    parser.add_argument('-d', '--debug', action='store_true', dest='debug', default=False,
                        help='prevents temporary files to be removed after execution')
    parser.add_argument('--log', dest='log_file', default=None, help='detailed log file')

    args = parser.parse_args(argv)
    if args.debug and args.wdir is None:
        raise CLIArgsException(parser, '"--debug" options requires explicit working dir with "--working-dir"')
    return args


def main(argv=None):
    args = parse_args(argv)
    activity = CleaningActivity(args, wdir=args.wdir, log_file=args.log_file, delete_on_exit=not args.debug)
    activity.run()
