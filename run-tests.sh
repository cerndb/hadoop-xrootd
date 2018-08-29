#!/usr/bin/env bash

set -e

CONNECTOR_RELEASE_NAME="hadoop-xrootd-1.0.0-SNAPSHOT"
ARCHITECTURE_PROFILE="amd64-Linux-gpp-jni"
docker build \
  --build-arg BUILD_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ") \
  --build-arg CONNECTOR_RELEASE_NAME=${CONNECTOR_RELEASE_NAME} \
  --build-arg ARCHITECTURE_PROFILE=${ARCHITECTURE_PROFILE} \
-t hadoop-xrootd-connector \
$(pwd)

docker run --rm -it hadoop-xrootd-connector

