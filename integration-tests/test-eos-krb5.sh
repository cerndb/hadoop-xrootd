#!/usr/bin/env bash

set -e

echo ''
echo '** Test that connector can use krb5 **'

echo ''
echo -n "Please enter kinit username [ENTER]: "
read KINIT_USER

kinit $KINIT_USER

FIRSTLETTER=$(echo $KINIT_USER | cut -c 1)

TEST_FILE="root://eosuser.cern.ch//eos/user/$FIRSTLETTER/$KINIT_USER/"

echo "** Try reading $TEST_FILE with krb5**"
hdfs dfs -ls $TEST_FILE