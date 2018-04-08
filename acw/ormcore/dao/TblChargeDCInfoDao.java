package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblChargeDCInfo;

public interface TblChargeDCInfoDao {
	 
	public List<TblChargeDCInfo> findChargeInfo(TblChargeDCInfo info);
		
	public int insert(TblChargeDCInfo info);
		
	public int update(TblChargeDCInfo info);
	
	public int updateStartChargeInfo(TblChargeDCInfo info);
	
}
