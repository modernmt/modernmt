#!/bin/sh
#Launch your test here

wdir=$(cd $(dirname $0) ; pwd)

python ${wdir}/tagevaluator.py "$@"
