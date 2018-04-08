package com.epgate.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epgate.cache.UserCache;
import com.epgate.net.proto.PhoneConstant;
import com.epgate.net.proto.WmIce104Util;
import com.epgate.net.sender.UsrGateMessageSender;
import com.epgate.net.server.UsrGateClient;
import com.epgate.net.server.UsrGateMessage;
import com.epgate.service.UserService;
import com.epgate.service.UsrGateService;
import com.netCore.util.ByteUtil;

/**
 * 收消息，解码   
 * 
 * 消息结构：2字节协议头+ 2字节长度 (小端)+ 1字节原因+2字节命令类型  + byte[] 数据内容

 */
public class UsrGateDecoder extends ByteToMessageDecoder {
	
	private static final Logger logger = LoggerFactory.getLogger(UsrGateDecoder.class);
	
	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext,
			ByteBuf byteBuf, List<Object> list) throws Exception {
		
		String errorMsg="";
		int readableBytes= byteBuf.readableBytes();
		if(readableBytes<7)//如果长度小于长度,不读
		{
		
			logger.debug("decode 1 readableBytes<7,readableBytes:{},channel:{}", readableBytes,channelHandlerContext.channel());


			return;
		}
		
		int pos= byteBuf.bytesBefore(PhoneConstant.HEAD_FLAG1);//找到的位置
		int pos1= byteBuf.bytesBefore(PhoneConstant.HEAD_FLAG2);//找到的位置
		int discardLen=0;
		if(pos < 0 || pos1<0 || (pos1-pos)!=1)//没找到，全部读掉
		{
			discardLen = readableBytes;
			logger.debug("decode not find flag header 0x45 0x43,readableBytes:{},channel:{}",readableBytes,channelHandlerContext.channel());
		}
		if(pos>0&&(pos1-pos)==1)
		{
			discardLen = pos;	
			logger.debug("decode  find flag header 0x45 0x43,pos:{},channel:{}",pos,channelHandlerContext.channel());
		}
		if(discardLen>0)
		{
			byte[] dicardBytes= new byte[discardLen];
			byteBuf.readBytes(dicardBytes);//
			
			logger.debug("discard>0 msg:{},channel:{}",WmIce104Util.ConvertHex(dicardBytes, 0),channelHandlerContext.channel());
			
			if(discardLen == readableBytes)
			{
				//没有数据可对，还回
				return;
			}
		}
		
		readableBytes= byteBuf.readableBytes();
		if(readableBytes<7)
		{
			logger.debug("decode readableBytes<7 readableBytes:{},channel:{}",readableBytes,channelHandlerContext.channel());
			
			return;
		}
		
		//1、先标记读索引（必须）
		byteBuf.markReaderIndex();
		
			
		int lengL = byteBuf.readByte()&0x0ff;
		int lengH = byteBuf.readByte()&0x0ff;
	    
		int msg_len = lengL+lengH*0x100;
		
		int remain_len = byteBuf.readableBytes();

		if(remain_len<msg_len )
		{
			logger.debug("ep remain_len<msg_len,remain_len:{},channel:{}", remain_len, channelHandlerContext.channel());
			
				
			byteBuf.resetReaderIndex();
			return ;
		}
		
		int cmdL = byteBuf.readByte()&0x0ff;
		int cmdH = byteBuf.readByte()&0x0ff;
	    
		int cmd = cmdL+cmdH*0x100;
		
		byte Msg[]= null;
		Msg= new byte[msg_len-2];
    	byteBuf.readBytes(Msg);

    	UsrGateMessage message = new UsrGateMessage();
    			
    	message.setLength(msg_len);
    	message.setCmd(cmd);

    	message.setBytes(Msg);
    			
    	list.add(message);
		
	}
	
	
	public static void decodeLogin(Channel channel,ByteBuffer byteBuffer)
	{
		int h = (int)byteBuffer.get();
		int m = (int)byteBuffer.get();
		int s = (int)byteBuffer.get();
		int usrGateId = byteBuffer.getInt();
		int vserion = (int)byteBuffer.get();
		UsrGateService.handleUsrGateLogin(channel, h,m,s,usrGateId,vserion);
	}
	
	public static void decodeAck(Channel channel,ByteBuffer byteBuffer)
	{
		short cmd  = byteBuffer.getShort();
		int h = (int)byteBuffer.get();
		int m = (int)byteBuffer.get();
		int s = (int)byteBuffer.get();
		
		UsrGateService.handleAck(channel,cmd,h,m,s);
	}
	public static void decodePhoneInit(Channel channel,ByteBuffer byteBuffer)
	{
		int h = (int)byteBuffer.get();
		int m = (int)byteBuffer.get();
		int s = (int)byteBuffer.get();
		
		String epCode = ByteUtil.getString(byteBuffer);
		int epGunNo = (int)byteBuffer.get();
		
		int usrId = byteBuffer.getInt();
		
		UsrGateService.handlePhoneInit(channel, h,m,s,epCode,epGunNo,usrId);	
	}
	public static void decodePhoneOnline(Channel channel,ByteBuffer byteBuffer)
	{
		int h = (int)byteBuffer.get();
		int m = (int)byteBuffer.get();
		int s = (int)byteBuffer.get();
		int usrId = byteBuffer.getInt();
		int online = (int)byteBuffer.get();
		
		UsrGateService.handlePhoneOnline(channel, h,m,s,usrId,online);
	}

	public static void decodeCharge(Channel channel,ByteBuffer byteBuffer)
	{
		int h = (int)byteBuffer.get();
		int m = (int)byteBuffer.get();
		int s = (int)byteBuffer.get();
		
		String epCode = ByteUtil.getString(byteBuffer);
		int epGunNo = (int)byteBuffer.get();
		
		int usrId = byteBuffer.getInt();
		
		int amt = byteBuffer.getInt();
		
		int chargeStyle = (int)byteBuffer.get();
		
		int bDispPrice = (int)byteBuffer.get();
		
		UsrGateService.handleCharge(channel, h,m,s,epCode,epGunNo,usrId,amt,chargeStyle,bDispPrice);
		
	}
	public static void decodeStopCharge(Channel channel,ByteBuffer byteBuffer)
	{
		int h = (int)byteBuffer.get();
		int m = (int)byteBuffer.get();
		int s = (int)byteBuffer.get();
		
		String epCode = ByteUtil.getString(byteBuffer);
		int epGunNo = (int)byteBuffer.get();
		
		int usrId = byteBuffer.getInt();
		UsrGateService.handleStopCharge(channel, h,m,s,epCode,epGunNo,usrId);
	}
	
	public static void decodeHeart(Channel channel,ByteBuffer byteBuffer)
	{
		//java.util.concurrent.
      	//byte[] data = PhoneProtocol.Package((byte)1, PhoneConstant.D_HEART);
        //UsrGateMessageSender.sendMessage(channel, data);
		UsrGateService.handleHeart(channel);
	}

	

}
