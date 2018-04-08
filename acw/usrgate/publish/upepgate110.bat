@echo off
D:
cd D:\eichong\protocol\dEpPlatform\dEpGate
rem call ant
pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/dEpGate/release/dEpGate.4.2.18.jar root@10.9.2.110:/root/depserver/dEpGate/bin
pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libECCommon.1.0.jar root@10.9.2.110:/root/depserver/ecnLibs3.0/lib
rem pscp.exe -l root -pw Eichongrrrkof -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/libCooperate.1.0.jar root@10.9.2.110:/root/depserver/ecnLibs3.0/lib