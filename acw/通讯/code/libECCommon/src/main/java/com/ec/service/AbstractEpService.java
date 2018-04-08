package com.ec.service;

import com.ec.cache.BaseEPCache;
import com.ec.constants.EpConstants;
import com.ec.constants.ErrorCodeConstants;
import com.ec.service.impl.EpServiceImpl;
import com.ormcore.model.CompanyRela;
import com.ormcore.model.TblElectricPile;

import java.util.List;

public class AbstractEpService {
    /**
     * 包含了所有实时属性，然后覆盖
     *
     * @param epCode 电桩编码
     * @return 电桩缓存
     */
    public static BaseEPCache getEpCacheFromDB(String epCode) {
        TblElectricPile dbEp = EpServiceImpl.getEpFromDB(epCode);
        if (dbEp == null) {
            AbstractCacheService.removeEpCache(epCode);
            return null;
        }

        BaseEPCache epCache = convertCache(dbEp);
        AbstractCacheService.addEpCache(epCache);

        return epCache;
    }

    public static BaseEPCache getEpCache(String epCode) {
        BaseEPCache epCache = AbstractCacheService.getEpCache(epCode);
        if (epCache != null)
            return epCache;

        return AbstractEpService.getEpCacheFromDB(epCode);
    }

    public static BaseEPCache convertCache(TblElectricPile dbEp) {
        BaseEPCache epCache = AbstractCacheService.getEpCache(dbEp.getEpCode());
        if (epCache == null) epCache = new BaseEPCache(dbEp.getEpCode());

        epCache.setPkEpId(dbEp.getPkEpId());
        epCache.setCode(dbEp.getEpCode());
        epCache.setName(dbEp.getEpName());
        epCache.setCurrentType(dbEp.getCurrentType());
        epCache.setGunNum(dbEp.getEpGunNum());

        epCache.setConcentratorId(dbEp.getStationId());
        epCache.setStationIndex(dbEp.getStationIndex());
        epCache.setRateid(dbEp.getRateid());
        epCache.setGateid(dbEp.getGateid());
        epCache.setCompany_number(dbEp.getCompany_number());
        epCache.setTypeSpanId(dbEp.getEpTypeSpanId());
        epCache.setOwnCityCode(dbEp.getElPiOwnCityCode());
        epCache.setOwnProvinceCode(dbEp.getElPiOwnProvinceCode());
        epCache.setState(dbEp.getElpiState());
        epCache.setDeleteFlag(dbEp.getDeleteFlag());
        //epCache.setNetStatus(dbEp.getComm_status());
        getOrgAuth(epCache);

        return epCache;
    }

    public static void getOrgAuth(BaseEPCache epCache)
    {
        List<CompanyRela> companyRelaList = EpServiceImpl.getCompanyRela(epCache.getPkEpId());
        epCache.setCompanyRelaList(companyRelaList);
    }

    public static BaseEPCache getEpRealStatus(BaseEPCache epCache)
    {
        if (epCache == null) {
            return null;
        }

        TblElectricPile dbEp = EpServiceImpl.getEpFromDB(epCache.getCode());
        if (dbEp == null) {
            AbstractCacheService.removeEpCache(epCache.getCode());
            return null;
        }

        epCache.setCompany_number(dbEp.getCompany_number());
        epCache.setState(dbEp.getElpiState());
        epCache.setDeleteFlag(dbEp.getDeleteFlag());

        return epCache;
    }

    public static int getCurrentType(String epCode) {
        BaseEPCache epClient = AbstractCacheService.getEpCache(epCode);
        if (epClient == null) {
            return -1;
        }
        return epClient.getCurrentType();
    }

    public static int checkEpCache(BaseEPCache epCache) {
        if (epCache == null) return ErrorCodeConstants.EP_UNCONNECTED;

        if (epCache.getState() == EpConstants.EP_STATUS_OFFLINE) {
            return ErrorCodeConstants.EP_PRIVATE;
        } else if (epCache.getState() < EpConstants.EP_STATUS_OFFLINE) {
            return ErrorCodeConstants.EP_NOT_ONLINE;
        }
        if (epCache.getDeleteFlag() != 0) {
            return ErrorCodeConstants.EP_NOT_ONLINE;
        }

        return 0;
    }
}
