package com.ec.netcore.client;

import com.ec.netcore.constants.CommStatusConstant;
import com.ec.netcore.util.TimeUtil;

import io.netty.channel.Channel;

public class ECTcpClient  implements ITcpClient {
	
	protected Channel channel=null;
	
	protected int type;//1:电桩(电桩编号);2：集中器(c+集中器id);3:api(ip);4:监控(w+ip)
	
	protected String identity="";
	
	protected int status =0;//0:未初始化，1:初始化成功;2：完成.准备关掉
	
	protected long lastUseTime;
	
	protected int version;
	
	
	public ECTcpClient()
	{
		this.channel = null;
		this.identity = "";
		this.status = 0;
		this.lastUseTime = 0;
		this.type=0;
		this.version =0;
	}
	
	
	
	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}
	
	public String getIdentity() {
		return identity;
	}

	public long getLastUseTime() {
		return lastUseTime;
	}

	public void setLastUseTime(long lastUseTime) {
		this.lastUseTime = lastUseTime;
	}

	public int getStatus()
	{
		return this.status;
		
	}
	public void setStatus(int status) {
		this.status = status;
	}
	
	public int getVersion() {
		return version;
	}



	public void setVersion(int version) {
		this.version = version;
	}



	public boolean isComm()
	{
        return status >= 2;
    }
	
	 public void close()
	{
		if(channel!=null)
		{
			channel.close();
			channel=null;
		}
		setStatus(0);
	}
	public void handleNetTimeOut()
	{
		close();
	}
	public void initNetSuccess(String identity,int version)
	{
		setStatus(CommStatusConstant.INIT_SUCCESS);
		setIdentity(identity);
		this.version = version;
		
		setLastUseTime(TimeUtil.currentTimeSecond());
	}
	

}
