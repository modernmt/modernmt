#!/usr/bin/env python

# Author: Roldano Cattoni, 02 March 2016

import sys, os, requests, json, string, pprint, codecs, re, time
import argparse


# check if an engine is providing translation service on host:port
#
def check_connection_with_engine(host, port):
    try:
        requests.get("http://%s:%s/translate?q=hello" % (host, str(port)))
    except Exception, e:
        return False
    return True



# verify that tags in the "tra_line" are the same of those in "src_line";
#  equality is based on number, name and type (content-full or self-closing).
#  Moreover tags in "tra_line" must be space-separated from words.
#
def verify_tags(src_line, tra_line):
    global tsc_total_occ, tsc_total_match, tco_total_occ, tco_total_match, tst_total_occ, tst_total_match

    src_stats = extract_tag_stats(src_line)
    tra_stats = extract_tag_stats(tra_line)

    # manage self-closing tags counts
    #
    tsc_dict_src = src_stats['TSC']
    tsc_dict_tra = tra_stats['TSC']
    # check if tags in tra_line are in src_line
    for tag_span, tra_occ in tsc_dict_tra.items():
        if tag_span in tsc_dict_src:
            src_occ = tsc_dict_src[tag_span]
            tsc_total_occ += max(src_occ, tra_occ)
            tsc_total_match += min(src_occ, tra_occ)
        else:
            tsc_total_occ += tra_occ
    # check tags in src_line that are not in tra_line
    for tag_span, src_occ in tsc_dict_src.items():
        if not tag_span in tsc_dict_tra:
            tsc_total_occ += src_occ

    # manage comment tags counts
    #
    tco_dict_src = src_stats['TCO']
    tco_dict_tra = tra_stats['TCO']
    src_occ = tco_dict_src['_ALL_']
    tra_occ = tco_dict_tra['_ALL_']
    tco_total_occ += max(src_occ, tra_occ)
    tco_total_match += min(src_occ, tra_occ)

    # manage standard tags counts
    #
    tst_dict_src = src_stats['TST']
    tst_dict_tra = tra_stats['TST']
    # check if tags in tra_line are in src_line
    for tag_span, tra_occ in tst_dict_tra.items():
        if tag_span in tst_dict_src:
            src_occ = tst_dict_src[tag_span]
            tst_total_occ += max(src_occ, tra_occ)
            tst_total_match += min(src_occ, tra_occ)
        else:
            tst_total_occ += tra_occ
    # check tags in src_line that are not in tra_line
    for tag_span, src_occ in tst_dict_src.items():
        if not tag_span in tst_dict_tra:
            tst_total_occ += src_occ

    return


# extract tag statistics from a piece of text
#
def extract_tag_stats(text):
    # tag types: selfclosing (TSC), comment (TCO), standard (TST), closing (TCL)
    global tsc_regex, tco_regex, tst_regex, tcl_regex
    
    # selfclosing tags: store and delete them
    #
    tsc_dict = {}
    for match in tsc_regex.finditer(text):
        tag_span= match.group(1)
        if tag_span in tsc_dict:
            tsc_dict[tag_span] += 1
        else:
            tsc_dict[tag_span] = 1
    text = tsc_regex.sub('', text)

    # comments: store just counts (not the span, because it will be translated) and delete them
    #
    tco_dict = {}
    tag_span= "_ALL_"
    tco_dict[tag_span] = 0
    for match in tco_regex.finditer(text):
        tco_dict[tag_span] += 1
    text = tco_regex.sub('', text)

    # standard: delete closing tags, then store the open tags
    #
    text = tcl_regex.sub('', text)
    tst_dict = {}
    for match in tst_regex.finditer(text):
        tag_span= match.group(1)
        if tag_span in tst_dict:
            tst_dict[tag_span] += 1
        else:
            tst_dict[tag_span] = 1

    return { 'TSC' : tsc_dict, 'TCO' : tco_dict, 'TST' : tst_dict }


# -----------
# global vars
# -----------

# tag types: selfclosing (TSC), comment (TCO), standard (TST), closing (TCL)
    
tsc_pattern = r'<([^>]+)/>'
tsc_regex = re.compile(tsc_pattern);
tco_pattern = r'<!--(.+)-->'
tco_regex = re.compile(tco_pattern);
tst_pattern = r'<([^/!][^>]*)>'
tst_regex = re.compile(tst_pattern);
tcl_pattern = r'</[^>]+>'
tcl_regex = re.compile(tcl_pattern);

# counters

tsc_total_occ = 0
tsc_total_match = 0
tco_total_occ = 0
tco_total_match = 0
tst_total_occ = 0
tst_total_match = 0

# info

score_threshold = 0.7
translation_file = "/tmp/test_tags.tf." + str(os.getpid())
api_host = "localhost"

# process parameters
#

parser = argparse.ArgumentParser(description='Compute the translated-tag-score', prog='tag_translation_score')
parser.add_argument('src_file', nargs=1,
                    help="the file to be translated and evaluated")
parser.add_argument("-l", "--log-file", dest="log_file", default="", metavar='LOG_FILE',
                    help="the file where to store the translations (default /dev/null)")
parser.add_argument("-p", "--api-port", dest="api_port", default="8045", metavar='API_PORT',
                    help="the port of the translation engine (default 8045)")

args = parser.parse_args()

src_file = args.src_file[0]
api_port = args.api_port
log_file = args.log_file


# check engine
#
if not check_connection_with_engine(api_host, api_port):
    results_dict = { "error" : "cannot connect to engine at %s:%s" % (api_host, api_port) }
    final_dict = { "passed" : False, "results" : results_dict }
    final_jstring = json.dumps(final_dict)
    print final_jstring
    exit(1)
    

# translate sentences of src_file and save them in translation_file
#
start_time_sec = time.time()
with open(src_file) as in_f:
    with open(translation_file, 'w') as out_f:
        for line in in_f:
            data = {"q" : line}
            r = requests.get("http://localhost:"+str(api_port)+"/translate?", params = data)
            j = json.loads(r.text)
            tra = j["translation"]
            out_f.write(tra.encode("UTF-8")+'\n')
time_sec_translation = "%.1f s" % (time.time() - start_time_sec)

# now open src_file and translation_file, and compare line by line wrt tags
#
start_time_sec = time.time()
line_no = 0
with open(src_file) as in_src:
    with open(translation_file) as in_tra:
        for line_src in in_src:
            line_no += 1
            line_tra = in_tra.readline()
            verify_tags(line_src, line_tra)
time_sec_score_computation = "%.1f s" % (time.time() - start_time_sec)

# manage log_file
#
if log_file != "":
    os.system("cat %s > %s" % (translation_file, log_file))

# remove tmp file
#
os.remove(translation_file)

# compute final statistics
#
total_match = (tsc_total_match + tco_total_match + tst_total_match)
total_occ = (tsc_total_occ + tco_total_occ + tst_total_occ)
if total_occ != 0:
    tra_tag_score = total_match / float(total_occ)
else:
    tra_tag_score = "NaN"
    

passed_flag = tra_tag_score == "NaN" or (tra_tag_score >= score_threshold)
detail_dict = { "match ratio of selfclosing tags" : "%d/%d" % (tsc_total_match, tsc_total_occ),
                "match ratio of comment tags" : "%d/%d" % (tco_total_match, tco_total_occ),
                "match ratio of standard tags" : "%d/%d" % (tst_total_match, tst_total_occ) }
results_dict = { "translated_tag_score" : tra_tag_score,
                 "score_threshold" : score_threshold,
                 "time_for_score_computation" : time_sec_score_computation,
                 "time_for_translation" : time_sec_translation,
                 "details" : detail_dict }
if log_file != "":
    results_dict["log_file"] = log_file


final_dict = { "passed" : passed_flag, "results" : results_dict }
final_jstring = json.dumps(final_dict)
print final_jstring
