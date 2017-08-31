import logging
import os

import time

import math

import copy
import torch.cuda.random as random
from torch import nn, torch
from torch.autograd import Variable

from onmt import Models, Optim, Constants


class TrainingInterrupt(Exception):
    def __init__(self, checkpoint):
        super(TrainingInterrupt, self).__init__()
        self.checkpoint = checkpoint


class NMTEngineTrainer:
    @staticmethod
    def new_instance(src_dict, trg_dict, model_params=None, random_seed=None, gpu_ids=None, init_value=0.1):
        if model_params is None:
            from nmmt import NMTEngine
            model_params = NMTEngine.Parameters()

        if gpu_ids is not None and len(gpu_ids) > 0:
            torch.cuda.set_device(gpu_ids[0])

        encoder = Models.Encoder(model_params, src_dict)
        decoder = Models.Decoder(model_params, trg_dict)
        generator = nn.Sequential(nn.Linear(model_params.rnn_size, trg_dict.size()), nn.LogSoftmax())

        model = Models.NMTModel(encoder, decoder)

        if gpu_ids is not None and len(gpu_ids) > 0:
            model.cuda()
            generator.cuda()

            if len(gpu_ids) > 1:
                model = nn.DataParallel(model, device_ids=gpu_ids, dim=1)
                generator = nn.DataParallel(generator, device_ids=gpu_ids, dim=0)
        else:
            model.cpu()
            generator.cpu()

        model.generator = generator

        for p in model.parameters():
            p.data.uniform_(-init_value, init_value)

        optim = Optim(model_params.optim, model_params.learning_rate, model_params.max_grad_norm,
                      lr_decay=model_params.learning_rate_decay, start_decay_at=model_params.start_decay_at)
        optim.set_parameters(model.parameters())

        return NMTEngineTrainer(model, optim, src_dict, trg_dict,
                                model_params=model_params, gpu_ids=gpu_ids, random_seed=random_seed)

    def __init__(self, model, optim, src_dict, trg_dict, model_params=None, gpu_ids=None, random_seed=None):
        self._logger = logging.getLogger('nmmt.NMTEngineTrainer')
        self._log_level = logging.INFO

        self._model = model
        self._optim = optim
        self._src_dict = src_dict
        self._trg_dict = trg_dict
        self._model_params = model_params
        self._gpu_ids = gpu_ids

        if random_seed is not None:
            torch.manual_seed(random_seed)
            random.manual_seed_all(random_seed)

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

    def _new_nmt_criterion(self, vocab_size):
        weight = torch.ones(vocab_size)
        weight[Constants.PAD] = 0
        criterion = nn.NLLLoss(weight, size_average=False)
        if self._gpu_ids is not None:
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

        self._model.eval()
        for i in range(len(data)):
            # exclude original indices
            batch = data[i][:-1]
            outputs = self._model(batch)
            # exclude <s> from targets
            targets = batch[1][1:]
            loss, _, num_correct = self._compute_memory_efficient_loss(outputs, targets, self._model.generator,
                                                                       criterion, evaluation=True)
            total_loss += loss
            total_num_correct += num_correct
            total_words += targets.data.ne(Constants.PAD).sum()

        self._model.train()
        return total_loss / total_words, float(total_num_correct) / total_words

    def train_model(self, train_data, valid_data=None, save_path=None, save_epochs=5):
        multi_gpu = self._gpu_ids is not None and len(self._gpu_ids) > 1

        # set the mask to None; required when the same model is trained after a translation
        if multi_gpu:
            decoder = self._model.module.decoder
        else:
            decoder = self._model.decoder
        decoder.attn.applyMask(None)
        self._model.train()

        # define criterion of each GPU
        criterion = self._new_nmt_criterion(self._trg_dict.size())

        perplexity_history = []
        checkpoint_files = []
        valid_acc, valid_ppl = None, None

        try:
            self._logger.log(self._log_level, 'Optim options:%s' % (repr(self._optim)))
            for epoch in range(self.start_epoch, self.max_epochs + 1):
                self._logger.log(self._log_level, 'Training epoch %g... START' % epoch)
                start_time_epoch = time.time()

                #  (1) train for one epoch on the training set
                train_loss, train_acc = self._train_epoch(epoch, train_data, self._model, criterion, self._optim)
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

                    self._optim.updateLearningRate(valid_loss, epoch)

                    self._logger.log(self._log_level,
                                     "trainModel Epoch %g Decaying learning rate to %g" % (epoch, self._optim.lr))

                if save_path is not None and save_epochs > 0:
                    if len(checkpoint_files) > 0 and len(checkpoint_files) > save_epochs - 1:
                        os.remove(checkpoint_files.pop(0))

                    opt_state_dict = self._model_params.__dict__
                    model_state_dict = self._model.module.state_dict() if multi_gpu else self._model.state_dict()
                    model_state_dict = {k: v for k, v in model_state_dict.items() if 'generator' not in k}
                    generator_state_dict = self._model.generator.module.state_dict() if multi_gpu \
                        else self._model.generator.state_dict()

                    #  (4) drop a checkpoint
                    checkpoint = {
                        'model': model_state_dict,
                        'generator': generator_state_dict,
                        'dicts': {'src': self._src_dict, 'tgt': self._trg_dict},
                        'opt': copy.deepcopy(opt_state_dict),
                        'epoch': epoch,
                        'optim': self._optim
                    }

                    if valid_acc is not None:
                        checkpoint_file = \
                            '%s_acc_%.2f_ppl_%.2f_e%d.pt' % (save_path, 100 * valid_acc, valid_ppl, epoch)
                    else:
                        checkpoint_file = '%s_acc_NA_ppl_NA_e%d.pt' % (save_path, epoch)

                    torch.save(checkpoint, checkpoint_file)
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

    def _train_epoch(self, epoch, train_data, model, criterion, optim):
        total_loss, total_words, total_num_correct = 0, 0, 0
        report_loss, report_tgt_words, report_src_words, report_num_correct = 0, 0, 0, 0
        start = time.time()

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
            optim.step()

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
