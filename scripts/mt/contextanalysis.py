import scripts
from scripts.libs import fileutils, shell

__author__ = 'Davide Caroselli'


class ContextAnalyzer:
    injector_section = 'context'
    injectable_fields = {
        'index_lang': (
            'language of the corpus to index, by default the analyzer indexes all the available languages', basestring,
            None),
    }

    def __init__(self):
        self._index_lang = None  # Injected

        self._analyzer_jar = scripts.MMT_JAR
        self._java_mainclass = 'eu.modernmt.cli.ContextAnalyzerMain'

    def create_index(self, index, source_path, log_file=None):
        fileutils.makedirs(index, exist_ok=True)

        command = ['java', '-cp', self._analyzer_jar, self._java_mainclass, '-i', index, '-c', source_path]
        if self._index_lang is not None:
            command += ['-l', self._index_lang]

        log = shell.DEVNULL

        try:
            if log_file is not None:
                log = open(log_file, 'w')

            shell.execute(command, stdout=log, stderr=log)
        finally:
            if log_file is not None:
                log.close()
