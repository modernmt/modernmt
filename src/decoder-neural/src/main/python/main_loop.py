import argparse
import json
import logging
import sys

import os

from nmmt import Suggestion, NMTDecoder


# I/O definitions
# ======================================================================================================================

class TranslationRequest:
    def __init__(self, source_lang, target_lang, source, suggestions=None, n_best=None, variant=None):
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.variant = variant
        self.source = source
        self.suggestions = suggestions if suggestions is not None else []
        self.n_best = n_best if n_best > 1 else 1

    @staticmethod
    def from_json_string(json_string):
        obj = json.loads(json_string)

        source = obj['source']
        source_language = obj['source_language']
        target_language = obj['target_language']
        n_best = obj['n_best'] if 'n_best' in obj else None
        variant = obj['variant'] if 'variant' in obj else None

        suggestions = []

        if 'suggestions' in obj:
            i = 0
            for sobj in obj['suggestions']:
                suggestion_source = sobj['source']
                suggestion_target = sobj['target']
                suggestion_score = float(sobj['score']) if 'score' in sobj else 0

                suggestions.append(Suggestion(suggestion_source, suggestion_target, suggestion_score))
                i += 1

        return TranslationRequest(source_language, target_language, source, suggestions, n_best, variant)


class TranslationResponse:
    def __init__(self, translations=None, exception=None):
        self.translations = translations
        self.error_type = type(exception).__name__ if exception is not None else None
        self.error_message = str(exception) if exception is not None and str(exception) else None

    def to_json_string(self):
        json_root = {}

        if self.translations is not None:
            json_array = []

            for translation in self.translations:
                alignment = []
                if translation.alignment:
                    alignment = [[e[0] for e in translation.alignment], [e[1] for e in translation.alignment]]

                json_array.append({
                    'text': translation.text,
                    'alignment': alignment
                })

            json_root['result'] = json_array
        else:
            error = {'type': self.error_type}
            if self.error_message is not None:
                error['message'] = self.error_message
            json_root['error'] = error

        return json.dumps(json_root).replace('\n', ' ')


class MainController:
    def __init__(self, decoder, stdout):
        self._decoder = decoder
        self._stdin = sys.stdin
        self._stdout = stdout

        self._logger = logging.getLogger('mainloop')

    def serve_forever(self):
        try:
            while True:
                line = self._stdin.readline()
                if not line:
                    break

                response = self.process(line)

                self._stdout.write(response.to_json_string())
                self._stdout.write('\n')
                self._stdout.flush()
        except KeyboardInterrupt:
            pass

    def process(self, line):
        try:
            request = TranslationRequest.from_json_string(line)
            translations = self._decoder.translate(request.source_lang, request.target_lang, request.source,
                                                   suggestions=request.suggestions, n_best=request.n_best,
                                                   variant=request.variant)
            return TranslationResponse(translations=translations)
        except BaseException as e:
            self._logger.exception('Failed to process request "' + line + '"')
            return TranslationResponse(exception=e)


class JSONLogFormatter(logging.Formatter):
    def __init__(self):
        super(JSONLogFormatter, self).__init__('%(message)s')

    def format(self, record):
        message = super(JSONLogFormatter, self).format(record)
        return json.dumps({
            'level': record.levelname,
            'message': message,
            'logger': record.name
        }).replace('\n', ' ')


# TM Decoder
# ======================================================================================================================
class _TMDecoder(object):
    def translate(self, source_lang, target_lang, text, suggestions=None, n_best=1,
                  tuning_epochs=None, tuning_learning_rate=None):
        return suggestions[0].target if len(suggestions) > 0 else ''


# Main function
# ======================================================================================================================

def run_main():
    # Args parse
    # ------------------------------------------------------------------------------------------------------------------
    parser = argparse.ArgumentParser(description='Run a forever-loop serving translation requests')
    parser.add_argument('model', metavar='MODEL', help='the path to the decoder model')
    parser.add_argument('-l', '--log-level', dest='log_level', metavar='LEVEL', help='select the log level',
                        choices=['critical', 'error', 'warning', 'info', 'debug'], default='info')
    parser.add_argument('-g', '--gpu', type=int, dest='gpu', metavar='GPU', help='the index of the GPU to use',
                        default=None)

    args = parser.parse_args()

    # Redirect default stderr and stdout to /dev/null
    # ------------------------------------------------------------------------------------------------------------------
    stderr = sys.stderr
    stdout = sys.stdout

    devnull_stream = open(os.devnull, 'w')

    # DO NOT REMOVE
    sys.stderr = devnull_stream
    sys.stdout = devnull_stream

    # Setting up logging
    # ------------------------------------------------------------------------------------------------------------------
    handler = logging.StreamHandler(stderr)
    handler.setFormatter(JSONLogFormatter())

    logger = logging.getLogger()
    logger.setLevel(logging.getLevelName(args.log_level.upper()))
    logger.addHandler(handler)

    # Main loop
    # ------------------------------------------------------------------------------------------------------------------
    try:
        decoder = NMTDecoder(args.model, gpu_id=args.gpu, random_seed=3435)
        controller = MainController(decoder, stdout)
        stdout.write("ok\n")
        stdout.flush()
        controller.serve_forever()
    except KeyboardInterrupt:
        pass  # ignore and exit
    except BaseException as e:
        logger.exception(e)


if __name__ == '__main__':
    run_main()
