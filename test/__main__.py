import argparse

# noinspection PyUnresolvedReferences
from testcases.onlinelearning import *
# noinspection PyUnresolvedReferences
from testcases.training import *
# noinspection PyUnresolvedReferences
from testcases.privacy import *

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Test suite for ModernMT',
                                     usage='%(prog)s [-h] [TEST_CASE]', prog='test')
    parser.add_argument('test_case', help='run the specified Test Case (run all available tests by default)',
                        nargs='?', default=None)
    args = parser.parse_args()

    if args.test_case is not None:
        suite = unittest.TestLoader().loadTestsFromTestCase(eval(args.test_case))
        unittest.TextTestRunner(verbosity=2).run(suite)
    else:
        unittest.main(verbosity=2)
