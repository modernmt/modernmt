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

sys.path.insert(0, os.path.abspath(os.path.join(LIB_DIR, 'opennmt')))

import onmt
from bpe import BPEEncoderBuilder
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
    def __init__(self, source_lang, target_lang, bpe_model, vocab_size=50000, max_line_length=80):
        self._source_lang = source_lang
        self._target_lang = target_lang
        self._vocab_size = vocab_size
        self._max_line_length = max_line_length
        self._logger = logging.getLogger('mmt.train.OpenNMTPreprocessor')

        self._bpe_trainer = BPEEncoderBuilder(bpe_model)
        self._preprocessor = TrainingPreprocessor(source_lang, target_lang)

    def process(self, corpora, validation_corpora, output_file, working_dir='.'):
        self._logger.info('Creating VBE vocabulary')
        bpe_encoder = self._create_vb_encoder(corpora)

        self._logger.info('Creating vocabularies')
        src_vocab, trg_vocab = self._create_vocabs(bpe_encoder, corpora)

        self._logger.info('Preparing training corpora')
        src_train, trg_train = self._prepare_corpora(corpora, bpe_encoder, src_vocab, trg_vocab)

        self._logger.info('Preparing validation corpora')
        validation_corpora, _ = self._preprocessor.process(validation_corpora, os.path.join(working_dir, 'valid_set'))
        src_valid, trg_valid = self._prepare_corpora(validation_corpora, bpe_encoder, src_vocab, trg_vocab)

        self._logger.info('Storing OpenNMT preprocessed data to "%s"' % output_file)
        torch.save({
            'dicts': {'src': src_vocab, 'tgt': trg_vocab},
            'train': {'src': src_train, 'tgt': trg_train},
            'valid': {'src': src_valid, 'tgt': trg_valid},
        }, output_file)

    def _create_vb_encoder(self, corpora):
        vb_builder = BPEEncoderBuilder.VocabularyBuilder()

        for corpus in corpora:
            with corpus.reader([self._source_lang, self._target_lang]) as reader:
                for source, target in reader:
                    vb_builder.add_line(source)
                    vb_builder.add_line(target)

        return self._bpe_trainer.learn(vb_builder.build(), symbols=self._vocab_size)

    def _create_vocabs(self, bpe_encoder, corpora):
        src_vocab = onmt.Dict([onmt.Constants.PAD_WORD, onmt.Constants.UNK_WORD,
                               onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD], lower=False)
        trg_vocab = onmt.Dict([onmt.Constants.PAD_WORD, onmt.Constants.UNK_WORD,
                               onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD], lower=False)

        for corpus in corpora:
            with corpus.reader([self._source_lang, self._target_lang]) as reader:
                for source, target in reader:
                    for word in bpe_encoder.encode_line(source):
                        src_vocab.add(word)
                    for word in bpe_encoder.encode_line(target):
                        trg_vocab.add(word)

        return src_vocab, trg_vocab

    def _prepare_corpora(self, corpora, bpe_encoder, src_vocab, trg_vocab):
        src, trg = [], []
        sizes = []
        count, ignored = 0, 0

        for corpus in corpora:
            with corpus.reader([self._source_lang, self._target_lang]) as reader:
                for source, target in reader:
                    src_words, trg_words = bpe_encoder.encode_line(source), bpe_encoder.encode_line(target)

                    if 0 < len(src_words) <= self._max_line_length and 0 < len(trg_words) <= self._max_line_length:
                        src.append(src_vocab.convertToIdx(src_words,
                                                          onmt.Constants.UNK_WORD))
                        trg.append(trg_vocab.convertToIdx(trg_words,
                                                          onmt.Constants.UNK_WORD,
                                                          onmt.Constants.BOS_WORD,
                                                          onmt.Constants.EOS_WORD))
                        sizes.append(len(src_words))
                    else:
                        ignored += 1

                    count += 1
                    if count % 100000 == 0:
                        self._logger.info(' %d sentences prepared' % count)

        self._logger.info('Prepared %d sentences (%d ignored due to length == 0 or > %d)' %
                          (len(src), ignored, self._max_line_length))

        return src, trg


class OpenNMTDecoder:
    def __init__(self, model, source_lang, target_lang, opts=onmt.Trainer.Options()):
        self._model = model
        self._source_lang = source_lang
        self._target_lang = target_lang
        self._opts = opts

    def train(self, data_path, working_dir):
        logger = logging.getLogger('mmt.train.OpenNMTDecoder')

        logger.info('Training started for data "%s", with options: %s' % (data_path, repr(self._opts)))

        self._opts.save_model = os.path.join(working_dir, 'train_model')

        if self._opts.seed >= 0:
            torch.manual_seed(self._opts.seed)  # Sets the seed for generating random numbers

        if self._opts.gpus:
            torch.cuda.set_device(self._opts.gpus[0])

        # Loading training data ----------------------------------------------------------------------------------------

        logger.info('Loading data from "%s"... START' % data_path)
        start_time = time.time()
        data_set = torch.load(data_path)
        logger.info('Loading data... END %.2fs' % (time.time() - start_time))

        logger.info('Creating Data... START')
        start_time = time.time()
        train_data = onmt.Dataset(data_set['train']['src'], data_set['train']['tgt'],
                                  self._opts.batch_size, self._opts.gpus)
        valid_data = onmt.Dataset(data_set['valid']['src'], data_set['valid']['tgt'],
                                  self._opts.batch_size, self._opts.gpus, volatile=True)
        src_dict, trg_dict = data_set['dicts']['src'], data_set['dicts']['tgt']
        logger.info('Creating Data... END %.2fs' % (time.time() - start_time))

        logger.info(' Vocabulary size. source = %d; target = %d' % (src_dict.size(), trg_dict.size()))
        logger.info(' Number of training sentences. %d' % len(data_set['train']['src']))
        logger.info(' Maximum batch size. %d' % self._opts.batch_size)

        # Building model -----------------------------------------------------------------------------------------------

        logger.info('Building model... START')
        start_time = time.time()

        encoder = onmt.Models.Encoder(self._opts, src_dict)
        decoder = onmt.Models.Decoder(self._opts, trg_dict)
        generator = torch.nn.Sequential(torch.nn.Linear(self._opts.rnn_size, trg_dict.size()), torch.nn.LogSoftmax())

        model = onmt.Models.NMTModel(encoder, decoder)

        if len(self._opts.gpus) > 0:
            model.cuda()
            generator.cuda()
        else:
            model.cpu()
            generator.cpu()

        if len(self._opts.gpus) > 1:
            model = torch.nn.DataParallel(model, device_ids=self._opts.gpus, dim=1)
            generator = torch.nn.DataParallel(generator, device_ids=self._opts.gpus, dim=0)

        model.generator = generator

        logger.info('Initializing model... START')
        start_time2 = time.time()
        for p in model.parameters():
            p.data.uniform_(-self._opts.param_init, self._opts.param_init)
        encoder.load_pretrained_vectors(self._opts)
        decoder.load_pretrained_vectors(self._opts)
        logger.info('Initializing model... END %.2fs' % (time.time() - start_time2))

        logger.info('Initializing optimizer... START')
        start_time2 = time.time()
        optim = onmt.Optim(
            self._opts.optim, self._opts.learning_rate, self._opts.max_grad_norm,
            lr_decay=self._opts.learning_rate_decay, start_decay_at=self._opts.start_decay_at
        )
        optim.set_parameters(model.parameters())
        logger.info('Initializing optimizer... END %.2fs' % (time.time() - start_time2))

        logger.info('Building model... END %.2fs' % (time.time() - start_time))

        # Training model -----------------------------------------------------------------------------------------------

        num_params = sum([p.nelement() for p in model.parameters()])
        logger.info(' Number of parameters: %d' % num_params)

        logger.info('Training model... START')
        try:
            start_time = time.time()
            trainer = onmt.Trainer(self._opts)
            trainer.trainModel(model, train_data, valid_data, data_set, optim)
            logger.info('Training model... END %.2fs' % (time.time() - start_time))
        except KeyboardInterrupt:
            logger.info('Training model... INTERRUPTED %.2fs' % (time.time() - start_time))

        # Saving last checkpoint ---------------------------------------------------------------------------------------

        all_checkpoints = []
        for checkpoint in glob.glob(self._opts.save_model + '_*.pt'):
            filename = os.path.splitext(os.path.basename(checkpoint))[0]
            epoch = filename.split('_')[-1]
            if not epoch.startswith('e'):
                raise Exception('Invalid checkpoint file "%s"' % checkpoint)

            epoch = int(epoch[1:])
            all_checkpoints.append([epoch, checkpoint])

        if len(all_checkpoints) == 0:
            raise Exception('Unable to find checkpoint files in "%s"' % working_dir)

        _, checkpoint = sorted(all_checkpoints, key=lambda x: x[0], reverse=True)[0]

        model_folder = os.path.abspath(os.path.join(self._model, os.path.pardir))
        if not os.path.isdir(model_folder):
            os.mkdir(model_folder)

        os.rename(checkpoint, self._model)


class NeuralEngine(Engine):
    def __init__(self, name, source_lang, target_lang):
        Engine.__init__(self, name, source_lang, target_lang)

        decoder_path = os.path.join(self.models_path, 'decoder')

        # Neural specific models
        self.memory = TranslationMemory(os.path.join(decoder_path, 'memory'), self.source_lang, self.target_lang)
        self.decoder = OpenNMTDecoder(os.path.join(decoder_path, 'model.pt'), self.source_lang, self.target_lang)
        self.onmt_preprocessor = OpenNMTPreprocessor(self.source_lang, self.target_lang,
                                                     os.path.join(decoder_path, 'vocabulary.bpe'))

    def is_tuning_supported(self):
        return False

    def type(self):
        return 'neural'


class NeuralEngineBuilder(EngineBuilder):
    def __init__(self, name, source_lang, target_lang, roots, debug=False, steps=None, split_trainingset=True,
                 validation_corpora=None):
        EngineBuilder.__init__(self, NeuralEngine(name, source_lang, target_lang), roots, debug, steps,
                               split_trainingset)
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
            corpora = filter(None, [args.filtered_bilingual_corpora, args.processed_bilingual_corpora,
                                    args.bilingual_corpora])[0]

            self._engine.memory.create(corpora, log=log)

    @EngineBuilder.Step('Preparing training data')
    def _prepare_training_data(self, args, skip=False):
        working_dir = self._get_tempdir('onmt_training')
        args.onmt_training_file = os.path.join(working_dir, 'train_processed.train.pt')

        if not skip:
            validation_corpora = BilingualCorpus.list(self._valid_corpora_path)
            corpora = filter(None, [args.filtered_bilingual_corpora, args.processed_bilingual_corpora,
                                    args.bilingual_corpora])[0]

            self._engine.onmt_preprocessor.process(corpora, validation_corpora, args.onmt_training_file)

    @EngineBuilder.Step('Neural decoder training')
    def _train_decoder(self, args, skip=False, delete_on_exit=False):
        working_dir = self._get_tempdir('onmt_model')

        if not skip:
            self._engine.decoder.train(args.onmt_training_file, working_dir)

            if delete_on_exit:
                shutil.rmtree(working_dir, ignore_errors=True)
