import glob
import json
import logging
import math
import time
import shutil

import os
from torch import nn, torch
from torch.autograd import Variable

from nmmt.torch_utils import torch_is_multi_gpu, torch_is_using_cuda
from onmt import Constants, Optim


class _Stats(object):
    def __init__(self):
        self.start_time = time.time()
        self.total_loss = 0
        self.src_words = 0
        self.tgt_words = 0
        self.num_correct = 0

    def update(self, loss, src_words, tgt_words, num_correct):
        self.total_loss += loss
        self.src_words += src_words
        self.tgt_words += tgt_words
        self.num_correct += num_correct

    @property
    def accuracy(self):
        return float(self.num_correct) / self.tgt_words

    @property
    def loss(self):
        return self.total_loss / self.tgt_words

    @property
    def perplexity(self):
        return math.exp(self.loss)

    def __str__(self):
        elapsed_time = time.time() - self.start_time

        return '[num_correct: %6.2f; %3.0f src tok; %3.0f tgt tok; ' \
               'acc: %6.2f; ppl: %6.2f; %3.0f src tok/s; %3.0f tgt tok/s]' % (
                   self.num_correct, self.src_words, self.tgt_words,
                   self.accuracy * 100, self.perplexity, self.src_words / elapsed_time, self.tgt_words / elapsed_time
               )


class NMTEngineTrainer:
    class Options(object):
        def __init__(self):
            self.log_level = logging.INFO

            self.bpe_symbols = 32000
            self.max_vocab_size = None  # Unlimited
            self.vocab_pruning_threshold = None  # Skip pruning

            self.batch_size = 64
            self.max_generator_batches = 32  # Maximum batches of words in a seq to run the generator on in parallel.

            self.report_steps = 100  # Log status every 'report_steps' steps
            self.validation_steps = 10000  # compute the validation score every 'validation_steps' steps
            self.checkpoint_steps = 10000  # Drop a checkpoint every 'checkpoint_steps' steps
            self.step_limit = None  # If set, run 'step_limit' steps at most

            self.optimizer = 'sgd'
            self.learning_rate = 1.
            self.max_grad_norm = 5
            self.lr_decay = 0.9
            self.lr_decay_steps = 10000  # decrease learning rate every 'lr_decay_steps' steps
            self.lr_decay_start_at = 50000  # start learning rate decay after 'start_decay_at' steps

            self.n_checkpoints = 20  # checkpoints saved during training and used for termination condition
            self.n_avg_checkpoints = 20  # number of checkpoints to merge at the end of training process

        def load_from_dict(self, d):
            for key in self.__dict__:
                if key in d:
                    self.__dict__[key] = d[key]

        def __str__(self):
            return str(self.__dict__)

        def __repr__(self):
            return str(self.__dict__)

    class State(object):
        def __init__(self, size):
            self.size = size
            self.learning_rate = None
            self.checkpoint = None
            self.history = []

        def empty(self):
            return len(self.history) == 0

        def __len__(self):
            return len(self.history)

        @property
        def last_step(self):
            return self.checkpoint['step'] if self.checkpoint is not None else 0

        def average_perplexity(self):
            if self.empty():
                return 0

            s = 0
            for checkpoint in self.history:
                s += checkpoint['perplexity']

            return s / len(self.history)

        @staticmethod
        def _delete_checkpoint(checkpoint):
            for path in glob.glob(checkpoint['file'] + '.*'):
                os.remove(path)

        def add_checkpoint(self, step, file_path, perplexity):
            self.checkpoint = {
                'step': step,
                'file': file_path,
                'perplexity': perplexity
            }

            self.history.insert(0, self.checkpoint)

            if len(self.history) > self.size:
                self._delete_checkpoint(self.history.pop())

        def save_to_file(self, file_path):
            with open(file_path, 'w') as stream:
                stream.write(json.dumps(self.__dict__, indent=4))

        @staticmethod
        def load_from_file(file_path):
            state = NMTEngineTrainer.State(0)
            with open(file_path, 'r') as stream:
                state.__dict__ = json.loads(stream.read())
            return state

    def __init__(self, engine, options=None, state=None, optimizer=None):
        self._logger = logging.getLogger('nmmt.NMTEngineTrainer')
        self._engine = engine
        self.opts = options if options is not None else NMTEngineTrainer.Options()

        self.state = state if state is not None else NMTEngineTrainer.State(self.opts.n_checkpoints)

        if optimizer is None:
            learning_rate = self.opts.learning_rate if self.state.learning_rate is None else self.state.learning_rate
            optimizer = Optim(self.opts.optimizer, learning_rate, max_grad_norm=self.opts.max_grad_norm,
                              lr_decay=self.opts.lr_decay, lr_start_decay_at=self.opts.lr_decay_start_at)

        self.optimizer = optimizer
        self.optimizer.set_parameters(engine.model.parameters())

    def reset_learning_rate(self, value):
        self.optimizer.lr = value
        self.optimizer.set_parameters(self._engine.model.parameters())

    def _log(self, message):
        if self.opts.log_level > logging.NOTSET:
            self._logger.log(self.opts.log_level, message)

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
        outputs_split = torch.split(outputs, self.opts.max_generator_batches)
        targets_split = torch.split(targets, self.opts.max_generator_batches)

        for i, (out_t, targ_t) in enumerate(zip(outputs_split, targets_split)):
            out_t = out_t.view(-1, out_t.size(2))
            scores_t = generator(out_t)
            loss_t = criterion(scores_t, targ_t.view(-1))
            pred_t = scores_t.max(1)[1]
            num_correct_t = pred_t.data.eq(targ_t.view(-1).data).masked_select(
                targ_t.ne(Constants.PAD).view(-1).data).sum()
            num_correct += num_correct_t
            loss += loss_t.data[0]
            if not evaluation:
                loss_t.div(batch_size).backward()

        grad_output = None if outputs.grad is None else outputs.grad.data
        return loss, grad_output, num_correct

    def _evaluate(self, step, criterion, dataset):
        total_loss = 0
        total_words = 0
        total_num_correct = 0

        model = self._engine.model
        model.eval()

        iterator = dataset.iterator(self.opts.batch_size, shuffle=False, volatile=True)

        for _, batch in iterator:
            # exclude original indices
            batch = batch[:-1]
            outputs = model(batch)
            # exclude <s> from targets
            targets = batch[1][1:]
            loss, _, num_correct = self._compute_memory_efficient_loss(outputs, targets, model.generator,
                                                                       criterion, evaluation=True)
            total_loss += loss
            total_num_correct += num_correct
            total_words += targets.data.ne(Constants.PAD).sum()

        model.train()

        valid_loss, valid_acc = total_loss / total_words, float(total_num_correct) / total_words
        valid_ppl = math.exp(min(valid_loss, 100))

        self._log('Validation at step %d: loss = %g, perplexity = %g, accuracy = %g' % (
            step, valid_loss, valid_ppl, (float(valid_acc) * 100)))

        return valid_ppl

    def _train_step(self, batch, criterion, stats):
        batch = batch[:-1]  # exclude original indices

        self._engine.model.zero_grad()
        outputs = self._engine.model(batch)
        targets = batch[1][1:]  # exclude <s> from targets
        loss, grad_output, num_correct = self._compute_memory_efficient_loss(outputs, targets,
                                                                             self._engine.model.generator, criterion)
        outputs.backward(grad_output)

        # update the parameters
        self.optimizer.step()

        src_words = batch[0][1].data.sum()
        tgt_words = targets.data.ne(Constants.PAD).sum()

        for stat in stats:
            stat.update(loss, src_words, tgt_words, num_correct)

    def train_model(self, train_dataset, valid_dataset=None, save_path=None):
        state_file_path = None if save_path is None else os.path.join(save_path, 'state.json')

        # set the mask to None; required when the same model is trained after a translation
        if torch_is_multi_gpu():
            decoder = self._engine.model.module.decoder
        else:
            decoder = self._engine.model.decoder
        decoder.attn.applyMask(None)

        self._engine.model.train()

        # define criterion of each GPU
        criterion = self._new_nmt_criterion(self._engine.trg_dict.size())

        step = self.state.last_step
        valid_ppl_best = None
        valid_ppl_stalled = 0  # keep track of how many consecutive validations do not improve the best perplexity

        try:
            checkpoint_stats = _Stats()
            report_stats = _Stats()

            iterator = train_dataset.iterator(self.opts.batch_size, loop=True, start_position=step)

            number_of_batches_per_epoch = len(iterator)
            self._log('Number of steps per epoch: %d' % number_of_batches_per_epoch)

            # forcing step limits to be smaller or (at most) equal to the number of steps per epochs
            report_steps = min(self.opts.report_steps, number_of_batches_per_epoch)
            validation_steps = min(self.opts.validation_steps, number_of_batches_per_epoch)
            checkpoint_steps = min(self.opts.checkpoint_steps, number_of_batches_per_epoch)
            lr_decay_steps = min(self.opts.lr_decay_steps, number_of_batches_per_epoch)

            self._log('Initial optimizer parameters: lr = %f, lr_decay = %f'
                      % (self.optimizer.lr, self.optimizer.lr_decay))

            for step, batch in iterator:
                # Steps limit ------------------------------------------------------------------------------------------
                if self.opts.step_limit is not None and step >= self.opts.step_limit:
                    break

                # Run step ---------------------------------------------------------------------------------------------
                self._train_step(batch, criterion, [checkpoint_stats, report_stats])
                step += 1

                epoch = float(step) / number_of_batches_per_epoch

                # Report -----------------------------------------------------------------------------------------------
                if (step % report_steps) == 0:
                    self._log('Step %d (epoch: %.2f): %s ' % (step, epoch, str(report_stats)))
                    report_stats = _Stats()

                if (step % number_of_batches_per_epoch) == 0:
                    self._log('New epoch %d is starting at step %d' % (int(epoch) + 1, step))

                valid_perplexity = None

                # Validation -------------------------------------------------------------------------------------------
                if valid_dataset is not None and (step % validation_steps) == 0:
                    valid_perplexity = self._evaluate(step, criterion, valid_dataset)

                    if valid_ppl_best is None or valid_perplexity < valid_ppl_best:
                        valid_ppl_best = valid_perplexity
                        valid_ppl_stalled = 0
                    else:
                        valid_ppl_stalled += 1

                    if valid_ppl_stalled > 0:
                        self._log('Validation perplexity at step %d (epoch %.2f): '
                                  '%f; current best: %f; stalled %d times'
                                  % (step, epoch, valid_perplexity, valid_ppl_best, valid_ppl_stalled))
                    else:
                        self._log('Validation perplexity at step %d (epoch %.2f): %f; new best: %f'
                                  % (step, epoch, valid_perplexity, valid_ppl_best))

                # Learning rate update --------------------------------------------------------------------------------
                if valid_ppl_stalled > 0:  # activate decay only if validation perplexity starts to increase
                    if step > self.optimizer.lr_start_decay_at:
                        if not self.optimizer.lr_start_decay:
                            self._log('Optimizer learning rate decay activated at step %d (epoch %.2f) '
                                      'with decay value %f; current lr value: %f'
                                      % (step, epoch, self.optimizer.lr_decay, self.optimizer.lr))
                        self.optimizer.lr_start_decay = True

                else:  # otherwise de-activate
                    if self.optimizer.lr_start_decay:
                        self._log('Optimizer learning rate decay de-activated at step %d (epoch %.2f); '
                                  'current lr value: %f' % (step, epoch, self.optimizer.lr))
                    self.optimizer.lr_start_decay = False

                if self.optimizer.lr_start_decay and (step % lr_decay_steps) == 0:
                    self.optimizer.updateLearningRate()
                    self._log('Optimizer learning rate after step %d (epoch %.2f) set to lr = %g'
                              % (step, epoch, self.optimizer.lr))

                # Checkpoint -------------------------------------------------------------------------------------------
                if (step % checkpoint_steps) == 0 and save_path is not None:
                    if valid_perplexity is None and valid_dataset is not None:
                        valid_perplexity = self._evaluate(step, criterion, valid_dataset)

                    checkpoint_ppl = valid_perplexity if valid_perplexity is not None else checkpoint_stats.perplexity
                    checkpoint_file = os.path.join(save_path, 'checkpoint_%d' % step)

                    previous_avg_ppl = self.state.average_perplexity()

                    self._log('Checkpoint at step %d (epoch %.2f): %s' % (step, epoch, str(checkpoint_stats)))
                    self._engine.save(checkpoint_file)
                    self.state.add_checkpoint(step, checkpoint_file, checkpoint_ppl)
                    self.state.learning_rate = self.optimizer.lr
                    self.state.save_to_file(state_file_path)

                    self._log('Checkpoint saved: path = %s ppl = %.2f' % (checkpoint_file, checkpoint_ppl))

                    avg_ppl = self.state.average_perplexity()
                    checkpoint_stats = _Stats()

                    # Terminate policy ---------------------------------------------------------------------------------
                    if len(self.state) >= self.opts.n_checkpoints:
                        perplexity_improves = previous_avg_ppl - avg_ppl > 0.0001

                        self._log('Terminate policy: avg_ppl = %g, previous_avg_ppl = %g, stopping = %r'
                                  % (avg_ppl, previous_avg_ppl, not perplexity_improves))

                        if not perplexity_improves:
                            break
        except KeyboardInterrupt:
            pass

        return self.state

    @staticmethod
    def merge_checkpoints(checkpoint_paths, output_path):
        if checkpoint_paths is None or len(checkpoint_paths) < 1:
            raise ValueError('Need to specify at least one checkpoint, %d provided.' % len(checkpoint_paths))

        if len(checkpoint_paths) < 2:
            shutil.copyfile(checkpoint_paths[0], output_path)

        def __sum(source, destination):
            for key, value in source.items():
                if isinstance(value, dict):
                    node = destination.setdefault(key, {})
                    __sum(value, node)
                else:
                    if isinstance(value, torch.FloatTensor):
                        destination[key] = torch.add(destination[key], 1.0, value)

            return destination

        def __divide(source, denominator):
            for key, value in source.items():
                if isinstance(value, dict):
                    node = source.setdefault(key, {})
                    __divide(node, denominator)
                else:
                    if isinstance(value, torch.FloatTensor):
                        source[key] = torch.div(value, denominator)

            return source

        output_checkpoint = torch.load(checkpoint_paths[0])

        for checkpoint_path in checkpoint_paths[1:]:
            checkpoint = torch.load(checkpoint_path)
            output_checkpoint = __sum(checkpoint, output_checkpoint)

        output_checkpoint = __divide(output_checkpoint, len(checkpoint_paths))

        torch.save(output_checkpoint, output_path)
