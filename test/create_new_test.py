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
    print "FAIL: The test specified already exists under \"" + tests_path + "\""
else:
    os.makedirs(tests_path)
    launch_script_path = tests_path + "launch.sh"
    content = "#!/bin/sh\n" \
        "#launch your test here\n" \
        "\n#Examples:\n" \
        "#java eu.modernmt.Class \"$@\"\n" \
        "#python myScript.py \"$@\"\n" \
        "\n#Use \"$@\" to forward all the arguments to your script."
    with open(launch_script_path,'w') as f:
        f.write(content)
    default_permissions = os.stat(launch_script_path)
    os.chmod(launch_script_path, default_permissions.st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)

    json_path = tests_path + "test.json"
    content = "{\n" \
        "\t\"enabled\": true,\n" \
        "\t\"name\": \"" + test_name + "\",\n" \
        "\t\"description\": \"Description of my the test\"\n" \
        "}"
    with open(json_path,'w') as f:
        f.write(content)

    print "SUCCESS: Test structure created under \"" + tests_path + "\""
    print "SUCCESS: Create script \"launch.sh\""
    print "SUCCESS: Create json file \"test.json\""
    print "\nPlease, edit both \"launch.sh\" and \"test.json\" with the information regarding your new test.\n"