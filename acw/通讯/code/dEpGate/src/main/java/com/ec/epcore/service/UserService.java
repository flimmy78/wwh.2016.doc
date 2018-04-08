package com.ec.epcore.service;

import com.ec.cache.*;
import com.ec.config.Global;
import com.ec.constants.EpConstants;
import com.ec.epcore.cache.EpGunCache;
import com.ec.epcore.net.client.EpCommClient;
import com.ec.epcore.net.codec.EpEncoder;
import com.ec.epcore.net.proto.CardAuthResp;
import com.ec.epcore.sender.EpMessageSender;
import com.ec.logs.LogConstants;
import com.ec.net.proto.Iec104Constant;
import com.ec.service.AbstractCacheService;
import com.ec.service.AbstractUserService;
import com.ec.service.impl.ChargeServiceImpl;
import com.ec.service.impl.UserServiceImpl;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import com.ormcore.model.TblChargeCard;
import com.ormcore.model.TblElectricPile;
import com.ormcore.model.TblUserNewcoupon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService extends AbstractUserService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(UserService.class.getName()));

    //orgNo & user_id
    public static Map<Integer, Integer> epOrgNoMap = new ConcurrentHashMap<>();

    public static int getUserIdByOrgNo(int orgNo) {
        Integer user_id = epOrgNoMap.get(orgNo);
        if (user_id == null) {
            user_id = UserServiceImpl.findUserInfoByOrgNo(orgNo);
            if (user_id == null) {
                logger.error(LogUtil.addExtLog("error!,orgNo"), orgNo);
                return 0;
            }
            epOrgNoMap.put(orgNo, user_id);
        }
        return user_id;
    }

    public static void addAmt(int accountId, BigDecimal amt, String serialNo) {
        try {
            logger.info("addAmt start,accountId:{},amt:{},serialNo:{}", new Object[]{accountId, amt, serialNo});

            UserCache uc = AbstractCacheService.getUserCache(accountId);//getUserCache(accountId);
            if (uc == null) {
                logger.info("addAmt fail,accountId:{},amt:{},getUserCache()==null", accountId, amt);
                return;
            }

            UserServiceImpl.addCost(accountId, amt);

            logger.info("addAmt finish!accountId:{},amt:{},serialNo:{}", new Object[]{accountId, amt, serialNo});
        } catch (Exception e) {
            logger.error("addAmt exception,getStackTrace:{}", e.getStackTrace());
        }
    }

    public static void subAmt(int accountId, BigDecimal amt, String serialNo) {
        try {
            logger.info("subAmt start. accountId:{},amt:{},serialNo:{}", new Object[]{accountId, amt, serialNo});
            UserCache uc = AbstractCacheService.getUserCache(accountId);
            if (uc == null) {
                logger.info("subAmt fail,accountId:{},amt:{},getUserCache()==null", accountId, amt);
                return;
            }

            UserServiceImpl.subCost(accountId, amt);

            logger.info("subAmt finish!accountId:{},amt:{},serialNo:{}", new Object[]{accountId, amt, serialNo});


        } catch (Exception e) {
            logger.error("subAmt exception,getStackTrace:{}", e.getStackTrace());
        }
    }

    public static CardAuthResp checkUserCard(String epCode, ChargeCardCache cardCache, String userPasswordMd5) {
        CardAuthResp ret = new CardAuthResp();

        //查电桩
        BaseEPCache epCache = CacheService.getEpCache(epCode);
        EpService.getEpRealStatus(epCache);
        if (epCache == null) {
            ret.setErrorCode(EpConstants.E_CARD_NOT_FIND_EP);
            return ret;
        }

        int errorCode = epCache.canAuth();
        if (errorCode > 0) {
            ret.setErrorCode(errorCode);
            return ret;
        }

        if (cardCache == null) {
            ret.setErrorCode(EpConstants.E_CARD_INVALID);
            return ret;
        }
        if (cardCache.getStatus() != 0) {
            ret.setErrorCode(EpConstants.E_CARD_LOSS);
            return ret;
        }

        UserCache userCache = UserService.getUserCache(cardCache.getUserId(), cardCache.getAccountId());
        UserRealInfo u = userCache.getUserRealInfo();
        if (u == null) {
            ret.setErrorCode(EpConstants.E_CARD_NOT_BIND_USER);
            return ret;
        }
        if (u.getStatus() != 1)//不是正常用户
        {
            ret.setErrorCode(EpConstants.E_CARD_NO_RIGHT);
            return ret;
        }
        if (u.getPassword().compareTo(userPasswordMd5) != 0) {
            ret.setErrorCode(EpConstants.E_CARD_AUTH_ERROR_PW);
            return ret;
        }

        int cardType = cardCache.getCardType();
        if (cardType != EpConstants.CARD_NORMAL &&
                cardType != EpConstants.CARD_NORMAL_PRIVATE &&
                cardType != EpConstants.CARD_CO_CREDIT &&
                cardType != EpConstants.CARD_THIRD_NORMAL &&
                cardType != EpConstants.CARD_THIRD_PRIVATE) {
            ret.setErrorCode(EpConstants.E_CARD_INVALID);
            return ret;
        }

        if (cardType == EpConstants.CARD_NORMAL && epCache.getCompany_number() > 0) {
            //普通卡不能刷带公司标识的桩
            ret.setErrorCode(EpConstants.E_CARD_NO_RIGHT);
            return ret;
        }

        //范围卡
        if (cardType == EpConstants.CARD_NORMAL_PRIVATE ||
                cardType == EpConstants.CARD_THIRD_PRIVATE ||
                cardType == EpConstants.CARD_CO_CREDIT) {
            int coNoOfCard = cardCache.getCompanyNumber();
            int coNoOfEp = epCache.getCompany_number();
            if (coNoOfCard <= 0 || coNoOfEp != coNoOfCard) {
                logger.debug(LogUtil.addFuncExtLog(LogConstants.FUNC_CARD_AUTH, "pay_mod|card'companyNumber|ep''companyNumber"),
                        new Object[]{cardType, coNoOfCard, coNoOfEp});
                ret.setErrorCode(EpConstants.E_CARD_NO_RIGHT);
                return ret;
            }
        }
        if (cardType == EpConstants.CARD_CO_CREDIT) {
            ret.setIsFrozenAmt(2);
        } else {
            ret.setIsFrozenAmt(1);

        }
        int remainAmt = u.getMoney().multiply(Global.DecTime2).intValue();
        ret.setRemainAmt(remainAmt);

        return ret;
    }

    public static void handleSelfCard(EpCommClient epCommClient, String epCode, int epGunNo, String inCardNo, String userPasswordMd5, byte[] cmdTimes) {
        ChargeCardCache cardCache = getCard(inCardNo);

        String outCardNo = "";
        int remainAmt = 0;
        int success = 0;
        int errorCode = 0;
        int payMode = 1;
        int isFrozenAmt = 1;
        UserCache userCache = null;

        CardAuthResp ret = checkUserCard(epCode, cardCache, userPasswordMd5);
        if (ret != null) {
            success = (ret.getErrorCode() != 0) ? 0 : 1;
            if (success == 1) {
                logger.info(LogUtil.addFuncExtLog(LogConstants.FUNC_CARD_AUTH, "epCode|epGunNo|inCardNo"),
                        new Object[]{epCode, epGunNo, inCardNo});
            } else {
                logger.error(LogUtil.addFuncExtLog(LogConstants.FUNC_CARD_AUTH, "epCode|epGunNo|inCardNo|errorCode"),
                        new Object[]{epCode, epGunNo, inCardNo, ret.getErrorCode()});
            }

            if (cardCache != null) userCache = AbstractCacheService.getUserCache(cardCache.getUserId());
            if (userCache != null) {
                EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
                AuthUserCache authUser = new AuthUserCache(userCache.getId(), userCache.getAccount(), DateUtil.getCurrentSeconds(), (short) 3);
                epGunCache.setAuth(authUser);
                payMode = EpChargeService.getCardPayMode(cardCache.getCardType());
            }
            remainAmt = ret.getRemainAmt();
            errorCode = ret.getErrorCode();
            isFrozenAmt = ret.getIsFrozenAmt();
            outCardNo = cardCache.getCardOutNo();
        } else {
            logger.error(LogUtil.addFuncExtLog(LogConstants.FUNC_CARD_AUTH, "epCode|inCardNo|userPasswordMd5"),
                    new Object[]{epCode, inCardNo, userPasswordMd5});
        }

        byte[] data = EpEncoder.do_card_auth_resq(epCode, epGunNo, inCardNo, outCardNo, payMode,
                isFrozenAmt, remainAmt, success, errorCode);

        EpMessageSender.sendMessage(epCommClient, 0, 0, Iec104Constant.C_CARD_AUTH_RESP, data, cmdTimes, epCommClient.getVersion());
    }

    public static ChargeCardCache getCard(String innerCardNo) {
        TblChargeCard cardInDb = UserServiceImpl.findCard(innerCardNo);
        if (cardInDb == null) {
            logger.error(LogUtil.addExtLog("inCardNo|cardInDb"), innerCardNo, null);
            return null;
        }

        return UserService.convertCardToCache(cardInDb);
    }

    public static void handleUserCardAuth(EpCommClient epCommClient, String epCode, int epGunNo,
            String inCardNo, String userPasswordMd5, int cardOrigin, byte[] cmdTimes) {
        switch (cardOrigin) {
            case 0:
                handleSelfCard(epCommClient, epCode, epGunNo, inCardNo, userPasswordMd5, cmdTimes);
                break;
            case 5:
                //EpGunService.handleCardAuth(epCode,epGunNo,cardOrigin,inCardNo,"wm1234567",cmdTimes);
                break;
            default:
                logger.error("card charge userCardAuth fail,epCode:{},epGunNo:{},inCardNo:{},cardOrigin:{}",
                        new Object[]{epCode, epGunNo, inCardNo, cardOrigin});
                break;
        }
    }

    /**
     * @param Account
     * @param passwordMd5
     * @return 0:成功,1:没找到用户;2:账户被冻结;3:密码错误;4:桩没绑定费率;5:有未支付的订单;6:余额不够
     */
    public static int checkUser(String epCode, String account, byte[] passwordMd5) {

        int ret = 0;
        UserRealInfo userRealInfo = findUserRealInfo(account);
        if (null == userRealInfo)
            return 1;
        if (1 != userRealInfo.getStatus()) {
            return 2;
        }

        String sPw = new String(passwordMd5);
        if (sPw.compareTo(userRealInfo.getPassword()) != 0) {
            return 3;
        }

        RateInfoCache rateInfo = RateService.getRateInfo(epCode);

        if (rateInfo == null) {
            return 4;
        }
        int userId = userRealInfo.getId();
        int unpayNum = ChargeServiceImpl.getUnpayOrder(userId);
        if (unpayNum > 0) {
            return 5;
        }
        BigDecimal minAmt = new BigDecimal(1.0);
        if (userRealInfo.getMoney().compareTo(rateInfo.getRateInfo().getMinFreezingMoney()) < 0 || userRealInfo.getMoney().compareTo(minAmt) < 0) {
            return 6;
        }
        /*int num = DB.chargeOrderDao.getUnpayOrder(userId);
        if (num > 0) {
            return 8;
        }*/

        return 0;

    }

    /**
     * 设置内存中用户新手券状态
     *
     * @param userCache
     */
    public static void setUserNewcoupon(UserCache userCache) {
        if (userCache == null)
            return;
        //判断该用户是否读过数据库，初始化userCache.getNewcouponAcStatus()=-1，userCache.getNewcouponDcStatus()=-1

        if (userCache.getNewcouponAcStatus() >= 0 && userCache.getNewcouponDcStatus() >= 0)
            return;
        TblUserNewcoupon newcoupon = UserServiceImpl.findNewcouponInfo(userCache.getId());
        if (newcoupon == null) {
            getChargedInfoByUser(userCache);
            return;
        }
        userCache.setNewcouponAcStatus(newcoupon.getAcStatus());
        userCache.setNewcouponDcStatus(newcoupon.getDcStatus());

    }

    /**
     * 从充电订单中获取用户是否已经充过电
     *
     * @param userCache
     */
    public static void getChargedInfoByUser(UserCache userCache) {
        if (userCache == null)
            return;

        userCache.setNewcouponAcStatus(0);
        userCache.setNewcouponDcStatus(0);
        List<TblElectricPile> epList = UserServiceImpl.getEpTypeByUserChargeOrder(userCache.getId());
        if (epList != null) {
            for (int i = 0; i < epList.size(); i++) {
                TblElectricPile ep = epList.get(i);
                if (ep.getCurrentType() == EpConstants.EP_AC_TYPE)
                    userCache.setNewcouponAcStatus(1);
                else if (ep.getCurrentType() == EpConstants.EP_DC_TYPE)
                    userCache.setNewcouponDcStatus(1);
            }
        }
        UserServiceImpl.insertNewcoupon(userCache.getId(), userCache.getNewcouponAcStatus(), userCache.getNewcouponDcStatus());
    }

    /*public static UserCache getMemOrgUser(int orgNo) {
        UserCache u = null;
        if (orgNo > 0)
            u = getUserCache(orgNo);
        *//*if(u==null)
    		u= getUserCache(usrAccount);
    	
    	if(u!=null)
    	{
    		if(u.getId()!=usrId || !u.getAccount().equals(usrAccount))
    		{
    			logger.error("getMemOrgUser usrId,usrAccount",
    					new Object[]{u.getId(),u.getAccount(),usrId,usrAccount});
    		}
    	}*//*
        return u;

    }

    public static UserCache getMemUser(int usrId, String usrAccount) {
        UserCache u = null;
        if (usrId > 0)
            u = getUserCache(usrId);
        *//*if (u == null)
            u = getUserCache(usrAccount);*//*

        if (u != null) {
            if (u.getId() != usrId || !u.getAccount().equals(usrAccount)) {
                logger.error("getMemUser usrId,usrAccount",
                        new Object[]{u.getId(), u.getAccount(), usrId, usrAccount});
            }
        }
        return u;

    }*/

    public static void cleanUsrCharge(int usrId, String usrAccount, String chargeSerialNo) {
        UserCache u = null;
        if (usrId > 0) {
            u = AbstractCacheService.getUserCache(usrId);
        }
        u.removeCharge(chargeSerialNo);
        CacheService.putUserCache(u);
    }

    public static void cleanUsrInfo(int usrId, String usrAccount) {
        if (usrId > 0) {
            UserCache u2 = AbstractCacheService.getUserCache(usrId);
            if (u2 != null) {
                u2.clean();
                CacheService.putUserCache(u2);
            }
        }

    }
}
