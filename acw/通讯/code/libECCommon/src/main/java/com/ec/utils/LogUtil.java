package com.ec.utils;

import com.ec.constants.Symbol;
import com.ec.logs.LogConstants;
import com.ec.net.proto.UtilProtocol;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class LogUtil {
    private static int index = 4;

    public static String addBaseLog(String param1, String param2) {

        String str = addLog(getUsrBaseLog(), param1);
        str = addLog(str, param2);

        return str;
    }

    public static String addBaseNoExtLog(String param) {
        return addLog(getBaseLog(index), new String[]{}) + Symbol.HALF_BLANK + param;
    }

    public static String getBaseLog() {
        return getBaseLog(index);
    }

    public static String getBaseExtLog() {
        return addLog(getBaseLog(index), new String[]{});
    }

    public static String addBaseExtLog(String param) {
        String[] str = param.split(Symbol.SHUXIAN_REG);

        return addLog(getBaseLog(index), str);
    }

    public static String addBaseExtLog(String[] param) {
        return addLog(getBaseLog(index), param);
    }

    public static String getExtLog() {
        return addLog(getLogMethodName(index), new String[]{});
    }

    public static String getExtLog(String param) {
        return getLogMethodName(index) + Symbol.HALF_BLANK + param;
    }

    public static String addExtLog(String param) {
        String[] str = param.split(Symbol.SHUXIAN_REG);

        return addLog(getLogMethodName(index), str);
    }

    public static String addExtLog(String[] param) {
        return addLog(getLogMethodName(index), param);
    }

    public static String addFuncExtLog(String param) {
        return addFuncExtLog(LogConstants.FUNC_CHANNEL, param, index + 1);
    }

    public static String addFuncExtLog(String func, String param) {
        return addFuncExtLog(func, param, index + 1);
    }

    private static String addFuncExtLog(String func, String param, int index) {
        String[] str = param.split(Symbol.SHUXIAN_REG);

        return addLog(getLogMethodName(index) + Symbol.HALF_BLANK + func, str);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String getPhoneBaseLog() {

        List names = new ArrayList<>();

        //names.add(addBase("user gate"));
        //names.add(addBase("severType"));
        names.add(addBase("cmd"));
        //names.add(addBase("ret"));
        names.add(addBase("epcode"));
        names.add(addBase("gunno"));
        names.add(addBase("userIdentity"));

        return StringUtil.join(Symbol.DOUHAO, names);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String getUsrBaseLog() {

        List names = new ArrayList<>();

        //names.add(addBase("user gate"));
        //names.add(addBase("severType"));
        names.add(addBase("cmd"));
        //names.add(addBase("ret"));
        names.add(addBase("epcode"));
        names.add(addBase("gunno"));
        names.add(addBase("orgno"));
        names.add(addBase("userIdentity"));

        return StringUtil.join(Symbol.DOUHAO, names);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String getEPBaseLog() {

        List names = new ArrayList<>();

        //names.add(addBase("ep gate"));
        names.add(addBase("cmd"));
        //names.add(addBase("ret"));
        names.add(addBase("epcode"));
        names.add(addBase("gunno"));
        names.add(addBase("orgno"));
        names.add(addBase("userIdentity"));

        return StringUtil.join(Symbol.DOUHAO, names);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String getCOBaseLog() {

        List names = new ArrayList<>();

        names.add(addBase("cmd"));
        names.add(addBase("epcode"));
        names.add(addBase("gunno"));
        names.add(addBase("orgno"));
        names.add(addBase("userIdentity"));
        names.add(addBase("token"));

        return StringUtil.join(Symbol.DOUHAO, names);
    }

    private static String addLog(String base, String log) {
        if (StringUtils.isEmpty(base)) return addBase(log);

        String[] arrStr = new String[]{base, addBase(log)};

        return StringUtil.join(Symbol.DOUHAO, arrStr);
    }

    private static String addLog(String base, String[] param) {
        String str = StringUtils.EMPTY;

        for (String aParam : param) str = addLog(str, aParam);

        if (base.indexOf(Symbol.SEMI_COLON) > 0) {
            str = base + Symbol.DOUHAO + str;
        } else if (StringUtils.isNotEmpty(base) && !base.endsWith(Symbol.HALF_BLANK)) {
            str = base + Symbol.HALF_BLANK + str;
        } else {
            str = base + str;
        }

        return str;
    }

    private static String addBase(String log) {

        String[] tmpStr = new String[]{log, "{}"};

        return StringUtil.join(Symbol.SEMI_COLON, tmpStr);
    }

    private static String getLogMethodName() {
        return getLogMethodName(index);
    }

    private static String getLogMethodName(int index) {
        StackTraceElement tmp = getLogStack(index);
        if (tmp == null) return StringUtils.EMPTY;

        return tmp.getMethodName();
    }

    private static StackTraceElement getLogStack(int index) {
        StackTraceElement[] tmp = new Throwable().getStackTrace();
        if (tmp.length < index) return null;

        return tmp[index - 1];
    }

    public static String getBaseLog(int index) {
        StackTraceElement tmp = getLogStack(index);
        if (tmp == null) return StringUtils.EMPTY;

        String str = tmp.getMethodName() + Symbol.HALF_BLANK;

        String className = tmp.getClassName();
        if (className.indexOf("usrcore") > 0) {
            str += getUsrBaseLog();
        } else if (className.indexOf("phonegate") > 0) {
            str += getPhoneBaseLog();
        } else if (className.indexOf("cooperate") > 0) {
            str += getCOBaseLog();
        } else {
            str += getEPBaseLog();
        }

        return str.trim();
    }

    public static String getLogName(String className) {
        int tmp = className.lastIndexOf(Symbol.PERIOD);
        if (tmp < 1) return className;

        String str = className.substring(tmp + 1) + Symbol.SPLIT;

        if (className.indexOf("usrcore") > 0) {
            str += LogConstants.PROJ_USRLAYER;
        } else if (className.indexOf("phonegate") > 0) {
            str += LogConstants.PROJ_DPHONEGATE;
        } else if (className.indexOf("epcore") > 0) {
            str += LogConstants.PROJ_DEPGATE;
        } else if (className.indexOf("cooperate") > 0) {
            str += LogConstants.PROJ_LIB_COOPERATE;
        } else {
            str += LogConstants.PROJ_LIB_ECCOMMON;
        }

        return str;
    }

    public static void main(String[] args) {
        //ByteBuffer byteBuffer = new ByteBuffer.allocate(2);
        //int userOrgin = (int)ByteBufferUtil.readUB2(byteBuffer);
        byte[] b = UtilProtocol.stringToBytes("688E00DE0ED2008201060000000000002E3603020010000001013603020010000001170707104420000104EA030000000000000000000000000000000000000000000000000000000000000000026842150A270711E0AB350A27071100000000000000000000000000000000000000007707000000000000000000005C7100007707000000000000BB98990011A4990000");
        System.out.print(LogUtil.getExtLog("fail chargeCache==null"));
    }
}
