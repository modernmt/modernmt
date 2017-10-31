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
    def __init__(self, source_lang, target_lang, bpe_model_path, bpe_symbols, max_vocab_size):
        self._source_lang = source_lang
        self._target_lang = target_lang
        self._bpe_model_path = bpe_model_path
        self._bpe_symbols = bpe_symbols
        self._max_vocab_size = max_vocab_size

        self._logger = logging.getLogger('mmt.neural.NMTPreprocessor')
        self._ram_limit_mb = 1024

    def process(self, corpora, valid_corpora, output_path, checkpoint=None):
        if checkpoint is not None:
            existing_bpe_path = checkpoint + '.bpe'
            existing_dat_path = checkpoint + '.dat'

            with _log_timed_action(self._logger, 'Loading BPE model from %s' % existing_bpe_path):
                shutil.copy(existing_bpe_path, self._bpe_model_path)
                bpe_encoder = SubwordTextProcessor.load_from_file(self._bpe_model_path)

            with _log_timed_action(self._logger, 'Loading vocabularies from %s' % existing_dat_path):
                checkpoint_dat = torch.load(existing_dat_path, map_location=lambda storage, loc: storage)
                src_vocab = checkpoint_dat['dicts']['src']
                trg_vocab = checkpoint_dat['dicts']['tgt']

        else:
            with _log_timed_action(self._logger, 'Creating BPE model'):
                vb_builder = SubwordTextProcessor.Builder(symbols=self._bpe_symbols,
                                                          max_vocabulary_size=self._max_vocab_size)
                bpe_encoder = vb_builder.build([c.reader([self._source_lang, self._target_lang]) for c in corpora])
                bpe_encoder.save_to_file(self._bpe_model_path)

            with _log_timed_action(self._logger, 'Creating vocabularies'):
                src_vocab = onmt.Dict([onmt.Constants.PAD_WORD, onmt.Constants.UNK_WORD,
                                       onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD], lower=False)
                trg_vocab = onmt.Dict([onmt.Constants.PAD_WORD, onmt.Constants.UNK_WORD,
                                       onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD], lower=False)

                for word in bpe_encoder.get_source_terms():
                    src_vocab.add(word)
                for word in bpe_encoder.get_target_terms():
                    trg_vocab.add(word)


        # self._logger.info('Prepared src_vocab: "%s"' % repr(src_vocab))
        # self._logger.info('Prepared src_vocab.__dict__: "%s"' % repr(src_vocab.__dict__))
        # self._logger.info('Prepared trg_vocab: "%s"' % repr(trg_vocab))
        # self._logger.info('Prepared trg_vocab.__dict__: "%s"' % repr(trg_vocab.__dict__))
        torch.save({
            'src': src_vocab,
            'tgt': trg_vocab
        }, os.path.join(output_path, 'vocab.pt'))

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

    def getPath(self):
        return self._bpe_model_path

class NMTDecoder:
    def __init__(self, model, source_lang, target_lang):
        self._logger = logging.getLogger('mmt.neural.NMTDecoder')

        self.model = model
        self._source_lang = source_lang
        self._target_lang = target_lang

    def train(self, train_path, working_dir, checkpoint_path=None, metadata_path=None, training_opts=None, nmt_preprocessor_path=None):
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
            # self._logger.info('Loaded src_dict: "%s"' % repr(src_dict))
            # self._logger.info('Loaded src_dict.__dict__: "%s"' % repr(src_dict.__dict__))
            # self._logger.info('Loaded tgt_dict: "%s"' % repr(tgt_dict))
            # self._logger.info('Loaded tgt_dict.__dict__: "%s"' % repr(tgt_dict.__dict__))

        # Creating trainer ---------------------------------------------------------------------------------------------
        if state is not None and state.checkpoint is not None and state.nmt_preprocessor_path is not None:
            with _log_timed_action(self._logger, 'Resuming engine from step %d' % state.checkpoint['step']):
                os.symlink(state.nmt_preprocessor_path, state.checkpoint['file'] + '.bpe')
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
                    self._logger.info('Reading neural engine metadata from %s' % metadata_path)

                with _log_timed_action(self._logger, 'Creating engine from scratch'):
                    engine = NMTEngine.new_instance(src_dict, tgt_dict, processor=None, metadata=metadata)

        trainer_opts = NMTEngineTrainer.Options()
        trainer_opts.set(training_opts)

        trainer = NMTEngineTrainer(engine, state=state, options=trainer_opts, nmt_preprocessor_path=nmt_preprocessor_path)

        # Training model -----------------------------------------------------------------------------------------------
        self._logger.info('Vocabulary size. source = %d; target = %d' % (src_dict.size(), tgt_dict.size()))
        self._logger.info('Engine parameters: %d' % engine.count_parameters())
        self._logger.info('Engine metadata: %s' % str(engine.metadata))
        self._logger.info('Trainer options: %s' % str(trainer.opts))

        with _log_timed_action(self._logger, 'Train model'):
            state = trainer.train_model(train_dataset, valid_dataset=valid_dataset, save_path=working_dir)

        # Saving last checkpoint ---------------------------------------------------------------------------------------
        if state.empty():
            raise Exception('Training interrupted before first checkpoint could be saved')

        checkpoint = state.history[0]['file']
        self._logger.info('Copying checkpoint at %s' % checkpoint)

        with _log_timed_action(self._logger, 'Storing model'):
            model_folder = os.path.abspath(os.path.join(self.model, os.path.pardir))
            if not os.path.isdir(model_folder):
                os.mkdir(model_folder)

            for f in glob.glob(checkpoint + '.*'):
                _, extension = os.path.splitext(f)
                shutil.copy(f, self.model + extension)

            with open(os.path.join(model_folder, 'model.conf'), 'w') as model_map:
                filename = os.path.basename(self.model)
                model_map.write('model.%s__%s = %s\n' % (self._source_lang, self._target_lang, filename))


class NeuralEngine(Engine):
    def __init__(self, name, source_lang, target_lang, bpe_symbols=90000, max_vocab_size=None, gpus=None):
        Engine.__init__(self, name, source_lang, target_lang)
        torch_setup(gpus=gpus, random_seed=3435)

        self._bleu_script = os.path.join(PYOPT_DIR, 'mmt-bleu.perl')

        decoder_path = os.path.join(self.models_path, 'decoder')

        # Neural specific models
        model_name = 'model.%s__%s' % (source_lang, target_lang)

        memory_path = os.path.join(decoder_path, 'memory')
        bpe_model = os.path.join(decoder_path, model_name + '.bpe')
        decoder_model = os.path.join(decoder_path, model_name)

        self.memory = TranslationMemory(memory_path, self.source_lang, self.target_lang)
        self.nmt_preprocessor = NMTPreprocessor(self.source_lang, self.target_lang, bpe_model_path=bpe_model,
                                                bpe_symbols=bpe_symbols, max_vocab_size=max_vocab_size)
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
            engine.save(self.decoder.model, store_data=False)

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

                output.write(translation.encode('utf-8'))
                output.write('\n')

        command = ['perl', self._bleu_script, reference_file]
        with open(output_file) as input_stream:
            stdout, _ = shell.execute(command, stdin=input_stream)

        return float(stdout) * 100


class NeuralEngineBuilder(EngineBuilder):

    def __init__(self, name, source_lang, target_lang, roots, debug=False, steps=None, split_trainingset=True,
                 validation_corpora=None, checkpoint=None, metadata=None, bpe_symbols=90000, max_vocab_size=None,
                 max_training_words=None, gpus=None, training_opts=None):
        EngineBuilder.__init__(self,
                               NeuralEngine(name, source_lang, target_lang, bpe_symbols=bpe_symbols,
                                            max_vocab_size=max_vocab_size, gpus=gpus),
                               roots, debug, steps, split_trainingset, max_training_words)
        self._valid_corpora_path = validation_corpora if validation_corpora is not None \
            else os.path.join(self._engine.data_path, TrainingPreprocessor.DEV_FOLDER_NAME)
        self._checkpoint = checkpoint
        self._metadata = metadata
        self._training_opts = training_opts
        self._gpus = gpus

    def _build_schedule(self):
        return EngineBuilder._build_schedule(self) + \
               [self._build_memory, self._prepare_training_data, self._train_decoder]

    def _check_constraints(self):
        recommended_gpu_ram = 2 * self._GB

        # if the user explicitly said that no GPU must be used, return immediately
        if self._gpus == -1:
            return

        # else, get the list of GPUs to employ using torch utils.
        # (This takes into account the user's choice)
        gpus = torch_utils.torch_get_gpus()

        # AT THE MOMENT TRAINING IS MONOGPU AND WE ONLY USE THE FIRST AVAILABLE GPU FOR TRAINING.
        # SO JUST CHECK CONSTRAINTS FOR IT.
        # THIS MAY CHANGE IN THE FUTURE
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
        command = ["nvidia-smi", "--query-gpu=memory.total", "--format=csv,noheader,nounits", "--id=%s" % ",".join(str(id) for id in gpu_ids)]
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
    def _train_decoder(self, args, skip=False, delete_on_exit=False):
        working_dir = self._get_tempdir('onmt_model')

        if not skip:
            self._engine.decoder.train(args.onmt_training_path, working_dir,
                                       checkpoint_path=self._checkpoint, metadata_path=self._metadata, training_opts=self._training_opts,nmt_preprocessor_path=self._engine.nmt_preprocessor.getPath())

            if delete_on_exit:
                shutil.rmtree(working_dir, ignore_errors=True)
