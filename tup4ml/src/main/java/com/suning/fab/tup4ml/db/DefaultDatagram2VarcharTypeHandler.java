package com.suning.fab.tup4ml.db;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.alibaba.fastjson.JSON;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.utils.JsonTransferUtil;
import com.suning.fab.tup4ml.utils.ZlibAndBase64Util;

public class DefaultDatagram2VarcharTypeHandler extends BaseTypeHandler<AbstractDatagram>{
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, AbstractDatagram parameter, JdbcType jdbcType)
			throws SQLException {
		// 将传过来的参数值，先用fastjson序列化成字符串，然后将字符串用gzip压缩后存储
		ProtoRegJsonMaker jsonMaker = new ProtoRegJsonMaker();
		jsonMaker.setBusinessClassName(parameter.getClass().getSimpleName());
		jsonMaker.setBusinessClass(parameter);
        ps.setString(i, ZlibAndBase64Util.compressData(JsonTransferUtil.toJsonWithDateFormat(jsonMaker)));
	}

	@Override
	public AbstractDatagram getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String retJson = ZlibAndBase64Util.decompressData(rs.getString(columnName));
		ProtoRegJsonMaker jsonMaker = JSON.parseObject(retJson, ProtoRegJsonMaker.class);
		return jsonMaker.getBusinessClass();
	}

	@Override
	public AbstractDatagram getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String retJson = ZlibAndBase64Util.decompressData(rs.getString(columnIndex));
		ProtoRegJsonMaker jsonMaker = JSON.parseObject(retJson, ProtoRegJsonMaker.class);
		return jsonMaker.getBusinessClass();
	}

	@Override
	public AbstractDatagram getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String retJson = ZlibAndBase64Util.decompressData(cs.getString(columnIndex));
		ProtoRegJsonMaker jsonMaker = JSON.parseObject(retJson, ProtoRegJsonMaker.class);
		return jsonMaker.getBusinessClass();
	}

}
