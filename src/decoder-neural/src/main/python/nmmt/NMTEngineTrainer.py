import logging
import math
import os
import time

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

            self.batch_size = 64
            self.max_generator_batches = 32  # Maximum batches of words in a seq to run the generator on in parallel.

            self.report_steps = 1000  # Log status every 'report_steps' steps
            self.validation_steps = 10000  # compute the validation score every 'validation_steps' steps
            self.checkpoint_steps = 10000  # Drop a checkpoint every 'checkpoint_steps' steps
            self.steps_limit = None  # If set, run 'steps_limit' steps at most

            self.optimizer = 'sgd'
            self.learning_rate = 1.
            self.max_grad_norm = 5
            self.lr_decay = 0.9
            self.lr_decay_steps = 40  # decrease learning rate every 'lr_decay_steps' steps
            self.lr_decay_start_at = 50000  # start learning rate decay after 'start_decay_at' steps

            self.early_stop = 5 # terminate training if validations is stalled for 'early_stop' times

    class State(object):
        def __init__(self):
            #TODO: what happens if this number becomes too large: overflow problem?
            self.step = 0
            self.file_path = None
            self.perplexity = None

        def checkpoint(self, step, file_path, perplexity):
            self.step = step
            self.file_path = file_path
            self.perplexity = perplexity
#            raise NotImplementedError

        def save_to_file(self, file_path):
            torch.save(self.__dict__, file_path)

        @staticmethod
        def load_from_file(file_path):
            state = NMTEngineTrainer.State()
            state.__dict__ = torch.load(file_path)
            return state

    def __init__(self, engine, options=None, optimizer=None):
        self._logger = logging.getLogger('nmmt.NMTEngineTrainer')
        self._engine = engine
        self.opts = options if options is not None else NMTEngineTrainer.Options()

        if optimizer is None:
            optimizer = Optim(self.opts.optimizer, self.opts.learning_rate, max_grad_norm=self.opts.max_grad_norm,
                              lr_decay=self.opts.lr_decay, lr_start_decay_at=self.opts.lr_decay_start_at)
            optimizer.set_parameters(engine.model.parameters())
        self.optimizer = optimizer

    def reset_learning_rate(self, value):
        self.optimizer.lr = value
        self.optimizer.set_parameters(self._engine.model.parameters())

    def _log(self, message):
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
            num_correct_t = pred_t.data.eq(targ_t.data).masked_select(targ_t.ne(Constants.PAD).data).sum()
            num_correct += num_correct_t
            loss += loss_t.data[0]
            if not evaluation:
                loss_t.div(batch_size).backward()

        grad_output = None if outputs.grad is None else outputs.grad.data
        return loss, grad_output, num_correct

    def _evaluate(self, criterion, dataset):
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
        return total_loss / total_words, float(total_num_correct) / total_words

    def train_model(self, train_dataset, valid_dataset=None, save_path=None, state=None):
        state_file_path = None if save_path is None else os.path.join(save_path, 'state.dat')

        # resume training if state file found
        if state is None:
            if state_file_path is None or not os.path.isfile(state_file_path):
                state = NMTEngineTrainer.State()
            else:
                state = NMTEngineTrainer.State.load_from_file(state_file_path)

        # set the mask to None; required when the same model is trained after a translation
        if torch_is_multi_gpu():
            decoder = self._engine.model.module.decoder
        else:
            decoder = self._engine.model.decoder
        decoder.attn.applyMask(None)
        self._engine.model.train()

        # define criterion of each GPU
        criterion = self._new_nmt_criterion(self._engine.trg_dict.size())

        run_validation = False
        run_dump = False
        run_lr_update = False
        run_report = False
        terminate = False

        valid_ppl_history = {0:None}   # key=0 refers to the best value
        valid_ppl_stalled = 0   # keep track of how many consecutive validations ddo not improve the actual best validation

        try:
            checkpoint_stats = _Stats()
            mini_batch_stats = _Stats()

            for step, batch in train_dataset.iterator(self.opts.batch_size, loop=True, start_position=state.step):


                if step > 0:
                    # activate report
                    if step % self.opts.report_steps == 0:
                        run_report = True

                    # activate learning rate update
                    if self.optimizer.lr_start_decay == True and step % self.opts.lr_decay_steps == 0:
                        run_lr_update = True

                    # activate validation
                    if step % self.opts.validation_steps == 0 and valid_dataset is not None:
                        run_validation = True

                    # activate dump
                    if step % self.opts.checkpoint_steps == 0:
                        run_dump = True

                    if self.opts.steps_limit is not None and step >= self.opts.steps_limit:
                        terminate = True



#                self._log('step:%d run_report:%s run_lr_update:%s run_validation:%s run_dump:%s terminate:%s ' % (step, repr(run_report), repr(run_lr_update), repr(run_validation), repr(run_dump), repr(terminate) ))


                if terminate:
                    break

                self._train_step(batch, criterion, [checkpoint_stats, mini_batch_stats])

                if run_report:
                    self._log('Step %d: %s' % (step, str(mini_batch_stats)))
                    mini_batch_stats = _Stats()


                valid_ppl = None
                if run_validation:
                    valid_loss, valid_acc = self._evaluate(criterion, valid_dataset)
                    valid_ppl = math.exp(min(valid_loss, 100))

                    self._log('Validation Set at step %d: loss = %g, perplexity = %g, accuracy = %g' % (
                        step, valid_loss, valid_ppl, (float(valid_acc) * 100)))

                    valid_ppl_history[step] = valid_ppl

                    # check and optionally update best value
                    if valid_ppl_history[0] is None or valid_ppl < valid_ppl_history[0]:
                        valid_ppl_history[0] = valid_ppl
                        valid_ppl_stalled = 0
                    else:
                        valid_ppl_stalled += 1


                    # TODO: compute the policy to start the learning rate_decay
                    # it is also possible to activate LR decay according to the validation scores
                    if step > self.optimizer.lr_start_decay_at:
                        self.optimizer.lr_start_decay = True

                    self._log('step:%s valid_ppl:%f valid_ppl_stalled:%d self.optimizer.lr_start_decay_at:%d self.optimizer.lr_start_decay:%s' % (step,valid_ppl, valid_ppl_stalled, self.optimizer.lr_start_decay_at, repr(self.optimizer.lr_start_decay)))

                    # TODO: compute the policy to terminate the training
                    # terminate training if validation perplexity does not improve for 'self.opts.early_stop' consecutive times
                    if valid_ppl_stalled >= self.opts.early_stop:
                        terminate = True


                if run_lr_update:
                    # Update the learning rate
                    self.optimizer.updateLearningRate(valid_ppl, step)

                    self._log('step:%d lr:%g' % (step, self.optimizer.lr))


                if run_dump:

                    # if validation has been done  in this exact step
                    # just take the value
                    # else re-validate
                    if valid_ppl is None  and valid_dataset is not None:
                        valid_loss, valid_acc = self._evaluate(criterion, valid_dataset)
                        valid_ppl = math.exp(min(valid_loss, 100))

                        self._log('Validation Set at step %d: loss = %g, perplexity = %g, accuracy = %g' % (
                            step, valid_loss, valid_ppl, (float(valid_acc) * 100)))

                        valid_ppl_history[step] = valid_ppl


                    checkpoint_ppl = valid_ppl

                    if save_path is not None:
                        self._log('Checkpoint %d: %s' % (step, str(checkpoint_stats)))

                        if checkpoint_ppl is not None:
                            checkpoint_file = 'checkpoint_s%d_ppl%.2f' % (step, checkpoint_ppl)
                        else:
                            checkpoint_file = 'checkpoint_s%d_pplUndef' % (step)
                        checkpoint_file = os.path.join(save_path, checkpoint_file)

                        self._engine.save(checkpoint_file)

                        state.checkpoint(step, checkpoint_file, checkpoint_ppl)
                        state.save_to_file(state_file_path)

                        self._logger.info('step:%d type(state): %s state: %s' % (step, type(state), state))

                    checkpoint_stats = _Stats()

                run_report = False
                run_validation = False
                run_lr_update = False
                run_dump = False

                state.step = step + 1  # next step


        except KeyboardInterrupt:
            pass

        return state

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
