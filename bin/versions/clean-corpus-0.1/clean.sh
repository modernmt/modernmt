#!/bin/bash

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/scripts"

corpus_in="$1"
src="$2"
trg="$3"
corpus_out="$4"

clean_ratio=3
clean_min=1
clean_max=80

${script_dir}/clean-corpus-n-ratio.perl -ratio ${clean_ratio} ${corpus_in} $src $trg ${corpus_out} ${clean_min} ${clean_max}