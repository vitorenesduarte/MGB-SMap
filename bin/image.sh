#!/usr/bin/env bash

if [ -z "$1" ]; then
  TAG=latest
else
  TAG="$1"
fi

DIR=$(dirname "$0")
IMAGE=vitorenesduarte/mgb-smap:${TAG}
DOCKERFILE=${DIR}/../Dockerfiles/mgb-smap

# build image
docker build \
    --no-cache \
    -t "${IMAGE}" -f "${DOCKERFILE}" .


# push image
docker push "${IMAGE}"

