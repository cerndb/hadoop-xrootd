#!/usr/bin/env bash

set -e

echo ''
echo '** Test that connector can browse EOS directories **'

TEST_FILE="root://eospublic.cern.ch/eos/opendata/cms/MonteCarlo2012/Summer12_DR53X/DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball/AODSIM/PU_RD1_START53_V7N-v1/20000/DCF94DC3-42CE-E211-867A-001E67398011.root"

echo "** Try reading $TEST_FILE **"
hdfs dfs -ls $TEST_FILE

echo "** Try downloading $TEST_FILE **"
hdfs dfs -tail $TEST_FILE > /dev/null