from cli import mmt_javamain
from cli.libs import fileutils, shell

__author__ = 'Davide Caroselli'


class ContextAnalyzer:
    injector_section = 'context'

    def __init__(self, index):
        self._index = index
        self._java_mainclass = 'eu.modernmt.cli.ContextAnalyzerMain'

    def create_index(self, corpora, lang, log_file=None):
        source_paths = set()

        for corpus in corpora:
            source_paths.add(corpus.root)

        fileutils.makedirs(self._index, exist_ok=True)

        args = ['-l', lang, '-i', self._index, '-c']
        for source_path in source_paths:
            args.append(source_path)

        command = mmt_javamain(self._java_mainclass, args)

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'w')

            shell.execute(command, stdout=log, stderr=log)
        finally:
            if log_file is not None:
                log.close()
