#!/bin/bash

bin_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/bin"

lmtype=ImprovedKneserNey

input="$1"
output="$2"
order="$3"

${bin_dir}/tlm -ps=NO -tr="cat $input | ${bin_dir}/add-start-end.sh" -oBIN=$output -n=$order -lm=${lmtype}