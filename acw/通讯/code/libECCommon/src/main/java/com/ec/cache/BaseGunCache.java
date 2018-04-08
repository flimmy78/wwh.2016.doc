package com.ec.cache;

import com.ec.constants.EpConstants;
import com.ec.constants.ErrorCodeConstants;
import com.ec.constants.GunConstants;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseGunCache {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(BaseGunCache.class.getName()));

    public BaseGunCache(){
    }

    public BaseGunCache(String epCode, int epGunNo) {
        this.epCode = epCode;
        this.epGunNo = epGunNo;

        lastUPTime = 0;//手机更新时间
        currentType = 0;
    }

    protected int pkEpId;

    protected String epCode;

    protected int epGunNo;

    protected int pkEpGunId;

    protected int currentType;

    protected int status;

    protected AuthUserCache auth;

    protected ChargeCache chargeCache;

    protected boolean isNeedFronzeAmt;

    protected long lastUPTime;//手机更新时间

    public int getPkEpGunId() {
        return pkEpGunId;
    }

    public void setPkEpGunId(int pkEpGunId) {
        this.pkEpGunId = pkEpGunId;
    }

    public int getPkEpId() {
        return pkEpId;
    }

    public int getCurrentType() {
        return currentType;
    }

    public void setCurrentType(int currentType) {
        this.currentType = currentType;
    }

    public int getCurUserId() {

        if (chargeCache != null && chargeCache.getUserId() > 0)
            return chargeCache.getUserId();

        if (auth != null && auth.getUsrId() > 0)
            return auth.getUsrId();

        return 0;
    }

    public void setPkEpId(int pkEpId) {
        this.pkEpId = pkEpId;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public ChargeCache getChargeCache() {
        return chargeCache;
    }

    public void setChargeCache(ChargeCache chargeCache) {
        this.chargeCache = chargeCache;
    }

    public boolean isNeedFronzeAmt() {
        return isNeedFronzeAmt;
    }

    public void setNeedFronzeAmt(boolean isNeedFronzeAmt) {
        this.isNeedFronzeAmt = isNeedFronzeAmt;
    }

    public int checkSingleYx(int value) {
        int ret = 0;
        if (value != 0 && value != 1) {
            ret = -1;
        }
        return ret;
    }

    public AuthUserCache getAuth() {
        return auth;
    }

    public void setAuth(AuthUserCache auth) {
        this.auth = auth;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("EpGunCache\n");
        sb.append("电桩pkId = ").append(this.getPkEpId()).append("\n");
        sb.append("电桩编号 = ").append(this.getEpCode()).append("\n");

        sb.append("枪口pkId = ").append(this.pkEpGunId).append("\n");
        sb.append("枪口编号 = ").append(this.epGunNo).append("\n");

        String sTime = DateUtil.StringYourDate(DateUtil.toDate(lastUPTime * 1000));
        sb.append("手机充电信息更新时间  = ").append(sTime).append("\n");


        if (this.chargeCache == null) {
            sb.append("无充电\n\r\n");
        } else {
            sb.append(this.chargeCache.toString()).append("\n");
        }

        if (this.auth == null) {
            sb.append("无鉴权用户\n");
        } else {
            sb.append(this.auth.toString()).append("\n");
        }


        return sb.toString();
    }

    public short getGunStatus() {
        short pos = 0;

        if (status == GunConstants.EP_GUN_STATUS_CHARGE)
            pos = 6;
        else if (status == GunConstants.EP_GUN_STATUS_BESPOKE_LOCKED)
            pos = 3;
        else if (status == GunConstants.EP_GUN_STATUS_EP_OPER) {
            pos = 5;// 等待插枪
        } else if (status == GunConstants.EP_GUN_STATUS_WAIT_CHARGE || status == GunConstants.EP_GUN_STATUS_TIMER_CHARGE) {
            pos = 17;// 等待充电
        }

        return pos;
    }

    public String getGunStatusDesc() {
        switch (status) {
            case GunConstants.EP_GUN_STATUS_IDLE:
                return "空闲";
            case GunConstants.EP_GUN_STATUS_EP_INIT:
                return "电桩初始化中";
            case GunConstants.EP_GUN_STATUS_BESPOKE_LOCKED:
                return "预约锁定中";
            case GunConstants.EP_GUN_STATUS_CHARGE:
                return "充电中";
            case GunConstants.EP_GUN_STATUS_STOP_USE:
                return "停用";
            case GunConstants.EP_GUN_STATUS_EP_OPER:
                return "电桩有人使用中";

            case GunConstants.EP_GUN_STATUS_USER_AUTH:
                return "用户占用";
            case GunConstants.EP_GUN_STATUS_SETTING:
                return "设置界面";
            case GunConstants.EP_GUN_STATUS_SELECT_CHARGE_MODE:
                return "充电模式选择";
            case GunConstants.EP_GUN_STATUS_EP_UPGRADE:
                return "升级中";
            case GunConstants.EP_GUN_STATUS_OFF_LINE:
                return "离线状态";

            default:
                return "未知状态(" + status + ")";
        }
    }

    public int canCharge(int startChargeStyle, int chargingUsrId, boolean init) {
        //先判断业务
        if (status == GunConstants.EP_GUN_STATUS_CHARGE ||
                status == GunConstants.EP_GUN_STATUS_BESPOKE_LOCKED ||
                status == GunConstants.EP_GUN_STATUS_USER_AUTH) {
            int curUsrId = getCurUserId();

            if (curUsrId <= 0) {
                logger.error(LogUtil.addExtLog("innormal,status|curUsrId"), status, curUsrId);
                return 0;
            }

            if (status == GunConstants.EP_GUN_STATUS_CHARGE) {
                if (curUsrId != chargingUsrId) {

                    return ErrorCodeConstants.EPE_OTHER_CHARGING;
                } else {
                    if (!init)
                        return ErrorCodeConstants.EPE_REPEAT_CHARGE;
                }
            } else if (status == GunConstants.EP_GUN_STATUS_BESPOKE_LOCKED) {
                if (curUsrId != chargingUsrId) {
                    return ErrorCodeConstants.EPE_OTHER_BESP;
                }
                return 0;
            } else
                return ErrorCodeConstants.USED_GUN;
        }

        return canCharge(startChargeStyle);
    }

    public int canCharge(int startChargeStyle) {
        if (status == GunConstants.EP_GUN_STATUS_SETTING) {
            return ErrorCodeConstants.EPE_IN_EP_OPER;
        }
        if (status == GunConstants.EP_GUN_STATUS_EP_OPER && startChargeStyle == EpConstants.CHARGE_TYPE_QRCODE) {
            return ErrorCodeConstants.EPE_IN_EP_OPER;
        }
        if (status == GunConstants.EP_GUN_STATUS_WAIT_CHARGE) {
            return ErrorCodeConstants.EPE_IN_EP_OPER;
        }
        if (status == GunConstants.EP_GUN_STATUS_EP_UPGRADE) {
            return ErrorCodeConstants.EP_UPDATE;
        }
        if (status > 30 ||
                status == GunConstants.EP_GUN_STATUS_EP_INIT ||
                status == GunConstants.EP_GUN_STATUS_OFF_LINE ||
                status == GunConstants.EP_GUN_STATUS_STOP_USE) {
            return ErrorCodeConstants.EPE_GUN_FAULT;
        }
        return 0;
    }

}