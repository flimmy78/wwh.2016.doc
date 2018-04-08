package com.epcentre.service;


import io.netty.channel.Channel;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.epcentre.protocol.Iec104Constant;
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
	
	
	public static Map<Channel,PhoneClient> mapCh2PhoneClient = new ConcurrentHashMap<Channel, PhoneClient>();
	public static ConcurrentHashMap<String,PhoneClient>  mapPhoneClients=new ConcurrentHashMap<String,PhoneClient>();
	
	public static String getCacheSize()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("PhoneService:\n" );
        
        sb.append("mapCh2PhoneClient count:").append(mapCh2PhoneClient.size()).append("\n");
        sb.append("mapPhoneClients count:").append(mapPhoneClients.size()).append("\n\n");
        
        return sb.toString();
		
	}
	
	
	public static void Confirm(Channel channel,short cmdtype,int successflag,int errorcode) throws IOException
	{
		 byte[] data = PhoneProtocol.do_confirm(cmdtype, (byte)successflag, (short)errorcode);	
		 PhoneMessageSender.sendMessage(channel, data);
		 
	}
	
	public static void addPhoneClient(PhoneClient commClient)
	{
		 if(commClient==null || commClient.getChannel()==null)
			return;
		 mapCh2PhoneClient.put(commClient.getChannel(), commClient);
	}
	public static void addPhoneClientByAccount(PhoneClient commClient)
	{
		PhoneClient commClient1 = mapPhoneClients.get(commClient.getIdentity());
		if(commClient1!=null)
		{
			mapCh2PhoneClient.remove(commClient1.getChannel());
		}
		
		PhoneClient commClient2 = mapCh2PhoneClient.get(commClient.getChannel());
		 if(commClient2==null)
			return;
		 mapPhoneClients.put(commClient.getIdentity(), commClient);
	}
	public static PhoneClient getPhoneClientByChannel(Channel ch)
	{
		 return mapCh2PhoneClient.get(ch);
	}
	public static PhoneClient getPhoneClientByIdentity(String phoneIdentity)
	{
		 return mapPhoneClients.get(phoneIdentity);
	}
	public static void removeChannel(Channel ch)
	{
		if(ch==null )
		{
			logger.error("removeChannel error!invalid ch:{}",ch);
			return ;
		}
		
		mapCh2PhoneClient.remove(ch);
	}
	public static void removePhoneClientByIdentity(String key)
	{
		if(key==null || key.length()<=0)
		{
			logger.error("removePhoneClientByIdentity error!invalid key:{}",key);
			return ;
		}
		mapPhoneClients.remove(key);
	}

	public static void handleStartCharge(PhoneClient  phoneClient,int fronzeAmt,short chargeType)
	{	
		UserCache u = UserService.getUserCache(phoneClient.getAccount());
		if(u==null)
		{
			 logger.info("phone startCharge not find user:{}",phoneClient.getAccount());
			 return;
		}
		byte[] cmdTimes = WmIce104Util.timeToByte();
		logger.info("charge accept accountId:{},account:{},chargeStyle:{},epCode:{},epGunNo:{} from phone",
				new Object[]{u.getId(),u.getAccount(),1,phoneClient.getEpCode(),phoneClient.getEpGunNo()});
		
		int errorCode = EpChargeService.apiStartElectric(phoneClient.getEpCode(), 
				phoneClient.getEpGunNo(), u.getId(), u.getAccount(), "", (short)EpConstant.CHARGE_TYPE_QRCODE,0,
				fronzeAmt, 1,1000, UserConstant.CMD_FROM_PHONE, u.getAccount(),cmdTimes);
		
		
        if(errorCode>0)
        {
        	 byte successflag =  0;
        	 logger.info("charge apiStartElectric fail errorCode:{} accountId:{},account:{},chargeStyle:{},epCode:{},epGunNo:{} to phone",
 					new Object[]{errorCode,u.getId(), u.getAccount(),1,phoneClient.getEpCode(),phoneClient.getEpGunNo()});
 			
           	 
           	 byte[] data = PhoneProtocol.do_confirm(PhoneConstant.D_START_CHARGE, successflag, (short)errorCode);
	         PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
        } 
	}
	
	public static void handleStopCharge(PhoneClient  phoneClient)
	{
		int errorCode = EpChargeService.apiStopElectric(phoneClient.getEpCode(),
				phoneClient.getEpGunNo(), phoneClient.getAccountId(),99,phoneClient.getIdentity());
		
        if(errorCode>0)
        {
        	 byte successflag =  0;
           	 logger.info("phone handleStopCharge fail errorCode:{}",errorCode);
           	 
           	 byte[] data = PhoneProtocol.do_confirm(PhoneConstant.D_STOP_CHARGE, successflag, (short)errorCode);
	         PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
        } 
	}
	
	public static void handleHeart(PhoneClient  phoneClient)
	{
		//java.util.concurrent.
       	 byte[] data = PhoneProtocol.Package((byte)1, PhoneConstant.D_HEART);
         PhoneMessageSender.sendMessage(phoneClient.getChannel(), data);
	}
	
	public static int initPhoneConnect(PhoneClient  phoneClient,int accountId,String epCode,int epGunNo,String checkCode,int cmdType)
	{
		 EpGunCache epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
		 if(epGunCache == null || epGunCache.getEpNetObject()==null)
		 {	
			 return EpConstantErrorCode.EP_UNCONNECTED;
		 }
		 if(!epGunCache.getEpNetObject().isComm())
		 {
			 return EpConstantErrorCode.EP_UNCONNECTED;
		 }
		 
		 UserRealInfo  userRealInfo = UserService.findUserRealInfo(accountId);
		if(null == userRealInfo)
		{
			logger.error("checkPhoneConnect! not find user {}",accountId);
			return EpConstantErrorCode.INVALID_ACCOUNT;
		}
		
		
		if(userRealInfo.getStatus() == 3)
		{
			logger.error("checkPhoneConnect! user:{},status:{} delete",accountId, userRealInfo.getStatus());
			return EpConstantErrorCode.INVALID_ACCOUNT;
		}
		if(userRealInfo.getStatus() == 2)
		{
			logger.error("checkPhoneConnect! user:{},status:{} fronze",accountId, userRealInfo.getStatus());
			return EpConstantErrorCode.INVALID_ACCOUNT_STATUS;
		}
		UserCache userInfo = UserService.getUserCache(userRealInfo.getAccount());
		
		String chargeGun = epCode + epGunNo;
		
		if(userInfo.getUseGun().length()>0 && userInfo.getUseGun().compareTo(chargeGun)!=0)
		{
			if(userInfo.getUseGunStaus() == 6)
			{
			return EpConstantErrorCode.EPE_REPEAT_CHARGE;
			}
			else if(userInfo.getUseGunStaus() == 3)
			{
				return EpConstantErrorCode.CANNOT_OTHER_OPER;
			}
			else
			{
				return EpConstantErrorCode.USER_OPER_IN_OTHER_EP;
			}
		}
		
		String phoneIdentity = userInfo.getAccount();
		PhoneClient  oldPhoneClient = getPhoneClientByIdentity(phoneIdentity);
		if(oldPhoneClient!=null )
		{
			if(oldPhoneClient.getChannel() != phoneClient.getChannel() && oldPhoneClient.getChannel()!=null)
			{
                removeChannel(oldPhoneClient.getChannel());
				oldPhoneClient.getChannel().close();
				oldPhoneClient.setStatus(0);
			}
		}
		
		//验证码
		String src = userRealInfo.getDeviceid() + userRealInfo.getPassword() + accountId;
		String calcCheckCode =  WmIce104Util.MD5Encode(src.getBytes());
		
		if(calcCheckCode.compareTo(checkCode)!=0 )
		{
			logger.debug("checkPhoneConnect!calcCheckCode:{},checkCode:{}",calcCheckCode,checkCode);
			logger.info("checkPhoneConnect fail!accountId:{}",accountId);
			return EpConstantErrorCode.ERROR_PHONE_CRC_CODE;
		}	
		
		//查电桩
		ElectricPileCache epCache = EpService.getEpByCode(epCode);
		if(epCache==null)
		{
			 return EpConstantErrorCode.EP_UNCONNECTED;
		}
		int error = EpService.getEpStatusFromDb(epCode);
 		if(error > 0)
 		{
 			return EpConstantErrorCode.EP_UNCONNECTED;
 		}
 		
		    
		 short pos = 0;
		 if(epGunCache.getStatus() == EpConstant.EP_GUN_STATUS_CHARGE )
		 {
			 if(epGunCache.getChargeCache()!=null && epGunCache.getChargeCache().getUserId()!=0 && epGunCache.getChargeCache().getUserId() != userInfo.getId())
			 {
				 logger.error("initPhoneConnect!epGunCache.getStatus():{}",epGunCache.getStatus());

			 	return EpConstantErrorCode.EPE_OTHER_CHARGING;
			 }
			 else{
				 pos = 6;
			 }
		 }
		 if(epGunCache.getStatus() == EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED)
		 {
			 if(epGunCache.getBespCache()==null || epGunCache.getBespCache().getAccountId()!= userInfo.getId())
			 {
				 logger.error("checkPhoneConnect!epGunCache.getStatus():{}",epGunCache.getStatus());
				
			
				 return EpConstantErrorCode.EPE_OTHER_BESP;
			 }
			 else
			 {
				 pos=3;
			 }
		 }
		 if(epGunCache.getStatus() == EpConstant.EP_GUN_STATUS_USER_AUTH)
		 {
			 if(epGunCache.getChargeCache()!=null && epGunCache.getChargeCache().getUserId() != userInfo.getId())
			 {
				 return EpConstantErrorCode.USED_GUN;
			 }
		 }
		 
		 if(epGunCache.getStatus() == EpConstant.EP_GUN_STATUS_EP_OPER && epGunCache.getChargeCache()!=null)
		 {
			 pos=5;
		 }
		 
		phoneClient.setAccountId(accountId);
		phoneClient.setAccount(userInfo.getAccount());
		phoneClient.setEpCode(epCode);
		phoneClient.setEpGunNo(epGunNo);
		phoneClient.setIdentity(userInfo.getAccount());
		phoneClient.setStatus(CommStatusConstant.INIT_SUCCESS);
		
		addPhoneClientByAccount(phoneClient);
		
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
			//
			epGunCache.pushFirstRealData();
			
		}
	
		return 0;
	}
	
	public static int handleConnectEp(Channel ch,int accountId,String epCode,int epGunNo, String checkCode,int cmdType,int version)
	{
		 //判断通道是否正常
		PhoneClient  phoneClient = PhoneService.getPhoneClientByChannel(ch);
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
			
		PhoneClient phoneClient =  getPhoneClientByChannel(channel);
		
		if (phoneClient != null) {
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
	

	@SuppressWarnings("rawtypes")
	public synchronized static void checkTimeOut(){
		
		Iterator iter = mapCh2PhoneClient.entrySet().iterator();
		int count=0;
		int sum=0;
		while (iter.hasNext()) {
			sum++;
			if(sum>10)
			{
				NetUtils.sleep(10);
				sum=0;
			}
			Map.Entry entry = (Map.Entry) iter.next();
			PhoneClient phoneClient=(PhoneClient) entry.getValue();	
			if(null == phoneClient)
			{
				continue;
			}
			
			long timeout =GameConfig.phoneConnectTimeout;
		
			long now = DateUtil.getCurrentSeconds();			
			long diff = now-phoneClient.getLastUseTime();	
			
			if(diff>timeout)
			{ 
				count++;
				logger.debug("PhoneService:checkTimeOut[diff:{},lastUseTime{}], remove phoneClient:{}",
						new Object[]{diff,phoneClient.getLastUseTime(),phoneClient});
				
				phoneClient.setStatus(0);
				Channel ch = phoneClient.getChannel();
				
				if(ch!=null )
				{
					ch.close();
				}	
				removePhoneClientByIdentity(phoneClient.getIdentity());
				iter.remove();
				ch=null;
				
			}
		
		}
		if(count>0)
		{
			logger.info("Phone Client communication had {} time out !",count);
		}
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
		PhoneClient phoneClient = mapPhoneClients.get(actionIdentity);
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
		
		PhoneClient phoneClient = mapPhoneClients.get(actionIdentity);
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
			//System.out.print("phoneServer onEventEVENT_PHONE_REAL\n");
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
			//logger.info("EventConstant.EVENT_EP_NET_STATUS,ret:{}",ret);
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
			e.printStackTrace();
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
