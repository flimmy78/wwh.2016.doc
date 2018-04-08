package com.ec.service;

import com.ec.cache.*;
import com.ec.config.Global;
import com.ec.constants.EpConstants;
import com.ec.constants.ErrorCodeConstants;
import com.ec.logs.LogConstants;
import com.ec.net.message.AliSMS;
import com.ec.net.message.JPushUtil;
import com.ec.net.message.MobiCommon;
import com.ec.net.proto.WmIce104Util;
import com.ec.service.impl.ChargeServiceImpl;
import com.ec.service.impl.RateServiceImpl;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import com.ec.utils.NumUtil;
import com.ormcore.model.RateInfo;
import com.ormcore.model.TblChargingrecord;
import com.ormcore.model.TblJpush;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractEpChargeService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(AbstractEpChargeService.class.getName()));

    public static ChargeCache convertFromDb(TblChargingrecord tblChargeRecord) {
        if (tblChargeRecord == null)
            return null;
        ChargeCache chargeCache = new ChargeCache();

        chargeCache.setEpCode(tblChargeRecord.getChreUsingmachinecode());
        chargeCache.setEpGunNo(tblChargeRecord.getChreChargingnumber());
        chargeCache.setSt(tblChargeRecord.getChreStartdate().getTime() / 1000);
        chargeCache.setStatus(tblChargeRecord.getStatus());

        chargeCache.setAccount(tblChargeRecord.getUserPhone());
        //chargeCache.setBespNo(tblChargeRecord.getChreBeginshowsnumber());
        chargeCache.setChargeSerialNo(tblChargeRecord.getChreTransactionnumber());
        chargeCache.setToken(tblChargeRecord.getThirdExtraData());

        chargeCache.setChOrCode(tblChargeRecord.getChreCode());

        chargeCache.setUserId(tblChargeRecord.getUserId());
        chargeCache.setUserIdentity(tblChargeRecord.getThirdUsrIdentity());
        //chargeCache.setPkUserCard(tblChargeRecord.getPkUserCard());
        UserOrigin userOrigin;
        if (tblChargeRecord.getPkUserCard() > 0) {
            ChargeCardCache cardCache = AbstractUserService.getChargeCardCache(tblChargeRecord.getPkUserCard());
            chargeCache.setCard(cardCache);
            chargeCache.setStartChargeStyle(EpConstants.CHARGE_TYPE_CARD);
            userOrigin = new UserOrigin(tblChargeRecord.getUserOrgNo(), 3, "");
        } else {
            chargeCache.setStartChargeStyle(EpConstants.CHARGE_TYPE_QRCODE);
            userOrigin = new UserOrigin(tblChargeRecord.getUserOrgNo(), 2, "");

        }

        BigDecimal value = tblChargeRecord.getFrozenAmt().multiply(Global.DecTime2);
        chargeCache.setFronzeAmt(value.intValue());
        chargeCache.setPayMode(tblChargeRecord.getPayMode());

        chargeCache.setUserOrigin(userOrigin);

        RateInfo rateInfo = new RateInfo();
        rateInfo.setJ_Rate(tblChargeRecord.getJPrice());
        rateInfo.setF_Rate(tblChargeRecord.getFPrice());
        rateInfo.setP_Rate(tblChargeRecord.getPPrice());
        rateInfo.setG_Rate(tblChargeRecord.getGPrice());
        rateInfo.setG_Rate(tblChargeRecord.getGPrice());
        rateInfo.setQuantumDate(tblChargeRecord.getQuantumDate());
        rateInfo.setServiceRate(tblChargeRecord.getServicePrice());
        rateInfo.setQuantumDate(tblChargeRecord.getQuantumDate());

        chargeCache.setRateInfo(rateInfo);
        int pkOrderId = ChargeServiceImpl.getOrderId(tblChargeRecord.getChreTransactionnumber());
        chargeCache.setPkOrderId(pkOrderId);

        return chargeCache;
    }

    public static ChargeCache GetUnFinishChargeCache(String epCode, int epGunNo) {
        TblChargingrecord tblChargeRecord = ChargeServiceImpl.getUnFinishedCharge(epCode, epGunNo);
        return convertFromDb(tblChargeRecord);
    }

    protected static ChargeCache GetChargeCacheFromDb(String serialNo) {
        TblChargingrecord tblChargeRecord = ChargeServiceImpl.getChargingRecord(serialNo);
        return convertFromDb(tblChargeRecord);
    }

    protected static int checkElectricDb(int orgNo, String epCode, int epGunNo, String account,
                                         ChargeCardCache card, int severType, short startChargeStyle, String checkCode) {
        return checkElectricDb(orgNo, epCode, epGunNo, account, card, severType, startChargeStyle, checkCode, 0, 0, 1);
    }

    protected static int checkElectricDb(int orgNo, String epCode, int epGunNo, String account,
                                         ChargeCardCache card, int severType, short startChargeStyle, int frozenAmt, int payMode) {
        return checkElectricDb(orgNo, epCode, epGunNo, account, card, severType, startChargeStyle, null, frozenAmt, payMode, 1);
    }

    protected static int checkElectricDb(int orgNo, String epCode, int epGunNo, String account,
                                         ChargeCardCache card, short startChargeStyle, int frozenAmt, int payMode, int sms) {
        return checkElectricDb(orgNo, epCode, epGunNo, account, card, 0, startChargeStyle, null, frozenAmt, payMode, sms);
    }

    /**
     * 参数检查
     *
     * @param epCode
     * @param account
     * @return
     */
    protected static int checkElectricDb(int orgNo, String epCode, int epGunNo, String account, ChargeCardCache card,
            int severType, short startChargeStyle, String checkCode, int frozenAmt, int payMode, int sms) {
        //检查用户
        int accountId = 0;
        if (severType == 3) {
            AbstractUserService.findUserId(orgNo, account);
        } else {
            accountId = Integer.valueOf(account);
        }
        if (accountId == 0) {
            logger.error(LogUtil.addBaseExtLog("startChargeStyle"),
                    new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, account, startChargeStyle});
            return ErrorCodeConstants.INVALID_ACCOUNT;
        }

        //用户能否充电
        UserRealInfo userRealInfo = AbstractUserService.findUserRealInfo(accountId);
        if (null == userRealInfo) {
            return ErrorCodeConstants.INVALID_ACCOUNT;
        }
        int error = userRealInfo.canCharge();
        if (error > 0) return error;
        AbstractCacheService.convertToCache(userRealInfo);

        //检查电桩
        BaseEPCache epCache = AbstractEpService.getEpCacheFromDB(epCode);
        if (epCache == null) return ErrorCodeConstants.EP_UNCONNECTED;

        //检查桩和枪
        error = epCache.canCharge(card, epGunNo);
        if (error > 0) return error;

        //if (!epCache.checkOrgOperate(orgNo)) return ErrorCodeConstants.EP_NOT_OPERATE;

        BaseGunCache epGunCache = AbstractEpGunService.getEpGunCache(epCache.getPkEpId(), epCode, epGunNo);
        if (epGunCache == null) {
            logger.error(LogUtil.addBaseExtLog("pkEpId"),
                    new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, account, epCache.getPkEpId()});
            return ErrorCodeConstants.INVALID_EP_GUN_NO;
        }

        //实时装载未完成的任务
        AbstractEpGunService.loadUnFinishedWork(epGunCache);
        error = epGunCache.canCharge(startChargeStyle, accountId, true);
        if (error > 0) return error;

        if (checkCode != null) {
            // 验证码
            String src = userRealInfo.getDeviceid() + userRealInfo.getPassword()
                    + userRealInfo.getId();
            String calcCheckCode = WmIce104Util.MD5Encode(src.getBytes());

            if (calcCheckCode.compareTo(checkCode) != 0) {
                logger.info(LogUtil.addBaseExtLog("checkCode|calcCheckCode"),
                        new Object[]{LogConstants.FUNC_PHONE_INIT, epCode, epGunNo, orgNo, accountId, checkCode, calcCheckCode});
                return ErrorCodeConstants.ERROR_PHONE_CRC_CODE;
            }
        } else {
            error = checkFrozenAmt(orgNo, accountId, epCode, epGunNo, frozenAmt, payMode, sms);
            if (error > 0) return error;
        }

        if (checkCode != null || (severType == 0 && card == null)) {
            AuthUserCache authUser = epGunCache.getAuth();
            if (authUser == null) {
                authUser = new AuthUserCache(accountId, userRealInfo.getAccount(), DateUtil.getCurrentSeconds(), (short) 1);
                epGunCache.setAuth(authUser);
            } else {
                authUser.setUsrId(accountId);
                authUser.setAccount(userRealInfo.getAccount());
                authUser.setLastTime(DateUtil.getCurrentSeconds());
            }
        }

        return 0;
    }

    protected static int checkFrozenAmt(int orgNo, int accountId, String epCode, int epGunNo, int frozenAmt, int payMode) {
        return checkFrozenAmt(orgNo, accountId, epCode, epGunNo, frozenAmt, payMode, 1);
    }

    protected static int checkFrozenAmt(int orgNo, int accountId, String epCode, int epGunNo, int frozenAmt, int payMode, int sms) {
        if (payMode == EpConstants.P_M_FIRST) {
            UserCache chargeUser = AbstractCacheService.getUserCache(accountId);
            if (chargeUser == null) return ErrorCodeConstants.INVALID_ACCOUNT;
            BigDecimal bdRemainAmt = chargeUser.getUserRealInfo().getMoney();

            //100倍后转为整数
            bdRemainAmt = bdRemainAmt.multiply(Global.DecTime2);
            int nRemainAmt = NumUtil.BigDecimal2ToInt(bdRemainAmt);
            BigDecimal bdFrozenAmt = NumUtil.intToBigDecimal2(frozenAmt);
            //充电冻结金额
            logger.info(LogUtil.addBaseExtLog("nRemainAmt|frozenAmt|payMode"),
                    new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, accountId, nRemainAmt, frozenAmt, payMode});

            //冻结金额
            if (nRemainAmt < 0 || frozenAmt <= 0 || nRemainAmt < frozenAmt) {
                return ErrorCodeConstants.EPE_NO_ENOUGH_MONEY;
            }

            if (!chargeUser.isRemainAmtWarn()
                    && (nRemainAmt - frozenAmt) < (chargeUser.getRemainAmtWarnValue() * 100)
                    && chargeUser.getAccount().length() == 12) {
                chargeUser.setRemainAmtWarn(true);
                int n = nRemainAmt - frozenAmt;
                String warnAmt = NumUtil.intToBigDecimal2(n).toString();

                logger.info(LogUtil.addBaseExtLog("managePhone|customerPhone"),
                        new Object[]{LogConstants.FUNC_START_CHARGE + " big account remainAmtWarning",
                                epCode, epGunNo, orgNo, accountId, chargeUser.getRemainAmtWarnPhone(), chargeUser.getRemainAmtWarnCPhone()});

                AbstractEpChargeService.msgRMAmtWarningToManager(accountId,
                        chargeUser.getRemainAmtWarnPhone(), chargeUser.getName(), warnAmt, sms);

                AbstractEpChargeService.msgRemainAmtWarning(accountId,
                        chargeUser.getRemainAmtWarnCPhone(), warnAmt, sms);
            }
        }

        return 0;
    }

    public static void msgRMAmtWarningToManager(int usrId, String phone, String name, String amt, int sms) {
        logger.info("onRMAmtWarningToManager,usrId:{},phone:{},name:{}",
                new Object[]{usrId, phone, name});
        if (phone == null || phone.length() != 11)
            return;
        if (sms != 1) {
            HashMap<String, Object> params = new HashMap<>();
            params.put("name", name);
            params.put("cost", amt);

            JSONObject jsonObject = JSONObject.fromObject(params);

            boolean flag = AliSMS.sendAliSMS(phone, "SMS_34475278", jsonObject.toString());
            if (!flag) {
                logger.error("onRMAmtWarningToManager fail,usrId,phone:{}", usrId, phone);
            }
        }
    }

    public static void msgRemainAmtWarning(int usrId, String phone, String amt, int sms) {
        logger.info("onRemainAmtWarning,usrId:{},phone:{}", usrId, phone);
        if (phone == null || phone.length() != 11)
            return;

        if (sms != 1) {
            HashMap<String, Object> params = new HashMap<>();
            params.put("cost", amt);

            JSONObject jsonObject = JSONObject.fromObject(params);

            boolean flag = AliSMS.sendAliSMS(phone, "SMS_34445317", jsonObject.toString());
            if (!flag) {
                logger.error("onRemainAmtWarning fail,usrId,phone:{}", usrId, phone);
            }
        }
    }

    public static void jmsgPauseOrderStat(int userId, int pkOrderId, String chargeOrderNo, String remainAmt) {
        TblJpush ju = RateServiceImpl.getJpushInfo(userId);
        if (ju == null) {
            logger.error("msgOrderPauseNotic do not find userId:{},chargeOrderNo,{},remainAmt:{}", new Object[]{userId, chargeOrderNo, remainAmt});
            return;
        }

        logger.info("[endcharge]msgOrderPauseNotic userId:{},chargeOrderNo:{}", userId, chargeOrderNo);

        String msg = String.format("您的订单{}已结算，结算余额{}元，已经返回到您的账户。",
                chargeOrderNo, remainAmt);

        Map<String, String> extras = new HashMap<>();
        extras.put("msg", msg);
        extras.put("orderid", "" + pkOrderId);
        extras.put("type", "12");
        extras.put("title", "充电订单正式结算");
        extras.put("tm", "" + DateUtil.getCurrentSeconds());

        JPushUtil.point2point("充电订单正式结算", msg, extras, ju.getJpushRegistrationid(), ju.getJpushDevicetype());
    }

    public static void jmsgPauseStat(int userId, String account, int pkOrderId, String chargeOrderNo) {
        TblJpush ju = RateServiceImpl.getJpushInfo(userId);
        if (ju == null) {
            logger.error("jmsgNoticePauseStat do not find userId:{},chargeOrderNo,{},remainAmt:{}", new Object[]{userId, account, pkOrderId, chargeOrderNo});
            return;
        }

        logger.info("[endcharge]orderPauseNotic userId:{},chargeOrderNo:{}", userId, chargeOrderNo);

        String msg = String.format("{手机号}您好，您的订单{订单编号}临时结算。结算完成后，订单余额会返回到您的账户中，临时结算不影响充电。",
                account, chargeOrderNo);

        Map<String, String> extras = new HashMap<>();
        extras.put("msg", msg);
        extras.put("orderid", "" + pkOrderId);
        extras.put("type", "11");
        extras.put("title", "充电订单临时结算");
        extras.put("tm", "" + DateUtil.getCurrentSeconds());

        JPushUtil.point2point("充电订单临时结算", msg, extras, ju.getJpushRegistrationid(), ju.getJpushDevicetype());
    }

    public static void msgPauseStat(String curUserAccount, int sms) {
        logger.debug("onPauseStatNotic send msg,curUserAccount:{}", curUserAccount);

        if (sms == 1) {
            try {
                String content = MessageFormat.format("{0}您好，上一次充电未结算。结算完成后，订单余额会返回到您的账户中，不影响本次充电。", curUserAccount);
                MobiCommon.sendWanMatMessage(content, curUserAccount);
            } catch (Exception e) {
                logger.error("onPauseStatNotic fail,e.getMessage:{}", e.getMessage());
            }
            return;
        }


        HashMap<String, Object> params = new HashMap<>();
        params.put("mbcode", curUserAccount);

        JSONObject jsonObject = JSONObject.fromObject(params);

        boolean flag = AliSMS.sendAliSMS(curUserAccount, "SMS_25850225", jsonObject.toString());
        if (!flag) {
            logger.debug("onPuaseStatNotic fail,userAccount:{}", curUserAccount);
        }

    }

}

