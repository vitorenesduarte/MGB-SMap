#!/usr/bin/env bash

cleanup(){
  docker stop $(docker ps -aq)
}

trap "cleanup; exit 255" SIGINT SIGTERM

dockIp() {
  docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$@"
}

localHost=127.0.0.1
vcd_image="vitorenesduarte/vcd:latest"
docker pull ${vcd_image}
docker run --rm -d -p 2181:2181 zookeeper &
sleep 3

# get ip address of zk-container
zkid=`docker ps | grep zookeeper | awk '{print $1}'`
zkAddress=`dockIp ${zkid}`
echo ${zkAddress}

echo "Starting MGB"

docker run --rm --net host -e "ZK=${zkAddress}"\
    -e "ID=0" \
    -e "NODE_NUMBER=3" \
    -e "HPORT=5000" \
    -e "CPORT=6000" \
    -p 6000:6000 ${vcd_image} >& mgb1.txt &

docker run --rm --net host -e "ZK=${zkAddress}" \
    -e "ID=1" \
    -e "NODE_NUMBER=3" \
    -e "HPORT=5001" \
    -e "CPORT=6001" \
    -p 6001:6001 ${vcd_image} >& mgb2.txt &

docker run --rm --net host -e "ZK=${zkAddress}" \
    -e "ID=2" \
    -e "NODE_NUMBER=3" \
    -e "HPORT=5002" \
    -e "CPORT=6002" \
    -p 6002:6002 ${vcd_image} >& mgb3.txt &

up=0
while [ ${up} != 3 ]; do
    sleep 1
    up=$(cat mgb*.txt |
             grep "Clustering OK!" |
             wc -l)
done

# echo "Starting SMAP"
# docker run --rm --net host -e "ZHOST=${localHost}"\
#    -e "ZPORT=5000" \
#    -e "SERVERPORT=8980" \
#    -e "RETRIES=400" \
#    -e "VERBOSE=false" \
#    -e "STATIC=true" \
#    0track/mgb-smap:latest &> smap1.txt &

# docker run --rm --net host -e "ZHOST=${localHost}"\
#    -e "ZPORT=5001" \
#    -e "SERVERPORT=8981" \
#    -e "VERBOSE=false" \
#    -e "RETRIES=400" \
#    -e "STATIC=true" \
#    0track/mgb-smap:latest &> smap2.txt &

# up=0
# while [ ${up} != 2 ]; do
#    sleep 1
#    up=$(cat smap*.txt |
#             grep "MGB-SMap Server started" |
#             wc -l)
# done

echo "Will sleep forever"
while true; do sleep 10000; done
