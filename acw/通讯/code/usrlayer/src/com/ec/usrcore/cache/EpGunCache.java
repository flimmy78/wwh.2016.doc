package com.ec.usrcore.cache;

import com.ec.cache.BaseGunCache;
import com.ec.cache.UserCache;
import com.ec.service.AbstractCacheService;
import com.ec.usrcore.service.CacheService;

public class EpGunCache extends BaseGunCache {

    public EpGunCache(String epCode, int epGunNo) {
        super(epCode, epGunNo);
    }

    public void cleanChargeInfo()
	{
		if(chargeCache!=null)
		{
			
			int userId = chargeCache.getUserId();
			
			if(userId>0)
			{
				UserCache u2= AbstractCacheService.getUserCache(userId);
				if(u2!=null )
				{
					u2.clean();
					CacheService.putUserCache(u2);
				}
			}
		}
	}
}
