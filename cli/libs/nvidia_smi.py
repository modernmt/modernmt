import subprocess


def list_gpus():
    try:
        stdout = subprocess.check_output(['nvidia-smi', '-L'])
        return map(lambda line: int(line.split(':')[0].replace('GPU ', '')),
                   [x for x in stdout.splitlines() if x.startswith('GPU ')])
    except subprocess.CalledProcessError:
        return []
    except OSError as e:
        if e.errno == 2:  # nvidia-smi not installed
            return []
        raise


def get_ram(gpu):
    try:
        stdout = subprocess.check_output(
            ['nvidia-smi', '--query-gpu=memory.total', '--format=csv,noheader,nounits', '--id=%d' % gpu])
        return int(stdout.strip()) * 1024 * 1024
    except subprocess.CalledProcessError:
        return 0
    except OSError as e:
        if e.errno == 2:   # nvidia-smi not installed
            return 0
        raise
