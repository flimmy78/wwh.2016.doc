#!/bin/bash

function convertFuncName(){
    if [ $1 == "capital" ]; then 
	return 1
    elif [ $1 == "bespokeByUser" ]; then 
	return 2
    elif [ $1 == "bespokeByEP" ]; then 
	return 3
    elif [ $1 == "chargeByUser" ]; then 
	return 4
    elif [ $1 == "chargeByEP" ]; then 
	return 5
    elif [ $1 == "cardByUser" ]; then 
	return 6
    elif [ $1 == "cardByEP" ]; then 
	return 7
    elif [ $1 == "initBystationId" ]; then 
	return 8
    elif [ $1 == "initByepCode" ]; then 
	return 9
    elif [ $1 == "realDataByEP" ]; then 
	return 10
    elif [ $1 == "realData" ]; then 
	return 11
    elif [ $1 == "cardAuth" ]; then 
	return 12
    elif [ $1 == "epChannel" ]; then 
	return 13
    elif [ $1 == "updateByEP" ]; then 
	return 14
    elif [ $1 == "updateByStation" ]; then 
	return 15
    else
        return 0
    fi
}

if [ $# -lt 1 ] || [ $# -gt 10 ]; then
    param=0
else
    convertFuncName $1
    param=$?
fi
if [ "$param" == 0 ]; then
    echo "Usage:"
    echo "    $0 capital accountId [path]"
    echo "    $0 bespokeByUser accountId [path]"
    echo "    $0 bespokeByEP epCode epGunNo [path]"
    echo "    $0 chargeByUser accountId [path]"
    echo "    $0 chargeByEP epCode epGunNo [path]"
    echo "    $0 cardByUser accountId [path]"
    echo "    $0 cardByEP epCode epGunNo [path]"
    echo "    $0 initBystationId stationId [path]"
    echo "    $0 initByepCode epCode [path]"
    echo "    $0 realDataByEP epCode epGunNo [path]"
    echo "    $0 realData epCode epGunNo dataType address [path]"
    echo "    $0 cardAuth inCardNo [path]"
    echo "    $0 epChannel [path]"
    echo "    $0 updateByEP epCode [path]"
    echo "    $0 updateByStation stationAddr [path]"
    exit
fi

if [ "$param" == 3 ] || [ "$param" == 5 ] || [ "$param" == 7 ] || [ "$param" == 10 ]; then
	if [ $# -lt 3 ] || [ $# -gt 4 ]; then
		echo "Usage: $0 funcName epCode epGunNo [path]"
		exit
	fi

	if [ ! -n "$4" ]; then
		INIT_PATH='./'
	else
		INIT_PATH=$4/
	fi
elif [ "$param" == 11 ]; then
	if [ $# -lt 5 ] || [ $# -gt 6 ]; then
		echo "Usage: $0 funcName epCode epGunNo dataType address [path]"
		exit
	fi

	if [ ! -n "$6" ]; then
		INIT_PATH='./'
	else
		INIT_PATH=$6/
	fi
elif [ "$param" == 13 ]; then
	if [ $# -lt 1 ] || [ $# -gt 2 ]; then
		echo "Usage: $0 funcName [path]"
		exit
	fi

	if [ ! -n "$2" ]; then
		INIT_PATH='./'
	else
		INIT_PATH=$2/
	fi
else
	if [ $# -lt 2 ] || [ $# -gt 3 ]; then
		echo "Usage: $0 funcName accountId|stationId|epCode [path]"
		exit
	fi

	if [ ! -n "$3" ]; then
		INIT_PATH='./'
	else
		INIT_PATH=$3/
	fi
fi

if [ "$param" == 1 ]; then 
    ./queryComm.sh "addAmt|subAmt|remainAmt" "accountId:" $2 $INIT_PATH
elif [ "$param" == "2" ]; then 
    ./queryComm.sh "bespoke|cancelBespoke|endBespoke" "accountId:" $2 $INIT_PATH
elif [ "$param" == "3" ]; then 
    ./queryComm.sh "bespoke|cancelBespoke|endBespoke" "epCode:" $2 "epGunNo:" $3 $INIT_PATH
elif [ "$param" == "4" ]; then 
    ./queryComm.sh "charge|stopCharge|endcharge|phoneConnect|connectEp" "accountId:" $2 $INIT_PATH
elif [ "$param" == "5" ]; then
    ./queryComm.sh "charge |stopCharge|endcharge" "epCode:" $2 "epGunNo:" $3 $INIT_PATH
elif [ "$param" == "6" ]; then 
    ./queryComm.sh "card fronze" "accountId:" $2 $INIT_PATH
elif [ "$param" == "7" ]; then
    ./queryComm.sh "userCardAuth|card fronze" "epCode:" $2 "epGunNo:" $3 $INIT_PATH
elif [ "$param" == "8" ]; then
    ./queryComm.sh "initConnect" "stationId:" $2 $INIT_PATH
elif [ "$param" == "9" ]; then
    ./queryComm.sh "initConnect" "epCode:" $2 $INIT_PATH
elif [ "$param" == "10" ]; then
    ./queryComm.sh "realData" "epCode:" $2 "epGunNo:" $3 $INIT_PATH
elif [ "$param" == "11" ]; then
    ./queryComm.sh "realData" "epCode:" $2 "epGunNo:" $3 "dataType:" $4 "address:" $5 $INIT_PATH
elif [ "$param" == "12" ]; then 
    ./queryComm.sh "userCardAuth" "inCardNo:" $2 $INIT_PATH
elif [ "$param" == "13" ]; then 
    ./queryComm.sh "epChannelHandler" $INIT_PATH
elif [ "$param" == "14" ]; then 
    ./queryComm.sh "handleVersion" "epCode:" $2 $INIT_PATH
elif [ "$param" == "15" ]; then 
    ./queryComm.sh "handleVersion" "stationAddr:" $2 $INIT_PATH
fi
