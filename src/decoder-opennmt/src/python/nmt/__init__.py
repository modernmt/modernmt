
class Suggestion:
    def __init__(self, source, target, score):
        self.source = source
        self.target = target
        self.score = score


class Decoder:
    def __init__(self, model_path, threads=None):
        """
        Creates a new instance of an NMT decoder
        :param model_path: path to the decoder model file/folder
        :type model_path: basestring
        """
        self._model_path = model_path
        self._number_of_threads = threads if threads is not None else None

    @property
    def number_of_threads(self):
        if self._number_of_threads is None:
            self._number_of_threads = self._preferred_threads()

        return self._number_of_threads

    def _preferred_threads(self):
        """
        :return: the number of preferred threads
        """
        raise NotImplementedError('abstract method')

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
        pass
