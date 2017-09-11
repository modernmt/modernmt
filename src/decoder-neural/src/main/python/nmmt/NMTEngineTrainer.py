import glob
import logging
import math
import os
import time

from torch import nn, torch
from torch.autograd import Variable

from nmmt.torch_utils import torch_is_multi_gpu, torch_is_using_cuda
from onmt import Constants


class TrainingInterrupt(Exception):
    def __init__(self, checkpoint):
        super(TrainingInterrupt, self).__init__()
        self.checkpoint = checkpoint


class NMTEngineTrainer:
    def __init__(self, engine):
        self._logger = logging.getLogger('nmmt.NMTEngineTrainer')
        self._log_level = logging.INFO

        self._engine = engine

        # Public-editable options
        self.log_interval = 50  # Log status every 'log_interval' updates
        self.batch_size = 64  # Maximum batch size
        self.max_generator_batches = 32  # Maximum batches of words in a seq to run the generator on in parallel.
        self.max_epochs = 40  # Maximum number of training epochs
        self.min_epochs = 10  # Minimum number of training epochs
        self.start_epoch = 1  # The epoch from which to start
        self.min_perplexity_decrement = .02  # If perplexity decrement is lower than this percentage, stop training

    def set_log_level(self, level):
        self._log_level = level

    @staticmethod
    def _new_nmt_criterion(vocab_size):
        weight = torch.ones(vocab_size)
        weight[Constants.PAD] = 0
        criterion = nn.NLLLoss(weight, size_average=False)
        if torch_is_using_cuda():
            criterion.cuda()
        return criterion

    def _compute_memory_efficient_loss(self, outputs, targets, generator, criterion, evaluation=False):
        # compute generations one piece at a time
        num_correct, loss = 0, 0
        outputs = Variable(outputs.data, requires_grad=(not evaluation), volatile=evaluation)

        batch_size = outputs.size(1)
        outputs_split = torch.split(outputs, self.max_generator_batches)
        targets_split = torch.split(targets, self.max_generator_batches)

        for i, (out_t, targ_t) in enumerate(zip(outputs_split, targets_split)):
            out_t = out_t.view(-1, out_t.size(2))
            scores_t = generator(out_t)
            loss_t = criterion(scores_t, targ_t.view(-1))
            pred_t = scores_t.max(1)[1]
            num_correct_t = pred_t.data.eq(targ_t.data).masked_select(targ_t.ne(Constants.PAD).data).sum()
            num_correct += num_correct_t
            loss += loss_t.data[0]
            if not evaluation:
                loss_t.div(batch_size).backward()

        grad_output = None if outputs.grad is None else outputs.grad.data
        return loss, grad_output, num_correct

    def _evaluate(self, criterion, data):
        total_loss = 0
        total_words = 0
        total_num_correct = 0

        model = self._engine.model

        model.eval()
        for i in range(len(data)):
            # exclude original indices
            batch = data[i][:-1]
            outputs = model(batch)
            # exclude <s> from targets
            targets = batch[1][1:]
            loss, _, num_correct = self._compute_memory_efficient_loss(outputs, targets, model.generator,
                                                                       criterion, evaluation=True)
            total_loss += loss
            total_num_correct += num_correct
            total_words += targets.data.ne(Constants.PAD).sum()

        model.train()
        return total_loss / total_words, float(total_num_correct) / total_words

    def train_model(self, train_data, valid_data=None, save_path=None, save_epochs=5):
        model = self._engine.model
        optimizer = self._engine.optimizer

        # set the mask to None; required when the same model is trained after a translation
        if torch_is_multi_gpu():
            decoder = model.module.decoder
        else:
            decoder = model.decoder
        decoder.attn.applyMask(None)
        model.train()

        # define criterion of each GPU
        criterion = self._new_nmt_criterion(self._engine.trg_dict.size())

        perplexity_history = []
        checkpoint_files = []
        valid_acc, valid_ppl = None, None

        try:
            self._logger.log(self._log_level, 'Optim options:%s' % repr(optimizer))
            for epoch in range(self.start_epoch, self.max_epochs + 1):
                self._logger.log(self._log_level, 'Training epoch %g... START' % epoch)
                start_time_epoch = time.time()

                #  (1) train for one epoch on the training set
                train_loss, train_acc = self._train_epoch(epoch, train_data, criterion)
                train_ppl = math.exp(min(train_loss, 100))
                self._logger.log(self._log_level, 'trainEpoch Epoch %g Train loss: %g perplexity: %g accuracy: %g' % (
                    epoch, train_loss, train_ppl, (float(train_acc) * 100)))

                force_termination = False

                if self.min_perplexity_decrement > 0.:
                    perplexity_history.append(train_ppl)
                    force_termination = self._should_terminate(perplexity_history)

                if valid_data:
                    #  (2) evaluate on the validation set
                    valid_loss, valid_acc = self._evaluate(criterion, valid_data)
                    valid_ppl = math.exp(min(valid_loss, 100))
                    self._logger.log(self._log_level,
                                     'trainModel Epoch %g Validation loss: %g perplexity: %g accuracy: %g' % (
                                         epoch, valid_loss, valid_ppl, (float(valid_acc) * 100)))

                    # (3) update the learning rate
                    optimizer.updateLearningRate(valid_loss, epoch)

                    self._logger.log(self._log_level,
                                     "trainModel Epoch %g Decaying learning rate to %g" % (epoch, optimizer.lr))

                if save_path is not None and save_epochs > 0:
                    if len(checkpoint_files) > 0 and len(checkpoint_files) > save_epochs - 1:
                        checkpoint_file = checkpoint_files.pop(0)
                        for f in glob.glob(checkpoint_file + '.*'):
                            os.remove(f)

                    # (4) drop a checkpoint
                    if valid_acc is not None:
                        checkpoint_file = \
                            '%s_acc_%.2f_ppl_%.2f_e%d' % (save_path, 100 * valid_acc, valid_ppl, epoch)
                    else:
                        checkpoint_file = '%s_acc_NA_ppl_NA_e%d' % (save_path, epoch)

                    self._engine.save(checkpoint_file, epoch=epoch)

                    checkpoint_files.append(checkpoint_file)
                    self._logger.log(self._log_level,
                                     "Checkpoint for epoch %d saved to file %s" % (epoch, checkpoint_file))

                if force_termination:
                    break

                self._logger.log(self._log_level,
                                 'Training epoch %g... END %.2fs' % (epoch, time.time() - start_time_epoch))
        except KeyboardInterrupt:
            raise TrainingInterrupt(checkpoint=checkpoint_files[-1] if len(checkpoint_files) > 0 else None)

        return checkpoint_files[-1] if len(checkpoint_files) > 0 else None

    def _train_epoch(self, epoch, train_data, criterion):
        total_loss, total_words, total_num_correct = 0, 0, 0
        report_loss, report_tgt_words, report_src_words, report_num_correct = 0, 0, 0, 0
        start = time.time()

        model = self._engine.model
        optimizer = self._engine.optimizer

        # Shuffle mini batch order.
        batch_order = torch.randperm(len(train_data))

        for i in range(len(train_data)):
            batch = train_data[batch_order[i]][:-1]  # exclude original indices

            model.zero_grad()
            outputs = model(batch)
            targets = batch[1][1:]  # exclude <s> from targets
            loss, grad_output, num_correct = self._compute_memory_efficient_loss(outputs, targets, model.generator,
                                                                                 criterion)

            outputs.backward(grad_output)

            # update the parameters
            optimizer.step()

            num_words = targets.data.ne(Constants.PAD).sum()
            report_loss += loss
            report_num_correct += num_correct
            report_tgt_words += num_words
            report_src_words += batch[0][1].data.sum()
            total_loss += loss
            total_num_correct += num_correct
            total_words += num_words

            if i % self.log_interval == -1 % self.log_interval:
                self._logger.log(self._log_level,
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
        if len(history) <= self.min_epochs:
            return False

        current_value = history[-1]
        previous_value = history[-2]

        decrement = (previous_value - current_value) / previous_value

        if 0 < decrement < self.min_perplexity_decrement:
            self._logger.log(self._log_level, 'Terminating training for perplexity threshold reached: '
                                              'new perplexity is %f, while previous was %f (-%.1f%%)'
                             % (current_value, previous_value, decrement * 100))
            return True
        else:
            self._logger.log(self._log_level, 'Continuing training: '
                                              'new perplexity is %f, while previous was %f (-%.1f%%)'
                             % (current_value, previous_value, decrement * 100))
            return False
