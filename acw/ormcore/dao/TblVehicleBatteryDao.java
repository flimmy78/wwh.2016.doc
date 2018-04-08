package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblVehicleBattery;

public interface TblVehicleBatteryDao {

	public List<TblVehicleBattery> select(String SerialNo);
	
	public int insert(TblVehicleBattery info);
		
	public int update(TblVehicleBattery info);
	
}
