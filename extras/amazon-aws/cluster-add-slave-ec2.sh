pem=/home/ubuntu/MyMemoryBot-1.pem
binaries=/home/ubuntu/mmt-0.12-SNAPSHOT_201602261141-ubuntu14_04.tar.gz
engine=b1

ip=$1
master=`ifconfig|xargs|awk '{print $7}'|sed -e 's/[a-z]*:/''/'`

scp -i $pem $pem ubuntu@$ip:
scp -i $pem $binaries ubuntu@$ip:
ssh -i $pem ubuntu@$ip "tar xvfz $binaries"
ssh -i $pem ubuntu@$ip 'sudo add-apt-repository -y ppa:openjdk-r/ppa && sudo apt-get -y update && sudo apt-get -y install openjdk-8-jdk'
ssh -i $pem ubuntu@$ip "ssh-keyscan $master > ~/.ssh/known_hosts"
ssh -i $pem ubuntu@$ip "mmt/mmt start -e $engine --master ubuntu@$master --master-pem $pem"