#!/usr/bin/env python

import re
from itertools import izip
from optparse import OptionParser

TAG_RE = re.compile(r'<[^>]+> ?')
def remove_tags(text):
    return TAG_RE.sub('', text)

if __name__ == "__main__":
    parser = OptionParser()
    parser.add_option("--source_file", dest="source_file", metavar='Source file',
                      help="the file containing the source sentences")
    parser.add_option("--translation_file", dest="translation_file", metavar='Translation file',
                      help="the file containing the translation sentences")
    parser.add_option("--filtered_source_file", dest="filtered_source_file", metavar='Filtered source file',
                      help="the file containing only the source sentences with tags")
    parser.add_option("--test_file", dest="test_file", metavar='Test file',
                      help="the file containing the translation from which tags have been stripped")
    parser.add_option("--filtered_reference_file", dest="filtered_reference_file", metavar='Filtered reference file',
                      help="the file containing only the translation sentences with tags")
    options, _ = parser.parse_args()

    if not options.source_file or not options.translation_file or not options.filtered_source_file \
            or not options.test_file or not options.filtered_source_file:
        parser.error('All the arguments are required')

    i = 0
    with open(options.filtered_source_file, 'a') as new_source, open(options.test_file, 'a') as test, open(options.filtered_reference_file, 'a') as new_translation:
        for source, translation in izip(open(options.source_file), open(options.translation_file)):
            s_no_tag = remove_tags(source)
            t_no_tag = remove_tags(translation)
            if s_no_tag != source and t_no_tag != translation:
                i += 1
                new_source.write(source)
                test.write(t_no_tag)
                new_translation.write(translation)
                print i
    print "DONE"