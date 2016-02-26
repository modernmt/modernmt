#!/usr/bin/env python

class Tester:

    def __init__(self):
        import json

    def run_all(self):
        import os
        current_path = os.path.dirname(os.path.realpath(__file__))
        tests_path = current_path + os.path.sep + "tests";
        for f in os.listdir(tests_path):
            print f

if __name__ == "__main__":
    Tester().run_all()