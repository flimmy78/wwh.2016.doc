package com.epcentre.dao;

import java.util.List;
import java.util.Map;

import com.epcentre.model.TblUserBusiness;
import com.epcentre.model.TblUserInfo;

public interface TblUserBusinessDao {
	
    
    public List<TblUserBusiness> findUserInfoById(int accountId);
    
    public Integer findUserInfoByOrgNo(int orgNo);
	
}
