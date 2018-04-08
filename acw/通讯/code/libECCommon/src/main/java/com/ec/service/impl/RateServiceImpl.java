package com.ec.service.impl;

import com.ormcore.dao.DB;
import com.ormcore.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RateServiceImpl {

    public static List<RateInfo> getLastUpdate() {
        List<RateInfo> riList = DB.rateInfoDao.getLastUpdate();
        if (riList == null || riList.size() <= 0) {
            return null;
        }
        return riList;
    }

    public static List<RateInfo> getAll() {
        List<RateInfo> riList = DB.rateInfoDao.getAll();
        if (riList == null || riList.size() <= 0) {
            return null;
        }
        return riList;
    }

    public static RateInfo findRateInfo(int rateId) {
        return DB.rateInfoDao.findRateInfofromId(rateId);
    }

    public static TblJpush getJpushInfo(int userId) {
        return DB.jpushDao.getByuserInfo(userId);
    }

    public static void deleteEqVersionFromDB(int id) {
        DB.equipmentVersionDao.deleteEqVersion(id);
    }

    public static void updateEqVersion(TblEquipmentVersion equipment) {
        DB.equipmentVersionDao.updateEqVersion(equipment);
    }

    public static void insertEqVersion(TblEquipmentVersion equipment) {
        DB.equipmentVersionDao.insertEqVersion(equipment);
    }

    public static List<TblBomList> getAllByTypeSpanId(int typeSpanId) {
        List<TblBomList> tblBomList = DB.bomListDao.getAllByTypeSpanId(typeSpanId);
        if (tblBomList == null || tblBomList.size() <= 0) {
            return null;
        }
        return tblBomList;
    }

    public static List<TblBomList> getBomByProductId(int typeSpanId, int pkId) {
        Map<String, Object> map = new HashMap<>();
        map.put("typeSpanId", typeSpanId);
        map.put("productId", pkId);
        return DB.bomListDao.getBomByProductId(map);
    }

    public static List<TblEquipmentVersion> findEqVersion(int id, int type) {
        TblEquipmentVersion equipment = new TblEquipmentVersion();
        equipment.setProductID(id);
        equipment.setProductType(type);
        List<TblEquipmentVersion> evList = DB.equipmentVersionDao.findEqVersion(equipment);
        if (evList == null || evList.size() <= 0) {
            return null;
        }
        return evList;
    }

    public static List<TblTypeSpan> getAllTypeSpan() {
        List<TblTypeSpan> tblTypeSpanList = DB.typeSpanDao.getAll();
        if (tblTypeSpanList == null || tblTypeSpanList.size() <= 0) {
            return null;
        }
        return tblTypeSpanList;
    }

}
