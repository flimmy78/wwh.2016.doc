package com.ec.service;

import com.ec.cache.BaseEPCache;
import com.ec.cache.BaseGunCache;
import com.ec.cache.UserCache;
import com.ec.cache.UserRealInfo;
import com.ec.netcore.queue.RepeatConQueue;
import com.ec.netcore.queue.RepeatMessage;
import com.ec.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractCacheService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(AbstractCacheService.class.getName()));

    protected static RepeatConQueue repeatMsgQue = new RepeatConQueue();

    public static void putRepeatMsg(RepeatMessage mes) {
        logger.debug(LogUtil.addExtLog("key|epGateReSendMsgQue"), mes.getKey(), repeatMsgQue.count());
        repeatMsgQue.put(mes);
    }

    public static void putSendMsg(RepeatMessage mes) {
        logger.debug(LogUtil.addExtLog("key|repeatMsgQue"), mes.getKey(), repeatMsgQue.count());
        repeatMsgQue.putSend(mes);
    }

    public static void removeRepeatMsg(String key) {
        logger.debug(LogUtil.addExtLog("key|repeatMsgQue"), key, repeatMsgQue.count());
        repeatMsgQue.remove(key);
    }

    //电桩
    protected static Map<String, BaseEPCache> mapEpCache = new ConcurrentHashMap<String, BaseEPCache>();

    public static Map<String, BaseEPCache> getMapEpCache() {
        return mapEpCache;
    }

    public static int getCurrentEpCount() {
        return mapEpCache.size();
    }

    public static void addEpCache(BaseEPCache epCache) {
        if (epCache != null) {
            String epCode = epCache.getCode();

            mapEpCache.put(epCode, epCache);
        }
    }

    public static BaseEPCache getEpCache(String epCode) {
        return mapEpCache.get(epCode);
    }

    public static void removeEpCache(String epCode) {
        mapEpCache.remove(epCode);
    }

    protected static Map<String, BaseGunCache> mapGun = new ConcurrentHashMap<String, BaseGunCache>();

    public static Map<String, BaseGunCache> getMapGun() {
        return mapGun;
    }

    public static BaseGunCache getEpGunCache(String epCode, int epGunNo) {
        String combEpGunNo = epCode + epGunNo;
        return mapGun.get(combEpGunNo);
    }

    public static void putEpGunCache(BaseGunCache cache) {
        if (cache == null)
            return;
        String epCode = cache.getEpCode();
        if (epCode == null || epCode.isEmpty())
            return;

        String combEpGunNo = epCode + cache.getEpGunNo();
        mapGun.put(combEpGunNo, cache);
    }

    protected static Map<Integer, UserCache> epUserInfoMap = new ConcurrentHashMap<>();

    public static Map<Integer, UserCache> getMapEpUserInfo() {
        return epUserInfoMap;
    }

    public static UserCache getUserCache(int userId) {
        return epUserInfoMap.get(userId);
    }

    public static void putUserCache(UserCache userCache) {
        if (userCache != null) {
            epUserInfoMap.put(userCache.getId(), userCache);
        }
    }

    public static UserCache convertToCache(UserRealInfo userRealInfo) {
        if (userRealInfo == null)
            return null;

        String userAccount = userRealInfo.getAccount();
        int userId = userRealInfo.getId();

        UserCache u = new UserCache(userId, userAccount, userRealInfo.getLevel());
        u.setUserRealInfo(userRealInfo);
        epUserInfoMap.put(userId, u);

        return u;
    }

    /*protected static Map<Integer, UserRealInfo> userInfoMap = new ConcurrentHashMap<Integer, UserRealInfo>();

    public static void addUserInfo(int userId, UserRealInfo userRealInfo) {
        userInfoMap.put(userId, userRealInfo);
    }

    public static void removeUserInfo(int userId) {
        userInfoMap.remove(userId);
    }

    public static UserRealInfo getUserInfo(int userId) {
        return userInfoMap.get(userId);
    }*/
}
