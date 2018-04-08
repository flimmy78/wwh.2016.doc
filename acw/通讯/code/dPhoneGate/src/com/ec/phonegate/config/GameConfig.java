package com.ec.phonegate.config;


import java.util.Properties;

import com.ec.usrcore.config.GameBaseConfig;

public class GameConfig extends GameBaseConfig{
	
	/** 端口号 */
	public static int port;

	/**手机连接超时*/
	public static int phoneConnectTimeout;

	public GameConfig()
    {
		GameConfig.loadGameConfig();
	}
	
	/**
	 * 加载GameConfig.properties文件
	 * @author 
	 * 2014-11-26
	 */
	public static void loadGameConfig(){
		Properties p = getProperties(fileName);
		
		port = Integer.valueOf(p.getProperty("port","8888").trim());
		
		phoneConnectTimeout = Integer.valueOf(p.getProperty("phoneConnectTimeout","60").trim());
	}

}
