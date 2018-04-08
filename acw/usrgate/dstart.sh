java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001 -server -Xloggc:logs/gc.log -Xms512m -Xmx1024m -XX:PermSize=64M -jar bin/dUsrGate.4.0.0.jar
