import glob
import logging
import os
import shutil
import sys

import time

from cli import mmt_javamain, LIB_DIR
from cli.libs import fileutils
from cli.libs import shell
from cli.mmt import BilingualCorpus
from cli.mmt.engine import Engine, EngineBuilder
from cli.mmt.processing import TrainingPreprocessor

sys.path.insert(0, os.path.abspath(os.path.join(LIB_DIR, 'pynmt')))

import onmt
from nmmt import NMTEngineTrainer, SubwordTextProcessor, TrainingInterrupt
from nmmt import ShardedDataset
import torch


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


class OpenNMTPreprocessor:
    def __init__(self, source_lang, target_lang, bpe_model):
        self._source_lang = source_lang
        self._target_lang = target_lang
        self._bpe_model = bpe_model

        self._logger = logging.getLogger('mmt.train.OpenNMTPreprocessor')
        self._ram_limit_mb = 1024

    def process(self, corpora, valid_corpora, output_path, bpe_symbols, max_vocab_size, working_dir='.',
                dump_dicts=False):
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

        self._logger.info('Creating VBE vocabulary')
        vb_builder = SubwordTextProcessor.Builder(symbols=bpe_symbols, max_vocabulary_size=max_vocab_size)
        bpe_encoder = vb_builder.build([_ReaderWrapper(c, [self._source_lang, self._target_lang]) for c in corpora])
        bpe_encoder.save_to_file(self._bpe_model)

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
        self._logger.info('Storing OpenNMT preprocessed training data to "%s"' % output_path)
        self._prepare_sharded_dataset(output_path, corpora, bpe_encoder, src_vocab, trg_vocab)

        self._logger.info('Preparing validation corpora')
        src_valid, trg_valid = self._prepare_corpora(valid_corpora, bpe_encoder, src_vocab, trg_vocab)

        output_file = os.path.join(output_path, 'train_processed.train.pt')
        self._logger.info('Storing OpenNMT preprocessed validation data to "%s"' % output_file)
        torch.save({
            'dicts': {'src': src_vocab, 'tgt': trg_vocab},
            'valid': {'src': src_valid, 'tgt': trg_valid},
        }, output_file)

        if dump_dicts:
            src_dict_file = os.path.join(working_dir, 'train_processed.src.dict')
            trg_dict_file = os.path.join(working_dir, 'train_processed.trg.dict')

            self._logger.info('Storing OpenNMT preprocessed source dictionary "%s"' % src_dict_file)
            src_vocab.writeFile(src_dict_file)

            self._logger.info('Storing OpenNMT preprocessed target dictionary "%s"' % trg_dict_file)
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


class OpenNMTDecoder:
    def __init__(self, model, source_lang, target_lang, gpus):
        self._model = model
        self._source_lang = source_lang
        self._target_lang = target_lang
        self._gpus = gpus

    def train(self, data_path, working_dir):
        logger = logging.getLogger('mmt.train.OpenNMTDecoder')
        logger.info('Training started for data "%s"' % data_path)

        save_model = os.path.join(data_path, 'train_model')

        # Loading training data ----------------------------------------------------------------------------------------
        logger.info('Loading training data from "%s"... START' % data_path)
        start_time = time.time()
        train_data = ShardedDataset.load(data_path, 64, cuda=self._gpus, volatile=False)
        logger.info('Loading training data... END %.2fs' % (time.time() - start_time))

        # Loading validation data and dictionaries ----------------------------------------------------------------------------------------
        data_file = os.path.join(data_path, 'train_processed.train.pt')
        logger.info('Loading validation data and dictionaries from "%s"... START' % data_file)
        start_time = time.time()
        data_set = torch.load(data_file)
        logger.info('Loading validation data... END %.2fs' % (time.time() - start_time))

        src_dict, trg_dict = data_set['dicts']['src'], data_set['dicts']['tgt']
        src_valid, trg_valid = data_set['valid']['src'], data_set['valid']['tgt']

        # Creating trainer ---------------------------------------------------------------------------------------------

        logger.info('Building model... START')
        start_time = time.time()
        trainer = NMTEngineTrainer.new_instance(src_dict, trg_dict, random_seed=3435, gpu_ids=self._gpus)
        logger.info('Building model... END %.2fs' % (time.time() - start_time))

        # Creating validation data sets -------------------------------------------------------------------------------------------
        logger.info('Creating Data... START')
        start_time = time.time()
        valid_data = onmt.Dataset(src_valid, trg_valid, trainer.batch_size, self._gpus, volatile=True)
        logger.info('Creating Data... END %.2fs' % (time.time() - start_time))

        logger.info(' Vocabulary size. source = %d; target = %d' % (src_dict.size(), trg_dict.size()))
        logger.info(' Maximum batch size. %d' % trainer.batch_size)

        # Training model -----------------------------------------------------------------------------------------------
        logger.info('Training model... START')
        try:
            start_time = time.time()
            checkpoint = trainer.train_model(train_data, valid_data=valid_data, save_path=save_model)
            logger.info('Training model... END %.2fs' % (time.time() - start_time))
        except TrainingInterrupt as e:
            checkpoint = e.checkpoint
            logger.info('Training model... INTERRUPTED %.2fs' % (time.time() - start_time))

        # Saving last checkpoint ---------------------------------------------------------------------------------------
        model_folder = os.path.abspath(os.path.join(self._model, os.path.pardir))
        if not os.path.isdir(model_folder):
            os.mkdir(model_folder)

        if checkpoint is not None:
            logger.info('Storing model "%s" to %s' % (checkpoint, self._model))
            os.rename(checkpoint, self._model)
        else:
            logger.info('checkpoint is None')

        with open(os.path.join(model_folder, 'model.map'), 'w') as model_map:
            model_map.write('%s__%s = model\n' % (self._source_lang, self._target_lang))


class NeuralEngine(Engine):
    def __init__(self, name, source_lang, target_lang, gpus=None):
        Engine.__init__(self, name, source_lang, target_lang)

        if torch.cuda.is_available():
            if gpus is None:
                gpus = range(torch.cuda.device_count()) if torch.cuda.is_available() else None
            else:
                # remove indexes of GPUs which are not valid,
                # because larger than the number of available GPU or smaller than 0
                gpus = [x for x in gpus if x < torch.cuda.device_count() or x < 0]
                if len(gpus) == 0:
                    gpus = None
        else:
            gpus = None

        decoder_path = os.path.join(self.models_path, 'decoder')

        # Neural specific models
        self.memory = TranslationMemory(os.path.join(decoder_path, 'memory'), self.source_lang, self.target_lang)
        self.decoder = OpenNMTDecoder(os.path.join(decoder_path, 'model.pt'), self.source_lang, self.target_lang, gpus)
        self.onmt_preprocessor = OpenNMTPreprocessor(self.source_lang, self.target_lang,
                                                     os.path.join(decoder_path, 'model.bpe'))

    def is_tuning_supported(self):
        return False

    def type(self):
        return 'neural'


class NeuralEngineBuilder(EngineBuilder):
    def __init__(self, name, source_lang, target_lang, roots, debug=False, steps=None, split_trainingset=True,
                 validation_corpora=None, bpe_symbols=90000, max_vocab_size=None, max_training_words=None, gpus=None):
        EngineBuilder.__init__(self, NeuralEngine(name, source_lang, target_lang, gpus), roots, debug, steps,
                               split_trainingset, max_training_words)
        self._bpe_symbols = bpe_symbols
        self._max_vocab_size = max_vocab_size
        self._valid_corpora_path = validation_corpora if validation_corpora is not None \
            else os.path.join(self._engine.data_path, TrainingPreprocessor.DEV_FOLDER_NAME)

    def _build_schedule(self):
        return EngineBuilder._build_schedule(self) + \
               [self._build_memory, self._prepare_training_data, self._train_decoder]

    def _check_constraints(self):
        pass

    # ~~~~~~~~~~~~~~~~~~~~~ Training step functions ~~~~~~~~~~~~~~~~~~~~~

    @EngineBuilder.Step('Creating translation memory')
    def _build_memory(self, args, skip=False, log=None):
        if not skip:
            corpora = filter(None, [args.processed_bilingual_corpora, args.bilingual_corpora])[0]

            self._engine.memory.create(corpora, log=log)

    @EngineBuilder.Step('Preparing training data')
    def _prepare_training_data(self, args, skip=False):
        working_dir = self._get_tempdir('onmt_training')
        args.onmt_training_path = working_dir

        if not skip:
            validation_corpora = BilingualCorpus.list(self._valid_corpora_path)
            validation_corpora, _ = self._engine.training_preprocessor.process(validation_corpora,
                                                                               os.path.join(working_dir, 'valid_set'))

            corpora = filter(None, [args.processed_bilingual_corpora, args.bilingual_corpora])[0]

            self._engine.onmt_preprocessor.process(corpora, validation_corpora, args.onmt_training_path,
                                                   bpe_symbols=self._bpe_symbols, max_vocab_size=self._max_vocab_size,
                                                   working_dir=working_dir)

    @EngineBuilder.Step('Neural decoder training')
    def _train_decoder(self, args, skip=False, delete_on_exit=False):
        working_dir = self._get_tempdir('onmt_model')

        if not skip:
            self._engine.decoder.train(args.onmt_training_path, working_dir)

            if delete_on_exit:
                shutil.rmtree(working_dir, ignore_errors=True)
