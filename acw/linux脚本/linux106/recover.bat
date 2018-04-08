@echo off
pscp.exe -l root -pw eichong -scp C:/Users/wangweihang/Desktop/acw/linux106/conf/start.sh root@10.9.2.106:/usr/local/epserver/gate
pscp.exe -l root -pw eichong -scp C:/Users/wangweihang/Desktop/acw/linux106/conf/mybatis.xml root@10.9.2.106:/usr/local/epserver/gate/conf
pscp.exe -l root -pw eichong -scp C:/Users/wangweihang/Desktop/acw/linux106/conf/applicationContext.xml root@10.9.2.106:/usr/local/epserver/gate/conf
pause