package com.ec.constants;

/**
 * user相关常量
 * @author xujianxin
 * @time May 23, 2013 1:40:53 PM
 */
public class UserConstants {

	/**账号状态:0 未激活*/
	public static final int UNACTIVATED = 0;
	/**账号状态:1 正常*/
	public static final int NORMAL = 1;
	/**账号状态:2 冻结*/
	public static final int FREEZE = 2;
	/**账号状态:3 永久冻结*/
	public static final int LONG_FREEZE = 3;
	
	public static final int WORK_IDLE = 0;
	
	public static final int WORK_BESPOKE = 3;
	
	public static final int WORK_CHARGE =6;
	
	public static final int WORK_OPERATION_EP =10;
	
	
	//
	public static final int CMD_FROM_API = 1;
	public static final int CMD_FROM_PHONE = 2;
	public static final int CMD_FROM_third = 3; //第三方
	
	public static final int CMD_FROM_MONTIOR = 100;
	
	public static final int ORG_PARTNER_MOBILE = 5;
	
	public static final int ORG_I_CHARGE = 1000;// 爱充
	
	public static final int ORG_PXGJ = 1002;//萍乡公交
	
	public static final int ORG_BQ = 1008;//北汽出行公司标识

	public static final int ORG_CCZC = 9002;//曹操专车标识

	public static final int ORG_XIAN = 9003;//西安一卡通

	public static final int ORG_EC = 1011;//E充

	public static final int ORG_SHSTOP = 9004;//上海停车办

	public static final int ORG_CHAT = 9006;//微信
}
