#!/usr/bin/env bash

docker pull vitorenesduarte/vcd:batching &

docker run -d -p 2181:2181 zookeeper &

docker run --net host -e "ZK=localhost" -e "ID=0" -e "NODE_NUMBER=3" -e "HPORT=5000" -e "CPORT=6000" -p 6000:6000 vitorenesduarte/vcd &

docker run --net host -e "ZK=localhost" -e "ID=1" -e "NODE_NUMBER=3" -e "HPORT=5001" -e "CPORT=6001" -p 6001:6001 vitorenesduarte/vcd &

docker run --net host -e "ZK=localhost" -e "ID=2" -e "NODE_NUMBER=3" -e "HPORT=5002" -e "CPORT=6002" -p 6002:6002 vitorenesduarte/vcd &
