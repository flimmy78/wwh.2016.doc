@echo off
pscp.exe -l root -pw eichong -scp root@10.9.2.106:/usr/local/epserver/gate/start.sh C:\Users\wangweihang\Desktop\acw\linux106\conf
pscp.exe -l root -pw eichong -scp root@10.9.2.106:/usr/local/epserver/gate/conf/mybatis.xml C:\Users\wangweihang\Desktop\acw\linux106\conf
pscp.exe -l root -pw eichong -scp root@10.9.2.106:/usr/local/epserver/gate/conf/applicationContext.xml C:\Users\wangweihang\Desktop\acw\linux106\conf

pscp.exe -l root -pw eichong -scp D:/eichong/protocol/ecnLibs3.0/lib/ormcore.1.0.jar root@10.9.2.106:/usr/local/epserver/ecnLibs3.0/lib
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/dUsrGate/conf/mybatis.xml root@10.9.2.106:/usr/local/epserver/gate/conf
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/dUsrGate/conf/applicationContext.xml root@10.9.2.106:/usr/local/epserver/gate/conf
pscp.exe -l root -pw eichong -scp D:/eichong/protocol/dEpPlatform/dUsrGate/release/dUsrGate.1.0.jar root@10.9.2.106:/usr/local/epserver/gate/bin
pscp.exe -l root -pw eichong -scp C:/Users/wangweihang/Desktop/acw/linux106/gameStartD.sh root@10.9.2.106:/usr/local/epserver/gate
pscp.exe -l root -pw eichong -scp C:/Users/wangweihang/Desktop/acw/linux106/start.sh root@10.9.2.106:/usr/local/epserver/gate
pause