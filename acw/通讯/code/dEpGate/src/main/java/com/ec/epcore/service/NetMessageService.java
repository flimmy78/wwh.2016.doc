package com.ec.epcore.service;



public class NetMessageService {
	
	public static void checkEpReSendMessage()
	{
		CacheService.checkRepeatMsg();
		
	}
}
