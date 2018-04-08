package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblUserInfo;

public interface TblUserInfoDao {
	
    public List<TblUserInfo> findUserInfoByPhone(String account);
    
    public List<TblUserInfo> findUserInfoById(int accountId);
    
    public int updateBalance(TblUserInfo info);
    
    public int subCost(TblUserInfo info);
    
    public int addCost(TblUserInfo info);

}
