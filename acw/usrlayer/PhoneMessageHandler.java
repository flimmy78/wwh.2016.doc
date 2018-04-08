package com.epcentre.server;

import io.netty.channel.Channel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.cache.PhoneClient;
import com.epcentre.protocol.PhoneConstant;
import com.epcentre.protocol.PhoneProtocol;
import com.epcentre.protocol.StreamUtil;
import com.epcentre.service.PhoneService;
import com.epcentre.utils.DateUtil;


/**
 * 接受客户端数据并处理
 * @author lwz
 * 2015.10.11
 */
public class PhoneMessageHandler{
	
	private static final Logger logger = LoggerFactory.getLogger(PhoneMessageHandler.class);
	
	
	public static void handleMessage(Channel channel, PhoneMessage message) throws  IOException
	{
		PhoneClient phoneClient = PhoneService.getClient(channel);
		if(phoneClient==null)
		{
			logger.error("handleMessage error! not find PhoneClient:"+channel.toString());
			return ;
		}
   	    byte[] msg = message.getBytes();
   	 
		int msgLen = msg.length;

		InputStream in = new ByteArrayInputStream(msg);
		
		int cos = StreamUtil.read(in);
		byte typel = StreamUtil.read(in);
		byte typeh = StreamUtil.read(in);
		int type1 = typel + typeh * 0x100;
		
		int type = type1 & 0xffff;
		
	
		switch (type) 
		{
			case PhoneConstant.D_CONNET_EP:// 1 连接充电桩
			case PhoneConstant.D_NEW_CONNET_EP:
			case PhoneConstant.D_VERSION_CONNET_EP:
			{
				if (msgLen != 0x3C && msgLen != 0x3D) {
					logger.debug("type:{},msgLen:{}",type,msgLen);
					break;
				}
				// 1 终端机器编码 BCD码 8Byte 16位编码
				byte[] code = StreamUtil.readWithLength(in, 16);
				String epCode = new String(code);
				
				// 2 充电枪编号
				int epGunNo = StreamUtil.read(in);
				
				// 3 用户ID号
				long accountid = StreamUtil.readLong(in);
				
				byte[] bCheckCode = StreamUtil.readWithLength(in, 32);
				String checkCode = new String(bCheckCode);
				
				int cmdType=type;
				int version = 1;
				if(type ==PhoneConstant.D_VERSION_CONNET_EP)
				{
					version = StreamUtil.read(in);
				}
				PhoneService.handleConnectEp(channel,(int)accountid,epCode,epGunNo,checkCode,cmdType,version);
			}
			break;
		
             case PhoneConstant.D_START_CHARGE://10 充电
			 {
				 if (msgLen != 8) {
					 byte[] data = PhoneProtocol.do_confirm(PhoneConstant.D_START_CHARGE, (byte)0, (short)0);
			         PhoneMessageSender.sendMessage(channel, data);
					 break;
				}
				 phoneClient.setLastUseTime(DateUtil.getCurrentSeconds());
				//1 冻结金额
				int fronzeAmt = StreamUtil.readInt(in);
				//2 充电方式
				short chargeType = (short)StreamUtil.read(in);
				
				PhoneService.handleStartCharge(phoneClient,fronzeAmt,chargeType);
			 }
			 break;
			 
             case PhoneConstant.D_HEART://10 充电
             {
            	 phoneClient.setLastUseTime(DateUtil.getCurrentSeconds());
            	 PhoneService.handleHeart(phoneClient);
     			
             }
             break;
             case PhoneConstant.D_STOP_CHARGE://11停止
			 {
				 if (msgLen != 3) {
					 byte[] data = PhoneProtocol.do_confirm(PhoneConstant.D_STOP_CHARGE, (byte)0, (short)0);
			         PhoneMessageSender.sendMessage(channel, data);
					 break;
				}
				 phoneClient.setLastUseTime(DateUtil.getCurrentSeconds());
				
				PhoneService.handleStopCharge(phoneClient);
			 }
			 break;
             case PhoneConstant.D_CONSUME_RECORD://消费记录确认
			 {
				 if (msgLen != 26) {
					 logger.error("consume record ack msgLen error,type:{},msgLen:{}",PhoneConstant.D_CONSUME_RECORD,msgLen);
					 break;
				}
				 phoneClient.setLastUseTime(DateUtil.getCurrentSeconds());
				
				 byte[] order = StreamUtil.readWithLength(in, 21);
				 String chargeOrder = new String(order);
				 //2 充电方式
				 short result = (short)StreamUtil.readUB2(in);
				
				PhoneService.handleConsumeRecordConfirm(phoneClient,chargeOrder,result);
			 }
			 break;
             case PhoneConstant.D_GUN_CAR_STATUS://消费记录确认
			 {
				
				 phoneClient.setLastUseTime(DateUtil.getCurrentSeconds());
				
				
				
				PhoneService.handleGun2CarLinkStatusResp(phoneClient);
			 }
			 break;
             
			 default:
				 logger.debug("handleMessage,default type{}",type);
			 break;
		  }
	}
}
   

