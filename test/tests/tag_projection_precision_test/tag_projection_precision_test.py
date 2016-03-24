#!/usr/bin/env python
import json, requests, time
from itertools import izip
from optparse import OptionParser

class TagProjectionPricisionTest:

    def __init__(self, api_port, source_file, translation_file, reference_file, verbosity_level):
        self.__api_port = api_port
        self.__source_file = source_file
        self.__translation_file = translation_file
        self.__reference_file = reference_file
        self.__verbosity_level = verbosity_level

    def log(self, message):
        if int(self.__verbosity_level) > 0:
            print message

    def start_test(self):
        self.log("Starting test")
        num_lines = sum(1 for line in open(self.__source_file))
        n = 0
        precision = 0
        err = 0
        for source, translation, reference in izip(open(self.__source_file), open(self.__translation_file),
                                                   open(self.__reference_file)):
            source = source.strip()
            translation = translation.strip()
            reference = reference.strip()
            start_time = time.time()
            n += 1
            try:
                print str(int(100*n/num_lines)) + "%\tid:" +str(n) + "\t" + str(len(source))
                results = self.query_engine(source, translation)
                tagged_translation = results['translation']
                if tagged_translation == reference:
                    precision += 1
                query_time = time.time() - start_time

                #self.log(str(int(100*n/num_lines)) + "%")
            except Exception as e:
                err += 1
                print "Error"
        print str(int(100*precision/num_lines)) + "\t #precision:" + str(precision)
        print str(int(100*err/num_lines)) + "\t #err:" + str(err)

    def query_engine(self, source, translation):
        headers = {'content-type': 'application/json'}
        payload = {'s': source, 't': translation}
        json_results = requests.get("http://localhost:"+str(self.__api_port)+"/tags-projection",
                                    params = payload, headers=headers)
        return json.loads(json_results.text)


if __name__ == "__main__":
    parser = OptionParser()
    parser.add_option("-p", "--api-port", dest="api_port", default="8045", metavar='API port',
                    help="the port of the translation engine (default 8045)")
    parser.add_option("-s", "--source_file", dest="source_file", default="data/TagBenchmark.src.en",
                      metavar='Source file', help="the file containing the source sentences \
                      (default: data/TagBenghmark.src.en")
    parser.add_option("-t", "--translation_file", dest="translation_file", default="data/TagBenchmark.test.it",
                      metavar='Translation file',
                      help="the file containing the translation from which tags have been stripped \
                      (default: data/TagBenghmark.test.it)")
    parser.add_option("-r", "--reference_file", dest="reference_file", default="data/TagBenchmark.ref.it",
                      metavar='Reference file',
                      help="the file containing the translation sentences with tags \
                      (default: data/TagBenghmark.ref.it)")
    parser.add_option("-v", "--verbosity-level", dest="verbosity_level",
              default="0",
              help="the verbosity level: should be 0=default,1")
    options, _ = parser.parse_args()

    if options.source_file or options.translation_file or options.reference_file:
        if not options.source_file or not options.translation_file or not options.reference_file:
            parser.error('The source_file, translation_file and reference_file arguments are required')
            parser.print_help()

    test = TagProjectionPricisionTest(**vars(options))
    test.start_test()