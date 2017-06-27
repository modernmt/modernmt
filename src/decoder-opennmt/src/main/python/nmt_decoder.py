import argparse
import json
import os
import sys

from bpe import BPEEncoder
from onmt import Translator

import logging


# Base models and Decoder definitions
# ======================================================================================================================

class Suggestion:
    def __init__(self, source, target, score):
        self.source = source
        self.target = target
        self.score = score


class MMTDecoder:
    def __init__(self, model_path):
        """
        Creates a new instance of an NMT decoder
        :param model_path: path to the decoder model file/folder
        :type model_path: basestring
        """
        self._model_path = model_path

    def translate(self, text, suggestions=None):
        """
        Returns a translation for the given Translation Request
        :param text: the tokenized text to be translated
        :type text: list

        :param suggestions: a collection of suggestions in order to adapt the translation
        :type suggestions: list

        :return: the best translation as a list of tokens
        """
        raise NotImplementedError('abstract method')

    def close(self):
        """
        Called before destroying this object.
        The decoder should release any resource acquired during execution.
        """
        raise NotImplementedError('abstract method')


class YodaDecoder(MMTDecoder):
    def __init__(self):
        MMTDecoder.__init__(self, '')

    def translate(self, text, suggestions=None):
        return reversed(text)

    def close(self):
        pass


class OpenNMTDecoder(MMTDecoder):
    def __init__(self, model, gpu=-1):
        MMTDecoder.__init__(self, model)

        opt = Translator.Options()
        opt.gpu = gpu

        self._logger = logging.getLogger('onmt.OpenNMTDecoder')
        self._translator = Translator(os.path.join(model, 'model.pt'), opt)
        self._bpe_encoder = BPEEncoder(os.path.join(model, 'codes.bpe'))

    def translate(self, text, suggestions=None):
        src_batch = [self._bpe_encoder.encode_line(text)]

        if len(suggestions) == 0 or not self._translator.opt.tunable:
            pred_batch, pred_score, gold_score = self._translator.translate(src_batch, None)
        else:
            for suggestion in suggestions:
                suggestion.source = self._bpe_encoder.encode_line(suggestion.source)
                suggestion.target = self._bpe_encoder.encode_line(suggestion.target)

            pred_batch, pred_score, gold_score = self._translator.translateWithAdaptation(src_batch, None, suggestions)

        output = pred_batch[0][0]

        # print of the nbest for each sentence of the batch
        for b in range(len(pred_batch)):
            for n in range(len(pred_batch[b])):
                self._logger.info(
                    "b:%d n:%d predScore[b][n]:%g predBatch[b][n]:%s" % (
                        b, n, pred_score[b][n], repr(pred_batch[b][n])))

        return self._bpe_encoder.decode_line(output)

    def close(self):
        pass


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
            self._logger.info('processing "' + line + '"')
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
    parser.add_argument('-g', '--gpu', dest='gpu', metavar='GPU', help='the index of the GPU to use',
                        default=-1)

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
        decoder = OpenNMTDecoder(args.model, gpu=args.gpu)

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
