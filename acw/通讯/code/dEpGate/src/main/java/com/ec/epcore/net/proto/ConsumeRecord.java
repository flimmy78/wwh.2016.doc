package com.ec.epcore.net.proto;

import java.math.BigDecimal;


public class ConsumeRecord {
	
	private String epCode;
	private int epGunNo;
	
	private String serialNo;
	private int accountType;
	private String epUserAccount;
	
	private int transType;
	
	private long startTime;
	
	private long endTime;
	
	private int jqValue;
	
	
	public int jzValue;
	
	
	private int fqValue;
	
	
	private int fzValue;
	
	
	
	private int pqValue;
	
	
	
	private int pzValue;
	
	
	
	private int gqValue;
	

	
	private int gzValue;
	
	
	private byte cdStyle;
	
	
	private  int curDBValue;
	
	
	private  int prevDBValue;
	
	
	
	private int jPrice;

	
	private int jDl;
	
	
	private int jAmt;
	
	
	private int fPrice;
	
	
	private int fDl;
	
	
	private int fAmt;
	
	
	private int pPrice;

	
	private int pDl;
	
	
	private int pAmt;
	
	
	private int gPrice;
	
	private int gDl;
	
	private int gAmt;
	
	private int totalDl;
	
	private String ywType;
	
	private int consumeValue;	
	
	private int consumePrice;
	
	private int stopCause;
	
	private int startSoc;

	private int endSoc;

	private int startMeterNum;
	private int totalChargeAmt;
	private int totalAmt;//总费用
	
	private int serviceAmt;
	
	private int userOrgin;
	
	private int chargeRemainAmt;
	
	private int chargeUseTimes;//充电
	
	private String carVinCode;
	//=======================以下为结算相关
	private int discountIdentity;//打折标识
	private BigDecimal  discountServicePrice;//打折服务费单价
	private int  discountServiceAmt;//打折服务费单价
	private int couponAmt ; //优惠券金额
	private int realCouponAmt;
	private int undiscountTotalAmt;
	
	private int type;

	
	public ConsumeRecord()
	{
		bespokeNo="";
		stopCause=0;
		userOrgin=0;
		
		carVinCode="";
		discountIdentity=0;
		discountServicePrice= new BigDecimal(0.0);
		couponAmt = 0; //优惠券金额
		realCouponAmt=0;
		undiscountTotalAmt=0;
		discountServiceAmt = 0;
		type = 0;
	}
	
	public int getAccountType() {
		return accountType;
	}


	public void setAccountType(int accountType) {
		this.accountType = accountType;
	}

	private String bespokeNo;
	

	public String getEpCode() {
		return epCode;
	}


	public void setEpCode(String epCode) {
		this.epCode = epCode;
	}


	public int getEpGunNo() {
		return epGunNo;
	}


	public void setEpGunNo(int epGunNo) {
		this.epGunNo = epGunNo;
	}


	public int getTotalAmt() {
		return totalAmt;
	}


	public void setTotalAmt(int totalAmt) {
		this.totalAmt = totalAmt;
	}


	public int getStartMeterNum() {
		return startMeterNum;
	}


	public void setStartMeterNum(int startMeterNum) {
		this.startMeterNum = startMeterNum;
	}


	public int getEndMeterNum() {
		return endMeterNum;
	}


	public void setEndMeterNum(int endMeterNum) {
		this.endMeterNum = endMeterNum;
	}

	private int endMeterNum;
	
	

	
	
	public int getUserOrgin() {
		return userOrgin;
	}


	public void setUserOrgin(int userOrgin) {
		this.userOrgin = userOrgin;
	}


	public int getStopCause() {
		return stopCause;
	}

	public void setStopCause(int stopCause) {
		this.stopCause = stopCause;
	}

	public int getStartSoc() {
		return startSoc;
	}

	public void setStartSoc(int startSoc) {
		this.startSoc = startSoc;
	}

	public int getEndSoc() {
		return endSoc;
	}

	public void setEndSoc(int endSoc) {
		this.endSoc = endSoc;
	}

	public String getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	public String getEpUserAccount() {
		return epUserAccount;
	}

	public void setEpUserAccount(String epUserAccount) {
		this.epUserAccount = epUserAccount;
	}

	public int getTransType() {
		return transType;
	}

	public void setTransType(int transType) {
		this.transType = transType;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public int getJqValue() {
		return jqValue;
	}

	public void setJqValue(int jqValue) {
		this.jqValue = jqValue;
	}

	public int getJzValue() {
		return jzValue;
	}

	public void setJzValue(int jzValue) {
		this.jzValue = jzValue;
	}

	public int getFqValue() {
		return fqValue;
	}

	public void setFqValue(int fqValue) {
		this.fqValue = fqValue;
	}

	public int getFzValue() {
		return fzValue;
	}

	public void setFzValue(int fzValue) {
		this.fzValue = fzValue;
	}

	public int getPqValue() {
		return pqValue;
	}

	public void setPqValue(int pqValue) {
		this.pqValue = pqValue;
	}

	public int getPzValue() {
		return pzValue;
	}

	public void setPzValue(int pzValue) {
		this.pzValue = pzValue;
	}

	public int getGqValue() {
		return gqValue;
	}

	public void setGqValue(int gqValue) {
		this.gqValue = gqValue;
	}

	public int getGzValue() {
		return gzValue;
	}

	public void setGzValue(int gzValue) {
		this.gzValue = gzValue;
	}

	public byte getCdStyle() {
		return cdStyle;
	}

	public void setCdStyle(byte cdStyle) {
		this.cdStyle = cdStyle;
	}

	public int getCurDBValue() {
		return curDBValue;
	}

	public void setCurDBValue(int curDBValue) {
		this.curDBValue = curDBValue;
	}

	public int getPrevDBValue() {
		return prevDBValue;
	}

	public void setPrevDBValue(int prevDBValue) {
		this.prevDBValue = prevDBValue;
	}

	public int getjPrice() {
		return jPrice;
	}

	public void setjPrice(int jPrice) {
		this.jPrice = jPrice;
	}

	public int getjDl() {
		return jDl;
	}

	public void setjDl(int jDl) {
		this.jDl = jDl;
	}

	public int getjAmt() {
		return jAmt;
	}

	public void setjAmt(int jAmt) {
		this.jAmt = jAmt;
	}

	public int getfPrice() {
		return fPrice;
	}

	public void setfPrice(int fPrice) {
		this.fPrice = fPrice;
	}

	public int getfDl() {
		return fDl;
	}

	public void setfDl(int fDl) {
		this.fDl = fDl;
	}

	public int getfAmt() {
		return fAmt;
	}

	public void setfAmt(int fAmt) {
		this.fAmt = fAmt;
	}

	public int getpPrice() {
		return pPrice;
	}

	public void setpPrice(int pPrice) {
		this.pPrice = pPrice;
	}

	public int getpDl() {
		return pDl;
	}

	public void setpDl(int pDl) {
		this.pDl = pDl;
	}

	public int getpAmt() {
		return pAmt;
	}

	public void setpAmt(int pAmt) {
		this.pAmt = pAmt;
	}

	public int getgPrice() {
		return gPrice;
	}

	public void setgPrice(int gPrice) {
		this.gPrice = gPrice;
	}

	public int getgDl() {
		return gDl;
	}

	public void setgDl(int gDl) {
		this.gDl = gDl;
	}

	public int getgAmt() {
		return gAmt;
	}

	public void setgAmt(int gAmt) {
		this.gAmt = gAmt;
	}

	public int getTotalDl() {
		return totalDl;
	}

	public void setTotalDl(int totalDl) {
		this.totalDl = totalDl;
	}

	public String getYwType() {
		return ywType;
	}

	public void setYwType(String ywType) {
		this.ywType = ywType;
	}

	public int getConsumeValue() {
		return consumeValue;
	}

	public void setConsumeValue(int consumeValue) {
		this.consumeValue = consumeValue;
	}

	public int getConsumePrice() {
		return consumePrice;
	}

	public void setConsumePrice(int consumePrice) {
		this.consumePrice = consumePrice;
	}

	
	
	
	
	
	
	public int getUndiscountTotalAmt() {
		return undiscountTotalAmt;
	}

	public void setUndiscountTotalAmt(int undiscountTotalAmt) {
		this.undiscountTotalAmt = undiscountTotalAmt;
	}

	public String getBespokeNo() {
		return bespokeNo;
	}


	public void setBespokeNo(String bespokeNo) {
		this.bespokeNo = bespokeNo;
	}

	

	public int getServiceAmt() {
		return serviceAmt;
	}


	public void setServiceAmt(int serviceAmt) {
		this.serviceAmt = serviceAmt;
	}
	
	

	


	/*public int getStatAmtRet() {
		return statAmtRet;
	}


	public void setStatAmtRet(int statAmtRet) {
		this.statAmtRet = statAmtRet;
	}*/


	public int getChargeRemainAmt() {
		return chargeRemainAmt;
	}


	public void setChargeRemainAmt(int chargeRemainAmt) {
		this.chargeRemainAmt = chargeRemainAmt;
	}
	

	

	/*public int getStatRet() {
		return statRet;
	}


	public void setStatRet(int statRet) {
		this.statRet = statRet;
	}*/


	public int getChargeUseTimes() {
		return chargeUseTimes;
	}


	public void setChargeUseTimes(int chargeUseTimes) {
		this.chargeUseTimes = chargeUseTimes;
	}
	

	public int getTotalChargeAmt() {
		return totalChargeAmt;
	}


	public void setTotalChargeAmt(int totalChargeAmt) {
		this.totalChargeAmt = totalChargeAmt;
	}

	

	public String getCarVinCode() {
		return carVinCode;
	}

	public void setCarVinCode(String carVinCode) {
		this.carVinCode = carVinCode;
	}

	
	
	public int getDiscountIdentity() {
		return discountIdentity;
	}

	public void setDiscountIdentity(int discountIdentity) {
		this.discountIdentity = discountIdentity;
	}
	

	public BigDecimal getDiscountServicePrice() {
		return discountServicePrice;
	}

	public void setDiscountServicePrice(BigDecimal discountServicePrice) {
		this.discountServicePrice = discountServicePrice;
	}
	
	

	public int getCouponAmt() {
		return couponAmt;
	}

	public void setCouponAmt(int couponAmt) {
		this.couponAmt = couponAmt;
	}

	public int getRealCouponAmt() {
		return realCouponAmt;
	}

	public void setRealCouponAmt(int realCouponAmt) {
		this.realCouponAmt = realCouponAmt;
	}

	public int getDiscountServiceAmt() {
		return discountServiceAmt;
	}

	public void setDiscountServiceAmt(int discountServiceAmt) {
		this.discountServiceAmt = discountServiceAmt;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		
		final StringBuilder sb = new StringBuilder();
        sb.append("NoCardConsumeRecord:\n");
        
        sb.append("{epCode=").append(epCode).append("}\n");
        sb.append("{epGunNo=").append(epGunNo).append("}\n");
        
        sb.append("{serialNo=").append(serialNo).append("}\n");
        
        sb.append("{accountType=").append(accountType).append("}\n");
        
        sb.append("{epUserAccount=").append(epUserAccount).append("}\n");
        sb.append("{transType=").append(transType).append("}\n");
    	
        sb.append("{startTime=").append(startTime).append("}\n");
    	
        sb.append("{endTime=").append(endTime).append("}\n");
        
        sb.append("{jDl=").append(jDl).append("}\n");
        sb.append("{jAmt=").append(jAmt).append("}\n");
        
        sb.append("{fDl=").append(fDl).append("}\n");
        sb.append("{fAmt=").append(fAmt).append("}\n");
        
        sb.append("{pDl=").append(pDl).append("}\n");
        sb.append("{pAmt=").append(pAmt).append("}\n");
        
        sb.append("{gDl=").append(gDl).append("}\n");
        sb.append("{gAmt=").append(gAmt).append("}\n");
        /////
        
        sb.append("{totalDl=").append(totalDl).append("}\n");
        
        sb.append("{totalChargeAmt=").append(totalChargeAmt).append("}\n");  
        sb.append("{serviceAmt=").append(serviceAmt).append("}\n");
        
        sb.append("{totalAmt=").append(totalAmt).append("}\n");
        sb.append("{startMeterNum=").append(startMeterNum).append("}\n");
        
        sb.append("{endMeterNum=").append(endMeterNum).append("}\n");
        sb.append("{stopCause=").append(stopCause).append("}\n");
        sb.append("{startSoc=").append(startSoc).append("}\n");
        sb.append("{endSoc=").append(endSoc).append("}\n");
        
        sb.append("{userOrgin=").append(userOrgin).append("}\n");
       
		return sb.toString();
	}
	

}
