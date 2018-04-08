package com.ec.epcore.service;

import com.ec.cache.RateInfoCache;
import com.ec.epcore.config.GameConfig;
import com.ec.epcore.net.client.EpCommClient;
import com.ec.epcore.net.codec.EpEncoder;
import com.ec.epcore.sender.EpMessageSender;
import com.ec.epcore.task.CheckRateTask;
import com.ec.net.proto.Iec104Constant;
import com.ec.netcore.core.pool.TaskPoolFactory;
import com.ec.service.AbstractRateService;
import com.ec.service.impl.RateServiceImpl;
import com.ec.utils.DateUtil;
import com.ec.utils.LogUtil;
import com.ormcore.dao.DB;
import com.ormcore.model.RateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class RateService extends AbstractRateService {
    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(RateService.class.getName()));

    public static void startCheckRateTimer(long initDelay) {

        CheckRateTask checkTask = new CheckRateTask();

        TaskPoolFactory.scheduleAtFixedRate("CHECK_RATE_UPDATE_TASK", checkTask, initDelay, 24 * 60 * 60, TimeUnit.SECONDS);

        //TaskPoolFactory.scheduleAtFixedRate("CHECK_NET_TIMEOUT_TASK", checkTask, initDelay, 300, TimeUnit.SECONDS);
    }

    public static void apiUpdateRateAction(String epCodes, int rateId) {
        RateInfo rateInfo = RateServiceImpl.findRateInfo(rateId);

        if (rateInfo == null) {
            logger.info("[Rate]RateCmd send to ep fail,not find rateinfo in db,rateId:{}", rateId);
            return;
        }
        RateInfoCache rateInfoCache = convertFromDb(rateInfo);
        if (!rateInfoCache.parseStage()) {
            rateInfoCache = RateService.getRateById(rateId);
        }
        if (rateInfoCache == null) {
            logger.info("[Rate]RateCmd send to ep fail,not find rateinfo in memory,rateId:{}", rateId);

            return;
        }
        RateService.AddRate(rateId, rateInfoCache);

        //内存里面有，等待更新的桩和现在的费率不同的下发到桩
        EpService.updateEpsRate(epCodes, rateId, rateInfoCache);
    }

    public static void init() {
        long initDelay = getRateInitDelayTime();
        logger.info("[Rate]startCheckRateTimer {} sec after", initDelay);
        startCheckRateTimer(initDelay);

        //取得全部费率
        List<RateInfo> rateList = RateServiceImpl.getAll();
        parseRates(rateList);
    }

    public static void handleConsumeModelReq(EpCommClient CommClient, String epCode, byte[] time) {
        RateInfoCache rateInfo = getRateInfo(epCode);

        if (null != rateInfo) {
            byte[] bConsumeModelProtoData = EpEncoder.do_consume_model(epCode, rateInfo);
            if (bConsumeModelProtoData == null) {
                logger.error("[Rate]handleConsumeModelReq exception,epCode:{}", epCode);
            }
            int modelId = Iec104Constant.C_CONSUME_MODEL;
            if (rateInfo.getRateInfo().getModelId() == 2)
                modelId = Iec104Constant.C_CONSUME_MODEL4;
            EpMessageSender.sendMessage(CommClient, 0, 0, modelId, bConsumeModelProtoData, time, CommClient.getVersion());
        } else {
            logger.info("[Rate]handleConsumeModelReq fail,not found rate info from epCache:{}", epCode);
        }
    }

    public static long getRateInitDelayTime() {
        long remainSecondsOfCurDay = DateUtil.getRemainSecondsOfCurDay();
        long secondsOfCurDay = 24 * 3600 - remainSecondsOfCurDay;
        long rateUpdateTime = GameConfig.rateUpdateTime;

        //如果在10分钟之内,那么定时
        if (Math.abs(secondsOfCurDay - rateUpdateTime) < GameConfig.rateUpdateTimediff) {
            return DateUtil.getRemainSecondsOfCurDay() + rateUpdateTime;
        }
        if (secondsOfCurDay > rateUpdateTime) {
            return DateUtil.getRemainSecondsOfCurDay() + rateUpdateTime;
        } else {
            return rateUpdateTime - secondsOfCurDay;
        }
    }

}
