import argparse
import os
import shutil

from cli import StatefulActivity, cleaning, datagen, train, Namespace, activitystep, CLIArgsException
from cli.mmt.engine import Engine, EngineNode
from cli.utils import nvidia_smi


class HWConstraintViolated(Exception):
    def __init__(self, cause):
        self.cause = cause


class CreateActivity(StatefulActivity):
    def __init__(self, engine, args, extra_argv=None, wdir=None, log_file=None, start_step=None, delete_on_exit=True):
        super().__init__(args, extra_argv, wdir=wdir, log_file=log_file, start_step=start_step,
                         delete_on_exit=delete_on_exit)
        self._engine = engine
        self.has_sub_activities = True

        if self.args.skip_cleaning:
            self._remove_step('clean')

        if args.resume:
            # force train step to be executed even if completed
            train_step_idx = self._index_of_step('train')
            self.state.step_no = min(self.state.step_no, train_step_idx - 1)

    @activitystep('Cleaning corpora')
    def clean(self):
        self.state.corpora_clean = self.wdir('corpora_clean')

        args = Namespace(src_lang=self.args.src_lang, tgt_lang=self.args.tgt_lang, input_path=self.args.input_path,
                         output_path=self.state.corpora_clean, debug=self.args.debug)
        activity = cleaning.CleaningActivity(args, wdir=self.wdir('_temp_cleaning'), log_file=self.log_fobj,
                                             delete_on_exit=self.delete_on_exit)
        activity.indentation = 4
        activity.run()

    @activitystep('Creating engine')
    def mkengine(self):
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
        self.state.decoder_dir = os.path.join(self._engine.models_path, 'decoder')
        os.makedirs(self.state.decoder_dir)

        with open(os.path.join(self.state.decoder_dir, 'model.conf'), 'w', encoding='utf-8') as config:
            config.write('[models]\n%s = %s/\n' % (lang_tag, lang_tag))

    @activitystep('Generating binary archives')
    def datagen(self):
        input_path = self.state.corpora_clean or self.args.input_path
        self.state.datagen_dir = self.wdir('data_generated')

        vocabulary_path = self.args.vocabulary_path
        if self.args.init_model is not None:
            vocabulary_path = os.path.join(self.args.init_model, 'model.vcb')

        args = Namespace(lang_pairs='%s:%s' % (self.args.src_lang, self.args.tgt_lang), debug=self.args.debug,
                         input_paths=[input_path], output_path=self.state.datagen_dir,
                         voc_size=self.args.voc_size, threads=self.args.threads,
                         count_threshold=self.args.count_threshold, vocabulary_path=vocabulary_path,
                         test_dir=self._engine.test_data_path if self.args.test_set else None)
        activity = datagen.DatagenActivity(args, wdir=self.wdir('_temp_datagen'), log_file=self.log_fobj,
                                           delete_on_exit=self.delete_on_exit)
        activity.indentation = 4
        activity.run()

    @activitystep('Training neural model')
    def train(self):
        self.state.nn_path = self.wdir('nn_model')

        init_model = None
        if self.args.init_model is not None:
            init_model = os.path.join(self.args.init_model, 'model.pt')

        args = Namespace(data_path=self.state.datagen_dir, output_path=self.state.nn_path, debug=self.args.debug,
                         num_checkpoints=self.args.num_checkpoints, resume=self.args.resume, init_model=init_model,
                         gpus=self.args.gpus, tensorboard_port=self.args.tensorboard_port,
                         train_steps=self.args.train_steps)

        activity = train.TrainActivity(args, self.extra_argv, wdir=self.wdir('_temp_train'),
                                       log_file=self.log_fobj, delete_on_exit=self.delete_on_exit)
        activity.indentation = 4
        activity.run()

        output_nn_path = os.path.join(self.state.decoder_dir, '%s__%s' % (self.args.src_lang, self.args.tgt_lang))
        if os.path.exists(output_nn_path):
            shutil.rmtree(output_nn_path)

        os.rename(self.state.nn_path, output_nn_path)

    def run(self):
        self._logger.info('Training started: engine=%s, lang_pair=%s__%s' %
                          (self._engine.name, self.args.src_lang, self.args.tgt_lang))

        print('\n=========== TRAINING STARTED ===========\n')
        print('ENGINE:   %s' % self._engine.name)
        print('LANGUAGE: %s > %s' % (self.args.src_lang, self.args.tgt_lang))
        print(flush=True)

        # Check if all requirements are fulfilled before actual engine training
        try:
            self._check_constraints()
        except HWConstraintViolated as e:
            print('\033[91mWARNING\033[0m: %s\n' % e.cause)

        # Run actual training
        super().run()

        print('\n=========== TRAINING SUCCESS ===========\n')
        print('You can now start, stop or check the status of the server with command:')
        print('\t./mmt start|stop|status ' + ('' if self._engine.name == 'default' else '-e %s' % self._engine.name))
        print(flush=True)

    @staticmethod
    def _check_constraints():
        gb = 1024 * 1024 * 1024
        gpu_list = nvidia_smi.list_gpus()
        recommended_gpu_ram = 8 * gb

        if len(gpu_list) == 0:
            raise HWConstraintViolated(
                'No GPU for Neural engine training, the process will take very long time to complete.')

        for gpu in gpu_list:
            gpu_ram = nvidia_smi.get_ram(gpu)

            if gpu_ram < recommended_gpu_ram:
                raise HWConstraintViolated(
                    'The RAM of GPU %d is only %.fG. More than %.fG of RAM recommended for each GPU.' %
                    (gpu, round(float(gpu_ram) / gb), recommended_gpu_ram / gb))


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Create a new ModernMT engine from scratch', prog='mmt create')
    parser.add_argument('src_lang', metavar='SOURCE_LANGUAGE', help='the source language (ISO 639-1)')
    parser.add_argument('tgt_lang', metavar='TARGET_LANGUAGE', help='the target language (ISO 639-1)')
    parser.add_argument('input_path', metavar='INPUT', help='the path to the parallel corpora collection')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, "default" will be used if absent',
                        default='default')
    parser.add_argument('-d', '--debug', action='store_true', dest='debug', default=False,
                        help='prevents temporary files to be removed after execution')
    parser.add_argument('-y', '--yes', action='store_true', dest='force_delete', default=False,
                        help='if set, skip engine overwrite confirmation check')
    parser.add_argument('--resume', action='store_true', dest='resume', default=False,
                        help='resume an interrupted training, '
                             'it can be used also to resume a training after its completion')

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
    datagen_args.add_argument('--no-test', action='store_false', dest='test_set', default=True,
                              help='skip automatically extraction of a test set from the provided training corpora')

    train_args = parser.add_argument_group('Train arguments (note: you can use all fairseq cli options)')
    train_args.add_argument('--from-model', dest='init_model', default=None,
                            help='start the training from the specified model, '
                                 'the path must contain "model.pt" and "model.vcb" files')
    train_args.add_argument('-n', '--checkpoints-num', dest='num_checkpoints', type=int, default=10,
                            help='number of checkpoints to average (default is 10)')
    train_args.add_argument('--gpus', dest='gpus', nargs='+', type=int, default=None,
                            help='the list of GPUs available for training (default is all available GPUs)')
    train_args.add_argument('--tensorboard-port', dest='tensorboard_port', type=int, default=None,
                            help='if specified, starts a tensorboard instance during training on the given port')
    train_args.add_argument('--train-steps', dest='train_steps', type=int, default=None,
                            help='by default the training stops when the validation loss reaches a plateau, with '
                                 'this option instead, the training process stops after the specified amount of steps')

    args, extra_argv = parser.parse_known_args(argv)

    if args.vocabulary_path is not None and args.init_model is not None:
        raise CLIArgsException(parser, 'Cannot specify both options: "--vocabulary" and "--from-model"')

    if args.tensorboard_port is not None:
        train.verify_tensorboard_dependencies(parser)

    return args, train.parse_extra_argv(parser, extra_argv)


def confirm_or_die(engine_name):
    proceed = True

    while True:
        resp = input('An engine named "%s" already exists, '
                     'are you sure you want to overwrite it? [y/N] ' % engine_name)
        resp = resp.lower()
        if len(resp) == 0 or resp == 'n':
            proceed = False
            break
        elif resp == 'y':
            break

    if not proceed:
        print('Aborted')
        exit(2)


def main(argv=None):
    args, extra_argv = parse_args(argv)
    engine = Engine(args.engine)

    wdir = engine.get_tempdir('training')
    log_file = engine.get_logfile('training', ensure=True, append=True)

    if engine.exists():
        if not args.resume and not args.force_delete:
            confirm_or_die(args.engine)

        node = EngineNode(engine)
        node.stop()

    if not args.resume:
        if os.path.isdir(engine.path):
            shutil.rmtree(engine.path)
        os.makedirs(engine.path)

        shutil.rmtree(wdir, ignore_errors=True)
        os.makedirs(wdir)

        if os.path.isfile(log_file):
            os.remove(log_file)

    activity = CreateActivity(engine, args, extra_argv, wdir=wdir, log_file=log_file, delete_on_exit=not args.debug)
    activity.run()
