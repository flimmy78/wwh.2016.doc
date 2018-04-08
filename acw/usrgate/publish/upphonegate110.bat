@echo off
D:
cd D:\eichong\protocol\dEpPlatform\dPhoneGate
call ant
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/ecnLibs3.0/lib/usrlayer.1.0.jar root@10.9.2.110:/root/depserver/ecnLibs3.0/lib
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/dPhoneGate/release/dPhoneGate.4.2.6.jar root@10.9.2.110:/root/depserver/dPhoneGate/bin
