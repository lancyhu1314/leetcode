package com.suning.fab.loan.utils;

import java.math.BigDecimal;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.utils.PropertyUtil;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 计算税金
 *
 * @param tranAmt 计税金额 
 *
 * @return FabAmount 税金
 *
 * @exception
 */
public class TaxUtil {
	private TaxUtil(){
		//nothing to do
	}
	
	public static FabAmount calcVAT(FabAmount tranAmt)
	{
		BigDecimal rate = new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06"));
		BigDecimal ratefactor = rate.add(new BigDecimal(1));
		BigDecimal taxAmt = BigDecimal.valueOf(tranAmt.getVal().doubleValue()).divide(ratefactor, 9, BigDecimal.ROUND_HALF_UP).multiply(rate)
				.setScale(2,BigDecimal.ROUND_HALF_UP );
		FabAmount tmpAmt = new FabAmount();
		tmpAmt.selfAdd(taxAmt.doubleValue());
		return tmpAmt;
	}
}
