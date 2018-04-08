package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblBomList;


public interface BomListDao {

	public List<TblBomList> getAll();
	
	public List<TblBomList> getAllByTypeSpanId(int Id);
	
	public List<TblBomList> getAllByBomListId(int Id);
}
