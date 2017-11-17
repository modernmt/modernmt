#!/usr/bin/env python

import os
import argparse
import sys
sys.path.insert(0, os.path.abspath(os.path.join(__file__, os.pardir, os.pardir)))
from cli.libs import shell

class IllegalArgumentException(Exception):
    def __init__(self, error):
        super(Exception, self).__init__(error)
        self.message = error

def main():
    # Set unbuffered stdout
    unbuffered = os.fdopen(sys.stdout.fileno(), 'w', 0)
    sys.stdout = unbuffered

    parser = argparse.ArgumentParser(description="Translate all files in the passed folder")
    parser.add_argument('input_folder1', metavar='INPUT', help='the xliff file file to translate, or the folder containing the xliff files to translate')
    parser.add_argument('input', metavar='INPUT', help='the xliff file file to translate, or the folder containing the xliff files to translate')

    args = parser.parse_args(sys.argv)

    if os.path.isfile(os.path.abspath(args.input)):
        input_file = os.path.abspath(args.input)
        if os.path.isfile(input_file):
            if not input_file.lower().endswith(('.xliff', '.sdlxliff')):
                raise IllegalArgumentException('Input file must be either .xliff or .sdlxliff')

        output_file = "/".join(input_file.split('/')[:-1]) + "/" + "out_" + input_file.split('/')[-1]
        translate_xliff_file(input_file, output_file)


    elif os.path.isdir(os.path.abspath(args.input)):
        input_folder = os.path.abspath(args.input)
        output_folder = os.path.join(input_folder, 'xliff_out')
        if os.path.exists(output_folder):
            if len(os.listdir(output_folder)) != 0:
                raise IllegalArgumentException('Output folder ' + output_folder + " already exists")
        else:
            os.mkdir(output_folder)

        for input_file in os.listdir(input_folder):
            input_file_abspath = os.path.join(input_folder, input_file)

            if os.path.isfile(input_file_abspath):
                if not input_file_abspath.lower().endswith(('.xliff', '.sdlxliff')):
                    continue

                output_file_abspath = os.path.join(output_folder, "out_" + input_file)
                translate_xliff_file(input_file_abspath, output_file_abspath)
    else:
        raise IllegalArgumentException('Input file/folder does not exist')

    return


def translate_xliff_file(input_file_abspath, output_file_abspath):
        mmt_script = os.path.abspath(os.path.join(os.path.dirname(os.path.realpath(__file__)), '..', 'mmt'))
        command = [mmt_script, 'translate', '--batch', '--xliff', '<', input_file_abspath, '>', output_file_abspath]

        print "Translating %s into %s..." % (input_file_abspath, output_file_abspath)
        shell.execute(" ".join(command))
        print "Done."


if __name__ == '__main__':
    main()

