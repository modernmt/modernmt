import glob
import logging
import os
import shutil
import sys
import time

from cli import mmt_javamain, LIB_DIR, PYOPT_DIR
from cli.libs import fileutils
from cli.libs import shell
from cli.mmt import BilingualCorpus
from cli.mmt.engine import Engine, EngineBuilder
from cli.mmt.processing import TrainingPreprocessor

sys.path.insert(0, os.path.abspath(os.path.join(LIB_DIR, 'pynmt')))

from nmmt import NMTEngineTrainer, NMTEngine, SubwordTextProcessor, MMapDataset, Suggestion
from nmmt import torch_setup
from nmmt import torch_utils


def _log_timed_action(logger, op, level=logging.INFO, log_start=True):
    class _logger:
        def __init__(self):
            self.logger = logger
            self.level = level
            self.op = op
            self.start_time = None
            self.log_start = log_start

        def __enter__(self):
            self.start_time = time.time()
            if self.log_start:
                self.logger.log(self.level, '%s... START' % self.op)

        def __exit__(self, exc_type, exc_val, exc_tb):
            self.logger.log(self.level, '%s END %.2fs' % (self.op, time.time() - self.start_time))

    return _logger()


class NeuralEngine(Engine):
    def __init__(self, name, source_lang, target_lang, bpe_symbols, max_vocab_size=None, vocab_pruning_threshold=None):
        Engine.__init__(self, name, source_lang, target_lang)

        self._bleu_script = os.path.join(PYOPT_DIR, 'mmt-bleu.perl')

        decoder_path = os.path.join(self.models_path, 'decoder')

        # Neural specific models
        model_name = 'model.%s__%s' % (source_lang, target_lang)

        memory_path = os.path.join(decoder_path, 'memory')
        decoder_model = os.path.join(decoder_path, model_name)

        self.memory = TranslationMemory(memory_path, self.source_lang, self.target_lang)
        self.nmt_preprocessor = NMTPreprocessor(self.source_lang, self.target_lang,
                                                bpe_symbols=bpe_symbols, max_vocab_size=max_vocab_size,
                                                vocab_pruning_threshold=vocab_pruning_threshold)
        self.decoder = NMTDecoder(decoder_model, self.source_lang, self.target_lang)

    def type(self):
        return 'neural'


class NeuralEngineBuilder(EngineBuilder):
    def __init__(self, name, source_lang, target_lang, roots, debug=False, steps=None, split_trainingset=True,
                 validation_corpora=None, checkpoint=None, metadata=None, max_training_words=None, gpus=None,
                 training_args=None):
        torch_setup(gpus=gpus, random_seed=3435)

        self._training_opts = NMTEngineTrainer.Options()
        if training_args is not None:
            self._training_opts.load_from_dict(training_args.__dict__)

        engine = NeuralEngine(name, source_lang, target_lang, bpe_symbols=self._training_opts.bpe_symbols,
                              max_vocab_size=self._training_opts.max_vocab_size,
                              vocab_pruning_threshold=self._training_opts.vocab_pruning_threshold)
        EngineBuilder.__init__(self, engine, roots, debug, steps, split_trainingset, max_training_words)

        self._valid_corpora_path = validation_corpora if validation_corpora is not None \
            else os.path.join(self._engine.data_path, TrainingPreprocessor.DEV_FOLDER_NAME)
        self._checkpoint = checkpoint
        self._metadata = metadata

    def _build_schedule(self):
        return EngineBuilder._build_schedule(self) + \
               [self._build_memory, self._prepare_training_data, self._train_decoder, self._merge_checkpoints]

    def _check_constraints(self):
        recommended_gpu_ram = 2 * self._GB

        # Get the list of GPUs to employ using torch utils (This takes into account the user's choice)
        gpus = torch_utils.torch_get_gpus()

        if gpus is None or len(gpus) == 0:
            raise EngineBuilder.HWConstraintViolated(
                'No GPU for Neural engine training, the process will take very long time to complete.')

        # AT THE MOMENT TRAINING IS MONOGPU AND WE ONLY USE THE FIRST AVAILABLE GPU FOR TRAINING.
        # SO JUST CHECK CONSTRAINTS FOR IT. THIS MAY CHANGE IN THE FUTURE
        gpus = [gpus[0]]

        gpus_ram = self._get_gpus_ram(gpus)

        for i in range(len(gpus_ram)):
            if gpus_ram[i] < recommended_gpu_ram:
                raise EngineBuilder.HWConstraintViolated(
                    'The RAM of GPU %d is only %.fG. More than %.fG of RAM recommended for each GPU.' %
                    (gpus[i], gpus_ram[i] / self._GB, recommended_gpu_ram / self._GB)
                )

    def _get_gpus_ram(self, gpu_ids):
        result = []
        command = ["nvidia-smi", "--query-gpu=memory.total", "--format=csv,noheader,nounits",
                   "--id=%s" % ",".join(str(i) for i in gpu_ids)]
        stdout, _ = shell.execute(command)
        for line in stdout.split("\n"):
            line = line.strip()
            if line:
                result.append(int(line) * self._MB)
        return result

    # ~~~~~~~~~~~~~~~~~~~~~ Training step functions ~~~~~~~~~~~~~~~~~~~~~

    @EngineBuilder.Step('Creating translation memory')
    def _build_memory(self, args, skip=False, log=None):
        if not skip:
            corpora = filter(None, [args.processed_bilingual_corpora, args.bilingual_corpora])[0]
            self._engine.memory.create(corpora, log=log)

    @EngineBuilder.Step('Preparing training data')
    def _prepare_training_data(self, args, skip=False, delete_on_exit=False):
        args.onmt_training_path = self._get_tempdir('onmt_training')

        if not skip:
            processed_valid_path = os.path.join(args.onmt_training_path, 'processed_valid')

            validation_corpora = BilingualCorpus.list(self._valid_corpora_path)
            validation_corpora, _ = self._engine.training_preprocessor.process(validation_corpora, processed_valid_path)

            corpora = filter(None, [args.processed_bilingual_corpora, args.bilingual_corpora])[0]

            self._engine.nmt_preprocessor.process(corpora, validation_corpora, args.onmt_training_path,
                                                  checkpoint=self._checkpoint)

            if delete_on_exit:
                shutil.rmtree(processed_valid_path, ignore_errors=True)

    @EngineBuilder.Step('Neural decoder training')
    def _train_decoder(self, args, skip=False):
        working_dir = self._get_tempdir('onmt_model')

        if not skip:
            self._engine.decoder.train(args.onmt_training_path, working_dir, self._training_opts,
                                       checkpoint_path=self._checkpoint, metadata_path=self._metadata)

    @EngineBuilder.Step('Saving neural model', optional=False)
    def _merge_checkpoints(self, _, skip=False):
        working_dir = self._get_tempdir('onmt_model')

        if not skip:
            self._engine.decoder.merge_checkpoints(working_dir, limit=self._training_opts.n_avg_checkpoints)
