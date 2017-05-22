import time

from nmt import Decoder


class OpenNMTDecoder(Decoder):
    def __init__(self, model_path):
        Decoder.__init__(self, model_path)
        # TODO: stub implementation

    def translate(self, text, suggestions=None):
        # TODO: stub implementation
        if (int(time.time()) % 2) == 0:
            raise ArithmeticError("fake exception")
        return reversed(text)

    def _preferred_threads(self):
        # TODO: stub implementation (should be number of GPUs)
        return 4
