import math
import os
import time

import torch
import torch.nn as nn
from torch.autograd import Variable

import onmt
import logging


class TrainingInterrupt(Exception):
    def __init__(self, checkpoint):
        super(TrainingInterrupt, self).__init__()
        self.checkpoint = checkpoint


class Trainer(object):
    class Options(object):
        def __init__(self):
            self.save_model = None  # Set by train

            self.seed = 3435
            self.gpus = range(torch.cuda.device_count()) if torch.cuda.is_available() else 0
            self.log_interval = 50

            # Model options --------------------------------------------------------------------------------------------
            self.encoder_type = "text"  # type fo encoder (either "text" or "img"
            self.layers = 2  # Number of layers in the LSTM encoder/decoder
            self.rnn_size = 500  # Size of LSTM hidden states
            self.word_vec_size = 500  # Word embedding sizes
            self.input_feed = 1  # Feed the context vector at each time step as additional input to the decoder
            self.brnn = True  # Use a bidirectional encoder
            self.brnn_merge = 'sum'  # Merge action for the bidirectional hidden states: [concat|sum]

            # Optimization options -------------------------------------------------------------------------------------
            self.batch_size = 64  # Maximum batch size
            self.max_generator_batches = 32  # Maximum batches of words in a seq to run the generator on in parallel.
            self.max_epochs = 40  # Maximum number of training epochs
            self.min_epochs = 8  # Minimum number of training epochs
            self.start_epoch = 1  # The epoch from which to start
            self.min_perplexity_decrement = .03  # If perplexity decrement is lower than this percentage, stop training
            self.param_init = 0.1  # Parameters are initialized over uniform distribution with support
            self.optim = 'sgd'  # Optimization method. [sgd|adagrad|adadelta|adam]
            self.max_grad_norm = 5  # If norm(gradient vector) > max_grad_norm, re-normalize
            self.dropout = 0.3  # Dropout probability; applied between LSTM stacks.
            self.curriculum = False
            self.extra_shuffle = False  # Shuffle and re-assign mini-batches

            # Learning rate --------------------------------------------------------------------------------------------
            self.learning_rate = 1.0
            self.learning_rate_decay = 0.9
            self.start_decay_at = 10

            # Pre-trained word vectors ---------------------------------------------------------------------------------
            self.pre_word_vecs_enc = None
            self.pre_word_vecs_dec = None

        def state_dict(self):
            return self.__dict__

        def load_state_dict(self, d):
            self.__dict__ = d
            # we force the encoder type to "text";
            # this trick makes the models build with an old version of the software compatible with the new version
            self.encoder_type = "text"  # type fo encoder (either "text" or "img"

        def __repr__(self):
            return repr(self.__dict__)

    def __init__(self, opt):
        self.opt = opt

        self._logger = logging.getLogger('onmt.Trainer')
        self._logger.info('Training Options:%s' % self.opt)

    def NMTCriterion(self, vocabSize):
        opt = self.opt
        weight = torch.ones(vocabSize)
        weight[onmt.Constants.PAD] = 0
        crit = nn.NLLLoss(weight, size_average=False)
        if opt.gpus:
            crit.cuda()
        return crit

    def memoryEfficientLoss(self, outputs, targets, generator, crit, eval=False):
        opt = self.opt
        # compute generations one piece at a time
        num_correct, loss = 0, 0
        outputs = Variable(outputs.data, requires_grad=(not eval), volatile=eval)

        batch_size = outputs.size(1)
        outputs_split = torch.split(outputs, opt.max_generator_batches)
        targets_split = torch.split(targets, opt.max_generator_batches)

        for i, (out_t, targ_t) in enumerate(zip(outputs_split, targets_split)):
            out_t = out_t.view(-1, out_t.size(2))
            scores_t = generator(out_t)
            loss_t = crit(scores_t, targ_t.view(-1))
            pred_t = scores_t.max(1)[1]
            num_correct_t = pred_t.data.eq(targ_t.data).masked_select(targ_t.ne(onmt.Constants.PAD).data).sum()
            num_correct += num_correct_t
            loss += loss_t.data[0]
            if not eval:
                loss_t.div(batch_size).backward()

        grad_output = None if outputs.grad is None else outputs.grad.data
        return loss, grad_output, num_correct

    def eval(self, model, criterion, data):
        total_loss = 0
        total_words = 0
        total_num_correct = 0

        model.eval()
        for i in range(len(data)):
            batch = data[i][:-1]  # exclude original indices
            outputs = model(batch)
            targets = batch[1][1:]  # exclude <s> from targets
            loss, _, num_correct = self.memoryEfficientLoss(
                outputs, targets, model.generator, criterion, eval=True)
            total_loss += loss
            total_num_correct += num_correct
            total_words += targets.data.ne(onmt.Constants.PAD).sum()

        model.train()
        return total_loss / total_words, float(total_num_correct) / total_words

    def train_model(self, model_ori, train_data, valid_data, dataset, optim_ori, save_epochs=5):
        model = model_ori
        optim = optim_ori

        # set the mask to None; required when the same model is trained after a translation
        model.decoder.attn.applyMask(None)
        model.train()

        # define criterion of each GPU
        criterion = self.NMTCriterion(dataset['dicts']['tgt'].size())

        perplexity_history = []
        checkpoint_files = []
        valid_acc, valid_ppl = None, None

        try:
            for epoch in range(self.opt.start_epoch, self.opt.max_epochs + 1):
                self._logger.info('Training epoch %g... START' % epoch)
                start_time_epoch = time.time()

                #  (1) train for one epoch on the training set
                train_loss, train_acc = self._train_epoch(epoch, train_data, model, criterion, optim)
                train_ppl = math.exp(min(train_loss, 100))
                self._logger.info('trainEpoch Epoch %g Train loss: %g perplexity: %g accuracy: %g' % (
                    epoch, train_loss, train_ppl, (float(train_acc) * 100)))

                force_termination = False

                if self.opt.min_perplexity_decrement > 0.:
                    perplexity_history.append(train_ppl)
                    force_termination = self._should_terminate(perplexity_history)

                if valid_data:
                    #  (2) evaluate on the validation set
                    valid_loss, valid_acc = self.eval(model, criterion, valid_data)
                    valid_ppl = math.exp(min(valid_loss, 100))
                    self._logger.info('trainModel Epoch %g Validation loss: %g perplexity: %g accuracy: %g' % (
                        epoch, valid_loss, valid_ppl, (float(valid_acc) * 100)))

                    # (3) update the learning rate
                    optim.updateLearningRate(valid_loss, epoch)

                    self._logger.info("trainModel Epoch %g Decaying learning rate to %g" % (epoch, optim.lr))

                if save_epochs > 0:
                    if len(checkpoint_files) > 0 and len(checkpoint_files) > save_epochs - 1:
                        os.remove(checkpoint_files.pop(0))

                    model_state_dict = model.module.state_dict() if len(self.opt.gpus) > 1 else model.state_dict()
                    model_state_dict = {k: v for k, v in model_state_dict.items() if 'generator' not in k}
                    generator_state_dict = model.generator.module.state_dict() if len(
                        self.opt.gpus) > 1 else model.generator.state_dict()
                    opt_state_dict = self.opt.state_dict()

                    #  (4) drop a checkpoint
                    checkpoint = {
                        'model': model_state_dict,
                        'generator': generator_state_dict,
                        'dicts': dataset['dicts'],
                        'opt': opt_state_dict,
                        'epoch': epoch,
                        'optim': optim
                    }

                    if valid_acc is not None:
                        checkpoint_file = \
                            '%s_acc_%.2f_ppl_%.2f_e%d.pt' % (self.opt.save_model, 100 * valid_acc, valid_ppl, epoch)
                    else:
                        checkpoint_file = '%s_acc_NA_ppl_NA_e%d.pt' % (self.opt.save_model, epoch)

                    torch.save(checkpoint, checkpoint_file)
                    checkpoint_files.append(checkpoint_file)
                    self._logger.info("Checkpoint for epoch %d saved to file %s" % (epoch, checkpoint_file))

                if force_termination:
                    break

                self._logger.info('Training epoch %g... END %.2fs' % (epoch, time.time() - start_time_epoch))
        except KeyboardInterrupt:
            raise TrainingInterrupt(checkpoint=checkpoint_files[-1] if len(checkpoint_files) > 0 else None)

        return checkpoint_files[-1] if len(checkpoint_files) > 0 else None

    def _train_epoch(self, epoch, train_data, model, criterion, optim):
        if self.opt.extra_shuffle and epoch > self.opt.curriculum:
            train_data.shuffle()

        # shuffle mini batch order
        batchOrder = torch.randperm(len(train_data))

        total_loss, total_words, total_num_correct = 0, 0, 0
        report_loss, report_tgt_words, report_src_words, report_num_correct = 0, 0, 0, 0
        start = time.time()
        for i in range(len(train_data)):

            batchIdx = batchOrder[i] if epoch > self.opt.curriculum else i
            batch = train_data[batchIdx][:-1]  # exclude original indices

            model.zero_grad()
            outputs = model(batch)
            targets = batch[1][1:]  # exclude <s> from targets
            loss, gradOutput, num_correct = self.memoryEfficientLoss(
                outputs, targets, model.generator, criterion)

            outputs.backward(gradOutput)

            # update the parameters
            optim.step()

            num_words = targets.data.ne(onmt.Constants.PAD).sum()
            report_loss += loss
            report_num_correct += num_correct
            report_tgt_words += num_words
            report_src_words += batch[0][1].data.sum()
            total_loss += loss
            total_num_correct += num_correct
            total_words += num_words

            if i % self.opt.log_interval == -1 % self.opt.log_interval:
                self._logger.info(
                    "trainEpoch epoch %2d, %5d/%5d; num_corr: %6.2f; %3.0f src tok; "
                    "%3.0f tgt tok; acc: %6.2f; ppl: %6.2f; %3.0f src tok/s; %3.0f tgt tok/s;" %
                    (epoch, i + 1, len(train_data),
                     report_num_correct,
                     report_src_words,
                     report_tgt_words,
                     (float(report_num_correct) / report_tgt_words) * 100,
                     math.exp(report_loss / report_tgt_words),
                     report_src_words / (time.time() - start),
                     report_tgt_words / (time.time() - start)))

                report_loss = report_tgt_words = report_src_words = report_num_correct = 0
                start = time.time()

        return total_loss / total_words, float(total_num_correct) / total_words

    def _should_terminate(self, history):
        if len(history) <= self.opt.min_epochs:
            return False

        current_value = history[-1]
        previous_value = history[-2]

        decrement = (previous_value - current_value) / previous_value

        if 0 < decrement < self.opt.min_perplexity_decrement:
            self._logger.info('Terminating training for perplexity threshold reached: '
                              'new perplexity is %f, while previous was %f (-%.1f%%)'
                              % (current_value, previous_value, decrement * 100))
            return True
        else:
            self._logger.info('Continuing training: '
                              'new perplexity is %f, while previous was %f (-%.1f%%)'
                              % (current_value, previous_value, decrement * 100))
            return False
