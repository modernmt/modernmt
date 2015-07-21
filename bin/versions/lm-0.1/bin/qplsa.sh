#! /bin/bash

sDir=$(cd $(dirname $0) ; /bin/pwd)

#Task data
bin=/hltsrv0/federico/plsa/bin
wdir=/panfs/panfem/test-hlt/federico/plsa/CC
#/hltsrv0/federico/plsa/ted
ldir=/scratch/federico

data=doc_en.00.bin
dict=ted.dict

#ted-en
topics=150
iter=2
prunefreq=5
spectopics=500
Tlist=$wdir/tlist
splits=2
model=model.$splits
txtfile=Wfile.$splits

#parameters
numSlots=1-3
ram=10G
qL=bld.q,bld-ib.q

#Preparation phase
jName=PLSA.PRE

#preparation ends when tlist is prepared
rm $Tlist 
jName=PLSA.TRAIN

range=`yes | head -n $splits | awk '{printf("%02d ",a);a++}'`
iter=`seq 1 1 $iter| tr "\012" " "`

qsub -cwd -N $jName -j y -q $qL -l mf=$ram -t $numSlots -o $wdir/log -S /bin/bash <<EOF


me=\`echo \$SGE_TASK_ID | awk '{printf("%02d",\$1-1)}'\`
lastid=\`echo \$SGE_TASK_LAST | awk '{printf("%02d",\$1-1)}'\`

(echo start ; date) > $wdir/monitor.\$SGE_TASK_ID
echo

if [[ ! -d $ldir ]]; then mkdir $ldir; fi

#################################
if [ \$me -eq \$lastid ]
then
(echo master starts ; uname -n ; date) > $wdir/monitor.\$SGE_TASK_ID

#prepare Tlist file
rm $Tlist
for sp in $range; do 
echo $wdir/$data.T.\$sp >> $Tlist
done

#tell slaves to copy and binarize data

for sp in $range; do

(echo cp $wdir/$data.\$sp.gz $wdir/$dict $ldir \; ;\
echo $bin/plsa -c=\"gunzip -c $ldir/$data.\$sp.gz\" -d=$ldir/$dict -b=$ldir/$data.\$sp \; ;\
echo rm $ldir/$data.\$sp.gz ) > $wdir/taskfor_\$sp
touch $wdir/doit_\$sp
done

(echo master prepare ; date) >> $wdir/monitor.\$SGE_TASK_ID

#wait that all have finished
while ls $wdir/doit_* &> /dev/null; do sleep 1; done

(echo master start iteration ; date) >> $wdir/monitor.\$SGE_TASK_ID

for it in $iter; do
for sp in $range; do

(echo master iteration \$it split \$sp; date) >> $wdir/monitor.\$SGE_TASK_ID

echo tell slave to run an iteration
(echo if [[ -e $wdir/$model ]] \; then cp $wdir/$model $ldir/$model \; fi ;
 echo $bin/plsa -c=$ldir/$data.\$sp -d=$ldir/$dict -st=$spectopics -hf=$ldir/$data.H.\$sp -tf=$ldir/$data.T.\$sp -wf=$ldir/$model -m=$ldir/$model -t=$topics -it=1 -tit=\$it ;\
echo cp $ldir/$data.T.\$sp $wdir ) > $wdir/taskfor_\$sp
touch $wdir/taskfor_\$sp
touch $wdir/doit_\$sp
done

(echo master start waiting \$it ; date) >> $wdir/monitor.\$SGE_TASK_ID


#echo wait that all have finished
while ls $wdir/doit_* &> /dev/null; do
(echo master waiting \$it ; date) >> $wdir/monitor.\$SGE_TASK_ID
ls $wdir/doit_*
sleep 1;
done

(echo master start recombination \$it ; date) >> $wdir/monitor.\$SGE_TASK_ID

echo recombine
$bin/plsa -ct=$Tlist -c=dummy -d=$wdir/$dict -m=$wdir/$model -t=$topics -it=1 -txt=$wdir/$txtfile

done


(echo master tells slaves to remove data; date) >> $wdir/monitor.\$SGE_TASK_ID

echo tell slaves to remove their local data
for sp in $range; do
echo rm $ldir/$dict $ldir/$data.\$sp $ldir/$model > $wdir/taskfor_\$sp
touch $wdir/taskfor_\$sp
touch $wdir/doit_\$sp
done
echo wait that all have finished

(echo master waits for slaves; date) >> $wdir/monitor.\$SGE_TASK_ID
 
while ls $wdir/doit_* &> /dev/null; do sleep 1; done

echo tell slaves to exit

(echo master tells slaves to exit; date) >> $wdir/monitor.\$SGE_TASK_ID

for sp in $range; do
echo exit > $wdir/taskfor_\$sp
touch $wdir/taskfor_\$sp
touch $wdir/doit_\$sp
done

(echo master waits for slaves; date) >> $wdir/monitor.\$SGE_TASK_ID
while ls $wdir/doit_* &> /dev/null; do sleep 1; done

(echo master ends; date) >> $wdir/monitor.\$SGE_TASK_ID

rm $wdir/$data.H* $wdir/$model $wdir/$data.T* $wdir/taskfor_*

#############################
else

(echo slave starts ; uname -n ; date) > $wdir/monitor.\$SGE_TASK_ID

while :
do

(echo slave \$me iteration \$it waits for job; echo \$cmd; date) >> $wdir/monitor.\$SGE_TASK_ID

touch $wdir

if [[ -e $wdir/doit_\$me ]]; then

cmd=\`cat $wdir/taskfor_\$me\`

(echo slave \$me starts executing; echo \$cmd; date) >> $wdir/monitor.\$SGE_TASK_ID

if [[ \$cmd == *exit* ]]; then
    #rm before cmd execution
    rm $wdir/doit_\$me >& /dev/null
    exit 0
else
    /bin/sh $wdir/taskfor_\$me
    #rm after cmd execution
    rm $wdir/doit_\$me >& /dev/null
fi

(echo slave ended executing; date) >> $wdir/monitor.\$SGE_TASK_ID

fi

sleep 1

done

fi

(echo end;uname -a; date) >> $wdir/monitor.\$SGE_TASK_ID

exit 0

EOF

