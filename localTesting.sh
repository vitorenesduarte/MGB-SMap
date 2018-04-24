#!/usr/bin/env bash

dockIp() {
  docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$@"
}

localHost=127.0.0.1
image="vitorenesduarte/vcd:latest"
docker pull ${image}
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
    -p 6000:6000 ${image} >& mgb1.txt &


docker run --rm --net host -e "ZK=${zkAddress}" \
    -e "ID=1" \
    -e "NODE_NUMBER=3" \
    -e "HPORT=5001" \
    -e "CPORT=6001" \
    -p 6001:6001 ${image} >& mgb2.txt &


docker run --rm --net host -e "ZK=${zkAddress}" \
    -e "ID=2" \
    -e "NODE_NUMBER=3" \
    -e "HPORT=5002" \
    -e "CPORT=6002" \
    -p 6002:6002 ${image} >& mgb3.txt &

sleep 3
docker run --rm --net host -e "ZHOST=${localHost}" -e "ZPORT=5000" -e "SERVERPORT=8980" -e "RETRIES=400" -e "STATIC=true" tfr011/mgb-smap:latest &> smap1.txt &
docker run --rm --net host -e "ZHOST=${localHost}" -e "ZPORT=5001"-e "SERVERPORT=8990" -e "RETRIES=400" -e "STATIC=true" tfr011/mgb-smap:latest &> smap2.txt &

#docker run --rm --net host -e "DB=mgbsmap" \
#    -e "HOST=${zkAddress}" \
#    -e "PORT=2181" \
#    -e "TYPE=run" \
#    -e "WORKLOAD=workloada" \
#    -e "SMAPPORT=8980" \
#    -e "STATIC=true" \
#    -e "THREADS=16" \
#    -e "RECORDCOUNT=10000" \
#    -e "OPERATIONCOUNT=10000" \
#    -e "FAST=true" \
#    0track/ycsb:latest &> ycsb1.txt

# docker run --rm --net host -e "DB=mgbsmap" \
#    -e "HOST=${zkAddress}" \
#    -e "PORT=2181" \
#    -e "TYPE=run" \
#    -e "WORKLOAD=workloada" \
#    -e "SMAPPORT=8990" \
#    -e "STATIC=true" \
#    -e "THREADS=16" \
#    -e "RECORDCOUNT=10000" \
#    -e "OPERATIONCOUNT=10000" \
#    -e "FAST=true" \
#    0track/ycsb:latest


cleanup(){
  docker stop $(docker ps -aq)
}

trap "cleanup; exit 255" SIGINT SIGTERM
echo "Will sleep forever"
while true; do sleep 10000; done
