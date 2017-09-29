import torch.optim as optim
from torch.nn.utils import clip_grad_norm

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
                 lr_decay=1, lr_start_decay_at=None):

        self.last_ppl = None
        self.max_grad_norm = max_grad_norm
        self.method = method
        self.lr = lr
        self.lr_decay = lr_decay
        self.lr_start_decay_at = lr_start_decay_at
        self.lr_start_decay = False

    def step(self):
        "Compute gradients norm."
        if self.max_grad_norm:
            clip_grad_norm(self.params, self.max_grad_norm)
        self.optimizer.step()

    def updateLearningRate(self):
        """
        Decay learning rate
        if perplexity on validation does not improve
        or if we hit the start_decay_at limit.
        """

        if self.lr_start_decay:
            self.lr = self.lr * self.lr_decay

        self.optimizer.param_groups[0]['lr'] = self.lr
