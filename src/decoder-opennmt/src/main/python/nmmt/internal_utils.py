import copy

import logging

import time


def opts_object(data=None):
    class _model:
        def __init__(self):
            if data is not None:
                self.__dict__ = copy.deepcopy(data)

        def __setattr__(self, key, value):
            self.__dict__[key] = value

    return _model()


def log_timed_action(logger, op, level=logging.INFO, log_start=True):
    class _logger:
        def __init__(self):
            self.logger = logger
            self.level = level
            self.op = op
            self.start_time = None
            self.log_start = log_start

        def __enter__(self):
            self.start_time = time.time()
            if self.log_start:
                self.logger.log(self.level, '%s... START' % self.op)

        def __exit__(self, exc_type, exc_val, exc_tb):
            self.logger.log(self.level, '%s END %.2fs' % (self.op, time.time() - self.start_time))

    return _logger()
