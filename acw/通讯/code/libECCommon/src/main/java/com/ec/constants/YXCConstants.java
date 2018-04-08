package com.ec.constants;

public class YXCConstants {
	
	public static final byte  PROTOCOL_VERSION_V4 = 4;//协议版本号
	
	//充电桩编码，bcd码长度
	public static final int LEN_BCD_CODE = 8;
	//充电桩编码长度
	public static final int LEN_CODE = 16;
	//充电交易流水号BCD码长度
	public static final int LEN_ELECTRICIZE_SERIALNO = 32;
	//充电交易流水号长度
	public static final int LEN_BCD_ELECTRICIZE_SERIALNO = 16;
	//账号长度
	public static final int LEN_ACCOUNT = 11;
	//账号长度
	public static final int LEN_BIG_ACCOUNT = 12;
	//账号BCD码长度
	public static final int LEN_BCD_ACCOUNT = 6;
	//卡号长度
	public static final int LEN_BANK_CARD = 16;
	//卡号BCD码长度
	public static final int LEN_BCD_BANK_CARD = 8;
	//二维码分段长度
	public static final int QRCODE_SECTION_LEN = 150;
	//电桩二进制程序分段长度
	public static final int EP_EXE_SECTION_LEN = 450;
	
	public static final double TEMP_TIME = 10.0;
	public static final double MONEY_TIME = 100.0;
	public static final double VOL_TIME = 10.0;
	public static final double CUR_TIME = 100.0;
	public static final double METER_NUM_TIME = 1000.0;
	public static final double DC_DB_NUM_TIME = 10.0;
	public static final double DC_BATT_VOL_TIME = 1000.0;
	
	//0：离线 1：故障 2：待机 3：工作 4：欠压故障 5 过压故障 6 过电流故障
	public static final int EP_WORK_STATUS_OFF = 0;
	public static final int EP_WORK_STATUS_FAULT = 1;
	public static final int EP_WORK_STATUS_STANDBY = 2;
	public static final int EP_WORK_STATUS_WORK = 3;
	public static final int EP_WORK_STATUS_LESS_VOL = 4;
	public static final int EP_WORK_STATUS_MORE_VOL = 5;
	public static final int EP_WORK_STATUS_MORE_CUR = 6;
	public static final int EP_WORK_STATUS_BESPOKE = 8;
    public static final int EP_WORK_STATUS_UPDATE = 9;
    
    
    public static final int YX_1_LINKED_CAR = 1;//车与桩连接确认开关状态
    public static final int YX_1_GUN_SIT = 2;//枪座状态
    public static final int YX_1_GUN_LID = 3;//充电枪盖状态
    public static final int YX_1_COMM_WITH_CAR = 4;//车与桩建立通信信号
    public static final int YX_1_CAR_PLACE = 5;//车位占用状态
    public static final int YX_1_CARD_READER_FAULT = 6;//读卡器故障
    public static final int YX_1_URGENT_STOP_FAULT = 7;//急停按钮动作故障
    public static final int YX_1_ARRESTER_EXCEPTION = 8;//避雷器故障
    
    public static final int YX_1_INSULATION_EXCEPTION = 9;//绝缘检测故障
    public static final int YX_1_GUN_UNCONNECT_WARN = 10;//充电枪未连接告警
    public static final int YX_1_TRANSRECORD_FULL_WARN=11;//交易记录已满告警
    public static final int YX_1_METER_ERROR=12;//电度表异常
    
    
    public static final int YX_1_BATTRY_ERROR_LINK = 17;//电池反接故障
    public static final int YX_1_FOGS_WARN = 18; //烟雾报警故障
    public static final int YX_1_BMS_ERROR = 19;//BMS 通信异常
    public static final int YX_1_DCMETER_ERROR=20;//直流电度表异常
    public static final int YX_1_DC_OUT_OVER_CURRENT_WARN=21;//直流输出过流告警
    
    public static final int YX_2_START_POS = 128;//
    
    public static final int YX_2_AC_IN_VOL_WARN = YX_2_START_POS +1;//交流输入电压过压/欠压
    public static final int YX_2_CHARGE_OVER_TEMP=YX_2_START_POS+2;//充电机过温故障
    public static final int YX_2_AC_CURRENT_LOAD_WARN=YX_2_START_POS+3;//交流电流过负荷告警
    public static final int YX_2_OUT_RELAY_STATUS=YX_2_START_POS+4;//输出继电器状态
    
    public static final int YX_2_DC_SUPPLY_CHARGE_STYLE=YX_2_START_POS + 9;//直流电源模块充电模式
    public static final int YX_2_BATTRY_SOC_WARN = YX_2_START_POS + 10;//整车动力蓄电池SOC告警
    public static final int YX_2_BATTRY_SAMPLE_OVER_TEMP = YX_2_START_POS + 11;//蓄电池模块采样点过温告警
    public static final int YX_2_OUT_LINKER_OVER_TEMP = YX_2_START_POS+ 12;//输出连接器过温
    
    public static final int YX_2_OUT_LINKER_STATUS =YX_2_START_POS+13;//输出连接器过温
    public static final int YX_2_BATTRY_CHARGE_OVER_CURRENT=YX_2_START_POS+14;//整车蓄电池充电过流告警
    
    public static final int YX_2_DC_OUT_VOL_WARN = YX_2_START_POS+ 15;//直流母线输出过压/欠压
    public static final int YX_2_BMS_VOL_WARN = YX_2_START_POS+ 16;//BMS过压/欠压
  
    //
    public static final int YC_START_POS = 256;//
    public static final int YC_WORKSTATUS = YC_START_POS + 1;//充电机状态
    public static final int YC_CAR_PLACE_LOCK = YC_START_POS + 2;//车位地锁状态
    public static final int YC_OUT_VOL= YC_START_POS + 3;//充电机输出电压
    public static final int YC_OUT_CURRENT=YC_START_POS + 4;//充电机输出电流
    
    public static final int YC_SOC= YC_START_POS + 5;//
    public static final int YC_TOTAL_TIME= YC_START_POS + 6;//累计充电时间
    public static final int YC_REMAIN_TIME= YC_START_POS + 7;//估计剩余时间
    
    public static final int YC_ERROR_CODE= YC_START_POS + 8;//错误代码
    
    public static final int YC_BATTARY_TYPE= YC_START_POS + 17;//电池类型
    public static final int YC_BATTARY_RATED_CAPACITY = YC_START_POS + 18;//整车动力蓄电池系统额定容量
    public static final int YC_BATTARY_MAKE_YEAR= YC_START_POS + 19;//电池组生产日期年
    public static final int YC_BATTARY_MAKE_DATE= YC_START_POS + 20;//电池组生产日期年
    public static final int YC_BATTARY_CHARGE_TIME= YC_START_POS + 21;//电池组生产日期年
    public static final int YC_SIGNLE_BATTRY_CAN_HIGH_VOL=YC_START_POS +22;//单体蓄电池最高允许充电电压
    public static final int YC_SIGNLE_BATTRY_HIGH_CURRENT=YC_START_POS +23;//最高允许充电电流
    public static final int YC_BATTRY_TOTAL_POWER=YC_START_POS +24;//动力蓄电池标称总能量
    public static final int YC_BATTRY_HIGH_VOL=YC_START_POS +25;//最高允许充电总电压(额定总电压)
    public static final int YC_BATTRY_CAN_HIGH_TEMP=YC_START_POS +26;//最高允许温度
    
    public static final int YC_SIGNLE_BATTRY_HIGH_VOL_GROUP=YC_START_POS +33;//单体蓄电池最高电压和组号
    public static final int YC_BATTRY_HIGHEST_TEMP=YC_START_POS +34;//电池组最高温度
    public static final int YC_BATTRY_LOWEST_TEMP=YC_START_POS +35;//电池组最高温度
    
    public static final int YC_CAR_BATTRY_TOTAL_VOL=YC_START_POS +36;//整车动力电池总电压
    
    public static final int YC_A_VOL=YC_START_POS +41;//A相电压
    public static final int YC_B_VOL=YC_START_POS +42;//B相电压
    public static final int YC_C_VOL=YC_START_POS +43;//C相电压
    
    public static final int YC_A_CURRENT=YC_START_POS +44;//A相电流
    public static final int YC_B_CURRENT=YC_START_POS +45;//B相电流
    public static final int YC_C_CURRENT=YC_START_POS +46;//C相电流
    
    public static final int YC_OUT_HIGH_VOL=YC_START_POS +47;//最高输出电压
    public static final int YC_OUT_LOW_VOL=YC_START_POS +48;//最低输出电压
    public static final int YC_OUT_HIGH_CURRENT=YC_START_POS +49;//最低输出电压
    
    public static final int YC_EP_TEMP=YC_START_POS + 50;//电桩内部温度
    	
    public static final int YC_CHARGER_MOD_START_POS=YC_START_POS +129;//电源模块1输出电压
    public static final int YC_CHARGER_MOD_PARAMS_NUM=8;//
    
    public static final int YC_VAR_START_POS = 2756;//
    public static final int YC_VAR_ACTIVE_TOTAL_METERNUM = YC_VAR_START_POS+1;//有功总电度
    public static final int YC_VAR_CHARGED_COST = YC_VAR_START_POS+2;//已充金额
    public static final int YC_VAR_CHARGED_PRICE = YC_VAR_START_POS+3;//单价
    public static final int YC_VAR_CHARGED_METER_NUM = YC_VAR_START_POS+4;//已充度数
    public static final int YC_VAR_ACTIVE_TOTAL_GUN1 = YC_VAR_START_POS+5;//枪口1有功总电度
    public static final int YC_VAR_ACTIVE_TOTAL_GUN2 = YC_VAR_START_POS+6;//枪口2有功总电度
    public static final int YC_VAR_ACTIVE_TOTAL_GUN3 = YC_VAR_START_POS+7;//枪口3有功总电度
    public static final int YC_VAR_ACTIVE_TOTAL_GUN4 = YC_VAR_START_POS+8;//枪口4有功总电度
    
    public static final int YC_VAR_CAR_VIN = YC_VAR_START_POS+9;//车辆识别码
    public static final int YC_VAR_BATTARY_FACTORY = YC_VAR_START_POS+10;//电池厂商
    
}
