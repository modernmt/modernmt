#! /bin/bash

set -m # Enable Job Control



function usage()
{
cmnd=$(basename $0);
cat<<EOF

$cmnd - train and/or test a probabilistic latent semantic model

USAGE:
$cmnd [options]

TRAINING OPTIONS:

-c arg    Collection of training documents e.g. 'gunzip -c docs.gz'
-d arg    Dictionary file (default dictionary)
-f        Force to use existing dictionary
-m arg    Output model file e.g. model
-n arg    Number of topics (default 100)
-i arg    Number of training iterations (default 20)
-t arg    Temporary working directory (default ./stat_PID)
-p arg    Prune words with counts < arg (default 2)
-k arg    Number of processes (default 5)

-r arg    Model output file in readable format
-s arg    Put top arg frequent words in special topic 0
-l arg    Log file (optional)
-v        Verbose
-h        Show this message


TESTING OPTIONS

-c arg    Testing documents e.g. test
-d arg    Dictionary file (default dictionary)
-m arg    Output model file e.g. model
-n arg    Number of topics (default 100)
-u  arg   Output unigram distribution
-i arg    Number of training iterations (default 20)
-t arg    Temporary working directory (default ./stat_PID)
-l arg    Log file (optional)
-v        Verbose
-h        Show this message


EOF
}



if [ ! $IRSTLM ]; then
echo "Set IRSTLM environment variable with path to irstlm"
exit 2
fi

#paths to scripts and commands in irstlm
scr=$IRSTLM/bin
bin=$IRSTLM/bin
gzip=`which gzip 2> /dev/null`;
gunzip=`which gunzip 2> /dev/null`;

#default parameters
tmpdir=stat_$$
data=""
topics=100
splits=5
iter=20
prunefreq=2
spectopics=0
logfile="/dev/null"
verbose=""
unigram=""

dict="dictionary"
forcedict=""
model=""
txtfile="/dev/null"

while getopts "hvfc:m:r:k:i:n:t:d:p:s:l:u:" OPTION
do
case $OPTION in
h)
usage
exit 0
;;
v)
verbose="--verbose";
;;
c)
data=$OPTARG
;;
m)
model=$OPTARG
;;
r)
txtfile=$OPTARG
;;
k)
splits=$OPTARG
;;
i)
iter=$OPTARG
;;
t)
tmpdir=$OPTARG
;;
d)
dict=$OPTARG
;;
f)
forcedict="TRUE"
;;
p)
prunefreq=$OPTARG
;;
s)
spectopics=$OPTARG
;;
n)
topics=$OPTARG
;;
l)
logfile=$OPTARG
;;
u)
unigram=$OPTARG
;;

?)
usage
exit 1
;;
esac
done

if [ $verbose ]; then
echo data=$data  model=$model  topics=$topics iter=$iter dict=$dict
logfile="/dev/stdout"
fi

if [ ! $unigram ]; then

#training branch

if [ ! "$data" -o ! "$model" ]; then
usage
exit 1
fi

if [ -e $model ]; then
echo "Output file $model already exists! either remove or rename it."
exit 1
fi

if [ -e $txtfile -a $txtfile != "/dev/null" ]; then
echo "Output file $txtfile already exists! either remove or rename it."
exit 1
fi


if [ -e $logfile -a $logfile != "/dev/null" -a $logfile != "/dev/stdout" ]; then
echo "Logfile $logfile already exists! either remove or rename it."
exit 1
fi

if [ ! -e $dict ]; then
echo extract dictionary >> $logfile
$bin/dict -i="$data" -o=$dict -PruneFreq=$prunefreq -f=y >> $logfile 2>&1
if [ `head -1 $dict| cut -d " " -f 3` -lt 10 ]; then
echo "Dictionary contains errors"
exit 2;
fi
else
echo "Warning: dictionary file already exists."
if [ $forcedict ]; then
echo "Warning: authorization to use it."
else
echo "No authorization to use it (see option -f)."
exit 1
fi
fi



#check tmpdir
tmpdir_created=0;
if [ ! -d $tmpdir ]; then
echo "Creating temporary working directory $tmpdir"
mkdir -p $tmpdir;
tmpdir_created=1;
else
echo "Cleaning temporary directory $tmpdir";
rm $tmpdir/* 2> /dev/null
if [ $? != 0 ]; then
echo "Warning: some temporary files could not be removed"
fi
fi

#####
echo split documents >> $logfile
$bin/plsa -c="$data" -d=$dict -b=$tmpdir/data -sd=$splits >> $logfile 2>&1

machine=`uname -s` 
if [ $machine == "Darwin" ] ; then
splitlist=`jot - 1 $splits`
iterlist=`jot - 1 $iter`
else
splitlist=`seq 1 1 $splits`
iterlist=`seq 1 1 $iter`
fi

#rm $tmpdir/Tlist
for sp in $splitlist ; do echo $tmpdir/data.T.$sp >> $tmpdir/Tlist 2>&1; done
#rm $model
for it in $iterlist ; do
for sp in $splitlist ; do
date; echo it $it split $sp
$bin/plsa -c=$tmpdir/data.$sp -d=$dict -st=$spectopics -hf=$tmpdir/data.H.$sp -tf=$tmpdir/data.T.$sp -wf=$model -m=$model -t=$topics -it=1 -tit=$it >> $logfile 2>&1 &
done
while [ 1 ]; do fg 2> /dev/null; [ $? == 1 ] && break; done

date; echo recombination

$bin/plsa -ct=$tmpdir/Tlist -c="$data" -d=$dict -hf=$tmpdir/data.H -m=$model -t=$topics -it=1 -txt=$txtfile >> $logfile 2>&1

done
date; echo End of training

echo "Cleaning temporary directory $tmpdir";
rm $tmpdir/* 2> /dev/null

if [ $tmpdir_created -eq 1 ]; then
echo "Removing temporary directory $tmpdir";
rmdir $tmpdir 2> /dev/null
if [ $? != 0 ]; then
echo "Warning: the temporary directory could not be removed."
fi
fi
exit 0
#testing branch
else

if [ ! $model -o ! -e $model ]; then
echo "Need to specify existing model"
exit 1;
fi


if [ ! $dict  -o ! -e $dict  ]; then
echo "Need to specify dictionary file of the model"
exit 1;
fi

$bin/plsa -inf="$data" -d=$dict -m=$model -hf=hfff.out$$ -t=$topics -it=$iter -f=$unigram >> $logfile 2>&1

rm hfff.out$$

fi



exit 0
