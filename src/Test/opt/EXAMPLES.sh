#! /bin/sh

# no args
tagevaluator/go-ter.sh 

# ok
tagevaluator/go-ter.sh separate tagevaluator/trial.ref tagevaluator/trial.hyp tagevaluator/scores/trial 1

# ok, no ERR
tagevaluator/go-ter.sh separate tagevaluator/trial.ref tagevaluator/trial.hyp tagevaluator/scores/trial 1 2>/dev/null


# wrong FIRST ARG
tagevaluator/go-ter.sh sep tagevaluator/trial.ref tagevaluator/trial.hyp tagevaluator/scores/trial 1 2>/dev/null

