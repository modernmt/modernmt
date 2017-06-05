import argparse
import json
import sys


from onmt import Suggestion, MMTDecoder
from onmt.opennmt import OpenNMTDecoder

class TranslationRequest:
    def __init__(self, _id, source, suggestions=None):
        self.id = _id
        self.source = source
        self.suggestions = suggestions if suggestions is not None else []

    @staticmethod
    def from_json_string(json_string):
        obj = json.loads(json_string)

        _id = int(obj['id'])
        source = obj['source'].split(' ')
        suggestions = []

        if 'suggestions' in obj:
            for sobj in obj['suggestions']:
                suggestion_source = sobj['source'].split(' ')
                suggestion_target = sobj['target'].split(' ')
                suggestion_score = float(sobj['score']) if 'score' in sobj else 0

                suggestions.append(Suggestion(suggestion_source, suggestion_target, suggestion_score))

        return TranslationRequest(_id, source, suggestions)


class TranslationResponse:
    def __init__(self, _id, translation=None, exception=None):
        self.id = _id
        self.translation = translation
        self.error_type = type(exception).__name__ if exception is not None else None
        self.error_message = str(exception) if exception is not None and str(exception) else None

    def to_json_string(self):
        jobj = {'id': self.id}

        if self.translation is not None:
            jobj['translation'] = ' '.join(self.translation)
        else:
            error = {'type': self.error_type}
            if self.error_message is not None:
                error['message'] = self.error_message
            jobj['error'] = error

        return json.dumps(jobj)


class MainController:
    def __init__(self, decoder):
        self._decoder = decoder
        self._stdin = sys.stdin
        self._stdout = sys.stdout

    def serve_forever(self):
        try:
            while True:
                line = self._stdin.readline()
                if not line:
                    break

                request = TranslationRequest.from_json_string(line)
                translation = self._decoder.translate(request.source, request.suggestions)
                response = TranslationResponse(request.id, translation=translation)

                self._stdout.write(response.to_json_string())
                self._stdout.write('\n')
                self._stdout.flush()
        except KeyboardInterrupt:
            pass


class YodaDecoder(MMTDecoder):
    def __init__(self):
        MMTDecoder.__init__(self, '')

    def translate(self, text, suggestions=None):
        return reversed(text)

    def _preferred_threads(self):
        return 1


def run_main():
    parser = argparse.ArgumentParser(description='Run a forever-loop serving translation requests')
    parser.add_argument('model', metavar='MODEL', help='the path to the decoder model')

    args = parser.parse_args()

    ###decoder = YodaDecoder()
    decoder = OpenNMTDecoder(args.model + '/model.pt')

    try:
        controller = MainController(decoder)
        controller.serve_forever()
    finally:
        decoder.close()


if __name__ == '__main__':
    run_main()
