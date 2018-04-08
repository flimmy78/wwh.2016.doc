package com.cooperate.elease;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooperate.CooperateFactory;
import com.cooperate.constant.KeyConsts;
import com.cooperate.utils.HttpUtils;
import com.cooperate.utils.SigTool;
import com.ec.constants.Symbol;
import com.ec.constants.UserConstants;
import com.ec.netcore.core.pool.TaskPoolFactory;
import com.ec.utils.LogUtil;

public class EleaseService {
	private static final Logger logger =  LoggerFactory.getLogger(LogUtil.getLogName(EleaseService.class.getName()));

	/**
	 * 实时数据key:epCode+epGun|time|send_url
	 * 消费记录key:epCode+epGun|time|send_url
	 */
	private static Map<String ,Map<String,Object>> mapRealData = new ConcurrentHashMap<String, Map<String,Object>>();
	
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
	
    public static String sendChargeResp(Map<String ,Object> params) {
    	
        return send2EChong(CooperateFactory.getCoPush(UserConstants.ORG_EC).getChargeRespUrl(), params);
    }
    public static String sendStopChargeResp(Map<String ,Object> params) {
    	
        return send2EChong(CooperateFactory.getCoPush(UserConstants.ORG_EC).getStopchargeRespUrl(), params);
    }
    public static String sendEpStatusChange(Map<String ,Object> params) {
    	logger.debug(LogUtil.addExtLog("E_EPSTATUS_CHANGE_URL"),CooperateFactory.getCoPush(UserConstants.ORG_EC).getStatusChangeUrl());
    	
        return send2EChong(CooperateFactory.getCoPush(UserConstants.ORG_EC).getStatusChangeUrl(), params);
    }
    public static String sendRealData(Map<String ,Object> params) {
    	logger.debug(LogUtil.addExtLog("E_REAL_DATA_URL"),CooperateFactory.getCoPush(UserConstants.ORG_EC).getRealDataUrl());
    	
        return send2EChong(CooperateFactory.getCoPush(UserConstants.ORG_EC).getRealDataUrl(), params);
    }
    public static String sendOrderInfo(Map<String ,Object> params) {
    	logger.debug(LogUtil.addExtLog("E_ORDER_URL"),CooperateFactory.getCoPush(UserConstants.ORG_EC).getOrderUrl());
    	
        return send2EChong(CooperateFactory.getCoPush(UserConstants.ORG_EC).getOrderUrl(), params);
    }
    private static String send2EChong(String url, Map<String ,Object> params2) {
    	logger.debug(LogUtil.addExtLog("url|params2"),url,params2);
    	
    	JSONObject jsonObject = JSONObject.fromObject(params2);
		
        Map<String, String> params = fullParams(jsonObject.toString());
        if (null == params) {
            logger.error(LogUtil.addExtLog("is fail;url"), url);
            return null;
        }
        String response = null;
        try {
            response = HttpUtils.httpPost(url, params);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info(LogUtil.addExtLog("response"),response);
        return response;
    }

    private static Map<String, String> fullParams(String info) {
        String app_id = CooperateFactory.getCoPush(UserConstants.ORG_EC).getAppId();  //e充网分配的app_id
        String app_key = CooperateFactory.getCoPush(UserConstants.ORG_EC).getAppKey();  //e充网分配的app_key
        
        logger.debug(LogUtil.addExtLog("fullParams!app_id|app_key|info"),
       		 new Object[]{app_id, app_key, info});
        
        Map<String, String> params = new HashMap<>();
        params.put("app_id", app_id);
        params.put("info", info);
        String sig = SigTool.getSignString(app_id, info, app_key);
        if (null == sig) {
             logger.error(LogUtil.addExtLog("sig generate is fail;app_id|info|app_key"),
            		 new Object[]{app_id, info, app_key});
            return null;
        }
        params.put("sig", sig);
        return params;
    }

	
	public static void startEleasePushTimeout(long initDelay) {
		
		CheckEleasePushTask checkTask = new CheckEleasePushTask();
				
		TaskPoolFactory.scheduleAtFixedRate("CHECK_ELEASEPUSH_TIMEOUT_TASK", checkTask, initDelay, 5, TimeUnit.SECONDS);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void checkEleasePushTimeout()
	{
		try {
			Iterator iter = mapRealData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				Map<String,Object> pointMap=(Map<String,Object>) entry.getValue();	
				String key = (String)entry.getKey();
	
				String[] val = key.split(Symbol.SHUXIAN_REG);
				if (val.length == 3) {
					if (KeyConsts.E_CHARGE_RESP_URL.equals(val[2])) {
					 	sendChargeResp(pointMap);
					} else if (KeyConsts.E_STOPCHARGE_RESP_URL.equals(val[2])) {
						sendStopChargeResp(pointMap);
					} else if (KeyConsts.E_STATUS_CHANGE_URL.equals(val[2])) {
						sendEpStatusChange(pointMap);
					} else if (KeyConsts.E_REAL_DATA_URL.equals(val[2])) {
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
