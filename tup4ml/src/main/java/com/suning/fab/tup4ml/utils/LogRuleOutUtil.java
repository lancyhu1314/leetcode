
package com.suning.fab.tup4ml.utils;

import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;

/**
 * 日志排除工具类；
 * @author 16092019
 *
 */
public abstract class LogRuleOutUtil {
	private LogRuleOutUtil() {
		throw new IllegalStateException("LogRuleOutUtil class");
	}

	private static  SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
	
	public static void setLogFilter(String ruleOut){
		LoggerUtil.info("Load Log RuleOut :{}  Start......",ruleOut);
		if(ruleOut==null||"".equals(ruleOut)) {
			filter.getExcludes().clear();			
		}else{
			filter.getExcludes().clear();
			String[] rules = ruleOut.split("\\,", -1);
			for(String rule:rules) {
				filter.getExcludes().add(StringUtil.parseString(rule));
			}
		}
	}
	
	public static SimplePropertyPreFilter getFilter() {
		return filter;
	}

	public static void setFilter(SimplePropertyPreFilter filter) {
		LogRuleOutUtil.filter = filter;
	}

	
	
}
