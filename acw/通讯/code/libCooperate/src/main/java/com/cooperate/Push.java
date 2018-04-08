package com.cooperate;

public abstract class Push implements IPush{
    protected String appId = "";
    protected String appKey = "";
    protected String appsecret = "";
    
    protected String chargeRespUrl = "";
    protected String stopchargeRespUrl = "";
    protected String chargeEventUrl = "";
    protected String statusChangeUrl = "";
    protected String netstatusChangeUrl = "";
    protected String realDataUrl = "";
    protected String orderUrl = "";
    protected String tokenUrl = "";
	
	protected int orgNo;
	protected int mode=0;//1:只发充电时实时数据，2:充电和空闲时都发实时数据
	protected long period;
	
	public Push()
	{
		this.mode = 1;
	}

	public String getAppId() {
		return appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public String getAppsecret() {
		return appsecret;
	}

	public String getChargeRespUrl() {
		return chargeRespUrl;
	}

	public String getStopchargeRespUrl() {
		return stopchargeRespUrl;
	}

	public String getChargeEventUrl() {
		return chargeEventUrl;
	}

	public String getStatusChangeUrl() {
		return statusChangeUrl;
	}

	public void setStatusChangeUrl(String statusChangeUrl) {
		this.statusChangeUrl = statusChangeUrl;
	}

	public String getNetstatusChangeUrl() {
		return netstatusChangeUrl;
	}

	public void setNetstatusChangeUrl(String netstatusChangeUrl) {
		this.netstatusChangeUrl = netstatusChangeUrl;
	}

	public String getRealDataUrl() {
		return realDataUrl;
	}

	public String getOrderUrl() {
		return orderUrl;
	}
	
	public String getTokenUrl() {
		return tokenUrl;
	}

	public int getOrgNo() {
		return orgNo;
	}

	public int getMode() {
		return mode;
	}

	public long getPeriod() {
		return period;
	}
}
