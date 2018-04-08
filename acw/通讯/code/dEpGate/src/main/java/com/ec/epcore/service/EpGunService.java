package com.ec.epcore.service;

import com.ec.cache.ChargeCardCache;
import com.ec.cache.UserCache;
import com.ec.constants.*;
import com.ec.epcore.cache.EpGunCache;
import com.ec.epcore.cache.RealACChargeInfo;
import com.ec.epcore.cache.RealDCChargeInfo;
import com.ec.epcore.config.GameConfig;
import com.ec.epcore.net.client.EpCommClient;
import com.ec.epcore.net.codec.EpEncoder;
import com.ec.epcore.net.proto.ConsumeRecord;
import com.ec.epcore.sender.EpMessageSender;
import com.ec.epcore.task.CheckGunTask;
import com.ec.epcore.task.EpMessageTask;
import com.ec.logs.LogConstants;
import com.ec.net.proto.Iec104Constant;
import com.ec.net.proto.SingleInfo;
import com.ec.netcore.core.pool.TaskPoolFactory;
import com.ec.service.AbstractEpGunService;
import com.ec.service.impl.ChargeServiceImpl;
import com.ec.service.impl.EpGunServiceImpl;
import com.ec.service.impl.UserServiceImpl;
import com.ec.utils.LogUtil;
import com.ec.utils.NetUtils;
import com.ec.utils.NumUtil;
import com.ormcore.model.RateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class EpGunService extends AbstractEpGunService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(EpGunService.class.getName()));

    @SuppressWarnings("rawtypes")
    public static void checkTimeout() {
        Iterator iter = CacheService.getMapGun().entrySet().iterator();
        int count = 0;
        while (iter.hasNext()) {

            count++;
            if (count > 10) {
                NetUtils.sleep(10);
                count = 0;
            }
            Map.Entry entry = (Map.Entry) iter.next();
            if (entry == null) {
                break;
            }

            EpGunCache epGunCache = (EpGunCache) entry.getValue();
            if (null == epGunCache) {
                logger.info("checkTimeout: epGunCache=null:\n");
                continue;
            }
            checkActionTimeOut(epGunCache);
        }
    }

    public static int checkAction(int gunStatus, int gunUserId, int action, int actionUserId) {

        /**
         * status.
         * 0:空闲；可以接收1.电桩用户名和密码鉴权，2.前端预约，3，前端充电，状态都转为10
         * 3：预约；可以接收1.取消预约，状态转为11,2.预约用户充电.在收到充电后转为6
         * 6，充电，可以接收1.停止充电,状态转为0，2.故障原因自动停止，状态转为9
         * 9：停用；不接收预约，不接收充高点
         * 10；桩操作占用.接受二维码充电
         * 11；预约冷却.接收同一用户充电和再预约
         * 12:用户鉴权成功
         */

        int ret = 0;
        switch (action) {
            case EventConstant.EVENT_DROP_CARPLACE: {
                if (gunStatus == GunConstants.EP_GUN_STATUS_BESPOKE_LOCKED) {
                    if (gunUserId != 0 && gunUserId != actionUserId) {
                        ret = ErrorCodeConstants.BESP_NO_NOT_EXIST;
                    }
                    return ret;
                }
                ret = ErrorCodeConstants.BESP_NO_NOT_EXIST;
            }
            break;
            default:
                break;

        }
        return ret;

    }

    public synchronized static void checkActionTimeOut(EpGunCache epGunCache) {
        if (epGunCache == null)
            return;
        //检查充电超时
        epGunCache.checkChargeCmdTimeOut(3);
    }

    public static void startCheckTimeoutServer(long initDelay) {

        CheckGunTask checkTask = new CheckGunTask();

        TaskPoolFactory.scheduleAtFixedRate("CHECK_BESPOKE_CHARGE_TASK", checkTask, initDelay, 5, TimeUnit.SECONDS);
    }

    public static void updateChargeInfoToDbByEpCode(int currentType, String epCode, int epGunNo, BigDecimal bdMeterNum, String serialNo, BigDecimal fronzeAmt, int startTime, int userId) {

        if (currentType != EpConstants.EP_AC_TYPE && currentType != EpConstants.EP_DC_TYPE) {
            logger.error("updateChargeInfoToDbByEpCode currentType error,epCode:{},epGunNo:{},currentType:{},accountId:{}",
                    new Object[]{epCode, epGunNo, currentType, userId});

            return;
        }
        if (currentType == EpConstants.EP_AC_TYPE) {
            ChargeServiceImpl.updateChargeACInfo(epCode, epGunNo, bdMeterNum, serialNo, fronzeAmt, startTime, userId);
        } else if (currentType == EpConstants.EP_DC_TYPE) {
            ChargeServiceImpl.updateChargeDCInfo(epCode, epGunNo, bdMeterNum, serialNo, fronzeAmt, startTime, userId);
        }
    }

    public static int dropCarPlaceLockAction(String epCode, int epGunNo, int accountId, float lng, float lag) {
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);

        if (epGunCache == null) {
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        //判断附近


        //TODO:userId
        int errorCode = checkAction(epGunCache.getStatus(), 0/*epGunCache.getCurUserId()*/, EventConstant.EVENT_DROP_CARPLACE, accountId);
        if (errorCode > 0)
            return errorCode;

        return epGunCache.dropCarPlaceLockAction();
    }

    public static void handleCardAuth(String epCode, int epGunNo, int userOrgin, String innerCardNo, String outCardNo, byte[] cmdTimes) {
        Map<String, Object> respMap = new ConcurrentHashMap<>();
        respMap.put("epcode", epCode);
        respMap.put("epgunno", epGunNo);
        respMap.put("innerno", innerCardNo);
        respMap.put("outno", outCardNo);
        respMap.put("cmdTimes", cmdTimes);

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache != null) {
            epGunCache.handleEventExtra(EventConstant.EVENT_CARD_AUTH, 5, 0, 0, null, respMap);

        } else {
            logger.error("card charge userCardAuth fail,epCode:{},epGunNo:{},inCardNo:{}  not find EpGunCache",
                    new Object[]{epCode, epGunNo, innerCardNo});
        }
    }

    public static void onAuthResp(String epCode, int epGunNo, String innerNo, String outerNo, int ret, int errorCode, byte[] cmdTimes) {
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache == null) {

            logger.info("onAuthResp,card charge userCardAuth fail,epCode:{},epGunNo:{},inCardNo:{} not find EpGunCache",
                    new Object[]{epCode, epGunNo, innerNo});
        } else {
            byte[] data = EpEncoder.do_card_auth_resq(epCode, epGunNo, innerNo, outerNo, 1, 0, 0, ret, errorCode);
            EpCommClient epCommClient = (EpCommClient) epGunCache.getEpNetObject();
            EpMessageSender.sendMessage(epCommClient, 0, 0, Iec104Constant.C_CARD_AUTH_RESP, data, cmdTimes, epCommClient.getVersion());

            logger.info("onAuthResp,card charge,userCardAuth resp epCode:{},epGunNo:{},inCardNo:{}",
                    new Object[]{epCode, epGunNo, innerNo});
        }
    }


    public static void clearIdentyCode(int pkEpId) {
        //保存到数据库
        EpGunServiceImpl.updateQR(pkEpId, 0, "");

        logger.debug("clearIdentyCode: updateQR,pkEpId:{}", pkEpId);
    }

    /**
     * @param addr
     * @param epType
     * @param epGunCache
     * @return
     */
    public static SingleInfo getSingleInfo(int addr, int epType, EpGunCache epGunCache) {
        SingleInfo singInfo = null;
        if (epType == EpConstants.EP_AC_TYPE) {
            RealACChargeInfo realChargeInfo = (RealACChargeInfo) epGunCache.getRealChargeInfo();
            singInfo = realChargeInfo.getFieldValue(addr);

        } else {
            RealDCChargeInfo realChargeInfo = (RealDCChargeInfo) epGunCache.getRealChargeInfo();
            singInfo = realChargeInfo.getFieldValue(addr);

        }
        return singInfo;
    }

    public static void startRepeatSendMessage() {

        EpMessageTask checkTask = new EpMessageTask();

        TaskPoolFactory.scheduleAtFixedRate("REPEAT_EP_MESSAGE_TASK", checkTask, 5, 5, TimeUnit.SECONDS);
    }

    public static int checkChargeAmt(int usrId, int fronzeAmt, int payMode, ConsumeRecord consumeRecord) {
        int ret = checkChargeAmt(String.valueOf(usrId), UserConstants.ORG_I_CHARGE, consumeRecord);
        if (ret < 0 || payMode != EpConstants.P_M_FIRST) return ret;

        int chargeAmt = consumeRecord.getTotalChargeAmt();
        int serviceAmt = consumeRecord.getServiceAmt();
        int consumeAmt = chargeAmt + serviceAmt;
        String epCode = consumeRecord.getEpCode();
        int gunNo = consumeRecord.getEpGunNo();
        String serialNo = consumeRecord.getSerialNo();
        int chargeCost = 0;
        if (consumeRecord.getType() == 1) fronzeAmt = fronzeAmt * 100;

        if (fronzeAmt < consumeAmt) {
            int diff = consumeAmt - fronzeAmt;
            logger.error(LogUtil.addBaseExtLog("stat error,fronzeAmt| < totalConsumeAmt|diff|serialNo")
                    , new Object[]{LogConstants.FUNC_END_CHARGE, epCode, gunNo, UserConstants.ORG_I_CHARGE, usrId, fronzeAmt, consumeAmt, (consumeAmt - fronzeAmt), serialNo});
            chargeCost = fronzeAmt;

            serviceAmt = serviceAmt - diff;//多出的钱从服务金额中扣除
            if (serviceAmt < 0) {
                serviceAmt = 0;
                chargeAmt = fronzeAmt;
            }
            logger.error(LogUtil.addBaseExtLog("chargeAmt|serviceAmt|serialNo")
                    , new Object[]{LogConstants.FUNC_END_CHARGE, epCode, gunNo, UserConstants.ORG_I_CHARGE, usrId, chargeAmt, serviceAmt, serialNo});
            consumeRecord.setTotalChargeAmt(chargeAmt);
            consumeRecord.setServiceAmt(serviceAmt);
        } else {
            chargeCost = consumeAmt;
        }
        consumeRecord.setTotalAmt(chargeCost);    //计算总的消费金额

        return 0;
    }

    public static int checkChargeAmt(String usrId, int orgNo, ConsumeRecord consumeRecord) {
        int chargeAmt = consumeRecord.getTotalChargeAmt();
        int serviceAmt = consumeRecord.getServiceAmt();
        String epCode = consumeRecord.getEpCode();
        int gunNo = consumeRecord.getEpGunNo();
        String serialNo = consumeRecord.getSerialNo();
        long maxChargeAmt = GameConfig.maxChargeAmt;
        long maxChargeServiceAmt = GameConfig.maxChargeServiceAmt;
        long maxChargeCost = GameConfig.maxChargeCost;
        if (consumeRecord.getType() == 1) {
            maxChargeAmt = GameConfig.maxChargeAmt * 100;
            maxChargeServiceAmt = GameConfig.maxChargeServiceAmt * 100;
            maxChargeCost = GameConfig.maxChargeCost * 100;
        }

        if (chargeAmt < 0 || chargeAmt > maxChargeAmt) {
            logger.error(LogUtil.addBaseExtLog("chargeAmt|serialNo")
                    , new Object[]{LogConstants.FUNC_CONSUME_RECORD, epCode, gunNo, orgNo, usrId, chargeAmt, serialNo});
            return -1;
        }
        if (serviceAmt < 0 || serviceAmt > maxChargeServiceAmt) {
            logger.error(LogUtil.addBaseExtLog("serviceAmt|serialNo")
                    , new Object[]{LogConstants.FUNC_CONSUME_RECORD, epCode, gunNo, orgNo, usrId, serviceAmt, serialNo});
            return -2;
        }
        logger.info(LogUtil.addBaseExtLog("chargeAmt|serviceAmt|serialNo")
                , new Object[]{LogConstants.FUNC_CONSUME_RECORD, epCode, gunNo, orgNo, usrId, chargeAmt, serviceAmt, serialNo});

        int consumeAmt = chargeAmt + serviceAmt;
        if (consumeAmt < 0 || consumeAmt > maxChargeCost) {
            logger.error(LogUtil.addBaseExtLog("consumeAmt|serialNo")
                    , new Object[]{LogConstants.FUNC_CONSUME_RECORD, epCode, gunNo, orgNo, usrId, consumeAmt, serialNo});
            return -3;
        }

        consumeRecord.setTotalAmt(consumeAmt);    //计算总的消费金额

        return 0;
    }

    /**
     * 处理大账户消费记录（北汽出行、西安一卡通）
     *
     * @param consumeRecord
     * @param epGunCache
     * @return 4：无效的交易流水号
     * 3:已经处理
     * 2:数据不存在
     * 1:处理成功
     */
    public static int endBigConsumeRecord(int pkEpGunId, ConsumeRecord consumeRecord) {
        String epCode = consumeRecord.getEpCode();
        int epGunNo = consumeRecord.getEpGunNo();
        String cardInNo = consumeRecord.getEpUserAccount();

        int orgNo = consumeRecord.getUserOrgin();
        int user_id = UserService.getUserIdByOrgNo(orgNo);
        if (user_id <= 0) {
            logger.error(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "not find user info,cardInNo|orgNo"), cardInNo, orgNo);
            return 4;
        }
        //检查有没有卡，如果没有，插入一条
        ChargeCardCache cardCache = UserService.getCard(cardInNo);
        int pkCardId = 0;
        if (cardCache == null) {
            pkCardId = UserServiceImpl.insertBigCard(cardInNo, orgNo, user_id);
        } else {
            pkCardId = cardCache.getId();
        }

        UserCache cardUser = UserService.getUserCache(user_id);

        int orderStatus = ChargeServiceImpl.getChargeOrderStatus(consumeRecord.getSerialNo());
        logger.debug(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "serialNo|orderStatus"), consumeRecord.getSerialNo(), orderStatus);
        if (orderStatus == 2 || orderStatus == 3)//
            return 3;
        if (cardUser == null) {
            logger.error(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "not find user info,cardInNo"), cardInNo);
            return 4;
        }
        logger.debug(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "cardUser"), cardUser);

        RateInfo rateInfo = RateService.getRateInfo(epCode).getRateInfo();
        String chOrCode = EpChargeService.makeChargeOrderNo(pkEpGunId, cardUser.getId());

        int chorType = EpChargeService.getOrType(cardUser.getLevel());

        int chargeTime = (int) ((consumeRecord.getEndTime() - consumeRecord.getStartTime()) / 60);
        EpGunServiceImpl.addChargeStat(pkEpGunId, consumeRecord.getTotalDl(), chargeTime, consumeRecord.getTotalAmt());

        boolean exceptionData = false;
        if (checkChargeAmt(consumeRecord.getEpUserAccount(), consumeRecord.getUserOrgin(), consumeRecord) < 0)
            exceptionData = true;
        EpChargeService.insertChargeWithConsumeRecord(cardUser.getId(), chorType, cardUser.getAccount(), pkCardId, consumeRecord.getUserOrgin(), pkEpGunId,
                epCode, epGunNo, EpConstants.CHARGE_TYPE_CARD, "", chOrCode, 2, new BigDecimal(0.0), 0, 0, consumeRecord,
                rateInfo, rateInfo.getServiceRate(), exceptionData);

        cardUser.removeCharge(consumeRecord.getSerialNo());
        return 1;
    }

    /**
     * 处理大账户信用卡
     *
     * @param consumeRecord
     * @return 4：无效的交易流水号
     * 3:已经处理
     * 2:数据不存在
     * 1:处理成功
     */
    public static int endCreditConsumeRecord(int pkEpGunId, ConsumeRecord consumeRecord) {
        String epCode = consumeRecord.getEpCode();
        int epGunNo = consumeRecord.getEpGunNo();
        String cardInNo = consumeRecord.getEpUserAccount();
        ChargeCardCache cardCache = UserService.getCard(cardInNo);
        if (cardCache == null) {
            logger.info("endcharge endCreditConsumeRecord not find user info,cardInNo:{}", cardInNo);
            return 4;
        }
        UserCache cardUser = UserService.getUserCache(cardCache.getUserId(), cardCache.getAccountId());

        int orderStatus = ChargeServiceImpl.getChargeOrderStatus(consumeRecord.getSerialNo());
        logger.debug("endcharge endCreditConsumeRecord serialNo:{},orderStatus:{}", consumeRecord.getSerialNo(), orderStatus);
        if (orderStatus == 2 || orderStatus == 3)//
            return 3;
        if (cardUser == null) {
            logger.info("endcharge endCreditConsumeRecord not find user info,cardInNo:{}", cardInNo);
            return 4;
        }
        logger.debug("endcharge endCreditConsumeRecord cardUser:{}", cardUser);

        BigDecimal servicePrice = null;
        int discountType = 0;
        RateInfo rateInfo = RateService.getRateInfo(epCode).getRateInfo();
        if (EpChargeService.isVinCodeDiscount(epCode, epGunNo, 0, EpConstants.P_M_POSTPAID, consumeRecord)) {
            servicePrice = consumeRecord.getDiscountServicePrice();
            discountType = ChargeRecordConstants.CHARGEORDER_THIRDTYPE_VIN;
        } else {
            servicePrice = rateInfo.getServiceRate();
        }
        int pkVinCode = consumeRecord.getDiscountIdentity();

        logger.info("endcharge endCreditConsumeRecord epCode:{},epGunNo:{},discountType:{},vinCode:{},pkVinCode:{},chargeSerialNo:{},discountServicePrice:{},rateInfo.getServiceRate():{}",
                new Object[]{epCode, epGunNo, discountType, consumeRecord.getCarVinCode(), pkVinCode, consumeRecord.getSerialNo(), servicePrice, rateInfo.getServiceRate()});

        String chOrCode = EpChargeService.makeChargeOrderNo(pkEpGunId, cardUser.getId());

        int chorType = EpChargeService.getOrType(cardUser.getLevel());

        int chargeTime = (int) ((consumeRecord.getEndTime() - consumeRecord.getStartTime()) / 60);

        EpGunServiceImpl.addChargeStat(pkEpGunId, consumeRecord.getTotalDl(), chargeTime, consumeRecord.getTotalAmt());

        BigDecimal discountAmt = NumUtil.intToBigDecimal2(consumeRecord.getRealCouponAmt());
        if (consumeRecord.getType() == 1) discountAmt = NumUtil.intToBigDecimal4(consumeRecord.getRealCouponAmt());

        boolean exceptionData = false;
        if (checkChargeAmt(consumeRecord.getEpUserAccount(), consumeRecord.getUserOrgin(), consumeRecord) < 0)
            exceptionData = true;
        EpChargeService.insertChargeWithConsumeRecord(cardUser.getId(), chorType, cardUser.getAccount(), cardCache.getId(), consumeRecord.getUserOrgin(), pkEpGunId,
                epCode, epGunNo, EpConstants.CHARGE_TYPE_CARD, "", chOrCode, 2, discountAmt, pkVinCode, discountType, consumeRecord,
                rateInfo, servicePrice, exceptionData);

        return 1;

    }

}
