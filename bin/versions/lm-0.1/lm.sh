#!/bin/bash


main_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bin_dir="${main_dir}/bin"

export IRSTLM=$main_dir

lmtype="witten-bell"

input="$1"
output="$2"
order="$3"
tmp_dir="$4"
cpu_cores=$(grep -c ^processor /proc/cpuinfo)

parts=$cpu_cores

input_se=$tmp_dir/input.se$$
output_arpa=$tmp_dir/output.arpa$$

# add start and end symbols
cat $input | ${bin_dir}/add-start-end.sh > $input_se

# estimate LM (in parallel) and save in ARPA format; by deafult singleton pruning is not performed  
${bin_dir}/build-lm_without_gzip.sh -i $input_se -k ${parts} -o $output_arpa -n ${order} -s $lmtype -t ${tmp_dir}/stat_$$ -l ${tmp_dir}/LOG_$$

# create binary version of IRSTLM
${bin_dir}/compile-lm $output_arpa $output
