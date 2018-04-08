package com.ec.service.impl;

import com.ec.constants.TimingChargeConstants;
import com.ec.utils.DateUtil;
import com.ormcore.dao.DB;
import com.ormcore.model.*;

import java.util.Date;
import java.util.List;

public class EpServiceImpl {

    public static List<TblEpGateConfig> getEpConfig() {
        TblEpGateConfig cfg = new TblEpGateConfig();
        List<TblEpGateConfig> epGateCfgList = DB.epGateCfgDao.find1(cfg);
        return epGateCfgList;
    }

    public static TblElectricPile getEpFromDB(String epCode) {
        List<TblElectricPile> epList = DB.epClientDao.findResultObject(epCode);
        if (epList == null || epList.size() <= 0) {
            return null;
        }
        return epList.get(0);
    }

    public static void updateAllCommStatus(int gateId) {
        DB.epClientDao.updateAllCommStatus(gateId);
    }

    public static void updateEpCommStatusToDb(int pkEpId,int commStatus,int gateId)
    {
        TblElectricPile updateEp= new TblElectricPile();
        updateEp.setPkEpId(pkEpId);
        updateEp.setComm_status(commStatus);
        updateEp.setGateid(gateId);

        DB.epClientDao.updateCommStatus(updateEp);
    }

    public static void updateEpsCommStatusToDb(int stationId,int commStatus,int gateId)
    {
        TblElectricPile updateEp= new TblElectricPile();
        updateEp.setComm_status(commStatus);
        updateEp.setStationId(stationId);
        updateEp.setGateid(gateId);

        DB.epClientDao.updateCommStatusByStationId(updateEp);
    }

    public static void updateEpRateToDb(int pkEpId,int rateid)
    {
        TblElectricPile tblElectricPile = new TblElectricPile();
        tblElectricPile.setPkEpId(pkEpId);
        tblElectricPile.setRateid(rateid);

        DB.epClientDao.updateRateId(tblElectricPile);
    }

    public static List<TblElectricPile> getEpsByStatus(int concentratorId) {
        List<TblElectricPile> epList = DB.epClientDao.getEpsByStatus(concentratorId);
        if (epList == null || epList.size() <= 0) {
            return null;
        }
        return epList;
    }

    public static List<TblElectricPile> getEpsByStationId(int concentratorId) {
        List<TblElectricPile> epList = DB.epClientDao.getEpsByStationId(concentratorId);
        if (epList == null || epList.size() <= 0) {
            return null;
        }
        return epList;
    }

    public static List<TblElectricPile> getLastUpdate() {
        List<TblElectricPile> epList = DB.epClientDao.getLastUpdate();
        if (epList == null || epList.size() <= 0) {
            return null;
        }
        return epList;
    }

    public static List<TblElectricPile> findResultObjectByCompany(int company_number) {
        List<TblElectricPile> epList = DB.epClientDao.findResultObjectByCompany(company_number);
        if (epList == null || epList.size() <= 0) {
            return null;
        }
        return epList;
    }

    public static List<TblElectricPile> findResultObjectBySpanId(int typeSpanId) {
        List<TblElectricPile> epList = DB.epClientDao.findResultObjectBySpanId(typeSpanId);
        if (epList == null || epList.size() <= 0) {
            return null;
        }
        return epList;
    }

    public static List<TblConcentrator> findConcentratorBySpanId(int typeSpanId) {
        List<TblConcentrator> centList = DB.concentratorDao.findResultObjectBySpanId(typeSpanId);
        if (centList == null || centList.size() <= 0) {
            return null;
        }
        return centList;
    }

    public static TblCompany findCompanyOne(int company_number) {
        List<TblCompany> companyList=DB.companyDao.findone(company_number);
        if(companyList ==null ||companyList.size()<1)
        {
            return null;
        }

        return companyList.get(0);
    }

    public static List<CompanyRela> getCompanyRela(int pkEpId) {
        CompanyRela companyRela = new CompanyRela();
        companyRela.setPkElectricpile(pkEpId);
        List<CompanyRela> crList = DB.companyRelaDao.CompanyRela_custlist(companyRela);
        if (crList == null || crList.size() <= 0) {
            return null;
        }
        return crList;
    }

    public static List<ElectricpileMeternum> getElectricpileMeternum(String epCode) {
        ElectricpileMeternum electricpileMeternum = new ElectricpileMeternum();
        electricpileMeternum.setReadDate(DateUtil.toString(new Date()));
        electricpileMeternum.setEpCode(epCode);
        electricpileMeternum.setTotalMeter("");
        electricpileMeternum.setGun1Meter("");
        electricpileMeternum.setGun2Meter("");
        electricpileMeternum.setGun3Meter("");
        electricpileMeternum.setGun4Meter("");
        List<ElectricpileMeternum> emList = DB.electricpileMeternumDao.ElectricpileMeternum_custlist(electricpileMeternum);
        if (emList == null || emList.size() <= 0) {
            return null;
        }

        return emList;
    }

    public static void insertElectricpileMeternum(String epCode, String tot, String gun1, String gun2, String gun3, String gun4) {
        ElectricpileMeternum electricpileMeternum = new ElectricpileMeternum();
        electricpileMeternum.setReadDate(DateUtil.toString(new Date()));
        electricpileMeternum.setEpCode(epCode);
        electricpileMeternum.setTotalMeter(tot);
        electricpileMeternum.setGun1Meter(gun1);
        electricpileMeternum.setGun2Meter(gun2);
        electricpileMeternum.setGun3Meter(gun3);
        electricpileMeternum.setGun4Meter(gun4);

        DB.electricpileMeternumDao.ElectricpileMeternum_insert(electricpileMeternum);
    }

    public static List<ElectricpileWorkarg> getElectricpileWorkarg(String epCode) {
        return getElectricpileWorkarg(epCode, null);
    }

    public static List<ElectricpileWorkarg> getElectricpileWorkarg(String epCode, Integer argId) {
        ElectricpileWorkarg epWorkarg = new ElectricpileWorkarg();
        epWorkarg.setEpCode(epCode);
        epWorkarg.setArgId(argId);
        List<ElectricpileWorkarg> ewList = DB.electricpileWorkargDao.ElectricpileWorkarg_custlist(epWorkarg);
        if (ewList == null || ewList.size() <= 0) {
            return null;
        }

        return ewList;
    }

    public static int updateElectricpileWorkarg(String epCode) {
        ElectricpileWorkarg epWorkarg = new ElectricpileWorkarg();
        epWorkarg.setEpCode(epCode);
        epWorkarg.setIssuedStatus(TimingChargeConstants.ISSUED_TIMING_CHARGE_STATUS_UNREC);

        return DB.electricpileWorkargDao.ElectricpileWorkarg_update(epWorkarg);
    }

    public static void insertWorkArg(String epCode, int id, String val, int status) {
        ElectricpileWorkarg epWorkarg = new ElectricpileWorkarg();
        epWorkarg.setEpCode(epCode);
        epWorkarg.setArgId(id);
        epWorkarg.setArgValue(val);
        epWorkarg.setIssuedStatus(status);
        DB.electricpileWorkargDao.ElectricpileWorkarg_insert(epWorkarg);
    }

    public static void updateWorkArg(String epCode, int id, int val) {
        updateWorkArg(epCode, id, String.valueOf(val), TimingChargeConstants.ISSUED_TIMING_CHARGE_STATUS_SUCCESS);
    }

    public static void updateWorkArg(String epCode, int id, String val, int status) {
        ElectricpileWorkarg epWorkarg = new ElectricpileWorkarg();
        epWorkarg.setEpCode(epCode);
        epWorkarg.setArgId(id);
        epWorkarg.setArgValue(val);
        epWorkarg.setIssuedStatus(status);
        DB.electricpileWorkargDao.ElectricpileWorkarg_update(epWorkarg);
    }

    public static TblTimingCharge findTimingCharge(String epCode, int timingChargeStatus) {
        TblTimingCharge qryTimingCharge = new TblTimingCharge();
        qryTimingCharge.setElpiElectricPileCode(epCode);
        qryTimingCharge.setTimingChargeStatus(timingChargeStatus);
        qryTimingCharge.setIssuedStatus(TimingChargeConstants.ISSUED_TIMING_CHARGE_STATUS_UNREC);
        List<TblTimingCharge> timingChargeList = DB.timingChargeDao.findTimingCharge(qryTimingCharge);
        if (timingChargeList == null || timingChargeList.size() <= 0) {
            return null;
        }

        return timingChargeList.get(0);
    }

    public static int updateTimingCharge(TblTimingCharge resTimingCharge, int issuedStatus) {
        resTimingCharge.setIssuedStatus(issuedStatus);
        resTimingCharge.setUpdateDate(DateUtil.currentDate());

        return DB.timingChargeDao.updateTimingCharge(resTimingCharge);
    }

}
