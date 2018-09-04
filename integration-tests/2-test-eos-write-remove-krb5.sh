#!/usr/bin/env bash

set -e

echo ''
echo '** Test that connector can use krb5 **'

echo ''
echo -n "Please enter kinit username [ENTER]: "
read KINIT_USER

kinit $KINIT_USER

FIRSTLETTER=$(echo $KINIT_USER | cut -c 1)

TEST_DIR="root://eosuser.cern.ch//eos/user/$FIRSTLETTER/$KINIT_USER/"
TEST_FILE="$TEST_DIR/xrootd-connector-test"
echo ''
echo "** Try listing $TEST_DIR which requires krb5**"
hdfs dfs -ls $TEST_DIR

head -c 100000 /dev/urandom > /tmp/xrootd-connector-test
echo ''
echo "** Try writing and reading of TEST_FILE which requires krb5**"
hdfs dfs -put /tmp/xrootd-connector-test $TEST_FILE
hdfs dfs -ls $TEST_FILE
hdfs dfs -cat $TEST_FILE | tail -1

echo ''
echo "** Try removing $TEST_DIR which requires krb5**"
hdfs dfs -rm $TEST_FILE
