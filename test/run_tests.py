#!/usr/bin/env python
import os, sys, json
from argparse import ArgumentParser


class Tester:

    TESTS_DIRECTORY_NAME = "tests"
    JSON_DESCRIPTION_FILE_NAME = "info.json"
    ERR_LOG_FILE_NAME = "err.log"
    ROOT_DIR = os.path.dirname(os.path.realpath(__file__))
    TESTS_PATH = os.path.join( ROOT_DIR, TESTS_DIRECTORY_NAME)

    def run_test(self, test_name, verbose = True):
        import subprocess
        test_path = os.path.join(Tester.TESTS_PATH,test_name)
        os.chdir(test_path)
        process = subprocess.Popen(['./launch.sh'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, err = process.communicate()
        os.chdir(Tester.ROOT_DIR)

        try:
            json_information = json.loads(out)
            if 'passed' not in json_information:
                raise ValueError("The key 'passed' is missing")
            if verbose:
                print json.dumps(json_information, indent=4, separators=(',', ': '))
            return json_information
        except Exception, e:
            json_error = {'passed': False, 'error': str(e), 'test_output': out}
            if verbose:
                print json.dumps(json_error, indent=4, separators=(',', ': '))
            return json_error

    def run_all(self):
        failed_tests = []
        available_tests = sorted(self.get_available_tests())
        for (test_name, test_dir) in available_tests:
            json_file_path = os.path.join(test_dir,Tester.JSON_DESCRIPTION_FILE_NAME)
            if self.check_well_formed_info_json(json_file_path) == True:
                with open(json_file_path) as data_file:
                    json_information = json.load(data_file)
                enabled = json_information['enabled']
                if enabled:
                    print "~~~~ " + test_name + " ~~~~"
                    print "Test description: " + json_information['description'] + "\n"
                    json_info = self.run_test(test_name, False)
                    if not json_info['passed']:
                        failed_tests.append(test_name)
                    print json.dumps(json_info, indent=4, separators=(',', ': '))
                    print "\n"
        if len(failed_tests) == 0:
            print "All tests have been passed."
        else:
            print "The folliwing tests failed:"
            print ",\n".join(failed_tests)

    def list_all_tests(self):
        available_tests = sorted(self.get_available_tests())
        enabled_tests = []
        disabled_tests = []
        malformed_tests = []
        if len(available_tests) == 0:
            print "No test found."
            return
        for (test_name, test_dir) in available_tests:
            json_file_path = os.path.join(test_dir,Tester.JSON_DESCRIPTION_FILE_NAME)
            is_json_valid = self.check_well_formed_info_json(json_file_path)
            if is_json_valid == True:
                with open(json_file_path) as data_file:
                    json_information = json.load(data_file)
                enabled = json_information['enabled']
                info = (test_name, json_information['enabled'], json_information['description'])
                if enabled:
                    enabled_tests.append(info)
                else:
                    disabled_tests.append(info)
            else:
                malformed_tests.append((test_name, is_json_valid))

        #Print information
        print "\n## ENABLED TEST ##"
        print "#%s\t%s\t%s" % ("Test_name", "Enabled", "Description")
        for test in enabled_tests:
            print "%s\t%s\t\"%s\"\n" % test
        print "\n## DISABLED TEST ##"
        print "#%s\t%s\t%s" % ("Test_name", "Enabled", "Description")
        for test in disabled_tests:
            print "%s\t%s\t\"%s\"\n" % test
        print "\n## MALFORMED TEST ##"
        print "#%s\t%s\t%s" % ("Test_name", "Status", "Error Description")
        for test in malformed_tests:
            print "%s\tmalformed\t\"%s\"\n" % test
        print "\n"

    def change_status_test(self, test_name, status):
        test_dir = os.path.join(Tester.TESTS_PATH,test_name)
        json_file_path = os.path.join(test_dir,Tester.JSON_DESCRIPTION_FILE_NAME)
        is_json_valid = self.check_well_formed_info_json(json_file_path)
        if is_json_valid == True:
            with open(json_file_path) as data_file:
                json_information = json.load(data_file)
            json_information['enabled'] = status
            new_json_string = json.dumps(json_information, indent=4, separators=(',', ': '))
            with open(json_file_path, 'w') as json_file:
                json_file.write(new_json_string)
            print "SUCCESS"
        else:
            print is_json_valid

    def print_info(self, test_name):
        test_dir = os.path.join(Tester.TESTS_PATH,test_name)
        json_file_path = os.path.join(test_dir,Tester.JSON_DESCRIPTION_FILE_NAME)
        is_json_valid = self.check_well_formed_info_json(json_file_path)
        if is_json_valid == True:
            with open(json_file_path) as data_file:
                json_information = json.load(data_file)
            print json.dumps(json_information, indent=4, separators=(',', ': '))
        else:
            print is_json_valid
    def get_available_tests(self):
        return [(test, os.path.join(Tester.TESTS_PATH,test)) for test in os.listdir(Tester.TESTS_PATH) if os.path.isdir(os.path.join(Tester.TESTS_PATH,test))]

    def check_well_formed_info_json(self, json_file_path):
        try:
            with open(json_file_path) as data_file:
                json_information = json.load(data_file)
            mandatory_keys = ["enabled", "description", "full_description", "author"]
            for key in mandatory_keys:
                if key not in json_information:
                    raise ValueError('The key \'' + key + '\' is missing')
            if not isinstance(json_information['enabled'], bool):
                raise ValueError('The value of the key \'enabled\' must be a boolean')
            return True
        except ValueError, e:
               return str(e)


if __name__ == "__main__":
    parser = ArgumentParser()

    group = parser.add_mutually_exclusive_group()
    group.add_argument('-a', "-all", dest="all", action='store_true',
                      help="Run all the enabled tests")
    group.add_argument('-n', "--name", dest="single", metavar='TEST_NAME',
                      help="Run the specified test")
    group.add_argument('-l', "--list", dest="list", action='store_true',
                      help="List all the available tests")
    group.add_argument('-e', "--enable", dest="enable", metavar='TEST_NAME',
                      help="Enable the specified test")
    group.add_argument('-d', "--disable", dest="disable", metavar='TEST_NAME',
                      help="Disable the specified test")
    group.add_argument('-i', "--info", dest="info", metavar='TEST_NAME',
                      help="Print information about the specified test")
    args = parser.parse_args()

    tester = Tester()
    if args.all:
        tester.run_all()
    elif args.list:
        tester.list_all_tests()
    elif args.single is not None:
        tester.run_test(args.single)
    elif args.enable is not None:
        tester.change_status_test(args.enable, True)
    elif args.disable is not None:
        tester.change_status_test(args.disable, False)
    elif args.info is not None:
        tester.print_info(args.info)
    else:
        parser.print_help()