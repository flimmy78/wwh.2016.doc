package com.ec.service.impl;

import com.ormcore.dao.DB;
import com.ormcore.model.*;

import java.math.BigDecimal;
import java.util.List;

public class UserServiceImpl {

    public static TblUserInfo findUserInfoById(int userId) {
        List<TblUserInfo> userInfoList = DB.userInfoDao.findUserInfoById(userId);
        if (null == userInfoList || userInfoList.size() != 1) {
            return null;
        }

        return userInfoList.get(0);
    }

    public static TblUserInfo findUserInfoByPhone(String account) {
        List<TblUserInfo> userInfoList = DB.userInfoDao.findUserInfoByPhone(account);
        if (null == userInfoList || userInfoList.size() != 1) {
            return null;
        }

        return userInfoList.get(0);
    }

    public static int findAccountId(int userId) {
        return DB.userInfoDao.findAccountId(userId);
    }

    public static BigDecimal findBalanceById(int accountId) {
        return DB.userInfoDao.findBalanceById(accountId);
    }

    public static Integer findUserInfoByOrgNo(int orgNo) {
        return DB.userBusinessDao.findUserInfoByOrgNo(orgNo);
    }

    public static void addCost(int accountId, BigDecimal amt) {
        TblUserInfo u = new TblUserInfo();
        u.setId(accountId);
        u.setBalance(amt);

        DB.userInfoDao.addCost(u);
    }

    public static void subCost(int accountId, BigDecimal amt) {
        TblUserInfo u = new TblUserInfo();
        u.setId(accountId);
        u.setBalance(amt);

        DB.userInfoDao.subCost(u);
    }

    public static TblUserThreshod findUserThreshodInfo(int userId) {
        TblUserThreshod usrThreshod = DB.userThreshodDao.findUserThreshodInfo(userId);
        if (usrThreshod == null) {
            return null;
        }
        return usrThreshod;
    }

    public static TblUserNewcoupon findNewcouponInfo(int userId) {
        List<TblUserNewcoupon> userNewcouponList = DB.userNewcouponDao.select(userId);
        if (userNewcouponList == null || userNewcouponList.size() <= 0) {
            return null;
        }
        return userNewcouponList.get(0);
    }

    public static void insertNewcoupon(int userId, int acStatus, int dcStatus) {
        TblUserNewcoupon newcoupon = new TblUserNewcoupon();
        newcoupon.setUserId(userId);
        newcoupon.setAcStatus(acStatus);
        newcoupon.setDcStatus(dcStatus);

        DB.userNewcouponDao.insert(newcoupon);
    }

    /**
     * 用户充电完成后  更新用户新手券状态
     *
     * @param userId
     * @param acStatus
     * @param dcStatus
     */
    public static void updateNewcoupon(int userId, int acStatus, int dcStatus) {
        TblUserNewcoupon newcoupon = new TblUserNewcoupon();
        newcoupon.setUserId(userId);
        newcoupon.setAcStatus(acStatus);
        newcoupon.setDcStatus(dcStatus);

        DB.userNewcouponDao.update(newcoupon);
    }

    public static void updateCoupon(int pkCoupon) {
        DB.couponDao.updateCoupon(pkCoupon);
    }

    public static List<TblElectricPile> getEpTypeByUserChargeOrder(int userId) {
        List<TblElectricPile> epList = DB.epClientDao.getEpTypeByUserChargeOrder(userId);
        if (epList == null || epList.size() <= 0) {
            return null;
        }
        return epList;
    }

    public static TblChargeCard findCard(String innerCardNo) {
        List<TblChargeCard> cardList = DB.chargeCardDao.findCard(innerCardNo);
        if (cardList == null || cardList.size() <= 0) {
            return null;
        }

        return cardList.get(0);
    }

    public static TblChargeCard findCardById(int pkUserCard) {
        List<TblChargeCard> chargeCardList = DB.chargeCardDao.findCardById(pkUserCard);
        if (chargeCardList == null || chargeCardList.size() <= 0) {
            return null;
        }
        return chargeCardList.get(0);
    }

    public static int insertBigCard(String innerCardNo, int orgNo, int userId) {
        TblChargeCard card = new TblChargeCard();

        card.setUc_Balance(new BigDecimal(0.0));
        card.setUc_CompanyNumber(orgNo);
        card.setUc_UserId(userId);
        card.setUc_Status(0);
        card.setUc_InternalCardNumber(innerCardNo);
        card.setUc_ExternalCardNumber(innerCardNo);
        card.setUc_pay_mode(12);

        return DB.chargeCardDao.insertCard(card);
    }

}
