package com.ec.usrcore.service;

import com.ec.cache.BaseEPCache;
import com.ec.netcore.core.pool.TaskPoolFactory;
import com.ec.service.AbstractCacheService;
import com.ec.usrcore.config.GameBaseConfig;
import com.ec.usrcore.net.client.EpGateNetConnect;
import com.ec.usrcore.task.CheckEpGateNetTimeOutTask;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheService extends AbstractCacheService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(CacheService.class.getName()));

    public static void startEpGateCommTimer(long initDelay) {
        CheckEpGateNetTimeOutTask checkTask = new CheckEpGateNetTimeOutTask();

        TaskPoolFactory.scheduleAtFixedRate("CHECK_EPGATENET_TIMEOUT_TASK", checkTask, initDelay, 10, TimeUnit.SECONDS);
    }

    /**
     * EpGate缓存
     */
    private static Map<Integer, EpGateNetConnect> epGateCommClents = new ConcurrentHashMap<Integer, EpGateNetConnect>();

    public static Map<Integer, EpGateNetConnect> getMapEpGate() {
        return epGateCommClents;
    }

    public static EpGateNetConnect getEpGate(int pkGateId) {
        return epGateCommClents.get(pkGateId);
    }

    public static void addEpGate(int pkGateId, EpGateNetConnect epGateClient) {
        epGateCommClents.put(pkGateId, epGateClient);
    }

    public static void removeEpGate(int pkGateId) {
        epGateCommClents.remove(pkGateId);
    }

    public static EpGateNetConnect getEpGate(String epCode) {
        BaseEPCache epCache = getEpCache(epCode);
        if (epCache == null || epCache.getGateid() == 0)
            return null;

        return epGateCommClents.get(epCache.getGateid());
    }

    private static Map<Channel, EpGateNetConnect> epGateChannel = new ConcurrentHashMap<Channel, EpGateNetConnect>();

    public static EpGateNetConnect getEpGateByCh(Channel channel) {
        return epGateChannel.get(channel);
    }

    public static void addEpGateByCh(Channel channel, EpGateNetConnect epGateClient) {
        epGateChannel.put(channel, epGateClient);
    }

    public static void removeEpGateByCh(Channel channel) {
        epGateChannel.remove(channel);
    }

    @SuppressWarnings("rawtypes")
    public static void checkEpGateTimeOut() {
        Iterator<Entry<Integer, EpGateNetConnect>> iter = epGateCommClents.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            EpGateNetConnect commClient = (EpGateNetConnect) entry.getValue();
            if (commClient == null) {
                logger.error(LogUtil.getExtLog("commClient is null"));
                continue;
            }

            //需要连接
            boolean bNeedReConnect = false;
            //检查
            long connectDiff = DateUtil.getCurrentSeconds() - commClient.getLastUseTime();
            //检查连接，是否需要重连
            int commStatus = commClient.getStatus();
            if (commStatus == 0 || commStatus == 1) {
                int times = (commClient.getConnectTimes() / 6) + 1;
                if (connectDiff > (times * GameBaseConfig.reconnectInterval)) {
                    bNeedReConnect = true;
                }
            } else {
                if (connectDiff > GameBaseConfig.netKeepLiveInterval) {
                    bNeedReConnect = true;
                }

            }

            if (bNeedReConnect) {
                commClient.reconnection();
            }

            long now = DateUtil.getCurrentSeconds();
            //1.检查连接是否活动,不活动的话发送心跳侦
            long activeDiff = now - commClient.getLastSendTime();
            if (activeDiff >= GameBaseConfig.heartInterval) {
                commClient.setLastSendTime(now);
                if (commClient.getChannel() != null) {
                    EpGateService.sendHeart(commClient.getChannel());
                }
            }
        }
    }
}
