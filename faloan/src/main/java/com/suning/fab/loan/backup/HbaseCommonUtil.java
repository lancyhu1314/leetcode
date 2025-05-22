package com.suning.fab.loan.backup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Scan;

import com.suning.fab.tup4j.utils.LoggerUtil;

public class HbaseCommonUtil {
	
	private HbaseCommonUtil(){
		super();
	}
	/*
	 * 将输入的字符串格式化成固定的位数输出(不够前面补fillNum)
	 * @str 输入的字符串
	 * @strLength 输出的长度
	 * @fillNum 填充的数字
	 */
	public static String addZeroForNum(String str, int strLength,String fillNum) {
		try {
			if (null == str) {
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
	 * 组成所有区域的访问SCan
	 */
	public static Scan[] salt(Scan scan, int ranges) throws IOException {
		List<String> splits = getAllRanges(ranges);
		Scan[] scans = null;
		try {
			scans = new Scan[ranges];
			byte[] start = scan.getStartRow();
			byte[] end = scan.getStopRow();
			for (int i = 0; i < ranges; i++) {
				scans[i] = new Scan(scan);
				scans[i].setStartRow(concat(splits.get(i).getBytes(), start));
				if (end.length!=0) {
					scans[i].setStopRow(concat(splits.get(i).getBytes(), end));
				}
				LoggerUtil.info("输出扫描的区域,StartRow:{},StopRow:{}",scans[i].getStartRow(),scans[i].getStopRow());
			}
		} catch (Exception e) {
			LoggerUtil.error("组成所有区域的访问SCan异常:{}",e);
		}
		return scans;
	}

	/*
	 * 获取所有分区数
	 */
	public static List<String> getAllRanges(int ranges) {
		List<String> list=new ArrayList<String>();
		try {
			int  length=String.valueOf(ranges).length();
			for (int i = 0; i < ranges; i++) {
				list.add(addZeroForNum(String.valueOf(i), length,"0"));
			}
			return list;
		} catch (Exception e) {
			LoggerUtil.error("获取所有分区数异常:{}",e);
		}
		return list;
	}

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
