import subprocess


def list_gpus():
    try:
        stdout = subprocess.check_output(['nvidia-smi', '-L'])
        return map(lambda line: int(line.split(':')[0].replace('GPU ', '')),
                   [x for x in stdout.splitlines() if x.startswith('GPU ')])
    except subprocess.CalledProcessError:
        return []


def get_ram(gpu):
    try:
        stdout = subprocess.check_output(
            ['nvidia-smi', '--query-gpu=memory.total', '--format=csv,noheader,nounits', '--id', gpu])
        return int(stdout.strip())
    except subprocess.CalledProcessError:
        return 0
