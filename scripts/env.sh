#!/bin/bash

# Constants
CONTEXT_ANALYZER_PLACEHOLDER="__CONTEXT_ANALYZER_DOMAIN__"

# Args
engine="$1"

if [ -z $engine ]; then
	engine="default"
fi

# File system

home=$( cd "$( dirname $( dirname "${BASH_SOURCE[0]}" ) )" && pwd )

bin_dir=$home/bin
context_analyzer_home=$bin_dir/context-analyzer
moses_home=$bin_dir/mosesdecoder
tokenizer_home=$bin_dir/tokenizer
corpus_cleaner_home=$bin_dir/clean-corpus
language_model_home=$bin_dir/lm

scripts_dir=$home/scripts

engine_dir=$home/engines/$engine
engine_runtime_dir=$engine_dir/run
engine_temp_dir=$engine_dir/tmp
engine_log_dir=$engine_dir/logs
engine_build_log_dir=$engine_log_dir/build
engine_context_analyzer_dir=$engine_dir/data/context-analyzer
engine_context_analyzer_index_dir=$engine_context_analyzer_dir/index
engine_moses_dir=$engine_dir/data/moses
engine_models_dir=$engine_dir/data/models