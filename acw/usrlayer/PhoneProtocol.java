package com.epcentre.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.constant.EpProtocolConstant;
import com.epcentre.service.ChargingInfo;



public class PhoneProtocol{
	
	private static final Logger logger = LoggerFactory.getLogger("PhoneLog");
	public static  byte[] Package(byte cos,short cmdtype) {
		
		try
		{
		PhoneHeader Header = new PhoneHeader();
		
		Header.setLength(3);

		ByteArrayOutputStream bmsg = new ByteArrayOutputStream( PhoneConstant.PHONE_SENDBUFFER);
		
		bmsg.write(Header.toByteArray());
		
		bmsg.write(cos);
		
		byte cmdtypeL = (byte)(cmdtype&0x00ff);		
		bmsg.write(cmdtypeL);
		
		byte cmdtypeH = (byte)((cmdtype>>8)&0x00ff);
		bmsg.write(cmdtypeH);
		
		return bmsg.toByteArray();
		}
		catch (IOException e) 
		{
			logger.error("Package1 exception,e.StackTrace:{}",e.getStackTrace());
			return null;
		}
	}

	public static  byte[] Package(byte[] data,byte cos,short cmdtype) 
	{
		
		try
		{
		PhoneHeader Header = new PhoneHeader();
		
		Header.setLength(3 + data.length);

		ByteArrayOutputStream bmsg = new ByteArrayOutputStream( PhoneConstant.PHONE_SENDBUFFER);
		
		bmsg.write(Header.toByteArray());
		
		bmsg.write(cos);
		
		byte cmdtypeL = (byte)(cmdtype&0x00ff);		
		bmsg.write(cmdtypeL);
		
		byte cmdtypeH = (byte)((cmdtype>>8)&0x00ff);
		bmsg.write(cmdtypeH);
		
		bmsg.write(data);
		
		return bmsg.toByteArray();
		}
		catch (IOException e) 
		{
			logger.error("Package2 exception,e.StackTrace:{}",e.getStackTrace());
			return null;
		}
	}

    public static byte[] do_confirm(short cmdtype,byte successflag,short errorcode){
		try
		{
			logger.error("do_confirm,cmdtype:{},successflag:{},errorcode:{}", new Object[]{cmdtype,successflag,errorcode});
			ByteArrayOutputStream bout = new ByteArrayOutputStream(PhoneConstant.PHONE_SENDBUFFER);
			
			//成功标识 BIN码 1Byte  1:成功0:失败
			bout.write(successflag);
			//成功标识 BIN码 1Byte  1:成功0:失败
			bout.write(WmIce104Util.short2Bytes(errorcode));
			
			
			return Package(bout.toByteArray(),(byte)1, cmdtype);
		}
		catch (IOException e) 
		{
			logger.error("do_confirm exception,e.StackTrace:{}",e.getStackTrace());
			return null;
		}
		
	}
    
    public static byte[] do_connect_ep_resp(short cmdtype,byte successflag,short errorcode,short pos, short currentType) {
		try
		{
			
			ByteArrayOutputStream bout = new ByteArrayOutputStream(PhoneConstant.PHONE_SENDBUFFER);
			
			//成功标识 BIN码 1Byte  1:成功0:失败
			bout.write(successflag);
			//2	错误编码	BIN码	2Byte	参见附录一	小端字节序
			bout.write(WmIce104Util.short2Bytes(errorcode));
			
			bout.write((byte)pos);
			
			if(cmdtype != 1)//新版本加交直流类型
			{
				bout.write((byte)currentType);
			}
			
			
			return Package(bout.toByteArray(),(byte)1, cmdtype);
		}
		catch(IOException e)
		{
			logger.error("do_connect_ep_resp exception,e.StackTrace:{}",e.getStackTrace());
			return null;
		}
		
	}
	
    public static  byte[] do_consume_record(short version,String chargeOrder,int st,int et,int totalMeterNum,int totalAmt,int serviceAmt,
    		int pkEpId,int userFirst,int CouPonAmt,int realCouPonAmt) {
		
		try
		{
		
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream(PhoneConstant.PHONE_SENDBUFFER);
		//1.订单编号
		bout.write(chargeOrder.getBytes());
		//2	开始时间
		bout.write(WmIce104Util.int2Bytes(st));
		//3.结束时间
		bout.write(WmIce104Util.int2Bytes(et));
		//4总度量
		bout.write(WmIce104Util.int2Bytes(totalMeterNum));
		//5总金额
		bout.write(WmIce104Util.int2Bytes(totalAmt));
		//6服务费
		bout.write(WmIce104Util.int2Bytes(serviceAmt));
		
		//7服务费
		bout.write(WmIce104Util.int2Bytes(pkEpId));
		if(version>=2)
		{
			//是否首次体验
			bout.write((byte)userFirst);
			//优惠券面额
			bout.write(WmIce104Util.int2Bytes(CouPonAmt));
			//优惠券抵扣金额
			bout.write(WmIce104Util.int2Bytes(realCouPonAmt));
		}
		
	
		return Package(bout.toByteArray(),(byte)1,PhoneConstant.D_CONSUME_RECORD);
		}
		catch(IOException e)
		{
			logger.error("do_consume_record exception,e.StackTrace:{}",e.getStackTrace());
			return null;
		}
	}
    public static  byte[] do_start_charge_event(int state) 
	{
		byte[] value = new byte[1];
		value[0] = (byte)state;
		return Package(value,(byte)0,PhoneConstant.D_START_CHARGE_EVENT);
		
	}
    
    
    public static  byte[] do_real_charge_info(ChargingInfo chargingInfo) {
		try
		{
	
			ByteArrayOutputStream bout = new ByteArrayOutputStream(PhoneConstant.PHONE_SENDBUFFER);
		
			bout.write((byte)chargingInfo.getWorkStatus());
		
			bout.write(WmIce104Util.short2Bytes((short)chargingInfo.getTotalTime()));
			
			//System.out.print("getOutVol:"+chargingInfo.getOutVol()+"\n");
			
			bout.write(WmIce104Util.short2Bytes((short)chargingInfo.getOutVol()));
			logger.debug("send phone chargingInfo.getOutVol():{}",chargingInfo.getOutVol());
			bout.write(WmIce104Util.short2Bytes((short)chargingInfo.getOutCurrent()));
			logger.debug("send phone chargingInfo.getOutCurrent():{}",chargingInfo.getOutCurrent());
			
			//电桩传上来的值是精确到三位小数，1000倍，手机需要精确到小数点后两位，倍数100倍
			
			int meterNum = chargingInfo.getChargeMeterNum()/10;
			bout.write(WmIce104Util.int2Bytes(meterNum));
			logger.debug("send phone getChargeMeterNum:{}",meterNum);
			int price = chargingInfo.getRateInfo();
			logger.debug("send phone price:{}",price);
			bout.write(WmIce104Util.short2Bytes((short)price));
			
			bout.write(WmIce104Util.int2Bytes(chargingInfo.getFronzeAmt()));
			
			bout.write(WmIce104Util.int2Bytes(chargingInfo.getChargeAmt()));
			logger.debug("send phone chargingInfo.getChargeAmt():{}",chargingInfo.getChargeAmt());
			
			bout.write((byte)chargingInfo.getSoc());
			logger.debug("send phone soc:{}",chargingInfo.getSoc());
			bout.write(WmIce104Util.int2Bytes(chargingInfo.getDeviceStatus()));
			bout.write(WmIce104Util.int2Bytes(chargingInfo.getWarns()));
		
			return Package(bout.toByteArray(),(byte)0,PhoneConstant.D_CHARGING_INFO);
		}
		catch(IOException e)
		{
			logger.error("do_real_charge_info exception,e.StackTrace:{}",e.getStackTrace());
			return null;
		}

	}
	
	
    public static  byte[] do_ep_net_status(int epNetStatus) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(PhoneConstant.PHONE_SENDBUFFER);

		bout.write((byte)epNetStatus);

		return Package(bout.toByteArray(),(byte)0,PhoneConstant.D_EP_NET_STATUS);

	}
    
    public static  byte[] do_gun2car_linkstatus(int status,byte[] time) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(PhoneConstant.PHONE_SENDBUFFER);

		try {
			bout.write((byte)status);
			bout.write(time);
			return Package(bout.toByteArray(),(byte)0,PhoneConstant.D_GUN_CAR_STATUS);
		} catch (Exception e) {
			logger.error("do_gun2car_linkstatus error!e.getMessage():{}",e.getMessage());
			return null;
		}
	}		
}

