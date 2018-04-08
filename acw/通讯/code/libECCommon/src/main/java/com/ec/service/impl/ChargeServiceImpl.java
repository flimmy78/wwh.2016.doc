package com.ec.service.impl;

import com.ec.constants.ChargeOrderConstants;
import com.ormcore.dao.DB;
import com.ormcore.model.*;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class ChargeServiceImpl {

    public static TblChargingrecord getChargingRecord(String chorTransactionnumber) {
        if (StringUtils.isEmpty(chorTransactionnumber)) return null;
        List<TblChargingrecord> chargeList = DB.chargingrecordDao.getByTranNumber(chorTransactionnumber);

        if (chargeList == null || chargeList.size() <= 0)
            return null;

        return chargeList.get(0);
    }

    public static TblChargingrecord getUnFinishedCharge(String epCode, int epGunNo) {
        TblChargingrecord tblQueryChargeRecord = new TblChargingrecord();
        tblQueryChargeRecord.setChreUsingmachinecode(epCode);
        tblQueryChargeRecord.setChreChargingnumber(epGunNo);

        List<TblChargingrecord> chargeList = DB.chargingrecordDao.getUnFinishedCharge(tblQueryChargeRecord);
        if (chargeList == null || chargeList.size() <= 0) {
            return null;
        }
        return chargeList.get(0);
    }

    public static List<TblChargingrecord> getUnFinishedChargeByUsrId(int userId) {
        TblChargingrecord tblQueryChargeRecord = new TblChargingrecord();
        tblQueryChargeRecord.setUserId(userId);

        List<TblChargingrecord> chargeList = DB.chargingrecordDao.getUnFinishedChargeByUsrId(tblQueryChargeRecord);
        if (chargeList == null || chargeList.size() <= 0) {
            return null;
        }
        return chargeList;
    }

    public static void updateUsrGateIp(String chargeSerialNo, String identity)
    {
        TblChargingrecord record = new TblChargingrecord();
        record.setChreTransactionnumber(chargeSerialNo);
        record.setUsrGateIp(identity);
        DB.chargingrecordDao.updateUsrGateIp(record);
    }

    public static void endChargeRecord(String serialNo, int status, Date et, BigDecimal endMeterNum, BigDecimal servicePrice) {
        TblChargingrecord record = new TblChargingrecord();
        record.setChreTransactionnumber(serialNo);
        record.setStatus(status);
        record.setServicePrice(servicePrice);

        String endShowsnumber = String.valueOf(endMeterNum);
        record.setChreEndshowsnumber(endShowsnumber);
        record.setChreEnddate(et);

        // 更新充电记录 记录结束时间和用电度数
        insertEndRecord(record);
    }

    public static int getChargeOrderStatus(String serialNo) {
        String ret = DB.chargeOrderDao.getOrderStatus(serialNo);
        return (ret == null) ? -1 : Integer.parseInt(ret);
    }

    public static int getOrderId(String chorTransactionnumber) {
        List<TblChargingOrder> orderList = DB.chargeOrderDao.findOrderId(chorTransactionnumber);
        if (orderList == null || orderList.size() <= 0)
            return 0;
        return Integer.parseInt(orderList.get(0).getPkChargingorder());
    }

    public static int getUnpayOrder(int userId) {
        return DB.chargeOrderDao.getUnpayOrder(userId);
    }

    public static void updateFailRecordToDb(String serialNo, int status) {
        TblChargingrecord record = new TblChargingrecord();

        record.setChreTransactionnumber(serialNo);
        record.setStatus(status);
        // 新增开始充电记录
        DB.chargingrecordDao.updateFailChargeRecord(record);
    }

    public static void updateChargeRecordStatus(String chargeSerialNo, int status) {
        TblChargingrecord record = new TblChargingrecord();
        record.setChreTransactionnumber(chargeSerialNo);
        record.setStatus(status);

        DB.chargingrecordDao.updateBeginRecordStatus(record);
    }

    public static int updateBeginRecord(TblChargingrecord record) {
        return DB.chargingrecordDao.updateBeginRecord(record);
    }

    public static int insertEndRecord(TblChargingrecord record) {
        return DB.chargingrecordDao.insertEndRecord(record);
    }

    public static int insertBeginRecord(TblChargingrecord record) {
        return DB.chargingrecordDao.insertBeginRecord(record);
    }

    public static int insertFullChargeRecord(TblChargingrecord record) {
        return DB.chargingrecordDao.insertFullChargeRecord(record);
    }

    public static int insertChargeOrder(TblChargingOrder order) {
        return DB.chargeOrderDao.insertChargeOrder(order);
    }

    public static int insertFullChargeOrder(TblChargingOrder order) {
        return DB.chargeOrderDao.insertFullChargeOrder(order);
    }

    public static void updateChargeOrder(String chargeSerialNo) {
        TblChargingOrder tblChargeOrder = new TblChargingOrder();
        tblChargeOrder.setChorTransactionnumber(chargeSerialNo);
        tblChargeOrder.setChorChargingstatus("" + ChargeOrderConstants.ORDER_PAUSE_STAT);
        DB.chargeOrderDao.updateStatus(tblChargeOrder);
    }

    public static void updateChargeOrder(TblChargingOrder order) {
        DB.chargeOrderDao.updateOrder(order);
    }

    public static TblChargeACInfo findChargeACInfo(String epCode, int epGunNo) {
        TblChargeACInfo tblRealData =new TblChargeACInfo();

        tblRealData.setEp_code(epCode);
        tblRealData.setEp_gun_no(epGunNo);

        List<TblChargeACInfo> acList= DB.chargeACInfoDao.findChargeInfo(tblRealData);
        if (acList == null || acList.size() <= 0) {
            return null;
        }
        return acList.get(0);
    }

    public static int insertChargeACInfo(String epCode, int epGunNo)
    {
        TblChargeACInfo tblRealData = new TblChargeACInfo();

        tblRealData.setEp_code(epCode);
        tblRealData.setEp_gun_no(epGunNo);

        return DB.chargeACInfoDao.insert(tblRealData);
    }

    public static TblChargeDCInfo findChargeDCInfo(String epCode, int epGunNo) {
        TblChargeDCInfo tblRealData =new TblChargeDCInfo();

        tblRealData.setEp_code(epCode);
        tblRealData.setEp_gun_no(epGunNo);

        List<TblChargeDCInfo> dcList= DB.chargeDCInfoDao.findChargeInfo(tblRealData);
        if (dcList == null || dcList.size() <= 0) {
            return null;
        }
        return dcList.get(0);
    }

    public static int insertChargeDCInfo(String epCode, int epGunNo)
    {
        TblChargeDCInfo tblRealData = new TblChargeDCInfo();

        tblRealData.setEp_code(epCode);
        tblRealData.setEp_gun_no(epGunNo);

        return DB.chargeDCInfoDao.insert(tblRealData);
    }

    public static void updateChargeACInfo(TblChargeACInfo tblRealData)
    {
        DB.chargeACInfoDao.update(tblRealData);
    }

    public static void updateChargeDCInfo(TblChargeDCInfo tblRealData)
    {
        DB.chargeDCInfoDao.update(tblRealData);
    }

    public static void updateChargeACInfo(String epCode, int epGunNo, BigDecimal bdMeterNum, String serialNo, BigDecimal fronzeAmt, int startTime, int userId)
    {
        TblChargeACInfo chargeInfo = new TblChargeACInfo();

        chargeInfo.setEp_code(epCode);
        chargeInfo.setEp_gun_no(epGunNo);

        chargeInfo.setChargeSerialNo(serialNo);
        chargeInfo.setChargeStartMeterNum(bdMeterNum);
        chargeInfo.setChargeStartTime(startTime);
        chargeInfo.setChargeUserId(userId);
        chargeInfo.setFronzeAmt(fronzeAmt);

        updateChargeACInfo(chargeInfo);
    }

    public static void updateChargeDCInfo(String epCode, int epGunNo, BigDecimal bdMeterNum, String serialNo, BigDecimal fronzeAmt, int startTime, int userId)
    {
        TblChargeDCInfo chargeInfo = new TblChargeDCInfo();

        chargeInfo.setEp_code(epCode);
        chargeInfo.setEp_gun_no(epGunNo);

        chargeInfo.setChargeSerialNo(serialNo);
        chargeInfo.setChargeStartMeterNum(bdMeterNum);
        chargeInfo.setChargeStartTime(startTime);
        chargeInfo.setChargeUserId(userId);
        chargeInfo.setFronzeAmt(fronzeAmt);

        updateChargeDCInfo(chargeInfo);
    }

    public static void addPurchaseHistoryToDB(BigDecimal cost, int type, int userId, int userOrigin,
                                              String content, String epCode, String serialNo, String bespokeNo) {
        TblPurchaseHistory phInfo = new TblPurchaseHistory(new Date(), cost, "", type, userId, userOrigin, content,
                epCode, serialNo, bespokeNo);
        DB.phDao.insertPurchaseRecord(phInfo);
    }

    public static void insertFaultRecord(int stopCause, String epCode, int pkEpId, int epGunNo, String serialNo, Date faultDate) {
        if (stopCause <= 2 || stopCause == 10)
            return;
        String faultCause = "" + stopCause;//getFaultDesc(stopCause);

        TblChargingfaultrecord faultrecord = new TblChargingfaultrecord();
        faultrecord.setCfreUsingmachinecode(epCode);
        faultrecord.setCfreElectricpileid(pkEpId);
        faultrecord.setCfreElectricpilename("");

        faultrecord.setcFRe_EphNo(epGunNo);

        faultrecord.setCfreChargingsarttime(faultDate);
        faultrecord.setCfreFaultcause(faultCause);
        faultrecord.setCfreTransactionnumber(serialNo);
        // 新增故障记录
        DB.chargingfaultrecordDao.insertFaultRecord(faultrecord);
    }

}
