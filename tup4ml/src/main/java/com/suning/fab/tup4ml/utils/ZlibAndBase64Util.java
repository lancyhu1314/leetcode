package com.suning.fab.tup4ml.utils;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public abstract class ZlibAndBase64Util {
	private ZlibAndBase64Util() {
		throw new IllegalStateException("ZlibAndBase64Util class");
	}

	/**
	 * 先zlib压缩在base64编码；
	 * @param data 入参；
	 * @return zlib+base64格式字符串；
	 */
	public static String compressData(String data) {
		try {  
			ByteArrayOutputStream bos = new ByteArrayOutputStream();  
			DeflaterOutputStream zos = new DeflaterOutputStream(bos);  
			zos.write(data.getBytes());  
			zos.close();  
			return new String(Base64.getEncoder().encode(bos.toByteArray()));
		} catch (Exception e) {
			LoggerUtil.error("ZlibAndBase64Util compressData Exception: {}", e);
		}  
		return null;  
	}  
	public static String decompressData(String encdata) {  
		try {  
			ByteArrayOutputStream bos = new ByteArrayOutputStream();  
			InflaterOutputStream zos = new InflaterOutputStream(bos);  
			zos.write(Base64.getDecoder().decode(encdata));  
			zos.close();  
			return new String(bos.toByteArray());  
		} catch (Exception e) {  
			LoggerUtil.error("ZlibAndBase64Util decompressData Exception: {}", e);
		}  
		return null;  
	}  
}
