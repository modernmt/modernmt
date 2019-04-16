import argparse
import json
import logging
import sys

from mmt.checkpoint import CheckpointRegistry
from mmt.decoder import MMTDecoder, Suggestion, Translation, ModelConfig


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


def setup_json_logging(log_level='INFO', stream=None):
    handler = logging.StreamHandler(stream or sys.stderr)
    handler.setFormatter(JSONLogFormatter())

    logger = logging.getLogger()
    logger.setLevel(logging.getLevelName(log_level.upper()))
    logger.addHandler(handler)

    return logger


class TranslationRequest(object):
    def __init__(self, source_lang, target_lang, query, suggestions=None, forced_translation=None):
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.query = query
        self.suggestions = suggestions if suggestions is not None else []
        self.forced_translation = forced_translation

    @staticmethod
    def from_json_string(json_string):
        obj = json.loads(json_string)

        if len(obj) == 0:
            return TranslationRequest(None, None, None)  # Test request

        query = obj['q']
        source_lang = obj['sl']
        target_lang = obj['tl']
        forced_translation = obj['f'] if 'f' in obj else None

        suggestions = []

        if 'hints' in obj:
            for suggestion_obj in obj['hints']:
                sugg_sl = suggestion_obj['sl']
                sugg_tl = suggestion_obj['tl']
                sugg_seg = suggestion_obj['seg']
                sugg_tra = suggestion_obj['tra']
                sugg_scr = float(suggestion_obj['scr']) if 'scr' in suggestion_obj else 0

                suggestions.append(Suggestion(sugg_sl, sugg_tl, sugg_seg, sugg_tra, sugg_scr))

        return TranslationRequest(source_lang, target_lang, query,
                                  suggestions=suggestions, forced_translation=forced_translation)


class TranslationResponse(object):
    @staticmethod
    def to_json_string(obj):
        if isinstance(obj, Translation):
            return TranslationResponse.__translation_to_json_string(obj)
        else:
            return TranslationResponse.__error_to_json_string(obj)

    @staticmethod
    def __error_to_json_string(cause):
        return json.dumps({
            'success': False,
            'data': {
                'type': 'UnknownError' if isinstance(cause, str) else type(cause).__name__,
                'msg': cause if isinstance(cause, str) else str(cause)
            }
        }).replace('\n', ' ')

    @staticmethod
    def __translation_to_json_string(translation):
        alignment = TranslationResponse._encode_alignment(translation.alignment)

        payload = {'text': translation.text}
        if alignment is not None:
            payload['a'] = alignment
        if translation.score is not None:
            payload['s'] = round(translation.score, 4)

        return json.dumps({
            'success': True,
            'data': payload,
        }).replace('\n', ' ')

    @staticmethod
    def _encode_alignment(a):
        return [[e[0] for e in a], [e[1] for e in a]] if a is not None else None


def _serve_forever(stdin, stdout, decoder):
    try:
        while True:
            line = stdin.readline()
            if not line:
                break

            request = TranslationRequest.from_json_string(line)

            if request.query is None:
                decoder.test()
                translation = Translation(text="")
            else:
                translation = decoder.translate(request.source_lang, request.target_lang, request.query,
                                                suggestions=request.suggestions,
                                                forced_translation=request.forced_translation)

            response = TranslationResponse.to_json_string(translation)

            stdout.write(response + '\n')
            stdout.flush()
    except KeyboardInterrupt:
        pass  # ignore and exit
    except BaseException as e:
        response = TranslationResponse.to_json_string(e)
        stdout.write(response + '\n')
        stdout.flush()

        exit(1)


def main(argv=None):
    # Args parse
    # ------------------------------------------------------------------------------------------------------------------
    parser = argparse.ArgumentParser(description='Run a forever-loop serving translations')
    parser.add_argument('model', metavar='MODEL', help='the path to the decoder model')
    parser.add_argument('-l', '--log-level', dest='log_level', metavar='LEVEL', help='select the log level',
                        choices=['critical', 'error', 'warning', 'info', 'debug'], default='info')
    parser.add_argument('-g', '--gpu', dest='gpu', help='specify the GPU to use (default none)', default=None, type=int)

    args = parser.parse_args(argv)

    # Redirecting stdout and stderr to /dev/null
    # ------------------------------------------------------------------------------------------------------------------
    dev_null = open('/dev/null', 'w')

    stdout = sys.stdout
    stderr = sys.stderr

    sys.stdout = dev_null
    sys.stderr = dev_null

    # Setting up logging
    # ------------------------------------------------------------------------------------------------------------------
    setup_json_logging(args.log_level, stream=stderr)

    # Main loop
    # ------------------------------------------------------------------------------------------------------------------
    config = ModelConfig.load(args.model)

    builder = CheckpointRegistry.Builder()
    for name, checkpoint_path in config.checkpoints:
        builder.register(name, checkpoint_path)
    checkpoints = builder.build(args.gpu)

    decoder = MMTDecoder(checkpoints, device=args.gpu, tuning_ops=config.tuning)

    stdout.write('READY\n')
    stdout.flush()

    _serve_forever(sys.stdin, stdout, decoder)


if __name__ == '__main__':
    main()
