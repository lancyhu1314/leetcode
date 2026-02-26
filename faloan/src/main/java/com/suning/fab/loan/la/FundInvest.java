package com.suning.fab.loan.la;

import java.io.Serializable;

public class FundInvest  implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7284284248437987314L;
	private String investee;   //接收投资者
	private String investMode; //投放模式：保理、消费贷
	private String channelType;//放款渠道      1-银行   2-易付宝  3-任性贷
	private String fundChannel;//资金通道    sap银行科目编号/易付宝总账科目
	private String outSerialNo;//外部流水单号：银行资金流水号/易付宝交易单号
	
	public FundInvest(String investee, String investMode, String channelType,
			String fundChannel, String outSerialNo) {
		super();
		this.investee = investee;
		this.investMode = investMode;
		this.channelType = channelType;
		this.fundChannel = fundChannel;
		this.outSerialNo = outSerialNo;
	}
	public FundInvest() {
		super();
	}
	/**
	 * @return the investee
	 */
	public String getInvestee() {
		return investee;
	}
	/**
	 * @param investee the investee to set
	 */
	public void setInvestee(String investee) {
		this.investee = investee;
	}
	/**
	 * @return the investMode
	 */
	public String getInvestMode() {
		return investMode;
	}
	/**
	 * @param investMode the investMode to set
	 */
	public void setInvestMode(String investMode) {
		this.investMode = investMode;
	}
	/**
	 * @return the channelType
	 */
	public String getChannelType() {
		return channelType;
	}
	/**
	 * @param channelType the channelType to set
	 */
	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}
	/**
	 * @return the fundChannel
	 */
	public String getFundChannel() {
		return fundChannel;
	}
	/**
	 * @param fundChannel the fundChannel to set
	 */
	public void setFundChannel(String fundChannel) {
		this.fundChannel = fundChannel;
	}
	/**
	 * @return the outSerialNo
	 */
	public String getOutSerialNo() {
		return outSerialNo;
	}
	/**
	 * @param outSerialNo the outSerialNo to set
	 */
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}
	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
