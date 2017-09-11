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
from nmmt import NMTEngineTrainer, SubwordTextProcessor, TrainingInterrupt, ShardedDataset, Suggestion, \
    torch_is_using_cuda, torch_setup
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


class BPEPreprocessor:
    def __init__(self, source_lang, target_lang, bpe_symbols, max_vocab_size, bpe_model):
        self._source_lang = source_lang
        self._target_lang = target_lang
        self._bpe_model = bpe_model
        self._bpe_symbols = bpe_symbols
        self._max_vocab_size = max_vocab_size

        self._logger = logging.getLogger('mmt.neural.BPEPreprocessor')

    def create(self, corpora):
        class _ReaderWrapper:
            def __init__(self, _corpus, _langs):
                self.corpus = _corpus
                self.langs = _langs
                self._reader = None

            def __enter__(self):
                self._reader = self.corpus.reader(self.langs).__enter__()
                return self._reader

            def __exit__(self, exc_type, exc_val, exc_tb):
                self._reader.__exit__(exc_type, exc_val, exc_tb)

        if not os.path.exists(self._bpe_model):
            self._logger.info('Creating BPE model')
            vb_builder = SubwordTextProcessor.Builder(symbols=self._bpe_symbols,
                                                      max_vocabulary_size=self._max_vocab_size)
            encoder = vb_builder.build([_ReaderWrapper(c, [self._source_lang, self._target_lang]) for c in corpora])
            encoder.save_to_file(self._bpe_model)
        else:
            self._logger.info('Creating BPE model: do nothing because it already exists')

    @staticmethod
    def load(model):
        return SubwordTextProcessor.load_from_file(model)


class NMTPreprocessor:
    def __init__(self, source_lang, target_lang, bpe_model):
        self._source_lang = source_lang
        self._target_lang = target_lang
        self._bpe_model = bpe_model

        self._logger = logging.getLogger('mmt.neural.NMTPreprocessor')
        self._ram_limit_mb = 1024

    def process(self, corpora, valid_corpora, output_path, working_dir='.', dump_dicts=False):

        bpe_encoder = BPEPreprocessor.load(self._bpe_model)

        self._logger.info('Creating vocabularies')
        src_vocab = onmt.Dict([onmt.Constants.PAD_WORD, onmt.Constants.UNK_WORD,
                               onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD], lower=False)
        trg_vocab = onmt.Dict([onmt.Constants.PAD_WORD, onmt.Constants.UNK_WORD,
                               onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD], lower=False)

        for word in bpe_encoder.get_source_terms():
            src_vocab.add(word)
        for word in bpe_encoder.get_target_terms():
            trg_vocab.add(word)

        self._logger.info('Preparing training corpora')
        self._logger.info('Storing NMT preprocessed training data to "%s"' % output_path)
        self._prepare_sharded_dataset(output_path, corpora, bpe_encoder, src_vocab, trg_vocab)

        self._logger.info('Preparing validation corpora')
        src_valid, trg_valid = self._prepare_corpora(valid_corpora, bpe_encoder, src_vocab, trg_vocab)

        output_file = os.path.join(output_path, 'train_processed.train.pt')
        self._logger.info('Storing NMT preprocessed validation data to "%s"' % output_file)
        torch.save({
            'dicts': {'src': src_vocab, 'tgt': trg_vocab},
            'valid': {'src': src_valid, 'tgt': trg_valid},
        }, output_file)

        if dump_dicts:
            src_dict_file = os.path.join(working_dir, 'train_processed.src.dict')
            trg_dict_file = os.path.join(working_dir, 'train_processed.trg.dict')

            self._logger.info('Storing NMT preprocessed source dictionary "%s"' % src_dict_file)
            src_vocab.writeFile(src_dict_file)

            self._logger.info('Storing NMT preprocessed target dictionary "%s"' % trg_dict_file)
            trg_vocab.writeFile(trg_dict_file)

    def _prepare_corpora(self, corpora, bpe_encoder, src_vocab, trg_vocab):
        src, trg = [], []
        sizes = []
        count, ignored = 0, 0

        for corpus in corpora:
            with corpus.reader([self._source_lang, self._target_lang]) as reader:
                for source, target in reader:
                    src_words = bpe_encoder.encode_line(source, is_source=True)
                    trg_words = bpe_encoder.encode_line(target, is_source=False)

                    if len(src_words) > 0 and len(trg_words) > 0:
                        src.append(src_vocab.convertToIdxTensor(src_words,
                                                                onmt.Constants.UNK_WORD))
                        trg.append(trg_vocab.convertToIdxTensor(trg_words,
                                                                onmt.Constants.UNK_WORD,
                                                                onmt.Constants.BOS_WORD,
                                                                onmt.Constants.EOS_WORD))
                        sizes.append(len(src_words))
                    else:
                        ignored += 1

                    count += 1
                    if count % 100000 == 0:
                        self._logger.info(' %d sentences prepared' % count)

        self._logger.info('Shuffling sentences')
        perm = torch.randperm(len(src))
        src = [src[idx] for idx in perm]
        trg = [trg[idx] for idx in perm]
        sizes = [sizes[idx] for idx in perm]

        self._logger.info('Sorting sentences by size')
        _, perm = torch.sort(torch.Tensor(sizes))
        src = [src[idx] for idx in perm]
        trg = [trg[idx] for idx in perm]

        self._logger.info('Prepared %d sentences (%d ignored due to length == 0)' % (len(src), ignored))

        return src, trg

    def _prepare_sharded_dataset(self, path, corpora, bpe_encoder, src_vocab, trg_vocab):
        count, added, ignored = 0, 0, 0

        # create the ShardedDataset builder
        builder = ShardedDataset.Builder(path)

        # fill the ShardedDataset with source pairs
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

    def train(self, data_path, working_dir):
        self._logger.info('Training started for data "%s"' % data_path)

        is_using_cuda = torch_is_using_cuda()
        save_model = os.path.join(working_dir, 'train_model')

        # Loading training data ----------------------------------------------------------------------------------------
        with _log_timed_action(self._logger, 'Loading training data from "%s"' % data_path):
            train_data = ShardedDataset.load(data_path, 64, cuda=is_using_cuda, volatile=False)

        # Loading validation data and dictionaries ---------------------------------------------------------------------
        data_file = os.path.join(data_path, 'train_processed.train.pt')

        with _log_timed_action(self._logger, 'Loading validation data and dictionaries from "%s"' % data_file):
            data_set = torch.load(data_file)
            src_dict, trg_dict = data_set['dicts']['src'], data_set['dicts']['tgt']
            src_valid, trg_valid = data_set['valid']['src'], data_set['valid']['tgt']
            valid_data = onmt.Dataset(src_valid, trg_valid, 64, is_using_cuda, volatile=True)

        # Creating trainer ---------------------------------------------------------------------------------------------
        with _log_timed_action(self._logger, 'Building trainer'):
            trainer = NMTEngineTrainer.new_instance(src_dict, trg_dict)

        # Training model -----------------------------------------------------------------------------------------------
        self._logger.info(' Vocabulary size. source = %d; target = %d' % (src_dict.size(), trg_dict.size()))
        self._logger.info(' Maximum batch size. %d' % trainer.batch_size)

        try:
            with _log_timed_action(self._logger, 'Train model'):
                checkpoint = trainer.train_model(train_data, valid_data=valid_data, save_path=save_model)
        except TrainingInterrupt as e:
            checkpoint = e.checkpoint

        # Saving last checkpoint ---------------------------------------------------------------------------------------
        if checkpoint is None:
            raise Exception('Training interrupted before first checkpoint could be saved')

        with _log_timed_action(self._logger, 'Storing model'):
            model_folder = os.path.abspath(os.path.join(self.model, os.path.pardir))
            if not os.path.isdir(model_folder):
                os.mkdir(model_folder)

            for f in glob.glob(checkpoint + '.*'):
                _, extension = os.path.splitext(f)
                os.rename(f, self.model + extension)

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
        pt_model = os.path.join(decoder_path, model_name)

        self.memory = TranslationMemory(memory_path, self.source_lang, self.target_lang)
        self.bpe_processor = BPEPreprocessor(source_lang, target_lang, bpe_symbols, max_vocab_size, bpe_model)
        self.nmt_preprocessor = NMTPreprocessor(self.source_lang, self.target_lang, bpe_model)
        self.decoder = NMTDecoder(pt_model, self.source_lang, self.target_lang)

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
            engine.parameters.tuning_max_learning_rate = best_lr
            engine.parameters.tuning_max_epochs = max_epochs
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
                 validation_corpora=None, bpe_symbols=90000, max_vocab_size=None, max_training_words=None, gpus=None):
        EngineBuilder.__init__(self,
                               NeuralEngine(name, source_lang, target_lang, bpe_symbols=bpe_symbols,
                                            max_vocab_size=max_vocab_size, gpus=gpus),
                               roots, debug, steps, split_trainingset, max_training_words)
        self._valid_corpora_path = validation_corpora if validation_corpora is not None \
            else os.path.join(self._engine.data_path, TrainingPreprocessor.DEV_FOLDER_NAME)

    def _build_schedule(self):
        return EngineBuilder._build_schedule(self) + [self._build_memory, self._build_bpe,
                                                      self._prepare_training_data, self._train_decoder]

    def _check_constraints(self):
        pass

    # ~~~~~~~~~~~~~~~~~~~~~ Training step functions ~~~~~~~~~~~~~~~~~~~~~

    @EngineBuilder.Step('Creating translation memory')
    def _build_memory(self, args, skip=False, log=None):
        if not skip:
            corpora = filter(None, [args.processed_bilingual_corpora, args.bilingual_corpora])[0]

            self._engine.memory.create(corpora, log=log)

    @EngineBuilder.Step('Creating BPE model')
    def _build_bpe(self, args, skip=False):
        if not skip:
            corpora = filter(None, [args.filtered_bilingual_corpora, args.processed_bilingual_corpora,
                                    args.bilingual_corpora])[0]

            self._engine.bpe_processor.create(corpora)

    @EngineBuilder.Step('Preparing training data')
    def _prepare_training_data(self, args, skip=False):
        working_dir = self._get_tempdir('onmt_training')
        args.onmt_training_path = working_dir

        if not skip:
            validation_corpora = BilingualCorpus.list(self._valid_corpora_path)
            validation_corpora, _ = self._engine.training_preprocessor.process(validation_corpora,
                                                                               os.path.join(working_dir, 'valid_set'))
            args.validation_corpora = validation_corpora
            corpora = filter(None, [args.processed_bilingual_corpora, args.bilingual_corpora])[0]

            self._engine.nmt_preprocessor.process(corpora, validation_corpora, args.onmt_training_path,
                                                  working_dir=working_dir)

    @EngineBuilder.Step('Neural decoder training')
    def _train_decoder(self, args, skip=False, delete_on_exit=False):
        working_dir = self._get_tempdir('onmt_model')

        if not skip:
            self._engine.decoder.train(args.onmt_training_path, working_dir)

            if delete_on_exit:
                shutil.rmtree(working_dir, ignore_errors=True)
