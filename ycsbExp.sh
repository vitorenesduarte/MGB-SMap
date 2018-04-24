#!/usr/bin/env bash


cleanup(){
    docker stop $(docker ps | grep ycsb | awk '{print $1}')
}

trap "cleanup; exit 255" SIGINT SIGTERM

localHost=127.0.0.1
docker run --rm --net host -e "DB=mgbsmap" \
    -e "HOST=${localHost}" \
    -e "PORT=2181" \
    -e "TYPE=run" \
    -e "WORKLOAD=workloada" \
    -e "SMAPPORT=8980" \
    -e "STATIC=true" \
    -e "THREADS=128" \
    -e "RECORDCOUNT=100" \
    -e "OPERATIONCOUNT=100000" \
    -e "FAST=true" \
    -e "EXTRA=-s -p updateproportion=1 -p readproportion=0 -p fieldlength=1 -p fieldcount=1" \
    0track/ycsb:latest &> ycsb1.txt &

docker run --rm --net host -e "DB=mgbsmap" \
    -e "HOST=${localHost}" \
    -e "PORT=2181" \
    -e "TYPE=run" \
    -e "WORKLOAD=workloada" \
    -e "SMAPPORT=8981" \
    -e "STATIC=true" \
    -e "THREADS=128" \
    -e "RECORDCOUNT=100" \
    -e "OPERATIONCOUNT=100000" \
    -e "FAST=true" \
    -e "EXTRA=-s -p updateproportion=1 -p readproportion=0 -p fieldlength=1 -p fieldcount=1" \
    0track/ycsb:latest &> ycsb2.txt &

wait

