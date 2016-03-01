pem=/home/ubuntu/MyMemoryBot-1.pem
engine=b1
ip=$1

echo ""
echo pem = $pem
echo engine = $engine
echo slave ip = $ip
echo ""
read -p "Correct? Press [Enter] to continue. If not type ctrl-c and edit this script."

ssh -i $pem ubuntu@$ip "mmt/mmt stop -e $engine"