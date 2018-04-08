#!/bin/bash

cd `dirname $0`
CUR_DIR=`pwd`
BIN_DIR=$CUR_DIR/bin/

count=`ls -l "$BIN_DIR"|grep "^-"|wc -l`
if [ $count -eq "1" ] 
then
    ls "$BIN_DIR" > txt
else
    exit 1
fi