import argparse
import os
import shutil

from cli import StatefulActivity, cleaning, datagen, train, Namespace, activitystep
from cli.mmt.engine import Engine, EngineNode


class CreateActivity(StatefulActivity):
    def __init__(self, engine, args, extra_argv=None, wdir=None, log_file=None, start_step=None, delete_on_exit=True):
        super().__init__(args, extra_argv, wdir=wdir, log_file=log_file, start_step=start_step,
                         delete_on_exit=delete_on_exit)
        self._engine = engine
        self.has_sub_activities = True

    @activitystep('Cleaning corpora')
    def clean(self):
        if self.args.skip_cleaning:
            return

        self.state.corpora_clean = self.wdir('corpora_clean')

        args = Namespace(src_lang=self.args.src_lang, tgt_lang=self.args.tgt_lang, input_path=self.args.input_path,
                         output_path=self.state.corpora_clean)
        activity = cleaning.CleaningActivity(args, wdir=self.wdir('_temp_cleaning'), log_file=self.log_fobj.name,
                                             delete_on_exit=self.delete_on_exit)
        activity.indentation = 4
        activity.run()

    @activitystep('Generating binary archives')
    def datagen(self):
        input_path = self.state.corpora_clean or self.args.input_path
        self.state.datagen_dir = self.wdir('data_generated')

        args = Namespace(lang_pairs='%s:%s' % (self.args.src_lang, self.args.tgt_lang),
                         input_paths=[input_path], output_path=self.state.datagen_dir,
                         voc_size=self.args.voc_size, threads=self.args.threads,
                         count_threshold=self.args.count_threshold, vocabulary_path=self.args.vocabulary_path)
        activity = datagen.DatagenActivity(args, wdir=self.wdir('_temp_datagen'), log_file=self.log_fobj.name,
                                           delete_on_exit=self.delete_on_exit)
        activity.indentation = 4
        activity.run()

    @activitystep('Training neural model')
    def train(self):
        self.state.nn_path = self.wdir('nn_model')
        args = Namespace(data_path=self.state.datagen_dir, output_path=self.state.nn_path,
                         num_checkpoints=self.args.num_checkpoints)

        activity = train.TrainActivity(args, self.extra_argv, wdir=self.wdir('_temp_train'),
                                       log_file=self.log_fobj.name, delete_on_exit=self.delete_on_exit)
        activity.indentation = 4
        activity.run()

    @activitystep('Creating engine')
    def finalize(self):
        if os.path.isdir(self._engine.path):
            shutil.rmtree(self._engine.path)
        os.makedirs(self._engine.path)

        # Write engine config
        config_content = \
            '<node xsi:schemaLocation="http://www.modernmt.eu/schema/config mmt-config-1.0.xsd"\n' \
            '      xmlns="http://www.modernmt.eu/schema/config"\n' \
            '      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n' \
            '   <engine source-language="%s" target-language="%s" />\n' \
            '</node>\n' % (self.args.src_lang, self.args.tgt_lang)

        with open(self._engine.config_path, 'w', encoding='utf-8') as config:
            config.write(config_content)

        # Create decoder folder
        lang_tag = '%s__%s' % (self.args.src_lang, self.args.tgt_lang)
        decoder_dir = os.path.join(self._engine.models_path, 'decoder')
        os.makedirs(decoder_dir)

        with open(os.path.join(decoder_dir, 'model.conf'), 'w', encoding='utf-8') as config:
            config.write('[models]\n%s = %s/\n' % (lang_tag, lang_tag))

        os.rename(self.state.nn_path, os.path.join(decoder_dir, lang_tag))


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Create and train a new ModernMT engine from scratch')
    parser.prog = 'mmt create'
    parser.add_argument('src_lang', metavar='SOURCE_LANGUAGE', help='the source language (ISO 639-1)')
    parser.add_argument('tgt_lang', metavar='TARGET_LANGUAGE', help='the target language (ISO 639-1)')
    parser.add_argument('input_path', metavar='INPUT', help='the path to the parallel corpora collection')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, "default" will be used if absent',
                        default='default')
    parser.add_argument('-d', '--debug', action='store_true', dest='debug', default=False,
                        help='prevents temporary files to be removed after execution')
    parser.add_argument('-y', '--yes', action='store_true', dest='force_delete', default=False,
                        help='if set, skip engine overwrite confirmation check')

    cleaning_args = parser.add_argument_group('Data cleaning arguments')
    cleaning_args.add_argument('--skip-cleaning', action='store_true', dest='skip_cleaning', default=False,
                               help='skip the cleaning step (input corpora MUST be in plain text parallel format)')

    datagen_args = parser.add_argument_group('Data generation arguments')
    datagen_args.add_argument('--voc-size', dest='voc_size', default=32768, type=int,
                              help='the vocabulary size to use (default is 32768)')
    datagen_args.add_argument('-T', '--threads', dest='threads', default=2, type=int,
                              help='the number of threads used in bounds search for vocabulary creation (default is 2)')
    datagen_args.add_argument('--count-threshold', dest='count_threshold', default=None, type=int,
                              help='all tokens with a count less than this threshold will be used '
                                   'only for alphabet generation in vocabulary creation, useful for very large corpus')
    datagen_args.add_argument('--vocabulary', metavar='VOCABULARY_PATH', dest='vocabulary_path', default=None,
                              help='use the specified bpe vocabulary model instead of re-train a new one from scratch')

    train_args = parser.add_argument_group('Train arguments (note: you can use all fairseq cli options)')
    train_args.add_argument('-n', '--checkpoints-num', dest='num_checkpoints', type=int, default=10,
                            help='number of checkpoints to average (default is 10)')

    args, extra_argv = parser.parse_known_args(argv)

    return args, train.parse_extra_argv(parser, extra_argv)


def main(argv=None):
    args, extra_argv = parse_args(argv)
    engine = Engine(args.engine)

    wdir = engine.get_tempdir('training')
    log_file = engine.get_logfile('training')

    if engine.exists():
        proceed = True

        if not args.force_delete:
            while True:
                resp = input('An engine named "%s" already exists, '
                             'are you sure you want to overwrite it? [y/N] ' % args.engine)
                resp = resp.lower()
                if len(resp) == 0 or resp == 'n':
                    proceed = False
                    break
                elif resp == 'y':
                    break

        if not proceed:
            print('Aborted')
            exit(0)
        else:
            shutil.rmtree(wdir, ignore_errors=True)
            os.makedirs(wdir)

            if os.path.isfile(log_file):
                os.remove(log_file)

            node = EngineNode(engine)
            node.stop()

    activity = CreateActivity(engine, args, extra_argv, wdir=wdir, log_file=log_file, delete_on_exit=not args.debug)
    activity.run()
