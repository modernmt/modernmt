#!/bin/bash

lang=$1
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/scripts"

perl ${script_dir}/tokenizer.perl -b -X -l $lang -no-escape
