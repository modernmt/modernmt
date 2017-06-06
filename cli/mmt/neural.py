import os
import shutil

from cli import mmt_javamain
from cli.libs import fileutils
from cli.libs import shell
from cli.mmt.engine import Engine, EngineBuilder


class TranslationMemory:
    def __init__(self, model, source_lang, target_lang):
        self._model = model
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._java_mainclass = 'eu.modernmt.cli.TranslationMemoryMain'

    def create(self, corpora, log=None):
        if log is None:
            log = shell.DEVNULL

        source_paths = set()

        for corpus in corpora:
            source_paths.add(corpus.get_folder())

        shutil.rmtree(self._model, ignore_errors=True)
        fileutils.makedirs(self._model, exist_ok=True)

        args = ['-s', self._source_lang, '-t', self._target_lang, '-m', self._model, '-c']
        for source_path in source_paths:
            args.append(source_path)

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdout=log, stderr=log)


class OpenNMTDecoder:
    def __init__(self, model, source_lang, target_lang):
        self._model = model
        self._source_lang = source_lang
        self._target_lang = target_lang

    def train(self, corpora, log=None):
        raise NotImplementedError


class NeuralEngine(Engine):
    def __init__(self, name, source_lang, target_lang):
        Engine.__init__(self, name, source_lang, target_lang)

        decoder_path = os.path.join(self.models_path, 'decoder')

        # Neural specific models
        self.memory = TranslationMemory(os.path.join(decoder_path, 'memory'), self.source_lang, self.target_lang)
        self.decoder = OpenNMTDecoder(os.path.join(decoder_path, 'model.pt'), self.source_lang, self.target_lang)

    def is_tuning_supported(self):
        return False

    def type(self):
        return 'neural'


class NeuralEngineBuilder(EngineBuilder):
    def __init__(self, name, source_lang, target_lang, roots, debug=False, steps=None, split_trainingset=True):
        EngineBuilder.__init__(self, NeuralEngine(name, source_lang, target_lang), roots, debug, steps,
                               split_trainingset)

    def _build_schedule(self):
        return EngineBuilder._build_schedule(self) + [self._build_memory, self._train_decoder]

    def _check_constraints(self):
        pass

    # ~~~~~~~~~~~~~~~~~~~~~ Training step functions ~~~~~~~~~~~~~~~~~~~~~

    @EngineBuilder.Step('Creating translation memory')
    def _build_memory(self, args, skip=False, log=None):
        if not skip:
            corpora = filter(None, [args.filtered_bilingual_corpora, args.processed_bilingual_corpora,
                                    args.bilingual_corpora])[0]

            self._engine.memory.create(corpora, log=log)

    @EngineBuilder.Step('Neural decoder training')
    def _train_decoder(self, args, skip=False, log=None):
        if not skip:
            corpora = filter(None, [args.filtered_bilingual_corpora, args.processed_bilingual_corpora,
                                    args.bilingual_corpora])[0]

            self._engine.decoder.train(corpora, log=log)
