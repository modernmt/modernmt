import sys


def log(message, nl=True):
    sys.stderr.write(message)
    if nl:
        sys.stderr.write('\n')
    sys.stderr.flush()
