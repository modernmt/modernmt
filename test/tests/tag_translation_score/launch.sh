#!/bin/sh
#Launch your test here

wdir=$(cd $(dirname $0) ; pwd)

if [ $# -eq 0 ]
then
    help=$(python ${wdir}/tts/tag_translation_score.py -h)
    echo '{"passed": true, "results": {"help": "'$help'"}}'
else
    python ${wdir}/tts/tag_translation_score.py "$@"
fi
