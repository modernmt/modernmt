import logging
import math

import numpy as np
import torch
from fairseq import optim, utils
from fairseq.data import LanguagePairDataset

from mmt import is_fairseq_0_10


class TuningOptions(object):
    def __init__(self):
        self.memory_suggestions_limit = None  # Ignore
        self.memory_query_min_results = None  # Ignore
        self.tuning_max_epochs = 4
        self.tuning_max_learning_rate = .0001
        self.tuning_max_batch_size = 4000

    def __str__(self):
        return str(self.__dict__)


class TuningDataset(torch.utils.data.Dataset):
    def __init__(self, lines, dictionary, append_eos=True, reverse_order=False):
        self._tokens_list = [
            dictionary.encode_line(line.strip(), line_tokenizer=dictionary.tokenize,
                                   add_if_not_exist=False, append_eos=append_eos,
                                   reverse_order=reverse_order).long()
            for line in lines]
        self._sizes = np.array([len(tokens) for tokens in self._tokens_list])
        self._size = len(self._tokens_list)

    def __getitem__(self, i):
        if i < 0 or i >= self._size:
            raise IndexError

        return self._tokens_list[i]

    def __len__(self):
        return self._size

    @property
    def sizes(self):
        return self._sizes

    @property
    def tokens(self):
        return self._tokens_list


class Tuner(object):
    def __init__(self, args, task, model, tuning_ops, device=None):
        self._logger = logging.getLogger('Tuner')

        self._cuda = torch.cuda.is_available() and device is not None
        self._tuning_ops = tuning_ops
        self._args = args
        self._task = task

        self._model = model

        self._criterion = task.build_criterion(args)
        if self._cuda:
            self._criterion = self._criterion.cuda()

        self.__train_step_kwargs = {'ignore_grad': False}
        self.__dataset_kwargs = {'left_pad_source': True, 'left_pad_target': False}

        if is_fairseq_0_10():
            self.__train_step_kwargs['update_num'] = 1
        else:
            self.__dataset_kwargs['max_source_positions'] = 4096
            self.__dataset_kwargs['max_target_positions'] = 4096

    def dataset(self, src_samples, tgt_samples, dictionary):
        src_dataset = TuningDataset(src_samples, dictionary)
        tgt_dataset = TuningDataset(tgt_samples, dictionary)

        return LanguagePairDataset(
            src_dataset, src_dataset.sizes, dictionary,
            tgt_dataset, tgt_dataset.sizes, dictionary,
            **self.__dataset_kwargs
        )

    def _build_optimizer(self):
        params = list(filter(lambda p: p.requires_grad, self._model.parameters()))
        if self._args.fp16:
            if self._cuda and torch.cuda.get_device_capability(0)[0] < 7:
                print('| WARNING: your device does NOT support faster training with --fp16, '
                      'please switch to FP32 which is likely to be faster')
            if self._args.memory_efficient_fp16:
                return optim.MemoryEfficientFP16Optimizer.build_optimizer(self._args, params)
            else:
                return optim.FP16Optimizer.build_optimizer(self._args, params)
        else:
            if self._cuda and torch.cuda.get_device_capability(0)[0] >= 7:
                print('| NOTICE: your device may support faster training with --fp16')
            return optim.build_optimizer(self._args, params)

    def estimate_tuning_parameters(self, suggestions):
        # it returns an actual learning_rate and epochs based on the quality of the suggestions
        # it is assured that at least one suggestion is provided (hence, len(suggestions) > 0)
        average_score = 0.0
        for suggestion in suggestions:
            average_score += suggestion.score
        average_score /= len(suggestions)

        # Empirically defined function to make the number of epochs dependent to the quality of the suggestions
        # epochs = max_epochs * average_score + 1
        # where max_epochs is the maximum number of epochs allowed;
        # hence epochs = max_epochs only with perfect suggestions
        # and epochs = 0, when the average_score is close to 0.0 (<1/max_epochs)
        tuning_epochs = int(self._tuning_ops.tuning_max_epochs * average_score)

        # Empirically defined function to make the learning rate dependent to the quality of the suggestions
        # lr = max_lr * sqrt(average_score)
        # hence lr = max_lr only with perfect suggestions
        # and lr = 0, when the average_score is exactly 0.0
        tuning_learning_rate = self._tuning_ops.tuning_max_learning_rate * math.sqrt(average_score)

        return tuning_epochs, tuning_learning_rate

    def tune(self, dataset, num_iterations, lr):
        if len(dataset) == 0:
            return

        epoch_itr = self._task.get_batch_iterator(
            dataset=dataset,
            max_tokens=self._tuning_ops.tuning_max_batch_size,
            max_sentences=None,
            max_positions=(4096, 4096),
            ignore_invalid_inputs=True,
            seed=1, num_shards=1, shard_id=0,
        )

        optimizer = self._build_optimizer()

        for step in range(num_iterations):
            for sample in epoch_itr.next_epoch_itr(shuffle=False, fix_batches_to_gpus=False):
                if len(sample) == 0:
                    continue

                if self._cuda:
                    sample = utils.move_to_cuda(sample)
                optimizer.set_lr(lr)
                self._train_step(optimizer, sample, step)
                del sample

    def _train_step(self, optimizer, sample, step=0):
        """Do forward, backward and parameter update."""
        seed = self._args.seed + step
        torch.manual_seed(seed)
        if self._cuda:
            torch.cuda.manual_seed(seed)

        self._model.train()
        self._criterion.train()
        optimizer.zero_grad()

        self._model.accumulate_grads = False

        try:
            # forward and backward
            self._task.train_step(sample, self._model, self._criterion, optimizer, **self.__train_step_kwargs)
        except RuntimeError as e:
            if 'out of memory' in str(e):
                self._logger.warning('ran out of memory, skipping batch')
                optimizer.zero_grad()
            else:
                raise e

        try:
            optimizer.step()
        except OverflowError as e:
            self._logger.warning('overflow detected, ' + str(e))
