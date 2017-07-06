import argparse
import json
import os
import sys

from nmmt import NMTDecoder

import logging


# Base models and Decoder definitions
# ======================================================================================================================
class Suggestion:
    def __init__(self, source, target, score):
        self.source = source
        self.target = target
        self.score = score


# I/O definitions
# ======================================================================================================================

class TranslationRequest:
    def __init__(self, source, suggestions=None):
        self.source = source
        self.suggestions = suggestions if suggestions is not None else []

    @staticmethod
    def from_json_string(json_string):
        obj = json.loads(json_string)

        source = obj['source']
        suggestions = []

        if 'suggestions' in obj:
            for sobj in obj['suggestions']:
                suggestion_source = sobj['source']
                suggestion_target = sobj['target']
                suggestion_score = float(sobj['score']) if 'score' in sobj else 0

                suggestions.append(Suggestion(suggestion_source, suggestion_target, suggestion_score))

        return TranslationRequest(source, suggestions)


class TranslationResponse:
    def __init__(self, translation=None, exception=None):
        self.translation = translation
        self.error_type = type(exception).__name__ if exception is not None else None
        self.error_message = str(exception) if exception is not None and str(exception) else None

    def to_json_string(self):
        jobj = {}

        if self.translation is not None:
            jobj['translation'] = self.translation
        else:
            error = {'type': self.error_type}
            if self.error_message is not None:
                error['message'] = self.error_message
            jobj['error'] = error

        return json.dumps(jobj).replace('\n', ' ')


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
            translation = self._decoder.translate(request.source, request.suggestions)
            return TranslationResponse(translation=translation)
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
        controller.serve_forever()
    except KeyboardInterrupt:
        pass  # ignore and exit
    except BaseException as e:
        logger.exception(e)


if __name__ == '__main__':
    run_main()
