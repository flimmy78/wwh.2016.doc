package com.ec.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserCache {

    private static final Logger logger = LoggerFactory.getLogger(UserCache.class);

    private int id; //
    private String account; //
    private int online;
    private String name;
    private int newcouponAcStatus;//用户交流新手券状态 -1，未判断 ，0：未使用，1：已使用
    private int newcouponDcStatus;//用户直流新手券状态-1，未判断 ，0：未使用，1：已使用

    private String invitePhone;//邀请者号码

    private int remainAmtWarnValue;
    private String remainAmtWarnPhone;
    private String remainAmtWarnCPhone;
    private boolean remainAmtWarn;
    private UserRealInfo userRealInfo;

    /**
     * 因为可以多充，所以对用户来说，没有当前充电.
     */
    private Map<String, ChargeCache> chargeList = new ConcurrentHashMap<String, ChargeCache>();

    private int level;//TODO,移动到充电开始的时候

    public UserCache(int userId, String userAccount, int userLevel) {
        id = userId; //
        account = userAccount; //
        level = userLevel;

        init();
    }

    public UserCache(int usrId,String userAccount,String userName,int userLevel,String userInvitePhone)
    {
        id = usrId; //
        account =userAccount; //
        level = userLevel;

        newcouponAcStatus=-1;//-1，未判断
        newcouponDcStatus=-1;//-1，未判断
        invitePhone=userInvitePhone;

        name=userName;

        init();
        remainAmtWarnValue= 0;
        remainAmtWarnPhone="";
        remainAmtWarnCPhone="";
        remainAmtWarn=false;

        invitePhone="";
    }

    /**
     * 装载该用户未完成的业务
     * 1.装载用户未完成的预约
     * 2.装载用户未完成的充电
     */
    private void init() {
        //EpBespokeService.getUnStopBespokeByUserIdFromDb(this);
        //BaseEpChargeService.getUnFinishChargeByUserIdFromDb(this);

    }

    public void removeCharge(String chargeSerialNo) {
        if (chargeSerialNo != null && chargeSerialNo.length() > 0) {
            chargeList.remove(chargeSerialNo);
        }
    }

    public void addCharge(ChargeCache cache) {
        chargeList.put(cache.getChargeSerialNo(), cache);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNewcouponAcStatus() {
        return newcouponAcStatus;
    }

    public void setNewcouponAcStatus(int newcouponAcStatus) {
        this.newcouponAcStatus = newcouponAcStatus;
    }

    public int getNewcouponDcStatus() {
        return newcouponDcStatus;
    }

    public void setNewcouponDcStatus(int newcouponDcStatus) {
        this.newcouponDcStatus = newcouponDcStatus;
    }

    public String getInvitePhone() {
        return invitePhone;
    }

    public void setInvitePhone(String invitePhone) {
        this.invitePhone = invitePhone;
    }

    public int getRemainAmtWarnValue() {
        return remainAmtWarnValue;
    }

    public void setRemainAmtWarnValue(int remainAmtWarnValue) {
        this.remainAmtWarnValue = remainAmtWarnValue;
    }

    public String getRemainAmtWarnPhone() {
        return remainAmtWarnPhone;
    }

    public void setRemainAmtWarnPhone(String remainAmtWarnPhone) {
        this.remainAmtWarnPhone = remainAmtWarnPhone;
    }

    public String getRemainAmtWarnCPhone() {
        return remainAmtWarnCPhone;
    }

    public void setRemainAmtWarnCPhone(String remainAmtWarnCPhone) {
        this.remainAmtWarnCPhone = remainAmtWarnCPhone;
    }

    public boolean isRemainAmtWarn() {
        return remainAmtWarn;
    }

    public void setRemainAmtWarn(boolean remainAmtWarn) {
        this.remainAmtWarn = remainAmtWarn;
    }

    public UserRealInfo getUserRealInfo() {
        return userRealInfo;
    }

    public void setUserRealInfo(UserRealInfo userRealInfo) {
        this.userRealInfo = userRealInfo;
    }

    public void clean() {
        chargeList = null;
        //level = 0;
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();
        sb.append("UserCache");
        sb.append("{id=").append(id).append("}\n");
        sb.append(",{account=").append(account).append("}\n");
        if (invitePhone != null) {
            sb.append(",{online=").append(online).append("}\n");
            sb.append(",{AcStatus=").append(newcouponAcStatus).append("}\n");
            sb.append(",{DcStatus=").append(newcouponDcStatus).append("}\n");
            sb.append(",{invitePhone=").append(invitePhone).append("}\n");
        }

        int chargeCount = chargeList.size();
        if (chargeCount > 0) {
            sb.append("charge list!count").append(chargeList.size()).append(":\n");
            Iterator iter = chargeList.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                ChargeCache o = (ChargeCache) entry.getValue();
                if (o == null)
                    continue;
                sb.append(o.toString());
            }
        }

        return sb.toString();
    }

    /**
     * 只有电桩失联并且预冻结的充电
     *
     * @return
     */
    public ChargeCache getHistoryCharge(String epGunNo) {
        Iterator iter = chargeList.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            ChargeCache o = (ChargeCache) entry.getValue();

            String s = o.getEpCode() + o.getEpGunNo();
            if (s.compareTo(epGunNo) == 0)
                continue;
            //TODO
            //INetObject netObject = epGun.getEpNetObject();
            //if(netObject!=null && !netObject.isComm())
            //	return o;
        }
        return null;
    }

    /**
     * @param usingGun
     * @return
     */
    public int canCharge(String usingGun, int chargingUsrId, int chargingUsrOrgNo, String chargingUsrIdentity, int pkCard, boolean allowMutliCharge) {
        Iterator iter = chargeList.entrySet().iterator();

        String epCode = "";
        int epGunNo = 0;
        int chargedUsrId = 0;
        String chargedUsrIdentity = "";

        int errorCode = 0;
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            ChargeCache o = (ChargeCache) entry.getValue();
            epCode = o.getEpCode();
            epGunNo = o.getEpGunNo();
            chargedUsrId = o.getUserId();
            chargedUsrIdentity = o.getUserIdentity();

			/*if(chargingUsrOrgNo == UserConstant.ORG_I_CHARGE ||
					(chargingUsrOrgNo != UserConstant.ORG_I_CHARGE&& chargingUsrIdentity.equals(chargedUsrIdentity)))
			{
				errorCode= canPauseStatOldCharge(epCode,epGunNo,usingGun);
				if(errorCode>0)
				{
					break;
				}
			}*/

        }

        logger.debug("canCharge,errorCode:{},usingGun:{},chargingUsrId:{},orgNo:{},chargingUsrIdentity:{},pkCard:{},allowMutliCharge:{}",
                new Object[]{errorCode, usingGun, chargingUsrId, chargingUsrOrgNo, chargingUsrIdentity, pkCard, allowMutliCharge,
                        epCode, epGunNo, chargedUsrId, chargedUsrIdentity});

        return errorCode;
    }
}
