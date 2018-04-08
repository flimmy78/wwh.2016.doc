package com.cooperate.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class AesCBC {  
/* 
* 加密用的Key 可以用26个字母和数字组成 
* 此处使用AES-128-CBC加密模式，key需要为16位。 
*/  
    private static String sKey="sklhdflsjfsdgdeg";  
    private static String ivParameter="cfbsdfgsdfxccvd1";  
    private static AesCBC instance=null;  
    private AesCBC(){  
  
    }  
    public static AesCBC getInstance(){  
        if (instance==null)  
            instance= new AesCBC();  
        return instance;  
    }  
    // 加密  
    public String encrypt(String sSrc, String encodingFormat, String sKey, String ivParameter) throws Exception {  
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");  
        byte[] raw = sKey.getBytes();  
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");  
        IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());//使用CBC模式，需要一个向量iv，可增加加密算法的强度  
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);  
        byte[] encrypted = cipher.doFinal(sSrc.getBytes(encodingFormat));  
        return new BASE64Encoder().encode(encrypted);//此处使用BASE64做转码。  
}  
  
    // 解密  
    public String decrypt(String sSrc, String encodingFormat, String sKey, String ivParameter) throws Exception {  
        try {  
            byte[] raw = sKey.getBytes("ASCII");  
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");  
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");  
            IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());  
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);  
            byte[] encrypted1 = new BASE64Decoder().decodeBuffer(sSrc);//先用base64解密  
            byte[] original = cipher.doFinal(encrypted1);  
            String originalString = new String(original,encodingFormat);  
            return originalString;  
        } catch (Exception ex) {  
            return null;  
        }  
}  
  
    public static void main(String[] args) throws Exception { 
    	
    	 System.out.println("加密后的字串是：" + AesCBC.getInstance().encrypt("{\"OperatorID\":\"425010765\",\"OperatorSecret\":\"38814A95B1EDDC8ADDA5B2BFA47C6481\"}", "utf-8", "1234567890abcdef", "1234567890abcdef")); 
//    	 System.out.println("加密后的字串是：" + AesCBC.getInstance().encrypt("{\"LastQueryTime\":\"2017-3-3 12:22:22\",\"PageNo\":\"1\",\"PageSize\":\"1\"}", "utf-8", "1234567890abcdef", "1234567890abcdef")); 
        // 解密  
        String DeString = AesCBC.getInstance().decrypt("3cYGcsYXezutkwUCXxOgo6WLQ/ZC0pcvmMFoyER4CQeUH26OynCo/m4XzwUfcKYaHIEuo0Rgq6y+XqPOMjZ6hjX+h40WnImljAbLomyM4P0=","utf-8","1234567890abcdef","1234567890abcdef");  
        System.out.println("解密后的字串是：" + DeString);  
        //3cYGcsYXezutkwUCXxOgo6WLQ/ZC0pcvmMFoyER4CQeDfOZjgrOKBhdS7itkQG1ZuBzI2aOKMLVhCyUREqe+9w==
        //3cYGcsYXezutkwUCXxOgo6WLQ/ZC0pcvmMFoyER4CQeDfOZjgrOKBhdS7itkQG1ZuBzI2aOKMLVhCyUREqe+9w== 有效
    }  
}  