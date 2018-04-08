package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblEquipmentVersion;

   public interface TblEquipmentVersionDao {

	

      public int insertEqVersion(TblEquipmentVersion equipment);
   
      
      public List<TblEquipmentVersion> findEqVersion(TblEquipmentVersion equipment);
      
      public int updateEqVersion(TblEquipmentVersion equipment);
      
      public int deleteEqVersion(int id);

}
