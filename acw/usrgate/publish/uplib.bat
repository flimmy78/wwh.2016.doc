@echo off
D:
cd D:\eichong\protocol\dEpPlatform\dEpGate
call ant
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/dEpGate/release/dEpGate.4.2.18.jar root@10.9.2.124:/root/depserver/dEpGate/bin
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libECCommon.1.0.jar root@10.9.2.124:/root/depserver/ecnLibs3.0/lib
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libCooperate.1.0.jar root@10.9.2.124:/root/depserver/ecnLibs3.0/lib
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/usrlayer.1.0.jar root@10.9.2.124:/root/depserver/ecnLibs3.0/lib

pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/dEpGate/release/dEpGate.4.2.18.jar root@10.9.2.123:/root/depserver/dEpGate/bin
pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libECCommon.1.0.jar root@10.9.2.123:/root/depserver/ecnLibs3.0/lib
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libCooperate.1.0.jar root@10.9.2.123:/root/depserver/ecnLibs3.0/lib
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/usrlayer.1.0.jar root@10.9.2.123:/root/depserver/ecnLibs3.0/lib

rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libCooperate.1.0.jar root@10.9.2.124:/root/depserver/ecnLibs3.0/lib
rem pause

rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/dEpGate/release/dEpGate.4.2.18.jar root@10.9.2.110:/root/depserver/dEpGate/bin
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libECCommon.1.0.jar root@10.9.2.110:/root/depserver/ecnLibs3.0/lib
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libCooperate.1.0.jar root@10.9.2.110:/root/depserver/ecnLibs3.0/lib

rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/dEpGate/release/dEpGate.4.2.18.jar root@10.9.2.106:/usr/local/depserver/dEpGate/bin
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libECCommon.1.0.jar root@10.9.2.106:/usr/local/depserver/ecnLibs3.0/lib
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libCooperate.1.0.jar root@10.9.2.106:/usr/local/depserver/ecnLibs3.0/lib
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/usrlayer.1.0.jar root@10.9.2.106:/usr/local/depserver/ecnLibs3.0/lib
