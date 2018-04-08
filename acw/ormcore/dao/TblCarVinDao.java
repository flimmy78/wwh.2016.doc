package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.RateInfo;
import com.epcentre.model.TblCarVin;


public interface TblCarVinDao {

	/**
	 * 查找vin对应的记录
	 * @param code
	 * @return
	 */
	public List<TblCarVin> selectByCode(String vinCode);
	public List<TblCarVin> selectById(int vinId);

	
}
