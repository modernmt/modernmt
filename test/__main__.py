import argparse
import os
import sys

import unittest


def main(argv=None):
    parser = argparse.ArgumentParser(description='Test suite for ModernMT',
                                     usage='%(prog)s [-h] [TEST_CASE]', prog='test')
    parser.add_argument('test_case', help='run the specified Test Case (run all available tests by default)',
                        nargs='?', default=None)
    args = parser.parse_args(argv)

    if args.test_case is not None:
        suite = unittest.TestLoader().loadTestsFromTestCase(eval(args.test_case))
        unittest.TextTestRunner(verbosity=2).run(suite)
    else:
        unittest.main(verbosity=2)


if __name__ == '__main__':
    __this_dir = os.path.dirname(os.path.realpath(__file__))
    __home = os.path.abspath(os.path.join(__this_dir, os.pardir))
    sys.path.insert(0, __home)

    # noinspection PyUnresolvedReferences
    from testcases.training import *
    # noinspection PyUnresolvedReferences
    from testcases.privacy import *

    # noinspection PyUnresolvedReferences
    # from testcases.onlinelearning import *

    main()
