package com.suning.fab.model.domain.amount;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * 金额分（最小辅币单位，简称为分）；<br/>
 * 注意！这个AmountF只用来做外部系统与账务核心之间的金额接口转换；<br/>
	 * 【faepp约定】：<br/>
	 *   外部系统的金额是主币单位的，送给faepp都乘以100;<br/>
	 *   外部系统的金额是辅币单位的则先计算转成主币单位，再乘以100送给faepp;（比如单位为角）<br/>
	 *   返回给外部系统的金额都是主币单位除以100后的数值；<br/>
 * @author 16030888
 * @version 1.0
 * @since 2017-12-06 20:56:47
 */
public class AmountF extends AbstractAmount {	
	private static final long serialVersionUID = 1L;

	/**
	 * 创建指定币种、单位为分的金额类；
	 * @param ccy 币种iso货币代码；
	 * @param val 金额数值，非负整数；
	 */
	public AmountF(String ccy, Long val){
		this(ccy, new BigDecimal(val));
	}
	
	/**
	 * 默认创建人民币币种、单位为分的金额类；
	 * @param val 金额数值，非负整数；
	 */
	public AmountF(Long val){
		this("CNY", new BigDecimal(val));
	}
	
	/**
	 * 创建指定币种、单位为分的金额类；
	 * @param ccy 币种iso货币代码；
	 * @param val 金额数值，非负整数的BigDecimal；
	 */
	public AmountF(String ccy, BigDecimal val){
		super(ccy, Unit.FEN, val);
	}
	
	/**
	 * 对外接口转换用，账务核心内部莫用！为了兼容外部系统的金额处理，按约定统一转换成主币单位金额；<br/>
	 * 【faepp约定】：<br/>
	 *   外部系统的金额是主币单位的，送给faepp都乘以100;<br/>
	 *   外部系统的金额是辅币单位的则先计算转成主币单位，再乘以100送给faepp;（比如单位为角）<br/>
	 *   返回给外部系统的金额都是主币单位除以100后的数值；<br/>
	 * @return 新的AmountY对象；
	 */
	public AmountY convert2Y(){
		return new AmountY(this.ccy.getCurrencyCode(), this.val.divide(new BigDecimal("100")));
	}

	/**
	 * 获取Long类型的金额数值，其单位为分；
	 * @return Long类型的金额数值；
	 */
	public Long intValue() {
		return this.val.longValue();
	}
	
	@Override
	public String toString() {
		DecimalFormat format = new DecimalFormat("0");
		return format.format(val);
	}
}