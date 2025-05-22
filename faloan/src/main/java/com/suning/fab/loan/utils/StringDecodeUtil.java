package com.suning.fab.loan.utils;

import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * String 解压缩工具
 * 
 * @author 16090227
 *
 */
public class StringDecodeUtil {
	private static Logger logger = LoggerFactory.getLogger(StringDecodeUtil.class);
	/**
	 * 压缩字符串
	 * 
	 * @param data
	 * @return
	 */
	public static String compressData(String data) {
		ByteArrayOutputStream bos;
		DeflaterOutputStream zos;
		try {
			bos = new ByteArrayOutputStream();
			zos = new DeflaterOutputStream(bos);
			zos.write(data.getBytes("UTF-8")); 
			zos.close();
			bos.close();
			return new String(Base64.encodeBase64(bos.toByteArray()),"UTF-8");
		} catch (IOException e) {
			logger.error("发生异常：{}",e);
		} 
		return null;
	}
	
	/**
	 * 解压字符串
	 *  
	 * @param encdata
	 * @return
	 */
	 public static String decompressData(String encdata) {  
	        try {  
	            ByteArrayOutputStream bos = new ByteArrayOutputStream();  
	            InflaterOutputStream zos = new InflaterOutputStream(bos);  
	            zos.write(Base64.decodeBase64(encdata.getBytes("UTF-8")));
	            zos.close();  
	            bos.close(); 
	            return new String(bos.toByteArray(),"UTF-8");
	        } catch (Exception ex) {  
	        	logger.error("发生异常：{}",ex);
	        }finally{
	        	
	        }
	        return null;  
	    } 

}
