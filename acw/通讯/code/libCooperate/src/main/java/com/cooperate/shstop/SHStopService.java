package com.cooperate.shstop;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooperate.CooperateFactory;
import com.cooperate.constant.KeyConsts;
import com.cooperate.utils.AesCBC;
import com.cooperate.utils.HMacMD5;
import com.cooperate.utils.HttpUtils;
import com.ec.constants.Symbol;
import com.ec.constants.UserConstants;
import com.ec.netcore.core.pool.TaskPoolFactory;
import com.ec.utils.LogUtil;

public class SHStopService {
	private static final Logger logger =  LoggerFactory.getLogger(LogUtil.getLogName(SHStopService.class.getName()));

	/**
	 * 实时数据key:epCode+epGun|time|send_url
	 * 消费记录key:epCode+epGun|time|send_url
	 */
	private static Map<String ,Map<String,Object>> mapRealData = new ConcurrentHashMap<String, Map<String,Object>>();
	
	/** 自增序列 */
	private static String seq = "0001";
	
	/** 凭证有效期 */
	private static Date tokenAvailableTime = new Date();
	
	/** token值 */
	private static String staticToken = "";
	
	public static Map<String,Object> getRealData(String key)
	{
		return mapRealData.get(key);
	}

	public static void addRealData(String key, Map<String,Object> pointMap)
	{
		mapRealData.put(key, pointMap);	
	}
	 
	public static void removeRealData(String key)
	{
		mapRealData.remove(key);
	}
	
    public static String sendRealData(Map<String ,Object> params) {
    	logger.debug(LogUtil.addExtLog("E_REAL_DATA_URL"),CooperateFactory.getCoPush(UserConstants.ORG_SHSTOP).getRealDataUrl());
    	
        return send2SHStop(CooperateFactory.getCoPush(UserConstants.ORG_SHSTOP).getRealDataUrl(), params);
    }
    public static String sendOrderInfo(Map<String ,Object> params) {
    	logger.debug(LogUtil.addExtLog("E_ORDER_URL"),CooperateFactory.getCoPush(UserConstants.ORG_SHSTOP).getOrderUrl());
    	
        return send2SHStop(CooperateFactory.getCoPush(UserConstants.ORG_SHSTOP).getOrderUrl(), params);
    }
    
    /**
     * 获取接口方的token
     * 
     * @return
     */
    private static String getToken(){
        String operatorID = KeyConsts.PRI_OPERATOR_ID;
        String operatorSecret = KeyConsts.PRI_OPERATOR_SECRET;
        
        Map<String, String> tokenParams = new LinkedHashMap<>();
        tokenParams.put("OperatorID", operatorID);
        tokenParams.put("OperatorSecret", operatorSecret);
        logger.debug(LogUtil.addExtLog("OperatorID|OperatorSecret"),operatorID,operatorSecret);
        
        //数据加密
        Map<String, String> dataParam = new HashMap<>();
        dataParam.put("OperatorID", operatorID);
        dataParam.put("OperatorSecret", operatorSecret);
        JSONObject jsonObject = JSONObject.fromObject(dataParam);
        String encryptedValue = null;
        try {
			encryptedValue = AesCBC.getInstance().encrypt(jsonObject.toString(), "utf-8", operatorSecret, KeyConsts.DATA_SECRET_IV);
		} catch (Exception e) {
			logger.error(LogUtil.addExtLog("encrypt data is fail;app_id|data|app_key"),
           		 new Object[]{operatorID, encryptedValue, operatorSecret});
			return null;
		}
        tokenParams.put("Data", encryptedValue);
        
        //时间戳
      	Date date = new Date();
      	DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
      	String timeStamp = dateFormat.format(date);
      	tokenParams.put("TimeStamp", timeStamp);
      	tokenParams.put("Seq", seq);
      	
      	//服务规则
      	String sigStr = operatorID + encryptedValue + timeStamp  + seq;
        //参数签名
        String sig = HMacMD5.getHmacMd5Str(operatorSecret, sigStr);
        if (null == sig) {
           logger.error(LogUtil.addExtLog("SigSecret is fail;operatorID|Data|timeStamp|seq"),
                  		 new Object[]{operatorID, encryptedValue, timeStamp, seq});
           return null;
        }
        tokenParams.put("Sig", sig);
      	
      	//请求获取token
        String response = null;
        try {
            response = HttpUtils.httpJSONPost(CooperateFactory.getCoPush(UserConstants.ORG_SHSTOP).getTokenUrl(), tokenParams, null);
        } catch (IOException e) {
        	logger.error(LogUtil.addExtLog("request token is error; result"),
             		 new Object[]{response});
        }
        
        //处理Token
    	if (response == null) {
    		logger.error(LogUtil.addExtLog("response token is null;"));
    		return null;
		}else{
	    	JSONObject retMap = JSONObject.fromObject(response); 
	    	
	    	if (!"0".equals(retMap.get("Ret").toString())) {
	    		logger.error(LogUtil.addExtLog("response value is error; Ret|Msg"),
	             		 new Object[]{retMap.get("Ret").toString(), retMap.get("Msg").toString()});
			}
	    	    	
	    	//解密Data获取包含token值的结果集
	    	String decryptToken = "";
	    	try {
	 			decryptToken = AesCBC.getInstance().decrypt(retMap.get("Data").toString(), "utf-8", operatorSecret, KeyConsts.DATA_SECRET_IV);
	 		} catch (Exception e) {
	 			logger.error(LogUtil.addExtLog("encrypt data is fail;app_id|data|app_key"),
	            		 new Object[]{operatorID, encryptedValue, operatorSecret});
	 			return null;
	 		}	    	
	    	
	    	//记录返回结果
	    	JSONObject retTokenValue = JSONObject.fromObject(decryptToken); 
	    	Iterator<String> keys = retTokenValue.keys();
	    	String key = "", errMsg = "", retCode = "", accessToken = "";
	    	boolean isWrongflag = false;
	    	while(keys.hasNext()) {  
	            key = (String) keys.next();
	            if ("OperatorID".equals(key) && !operatorID.equals(retTokenValue.get(key).toString())) {
	            	logger.error(LogUtil.addExtLog("response operatorID is mismatch; operatorID"),
		             		 new Object[]{operatorID});
		        	return null;
				}
	            
	            if ("SuccStat".equals(key) && !"0".equals(retTokenValue.get(key).toString())) {
	            	isWrongflag = true;
				}
	            
	            if ("AccessToken".equals(key)) {
	            	accessToken = retTokenValue.get(key).toString();
				}
	            
	            if ("FailReason".equals(key)) {
	            	retCode = retTokenValue.get(key).toString();
	            	if (retCode.equals("0")) {
	            		
					}else if (retCode.equals("1")) {						
	            		errMsg = "1:无此运营商";
					}else if (retCode.equals("2")){
						errMsg = "2:密钥错误";
					}else{
						errMsg = "3~99:自定义";
					}
				}
	            
	            if ("TokenAvailableTime".equals(key)) {
	            	int availTime = Integer.parseInt(retTokenValue.get(key).toString());
	            	
	        		Calendar calObject = Calendar.getInstance();
	        		calObject.setTime(date);
	        		calObject.add(Calendar.SECOND, (int) availTime);
	        		tokenAvailableTime = calObject.getTime();
				}
	        } 
	    	if (isWrongflag && errMsg.length() > 0) {
	    		logger.error(LogUtil.addExtLog("response token is wrong; msg"),
	             		 new Object[]{errMsg});
	        	return null;
			}
	        
	    	if (accessToken.length() < 0) {
	    		logger.error(LogUtil.addExtLog("response accessToken is null;"));
	        	return null;
			}else{
		        accessToken = KeyConsts.AUTH_TOKEN + accessToken;
		        staticToken = accessToken;
		        
//		        //token中的自增序列
//		        String tempSeq = String.valueOf(Integer.parseInt(seq) + 1);
//		        int length = tempSeq.length();
//		        seq = (length == 1) ? ("000" + tempSeq) : (length == 2) ? ("00" + tempSeq) : (length == 3) ? ("0" + tempSeq) : tempSeq;
		        
		        return accessToken;
			}
        }
    }
    
    private static String send2SHStop(String url, Map<String ,Object> params2) {
    	logger.debug(LogUtil.addExtLog("url|params2|tokenAvailableTime"),new Object[]{url,params2,tokenAvailableTime});
    	
    	Map<String, Object> encData = new HashMap<>();
    	
    	//根据URL来传入不同的数据参数类型
    	if (CooperateFactory.getCoPush(UserConstants.ORG_SHSTOP).getRealDataUrl().equals(url)) {
    		encData.put("ConnectorStatusInfo", params2);
		}else{
			encData.put("OrderInfo", params2);
		}
    	JSONObject jsonObject = JSONObject.fromObject(encData);
		
    	//token值
    	String token = staticToken;
    	Date now = new Date();
    	if(now.compareTo(tokenAvailableTime) >= 0){
    		token = getToken();
    	}
    	if (token == null || token.length() < 1) {
    		logger.debug(LogUtil.addExtLog("token is null"));
		}
    	
        Map<String, String> params = fullParams(jsonObject.toString());
        if (null == params) {
            logger.error(LogUtil.addExtLog("params is null"));
            return null;
        }
        String response = null;
        try {
            response = HttpUtils.httpJSONPost(url, params, token);
            
            if (response == null || "".equals(response)) {
            	logger.info(LogUtil.addExtLog("response is null"));
            	return null;
			}else{
				//将String转成Map
				JSONObject  jasonObject = JSONObject.fromObject(response);
				Map map = (Map)jasonObject;
				//如果Ret=4002，重新请求token后再次推送数据
				String resultCode = String.valueOf(map.get("Ret"));
				if ("4002".equals(resultCode)) {
					token = getToken();
					response = HttpUtils.httpJSONPost(url, params, token);
				}
			}
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info(LogUtil.addExtLog("response"),response);
        return response;
    }

    /*private static void fullParams(Map<String,Object> map) {
        String sig = SigTool.getSignMd5(map, CooperateFactory.getCoPush(UserConstants.ORG_SHSTOP).getAppsecret());
        if (null == sig)  {
            logger.error(LogUtil.addExtLog("sig generate is fail;map"), map);
        	map = null;
        } else {
        	map.put("sign", sig);
        }
    }*/

    private static Map<String, String> fullParams(String info) {
    	logger.debug(LogUtil.addExtLog("info msg: "), info);
        Map<String, String> params = new LinkedHashMap<>();

        params.put("OperatorID", KeyConsts.PRI_OPERATOR_ID);
        
        
        String encryptedValue = null;
        try {
			encryptedValue = AesCBC.getInstance().encrypt(info, "utf-8", KeyConsts.PRI_OPERATOR_SECRET, KeyConsts.DATA_SECRET_IV);
		} catch (Exception e) {
			logger.error(LogUtil.addExtLog("encrypt data is fail; info"),
           		 new Object[]{info});
			return null;
		}
        params.put("Data", encryptedValue);
        
        //时间戳
      	Date date = new Date();
      	DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
      	String timeStamp = dateFormat.format(date);
      	params.put("TimeStamp", timeStamp);
      	params.put("Seq", seq);
      	
      	//服务规则
      	String sigStr = KeyConsts.PRI_OPERATOR_ID + encryptedValue + timeStamp  + seq;
        //参数签名
        String sig = HMacMD5.getHmacMd5Str(KeyConsts.PRI_OPERATOR_SECRET, sigStr);
        if (null == sig) {
           logger.error(LogUtil.addExtLog("SigSecret is fail;operatorID|Data|timeStamp|seq"),
                  		 new Object[]{KeyConsts.PRI_OPERATOR_ID, encryptedValue, timeStamp, seq});
           return null;
        }
        params.put("Sig", sig);
        
        return params;
    }

	
	public static void startSHSTOPPushTimeout(long initDelay) {
		
		CheckSHStopPushTask checkTask = new CheckSHStopPushTask();
				
		TaskPoolFactory.scheduleAtFixedRate("CHECK_SHSTOPPUSH_TIMEOUT_TASK", checkTask, initDelay, 5, TimeUnit.SECONDS);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void checkSHSTOPPushTimeout()
	{
		try {
			Iterator iter = mapRealData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				Map<String,Object> pointMap=(Map<String,Object>) entry.getValue();	
				String key = (String)entry.getKey();
	
				String[] val = key.split(Symbol.SHUXIAN_REG);
				if (val.length == 3) {
					if (KeyConsts.E_REAL_DATA_URL.equals(val[2])) {
						sendRealData(pointMap);
					} else if (KeyConsts.E_ORDER_URL.equals(val[2])) {
						sendOrderInfo(pointMap);
					}
				}
				removeRealData(key);
			}
		} catch (Exception e) {
			logger.error(LogUtil.addExtLog("exception"), e.getStackTrace());
		}
	}
}
