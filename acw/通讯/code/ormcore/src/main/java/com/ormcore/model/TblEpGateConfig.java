package com.ormcore.model;

public class TblEpGateConfig  {
	
	
	
	private java.lang.Integer pkGateid; // 
	private java.lang.String gtseGatename; // Gate服务器名称
	private java.lang.String gtseGateip; // Gate服务器Ip
	private java.lang.Integer gtseGateport; // Gate服务器端口
	private java.util.Date gtseCreatetime; // 创建时间
	private java.util.Date gtseUpdatetime; // 修改时间
	private java.lang.Integer  gateState;//GATE服务器状态 1-正常 2-移除
	private java.lang.Integer  failTimes;//GATE服务器重连失败次数
	
	/**
     * 获取属性
     *
     * @return pkGateid
     */
	public java.lang.Integer getPkGateid() {
		return pkGateid;
	}
	
	/**
	 * 设置属性
	 *
	 * @param pkGateid
	 */
	public void setPkGateid(java.lang.Integer pkGateid) {
		this.pkGateid = pkGateid;
	}
	
	/**
     * 获取Gate服务器名称属性
     *
     * @return gtseGatename
     */
	public java.lang.String getGtseGatename() {
		return gtseGatename;
	}
	
	/**
	 * 设置Gate服务器名称属性
	 *
	 * @param gtseGatename
	 */
	public void setGtseGatename(java.lang.String gtseGatename) {
		this.gtseGatename = gtseGatename;
	}
	
	/**
     * 获取Gate服务器Ip属性
     *
     * @return gtseGateip
     */
	public java.lang.String getGtseGateip() {
		return gtseGateip;
	}
	
	/**
	 * 设置Gate服务器Ip属性
	 *
	 * @param gtseGateip
	 */
	public void setGtseGateip(java.lang.String gtseGateip) {
		this.gtseGateip = gtseGateip;
	}
	
	/**
     * 获取Gate服务器端口属性
     *
     * @return gtseGateport
     */
	public java.lang.Integer getGtseGateport() {
		return gtseGateport;
	}
	
	/**
	 * 设置Gate服务器端口属性
	 *
	 * @param gtseGateport
	 */
	public void setGtseGateport(java.lang.Integer gtseGateport) {
		this.gtseGateport = gtseGateport;
	}
	
	/**
     * 获取创建时间属性
     *
     * @return gtseCreatetime
     */
	public java.util.Date getGtseCreatetime() {
		return gtseCreatetime;
	}
	
	/**
	 * 设置创建时间属性
	 *
	 * @param gtseCreatetime
	 */
	public void setGtseCreatetime(java.util.Date gtseCreatetime) {
		this.gtseCreatetime = gtseCreatetime;
	}
	
	/**
     * 获取修改时间属性
     *
     * @return gtseUpdatetime
     */
	public java.util.Date getGtseUpdatetime() {
		return gtseUpdatetime;
	}
	
	/**
	 * 设置修改时间属性
	 *
	 * @param gtseUpdatetime
	 */
	public void setGtseUpdatetime(java.util.Date gtseUpdatetime) {
		this.gtseUpdatetime = gtseUpdatetime;
	}
	

	public java.lang.Integer getGateState() {
		return gateState;
	}

	public void setGateState(java.lang.Integer gateState) {
		this.gateState = gateState;
	}

	public java.lang.Integer getFailTimes() {
		return failTimes;
	}

	public void setFailTimes(java.lang.Integer failTimes) {
		this.failTimes = failTimes;
	}

}