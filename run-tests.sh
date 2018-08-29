#!/usr/bin/env bash

set -e

docker build \
  --build-arg BUILD_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ") \
  -t hadoop-xrootd-connector $(pwd)

docker run --rm -it hadoop-xrootd-connector

