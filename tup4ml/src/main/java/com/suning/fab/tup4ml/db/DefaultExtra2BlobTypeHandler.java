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
import com.suning.fab.model.domain.fator.IExtraCollection;
import com.suning.fab.tup4ml.utils.LoggerUtil;

/**
 * 将AbstractExtraCollection对象自动序列化为byte类型，无需人工干预
 *
 * @author 17060915
 * @since 2018年3月2日下午4:51:41
 */
public class DefaultExtra2BlobTypeHandler extends BaseTypeHandler<IExtraCollection> {
    static final String ENCONDING = "UTF-8";

    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, IExtraCollection parameter, JdbcType jdbcType)
            throws SQLException {
        // 将传过来的参数值，先用fastjson序列化成字符串，然后将字符串用gzip压缩后存储
        JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd";
        String str = JSON.toJSONString(parameter,SerializerFeature.WriteDateUseDateFormat,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.IgnoreErrorGetter);   
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
//            gzip = new GZIPOutputStream(out);
//            gzip.write(str.getBytes(ENCONDING));
//            gzip.close();
//            
//            byte[] outBytes = out.toByteArray();
            byte[] outBytes = str.getBytes(ENCONDING);
            ByteArrayInputStream bis = new ByteArrayInputStream(outBytes);
            ps.setBinaryStream(i, null, 0);
            ps.setBinaryStream(i, bis, outBytes.length);
        } catch (Exception e) {
            LoggerUtil.error("gzip zip error!! {}",e);
        }
        
    }

    @Override
    public IExtraCollection getNullableResult(ResultSet rs, String columnName) throws SQLException {

        Blob blob = rs.getBlob(columnName);
        byte[] returnValue = null;
        if (null != blob) {
            returnValue = blob.getBytes(1, (int) blob.length());
        }
        
        return parseByteToObject(returnValue);
    }

    @Override
    public IExtraCollection getNullableResult(ResultSet rs, int columnIndex) throws SQLException {

        Blob blob = rs.getBlob(columnIndex);
        byte[] returnValue = null;
        if (null != blob) {
          returnValue = blob.getBytes(1, (int) blob.length());
        }

        return parseByteToObject(returnValue);
    }

    @Override
    public IExtraCollection getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {

        Blob blob = cs.getBlob(columnIndex);
        byte[] returnValue = null;
        if (null != blob) {
          returnValue = blob.getBytes(1, (int) blob.length());
        }
        return parseByteToObject(returnValue);
    }

    /**
     * 将从数据库中读取的字节转为Object对象；
     * @param returnValue 数据库读取返回的字节数据；
     * @return 返回扩展集合接口数据；
     */
    private IExtraCollection parseByteToObject(byte[] returnValue) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(returnValue);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return JSON.parseObject(out.toString(ENCONDING),IExtraCollection.class);
        } catch (Exception e) {
            LoggerUtil.error("gzip ungzip error!! {}",e);
        }
        return null;
    }
}