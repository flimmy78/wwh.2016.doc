package com.epgate.net.codec;

import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epgate.constant.EpGateConstant;
import com.epgate.net.proto.PhoneConstant;
import com.epgate.service.ChargingInfo;
import com.netCore.netty.buffer.DynamicByteBuffer;


public class UsrGateEncoder extends MessageToByteEncoder{


	private static final Logger logger = LoggerFactory.getLogger(UsrGateEncoder.class);
	
	
	/**
	 * 不管channel.write(arg0)发送的是什么类型，
	 * 最终都要组装成 ByteBuf 发送,
	 * 所以encode需要返回 ByteBuf 类型的对象
	 
	 * @param chc
	 * @param bb   (Message)
	 * @param byteBuf   (Byte)
	 * @return
	 * @throws Exception
	 */
	@Override
	protected void encode(ChannelHandlerContext chc, Object msg, ByteBuf byteBuf)
			throws Exception {
		
		if(msg instanceof ByteBuf){
			
			ByteBuf byteBufIn = (ByteBuf)msg;
			byte[] bb = new byte[byteBufIn.readableBytes()];
			byteBufIn.getBytes(0, bb);
			
			byteBuf.writeBytes(bb);
			
		}else if(msg instanceof byte[]){
			
			byte[] bb = (byte[])msg;
			byteBuf.writeBytes(bb);
			
		}else{
			
			logger.debug("未知的消息类型:{}",chc.channel().toString());
			
		}
	}
	
	public static  byte[] Package(int cmd,byte[] msgBody) {
		
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate(msgBody.length+4);
		
		short len = (short)msgBody.length;
		byteBuffer.put(PhoneConstant.HEAD_FLAG1);
		byteBuffer.put(PhoneConstant.HEAD_FLAG2);
		byteBuffer.putShort(len);
		byteBuffer.putShort((short)cmd);
		
		byteBuffer.put(msgBody);
		
		return byteBuffer.getBytes();
		
	}
	public static  byte[] login(int usrGateId,int epGateId,int h,int m, int s,int ret,int errorCode)
	{
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();
		
		byteBuffer.putInt(usrGateId);
		byteBuffer.putInt(epGateId);
		byteBuffer.put((byte)h);
		byteBuffer.put((byte)m);
		byteBuffer.put((byte)s);
		
		byteBuffer.put((byte)ret);
		byteBuffer.putShort((short)errorCode);
		
		return Package(EpGateConstant.EP_ACK,byteBuffer.getBytes());
	}
	public static  byte[] ack(short cmd,int h,int m, int s)
	{
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();
		
		byteBuffer.putShort(cmd);
		byteBuffer.put((byte)h);
		byteBuffer.put((byte)m);
		byteBuffer.put((byte)s);
		
		return Package(EpGateConstant.EP_ACK,byteBuffer.getBytes());
	}
	public static  byte[] do_connect_ep_resp(int h,int m, int s)
	{
		return null;
	}
	
	public static  byte[] phoneOnline(int h,int m, int s) {
		
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();
		
		byteBuffer.put((byte)h);
		byteBuffer.put((byte)m);
		byteBuffer.put((byte)s);
		
		return Package(EpGateConstant.EP_PHONE_ONLINE,byteBuffer.getBytes());
	}
	
	public static byte[] charge(int h,int m, int s, String epCode,int epGunNo,int  usrId,int ret,int errorCode)
	{
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();
		
		byteBuffer.put((byte)h);
		byteBuffer.put((byte)m);
		byteBuffer.put((byte)s);
		
		byteBuffer.putString(epCode);
		
		byteBuffer.put((byte)epGunNo);
		byteBuffer.putInt(usrId);
		byteBuffer.put((byte)ret);
		byteBuffer.putShort((short)errorCode);
		
		return Package(EpGateConstant.EP_CHARGE,byteBuffer.getBytes());
	}
	
	public static byte[] stopCharge(int h,int m, int s, String epCode,int epGunNo,int  usrId,int ret,int errorCode)
	{
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();
		
		byteBuffer.put((byte)h);
		byteBuffer.put((byte)m);
		byteBuffer.put((byte)s);
		
		byteBuffer.putString(epCode);
		byteBuffer.put((byte)epGunNo);
		
		byteBuffer.putInt(usrId);
		byteBuffer.put((byte)ret);
		byteBuffer.putShort((short)errorCode);
		
		return Package(EpGateConstant.EP_STOP_CHARGE,byteBuffer.getBytes());
	}
	
	public static byte[] chargeEvent(int h,int m, int s, String epCode,int epGunNo,int status)
	{
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();
		
		byteBuffer.put((byte)h);
		byteBuffer.put((byte)m);
		byteBuffer.put((byte)s);
		
		byteBuffer.putString(epCode);
		byteBuffer.put((byte)epGunNo);
		

		byteBuffer.put((byte)status);
		
		return Package(EpGateConstant.EP_CHARGE_EVENT,byteBuffer.getBytes());
	}
	
	public static byte[] chargeRecord(int h,int m, int s, String epCode,int epGunNo,
			String chargeOrder,long st,long et,int totalMeterNum,int totalAmt,int serviceAmt,int pkEpId)
	{
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();
		
		byteBuffer.put((byte)h);
		byteBuffer.put((byte)m);
		byteBuffer.put((byte)s);
		
		byteBuffer.putString(epCode);
		byteBuffer.put((byte)epGunNo);
		
		byteBuffer.putString(chargeOrder);
		byteBuffer.putLong(st);
		byteBuffer.putLong(et);
		byteBuffer.putInt(totalMeterNum);
		byteBuffer.putInt(totalAmt);
		byteBuffer.putInt(serviceAmt);
		byteBuffer.putInt(pkEpId);
		
		
		
		return Package(EpGateConstant.EP_CONSUME_RECODE,byteBuffer.getBytes());
	}
	public static byte[] chargeRealInfo(ChargingInfo info)
	{
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();
		
		/*byteBuffer.put((byte)h);
		byteBuffer.put((byte)m);
		byteBuffer.put((byte)s);
		
		byteBuffer.putString(epCode);
		byteBuffer.put((byte)epGunNo);
		

		byteBuffer.put((byte)status);*/
		
		return Package(EpGateConstant.EP_REALINFO,byteBuffer.getBytes());
	}
	public static byte[] do_ep_net_status(int h,int m,int s,int online,ArrayList<String> epCodes)
	{
		DynamicByteBuffer byteBuffer = DynamicByteBuffer.allocate();
		
		byteBuffer.put((byte)h);
		byteBuffer.put((byte)m);
		byteBuffer.put((byte)s);
		
		byteBuffer.put((byte)online);
		
		byteBuffer.putShort((short)epCodes.size());
		for(int i=0;i<epCodes.size();i++)
		{
			byteBuffer.putString(epCodes.get(i));
		}
		
		return Package(EpGateConstant.EP_ONLINE,byteBuffer.getBytes());
		
	}
	





}
