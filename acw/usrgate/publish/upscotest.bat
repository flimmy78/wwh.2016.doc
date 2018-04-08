@echo off
D:
cd D:\eichong\protocol\Test\scoTest
call ant
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/Test/scoTest/release/scoTest.1.0.0.jar root@10.9.2.106:/usr/local/depserver/scoTest/bin
