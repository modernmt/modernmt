import argparse

import sys

import time

from nmmt import NMTDecoder


class Suggestion:
    def __init__(self, source, target, score):
        self.source = source
        self.target = target
        self.score = score


def run_main():
    # Args parse
    # ------------------------------------------------------------------------------------------------------------------
    parser = argparse.ArgumentParser(description='Run a forever-loop serving translation requests')
    parser.add_argument('model_en_it', metavar='MODEL_EN', help='the path to the decoder model')
    parser.add_argument('model_it_en', metavar='MODEL_IT_EN', help='the path to the decoder model')

    args = parser.parse_args()

    # Main loop
    # ------------------------------------------------------------------------------------------------------------------
    try:
        decoder_en_it = []
        decoder_it_en = []

        print 'READY'

        while True:
            line = sys.stdin.readline()
            if not line:
                break

            cmd, arg = line.strip().split(' ', 1)

            if cmd == 'load':
                decoders = decoder_en_it if arg == 'en' else decoder_it_en

                now = time.time()
                if arg == 'en':
                    decoders.append(NMTDecoder(args.model_en_it, gpu_id=0, random_seed=3435))
                else:
                    decoders.append(NMTDecoder(args.model_it_en, gpu_id=0, random_seed=3435))
                print '>> DONE in %f seconds' % (time.time() - now), len(decoders)
            elif cmd == 'unload':
                decoders = decoder_en_it if arg == 'en' else decoder_it_en

                now = time.time()

                decoder = decoders.pop()
                del decoder

                print '>> DONE in %f seconds' % (time.time() - now), len(decoders)
            elif cmd == 'translate':
                lang, query = arg.split(' ', 1)

                decoders = decoder_en_it if lang == 'en' else decoder_it_en

                if len(decoders) == 0:
                    print '>> decoder not loaded'
                else:
                    now = time.time()
                    translation = decoders[-1].translate('en', 'it', query, [Suggestion('hello world', 'ciao mondo', 1.)])
                    print '>> DONE in %f seconds' % (time.time() - now)
                    print translation
    except KeyboardInterrupt:
        pass  # ignore and exit


if __name__ == '__main__':
    run_main()
