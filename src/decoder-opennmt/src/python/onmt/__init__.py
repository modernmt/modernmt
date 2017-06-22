import Models
import Constants

from Dataset import Dataset
from Beam import Beam
from Dict import Dict
from Optim import Optim
from Trainer import Trainer
from Translator import Translator
from Options import Options


class Suggestion:
    def __init__(self, source, target, score):
        self.source = source
        self.target = target
        self.score = score


class MMTDecoder:
    def __init__(self, model_path):
        """
        Creates a new instance of an NMT decoder
        :param model_path: path to the decoder model file/folder
        :type model_path: basestring
        """
        self._model_path = model_path

    def translate(self, text, suggestions=None):
        """
        Returns a translation for the given Translation Request
        :param text: the tokenized text to be translated
        :type text: list

        :param suggestions: a collection of suggestions in order to adapt the translation
        :type suggestions: list

        :return: the best translation as a list of tokens
        """
        raise NotImplementedError('abstract method')

    def close(self):
        """
        Called before destroying this object.
        The decoder should release any resource acquired during execution.
        """
        raise NotImplementedError('abstract method')
