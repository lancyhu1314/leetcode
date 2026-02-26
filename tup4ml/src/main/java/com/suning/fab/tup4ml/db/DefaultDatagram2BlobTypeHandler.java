package com.suning.fab.tup4ml.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.utils.LoggerUtil;

public class DefaultDatagram2BlobTypeHandler extends BaseTypeHandler<AbstractDatagram>{
	static final String ENCONDING = "UTF-8";
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, AbstractDatagram parameter, JdbcType jdbcType)
			throws SQLException {
//		// 将传过来的参数值，先用fastjson序列化成字符串，然后将字符串用gzip压缩后存储
//		ProtoRegJsonMaker jsonMaker = new ProtoRegJsonMaker();
//		jsonMaker.setBusinessClassName(parameter.getClass().getSimpleName());
//		jsonMaker.setBusinessClass(parameter);
//        ps.setString(i, ZlibAndBase64Util.compressData(JsonTransferUtil.toJsonWithDateFormat(parameter)));
		
		// 将传过来的参数值，先用fastjson序列化成字符串，然后将字符串用gzip压缩后存储
				ProtoRegJsonMaker jsonMaker = new ProtoRegJsonMaker();
				jsonMaker.setBusinessClassName(parameter.getClass().getSimpleName());
				jsonMaker.setBusinessClass(parameter);
		        JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd";
		        String str = JSON.toJSONString(jsonMaker, SerializerFeature.WriteDateUseDateFormat,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.IgnoreErrorGetter);  
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				GZIPOutputStream gzip;
				try {
//					gzip = new GZIPOutputStream(out);
//					gzip.write(str.getBytes(ENCONDING));
//					gzip.close();

//					byte[] outBytes = out.toByteArray();
				    byte[] outBytes = str.getBytes(ENCONDING);
					ByteArrayInputStream bis = new ByteArrayInputStream(outBytes);
		            ps.setBinaryStream(i, null, 0);
					ps.setBinaryStream(i, bis, outBytes.length);
				} catch (Exception e) {
					LoggerUtil.error("gzip zip error!! {}",e);
				}

	}

	@Override
	public AbstractDatagram getNullableResult(ResultSet rs, String columnName) throws SQLException {
//		String retJson = ZlibAndBase64Util.decompressData(rs.getString(columnName));
//		ProtoRegJsonMaker jsonMaker = JSON.parseObject(retJson, ProtoRegJsonMaker.class);
//		return jsonMaker.getBusinessClass();
		
		Blob blob = rs.getBlob(columnName);
		byte[] returnValue = null;
		if (null != blob) {
			returnValue = blob.getBytes(1, (int) blob.length());
		}

		return parseByteToObject(returnValue);
	}

	@Override
	public AbstractDatagram getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
//		String retJson = ZlibAndBase64Util.decompressData(rs.getString(columnIndex));
//		ProtoRegJsonMaker jsonMaker = JSON.parseObject(retJson, ProtoRegJsonMaker.class);
//		return jsonMaker.getBusinessClass();
		
		Blob blob = rs.getBlob(columnIndex);
		byte[] returnValue = null;
		if (null != blob) {
			returnValue = blob.getBytes(1, (int) blob.length());
		}

		return parseByteToObject(returnValue);
	}

	@Override
	public AbstractDatagram getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
//		String retJson = ZlibAndBase64Util.decompressData(cs.getString(columnIndex));
//		ProtoRegJsonMaker jsonMaker = JSON.parseObject(retJson, ProtoRegJsonMaker.class);
//		return jsonMaker.getBusinessClass();
		
		Blob blob = cs.getBlob(columnIndex);
		byte[] returnValue = null;
		if (null != blob) {
			returnValue = blob.getBytes(1, (int) blob.length());
		}
		return parseByteToObject(returnValue);
	}

	private AbstractDatagram parseByteToObject(byte[] returnValue) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayInputStream in = new ByteArrayInputStream(returnValue);
		try {
			GZIPInputStream ungzip = new GZIPInputStream(in);
			byte[] buffer = new byte[256];
			int n;
			while ((n = ungzip.read(buffer)) >= 0) {
				out.write(buffer, 0, n);
			}
			ProtoRegJsonMaker jsonMaker = JSON.parseObject(out.toString(ENCONDING),ProtoRegJsonMaker.class);
			return jsonMaker.getBusinessClass();
		} catch (Exception e) {
			LoggerUtil.error("gzip ungzip error!! {}",e);
		}
		return null;
	}
}
