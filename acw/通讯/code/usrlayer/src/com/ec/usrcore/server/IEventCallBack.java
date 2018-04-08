package com.ec.usrcore.server;

import java.util.Map;

public interface IEventCallBack {
    /**
     * 爱充手机用户能否使用电桩
     *
     * @param orgNo
     * @param userIdentity
     * @param epCode
     * @param epGunNo
     * @param account
     * @param ret
     * @param errorCode
     * @param status
     * @param currentType
     */
    void onCanUseEp(int orgNo, String userIdentity, String epCode, int epGunNo, String account, int ret, int errorCode, int status, int currentType);


    /**
     * 电桩放电事件
     *
     * @param orgNo
     * @param userIdentity
     * @param epCode
     * @param epGunNo
     * @param extra
     * @param status
     */
    void onChargeEvent(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int status);

    /**
     * 启动充电EpGate应答
     *
     * @param orgNo
     * @param userIdentity
     * @param epCode
     * @param epGunNo
     * @param extra,第三方调用时根据版本号组合不同的参数值
     * @param ret
     * @param errorCode
     */
    void onStartCharge(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int ret, int errorCode);


    /**
     * 电桩停止充电应答
     *
     * @param token
     * @param orgNo
     * @param userIdentity
     * @param epCode
     * @param epGunNo
     * @param ret
     * @param errorCode
     */
    void onStopCharge(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int ret, int errorCode);


    /**
     * 根据订单号查询订单详情应答
     *
     * @param orgNo
     * @param extra
     * @param ret
     * @param errorCode
     */
    void onQueryOrderInfo(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int ret, int errorCode);

    /**
     * 实时数据
     *
     * @param token
     * @param orgNo
     * @param userIdentity
     * @param epCode
     * @param epGunNo
     * @param realData
     */
    void onRealData(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, Map<String, Object> realData);


    /**
     * 消费记录
     *
     * @param orgNo
     * @param userIdentity
     * @param epCode
     * @param epGunNo
     * @param extra
     * @param data
     */
    void onChargeOrder(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, Map<String, Object> data);

    /**
     * 枪与车连接状态变化通知，手机端实现，e租网等第三方平台不实现
     *
     * @param orgNo
     * @param userIdentity
     * @param epCode
     * @param epGunNo
     * @param extra
     * @param status
     */
    void onGunLinkStatusChange(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int status);

    /**
     * 枪工作状态变化通知，手机端实现，e租网等第三方平台不实现
     *
     * @param orgNo
     * @param userIdentity
     * @param epCode
     * @param epGunNo
     * @param extra
     * @param status
     */
    void onGunWorkStatusChange(int orgNo, String userIdentity, String epCode, int epGunNo, String extra, int status);

}
