@echo off
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libECCommon.1.0.jar root@10.9.2.154:/usr/local/tomcat/webapps/html/WEB-INF/lib
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/usrlayer.1.0.jar root@10.9.2.154:/usr/local/tomcat/webapps/html/WEB-INF/lib
rem pause