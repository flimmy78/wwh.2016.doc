#!/bin/bash

if [ -f commfile.txt ]
then
    rm -f commfile.txt
fi

function queryFund(){
for file in `ls -tr EpGateLog.*`
do
    if [ -d $file ] #如果 file存在且是一个目录则为真
    then
        queryFund $file
    else
        local path=$1"/"$file #得到文件的完整的目录
        local name=$file       #得到文件的名字
        grep $1 $name |grep `date +%Y-%m-%d` >> commfile.txt
    fi

done
}

if [ $# -lt 1 ]; then
    echo "Usage:"
    echo "    $0 检索字符串"
    exit
fi

start='ServerStart|'
queryFund $start$1

var=$(awk 'END{print $2}' commfile.txt)
rm -f commfile.txt
grep $1 EpGateLog.* |grep `date +%Y-%m-%d` |grep $var -A 100 >> commfile.txt
cat commfile.txt
