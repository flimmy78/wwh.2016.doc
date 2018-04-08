package com.ec.usrcore.service;

import com.ec.cache.AuthUserCache;
import com.ec.cache.BaseGunCache;
import com.ec.logs.LogConstants;
import com.ec.service.AbstractEpGunService;
import com.ec.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpGunService extends AbstractEpGunService {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(EpGunService.class.getName()));

    public static void unUseEpGun(String epCode, int epGunNo, int orgNo, int userIdentity) {
        BaseGunCache gunCache = CacheService.getEpGunCache(epCode, epGunNo);
        if (gunCache == null)
            return;
        AuthUserCache auth = gunCache.getAuth();
        if (auth == null)
            return;

        /*if (orgNo != UserConstants.ORG_I_CHARGE)
            return;*/

        if (userIdentity == auth.getUsrId()) {
            logger.info(LogUtil.getBaseLog(), new Object[]{LogConstants.FUNC_PHONE_DISCONNECT, epCode, epGunNo, orgNo, userIdentity});
            gunCache.setAuth(null);
        }
    }
}
