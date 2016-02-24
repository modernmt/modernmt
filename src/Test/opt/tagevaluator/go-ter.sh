#! /bin/bash

wdir=$(cd $(dirname $0) ; pwd)
bindir=$wdir/scripts
javadir=$wdir/lib

# Examples: 
#   go-ter.sh separate trial.ref trial.hyp scores/trial
#   go-ter.sh separate trial.ref trial.hyp scores/trial 1
#   go-ter.sh countSpaces trial.ref trial.hyp scores/trial 

option=$1
ref=$2
hyp=$3
of=$4

if [[ ( $# -ne 4 ) && ( $# -ne 5) || $option = *[hH]* ]]; then
    echo "Usage: $0 optionOnTags [none|separate|countSpaces] refFile hypFile outFile [verbosity (0=default,1,2)]"
    exit 1
fi

if [[ $# -eq 5 ]]; then
verbose=$5
if [[ $verbose != [012] ]]; then
    echo "Wrong verbosity level ($verbose)"
    echo "Usage: $0 optionOnTags [none|separate|countSpaces] refFile hypFile outFile [verbosity (0=default,1,2)]"
    exit 1
fi
else
verbose=0
fi


if [[ $option != "none" && 
      $option != "separate" && 
      $option != "countSpaces" ]]; then
    echo "argument error"
    echo "Usage: $0 optionOnTags [none|separate|countSpaces] refFile hypFile outFile"
    exit 1
fi

if [[ ! -e $ref ]]; then
    echo "File >>$ref<< does not exist"
    exit 1
fi
if [[ ! -e $hyp ]]; then
    echo "File >>$hyp<< does not exist"
    exit 1
fi


if [[ $option = "none" ]]; then
    cat $ref | perl $bindir/compactTags.pl |\
               awk '{print $0,"(0.0-"c++")"}' > __ref__$$
    cat $hyp | perl $bindir/compactTags.pl |\
               awk '{print $0,"(0.0-"c++")"}' > __hyp__$$

elif [[ $option = "separate" ]]; then
    cat $ref | perl $bindir/addSpacesAroundTags.pl |\
               perl $bindir/compactTags.pl |\
               awk '{print $0,"(0.0-"c++")"}' > __ref__$$
    cat $hyp | perl $bindir/addSpacesAroundTags.pl |\
               perl $bindir/compactTags.pl |\
               awk '{print $0,"(0.0-"c++")"}' > __hyp__$$

elif [[ $option = "countSpaces" ]]; then
    cat $ref | perl $bindir/compactTags.pl |\
               perl $bindir/countSpacesAroundTags.pl |\
               awk '{print $0,"(0.0-"c++")"}' > __ref__$$
    cat $hyp | perl $bindir/compactTags.pl |\
               perl $bindir/countSpacesAroundTags.pl |\
               awk '{print $0,"(0.0-"c++")"}' > __hyp__$$
fi

tmpLog=${of}.tmpLog.$$
java -Dfile.encoding=UTF8 -jar $javadir/tercom.7.25.jar -s -r __ref__$$ -h __hyp__$$ -n $of >& ${tmpLog}
rm __ref__$$ __hyp__$$

perl $bindir/evalTags.pl ${of}.pra_more $verbose >> $tmpLog
tail -n -1 $tmpLog
cat $tmpLog 1>&2
rm ${of}*
