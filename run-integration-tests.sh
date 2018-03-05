#!/usr/bin/env bash

BUILD_PATH=$(pwd)

docker build -t gitlab-registry.cern.ch/awg/hadoop-xrootd-connector $BUILD_PATH || exit 2

docker push gitlab-registry.cern.ch/awg/hadoop-xrootd-connector || exit 2

docker run --rm -it -v $BUILD_PATH:/data gitlab-registry.cern.ch/awg/hadoop-xrootd-connector || exit 2

