import argparse
import json
import os
import sys

from onmt import Suggestion, MMTDecoder
from onmt.opennmt import OpenNMTDecoder

import logging


class TranslationRequest:
    def __init__(self, source, suggestions=None):
        self.source = source
        self.suggestions = suggestions if suggestions is not None else []

    @staticmethod
    def from_json_string(json_string):
        obj = json.loads(json_string)

        source = obj['source'].split(' ')
        suggestions = []

        if 'suggestions' in obj:
            for sobj in obj['suggestions']:
                suggestion_source = sobj['source'].split(' ')
                suggestion_target = sobj['target'].split(' ')
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
            jobj['translation'] = ' '.join(self.translation)
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


class YodaDecoder(MMTDecoder):
    def __init__(self):
        MMTDecoder.__init__(self, '')

    def translate(self, text, suggestions=None):
        return reversed(text)

    def close(self):
        pass


def run_main():
    # Args parse
    # ------------------------------------------------------------------------------------------------------------------
    parser = argparse.ArgumentParser(description='Run a forever-loop serving translation requests')
    parser.add_argument('-l', '--log-level', dest='log_level', metavar='LEVEL', help='select the log level',
                        choices=['critical', 'error', 'warning', 'info', 'debug'], default='info')
    parser.add_argument('-model', metavar='MODEL', help='the path to the decoder model')
    parser.add_argument('-g', '-gpu', dest='gpu', metavar='GPU', help='the index of the GPU to use',
                        default=-1)
    parser.add_argument('-beam_size', type=int, default=5,
                        help='Beam size')
    parser.add_argument('-batch_size', type=int, default=30,
                        help='Batch size')
    parser.add_argument('-max_sent_length', type=int, default=100,
                        help='Maximum sentence length.')
    parser.add_argument('-replace_unk', action="store_true",
                        help="""Replace the generated UNK tokens with the source
                    token that had the highest attention weight. If phrase_table
                    is provided, it will lookup the identified source token and
                    give the corresponding target token. If it is not provided
                    (or the identified source token does not exist in the
                    table) then it will copy the source token""")
    parser.add_argument('-verbose', action="store_true",
                        help='Print scores and predictions for each sentence')
    parser.add_argument('-dump_beam', type=str, default="",
                        help='File to dump beam information to.')
    parser.add_argument('-n_best', type=int, default=1,
                        help="""If verbose is set, will output the n_best
                    decoded sentences""")
    parser.add_argument('-tuning_epochs', type=int, default=5,
                        help='Number of tuning epochs')
    parser.add_argument('-seed', type=int, default=3435,
                        help="Random seed for generating random numbers (-1 for un-defined the seed; default is 3435);")
    parser.add_argument('-tunable', action="store_true",
                        help='Enable fine tuning')
    parser.add_argument('-reset', action="store_true",
                        help='Reset model to the original model after each translation')

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
    decoder = None

    try:
        decoder = OpenNMTDecoder(args)

        controller = MainController(decoder, stdout)
        controller.serve_forever()
    except KeyboardInterrupt:
        pass  # ignore and exit
    except BaseException as e:
        logger.exception(e)
    finally:
        if decoder is not None:
            # noinspection PyBroadException
            try:
                decoder.close()
            except:
                pass


if __name__ == '__main__':
    run_main()
