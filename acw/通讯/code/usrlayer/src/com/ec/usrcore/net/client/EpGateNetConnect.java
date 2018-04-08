package com.ec.usrcore.net.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.netcore.model.conf.ClientConfig;
import com.ec.netcore.netty.client.AbstractNettyClient;
import com.ec.netcore.util.IPUtil;
import com.ec.usrcore.net.codec.EpGateDecoder;
import com.ec.usrcore.net.codec.EpGateEncoder;
import com.ec.usrcore.server.CommonServer;
import com.ec.usrcore.service.CacheService;
import com.ec.usrcore.service.EpGateService;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;


public class EpGateNetConnect extends AbstractNettyClient {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(EpGateNetConnect.class.getName()));

    public EpGateNetConnect(ClientConfig cfg, ByteToMessageDecoder decoder, MessageToByteEncoder<?> encoder) {
        super(cfg, decoder, encoder);
        //clientConfig =  cfg;
        identity = cfg.getIp() + cfg.getPort();

    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
        status = 0;
    }

    private long lastSendTime = 0;

    public static EpGateNetConnect getNewInstance(ClientConfig clrCfg) {
        ByteToMessageDecoder decoder = new EpGateDecoder();
        MessageToByteEncoder encoder = new EpGateEncoder();

        return new EpGateNetConnect(clrCfg, decoder, encoder);
    }

    public long getLastSendTime() {
        return lastSendTime;
    }


    public void setLastSendTime(long lastSendTime) {
        this.lastSendTime = lastSendTime;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void channelClosed(ChannelHandlerContext ctx) {
        //logger.info("server close...");
        try {
            Channel channel = ctx.channel();

            Iterator iter = CacheService.getMapEpGate().entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                EpGateNetConnect epGateClient = (EpGateNetConnect) entry.getValue();
                if (null == epGateClient) continue;
                if (epGateClient.getChannel().equals(channel)) {
                    CacheService.removeEpGate((int) entry.getKey());
                }
            }
            CacheService.removeEpGateByCh(channel);

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx) {
        logger.info(LogUtil.getExtLog("server conn..."));
        //服务服连接成功
        Channel channel = ctx.channel();
        //commClient.clearConnecTtimes();
        setStatus(1);
        setChannel(channel);

        setLastSendTime(DateUtil.getCurrentSeconds());
        EpGateService.sendEpGateLogin(channel, CommonServer.getInstance().getSeverType());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        logger.info(LogUtil.addExtLog("server exception..."));
        close();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object obj) {
        //logger.info("server receive...");
        Channel channel = ctx.channel();
        String name = IPUtil.getNameByChannel(channel);

        EpGateMessage message = (EpGateMessage) obj;
        EpGateMessageHandler.handleMessage(channel, message);
    }

    @Override
    public void stop() {
        super.stop();
        logger.info(LogUtil.addExtLog("server stop..."));

    }

    @Override
    public void regiest(Channel arg0) {
        // TODO Auto-generated method stub
    }

}
