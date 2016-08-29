#!/usr/bin/env python
import json, re, requests, time
from itertools import izip
from optparse import OptionParser


class TagProjectionPricisionTest:
    MIN_PEE = 0.05
    TAG_RE = re.compile(r'<[^>]+>')
    TAG_POS_RE = re.compile(r'.*?<[^>]+>.*?')

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
        less_tags = 0
        more_tags = 0
        total_number_of_tags = 0
        number_of_lost_tags = 0
        different_number_of_chars = 0
        wrong_positions = 0
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
                self.log(str(int(100 * n / num_lines)) + "%\t#line:" + str(n) + "\tsource length:" + str(len(source)))
                results = self.query_engine(source, translation)
                alignments = results['alignments']
                tagged_translation = results['translation']
                if tagged_translation == reference:
                    precision += 1
                else:
                    self.log(source + "|||" + tagged_translation + "|||" + reference + "\n" + str(alignments))
                    tagged_number_of_tags = self.count_tags(tagged_translation)
                    reference_number_of_tags = self.count_tags(reference)
                    total_number_of_tags += reference_number_of_tags
                    if tagged_number_of_tags != reference_number_of_tags:
                        if tagged_number_of_tags > reference_number_of_tags:
                            more_tags += 1
                        else:
                            less_tags += 1
                        number_of_lost_tags += abs(reference_number_of_tags - tagged_number_of_tags)
                    else:
                        if self.tag_position_analysys(tagged_translation, reference):
                            different_number_of_chars += 1
                        else:
                            wrong_positions += 1
                tot_query_time += time.time() - start_time

                translations.append(translation)
                references.append(reference)

                self.log("precision: " + str(precision)
                         + "\t#errors:" + str(err)
                         + "\tmore_tags" + str(more_tags)
                         + "\tless_tags" + str(less_tags)
                         + "\ttot_num_of_tags" + str(total_number_of_tags)
                         + "\tmissed_tags" + str(number_of_lost_tags)
                         + "\twrong_positions" + str(wrong_positions)
                         + "\tdiff_spaces" + str(different_number_of_chars))

                self.log("precision: " + str(round(100 * precision / (num_lines - err), 2))
                         + "%\t#errors:" + str(round(100 * err / num_lines, 2))
                         + "%\tmore_tags:" + str(round(100 * more_tags / (num_lines - err), 2))
                         + "%\tless_tags:" + str(round(100 * less_tags / (num_lines - err), 2))
                         + "%\tmissed_tags:" + str(round(100 * number_of_lost_tags / total_number_of_tags, 2))
                         + "%\twrong_positions:" + str(round(100 * wrong_positions / (num_lines - err), 2))
                         + "%\tdiff_chars:" + str(round(100 * different_number_of_chars / (num_lines - err), 2))
                         + "%\tavg_query_time [s]" + str(round(10000 * tot_query_time / num_lines, 2)))

            except Exception as e:
                err += 1
                self.log(str(e))

        avg_pee = self.get_avg_post_editing_effort(translations, references)

        return {'average PEE': round(100 * avg_pee) / 100,
                'precision': round(100 * precision / (num_lines - err), 2),
                'encoding_errors': round(100 * err / num_lines, 2),
                'more_tags': round(100 * more_tags / (num_lines - err), 2),
                'less_tags': round(100 * less_tags / (num_lines - err), 2),
                'missed_tags': round(100 * number_of_lost_tags / total_number_of_tags, 2),
                'wrong_positions': round(100 * wrong_positions / (num_lines - err), 2),
                'diff_chars': round(100 * different_number_of_chars / (num_lines - err), 2),
                'avg_query_time [s]': round(10000 * tot_query_time / num_lines, 2)
                }

    def query_engine(self, source, translation):
        headers = {'content-type': 'application/json'}
        payload = {'s': source, 't': translation, 'sl':'en', 'tl':'it','d': 1}
        json_results = requests.get("http://localhost:" + str(self.__api_port) + "/tags-projection",
                                    params=payload, headers=headers)
        return json.loads(json_results.text)['data']

    def count_tags(self, text):
        return len(TagProjectionPricisionTest.TAG_RE.findall(text))

    def tag_position_analysys(self, tagged_translation, reference):
        tagged_translation = re.sub("\\s+", r'', tagged_translation)
        reference = re.sub("\\s+", r'', reference)
        tagged_tags = TagProjectionPricisionTest.TAG_POS_RE.findall(tagged_translation)
        reference_tags = TagProjectionPricisionTest.TAG_POS_RE.findall(reference)
        return tagged_tags == reference_tags

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

        return 1 - sum(body) / len(body)


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
