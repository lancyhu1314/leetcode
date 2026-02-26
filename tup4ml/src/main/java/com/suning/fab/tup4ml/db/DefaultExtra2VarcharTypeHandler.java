package com.suning.fab.tup4ml.db;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.alibaba.fastjson.JSON;
import com.suning.fab.model.domain.fator.IExtraCollection;
import com.suning.fab.tup4ml.utils.JsonTransferUtil;
import com.suning.fab.tup4ml.utils.ZlibAndBase64Util;

/**
 * 将AbstractExtraCollection对象自动序列化为Varchar类型，无需人工干预
 *
 * @author 17060915
 * @since 2018年3月2日下午4:51:41
 */
public class DefaultExtra2VarcharTypeHandler extends BaseTypeHandler<IExtraCollection> {    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, IExtraCollection parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, ZlibAndBase64Util.compressData(JsonTransferUtil.toJsonWithDateFormat(parameter)));
    }

    @Override
    public IExtraCollection getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String retJson = ZlibAndBase64Util.decompressData(rs.getString(columnName));
		return JSON.parseObject(retJson, IExtraCollection.class);
    }

    @Override
    public IExtraCollection getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String retJson = ZlibAndBase64Util.decompressData(rs.getString(columnIndex));
		return JSON.parseObject(retJson, IExtraCollection.class);
    }

    @Override
    public IExtraCollection getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String retJson = ZlibAndBase64Util.decompressData(cs.getString(columnIndex));
		return JSON.parseObject(retJson, IExtraCollection.class);
    }

}