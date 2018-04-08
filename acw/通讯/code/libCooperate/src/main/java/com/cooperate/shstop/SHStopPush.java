package com.cooperate.shstop;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooperate.Push;
import com.cooperate.config.ConfigManager;
import com.cooperate.constant.KeyConsts;
import com.cooperate.utils.Strings;
import com.ec.constants.Symbol;
import com.ec.logs.LogConstants;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import com.ec.utils.NumUtil;

public class SHStopPush extends Push{
	private static final Logger logger =  LoggerFactory.getLogger(LogUtil.getLogName(SHStopPush.class.getName()));
	
	public SHStopPush()
	{
		
	}

	public int convertWorkStauts(int param)
	{
		// 充电设备接口状态(1：空闲， 2：占用（未充电）， 3：占用（充电中）， 4：占用（预约锁定）, 255：故障)
		int value = 1;
		if(param==3)
			value= 3;
		else if(param==1||param==0||param>30)
			value= 255;
		else if(param==8)
			value= 4;
		else if(param==2)
			value= 1;
		else
			value= 2;
		return value;
	}
	
	@Override
	public boolean init(String filename) {
		ConfigManager conf = ConfigManager.getMessageManager();
		boolean b=conf.init(filename);
    	
		appId = conf.getEChongProperties(KeyConsts.E_APP_ID);
		appKey = conf.getEChongProperties(KeyConsts.E_APP_KEY);
		appsecret = conf.getEChongProperties(KeyConsts.APPSECRET);
		realDataUrl = conf.getEChongProperties(KeyConsts.E_REAL_DATA_URL);
		orderUrl = conf.getEChongProperties(KeyConsts.E_ORDER_URL);
		tokenUrl = conf.getEChongProperties(KeyConsts.E_TOKEN_URL);
        
		orgNo = Integer.valueOf(conf.getEChongProperties(KeyConsts.E_ORG_NO,"9004"));
		mode = Integer.valueOf(conf.getEChongProperties(KeyConsts.E_REAL_DATA_MODE,"1"));
		period = Integer.valueOf(conf.getEChongProperties(KeyConsts.E_REAL_DATA_PERIOD,"30"));
		
		return b;
	}
	
	@Override
	public void onChargeEpResp(String token,int orgNo,String userIdentity,String epCode,int epGunNo, String extra,int ret,int errorCode) {
	}

	@Override
	public void onStopChargeEpResp(String token,int orgNo,String userIdentity,String epCode,int epGunNo, String extra,int ret,int errorCode) {
	}

	@Override
	public void onChargeEvent(int orgNo,String userIdentity,String epCode,int epGunNo, String extra,int ret,int errorCode) {
	}
	
	@Override
	public void onEpStatusChange(String token,int orgNo,String userIdentity,String epCode,int epGunNo
			,int inter_type,Map<String ,Object> realData,String extra) {
	}
	
	@Override
	public void onEpNetStatusChange(int orgNo, String epCode, int netStatus) {
	}

	@Override
	public void onRealData(String token,int orgNo,String userIdentity,String epCode,int epGunNo,int inter_type,float servicePrice,Map<String ,Object> realData,String extra) {
		try {
	        logger.info(LogUtil.addBaseExtLog("servicePrice|realData|extra"),new Object[]{LogConstants.FUNC_ONREALDATA,epCode,epGunNo,orgNo,userIdentity,token,servicePrice,realData,extra});

	        HashMap<String,Object> resMap=new HashMap<String,Object>();
			resMap.put("session_id", token);
			resMap.put("ConnectorID", String.format("%s%02d", epCode,epGunNo));//pile_code 是 string 充电桩编码
			resMap.put("Status", convertWorkStauts(Strings.getIntValue(realData,"3_1")));
			resMap.put("user_id", userIdentity);
		
			int v = Strings.getIntValue(realData,"3_44");
			resMap.put("CurrentA", v);//A相电流
			v = Strings.getIntValue(realData,"3_45");
			resMap.put("CurrentB", v);//B相电流
			v = Strings.getIntValue(realData,"3_46");
			resMap.put("CurrentC", v);//C相电流
			
			v = Strings.getIntValue(realData,"3_41");
			resMap.put("VoltageA", v);//A相电压
			v = Strings.getIntValue(realData,"3_42");
			resMap.put("VoltageB", v);//B相电压
			v = Strings.getIntValue(realData,"3_43");
			resMap.put("VoltageC", v);//C相电压
		
			v = Strings.getIntValue(realData,"1_5");
			if (v == 0) {
				v = 50;
			} else if (v == 1) {
				v = 100;
			} else {
				v = 0;
			}
			resMap.put("ParkStatus", v);//车位状态
			v = Strings.getIntValue(realData,"3_2");
			if (v == 1) {
				v = 10;
			} else if (v > 2) {
				v = 50;
			} else {
				v = 0;
			}
			resMap.put("LockStatus", v);//地锁状态
			
			v = Strings.getIntValue(realData,"3_5");
			resMap.put("soc", NumUtil.intToBigDecimal2(v).multiply(new BigDecimal(10)));//剩余电量（ 汽车电量的百分比）

			long time = DateUtil.getCurrentSeconds();
			SHStopService.addRealData(resMap.get("ConnectorID") + Symbol.SPLIT + time
					+ Symbol.SPLIT + KeyConsts.E_REAL_DATA_URL, resMap);
		} catch (Exception e) {
			logger.error(LogUtil.addExtLog("exception"), e.getStackTrace());
		}
	}

	@Override
	public void onChargeOrder(String token,int orgNo,String userIdentity,String epCode,int epGunNo,
			int inter_type,float money,float elect_money,float service_money,float elect,float start_elect,float end_elect
			,float cusp_elect,float cusp_elect_price,float cusp_service_price,float cusp_money,float cusp_elect_money,float cusp_service_money
			,float peak_elect,float peak_elect_price,float peak_service_price,float peak_money,float peak_elect_money,float peak_service_money
			,float flat_elect,float flat_elect_price,float flat_service_price,float flat_money,float flat_elect_money,float flat_service_money
			,float valley_elect,float valley_elect_price,float valley_service_price,float valley_money,float valley_elect_money,float valley_service_money
			,int start_time,int end_time,int stop_model,int stop_reason,int soc,int time,String extra) 
	{
		try {
	        logger.info(LogUtil.addBaseExtLog("inter_type|money|extra|start_time|end_time"),new Object[]{LogConstants.FUNC_ONCHARGEORDER,
        		epCode,epGunNo,orgNo,userIdentity,token,inter_type,money,extra, start_time, end_time});
	    	
			HashMap<String,Object> resMap=new HashMap<String,Object>();
			
			resMap.put("OperatorID", KeyConsts.PRI_OPERATOR_ID);
			//充电桩编码
			resMap.put("ConnectorID", String.format("%s%02d", epCode,epGunNo));//pile_code 是 string 充电桩编码
			
			resMap.put("StartChargeSeq", extra);//充电订单号
			
			resMap.put("UserChargeType", token);//用户发起充电类型
			
			resMap.put("MobileNumber", userIdentity);//用户手机号
			
			//money 是 float 本次充电消费总金额（ 电费+服务费）
			resMap.put("Money", money);
		
			//elect_money 是 float 本次充电电费金额
			resMap.put("ElectMoney", elect_money);
			
			//service_money 是 float 本次充电服务费金额
			resMap.put("ServiceMoney", service_money);
			
			//elect 是 float 充电电量
			resMap.put("Elect", elect);
			
			//start_elect 是 float 开始充电电量
			resMap.put("start_elect", start_elect);
			
			//end_elect 是 float 结束充电电量
			resMap.put("end_elect", end_elect);
			
			//cusp_elect 是 float 尖阶段电量
			resMap.put("CuspElect", cusp_elect);
			
			//cusp_elect_price 是 float 尖电价价格
			resMap.put("CuspElectPrice", cusp_elect_price);
			
			//cusp_service_price 是 float 尖服务费单价
			resMap.put("CuspServicePrice", cusp_service_price);
			
			//cusp_money 是 float 尖总金额
			resMap.put("CuspMoney", cusp_money);
			
			//cusp_elect_money 是 float 尖充电金额
			resMap.put("CuspElectMoney", cusp_elect_money);
			
			//cusp_service_money 是 float 尖服务费金额
			resMap.put("CuspServiceMoney", cusp_service_money);
			
			//peak_elect 是 float 峰阶段电量
			resMap.put("PeakElect", peak_elect);
			
			//peak_elect_price 是 float 峰电价价格
			resMap.put("PeakElectPrice", peak_elect_price);
			
			//peak_service_price 是 float 峰服务费单价
			resMap.put("PeakServicePrice", peak_service_price);
			
			//peak_money 是 float 峰总金额
			resMap.put("PeakMoney", peak_money);
			
			//peak_elect_money 是 float 峰充电金额
			resMap.put("PeakElectMoney", peak_elect_money);
			
			//peak_service_money 是 float 峰服务费金额
			resMap.put("PeakServiceMoney", peak_service_money);
			
			//flat_elect 是 float 平阶段电量
			resMap.put("FlatElect", flat_elect);
			
			//flat_elect_price 是 float 平阶段电价
			resMap.put("FlatElectPrice", flat_elect_price);
			
			//flat_service_price 是 float 平阶段服务费单价
			resMap.put("FlatServicePrice", flat_service_price);
			
			//flat_money 是 float 平总金额
			resMap.put("FlatMoney", flat_money);
			
			//flat_elect_money 是 float 平充电金额
			resMap.put("FlatElectMoney", flat_elect_money);
			
			//flat_service_money 是 float 平 服务费金额
			resMap.put("FlatServiceMoney", flat_service_money);
			
			//valley_elect 是 float 谷阶段电量
			resMap.put("ValleyElect", valley_elect);
			
			//valley_elect_price 是 float 谷阶段电价
			resMap.put("ValleyElectPrice", valley_elect_price);
			
			//valley_service_price 是 float 谷阶段服务费单价
			resMap.put("ValleyServicePrice", valley_service_price);
			
			//valley_money 是 float 谷总金额
			resMap.put("ValleyMoney", valley_money);
			
			//valley_elect_money 是 float 谷充电金额
			resMap.put("ValleyElectMoney", valley_elect_money);
			
			//valley_service_money 是 float 谷服务费金额
			resMap.put("ValleyServiceMoney", valley_service_money);
			
			//start_time 是 int 充电开始时间（ 秒格式 Unix 时间戳）
			resMap.put("StartTime", DateUtil.longDateToString(new Long(start_time)*1000));
			
			//end_time 是 int 充电结束时间（ 秒格式 Unix 时间戳）
			resMap.put("EndTime", DateUtil.longDateToString(new Long(end_time)*1000));
			
			//支付金额
			resMap.put("PaymentAmount", money);
			
			//支付时间
			resMap.put("PayTime", DateUtil.longDateToString(new Long(time)*1000));
			
			//支付方式
			resMap.put("PayChannel", 6);
		
			//优惠信息描述
			resMap.put("DiscountInfo", "无");
			
			SHStopService.addRealData(resMap.get("ConnectorID") + Symbol.SPLIT + resMap.get("time")
					+ Symbol.SPLIT + KeyConsts.E_ORDER_URL, resMap);
		} catch (Exception e) {
			logger.error(LogUtil.addExtLog("exception"), e.getStackTrace());
		}
	}
	
}
