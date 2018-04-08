package com.epcentre.dao;

import java.util.List;
import java.util.Map;

import com.epcentre.model.TblUserInfo;
import com.epcentre.model.TblUserNormal;

public interface TblUserNormalDao {
	
    public List<TblUserNormal> findUserInfoById(int accountId);
 
}
