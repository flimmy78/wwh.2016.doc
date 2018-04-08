package com.ec.usrcore.service;

import com.ec.cache.BaseEPCache;
import com.ec.constants.EpConstants;
import com.ec.constants.ErrorCodeConstants;
import com.ec.service.AbstractEpChargeService;
import com.ec.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpChargeService extends AbstractEpChargeService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(EpChargeService.class.getName()));

    public static int initClientConnect(String token, int orgNo, String userIdentity, int severType,
                                        String epCode, int epGunNo, String checkCode) {
        int error = checkElectricDb(orgNo, epCode, epGunNo, userIdentity, null, severType, EpConstants.CHARGE_TYPE_QRCODE, checkCode);
        if (error > 0) return error;

        return EpGateService.sendClientConnect(epCode, epGunNo, Integer.valueOf(userIdentity));
    }

    /**
     * api开始充电
     *
     * @param epCode
     * @param epGunNo
     * @param accountId
     * @param account
     * @param bespNo
     * @param ermFlag
     * @param appClientIp
     * @param frozenAmt
     * @param source,但来自于爱充的用户需要收费，来自于其他合作伙伴有可能不冻结钱.只记录充电和消费记录
     * @return
     */
    public static int apiStartElectric(String token, int orgNo, String userIdentity, int severType, String epCode, int epGunNo,
                                       short startChargeStyle, int chargingAmt, int payMode, int watchPrice,
                                       String carCode, String vinCode) {
        int error = checkElectricDb(orgNo, epCode, epGunNo, userIdentity, null, severType, startChargeStyle, chargingAmt, payMode);
        if (error > 0) return error;

        return EpGateService.sendCharge(epCode, epGunNo, userIdentity, startChargeStyle, chargingAmt, payMode, watchPrice, orgNo, carCode, vinCode, token);
    }

    public static int apiStopElectric(String token, int orgNo, String userIdentity, String epCode, int epGunNo) {
        //检查电桩
        BaseEPCache epCache = CacheService.getEpCache(epCode);
        if (epCache == null) {
            epCache = EpService.getEpCacheFromDB(epCode);
            if (epCache == null) return ErrorCodeConstants.EP_UNCONNECTED;
        }

        return EpGateService.sendStopCharge(epCode, epGunNo, orgNo, userIdentity, token);
    }

    public static int queryOrderInfo(String token, int orgNo, String userIdentity, String epCode, int epGunNo) {
        if (epCode.length() != 16) {
            return ErrorCodeConstants.INVALID_EP_CODE;
        }

        //检查电桩
        BaseEPCache epCache = EpService.getEpCache(epCode);
        if (epCache == null) {
            return ErrorCodeConstants.EP_UNCONNECTED;
        }

        return EpGateService.sendOrderInfo(epCode, epGunNo, orgNo, userIdentity, token);
    }

    public static int phoneDisconnect(int orgNo, String userIdentity, String epCode, int epGunNo) {
        //检查电桩
        BaseEPCache epCache = EpService.getEpCache(epCode);
        if (epCache == null) {
            logger.debug(LogUtil.addExtLog("errorCode"), ErrorCodeConstants.EP_UNCONNECTED);
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        EpGunService.unUseEpGun(epCode, epGunNo, orgNo, Integer.valueOf(userIdentity));

        return EpGateService.sendClientOnline(epCode, Integer.valueOf(userIdentity), 0);
    }
}

