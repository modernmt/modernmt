import os
import time

from cli.mmt import BilingualCorpus
from cli.mmt.cluster import ClusterNode
from cli.mmt.engine import EngineBuilder
from cli.mmt.processing import TrainingPreprocessor

__author__ = 'Davide Caroselli'


def _pretty_print_time(elapsed):
    elapsed = int(elapsed)
    parts = []

    if elapsed > 86400:  # days
        d = int(elapsed / 86400)
        elapsed -= d * 86400
        parts.append('%dd' % d)
    if elapsed > 3600:  # hours
        h = int(elapsed / 3600)
        elapsed -= h * 3600
        parts.append('%dh' % h)
    if elapsed > 60:  # minutes
        m = int(elapsed / 60)
        elapsed -= m * 60
        parts.append('%dm' % m)
    parts.append('%ds' % elapsed)

    return ' '.join(parts)


class Training(EngineBuilder.Listener):
    @staticmethod
    def phrase_based(name, source_lang, target_lang, roots, debug, steps, split_trainingset, max_training_words):
        from cli.mmt.phrasebased import PhraseBasedEngineBuilder
        builder = PhraseBasedEngineBuilder(name, source_lang, target_lang, roots, debug,
                                           steps, split_trainingset, max_training_words)
        return Training(builder)

    @staticmethod
    def neural(name, source_lang, target_lang, roots, debug, steps, split_trainingset, validation_corpora,
               checkpoint, metadata, max_training_words, gpus, training_args):
        from cli.mmt.neural import NeuralEngineBuilder
        builder = NeuralEngineBuilder(name, source_lang, target_lang, roots, debug, steps, split_trainingset,
                                      validation_corpora, checkpoint, metadata, max_training_words, gpus, training_args)
        return Training(builder)

    def __init__(self, builder, line_len=70):
        EngineBuilder.Listener.__init__(self)

        self._builder = builder
        self._line_len = line_len

        self._engine_name = None
        self._steps_count = 0
        self._current_step_num = 0

        self._step_start_time = 0
        self._start_time = 0

    def start(self):
        self._builder.build(self)

    def resume(self):
        self._builder.resume(self)

    # EngineBuilder.Listener

    def on_hw_constraint_violated(self, message):
        print '\033[91mWARNING\033[0m: %s\n' % message

    def on_training_begin(self, steps, engine, bilingual_corpora, monolingual_corpora):
        self._steps_count = len(steps)
        self._start_time = time.time()
        self._engine_name = engine.name if engine.name != 'default' else None

        print '\n=========== TRAINING STARTED ===========\n'
        print 'ENGINE:  %s' % engine.name
        print 'BILINGUAL CORPORA: %d documents' % len(bilingual_corpora)
        print 'MONOLINGUAL CORPORA: %d documents' % len(monolingual_corpora)
        print 'LANGS:   %s > %s' % (engine.source_lang, engine.target_lang)
        print

    def on_step_begin(self, step, name):
        self._current_step_num += 1

        message = 'INFO: (%d of %d) %s... ' % (self._current_step_num, self._steps_count, name)
        print message.ljust(self._line_len),

        self._step_start_time = time.time()

    def on_step_end(self, step, name):
        elapsed_time = time.time() - self._step_start_time
        print 'DONE (in %s)' % _pretty_print_time(elapsed_time)

    def on_training_end(self, engine):
        print '\n=========== TRAINING SUCCESS ===========\n'
        print 'You can now start, stop or check the status of the server with command:'
        print '\t./mmt start|stop|status ' + ('' if self._engine_name is None else '-e %s' % self._engine_name)
        print


class Tuning(ClusterNode.TuneListener):
    @staticmethod
    def neural(max_lines=None, lr_delta=0.1, max_epochs=10, gpus=None):
        return Tuning(lambda node, corpora, listener, debug:
                      node.nmt_tune(corpora=corpora, debug=debug, listener=listener,
                                    max_lines=max_lines, lr_delta=lr_delta, max_epochs=max_epochs, gpus=gpus), False)

    @staticmethod
    def phrase_based(context_enabled=True, random_seeds=True, max_iterations=25, accuracy='default'):
        if accuracy == 'fast':
            accuracy = 10
        elif accuracy == 'best':
            accuracy = None
        else:
            accuracy = 1

        return Tuning(lambda node, corpora, listener, debug:
                      node.phrase_based_tune(corpora=corpora, debug=debug, listener=listener,
                                             context_enabled=context_enabled, random_seeds=random_seeds,
                                             max_iterations=max_iterations, early_stopping_value=accuracy), True)

    def __init__(self, start_fn, node_running, line_len=70):
        ClusterNode.TuneListener.__init__(self)

        self._start_fn = start_fn
        self._node_running = node_running

        self.line_len = line_len
        self._steps_count = 0
        self._current_step = 0
        self._api_base_path = None

        self._start_time = 0

    def requires_node_running(self):
        return self._node_running

    def requires_node_stop(self):
        return not self._node_running

    def start(self, node, corpora, debug=False):
        if corpora is None:
            corpora = BilingualCorpus.list(os.path.join(node.engine.data_path, TrainingPreprocessor.DEV_FOLDER_NAME))

        self._start_fn(node, corpora, self, debug)

    def on_tuning_begin(self, corpora, node, steps_count):
        self._steps_count = steps_count

        print '\n============ TUNING STARTED ============\n'
        print 'ENGINE:  %s' % node.engine.name
        print 'CORPORA: %s (%d documents)' % (corpora[0].get_folder(), len(corpora))
        print 'LANGS:   %s > %s' % (node.engine.source_lang, node.engine.target_lang)
        print

    def on_step_begin(self, step):
        self._current_step += 1
        message = 'INFO: (%d of %d) %s... ' % (self._current_step, self._steps_count, step)
        print message.ljust(self.line_len),

        self._start_time = time.time()

    def on_step_end(self, _):
        elapsed_time = int(time.time() - self._start_time)
        print 'DONE (in %s)' % _pretty_print_time(elapsed_time)

    def on_tuning_end(self, node, final_bleu):
        print '\n============ TUNING SUCCESS ============\n'
        print '\nFinal BLEU: %.2f\n' % (final_bleu * 100.)

        if node.api is not None:
            print 'You can try the API with:'
            print '\tcurl "%s/translate?q=hello+world&context=computer"' % node.api.base_path + ' | python -mjson.tool'
            print
