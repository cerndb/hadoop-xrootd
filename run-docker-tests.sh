#!/usr/bin/env bash

set -e

BUILD_PATH=$(pwd)

docker build -t hadoop-xrootd-connector $BUILD_PATH

docker run --rm -it hadoop-xrootd-connector make test

