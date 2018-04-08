package com.ec.epcore.service;

import com.ec.cache.RateInfoCache;
import com.ec.constants.EpConstants;
import com.ec.constants.ErrorCodeConstants;
import com.ec.constants.YXCConstants;
import com.ec.epcore.cache.*;
import com.ec.epcore.config.GameConfig;
import com.ec.epcore.net.client.EpCommClient;
import com.ec.epcore.net.codec.ApiEncoder;
import com.ec.epcore.net.codec.EpEncoder;
import com.ec.epcore.net.codec.UsrGateEncoder;
import com.ec.epcore.sender.EpMessageSender;
import com.ec.service.impl.EpGunServiceImpl;
import com.ec.service.impl.EpServiceImpl;
import com.ec.epcore.task.CheckMeterTask;
import com.ec.net.proto.Iec104Constant;
import com.ec.net.proto.SingleInfo;
import com.ec.net.proto.WmIce104Util;
import com.ec.netcore.conf.CoreConfig;
import com.ec.netcore.core.pool.TaskPoolFactory;
import com.ec.service.AbstractEpService;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import com.ormcore.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class EpService extends AbstractEpService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(EpService.class.getName()));

    /**
     * 取最近更新过费率的电桩
     */
    public static void checkModifyRate()
    {
        List<TblElectricPile> epList = EpServiceImpl.getLastUpdate();
        int size = epList.size();
        logger.info("checkModifyRate,size:{}", size);
        if (epList == null) return;
        for(int i=0;i<size;i++)
        {
            TblElectricPile epInfo=epList.get(i);
            ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epInfo.getEpCode());
            if(epCache == null)
                continue;

            epCache.setRateid(epInfo.getRateid());
            epCache.setTypeSpanId(epInfo.getEpTypeSpanId());
        }


    }

    public static ElectricPileCache loadDiscreteEpConnect(int commVersion, String epCode, int bootStatus) {
        boolean bFrist = false;
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);
        if (epCache == null) {
            bFrist = true;
            CacheService.addEpCache(new ElectricPileCache(epCode));
            epCache = (ElectricPileCache)getEpCacheFromDB(epCode);
            if (epCache == null) return null;
        }

        int concentratorId = epCache.getConcentratorId();
        if (concentratorId > 0) {
            logger.error("initConnect,epCode:{},fail dbEp.getStationId()>0,stationId:{}", epCode, concentratorId);
            return null;
        }

        int gateId = CoreConfig.gameConfig.getId();
        epCache.setGateid(gateId);
        if (bootStatus == 1) {
            epCache.initGuns(bootStatus);
            return epCache;
        }
        // 每次连接上来更新下最大临时充电次数
        int MaxNum = EpService.getTempChargeMaxNumFromDB(epCache.getCompany_number());
        epCache.setTempChargeMaxNum(MaxNum);
        // 初始化设备硬件版本信息
        EqVersionService.getEpVersionFromDB(epCache, 1);

        // 初始化枪对象
        if (bFrist) {
            if (!epCache.initGuns(0)) {
                return null;
            }
        }

        return epCache;
    }

    public static boolean imitateInitDiscreteEp(int commVersion,String epCode)
    {
        ElectricPileCache epCache= loadDiscreteEpConnect(commVersion, epCode, 0);
        if(epCache==null)
        {
            //logger.error("imitateInitDiscreteEp epCache=null,epCode:{}",epCode);
            return false;
        }
        int gateId = CoreConfig.gameConfig.getId();

        //模拟电桩连接
        EpCommClient epCommClient = new EpCommClient();

        epCommClient.initNetSuccess(epCode, commVersion, EpConstants.COMM_MODE_OF_DISCRETE_POLE);

        epCommClient.setBootStatus(2);
        epCommClient.setGateId(gateId);
        epCommClient.setRevINum(0);

        epCache.setEpNetObject(epCommClient);
        epCache.setGateid(gateId);

        epCache.setLastUseTime(DateUtil.getCurrentSeconds());


        //更新桩状态

        EpServiceImpl.updateEpCommStatusToDb(epCache.getPkEpId(), 0, epCache.getGateid());

        //增加到缓存
        CacheService.addEpCache(epCache);


        return true;


    }

    public static boolean initDiscreteEpConnect(int commVersion,String epCode,EpCommClient epCommClient,int bootStatus)
    {
        //1.判断是否是有效桩体
        ElectricPileCache epCache = loadDiscreteEpConnect(commVersion,epCode,bootStatus);//1.判断缓存里时候有

        if(epCache == null )
        {
            logger.error("initConnect fail,epCode:{},dbEp == null,close channel:{}",epCode,epCommClient.getChannel());

            //2.无效桩，强制关闭
            epCommClient.close();
            EpCommClientService.removeEpCommClient(epCommClient);
            return false;
        }
        if(epCache.getConcentratorId()>0)
        {
            logger.error("initConnect fail,epCode:{},epCache.getConcentratorId()>0,concentratorID:{},close channel:{}",
                    new Object[]{epCode,epCache.getConcentratorId(),epCommClient.getChannel()});
            //2.无效桩，强制关闭
            epCommClient.close();
            EpCommClientService.removeEpCommClient(epCommClient);
            return false;
        }

        int gateId = CoreConfig.gameConfig.getId();
        EpCommClientService.handleOldClient(epCommClient, epCache.getCode());

        epCommClient.initNetSuccess(epCode, commVersion, EpConstants.COMM_MODE_OF_DISCRETE_POLE);
        epCommClient.setBootStatus(bootStatus);
        epCommClient.setGateId(gateId);
        epCommClient.setRevINum(0);

        epCache.setEpNetObject(epCommClient);
        epCache.setGateid(gateId);
        updateNetObject(epCache);

        byte[] changeGateData = ApiEncoder.epIpChange(epCode,epCache.getGateid());
        AppClientService.notifyEpGate(changeGateData);

        //更新桩状态
        EpService.onEpCommStatusChange(epCode, epCache.getPkEpId(), 1, epCache.getGateid());
        //增加到缓存
        CacheService.addEpCache(epCache);
        epCache.setLastUseTime(DateUtil.getCurrentSeconds());

        logger.info(LogUtil.addExtLog("epCode|getId|commVersion|boot|channel"),
                new Object[]{epCode, epCache.getGateid(), commVersion, bootStatus, epCommClient.getChannel()});
        return true;
    }

    public static boolean updateNetObject(ElectricPileCache epCache) {

        for (int i = 1; i <= epCache.getGunNum(); i++) {
            EpGunCache gunCache = (EpGunCache) CacheService.getEpGunCache(epCache.getCode(), i);

            if (gunCache != null) {
                gunCache.setConcentratorId(epCache.getConcentratorId());
                CacheService.putEpGunCache(gunCache);

                gunCache.setEpNetObject(epCache.getEpNetObject());
            } else {
                return false;
            }
        }

        return true;
    }

    public static void onEpCommClientDelete(ElectricPileCache epCache) {
        for (int i = 1; i <= epCache.getGunNum(); i++) {
            EpGunCache loopEpGunCache = (EpGunCache) CacheService.getEpGunCache(epCache.getCode(), i);
            loopEpGunCache.setConcentratorId(epCache.getConcentratorId());
            loopEpGunCache.onNetStatus(0);
            loopEpGunCache.setEpNetObject(null);
        }
    }

    public static void onEpCommStatusChange(String epCode,int pkEpId,int commStatus,int gateId)
    {
        EpServiceImpl.updateEpCommStatusToDb(pkEpId, commStatus, gateId);
        ArrayList<String> epCodes = new ArrayList<String>();
        epCodes.add(epCode);
        EpService.sendEpStatusToUsrGate(epCodes, commStatus);
    }

    public static void updateEpsRate(String epcodes,int rateid,RateInfoCache rateInfo)
    {
        String[] epCodeArray = epcodes.split(",+");
        for(String eachEpCode : epCodeArray)
        {
            int errorCode = updateEpRate(eachEpCode,rateid,rateInfo);

            logger.info("[Rate]send rateinfo to ep,errorCode(0:success,1:fail):{},epCode:{},rateId:{}",
                    new Object[]{errorCode,eachEpCode,rateid});

        }
    }

    public static int updateEpRate(String epCode,int rateid, RateInfoCache info)
    {
        ElectricPileCache epClient = (ElectricPileCache)CacheService.getEpCache(epCode);
        if(epClient == null)
        {
            return 1;
        }
        epClient.setRateid(rateid);

        EpCommClient commClient = (EpCommClient)epClient.getEpNetObject();

        if(commClient == null || !commClient.isComm())
        {
            return 2;
        }

        byte []cmdTimes = WmIce104Util.timeToByte();

        byte[] bRateData= EpEncoder.do_consume_model(epCode,info);

        if(bRateData ==null)
        {
            logger.error("[Rate]updateEpRate,bRateData ==null exception,epCode:{},rateid:{}",epCode,rateid);
            return 1;
        }

        int modelId = Iec104Constant.C_CONSUME_MODEL;
        if (info.getRateInfo().getModelId() == 2)
            modelId = Iec104Constant.C_CONSUME_MODEL4;
        EpMessageSender.sendMessage(commClient, 0, 0, modelId, bRateData, cmdTimes, commClient.getVersion());

        return 0;
    }


    private static void sendRateInfoByGroup(String epCodes,int rateId,RateInfoCache rateInfo)
    {
        String[] epCodeArray = epCodes.split(",+");
        for(String epCodeEach : epCodeArray)
        {
            ElectricPileCache epClient = (ElectricPileCache)CacheService.getEpCache(epCodeEach);
            if(epClient == null){
                logger.error("[Rate]sendRateInfoByGroup not find ElectricPile:{},rateId:{}",epCodeEach,rateId);
                continue;
            }
            //EpCommClient commClient = EpCommClientService.getCommClient(epClient.getConcentratorId(),epClient.getCode());
            EpCommClient commClient = (EpCommClient)epClient.getEpNetObject();
            if(commClient==null || !commClient.isComm()) {
                logger.error("[Rate]sendRateInfoByGroup ElectricPile:{} is not comm",epCodeEach);
                continue;
            }

            byte[] cmdTimes =  WmIce104Util.timeToByte();
            byte[] bConsumeModelProtoData= EpEncoder.do_consume_model(epCodeEach,rateInfo);
            if(bConsumeModelProtoData == null)
            {
                logger.error("[Rate]sendRateInfoByGroup epCode:{},rateId:{},bConsumeModelProtoData == null exception",epCodeEach,rateId);
                continue;
            }
            EpMessageSender.sendMessage(commClient,0,0,Iec104Constant.C_CONSUME_MODEL, bConsumeModelProtoData,cmdTimes,commClient.getVersion());
            epClient.setRateid(rateId);
        }
    }

    public static void handleCommClientTimeOut(String clientIdentity,int gateId)
    {
        if(clientIdentity==null || clientIdentity.length()<1)
        {
            logger.info("[epChannel],handleCommClientTimeOut clientIdentity is empty");
            return;
        }

        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(clientIdentity);

        if(epCache == null)
        {
            logger.info("[epChannel],handleCommClientTimeOut not find ep:{}",clientIdentity);
        }
        else if (epCache.getEpNetObject() != null)
        {
            logger.info("[epChannel],handleCommClientTimeOut clientIdentity:{}",clientIdentity);
            epCache.onNetStatus(0);

            EpService.onEpCommStatusChange(epCache.getCode(),epCache.getPkEpId(), 0, gateId);
            epCache.setEpNetObject(null);

        }

    }
    public static int doNearCallEpAction(String epCode,int type,int time,int  accountId,float lng,float lag)

    {
        ElectricPileCache epCache= (ElectricPileCache)CacheService.getEpCache(epCode);
        if(epCache==null)
        {
            logger.info("not find ElectricPileCache,epCode:{}",epCode);
            return ErrorCodeConstants.EP_UNCONNECTED;

        }
        //检查电桩
        if (getEpRealStatus(epCache) == null) return ErrorCodeConstants.EP_UNCONNECTED;
        int errorCode = epCache.canNearCall();
        if(errorCode > 0)
        {
            logger.info("doNearCallEpAction fail.epCode:{},errorCode",epCode,errorCode);
            return errorCode;
        }
        for(int i=0;i<epCache.getGunNum();i++)
        {
            EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, i+1);
            if(epGunCache==null)
                continue;
        }

        return epCache.callEpAction(type, time,(float)0.0,(float)0.0);
    }

    public static void handleEpGateChange(String epCode, int newGateId) {
        byte[] msg = ApiEncoder.notifyEpGate(epCode, newGateId);

        // 遍历,每个连接上的后台都发一份
        AppClientService.notifyEpGate(msg);
    }

    public static int OpenGunLid(String epCode,int epGunNo)
    {
        int errorCode = 0;
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);//1.判断缓存里时候有
        if(epCache == null )
        {
            logger.info("OpenGunLid did not find ep:" + epCode
                    + "\r\n");
            return ErrorCodeConstants.INVALID_EP_CODE;
        }
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if(epGunCache == null)
        {
            return ErrorCodeConstants.INVALID_EP_CODE;
        }

        if(!epGunCache.getEpNetObject().isComm())
        {
            return ErrorCodeConstants.EP_UNCONNECTED;

        }
        RealACChargeInfo chargeInfo =(RealACChargeInfo)(epGunCache.getRealChargeInfo());
        if(chargeInfo.getGunLidStatus() ==0)
        {
            return ErrorCodeConstants.EPE_GUN_LID_OPEND;
        }

        //EpGunService.testChangeGunSignalStatus(epCode,epGunNo,1);

        return 0;
    }
    //////////////////////////////////////////////

    public static int DropGroupLock(String epCode,int epGunNo)
    {
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);
        if(epCache == null )
        {
            logger.info("DropGroupLock did not find ep:" + epCode
                    + "\r\n");
            return ErrorCodeConstants.INVALID_EP_CODE;
        }


        return 0;
    }

    public static int EpStat(String epCode)
    {
        ElectricPileCache epClient= (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epClient == null )
        {
            return ErrorCodeConstants.INVALID_EP_CODE;
        }
        EpCommClient commClient = (EpCommClient)epClient.getEpNetObject();
        if(commClient==null || !commClient.isComm()) {
            logger.error("EpStat ElectricPile is not comm,epCode:{}",epCode);
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        byte []cmdTimes = WmIce104Util.timeToByte();

        byte[] statData= EpEncoder.do_ep_stat(epCode);
        EpMessageSender.sendMessage(commClient,0,0,Iec104Constant.C_EP_STAT, statData,cmdTimes,commClient.getVersion());


        return 0;
    }
    public static int getGunNo(int address,int type)
    {
        switch(type)
        {
            case 1:
                return (address/128)+1;

            case 3:
                return (address/128)+1;
            case 11:
                return (address/2500)+1;
            case 132:
                return (address/128)+1;
            default:
                return 0;
        }
    }

    public static void handleOneBitYxInfo(String epCode,Vector<SingleInfo> singleInfos)
    {
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epCache==null)
        {
            logger.error("handleOneBitYxInfo,realData dataType:1=oneBitYx,epCode:{},not find ElectricPileCache",epCode);
            return ;
        }
        Map<String, GunPointMap> pointMaps = new ConcurrentHashMap<String,GunPointMap>();
        for(int i=0;i< singleInfos.size(); i++ )
        {
            SingleInfo info = singleInfos.get(i);
            int address = info.getAddress();//紧凑排列
            int yxAddress = address%128;

            if(!EpChargeService.isValidAddress(yxAddress, 1))
            {
                logger.debug("handleOneBitYxInfo,realData dataType:1=oneBitYx,epCode:{},address:{}, isValid yxAddress:{}",
                        new Object[]{epCode,address,yxAddress});
                continue;
            }


            int epGunNo = EpService.getGunNo(address,1);
            if(epGunNo<0)
            {
                logger.error("handleOneBitYxInfo,realData dataType:1=oneBitYx,epCode:{},epGunNo:{},address:{} invalid gun no",
                        new Object[]{epCode,epGunNo,address});
                continue;
            }
            GunPointMap gunPointMap= pointMaps.get(epCode+epGunNo);
            if(gunPointMap==null)
            {
                gunPointMap = new GunPointMap(epCode,epGunNo);
                pointMaps.put(epCode+epGunNo, gunPointMap);
            }


            info.setAddress(yxAddress);
            RealChargeInfo.AddPoint(gunPointMap.getPointMap(), info);
        }


        disptachPointToGun(1,pointMaps);
    }

    public static void handleOneBitYxInfo_v4(String epCode,int epGunNo,Vector<SingleInfo> singleInfos)
    {
        if(epGunNo<0)
        {
            logger.error("handleOneBitYxInfo_v4,realData dataType:1=oneBitYx,epCode:{},epGunNo:{},invalid gun no",epCode,epGunNo);
            return ;
        }
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);
        if(epCache==null)
        {
            logger.error("handleOneBitYxInfo_v4,realData dataType:1=oneBitYx,epCode:{},epGunNo:{},not find ElectricPileCache",epCode,epGunNo);
            return ;
        }
        GunPointMap gunPointMap=new GunPointMap(epCode,epGunNo);
        for(int i=0;i< singleInfos.size(); i++ )
        {
            SingleInfo info = singleInfos.get(i);
            int address = info.getAddress();//紧凑排列
            int yxAddress = address%128;

            if(!EpChargeService.isValidAddress(yxAddress, 1))
            {
                logger.debug("handleOneBitYxInfo_v4,realData dataType:1=oneBitYx,epCode:{},epGunNo:{},isValid address:{},yxAddress:{}",
                        new Object[]{epCode, epGunNo, address, yxAddress});
                continue;
            }
            info.setAddress(yxAddress);
            RealChargeInfo.AddPoint(gunPointMap.getPointMap(), info);
        }

        EpGunCache epGunCache =  (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if(epGunCache !=null)
        {
            epGunCache.onRealDataChange(gunPointMap.getPointMap(), 1);
        }
    }

    public static void handleTwoBitYxInfo(String epCode,Vector<SingleInfo> singleInfos)
    {
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epCache==null)
        {
            logger.error("handleTwoBitYxInfo,realData dataType:2=twoBitYx,epCode:{},not find ElectricPileCache",epCode);
            return ;
        }

        Map<String, GunPointMap> pointMaps = new ConcurrentHashMap<String,GunPointMap>();

        for(int i=0;i< singleInfos.size(); i++ )
        {
            SingleInfo info = singleInfos.get(i);
            int address = info.getAddress();//紧凑排列
            int yxAddress = address%128;
            if(!EpChargeService.isValidAddress(yxAddress, 3))
            {
                logger.debug("handleTwoBitYxInfo,realData dataType:2=twoBitYx,epCode:{},address:{}, isValid yxAddress:{}",
                        new Object[]{epCode,address,yxAddress});
                continue;
            }

            int epGunNo = EpService.getGunNo(address,3);
            if(epGunNo<0)
            {
                logger.error("handleTwoBitYxInfo,realData dataType:2=twoBitYx,epCode:{},epGunNo:{},address:{} invalid gun no",
                        new Object[]{epCode,epGunNo,address});
                continue;
            }

            GunPointMap gunPointMap= pointMaps.get(epCode+epGunNo);
            if(gunPointMap==null)
            {
                gunPointMap = new GunPointMap(epCode,epGunNo);
                pointMaps.put(epCode+epGunNo, gunPointMap);
            }

            info.setAddress(yxAddress+YXCConstants.YX_2_START_POS);
            RealChargeInfo.AddPoint(gunPointMap.getPointMap(), info);
        }
        disptachPointToGun(1,pointMaps);
    }

    public static void handleTwoBitYxInfo_v4(String epCode,int epGunNo,Vector<SingleInfo> singleInfos)
    {
        if(epGunNo<0)
        {
            logger.error("handleTwoBitYxInfo_v4,realData dataType:2=twoBitYx,epCode:{},epGunNo:{},invalid gun no",epCode,epGunNo);
            return ;
        }
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);
        if(epCache==null)
        {
            logger.error("handleTwoBitYxInfo_v4,realData dataType:2=twoBitYx,epCode:{},epGunNo:{},not find ElectricPileCache",epCode,epGunNo);
            return ;
        }
        GunPointMap gunPointMap=new GunPointMap(epCode,epGunNo);

        for(int i=0;i< singleInfos.size(); i++ )
        {
            SingleInfo info = singleInfos.get(i);
            int address = info.getAddress();//紧凑排列
            int yxAddress = address%128;
            if(!EpChargeService.isValidAddress(yxAddress, 3))
            {
                logger.debug("handleTwoBitYxInfo_v4,realData dataType:2=twoBitYx,epCode:{},epGunNo:{},isValid address:{},yxAddress:{}",
                        new Object[]{epCode, epGunNo, address, yxAddress});
                continue;
            }

            info.setAddress(yxAddress+YXCConstants.YX_2_START_POS);
            RealChargeInfo.AddPoint(gunPointMap.getPointMap(), info);
        }

        EpGunCache epGunCache =  (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if(epGunCache !=null)
        {
            epGunCache.onRealDataChange(gunPointMap.getPointMap(), 3);
        }
    }

    public static void handleYcInfo(String epCode,Vector<SingleInfo> singleInfos)
    {
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epCache==null)
        {
            logger.error("handleYcInfo,realData dataType:3=yc,epCode:{}, not find ElectricPileCache",epCode);
            return ;
        }

        Map<String, GunPointMap> pointMaps = new ConcurrentHashMap<>();

        for(int i=0;i< singleInfos.size(); i++ )
        {
            SingleInfo info = singleInfos.get(i);
            int address = info.getAddress();//紧凑排列
            int ycAddress = address%2500;

            if(!EpChargeService.isValidAddress(ycAddress,11))
            {
                logger.debug("handleYcInfo,realData dataType:3=yc,epCode:{},address:{}, isValid ycAddress:{}",
                        new Object[]{epCode,address,ycAddress});
                continue;
            }


            int epGunNo = EpService.getGunNo(address,11);
            if(epGunNo<0)
            {
                logger.error("handleYcInfo,realData dataType:3=yc,epCode:{},epGunNo:{},address:{} invalid gun no",
                        new Object[]{epCode,epGunNo,address});
                continue;
            }


            GunPointMap gunPointMap= pointMaps.get(epCode+epGunNo);
            if(gunPointMap==null)
            {
                gunPointMap = new GunPointMap(epCode,epGunNo);
                pointMaps.put(epCode+epGunNo, gunPointMap);
            }

            info.setAddress(ycAddress+YXCConstants.YC_START_POS);

            RealChargeInfo.AddPoint(gunPointMap.getPointMap(), info);
        }
        disptachPointToGun(11,pointMaps);
    }
    public static void handleYcInfo_v4(String epCode,int epGunNo,Vector<SingleInfo> singleInfos)
    {
        if(epGunNo<0)
        {
            logger.error("handleYcInfo_v4,realData dataType:3=yc,epCode:{},epGunNo:{},invalid gun no",epCode,epGunNo);
            return ;
        }
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);
        if(epCache==null)
        {
            logger.error("handleYcInfo_v4,realData dataType:3=yc,epCode:{},epGunNo:{},not find ElectricPileCache",epCode,epGunNo);
            return ;
        }
        GunPointMap gunPointMap= new GunPointMap(epCode,epGunNo);

        for(int i=0;i< singleInfos.size(); i++ )
        {
            SingleInfo info = singleInfos.get(i);
            int address = info.getAddress();//紧凑排列
            int ycAddress = address%2500;

            if(!EpChargeService.isValidAddress(ycAddress,11))
            {
                logger.debug("handleYcInfo_v4,realData dataType:3=yc,epCode:{},epGunNo:{},isValid address:{},ycAddress:{}",
                        new Object[]{epCode, epGunNo, address, ycAddress});
                continue;
            }
            info.setAddress(ycAddress+YXCConstants.YC_START_POS);

            RealChargeInfo.AddPoint(gunPointMap.getPointMap(), info);
        }
        EpGunCache epGunCache =  (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if(epGunCache !=null)
        {
            epGunCache.onRealDataChange(gunPointMap.getPointMap(), 11);
        }
    }
    public static void handleVarYcInfo(String epCode,Vector<SingleInfo> singleInfos)
    {
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epCache==null)
        {
            logger.error("handleVarYcInfo,realData dataType:4=varYc,epCode:{}, not find ElectricPileCache",epCode);
            return ;
        }

        Map<String, GunPointMap> pointMaps = new ConcurrentHashMap<String,GunPointMap>();

        for(int i=0;i< singleInfos.size(); i++ )
        {
            SingleInfo info = singleInfos.get(i);
            int address = info.getAddress();//紧凑排列
            int ycAddress = address%128;
            int epGunNo = EpService.getGunNo(address,132);
            if(epGunNo<0)
            {
                logger.error("handleVarYcInfo,realData dataType:4=varYc,epCode:{},epGunNo:{},address:{} invalid gun no",
                        new Object[]{epCode,epGunNo,address});
                continue;
            }

            GunPointMap gunPointMap= pointMaps.get(epCode+epGunNo);
            if(gunPointMap==null)
            {
                gunPointMap = new GunPointMap(epCode,epGunNo);
                pointMaps.put(epCode+epGunNo, gunPointMap);
            }
            info.setAddress(ycAddress+YXCConstants.YC_VAR_START_POS);

            RealChargeInfo.AddPoint(gunPointMap.getPointMap(), info);
        }

        disptachPointToGun(132,pointMaps);
    }

    public static void handleVarYcInfo_v4(String epCode,int epGunNo,Vector<SingleInfo> singleInfos)
    {
        if(epGunNo<0)
        {
            logger.error("handleVarYcInfo_v4,realData dataType:4=varYc,epCode:{},epGunNo:{},invalid gun no",epCode,epGunNo);
            return ;
        }
        ElectricPileCache epCache = (ElectricPileCache)CacheService.getEpCache(epCode);
        if(epCache==null)
        {
            logger.error("handleVarYcInfo_v4,realData dataType:4=varYc,epCode:{},epGunNo:{},not find ElectricPileCache",epCode,epGunNo);
            return ;
        }

        GunPointMap gunPointMap = new GunPointMap(epCode,epGunNo);
        for(int i=0;i< singleInfos.size(); i++ )
        {
            SingleInfo info = singleInfos.get(i);
            int address = info.getAddress();

            info.setAddress(address+YXCConstants.YC_VAR_START_POS);
            RealChargeInfo.AddPoint(gunPointMap.getPointMap(), info);
        }
        if (!epCache.isMeterFlag()) updateEPMeternum(epCache, gunPointMap.getPointMap());

        EpGunCache epGunCache =  (EpGunCache) CacheService.getEpGunCache(epCode, epGunNo);
        if(epGunCache !=null)
        {
            epGunCache.onRealDataChange(gunPointMap.getPointMap(), 132);
        }
    }

    private static void updateEPMeternum(ElectricPileCache epCache, Map<Integer, SingleInfo> pointMap)
    {
        epCache.setMeterFlag(true);
        String tot = "";
        SingleInfo tmp = pointMap.get(YXCConstants.YC_VAR_ACTIVE_TOTAL_METERNUM);
        if (tmp == null) {
            return;
        } else {
            tot = String.valueOf(tmp.getIntValue());
        }

        List<ElectricpileMeternum> electricpileMeternumList = EpServiceImpl.getElectricpileMeternum(epCache.getCode());
        if (electricpileMeternumList == null) {
            String gun1 = "";
            String gun2 = "";
            String gun3 = "";
            String gun4 = "";
            tmp = pointMap.get(YXCConstants.YC_VAR_ACTIVE_TOTAL_GUN1);
            if (tmp != null) gun1 = String.valueOf(pointMap.get(YXCConstants.YC_VAR_ACTIVE_TOTAL_GUN1).getIntValue());
            tmp = pointMap.get(YXCConstants.YC_VAR_ACTIVE_TOTAL_GUN2);
            if (tmp != null) gun2 = String.valueOf(pointMap.get(YXCConstants.YC_VAR_ACTIVE_TOTAL_GUN2).getIntValue());
            tmp = pointMap.get(YXCConstants.YC_VAR_ACTIVE_TOTAL_GUN3);
            if (tmp != null) gun3 = String.valueOf(pointMap.get(YXCConstants.YC_VAR_ACTIVE_TOTAL_GUN3).getIntValue());
            tmp = pointMap.get(YXCConstants.YC_VAR_ACTIVE_TOTAL_GUN4);
            if (tmp != null) gun4 = String.valueOf(pointMap.get(YXCConstants.YC_VAR_ACTIVE_TOTAL_GUN4).getIntValue());

            //更新
            EpServiceImpl.insertElectricpileMeternum(epCache.getCode(), tot, gun1, gun2, gun3, gun4);
        }

        logger.info(LogUtil.addExtLog("epCode|tot"), new Object[]{epCache.getCode(), tot});
    }

    public static void startCheckTimeoutServer() {

        CheckMeterTask checkTask =  new CheckMeterTask();

        TaskPoolFactory.scheduleAtFixedRate("CHECK_METER_TASK", checkTask, DateUtil.getRemainSecondsOfCurDay(), 24*60*60, TimeUnit.SECONDS);
    }

    @SuppressWarnings("rawtypes")
    public static void checkTimeout() {
        Iterator iter = CacheService.getMapEpCache().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            if(entry==null) break;

            ElectricPileCache epCache = (ElectricPileCache) entry.getValue();
            if (null == epCache) continue;
            epCache.setMeterFlag(false);
            logger.debug(LogUtil.addExtLog("epCache"), new Object[]{epCache.getCode()});
        }

    }

    public static void disptachPointToGun(int type, Map<String, GunPointMap> pointMaps)
    {
        Iterator iter = pointMaps.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            GunPointMap gunPointMap=(GunPointMap) entry.getValue();

            if(gunPointMap==null)
            {
                continue;
            }
            EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(gunPointMap.getEpCode(), gunPointMap.getEpGunNo());
            if(epGunCache !=null)
            {
                epGunCache.onRealDataChange(gunPointMap.getPointMap(), type);
            }
        }
    }

    public static int queryConsumeRecord(String epCode,int startPos,int enteryNum)
    {
        ElectricPileCache epClient= (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epClient == null )
        {
            return ErrorCodeConstants.INVALID_EP_CODE;
        }

        EpCommClient commClient = (EpCommClient) epClient.getEpNetObject();
        if(commClient==null || !commClient.isComm())
        {
            logger.error("queryConsumeRecord ElectricPile is not comm,epCode:{}",epCode);
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        byte []cmdTimes = WmIce104Util.timeToByte();




        byte[] statData= EpEncoder.do_query_consume_record(epCode, startPos, (short) enteryNum);
        EpMessageSender.sendMessage(commClient, 0, 0, Iec104Constant.C_QUERY_CONSUME_RECORD, statData, cmdTimes, commClient.getVersion());


        return 0;
    }

    public static int queryCommSignal(String epCode,short stationAddr)
    {
        ElectricPileCache epClient= (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epClient == null )
        {
            return ErrorCodeConstants.INVALID_EP_CODE;
        }

        EpCommClient commClient = (EpCommClient)epClient.getEpNetObject();
        if(commClient==null || !commClient.isComm())
        {
            logger.error("queryCommSignal ElectricPile is not comm,epCode:{}",epCode);
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        byte []cmdTimes = WmIce104Util.timeToByte();

        byte[] statData= EpEncoder.do_query_comm_signal(epCode, stationAddr);
        EpMessageSender.sendMessage(commClient, 0, 0, Iec104Constant.C_QUERY_COMM_SIGNAL, statData, cmdTimes, commClient.getVersion());


        return 0;
    }

    public static void handleCommSignal(String epCode,int signal)
    {
        ElectricPileCache epClient= (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epClient == null )
        {
            logger.error("handleCommSignal epClient:{} is null",epCode);
            return ;
        }
        EpCommClient commClient =(EpCommClient)epClient.getEpNetObject();
        if(commClient == null )
        {
            logger.error("handleCommSignal epClient.getEpNetObject() is null,epCode:{}", epCode);
            return ;
        }

        commClient.setCommSignal(signal);
    }

    public static int handleEpIdentyCodeQuery(EpCommClient commClient,String epCode,int gunno,int hour,int min,int sec)
    {
        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode, gunno);
        if(epGunCache == null)
        {
            logger.error("handleEpIdentyCodeQuery not find EpGun,epCode:{},gunno:{}",epCode,gunno);
            return ErrorCodeConstants.EP_UNCONNECTED;
        }

        long createTime = epGunCache.getCreateIdentyCodeTime();
        String identyCode = epGunCache.getIdentyCode();

        long now = DateUtil.getCurrentSeconds();

        boolean bIsCreateICode =false;
        long diff = now-createTime;
        if(diff>=GameConfig.identycodeTimeout1 || identyCode.length()<6)
        {
            String number=createIdentyCode(now,epGunCache.getPkEpGunId());

            epGunCache.setIdentyCode(number);
            epGunCache.setCreateIdentyCodeTime(now);
            createTime = now;

            bIsCreateICode= true;

            identyCode =  number;
            logger.debug("handleEpIdentyCodeQuery: updateQR,epCode:{},epGunNo:{},identyCode:{}",
                    new Object[]{epCode,gunno,identyCode});
        }
        int errorCode=0;
        if(commClient!=null && commClient.isComm())
        {
            byte[] data= EpEncoder.do_ep_identyCode(epCode, (byte)gunno,identyCode,
                    createTime,(byte)hour,(byte)min,(byte)sec,commClient.getVersion());
            byte[] cmdTimes = WmIce104Util.timeToByte(hour, min, sec);
            EpMessageSender.sendMessage(commClient,0,0,Iec104Constant.M_EP_IDENTYCODE, data,cmdTimes,commClient.getVersion());


            logger.info("handleEpIdentyCodeQuery send identyCode:{},epCode:{},gunno:{}",
                    new Object[]{identyCode,epCode,gunno});
        }
        else
        {
            logger.error("handleEpIdentyCodeQuery commClient:{} is not comm",commClient);
            errorCode= ErrorCodeConstants.EP_UNCONNECTED;
        }

        if(bIsCreateICode)
        {
            logger.info("handleEpIdentyCodeQuery,save db,epCode:{},epGunNo:{},identyCode:{}",
                    new Object[]{epCode, gunno, identyCode});

            //保存到数据库
            EpGunServiceImpl.updateQR(epGunCache.getPkEpGunId(), now+GameConfig.identycodeTimeout2, identyCode);
        }

        return errorCode;
    }

    public static String createIdentyCode(long dt,int pkEpId)
    {

        long t1= dt % 960;
        long t2 = pkEpId % 100;
        long t3 = (dt % pkEpId)%10;

        return String.format("%03d%02d%d",t1,t2,t3);
    }

    public static void sendEpStatusToUsrGate(ArrayList<String> epCodes,int status)
    {
        if(epCodes.size()>0)
        {
            byte[] time = WmIce104Util.timeToByte();
            byte[] data = UsrGateEncoder.do_ep_net_status(time[0],time[1],time[2],status, epCodes);

            UsrGateService.notifyUsrGate(data);
        }
    }

    /**
     *
     * @param epCommClient
     * @param epCode
     * @param totalTime
     * @param totalCount
     * @param totalDl
     * @param cmdTimes
     */
    public static void handleStatReq(EpCommClient epCommClient, String epCode,
                                     int totalTime, int totalCount, int totalDl,byte []cmdTimes) {

        Map<String, Object> statMap = new ConcurrentHashMap<String, Object>();

        statMap.put("totalTime",totalTime);
        statMap.put("totalCount",totalCount);
        statMap.put("totalDl",totalDl);

        EpGunCache epGunCache = (EpGunCache) CacheService.getEpGunCache(epCode,1);
        if(epGunCache != null)
        {
            //epGunCache.handleEvent(EventConstant.EVENT_EP_STAT,0,0,null,statMap);
        }

    }
    public static void handleEpDevices(EpCommClient epCommClient, String epCode,
                                       int epGunNo, int isSupportGunLock, int isSupportGunSit,
                                       int isSupportBmsComm, int isSupportCarPlace) {

    }
    public static void handleOpenGunLock(EpCommClient epCommClient, String epCode,
                                         int epGunNo, int nRet, int gunLockStatus) {
    }
    public static void handleNoCardAuthByPw(EpCommClient CommClient, String epCode,
                                            int epGunNo, String account, byte[] pwMd5,byte []time) {
		/*try {

			int ret = UserService.checkUser(epCode, account, pwMd5);
			byte bSuccess = (byte) 0;
			if (ret == 0) {
				bSuccess = (byte) 1;
			}
			String strSuccessDesc = "";
			switch (ret) {
			case 0:
				strSuccessDesc = "0000";
				break;
			case 1:
				strSuccessDesc = "0001";
				break;
			case 2:
				strSuccessDesc = "0002";
				break;
			case 3:
				strSuccessDesc = "0003";
				break;
			case 4:
				strSuccessDesc = "0004";
				break;
			case 5:
				strSuccessDesc = "0005";
				break;
			case 6:
				strSuccessDesc = "0006";
				break;
			case 8:
				strSuccessDesc = "0008";
				break;
			default:
				strSuccessDesc = "1000";
				break;
			}
			
			//TblUserInfo u = UserService.getMemUserInfo(account);
			//java.math.BigDecimal Dec2 = new BigDecimal("100");
			int blance = (int) (u.getBalance().multiply(Dec2).doubleValue());

		byte[] yzmdata = CDZServerProtocol.do_nocard_auth_by_yzm((short)0,0,0,0,epCode,
					epGunNo, bSuccess, strSuccessDesc, blance, account);

			InnerApiMessageSender.sendMessage(CommClient.getChannel(), yzmdata);

		} catch (IOException e) {
			e.printStackTrace();
		}*/
    }

    public static void handleBlankListReq(EpCommClient CommClient,String epCode) {
		/*try {
			Vector<BlankUser> vBlankUsers = new Vector<BlankUser>(12);
			// BlankUser bu1=new BlankUser("1234567890123456",(byte)1);
			// vBlankUsers.add(bu1);

				byte[] msg = CDZServerProtocol.do_blank_list((short)0,0,0,0,"3120141218010159",
					vBlankUsers);
			EpMessageSender.sendMessage(CommClient.getChannel(), msg);

		} catch (IOException e) {
			e.printStackTrace();
		}*/
    }
    public static void handleBalanceWarning(String epCode, int epGunNo,
                                            String Account,byte []cmdTimes) {
	
		/*ChargeCache electricCacheObj = EpChargeService.getChargeCache( epCode,epGunNo);
		if (electricCacheObj != null) {
			byte[] msg = AppServerProtocol.BalanceWarning(epCode, Account);

			Channel appClientCh = AppClientService.getAppChannel(electricCacheObj.getClientIp());
			// 将需要转发的数据转发给app后台服务器	
			InnerApiMessageSender.gateSendToGame(appClientCh,AppManageCmd.G2A_ELECTRIC_BALANCE_WARNING,(Integer) 0, msg);
		} */
    }

    public static void handleTempChargeRet(String epCode,int maxNum)
    {
        ElectricPileCache epClient= (ElectricPileCache)CacheService.getEpCache(epCode);
        if(epClient == null )
        {
            return ;
        }
        if(maxNum == epClient.getTempChargeMaxNum())
        {
            logger.debug("[tempCharge] tempChargeRet, maxNum == epClient.getTempChargeMaxNum(),epCode:{}",epCode);
            return;
        }
        setTempChargeNum(epCode);
    }

    public static int queryTempChargeNum(String epCode)
    {
        ElectricPileCache epClient= (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epClient == null )
        {
            return ErrorCodeConstants.INVALID_EP_CODE;
        }

        EpCommClient commClient = (EpCommClient)epClient.getEpNetObject();
        if(commClient==null || !commClient.isComm())
        {
            logger.error("[tempCharge]queryTempChargeNum ElectricPile is not comm,epCode:{}",epCode);
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        byte []cmdTimes = WmIce104Util.timeToByte();

        byte[] statData= EpEncoder.do_query_tempChargeNum(epCode);
        EpMessageSender.sendMessage(commClient,0,0,Iec104Constant.C_GET_TEMPCHARGE_NUM, statData,cmdTimes,commClient.getVersion());
        logger.debug("[tempCharge]send queryTempChargeNum success epCode:{}",epCode);

        return 0;
    }
    public static int setTempChargeNum(String epCode)
    {
        ElectricPileCache epClient= (ElectricPileCache)CacheService.getEpCache(epCode);

        if(epClient == null )
        {
            return ErrorCodeConstants.INVALID_EP_CODE;
        }

        EpCommClient commClient = (EpCommClient)epClient.getEpNetObject();
        if(commClient==null || !commClient.isComm())
        {
            logger.error("[tempCharge] queryTempChargeNum ElectricPile is not comm,epCode:{}",epCode);
            return ErrorCodeConstants.EP_UNCONNECTED;
        }
        byte []cmdTimes = WmIce104Util.timeToByte();

        int maxNum =epClient.getTempChargeMaxNum();
        byte[] statData= EpEncoder.do_set_tempChargeNum(epCode,(byte)maxNum);
        //EpMessageSender.sendMessage(commClient,0,0,Iec104Constant.C_SET_TEMPCHARGE_NUM, statData,cmdTimes,commClient.getVersion());

        //命令加时标
        String messagekey = String.format("%03d", Iec104Constant.C_SET_TEMPCHARGE_NUM);
        EpMessageSender.sendRepeatMessage(commClient,messagekey,0,0,Iec104Constant.C_SET_TEMPCHARGE_NUM, statData,cmdTimes,commClient.getVersion());

        logger.debug("[tempCharge]send setTempChargeNum success epCode:{},maxNum:{}",epCode,maxNum);
        return 0;
    }


    public static void queryAllEpByCompanyNumber(int company_number,int maxTempChargeNum)
    {
        List<TblElectricPile> epPileList=EpServiceImpl.findResultObjectByCompany(company_number);
        if(epPileList ==null){
            logger.error("[tempCharge],queryAllEpByCompanyNumber fail,not find ep from DB company_number:{}",company_number);
            return ;
        }
        for(int i=0;i<epPileList.size();i++)
        {
            TblElectricPile ep= epPileList.get(i);
            ElectricPileCache epClient = (ElectricPileCache)CacheService.getEpCache(ep.getEpCode());
            if(epClient == null ){
                logger.error("[tempCharge],queryAllEpByCompanyNumber fail,epClient = null,epCode:{}",ep.getEpCode());
                continue;
            }
            epClient.setTempChargeMaxNum(maxTempChargeNum);

            setTempChargeNum(epClient.getCode());
        }
    }

    public static int getTempChargeMaxNumFromDB(int company_number)
    {
        TblCompany company= EpServiceImpl.findCompanyOne(company_number);
        if(company ==null)
        {
            logger.error("[tempCharge],getTempChargeMaxNumFromDB fail,not find company from DB company_number:{}",company_number);
            return 0;
        }

        return company.getCpynum();

    }

    public static void sendWorkArg(String epCodes)
    {
        String[] epCodeArray = epCodes.split(",");

        for(String epCode : epCodeArray)
        {
            ElectricPileCache epClient = (ElectricPileCache)CacheService.getEpCache(epCode);
            if(epClient == null)
            {
                logger.error(LogUtil.addExtLog("ElectricPileCache is null,epCode:"),new Object[]{epCode});
            }

            EpCommClient commClient = (EpCommClient)epClient.getEpNetObject();
            if(commClient == null || !commClient.isComm())
            {
                logger.error(LogUtil.addExtLog("EpCommClient is null,epCode:"),new Object[]{epCode});
            }

            List<ElectricpileWorkarg> list = EpServiceImpl.getElectricpileWorkarg(epCode);
            if (list == null) continue;

            byte[] bdata = EpEncoder.doIssuedWorkArg(epCode, list);
            byte[] cmdTimes = WmIce104Util.timeToByte();
            EpMessageSender.sendMessage(commClient, 0, 0, Iec104Constant.C_SET_EP_WORK_ARG, bdata, cmdTimes, commClient.getVersion());

            //更新定时充电列表中桩的状态
            int ret = EpServiceImpl.updateElectricpileWorkarg(epCode);
            logger.info(LogUtil.addExtLog("epCode|ret"),new Object[]{epCode, ret});
        }
    }
}
