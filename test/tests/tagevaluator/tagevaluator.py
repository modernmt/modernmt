#!/usr/bin/env python

import os, sys, subprocess, json
from optparse import OptionParser


# Global vars
#
evaluator_cmd = "./te/go-ter.sh";
out_file      = "./te/scores/trial";

parser = OptionParser()
parser.add_option("-l", "--log-file", dest="log_file",
		  default="",
                  help="the file to save the log info")
parser.add_option("-r", "--ref-file", dest="ref_file",
		  default="./te/trial.ref",
		  help="the reference file (mandatory)")
parser.add_option("-y", "--hyp-file", dest="hyp_file",
		  default="./te/trial.hyp",
		  help="the hypothesis file (mandatory)")
parser.add_option("-t", "--option-on-tag", dest="option_on_tag",
		  default="separate",
		  help="the way tag are processed: must be one of [none|separate|countSpaces] (mandatory)")
parser.add_option("-v", "--verbosity-level", dest="verbosity_level",
		  default="0",
		  help="the verbosity level: should be 0=default,1,2")


# Run
#
(options, args) = parser.parse_args()

if options.option_on_tag == "" or options.ref_file == "" or options.hyp_file == "":
    print "missing a mandatory option" ; parser.print_help() ; exit(1)

cmd_list = [evaluator_cmd, options.option_on_tag, options.ref_file, options.hyp_file, out_file, options.verbosity_level]

# invocate the lowlevel command
#
p = subprocess.Popen(cmd_list, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
out, err = p.communicate()

# process the output and error
#
tag_error_rate = float(out)
if options.log_file != "":
    with open(options.log_file, "w") as out_log:
        for line in err:
            out_log.write(line)

# organize final results
#
passed_flag = True
results_dict = { "tag_error_rate" : tag_error_rate,
                 "reference_file" : options.ref_file,
                 "hypothesis_file" : options.hyp_file}
if options.log_file != "":
    results_dict["log_file"] = options.log_file

final_dict = { "passed" : passed_flag,
               "results" : results_dict }
final_jstring = json.dumps(final_dict)
print final_jstring

