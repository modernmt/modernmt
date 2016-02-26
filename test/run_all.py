#!/usr/bin/env python
import os, json
class Tester:

    TESTS_DIRECTORY_NAME = "tests"
    JSON_DESCRIPTION_FILE_NAME = "test.json"

    def run_all(self):
        print "TODO"

    def list_all_tests(self, verbose):
        available_test = sorted(self.get_available_tests())
        if len(available_test) == 0:
            print "No test found."
            return
        for test_dir in available_test:
            json_file_path = test_dir + os.path.sep + Tester.JSON_DESCRIPTION_FILE_NAME
            try:
                json_description = json.load(json_file_path)
                enabled = True
                if enabled:
                    None #Add to enabled
                else:
                    None #Add to disabled
            except ValueError:
                None #Add to failed
        print "Available tests:\n"

    def get_available_tests(self):
        current_path = os.path.dirname(os.path.realpath(__file__))
        tests_path = current_path + os.path.sep + Tester.TESTS_DIRECTORY_NAME
        return [test for test in os.listdir(tests_path) if os.path.isdir(test)]

if __name__ == "__main__":
    Tester().list_all_tests()