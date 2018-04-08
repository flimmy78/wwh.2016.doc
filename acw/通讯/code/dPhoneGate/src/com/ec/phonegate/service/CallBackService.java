package com.ec.phonegate.service;

import com.ec.logs.LogConstants;
import com.ec.net.message.JPushUtil;
import com.ec.phonegate.client.PhoneClient;
import com.ec.phonegate.proto.PhoneConstant;
import com.ec.phonegate.proto.PhoneProtocol;
import com.ec.phonegate.sender.PhoneMessageSender;
import com.ec.service.impl.RateServiceImpl;
import com.ec.usrcore.server.IEventCallBack;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import com.ec.utils.NetUtils;
import com.ormcore.model.TblJpush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CallBackService implements IEventCallBack {

    private final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(CallBackService.class.getName()));

    /**
     * 手机连接应答（usrGate->phone）
     */
    public void onCanUseEp(int orgNo, String userIdentity, String epCode, int epGunNo, String account, int ret, int errorCode, int status, int currentType) {
        logger.info(LogUtil.addBaseExtLog("ret|errorCode|status|currentType"),
                new Object[]{LogConstants.FUNC_PHONE_INIT, epCode, epGunNo, userIdentity, ret, errorCode, status, currentType});

        PhoneClient phoneClient = CachePhoneService.getPhoneClientByAccountId(Integer.valueOf(userIdentity));
        if (phoneClient == null) {
            logger.error(LogUtil.getExtLog("phoneClient is null"));
            return;
        } else {
            phoneClient.setAccount(account);
            phoneClient.setIdentity(account);
        }

        if (ret == 1) {
            PhoneService.sendEPMessage(phoneClient.getChannel(), (short) phoneClient.getCmd(), ret, errorCode, status, currentType);
            phoneClient.setConnectFlg(true);
        } else {
            PhoneService.sendEPMessage(phoneClient.getChannel(), (short) phoneClient.getCmd(), ret, errorCode, 0, 0);
        }
    }

    /**
     * 充电事件（usrGate->phone）
     */
    public void onChargeEvent(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int status) {
        logger.info(LogUtil.addBaseExtLog("extra|status"),
                new Object[]{LogConstants.FUNC_ONCHARGEEVENT, epCode, epGunNo, userIdentity, extra, status});

        PhoneClient phoneClient = CachePhoneService.getPhoneClientByAccountId(Integer.valueOf(userIdentity));
        if (phoneClient == null) {
            logger.error(LogUtil.getExtLog("phoneClient is null"));
            return;
        }

        byte[] data = PhoneProtocol.do_start_charge_event(status);

        PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
    }

    /**
     * 充电应答（usrGate->phone）
     */
    public void onStartCharge(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int ret, int errorCode) {
        logger.info(LogUtil.addBaseExtLog("extra|ret|errorCode"),
                new Object[]{LogConstants.FUNC_ONSTARTCHARGE, epCode, epGunNo, userIdentity, extra, ret, errorCode});

        PhoneClient phoneClient = CachePhoneService.getPhoneClientByAccountId(Integer.valueOf(userIdentity));
        if (phoneClient == null) {
            logger.error(LogUtil.getExtLog("phoneClient is null"));
            return;
        }

        PhoneService.sendMessage(phoneClient.getChannel(), PhoneConstant.D_START_CHARGE, ret, errorCode);
    }

    /**
     * 停止充电应答（usrGate->phone）
     */
    public void onStopCharge(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int ret, int errorCode) {
        logger.info(LogUtil.addBaseExtLog("extra|ret|errorCode"),
                new Object[]{LogConstants.FUNC_ONSTOPCHARGE, epCode, epGunNo, userIdentity, extra, ret, errorCode});

        PhoneClient phoneClient = CachePhoneService.getPhoneClientByAccountId(Integer.valueOf(userIdentity));
        if (phoneClient == null) {
            logger.error(LogUtil.getExtLog("phoneClient is null"));
            return;
        }

        PhoneService.sendMessage(phoneClient.getChannel(), PhoneConstant.D_STOP_CHARGE, ret, errorCode);
    }

    public void onQueryOrderInfo(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int ret, int errorCode) {
    }

    /**
     * 充电实时数据（usrGate->phone）
     */
    public void onRealData(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, Map<String, Object> realData) {
        logger.info(LogUtil.addBaseExtLog("extra|data"),
                new Object[]{LogConstants.FUNC_ONREALDATA, epCode, epGunNo, userIdentity, extra, realData});

        PhoneClient phoneClient = CachePhoneService.getPhoneClientByAccountId(Integer.valueOf(userIdentity));
        if (phoneClient == null) {
            logger.error(LogUtil.getExtLog("phoneClient is null"));
            return;
        }

        byte[] data = PhoneProtocol.do_real_charge_info(realData);
        PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
    }

    /**
     * 消费记录（usrGate->phone）
     */
    public void onChargeOrder(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, Map<String, Object> data) {
        logger.info(LogUtil.addBaseExtLog("extra|data"),
                new Object[]{LogConstants.FUNC_ONCHARGEORDER, epCode, epGunNo, userIdentity, extra, data});

        PhoneClient phoneClient = CachePhoneService.getPhoneClientByAccountId(Integer.valueOf(userIdentity));
        if (phoneClient == null) {
            logger.error(LogUtil.getExtLog("phoneClient is null"));
            return;
        }

        String chargeOrder = (String) data.get("orderNo");
        long lst = (long) data.get("st");
        long let = (long) data.get("et");

        int st = (int) lst;
        int et = (int) let;

        int totalMeterNum = (int) data.get("elect");
        int totalAmt = (int) data.get("elect_money");
        int serviceAmt = (int) data.get("service_money");
        int pkEpId = (int) data.get("pkEpId");

        int version = phoneClient.getVersion();
        int couPonAmt = 0;
        int userFirst = 0;
        int realCouPonAmt = 0;
        if (version >= 2) {
            couPonAmt = (int) data.get("New_conpon");
            userFirst = (int) data.get("Conpon_face_value");
            realCouPonAmt = (int) data.get("Conpon_discount_value");
        }

        byte[] extraData = PhoneProtocol.do_consume_record((short) version, chargeOrder, st, et, totalMeterNum, totalAmt, serviceAmt,
                pkEpId, userFirst, couPonAmt, realCouPonAmt);

        PhoneMessageSender.sendRepeatMessage(phoneClient.getChannel(), extraData, chargeOrder, phoneClient.getVersion());
    }

    /**
     * 枪与车连接状态变化通知事件（usrGate->phone）
     */
    public void onGunLinkStatusChange(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int status) {
        logger.info(LogUtil.addBaseExtLog("extra|status"),
                new Object[]{LogConstants.FUNC_GUNLINK_STATUS, epCode, epGunNo, userIdentity, extra, status});

        PhoneClient phoneClient = CachePhoneService.getPhoneClientByAccountId(Integer.valueOf(userIdentity));
        if (phoneClient == null) {
            logger.error(LogUtil.getExtLog("phoneClient is null"));
            return;
        }

        byte[] hmsTime = NetUtils.timeToByte();
        int gunStatus = status;
        gunStatus += 1;
        byte[] data = PhoneProtocol.do_gun2car_status(gunStatus, PhoneConstant.D_GUN_CAR_STATUS, hmsTime);

        String messagekey = userIdentity + PhoneConstant.D_GUN_CAR_STATUS;

        PhoneMessageSender.sendRepeatMessage(phoneClient.getChannel(), data, messagekey, phoneClient.getVersion());
    }

    /**
     * 枪工作状态变化通知事件（usrGate->phone）
     */
    public void onGunWorkStatusChange(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int status) {
        logger.info(LogUtil.addBaseExtLog("extra|status"),
                new Object[]{LogConstants.FUNC_GUNWORK_STATUS, epCode, epGunNo, userIdentity, extra, status});

        PhoneClient phoneClient = CachePhoneService.getPhoneClientByAccountId(Integer.valueOf(userIdentity));
        if (phoneClient == null) {
            if (status == 3 && !"".equals(extra)) {
                jmsgChargeStat(Integer.valueOf(userIdentity), epCode, epGunNo, status);
            } else {
                logger.error(LogUtil.getExtLog("phoneClient is null"));
            }
            return;
        }

        byte[] hmsTime = NetUtils.timeToByte();
        byte[] data = PhoneProtocol.do_gun2car_status(status, PhoneConstant.D_GUN_WORK_STATUS, hmsTime);

        String messagekey = userIdentity + PhoneConstant.D_GUN_WORK_STATUS;

        PhoneMessageSender.sendRepeatMessage(phoneClient.getChannel(), data, messagekey, phoneClient.getVersion());
    }

    public void jmsgChargeStat(int userId, String epCode, int epGunNo, int status) {
        TblJpush ju = RateServiceImpl.getJpushInfo(userId);
        if (ju == null) {
            logger.error(LogUtil.addExtLog("msgChargeNotic do not find userId|epCode|epGunNo"), new Object[]{userId, epCode, epGunNo});
            return;
        }

        logger.info(LogUtil.addExtLog("msgChargeNotic userId|epCode|epGunNo"), new Object[]{userId, epCode, epGunNo});

        String msg = String.format("您在电桩{}枪口{}上的轮充等待，已经开始充电。", epCode, epGunNo);

        Map<String, String> extras = new HashMap<>();
        extras.put("msg", msg);
        extras.put("epCode", "" + epCode);
        extras.put("epGunNo", "" + epGunNo);
        extras.put("title", "轮充等待开始充电");
        extras.put("tm", "" + DateUtil.getCurrentSeconds());

        JPushUtil.point2point("开始充电", msg, extras, ju.getJpushRegistrationid(), ju.getJpushDevicetype());
    }
}
