package com.epcentre.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.cache.EpCommClient;
import com.epcentre.cache.INetObject;
import com.epcentre.cache.PhoneClient;
import com.epcentre.config.GameConfig;
import com.epcentre.protocol.EpEncodeProtocol;
import com.epcentre.service.EpGunService;
import com.epcentre.service.PhoneService;
import com.epcentre.utils.DateUtil;
import com.netCore.queue.RepeatMessage;
/**
 * 发送消息必须通过此类
 * @author 
 * 2014-11-28 上午11:32:54
 */
public class PhoneMessageSender {
	
	private static final Logger logger = LoggerFactory.getLogger(PhoneMessageSender.class);
	
	public static ChannelFuture  sendMessage(Channel channel,Object object) {
		if(channel == null)
			return null;
		 
		if (channel == null || !channel.isWritable()) {
			return null;
		}
		
        PhoneClient phoneClient = PhoneService.getPhoneClientByChannel(channel);
		
        /*if(phoneClient !=null && MsgWhiteList.isOpen())
		{
			ElectricUserInfo u = phoneClient.getUserinfo();
			if(u !=null && MsgWhiteList.find(u.getPhone()))
			{
			    FileUtils.appendLog(u.getPhone()+"_phone.log", WmIce104Util.ConvertHex((byte[])object,3));
			}
		}*/
		ChannelFuture future = channel.writeAndFlush(object);
		return future;
	}
	
	
	public static ChannelFuture  sendRepeatMessage(Channel channel,String repeatMsgKey,byte[] msg,int version) {
		
		//1.先立即发送
		sendMessage(channel,msg);
		
		if(GameConfig.resendPhoneMsgFlag ==1 && version>=2)
		{
			RepeatMessage repeatMsg= 
					new RepeatMessage(channel,6,GameConfig.resendPhoneMsgTime,repeatMsgKey,msg);
			
			repeatMsg.setLastSendTime( DateUtil.getCurrentSeconds());
			EpGunService.putSendMsg(repeatMsg);
		}
		
		return null;
		
	}
	
	
	

	
}
