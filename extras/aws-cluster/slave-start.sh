# Edit this params before running
pem=/home/ubuntu/MyMemoryBot-1.pem
engine=b1

ip=$1
master=`ifconfig|xargs|awk '{print $7}'|sed -e 's/[a-z]*:/''/'`

echo ""
echo pem = $pem
echo engine = $engine
echo slave ip = $ip
echo master ip = $master
echo ""
read -p "Correct? Press [Enter] to continue. If not type ctrl-c and edit the script."

ssh -i $pem ubuntu@$ip "ssh-keyscan $master > ~/.ssh/known_hosts"
ssh -i $pem ubuntu@$ip "mmt/mmt start -e $engine --master ubuntu@$master --master-pem $pem"