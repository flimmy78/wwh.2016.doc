package com.ec.epcore.service;

import com.ec.cache.*;
import com.ec.common.net.U2ECmdConstants;
import com.ec.constants.*;
import com.ec.epcore.cache.EpGunCache;
import com.ec.epcore.cache.RealChargeInfo;
import com.ec.epcore.config.GameConfig;
import com.ec.epcore.net.codec.UsrGateEncoder;
import com.ec.epcore.net.proto.ConsumeRecord;
import com.ec.epcore.net.sender.UsrGateMessageSender;
import com.ec.epcore.net.client.UsrGateClient;
import com.ec.logs.LogConstants;
import com.ec.service.AbstractUserService;
import com.ec.service.impl.ChargeServiceImpl;
import com.ec.epcore.task.CheckUsrGateTask;
import com.ec.net.proto.WmIce104Util;
import com.ec.netcore.client.ChannelManage;
import com.ec.netcore.conf.CoreConfig;
import com.ec.netcore.core.pool.TaskPoolFactory;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import com.ec.utils.NetUtils;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UsrGateService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(UsrGateService.class.getName()));

    private static ChannelManage cm = new ChannelManage();

    public static boolean isValidCmd(Channel channel, int cmd) {
        UsrGateClient usrGate = getClient(channel);
        if (usrGate == null)
            return false;
        if ((usrGate.getStatus() < CommStatusConstant.INIT_SUCCESS && cmd == U2ECmdConstants.EP_LOGIN))
            return true;
        else if (usrGate.getStatus() == CommStatusConstant.INIT_SUCCESS &&
                (cmd == U2ECmdConstants.EP_ACK ||
                        cmd == U2ECmdConstants.EP_HEART ||
                        cmd == U2ECmdConstants.EP_ONLINE ||
                        cmd == U2ECmdConstants.PHONE_ONLINE ||
                        cmd == U2ECmdConstants.PHONE_CONNECT_INIT ||
                        cmd == U2ECmdConstants.EP_CHARGE ||
                        cmd == U2ECmdConstants.EP_CHARGE_EVENT ||
                        cmd == U2ECmdConstants.EP_STOP_CHARGE ||
                        cmd == U2ECmdConstants.EP_CONSUME_RECODE ||
                        cmd == U2ECmdConstants.EP_GUN_CAR_STATUS ||
                        cmd == U2ECmdConstants.CCZC_QUERY_ORDER))
            return true;

        return false;
    }

    public static String getCacheSize() {
        return cm.getCacheSize();

    }

    public static void addConnect(UsrGateClient client) {
        int ret = cm.addConnect(client);
        if (ret == 0) {
            logger.error("addClient fail");
        }
    }

    public static void addClient(UsrGateClient client) {
        int ret = cm.addClient(client);
        if (ret == 0) {
            logger.error("addClient fail");
        }

    }

    public static UsrGateClient getClient(Channel ch) {
        return (UsrGateClient) cm.get(ch);
    }

    public static UsrGateClient getClient(String key) {
        return (UsrGateClient) cm.get(key);
    }

    public static void removeClient(Channel ch) {
        int ret = cm.remove(ch);
        if (ret < 1) {
            logger.error("removeUsrGate fail! ch:{},ret:{}", ch, ret);
        }
    }

    public static void removeClient(String key) {
        int ret = cm.remove(key);
        if (ret < 1) {
            logger.error("removeUsrGate fail! key:{},ret:{}", key, ret);
        }

    }

    /**
     * 用户网关登录
     *
     * @param channel
     * @param usrGateId
     * @return
     */
    public static int usrGateLogin(Channel channel, int OrgType, int version) {
        //如果usrGateId已经有了,不能让新用户登录
        UsrGateClient usrGate = getClient(channel);
        if (usrGate == null) {
            logger.error("usrGate login fail,usrGate ==null,OrgType:{},ip:{}",
                    new Object[]{OrgType, channel});
            return 7001;
        }
        String ip = NetUtils.getClientIp(channel);
        UsrGateClient usrGate2 = getClient(ip);
        if (usrGate2 != null && usrGate2.getStatus() == CommStatusConstant.INIT_SUCCESS) {
            logger.error("usrGate login,close oldClient,OrgType:{},newCh:{},oldCh:{}",
                    new Object[]{OrgType, channel, usrGate2.getChannel()});
            usrGate2.getChannel().close();
            cm.remove(usrGate2);
        }

        //添加新连接
        usrGate.setIdentity(ip);
        usrGate.setStatus(CommStatusConstant.INIT_SUCCESS);
        usrGate.setVersion(version);
        usrGate.getUserOrigin().setCmdFromSource(OrgType);
        usrGate.getUserOrigin().setCmdChIdentity(ip);
        usrGate.setIp(ip);
        addClient(usrGate);

        logger.info("usrGate login success,OrgType:{},ip:{}", OrgType, ip);
        return 0;

    }

    /**
     * 用户gate网关登录
     *
     * @param channel
     * @param h
     * @param m
     * @param s
     * @param OrgType
     * @param version
     */
    public static void handleUsrGateLogin(Channel channel, int h, int m, int s, int OrgType, int version) {
        logger.debug("usrGate login,OrgType:{},version:{}", OrgType, version);

        //判断通道是否正常
        int errorCode = usrGateLogin(channel, OrgType, version);

        int ret = 1;
        if (errorCode > 0) {
            logger.error("usrGate login fail,OrgType:{},version:{}", OrgType, version);
            ret = 0;
        }

        int epGateId = CoreConfig.gameConfig.getId();
        byte[] data = UsrGateEncoder.login(0, epGateId, h, m, s, ret, errorCode);

        UsrGateMessageSender.sendMessage(channel, data);

    }

    /**
     * 处理ACK
     *
     * @param channel
     * @param cmd
     * @param usrId
     * @param h
     * @param m
     * @param s
     */
    public static void handleAck(Channel channel, short cmd, long usrId, int h, int m, int s) {
        setLastUseTime(channel);
        String messagekey = "" + usrId + cmd;//+h+m+s;

        UsrGateMessageSender.removeRepeatMsg(messagekey);
    }

    /**
     * 处理心跳
     *
     * @param channel
     */
    public static void handleHeart(Channel channel) {
        setLastUseTime(channel);

        byte[] data = UsrGateEncoder.heard();
        UsrGateMessageSender.sendMessage(channel, data);
    }

    /**
     * 处理电桩在线回复
     *
     * @param channel
     */
    public static void handleEpOnlineResp(Channel channel) {
        setLastUseTime(channel);
    }

    /**
     * 处理手机在线
     *
     * @param channel
     * @param h
     * @param m
     * @param s
     * @param usrId
     * @param online
     */
    public static void handlePhoneOnline(Channel channel, int h, int m, int s, long usrId, int online) {
        setLastUseTime(channel);
        UserCache usr = UserService.getUserCache((int) usrId);
        if (usr != null) {
            logger.info("usrGate phoneOnline,accountId:{},online:{}", usrId, online);
            usr.setOnline(online);
        }

        byte[] data = UsrGateEncoder.phoneOnline(h, m, s);

        UsrGateMessageSender.sendMessage(channel, data);

		/*if(online == 0)
        {
			UserService.removeUserCache((int)usrId);
		}*/
    }

    /**
     * 检查连接参数、判断
     *
     * @param epCode
     * @param epGunNo
     * @param epGunCache
     * @param usrId
     * @return
     */
    public static int checkConnectEp(String epCode, int epGunNo, long usrId, int OrgNo) {
        //查电桩
        BaseEPCache epCache = CacheService.getEpCache(epCode);
        if (epCache == null) {
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache == null || epGunCache.getEpNetObject() == null) {
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        int errorCode = epGunCache.checkEpNetConnect();
        if (errorCode > 0) return errorCode;

        UserCache userInfo = UserService.getUserCache((int) usrId);
        if (userInfo == null) {
            return ErrorCodeConstants.INVALID_ACCOUNT;
        }

        errorCode = epGunCache.canWatchCharge((int) usrId);
        if (errorCode > 0) {
            return errorCode;
        } else if (errorCode == -1) {
            errorCode = epCache.canCharge(null, epGunNo);
            if (errorCode > 0) return errorCode;
            errorCode = epGunCache.canCharge(EpConstants.CHARGE_TYPE_QRCODE, (int) usrId, true);
            if (errorCode > 0) return errorCode;

            //errorCode = userInfo.canCharge(epCode+epGunNo,userInfo.getId(),OrgNo,"",0,false);
            //if(errorCode>0) return errorCode;
        }

        userInfo.setOnline(1); //设置手机在线
        return 0;
    }

    /**
     * 初始化手机连接
     *
     * @param h
     * @param m
     * @param s
     * @param epCode
     * @param epGunNo
     * @param usrId
     * @return
     */
    public static int phoneConnectEp(Channel channel, int h, int m, int s, String epCode, int epGunNo, long usrId) {
        UsrGateClient usrGate = getClient(channel);
        if (usrGate == null) {
            logger.error(LogUtil.addBaseExtLog("channel"),
                    new Object[]{LogConstants.FUNC_PHONE_INIT, epCode, epGunNo, UserConstants.ORG_I_CHARGE, usrId, channel});
            return 7001;
        }

        int error = checkConnectEp(epCode, epGunNo, usrId, UserConstants.ORG_I_CHARGE);
        if (error > 0) {
            logger.error(LogUtil.addBaseExtLog("channel"),
                    new Object[]{LogConstants.FUNC_PHONE_INIT, epCode, epGunNo, UserConstants.ORG_I_CHARGE, usrId, channel});
            return error;
        }

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        short pos = epGunCache.getGunStatus();

        logger.info(LogUtil.addBaseExtLog("pos|channel"),
                new Object[]{LogConstants.FUNC_PHONE_INIT, epCode, epGunNo, UserConstants.ORG_I_CHARGE, usrId, pos, channel});

        byte ret = 0x01;
        byte[] respData = UsrGateEncoder.do_connect_ep_resp(h, m, s, epCode,
                epGunNo, usrId, ret, 0, (byte) pos);
        if (respData != null) {
            UsrGateMessageSender.sendMessage(channel, respData);
        }
        if (pos == 6) {
            // 卡充电在手机上显示
            if (epGunCache.getChargeCache() != null
                    && epGunCache.getChargeCache().getStartChargeStyle() == 3
                    && epGunCache.getChargeCache().getUserOrigin() != null) {
                epGunCache.getChargeCache().getUserOrigin().setCmdFromSource(2);
            }
            //修改用户对应的usrGate的IP地址
            if (epGunCache.getChargeCache() != null &&
                    epGunCache.getCurUserId() == usrId) {
                //如果用户gate的IP和充电缓存中的IP不同，则修改充电缓存和数据库充电记录对应的IP
                if (usrGate.getIdentity().compareTo(epGunCache.getChargeCache().getUserOrigin().getCmdChIdentity()) != 0) {
                    epGunCache.getChargeCache().getUserOrigin().setCmdChIdentity(usrGate.getIdentity());
                    //修改充电记录中的ip
                    ChargeServiceImpl.updateUsrGateIp(epGunCache.getChargeCache().getChargeSerialNo(), usrGate.getIdentity());
                }
            }
            if (epGunCache.getCurUserId() == usrId) {
                epGunCache.pushFirstRealData();
            }
        }

        UserCache u = UserService.getUserCache((int) usrId);

        int orgNo = UserConstants.ORG_I_CHARGE;
        String usrLog = "" + usrId;
        String token = "";

        int gun2carLinkStatus = epGunCache.get_gun2carLinkStatus();
        sendGun2CarLinkStatus(gun2carLinkStatus, channel, epCode, epGunNo, orgNo, usrLog, token);

        AuthUserCache authUser = new AuthUserCache((int) usrId, u.getAccount(), DateUtil.getCurrentSeconds(), (short) 1);
        epGunCache.setAuth(authUser);
        return 0;
    }

    /**
     * 初始化手机
     *
     * @param channel
     * @param h
     * @param m
     * @param s
     * @param epCode
     * @param epGunNo
     * @param usrId
     */
    public static void handlePhoneInit(Channel channel, int h, int m, int s, String epCode, int epGunNo, long usrId) {
        setLastUseTime(channel);
        int errorCode = phoneConnectEp(channel, h, m, s, epCode, epGunNo, usrId);
        if (errorCode > 0) {
            logger.error(LogUtil.addBaseExtLog("errorCode|channel"),
                    new Object[]{LogConstants.FUNC_GUNLINK_STATUS, epCode, epGunNo, UserConstants.ORG_I_CHARGE, usrId, errorCode, channel});
            //返回失败
            byte[] respData = UsrGateEncoder.do_connect_ep_resp(h, m, s, epCode, epGunNo, usrId, 0, errorCode, (byte) 0);
            if (respData != null) {
                UsrGateMessageSender.sendMessage(channel, respData);
            }
        }
    }

    public static int startCharge(Channel channel, String ip, String epCode,
              int epGunNo, int orgNo, int OrgType, String usrLog, String token, int fronzeAmt, int payMode, int chargeStyle,
              int bDispPrice, String carNo, String carVin) {
        if (orgNo != UserConstants.ORG_I_CHARGE && payMode == EpConstants.P_M_FIRST)
            return ErrorCodeConstants.INVALID_ACCOUNT;

        if (orgNo != UserConstants.ORG_I_CHARGE) {
            if (orgNo == UserConstants.ORG_CCZC)//曹操专车检查车与枪连接状态，断开不能充电
            {
                int error = checkCarLinkStatus(epCode, epGunNo);
                if (error > 0) {
                    logger.error(LogUtil.addBaseExtLog("errorCode"),
                            new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, usrLog, error});
                    return error;
                }
            }
        }

        byte[] cmdTimes = WmIce104Util.timeToByte();
        logger.info(LogUtil.addBaseExtLog("chargeStyle|carNo|OrgType|ip"),
                new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, usrLog, chargeStyle,
                        carNo, carVin, OrgType, ip});

        int errorCode = EpChargeService.apiStartElectric(epCode,epGunNo, usrLog, null, (short) chargeStyle,
                fronzeAmt, payMode, orgNo, OrgType, ip, token, cmdTimes);
        if (errorCode > 0) {
            logger.error(LogUtil.addBaseExtLog("errorCode"),
                    new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, usrLog, errorCode});
        }

        return errorCode;
    }

    /**
     * 处理充电
     *
     * @param channel
     * @param h
     * @param m
     * @param s
     * @param epCode
     * @param epGunNo
     * @param OrgNo
     * @param usrLog
     * @param token
     * @param fronzeAmt
     * @param payMode
     * @param chargeStyle
     * @param bDispPrice
     * @param carNo
     * @param carVin
     */
    public static void handleCharge(Channel channel, int h, int m, int s, String epCode,
            int epGunNo, int orgNo, String usrLog, String token, int fronzeAmt, int payMode, int chargeStyle,
            int bDispPrice, String carNo, String carVin) {
        logger.info(LogUtil.addBaseExtLog("payMode|amt|chargeStyle|bDispPrice|carNo|carVin|token"),
                new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, usrLog,
                payMode, fronzeAmt, chargeStyle, bDispPrice, carNo, carVin, token});

        UsrGateClient usrGate = getClient(channel);
        if (usrGate == null) {
            logger.error(LogUtil.addBaseExtLog("not find usrGate,channel"),
                    new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, usrLog, channel});
            return;
        }
        usrGate.setLastUseTime(DateUtil.getCurrentSeconds());

        int errorCode = startCharge(channel, usrGate.getIp(), epCode, epGunNo, orgNo, usrGate.getUserOrigin().getCmdFromSource(), usrLog, token, fronzeAmt, payMode, chargeStyle,
                bDispPrice, carNo, carVin);
        byte successflag = 1;
        if (errorCode > 0) {
            logger.error(LogUtil.addBaseExtLog("fail errorCode|channel"),
                    new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, usrLog, errorCode, channel});
            successflag = 0;
        }
        String extraData = token;
        if (orgNo == UserConstants.ORG_CCZC || orgNo == UserConstants.ORG_CHAT) {
            extraData = EpChargeService.getExtraData_CCZC(epCode, epGunNo, usrLog, token, 0, successflag, errorCode);
        }
        byte[] data = UsrGateEncoder.charge(h, m, s, epCode, epGunNo, orgNo, usrLog, extraData, successflag, (short) errorCode);
        if (data != null) {
            UsrGateMessageSender.sendMessage(channel, data);
        }
    }

    /**
     * 发送车与枪连接状态
     *
     * @param status
     * @param channel
     * @param epCode
     * @param epGunNo
     * @param OrgNo
     * @param usrLog
     * @param token
     */
    public static void sendGun2CarLinkStatus(int status, Channel channel, String epCode, int epGunNo, int orgNo, String usrLog, String token) {
        byte time[] = WmIce104Util.timeToByte();
        byte[] pushData = UsrGateEncoder.do_gun2car_linkstatus(time[0], time[1], time[2], status, epCode, epGunNo, orgNo, usrLog, token);

        if (pushData == null) {
            logger.error(LogUtil.addBaseExtLog("token|status"),
                    new Object[]{LogConstants.FUNC_GUNLINK_STATUS, epCode, epGunNo, orgNo, usrLog, token, status});
        } else {
            String messagekey = usrLog + U2ECmdConstants.EP_GUN_CAR_STATUS;//
            UsrGateMessageSender.sendRepeatMessage(channel, pushData, messagekey);
            logger.debug(LogUtil.addBaseExtLog("token|status"),
                    new Object[]{LogConstants.FUNC_GUNLINK_STATUS, epCode, epGunNo, orgNo, usrLog, token, status});
        }
    }

    /**
     * 处理枪和车连接状态
     *
     * @param status
     * @param usrId
     * @param epCode
     * @param epGunNo
     */
    public static void handleGun2CarLinkStatus(int status, long usrId, String epCode, int epGunNo) {
        Iterator iter = cm.getMapClients().entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            UsrGateClient usrGate = (UsrGateClient) entry.getValue();
            if (null == usrGate || usrGate.getChannel() == null || !usrGate.isComm()
                    || usrGate.getUserOrigin() == null
                    || usrGate.getUserOrigin().getCmdFromSource() != UserConstants.CMD_FROM_PHONE) {
                continue;
            }
            if (usrGate.getStatus() < CommStatusConstant.INIT_SUCCESS) {
                continue;
            }
            if (status != 1) {
                status = 2;
            }
            int orgNo = UserConstants.ORG_I_CHARGE;
            String usrLog = "" + usrId;
            String token = "";

            sendGun2CarLinkStatus(status, usrGate.getChannel(), epCode, epGunNo, orgNo, usrLog, token);
        }
    }

    /**
     * 发送枪口工作状态
     *
     * @param status
     * @param channel
     * @param epCode
     * @param epGunNo
     * @param OrgNo
     * @param usrLog
     * @param token
     */
    public static void sendGunWorkStatus(int status, Channel channel, String epCode, int epGunNo, int OrgNo, String usrLog, String token) {
        byte time[] = WmIce104Util.timeToByte();

        byte[] pushData = UsrGateEncoder.do_gun_workstatus(time[0], time[1], time[2], status, epCode, epGunNo, OrgNo, usrLog, token);

        if (pushData == null) {
            logger.error("usrGate carLinkStatus fail pushData == null,accountId:{},gun2car link status:{}", usrLog, status);
        } else {
            String messagekey = usrLog + U2ECmdConstants.EP_GUN_WORK_STATUS;//
            UsrGateMessageSender.sendRepeatMessage(channel, pushData, messagekey);
            logger.debug("usrGate gunWorkStatus success accountId:{},gun workstatus:{},epCode:{},epGunNo:{}",
                    new Object[]{usrLog, status, epCode, epGunNo});
        }


    }

    /**
     * 发送枪工作状态
     *
     * @param status
     * @param usrId
     * @param epCode
     * @param epGunNo
     */
    public static void handleGunWorkStatus(int oldstatus, int status, long usrId, String epCode, int epGunNo) {
        Iterator iter = cm.getMapClients().entrySet().iterator();

        while (iter.hasNext()) {

            Map.Entry entry = (Map.Entry) iter.next();
            UsrGateClient usrGate = (UsrGateClient) entry.getValue();
            if (null == usrGate || usrGate.getChannel() == null || !usrGate.isComm()
                    || usrGate.getUserOrigin() == null
                    || usrGate.getUserOrigin().getCmdFromSource() != UserConstants.CMD_FROM_PHONE) {
                continue;
            }
            if (usrGate.getStatus() < CommStatusConstant.INIT_SUCCESS) {
                continue;
            }

            int orgNo = UserConstants.ORG_I_CHARGE;
            String usrLog = "" + usrId;
            String token = "" + oldstatus;

            sendGunWorkStatus(status, usrGate.getChannel(), epCode, epGunNo, orgNo, usrLog, token);
        }

    }

    /**
     * 处理停止充电命令
     *
     * @param channel
     * @param h
     * @param m
     * @param s
     * @param epCode
     * @param epGunNo
     * @param OrgNo
     * @param usrLog
     * @param token
     */
    public static void handleStopCharge(Channel channel, int h, int m, int s, String epCode,
            int epGunNo, int orgNo, String usrLog, String token) {
        logger.info(LogUtil.addBaseExtLog("token"),
                new Object[]{LogConstants.FUNC_STOP_CHARGE, epCode, epGunNo, orgNo, usrLog, token});

        UsrGateClient usrGate = getClient(channel);
        if (usrGate == null) {
            logger.error(LogUtil.addBaseExtLog("fail not find usrGate,token|channel"),
                    new Object[]{LogConstants.FUNC_STOP_CHARGE, epCode, epGunNo, orgNo, usrLog, token, channel});
            return;
        }

        setLastUseTime(channel);
        int errorCode = stopCharge(usrGate.getIp(), epCode, epGunNo, orgNo, usrLog, token);
        byte successflag = 1;
        if (errorCode > 0) {
            logger.error(LogUtil.addBaseExtLog("fail errorCode|channel"),
                    new Object[]{LogConstants.FUNC_STOP_CHARGE, epCode, epGunNo, orgNo, usrLog, errorCode, channel});
            successflag = 0;
        }
        String extraData = token;
        if (orgNo == UserConstants.ORG_CCZC || orgNo == UserConstants.ORG_CHAT) {
            EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
            if (epGunCache != null)
                extraData = epGunCache.getOrgNoExtra(orgNo, 1, successflag, errorCode);
        }
        byte[] data = UsrGateEncoder.stopCharge(h, m, s, epCode, epGunNo, orgNo, usrLog,
                extraData, successflag, (short) errorCode);
        if (data != null) {
            UsrGateMessageSender.sendMessage(channel, data);
        }
    }

    /**
     * 停止充电
     *
     * @param ip
     * @param epCode
     * @param epGunNo
     * @param OrgNo
     * @param usrLog
     * @param token
     * @return
     */
    public static int stopCharge(String ip, String epCode, int epGunNo, int orgNo, String usrLog, String token) {
        int usrId;
        if (orgNo != UserConstants.ORG_I_CHARGE) {
            usrId = AbstractUserService.findUserId(orgNo, usrLog);
        } else {
            usrId = Integer.parseInt(usrLog);
        }
        if (usrId <= 0) {
            logger.error(LogUtil.addBaseExtLog("token"),
                    new Object[]{LogConstants.FUNC_START_CHARGE, epCode, epGunNo, orgNo, usrLog, token});
            return ErrorCodeConstants.INVALID_ACCOUNT;
        }

        int errorCode = EpChargeService.apiStopElectric(epCode,
                epGunNo, orgNo, usrId, usrLog, orgNo, ip);
        if (errorCode > 0) {
            logger.error(LogUtil.addBaseExtLog("fail errorCode|token"),
                    new Object[]{LogConstants.FUNC_STOP_CHARGE, epCode, epGunNo, orgNo, usrLog, errorCode, token});
        }
        return errorCode;
    }


    /**
     * @param channel
     */
    public static void offLine(Channel channel) {
        UsrGateClient usrGate = getClient(channel);

        if (usrGate != null) {
            logger.info(LogUtil.addExtLog("offLine. commClient|Identity"), usrGate.getChannel(), usrGate.getIdentity());
            channel.close();

            usrGate.setStatus(0);
            removeClient(usrGate.getIdentity());
            removeClient(channel);
        }
    }

    public static void startCommClientTimeout(long initDelay) {
        CheckUsrGateTask checkTask = new CheckUsrGateTask();

        TaskPoolFactory.scheduleAtFixedRate("CHECK_USRGATE_TIMEOUT_TASK", checkTask, initDelay, 10, TimeUnit.SECONDS);
    }

    @SuppressWarnings("rawtypes")
    public synchronized static void checkTimeOut() {
        String msg = cm.checkTimeOut(GameConfig.usrGateNoInitTimeout, GameConfig.usrGateTimeout);

        logger.info(LogUtil.addExtLog("checkTimeOut"), msg);
    }

    public static boolean isComm(UserOrigin userOrigin) {
        String actionIdentity = "";
        if (userOrigin != null)
            actionIdentity = userOrigin.getCmdChIdentity();

        if (actionIdentity.length() == 0) {
            logger.error("usrGate actionIdentity is null,userOrigin:{}", userOrigin);
            return false;
        }
        UsrGateClient usrGate = getClient(actionIdentity);
        if (usrGate == null) {
            logger.error("usrGate not find usrGateClient,actionIdentity:{}", actionIdentity);
            return false;
        }
        return usrGate.isComm();
    }

    public static void onEvent(int type, UserOrigin userOrigin, int ret, int cause, Object srcParams, Object extraData) {
        try {
            String actionIdentity = "";
            if (userOrigin != null)
                actionIdentity = userOrigin.getCmdChIdentity();
            logger.debug("usrGate onEvent type:{},actionIdentity:{},extraData:{}", new Object[]{type, actionIdentity, extraData});


            UsrGateClient usrGate = getClient(actionIdentity);
            if (usrGate == null) {
                logger.info("usrGate onEvent fail, not find UsrGateClient,actionIdentity:{},type:{}", actionIdentity, type);
                return;
            }
            if (!usrGate.isComm()) {
                logger.info("usrGate onEvent fail, UsrGateClient isComm=false,actionIdentity:{},type:{}", actionIdentity, type);
                return;
            }
            switch (type) {
                case EventConstant.EVENT_BESPOKE:
                    break;

                case EventConstant.EVENT_CHARGE_EP_RESP: {
                    Map<String, Object> paramsMap = (Map<String, Object>) extraData;

                    String epCode = (String) paramsMap.get("epcode");
                    int epGunNo = (int) paramsMap.get("epgunno");
                    int OrgNo = (int) paramsMap.get("orgn");
                    String usrLog = (String) paramsMap.get("usrLog");
                    String token = (String) paramsMap.get("token");
                    String extra = (String) paramsMap.get("extraData");
                    logger.debug("usrGate service onEvent ,OrgNo:{}:extra:{}", OrgNo, extra);

                    byte[] time = WmIce104Util.timeToByte();

                    if (OrgNo == UserConstants.ORG_CCZC || OrgNo == UserConstants.ORG_CHAT) {
                        token = extra;
                    }

                    byte[] data = UsrGateEncoder.charge(time[0], time[1], time[2], epCode, epGunNo, OrgNo, usrLog, token, ret, (short) cause);

                    if (data != null) {
                        UsrGateMessageSender.sendMessage(usrGate.getChannel(), data);
                    }
                    logger.debug("usrGate service onEvent EVENT_CHARGE,OrgNo:{}:usrLog:{},token:{}\n",
                            new Object[]{OrgNo, usrLog, extra});
                }
                break;

                case EventConstant.EVENT_STOP_CHARGE_EP_RESP: {
                    Map<String, Object> paramsMap = (Map<String, Object>) extraData;

                    String epCode = (String) paramsMap.get("epcode");
                    int epGunNo = (int) paramsMap.get("epgunno");
                    int OrgNo = (int) paramsMap.get("orgn");
                    //if (OrgNo == UserConstants.ORG_CHAT) return;

                    String usrLog = (String) paramsMap.get("usrLog");
                    String token = (String) paramsMap.get("token");
                    String extra = (String) paramsMap.get("extraData");

                    byte[] time = WmIce104Util.timeToByte();

                    if (OrgNo == UserConstants.ORG_CCZC || OrgNo == UserConstants.ORG_EC || OrgNo == UserConstants.ORG_CHAT) {
                        token = extra;
                    }

                    byte[] data = UsrGateEncoder.stopCharge(time[0], time[1], time[2], epCode, epGunNo, OrgNo, usrLog, token, (byte) ret, (short) cause);

                    if (data != null) {
                        UsrGateMessageSender.sendMessage(usrGate.getChannel(), data);
                    }
                    logger.debug("usrGate service onEvent EVENT_STOP_CHARGE\n");
                }
                break;
                case EventConstant.EVENT_CONSUME_RECORD: {
                    Map<String, Object> paramsMap = (Map<String, Object>) srcParams;

                    String epCode = (String) paramsMap.get("epcode");
                    int epGunNo = (int) paramsMap.get("epgunno");

                    int OrgNo = (int) paramsMap.get("orgn");
                    String usrLog = (String) paramsMap.get("usrLog");
                    String token = (String) paramsMap.get("token");

                    int pkEpId = (int) paramsMap.get("pkEpId");

                    String chargeOrder = (String) paramsMap.get("orderid");
                    int usrId = (int) paramsMap.get("usrId");
                    int userFirst = (int) paramsMap.get("userFirst");

                    ConsumeRecord consumeRecord = (ConsumeRecord) extraData;
                    int couPonAmt = (int) paramsMap.get("couPonAmt");
                    int realCouPonAmt = (int) paramsMap.get("realCouPonAmt");
                    if (consumeRecord.getType() == 1) {
                        couPonAmt = couPonAmt / 100;
                        realCouPonAmt = realCouPonAmt / 100;
                    }

                    byte[] time = WmIce104Util.timeToByte();
                    byte[] data = null;
                    if (OrgNo == 0)
                        OrgNo = UserConstants.ORG_I_CHARGE;
                    if (OrgNo == UserConstants.ORG_I_CHARGE) {
                        if (consumeRecord.getType() == 1) {
                            data = UsrGateEncoder.IchargeRecord(time[0], time[1], time[2], epCode, epGunNo, OrgNo, usrLog, token, pkEpId,
                                    chargeOrder, consumeRecord.getStartTime(), consumeRecord.getEndTime(),
                                    consumeRecord.getTotalDl(), consumeRecord.getTotalChargeAmt() / 100,
                                    consumeRecord.getServiceAmt() / 100, userFirst, couPonAmt, realCouPonAmt);
                        } else {
                            data = UsrGateEncoder.IchargeRecord(time[0], time[1], time[2], epCode, epGunNo, OrgNo, usrLog, token, pkEpId,
                                    chargeOrder, consumeRecord.getStartTime(), consumeRecord.getEndTime(),
                                    consumeRecord.getTotalDl(), consumeRecord.getTotalChargeAmt(),
                                    consumeRecord.getServiceAmt(), userFirst, couPonAmt, realCouPonAmt);
                        }
                    } else {
                        data = UsrGateEncoder.chargeRecord(time[0], time[1], time[2], epCode, epGunNo, OrgNo, usrLog, token,
                                chargeOrder, consumeRecord, userFirst, couPonAmt, realCouPonAmt);

                    }
                    if (data != null) {
                        String messagekey = usrGate.getChannel().toString() + usrId + U2ECmdConstants.EP_CONSUME_RECODE;//+time[0]+time[1]+time[2];
                        UsrGateMessageSender.sendRepeatMessage(usrGate.getChannel(), data, messagekey);
                    }
                    logger.debug("usrGate service onEvent EVENT_CONSUME_RECORD\n");
                }
                break;
                case EventConstant.EVENT_REAL_CHARGING: {
                    Map<String, Object> paramsMap = (Map<String, Object>) srcParams;

                    String epCode = (String) paramsMap.get("epcode");
                    int epGunNo = (int) paramsMap.get("epgunno");
                    int usrId = (int) paramsMap.get("usrId");
                    int OrgNo = (int) paramsMap.get("orgn");
                    String usrLog = (String) paramsMap.get("usrLog");
                    String token = (String) paramsMap.get("token");


                    logger.debug("usrGate service onEvent EVENT_REAL_CHARGING,orgn:{},usrId:{}\n", OrgNo, usrId);
                    if (extraData == null) {
                        logger.error("usrGate service onEvent EVENT_REAL_CHARGING error,extraData==null");
                        return;
                    }
                    if (OrgNo == 0)
                        OrgNo = UserConstants.ORG_I_CHARGE;
                    if (OrgNo != UserConstants.ORG_I_CHARGE && OrgNo != UserConstants.ORG_CHAT) {
                        logger.error("usrGate service onEvent EVENT_REAL_CHARGING error,OrgNo:{}", OrgNo);
                        return; //只有爱充的手机实时数据发送
                    }
                    UserCache usr = UserService.getUserCache((int) usrId);
                    if (usr == null) {
                        logger.info("usrGate service onEvent  EVENT_REAL_CHARGING fail,usr is null or usr is not online");
                        break;
                    }
                    byte[] time = WmIce104Util.timeToByte();

                    ChargingInfo chargingInfo = (ChargingInfo) extraData;

                    byte[] data = UsrGateEncoder.chargeRealInfo(time[0], time[1], time[2], epCode, epGunNo, OrgNo, usrLog,
                            token, chargingInfo);
                    UsrGateMessageSender.sendMessage(usrGate.getChannel(), data);
                    logger.debug("usrGate service onEvent EVENT_REAL_CHARGING\n");
                }
                break;
                case EventConstant.EVENT_START_CHARGE_EVENT: {
                    Map<String, Object> paramsMap = (Map<String, Object>) extraData;

                    String epCode = (String) paramsMap.get("epcode");
                    int epGunNo = (int) paramsMap.get("epgunno");
                    int OrgNo = (int) paramsMap.get("orgn");
                    String usrLog = (String) paramsMap.get("usrLog");
                    String token = (String) paramsMap.get("token");
                    String extra = (String) paramsMap.get("extraData");
                    byte[] time = WmIce104Util.timeToByte();

                    if (OrgNo == UserConstants.ORG_EC || OrgNo == UserConstants.ORG_CHAT) {
                        token = extra;
                    }
                    byte[] data = UsrGateEncoder.chargeEvent(time[0], time[1], time[2], epCode, epGunNo,
                            OrgNo, usrLog, token, ret);

                    UsrGateMessageSender.sendMessage(usrGate.getChannel(), data);
                    logger.debug("usrGate service onEvent EVENT_START_CHARGE_EVENT\n");
                }
                break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error(LogUtil.addExtLog("exception"), e.getStackTrace());
            return;
        }
    }

    public static void setLastUseTime(Channel channel) {
        UsrGateClient usrGate = getClient(channel);
        if (usrGate != null) {
            usrGate.setLastUseTime(DateUtil.getCurrentSeconds());
        }
    }

    /**
     * 广播到所有用户gate电桩状态
     *
     * @param msg
     */
    public static void notifyUsrGate(byte[] msg) {
        Iterator iter = cm.getMapClients().entrySet().iterator();

        while (iter.hasNext()) {

            Map.Entry entry = (Map.Entry) iter.next();
            UsrGateClient usrGate = (UsrGateClient) entry.getValue();
            if (null == usrGate || usrGate.getChannel() == null || !usrGate.isComm()) {
                continue;
            }
            if (usrGate.getStatus() < CommStatusConstant.INIT_SUCCESS) {
                continue;
            }
            UsrGateMessageSender.sendMessage(usrGate.getChannel(), msg);
        }
    }

    public static String stat() {
        final StringBuilder sb = new StringBuilder();

        sb.append("getCacheSize()：").append(getCacheSize()).append(" \n\n");

        Iterator iter = cm.getMapClients().entrySet().iterator();

        while (iter.hasNext()) {

            Map.Entry entry = (Map.Entry) iter.next();

            UsrGateClient usrGate = (UsrGateClient) entry.getValue();
            if (usrGate != null) {
                sb.append(usrGate.toString()).append(" \n");
            }
        }
        return sb.toString();
    }

    /**
     * 曹操专车查询订单
     *
     * @param channel
     * @param h
     * @param m
     * @param s
     * @param epCode
     * @param epGunNo
     * @param OrgNo
     * @param usrLog
     * @param token
     */
    public static void handleQueryOrder(Channel channel, int h, int m, int s, String epCode,
            int epGunNo, int orgNo, String usrLog, String token) {
        logger.info(LogUtil.addBaseExtLog("extra"),
                new Object[]{LogConstants.FUNC_QUERY_ORDER, epCode, epGunNo, orgNo, usrLog, token});

        UsrGateClient usrGate = getClient(channel);
        if (usrGate == null) {
            logger.error(LogUtil.addExtLog("fail not find usrGate,channel"), channel);
            return;
        }
        setLastUseTime(channel);

        byte data[] = null;
        String extraData = "";
        int errorCode = checkQueryOrderParam(epCode, epGunNo, orgNo, usrLog, token);
        if (errorCode > 0)//查询失败
        {
            data = UsrGateEncoder.orderInfo(h, m, s, epCode, epGunNo, orgNo, usrLog,
                    extraData, 0, errorCode);
            logger.error(LogUtil.addBaseExtLog("fail errorCode|token"),
                    new Object[]{LogConstants.FUNC_QUERY_ORDER, epCode, epGunNo, orgNo, usrLog, errorCode, token});
        } else //查询成功
        {
            extraData = EpChargeService.getExtraData_CCZC(epCode, epGunNo, usrLog,
                    token, 3, 1, 0);
            data = UsrGateEncoder.orderInfo(h, m, s, epCode, epGunNo, orgNo, usrLog,
                    extraData, 1, 0);
        }
        if (data != null) {
            UsrGateMessageSender.sendMessage(channel, data);
            logger.debug("usrGate handleQueryOrder send success");
        }
    }

    /**
     * 曹操专车查询订单参数检查
     *
     * @param epCode
     * @param epGunNo
     * @param OrgNo
     * @param usrLog
     * @param token
     * @return
     */
    public static int checkQueryOrderParam(String epCode, int epGunNo, int orgNo, String usrLog, String token) {
        BaseEPCache epCache = CacheService.getEpCache(epCode);
        if (epCache == null) {
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache == null) {
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        if (epGunCache.getChargeCache() == null) {
            return 1;
        }
        if (orgNo != UserConstants.ORG_CCZC && orgNo != UserConstants.ORG_CHAT) {
            return ErrorCodeConstants.INVALID_ACCOUNT;
        }
        if (epGunCache.getChargeCache().getUserOrigin() == null
                || epGunCache.getChargeCache().getUserOrigin().getOrgNo() != orgNo) {
            return ErrorCodeConstants.INVALID_ACCOUNT;
        }

        int usrId = UserService.getUserIdByOrgNo(orgNo);
        if (usrId <= 0) {
            return ErrorCodeConstants.INVALID_ACCOUNT;
        }
        if (usrLog == null || !usrLog.equals(epGunCache.getChargeCache().getUserIdentity()) ||
                epGunCache.getChargeCache().getUserId() != usrId) {
            return ErrorCodeConstants.INVALID_ACCOUNT;
        }
        return 0;
    }

    /**
     * 检查车与枪连接状态，曹操专车
     *
     * @param epCode
     * @param epGunNo
     * @return
     */
    public static int checkCarLinkStatus(String epCode, int epGunNo) {
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if (epGunCache == null) return ErrorCodeConstants.EP_UNCONNECTED;
        RealChargeInfo realInfo = epGunCache.getRealChargeInfo();
        if (realInfo == null) return ErrorCodeConstants.EP_UNCONNECTED;
        return realInfo.checkCarLinkStatus();
    }
}
