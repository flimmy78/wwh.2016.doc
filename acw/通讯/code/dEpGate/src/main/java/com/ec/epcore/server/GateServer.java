package com.ec.epcore.server;

import com.ec.service.impl.EpServiceImpl;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.cooperate.real3rdFactory;
import com.ec.epcore.config.GameConfig;
import com.ec.epcore.net.client.MonitorConnect;
import com.ec.epcore.net.codec.ApiDecoder;
import com.ec.epcore.net.codec.ApiEncoder;
import com.ec.epcore.net.codec.EpDecoder;
import com.ec.epcore.net.codec.EpEncoder;
import com.ec.epcore.net.codec.UsrGateDecoder;
import com.ec.epcore.net.codec.UsrGateEncoder;
import com.ec.epcore.net.server.EpNettyServer;
import com.ec.epcore.net.server.InnerApiNettyServer;
import com.ec.epcore.net.server.UsrGateServer;
import com.ec.epcore.net.server.WatchHttpServer;
import com.ec.epcore.service.EpCommClientService;
import com.ec.epcore.service.EpGunService;
import com.ec.epcore.service.EpService;
import com.ec.epcore.service.EqVersionService;
import com.ec.epcore.service.MonitorService;
import com.ec.epcore.service.RateService;
import com.ec.epcore.service.UsrGateService;
import com.ec.netcore.conf.CoreConfig;
import com.ec.netcore.model.conf.ClientConfig;
import com.ec.netcore.model.conf.ClientConfigs;
import com.ec.netcore.model.conf.ServerConfig;
import com.ec.netcore.model.conf.ServerConfigs;
import com.ec.netcore.server.impl.AbstractGameServer;
import com.ec.utils.LogUtil;
import com.ormcore.cache.GameContext;

public class GateServer extends AbstractGameServer {
    private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(GateServer.class.getName()));

    private static GateServer gameServer;

    private static final Object lock = new Object();

    /**
     * 1.创建电桩服务侦听
     * 2.创建usr gate服务侦听
     * 3.创建本程序调试服务
     * 4.建立reg-server连接服务
     * 5.建立third-server连接服务
     * 6.建立数据分析中心连接服务
     */
    public GateServer() {
        //super();

        ServerConfigs serverConfigs = CoreConfig.serverConfigs;
        ClientConfigs clientConfigs = CoreConfig.clientConfigs;

        if (serverConfigs == null || clientConfigs == null) {
            String errMsg = "【Gate服务器-客户端】缺少【外部】访问配置...服务器强行退出！";
            logger.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        //gate服务器
        //1.创建电桩侦听服务
        ServerConfig epSvrCfg = serverConfigs.get("ep-server");
        if (epSvrCfg != null) {

            ByteToMessageDecoder decoder = new EpDecoder();
            MessageToByteEncoder encoder = new EpEncoder();

            EpNettyServer epServer = new EpNettyServer(epSvrCfg, decoder, encoder, 0, 0);
            nettyServerList.add(epServer);

        } else {
            String errMsg = "ep server dont find config! exit";
            logger.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        //2.创建usrgate服务
        ServerConfig usrGateSvrCfg = serverConfigs.get("usrgate-server");
        if (usrGateSvrCfg != null) {

            ByteToMessageDecoder decoder = new UsrGateDecoder();
            MessageToByteEncoder encoder = new UsrGateEncoder();

            UsrGateServer nettyServer = new UsrGateServer(usrGateSvrCfg, decoder, encoder, 0, 0);
            nettyServerList.add(nettyServer);

        } else {
            String errMsg = "usrgate server dont find config! exit";
            logger.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        //3.创建API服务
        ServerConfig manageSvrCfg = serverConfigs.get("manage-server");
        if (manageSvrCfg != null) {

            ByteToMessageDecoder decoder = new ApiDecoder();
            MessageToByteEncoder encoder = new ApiEncoder();

            InnerApiNettyServer apiServer = new InnerApiNettyServer(manageSvrCfg, decoder, encoder, 0, 0);
            nettyServerList.add(apiServer);

        } else {
            String errMsg = "manage server dont find config! exit";
            logger.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        //3.创建本程序调试服务
        ServerConfig watchHttpSvrCfg = serverConfigs.get("epwatch-server");
        if (watchHttpSvrCfg != null) {

            WatchHttpServer watchHttpServer = new WatchHttpServer(watchHttpSvrCfg);
            nettyHttpServerList.add(watchHttpServer);

        } else {
            String errMsg = "epwatch server dont find config! exit";
            logger.error(errMsg);
            throw new RuntimeException(errMsg);

        }

        //启动客户端
        //6.建立数据分析中心连接服务
        ClientConfig monitorSvrCfg = clientConfigs.get("monitor-server");
        if (monitorSvrCfg != null) {

            MonitorConnect commClient = MonitorConnect.getNewInstance(monitorSvrCfg);

            commClient.start();

            MonitorService.setCommClient(commClient);

        } else {
            String errMsg = "monitor-server  dont find config! exit";
            logger.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        //初始化升级版本列表，启动定时升级线程
        EqVersionService.init();
        logger.debug("初始化升级版本列表\n");
        //初始化费率表
        RateService.init();
        logger.debug("初始化费率表\n");

        //第三方工厂初始化
        real3rdFactory.init();
    }

    /**
     * 创建服务端服务器
     *
     * @return
     * @author 2014-11-28
     */
    public static GateServer getInstance() {
        synchronized (lock) {
            if (gameServer == null) {
                gameServer = new GateServer();
            }
        }
        return gameServer;
    }

    public void init() {
        super.init();
        new GameConfig();//初始化服务器基础配置
        logger.info("GameConfig成功...");
        new GameContext(null);//初始化spring,加载数据库全局数据
        EpServiceImpl.updateAllCommStatus(CoreConfig.gameConfig.getId());//电桩断线更新
        logger.info("初始化服务成功...");
    }

    @Override
    public void start() {
        logger.error("watchHttpServer start");

        super.start();
    }

    @Override
    public void stop() {
        //1、停止 netty服务器、停止 netty客户端、关闭线程池、关闭任务池
        super.stop();
    }

    @Override
    public void startTimerServer() {

        //检查枪上的预约和充电超时
        EpGunService.startCheckTimeoutServer(10);
        EpService.startCheckTimeoutServer();

        //检查电桩僵尸状态通讯
        EpCommClientService.startCommClientTimeout(5);

        //检查UsrGate僵尸状态通讯
        UsrGateService.startCommClientTimeout(5);

        //检查连接监控中心通讯状态
        MonitorService.startMonitorCommTimer(10);

        EpGunService.startRepeatSendMessage();

        super.startTimerServer();

        logger.info("所有定时任务启动成功!");

    }
}
