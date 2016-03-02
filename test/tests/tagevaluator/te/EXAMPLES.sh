#! /bin/sh

sDir=$(cd $(dirname $0) ; pwd)
cd $sDir

# no args
./go-ter.sh 
echo

# ok
./go-ter.sh separate trial.ref trial.hyp scores/trial 1 2>LOG
echo

# ok, no ERR
./go-ter.sh separate trial.ref trial.hyp scores/trial 1 2>/dev/null
echo


# wrong FIRST ARG
./go-ter.sh sep trial.ref trial.hyp scores/trial 1 2>/dev/null
echo

# challenging data
./go-ter.sh separate extra_data/TagBenchmark-v0.1.it  extra_data/TagBenchmark-v0.1.en.trans scores/trial 2>/dev/null
echo
