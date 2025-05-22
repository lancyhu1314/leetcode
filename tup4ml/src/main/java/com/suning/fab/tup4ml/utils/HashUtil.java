package com.suning.fab.tup4ml.utils;

import java.security.MessageDigest;

import com.suning.fab.tup4ml.exception.FabRuntimeException;

public abstract class HashUtil {
	private HashUtil() {
		throw new IllegalStateException("HashUtil class");
	}

	/**  
	 * MD5加密  
	 * @param message 要进行MD5加密的字符串  
	 * @return 加密结果为32位字符串  
	 */    
	public static String getMD5(String message) {
		MessageDigest messageDigest = null;    
		StringBuilder md5StrBuff = new StringBuilder();    
		try {    
			messageDigest = MessageDigest.getInstance("MD5");    
			messageDigest.reset();    
			messageDigest.update(message.getBytes("UTF-8"));    

			byte[] byteArray = messageDigest.digest();    
			for (int i = 0; i < byteArray.length; i++)     
			{    
				if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)    
					md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));    
				else    
					md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));    
			}    
		} catch (Exception e) {    
			throw new FabRuntimeException(e, "TUP107");    
		}
		return md5StrBuff.toString().toUpperCase();//字母大写
	}
}
