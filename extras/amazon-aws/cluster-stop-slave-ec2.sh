pem=/home/ubuntu/MyMemoryBot-1.pem
binaries=/home/ubuntu/mmt-0.12-SNAPSHOT_201602261141-ubuntu14_04.tar.gz
engine=b1

ip=$1
ssh -i $pem ubuntu@$ip "mmt/mmt stop -e $engine"