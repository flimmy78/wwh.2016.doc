package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblChargeCard;

public interface ChargeCardDao {
	 
    public List<TblChargeCard> findCard(String  uc_InternalCardNumber);
    public List<TblChargeCard> findCardById(int Id);
    
    public int insertCard(TblChargeCard card);
	
}
