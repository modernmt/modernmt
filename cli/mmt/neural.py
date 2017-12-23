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

import onmt
import nmmt
from nmmt import NMTEngineTrainer, NMTEngine, SubwordTextProcessor, MMapDataset, Suggestion
from nmmt import torch_setup
from nmmt import torch_utils
import torch


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


class TranslationMemory:
    def __init__(self, model, source_lang, target_lang):
        self._model = model
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._java_mainclass = 'eu.modernmt.cli.TranslationMemoryMain'

    def create(self, corpora, log=None):
        if log is None:
            log = shell.DEVNULL

        source_paths = set()

        for corpus in corpora:
            source_paths.add(corpus.get_folder())

        shutil.rmtree(self._model, ignore_errors=True)
        fileutils.makedirs(self._model, exist_ok=True)

        args = ['-s', self._source_lang, '-t', self._target_lang, '-m', self._model, '-c']
        for source_path in source_paths:
            args.append(source_path)

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdout=log, stderr=log)


class NMTPreprocessor:
    def __init__(self, source_lang, target_lang, bpe_symbols, max_vocab_size, vocab_pruning_threshold):
        self._source_lang = source_lang
        self._target_lang = target_lang
        self._bpe_symbols = bpe_symbols
        self._max_vocab_size = max_vocab_size
        self._vocab_pruning_threshold = vocab_pruning_threshold

        self._logger = logging.getLogger('mmt.neural.NMTPreprocessor')
        self._ram_limit_mb = 1024

    def process(self, corpora, valid_corpora, output_path, checkpoint=None):
        bpe_output_path = os.path.join(output_path, 'vocab.bpe')
        voc_output_path = os.path.join(output_path, 'vocab.pt')

        if checkpoint is not None:
            existing_bpe_path = checkpoint + '.bpe'
            existing_dat_path = checkpoint + '.dat'
            existing_vcb_path = checkpoint + '.vcb'

            with _log_timed_action(self._logger, 'Loading BPE model from %s' % existing_bpe_path):
                shutil.copy(existing_bpe_path, bpe_output_path)
                bpe_encoder = SubwordTextProcessor.load_from_file(bpe_output_path)

            with _log_timed_action(self._logger, 'Loading vocabularies from %s' % existing_dat_path):
                checkpoint_vcb = torch.load(existing_vcb_path, map_location=lambda storage, loc: storage)
                src_vocab = checkpoint_vcb['src']
                trg_vocab = checkpoint_vcb['tgt']

        else:
            with _log_timed_action(self._logger, 'Creating BPE model'):
                vb_builder = SubwordTextProcessor.Builder(symbols=self._bpe_symbols,
                                                          max_vocabulary_size=self._max_vocab_size,
                                                          vocab_pruning_threshold=self._vocab_pruning_threshold)
                bpe_encoder = vb_builder.build([c.reader([self._source_lang, self._target_lang]) for c in corpora])
                bpe_encoder.save_to_file(bpe_output_path)

            with _log_timed_action(self._logger, 'Creating vocabularies'):
                src_vocab = onmt.Dict([onmt.Constants.PAD_WORD, onmt.Constants.UNK_WORD,
                                       onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD], lower=False)
                trg_vocab = onmt.Dict([onmt.Constants.PAD_WORD, onmt.Constants.UNK_WORD,
                                       onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD], lower=False)

                for word in bpe_encoder.get_source_terms():
                    src_vocab.add(word)
                for word in bpe_encoder.get_target_terms():
                    trg_vocab.add(word)

        torch.save({
            'src': src_vocab,
            'tgt': trg_vocab
        }, voc_output_path)

        with _log_timed_action(self._logger, 'Preparing training corpora'):
            train_output_path = os.path.join(output_path, 'train_dataset')
            self._prepare_corpora(corpora, bpe_encoder, src_vocab, trg_vocab, train_output_path)

        with _log_timed_action(self._logger, 'Preparing validation corpora'):
            valid_output_path = os.path.join(output_path, 'valid_dataset')
            self._prepare_corpora(valid_corpora, bpe_encoder, src_vocab, trg_vocab, valid_output_path)

    def _prepare_corpora(self, corpora, bpe_encoder, src_vocab, trg_vocab, output_path):
        count, added, ignored = 0, 0, 0

        builder = MMapDataset.Builder(output_path)

        for corpus in corpora:
            with corpus.reader([self._source_lang, self._target_lang]) as reader:
                for source, target in reader:
                    src_words = bpe_encoder.encode_line(source, is_source=True)
                    trg_words = bpe_encoder.encode_line(target, is_source=False)

                    if len(src_words) > 0 and len(trg_words) > 0:
                        source = src_vocab.convertToIdxList(src_words,
                                                            onmt.Constants.UNK_WORD)
                        target = trg_vocab.convertToIdxList(trg_words,
                                                            onmt.Constants.UNK_WORD,
                                                            onmt.Constants.BOS_WORD,
                                                            onmt.Constants.EOS_WORD)
                        builder.add([source], [target])
                        added += 1

                    else:
                        ignored += 1

                    count += 1
                    if count % 100000 == 0:
                        self._logger.info(' %d sentences prepared' % count)

        self._logger.info('Prepared %d sentences (%d ignored due to length == 0)' % (added, ignored))

        return builder.build(self._ram_limit_mb)


class NMTDecoder:
    def __init__(self, model, source_lang, target_lang):
        self._logger = logging.getLogger('mmt.neural.NMTDecoder')

        self.model = model
        self._source_lang = source_lang
        self._target_lang = target_lang

    def train(self, train_path, working_dir, training_opts, checkpoint_path=None, metadata_path=None):
        self._logger.info('Training started for data "%s"' % train_path)

        state = None
        state_file = os.path.join(working_dir, 'state.json')

        if os.path.isfile(state_file):
            state = NMTEngineTrainer.State.load_from_file(state_file)

        # Loading training data ----------------------------------------------------------------------------------------
        with _log_timed_action(self._logger, 'Loading training data from "%s"' % train_path):
            train_dataset_path = os.path.join(train_path, 'train_dataset')
            valid_dataset_path = os.path.join(train_path, 'valid_dataset')
            vocab_path = os.path.join(train_path, 'vocab.pt')

            train_dataset = MMapDataset.load(train_dataset_path)
            valid_dataset = MMapDataset.load(valid_dataset_path)
            vocab = torch.load(vocab_path)
            src_dict, tgt_dict = vocab['src'], vocab['tgt']

        # Creating trainer ---------------------------------------------------------------------------------------------
        if state is not None and state.checkpoint is not None:
            with _log_timed_action(self._logger, 'Resuming engine from step %d' % state.checkpoint['step']):
                engine = NMTEngine.load_from_checkpoint(state.checkpoint['file'])
        else:
            if checkpoint_path is not None:
                with _log_timed_action(self._logger, 'Loading engine from %s' % checkpoint_path):
                    engine = NMTEngine.load_from_checkpoint(checkpoint_path)
            else:
                metadata = None
                if metadata_path is not None:
                    metadata = NMTEngine.Metadata()
                    metadata.load_from_file(metadata_path)
                    self._logger.info('Neural engine metadata read from %s' % metadata_path)

                with _log_timed_action(self._logger, 'Reading BPE processor'):
                    bpe_model_path = os.path.join(train_path, 'vocab.bpe')
                    bpe_encoder = SubwordTextProcessor.load_from_file(bpe_model_path)

                with _log_timed_action(self._logger, 'Creating engine from scratch'):
                    engine = NMTEngine.new_instance(src_dict, tgt_dict, bpe_encoder, metadata=metadata)

        engine.running_state = NMTEngine.HOT

        trainer = NMTEngineTrainer(engine, state=state, options=training_opts)

        # Training model -----------------------------------------------------------------------------------------------
        self._logger.info('Vocabulary size. source = %d; target = %d' % (src_dict.size(), tgt_dict.size()))
        self._logger.info('Engine parameters: %d' % engine.count_parameters())
        self._logger.info('Engine metadata: %s' % str(engine.metadata))
        self._logger.info('Trainer options: %s' % str(trainer.opts))

        with _log_timed_action(self._logger, 'Train model'):
            state = trainer.train_model(train_dataset, valid_dataset=valid_dataset, save_path=working_dir)

        if state.empty():
            raise Exception('Training interrupted before first checkpoint could be saved')

    def merge_checkpoints(self, checkpoints_folder, limit=None):
        state = NMTEngineTrainer.State.load_from_file(os.path.join(checkpoints_folder, 'state.json'))

        # Create destination folder
        model_folder = os.path.abspath(os.path.join(self.model, os.path.pardir))
        if not os.path.isdir(model_folder):
            os.mkdir(model_folder)

        # Copy checkpoints files excluding .dat
        for f in glob.glob(state.checkpoint['file'] + '.*'):
            _, extension = os.path.splitext(f)

            if extension != '.dat':
                shutil.copy(f, self.model + extension)

        # Merging checkpoints
        checkpoints = [c['file'] + '.dat' for c in state.history]
        if limit is not None and len(checkpoints) > limit:
            checkpoints = checkpoints[:limit]

        with _log_timed_action(self._logger, 'Merge checkpoints %r to %s' % (checkpoints, model_folder)):
            NMTEngineTrainer.merge_checkpoints(checkpoints, self.model + '.dat')

        with open(os.path.join(model_folder, 'model.conf'), 'w') as model_map:
            filename = os.path.basename(self.model)
            model_map.write('[models]\n')
            model_map.write('%s__%s = %s\n' % (self._source_lang, self._target_lang, filename))


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

    def tune(self, validation_set, working_dir, lr_delta=0.1, max_epochs=10, gpus=None, log_file=None):
        torch_setup(gpus=gpus, random_seed=3435)

        logger = logging.getLogger('NeuralEngine.Tuning')

        if log_file is not None:
            fh = logging.FileHandler(log_file, mode='w')
            fh.setLevel(logging.DEBUG)
            fh.setFormatter(logging.Formatter('%(asctime)-15s [%(levelname)s] - %(message)s'))
            logger.addHandler(fh)
            logger.setLevel(logging.DEBUG)

        with _log_timed_action(logger, 'Loading decoder'):
            model_folder = os.path.abspath(os.path.join(self.decoder.model, os.path.pardir))
            decoder = nmmt.NMTDecoder(model_folder, gpus[0])
            logging.getLogger('nmmt.NMTEngine').disabled = True  # prevent translation log

        with _log_timed_action(logger, 'Creating reference file'):
            reference_file = os.path.join(working_dir, 'reference.out')
            with open(reference_file, 'wb') as stream:
                for _, target in validation_set:
                    stream.write(target)
                    stream.write('\n')

        # Tuning -------------------------------------------------------------------------------------------------------
        probes = []
        runs = int(1. / lr_delta)

        for run in range(1, runs + 1):
            learning_rate = round(run * lr_delta, 5)

            with _log_timed_action(logger, 'Tuning run %d/%d' % (run, runs)):
                output_file = os.path.join(working_dir, 'run%d.out' % run)
                bleu_score = self._tune_run(decoder, validation_set, learning_rate, max_epochs,
                                            output_file, reference_file)

            logger.info('Run %d completed: lr=%f, bleu=%f' % (run, learning_rate, bleu_score))
            probes.append((learning_rate, bleu_score))

        best_lr, best_bleu = sorted(probes, key=lambda x: x[1], reverse=True)[0]

        with _log_timed_action(logger, 'Updating engine with learning_rate %f (bleu=%f)' % (best_lr, best_bleu)):
            engine = decoder.get_engine(self.source_lang, self.target_lang)
            engine.metadata.tuning_max_learning_rate = best_lr
            engine.metadata.tuning_max_epochs = max_epochs
            engine.save(self.decoder.model, store_data=False, store_processor=False)

        return best_bleu / 100.

    def _tune_run(self, decoder, corpora, lr, epochs, output_file, reference_file):
        with open(output_file, 'wb') as output:
            for source, target in corpora:
                if lr == 0.:
                    suggestions = None
                else:
                    suggestions = [Suggestion(source, target, 1.)]
                translation = decoder.translate(self.source_lang, self.target_lang, source,
                                                suggestions=suggestions, tuning_epochs=epochs, tuning_learning_rate=lr)

                output.write(translation.text.encode('utf-8'))
                output.write('\n')

        command = ['perl', self._bleu_script, reference_file]
        with open(output_file) as input_stream:
            stdout, _ = shell.execute(command, stdin=input_stream)

        return float(stdout) * 100


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
