package com.ec.service;

import com.ec.cache.BaseGunCache;
import com.ec.cache.ChargeCache;
import com.ec.cache.UserCache;
import com.ec.constants.GunConstants;
import com.ec.constants.YXCConstants;
import com.ec.service.impl.EpGunServiceImpl;
import com.ec.utils.LogUtil;
import com.ormcore.model.TblElectricPileGun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEpGunService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(AbstractEpGunService.class.getName()));

    public static boolean checkWorkStatus(int status) {
        if (status != GunConstants.EP_GUN_W_STATUS_OFF_LINE &&
                status != GunConstants.EP_GUN_W_STATUS_FAULT &&
                status != GunConstants.EP_GUN_W_STATUS_IDLE &&
                status != GunConstants.EP_GUN_W_STATUS_WORK &&
                status != GunConstants.EP_GUN_W_STATUS_BESPOKE &&
                status != GunConstants.EP_GUN_W_STATUS_UPGRADE &&
                status != GunConstants.EP_GUN_W_STATUS_USER_OPER &&
                status != GunConstants.EP_GUN_W_STATUS_SETTING &&
                status != GunConstants.EP_GUN_W_STATUS_SELECT_CHARGE_MODE &&
                status != GunConstants.EP_GUN_W_INIT &&
                status < (GunConstants.EP_GUN_W_STATUS_SELECT_CHARGE_MODE + 1))//以后充电模式加了状态往后移
        {
            return false;
        } else {
            if (status > GunConstants.EP_GUN_W_STATUS_URGENT_STOP)
                return false;
        }
        return true;
    }

    public static boolean checkCardInfoAddr(int addr) {
        return (addr >= YXCConstants.YC_BATTARY_TYPE && addr <= YXCConstants.YC_BATTRY_CAN_HIGH_TEMP)
                || (addr >= YXCConstants.YC_SIGNLE_BATTRY_HIGH_VOL_GROUP && addr <= YXCConstants.YC_CAR_BATTRY_TOTAL_VOL)
                || addr == YXCConstants.YC_VAR_CAR_VIN || addr == YXCConstants.YC_VAR_BATTARY_FACTORY;
    }

    public static boolean checkCarPlaceLockStatus(int status) {
        return !(status != 0 &&
                status != 1 &&
                status != 2 &&
                status != 3 &&
                status != 4);
    }

    public static int convertEpWorkStatus(int epWorStatus) {
        int ret = -1;
        switch (epWorStatus) {

            case GunConstants.EP_GUN_W_STATUS_OFF_LINE://离线
                ret = GunConstants.EP_GUN_STATUS_OFF_LINE;
                break;
            case GunConstants.EP_GUN_W_STATUS_FAULT://故障，停用
                ret = GunConstants.EP_GUN_STATUS_STOP_USE;
                break;
            case GunConstants.EP_GUN_W_STATUS_IDLE://空闲
                ret = GunConstants.EP_GUN_STATUS_IDLE;//空闲
                break;
            case GunConstants.EP_GUN_W_STATUS_WORK:// 工作(充电)
                ret = GunConstants.EP_GUN_STATUS_CHARGE;
                break;
            case GunConstants.EP_GUN_W_STATUS_BESPOKE://预约
                ret = GunConstants.EP_GUN_STATUS_BESPOKE_LOCKED;
                break;
            case GunConstants.EP_GUN_W_STATUS_UPGRADE:// 在线升级
                ret = GunConstants.EP_GUN_STATUS_EP_UPGRADE;
                break;
            case GunConstants.EP_GUN_W_STATUS_USER_OPER:// 操作中(待定,防止用户在操作中被预约)
                ret = GunConstants.EP_GUN_STATUS_EP_OPER;//操作
                break;
            case GunConstants.EP_GUN_W_STATUS_WAIT_CHARGE:// 等待充电
                ret = GunConstants.EP_GUN_STATUS_WAIT_CHARGE;//等待充电
                break;
            case GunConstants.EP_GUN_W_STATUS_TIMER_CHARGE:// 定时等待
                ret = GunConstants.EP_GUN_STATUS_TIMER_CHARGE;//定时等待
                break;
            case GunConstants.EP_GUN_W_STATUS_SETTING://设置状态
                ret = GunConstants.EP_GUN_STATUS_SETTING;//设置
                break;

            case GunConstants.EP_GUN_W_STATUS_SELECT_CHARGE_MODE://充电模式选择
                ret = GunConstants.EP_GUN_STATUS_SELECT_CHARGE_MODE;
                break;
            default:
                if (epWorStatus >= GunConstants.EP_GUN_W_STATUS_LESS_VOL_FAULT && epWorStatus <= GunConstants.EP_GUN_W_STATUS_URGENT_STOP) {
                    ret = GunConstants.EP_GUN_STATUS_STOP_USE;
                }
                break;
        }
        return ret;
    }

    public static void modifyStatus(BaseGunCache epGunCache, int status, boolean modifyDb) {
        logger.debug(LogUtil.addExtLog("this.status|status"), epGunCache.getStatus(), status);
        epGunCache.setStatus(status);

        if (modifyDb) {
            EpGunServiceImpl.updateGunState(epGunCache.getPkEpGunId(), status);
        }
    }

    public static BaseGunCache getEpGunCache(int pkEpId, String epCode, int epGunNo) {
        BaseGunCache epGunCache = AbstractCacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache == null) epGunCache = new BaseGunCache(epCode, epGunNo);

        TblElectricPileGun tblEpGun = EpGunServiceImpl.getDbEpGun(pkEpId, epGunNo);
        if (tblEpGun == null) {
            logger.error(LogUtil.addExtLog("init error!did not find gun,pkEpId|epGunNo"), pkEpId, epGunNo);
            return null;
        }

        int epGunStatusInDb = tblEpGun.getEpState();
        //以数据库最后枪头状态为准
        modifyStatus(epGunCache, epGunStatusInDb, false);

        epGunCache.setPkEpGunId(tblEpGun.getPkEpGunId());

        AbstractCacheService.putEpGunCache(epGunCache);

        return epGunCache;
    }

    public static boolean loadUnFinishedWork(BaseGunCache epGunCache) {
        String epCode = epGunCache.getEpCode();
        int epGunNo = epGunCache.getEpGunNo();
        epGunCache.setChargeCache(null);

        //3.取最新的未完成的充电记录
        ChargeCache tmpChargeCache = AbstractEpChargeService.GetUnFinishChargeCache(epCode, epGunNo);
        if (tmpChargeCache != null) {
            logger.debug(LogUtil.addExtLog("tmpChargeCache.getStatus()"), tmpChargeCache.getStatus());
            //String chargeAccount = tmpChargeCache.getAccount();
            //装载未完成充电用户
            UserCache userCache = AbstractCacheService.getUserCache(tmpChargeCache.getUserId());
            if (userCache != null) {
                logger.error(LogUtil.addExtLog("epCode|epGunNo|set chargeInfo to userCache"), new Object[]{epCode, epGunNo, userCache});
                if (userCache.getId() == tmpChargeCache.getUserId()) userCache.addCharge(tmpChargeCache);
                tmpChargeCache.getUserOrigin().setCmdChIdentity(userCache.getAccount());
            }
            epGunCache.setChargeCache(tmpChargeCache);
        }

        logger.info(LogUtil.addExtLog("epCode|epGunNo|init status"), new Object[]{epCode, epGunNo, epGunCache.getStatus()});

        return true;
    }

}
