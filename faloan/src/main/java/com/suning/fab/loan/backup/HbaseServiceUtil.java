package com.suning.fab.loan.backup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Scan;

import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

public class HbaseServiceUtil {
	
	private HbaseServiceUtil() {
		super();
	}
	
	/*
	 * 根据partitionfield的信息获取部分分区列表
	 * @param partitionfield表分区存储依赖字段，格式为XXX,XXX
	 * @param partitionlength 分区数的长度，比如128个分区长度为3
	 * @param partitioncount 分区数的个数
	 * @param paramList 分区数的依赖字段所对应值列表
	 * @return 返回分区的列表
	 */
	public static List<String> getPartRangeNo (String partitionfield,int partitionlength,int partitioncount,List<Map<String, String>> paramList){
		List<String> rangeList=new ArrayList<String>();
		try {
			for (Map<String, String> paramMap: paramList) {
				StringBuilder rangeld = new StringBuilder();
				String[] partitionfieldplit = partitionfield.split(",", -1);
				for (int i = 0; i < partitionfieldplit.length; i++) {
					rangeld.append(paramMap.get(partitionfieldplit[i]).trim());
				}
				int range= Math.abs(rangeld.toString().hashCode()%partitioncount);
				rangeList.add(addNumForFill(String.valueOf(range),partitionlength,"0"));
			}
		} catch (Exception e) {
			LoggerUtil.error("根据partitionfield的信息获取部分分区列表异常:{}",e);
		}
		return rangeList;
	}
	/*
	 * 根据partitionfield的信息获取分区号
	 * @param partitionfield表分区存储依赖字段，格式为XXX,XXX
	 * @param partitionlength 分区数的长度，比如128个分区长度为3
	 * @param partitioncount 分区数的个数
	 * @param paramMap 分区数的依赖字段所对应值
	 * @return 返回分区值
	 */
	public static String getRangeNo (String partitionfield,int partitionlength,int partitioncount,Map<String, String> paramMap){
		StringBuilder rangeld = new StringBuilder();
		String[] partitionfieldplit = partitionfield.split(",", -1);
		for (int i = 0; i < partitionfieldplit.length; i++) {
			rangeld.append(paramMap.get(partitionfieldplit[i]).trim());
		}
		int range= Math.abs(rangeld.toString().hashCode()%partitioncount);
		return addNumForFill(String.valueOf(range),partitionlength,"0");
	}

	/*
	 * 根据rowkey的信息组成startrowkey
	 * @param rowkeyfield rowkey的依赖字段，格式为xxx_长度,xxx_长度,xxx_长度
	 * @param paramMap rowkey依赖字段所对应值
	 * @return 返回startRoekey
	 */
	public static String getCombinationStartRowKey (String rowkeyfield,Map<String, String> paramMap){
		StringBuilder rowKey = new StringBuilder();
		String[] rowkeysplit = rowkeyfield.split(",", -1);
		for (int i = 0; i < rowkeysplit.length; i++) {
			String[] rowkeyInfo = rowkeysplit[i].split("_", -1);
			String key=rowkeyInfo[0].trim();
			int length=Integer.parseInt(rowkeyInfo[1].trim());
			rowKey.append(addNumForFill(paramMap.get(key),length,"0"));
		}
		return rowKey.toString();
	}

	/*
	 * 根据rowkey的信息组成stoprowkey
	 * @param rowkeyfield rowkey的依赖字段，格式为xxx_长度,xxx_长度,xxx_长度
	 * @param paramMap rowkey依赖字段所对应值
	 * @return 返回stopRoekey
	 */
	public static String getCombinationStopRowKey (String rowkeyfield,Map<String, String> paramMap){
		StringBuilder rowKey = new StringBuilder();
		String[] rowkeysplit = rowkeyfield.split(",", -1);
		for (int i = 0; i < rowkeysplit.length; i++) {
			String[] rowkeyInfo = rowkeysplit[i].split("_", -1);
			String key=rowkeyInfo[0].trim();
			int length=Integer.parseInt(rowkeyInfo[1].trim());
			String tmpkey=paramMap.get(key);
			if (VarChecker.isEmpty(tmpkey)) {
				rowKey.append(addNumForFill(paramMap.get(key),length,"0"));
			}else{
				rowKey.append(addNumForFill(paramMap.get(key),length,"9"));
			}

		}
		return rowKey.toString();
	}

	/*
	 * 根据rowkey的信息组成stoprowkey
	 * @param rowkeyfield rowkey的依赖字段，格式为xxx_长度,xxx_长度,xxx_长度
	 * @param paramMap rowkey依赖字段所对应值
	 * @return 返回stopRoekey
	 */
	public static String getCombinationStopRowKeyWithAlphabet (String rowkeyfield,Map<String, String> paramMap ,String fillAlphabet){
		StringBuilder rowKey = new StringBuilder();
		String[] rowkeysplit = rowkeyfield.split(",", -1);
		for (int i = 0; i < rowkeysplit.length; i++) {
			String[] rowkeyInfo = rowkeysplit[i].split("_", -1);
			String key=rowkeyInfo[0].trim();
			int length=Integer.parseInt(rowkeyInfo[1].trim());
			String tmpkey=paramMap.get(key);
			if (VarChecker.isEmpty(tmpkey)) {
				rowKey.append(addNumForFill(paramMap.get(key),length,"0"));
			}else{
				rowKey.append(addNumForFill(paramMap.get(key),length,fillAlphabet));
			}

		}
		return rowKey.toString();
	}

	/*
	 * 将输入的字符串格式化成固定的位数输出(不够前面补fillNum)
	 * @param str 输入的字符串
	 * @param strLength 输出的长度
	 * @param fillNum 填充的数字
	 * @return 格式化后的值
	 */
	public static String addNumForFill(String str, int strLength,String fillNum) {
		try {
			if (str == null) {
				return StringUtils.leftPad("", strLength, fillNum);
			} else {
				return StringUtils.leftPad(str, strLength, fillNum);
			}
		} catch (Exception e) {
			LoggerUtil.error("将输入的字符串格式化成固定的位数输出(不够前面补fillNum)异常:{}",e);
		}
		return "";
	}

	/*
	 * 组成所有区域的访问SCan列表
	 * @param scan hbase 的扫描信息，包含分页大小等
	 * @param partitioncount 分区数的个数
	 * @param partitionlength 分区数的长度，比如128个分区长度为3
	 * @return scan 列表
	 */

	public static Scan[] allAreaSalt(Scan scan, int partitioncount, int partitionlength){
		List<String> splits = getAllRanges(partitioncount, partitionlength);
		Scan[] scans = new Scan[partitioncount];
		try {
			byte[] start = scan.getStartRow();
			byte[] end = scan.getStopRow();
			for (int i = 0; i < partitioncount; i++) {
				scans[i] = new Scan(scan);
				if (start.length != 0) {
					scans[i].setStartRow(concat(splits.get(i).getBytes(), start));
				}
				if (end.length != 0) {
					scans[i].setStopRow(concat(splits.get(i).getBytes(), end));
				}
			}
		} catch (Exception e) {
			LoggerUtil.error("组成所有区域的访问SCan列表异常:{}",e);
		}
		return scans;
	}

	/*
	 * 组成部分区域的访问SCan列表
	 * @param scan hbase 的扫描信息，包含分页大小等
	 * @param partitioncount 分区数的个数
	 * @param partitionlength 分区数的长度，比如128个分区长度为3
	 * @return scan 列表
	 */

	public static Scan[] partAreaSalt(Scan scan, int partitioncount, int partitionlength,String partitionfield,List<Map<String, String>> paramList){
		Scan[] scans =null;
		if (paramList==null || paramList.isEmpty()) {			
			LoggerUtil.error("组成部分区域的访问SCan的参数paramList为空！");
			return new Scan[]{};
		}
		try {
			scans = new Scan[paramList.size()];
			List<String> splits = getPartRangeNo(partitionfield, partitionlength, partitioncount, paramList);
			byte[] start = scan.getStartRow();
			byte[] end = scan.getStopRow();
			for (int i = 0; i < paramList.size(); i++) {
				scans[i] = new Scan(scan);
				if (start.length != 0) {
					scans[i].setStartRow(concat(splits.get(i).getBytes(), start));
				}
				if (end.length != 0) {
					scans[i].setStopRow(concat(splits.get(i).getBytes(), end));
				}
			}
		} catch (Exception e) {
			LoggerUtil.error("组成部分区域的访问SCan列表异常:{}",e);
		}
		return scans;
	}

	/*
	 * 获取所有分区数
	 * @param partitioncount 分区数的个数
	 * @param partitionlength 分区数的长度，比如128个分区长度为3
	 * @return 所有分区的列表
	 */
	public static List<String> getAllRanges(int partitioncount,int partitionlength) {
		List<String> list=new ArrayList<String>();
		for (int i = 0; i < partitioncount; i++) {
			list.add(addNumForFill(String.valueOf(i), partitionlength,"0"));
		}
		return list;
	}


	/*
	 * 拼接分区前缀和rowkey
	 * @param prefix 分区前缀
	 * @param row rowkey
	 * @return 拼接后的值
	 */
	public static byte[] concat(byte[] prefix, byte[] row) {
		if (null == prefix || prefix.length == 0) {
			return row;
		}
		if (null == row || row.length == 0) {
			return prefix;
		}
		byte[] newRow = new byte[row.length + prefix.length];
		if (row.length != 0) {
			System.arraycopy(row, 0, newRow, prefix.length, row.length);
		}
		if (prefix.length != 0) {
			System.arraycopy(prefix, 0, newRow, 0, prefix.length);
		}
		return newRow;
	}

}
