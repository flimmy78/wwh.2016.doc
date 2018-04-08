package com.epcentre.dao;

import java.util.List;
import java.util.Map;

import com.epcentre.model.TblChargingrecord;

/**
 * @Description: 充电记录操作DAO层接口Maper
 * @author songjf
 * @createTime：2015-4-10 下午01:58:51
 * @updator：
 * @updateTime：
 * @version：V1.0
 */
public interface ChargingrecordDao {

	/**
	 * @Title: getByTranNumber
	 * @Description: (根据流水号获取充电记录信息)
	 * @param params
	 * @return
	 */
	public List<TblChargingrecord> getByTranNumber(
			java.lang.String chreTransactionnumber);
	
	/**
	 * @Title: selectByChreCode
	 * @Description: (根据充电订单获取充电记录)
	 * @param params
	 * @return 
	 */
	public TblChargingrecord selectByChreCode(
			java.lang.String pkChargingOrder);

	public <K, V> Map<K, V> findOne(java.lang.Integer pkChargingrecord);

	public <T, K, V> List<T> find(Map<K, V> params);
	
	public List<TblChargingrecord> getUnFinishedCharge(TblChargingrecord tblChargingrecord);
	
	public List<TblChargingrecord> getUnFinishedChargeByUsrId(TblChargingrecord tblChargingrecord);
	
	
	/**
	 * @Title: insertBeginRecord
	 * @Description: 新增开始充电记录
	 * @param params
	 * @return
	 */
	public int insertBeginRecord(TblChargingrecord tblChargingrecord);
	
	/**
	 * @Title: insertBeginRecord
	 * @Description: 新增开始充电记录
	 * @param params
	 * @return
	 */
	public int insertFullChargeRecord(TblChargingrecord tblChargingrecord);

	/**
	 * @Title: insertEndRecord
	 * @Description: 新增结束充电记录 根据流水号更新数据
	 * @param params
	 * @return
	 */
	public int insertEndRecord(TblChargingrecord tblChargingrecord);

	/**
	 * @Title: updatechReCode
	 * @Description: 更新充电订单编号
	 * @param params
	 * @return
	 */
	public int updatechReCode(TblChargingrecord tblChargingrecord);

	public int delete(java.lang.Integer pkChargingrecord);
	
	public int ischarging(int userId);
	
	public int deleteBeginRecord(java.lang.String chreTransactionnumber);
	
	public int updateBeginRecord(TblChargingrecord tblChargingrecord);
	
	public int updateFailChargeRecord(TblChargingrecord tblChargingrecord);
	
	public int updateBeginRecordStatus(TblChargingrecord tblChargingrecord);
	public int  updateThird(TblChargingrecord tblChargingrecord);
}
