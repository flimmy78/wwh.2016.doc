package com.epcentre.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.epcentre.config.Global;
import com.epcentre.protocol.TimeStage;
import com.epcentre.protocol.WmIce104Util;
import com.epcentre.server.AnalyzeMessageHandler;


/**
 * 
 * tbl_RateInformation表
 * @author mew
 *
 */
public class RateInfo  implements Serializable{
	private java.lang.Integer id; // 计费模型ID
	
	private java.math.BigDecimal freezingMoney; // 预冻结金额
	private java.math.BigDecimal minFreezingMoney; // 最小冻结金额
	private java.lang.String quantumDate; // 采用JSON形式存储实际格式
	private java.math.BigDecimal j_Rate; // 尖时段电价
	private java.math.BigDecimal f_Rate; // 峰时段电价
	private java.math.BigDecimal p_Rate; // 平时段电价
	private java.math.BigDecimal g_Rate; // 谷时段电价
	
	private java.math.BigDecimal bespokeRate; // 预约费率
	private java.math.BigDecimal ServiceRate; // 服务费
	
	private java.math.BigDecimal WarnAmt; //
	
	private ArrayList<TimeStage> timeStageList = null;//new ArrayList<TimeStage>();
	
	private static final Logger logger = LoggerFactory.getLogger(RateInfo.class);
	
	public RateInfo()
	{
		ServiceRate= new BigDecimal(0.0);
	}
	private byte[] comm_data;

	public byte[] getComm_data() {
		return comm_data;
	}
	
	public java.math.BigDecimal getWarnAmt() {
		return WarnAmt;
	}

	public void setWarnAmt(java.math.BigDecimal warnAmt) {
		WarnAmt = warnAmt;
	}



	public java.lang.Integer getId() {
		return id;
	}



	public void setId(java.lang.Integer id) {
		this.id = id;
	}



	



	public java.math.BigDecimal getFreezingMoney() {
		return freezingMoney;
	}



	public void setFreezingMoney(java.math.BigDecimal freezingMoney) {
		this.freezingMoney = freezingMoney;
	}



	public java.math.BigDecimal getMinFreezingMoney() {
		return minFreezingMoney;
	}



	public void setMinFreezingMoney(java.math.BigDecimal minFreezingMoney) {
		this.minFreezingMoney = minFreezingMoney;
	}



	public java.lang.String getQuantumDate() {
		return quantumDate;
	}



	public void setQuantumDate(java.lang.String quantumDate) {
		this.quantumDate = quantumDate;
	}



	public java.math.BigDecimal getJ_Rate() {
		return j_Rate;
	}



	public void setJ_Rate(java.math.BigDecimal j_Rate) {
		this.j_Rate = j_Rate;
	}



	public java.math.BigDecimal getF_Rate() {
		return f_Rate;
	}



	public void setF_Rate(java.math.BigDecimal f_Rate) {
		this.f_Rate = f_Rate;
	}



	public java.math.BigDecimal getP_Rate() {
		return p_Rate;
	}



	public void setP_Rate(java.math.BigDecimal p_Rate) {
		this.p_Rate = p_Rate;
	}

	public java.math.BigDecimal getBespokeRate() {
		return bespokeRate;
	}



	public void setBespokeRate(java.math.BigDecimal bespokeRate) {
		this.bespokeRate = bespokeRate;
	}

	public ArrayList<TimeStage> getTimeStageList() {
		return timeStageList;
	}


	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Rateinfo:\n");
        sb.append("{费率ID=").append(id).append("}\n");
       
        sb.append(",{预冻结金额=").append(freezingMoney).append("}\n");
        sb.append(",{最小冻结金额=").append(minFreezingMoney).append("}\n");
        sb.append(",{ Quantumdate=").append(quantumDate).append("}\n");
        sb.append(",{尖时段电价=").append(j_Rate).append("}\n");
        sb.append(",{峰时段电价=").append(f_Rate).append("}\n");
        sb.append(",{平时段电价=").append(p_Rate).append("}\n");
        sb.append(",{谷时段电价=").append(g_Rate).append("}\n");
        sb.append(",{预约费率=").append(bespokeRate).append("}\n");
        sb.append(",{服务费=").append(ServiceRate);
		sb.append('}');
        return sb.toString();
    }



	public java.math.BigDecimal getG_Rate() {
		return g_Rate;
	}



	public void setG_Rate(java.math.BigDecimal g_Rate) {
		this.g_Rate = g_Rate;
	}



	public java.math.BigDecimal getServiceRate() {
		return ServiceRate;
	}



	public void setServiceRate(java.math.BigDecimal serviceRate) {
		ServiceRate = serviceRate;
	}
	public ArrayList<TimeStage> jParseStage(JSONArray ja)
	{
        if(ja==null){
        	return null;
        }
        if(ja.size()<1){
        	return null;
        }
        
        ArrayList<TimeStage> timeStageList= new ArrayList<TimeStage>();
        // 循环添加Employee对象（可能有多个）
        for (int i = 0; i < ja.size(); i++) {
        	JSONObject jStage = ja.getJSONObject(i);
        	if(jStage!=null){
        		
        		TimeStage timestage = new TimeStage();
	        	timestage.setStartTime(jStage.getInt("st"));
	        	timestage.setEndTime(jStage.getInt("et"));
	        	timestage.setFlag(jStage.getInt("mark"));
	        	timeStageList.add(timestage);
        	}
        }
        if(timeStageList.size()<1){
        	return null;
        }
        
        return timeStageList;
	}
	public boolean parseStage()
	{
		try {
			
			JSONObject jb = JSONObject.fromObject(getQuantumDate());
		    JSONArray ja=jb.getJSONArray("data");
		   
		    timeStageList  = jParseStage(ja);
		    if(timeStageList ==null)
		    {
		    	return false;
		    }
		    
			ByteArrayOutputStream bout = new ByteArrayOutputStream(256);
			
			bout.write(WmIce104Util.int2Bytes(getId()));
			//3	生效日期	BIN码	7Byte	年月日
			byte[] date= new byte[]{0x0,0x0,0x0,0x0,0x0,0x0,0x0};
			bout.write(date);
			
			//2015年1月29日
			//4	失效日期	BIN码	7Byte
			bout.write(date);
			
			byte[] bPrepareFrozenAmt = WmIce104Util.int2Bytes((int) (getFreezingMoney().multiply(Global.DecTime2).doubleValue()));
			
			bout.write(bPrepareFrozenAmt);
			
			byte[] bMinFrozenAmt = WmIce104Util.int2Bytes((int) (getMinFreezingMoney().multiply(Global.DecTime2).doubleValue()));
			bout.write(bMinFrozenAmt);
			int nTimeState = timeStageList.size();
			byte bTimeStage = (byte)timeStageList.size();
			bout.write(bTimeStage);
			for(int i=0;i< nTimeState; i++)
			{
				TimeStage ts = timeStageList.get(i);
				bout.write(ts.toByteArray());
			}
			
			byte[] b_j_rate = WmIce104Util.int2Bytes((int) (getJ_Rate().multiply(Global.DecTime3).doubleValue()));
			bout.write(b_j_rate);
			
			byte[] b_f_rate = WmIce104Util.int2Bytes((int) (getF_Rate().multiply(Global.DecTime3).doubleValue()));
			bout.write(b_f_rate);
			
			byte[] b_p_rate = WmIce104Util.int2Bytes((int) (getP_Rate().multiply(Global.DecTime3).doubleValue()));
			bout.write(b_p_rate);
			
			byte[] b_g_rate = WmIce104Util.int2Bytes((int) (getG_Rate().multiply(Global.DecTime3).doubleValue()));
			bout.write(b_g_rate);
			
			
			byte[] b_ordering_rate = WmIce104Util.int2Bytes((int) (getBespokeRate().multiply(Global.DecTime3).doubleValue()));
			bout.write(b_ordering_rate);
			
			byte[] b_service_rate = WmIce104Util.int2Bytes((int) (getServiceRate().multiply(Global.DecTime3).doubleValue()));
			bout.write(b_service_rate);
			
			byte[] b_warn_amt = WmIce104Util.int2Bytes((int) (getWarnAmt().multiply(Global.DecTime3).doubleValue()));
			bout.write(b_warn_amt);
			
			comm_data = bout.toByteArray();
			
			return true;
		} catch (IOException e) {
			comm_data =null;
			logger.error("parseStage exception,e.getStackTrace:{}",e.getMessage());
			return false;
		}
		
	}
}