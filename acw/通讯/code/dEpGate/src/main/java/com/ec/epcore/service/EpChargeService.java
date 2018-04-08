package com.ec.epcore.service;

import com.ec.cache.*;
import com.ec.config.Global;
import com.ec.constants.*;
import com.ec.epcore.cache.*;
import com.ec.epcore.config.GameConfig;
import com.ec.epcore.net.client.EpCommClient;
import com.ec.epcore.net.codec.EpEncoder;
import com.ec.epcore.net.proto.ChargeCmdResp;
import com.ec.epcore.net.proto.ChargeEvent;
import com.ec.epcore.net.proto.ConsumeRecord;
import com.ec.epcore.sender.EpMessageSender;
import com.ec.epcore.test.ImitateEpService;
import com.ec.logs.LogConstants;
import com.ec.net.proto.Iec104Constant;
import com.ec.net.proto.WmIce104Util;
import com.ec.service.AbstractCacheService;
import com.ec.service.AbstractEpChargeService;
import com.ec.service.impl.ChargeServiceImpl;
import com.ec.service.impl.EpGunServiceImpl;
import com.ec.service.impl.UserServiceImpl;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import com.ec.utils.NumUtil;
import com.ec.utils.StringUtil;
import com.ormcore.model.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class EpChargeService extends AbstractEpChargeService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(EpChargeService.class.getName()));

    private static final String ChargeYearDateFmt = "yyyy-MM-dd HH:mm:ss";
    private static final String ChargeDayShortDateFmt = "MM-dd HH:mm";

    public static boolean isValidAddress(int address, int type) {
        //logger.debug("into isValidAddress address:{},type:{}",address,type);
        boolean ret = true;
        switch (type) {
            case 1: {
                if (address >= 13 && address <= 16)//保留
                {
                    ret = false;
                } else if (address > 21)//保留
                {
                    ret = false;
                }
            }
            break;
            case 3: {
                if (address >= 5 && address <= 8)//保留
                {
                    ret = false;
                } else if (address > 16)//保留
                {
                    ret = false;
                }
            }
            break;
            case 11: {
                if (address > 8 && address <= 16)//保留
                {
                    ret = false;
                } else if (address >= 37 && address <= 40)//保留
                {
                    ret = false;
                } else if (address >= 50 && address <= 128)//保留
                {
                    ret = false;
                }


            }
            break;
            case 132: {
                if (address >= 11)//保留
                {
                    ret = false;
                }
            }
            break;
            default:
                ret = false;
                break;
        }

        //logger.debug("isValidAddress ret:{}",ret);
        return ret;

    }

    public static void getUnFinishChargeByUserIdFromDb(UserCache u) {
        List<TblChargingrecord> chargeList = ChargeServiceImpl.getUnFinishedChargeByUsrId(u.getId());
        if (chargeList == null)
            return;
        for (TblChargingrecord tblChargeRecord : chargeList) {
            ChargeCache cache = convertFromDb(tblChargeRecord);
            u.addCharge(cache);
        }
    }

    public static int appApiStartElectric(String epCode, int epGunNo, String bespNo,
                                          int accountId, String account, short ermFlag,
                                          double fronzeAmt, int orgNo, int payMode, String clientIp) {
        logger.info(LogUtil.addBaseExtLog("account|ermFlag|fronzeAmt"),
                new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, accountId, account, ermFlag, fronzeAmt});

        BigDecimal bdFronzeAmt = new BigDecimal(fronzeAmt);
        bdFronzeAmt = bdFronzeAmt.multiply(Global.DecTime2);
        int nFronzeamt = NumUtil.BigDecimal2ToInt(bdFronzeAmt);

        byte[] cmdTimes = WmIce104Util.timeToByte();

        return EpChargeService.apiStartElectric(epCode, epGunNo, String.valueOf(accountId), null, ermFlag,
                nFronzeamt, payMode, orgNo, 1, clientIp, "", cmdTimes);
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
    public static int apiStartElectric(String epCode, int epGunNo, String account, ChargeCardCache card, short startChargeStyle,
               int frozenAmt, int payMode, int orgNo, int fromSource, String actionIdentity, String token, byte[] cmdTimes) {
        logger.debug(LogUtil.addBaseExtLog("startChargeStyle|payMode|fromSource"), new Object[]{LogConstants.FUNC_START_CHARGE,
                epCode, epGunNo, orgNo, account, startChargeStyle, payMode, fromSource});

        int error;
        if (card == null) {
            error = checkElectricDb(orgNo, epCode, epGunNo, account, null, startChargeStyle, frozenAmt, payMode, GameConfig.sms);
            if (error > 0) return error;
        } else {
            error = checkFrozenAmt(orgNo, Integer.valueOf(account), epCode, epGunNo, frozenAmt, payMode, GameConfig.sms);
            if (error > 0) return error;
        }

        //检查电桩
        ElectricPileCache epCache = (ElectricPileCache) CacheService.getEpCache(epCode);
        if (epCache == null) {
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        if (!epCache.checkOrgOperate(orgNo)) return ErrorCodeConstants.EP_NOT_OPERATE;

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        error = epGunCache.checkEpNetConnect();
        if (error > 0) return error;

        error = epGunCache.startChargeAction(card, account, startChargeStyle, frozenAmt,
                payMode, orgNo, fromSource, actionIdentity, token, cmdTimes);

        return error;
    }

    public static int appApiStopElectric(String epCode, int epGunNo, int orgNo, int userId, String account, int source, String apiClientIp) {
        if (orgNo != UserConstants.ORG_I_CHARGE) {
            userId = UserService.getUserIdByOrgNo(orgNo);
            if (userId == 0) {
                return ErrorCodeConstants.INVALID_ACCOUNT;
            }
        }

        return apiStopElectric(epCode, epGunNo, orgNo, userId, account, source, apiClientIp);
    }

    /**
     * @param epCode
     * @param epGunNo
     * @param orgNo
     * @param userId
     * @param account，当orgNo!=UserConstants.ORG_I_CHARGE时，account为第三方用户标识
     * @param source
     * @param apiClientIp
     * @return
     */
    public static int apiStopElectric(String epCode, int epGunNo, int orgNo, int userId, String account, int source, String apiClientIp) {
        //检查电桩
        BaseEPCache epCache = CacheService.getEpCache(epCode);
        if (epCache == null) {
            if (epCache == null) return ErrorCodeConstants.EP_UNCONNECTED;
        }

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache == null) {
            if (epGunCache == null) return ErrorCodeConstants.EP_UNCONNECTED;
        }
        int error = epGunCache.checkEpNetConnect();
        if (error > 0) return error;

        return epGunCache.stopChargeAction(orgNo, userId, account, source, apiClientIp);
    }

    public static void handEpStartChargeResp(EpCommClient epCommClient, ChargeCmdResp chargeCmdResp, byte[] cmdTimes) {
        logger.info(LogUtil.addExtLog("cmd|epCode|epGunNo|cmdTime|ChargeCmdResp")
                , new Object[]{LogConstants.FUNC_ONSTARTCHARGE, chargeCmdResp.getEpCode(), chargeCmdResp.getEpGunNo(), cmdTimes, chargeCmdResp});

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(chargeCmdResp.getEpCode(), chargeCmdResp.getEpGunNo());

        if (epGunCache != null) {
            epGunCache.onEpStartCharge(chargeCmdResp);
        }
    }

    /*
	 * 停止应答
	 */
    public static void handEpStopChargeResp(String epCode, int epGunNo, int ret, byte[] cmdTimes) {
        logger.debug("handEpStopChargeResp epCode:{},epGunNo:{},ret:{}",
                new Object[]{epCode, epGunNo, ret});
    }

    /**
     * @param chargeEvent
     * @return 4:参数错误
     */
    private static int checkRespChargeEventParams(ChargeEvent chargeEvent) {
        if (chargeEvent == null) {
            logger.error(LogUtil.getExtLog("chargeEvent is null"));
            return 4;
        }
        String epCode = chargeEvent.getEpCode();
        if (epCode == null || epCode.length() != 16) {
            logger.error(LogUtil.addExtLog("invalid epCode"), epCode);
            return 4;
        }
        int epGunNo = chargeEvent.getEpGunNo();

        if (chargeEvent.getSuccessFlag() != 0 && chargeEvent.getSuccessFlag() != 1) {
            logger.error(LogUtil.addExtLog("invalid successFlag"), chargeEvent.getSuccessFlag());
            return 4;
        }

        String serialNo = chargeEvent.getSerialNo();
        if (serialNo == null || serialNo.length() != 32) {
            logger.error(LogUtil.addExtLog("not find serialno"), serialNo);
            return 5;
        }
        String zeroSerialNo = StringUtil.repeat("0", 32);
        if (serialNo.compareTo(zeroSerialNo) == 0) {
            logger.error(LogUtil.addExtLog("invalid SerialNo"), serialNo);
            return 6;
        }

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache == null) {
            logger.error(LogUtil.addExtLog("not find epGunCache,epCode|epGunNo"), epCode, epGunNo);
            return 7;
        }
        return 0;

    }

    public static int handleStartElectricizeEventV3(EpCommClient epCommClient, ChargeEvent chargeEvent, byte[] cdmTimes) {
        logger.info(LogUtil.addExtLog("onEpStartChargeEvent,chargeEvent"), chargeEvent);

        String retEpCode = null;
        int retEpGunNo = 0;
        String serialNo = null;

        //1.检查充电时间参数
        int retCode = checkRespChargeEventParams(chargeEvent);
        if (retCode == 0) {
            retEpCode = chargeEvent.getEpCode();
            retEpGunNo = chargeEvent.getEpGunNo();
            serialNo = chargeEvent.getSerialNo();

            EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(retEpCode, retEpGunNo);

            retCode = epGunCache.handleStartChargeEvent(chargeEvent);
        } else {
            if (chargeEvent.getEpCode() != null && chargeEvent.getEpCode().length() == 16) {
                retEpCode = chargeEvent.getEpCode();
            } else {
                retEpCode = StringUtil.repeat("0", 16);
            }
            if (retCode == 5) {
                serialNo = StringUtil.repeat("0", 32);
            } else {
                serialNo = chargeEvent.getSerialNo();
            }

        }

        if (!ImitateEpService.IsStart()) {
            byte[] confirmdata = EpEncoder.do_charge_event_confirm(chargeEvent.getEpCode(), chargeEvent.getEpGunNo(), chargeEvent.getSerialNo(), retCode);

            if (confirmdata == null) {
                logger.error(LogUtil.getExtLog("do_charge_event_confirm exception"));
                return retCode;
            }

            EpMessageSender.sendMessage(epCommClient, 0, 0, (byte) Iec104Constant.C_CHARGE_EVENT_CONFIRM, confirmdata, cdmTimes, epCommClient.getVersion());

            logger.info(LogUtil.addExtLog("cmd|epCode|epGunNo|retCode|serialNo")
                    , new Object[]{LogConstants.FUNC_ONCHARGEEVENT, chargeEvent.getEpCode(), chargeEvent.getEpGunNo(), retCode, chargeEvent.getSerialNo()});
        }
        return retCode;
    }

    public static void updateBeginRecordToDb(int chargeUserId, int chorType,
            String chargeAccount, int pkUserCard, int userOrigin, int pkEpId, String epCode,
            int epGunNo, int chargingmethod, String chOrCode, int frozenAmt, long st,
            String chargeSerialNo, int startMeterNum, RateInfo rateInfo, int status, int thirdCode, int thirdType) {
        TblChargingrecord record = new TblChargingrecord();

        BigDecimal bdMeterNum = NumUtil.intToBigDecimal3(startMeterNum);
        // 开始充电表低示数
        String beginShowsNumber = String.valueOf(bdMeterNum);
        record.setChreBeginshowsnumber(beginShowsNumber);

        // 充电开始时间
        Date startDate = DateUtil.toDate(st * 1000);
        record.setChreStartdate(startDate);
        record.setChreEnddate(startDate);
        record.setChreUsingmachinecode(epCode);
        record.setChreChargingnumber(epGunNo);
        record.setChreResttime(0);
        record.setChreTransactionnumber(chargeSerialNo);
        record.setUserId(chargeUserId);
        record.setUserPhone(chargeAccount);
        record.setChreElectricpileid(pkEpId);

        record.setChreChargingmethod(chargingmethod);

        record.setChreEndshowsnumber("0");
        record.setChreCode(chOrCode);
        record.setStatus(status);
        record.setJPrice(rateInfo.getJ_Rate());
        record.setFPrice(rateInfo.getF_Rate());
        record.setPPrice(rateInfo.getP_Rate());
        record.setGPrice(rateInfo.getG_Rate());
        record.setQuantumDate(rateInfo.getQuantumDate());
        record.setPkUserCard(pkUserCard);
        record.setUserOrigin(userOrigin);
        BigDecimal bdFrozenAmt = NumUtil.intToBigDecimal2(frozenAmt);

        record.setFrozenAmt(bdFrozenAmt);

        record.setThirdCode(thirdCode);
        record.setThirdType(thirdType);

        // 新增开始充电记录
        ChargeServiceImpl.updateBeginRecord(record);
    }

    public static void insertChargeRecordToDb(int chargeUserId, int chorType, String chargeAccount,
            int pkUserCard, int userOrigin, int pkEpId, String epCode, int epGunNo, int chargingmethod,
            String chOrCode, int frozenAmt, int payMode, int userOrgNo, ChargeEvent chargeEvent,
            RateInfo rateInfo, int status, String thirdUsrIdentity, String token, String usrGateIp) {
        TblChargingrecord record = new TblChargingrecord();

        BigDecimal bdMeterNum = NumUtil.intToBigDecimal3(chargeEvent.getStartMeterNum());
        // 开始充电表低示数
        String beginShowsNumber = String.valueOf(bdMeterNum);
        record.setChreBeginshowsnumber(beginShowsNumber);

        // 充电开始时间
        Date startDate = DateUtil.toDate(chargeEvent.getStartChargeTime() * 1000);
        record.setChreStartdate(startDate);
        record.setChreEnddate(startDate);
        record.setChreUsingmachinecode(chargeEvent.getEpCode());
        record.setChreChargingnumber(chargeEvent.getEpGunNo());
        record.setChreReservationnumber(chargeEvent.getBespokeNo());
        record.setChreResttime(chargeEvent.getRemainTime());
        record.setChreTransactionnumber(chargeEvent.getSerialNo());
        record.setUserId(chargeUserId);
        record.setUserPhone(chargeAccount);
        record.setChreElectricpileid(pkEpId);

        record.setChreChargingmethod(chargingmethod);

        record.setChreEndshowsnumber("0");
        record.setChreCode(chOrCode);


        record.setStatus(status);
        record.setJPrice(rateInfo.getJ_Rate());
        record.setFPrice(rateInfo.getF_Rate());
        record.setPPrice(rateInfo.getP_Rate());
        record.setGPrice(rateInfo.getG_Rate());
        record.setServicePrice(rateInfo.getServiceRate());
        record.setQuantumDate(rateInfo.getQuantumDate());
        record.setPkUserCard(pkUserCard);
        record.setUserOrigin(0);

        BigDecimal bdFrozenAmt = NumUtil.intToBigDecimal2(frozenAmt);
        record.setFrozenAmt(bdFrozenAmt);
        record.setUserOrgNo(userOrgNo);
        record.setPayMode(payMode);
        record.setThirdUsrIdentity(thirdUsrIdentity);
        record.setThirdExtraData(token);
        record.setUsrGateIp(usrGateIp);
        // 新增开始充电记录
        ChargeServiceImpl.insertBeginRecord(record);
    }

    public static int insertChargeOrderToDb(int chargeUserId, int chorType,
            int pkUserCard, int userOrigin, int pkEpId, String epCode,
            int epGunNo, int chargingmethod, String chOrCode, int frozenAmt,
            int payMode, int userOrgNo, long st, String chargeSerialNo
            , RateInfo rateInfo, String thirdUsrIdentity, String token) {
        //订单编号，赞借用流水号
        Date date = new Date();

        Date dtStart = new Date(st * 1000);
        // 计算总电量
        String beginTime = DateUtil.toDateFormat(dtStart, "MM-dd HH:mm");

        BigDecimal bdZero = new BigDecimal(0.0);

        BigDecimal chargeAmt = new BigDecimal(0.0);
        BigDecimal serviceAmt = new BigDecimal(0.0);
        // 充电消费订单
        TblChargingOrder order = new TblChargingOrder();
        order.setChorCode(chOrCode);

        order.setChorPilenumber(epCode);
        order.setChorUserid("" + chargeUserId);

        order.setChorType(chorType);
        //order.setChorType(0);

        order.setChorMoeny(chargeAmt.add(serviceAmt) + "");
        order.setChOr_tipPower(bdZero);
        order.setChOr_peakPower(bdZero);
        order.setChOr_usualPower(bdZero);
        order.setChOr_valleyPower(bdZero);
        order.setChorQuantityelectricity(bdZero);
        order.setChorTimequantum(beginTime + " - ");
        order.setChorMuzzle(epGunNo);
        order.setChorChargingstatus(ChargeOrderConstants.ORDER_STATUS_WAIT + "");
        order.setChorTranstype("1");

        order.setChorCreatedate(new Date());
        order.setChorUpdatedate(new Date());
        //order.setChorUsername(userInfo.getUsinFacticityname() == null ? "":userInfo.getUsinFacticityname());
        order.setChorUsername("");
        order.setChorTransactionnumber(chargeSerialNo);
        order.setChorChargemoney(chargeAmt);
        order.setChorServicemoney(serviceAmt);
        order.setChorOrdertype(0);
        beginTime = DateUtil.toDateFormat(dtStart, ChargeYearDateFmt);

        order.setChargeBegintime(beginTime);
        order.setChargeEndtime(beginTime);
        order.setPkUserCard(pkUserCard);
        order.setUserOrigin(0);
        order.setUserOrgNo(userOrgNo);
        //order.setPayMode(payMode);
        order.setThirdUsrIdentity(thirdUsrIdentity);
        order.setThirdExtraData(token);
        // 新增充电消费订单
        int pkOrderId = ChargeServiceImpl.getOrderId(chargeSerialNo);
        if (pkOrderId == 0) return ChargeServiceImpl.insertChargeOrder(order);
        return pkOrderId;
    }

    public static String getMeterNum(int nMeterNum) {
        BigDecimal bdMeterNum = NumUtil.intToBigDecimal3(nMeterNum);

        String strMeterNum = String.valueOf(bdMeterNum);
        return strMeterNum;
    }

    public static void insertFullChargeOrder(int chargeUserId, int chorType,
             String chargeAccount, int pkUserCard, int userOrigin, int pkEpId,
             String epCode, int epGunNo, int chargingmethod, String bespokeNo,
             String chOrCode, int payMode, BigDecimal couPonAmt, int pkCouPon,
             int thirdType, ConsumeRecord consumeRecord, RateInfo rateInfo, int orderStatus) {
        // 尖时段用电度数
        BigDecimal tipPower = NumUtil.intToBigDecimal3(consumeRecord.getjDl());
        // 峰时段用电度数
        BigDecimal peakPower = NumUtil.intToBigDecimal3(consumeRecord.getfDl());
        // 平时段用电度数
        BigDecimal usualPower = NumUtil.intToBigDecimal3(consumeRecord.getpDl());
        // 谷时段用电度数
        BigDecimal valleyPower = NumUtil.intToBigDecimal3(consumeRecord.getgDl());
        // 时间段

        BigDecimal totalPower = NumUtil.intToBigDecimal3(consumeRecord.getTotalDl());

        Date dtStart = new Date(consumeRecord.getStartTime() * 1000);
        Date dtEnd = new Date(consumeRecord.getEndTime() * 1000);
        // 计算总电量
        String beginTime = DateUtil.toDateFormat(dtStart, "MM-dd HH:mm");
        String endTime = DateUtil.toDateFormat(dtEnd, "MM-dd HH:mm");

        BigDecimal chargeAmt = new BigDecimal(0);
        BigDecimal serviceAmt = new BigDecimal(0);
        if (consumeRecord.getType() == 0) {
            chargeAmt = NumUtil.intToBigDecimal2(consumeRecord.getTotalChargeAmt());
            serviceAmt = NumUtil.intToBigDecimal2(consumeRecord.getServiceAmt());
        } else {
            chargeAmt = NumUtil.intToBigDecimal4(consumeRecord.getTotalChargeAmt());
            serviceAmt = NumUtil.intToBigDecimal4(consumeRecord.getServiceAmt());
        }
        String thirdUsrIdentity = "";
        if (userOrigin == UserConstants.ORG_BQ || consumeRecord.getUserOrgin() == UserConstants.ORG_XIAN) {
            thirdUsrIdentity = consumeRecord.getEpUserAccount();
        }
        // 充电消费订单
        TblChargingOrder order = new TblChargingOrder();
        order.setChorCode(chOrCode);

        order.setChorAppointmencode(bespokeNo);

        order.setChorPilenumber(epCode);
        order.setChorUserid("" + chargeUserId);

        order.setChorType(chorType);

        order.setChorMoeny(chargeAmt.add(serviceAmt) + "");
        order.setChOr_tipPower(tipPower);
        order.setChOr_peakPower(peakPower);
        order.setChOr_usualPower(usualPower);
        order.setChOr_valleyPower(valleyPower);
        order.setChorQuantityelectricity(totalPower);
        order.setChorTimequantum(beginTime + " - " + endTime);
        order.setChorMuzzle(epGunNo);
        order.setChorChargingstatus("" + orderStatus);
        order.setChorTranstype("" + consumeRecord.getTransType());

        order.setChorCreatedate(new Date());
        order.setChorUpdatedate(new Date());
        order.setChorUsername("");
        order.setChorTransactionnumber(consumeRecord.getSerialNo());
        order.setChorChargemoney(chargeAmt);
        order.setChorServicemoney(serviceAmt);
        order.setChorOrdertype(0);
        beginTime = DateUtil.toDateFormat(dtStart, ChargeYearDateFmt);
        endTime = DateUtil.toDateFormat(dtEnd, ChargeYearDateFmt);

        order.setChargeBegintime(beginTime);
        order.setChargeEndtime(endTime);
        order.setPkUserCard(pkUserCard);
        order.setUserOrgNo(userOrigin);
        order.setUserOrigin(userOrigin);
        order.setThirdUsrIdentity(thirdUsrIdentity);
        order.setThirdExtraData("");
        order.setPkCoupon(pkCouPon);
        order.setCouPonAmt(couPonAmt);
        order.setThirdType(thirdType);
        if (consumeRecord.getType() == 1) {
            order.setStartSoc(consumeRecord.getStartSoc());
            order.setEndSoc(consumeRecord.getEndSoc());
        }
        // 新增充电消费订单
        ChargeServiceImpl.insertFullChargeOrder(order);
    }

    public static void insertFullChargeRecord(int chargeUserId, int chorType,
            String chargeAccount, int pkUserCard, int userOrigin, int pkEpId, String epCode, int epGunNo, int chargingmethod,
            String bespokeNo, String chOrCode, int payMode, ConsumeRecord consumeRecord, RateInfo rateInfo) {
        Date dtStart = new Date(consumeRecord.getStartTime() * 1000);
        Date dtEnd = new Date(consumeRecord.getEndTime() * 1000);
        String thirdUsrIdentity = "";
        if (userOrigin == UserConstants.ORG_BQ || consumeRecord.getUserOrgin() == UserConstants.ORG_XIAN) {
            thirdUsrIdentity = consumeRecord.getEpUserAccount();
        }

        TblChargingrecord record = new TblChargingrecord();
        // 开始充电表低示数
        String beginShowsNumber = getMeterNum(consumeRecord.getStartMeterNum());
        String endShowsnumber = getMeterNum(consumeRecord.getEndMeterNum());

        // 充电开始时间

        record.setUserId(chargeUserId);
        record.setUserPhone(chargeAccount);
        record.setChreElectricpileid(pkEpId);
        record.setChreUsingmachinecode(epCode);

        record.setChreTransactionnumber(consumeRecord.getSerialNo());

        record.setChreStartdate(dtStart);
        record.setChreChargingnumber(epGunNo);
        record.setChreChargingmethod(chargingmethod);
        record.setChreReservationnumber("");

        record.setChreBeginshowsnumber(beginShowsNumber);
        record.setChreEndshowsnumber(endShowsnumber);

        record.setChreCode(chOrCode);

        record.setChreEnddate(dtEnd);
        record.setChreResttime(0);
        record.setStatus(1);
        record.setJPrice(rateInfo.getJ_Rate());
        record.setFPrice(rateInfo.getF_Rate());
        record.setPPrice(rateInfo.getP_Rate());
        record.setGPrice(rateInfo.getG_Rate());
        record.setQuantumDate(rateInfo.getQuantumDate());
        record.setPkUserCard(pkUserCard);
        record.setUserOrigin(userOrigin);
        record.setUserOrgNo(userOrigin);
        record.setPayMode(payMode);
        record.setServicePrice(rateInfo.getServiceRate());
        record.setThirdUsrIdentity(thirdUsrIdentity);
        record.setThirdExtraData("");
        // 新增开始充电记录
        ChargeServiceImpl.insertFullChargeRecord(record);

    }

    public static void insertChargeWithConsumeRecord(int chargeUserId,
             int chorType, String chargeAccount, int pkUserCard, int userOrigin,
             int pkEpId, String epCode, int epGunNo, int chargingmethod,
             String bespokeNo, String chOrCode, int payMode,
             BigDecimal couPonAmt, int pkCouPon, int thirdType,
             ConsumeRecord consumeRecord, RateInfo rateInfo,
             BigDecimal servicePrice, boolean exceptionData) {
        // 尖时段用电度数
        BigDecimal tipPower = NumUtil.intToBigDecimal3(consumeRecord.getjDl());
        // 峰时段用电度数
        BigDecimal peakPower = NumUtil.intToBigDecimal3(consumeRecord.getfDl());
        // 平时段用电度数
        BigDecimal usualPower = NumUtil.intToBigDecimal3(consumeRecord.getpDl());
        // 谷时段用电度数
        BigDecimal valleyPower = NumUtil.intToBigDecimal3(consumeRecord.getgDl());
        // 时间段
        BigDecimal totalPower = NumUtil.intToBigDecimal3(consumeRecord.getTotalDl());

        Date dtStart = new Date(consumeRecord.getStartTime() * 1000);
        Date dtEnd = new Date(consumeRecord.getEndTime() * 1000);
        // 计算总电量
        String beginTime = DateUtil.toDateFormat(dtStart, "MM-dd HH:mm");
        String endTime = DateUtil.toDateFormat(dtEnd, "MM-dd HH:mm");

        BigDecimal chargeAmt = new BigDecimal(0);
        BigDecimal serviceAmt = new BigDecimal(0);
        if (consumeRecord.getType() == 0) {
            chargeAmt = NumUtil.intToBigDecimal2(consumeRecord.getTotalChargeAmt());
            serviceAmt = NumUtil.intToBigDecimal2(consumeRecord.getServiceAmt());
        } else {
            chargeAmt = NumUtil.intToBigDecimal4(consumeRecord.getTotalChargeAmt());
            serviceAmt = NumUtil.intToBigDecimal4(consumeRecord.getServiceAmt());
        }
        String thirdUsrIdentity = "";
        if (userOrigin == UserConstants.ORG_BQ || consumeRecord.getUserOrgin() == UserConstants.ORG_XIAN) {
            thirdUsrIdentity = consumeRecord.getEpUserAccount();
        }
        // 充电消费订单
        TblChargingOrder order = new TblChargingOrder();
        order.setChorCode(chOrCode);

        order.setChorAppointmencode(bespokeNo);

        order.setChorPilenumber(epCode);
        order.setChorUserid("" + chargeUserId);

        order.setChorType(chorType);

        order.setChOr_tipPower(tipPower);
        order.setChOr_peakPower(peakPower);
        order.setChOr_usualPower(usualPower);
        order.setChOr_valleyPower(valleyPower);
        order.setChorQuantityelectricity(totalPower);
        order.setChorTimequantum(beginTime + " - " + endTime);
        order.setChorMuzzle(epGunNo);
        if (!exceptionData) {
            order.setChorMoeny(chargeAmt.add(serviceAmt) + "");
            order.setChorChargingstatus(ChargeOrderConstants.ORDER_STATUS_DONE + "");
        } else {
            order.setChorMoeny("0");
            order.setChorChargingstatus(ChargeOrderConstants.ORDER_STATUS_EXCEPTION + "");
        }
        order.setChorTranstype("" + consumeRecord.getTransType());

        order.setChorCreatedate(new Date());
        order.setChorUpdatedate(new Date());
        order.setChorUsername("");
        order.setChorTransactionnumber(consumeRecord.getSerialNo());
        order.setChorChargemoney(chargeAmt);
        order.setChorServicemoney(serviceAmt);
        order.setChorOrdertype(0);
        beginTime = DateUtil.toDateFormat(dtStart, ChargeYearDateFmt);
        endTime = DateUtil.toDateFormat(dtEnd, ChargeYearDateFmt);

        order.setChargeBegintime(beginTime);
        order.setChargeEndtime(endTime);
        order.setPkUserCard(pkUserCard);
        order.setUserOrgNo(userOrigin);
        order.setUserOrigin(userOrigin);
        order.setThirdUsrIdentity(thirdUsrIdentity);
        order.setThirdExtraData("");
        order.setPkCoupon(pkCouPon);
        order.setCouPonAmt(couPonAmt);
        order.setThirdType(thirdType);
        // order.setThirdExtraData(thirdExtraData);
        if (consumeRecord.getType() == 1) {
            order.setStartSoc(consumeRecord.getStartSoc());
            order.setEndSoc(consumeRecord.getEndSoc());
        }
        // 新增充电消费订单
        ChargeServiceImpl.insertFullChargeOrder(order);

        TblChargingrecord record = new TblChargingrecord();
        // 开始充电表低示数
        String beginShowsNumber = getMeterNum(consumeRecord.getStartMeterNum());
        String endShowsnumber = getMeterNum(consumeRecord.getEndMeterNum());

        // 充电开始时间
        record.setUserId(chargeUserId);
        record.setUserPhone(chargeAccount);
        record.setChreElectricpileid(pkEpId);
        record.setChreUsingmachinecode(epCode);

        record.setChreTransactionnumber(consumeRecord.getSerialNo());

        record.setChreStartdate(dtStart);
        record.setChreChargingnumber(epGunNo);
        record.setChreChargingmethod(chargingmethod);
        record.setChreReservationnumber("");

        record.setChreBeginshowsnumber(beginShowsNumber);
        record.setChreEndshowsnumber(endShowsnumber);

        record.setChreCode(chOrCode);

        record.setChreEnddate(dtEnd);
        record.setChreResttime(0);
        record.setStatus(1);
        record.setJPrice(rateInfo.getJ_Rate());
        record.setFPrice(rateInfo.getF_Rate());
        record.setPPrice(rateInfo.getP_Rate());
        record.setGPrice(rateInfo.getG_Rate());
        record.setQuantumDate(rateInfo.getQuantumDate());
        record.setPkUserCard(pkUserCard);
        record.setUserOrigin(userOrigin);
        record.setUserOrgNo(userOrigin);
        record.setPayMode(payMode);
        record.setServicePrice(servicePrice);
        record.setThirdUsrIdentity(thirdUsrIdentity);
        record.setThirdExtraData("");
        record.setThirdCode(pkCouPon);
        record.setThirdType(thirdType);
        // 新增开始充电记录
        ChargeServiceImpl.insertFullChargeRecord(record);
    }

    public static void updateChargeToDb(EpGunCache epGunCache, ChargeCache chargeCache, ConsumeRecord consumeRecord,
                                        boolean exceptionData, BigDecimal discountAmt, int pkCouPon, int thirdType, BigDecimal servicePrice) {
        String epCode = epGunCache.getEpCode();
        int epGunNo = epGunCache.getEpGunNo();
        int pkEpId = epGunCache.getPkEpGunId();

        int payMode = chargeCache.getPayMode();
        String chargeUserAccount = chargeCache.getAccount();
        int userId = chargeCache.getUserId();
        RateInfo rateInfo = chargeCache.getRateInfo();
        String chOrCode = chargeCache.getChOrCode();


        // 尖时段用电度数
        BigDecimal tipPower = NumUtil.intToBigDecimal3(consumeRecord.getjDl());
        // 峰时段用电度数
        BigDecimal peakPower = NumUtil.intToBigDecimal3(consumeRecord.getfDl());
        // 平时段用电度数
        BigDecimal usualPower = NumUtil.intToBigDecimal3(consumeRecord.getpDl());
        // 谷时段用电度数
        BigDecimal valleyPower = NumUtil.intToBigDecimal3(consumeRecord.getgDl());
        // 时间段

        BigDecimal totalPower = NumUtil.intToBigDecimal3(consumeRecord.getTotalDl());


        Date dtStart = new Date(consumeRecord.getStartTime() * 1000);
        Date dtEnd = new Date(consumeRecord.getEndTime() * 1000);
        // 计算总电量
        String beginTime = DateUtil.toDateFormat(dtStart, "MM-dd HH:mm");
        String endTime = DateUtil.toDateFormat(dtEnd, "MM-dd HH:mm");

        BigDecimal chargeAmt = new BigDecimal(0);
        BigDecimal serviceAmt = new BigDecimal(0);
        BigDecimal chargeCost = new BigDecimal(0);
        if (consumeRecord.getType() == 0) {
            chargeAmt = NumUtil.intToBigDecimal2(consumeRecord.getTotalChargeAmt());
            serviceAmt = NumUtil.intToBigDecimal2(consumeRecord.getServiceAmt());
            chargeCost = NumUtil.intToBigDecimal2(consumeRecord.getUndiscountTotalAmt());
        } else {
            chargeAmt = NumUtil.intToBigDecimal4(consumeRecord.getTotalChargeAmt());
            serviceAmt = NumUtil.intToBigDecimal4(consumeRecord.getServiceAmt());
            chargeCost = NumUtil.intToBigDecimal4(consumeRecord.getUndiscountTotalAmt());
        }
        // 充电消费订单
        TblChargingOrder order = new TblChargingOrder();
        order.setChorCode(chOrCode);

        order.setChorAppointmencode("000000000000");

        order.setChorPilenumber(epCode);
        order.setChorUserid("" + userId);

        if (AbstractCacheService.getUserCache(userId) != null)
            order.setChorType(getOrType(AbstractCacheService.getUserCache(userId).getLevel()));
        order.setThirdUsrIdentity("");
        order.setThirdExtraData("");

        order.setChorMoeny(chargeCost + "");
        logger.debug("updateChargeToDb,consumeRecord.getTotalChargeAmt():{},chargeAmt:{}",
                consumeRecord.getTotalChargeAmt(), chargeAmt);
        order.setChorChargemoney(chargeAmt);
        order.setChorServicemoney(serviceAmt);

        order.setChOr_tipPower(tipPower);
        order.setChOr_peakPower(peakPower);
        order.setChOr_usualPower(usualPower);
        order.setChOr_valleyPower(valleyPower);
        order.setChorQuantityelectricity(totalPower);
        order.setChorTimequantum(beginTime + " - " + endTime);
        order.setChorTransactionnumber(consumeRecord.getSerialNo());
        order.setChorMuzzle(epGunNo);
        order.setPkCoupon(pkCouPon);
        order.setCouPonAmt(discountAmt);
        order.setThirdType(thirdType);

        if (!exceptionData) {

            if (payMode == EpConstants.P_M_FIRST) {
                order.setChorChargingstatus(ChargeOrderConstants.ORDER_STATUS_SUCCESS + "");
            } else {
                order.setChorChargingstatus(ChargeOrderConstants.ORDER_STATUS_DONE + "");
            }
        } else {
            order.setChorChargingstatus(ChargeOrderConstants.ORDER_STATUS_EXCEPTION + "");
        }

        order.setChorTranstype(consumeRecord.getTransType() + "");
        String bespokeNo = StringUtil.repeat("0", 12);
        order.setChorAppointmencode(bespokeNo);

        order.setChorUpdatedate(new Date());

        order.setChorUsername("");
        order.setChorTransactionnumber(consumeRecord.getSerialNo());

        beginTime = DateUtil.toDateFormat(dtStart, ChargeYearDateFmt);
        endTime = DateUtil.toDateFormat(dtEnd, ChargeYearDateFmt);
        order.setChargeBegintime(beginTime);
        order.setChargeEndtime(endTime);
        if (consumeRecord.getType() == 1) {
            order.setStartSoc(consumeRecord.getStartSoc());
            order.setEndSoc(consumeRecord.getEndSoc());
        }
        // 新增充电消费订单
        int pkOrderId = ChargeServiceImpl.getOrderId(consumeRecord.getSerialNo());
        if (pkOrderId > 0) {
            ChargeServiceImpl.updateChargeOrder(order);
        } else {
            ChargeServiceImpl.insertFullChargeOrder(order);
        }

        // 根据交易流水号更新充电订单编号
        TblChargingrecord record = new TblChargingrecord();

        String beginShowsNumber = getMeterNum(consumeRecord.getStartMeterNum());
        String endShowsnumber = getMeterNum(consumeRecord.getEndMeterNum());

        // 开始充电表低示数

        // 充电开始时间
        record.setUserId(userId);
        record.setUserPhone(chargeUserAccount);

        record.setChreElectricpileid(pkEpId);
        record.setChreUsingmachinecode(epCode);

        record.setChreTransactionnumber(consumeRecord.getSerialNo());
        record.setChreBeginshowsnumber(beginShowsNumber);
        record.setChreEndshowsnumber(endShowsnumber);
        record.setChreStartdate(dtStart);
        record.setChreEnddate(dtEnd);
        record.setChreChargingnumber(epGunNo);
        record.setChreChargingmethod(1);
        record.setChreReservationnumber(bespokeNo);

        record.setChreResttime(0);

        record.setJPrice(rateInfo.getJ_Rate());
        record.setFPrice(rateInfo.getF_Rate());
        record.setPPrice(rateInfo.getP_Rate());
        record.setGPrice(rateInfo.getG_Rate());
        record.setQuantumDate(rateInfo.getQuantumDate());
        record.setChreCode(order.getChorCode());
        record.setServicePrice(servicePrice);

        record.setStatus(1);
        record.setThirdCode(pkCouPon);
        record.setThirdType(thirdType);

        // 新增开始充电记录
        ChargeServiceImpl.insertEndRecord(record);
    }

    public static String getFaultDesc(int nStopRet) {
        String faultCause = "";
        switch (nStopRet) {
            case 2:
                faultCause = "用户强制结束";
                break;
            case 3:
                faultCause = "急停";
                break;
            case 4:
                faultCause = "连接线断掉";
                break;
            case 5:
                faultCause = "电表异常";
                break;
            case 6:
                faultCause = "过流停止";
                break;
            case 7:
                faultCause = "过压停止";
                break;
            case 8:
                faultCause = "防雷器故障";
                break;
            case 9:
                faultCause = "接触器故障";
                break;
            case 10:
                faultCause = "充电金额不足";
                break;
            case 11:
                faultCause = "漏电保护器";
                break;
            case 13:
                faultCause = "BMS通信异常故障";
                break;
            case 14:
                faultCause = "违规拔枪";
                break;
            case 15:
                faultCause = "电桩断电";
                break;
            default:
                faultCause = "电桩保护,nStopRet:" + nStopRet;
                break;
        }
        return faultCause;
    }

    public static String getStopChargeDesc(int nCause) {
        String faultCause = "";
        switch (nCause) {
            case 3:
                faultCause = "急停";
                break;
            case 4:
                faultCause = "连接线断掉";
                break;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            case 13:
                faultCause = "电桩保护(原因" + nCause + ")";
                break;
            case 10:
                faultCause = "充电金额不足";
                break;
            case 12:
                faultCause = "自动充满";
                break;
            case 14:
                faultCause = "违规拔枪";
                break;
            case 15:
                faultCause = "电桩断电";
                break;
            case 16:
            case 17:
            case 18:
                faultCause = "电桩保护(原因" + nCause + ")";
                break;
            case 19:
                faultCause = "车BMS主动停止";
                break;
            default:
                faultCause = "电桩保护(原因" + nCause + ")";
                break;
        }
        return faultCause;
    }

    public static int handleStopElectricizeEvent(EpCommClient epCommClient, String epCode, int epGunNo, String serialNo, long et,
            short nStopRet, int nDbValue, byte ChargeStye, byte offstates, byte successflag, byte[] time, byte[] cmdTimes) throws IOException {
        short isException = 0;
        int exceptionCode = 0;

        //1.
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache == null) {
            logger.error("stopcharge Event error epGunCache==null,epCode:{},epGunNo:{}", epCode, epGunNo);
            return 1;
        }
        short nTimeOut = 0;
        ChargeCache chargeCache = epGunCache.getChargeCache();
        //在线二维码停止充电超时

        if (chargeCache == null) {
            logger.error("stopcharge event error not find ElectricCache,epCode:{},epGunNo:{}",
                    epCode, epGunNo);
            return 2;
        }
        if (chargeCache.getStatus() == ChargeRecordConstants.CS_CHARGE_END) {
            logger.error("stopcharge event error had been stop,epCode:{},epGunNo:{}",
                    epCode, epGunNo);
            return 3;
        }

        //桩停止充电不成功
        if (successflag != 1) {
            logger.error("stopcharge fail,epCode:{},epGunNo:{},successflag:{}",
                    new Object[]{epCode, epGunNo, successflag});
            return 3;
        }
        chargeCache.setEt(et);

        if (nStopRet > 12) {
            nStopRet = 1;
        }
        if (nStopRet == 1 || nStopRet == 2 || nStopRet == 3 || nStopRet == 10) {
            isException = 1;
        } else {
            exceptionCode = nStopRet;
        }
        short nnStopRet = nStopRet;
        if (nStopRet > 11)// 充电桩正常退出
        {
            nnStopRet = nStopRet;
        }
        chargeCache.setStopCause(nStopRet);

        ChargeServiceImpl.endChargeRecord(chargeCache.getChargeSerialNo(), ChargeRecordConstants.CS_CHARGE_END,
                DateUtil.toDate(et * 1000), new BigDecimal(0), chargeCache.getRateInfo().getServiceRate());

        chargeCache.setStatus(ChargeRecordConstants.CS_CHARGE_END);

        return 0;
    }

    private static int convertChargeCodeToAuthCode(int code) {
        switch (code) {
            case 1002: {
                return 1;
            }

            case ErrorCodeConstants.EP_UNCONNECTED:
            case ErrorCodeConstants.INVALID_EP_CODE:
            case ErrorCodeConstants.INVALID_EP_GUN_NO:
            case ErrorCodeConstants.INVALID_RATE: {
                return 2;
            }

            case 6800:
            case ErrorCodeConstants.CANNOT_OTHER_OPER: {
                return 4;
            }

            case ErrorCodeConstants.INVALID_ACCOUNT: {
                return 3;
            }

            default:
                break;
        }
        return code;

    }

    public static int getCardPayMode(int cardPayMode) {
        if (cardPayMode == EpConstants.CARD_CO_CREDIT) {
            return 2;
        }
        return 1;
    }

    /**
     * @param epCode
     * @param epGunNo
     * @param innerCardNo
     * @param fronzeAmt
     * @param cmdTimes
     * @return 1：金额不足
     * 2：没找到桩
     * 3：无效卡
     * 4：在其他桩上使用，不能充电
     */
    private static int fronzeCardAmt(String epCode, int epGunNo, String innerCardNo, int fronzeAmt, byte[] cmdTimes) {
        try {
            BaseEPCache epCache = CacheService.getEpCache(epCode);
            if (epCache == null) {
                return 2;
            }
            int errorCode = 0;

            EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
            if (epGunCache == null)
                return 2;

            ChargeCardCache cardCache = UserService.getCard(innerCardNo);
            if (cardCache == null)
                return 3;

            int payMode = getCardPayMode(cardCache.getCardType());

            errorCode = epCache.canCharge(cardCache.getId(), payMode, cardCache.getCompanyNumber(), epGunNo);
            if (errorCode > 0) {
                logger.error(LogUtil.addBaseExtLog("errorCode"),
                        new Object[]{LogConstants.FUNC_CARD_FRONZE_AMT, epCode, epGunNo, cardCache.getCompanyNumber(), innerCardNo, errorCode});
                return 3;
            }

            int usrId = cardCache.getUserId();
            UserCache userCache = AbstractCacheService.getUserCache(cardCache.getUserId());
            if (userCache == null)
                return 3;

            AuthUserCache authCache = epGunCache.getAuth();
            long now = DateUtil.getCurrentSeconds();

            if (authCache != null) {
                long authTime = authCache.getLastTime();
                int curUsrId = epGunCache.getCurUserId();
                if (!cardCache.isAllowMutliCharge() &&
                        (now - authTime) < GameConfig.userAuthTimeOut &&
                        curUsrId > 0 && curUsrId != userCache.getId()) {
                    errorCode = 4;
                } else {
                    authCache.setLastTime(now);
                }
            } else {
                authCache = new AuthUserCache(userCache.getId(), userCache.getAccount(), now, (short) 3);
                epGunCache.setAuth(authCache);
            }

            if (errorCode == 0) {
                logger.info(LogUtil.addBaseExtLog("charge card fronze amt success errorCode|innerCardNo|cardPayMode"),
                        new Object[]{LogConstants.FUNC_CARD_FRONZE_AMT, epCode, epGunNo, cardCache.getCompanyNumber(), usrId, errorCode, innerCardNo, payMode});

                errorCode = apiStartElectric(epCode, epGunNo, String.valueOf(usrId), cardCache, EpConstants.CHARGE_TYPE_CARD,
                        fronzeAmt, payMode, cardCache.getCompanyNumber(), 2, userCache.getAccount(), "", cmdTimes);

                return convertChargeCodeToAuthCode(errorCode);
            }
            return errorCode;
        } catch (Exception e) {
            logger.error(LogUtil.addExtLog("exception"), e.getStackTrace());
            return 3;
        }
    }

    public static void handleCardFronzeAmt(EpCommClient epCommClient, String epCode, int epGunNo, String innerCardNo, int fronzeAmt, byte[] cmdTimes) throws IOException {
        int errorCode = fronzeCardAmt(epCode, epGunNo, innerCardNo, fronzeAmt, cmdTimes);
        if (errorCode > 0) {
            logger.error(LogUtil.addFuncExtLog(LogConstants.FUNC_CARD_FRONZE_AMT, "epCode|epGunNo|inCardNo|errorCode"),
                    new Object[]{epCode, epGunNo, innerCardNo, errorCode});

            byte[] data = EpEncoder.do_card_frozen_amt(epCode, epGunNo, innerCardNo, 0, errorCode);
            EpMessageSender.sendMessage(epCommClient, 0, 0, Iec104Constant.M_CARD_FRONZE_AMT_RET, data, cmdTimes, epCommClient.getVersion());
        }
    }

    /**
     * 检查订单数据库状态
     */
    public static int checkDBOrderStatus(String chargeSerialNo) {
        int ret;
        int orderStatus = ChargeServiceImpl.getChargeOrderStatus(chargeSerialNo);
        if (orderStatus == 2 || orderStatus == 3)//
        {
            logger.info(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "serialNo|orderStatus"), chargeSerialNo, orderStatus);
            ret = 3;
        } else
            ret = orderStatus;

        return ret;
    }

    /**
     * 检查参数
     *
     * @param consumeRecord
     * @return
     */
    private static int checkConsumeRecordParams(ConsumeRecord consumeRecord) {
        if (consumeRecord == null) {
            return 4;
        }
        //1.检查流水号是否合法
        String epCode = consumeRecord.getEpCode();
        int epGunNo = consumeRecord.getEpGunNo();

        String serialNo = consumeRecord.getSerialNo();
        String zeroSerialNo = StringUtil.repeat("0", 32);
        String zeroEpCode = StringUtil.repeat("0", 32);

        if (serialNo == null || serialNo.length() != 32) {
            consumeRecord.setSerialNo(zeroSerialNo);
            return 5;
        } else {
            if (serialNo.compareTo(zeroSerialNo) == 0) {
                return 4;
            }
        }
        if (epCode == null || epCode.length() != 16) {
            epCode = zeroEpCode;
            return 4;
        } else {
            if (epCode.compareTo(zeroEpCode) == 0) {
                return 4;
            }
        }

        return 0;
    }

    public static int isNewCouponStatus(int currentType, int newCouponAcStatus, int newCouponDcStatus) {
        if ((newCouponAcStatus == 0 && currentType == EpConstants.EP_AC_TYPE) ||
                (newCouponDcStatus == 0 && currentType == EpConstants.EP_DC_TYPE)) {
            return 1;//该用户首次使用(分交直流)
        }
        return 0;
    }

    public static int getPayMode(ConsumeRecord consumeRecord) {
        if (consumeRecord.getAccountType() == 3)
            return EpConstants.P_M_POSTPAID;

        if (consumeRecord.getAccountType() == 4 && consumeRecord.getUserOrgin() == UserConstants.ORG_PXGJ)
            return EpConstants.P_M_POSTPAID;

        return EpConstants.P_M_FIRST;
    }

    /**
     * 处理后付费订单
     *
     * @param epGunCache
     * @param consumeRecord
     * @return
     */
    public static int statAfterPayCharge(EpGunCache epGunCache, ConsumeRecord consumeRecord) {
        int chOrStatus = checkDBOrderStatus(consumeRecord.getSerialNo());

        if (chOrStatus != ChargeOrderConstants.ORDER_STATUS_NO) {
            return 1;
        }
        String epCode = consumeRecord.getEpCode();
        int epGunNo = consumeRecord.getEpGunNo();

        if (consumeRecord.getAccountType() == 3) {
            if (consumeRecord.getUserOrgin() == UserConstants.ORG_BQ || consumeRecord.getUserOrgin() == UserConstants.ORG_XIAN) {
                return EpGunService.endBigConsumeRecord(epGunCache.getPkEpGunId(), consumeRecord);
            } else if (consumeRecord.getUserOrgin() == 5) {
                logger.info(LogUtil.addBaseExtLog("discard,CPYNUMBER_BQ old order")
                        , new Object[]{LogConstants.FUNC_CONSUME_RECORD, epCode, epGunNo, consumeRecord.getUserOrgin(), consumeRecord.getEpUserAccount(), consumeRecord.getSerialNo()});
                return 1;
            } else {
                return EpGunService.endCreditConsumeRecord(epGunCache.getPkEpGunId(), consumeRecord);

            }
        } else if (consumeRecord.getAccountType() == 4 && consumeRecord.getUserOrgin() == UserConstants.ORG_PXGJ) {
            return epGunCache.endPXConsumeRecord(consumeRecord);
        }

        logger.info(LogUtil.addBaseExtLog("invalid accountType|serialNo"),
                new Object[]{LogConstants.FUNC_CONSUME_RECORD, epCode, epGunNo, consumeRecord.getUserOrgin(), consumeRecord.getEpUserAccount(), consumeRecord.getAccountType(), consumeRecord.getSerialNo()});
        return 1;
    }

    /**
     * TODO:确认回复需要回复
     * 消费记录电桩编号和枪口编号一定要准确，错了就发到别的地方
     *
     * @param channel
     * @param consumeRecord
     * @throws IOException
     */
    public static void handleConsumeRecord(int cmdNo, EpCommClient epCommClient, ConsumeRecord consumeRecord, byte[] cmdTimes) throws IOException {
        logger.debug(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "consumeRecord"), consumeRecord);

        //1.检查订单参数
        int retCode = checkConsumeRecordParams(consumeRecord);
        if (retCode != 0) {
            logger.error(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "error,retCode"), retCode);
            byte[] confirmdata = EpEncoder.do_consumerecord_confirm(consumeRecord.getEpCode(), consumeRecord.getEpGunNo(), consumeRecord.getSerialNo(), retCode);
            EpMessageSender.sendMessage(epCommClient, 0, 0, cmdNo, confirmdata, cmdTimes, epCommClient.getVersion());

            return;
        }
        //2.检查桩和枪是否存在
        String epCode = consumeRecord.getEpCode();
        int epGunNo = consumeRecord.getEpGunNo();
        String serialNo = consumeRecord.getSerialNo();

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache == null) {
            logger.error(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "did not find gun,epCode|epGunNo"),
                    new Object[]{epCode, epGunNo});
            byte[] confirmdata = EpEncoder.do_consumerecord_confirm(consumeRecord.getEpCode(), consumeRecord.getEpGunNo(), consumeRecord.getSerialNo(), retCode);
            EpMessageSender.sendMessage(epCommClient, 0, 0, cmdNo, confirmdata, cmdTimes, epCommClient.getVersion());
            return;
        }
        //3.判断订单是后付费还是预付费
        retCode = 1;
        int payMode = getPayMode(consumeRecord);
        if (payMode == EpConstants.P_M_POSTPAID) {
            //4.处理后付费订单
            retCode = statAfterPayCharge(epGunCache, consumeRecord);
        } else {
            //5.判断是不是历史订单
            boolean isHistoryRecord = false;
            ChargeCache curCC = epGunCache.getChargeCache();
            ChargeCache statingCC = null;
            if (curCC != null && serialNo.equals(curCC.getChargeSerialNo())) {
                statingCC = curCC;
            } else {
                isHistoryRecord = true;
                statingCC = EpChargeService.GetChargeCacheFromDb(serialNo);
            }

            retCode = 0;
            //6.判断历史订单是否已经处理过
            if (isHistoryRecord) {
                int ret = EpChargeService.checkDBOrderStatus(serialNo);
                //订单已经处理过
                if (ret == ChargeOrderConstants.ORDER_STATUS_DONE)//
                {
                    retCode = 3;
                }
            }
            logger.info(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "epCode|epGunNo|isHistoryRecord|statingCC|retCode"),
                    new Object[]{epCode, epGunNo, isHistoryRecord, statingCC, retCode});


            //7.结算订单
            if (retCode == 0 && statingCC != null) {
                retCode = epGunCache.checkConsumeRecord(statingCC, consumeRecord.getEpUserAccount());
                //爱充正常用户充电记录
                if (retCode == 1) {
                    retCode = epGunCache.statFirstPayCharge(statingCC, consumeRecord);
                }
            }
            //8.清理当前枪状态
            if (!isHistoryRecord && retCode == 1) {
                logger.debug(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "serialNo|curCC"), serialNo, curCC);
                //6.清除实时数据信息
                epGunCache.cleanChargeInfoInRealData();
                //8.清空掉充电缓存对象
                epGunCache.cleanChargeInfo();
            }

        }

        logger.info(LogUtil.addFuncExtLog(LogConstants.FUNC_CONSUME_RECORD, "epCode|epGunNo|serialNo|retCode"),
                new Object[]{epCode, epGunNo, serialNo, retCode});

        if (retCode >= 0 && retCode <= 4) {
            byte[] confirmdata = null;

            confirmdata = EpEncoder.do_consumerecord_confirm(epCode, epGunNo, consumeRecord.getSerialNo(), retCode);
            EpMessageSender.sendMessage(epCommClient, 0, 0, cmdNo, confirmdata, cmdTimes, epCommClient.getVersion());
        }

    }


    public static void handleHisConsumeRecord(EpCommClient epCommClient, ConsumeRecord consumeRecord) throws IOException {
        Logger log = LoggerFactory.getLogger("epRamLog");

        log.info("consumeRecord:{}", consumeRecord);
    }

    /**
     * 该函数用来测试强制移除充电业务
     *
     * @param epCode
     * @param epGunNo
     */
    static public String forceRemoveCharge(String epCode, int epGunNo) {
        if (epCode != null && epCode.length() != YXCConstants.LEN_CODE) {
            return "remove charge cache! code invalid! \n";
        }
        BaseEPCache epClient = CacheService.getEpCache(epCode);
        if (epClient == null) {
            return "remove charge cache! did not find ep" + epCode + " \n";
        }

        int nGunNum = epClient.getGunNum();


        if (epGunNo < 1 || epGunNo > nGunNum) {
            return "remove charge cache! epGunNo invalid" + epGunNo + " \n";
        }

        //String epGunNoKey = epCode +epGunNo;
        //int currentType = epClient.getCurrentType();
        //ChargeCache chargeCache=null;

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache != null) {
            if (epGunCache.getChargeCache() != null) {
                long et = DateUtil.getCurrentSeconds();
                ChargeServiceImpl.endChargeRecord(epGunCache.getChargeCache().getChargeSerialNo(), ChargeRecordConstants.CS_CHARGE_END,
                        DateUtil.toDate(et * 1000), new BigDecimal(0), epGunCache.getChargeCache().getRateInfo().getServiceRate());
                logger.info("forceRemovecharge,endChargeRecord,epCode:{},epGunNo:{}", epCode, epGunNo);
            }

            //TODO://如果处理数据库
            epGunCache.cleanChargeInfo();

            BigDecimal bdZero = new BigDecimal(0.0);
            EpGunService.updateChargeInfoToDbByEpCode(epClient.getCurrentType(), epCode, epGunNo,
                    bdZero, "", bdZero, 0, 0);
            logger.info("forceRemovecharge,epCode:{},epGunNo:{}", epCode, epGunNo);
        }
        return "";
    }

    /**
     * 该函数用来测试强制移除充电业务
     *
     * @param epCode
     * @param epGunNo
     */
    static public String forceRemoveBespoke(String epCode, int epGunNo) {
        if (epCode != null && epCode.length() != YXCConstants.LEN_CODE) {
            return "remove bespoke cache! code invalid! \n";
        }
        BaseEPCache epClient = CacheService.getEpCache(epCode);
        if (epClient == null) {
            return "remove bespoke cache! did not find ep" + epCode + " \n";
        }

        int nGunNum = epClient.getGunNum();


        if (epGunNo < 1 || epGunNo > nGunNum) {
            return "remove bespoke cache! epGunNo invalid" + epGunNo + " \n";
        }

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache != null) {
            //epGunCache.cleanBespokeInfo();
            logger.info("forceRemovebespoke,epCode:{},epGunNo:{}", epCode, epGunNo);
        }
        return "";
    }

    public static String makeSerialNo() {
        Date now = new Date();
        Random random = new Random();

        return DateUtil.toDateFormat(now, Global.serialSecFormat) + "1" + String.format("%03d", Math.abs(random.nextLong() % 1000));
    }

    private static String getFixLenthString(int strLength) {

        Random rm = new Random();

        // 获得随机数  
        double a = rm.nextDouble();

        // 将获得的获得随机数转化为字符串  
        String fixLenthString = String.valueOf(a);

        // 返回固定的长度的随机数  
        return fixLenthString.substring(2, strLength + 2);
    }

    public static String makeChargeOrderNo(long pkGunId, long usrId) {
        long now = DateUtil.getCurrentSeconds();

        long l1 = pkGunId % 100000;
        long l2 = pkGunId % 1000000;

        String chOrCode = String.format("%d%05d%06d", now, l1, l2);

        return chOrCode;
    }

    public static int getOrType(int level) {
        int chorType = 1;
        if (level == 6)
            chorType = 1;
        else if (level == 5)
            chorType = 2;
        else
            chorType = 3;

        return chorType;
    }

    private static TblPowerModule converPowerModule(ChargePowerModInfo info) {
        TblPowerModule powerModule = new TblPowerModule();

        powerModule.setPkPowerModuleid(info.getPkId());
        //1.输出电压
        powerModule.setOutput_voltage((new BigDecimal(info.getOutVol())).multiply(Global.Dec1));

        //2.输出电流
        powerModule.setOutput_current((new BigDecimal(info.getOutCurrent())).multiply(Global.Dec2));

        //3.A相电压
        powerModule.setA_voltage((new BigDecimal(info.getaVol())).multiply(Global.Dec1));

        //4.B相电压
        powerModule.setB_voltage((new BigDecimal(info.getbVol())).multiply(Global.Dec1));

        //5.C相电压
        powerModule.setC_voltage((new BigDecimal(info.getcVol())).multiply(Global.Dec1));

        //6.A相电流
        powerModule.setA_current((new BigDecimal(info.getaCurrent())).multiply(Global.Dec2));

        //7.B相电流
        powerModule.setB_current((new BigDecimal(info.getbCurrent())).multiply(Global.Dec2));

        //8.C相电流
        powerModule.setC_current((new BigDecimal(info.getcCurrent())).multiply(Global.Dec2));

        //9.模块温度
        powerModule.setTemperature((new BigDecimal(info.getTemp())).multiply(Global.Dec1));

        return powerModule;

    }

    public static void updatePowerInfoToDB(ChargePowerModInfo info) {

        if (info == null) {
            logger.error("updatePowerInfoToDB error!ChargePowerModInfo:{}", info);
            return;
        }

        TblPowerModule dbInfo = converPowerModule(info);

        dbInfo.setPkPowerModuleid(info.getPkId());

        EpGunServiceImpl.updatePowerInfo(dbInfo);

    }

    public static void insertPowerInfoDB(ChargePowerModInfo info, String chargeSerialNo) {
        if (info == null || chargeSerialNo == null || chargeSerialNo.length() != 32) {
            logger.error("insertPowerInfoDB error!ChargePowerModInfo:{},chargeSerialNo:{}", info, chargeSerialNo);
            return;
        }

        TblPowerModule dbInfo = converPowerModule(info);
        dbInfo.setChargeSerialNo(chargeSerialNo);

        dbInfo.setPowerModuleName("电源模块_" + info.getModId());

        int pkId = EpGunServiceImpl.insertPowerInfo(dbInfo);

        info.setPkId(pkId);

    }

    public static void savePowerModule(Map<Integer, ChargePowerModInfo> mapPowerModule, String chargeSerialNo) {
        logger.debug("savePowerModule");
        if (mapPowerModule.size() == 0) //没有数据需要更新
        {
            return;
        }
        Iterator iter = mapPowerModule.entrySet().iterator();

        while (iter.hasNext()) {

            Map.Entry entry = (Map.Entry) iter.next();

            ChargePowerModInfo powerModInfo = (ChargePowerModInfo) entry.getValue();
            if (powerModInfo == null || powerModInfo.getChange() != 1)
                continue;

            if (powerModInfo.getPkId() == 0)
                insertPowerInfoDB(powerModInfo, chargeSerialNo);
            else {
                updatePowerInfoToDB(powerModInfo);
            }

            powerModInfo.setChange(0);
        }
    }

    private static TblVehicleBattery convertChargeCarInfo(ChargeCarInfo info) {
        TblVehicleBattery battery = new TblVehicleBattery();


        battery.setBattery_manufacturers(info.getBatteryManufacturer());

        battery.setBattery_rated_capacity((int) info.getTotalBattryCapacity());
        battery.setBattery_type((int) info.getBattryType());
        battery.setCycle_count((int) info.getBattryChargeTimes());
        battery.setMax_current((new BigDecimal(info.getSignleAllowableHighCurrent())).multiply(Global.Dec2));
        battery.setMax_temperature((new BigDecimal(info.getAllowableHighTotalTemp())).multiply(Global.Dec1));
        battery.setSingle_max_vol((new BigDecimal(info.getSignleAllowableHighVol())).multiply(Global.Dec1));
        battery.setTotal_energy((new BigDecimal(info.getTotalBattryPower())).multiply(Global.Dec1));
        battery.setVin(info.getCarIdenty());
        battery.setTotal_rated_voltage((new BigDecimal(info.getAllowableHighTotalVol())).multiply(Global.Dec1));

        int year = info.getBattryMadeYear();
        int day = info.getBattryMadeDay();

        String dateString = String.format("%04d%02d%02d", year, day / 100, day % 100);
        battery.setProduction_date(DateUtil.parse(dateString));

        return battery;
    }

    public static void updateChargeCarInfoToDB(ChargeCarInfo info) {

        if (info == null) {
            logger.error("updatePowerInfoToDB error!ChargePowerModInfo:{}", info);
            return;
        }

        TblVehicleBattery dbInfo = convertChargeCarInfo(info);

        dbInfo.setPk_VehicleBattery(info.getPkId());

        EpGunServiceImpl.updateVehicle(dbInfo);
    }

    public static void insertChargeCarInfoToDB(ChargeCarInfo info, String chargeSerialNo) {
        if (info == null || chargeSerialNo == null || chargeSerialNo.length() != 32) {
            logger.error("insertChargeCarInfoToDB error!ChargePowerModInfo:{},chargeSerialNo:{}", info, chargeSerialNo);
            return;
        }

        TblVehicleBattery dbInfo = convertChargeCarInfo(info);
        dbInfo.setChargeSerialNo(chargeSerialNo);

        int pkId = EpGunServiceImpl.insertVehicle(dbInfo);
        info.setPkId(pkId);
    }

    public static void onChargeFail(int userId, ChargeCache chargeCache) {
        if (chargeCache.getStatus() == ChargeRecordConstants.CS_CHARGE_FAIL) {
            logger.error("onChargeFail had handle,chargeCache:{}", chargeCache);
            return;
        }
        chargeCache.setStatus(ChargeRecordConstants.CS_CHARGE_FAIL);

        try {

            ChargeServiceImpl.updateChargeRecordStatus(chargeCache.getChargeSerialNo(), ChargeRecordConstants.CS_CHARGE_FAIL);

            //2.退钱.没有更新成功不退钱，否则造成下一次退钱
            BigDecimal bdFronzeAmt = NumUtil.intToBigDecimal2(chargeCache.getFronzeAmt());
            logger.info("onChargeFail,return accountId:{},fronze amt:{}", userId, bdFronzeAmt);
            UserService.addAmt(userId, bdFronzeAmt, chargeCache.getChargeSerialNo());
        } catch (Exception e) {
            logger.error("updateBeginRecordStatus exception.accountId:{},chargeCache:{},getStackTrace:{}",
                    new Object[]{userId, chargeCache, e.getStackTrace()});
        }
    }

    public static void saveVehicleBatteryToDb(ChargeCarInfo info, String chargeSerialNo) {
        logger.debug("saveVehicleBatteryToDb");
        if (info == null) {
            return;
        }
        if (info.getPkId() == 0)//第一次保存
        {
            insertChargeCarInfoToDB(info, chargeSerialNo);
        } else//有数据更新
        {
            updateChargeCarInfoToDB(info);
        }
    }

    public static TblCoupon queryCoupon(int cpUserid, int cpLimitation, int consemeAmt) {
        return EpGunServiceImpl.queryCoupon(cpUserid, cpLimitation, 0, consemeAmt);
    }

    public static void statChargeAmt(int userId, int fronzeAmt, int payMode,
                                     ConsumeRecord consumeRecord, int couponAmt, int pkOrderId, String orderNo, boolean isPauseStat) {

        int realConsumeAmt = consumeRecord.getTotalAmt();

        int realDiscountAmt = consumeRecord.getRealCouponAmt();


        logger.info("endcharge stat accountId:{},fronzeAmt:{},realConsumeAmt:{},realDiscountAmt:{},isPauseStat:{}",
                new Object[]{userId, fronzeAmt, realConsumeAmt, realDiscountAmt, isPauseStat});

        //先付费的结算资金
        if (payMode != EpConstants.P_M_FIRST) {
            logger.info("endcharge stat 1 accountId:{}", userId);
            return;
        }
        if (consumeRecord.getType() == 1) fronzeAmt = fronzeAmt * 100;
        int remainAmt = fronzeAmt - realConsumeAmt; //剩余金额
        logger.info("endcharge stat userId:{},fronzeAmt:{},realAmt:{},remainAmt:{}", new Object[]{userId, fronzeAmt, realConsumeAmt, remainAmt});

        if (remainAmt <= 0) {
            logger.info("endcharge stat 2 accountId:{}", userId);
            return;
        }

        BigDecimal bdRemainAmt = NumUtil.intToBigDecimal2(remainAmt);
        if (consumeRecord.getType() == 1) bdRemainAmt = NumUtil.intToBigDecimal42(remainAmt);
        UserService.addAmt(userId, bdRemainAmt, consumeRecord.getSerialNo());

        if (isPauseStat) {
            try {
                EpChargeService.jmsgPauseOrderStat(userId, pkOrderId, orderNo, bdRemainAmt.toString());
            } catch (Exception e) {

            }
        }
    }

    /**
     * 处理电桩上送的正确消费记录
     *
     * @param gunCache
     * @param consumeRecord
     * @param chargeCacheObj
     * @param chargeUser
     * @param currentType
     * @param totalChargeTime
     * @param totalChargeMeterNum
     * @return
     */
    public static int handleRightConsumeRecord(EpGunCache gunCache, ConsumeRecord consumeRecord,
                                               ChargeCache chargeCacheObj, UserCache chargeUser, int currentType,
                                               int totalChargeTime, int totalChargeMeterNum, boolean isPauseStat) {
        if (gunCache == null) {
            logger.error("endcharge handleRightConsumeRecord fail,gunCache is null,epCode:{},epGunNo:{},SerialNo:{}",
                    new Object[]{consumeRecord.getEpCode(), consumeRecord.getEpGunNo(), consumeRecord.getSerialNo()});
            return 0;
        }
        if (chargeUser == null) {
            logger.error("endcharge handleRightConsumeRecord fail,chargeUser is null,epCode:{},epGunNo:{},SerialNo:{}",
                    new Object[]{consumeRecord.getEpCode(), consumeRecord.getEpGunNo(), consumeRecord.getSerialNo()});
            return 0;
        }

        int userId = chargeUser.getId();
        String epCode = gunCache.getEpCode();
        int epGunNo = gunCache.getEpGunNo();
        String chargeSerialNo = chargeCacheObj.getChargeSerialNo();

        int discountType = getDiscountType(gunCache, chargeUser, chargeCacheObj.getFronzeAmt(), chargeCacheObj.getPayMode(), consumeRecord);

        int discountIdentity = consumeRecord.getDiscountIdentity();
        int couponAmt = consumeRecord.getCouponAmt();
        if (discountType == 0) {
            consumeRecord.setDiscountServicePrice(chargeCacheObj.getRateInfo().getServiceRate());
        }

        logger.info("endcharge before statChargeAmt,epCode:{},epGunNo:{},SerialNo:{},TotalDl:{},totalAmt:{}," +
                        "couponAmt:{},discountAmt:{},discountIdentity:{},discountType:{}",
                new Object[]{consumeRecord.getEpCode(), consumeRecord.getEpGunNo(),
                        consumeRecord.getSerialNo(), totalChargeMeterNum, consumeRecord.getTotalAmt(),
                        couponAmt, consumeRecord.getRealCouponAmt(), discountIdentity, discountType});

        //2.结算钱
        EpChargeService.statChargeAmt(chargeUser.getId(), chargeCacheObj.getFronzeAmt(),
                chargeCacheObj.getPayMode(), consumeRecord, couponAmt,
                chargeCacheObj.getPkOrderId(), chargeCacheObj.getChOrCode(), isPauseStat);


        int totalAmt = consumeRecord.getTotalAmt();
        int realCouponAmt = consumeRecord.getRealCouponAmt();

        int realAmt = totalAmt;

        if (consumeRecord.getType() == 0) {
            ChargeServiceImpl.addPurchaseHistoryToDB(NumUtil.intToBigDecimal2(realAmt), 1, userId, 0, "充电消费", epCode, chargeSerialNo, "");
        } else {
            ChargeServiceImpl.addPurchaseHistoryToDB(NumUtil.intToBigDecimal4(realAmt), 1, userId, 0, "充电消费", epCode, chargeSerialNo, "");
        }
        if (realCouponAmt != 0)//如果有优惠券抵扣
        {
            String tips = "优惠券抵扣";
            if (discountType == ChargeRecordConstants.CHARGEORDER_THIRDTYPE_VIN) {
                tips = "VIN码优惠";
            }

            if (consumeRecord.getType() == 0) {
                ChargeServiceImpl.addPurchaseHistoryToDB(NumUtil.intToBigDecimal2(realCouponAmt), 6, userId, 0, tips, epCode, chargeSerialNo, "");
            } else {
                ChargeServiceImpl.addPurchaseHistoryToDB(NumUtil.intToBigDecimal4(realCouponAmt), 6, userId, 0, tips, epCode, chargeSerialNo, "");
            }
        }

        //3.记录正常数据到订单
        if (chargeCacheObj.getPkOrderId() > 0 || ChargeServiceImpl.getChargingRecord(chargeSerialNo) != null) {
            if (consumeRecord.getType() == 0) {
                updateChargeToDb(gunCache, chargeCacheObj,
                        consumeRecord, false, NumUtil.intToBigDecimal2(realCouponAmt),
                        discountIdentity, discountType, consumeRecord.getDiscountServicePrice());
            } else {
                updateChargeToDb(gunCache, chargeCacheObj,
                        consumeRecord, false, NumUtil.intToBigDecimal4(realCouponAmt),
                        discountIdentity, discountType, consumeRecord.getDiscountServicePrice());
            }
        } else {
            int chorType = EpChargeService.getOrType(chargeUser.getLevel());

            int pkUserCard = chargeCacheObj.getPkUserCard();
            insertFullChargeOrder(chargeUser.getId(), chorType, chargeUser.getAccount(), pkUserCard, consumeRecord.getUserOrgin(),
                    gunCache.getPkEpGunId(), gunCache.getEpCode(), gunCache.getEpGunNo(), EpConstants.CHARGE_TYPE_CARD, "", chargeCacheObj.getChOrCode(), 2,
                    new BigDecimal(realCouponAmt), discountIdentity, discountType, consumeRecord, chargeCacheObj.getRateInfo(), ChargeOrderConstants.ORDER_STATUS_SUCCESS);

        }

        logger.info("endcharge success,epCode:{},epGunNo:{},SerialNo:{},TotalDl:{},totalAmt:{}," +
                        "getChargeUseTimes:{},getPkCouPon:{},ThirdType:{},realCouponAmt",
                new Object[]{consumeRecord.getEpCode(), consumeRecord.getEpGunNo(),
                        consumeRecord.getSerialNo(), totalChargeMeterNum, totalAmt, totalChargeTime,
                        discountIdentity, discountType, realCouponAmt});

        int AcStatus = chargeUser.getNewcouponAcStatus();
        int DcStatus = chargeUser.getNewcouponDcStatus();
        //3.送邀请者优惠券
        if (AcStatus == 0 && DcStatus == 0) {
            sendInviteCoupon(chargeUser.getInvitePhone());
        }
        //4.更新用户新手状态
        updateNewcouponStatus(chargeUser, currentType);

        //4.记录充电统计
        EpGunServiceImpl.addChargeStat(gunCache.getPkEpGunId(), totalChargeMeterNum,
                totalChargeTime, totalAmt);

        return couponAmt;
    }

    /**
     * 计算实际优惠
     *
     * @param totalAmt
     * @param couPonAmt
     * @return
     */
    public static int calcRealCouPonAmt(int totalAmt, int couPonAmt) {
        int realCouPonAmt = 0;
        if (couPonAmt <= 0)
            return realCouPonAmt;

        if (totalAmt >= couPonAmt)
            realCouPonAmt = couPonAmt;
        else
            realCouPonAmt = totalAmt;

        return realCouPonAmt;
    }

    /**
     * 查找用户的可用优惠券
     *
     * @param usrId
     * @param currentType
     * @param acStatus
     * @param dcStatus
     * @param totalAmt
     * @return
     */
    public static TblCoupon findCoupon(int usrId, int currentType, int acStatus, int dcStatus, int totalAmt) {
        try {
            TblCoupon tblCoupon = null;
            int cpLimitation = getCouponEpType(currentType);
            int cpStatus = getNewCouponStatus(currentType, acStatus, dcStatus);
            if (cpStatus == 0) {
                tblCoupon = EpGunServiceImpl.queryCoupon(usrId, cpLimitation, 2, totalAmt);
                if (tblCoupon == null) {
                    tblCoupon = queryCoupon(usrId, cpLimitation, totalAmt);
                }
            } else
                tblCoupon = queryCoupon(usrId, cpLimitation, totalAmt);

            return tblCoupon;
        } catch (Exception e) {
            logger.error("findCoupon exception! accountId:{},currentType:{},acStatus:{},dcStatus:{},itotalAmt:{}",
                    new Object[]{usrId, currentType, acStatus, dcStatus, totalAmt});
            return null;
        }
    }

    /**
     * 给邀请车发送新手优惠券
     *
     * @param inviteUsrAccount
     */
    public static void sendInviteCoupon(String inviteUsrAccount) {
        UserRealInfo userInfo = UserService.findUserRealInfo(inviteUsrAccount);
        if (userInfo == null) {
            logger.error("endcharge stat not find Invite user,phone:{}", inviteUsrAccount);
            return;
        }

        EpGunServiceImpl.insertInviteCoupon(userInfo.getId());
    }

    public static int getNewCouponStatus(int currentType, int acStatus, int dcStatus) {
        if (currentType == EpConstants.EP_AC_TYPE)
            return acStatus;
        return dcStatus;
    }

    public static int getCouponEpType(int currentType) {
        if (currentType == EpConstants.EP_AC_TYPE)
            return 1;
        return 2;
    }

    public static boolean isVinCodeDiscount(String epCode, int epGunNo, int fronzeAmt, int payMode, ConsumeRecord consumeRecord) {
        if (consumeRecord == null)
            return false;
        String carVinCode = consumeRecord.getCarVinCode();
        if (carVinCode == null || carVinCode.length() <= 0)
            return false;

        logger.info("isVinCodeDiscount,epCode:{},epGunNo:{},carVinCode:{},carVinCode hex:{},chargeSerialNo:{}",
                new Object[]{epCode, epGunNo, carVinCode, WmIce104Util.ConvertHex2(carVinCode.getBytes()), consumeRecord.getSerialNo()});

        try {
            TblCarVin carVin = getCarVinByCode(epCode, epGunNo, carVinCode);
            if (carVin != null) {
                consumeRecord.setDiscountIdentity(carVin.getPkCarVin());
                consumeRecord.setDiscountServicePrice(carVin.getVinServicemoney());

                calcVinCodeDiscount(consumeRecord, fronzeAmt, payMode);

                return true;

            }
        } catch (Exception e) {
            logger.error("checkVinCodeDiscount,epCode:{},epGunNo:{},carVinCode:{},chargeSerialNo:{}",
                    new Object[]{epCode, epGunNo, carVinCode, consumeRecord.getSerialNo()});

        }

        return false;
    }

    public static void calcVinCodeDiscount(ConsumeRecord consumeRecord, int fronzeAmt, int payMode) {
        BigDecimal servicePrice = consumeRecord.getDiscountServicePrice();

        //1.重新计算服务费
        int totalDl = consumeRecord.getTotalDl();

        BigDecimal dl = new BigDecimal(totalDl).multiply(Global.Dec3);
        int oldServiceAmt = consumeRecord.getServiceAmt();

        int oldTotalAmt = consumeRecord.getTotalChargeAmt() + oldServiceAmt;
        int serviceAmt = dl.multiply(servicePrice).multiply(Global.DecTime2).intValue();
        if (consumeRecord.getType() == 1) {
            serviceAmt = dl.multiply(servicePrice).multiply(Global.DecTime4).intValue();
            fronzeAmt = fronzeAmt * 100;
        }

        consumeRecord.setUndiscountTotalAmt(oldTotalAmt);


        int totalAmt = consumeRecord.getTotalChargeAmt() + serviceAmt;
        int discountAmt = 0;
        if (payMode == EpConstants.P_M_FIRST) {
            if (totalAmt <= fronzeAmt) {
                discountAmt = oldServiceAmt - serviceAmt;
            } else {
                totalAmt = fronzeAmt;
                discountAmt = oldTotalAmt - fronzeAmt;
            }
        } else {
            discountAmt = oldTotalAmt - totalAmt;
        }

        logger.info("calcVinCodeDiscount,oldServiceAmt:{},oldTotalAmt:{},serviceAmt:{},totalAmt:{},discountAmt:{}",
                new Object[]{oldServiceAmt, oldTotalAmt, serviceAmt, totalAmt, discountAmt});

        consumeRecord.setDiscountServiceAmt(serviceAmt);
        consumeRecord.setTotalAmt(totalAmt);
        consumeRecord.setCouponAmt(discountAmt);
        consumeRecord.setRealCouponAmt(discountAmt);
    }

    /**
     * 检查VINCODE费率优惠情况
     *
     * @param gunCache
     * @param consumeRecord
     * @param chargeCacheObj
     * @param chargeUser
     * @param currentType
     * @param totalChargeTime
     * @param totalChargeMeterNum
     * @return
     */
    public static int getDiscountType(EpGunCache gunCache, UserCache chargeUser, int fronzeAmt, int payMode, ConsumeRecord consumeRecord) {
        if (gunCache == null)
            return 0;

        if (isVinCodeDiscount(gunCache.getEpCode(), gunCache.getEpGunNo(), fronzeAmt, payMode, consumeRecord))
            return ChargeRecordConstants.CHARGEORDER_THIRDTYPE_VIN;

        if (isCoupondDiscount(gunCache, consumeRecord, chargeUser))
            return ChargeRecordConstants.CHARGEORDER_THIRDTYPE_COUPON;

        //

        consumeRecord.setUndiscountTotalAmt(consumeRecord.getTotalAmt());

        return 0;


    }

    /**
     * 使用优惠券统计
     *
     * @param gunCache
     * @param consumeRecord
     * @param chargeCacheObj
     * @param chargeUser
     * @param currentType
     * @param totalChargeTime
     * @param totalChargeMeterNum
     * @return
     */
    public static boolean isCoupondDiscount(EpGunCache gunCache, ConsumeRecord consumeRecord,
                                            UserCache chargeUser) {
        if (gunCache == null)
            return false;

        if (consumeRecord == null)
            return false;

        int couponAmt = 0; //优惠券金额
        int pkCouPon = 0;

        int currentType = gunCache.getRealChargeInfo().getCurrentType();

        //1.获取优惠券
        TblCoupon tblCoupon = null;

        try {
            int AcStatus = chargeUser.getNewcouponAcStatus();
            int DcStatus = chargeUser.getNewcouponDcStatus();
            int totalAmt = consumeRecord.getTotalAmt();
            int iTotalAmt = totalAmt / 100;//总金额 ，单位：元
            if (consumeRecord.getType() == 1) iTotalAmt = iTotalAmt / 100;
            tblCoupon = findCoupon(chargeUser.getId(), currentType, AcStatus, DcStatus, iTotalAmt);
            if (tblCoupon == null)
                return false;

            //3.设置优惠券已使用
            pkCouPon = tblCoupon.getPkCoupon();
            couponAmt = tblCoupon.getCpCouponvalue() * 100;
            if (consumeRecord.getType() == 1) couponAmt = couponAmt * 100;
            UserServiceImpl.updateCoupon(pkCouPon);

            consumeRecord.setCouponAmt(couponAmt);

            consumeRecord.setDiscountIdentity(pkCouPon);

            int realCouPonAmt = 0;
            realCouPonAmt = calcRealCouPonAmt(totalAmt, couponAmt);
            consumeRecord.setTotalAmt(totalAmt - realCouPonAmt);
            consumeRecord.setRealCouponAmt(realCouPonAmt);
            consumeRecord.setUndiscountTotalAmt(totalAmt);


            logger.info("endcharge useCouPonStat,accountId:{},pkCouPon:{},couPonAmt:{},epCode:{},epGunNo:{},AcStatus:{},DcStatus:{},currentType:{}",
                    new Object[]{chargeUser.getId(), pkCouPon, couponAmt, gunCache.getEpCode(),
                            gunCache.getEpGunNo(), AcStatus, DcStatus, currentType});
            return true;


        } catch (Exception e) {
            logger.error("endcharge get coupon exception,userId:{}", chargeUser.getId());
            return false;
        }
    }


    /**
     * 使用vin统计
     *
     * @param gunCache
     * @param consumeRecord
     * @param chargeCacheObj
     * @param chargeUser
     * @param totalChargeTime
     * @param totalChargeMeterNum
     * @return
     */
    public static void useCarVinStat(EpGunCache gunCache, ConsumeRecord consumeRecord,
                                     ChargeCache chargeCacheObj, UserCache chargeUser, int currentType,
                                     int totalChargeTime, int totalChargeMeterNum) {

        int totalAmt = consumeRecord.getTotalAmt();
        int fronzeAmt = chargeCacheObj.getFronzeAmt();
        if (consumeRecord.getType() == 1) fronzeAmt = fronzeAmt * 100;

        if (totalAmt > fronzeAmt)//重新计算后如果总的费用大于预冻
        {
            consumeRecord.setServiceAmt(fronzeAmt - consumeRecord.getTotalChargeAmt());
            consumeRecord.setTotalAmt(fronzeAmt);
        }

        logger.info("endcharge useCarVinStat,accountId:{},realServiceAmt:{},realTotalAmt:{},epCode:{},epGunNo:{}",
                new Object[]{
                        chargeUser.getId(),
                        consumeRecord.getServiceAmt(),
                        consumeRecord.getTotalAmt(),
                        gunCache.getEpCode(),
                        gunCache.getEpGunNo()});
    }

    /**
     * 从数据库中获取对应的vin
     *
     * @param epCode
     * @param epGunNo
     * @param vinCode
     * @return
     */
    public static TblCarVin getCarVinByCode(String epCode, int epGunNo, String vinCode) {
        if (vinCode == null || vinCode.length() <= 0) {
            logger.error("charge vinCode=null,epCode:{},epGunNo:{}",
                    new Object[]{epCode, epGunNo});
            return null;
        }
        TblCarVin carVin = EpGunServiceImpl.selectByCode(vinCode);
        if (carVin == null) {
            logger.error("charge not find carVin in db,epCode:{},epGunNo:{},carVin:{},carVin(hex):{}",
                    new Object[]{epCode, epGunNo, vinCode, WmIce104Util.ConvertHex2(vinCode.getBytes())});
            return null;
        }

        return carVin;
    }

    /**
     *
     */

    /**
     *
     */
    public static void updateNewcouponStatus(UserCache chargeUser, int epType) {
        int AcStatus = chargeUser.getNewcouponAcStatus();
        int DcStatus = chargeUser.getNewcouponDcStatus();

        if (AcStatus == 0 && epType == EpConstants.EP_AC_TYPE) {
            chargeUser.setNewcouponAcStatus(1);
            UserServiceImpl.insertNewcoupon(chargeUser.getId(), chargeUser.getNewcouponAcStatus(), chargeUser.getNewcouponDcStatus());
        } else if (DcStatus == 0 && epType == EpConstants.EP_DC_TYPE) {
            chargeUser.setNewcouponDcStatus(1);
            UserServiceImpl.insertNewcoupon(chargeUser.getId(), chargeUser.getNewcouponAcStatus(), chargeUser.getNewcouponDcStatus());
        }

    }

    public static int pauseStatCharge(ChargeCache chargeCache) {
        logger.info("pauseStatCharge enter");
        ChargeServiceImpl.updateChargeOrder(chargeCache.getChargeSerialNo());

        chargeCache.setStatus(ChargeRecordConstants.CS_PAUSE_STAT);

        if (chargeCache.getAccount() != null && chargeCache.getAccount().length() == 11) {
            msgPauseStat(chargeCache.getAccount(), GameConfig.sms);
            jmsgPauseStat(chargeCache.getUserId(), chargeCache.getAccount(), chargeCache.getPkOrderId(), chargeCache.getChOrCode());
        }

        return 0;
    }


    public static String getExtraData_CHAT(String epCode, int epGunNo, String usrLog, String extra, int chargeFlag, int successFlag, int ret) {
        String extraData = getExtraData_CCZC(epCode, epGunNo, usrLog, extra, chargeFlag, successFlag, ret);

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        ChargeCache chargeCache = epGunCache.getChargeCache();
        return extraData + "|" + chargeCache.getFronzeAmt();
    }

    public static String getExtraData_CCZC(String epCode, int epGunNo, String usrLog, String extra, int chargeFlag, int successFlag, int ret) {
        String timeStart = DateUtil.currentStringDate(); //充电开始时间
        String timeEnd = "";  //充电结束时间（充电未结束时为空）
        String OrderId = "";  //服务商生成的订单编号
        long timeCharge = 0;  //充电时长（秒）
        BigDecimal feeTotal = new BigDecimal(0);  //充电金额
        BigDecimal power = new BigDecimal(0);//充电电量
        BigDecimal soc = new BigDecimal(0);//电池百分比(整数 0-100)【直流充电时返回】

        String endInfo = "";//充电结束原因


        BigDecimal feeService = new BigDecimal(0);//服务费用
        BigDecimal feeElectric = new BigDecimal(0);//电量费用
        String cityCode = "";//城市编号
        int chargeType = 0;//充电类型0：交流1：直流


        String extraData = "";
        BaseEPCache epCache = CacheService.getEpCache(epCode);
        ;
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epCache == null || epGunCache == null) {
            return "";//
        }

        cityCode = epCache.getOwnCityCode();
        chargeType = epCache.getCurrentType();
        if (chargeType == EpConstants.EP_AC_TYPE)
            chargeType = 0;
        else
            chargeType = 1;

        ChargeCache chargeCache = epGunCache.getChargeCache();
        int status = 0;//状态【0：开始充电异常1:充电中,2:结束充电正常9：结束充电异常，3：充电等待】
        if (chargeFlag == 0)// 开始充电返回
        {
            if (successFlag == 1)//成功
                status = 1;//等待充电状态
            else //充电成功或者失败，0：开始充电异常1:充电中
                status = 0;
        } else if (chargeFlag == 1)// 结束充电返回
        {
            if (successFlag == 1)// 结束充电成功
                status = 2;//2:结束充电正常
            else
                status = 9;//9：结束充电异常
        } else if (chargeFlag == 2) //充电事件
        {
            if (successFlag == 1)//成功
            {
                status = 1;  //1:充电中
            } else {
                status = 0;  //0：开始充电异常
            }

        } else if (chargeFlag == 3) //订单查询
        {
            if (chargeCache != null) {
                int chargeStatus = chargeCache.getStatus();
                switch (chargeStatus) {
                    case ChargeRecordConstants.CS_CHARGING://充电中
                        status = 1;
                        break;
                    case ChargeRecordConstants.CS_CHARGE_FAIL://充电失败
                        status = 0;
                        break;
                    case ChargeRecordConstants.CS_ACCEPT_CONSUMEER_CMD://接受到充电客户的命令
                    case ChargeRecordConstants.CS_WAIT_INSERT_GUN://等待插枪
                    case ChargeRecordConstants.CS_WAIT_CHARGE://等待充电
                        status = 3;
                        break;
                    case ChargeRecordConstants.CS_PAUSE_STAT://临时结算
                    case ChargeRecordConstants.CS_STAT://结算
                    case ChargeRecordConstants.CS_STAT_FINISHED://结算完成
                    case ChargeRecordConstants.CS_CHARGE_END://已经结束充电
                        status = 2;
                        break;
                    default:
                        break;
                }
            } else {
                int workStatus = epGunCache.getRealChargeInfo().getWorkingStatus();
                if (workStatus == YXCConstants.EP_WORK_STATUS_WORK)
                    status = 9;
                else
                    status = 2;

            }
        }

        if (chargeCache != null) {
            OrderId = chargeCache.getChOrCode();
            timeStart = DateUtil.StringYourDate(DateUtil.toDate(chargeCache.getSt() * 1000));
            timeEnd = DateUtil.StringYourDate(DateUtil.toDate(chargeCache.getEt() * 1000));
            if (timeEnd.startsWith("1970-01-01")) timeEnd = "";
            feeService = chargeCache.getRateInfo().getServiceRate();

            if (ret == ErrorCodeConstants.EPE_GUN_NOT_LINKED) {
                endInfo = "车与枪未连接";
            } else if (chargeFlag == 1) {
                endInfo = chargeCache.getStopCauseDesc(ret);
            } else {
                endInfo = "";
            }
        }
        RealChargeInfo chargeInfo = epGunCache.getRealChargeInfo();
        if (chargeInfo != null) {
            timeCharge = chargeInfo.getChargedTime() * 60;
            feeTotal = NumUtil.intToBigDecimal2(chargeInfo.getChargedCost());
            power = NumUtil.intToBigDecimal3(chargeInfo.getChargedMeterNum());
            if (chargeType == 1)// 直流
            {
                soc = NumUtil.intToBigDecimal2(((RealDCChargeInfo) chargeInfo)
                        .getSoc());
            }
            feeService = feeService.multiply(power);
        } else //没有实时电量时服务费为0
            feeService = feeService.multiply(new BigDecimal(0));

        extraData = enCodeExtraData(epCode, usrLog, extra, timeStart, timeEnd,
                OrderId, timeCharge, feeTotal, power, soc, endInfo, feeService,
                feeElectric, cityCode, chargeType, status);

        logger.info("cczc epCode:{},epGunNo:{}, extraData:{}", new Object[]{
                epCode, epGunNo, extraData});
        return extraData;
    }

    public static String enCodeExtraData(String epCode, String usrLog, String extra,
                                         String timeStart, String timeEnd, String OrderId, long timeCharge,
                                         BigDecimal feeTotal,
                                         BigDecimal power,
                                         BigDecimal soc,

                                         String endInfo,
                                         BigDecimal feeService,
                                         BigDecimal feeElectric,
                                         String cityCode,
                                         int chargeType,
                                         int status) {
        String sTimeCharge = "" + timeCharge;
        String sFeeTotal = feeTotal.setScale(2, java.math.BigDecimal.ROUND_HALF_UP).toString();
        String sChargeType = "" + chargeType;
        String sPower = power.setScale(3, java.math.BigDecimal.ROUND_HALF_UP).toString();
        String sSoc = soc.setScale(2, java.math.BigDecimal.ROUND_HALF_UP).toString();
        String sStatus = "" + status;
        String sFeeService = feeService.setScale(2, java.math.BigDecimal.ROUND_HALF_UP).toString();
        String sfeeElectric = feeElectric.setScale(2, java.math.BigDecimal.ROUND_HALF_UP).toString();

        String extraData = "";

        String[] array = {OrderId, epCode, extra, usrLog, timeStart, timeEnd,
                sTimeCharge, sFeeTotal, sChargeType, sPower, sSoc,
                sStatus, endInfo, sFeeService, sfeeElectric, cityCode};
        extraData = StringUtils.join(array, "|");
        return extraData;
    }


    public static String getExtraData_EC(String epCode, int epGunNo, String extra, String usrLog, int chargeFlag, int successFlag, int retCode) {

        int soc = 0;//电池百分比(整数 0-100)【直流充电时返回】

        String extraData = "";
        BaseEPCache epCache = CacheService.getEpCache(epCode);
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epCache == null || epGunCache == null) {
            return "";//
        }


        RealChargeInfo chargeInfo = epGunCache.getRealChargeInfo();
        if (chargeInfo != null) {
            if (epCache.getCurrentType() == EpConstants.EP_DC_TYPE) {
                soc = ((RealDCChargeInfo) chargeInfo).getSoc();
            }
        }
        String sSoc = "" + soc;
        String sGunNo = "0" + epGunNo;
        String endInfo = "0";//0:无错误1: 已经开机2: 未开机3: 枪未连接4:其他错误
        if (retCode == ErrorCodeConstants.EPE_GUN_NOT_LINKED)
            endInfo = "3";
        else if (retCode == ErrorCodeConstants.EP_UNCONNECTED)
            endInfo = "2";
        else if (retCode == 0)
            endInfo = "0";
        else if (retCode == ErrorCodeConstants.CANNOT_OTHER_OPER ||
                retCode == ErrorCodeConstants.USED_GUN ||
                retCode == ErrorCodeConstants.EPE_REPEAT_CHARGE ||
                retCode == ErrorCodeConstants.EPE_OTHER_CHARGING)
            endInfo = "1";
        else
            endInfo = "4";

        int status = 2;//状态【1：开始,2:停止】
        if (chargeFlag == 0)// 开始充电返回
        {
            status = 1;
        } else if (chargeFlag == 1)// 结束充电返回
        {
            status = 2;
            if (successFlag == 1)//正常结束
            {
                endInfo = "0";
            }
        }
        String sStatus = "" + status;
        if (successFlag != 1)//操作失败
            successFlag = 2;

        String time = "" + DateUtil.getCurrentSeconds(); //时间
        String sSuccessFlag = "" + successFlag;
        String[] array = {extra, epCode + sGunNo, "1", usrLog, sStatus, sSuccessFlag, endInfo, sSoc, time};
        extraData = StringUtils.join(array, "|");

        logger.info("EC epCode:{},epGunNo:{}, extraData:{}", new Object[]{
                epCode, epGunNo, extraData});
        return extraData;
    }

}