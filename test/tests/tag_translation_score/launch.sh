#!/bin/sh
#Launch your test here

wdir=$(cd $(dirname $0) ; pwd)

if [ $# -eq 0 ]
then
    python ${wdir}/tts/tag_translation_score.py -l ${wdir}/LOG ${wdir}/tts/trial.src
else
    python ${wdir}/tts/tag_translation_score.py "$@"
fi
