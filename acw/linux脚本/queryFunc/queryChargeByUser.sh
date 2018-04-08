#!/bin/bash

if [ ! -n "$1" ] ;then
    echo "accountId参数不能为空!"
    exit 1
fi
if [ ! -n "$2" ] ;then
    INIT_PATH='./'
else
    INIT_PATH=$2/
fi

./queryComm.sh "charge|stopCharge|endcharge" "accountId:" $1 $INIT_PATH

