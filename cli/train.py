import argparse
import collections
import os
import pickle
import re
import shutil

import torch

from cli import CLIArgsException, StatefulActivity, activitystep
from cli.mmt import MMT_FAIRSEQ_USER_DIR
from cli.utils import osutils


def _last_n_checkpoints(path, n, regex):
    pt_regexp = re.compile(regex)
    files = os.listdir(path)

    entries = []
    for f in files:
        m = pt_regexp.fullmatch(f)
        if m is not None:
            sort_key = int(m.group(1))
            entries.append((sort_key, m.group(0)))

    return [os.path.join(path, x[1]) for x in sorted(entries, reverse=True)[:n]]


class TrainActivity(StatefulActivity):
    def __init__(self, args, extra_argv=None, wdir=None, log_file=None, start_step=None, delete_on_exit=True):
        super().__init__(args, extra_argv, wdir, log_file, start_step, delete_on_exit)

    @activitystep('Train neural network')
    def train_nn(self):
        # Start training
        self.state.nn_path = self.wdir('nn_model')

        cmd = ['fairseq-train', self.args.data_path, '--save-dir', self.state.nn_path, '--task', 'mmt_translation',
               '--user-dir', MMT_FAIRSEQ_USER_DIR, '--share-all-embeddings', '--no-progress-bar']
        cmd += self.extra_argv

        process = osutils.shell_exec(cmd, stderr=self.log_fobj, stdout=self.log_fobj, background=True)
        try:
            process.wait()
        except KeyboardInterrupt:
            process.terminate()

    @activitystep('Averaging checkpoints')
    def avg_checkpoints(self):
        checkpoints = _last_n_checkpoints(self.state.nn_path, self.args.num_checkpoints, r'checkpoint_\d+_(\d+)\.pt')
        if len(checkpoints) == 0:
            # by epoch
            checkpoints = _last_n_checkpoints(self.state.nn_path, self.args.num_checkpoints, r'checkpoint(\d+)\.pt')

        if len(checkpoints) == 0:
            raise ValueError('no checkpoints found in ' + self.state.nn_path)

        self._logger.info('Averaging checkpoints: ' + str(checkpoints))

        with open(os.path.join(self.args.data_path, 'decode_lengths.bin'), 'rb') as f:
            decode_lengths = pickle.load(f)

        # Average checkpoints
        params_dict = collections.OrderedDict()
        params_keys = None
        avg_state = None

        for f in checkpoints:
            state = torch.load(f, map_location=lambda s, _: torch.serialization.default_restore_location(s, 'cpu'))
            # Copies over the settings from the first checkpoint
            if avg_state is None:
                avg_state = state

            model_params = state['model']

            model_params_keys = list(model_params.keys())
            if params_keys is None:
                params_keys = model_params_keys
            elif params_keys != model_params_keys:
                raise KeyError(
                    'For checkpoint {}, expected list of params: {}, '
                    'but found: {}'.format(f, params_keys, model_params_keys)
                )

            for k in params_keys:
                if k not in params_dict:
                    params_dict[k] = []
                p = model_params[k]
                if isinstance(p, torch.HalfTensor):
                    p = p.float()
                params_dict[k].append(p)

        averaged_params = collections.OrderedDict()
        # v should be a list of torch Tensor.
        for k, v in params_dict.items():
            summed_v = None
            for x in v:
                summed_v = summed_v + x if summed_v is not None else x
            averaged_params[k] = summed_v / len(v)

        avg_state['model'] = averaged_params
        avg_state['decode_stats'] = decode_lengths

        # Save model
        os.makedirs(self.args.output_path, exist_ok=True)
        torch.save(avg_state, os.path.join(self.args.output_path, 'model.pt'))
        shutil.copy(os.path.join(self.args.data_path, 'model.vcb'), os.path.join(self.args.output_path, 'model.vcb'))


def parse_extra_argv(parser, extra_argv):
    for reserved_opt in ['--save-dir', '--user-dir', '--task', '--no-progress-bar', '--share-all-embeddings']:
        if reserved_opt in extra_argv:
            raise CLIArgsException(parser, 'overriding option "%s" is not allowed' % reserved_opt)

    cmd_extra_args = extra_argv[:]

    if '-a' not in cmd_extra_args and '--arch' not in cmd_extra_args:
        cmd_extra_args.extend(['--arch', 'transformer_mmt_base'])
    if '--clip-norm' not in cmd_extra_args:
        cmd_extra_args.extend(['--clip-norm', '0.0'])
    if '--label-smoothing' not in cmd_extra_args:
        cmd_extra_args.extend(['--label-smoothing', '0.1'])
    if '--attention-dropout' not in cmd_extra_args:
        cmd_extra_args.extend(['--attention-dropout', '0.1'])
    if '--dropout' not in cmd_extra_args:
        cmd_extra_args.extend(['--dropout', '0.3'])
    if '--weight-decay' not in cmd_extra_args:
        cmd_extra_args.extend(['--weight-decay', '0.0'])
    if '--criterion' not in cmd_extra_args:
        cmd_extra_args.extend(['--criterion', 'label_smoothed_cross_entropy'])
    if '--optimizer' not in cmd_extra_args:
        cmd_extra_args.extend(['--optimizer', 'adam'])
        if '--adam-betas' not in cmd_extra_args:
            cmd_extra_args.extend(['--adam-betas', '(0.9, 0.98)'])
    if '--log-interval' not in cmd_extra_args:
        cmd_extra_args.extend(['--log-interval', '100'])
    if '--lr' not in cmd_extra_args:
        cmd_extra_args.extend(['--lr', '0.0001'])
    if '--lr-scheduler' not in cmd_extra_args:
        cmd_extra_args.extend(['--lr-scheduler', 'inverse_sqrt'])
    if '--min-lr' not in cmd_extra_args:
        cmd_extra_args.extend(['--min-lr', '1e-09'])
    if '--warmup-init-lr' not in cmd_extra_args:
        cmd_extra_args.extend(['--warmup-init-lr', '1e-07'])
    if '--warmup-updates' not in cmd_extra_args:
        cmd_extra_args.extend(['--warmup-updates', '4000'])
    if '--max-tokens' not in cmd_extra_args:
        cmd_extra_args.extend(['--max-tokens', '1536'])
    if '--save-interval-updates' not in cmd_extra_args:
        cmd_extra_args.extend(['--save-interval-updates', '2000'])
    if '--keep-interval-updates' not in cmd_extra_args:
        cmd_extra_args.extend(['--keep-interval-updates', '10'])
    if '--keep-last-epochs' not in cmd_extra_args:
        cmd_extra_args.extend(['--keep-last-epochs', '10'])

    return cmd_extra_args


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description='Train the neural model')
    parser.prog = 'mmt train'
    parser.add_argument('data_path', metavar='DATA_FOLDER',
                        help='data folder holding binarized training and validation sets')
    parser.add_argument('output_path', metavar='OUTPUT', help='the model output path')
    parser.add_argument('-n', '--checkpoints-num', dest='num_checkpoints', type=int, default=10,
                        help='number of checkpoints to average (default is 10)')
    parser.add_argument('-w', '--working-dir', metavar='WORKING_DIR', dest='wdir', default=None,
                        help='the working directory for temporary files (default is os temp folder)')
    parser.add_argument('-d', '--debug', action='store_true', dest='debug', default=False,
                        help='prevents temporary files to be removed after execution')
    parser.add_argument('--log', dest='log_file', default=None, help='detailed log file')

    args, extra_argv = parser.parse_known_args(argv)
    if args.debug and args.wdir is None:
        raise CLIArgsException(parser, '"--debug" options requires explicit working dir with "--working-dir"')

    return args, parse_extra_argv(parser, extra_argv)


def main(argv=None):
    args, extra_argv = parse_args(argv)
    activity = TrainActivity(args, extra_argv, wdir=args.wdir, log_file=args.log_file, delete_on_exit=not args.debug)
    activity.run()
