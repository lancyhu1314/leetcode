package com.suning.fab.model.domain.amount;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * 金额元（主币单位，简称为元）；
 * @author 16030888
 * @version 1.0
 * @since 2017-12-06 20:57:08
 */
public class AmountY extends AbstractAmount {
	private static final long serialVersionUID = 1L;

	/**
	 * 创建指定币种、单位为元的金额类；
	 * @param ccy 币种iso货币代码；
	 * @param val 金额数值，字符串格式，要求为精度跟币种ccy匹配的浮点数；
	 */
	public AmountY(String ccy, String val){
		this(ccy, new BigDecimal(val));
	}
	
	/**
	 * 默认创建人民币币种、单位为元的金额类；
	 * @param val 金额数值，字符串格式，要求为精度跟币种ccy匹配的浮点数；
	 */
	public AmountY(String val){
		this("CNY", new BigDecimal(val));
	}

	/**
	 * 创建指定币种、单位为元的金额类；
	 * @param ccy 币种iso货币代码；
	 * @param val 金额数值，要求为精度跟币种ccy匹配的浮点数；
	 * 
	 */
	@Deprecated
	public AmountY(String ccy, Double val){
		this(ccy, new BigDecimal(val));
	}
	
	/**
	 * 默认创建人民币币种、单位为元的金额类；
	 * @param val 金额数值，要求为精度跟币种ccy匹配的浮点数；
	 */
	@Deprecated
	public AmountY(Double val){
		this("CNY", new BigDecimal(val));
	}

	/**
	 * 创建指定人民币币种、单位为元的金额类；
	 * @param val 金额数值，要求为精度跟币种ccy匹配的BigDecimal；
	 */
	public AmountY(BigDecimal val){
		this("CNY", val);
	}
	
	/**
	 * 创建指定币种、单位为元的金额类；
	 * @param ccy 币种iso货币代码；
	 * @param val 金额数值，要求为精度跟币种ccy匹配的BigDecimal；
	 */
	public AmountY(String ccy, BigDecimal val){
		super(ccy, Unit.YUAN, val);
	}
	
	/**
	 * 比较两个单位都为元的金额的大小，如果币种不同则抛出异常
	 * @param other 另一个单位也为元的金额
	 * @return 返回-1则本金额小于other；返回0则本金额等于other；返回1则本金额大于other；
	 */
	public int compareTo(AmountY other) {
		if(0 != ccy.getCurrencyCode().compareToIgnoreCase(other.ccy.getCurrencyCode())){
			throw new IllegalArgumentException(AmountConstant.ExceptionConstant.DIFFERENTCURRENCY);
		}
		return val.compareTo(other.val) ;
	}

	/**
	 * 金额元的加法，返回新的金额元；
	 * @param other 加数；
	 * @return 如果被加数和加数之间币种不一样则抛出异常；否则用新的金额元返回加法操作的结果；
	 */
	public AmountY add(AmountY other){
		if(0 != ccy.getCurrencyCode().compareToIgnoreCase(other.ccy.getCurrencyCode())){
			throw new IllegalArgumentException(AmountConstant.ExceptionConstant.DIFFERENTCURRENCY);
		}
		return new AmountY(this.ccy.getCurrencyCode(), this.val.add(other.val));
	}

	/**
	 * 对金额元自身做加法，返回该金额元；
	 * @param other 加数；
	 * @return 如果被加数和加数之间币种不一样则抛出异常；否则对该金额元做一次加法操作；
	 */
	public AmountY selfAdd(AmountY other){
		if(0 != ccy.getCurrencyCode().compareToIgnoreCase(other.ccy.getCurrencyCode())){
			throw new IllegalArgumentException(AmountConstant.ExceptionConstant.DIFFERENTCURRENCY);
		}
		this.val = this.val.add(other.val).setScale(this.ccy.getDefaultFractionDigits(), BigDecimal.ROUND_HALF_UP);;
		return this;
	}

	/**
	 * 金额元的减法，返回新的金额元；
	 * @param other 减数；
	 * @return 如果被减数和减数之间币种不一样则抛出异常；否则用新的金额元返回减法操作的结果；
	 */
	public AmountY sub(AmountY other){
		if(0 != ccy.getCurrencyCode().compareToIgnoreCase(other.ccy.getCurrencyCode())){
			throw new IllegalArgumentException(AmountConstant.ExceptionConstant.DIFFERENTCURRENCY);
		}
		return new AmountY(this.ccy.getCurrencyCode(), this.val.subtract(other.val));
	}

	/**
	 * 对金额元自身做减法，返回该金额元；
	 * @param other 减数；
	 * @return 如果被减数和减数之间币种不一样则抛出异常；否则对该金额元做一次减法操作；
	 */
	public AmountY selfSub(AmountY other){
		if(0 != ccy.getCurrencyCode().compareToIgnoreCase(other.ccy.getCurrencyCode())){
			throw new IllegalArgumentException(AmountConstant.ExceptionConstant.DIFFERENTCURRENCY);
		}
		this.val = this.val.subtract(other.val).setScale(this.ccy.getDefaultFractionDigits(), BigDecimal.ROUND_HALF_UP);;
		return this;
	}

	/**
	 * 转换成同币种但单位为最小辅币的金额；<br/>
	 * 【faepp约定】：<br/>
	 *   外部系统的金额是主币单位的，送给faepp都乘以100;<br/>
	 *   外部系统的金额是辅币单位的则先计算转成主币单位，再乘以100送给faepp;（比如单位为角）<br/>
	 *   返回给外部系统的金额都是主币单位除以100后的数值；<br/>
	 * @return 新的AmountF对象；
	 */
	public AmountF convert2F(){
		return new AmountF(this.ccy.getCurrencyCode(), this.val.multiply(new BigDecimal("100")));
	}

	/**
	 * 取反操作，对金额数值取相反数；
	 * @return 新的AmountY实例；
	 */
	public AmountY negate() {
		return new AmountY(this.ccy.getCurrencyCode(), val.negate());
	}

	@Override
	public String toString() {
		int len = this.ccy.getDefaultFractionDigits();
		StringBuilder bld = new StringBuilder("0");
		if(len > 0){
			bld.append("."); 
			for(int i = 0; i < len; ++i){
				bld.append("0"); 
			}
		}
		DecimalFormat format = new DecimalFormat(bld.toString());
		return format.format(val);
	}
	
	/**
	 * 转成最小辅币单位的金额数值，即金额分；
	 * @return 返回转换后的字符串数值；
	 */
	public String toStringF() {
		int len = this.ccy.getDefaultFractionDigits();
		Long multiFactor = 1L;
		for(int i = 0; i < len; ++i){
			multiFactor *= 10L;
		}
		BigDecimal newVal = val.multiply(new BigDecimal(multiFactor));
		DecimalFormat format = new DecimalFormat("0");
		return format.format(newVal);
	}
}