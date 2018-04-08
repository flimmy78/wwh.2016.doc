package com.ec.service;

import com.ec.cache.BaseEPCache;
import com.ec.cache.RateInfoCache;
import com.ec.constants.EpConstants;
import com.ec.service.impl.RateServiceImpl;
import com.ec.utils.LogUtil;
import com.ormcore.model.RateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AbstractRateService {
    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(AbstractRateService.class.getName()));

    //费率缓存
    public static Map<Integer, RateInfoCache> rateMap = new ConcurrentHashMap<>();

    public static void parseRates(List<RateInfo> rateList)
    {
        if (rateList == null) return;
        for (RateInfo rateInfo : rateList) {
            RateInfoCache rateInfoCache = convertFromDb(rateInfo);

            if (rateInfoCache != null) {
                if (rateInfoCache.parseStage()) {
                    logger.debug(LogUtil.addExtLog("add rate,id"), rateInfo.getId());
                    AbstractRateService.AddRate(rateInfo.getId(), rateInfoCache);
                } else {
                    logger.error(LogUtil.addExtLog("rate info fail,rate id|rateInfo"), rateInfo.getId(), rateInfo.getQuantumDate());
                }
            }
        }
    }

    public static RateInfoCache convertFromDb(RateInfo rateInfo) {
        if (rateInfo == null)
            return null;
        RateInfoCache rateInfoCache = new RateInfoCache();
        rateInfoCache.setRateInfo(rateInfo);

        return rateInfoCache;
    }

    public static void checkModifyRate() {
        List<RateInfo> rateList = RateServiceImpl.getLastUpdate();
        parseRates(rateList);
    }


    public static RateInfoCache getRateInfo(String epCode) {
        BaseEPCache electricUser = AbstractCacheService.getEpCache(epCode);
        if (electricUser == null) {
            logger.error(LogUtil.addExtLog("dont find getRateInfo,epCode"), epCode);
            return null;
        }

        int rateInfoId = electricUser.getRateid();
        return getRateById(rateInfoId);
    }

    public static synchronized RateInfoCache getRateById(Integer Id) {
        return rateMap.get(Id);
    }

    public static synchronized void AddRate(Integer Id, RateInfoCache rate) {
        if (Id > 0 && rate != null) {
            rateMap.put(Id, rate);
        } else {
            logger.error(LogUtil.addExtLog("fail,because of,Id|rate"), Id, rate);
        }
    }
    //小于30分钟或第小于5分钟不收钱

    /**
     * 1.第一个30分钟，不足三十分钟按照三十分钟计算
     * 2.后面的三十分钟,小于5分钟不收钱,5-30按照三十分钟收钱
     *
     * @param realBespTime
     * @return
     */
    public static long calcBespTime(long realBespTime) {
        if (realBespTime < 0) {
            logger.error(LogUtil.addExtLog("realBespTime"), realBespTime);
            return 0;
        }
        long minUnit = EpConstants.MIN_BESP_TIME_UINT;//按三十分钟计价，不足三十分钟按三十分钟算
        long n1 = (int) realBespTime % minUnit;
        long n2 = (int) realBespTime / minUnit;
        long retRealBespTime;
        if (n2 == 0 || (n2 >= 1 && n1 > EpConstants.FREE_BESP_TIME)) {
            //1.第一个30分钟，不足三十分钟按照三十分钟计算
            //2.后面的三十分钟,小于5分钟不收钱,5-30按照三十分钟收钱
            retRealBespTime = ((realBespTime / minUnit) + 1) * minUnit / 60;
        } else {
            if (n1 <= EpConstants.FREE_BESP_TIME)//如果小于5分钟，扔掉
            {
                realBespTime = realBespTime - n1;
            }
            retRealBespTime = realBespTime / 60;
        }
        logger.info(LogUtil.addExtLog("realBespTime|retRealBespTime"), realBespTime, retRealBespTime);
        return retRealBespTime;
    }

    public static long calcBespTime(long st, long et, long user_cancel_t) {
        if (et < st) {
            logger.debug(LogUtil.addExtLog("st|et"), st, et);
            return 0;
        }
        if (st < 10000) {
            logger.debug(LogUtil.addExtLog("st"), st);
            return 0;
        }
        if (user_cancel_t > 0) {
            long diff = user_cancel_t - st;

            et = st + diff;
        }

        long realBespTime = et - st;
        logger.debug(LogUtil.addExtLog("st|et|user_cancel_t"), new Object[]{st, et, user_cancel_t});

        return calcBespTime(realBespTime);
    }

    public static BigDecimal calcBespAmt(BigDecimal bespokeRate, long bespTime) {
        BigDecimal bespAmt = new BigDecimal(bespTime);

        bespAmt = bespAmt.multiply(bespokeRate);
        bespAmt = bespAmt.setScale(2, BigDecimal.ROUND_HALF_UP);

        return bespAmt;
    }

}
