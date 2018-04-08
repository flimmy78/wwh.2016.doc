package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblPartner;

public interface TblPartnerDao {
	
	public List<TblPartner> getPartners();
	
	public void updateClientInfo(TblPartner partner);
	
	public void updateToken(TblPartner partner);
	
	public void updateKey(TblPartner partner);
	
	
}
