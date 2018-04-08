package com.ec.usrcore.service;

import com.ec.service.AbstractEpService;
import com.ec.usrcore.net.client.EpGateNetConnect;
import com.ec.utils.DateUtil;
import io.netty.channel.Channel;

public class EpService extends AbstractEpService {

    public static Channel checkEpGate(String epCode) {
        EpGateNetConnect commClient = CacheService.getEpGate(epCode);

        if (commClient == null) {
            return null;
        }
        if (!commClient.isComm()) {
            return null;
        }

        commClient.setLastSendTime(DateUtil.getCurrentSeconds());
        return commClient.getChannel();
    }

}
