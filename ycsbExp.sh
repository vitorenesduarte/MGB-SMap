#!/usr/bin/env bash

localHost=127.0.0.1
docker run --rm --net host -e "DB=mgbsmap" \
    -e "HOST=${localHost}" \
    -e "PORT=2181" \
    -e "TYPE=run" \
    -e "WORKLOAD=workloada" \
    -e "SMAPPORT=8980" \
    -e "STATIC=true" \
    -e "THREADS=64" \
    -e "RECORDCOUNT=100" \
    -e "OPERATIONCOUNT=100000" \
    -e "FAST=true" \
    -e "EXTRA=-s" \
    0track/ycsb:latest &> ycsb1.txt &

docker run --rm --net host -e "DB=mgbsmap" \
    -e "HOST=${localHost}" \
    -e "PORT=2181" \
    -e "TYPE=run" \
    -e "WORKLOAD=workloada" \
    -e "SMAPPORT=8981" \
    -e "STATIC=true" \
    -e "THREADS=64" \
    -e "RECORDCOUNT=100" \
    -e "OPERATIONCOUNT=100000" \
    -e "FAST=true" \
    -e "EXTRA=-s" \
    0track/ycsb:latest &> ycsb2.txt &

cleanup(){
  docker stop $(docker ps -aq)
}

wait

trap "cleanup; exit 255" SIGINT SIGTERM
