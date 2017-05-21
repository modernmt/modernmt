import sys


def _log(message, nl=True):
    sys.stderr.write(message)
    if nl:
        sys.stderr.write('\n')
    sys.stderr.flush()


def assert_equals(value, expected):
    if not value == expected:
        raise AssertionError('assert_equals failed, found "%s" but expected "%s"' % (str(value), str(expected)))
    else:
        _log('assert success: "%s" == "%s"' % (str(value), str(expected)))


def assert_size_increased(new_value, old_value):
    if not new_value.gt(old_value):
        raise AssertionError(
            'assert_size_increased failed, new size is "%s" but old is "%s"' % (str(new_value), str(old_value)))

    _log('assert success: "%s" > "%s"' % (str(new_value), str(old_value)))
