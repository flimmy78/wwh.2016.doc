package com.cooperate;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooperate.cczc.CCZCPush;
import com.cooperate.constant.KeyConsts;
import com.cooperate.elease.EleasePush;
import com.cooperate.elease.EleaseService;
import com.cooperate.shstop.SHStopPush;
import com.cooperate.shstop.SHStopService;
import com.ec.constants.UserConstants;
import com.ec.utils.LogUtil;

public class CooperateFactory {
    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(CooperateFactory.class.getName()));

    private static String path = System.getProperty("file.separator")
            + System.getProperty("user.dir")
            + System.getProperty("file.separator") + "conf"
            + System.getProperty("file.separator") + "cooperate"
            + System.getProperty("file.separator");

    private static ConcurrentHashMap<Integer, Push> cooperateMaps = new ConcurrentHashMap<>();

    public static void init() {
        try {
            if (isCooperate(UserConstants.ORG_EC)) {
                EleasePush eleasePush = new EleasePush();
                if (eleasePush.init(path + KeyConsts.ELEASE_SETTING)) {
                    logger.info(LogUtil.addExtLog("orgNo"), UserConstants.ORG_EC);
                    cooperateMaps.put(UserConstants.ORG_EC, eleasePush);

                    EleaseService.startEleasePushTimeout(10);
                }
            }

            if (isCooperate(UserConstants.ORG_CCZC)) {
                CCZCPush cczcPush = new CCZCPush();
                if (cczcPush.init(path + KeyConsts.CCZC_SETTING)) {
                    logger.info(LogUtil.addExtLog("orgNo"), UserConstants.ORG_CCZC);
                    cooperateMaps.put(UserConstants.ORG_CCZC, cczcPush);
                }
            }

            if (isCooperate(UserConstants.ORG_SHSTOP)) {
                SHStopPush shPush = new SHStopPush();
                if (shPush.init(path + KeyConsts.SHSTOP_SETTING)) {
                    logger.info(LogUtil.addExtLog("orgNo"), UserConstants.ORG_SHSTOP);
                    cooperateMaps.put(UserConstants.ORG_SHSTOP, shPush);

                    SHStopService.startSHSTOPPushTimeout(10);
                }
            }
        } catch (Exception e) {
            logger.error(LogUtil.addExtLog("exception"), e.getStackTrace());
        }
    }

    public static boolean isCooperate(int orgNo) {
        File file;

        if (orgNo == UserConstants.ORG_EC) {
            file = new File(path + KeyConsts.ELEASE_SETTING);
            if (file.exists()) return true;
        } else if (orgNo == UserConstants.ORG_CCZC) {
            file = new File(path + KeyConsts.CCZC_SETTING);
            if (file.exists()) return true;
        } else if (orgNo == UserConstants.ORG_SHSTOP) {
            file = new File(path + KeyConsts.SHSTOP_SETTING);
            if (file.exists()) return true;
        }

        return false;
    }

    public static IPush getPush(int orgNo) {
        return cooperateMaps.get(orgNo);
    }

    public static Push getCoPush(int orgNo) {
        return cooperateMaps.get(orgNo);
    }
}
