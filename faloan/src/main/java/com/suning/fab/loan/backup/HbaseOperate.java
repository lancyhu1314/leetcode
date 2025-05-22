package com.suning.fab.loan.backup;

import java.util.List;
import java.util.Map;

public interface HbaseOperate {

	/** 根据rowkey查询详细信息
	 * 
	 * @param tableName 		表名
	 * @param rowkeyfield  		rowkey依赖字段及字段长度
	 * @param pagesize  		限制返回数据量（分页查询使用）
	 * @param partitionfield  	分区的依赖字段
	 * @param partitionlength  	分区数的长度，比如128个区长度为3
	 * @param partitioncount  	分区的个数
	 * @param paramMap  		查询入参集合
	 * */
	public Map<String,List<Map<String, Object>>> getDetailByRowKey(String tableName, String rowkeyfield, String partitionfield,
			int partitionlength, int partitioncount, Map<String, String> paramMap);

	/**
	 * 单条写Hbase
	 * @param tableName		表名
	 * @param rowkey		行键（唯一标识）
	 * @param map			数据格式Map<String, byte[]>：String为表名，byte[]为数据
	 */
	public void singleInsertData(BackupEvent be);

}
