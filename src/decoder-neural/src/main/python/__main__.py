# coding=utf-8
import argparse
import json
import logging
import os
import sys


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


def run_main():
    # Args parse
    # ------------------------------------------------------------------------------------------------------------------
    parser = argparse.ArgumentParser(description='Run a forever-loop serving translations')
    parser.add_argument('model', metavar='MODEL', help='the path to the decoder model')
    parser.add_argument('-l', '--log-level', dest='log_level', metavar='LEVEL', help='select the log level',
                        choices=['critical', 'error', 'warning', 'info', 'debug'], default='info')
    parser.add_argument('-g', '--gpu', dest='gpu', help='specify the GPU to use (default none)', default=None, type=int)

    args = parser.parse_args()

    # Redirect default stderr and stdout to /dev/null
    # ------------------------------------------------------------------------------------------------------------------
    stderr = os.fdopen(sys.stderr.fileno(), 'w', 0)
    stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)

    devnull_stream = open(os.devnull, 'w')

    # DO NOT REMOVE
    sys.stderr = devnull_stream
    sys.stdout = devnull_stream

    # Setting up logging
    # ------------------------------------------------------------------------------------------------------------------
    from nmmt import set_tensorflow_log_level
    set_tensorflow_log_level(9999)

    handler = logging.StreamHandler(stderr)
    handler.setFormatter(JSONLogFormatter())

    logger = logging.getLogger()
    logger.setLevel(logging.getLevelName(args.log_level.upper()))
    logger.addHandler(handler)

    # Main loop
    # ------------------------------------------------------------------------------------------------------------------
    from nmmt.transformer import ModelConfig, TransformerDecoder
    from nmmt.checkpoint import CheckpointPool

    config = ModelConfig.load(args.model)

    # Init checkpoints
    builder = CheckpointPool.Builder()
    for name, checkpoint_path in config.checkpoints:
        builder.register(name, checkpoint_path)
    checkpoints = builder.build()

    decoder = TransformerDecoder(args.gpu, checkpoints, config=config)

    stdout.write('READY\n')
    stdout.flush()

    decoder.serve_forever(sys.stdin, stdout)


if __name__ == '__main__':
    run_main()
