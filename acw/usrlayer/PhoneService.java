package com.epcentre.service;


import io.netty.channel.Channel;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.cache.AuthUserCache;
import com.epcentre.cache.ElectricPileCache;
import com.epcentre.cache.EpGunCache;
import com.epcentre.cache.PhoneClient;
import com.epcentre.cache.UserCache;
import com.epcentre.cache.UserOrigin;
import com.epcentre.cache.UserRealInfo;
import com.epcentre.config.GameConfig;
import com.epcentre.constant.CommStatusConstant;
import com.epcentre.constant.EpConstant;
import com.epcentre.constant.EpConstantErrorCode;
import com.epcentre.constant.EventConstant;
import com.epcentre.constant.UserConstant;
import com.epcentre.protocol.PhoneConstant;
import com.epcentre.protocol.PhoneProtocol;
import com.epcentre.protocol.WmIce104Util;
import com.epcentre.server.PhoneMessageSender;
import com.epcentre.task.CheckPhoneCommClientTask;
import com.epcentre.utils.DateUtil;
import com.epcentre.utils.NetUtils;
import com.netCore.core.pool.TaskPoolFactory;


public class PhoneService{

	private static final Logger logger = LoggerFactory.getLogger("PhoneLog");
	
	
	private static ChannelManage cm = new ChannelManage();
	
	public static String getCacheSize()
	{
		return cm.getCacheSize();
	}
	
	public static void addConnect(PhoneClient  client)
	{
		int ret = cm.addConnect(client);
		if(ret==0)
		{
			logger.error("addClient fail");
		}
	}
	public static void addClient(PhoneClient  client)
	{
		int ret = cm.addClient(client);
		if(ret==0)
		{
			logger.error("addClient fail");
		}
		
	}
	public static PhoneClient getClient(Channel  ch)
	{
		return (PhoneClient) cm.get(ch);
	}
	public static PhoneClient getClient(String key)
	{
		return (PhoneClient) cm.get(key);
	}
	
	public static void Confirm(Channel channel,short cmdtype,int successflag,int errorcode) throws IOException
	{
		 byte[] data = PhoneProtocol.do_confirm(cmdtype, (byte)successflag, (short)errorcode);	
		 PhoneMessageSender.sendMessage(channel, data); 
	}
	


	public static void handleStartCharge(PhoneClient  phoneClient,int fronzeAmt,short chargeType)
	{	
		logger.info("handleStartCharge:phoneClient:{},fronzeAmt:{}",phoneClient,fronzeAmt);
		UserCache u = UserService.getUserCache(phoneClient.getAccount());
		if(u==null)
		{
			 logger.info("phone startCharge not find account:{}",phoneClient.getAccount());
			 return;
		}
		byte[] cmdTimes = WmIce104Util.timeToByte();
		logger.info("charge accept accountId:{},account:{},chargeStyle:{},epCode:{},epGunNo:{} from phone",
				new Object[]{u.getId(),u.getAccount(),1,phoneClient.getEpCode(),phoneClient.getEpGunNo()});
		
		int errorCode = EpChargeService.apiStartElectric(phoneClient.getEpCode(), 
				phoneClient.getEpGunNo(), u,"",null, "", (short)EpConstant.CHARGE_TYPE_QRCODE,
				fronzeAmt, 1,UserConstant.ORG_I_CHARGE, UserConstant.CMD_FROM_PHONE, u.getAccount(),cmdTimes);
		
		byte successflag =  0;
        if(errorCode>0)
        {
        	 
        	 logger.info("charge apiStartElectric fail errorCode:{} accountId:{},account:{},chargeStyle:{},epCode:{},epGunNo:{} to phone",
 					new Object[]{errorCode,u.getId(), u.getAccount(),1,phoneClient.getEpCode(),phoneClient.getEpGunNo()});
        } 
        else
        {
        	successflag=1;
        	errorCode=0;
        }
        
      	byte[] data = PhoneProtocol.do_confirm(PhoneConstant.D_START_CHARGE, successflag, (short)errorCode);
        PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
	}
	
	public static void handleStopCharge(PhoneClient  phoneClient)
	{
		int errorCode = EpChargeService.apiStopElectric(phoneClient.getEpCode(),
				phoneClient.getEpGunNo(), UserConstant.ORG_I_CHARGE,phoneClient.getAccountId(),phoneClient.getAccount(),99,phoneClient.getIdentity());
		
		byte successflag =  1;
        if(errorCode>0)
        {
        	successflag =  1;
           	 logger.info("phone handlestopcharge fail errorCode:{}",errorCode);
        } 
        else
        {
        	errorCode=0;
        }
        byte[] data = PhoneProtocol.do_confirm(PhoneConstant.D_STOP_CHARGE, successflag, (short)errorCode);
        PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
        
	}
	
	public static void handleHeart(PhoneClient  phoneClient)
	{
		//java.util.concurrent.
       	 byte[] data = PhoneProtocol.Package((byte)1, PhoneConstant.D_HEART);
         PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
	}
	private static short getGunStatus(EpGunCache epGunCache)
	{
		short pos=0;
		int gunStatus = epGunCache.getStatus();
		 if(gunStatus == EpConstant.EP_GUN_STATUS_CHARGE)
			pos=6;
		 else if(gunStatus == EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED)
			pos=3;
		 else if(gunStatus ==EpConstant.EP_GUN_STATUS_EP_OPER)
		 {
			 pos=5;//等待充电
		 }
		 return pos;
	}
	private static void handlePhoneOldClient(PhoneClient  phoneClient,String usrAccount)
	{
		cm.handleOldClient(phoneClient, usrAccount);
	}
	
	
	public static int initPhoneConnect(PhoneClient  phoneClient,int accountId,String epCode,int epGunNo,String checkCode,int cmdType)
	{
		 UserRealInfo  userRealInfo = UserService.findUserRealInfo(accountId);
		 if(null == userRealInfo)
		 {
			return EpConstantErrorCode.INVALID_ACCOUNT;
		 }
		 if(userRealInfo.getLevel()!=6)
		 {
			 return EpConstantErrorCode.INVALID_ACCOUNT;
		 }
		 
		//验证码
		String src = userRealInfo.getDeviceid() + userRealInfo.getPassword() + accountId;
		String calcCheckCode =  WmIce104Util.MD5Encode(src.getBytes());
		
		if(calcCheckCode.compareTo(checkCode)!=0 )
		{
			logger.debug("checkPhoneConnect!accountId:{},calcCheckCode:{},checkCode:{}",
					new Object[]{accountId,calcCheckCode,checkCode});
			logger.info("checkPhoneConnect fail!accountId:{}",accountId);
			return EpConstantErrorCode.ERROR_PHONE_CRC_CODE;
		}	
			
		int errorCode = userRealInfo.canCharge();
		if(errorCode>0)
			return errorCode;
		
		//查电桩
		ElectricPileCache epCache = EpService.getEpByCode(epCode);
		if(epCache==null)
		{
			 return EpConstantErrorCode.EP_UNCONNECTED;
		}
		EpGunCache epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
		 if(epGunCache == null || epGunCache.getEpNetObject()==null)
		 {	
			 return EpConstantErrorCode.EP_UNCONNECTED;
		 }
		 if(!epGunCache.getEpNetObject().isComm())
		 {
			 return EpConstantErrorCode.EP_UNCONNECTED;
		 }
		 
		 UserCache userInfo = UserService.getUserCache(accountId);
		 
		 errorCode = epGunCache.canWatchCharge(accountId);
		 short pos=-1;
		 if(errorCode>0)
		 {
			 return errorCode;
		 }
		 else if(errorCode==-1)
		 {
			 errorCode = epCache.canCharge(true, 0,0,0,epGunNo);
		
	 		 if(errorCode > 0) return errorCode;
	 		 
	 		 errorCode = epGunCache.canCharge(EpConstant.CHARGE_TYPE_QRCODE,accountId,true);
			 if(errorCode>0) return errorCode;
			
			 errorCode = userInfo.canCharge(epCode+epGunNo,userInfo.getId(),UserConstant.ORG_I_CHARGE,"",0,false);
			 if(errorCode>0) return errorCode;
			 
			 pos = getGunStatus(epGunCache);
		 }
		 else
		 {
			 pos=6; 
		 }
		 
		//处理旧连接
		String phoneIdentity = userInfo.getAccount();
		handlePhoneOldClient(phoneClient,phoneIdentity);
		
		//添加新连接
		phoneClient.initSuccess(accountId, phoneIdentity, epCode, epGunNo);
		
		
		byte ret=0x01;
		logger.debug("initPhoneConnect accountId:{},epCode:{},epGunNo:{},pos:{}",new Object[]{accountId,epCode,epGunNo,pos});
		
		short currentType=(short)((epGunCache.getRealChargeInfo().getCurrentType()==EpConstant.EP_AC_TYPE )?1:2);
	
			
		byte[] respData = PhoneProtocol.do_connect_ep_resp((short)cmdType, ret, (short)0, pos,currentType);	
		if(respData==null)
		{
			logger.info("handleConnectEp respData == null,accountId:{}",accountId);
		}
		
		PhoneMessageSender.sendMessage(phoneClient.getChannel(), respData);
		
		if(pos ==6)
		{
			//卡充电在手机上显示
			if(epGunCache.getChargeCache()!=null && 
					epGunCache.getChargeCache().getStartChargeStyle()==3 && 
					epGunCache.getChargeCache().getUserOrigin()!=null)
			{
				epGunCache.getChargeCache().getUserOrigin().setCmdFromSource(2);
			}
			
			epGunCache.pushFirstRealData();
			
		}
		else
		{
			 AuthUserCache authUser = new AuthUserCache(userInfo.getId(),userInfo.getAccount(),DateUtil.getCurrentSeconds(),(short)1);
			 epGunCache.setAuthCache(authUser);
		}
		
		if(phoneClient.getVersion()>=3)
		{
			
			int gun2carLinkStatus = epGunCache.get_gun2carLinkStatus();
			sendGun2CarLinkStatus(gun2carLinkStatus,phoneClient);
		}
		
		return 0;
	}
	public static void sendGun2CarLinkStatus(int status,PhoneClient  phoneClient)
	{
		if(phoneClient==null)
		{
			logger.error("handleGun2CarLinkStatus phoneClient == null");
			return ;
		}
		byte time[]  = WmIce104Util.timeToByte();
		
		int gunStatus = status;
		
		
		gunStatus +=1;
		
		byte[] pushData = PhoneProtocol.do_gun2car_linkstatus(gunStatus, time);
		
		if(pushData==null)
		{
			logger.error("handleGun2CarLinkStatus pushData == null,usrAccount:{},gun2car link status:{}",phoneClient.getAccount(),status);
		}
		else
		{
			String repeatMsgKey = phoneClient.getAccount()+PhoneConstant.D_GUN_CAR_STATUS;
			PhoneMessageSender.sendRepeatMessage(phoneClient.getChannel(), repeatMsgKey, pushData, phoneClient.getVersion());
		}
		
		
	}
	public static void handleGun2CarLinkStatus(int status,String usrAccount)
	{
		PhoneClient  phoneClient = getClient(usrAccount);
		//
		if(phoneClient == null)
		{
			logger.error("handleGun2CarLinkStatus had not find {}'PhoneClient",usrAccount);
			
			return ;
		}
		if(!phoneClient.isComm())
		{
			logger.error("handleGun2CarLinkStatus {}'PhoneClient is not comm",usrAccount);
			return ;
		}

		if(phoneClient.getVersion()<=2)
		{
			logger.debug("handleGun2CarLinkStatus {}'PhoneClient'version is comm",usrAccount);
			return ;
		}
		
		if(status!=1)
		{
			status=2;
		}
		sendGun2CarLinkStatus(status,phoneClient);
		
	}
	public static void handleGun2CarLinkStatusResp(PhoneClient  phoneClient )
	{
		logger.debug("handleGun2CarLinkStatusResp");
		//
		if(phoneClient == null)
		{
			logger.error("handleGun2CarLinkStatus PhoneClient is null");
			
			return ;
		}
		EpGunService.removeRepeatMsg(phoneClient.getAccount()+PhoneConstant.D_GUN_CAR_STATUS);
		
	}
	public static int handleConnectEp(Channel ch,int accountId,String epCode,int epGunNo, String checkCode,int cmdType,int version)
	{
		 //判断通道是否正常
		PhoneClient  phoneClient = getClient(ch);
		//
		if(phoneClient == null)
		{
			return 0;
		}
		
		phoneClient.setVersion(version);
		int errorCode = initPhoneConnect(phoneClient,accountId,epCode,epGunNo,checkCode,cmdType);
		
		
		if( errorCode > 0)
		{
			logger.info("initPhoneConnect accountId:{},epConnectGun:{},errorCode:{}",new Object[]{accountId,epCode+epGunNo,errorCode});
			byte[] respData = PhoneProtocol.do_connect_ep_resp((short)cmdType, (byte)0x00, (short) errorCode, (short)0,(short)0);	
			if(respData==null)
			{
				logger.error("handleConnectEp respData == null,accountId:{}",accountId);
			}
			else
			{
				PhoneMessageSender.sendMessage(ch, respData);
			}
		}
		return 0;
	}
	

	public static void offLine(Channel channel){
			
		PhoneClient phoneClient =  getClient(channel);
		
		if (phoneClient != null) {
			
			cleanPhoneAuthUser(phoneClient);
			
			logger.debug("PhoneService offLine. commClient:{},account:{}",phoneClient.getChannel(),phoneClient.getAccount());
		}
		else
		{
			//logger.info("\n\n\noffLine,phoneClient:");
		}
			
	}
	
	public static void startCommClientTimeout(long initDelay) {
		
		CheckPhoneCommClientTask checkTask =  new CheckPhoneCommClientTask();
				
		TaskPoolFactory.scheduleAtFixedRate("CHECK_PHONECLIENT_TIMEOUT_TASK", checkTask, initDelay, 10, TimeUnit.SECONDS);
	}
	
	private static void cleanPhoneAuthUser(PhoneClient phoneClient)
	{
		if(phoneClient==null)
			return ;
		
		EpGunCache epGunCache =  EpGunService.getEpGunCache(phoneClient.getEpCode(), phoneClient.getEpGunNo());
		if(epGunCache==null)
			return ;
		AuthUserCache authUser= epGunCache.getAuthCache();
		if(authUser==null)
			return ;
		if(authUser.getStyle()==1 && authUser.getUsrId()==phoneClient.getAccountId() )
			epGunCache.setAuthCache(null);
		
	}
	

	@SuppressWarnings("rawtypes")
	public synchronized static void checkTimeOut(){
		
		String msg = cm.checkTimeOut(GameConfig.phoneNoInitTimeout, GameConfig.phoneConnectTimeout);
		
		logger.info("checkTimeOut:{} ",msg);
		
		
	}
	
	public static boolean isComm(UserOrigin userOrigin)
	{
		String actionIdentity="";
		if(userOrigin!=null)
			actionIdentity = userOrigin.getCmdChIdentity();
		//logger.info("phone service actionIdentity:{}",actionIdentity);
		if(actionIdentity.length()==0)
		{
			logger.error("isComm!userOrigin:{},actionIdentity:{}",userOrigin,actionIdentity);
			return false;
		}
		PhoneClient phoneClient = (PhoneClient) cm.get(actionIdentity);
		if(phoneClient==null)
		{
			logger.error("isComm not find PhoneClient,actionIdentity:{}",actionIdentity);
			return false;
		}
		return phoneClient.isComm();
		
	}

	public static void onEvent(int type,UserOrigin userOrigin,int ret,int cause,Object srcParams, Object extraData)
	{
		try{
		String actionIdentity="";
		if(userOrigin!=null)
			actionIdentity = userOrigin.getCmdChIdentity();
		logger.debug("phone service type:{},actionIdentity:{}",type,actionIdentity);
		
		PhoneClient phoneClient = (PhoneClient) cm.get(actionIdentity);
		if(phoneClient==null)
		{
			logger.info("handleActionResponse not find PhoneClient,actionIdentity:{}",actionIdentity);
			return ;
		}
		switch(type)
		{
		
		case EventConstant.EVENT_BESPOKE:
			break;
			
		case EventConstant.EVENT_CHARGE:
		{
			//logger.info("phone EventConstant.EVENT_CHARGE ret:{}",ret);
			 byte[] data = PhoneProtocol.do_confirm(PhoneConstant.D_START_CHARGE, (byte)ret, (short)cause);
	         PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
		}
			break;
		
		case EventConstant.EVENT_STOP_CHARGE:
		{
			 byte[] data = PhoneProtocol.do_confirm(PhoneConstant.D_STOP_CHARGE, (byte)ret, (short)cause);
	         PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
		}
			break;
		case EventConstant.EVENT_CONSUME_RECORD:
		{
			Map<String, Object> consumeRecordMap = (Map<String, Object>)extraData;
			
			String chargeOrder = (String)consumeRecordMap.get("orderid");
			
			//logger.info("chargeOrder,{}",chargeOrder);
	
			long lst= (long)consumeRecordMap.get("st");
			long let= (long)consumeRecordMap.get("et");
			
			
			int st = (int)lst;
			int et = (int)let;
			
			int totalMeterNum = (int)consumeRecordMap.get("totalMeterNum");
			int totalAmt = (int)consumeRecordMap.get("totalAmt");
			int serviceAmt = (int)consumeRecordMap.get("serviceAmt");
			int pkEpId = (int)consumeRecordMap.get("pkEpId");
			
			
			int version = phoneClient.getVersion();
			int couPonAmt = 0;
			int userFirst = 0; 
			int realCouPonAmt = 0;
			if(version>=2)
			{
				userFirst = (int)consumeRecordMap.get("userFirst");
				couPonAmt = (int)consumeRecordMap.get("couPonAmt");
				realCouPonAmt = (int)consumeRecordMap.get("realCouPonAmt");
			}
			
			byte[] data = PhoneProtocol.do_consume_record((short)version,chargeOrder,st,et,totalMeterNum,totalAmt,serviceAmt,
					pkEpId,userFirst,couPonAmt,realCouPonAmt);
			 
			String messagekey = chargeOrder;
			
			
			PhoneMessageSender.sendRepeatMessage(phoneClient.getChannel(),messagekey, data,phoneClient.getVersion());
		}
			break;
		case EventConstant.EVENT_REAL_CHARGING:
		{
			logger.debug("phoneServer onEventEVENT_PHONE_REAL\n");
			if(extraData==null)
			{
				logger.info("onEvent error,extraData==null");
				return ;
			}
			ChargingInfo chargingInfo = (ChargingInfo)extraData;
			byte[] data = PhoneProtocol.do_real_charge_info(chargingInfo);
			PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
		}
			break;
		case EventConstant.EVENT_START_CHARGE_EVENT:
		{
			logger.debug("phoneServer onEvent EVENT_START_CHARGE_EVENT\n");
			
			
			byte[] data = PhoneProtocol.do_start_charge_event(ret);
			
			PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
		}
			break;
		case EventConstant.EVENT_EP_NET_STATUS:
		{
			logger.debug("EventConstant.EVENT_EP_NET_STATUS,ret:{}",ret);
			byte[] data = PhoneProtocol.do_ep_net_status(ret);
			PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
		}
		   break;
		default:
			break;
		
		  }
		}
		catch (Exception e) 
		{
			logger.error("onEvent exception:{},type:{}",e.getStackTrace(),type);
			
			return ;
		}
	}
	
	public static void handleConsumeRecordConfirm(PhoneClient  phoneClient,String chargeOrder,short result)
	{
		logger.info("receive phone ConsumeRecordConfirm,chargeOrder:{},result:{}",chargeOrder,result);
		String messagekey = chargeOrder;
		EpGunService.removeRepeatMsg(messagekey);
	}
	
}
