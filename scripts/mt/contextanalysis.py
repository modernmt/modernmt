import json
import os
import subprocess
import urllib
import urllib2
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

        self._server_port = None
        self._server_process = None

        self._analyzer_jar = os.path.join(scripts.BIN_DIR, 'context-analyzer-1.1', 'context-analyzer-1.1.jar')

    def create_index(self, index, source_path, log_file=None):
        fileutils.makedirs(index, exist_ok=True)

        command = ['java', '-cp', self._analyzer_jar, 'net.translated.contextanalyzer.http.cli.CreateIndex', '-i',
                   index, '-c', source_path]
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

    def start_server(self, index, port, log_file=None):
        log = shell.DEVNULL if log_file is None else open(log_file, 'w')

        command = ['java', '-cp', self._analyzer_jar, 'net.translated.contextanalyzer.http.SimpleServerExecutor', '-i', index, '-p', str(port)]
        self._server_process = subprocess.Popen(command, stdout=log, stderr=log)
        self._server_port = port

    def stop_server(self):
        if self._server_process is not None:
            self._server_process.terminate()
            self._server_process.wait()

            self._server_process = None
            self._server_port = None

    def get_context(self, sentence, lang):
        return self._get_context('context', sentence, lang)

    def get_contextd(self, document, lang):
        return self._get_context('file', document, lang)

    def _get_context(self, param_name, param, lang):
        if self._server_process is None:
            raise Exception('Context Analyzer server is not running')

        url = 'http://localhost:{port}/context'.format(port=self._server_port)
        params = {
            'language': lang,
            param_name: param
        }

        url = url + '?' + urllib.urlencode(params)
        raw_response = urllib2.urlopen(url).read()

        return json.loads(raw_response)
