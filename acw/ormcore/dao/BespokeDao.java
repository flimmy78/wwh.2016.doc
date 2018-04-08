package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblBespoke;

public interface BespokeDao {
	
	
	 public List<TblBespoke> getBesp(long id);
	 public void update(TblBespoke besp);
	 public void updateRedo(TblBespoke besp);
	 public List<TblBespoke> getUnStopBesp(TblBespoke info);
	 public List<TblBespoke> getUnStopBespByUserId(TblBespoke info);
	 public void updateAppClient(TblBespoke besp);
	 
	 public long insert(TblBespoke info);
	 
}
