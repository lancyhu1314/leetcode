package com.suning.fab.model.domain.amount;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;


/**
 * 金额基类；
 * @author 16030888
 * @version 1.0
 * @since 2017-12-06 20:53:33
 */
public abstract class AbstractAmount  implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 金额数值
	 */
	protected BigDecimal val = null;
	/**
	 * 货币单位
	 */
	protected final Unit unit;
	/**
	 * 币种信息
	 */
	protected final Currency ccy;

	protected AbstractAmount(String ccy, Unit unit, BigDecimal val){
		//检查金额参数是否合法
		if(null == ccy || ccy.isEmpty()){
			throw new IllegalArgumentException(AmountConstant.ExceptionConstant.CURRENCYILLEGAL);
		}

		this.ccy = Currency.getInstance(ccy);
		this.unit = unit;
		
		//按币种精度四舍五入
		if(Unit.FEN == this.unit){
			this.val = val.setScale(0, BigDecimal.ROUND_HALF_UP);
		}else{
			this.val = val.setScale(this.ccy.getDefaultFractionDigits(), BigDecimal.ROUND_HALF_UP);
		}
		
		//校验金额数值是否越界
		if(this.val.compareTo(AmountConstant.ValueConstant.MAXAMOUNTVALUE) > 0){
			throw new IllegalArgumentException(AmountConstant.ExceptionConstant.EXCEEDMAXAMOUNTVALUE);
		}else if(this.val.compareTo(AmountConstant.ValueConstant.MINAMOUNTVALUE) < 0){
			throw new IllegalArgumentException(AmountConstant.ExceptionConstant.EXCEEDMINAMOUNTVALUE);
		}
	}

	@SuppressWarnings("unused")
	private AbstractAmount(){
		this.unit = null;
		this.ccy = null;
	}

	/**
	 * 返回金额数值，会新建一个BigDecimal实例，不影响金额类本身的数值对象。
	 * @return 新的BigDecimal实例；
	 */
	public BigDecimal getVal() {
		if(Unit.FEN == this.unit){
			return val.add(BigDecimal.ZERO).setScale(0, BigDecimal.ROUND_HALF_UP);
		}else{
			return val.add(BigDecimal.ZERO).setScale(this.ccy.getDefaultFractionDigits(), BigDecimal.ROUND_HALF_UP);
		}
	}
	
	/**
	 * 获取金额币种信息的iso货币代码；
	 * @return 字符串类型的iso货币代码；
	 */
	public String getCcy(){
		return ccy.getCurrencyCode();
	}
	
	/**
	 * 判断金额是否等于0
	 * @return true -- 等于0；false -- 不等于0;
	 */
	public boolean isZero()
	{
		return (0 == val.signum())?true:false;
	}

	/**
	 * 判断金额是否负值；
	 * @return true -- 负值；false -- 非负值；
	 */
	public boolean isNegative()
	{
		return (-1 == val.signum())?true:false;
	}

	/**
	 * 判断金额是否正值；
	 * @return true -- 正值；false -- 非正值；
	 */
	public boolean isPositive()
	{
		return (1 == val.signum())?true:false;
	}

	@Override
	public String toString() {
		return val.toString();
	}
}