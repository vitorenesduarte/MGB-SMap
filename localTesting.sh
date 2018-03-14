#!/usr/bin/env bash

dockIp() {
  docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$@"
}

docker pull vitorenesduarte/vcd:batching
docker run --rm -d -p 2181:2181 zookeeper &
sleep 3

# get ip address of zk-container
zkid=`docker ps | grep zookeeper | awk '{print $1}'`
zkAddress=`dockIp ${zkid}`
echo ${zkAddress}

docker run --rm --net host -e "ZK=${zkAddress}" -e "ID=0" -e "NODE_NUMBER=3" -e "HPORT=5000" -e "CPORT=6000" -p 6000:6000 vitorenesduarte/vcd >& t1.txt &

docker run --rm --net host -e "ZK=${zkAddress}" -e "ID=1" -e "NODE_NUMBER=3" -e "HPORT=5001" -e "CPORT=6001" -p 6001:6001 vitorenesduarte/vcd >& t2.txt &

docker run --rm --net host -e "ZK=${zkAddress}" -e "ID=2" -e "NODE_NUMBER=3" -e "HPORT=5002" -e "CPORT=6002" -p 6002:6002 vitorenesduarte/vcd >& t3.txt &

docker run --rm --net host -e "ZHOST=${zkAddress}" mgb-smap:latest &

cleanup(){
  docker stop $(docker ps -aq)
}

trap "cleanup; exit 255" SIGINT SIGTERM
echo "Will sleep forever"
while true; do sleep 10000; done
