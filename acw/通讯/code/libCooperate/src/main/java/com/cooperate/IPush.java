package com.cooperate;

import java.util.Map;

public interface IPush {
	
	public boolean init(String filename);
	
	/**
	 * 充电电桩应答
	 * @param token
	 * @param orgNo
	 * @param userIdentity
	 * @param epCode
	 * @param epGunNo
	 * @param extra
	 * @param ret
	 * @param errorCode
	 */
	public void onChargeEpResp(String token,int orgNo,String userIdentity,String epCode,int epGunNo, String extra,int ret,int errorCode);
	
	/**
	 * 停止充电电桩应答
	 * @param token
	 * @param orgNo
	 * @param userIdentity
	 * @param epCode
	 * @param epGunNo
	 * @param extra
	 * @param ret
	 * @param errorCode
	 */
	public void onStopChargeEpResp(String token,int orgNo,String userIdentity,String epCode,int epGunNo, String extra,int ret,int errorCode);
	

	/**
	 * 充电事件
	 * @param orgNo
	 * @param userIdentity
	 * @param epCode
	 * @param epGunNo
	 * @param extra
	 * @param ret
	 * @param errorCode
	 */
	public void onChargeEvent(int orgNo,String userIdentity,String epCode,int epGunNo, String extra,int ret,int errorCode);

	public void onEpStatusChange(String token,int orgNo,String userIdentity,String epCode,int epGunNo
			,int inter_type,Map<String ,Object> realData,String extra);

	public void onEpNetStatusChange(int orgNo,String epCode,int netStatus);
	
	public void onRealData(String token,int orgNo,String userIdentity,String epCode,int epGunNo,int inter_type,float servicePrice,Map<String ,Object> realData,String extra);
	
	public void onChargeOrder(String token,int orgNo,String userIdentity,String epCode,int epGunNo
			,int inter_type,float money,float elect_money,float service_money,float elect,float start_elect,float end_elect
			,float cusp_elect,float cusp_elect_price,float cusp_service_price,float cusp_money,float cusp_elect_money,float cusp_service_money
			,float peak_elect,float peak_elect_price,float peak_service_price,float peak_money,float peak_elect_money,float peak_service_money
			,float flat_elect,float flat_elect_price,float flat_service_price,float flat_money,float flat_elect_money,float flat_service_money
			,float valley_elect,float valley_elect_price,float valley_service_price,float valley_money,float valley_elect_money,float valley_service_money
			,int start_time,int end_time,int stop_model,int stop_reason,int soc,int time,String extra);
}
