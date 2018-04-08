@echo off
D:
cd D:\eichong\protocol\dEpPlatform\dWebGate
call ant
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/dWebGate/release/dWebGate.4.0.0.jar root@10.9.2.106:/usr/local/depserver/dWebGate/bin
