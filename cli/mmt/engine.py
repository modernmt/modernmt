import inspect
import json
import logging
import os
import shutil
import time
from xml.dom import minidom

import glob

import cli
from cli import IllegalArgumentException, CorpusNotFoundInFolderException, mmt_javamain
from cli.libs import osutils, nvidia_smi
from cli.libs.osutils import ShellError
from cli.mmt import BilingualCorpus

__author__ = 'Davide Caroselli'


class TMCleaner:
    def __init__(self, source_lang, target_lang):
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._java_main = 'eu.modernmt.cli.CleaningPipelineMain'

    def clean(self, corpora, output_path, log=None):
        if log is None:
            log = osutils.DEVNULL

        args = ['-s', self._source_lang, '-t', self._target_lang,
                '--output', output_path, '--input']

        input_paths = set([corpus.get_folder() for corpus in corpora])

        for root in input_paths:
            args.append(root)

        extended_heap_mb = int(osutils.mem_size() * 90 / 100)

        command = mmt_javamain(self._java_main, args=args, max_heap_mb=extended_heap_mb)
        osutils.shell_exec(command, stdout=log, stderr=log)

        return BilingualCorpus.list(self._source_lang, self._target_lang, output_path)


class TrainingPreprocessor:
    def __init__(self, source_lang, target_lang):
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._java_main = 'eu.modernmt.cli.TrainingPipelineMain'

    def process(self, corpora, output_path, test_data_path=None, dev_data_path=None, log=None):
        if log is None:
            log = osutils.DEVNULL

        args = ['-s', self._source_lang, '-t', self._target_lang, '--output', output_path, '--input']

        for root in set([corpus.get_folder() for corpus in corpora]):
            args.append(root)

        if dev_data_path is not None:
            args.append('--dev')
            args.append(dev_data_path)
        if test_data_path is not None:
            args.append('--test')
            args.append(test_data_path)

        command = mmt_javamain(self._java_main, args)
        osutils.shell_exec(command, stdout=log, stderr=log)

        return BilingualCorpus.list(self._source_lang, self._target_lang, output_path)


class FastAlign:
    def __init__(self, model, source_lang, target_lang):
        # FastAlign only supports base languages, without regions
        self._model = os.path.join(model, '%s__%s.mdl' % (source_lang.split('-')[0], target_lang.split('-')[0]))
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._build_bin = os.path.join(cli.BIN_DIR, 'fa_build')
        self._align_bin = os.path.join(cli.BIN_DIR, 'fa_align')
        self._export_bin = os.path.join(cli.BIN_DIR, 'fa_export')

    def build(self, corpora, log=None):
        if log is None:
            log = osutils.DEVNULL

        shutil.rmtree(self._model, ignore_errors=True)
        osutils.makedirs(self._model, exist_ok=True)

        source_path = set([corpus.get_folder() for corpus in corpora])
        assert len(source_path) == 1
        source_path = source_path.pop()

        command = [self._build_bin, '-s', self._source_lang, '-t', self._target_lang, '-i', source_path,
                   '-m', self._model, '-I', '4']
        osutils.shell_exec(command, stdout=log, stderr=log)


class NeuralDecoder(object):
    def __init__(self, source_lang, target_lang, model, gpus):
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.model = model
        self._gpus = gpus
        self._t2t_dir = os.path.join(cli.LIB_DIR, 't2t')

    def _get_env(self, train_path=None, eval_path=None, bpe=None):
        gpus = ','.join([str(x) for x in self._gpus]) if self._gpus is not None and len(self._gpus) > 0 else '-1'

        env = os.environ.copy()
        env.update({
            'MMT_PROBLEM_SOURCE_LANG': self.source_lang,
            'MMT_PROBLEM_TARGET_LANG': self.target_lang,
            'MMT_PROBLEM_BPE': str(bpe),
            'MMT_PROBLEM_TRAIN_PATH': str(train_path),
            'MMT_PROBLEM_DEV_PATH': str(eval_path),

            'CUDA_DEVICE_ORDER': 'PCI_BUS_ID',
            'CUDA_VISIBLE_DEVICES': gpus
        })

        return env

    @staticmethod
    def _get_common_root(corpora):
        roots = set([corpus.get_folder() for corpus in corpora])
        if len(roots) > 1:
            raise ValueError('Corpora must be contained in the same folder: ' + str(corpora))
        return roots.pop()

    def prepare_data(self, train_corpora, eval_corpora, output_path, log=None, bpe_symbols=2 ** 15, fromCkpt=None):
        if log is None:
            log = osutils.DEVNULL

        data_dir = os.path.join(output_path, 'data')
        tmp_dir = os.path.join(output_path, 'tmp')

        train_path = self._get_common_root(train_corpora)
        eval_path = self._get_common_root(eval_corpora)

        shutil.rmtree(data_dir, ignore_errors=True)
        osutils.makedirs(data_dir)

        # if an existing checkpoint is loaded for starting the training (i.e fromCkpt!=None)
        # copy the subtoken vocabulary associated to the existing checkpoint into the right location,
        # so that the subtoken vocabulary is not re-created from the new training data,
        # and so that it is only exploited to bpe-fy the new data
        # it assumes that the vocabulary is called "model.vcb" and is located in the same directory of the checkpoint
        if fromCkpt is not None:
            shutil.copyfile(os.path.join(fromCkpt, 'model.vcb'), os.path.join(data_dir, 'model.vcb'))

        if not os.path.isdir(tmp_dir):
            osutils.makedirs(tmp_dir)

        env = self._get_env(train_path, eval_path, bpe=bpe_symbols)
        command = ['t2t-datagen', '--t2t_usr_dir', self._t2t_dir,
                   '--data_dir=%s' % data_dir, '--tmp_dir=%s' % tmp_dir, '--problem=translate_mmt']

        osutils.shell_exec(command, stdout=log, stderr=log, env=env)

    def train_model(self, train_dir, output_dir, batch_size=1024, n_train_steps=None, n_eval_steps=1000,
                    hparams='transformer_base', log=None, fromCkpt=None):
        if log is None:
            log = osutils.DEVNULL

        if not os.path.isdir(output_dir):
            osutils.makedirs(output_dir)

        # if an existing checkpoint is loaded for starting the training (i.e fromCkpt != None)
        # copy the checkpoint files into the right location
        if fromCkpt is not None:
            self._copy_and_fix_model(fromCkpt, output_dir, gpus=self._gpus)

        data_dir = os.path.join(train_dir, 'data')

        src_model_vocab = os.path.join(data_dir, 'model.vcb')
        tgt_model_vocab = os.path.join(output_dir, 'model.vcb')

        if not os.path.isfile(tgt_model_vocab):
            os.symlink(src_model_vocab, tgt_model_vocab)

        env = self._get_env()
        hparams_p = 'batch_size=%d' % batch_size
        command = ['t2t-trainer', '--t2t_usr_dir', self._t2t_dir,
                   '--data_dir=%s' % data_dir,
                   '--problem=translate_mmt',
                   '--model=transformer',
                   '--hparams_set=%s' % hparams,
                   '--output_dir=%s' % output_dir,
                   '--local_eval_frequency=%d' % n_eval_steps,
                   '--train_steps=%d' % (n_train_steps if n_train_steps is not None else 100000000),
                   '--worker_gpu=%d' % len(self._gpus),
                   '--hparams', hparams_p]

        process = osutils.shell_exec(command, stdout=log, stderr=log, env=env, background=True)

        try:
            return_code = process.wait()
            if return_code != 0:
                raise ShellError(' '.join(command), return_code, None)
        except KeyboardInterrupt:
            process.kill()

    def finalize_model(self, train_dir, model_dir, n_checkpoints=None, gpus=None):
        import warnings

        with warnings.catch_warnings():
            warnings.filterwarnings('ignore', category=FutureWarning)

            import tensorflow as tf
            import numpy as np
            import six

            os.environ['TF_CPP_MIN_LOG_LEVEL'] = '9999'

        data_dir = os.path.join(train_dir, 'data')

        logger = logging.getLogger('NeuralDecoder')

        # Get checkpoints list
        checkpoint_paths = {}

        # Used to select only one (the last) of the two checkpoints created at each validation;
        # it assumes that the validation is computed with a frequency higher or equal to 100.
        mask_value = 100

        for checkpoint_path in tf.train.get_checkpoint_state(model_dir).all_model_checkpoint_paths:
            steps = int(checkpoint_path[checkpoint_path.rfind('-') + 1:])
            masked_steps = steps - (steps % mask_value)

            if masked_steps in checkpoint_paths:
                e_steps, e_checkpoint_path = checkpoint_paths[masked_steps]
                if steps > e_steps:
                    checkpoint_paths[masked_steps] = steps, checkpoint_path
            else:
                checkpoint_paths[masked_steps] = steps, checkpoint_path

        checkpoint_pairs = [(steps, path) for steps, path in checkpoint_paths.values()]
        if n_checkpoints is not None and len(checkpoint_pairs) > n_checkpoints:
            checkpoint_pairs = checkpoint_pairs[:n_checkpoints]

        global_steps = max([(steps - (steps % 100)) for steps, _ in checkpoint_pairs])
        checkpoints = [path for (_, path) in checkpoint_pairs]

        logger.info('(finalize_model) Averaging checkpoints: %s' % str(checkpoints))

        # Read variables from all checkpoints and average them.
        var_list = tf.contrib.framework.list_variables(checkpoints[0])
        var_dtypes = {}
        var_values = {name: np.zeros(shape) for name, shape in var_list if not name.startswith('global_step')}

        for checkpoint in checkpoints:
            reader = tf.contrib.framework.load_checkpoint(checkpoint)

            for name in var_values:
                tensor = reader.get_tensor(name)
                var_dtypes[name] = tensor.dtype
                var_values[name] += tensor

        for name in var_values:  # Average.
            var_values[name] /= len(checkpoints)

        if gpus is not None:
            gpu = gpus[0]
            device = '/device:GPU:%s' % gpu
        else:
            gpu = None
            device = '/cpu:0'

        logger.info('(finalize_model) Running on device: %s' % device)

        with tf.device(device):
            tf_vars = [tf.get_variable(n, shape=var_values[n].shape, dtype=var_dtypes[n]) for n in var_values]
            placeholders = [tf.placeholder(v.dtype, shape=v.shape) for v in tf_vars]
            assign_ops = [tf.assign(v, p) for (v, p) in zip(tf_vars, placeholders)]
            tf.Variable(0, name='global_step', trainable=False, dtype=tf.int64)

        saver = tf.train.Saver(tf.global_variables(), save_relative_paths=True)

        # Build a model consisting only of variables, set them to the average values.
        model_output_path = os.path.join(self.model, '%s__%s' % (self.source_lang, self.target_lang))

        if not os.path.isdir(model_output_path):
            os.makedirs(model_output_path)

        session_config = tf.ConfigProto(allow_soft_placement=True)
        session_config.gpu_options.allow_growth = True
        if gpu is not None:
            session_config.gpu_options.force_gpu_compatible = True
            session_config.gpu_options.visible_device_list = str(gpu)

        sess = tf.Session(config=session_config)
        sess.run(tf.initialize_all_variables())
        for p, assign_op, (name, value) in zip(placeholders, assign_ops, six.iteritems(var_values)):
            sess.run(assign_op, {p: value})

        # Use the built saver to save the averaged checkpoint.
        saver.save(sess, os.path.join(model_output_path, 'model-avg'), global_step=global_steps)

        # Copy auxiliary files
        shutil.copyfile(os.path.join(model_dir, 'hparams.json'), os.path.join(model_output_path, 'hparams.json'))
        shutil.copyfile(os.path.join(data_dir, 'model.vcb'), os.path.join(model_output_path, 'model.vcb'))

        # Write config file
        with open(os.path.join(self.model, 'model.conf'), 'w') as model_conf:
            model_conf.write('[models]\n')
            model_conf.write('%s__%s = %s__%s/\n' %
                             (self.source_lang, self.target_lang, self.source_lang, self.target_lang))

    def _copy_and_fix_model(self, fromCkpt, output_dir, gpus=None):
        import warnings

        with warnings.catch_warnings():
            warnings.filterwarnings('ignore', category=FutureWarning)

            import tensorflow as tf
            import numpy as np
            import six

            os.environ['TF_CPP_MIN_LOG_LEVEL'] = '9999'

        # get the checkpoint to load
        wildcard = fromCkpt + "/model.ckpt*.meta"
        for file in glob.glob(wildcard):
            if os.path.isfile(file):
                checkpoint = os.path.splitext(file)[0]

        # Read variables from the checkpoint.
        var_list = tf.contrib.framework.list_variables(checkpoint)
        var_dtypes = {}
        var_values = {name: np.zeros(shape) for name, shape in var_list if not name.startswith('global_step')}

        reader = tf.contrib.framework.load_checkpoint(checkpoint)
        for name in var_values:
            tensor = reader.get_tensor(name)
            var_dtypes[name] = tensor.dtype
            var_values[name] = tensor

        if gpus is not None:
            gpu = gpus[0]
            device = '/device:GPU:%s' % gpu
        else:
            gpu = None
            device = '/cpu:0'

        with tf.device(device):
            tf_vars = [tf.get_variable(n, shape=var_values[n].shape, dtype=var_dtypes[n]) for n in var_values]
            placeholders = [tf.placeholder(v.dtype, shape=v.shape) for v in tf_vars]
            assign_ops = [tf.assign(v, p) for (v, p) in zip(tf_vars, placeholders)]
            tf.Variable(0, name='global_step', trainable=False, dtype=tf.int64)

        saver = tf.train.Saver(tf.global_variables(), save_relative_paths=True)

        session_config = tf.ConfigProto(allow_soft_placement=True)
        session_config.gpu_options.allow_growth = True
        if gpu is not None:
            session_config.gpu_options.force_gpu_compatible = True
            session_config.gpu_options.visible_device_list = str(gpu)

        sess = tf.Session(config=session_config)
        sess.run(tf.initialize_all_variables())
        for p, assign_op, (name, value) in zip(placeholders, assign_ops, six.iteritems(var_values)):
            sess.run(assign_op, {p: value})

        # Use the built saver to save the checkpoint.
        saver.save(sess, os.path.join(output_dir, 'model.ckpt'), global_step=0)

        # Copy auxiliary files
        shutil.copyfile(os.path.join(fromCkpt, 'hparams.json'), os.path.join(output_dir, 'hparams.json'))


class Engine(object):
    @staticmethod
    def _get_config_path(name):
        return os.path.join(cli.ENGINES_DIR, name, 'engine.xconf')

    @staticmethod
    def list():
        return sorted([name for name in os.listdir(cli.ENGINES_DIR) if os.path.isfile(Engine._get_config_path(name))])

    @staticmethod
    def load(name):
        if os.sep in name:
            raise IllegalArgumentException('Invalid engine name: "%s"' % name)

        config_path = Engine._get_config_path(name)

        if not os.path.isfile(config_path):
            raise IllegalArgumentException("Engine '%s' not found" % name)

        # parse the source language and target language from the configuration file
        def _get_child(root, child_name):
            elements = root.getElementsByTagName(child_name)
            return elements[0] if len(elements) > 0 else None

        languages = []

        config_root = minidom.parse(config_path).documentElement
        engine_el = _get_child(config_root, 'engine')
        lang_el = _get_child(engine_el, 'languages')

        if lang_el is not None:
            for pair_el in lang_el.getElementsByTagName('pair'):
                source_lang = pair_el.getAttribute('source')
                target_lang = pair_el.getAttribute('target')
                languages.append((source_lang, target_lang))
        else:
            source_lang = engine_el.getAttribute('source-language')
            target_lang = engine_el.getAttribute('target-language')
            languages.append((source_lang, target_lang))

        return Engine(name, languages)

    def __init__(self, name, languages):
        # properties
        self.name = name if name is not None else 'default'
        self.languages = languages

        # base paths
        self.config_path = self._get_config_path(self.name)
        self.path = os.path.join(cli.ENGINES_DIR, name)
        self.test_data_path = os.path.join(self.path, 'test_data')
        self.models_path = os.path.join(self.path, 'models')
        self.runtime_path = os.path.join(cli.RUNTIME_DIR, self.name)
        self.logs_path = os.path.join(self.runtime_path, 'logs')
        self.temp_path = os.path.join(self.runtime_path, 'tmp')

    def exists(self):
        return os.path.isfile(self.config_path)

    def get_logfile(self, name, ensure=True, append=False):
        if ensure and not os.path.isdir(self.logs_path):
            osutils.makedirs(self.logs_path, exist_ok=True)

        logfile = os.path.join(self.logs_path, name + '.log')

        if not append and ensure and os.path.isfile(logfile):
            os.remove(logfile)

        return logfile

    def get_tempdir(self, name, ensure=True):
        if ensure and not os.path.isdir(self.temp_path):
            osutils.makedirs(self.temp_path, exist_ok=True)

        folder = os.path.join(self.temp_path, name)

        if ensure:
            shutil.rmtree(folder, ignore_errors=True)
            os.makedirs(folder)

        return folder

    def get_tempfile(self, name, ensure=True):
        if ensure and not os.path.isdir(self.temp_path):
            osutils.makedirs(self.temp_path, exist_ok=True)
        return os.path.join(self.temp_path, name)

    def clear_tempdir(self, subdir=None):
        path = os.path.join(self.temp_path, subdir) if subdir is not None else self.temp_path
        shutil.rmtree(path, ignore_errors=True)


class EngineBuilder:
    _MB = (1024 * 1024)
    _GB = (1024 * 1024 * 1024)

    class Step:
        class Instance:
            def __init__(self, f, seq_num, name, optional, hidden):
                self.id = f.__name__.strip('_')
                self.name = name

                self._optional = optional
                self._hidden = hidden
                self._f = f
                self._seq_num = seq_num

            def is_optional(self):
                return self._optional

            def is_hidden(self):
                return self._hidden

            def pos(self):
                return self._seq_num

            def __call__(self, *args, **kwargs):
                names, _, _, _ = inspect.getargspec(self._f)

                if 'delete_on_exit' not in names:
                    del kwargs['delete_on_exit']
                if 'log' not in names:
                    del kwargs['log']
                if 'skip' not in names:
                    del kwargs['skip']

                self._f(*args, **kwargs)

        def __init__(self, seq_num, name, optional=True, hidden=False):
            self._seq_num = seq_num
            self._name = name
            self._optional = optional
            self._hidden = hidden

        def __call__(self, *_args, **_kwargs):
            return self.Instance(_args[0], self._seq_num, self._name, self._optional, self._hidden)

    class __Args(object):
        def __init__(self):
            pass

        def __getattr__(self, item):
            return self.__dict__[item] if item in self.__dict__ else None

        def __setattr__(self, key, value):
            self.__dict__[key] = value

    class __Schedule:
        def __init__(self, plan, filtered_steps=None):
            self._plan = plan
            self._passed_steps = []

            all_steps = self.all_steps()

            if filtered_steps is not None:
                self._scheduled_steps = filtered_steps

                unknown_steps = [step for step in self._scheduled_steps if step not in all_steps]
                if len(unknown_steps) > 0:
                    raise IllegalArgumentException('Unknown training steps: ' + str(unknown_steps))
            else:
                self._scheduled_steps = all_steps

        def __len__(self):
            return len(self._scheduled_steps)

        def __iter__(self):
            class __Inner:
                def __init__(self, plan):
                    self._plan = plan
                    self._idx = 0

                def next(self):
                    if self._idx < len(self._plan):
                        self._idx += 1
                        return self._plan[self._idx - 1]
                    else:
                        raise StopIteration

            return __Inner([el for el in self._plan if el.id in self._scheduled_steps or not el.is_optional()])

        def visible_steps(self):
            return [x.id for x in self._plan if x.id in self._scheduled_steps and not x.is_hidden()]

        def all_steps(self):
            return [e.id for e in self._plan]

        def store(self, path):
            with open(path, 'w') as json_file:
                json.dump(self._passed_steps, json_file)

        def load(self, path):
            try:
                with open(path) as json_file:
                    self._passed_steps = json.load(json_file)
            except IOError:
                self._passed_steps = []

        def step_completed(self, step):
            self._passed_steps.append(step)

        def is_completed(self, step):
            return step in self._passed_steps

    @staticmethod
    def all_visible_steps():
        entries = [(name, value) for name, value in inspect.getmembers(EngineBuilder)
                   if isinstance(value, EngineBuilder.Step.Instance) and not value.is_hidden()]
        return [name[1:] for name, value in sorted(entries, key=lambda x: x[1].pos())]

    @staticmethod
    def _all_steps_methods():
        entries = [(name, value) for name, value in inspect.getmembers(EngineBuilder)
                   if isinstance(value, EngineBuilder.Step.Instance)]
        return [value for name, value in sorted(entries, key=lambda x: x[1].pos())]

    def __init__(self, engine_name, source_lang, target_lang, roots, gpus,
                 debug=False, steps=None, split_train=True, validation_path=None, batch_size=1024,
                 n_train_steps=None, n_eval_steps=1000, hparams='transformer_base', bpe_symbols=2 ** 15, fromCkpt=None):
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.roots = roots

        self._gpus = gpus
        self._engine = Engine(engine_name, [(source_lang, target_lang)])
        self._delete_on_exit = not debug
        self._split_train = split_train
        self._validation_path = validation_path
        self._bpe_symbols = bpe_symbols
        self._batch_size = batch_size
        self._n_train_steps = n_train_steps
        self._n_eval_steps = n_eval_steps
        self._hparams = hparams
        self._fromCkpt = fromCkpt

        self._temp_dir = None

        self._schedule = EngineBuilder.__Schedule(self._all_steps_methods(), steps)
        self._cleaner = TMCleaner(self.source_lang, self.target_lang)
        self._training_preprocessor = TrainingPreprocessor(self.source_lang, self.target_lang)
        self._aligner = FastAlign(os.path.join(self._engine.models_path, 'aligner'), self.source_lang, self.target_lang)
        self._decoder = NeuralDecoder(self.source_lang, self.target_lang,
                                      os.path.join(self._engine.models_path, 'decoder'), gpus)

    def _get_tempdir(self, name, delete_if_exists=False):
        path = os.path.join(self._temp_dir, name)
        if delete_if_exists:
            shutil.rmtree(path, ignore_errors=True)
        if not os.path.isdir(path):
            osutils.makedirs(path, exist_ok=True)
        return path

    # ~~~~~~~~~~~~~~~~~~~~~~~~~~ Engine creation management ~~~~~~~~~~~~~~~~~~~~~~~~~~

    def build(self):
        self._build(resume=False)

    def resume(self):
        self._build(resume=True)

    def _build(self, resume):
        self._temp_dir = self._engine.get_tempdir('training', ensure=(not resume))

        checkpoint_path = os.path.join(self._temp_dir, 'checkpoint.json')
        if resume:
            self._schedule.load(checkpoint_path)
        else:
            self._schedule.store(checkpoint_path)

        corpora = BilingualCorpus.list(self.source_lang, self.target_lang, self.roots)

        if len(corpora) == 0:
            raise CorpusNotFoundInFolderException('Could not find %s > %s corpora in path %s' %
                                                  (self.source_lang, self.target_lang, ', '.join(self.roots)))

        # if no old engines (i.e. engine folders) can be found, create a new one from scratch
        # if we are not trying to resume an old one, create from scratch anyway
        if not os.path.isdir(self._engine.path) or not resume:
            shutil.rmtree(self._engine.path, ignore_errors=True)
            os.makedirs(self._engine.path)

        # Create a new logger for the building activities,
        log_file = self._engine.get_logfile('training', append=resume)
        log_stream = open(log_file, 'ab' if resume else 'wb')
        logging.basicConfig(format='%(asctime)-15s [%(levelname)s] - %(message)s',
                            level=logging.DEBUG, stream=log_stream)
        logger = logging.getLogger('EngineBuilder')

        # Start the engine building (training) phases
        steps_count = len(self._schedule.visible_steps())
        log_line_len = 70

        try:
            logger.log(logging.INFO, 'Training started: engine=%s, corpora=%d, lang_pair=%s-%s' %
                       (self._engine.name, len(corpora), self.source_lang, self.target_lang))

            print '\n=========== TRAINING STARTED ===========\n'
            print 'ENGINE:  %s' % self._engine.name
            print 'CORPORA: %d corpora' % len(corpora)
            print 'LANGS:   %s > %s' % (self.source_lang, self.target_lang)
            print

            # Check if all requirements are fulfilled before actual engine training
            try:
                self._check_constraints()
            except EngineBuilder.HWConstraintViolated as e:
                print '\033[91mWARNING\033[0m: %s\n' % e.cause

            args = EngineBuilder.__Args()
            args.corpora = corpora

            # ~~~~~~~~~~~~~~~~~~~~~ RUN ALL STEPS ~~~~~~~~~~~~~~~~~~~~~
            # Note: if resume is true, a step is only run if it was not in the previous attempt

            step_index = 1

            for method in self._schedule:
                if not method.is_hidden():
                    print ('INFO: (%d of %d) %s... ' % (step_index, steps_count, method.name)).ljust(log_line_len),

                skip = self._schedule.is_completed(method.id)
                self._step_start_time = time.time()

                logger.log(logging.INFO, 'Training step "%s" (%d/%d) started' %
                           (method.id, step_index, len(self._schedule)))

                start_time = time.time()
                method(self, args, skip=skip, log=log_stream, delete_on_exit=self._delete_on_exit)
                elapsed_time_str = self._pretty_print_time(time.time() - start_time)

                if not method.is_hidden():
                    step_index += 1
                    print 'DONE (in %s)' % elapsed_time_str

                logger.log(logging.INFO, 'Training step "%s" completed in %s' % (method.id, elapsed_time_str))

                self._schedule.step_completed(method.id)
                self._schedule.store(checkpoint_path)

            print '\n=========== TRAINING SUCCESS ===========\n'
            print 'You can now start, stop or check the status of the server with command:'
            print '\t./mmt start|stop|status ' + ('' if self._engine.name == 'default' else '-e %s' % self._engine.name)
            print

            if self._delete_on_exit:
                self._engine.clear_tempdir('training')
        except Exception:
            logger.exception('Unexpected exception')
            raise
        finally:
            log_stream.close()

    @staticmethod
    def _pretty_print_time(elapsed):
        elapsed = int(elapsed)
        parts = []

        if elapsed > 86400:  # days
            d = int(elapsed / 86400)
            elapsed -= d * 86400
            parts.append('%dd' % d)
        if elapsed > 3600:  # hours
            h = int(elapsed / 3600)
            elapsed -= h * 3600
            parts.append('%dh' % h)
        if elapsed > 60:  # minutes
            m = int(elapsed / 60)
            elapsed -= m * 60
            parts.append('%dm' % m)
        parts.append('%ds' % elapsed)

        return ' '.join(parts)

    class HWConstraintViolated(Exception):
        def __init__(self, cause):
            self.cause = cause

    def _check_constraints(self):
        if len(self._gpus) == 0:
            raise EngineBuilder.HWConstraintViolated(
                'No GPU for Neural engine training, the process will take very long time to complete.')

        recommended_gpu_ram = 8 * self._GB

        for gpu in self._gpus:
            gpu_ram = nvidia_smi.get_ram(gpu)

            if gpu_ram < recommended_gpu_ram:
                raise EngineBuilder.HWConstraintViolated(
                    'The RAM of GPU %d is only %.fG. More than %.fG of RAM recommended for each GPU.' %
                    (gpu, round(float(gpu_ram) / self._GB), recommended_gpu_ram / self._GB))

    # ~~~~~~~~~~~~~~~~~~~~~ Training step functions ~~~~~~~~~~~~~~~~~~~~~

    @Step(1, 'Corpora cleaning')
    def _clean_tms(self, args, skip=False, log=None):
        folder = self._get_tempdir('clean_corpora')

        if skip:
            args.corpora = BilingualCorpus.list(self.source_lang, self.target_lang, folder)
        else:
            args.corpora = self._cleaner.clean(args.corpora, folder, log=log)

    @Step(2, 'Corpora pre-processing')
    def _preprocess(self, args, skip=False, log=None):
        preprocessed_folder = self._get_tempdir('preprocessed_corpora')
        train_folder = os.path.join(preprocessed_folder, 'train')
        valid_folder = os.path.join(preprocessed_folder, 'validation')
        raw_valid_folder = os.path.join(preprocessed_folder, 'extracted_validation')

        if skip:
            args.processed_train_corpora = BilingualCorpus.list(self.source_lang, self.target_lang, train_folder)
            args.processed_valid_corpora = BilingualCorpus.list(self.source_lang, self.target_lang, valid_folder)
        else:
            if not args.corpora:
                raise CorpusNotFoundInFolderException('Could not find any valid %s > %s segments in your input.' %
                                                      (self.source_lang, self.target_lang))

            test_data_path = self._engine.test_data_path if self._split_train else None
            dev_data_path = raw_valid_folder if self._split_train else None
            args.processed_train_corpora = self._training_preprocessor.process(args.corpora, train_folder, log=log,
                                                                               test_data_path=test_data_path,
                                                                               dev_data_path=dev_data_path)
            valid_corpora = BilingualCorpus.list(self.source_lang, self.target_lang,
                                                 dev_data_path or self._validation_path)

            if not valid_corpora:
                raise CorpusNotFoundInFolderException('Could not find any valid %s > %s segments for validation.' %
                                                      (self.source_lang, self.target_lang))

            args.processed_valid_corpora = self._training_preprocessor.process(valid_corpora, valid_folder, log=log)

    @Step(3, 'Aligner training')
    def _train_aligner(self, args, skip=False, log=None):
        if not skip:
            corpora = filter(None, [args.processed_train_corpora, args.corpora])[0]
            self._aligner.build(corpora, log=log)

    @Step(4, 'Writing config', optional=False, hidden=True)
    def _write_config(self, _):
        xml_template = \
            '<node xsi:schemaLocation="http://www.modernmt.eu/schema/config mmt-config-1.0.xsd"\n' \
            '      xmlns="http://www.modernmt.eu/schema/config"\n' \
            '      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n' \
            '   <engine source-language="%s" target-language="%s" />\n' \
            '</node>'

        with open(self._engine.config_path, 'wb') as out:
            out.write(xml_template % (self.source_lang, self.target_lang))

    @Step(5, 'Preparing data')
    def _prepare_data(self, args, skip=False, log=None):
        if not skip:
            args.prepared_data_path = self._get_tempdir('neural_train_data')
            train_corpora = filter(None, [args.processed_train_corpora, args.corpora])[0]
            eval_corpora = args.processed_valid_corpora or BilingualCorpus.list(self.source_lang, self.target_lang,
                                                                                self._validation_path)
            self._decoder.prepare_data(train_corpora, eval_corpora, args.prepared_data_path,
                                       log=log, bpe_symbols=self._bpe_symbols, fromCkpt=self._fromCkpt)

    @Step(6, 'Training model')
    def _train_model(self, args, skip=False, log=None):
        if not skip:
            if args.prepared_data_path == None:
                args.prepared_data_path = self._get_tempdir('neural_train_data')
            args.train_model_path = self._get_tempdir('neural_model')
            self._decoder.train_model(args.prepared_data_path, args.train_model_path, log=log,
                                      batch_size=self._batch_size, hparams=self._hparams,
                                      n_train_steps=self._n_train_steps, n_eval_steps=self._n_eval_steps, fromCkpt=self._fromCkpt)

    @Step(7, 'Pack model')
    def _pack_model(self, args, skip=False):
        if not skip:
            if args.prepared_data_path == None:
                args.prepared_data_path = self._get_tempdir('neural_train_data')
            if args.train_model_path == None:
                args.train_model_path = self._get_tempdir('neural_model')

            self._decoder.finalize_model(args.prepared_data_path, args.train_model_path, gpus=self._gpus)
