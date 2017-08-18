import torch.optim as optim
from torch.nn.utils import clip_grad_norm

import logging

class Optim(object):

    def set_parameters(self, params):
        self.params = list(params)  # careful: params may be a generator
        if self.method == 'sgd':
            self.optimizer = optim.SGD(self.params, lr=self.lr)
        elif self.method == 'adagrad':
            self.optimizer = optim.Adagrad(self.params, lr=self.lr)
        elif self.method == 'adadelta':
            self.optimizer = optim.Adadelta(self.params, lr=self.lr)
        elif self.method == 'adam':
            self.optimizer = optim.Adam(self.params, lr=self.lr)
        else:
            raise RuntimeError("Invalid optim method: " + self.method)

    def __init__(self, method, lr, max_grad_norm,
                 lr_decay=1, start_decay_at=None):
        self._logger = logging.getLogger('ommt.Optim')
        self._log_level = logging.INFO

        self.last_ppl = None
        self.lr = lr
        self.max_grad_norm = max_grad_norm
        self.method = method
        self.lr_decay = lr_decay
        self.start_decay_at = start_decay_at
        self.start_decay = False

        self._logger.log(self._log_level, 'self.start_decay_at:%s self.start_decay:%s self.lr_decay:%s last_ppl:%s' % (repr(self.start_decay_at), repr(self.start_decay), repr(self.lr_decay), repr(self.last_ppl)) )

    def step(self):
        "Compute gradients norm."
        if self.max_grad_norm:
            clip_grad_norm(self.params, self.max_grad_norm)
        self.optimizer.step()

    def updateLearningRate(self, ppl, epoch):
        """
        Decay learning rate
        if perplexity on validation does not improve
        or if we hit the start_decay_at limit.
        """

        self._logger.log(self._log_level, 'epoch:%d self.start_decay_at:%s self.start_decay:%s self.lr:%s ppl:%s self.last_ppl:%s' % (epoch, repr(self.start_decay_at), repr(self.start_decay),repr(self.lr),repr(ppl),repr(self.last_ppl)) )
        if self.start_decay_at is not None and epoch >= self.start_decay_at:
            self.start_decay = True

        if self.last_ppl is not None and ppl > self.last_ppl:
            self.start_decay = True

        self._logger.log(self._log_level, 'hence epoch:%d self.start_decay:%s ' % (epoch, repr(self.start_decay)) )
        if self.start_decay:
            self.lr = self.lr * self.lr_decay
            # print("Decaying learning rate to %g" % self.lr)
        self._logger.log(self._log_level, 'hence epoch:%d self.lr:%s' % (epoch, repr(self.lr)) )

        self.last_ppl = ppl
        self.optimizer.param_groups[0]['lr'] = self.lr
