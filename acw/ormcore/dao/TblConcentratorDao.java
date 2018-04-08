package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblConcentrator;


public interface TblConcentratorDao {
	
	public int findStation(int id);
	
	
	public List<TblConcentrator> findResultObjectBySpanId(int typeId);
	

}
