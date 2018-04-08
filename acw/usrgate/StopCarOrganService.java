package com.epcentre.epconsumer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import net.sf.json.JSONObject;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.config.GameConfig;
import com.epcentre.constant.EpConstant;
import com.epcentre.utils.DateUtil;
import com.epcentre.utils.RequestUtil;

public class StopCarOrganService  {
	
	private static final Logger logger = LoggerFactory.getLogger(StopCarOrganService.class);
	

	public static long getTimeStamp()
	{
		return System.currentTimeMillis();
	}
	
    private static final String DateFmt = "yyyy-MM-dd HH:mm:ss";
	
	private static String  getFmtTime(long time)
	{
		java.util.Date date = new Date(time*1000);
		
		return DateUtil.toDateFormat(date, DateFmt); 
	}
	
	public static int ChangeWorkStatus(int epWorkStatus)
	{
		int workStatus=4;
		if(epWorkStatus==EpConstant.EP_GUN_W_STATUS_OFF_LINE)
			workStatus = 4; //离线
		else if(epWorkStatus==EpConstant.EP_GUN_W_STATUS_WORK)
			workStatus = 3; //工作
		else if(epWorkStatus==EpConstant.EP_GUN_W_STATUS_FAULT||
				epWorkStatus>EpConstant.EP_GUN_W_INIT)
			workStatus = 1;//故障
		else
			workStatus = 2;//空闲
		
		return workStatus;
	}
	
	
	/**
	 * 3充电桩充电记录
	 */
	public static void realData(String epCode,int gunNo,int workStatus,int gunConnStatus,int faultCode,long createTime,BigDecimal vol_a,
		BigDecimal vol_b,BigDecimal vol_c,BigDecimal cur_a,BigDecimal cur_b,BigDecimal cur_c, BigDecimal power,
		BigDecimal volt,BigDecimal cur,int soc,int restTime)
	{
		logger.debug("sco realData enter");
		
		if(GameConfig.scoCfg == null)
		{
		    logger.error("GameConfig.scoCfg is null");
		    return;
		}
		String method=GameConfig.scoCfg.getStatusMethod();
		String partnerKey = GameConfig.scoCfg.getPartnerKey();
		String partnerId = GameConfig.scoCfg.getPartnerId();
		String baseUri = GameConfig.scoCfg.getBaseUrl()+GameConfig.scoCfg.getStatusMethod();
   
		//参数转换
		String statusTime= getFmtTime(createTime);
		String svolA = vol_a.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		String svolB = vol_b.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		String svolC = vol_c.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		String scurA = cur_a.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		String scurB = cur_b.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		String scurC = cur_c.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		String sPower = power.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		String sVolt = volt.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		String sCur = cur.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		
		
		
		
		HashMap<String,Object>  paramsMap=new HashMap<String,Object>();

        //运营服务商标识
		paramsMap.put("operatorId","310000002");
//		1.充电桩编码poleId字符串由平台提前注册接入	
		paramsMap.put("equipNo", epCode);
//		2.充电枪编码gunNo字符串由平台提前注册接入
		String sgunNo=""+gunNo;
		paramsMap.put("gunNo", sgunNo);
//		3.工作状态
		String sworkStatus=""+workStatus;
		paramsMap.put("chargepoleStatus", sworkStatus);
//		4.枪口连接状态	
		String sgunConnStatus=""+gunConnStatus;
		paramsMap.put("gunConnStatus", sgunConnStatus);
//		5.故障代码，0:急停故障, 1:电表故障，2:接触器故障，3:读卡器故障 ，4:内部过温故障，5:连接器故障，6:绝缘故障，7:其他
		String sfaultCode=""+faultCode;
		paramsMap.put("faultCode", sfaultCode);
//		6.状态时间参照yyyy-MM-dd HH:mm:ss
		paramsMap.put("statusTime", statusTime);
//		7.输入侧A相电压（6位小数位）
		paramsMap.put("voltA", svolA);
//		8.输入侧B相电压（6位小数位）
		paramsMap.put("voltB", svolB);
//		9.输入侧C相电压（6位小数位）
		paramsMap.put("voltC", svolC);
//		10.输入侧A相电流（6位小数位）
		paramsMap.put("curA", scurA);
//		10.输入侧B相电流（6位小数位）
		paramsMap.put("curB", scurB);
//		11.输入侧C相电流（6位小数位）
		paramsMap.put("curC", scurC);
//		11.输入有功功率（6位小数位）
		paramsMap.put("power", sPower);
//      12.充电机输出电压（6位小数位）
		paramsMap.put("volt", sVolt);
//      13.充电机输出电流（6位小数位）
		paramsMap.put("cur", sCur);
		String sSoc=""+soc;
//      14.车辆SOC
		paramsMap.put("soc", sSoc);
		String sRestTime=""+restTime;
//      15.剩余充电时长
		paramsMap.put("restTime", sRestTime);
		ArrayList list = new ArrayList();
		list.add(paramsMap);
		HashMap<String,Object>  paramsMapTemp=new HashMap<String,Object>();
		paramsMapTemp.put("data", list);
		JSONObject jsonObject = JSONObject.fromObject(paramsMapTemp);
		

		String timestamp = ""+ getTimeStamp();		
		String token= RandomStringUtils.randomAlphanumeric(16);
		String toSign = jsonObject.toString()+"#"+ token + "#" + timestamp;
	    String sign = ApiSecurityUtils.encrypt(toSign, partnerKey, ApiSecurityUtils.HS256);
		
		HashMap<String,Object>  params=new HashMap<String,Object>();
		params.put("partnerId", partnerId);
		params.put("timestamp", timestamp);
		params.put("token", token);
		params.put("input", jsonObject.toString());
		params.put("sign", sign);
		
		logger.debug("realData!baseUri:{},paramsMap:{}",baseUri,params);
		
	   
    	String res = RequestUtil.doPost(baseUri, params);
    
    	logger.debug("sco realData res:{}",res);
	   
	}
	
	
	/**
	 * 3充电桩充电记录
	 */
	public static void chargeRecord(String epCode,int gunNo,BigDecimal bdmeterNum,
		long lStartTime,long lEndTime)
	{
		//String method="/api/pipe/pile-charge-record";
		if(GameConfig.scoCfg == null)
		{
		    logger.error("GameConfig.scoCfg is null");
		    return;
		}
		String method=GameConfig.scoCfg.getStatusMethod();
		String partnerKey = GameConfig.scoCfg.getPartnerKey();
		String partnerId = GameConfig.scoCfg.getPartnerId();
		String baseUri = GameConfig.scoCfg.getBaseUrl()+GameConfig.scoCfg.getChargeMethod();
	    
		
		long totalUsedTime=lEndTime - lStartTime;
		//参数转换
		String startTime= getFmtTime(lStartTime);
		String endTime =getFmtTime(lEndTime);
	
		HashMap<String,Object>  paramsMap=new HashMap<String,Object>();
        //运营服务商标识
		paramsMap.put("operatorId","310000002");
//		1.充电桩编码
		paramsMap.put("equipNo", epCode);
//		2.充电枪编码gunNo字符串由平台提前注册接入
		paramsMap.put("gunNo", gunNo);
//		3.充电量meterNumFLOAT单位度 (小数点后3位 )	
		String meterNum = bdmeterNum.setScale(3,java.math.BigDecimal.ROUND_HALF_UP).toString();
		paramsMap.put("pq", meterNum);
//		4.开始时间startTime字符串参照yyyy-MM-dd HH:mm:ss
		paramsMap.put("startTime", startTime);
//		5.结束时间endTime字符串参照yyyy-MM-dd HH:mm:ss
		paramsMap.put("endTime", endTime);
		ArrayList list = new ArrayList();
		list.add(paramsMap);
		HashMap<String,Object>  paramsMapTemp=new HashMap<String,Object>();
		paramsMapTemp.put("data", list);
		JSONObject jsonObject = JSONObject.fromObject(paramsMapTemp);
		
		
		String timestamp = ""+ getTimeStamp();
		String token= RandomStringUtils.randomAlphanumeric(16);
		logger.debug("input:{}",jsonObject.toString());
		
		String toSign =jsonObject.toString()+"#"+ token + "#" + timestamp;	
		String sign = ApiSecurityUtils.encrypt(toSign, partnerKey, ApiSecurityUtils.HS256);
	
		HashMap<String,Object>  params=new HashMap<String,Object>();
		params.put("partnerId", partnerId);
		params.put("timestamp", timestamp);
		params.put("token", token);
		params.put("input", jsonObject.toString());
		params.put("sign", sign);
			
	    logger.debug("chargeRecord!baseUri:{},paramsMap:{}",baseUri,params);
    	String res = RequestUtil.doPost(baseUri, params);
    
    	logger.debug("sco chargeRecord res:{}",res);

	}
	
	
	
	public static void main(String[] args) {
		
		
	/*	valid=1;
		serverIp="121.40.18.116";
		//serverIp="hzskynet.i91pv.com";
		//serverPort=55555;
		serverPort=8888;
		partnerId="310000002";
		partnerKey="tlDYWr1WtNvIyDFI";
		sendRealDataCyc=60;
		baseUrl = "https://"+serverIp+":"+serverPort;
		statusMethod="/api/pipe/status";
		chargeMethod="/api/pipe/charge-records";
		
		StopCarOrganService.realData("3301021010000009", 1, 2, 0, 0, 1470817277, 
		new BigDecimal(220.1), new BigDecimal(220.2), new BigDecimal(220.3), new BigDecimal(1.1), 
		new BigDecimal(1.2),new BigDecimal(1.3), new BigDecimal(50), new BigDecimal(380.0), 
		new BigDecimal(40.0), 65, 50);
		
		
		StopCarOrganService.chargeRecord("3301021010000009", 1,new BigDecimal(111.2),
				1467012163,1467012178);
		*/
		
	}
	
}
