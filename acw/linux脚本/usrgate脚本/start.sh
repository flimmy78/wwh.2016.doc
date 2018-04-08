#!/bin/bash

ulimit -c unlimited

cd `dirname $0`
CUR_DIR=`pwd`
BIN_DIR=$CUR_DIR/bin
CONF_DIR=$CUR_DIR

./queryFile.sh
if [ ! -f txt ]; then
    echo "epGate file too many"
    exit 1
fi
filename=`head -1 txt`
rm -f txt

JAVA_DEBUG_OPTS=""
if [ "$1" = "debug" ]; then
    address=`sed '/address/!d;s/.*=//' "$CONF_DIR"/pg.version | tr -d '\r'`
    if [ ! -n "$address" ]; then
        address=8001
    fi
    JAVA_DEBUG_OPTS=" -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=$address "
fi
JAVA_JMX_OPTS=""
if [ "$1" = "jmx" ]; then
    port=`sed '/port/!d;s/.*=//' "$CONF_DIR"/pg.version | tr -d '\r'`
    if [ ! -n "$port" ]; then
        port=40124
    fi
    JAVA_JMX_OPTS=" -Dcom.sun.management.jmxremote.port=$port -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false "
fi
xms=`sed '/xms/!d;s/.*=//' "$CONF_DIR"/pg.version | tr -d '\r'`
if [ ! -n "$xms" ]; then
    xms=512m
fi
xmx=`sed '/xmx/!d;s/.*=//' "$CONF_DIR"/pg.version | tr -d '\r'`
if [ ! -n "$xmx" ]; then
    xmx=1024m
fi
permsize=`sed '/permsize/!d;s/.*=//' "$CONF_DIR"/pg.version | tr -d '\r'`
if [ ! -n "$permsize" ]; then
    permsize=64m
fi
maxsize=`sed '/maxsize/!d;s/.*=//' "$CONF_DIR"/pg.version | tr -d '\r'`
if [ ! -n "$maxsize" ]; then
    maxsize=128m
fi
JAVA_MEM_OPTS=""
BITS=`java -version 2>&1 | grep -i 1.8`
if [ -n "$BITS" ]; then
    xmn=`sed '/xmn/!d;s/.*=//' "$CONF_DIR"/pg.version | tr -d '\r'`
    if [ -n "$xmn" ]; then
        JAVA_MEM_OPTS=" -server -Xloggc:logs/gc.log -Xms$xms -Xmx$xmx -Xmn$xmn -XX:MetaspaceSize=$permsize -XX:MaxMetaspaceSize=$maxsize -Xss256k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
    else
        JAVA_MEM_OPTS=" -server -Xloggc:logs/gc.log -Xms$xms -Xmx$xmx -XX:MetaspaceSize=$permsize -XX:MaxMetaspaceSize=$maxsize "
    fi
else
    JAVA_MEM_OPTS=" -server -Xloggc:logs/gc.log -Xms$xms -Xmx$xmx -XX:PermSize=$permsize -XX:MaxPermSize=$maxsize "
fi

java $JAVA_MEM_OPTS $JAVA_DEBUG_OPTS $JAVA_JMX_OPTS -jar $BIN_DIR/$filename
