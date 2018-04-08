package com.ec.epcore.cache;

import com.cooperate.CooperateFactory;
import com.ec.cache.BaseEPCache;
import com.ec.constants.ErrorCodeConstants;
import com.ec.constants.UserConstants;
import com.ec.epcore.net.client.EpCommClient;
import com.ec.epcore.net.codec.EpEncoder;
import com.ec.epcore.sender.EpMessageSender;
import com.ec.epcore.service.CacheService;
import com.ec.net.proto.Iec104Constant;
import com.ec.net.proto.WmIce104Util;

public class ElectricPileCache extends BaseEPCache {

    private EqVersionCache versionCache; //电桩实际版本和升级版本信息

    private boolean meterFlag;

    public ElectricPileCache(String epCode) {
        super(epCode);

        versionCache = new EqVersionCache();
        meterFlag = false;
    }

    public EqVersionCache getVersionCache() {
        return versionCache;
    }

    public void setVersionCache(EqVersionCache versionCache) {
        this.versionCache = versionCache;
    }

    public boolean isMeterFlag() {
        return meterFlag;
    }

    public void setMeterFlag(boolean meterFlag) {
        this.meterFlag = meterFlag;
    }

    public void onEpCommClientDelete() {
        for (int i = 1; i <= gunNum; i++) {
            EpGunCache loopEpGunCache = (EpGunCache) CacheService.getEpGunCache(code, i);
            loopEpGunCache.setConcentratorId(this.getConcentratorId());
            loopEpGunCache.onNetStatus(0);
            loopEpGunCache.setEpNetObject(null);
        }
    }

    public boolean updateNetObject() {

        for (int i = 1; i <= gunNum; i++) {
            EpGunCache gunCache = (EpGunCache) CacheService.getEpGunCache(code, i);

            if (gunCache != null) {
                gunCache.setConcentratorId(this.getConcentratorId());
                CacheService.putEpGunCache(gunCache);

                gunCache.setEpNetObject(epNetObject);
            } else {
                return false;
            }
        }

        return true;
    }

    public boolean initGuns(int bootLoader) {

        for (int i = 1; i <= gunNum; i++) {
            EpGunCache gunCache = (EpGunCache) CacheService.getEpGunCache(code, i);
            if (gunCache == null)
                gunCache = new EpGunCache();
            gunCache.setEpGunNo(i);
            gunCache.setEpCode(code);
            gunCache.setPkEpId(this.pkEpId);

            if (gunCache.init(this, i, bootLoader)) {
                gunCache.setConcentratorId(this.getConcentratorId());
                CacheService.putEpGunCache(gunCache);

                gunCache.setEpNetObject(epNetObject);
            } else {
                return false;
            }
        }

        return true;
    }

    public void onNetStatus(int epStatus) {
        boolean canOperate = checkOrgOperate(UserConstants.ORG_EC);
        for (int i = 1; i <= gunNum; i++) {
            EpGunCache loopEpGunCache = (EpGunCache) CacheService.getEpGunCache(code, i);
            loopEpGunCache.onNetStatus(epStatus);
            if (canOperate) loopEpGunCache.handleOffSignleOrgNo(UserConstants.ORG_EC);
        }
    }

    public int callEpAction(int type, int time, float lng, float lag) {
        EpCommClient commClient = (EpCommClient) getEpNetObject();
        if (commClient == null) {
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        if (!commClient.isComm()) {
            return ErrorCodeConstants.EP_UNCONNECTED;

        }
        byte[] data = EpEncoder.do_near_call_ep(this.code, type, time);

        byte[] cmdTimes = WmIce104Util.timeToByte();

        EpMessageSender.sendMessage(commClient, 0, 0, Iec104Constant.C_NEAR_CALL_EP, data, cmdTimes, commClient.getVersion());

        return 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ElectricPileCache");

        sb.append("{电桩pkEpId = ").append(pkEpId).append("}\n");
        sb.append("{电桩名称 = ").append(name).append("}\n");
        sb.append("{电桩编号 = ").append(code).append("}\n");
        sb.append("{枪口数量 = ").append(gunNum).append("}\n");
        sb.append("{集中器pkId = ").append(concentratorId).append("}\n");
        sb.append("{在站中序号 = ").append(stationIndex).append("}\n");
        sb.append("{epType(电桩类型) = ").append(epType).append(getEpTypeDesc()).append("}\n");

        switch (currentType) {
            case 5:
                sb.append("{电桩类型 = ").append("5直流").append("}\n");
                break;
            case 14:
                sb.append("{电桩类型 = ").append("14交流").append("}\n");
                break;
            default:
                sb.append("{电桩类型 = ").append(currentType).append("未知").append("}\n");
                break;

        }
        sb.append("{gateid = ").append(gateid).append("}\n");
        sb.append("{产品ID = ").append(typeSpanId).append("}\n");
        sb.append("{地址 = ").append(address).append("}\n");
        sb.append("{省代码 = ").append(ownProvinceCode).append("}\n");
        sb.append("{市代码 = ").append(ownCityCode).append("}\n");

        sb.append("{公司标识 = ").append(company_number).append("}\n\r\n");

        sb.append("{费率id = ").append(rateid).append("}\n");
        sb.append("{最大临时充电次数 = ").append(tempChargeMaxNum).append("}\n");

        return sb.toString();

    }

    public boolean checkOrgOperate(int orgNo) {
        if (!CooperateFactory.isCooperate(orgNo)) return true;

        //合作方电桩过滤
        return canOrgOperate(orgNo);
    }
}


