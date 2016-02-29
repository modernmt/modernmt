#!/usr/bin/env python
import os, sys, stat

if(len(sys.argv) < 2):
    print "You must specify the name of the test to create as first argument"
    print "\nExample\n\t" + os.path.basename(__file__) + " MyNewTest\n"
    sys.exit(0)

test_name = sys.argv[1]

current_path = os.path.dirname(os.path.realpath(__file__))
tests_path = os.path.join(current_path, "tests", test_name)
if(os.path.isdir(tests_path)):
    print "FAIL: The test specified already exists under \"" + tests_path + "\""
else:
    os.makedirs(tests_path)
    launch_script_path = os.path.join(tests_path, "launch.sh")
    content = "#!/bin/sh\n" \
        "#Launch your test here\n" \
        "\n#Examples:\n" \
        "#java eu.modernmt." + test_name + " \"$@\"\n" \
        "#python " + test_name + ".py \"$@\"\n" \
        "#./" + test_name + ".sh \"$@\"\n" \
        "\n#Note: use \"$@\" to forward all the arguments to your script."
    with open(launch_script_path,'w') as f:
        f.write(content)
    default_permissions = os.stat(launch_script_path)
    os.chmod(launch_script_path, default_permissions.st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)

    json_path = os.path.join(tests_path, "info.json")
    content = "{\n" \
        "\t\"enabled\": true,\n" \
        "\t\"description\": \"This is a short description of " + test_name + "\",\n" \
        "\t\"full_description\": \"This is a complete and exhaustive description of " + test_name + "\"\n" \
        "}"
    with open(json_path,'w') as f:
        f.write(content)

    print "SUCCESS: Test structure created under \"" + tests_path + "\""
    print "SUCCESS: Create script \"launch.sh\""
    print "SUCCESS: Create json file \"info.json\""
    print "\nStructure created:"
    print "tests"
    print "|-" + test_name
    print "|  |-launch.sh"
    print "|  |-info.json"
    print "\nPlease, edit both \"launch.sh\" and \"info.json\" with the information regarding your new test.\n"