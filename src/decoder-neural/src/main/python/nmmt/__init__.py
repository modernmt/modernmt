import json


def set_tensorflow_log_level(level):
    import os
    os.environ['TF_CPP_MIN_LOG_LEVEL'] = str(level)
    import tensorflow as tf
    tf.logging.set_verbosity(level)


class Translation(object):
    def __init__(self, text, alignment=None):
        self.text = text
        self.alignment = alignment


class Suggestion(object):
    def __init__(self, source_lang, target_lang, segment, translation, score):
        self.source_lang = source_lang
        self.target_lang = target_lang
        self.segment = segment
        self.translation = translation
        self.score = score


class UnsupportedLanguageException(BaseException):
    def __init__(self, source_language, target_language):
        self.message = "No engine found for %s -> %s." % (source_language, target_language)


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
                'msg': cause if isinstance(cause, str) else cause.message
            }
        }).replace('\n', ' ')

    @staticmethod
    def __translation_to_json_string(translation):
        alignment = TranslationResponse._encode_alignment(translation.alignment)

        payload = {'text': translation.text}
        if alignment is not None:
            payload['a'] = alignment

        return json.dumps({
            'success': True,
            'data': payload,
        }).replace('\n', ' ')

    @staticmethod
    def _encode_alignment(a):
        return [[e[0] for e in a], [e[1] for e in a]] if a is not None else None
