#!/usr/bin/env bash

dockIp() {
  docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$@"
}

docker pull vitorenesduarte/vcd:latest
docker run --rm -d -p 2181:2181 zookeeper &
sleep 3

# get ip address of zk-container
zkid=`docker ps | grep zookeeper | awk '{print $1}'`
zkAddress=`dockIp ${zkid}`
echo ${zkAddress}

docker run --rm --net host -e "ZK=${zkAddress}"\
    -e "ID=0" \
    -e "NODE_NUMBER=3" \
    -e "HPORT=5000" \
    -e "CPORT=6000" \
    -p 6000:6000 vitorenesduarte/vcd >& mgb1.txt &

docker run --rm --net host -e "ZK=${zkAddress}" \
    -e "ID=1" \
    -e "NODE_NUMBER=3" \
    -e "HPORT=5001" \
    -e "CPORT=6001" \
    -p 6001:6001 vitorenesduarte/vcd >& mgb2.txt &

docker run --rm --net host -e "ZK=${zkAddress}" \
    -e "ID=2" \
    -e "NODE_NUMBER=3" \
    -e "HPORT=5002" \
    -e "CPORT=6002" \
    -p 6002:6002 vitorenesduarte/vcd >& mgb3.txt &


#./bin/ycsb run mgbsmap -s -P workloads/workloadc -threads 64 \
# -p host=172.17.0.2 \
# -p port=2181 \
# -p verbose=false \
# -p recordcount=1000000 \
# -p operationcount=200000 > ~/current_code_work/ycsb64_c_true_nothing.txt

#docker run --rm --net host -e "ZHOST=${zkAddress}" -e "SERVERPORT=8980" -e "RETRIES=400" tfr011/mgb-smap:latest

#docker run --rm --net host -e "DB=mgbsmap" \
#    -e "HOST=${zkAddress}" \
#    -e "PORT=2181" \
#    -e "TYPE=run" \
#    -e "WORKLOAD=workloada" \
#    -e "SMAPPORT=8980" \
#    -e "THREADS=128" \
#    -e "RECORDCOUNT=1000000" \
#    -e "OPERATIONCOUNT=10000" \
#    -e "FAST=true" \
#    0track/ycsb:latest

cleanup(){
  docker stop $(docker ps -aq)
}

trap "cleanup; exit 255" SIGINT SIGTERM
echo "Will sleep forever"
while true; do sleep 10000; done
