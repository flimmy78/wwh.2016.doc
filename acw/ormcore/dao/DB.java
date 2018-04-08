package com.epcentre.dao;

import com.epcentre.cache.GameContext;
import com.epcentre.model.TblUserNewcoupon;

public class DB {
	public static RateInfoDao rateInfoDao = (RateInfoDao) GameContext.getBean("rateInfoDao");
		
	public static TblUserInfoDao userInfoDao = (TblUserInfoDao) GameContext.getBean("tblUserInfoDao");
	
	public static TblUserNormalDao userNormalDao = (TblUserNormalDao) GameContext.getBean("tblUserNormalDao");
	
	public static TblUserBusinessDao userBusinessDao = (TblUserBusinessDao) GameContext.getBean("tblUserBusinessDao");
	
	
	public static TblElectricPileDao epClientDao = (TblElectricPileDao) GameContext.getBean("tblElectricPileDao");

	public static BespokeDao bespDao = (BespokeDao) GameContext.getBean("bespokeDao");
	
	public static ChargingOrderDao chargeOrderDao = (ChargingOrderDao) GameContext.getBean("chargingOrderDao");
	
	public static PurchaseHistoryDao phDao = (PurchaseHistoryDao) GameContext.getBean("purchaseHistoryDao");
	
	public static EpGunDao epGunDao = (EpGunDao) GameContext.getBean("epGunDao");
	
	public static ChargingrecordDao chargingrecordDao = (ChargingrecordDao) GameContext.getBean("chargingrecordDao");
	
	public static ChargingfaultrecordDao chargingfaultrecordDao = (ChargingfaultrecordDao) GameContext.getBean("chargingfaultrecordDao");
	
	
    public static TblConcentratorDao concentratorDao = (TblConcentratorDao) GameContext.getBean("tblConcentratorDao");
    
   
    public static TblPartnerDao partnerDao = (TblPartnerDao) GameContext.getBean("tblPartnerDao");
    
    public static TblPartnerTimeDao partnerTimeDao = (TblPartnerTimeDao) GameContext.getBean("tblPartnerTimeDao");

    public static TblEquipmentVersionDao equipmentVersionDao = (TblEquipmentVersionDao) GameContext.getBean("tblEquipmentVersionDao");

    public static ChargeCardDao chargeCardDao = (ChargeCardDao) GameContext.getBean("chargeCardDao");
   
    public static TblChargeACInfoDao chargeACInfoDao = (TblChargeACInfoDao) GameContext.getBean("tblChargeACInfoDao");
	
	public static TblChargeDCInfoDao chargeDCInfoDao = (TblChargeDCInfoDao) GameContext.getBean("tblChargeDCInfoDao");
	
    public static BomListDao bomListDao = (BomListDao) GameContext.getBean("bomListDao");
    
    public static TypeSpanDao typeSpanDao = (TypeSpanDao) GameContext.getBean("typeSpanDao");
    
    public static EquipmentRepairDao equipmentRepairDao = (EquipmentRepairDao) GameContext.getBean("equipmentRepairDao");
    
    public static TblVehicleBatteryDao vehicleBatteryDao = (TblVehicleBatteryDao) GameContext.getBean("tblVehicleBatteryDao");

    public static TblPowerModuleDao powerModuleDao = (TblPowerModuleDao) GameContext.getBean("tblPowerModuleDao");

    public static TblUserNewcouponDao userNewcouponDao = (TblUserNewcouponDao)GameContext.getBean("tblUserNewcouponDao");

    public static TblCouponDao couponDao = (TblCouponDao) GameContext.getBean("tblCouponDao");
    public static TblCarVinDao carVinDao = (TblCarVinDao) GameContext.getBean("tblCarVinDao");
    
    public static TblJpushDao jpushDao = (TblJpushDao) GameContext.getBean("tblJpushDao");
    
    public static TblUserThreshodDao userThreshodDao = (TblUserThreshodDao) GameContext.getBean("tblUserThreshodDao");
}
