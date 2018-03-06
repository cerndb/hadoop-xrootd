#!/usr/bin/env bash

set -e

BUILD_PATH=$(pwd)

docker build -t gitlab-registry.cern.ch/awg/hadoop-xrootd-connector $BUILD_PATH

docker push gitlab-registry.cern.ch/awg/hadoop-xrootd-connector

docker run --rm -it -v $BUILD_PATH:/data gitlab-registry.cern.ch/awg/hadoop-xrootd-connector

