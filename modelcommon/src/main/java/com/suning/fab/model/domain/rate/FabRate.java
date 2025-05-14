package com.suning.fab.model.domain.rate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * 利率类
 * @author 13121376
 *
 */
public class FabRate implements Serializable{
	private static final long serialVersionUID = 1L;
	
	protected BigDecimal rate;
	protected Integer daysPerYear = 360;
	protected Integer daysPerMonth = 30;
	protected String rateUnit = "Y";
	protected Integer precision = 20;

	public enum RateUnit {
		YEAR("Y"), MONTH("M"), DAY("D");
		private final String value;

		RateUnit(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}

	public FabRate(Double val, Integer daysPerYear, Integer daysPerMonth,
			String rateType) {
		super();
		this.rate = new BigDecimal(val);
		this.daysPerYear = daysPerYear;
		this.daysPerMonth = daysPerMonth;
		this.rateUnit = rateType;
	}
	public FabRate(Double val) {
		super();
		this.rate = new BigDecimal(val);
	}

	public FabRate() {
		super();
	}

	public FabRate(FabRate x) {
		super();
		this.rate = x.rate;
		this.daysPerMonth = x.daysPerMonth;
		this.daysPerYear = x.daysPerYear;
		this.rateUnit = x.rateUnit;
		this.precision = x.precision;
	}


	public BigDecimal getYearRate() {
		if (rateUnit.equals(RateUnit.YEAR.getValue()))
		{
			return rate;
		}
		else if (rateUnit.equals(RateUnit.MONTH.getValue()))
		{
			return rate.multiply(new BigDecimal(12));
		}
		else if (rateUnit.equals(RateUnit.DAY.getValue()))
		{
			return rate.multiply(new BigDecimal(daysPerYear));
		}
		return null;
	}

	public BigDecimal getMonthRate() {
		if (rateUnit.equals(RateUnit.YEAR.getValue()))
		{	
			return rate.divide(new BigDecimal(12), precision ,BigDecimal.ROUND_HALF_UP); 
		}
		else if (rateUnit.equals(RateUnit.MONTH.getValue()))
		{
			return rate;
		}
		else if (rateUnit.equals(RateUnit.DAY.getValue()))
		{
			return rate.multiply(new BigDecimal(daysPerMonth)); 
		}
		return null;
	}

	public BigDecimal getDayRate() {
		if (rateUnit.equals(RateUnit.YEAR.getValue()))
		{
			return rate.divide(new BigDecimal(daysPerYear), precision ,BigDecimal.ROUND_HALF_UP);
		}
		else if (rateUnit.equals(RateUnit.MONTH.getValue()))
		{
			return rate.divide(new BigDecimal(daysPerMonth), precision ,BigDecimal.ROUND_HALF_UP);
		}
		else if (rateUnit.equals(RateUnit.DAY.getValue()))
		{
			return rate;
		}
		return null;
	}

	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("###.#########");
		return df.format(rate);
	}

	public BigDecimal getVal() {
		return rate;
	}
	
	public Integer getDaysPerYear() {
		return daysPerYear;
	}

	public void setDaysPerYear(Integer daysPerYear) {
		this.daysPerYear = daysPerYear;
	}

	public Integer getDaysPerMonth() {
		return daysPerMonth;
	}

	public void setDaysPerMonth(Integer daysPerMonth) {
		this.daysPerMonth = daysPerMonth;
	}

	public String getRateUnit() {
		return rateUnit;
	}

	public void setRateUnit(String rateUnit) {
		this.rateUnit = rateUnit;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	
}