package com.ec.net.message;


import java.util.HashMap;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSONObject;
//import com.alibaba.fastjson.JSONObject;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;

public class AliSMS {
	private static Log log = LogFactory.getLog(AliSMS.class);
	
	private static String APPKEY = "23459118";
	private static String SECRET = "7c2d3d3bf321244d95f3763138db053c";
	private static String URL = "http://gw.api.taobao.com/router/rest";
	
	/**
	 * 阿里大于短信发送
	 * @param phoneNum 接受手机号
	 * @param smsId 短信模板id
	 * @param parValue json参数字符串：{"name":"张三"}
	 * @return
	 */
	public static boolean sendAliSMS(String phoneNum,String smsId,String parValue){
		
		TaobaoClient client = new DefaultTaobaoClient(URL, APPKEY, SECRET);
		AlibabaAliqinFcSmsNumSendRequest req = new AlibabaAliqinFcSmsNumSendRequest();
		req.setSmsType("normal");
		req.setSmsFreeSignName("爱充网");
		req.setSmsParamString(parValue);
		req.setRecNum(phoneNum);
		req.setSmsTemplateCode(smsId);
		try{
			AlibabaAliqinFcSmsNumSendResponse rsp = client.execute(req);
			//解析发送结果
			JSONObject jo = JSONObject.parseObject(rsp.getBody());
			JSONObject jo1 = jo.getJSONObject("alibaba_aliqin_fc_sms_num_send_response");
			JSONObject jo2 = jo1.getJSONObject("result");
			String code = jo2.getString("err_code");
			if("0".equals(code)) return true;
			log.error("发送短信失败，返回码：" + code);
			return false;
		}catch(Exception e){
			log.error("发送阿里短信失败");
			log.error(e.getMessage());
			return false;
		}
		
	}
	
	public static void main(String[] args){
		//boolean flag=AliSMS.sendAliSMS("13757137030", "SMS_16775083", "{\"error\":\"19\"}");
		//flag=AliSMS.sendAliSMS("13757137030", "SMS_16830122", "{\"time\":\"2016-11-06 13:52:19\"}");
		
		HashMap<String,Object>  params=new HashMap<String,Object>();
		params.put("time", "15");
		params.put("addr", "dddd");
		
		
		
		String jsonObject = JSONObject.toJSONString(params);
		
		boolean flag=AliSMS.sendAliSMS("13757137030", "SMS_17110100",jsonObject );
		
		/*String back = "{\"alibaba_aliqin_fc_sms_num_send_response\":"
				+ "{\"result\":{\"err_code\":\"0\",\"model\":\"103108886119^1103955653484\",\"success\":true},\"request_id\":\"3jvmqc7gt6wj\"}}";
		JSONObject jo = JSONObject.parseObject(back);
		JSONObject jo1 = jo.getJSONObject("alibaba_aliqin_fc_sms_num_send_response");
		JSONObject jo2 = jo1.getJSONObject("result");
		String code = jo.get("err_code").toString();
		System.out.println(code);*/
		
	}
}
