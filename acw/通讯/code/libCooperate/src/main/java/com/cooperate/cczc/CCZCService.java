package com.cooperate.cczc;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooperate.CooperateFactory;
import com.cooperate.utils.HttpUtils;
import com.cooperate.utils.SigTool;
import com.ec.constants.UserConstants;
import com.ec.utils.LogUtil;

public class CCZCService {
	private static final Logger logger =  LoggerFactory.getLogger(LogUtil.getLogName(CCZCService.class.getName()));
	
    public static String sendChargeResp(Map<String ,Object> params) {
    	
        return send2CCZC(CooperateFactory.getCoPush(UserConstants.ORG_CCZC).getChargeRespUrl(), params);
    }
    public static String sendStopChargeResp(Map<String ,Object> params) {
    	
        return send2CCZC(CooperateFactory.getCoPush(UserConstants.ORG_CCZC).getStopchargeRespUrl(), params);
    }
    public static String sendRealData(Map<String ,Object> params) {
    	logger.debug(LogUtil.addExtLog("E_REAL_DATA_URL"),CooperateFactory.getCoPush(UserConstants.ORG_CCZC).getRealDataUrl());
    	
        return send2CCZC(CooperateFactory.getCoPush(UserConstants.ORG_CCZC).getRealDataUrl(), params);
    }
    public static String sendOrderInfo(Map<String ,Object> params) {
    	
        return send2CCZC(CooperateFactory.getCoPush(UserConstants.ORG_CCZC).getOrderUrl(), params);
    }
    private static String send2CCZC(String url, Map<String ,Object> params) {
    	logger.debug(LogUtil.addExtLog("url|params"),url,params);
		
        fullParams(params);
        if (null == params) {
            logger.error(LogUtil.addExtLog("is fail;url"), url);
            return null;
        }
        String response = null;
        try {
            response = HttpUtils.httpPostObject(url, params);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info(LogUtil.addExtLog("response"),response);
        return response;
    }

    private static void fullParams(Map<String,Object> map) {
        String sig = SigTool.getSignMd5(map, CooperateFactory.getCoPush(UserConstants.ORG_CCZC).getAppsecret());
        if (null == sig)  {
            logger.error(LogUtil.addExtLog("sig generate is fail;map"), map);
        	map = null;
        } else {
        	map.put("sign", sig);
        }
    }

}
