#!/usr/bin/env python
import json, re, requests, time
from itertools import izip
from optparse import OptionParser

class TagProjectionPricisionTest:

    MIN_PEE = 0.05
    TAG_RE = re.compile(r'<[^>]+>')

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
        try:
            results = self.launch()
            passed = self.check_results(results)
            json_response = {"passed": passed, "results": results}
        except Exception as e:
            json_response = {"passed": False, "error": str(e)}
        print json.dumps(json_response)

    def launch(self):
        self.log("Starting test")
        num_lines = sum(1 for line in open(self.__source_file))
        n = 0
        precision = 0
        err = 0
        different_number_of_tags = 0
        total_number_of_tags = 0
        number_of_lost_tags = 0
        different_number_of_chars = 0
        tot_query_time = 0
        translations = []
        references = []
        for source, translation, reference in izip(open(self.__source_file), open(self.__translation_file),
                                                   open(self.__reference_file)):
            source = source.strip()
            translation = translation.strip()
            reference = reference.strip()
            start_time = time.time()
            n += 1

            try:
                self.log(str(int(100*n/num_lines)) + "%\t#line:" +str(n) + "\tsource length:" + str(len(source)))
                results = self.query_engine(source, translation)
                tagged_translation = results['translation']
                if tagged_translation == reference:
                    precision += 1
                else:
                    self.log(tagged_translation + "|||" + reference)
                    tagged_number_of_tags = self.count_tags(tagged_translation)
                    reference_number_of_tags = self.count_tags(reference)
                    total_number_of_tags += reference_number_of_tags
                    if tagged_number_of_tags != reference_number_of_tags:
                        different_number_of_tags += 1
                        number_of_lost_tags += abs(reference_number_of_tags - tagged_number_of_tags)
                    else:
                        different_number_of_chars += 1
                tot_query_time += time.time() - start_time

                translations.append(translation)
                references.append(reference)

                self.log("precision: " + str(precision) + "\t#errors:" + str(err) + "\tdiff_tags"\
                      + str(different_number_of_tags) + "\ttot_num_of_tags" + str(total_number_of_tags) \
                      + "\tmissed_tags" + str(number_of_lost_tags)\
                      + "\tdiff_spaces" + str(different_number_of_chars))

                self.log("precision: " + str(round(100*precision/(num_lines - err)))\
                      + "%\t#errors:" + str(round(100*err/num_lines))\
                      + "%\tdiff_tags:" + str(round(100*different_number_of_tags/(num_lines - err)))\
                      + "%\tmissed_tags:" +str(round(100*number_of_lost_tags/total_number_of_tags))\
                      + "%\tdiff_chars:" + str(round(100*different_number_of_chars/(num_lines - err)))\
                      + "%\tavg_query_time [s]" + str(round(10000*tot_query_time/num_lines)))

            except Exception as e:
                err += 1
                self.log(str(e))

        avg_pee = self.get_avg_post_editing_effort(translations, references)

        return {'average PEE' : round(100*avg_pee)/100,
                'precision' : round(100*precision/(num_lines - err)),
                'encoding_errors' : round(100*err/num_lines),
                'diff_tags': round(100*different_number_of_tags/(num_lines - err)),
                'missed_tags': round(100*number_of_lost_tags/total_number_of_tags),
                'diff_chars': round(100*different_number_of_chars/(num_lines - err)),
                'avg_query_time [s]': round(10000*tot_query_time/num_lines)
                }


    def query_engine(self, source, translation):
        headers = {'content-type': 'application/json'}
        payload = {'s': source, 't': translation, 'f': 0}
        json_results = requests.get("http://localhost:"+str(self.__api_port)+"/tags-projection",
                                    params = payload, headers=headers)
        return json.loads(json_results.text)

    def count_tags(self, text):
        return len(TagProjectionPricisionTest.TAG_RE.findall(text))

    def check_results(self, results):
        return results['average PEE'] <= TagProjectionPricisionTest.MIN_PEE

    def get_avg_post_editing_effort(self, translations, references):
        url = 'http://api.mymemory.translated.net/computeMatch.php'

        data = {
            'sentences': translations,
            'reference_sentences': references
        }

        r = requests.post(url, data=json.dumps(data), headers={'Content-type': 'application/json'})
        body = r.json()

        if r.status_code != requests.codes.ok:
            raise Exception('Matecat Score service not available (' + str(r.status_code) + '): ' + body['error'])

        return 1 - sum(body)/len(body)

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