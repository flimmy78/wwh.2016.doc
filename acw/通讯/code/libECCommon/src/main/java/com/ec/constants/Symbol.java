package com.ec.constants;

public class Symbol {
	
	public static final String FENHAO=";";//分号
	public static final String FENHAO_REG=";|；";//分号
	public static final String MAOHAO_REG=":|：";//冒号
	public static final String DOUHAO=",";
	public static final String DOUHAO_REG=",|，";
	public static final String XIEGANG_REG="/";
	public static final String SPLIT="|";
	public static final String PERIOD=".";
	public static final String SHUXIAN_REG="\\|";
	public static final String XIAHUAXIAN_REG="_";
	public static final String JIANHAO = "-";
	public static final String JINGHAO_REG="\\#";
	public static final String AT_REG="@";
	public static final String HALF_BLANK=" ";
	public static final String SEMI_COLON=":";
	
	/** },{ */
	public static final String DAKUOHAOREG ="\\},\\{";
	/** { */
	public static final String DAKUOHAOREG_LEFT ="\\{";
	/** } */
	public static final String DAKUOHAOREG_RIGHT ="\\}";
	
	public static void main(String[] args){
		String s = "aaaaaa：bbbbb";
		System.out.println(s.split(MAOHAO_REG)[0]);
		System.out.println(s.split(MAOHAO_REG)[1]);
	}
	
}
