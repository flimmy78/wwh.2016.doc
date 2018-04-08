#!/bin/bash

if [ -f commfile.txt ]
then
    rm -f commfile.txt
fi
if [ -f commFile.txt ]
then
    rm -f commFile.txt
fi


function queryFund(){
for file in `ls -tr $1/*/EpGateLog.*`
do
    if [ -d $1"/"$file ] #如果 file存在且是一个目录则为真
    then
        queryFund $1"/"$file
    else
        local path=$1"/"$file #得到文件的完整的目录
        local name=$file       #得到文件的名字
        egrep $2 $name >> commfile.txt
    fi

done
}

if [ ! -n "$6" ] ;then
    if [ -n "$3" ] ;then
        INIT_PATH=$4
    else
        INIT_PATH=$2
    fi
else
    if [ ! -n "${10}" ] ;then
        INIT_PATH=$6
    else
        INIT_PATH=${10}
    fi
fi
queryFund $INIT_PATH $1

if [ ! -n "$6" ] ;then
    if [ -n "$3" ] ;then
        grep $2$3 commfile.txt >> commFile.txt
    else
        grep $1 commfile.txt >> commFile.txt
    fi
else
    if [ ! -n "${10}" ] ;then
        grep $2$3 commfile.txt | grep $4$5 >> commFile.txt
    else
        grep $2$3 commfile.txt | grep $4$5 | grep $6$7 | grep $8$9 >> commFile.txt
    fi
fi
cat commFile.txt
rm -f commfile.txt
