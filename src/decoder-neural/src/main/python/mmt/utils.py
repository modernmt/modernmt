import json
import logging
import sys

from mmt.decoder import Translation, Suggestion


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


def setup_basic_logging(log_level='INFO'):
    logging.basicConfig(format='%(asctime)s %(levelname)-8s %(message)s')

    logger = logging.getLogger()
    logger.setLevel(logging.getLevelName(log_level.upper()))

    return logger


def mask_std_streams():
    dev_null = open('/dev/null', 'w')

    stdout = sys.stdout
    stderr = sys.stderr

    sys.stdout = dev_null
    sys.stderr = dev_null

    return stdout, stderr


class TranslationRequest(object):
    def __init__(self, source_lang, target_lang, batch, suggestions=None, forced_translation=None):
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.batch = batch
        self.suggestions = suggestions if suggestions is not None else []
        self.forced_translation = forced_translation

    @staticmethod
    def from_json_string(json_string):
        obj = json.loads(json_string)

        if len(obj) == 0:
            return TranslationRequest(None, None, None)  # Test request

        batch = obj['q'].split('\n')
        source_lang = obj['sl']
        target_lang = obj['tl']
        forced_translation = None
        if 'f' in obj:
            forced_translation = obj['f'].split('\n')
            if (len(batch) != len(forced_translation)):
                raise ValueError("Number of inputs ans forced translations differs ({} vs {}".format(len(batch), len(forced_translation)))

        suggestions = []

        if 'hints' in obj:
            for suggestion_obj in obj['hints']:
                sugg_sl = suggestion_obj['sl']
                sugg_tl = suggestion_obj['tl']
                sugg_seg = suggestion_obj['seg']
                sugg_tra = suggestion_obj['tra']
                sugg_scr = float(suggestion_obj['scr']) if 'scr' in suggestion_obj else 0

                suggestions.append(Suggestion(sugg_sl, sugg_tl, sugg_seg, sugg_tra, sugg_scr))

        return TranslationRequest(source_lang, target_lang, batch,
                                  suggestions=suggestions, forced_translation=forced_translation)


class TranslationResponse(object):
    @staticmethod
    def to_json_string(obj):
        if isinstance(obj, BaseException):
            return TranslationResponse.__error_to_json_string(obj)
        else:
            return TranslationResponse.__translations_to_json_string(obj)

    @staticmethod
    def __error_to_json_string(cause):
        return json.dumps({
            'success': False,
            'type': 'UnknownError' if isinstance(cause, str) else type(cause).__name__,
            'msg': cause if isinstance(cause, str) else str(cause)
        }).replace('\n', ' ')

    @staticmethod
    def __translations_to_json_string(translations):
        def __encode_alignment(a):
            return [[e[0] for e in a], [e[1] for e in a]] if a is not None else None

        def __to_json(translation):
            alignment = __encode_alignment(translation.alignment)

            payload = {'text': translation.text}
            if alignment is not None:
                payload['a'] = alignment
            if translation.score is not None:
                payload['s'] = round(translation.score, 4)

            return payload

        return json.dumps({
            'success': True,
            'data': [__to_json(translation) for translation in translations],
        }).replace('\n', ' ')


def serve_forever(stdin, stdout, decoder):
    stdout.write('READY\n')
    stdout.flush()

    try:
        while True:
            line = stdin.readline()
            if not line:
                break

            request = TranslationRequest.from_json_string(line)

            if request.batch is None:
                decoder.test()
                translations = []
            else:
                translations = decoder.translate(request.source_lang, request.target_lang, request.batch,
                                                 suggestions=request.suggestions,
                                                 forced_translation=request.forced_translation)

            response = TranslationResponse.to_json_string(translations)

            stdout.write(response + '\n')
            stdout.flush()
    except KeyboardInterrupt:
        pass  # ignore and exit
    except BaseException as e:
        response = TranslationResponse.to_json_string(e)
        stdout.write(response + '\n')
        stdout.flush()

        exit(1)
