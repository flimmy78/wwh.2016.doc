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
for file in `ls -tr */EpGateLog.*`
do
    local name=$file       #得到文件的名字
    grep $1 $name >> commfile.txt

done
}

if [ $# -lt 1 ]; then
    echo "Usage:"
    echo "    $0 检索字符串 [condition]"
    exit
fi

start='ServerStart|'
queryFund $start$1

var=$(awk 'END{print $2}' commfile.txt)
rm -f commfile.txt

queryFund $1
if [ ! -n "$2" ]; then
    grep $var commfile.txt -A 1000000 >> commFile.txt
else
    grep $var commfile.txt -A 1000000 >> commFile.txt
    grep $2 commFile.txt
fi

rm -f commfile.txt