package com.ec.epcore.service;

import com.ec.cache.UserCache;
import com.ec.cache.UserRealInfo;
import com.ec.epcore.config.GameConfig;
import com.ec.service.impl.UserServiceImpl;
import com.ec.service.AbstractCacheService;
import com.ec.utils.LogUtil;
import com.ormcore.model.TblUserThreshod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheService extends AbstractCacheService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(CacheService.class.getName()));

    public static String getCacheSize()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("EpService:\n");

        sb.append("mapEpCache count:").append(mapEpCache.size()).append("\n\n");

        return sb.toString();
    }

    public static String getGunCacheSize()
    {
        return "EpGunService:\nmapGun.size():"+ mapGun.size()+"\n\n";
    }

    public static void checkRepeatMsg()
    {
        if(GameConfig.resendEpMsgFlag==1)
        {
            logger.debug("checkRepeatMsg start repeatMsgQue,{}", repeatMsgQue.count());
            repeatMsgQue.checkSend(0);
        }
        else
        {
            logger.debug("checkRepeatMsg GameConfig.resendEpMsgFlag:{}",GameConfig.resendEpMsgFlag);
        }
    }

    public static UserCache convertToCache(UserRealInfo userRealInfo) {
        if (userRealInfo == null)
            return null;

        String usrAccount = userRealInfo.getAccount();
        int userId = userRealInfo.getId();

        UserCache u = new UserCache(userId, usrAccount, userRealInfo.getName(), userRealInfo.getLevel(), userRealInfo.getInvitePhone());

        u.setRemainAmtWarnPhone("");
        u.setRemainAmtWarnCPhone("");
        u.setRemainAmtWarnValue(999999);
        if (usrAccount != null && usrAccount.length() == 12) {
            TblUserThreshod usrThreshod = UserServiceImpl.findUserThreshodInfo(userId);
            if (null != usrThreshod) {
                u.setRemainAmtWarnPhone(usrThreshod.getPhone());
                u.setRemainAmtWarnCPhone(usrThreshod.getCustomerPhone());
                u.setRemainAmtWarnValue(usrThreshod.getWarnMoney());
            }
        }
        //查询并设置用户是否新手
        UserService.setUserNewcoupon(u);

        if (epUserInfoMap.get(userId) == null) {
            epUserInfoMap.put(userId, u);
        }

        return u;
    }

/*    public static void startEpGateCommTimer(long initDelay) {
        CheckEpGateNetTimeOutTask checkTask = new CheckEpGateNetTimeOutTask();

        TaskPoolFactory.scheduleAtFixedRate("CHECK_EPGATENET_TIMEOUT_TASK", checkTask, initDelay, 10, TimeUnit.SECONDS);
    }

    *//**
     * EpGate缓存
     *//*
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
            Entry entry = (Entry) iter.next();

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
    }*/
}
