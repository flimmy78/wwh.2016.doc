package com.ec.cache;

import com.ec.constants.EpConstants;
import com.ec.constants.ErrorCodeConstants;
import com.ec.netcore.client.ECTcpClient;
import com.ormcore.model.CompanyRela;

import java.util.List;

public class BaseEPCache {

    protected Integer pkEpId; //
    protected String code; //
    protected String name;
    protected int concentratorId;//站索引

    protected int stationIndex;//站索引
    protected int gunNum;
    protected int currentType; //
    protected int epType;
    protected int rateid; //
    protected int gateid; //
    protected Integer typeSpanId; // 产品类型Id

    protected long lastUseTime;

    protected int isSupportCarPlace;
    protected int isSupportBmsComm;
    protected int isSupportGunLock;
    protected int isSupportGunSit;

    protected String address;
    protected int company_number; //

    protected int tempChargeMaxNum;

    protected int state;//电桩状态
    protected int deleteFlag;//删除标识

    protected String ownCityCode;//市code
    protected String ownProvinceCode;//省code

    protected ECTcpClient epNetObject;//电桩网络连接

    protected List<CompanyRela> companyRelaList;

    public BaseEPCache(String epCode) {
        gateid = 0;
        pkEpId = 0; //
        code = epCode; //
        name = "";
        concentratorId = 0;//站索引

        stationIndex = 0;//站索引
        gunNum = 0;
        currentType = 0; //
        epType = 0;
        rateid = 0; //
        gateid = 0; //
        lastUseTime = 0;
        isSupportCarPlace = 0;
        isSupportBmsComm = 0;
        isSupportGunLock = 0;
        isSupportGunSit = 0;

        address = "";
        typeSpanId = 0;

        state = 0;
        deleteFlag = 0;

        ownCityCode = "";
        ownProvinceCode = "";

        tempChargeMaxNum = 0;
        companyRelaList = null;
    }


    public int getTempChargeMaxNum() {
        return tempChargeMaxNum;
    }

    public void setTempChargeMaxNum(int tempChargeMaxNum) {
        this.tempChargeMaxNum = tempChargeMaxNum;
    }

    public String getOwnCityCode() {
        return ownCityCode;
    }

    public void setOwnCityCode(String ownCityCode) {
        this.ownCityCode = ownCityCode;
    }

    public String getOwnProvinceCode() {
        return ownProvinceCode;
    }

    public void setOwnProvinceCode(String ownProvinceCode) {
        this.ownProvinceCode = ownProvinceCode;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(int deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public Integer getTypeSpanId() {
        return typeSpanId;
    }

    public void setTypeSpanId(Integer typeSpanId) {
        this.typeSpanId = typeSpanId;
    }

    public ECTcpClient getEpNetObject() {
        return epNetObject;
    }

    public void setEpNetObject(ECTcpClient epNetObject) {
        this.epNetObject = epNetObject;
    }

    public Integer getEpType() {
        return epType;
    }

    public void setEpType(Integer epType) {
        this.epType = epType;
    }

    public Integer getIsSupportCarPlace() {
        return isSupportCarPlace;
    }

    public void setIsSupportCarPlace(Integer isSupportCarPlace) {
        this.isSupportCarPlace = isSupportCarPlace;
    }

    public Integer getIsSupportBmsComm() {
        return isSupportBmsComm;
    }

    public void setIsSupportBmsComm(Integer isSupportBmsComm) {
        this.isSupportBmsComm = isSupportBmsComm;
    }

    public Integer getIsSupportGunLock() {
        return isSupportGunLock;
    }

    public void setIsSupportGunLock(Integer isSupportGunLock) {
        this.isSupportGunLock = isSupportGunLock;
    }

    public Integer getIsSupportGunSit() {
        return isSupportGunSit;
    }

    public void setIsSupportGunSit(Integer isSupportGunSit) {
        this.isSupportGunSit = isSupportGunSit;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGunNum() {
        return gunNum;
    }

    public void setGunNum(int gunNum) {
        this.gunNum = gunNum;
    }

    public void setPkEpId(int pkEpId) {
        this.pkEpId = pkEpId;
    }

    public long getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(long lastUseTime) {
        this.lastUseTime = lastUseTime;
    }


    public int getCurrentType() {
        return currentType;
    }

    public void setCurrentType(int currentType) {
        this.currentType = currentType;
    }

    public int getRateid() {
        return rateid;
    }

    public void setRateid(int rateid) {
        this.rateid = rateid;
    }

    public int getPkEpId() {
        return pkEpId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getConcentratorId() {
        return concentratorId;
    }

    public void setConcentratorId(int concentratorId) {
        this.concentratorId = concentratorId;
    }

    public int getStationIndex() {
        return stationIndex;
    }

    public void setStationIndex(int stationIndex) {
        this.stationIndex = stationIndex;
    }

    public int getGateid() {
        return gateid;
    }

    public void setGateid(int gateid) {
        this.gateid = gateid;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<CompanyRela> getCompanyRelaList() {
        return companyRelaList;
    }

    public void setCompanyRelaList(List<CompanyRela> companyRelaList) {
        this.companyRelaList = companyRelaList;
    }

    public int getCompany_number() {
        return company_number;
    }

    public void setCompany_number(int company_number) {
        this.company_number = company_number;
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

    public int canNearCall() {
        if (state == EpConstants.EP_STATUS_OFFLINE) {
            return ErrorCodeConstants.EP_PRIVATE;
        } else if (state < EpConstants.EP_STATUS_OFFLINE) {
            return ErrorCodeConstants.EP_NOT_ONLINE;
        }
        if (deleteFlag != 0) {
            return ErrorCodeConstants.EP_NOT_ONLINE;
        }

        return 0;
    }

    public int canCharge(ChargeCardCache card, int epGunNo) {
        int pkUserCard = (card != null) ? card.getId() : 0;
        int cardType = (card != null) ? card.getCardType() : 0;
        int cardOrgNo = (card != null) ? card.getCompanyNumber() : 0;
        return canCharge(pkUserCard, cardType, cardOrgNo, epGunNo);
    }

    /**
     * @param pkUserCard,如果没有绑卡，那么不在线的桩不能充电
     * @return
     */
    public int canCharge(int pkUserCard, int cardType, int cardOrgNo, int epGunNo) {
        if (currentType != EpConstants.EP_DC_TYPE && currentType != EpConstants.EP_AC_TYPE) {
            return ErrorCodeConstants.EPE_CHARGE_STYLE;
        }

        if (epGunNo < 1 || epGunNo > getGunNum()) {
            return ErrorCodeConstants.INVALID_EP_GUN_NO;//
        }

        if (state == EpConstants.EP_STATUS_OFFLINE) {
            if (pkUserCard == 0) {
                return ErrorCodeConstants.EP_PRIVATE;
            } else {
                if (cardType == EpConstants.CARD_NORMAL || cardType == EpConstants.CARD_THIRD_NORMAL) {
                    return ErrorCodeConstants.EP_PRIVATE;
                } else {
                    if (company_number != cardOrgNo) {
                        return ErrorCodeConstants.EP_PRIVATE;
                    }
                }
            }
        } else if (state < EpConstants.EP_STATUS_OFFLINE) {
            return ErrorCodeConstants.EP_NOT_ONLINE;
        }
        if (deleteFlag != 0) {
            return ErrorCodeConstants.EP_NOT_ONLINE;
        }

        return 0;
    }

    public int canAuth() {
        if (state < EpConstants.EP_STATUS_OFFLINE) {
            return EpConstants.E_CARD_NOT_FIND_EP;
        }
        if (deleteFlag != 0) {
            return EpConstants.E_CARD_NOT_FIND_EP;
        }
        return 0;
    }

    public boolean canOrgOperate(int orgNo) {
        if (companyRelaList == null) return false;
        for (CompanyRela companyRela : companyRelaList)
            if (companyRela.getCpyCompanyNumber() == orgNo) return true;

        return false;
    }

    public String getEpTypeDesc() {

        String desc;
        switch (this.epType) {
            case 0:
                desc = "落地式";
                break;
            case 1:
                desc = "壁挂式";
                break;
            case 2:
                desc = "拉杆式";
                break;
            case 3:
                desc = "便携式";
                break;

            default:
                desc = "未知";
                break;
        }
        return desc;
    }
}