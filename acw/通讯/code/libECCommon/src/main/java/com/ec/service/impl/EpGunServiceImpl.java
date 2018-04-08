package com.ec.service.impl;

import com.ec.utils.NumUtil;
import com.ormcore.dao.DB;
import com.ormcore.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpGunServiceImpl {

    public static TblElectricPileGun getDbEpGun(int pkEpId, int epGunNo) {
        TblElectricPileGun tblEpGun = new TblElectricPileGun();
        tblEpGun.setPkEpId(pkEpId);
        tblEpGun.setEpGunNo(epGunNo);

        List<TblElectricPileGun> epGunList = DB.epGunDao.findEpGunInfo(tblEpGun);
        if (epGunList == null || epGunList.size() <= 0) {
            return null;
        }
        return epGunList.get(0);
    }

    public static void updateGunState(int pkEpGunId, int status) {
        TblElectricPileGun info = new TblElectricPileGun();
        info.setPkEpGunId(pkEpGunId);
        info.setEpState(status);
        DB.epGunDao.updateGunState(info);
    }

    public static void addChargeStat(int pkEpGunId, int chargeMeterNum, int chargeTime, int chargeAmt) {
        TblElectricPileGun info = new TblElectricPileGun();
        info.setPkEpGunId(pkEpGunId);

        info.setTotalChargeMeter(NumUtil.intToBigDecimal3(chargeMeterNum));
        info.setTotalChargeTime(chargeTime);
        info.setTotalChargeAmt(NumUtil.intToBigDecimal2(chargeAmt));

        DB.epGunDao.addChargeStat(info);
    }

    public static void updateDeviceList(int pkEpGunId, int hadLid, int hadSitSignal, int hadRadar, int hadCarPlaceLock, int hadBmsComm) {
        TblElectricPileGun info = new TblElectricPileGun();
        info.setPkEpGunId(pkEpGunId);

        info.setHadLid(hadLid);
        info.setHadSitSignal(hadSitSignal);

        info.setHadRadar(hadRadar);
        info.setHadCarPlaceLock(hadCarPlaceLock);
        info.setHadBmsComm(hadBmsComm);
        DB.epGunDao.updateDeviceList(info);
    }

    public static void updateQR(int pkEpGunId, long date, String identyCode)
    {
        TblElectricPileGun tblGun=new TblElectricPileGun();

        tblGun.setPkEpGunId(pkEpGunId);
        tblGun.setQrdate(date);
        tblGun.setQr_codes(identyCode);

        //保存到数据库
        DB.epGunDao.updateQR(tblGun);
    }

    public static TblCarVin selectByCode(String vinCode) {
        List<TblCarVin> carVinList = DB.carVinDao.selectByCode(vinCode);
        if(carVinList == null ||carVinList.size() <= 0)
        {
            return null;
        }

        return carVinList.get(0);
    }

    public static TblCarVin getCarVinById(int vinId) {
        List<TblCarVin> carVinList = DB.carVinDao.selectById(vinId);
        if (carVinList == null || carVinList.size() <= 0) {
            return null;
        }

        return carVinList.get(0);
    }

    public static int insertVehicle(TblVehicleBattery dbInfo) {
        return DB.vehicleBatteryDao.insert(dbInfo);
    }

    public static int updateVehicle(TblVehicleBattery dbInfo) {
        return DB.vehicleBatteryDao.update(dbInfo);
    }

    public static void insertInviteCoupon(int cpUserid) {
        List<Integer> list = DB.couponDao.queryActivity(4);
        if (list != null && list.size() == 1) {
            Map<String, Object> map = new HashMap<>();

            map.put("cpUserid", cpUserid);
            map.put("pkActivity", list.get(0));

            DB.couponDao.insertInviteCoupon(map);
        }
    }

    public static TblCoupon queryCoupon(int cpUserid, int cpLimitation, int actActivityrule, int consemeAmt) {
        Map<String, Object> map = new HashMap<>();

        map.put("cpUserid", cpUserid);
        map.put("cpLimitation", cpLimitation);
        map.put("actActivityrule", actActivityrule);
        map.put("consemeAmt", consemeAmt);

        List<TblCoupon> list = DB.couponDao.queryCoupon(map);
        if (list != null && list.size() > 0) {
            if ("0".equals(list.get(0).getCpRate())) return null;
            return list.get(0);
        } else {
            return null;
        }
    }

    public static int insertPowerInfo(TblPowerModule dbInfo) {
        return DB.powerModuleDao.insert(dbInfo);
    }

    public static int updatePowerInfo(TblPowerModule dbInfo) {
        return DB.powerModuleDao.update(dbInfo);
    }
}
