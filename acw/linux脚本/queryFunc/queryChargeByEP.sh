#!/bin/bash

if [ ! -n "$1" ] ;then
    echo "epCode参数不能为空!"
    exit 1
fi
if [ ! -n "$2" ] ;then
    echo "epGunNo参数不能为空!"
    exit 1
fi

if [ ! -n "$3" ] ;then
    INIT_PATH='./'
else
    INIT_PATH=$3/
fi

./queryComm.sh "charge|stopCharge|endcharge" "epCode:" $1 "epGunNo:" $2 $INIT_PATH

