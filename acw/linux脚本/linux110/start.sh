#!/bin/bash


mv nohup.out log.bak


echo ''
echo '            服务器后台运行中.........'
echo ''

nohup ./gameStart.sh $1 &
