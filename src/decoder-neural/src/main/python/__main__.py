import argparse
import sys

from mmt import utils
from mmt.checkpoint import CheckpointRegistry
from mmt.decoder import MMTDecoder, ModelConfig


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
    stdout, stderr = utils.mask_std_streams()

    # Setting up logging
    # ------------------------------------------------------------------------------------------------------------------
    utils.setup_json_logging(args.log_level, stream=stderr)

    # Main loop
    # ------------------------------------------------------------------------------------------------------------------
    config = ModelConfig.load(args.model)

    builder = CheckpointRegistry.Builder()
    for name, checkpoint_path in config.checkpoints:
        builder.register(name, checkpoint_path)
    checkpoints = builder.build(args.gpu)

    decoder = MMTDecoder(checkpoints, device=args.gpu, tuning_ops=config.tuning)
    utils.serve_forever(sys.stdin, stdout, decoder)


if __name__ == '__main__':
    main()
