import logging
import os

from nmmt.NMTEngine import NMTEngine
from nmmt.internal_utils import log_timed_action
from nmmt.torch_utils import torch_setup

import ConfigParser


class UnsupportedLanguageException(BaseException):
    def __init__(self, source_language, target_language):
        self.message = "No engine and text processors found for %s -> %s." % (source_language, target_language)


class NMTDecoder:
    def __init__(self, model_path, gpu_id=None, random_seed=None):
        torch_setup(gpus=[gpu_id] if gpu_id is not None else None, random_seed=random_seed)

        self._logger = logging.getLogger('nmmt.NMTDecoder')
        self._engines, self._engines_checkpoint = {}, {}
        self._cold_engines, self._warm_engines, self._hot_engines = [], [], []
        self._cold_size, self._warm_size, self._hot_size = 1000, 5, 2

        # create and put in its map a TextProcessor and a NMTEngine for each line in model.conf
        settings = ConfigParser.ConfigParser()
        settings.read(os.path.join(model_path, 'model.conf'))

        try:
            self._cold_size = settings.getint('setting', 'cold_size')
        except ConfigParser.NoSectionError:
            pass
        except ConfigParser.NoOptionError:
            pass

        try:
            self._warm_size = settings.getint('setting', 'warm_size')
        except ConfigParser.NoSectionError:
            pass
        except ConfigParser.NoOptionError:
            pass

        try:
            self._hot_size = settings.getint('setting', 'hot_size')
        except ConfigParser.NoSectionError:
            pass
        except ConfigParser.NoOptionError:
            pass


        self._logger.info("Hot model size:%d" % (self._hot_size))
        self._logger.info("Warm model size:%d" % (self._warm_size))
        self._logger.info("Cold model size:%d" % (self._cold_size))

        if not settings.has_section('models'):
            raise Exception('no model specified in %s' % os.path.join(model_path, 'model.conf'))

        for key, model_name in settings.items('models'):

            model_file = os.path.join(model_path, model_name)

            # the running state of the engines depend on their position in the configration file:
            # the higher in the list the better its state
            with log_timed_action(self._logger, 'Loading "%s" model from checkpoint' % key):
                self._engines[key] = NMTEngine.load_from_checkpoint(model_file)

                if len(self._hot_engines) < self._hot_size:
                    # the engine is automatically created in COLD state
                    # and now it is upgraded to HOT
                    self._engines[key].running_state = NMTEngine.HOT
                    self._hot_engines.append(key)
                elif len(self._warm_engines) < self._warm_size:
                    # the engine is automatically created in WARM state
                    self._engines[key].running_state = NMTEngine.WARM
                    self._warm_engines.append(key)
                else:
                    self._cold_engines.append(key)

            self._logger.info("Model %s loaded, its running state is %s" % (key, self._engines[key].running_state))

        self._logger.info("Hot models:%s" % self._hot_engines)
        self._logger.info("Warm models:%s" % self._warm_engines)
        self._logger.info("Cold models:%s" % self._cold_engines)
        # Public-editable options
        self.beam_size = 5
        self.max_sent_length = 160

    def get_engine(self, source_lang, target_lang, variant=None):
        key = source_lang + '__' + target_lang
        if variant is not None:
            key += "__" + variant
        if key not in self._engines:
            return None

        current_engine = self._engines[key]

        if current_engine.running_state != NMTEngine.HOT: # upgrade the engine to HOT

            if current_engine.running_state == NMTEngine.WARM:
                self._warm_engines.remove(key)
            else:
                self._cold_engines.remove(key)

            if len(self._hot_engines) >= self._hot_size: # no more space for hot engines
                if len(self._warm_engines) >= self._warm_size: # no more space for warm engines
                    # move the last warm engine to cold
                    e = self._warm_engines.pop()
                    self._engines[e].running_state = NMTEngine.COLD
                    self._cold_engines.insert(0, e)

                # move the last hot engine to warm, which has at least one space
                e = self._hot_engines.pop()
                self._engines[e].running_state = NMTEngine.WARM
                self._warm_engines.insert(0, e)

            current_engine.running_state = NMTEngine.HOT
            self._hot_engines.insert(0, key)

        return current_engine

    def translate(self, source_lang, target_lang, text, suggestions=None, n_best=1,
                  tuning_epochs=None, tuning_learning_rate=None, variant=None):
        # (0) Get NMTEngine for current key (direction and variant if specified);
        #     and if needed it upgrades the engine to running state HOT
        #     if it does not exist, raise an exception
        engine = self.get_engine(source_lang, target_lang, variant)

        reset_model = False

        # (1) Tune engine if suggestions provided
        if suggestions is not None and len(suggestions) > 0:
            engine.tune(suggestions, epochs=tuning_epochs, learning_rate=tuning_learning_rate)
            reset_model = True

        # (2) Translate and compute word alignment
        result = engine.translate(text, n_best=n_best, beam_size=self.beam_size, max_sent_length=self.max_sent_length)

        # (3) Reset model if needed
        if reset_model:
            engine.reset_model()

        return result
