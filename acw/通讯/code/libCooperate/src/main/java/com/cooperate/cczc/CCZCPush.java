package com.cooperate.cczc;

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

public class CCZCPush extends Push{
	private static final Logger logger =  LoggerFactory.getLogger(LogUtil.getLogName(CCZCPush.class.getName()));
	
	public CCZCPush()
	{
		
	}
	
	public int convertGunStauts(int param)
	{
		int value = 2;
		if(param==0)
			value= 1;
		else if(param==1)
			value= 3;
		return value;
	}
	public int convertWorkStauts(int param)
	{
		//inter_work_state 是 int 电桩接口工作状态(1:充电， 2:待机， 3:故障， 4:充电结束,5: 被预约, 6:暂停充电)
		//4,6
		int value = 2;
		if(param==3)
			value= 1;
		else if(param==1||param==0||param==1||param>30)
			value= 3;
		else if(param==8)
			value= 5;
		else if(param==1)
			value= 6;
		else
			value= 2;
		return value;
	}
	
	@Override
	public boolean init(String filename) {
		ConfigManager conf = ConfigManager.getMessageManager();
		boolean b=conf.init(filename);
    	
		appsecret = conf.getEChongProperties(KeyConsts.APPSECRET);
		chargeRespUrl = conf.getEChongProperties(KeyConsts.E_CHARGE_RESP_URL);
		stopchargeRespUrl = conf.getEChongProperties(KeyConsts.E_STOPCHARGE_RESP_URL);
		chargeEventUrl = conf.getEChongProperties(KeyConsts.E_CHARGE_EVENT_URL);
		realDataUrl = conf.getEChongProperties(KeyConsts.E_REAL_DATA_URL);
		orderUrl = conf.getEChongProperties(KeyConsts.E_ORDER_URL);
        
		orgNo = Integer.valueOf(conf.getEChongProperties(KeyConsts.E_ORG_NO,"9002"));
		mode = Integer.valueOf(conf.getEChongProperties(KeyConsts.E_REAL_DATA_MODE,"1"));
		period = Integer.valueOf(conf.getEChongProperties(KeyConsts.E_REAL_DATA_PERIOD,"30"));

		return b;
	}
	
	@Override
	public void onChargeEpResp(String token,int orgNo,String userIdentity,String epCode,int epGunNo, String extra,int ret,int errorCode) {
        logger.info(LogUtil.addBaseExtLog("extra"),new Object[]{LogConstants.FUNC_ONSTARTCHARGE,
    		epCode,epGunNo,orgNo,userIdentity,token,extra});

        Map<String,Object> resMap = strToMap(extra);

		if (resMap.size() > 0) CCZCService.sendChargeResp(resMap);
	}

	@Override
	public void onStopChargeEpResp(String token,int orgNo,String userIdentity,String epCode,int epGunNo, String extra,int ret,int errorCode) {
        logger.info(LogUtil.addBaseExtLog("extra"),new Object[]{LogConstants.FUNC_ONSTOPCHARGE,
    		epCode,epGunNo,orgNo,userIdentity,token,extra});

        Map<String,Object> resMap = strToMap(extra);

		if (resMap.size() > 0) CCZCService.sendStopChargeResp(resMap);
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
	        logger.info(LogUtil.addBaseExtLog("realData|extra"),new Object[]{LogConstants.FUNC_ONREALDATA,
        		epCode,epGunNo,orgNo,userIdentity,token,realData,extra});

			HashMap<String,Object> resMap=new HashMap<String,Object>();
			resMap.put("pile_code", String.format("%s%02d", epCode,epGunNo));//pile_code 是 string 充电桩编码
			
			resMap.put("inter_type", inter_type);
		
			int inter_conn_state= Strings.getIntValue(realData,"1_1");
			resMap.put("inter_conn_state", convertGunStauts(inter_conn_state));
			
			//
			int inter_work_state= Strings.getIntValue(realData,"3_1");
			if(inter_work_state ==8)
				resMap.put("inter_order_state", 2);
			else
				resMap.put("inter_order_state", 1);
			
			resMap.put("inter_work_state", convertWorkStauts(inter_work_state));
		
			int v= Strings.getIntValue(realData,"3_3");
			if(v>=0)
				resMap.put("voltage", NumUtil.intToBigDecimal1(v));
			
			v= Strings.getIntValue(realData,"3_4");
			if(v>=0)
			resMap.put("current", NumUtil.intToBigDecimal2(v));
			
			resMap.put("soc", Strings.getIntValue(realData,"3_5"));//soc 是 int 当前的 SOC（ 汽车电量的百分比）
			//resMap.put("elect_type", epGunNo);//elect_type 否 int 电表类型(1:直流, 2:交流)
			resMap.put("elect_address", "");//elect_address 否 string 电表地址(ASCII 码)
			//resMap.put("inter_type", epGunNo);//elect_rate 否 int 电表倍率
			//resMap.put("active_power", "");//active_power 否 float 有功功率
			//resMap.put("reactive_power", "");//reactive_power 否 float 无功功率
			//resMap.put("active_energy", epGunNo);//active_energy 否 float 电表有功电能
			//resMap.put("reactive_energy", epGunNo);//reactive_energy 否 float 电表无功电能
			resMap.put("fault_code", 7);//fault_code 是 int 故障码(0:急停故障, 1:电表故障， 2:接触器故
			//障， 3:读卡器故障 ， 4:内部过温故障， 5:连接器
			//故障， 6:绝缘故障， 7:其他)注： 需要将电桩内部
			//故障码转换成以上故障码。 无故障则传 7
			resMap.put("err_code", 2);//err_code 是 int 错误码(0:电流异常， 1:电压异常， 2:其他)无错误则传 2
			v= Strings.getIntValue(realData,"3_7");
			if(v>=0)
				resMap.put("res_time", v);//res_time 是 int 剩余充电时间， 单位分钟。 未在充电状态则传 0
			else
				resMap.put("res_time", 0);
			resMap.put("parking_state", 0);//parking_state 否 int 车位状态:0:未知 1:空闲 2:占用 3:故障
			resMap.put("time", DateUtil.getCurrentSeconds());//time 是 int 上报时间（ 秒格式 Unix 时间戳）
			
			CCZCService.sendRealData(resMap);
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
	        logger.info(LogUtil.addBaseExtLog("inter_type|money|extra"),new Object[]{LogConstants.FUNC_ONCHARGEORDER,
        		epCode,epGunNo,orgNo,userIdentity,token,inter_type,money,extra});
			
			HashMap<String,Object> resMap=new HashMap<String,Object>();
			//充电桩编码
			resMap.put("pile_code", String.format("%s%02d", epCode,epGunNo));//pile_code 是 string 充电桩编码
			
			resMap.put("user_id", Long.parseLong(userIdentity));//(调用启动/停止充电时传递的 user_id)
			
			resMap.put("session_id", Long.parseLong(token));
			
			//money 是 float 本次充电消费总金额（ 电费+服务费）
			resMap.put("money", money);
		
			//elect_money 是 float 本次充电电费金额
			resMap.put("elect_money", elect_money);//(调用启动/停止充电时传递的 user_id)
			
			//service_money 是 float 本次充电服务费金额
			resMap.put("service_money", service_money);//(调用启动/停止充电时传递的 user_id)
			
			//elect 是 float 充电电量
			resMap.put("elect", elect);//(调用启动/停止充电时传递的 user_id)
			
			//start_elect 是 float 开始充电电量
			resMap.put("start_elect", start_elect);//(调用启动/停止充电时传递的 user_id)
			
			//end_elect 是 float 结束充电电量
			resMap.put("end_elect", end_elect);//(调用启动/停止充电时传递的 user_id)
			
			//cusp_elect 是 float 尖阶段电量
			resMap.put("cusp_elect", cusp_elect);//(调用启动/停止充电时传递的 user_id)
			
			//cusp_elect_price 是 float 尖电价价格
			resMap.put("cusp_elect_price", cusp_elect_price);//(调用启动/停止充电时传递的 user_id)
			
			//cusp_service_price 是 float 尖服务费单价
			resMap.put("cusp_service_price", cusp_service_price);//(调用启动/停止充电时传递的 user_id)
			
			//cusp_money 是 float 尖总金额
			resMap.put("cusp_money", cusp_money);//(调用启动/停止充电时传递的 user_id)
			
			//cusp_elect_money 是 float 尖充电金额
			resMap.put("cusp_elect_money", cusp_elect_money);//(调用启动/停止充电时传递的 user_id)
			
			//cusp_service_money 是 float 尖服务费金额
			resMap.put("cusp_service_money", cusp_service_money);//(调用启动/停止充电时传递的 user_id)
			
			//peak_elect 是 float 峰阶段电量
			resMap.put("peak_elect", peak_elect);//(调用启动/停止充电时传递的 user_id)
			
			//peak_elect_price 是 float 峰电价价格
			resMap.put("peak_elect_price", peak_elect_price);//(调用启动/停止充电时传递的 user_id)
			
			//peak_service_price 是 float 峰服务费单价
			resMap.put("peak_service_price", peak_service_price);//(调用启动/停止充电时传递的 user_id)
			
			//peak_money 是 float 峰总金额
			resMap.put("peak_money", peak_money);//(调用启动/停止充电时传递的 user_id)
			
			//peak_elect_money 是 float 峰充电金额
			resMap.put("peak_elect_money", peak_elect_money);//(调用启动/停止充电时传递的 user_id)
			
			//peak_service_money 是 float 峰服务费金额
			resMap.put("peak_service_money", peak_service_money);//(调用启动/停止充电时传递的 user_id)
			
			//flat_elect 是 float 平阶段电量
			resMap.put("flat_elect", flat_elect);//(调用启动/停止充电时传递的 user_id)
			
			//flat_elect_price 是 float 平阶段电价
			resMap.put("flat_elect_price", flat_elect_price);//(调用启动/停止充电时传递的 user_id)
			
			//flat_service_price 是 float 平阶段服务费单价
			resMap.put("flat_service_price", flat_service_price);//(调用启动/停止充电时传递的 user_id)
			
			//flat_money 是 float 平总金额
			resMap.put("flat_money", flat_money);//(调用启动/停止充电时传递的 user_id)
			
			//flat_elect_money 是 float 平充电金额
			resMap.put("flat_elect_money", flat_elect_money);//(调用启动/停止充电时传递的 user_id)
			
			//flat_service_money 是 float 平 服务费金额
			resMap.put("flat_service_money", flat_service_money);//(调用启动/停止充电时传递的 user_id)
			
			//valley_elect 是 float 谷阶段电量
			resMap.put("valley_elect", valley_elect);//(调用启动/停止充电时传递的 user_id)
			
			//valley_elect_price 是 float 谷阶段电价
			resMap.put("valley_elect_price", valley_elect_price);//(调用启动/停止充电时传递的 user_id)
			
			//valley_service_price 是 float 谷阶段服务费单价
			resMap.put("valley_service_price", valley_service_price);//(调用启动/停止充电时传递的 user_id)
			
			//valley_money 是 float 谷总金额
			resMap.put("valley_money", valley_money);//(调用启动/停止充电时传递的 user_id)
			
			//valley_elect_money 是 float 谷充电金额
			resMap.put("valley_elect_money", valley_elect_money);//(调用启动/停止充电时传递的 user_id)
			
			//valley_service_money 是 float 谷服务费金额
			resMap.put("valley_service_money", valley_service_money);//(调用启动/停止充电时传递的 user_id)
			
			//start_time 是 int 充电开始时间（ 秒格式 Unix 时间戳）
			resMap.put("start_time", start_time);//(调用启动/停止充电时传递的 user_id)
			
			//end_time 是 int 充电结束时间（ 秒格式 Unix 时间戳）
			resMap.put("end_time", end_time);//(调用启动/停止充电时传递的 user_id)
			
			//stop_model 是 int 停止时充电模式， 1 表示恒压， 2 表示恒流。
			resMap.put("stop_model", stop_model);//(调用启动/停止充电时传递的 user_id)
			
			//stop_reason 是 int 停止充电原因,1: 故障 2 充满 3 刷卡 4 其他,收到 e 充网的停止请求而停止则传 4
			resMap.put("stop_reason", stop_reason);//(调用启动/停止充电时传递的 user_id)
			
			
			//soc 是 int 当前的 soc（ 汽车电量的百分比， 范围 0-100）
			resMap.put("soc", soc);
			
			//time 是 int 订单创建时间（ 秒格式 
			resMap.put("time", time);
			
			CCZCService.sendOrderInfo(resMap);
		} catch (Exception e) {
			logger.error(LogUtil.addExtLog("exception"), e.getStackTrace());
		}
	}

	private Map<String,Object> strToMap(String extra) {
		Map<String,Object> resMap = new HashMap<String,Object>();

		try {
			String[] val = extra.split(Symbol.SHUXIAN_REG);
			resMap.put("orderId", val[0]);
			resMap.put("stubId", val[1]);
			resMap.put("outOrderId", val[2]);
			resMap.put("driverId", val[3]);
			resMap.put("timeStart", val[4]);
			resMap.put("timeEnd", val[5]);
			resMap.put("timeCharge", val[6]);
			resMap.put("feeTotal", val[7]);
			resMap.put("chargeType", val[8]);
			resMap.put("power", val[9]);
			resMap.put("soc", val[10]);
			resMap.put("status", val[11]);
			resMap.put("endInfo", val[12]);
			resMap.put("feeService", val[13]);
			resMap.put("feeElectric", val[14]);
			resMap.put("cityCode", val[15]);
		} catch (Exception e) {
			logger.error(LogUtil.addExtLog("exception"), e.getStackTrace());
		}

		return resMap;
	}
}
