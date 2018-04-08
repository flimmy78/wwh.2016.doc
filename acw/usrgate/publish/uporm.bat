@echo off
rem pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/ormcore.1.0.jar root@10.9.2.106:/usr/local/epserver/ecnLibs3.0/lib
rem pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/ormcore.1.0.jar root@10.9.2.106:/usr/local/depserver/ecnLibs3.0/lib
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/ormcore.1.0.jar root@10.9.2.110:/root/depserver/ecnLibs3.0/lib
rem pause