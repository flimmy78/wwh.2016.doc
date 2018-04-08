package com.epcentre.dao;

import java.util.List;
import java.util.Map;

import com.epcentre.model.TblUserNewcoupon;

public interface TblUserNewcouponDao {
	
    public List<TblUserNewcoupon> select(int id);

    public int update(TblUserNewcoupon userNewcoupon);
    
    public int insert(TblUserNewcoupon userNewcoupon);
	
}
