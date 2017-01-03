from cli import mmt_javamain
from cli.libs import fileutils, shell

__author__ = 'Davide Caroselli'


class ContextAnalyzer:
    injector_section = 'context'

    def __init__(self, index, source_lang, target_lang):
        self._index = index
        self._source_lang = source_lang
        self._target_lang = target_lang

        self._java_mainclass = 'eu.modernmt.cli.ContextAnalyzerMain'

    def create_index(self, corpora, log=None):
        if log is None:
            log = shell.DEVNULL

        source_paths = set()

        for corpus in corpora:
            source_paths.add(corpus.get_folder())

        fileutils.makedirs(self._index, exist_ok=True)

        args = ['-s', self._source_lang, '-t', self._target_lang, '-i', self._index, '-c']
        for source_path in source_paths:
            args.append(source_path)

        command = mmt_javamain(self._java_mainclass, args)
        shell.execute(command, stdout=log, stderr=log)
