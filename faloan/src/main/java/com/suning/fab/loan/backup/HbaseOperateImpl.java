package com.suning.fab.loan.backup;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.utils.LoggerUtil;

@Service
public class HbaseOperateImpl implements HbaseOperate{

	@Autowired
	private HbaseConnection connection;

	@SuppressWarnings("unchecked")
	@Override
	public Map<String,List<Map<String, Object>>> getDetailByRowKey(String tableName, String rowkeyfield, String partitionfield, 
			int partitionlength, int partitioncount, Map<String, String> paramMap) {

		Map<String,List<Map<String, Object>>> resultMap = new HashMap<String,List<Map<String, Object>>>();
		/*Table table = null;
		SerializableImpl jdkSerializable = new SerializableImpl();

		try {
			String rangeNo = HbaseServiceUtil.getRangeNo(partitionfield, partitionlength, partitioncount, paramMap);
			String rowKey = HbaseServiceUtil.getCombinationStartRowKey(rowkeyfield, paramMap);
			LoggerUtil.debug("查询数据所在分区为:{},查询的rowKey为:{}", rangeNo, rowKey);
			Get get = new Get(Bytes.toBytes(rangeNo.concat(rowKey)));
			table = connection.getSuningHBaseClient().getTable(tableName);

			Result result = table.get(get);
			if (null == result || result.isEmpty()) {
				LoggerUtil.debug("查询Hbase没有找到该值:{}", rowKey);
				return resultMap;
			}
			for(Cell cell:result.rawCells()){
				List<Map<String, Object>> listMap;
				if(null == cell && 0 == CellUtil.cloneValue(cell).length){
					continue;
				}
				listMap = jdkSerializable.deserialize(CellUtil.cloneValue(cell), List.class);

				if(null != listMap){
					resultMap.put(new String(CellUtil.cloneQualifier(cell)), listMap);
				}
			}
			return resultMap;
		} catch (IOException e) {
			LoggerUtil.error("Hbase查询数据反序列化失败：{}", e);
		} catch (Exception e) {
			LoggerUtil.error("HbaseOperateImpl getDetailByRowKey异常:{}",e);
		} finally {
			if (table!=null) {
				try {
					table.close();//释放资源
				} catch (Exception e) {
					table=null;
					LoggerUtil.error("HbaseOperateImpl getDetailByRowKey释放资源异常:{}",e);
				}
			}
		}*/
		return resultMap;
	}

	@Override
	public void singleInsertData(BackupEvent be) {
		//获取表名
		String tableName = ScmProtiesUtil.getProperty(ConstantDeclare.HBASE.TABLENAME,"ns_faccount:hb_faloanrepaydata");
		//格式化rowkey
		String acctno = HbaseCommonUtil.addZeroForNum(be.getBackupData().getAcctno(), 32, "0");
		String trandate = HbaseCommonUtil.addZeroForNum(be.getBackupData().getCtx().getTranDate(), 10, "0");
		String serseqno = HbaseCommonUtil.addZeroForNum(be.getBackupData().getCtx().getSerSeqNo().toString(), 10, "0");
		String partitioncount = ScmProtiesUtil.getProperty(ConstantDeclare.HBASE.PARTITIONCOUNT, "1000");
		String partitionlength = ScmProtiesUtil.getProperty(ConstantDeclare.HBASE.PARTITIONLENGTH, "3");
		String range = HbaseCommonUtil.addZeroForNum(String.valueOf(Math.abs(be.getBackupData().getAcctno().hashCode() % Integer.parseInt(partitioncount))), Integer.parseInt(partitionlength), "0");
		String rowkey = range + acctno + trandate + serseqno;
		
		Table table = null;
		try {
			table = connection.getSuningHBaseClient().getTable(tableName);
			Put put = new Put(Bytes.toBytes(rowkey));
			
			for(Entry<String, byte[]> en:be.getBackupData().getData().entrySet()){
				put.addColumn(Bytes.toBytes(ConstantDeclare.HBASE.COLUMN), Bytes.toBytes(en.getKey()), en.getValue());
			}
			//写入数据
			table.put(put);

			LoggerUtil.info("Hbase写入成功，rowKey:[{}]", rowkey);
		} catch (Exception e) {
			LoggerUtil.error("HbaseOperateImpl singleInsertData异常:{}",e);
		} finally {
			if (table!=null) {
				try {
					//释放资源
					table.close();
				} catch (Exception e) {
					table=null;
					LoggerUtil.error("HbaseOperateImpl getDetailByRowKey释放资源异常:{}",e);
				}
			}
		}
	}

}
