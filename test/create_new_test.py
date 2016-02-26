#!/usr/bin/env python
import os, sys, stat

if(len(sys.argv) < 2):
    print "You must specify the name of the test to create as first argument"
    print "\nExample\n\t" + os.path.basename(__file__) + " MyNewTest\n"
    sys.exit(0)

test_name = sys.argv[1]

current_path = os.path.dirname(os.path.realpath(__file__))
tests_path = current_path + os.path.sep + "tests" + os.path.sep + test_name + os.path.sep
if(os.path.isdir(tests_path)):
    print "The test specified already exists under \"" + tests_path + "\""
else:
    os.makedirs(tests_path)
    launch_script_path = tests_path + "launch.sh"
    with open(launch_script_path,'w') as f:
        f.write("#!/bin/sh\n"
                "#launch your test here\n"
                "\n#Examples:"
                "\n#java eu.modernmt.Class \"$@\""
                "\n#python myScript.py \"$@\""
                "\n\n#Use \"$@\" to forward all the arguments to your script.")
    default_permissions = os.stat(launch_script_path)
    os.chmod(launch_script_path, default_permissions.st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    print "Test structure created under \"" + tests_path + "\""
