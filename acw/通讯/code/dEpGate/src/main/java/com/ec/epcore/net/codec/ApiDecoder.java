package com.ec.epcore.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.epcore.net.server.InnerApiMessage;
import com.ec.netcore.util.DeflaterAndInflater;

/**
 * 收消息，解码   (网关收到游戏逻辑服消息)
 * 
 * 消息结构： 4字节长度 + 2字节协议号  + byte[] 数据内容
 * 
 */
public class ApiDecoder extends ByteToMessageDecoder {
	
	private static final Logger logger = LoggerFactory.getLogger(ApiDecoder.class);
	
	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext,
			ByteBuf byteBuf, List<Object> list) throws Exception {
		
		//1、先标记读索引（必须）
		byteBuf.markReaderIndex();
		
		//2、判断可读数据长度是否小于 最小数据长度，若数据不足，则返回
		if(byteBuf.readableBytes() < 4 ){ //4字节长度 
			logger.info("读取消息的时候1，可读数据长度【{}】低于最小数据长度【{}】!",new Object[]{byteBuf.readableBytes(), 4 });
			return;
		}
		
		//4、读取消息内容长度，判断消息数据是否完整，若不完整，则重设读索引，并返回
		int length = byteBuf.readInt();//读取消息内容长度
		if(byteBuf.readableBytes() < length){//数据内容长度 
			logger.info("game->gate读取数据的时候2，可读数据长度【{}】低于实际数据长度【{}】!",new Object[]{byteBuf.readableBytes(), length });
			byteBuf.resetReaderIndex();
			return;
		}
		
		//5、读取 协议号
		short protocolNum = byteBuf.readShort();//读取协议号
		
		//6、读取收消息的用户id
		short userIdsLen = byteBuf.readShort();
		int[] userIds = new int[userIdsLen];
	    for (int i = 0; i < userIdsLen; ++i) {
	      userIds[i] = byteBuf.readInt();
	    }
		//7、读取是否压缩
		byte isCompress = byteBuf.readByte();
		
		//读取消息体       消息格式：4字节长度（其余数据的总长度），2字节协议号，2字节userId数组长度，userId数组,消息数据
		 byte[] bb = new byte[length - 2 - 2 - (userIdsLen * 4) - 1];
		byteBuf.readBytes(bb);//读取数据内容
		//如果压缩了，解压
		if(isCompress==1){
			//bb = DeflaterAndInflater.decompressBytes(bb);
			byte[] bbDeompress = DeflaterAndInflater.decompressBytes(bb);
			logger.info("解压前长度：	"+bb.length+"	,解压后长度：	"+bbDeompress.length);
			bb = bbDeompress;
		}
		
		//8、将协议号和数据内容转换成需要的对象
		InnerApiMessage message = new InnerApiMessage();
		
		message.setLength(length);
		message.setProtocolNum(protocolNum);
		message.setUserIds(userIds);
		message.setIsCompress(isCompress);
		message.setBytes(bb);
		
		list.add(message);
	}

	

}
