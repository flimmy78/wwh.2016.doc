package com.ec.usrcore.cache;

import com.ec.cache.BaseEPCache;

public class ElectricPileCache extends BaseEPCache {

    public ElectricPileCache(String epCode) {
        super(epCode);
    }

    private int netStatus;

    public int getNetStatus() {
        return netStatus;
    }

    public void setNetStatus(int netStatus) {
        this.netStatus = netStatus;
    }
}


