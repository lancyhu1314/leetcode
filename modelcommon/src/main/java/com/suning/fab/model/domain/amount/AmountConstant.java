package com.suning.fab.model.domain.amount;

import java.math.BigDecimal;

/**
 * 金额相关常量定义；
 * @author 16030888
 *
 */
public class AmountConstant {
	private AmountConstant() {}
	public static final class ExceptionConstant{
		private ExceptionConstant() {}
		public static final String CURRENCYILLEGAL = "币种非法";
		public static final String DIFFERENTCURRENCY = "操作的两个金额其币种不同";
		public static final String EXCEEDMAXAMOUNTVALUE = "金额数值大于上限最大值";
		public static final String EXCEEDMINAMOUNTVALUE = "金额数值小于下限最小值";
	}
	public static final class ValueConstant{
		private ValueConstant() {}

		//DECIMAL(17,2)的最大值
		public static final BigDecimal MAXAMOUNTVALUE = new BigDecimal("999999999999999.99");

		//DECIMAL(17,2)的最小值
		public static final BigDecimal MINAMOUNTVALUE = new BigDecimal("-999999999999999.99");

	}
}
