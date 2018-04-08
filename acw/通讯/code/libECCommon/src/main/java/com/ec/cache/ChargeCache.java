package com.ec.cache;


import com.ec.constants.ChargeRecordConstants;
import com.ec.utils.DateUtil;
import com.ec.utils.NumUtil;
import com.ormcore.model.RateInfo;


public class ChargeCache {

    private long st;//精确到秒
    private long et;//精确到秒

    private String epCode;
    private int epGunNo;

    private short stopCause;

    private short startChargeStyle;//发起充电的方式 ,1 ：二维码，2：用户名密码；3:卡

    private UserOrigin userOrigin;

    private RateInfo rateInfo;

    private String bespNo;

    private long lastModifyTime;//用来判断超时

    private int status;//【定义在:ChargeStatus】

    private int payMode;

    private String chargeSerialNo;
    private String chOrCode;
    private int pkOrderId;

    private int fronzeAmt;

    private int userId;
    private String usrAccount;

    private ChargeCardCache card;

    private String carVin;

    private int chargingMethod;

    private String userIdentity; //用户标识
    private String token;//可以是第三方的session_id,也可以是第三方的流水号

    private long lastCmdTime; //hly


    public String getChOrCode() {
        return chOrCode;
    }

    public void setChOrCode(String chOrCode) {
        this.chOrCode = chOrCode;
    }

    public int getFronzeAmt() {
        return fronzeAmt;
    }

    public void setFronzeAmt(int fronzeAmt) {
        this.fronzeAmt = fronzeAmt;
    }

    public String getChargeSerialNo() {
        return chargeSerialNo;
    }

    public void setChargeSerialNo(String chargeSerialNo) {
        this.chargeSerialNo = chargeSerialNo;
    }


    public long getLastCmdTime() {//hly
        return lastCmdTime;
    }

    public void setLastCmdTime(long lastCmdTime) { //hly
        this.lastCmdTime = lastCmdTime;
    }

    public short getStopCause() {
        return stopCause;
    }

    public void setStopCause(short stopCause) {
        this.stopCause = stopCause;
    }


    public void setLastModifyTime(long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }


    public String getBespNo() {
        return bespNo;
    }

    public long getLastModifyTime() {
        return lastModifyTime;
    }

    public void setBespNo(String bespNo) {
        this.bespNo = bespNo;
    }

    public long getSt() {
        return st;
    }

    public void setSt(long st) {
        this.st = st;
    }

    public long getEt() {
        return et;
    }

    public void setEt(long et) {
        this.et = et;
    }


    public short getStartChargeStyle() {
        return startChargeStyle;
    }

    public void setStartChargeStyle(short startChargeStyle) {
        this.startChargeStyle = startChargeStyle;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public UserOrigin getUserOrigin() {
        return userOrigin;
    }

    public void setUserOrigin(UserOrigin userOrigin) {
        this.userOrigin = userOrigin;
    }

    private void init() {
        chargeSerialNo = "";
        chOrCode = "";
        card = null;
        token = "";
        userIdentity = "";
    }

    public ChargeCache() {
        init();
    }


    public String getEpCode() {
        return epCode;
    }

    public void setEpCode(String epCode) {
        this.epCode = epCode;
    }

    public int getEpGunNo() {
        return epGunNo;
    }

    public void setEpGunNo(int epGunNo) {
        this.epGunNo = epGunNo;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPkUserCard() {
        if (card == null)
            return 0;
        return card.getId();
    }

    public int getPkOrderId() {
        return pkOrderId;
    }

    public void setPkOrderId(int pkOrderId) {
        this.pkOrderId = pkOrderId;
    }

    public ChargeCardCache getCard() {
        return card;
    }

    public void setCard(ChargeCardCache card) {
        this.card = card;
    }

    public String getAccount() {
        return usrAccount;
    }

    public void setAccount(String account) {
        this.usrAccount = account;
    }

    public String getUserIdentity() {
        return userIdentity;
    }

    public void setUserIdentity(String userIdentity) {
        this.userIdentity = userIdentity;
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();

        sb.append("充电信息:\n");
        sb.append(",{电桩编号 = ").append(epCode).append("}\n");
        sb.append(",{枪编号 = ").append(epGunNo).append("}\n");
        sb.append(",{交易流水号 = ").append(chargeSerialNo).append("}\n");
        sb.append(",{订单编号 = ").append(chOrCode).append("}\n");
        sb.append(",{付款方式 = ").append(payMode).append(getPayModeDesc(payMode)).append("}\n");

        sb.append(",{预充金额 = ").append(NumUtil.intToBigDecimal2(fronzeAmt)).append("}\n");

        String sTime = DateUtil.StringYourDate(DateUtil.toDate(st * 1000));

        sb.append(",{开始时间 = ").append(sTime).append("}\n");

        sTime = DateUtil.StringYourDate(DateUtil.toDate(et * 1000));
        sb.append(",{结束时间 = ").append(sTime).append("}\n");
        sb.append(",{停止原因 = ").append(stopCause).append(getStopCauseDesc(stopCause)).append("}\n");
        sb.append(",{发起充电方式 = ").append(startChargeStyle).append(getChargeStyleDesc(startChargeStyle)).append("}\n");

        sb.append(",{用户ID = ").append(userId).append("}\n");
        sb.append(",{用户账号 = ").append(usrAccount).append("}\n");
        sb.append(",{用户标识 = ").append(userIdentity).append("}\n");

        sb.append(card).append("\n");
        sb.append(",{状态 = ").append(status);
        switch (status) {
            case ChargeRecordConstants.CS_STAT_FINISHED:
                sb.append("结算完成").append("}\n");
                break;
            case ChargeRecordConstants.CS_STAT:
                sb.append("结算中").append("}\n");
                break;
            case ChargeRecordConstants.CS_PAUSE_STAT:
                sb.append("临时结算").append("}\n");
                break;
            case ChargeRecordConstants.CS_WAIT_INSERT_GUN:
                sb.append("等待插枪").append("}\n");
                break;
            case ChargeRecordConstants.CS_CHARGE_FAIL:
                sb.append("充电失败").append("}\n");
                break;
            case ChargeRecordConstants.CS_ACCEPT_CONSUMEER_CMD:
                sb.append("等待回复充电命令").append("}\n");
                break;
            case ChargeRecordConstants.CS_CHARGING:
                sb.append("充电中").append("}\n");
                break;
            case ChargeRecordConstants.CS_WAIT_CHARGE:
                sb.append("等待充电").append("}\n");
                break;
            default:
                sb.append("其他").append("}\n");
                break;

        }

        sb.append(",{用户来源=").append(userOrigin).append("}\n");
        sb.append(",{车辆识别码=").append(carVin).append("}\n");

        return sb.toString();
    }

    public String getPayModeDesc(int pmMode) {
        String desc;
        switch (pmMode) {
            case 1:
                desc = "先付费";
                break;
            case 2:
                desc = "后付费";
                break;

            default:
                desc = "未知状态";
                break;
        }
        return desc;
    }

    public String getStopCauseDesc(int stopCause) {
        String desc;
        switch (stopCause) {
            case 1:
                desc = "正常结束";
                break;
            case 2:
                desc = "用户强制结束";
                break;
            case 3:
                desc = "急停";
                break;
            case 4:
                desc = "连接线断掉";
                break;
            case 5:
                desc = "电表异常";
                break;
            case 6:
                desc = "过流停止";
                break;
            case 7:
                desc = "过压停止";
                break;
            case 8:
                desc = "防雷器故障";
                break;
            case 9:
                desc = "接触器故障";
                break;
            case 10:
                desc = "余额不足";
                break;
            case 11:
                desc = "漏电保护器";
                break;
            case 12:
                desc = "自动完成";
                break;
            case 13:
                desc = "BMS通信异常故障";
                break;
            case 14:
                desc = "违规拔枪";
                break;
            case 15:
                desc = "电桩断电";
                break;
            default:
                desc = "未知原因";
                break;
        }
        return desc;
    }

    public String getChargeStyleDesc(int startChargeStyle) {
        String desc;
        switch (startChargeStyle) {
            case 1:
                desc = "二维码充电";
                break;
            case 2:
                desc = "账号充电";
                break;
            case 3:
                desc = "卡充电";
                break;
            default:
                desc = "未知充电方式";
                break;
        }
        return desc;
    }

    public String getChargeStyleSHStop() {
        String desc;
        switch (startChargeStyle) {
            case 1:
                desc = "1";
                break;
            case 3:
                desc = "2";
                break;
            default:
                desc = "3";
                break;
        }
        return desc;
    }

    public String getGunDesc(int states) {
        String desc;
        switch (states) {
            case 0:
                desc = "空闲";
                break;
            case 3:
                desc = "预约中";
                break;
            case 6:
                desc = "充电中";
                break;
            case 9:
                desc = "停用中";
                break;
            default:
                desc = "未知状态";
                break;
        }
        return desc;

    }

    public RateInfo getRateInfo() {
        return rateInfo;
    }

    public void setRateInfo(RateInfo rateInfo) {
        this.rateInfo = rateInfo;
    }

    public int getPayMode() {
        return payMode;
    }

    public void setPayMode(int payMode) {
        this.payMode = payMode;
    }

    public String getCarVin() {
        return carVin;
    }

    public void setCarVin(String carVin) {
        this.carVin = carVin;
    }

    public int getChargingMethod() {
        return chargingMethod;
    }

    public void setChargingMethod(int chargingMethod) {
        this.chargingMethod = chargingMethod;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
