package com.ec.service;

import com.ec.cache.ChargeCardCache;
import com.ec.cache.UserCache;
import com.ec.cache.UserRealInfo;
import com.ec.constants.EpConstants;
import com.ec.constants.ErrorCodeConstants;
import com.ec.constants.UserConstants;
import com.ec.service.impl.UserServiceImpl;
import com.ec.utils.LogUtil;
import com.ormcore.model.TblChargeCard;
import com.ormcore.model.TblUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class AbstractUserService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(AbstractUserService.class.getName()));

    public static UserCache getUserCache(String account) {
        UserRealInfo userRealInfo = findUserRealInfo(account);
        if (userRealInfo == null)
            return null;

        return AbstractCacheService.convertToCache(userRealInfo);
    }

    public static UserCache getUserCache(int userId) {
        UserRealInfo userRealInfo = findUserRealInfo(userId);
        if (userRealInfo == null) return null;

        return AbstractCacheService.convertToCache(userRealInfo);
    }

    public static UserCache getUserCache(int userId, int accountId) {
        UserRealInfo userRealInfo = findUserRealInfo(userId, accountId);
        if (userRealInfo == null) return null;

        return AbstractCacheService.convertToCache(userRealInfo);
    }

    /*public static BigDecimal getRemainBalance(int accountId) {
        UserRealInfo u = findUserRealInfo(accountId);
        //假设数据
        if (null != u) {
            return u.getMoney();
        }
        return new BigDecimal(0.0);
    }*/

    public static UserRealInfo convertUsrRealInfo(TblUserInfo tblUserInfo) {
        try {
            if (tblUserInfo == null)
                return null;

            UserRealInfo u = new UserRealInfo();

            int usrId = tblUserInfo.getId();
            int accountId = UserServiceImpl.findAccountId(usrId);
            if (accountId == 0) {
                accountId = tblUserInfo.getAccountId();
            } else {
                tblUserInfo.setAccountId(accountId);
            }
            BigDecimal balance = UserServiceImpl.findBalanceById(accountId);
            tblUserInfo.setBalance(balance);
            String usrAccount = tblUserInfo.getPhone();

            u.setName(tblUserInfo.getName());
            u.setAccount(usrAccount);
            u.setId(usrId);
            u.setPassword(tblUserInfo.getPassword());
            u.setStatus(tblUserInfo.getStatus());
            u.setLevel(tblUserInfo.getLevel());

            u.setDeviceid(tblUserInfo.getDeviceid());
            u.setInvitePhone(tblUserInfo.getInvitePhone());
            u.setMoney(tblUserInfo.getBalance());

            logger.info(LogUtil.addExtLog("u.getId|u.getLevel"), u.getId(), u.getLevel());

            return u;
        } catch (Exception e) {
            logger.error(LogUtil.addExtLog("exception"), e.getStackTrace());
            return null;
        }
    }

    public static UserRealInfo findUserRealInfo(int userId) {
        TblUserInfo userInfo = UserServiceImpl.findUserInfoById(userId);

        return convertUsrRealInfo(userInfo);
    }

    public static UserRealInfo findUserRealInfo(int userId, int accountId) {
        TblUserInfo userInfo = UserServiceImpl.findUserInfoById(userId);
        if (userInfo == null) return null;
        userInfo.setAccountId(accountId);

        return convertUsrRealInfo(userInfo);
    }

    public static UserRealInfo findUserRealInfo(String userAccount) {
        TblUserInfo userInfo = UserServiceImpl.findUserInfoByPhone(userAccount);
        return convertUsrRealInfo(userInfo);
    }

    public static ChargeCardCache getChargeCardCache(int pkUserCard) {
        TblChargeCard cardInDb = UserServiceImpl.findCardById(pkUserCard);
        return convertCardToCache(cardInDb);
    }

    public static ChargeCardCache convertCardToCache(TblChargeCard tabCardInfo) {
        if (tabCardInfo == null)
            return null;

        boolean allowMutliCharge = false;
        int cardType = tabCardInfo.getUc_pay_mode();
        if (cardType == EpConstants.CARD_THIRD_NORMAL ||
                cardType == EpConstants.CARD_THIRD_PRIVATE ||
                cardType == EpConstants.CARD_CO_CREDIT) {
            allowMutliCharge = true;
        }

        return new ChargeCardCache(
                tabCardInfo.getPk_UserCard(), tabCardInfo.getUc_InternalCardNumber(),
                tabCardInfo.getUc_ExternalCardNumber(), tabCardInfo.getUc_CompanyNumber(),
                tabCardInfo.getUc_UserId(), tabCardInfo.getUc_Status(), tabCardInfo.getUc_pay_mode(),
                tabCardInfo.getAccount_id(), allowMutliCharge
        );
    }

    public static int findUserId(int orgNo, String userAccount) {
        int accountId = 0;
        if (userAccount == null) return accountId;

        if (orgNo == UserConstants.ORG_I_CHARGE) {
            UserRealInfo realU = findUserRealInfo(userAccount);
            if (null != realU) {
                accountId = realU.getId();
            }
        } else {
            accountId = UserServiceImpl.findUserInfoByOrgNo(orgNo);
            if (accountId == 0) return accountId;
            UserRealInfo userRealInfo = findUserRealInfo(accountId);
            if (null == userRealInfo) {
                accountId = 0;
            }
        }

        return accountId;
    }

    public static int canCharge(int usrId) {
        UserRealInfo realU = findUserRealInfo(usrId);
        if (null == realU) {
            return ErrorCodeConstants.INVALID_ACCOUNT;
        }
        int errorCode = realU.canCharge();
        if (errorCode > 0)
            return errorCode;

        return 0;
    }

}
