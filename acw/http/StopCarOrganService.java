package com.epcentre.epconsumer;

import io.netty.handler.codec.http.HttpContent;
import io.netty.util.CharsetUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import net.sf.json.JSONObject;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.utils.DateUtil;
import com.netCore.netty.httpclient.HttpPostClient;
import com.netCore.netty.httpclient.HttpPostReq;

public class StopCarOrganService {
	
	private static final Logger logger = LoggerFactory.getLogger(StopCarOrganService.class);
	
	
private static String serverIp;
	private static int serverPort;
	private static String baseUrl;
	private static String partnerId;
	private static String partnerKey;
	private static int valid=0;
	private static long sendRealDataCyc=60;//发送实时数据周期

	
	
	public static void Init(String ip,int port,String Id,String key,int validFlag,long sendCycle)
	{
		valid=validFlag;
		serverIp = ip;
		serverPort = port;
		partnerId = Id;
		partnerKey = key;
		sendRealDataCyc = sendCycle;
		baseUrl = "https://"+serverIp+":"+serverPort;
	}
	
	
	
	public static String getServerIp() {
		return serverIp;
	}

	public static int getServerPort() {
		return serverPort;
	}

	public static String getPartnerId() {
		return partnerId;
	}
	public static String getPartnerKey() {
		return partnerKey;
	}

	public static int getValid() {
		return valid;
	}
	public static void setValid(int validFlag) {
		 valid = validFlag;
	}
	public static long getSendRealDataCyc() {
		return sendRealDataCyc;
	}

	
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
	private static String  getFmtTime(Date date)
	{
		return DateUtil.toDateFormat(date, DateFmt); 
	}
	
	/**
	 * 3充电桩充电记录
	 */
	public static void realData(String epCode,int gunNo,int workStatus,int gunConnStatus,int faultCode,long createTime,BigDecimal vol_a,
		BigDecimal vol_b,BigDecimal vol_c,BigDecimal cur_a,BigDecimal cur_b,BigDecimal cur_c, BigDecimal power,
		BigDecimal volt,BigDecimal cur,int soc,int restTime)
	{
		String method="/api/pipe/chargepilestatus";
		
		//参数转换
		String statusTime= getFmtTime(createTime);
		BigDecimal value = vol_a.multiply(new BigDecimal(1000000.0));
		String svolA = value.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		value = vol_b.multiply(new BigDecimal(1000000.0));
		String svolB = value.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		value = vol_c.multiply(new BigDecimal(1000000.0));
		String svolC = value.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		value = cur_a.multiply(new BigDecimal(1000000.0));
		String scurA = value.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		value = cur_b.multiply(new BigDecimal(1000000.0));
		String scurB = value.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		value = cur_c.multiply(new BigDecimal(1000000.0));
		String scurC = value.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		value = power.multiply(new BigDecimal(1000000.0));
		String sPower = value.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		value = volt.multiply(new BigDecimal(1000000.0));
		String sVolt = value.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		value = cur.multiply(new BigDecimal(1000000.0));
		String sCur = value.setScale(6,java.math.BigDecimal.ROUND_HALF_UP).toString();
		
		
		
		
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
		String toSign = token + "#" + timestamp;
	    String sign = ApiSecurityUtils.encrypt(toSign, partnerKey, ApiSecurityUtils.HS256);
		
		HashMap<String,Object>  params=new HashMap<String,Object>();
		params.put("partnerId", partnerId);
		params.put("timestamp", timestamp);
		params.put("token", token);
		params.put("input", jsonObject.toString());
		params.put("sign", sign);
		
		String baseUri=baseUrl +method;
	    logger.debug("realData!baseUri:{},paramsMap:{}",baseUri,params);
		
		try {
			SCOHttpPostReq req = new SCOHttpPostReq();
			req.setParams(params);
			req.setBaseUri(baseUri);
			req.setMethod(method);
			req.setRepeatSend(true);
			req.setSendNum(0);
			
			new HttpPostClient(req).run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * 3充电桩充电记录
	 */
	public static void chargeRecord(String epCode,int gunNo,BigDecimal bdmeterNum,
		long lStartTime,long lEndTime)
	{
		String method="/api/pipe/pile-charge-record";
		
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
		BigDecimal value = bdmeterNum.multiply(new BigDecimal(1000.0));
		String meterNum = value.setScale(3,java.math.BigDecimal.ROUND_HALF_UP).toString();
		paramsMap.put("meterNum", meterNum);
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
		
		String toSign = token + "#" + timestamp;	
		String sign = ApiSecurityUtils.encrypt(toSign, partnerKey, ApiSecurityUtils.HS256);
	
		HashMap<String,Object>  params=new HashMap<String,Object>();
		params.put("partnerId", partnerId);
		params.put("timestamp", timestamp);
		params.put("token", token);
		params.put("input", jsonObject.toString());
		params.put("sign", sign);
			
		String baseUri=baseUrl +method;
	    logger.debug("chargeRecord!baseUri:{},paramsMap:{}",baseUri,params);
		
		try {
			HttpPostReq req = new HttpPostReq();
			req.setParams(params);
			req.setBaseUri(baseUri);
			req.setMethod(method);
			req.setRepeatSend(true);
			req.setSendNum(0);
			
			new HttpPostClient(req).run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static int handHttpContent(HttpContent content,HttpPostReq req)
	{
		String sContent =content.content().toString(CharsetUtil.UTF_8);
		logger.info(sContent);
	    int a=sContent.indexOf('{');
	    if(a==0)
	    {
	    	JSONObject jb = JSONObject.fromObject(sContent.substring(a));
		 	int bResult = jb.getInt("ret");
		 	logger.debug("ret:{}",bResult);
		 	if(bResult == 0)
		 	{
		 	}
		 	else
		 	{
		 		logger.error("sContent:{},Uri:{}",sContent,req.getBaseUri()+req.getMethod());
		 	}
        }
	    return 0;
	}
	
	public static void main(String[] args) {
		
		/*StopCarOrganService.realData("3301021010000007", 1, 2, 0, 0, 0, 
				new BigDecimal(220.1), new BigDecimal(220.2), new BigDecimal(220.3), new BigDecimal(1.1), 
				new BigDecimal(1.2),new BigDecimal(1.3), new BigDecimal(50), new BigDecimal(380.0), 
				new BigDecimal(40.0), 65, 50);
				
		
		StopCarOrganService.chargeRecord("3301021010000007", 1,new BigDecimal(111.2),
				1467012163,1467012178);
		*/
		
	}
	
}
