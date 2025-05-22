/**
 * @author 14050269 Howard
 * @version 创建时间：2016年8月10日 下午3:02:36
 * 类说明
 */
package com.suning.fab.loan.accounting;
public class LnsEventMsg {
	private String s_TermDate = "";
	private Integer s_SerialNo = 0;
	private Integer s_LineNo = 0;
	private String s_EvtCode = "";
	private String s_Brc = "";
	private String s_Teller = "";
	private String s_Ccy = "";// 币种
	private String s_TranCode = "";
	private String s_ChannelId = "";
	private String s_TermTime = "";
	private String s_BriefCode = "";// 摘要代码
	private String s_TranBrief = "";// 摘要描述

	private String userno;// 会员编号
	private String accsrccod; // 账户类型
	private String acctseq; // 账户序号
	private String merchantno; // 商户编号
	private String custtype; // 客户类型
	private String prdcode; // 账户产品代码
	private String touserno; // 收款方会员编号
	private String toaccsrccod; // 收款方账户类型
	private String toacctseq; // 收款方账户序号
	private String tomerchantno; // 收款方商户编号
	private String tocusttype; // 收款方客户类型
	private String toprdcode; // 收款方账户产品代码
	private Double tranamt; // 交易金额
	private Double amt1; // 金额1
	private Double amt2; // 金额2
	private Double amt3; // 金额3
	private Double amt4; // 金额4
	private Double amt5; // 金额5
	private String trancode; // 交易码
	private String subtrancode; // 子交易代码
	private String bankchannel; // 银行渠道编号
	private String sapid; // 支付方式
	private String paytypecode; // 支付类型
	private String paychannelcode; // 支付渠道
	private String providercode; // 资金提供商
	private String otherfeetype; // 其他收入类型
	private String chtype; // 收单类型
	private String productcode; // 产品编码
	private String tunnelData; // 隧道字段
	private String billno; // 付款单号
	private String reserv1; // 预留项1
	private String reserv2; // 预留项2
	private String reserv3; // 预留项3
	private String reserv4; // 预留项4
	private String reserv5; // 预留项5
	private String reserv6; // 预留项6
	private String reserv7; // 预留项7
	private String reserv8; // 预留项8
	private String reserv9; // 预留项9
	private String reserv10; // 预留项10
	private String chserialNo; // 关联收单(批次)流水号
	private String chrelSerNo; // 收单(批次)流水号
	/**
	 * @return the s_TermDate
	 */
	public String getS_TermDate() {
		return s_TermDate;
	}
	/**
	 * @param s_TermDate the s_TermDate to set
	 */
	public void setS_TermDate(String s_TermDate) {
		this.s_TermDate = s_TermDate;
	}
	/**
	 * @return the s_SerialNo
	 */
	public Integer getS_SerialNo() {
		return s_SerialNo;
	}
	/**
	 * @param s_SerialNo the s_SerialNo to set
	 */
	public void setS_SerialNo(Integer s_SerialNo) {
		this.s_SerialNo = s_SerialNo;
	}
	/**
	 * @return the s_LineNo
	 */
	public Integer getS_LineNo() {
		return s_LineNo;
	}
	/**
	 * @param s_LineNo the s_LineNo to set
	 */
	public void setS_LineNo(Integer s_LineNo) {
		this.s_LineNo = s_LineNo;
	}
	/**
	 * @return the s_EvtCode
	 */
	public String getS_EvtCode() {
		return s_EvtCode;
	}
	/**
	 * @param s_EvtCode the s_EvtCode to set
	 */
	public void setS_EvtCode(String s_EvtCode) {
		this.s_EvtCode = s_EvtCode;
	}
	/**
	 * @return the s_Brc
	 */
	public String getS_Brc() {
		return s_Brc;
	}
	/**
	 * @param s_Brc the s_Brc to set
	 */
	public void setS_Brc(String s_Brc) {
		this.s_Brc = s_Brc;
	}
	/**
	 * @return the s_Teller
	 */
	public String getS_Teller() {
		return s_Teller;
	}
	/**
	 * @param s_Teller the s_Teller to set
	 */
	public void setS_Teller(String s_Teller) {
		this.s_Teller = s_Teller;
	}
	/**
	 * @return the s_Ccy
	 */
	public String getS_Ccy() {
		return s_Ccy;
	}
	/**
	 * @param s_Ccy the s_Ccy to set
	 */
	public void setS_Ccy(String s_Ccy) {
		this.s_Ccy = s_Ccy;
	}
	/**
	 * @return the s_TranCode
	 */
	public String getS_TranCode() {
		return s_TranCode;
	}
	/**
	 * @param s_TranCode the s_TranCode to set
	 */
	public void setS_TranCode(String s_TranCode) {
		this.s_TranCode = s_TranCode;
	}
	/**
	 * @return the s_ChannelId
	 */
	public String getS_ChannelId() {
		return s_ChannelId;
	}
	/**
	 * @param s_ChannelId the s_ChannelId to set
	 */
	public void setS_ChannelId(String s_ChannelId) {
		this.s_ChannelId = s_ChannelId;
	}
	/**
	 * @return the s_TermTime
	 */
	public String getS_TermTime() {
		return s_TermTime;
	}
	/**
	 * @param s_TermTime the s_TermTime to set
	 */
	public void setS_TermTime(String s_TermTime) {
		this.s_TermTime = s_TermTime;
	}
	/**
	 * @return the s_BriefCode
	 */
	public String getS_BriefCode() {
		return s_BriefCode;
	}
	/**
	 * @param s_BriefCode the s_BriefCode to set
	 */
	public void setS_BriefCode(String s_BriefCode) {
		this.s_BriefCode = s_BriefCode;
	}
	/**
	 * @return the s_TranBrief
	 */
	public String getS_TranBrief() {
		return s_TranBrief;
	}
	/**
	 * @param s_TranBrief the s_TranBrief to set
	 */
	public void setS_TranBrief(String s_TranBrief) {
		this.s_TranBrief = s_TranBrief;
	}
	/**
	 * @return the userno
	 */
	public String getUserno() {
		return userno;
	}
	/**
	 * @param userno the userno to set
	 */
	public void setUserno(String userno) {
		this.userno = userno;
	}
	/**
	 * @return the accsrccod
	 */
	public String getAccsrccod() {
		return accsrccod;
	}
	/**
	 * @param accsrccod the accsrccod to set
	 */
	public void setAccsrccod(String accsrccod) {
		this.accsrccod = accsrccod;
	}
	/**
	 * @return the acctseq
	 */
	public String getAcctseq() {
		return acctseq;
	}
	/**
	 * @param acctseq the acctseq to set
	 */
	public void setAcctseq(String acctseq) {
		this.acctseq = acctseq;
	}
	/**
	 * @return the merchantno
	 */
	public String getMerchantno() {
		return merchantno;
	}
	/**
	 * @param merchantno the merchantno to set
	 */
	public void setMerchantno(String merchantno) {
		this.merchantno = merchantno;
	}
	/**
	 * @return the custtype
	 */
	public String getCusttype() {
		return custtype;
	}
	/**
	 * @param custtype the custtype to set
	 */
	public void setCusttype(String custtype) {
		this.custtype = custtype;
	}
	/**
	 * @return the prdcode
	 */
	public String getPrdcode() {
		return prdcode;
	}
	/**
	 * @param prdcode the prdcode to set
	 */
	public void setPrdcode(String prdcode) {
		this.prdcode = prdcode;
	}
	/**
	 * @return the touserno
	 */
	public String getTouserno() {
		return touserno;
	}
	/**
	 * @param touserno the touserno to set
	 */
	public void setTouserno(String touserno) {
		this.touserno = touserno;
	}
	/**
	 * @return the toaccsrccod
	 */
	public String getToaccsrccod() {
		return toaccsrccod;
	}
	/**
	 * @param toaccsrccod the toaccsrccod to set
	 */
	public void setToaccsrccod(String toaccsrccod) {
		this.toaccsrccod = toaccsrccod;
	}
	/**
	 * @return the toacctseq
	 */
	public String getToacctseq() {
		return toacctseq;
	}
	/**
	 * @param toacctseq the toacctseq to set
	 */
	public void setToacctseq(String toacctseq) {
		this.toacctseq = toacctseq;
	}
	/**
	 * @return the tomerchantno
	 */
	public String getTomerchantno() {
		return tomerchantno;
	}
	/**
	 * @param tomerchantno the tomerchantno to set
	 */
	public void setTomerchantno(String tomerchantno) {
		this.tomerchantno = tomerchantno;
	}
	/**
	 * @return the tocusttype
	 */
	public String getTocusttype() {
		return tocusttype;
	}
	/**
	 * @param tocusttype the tocusttype to set
	 */
	public void setTocusttype(String tocusttype) {
		this.tocusttype = tocusttype;
	}
	/**
	 * @return the toprdcode
	 */
	public String getToprdcode() {
		return toprdcode;
	}
	/**
	 * @param toprdcode the toprdcode to set
	 */
	public void setToprdcode(String toprdcode) {
		this.toprdcode = toprdcode;
	}
	/**
	 * @return the tranamt
	 */
	public Double getTranamt() {
		return tranamt;
	}
	/**
	 * @param tranamt the tranamt to set
	 */
	public void setTranamt(Double tranamt) {
		this.tranamt = tranamt;
	}
	/**
	 * @return the amt1
	 */
	public Double getAmt1() {
		return amt1;
	}
	/**
	 * @param amt1 the amt1 to set
	 */
	public void setAmt1(Double amt1) {
		this.amt1 = amt1;
	}
	/**
	 * @return the amt2
	 */
	public Double getAmt2() {
		return amt2;
	}
	/**
	 * @param amt2 the amt2 to set
	 */
	public void setAmt2(Double amt2) {
		this.amt2 = amt2;
	}
	/**
	 * @return the amt3
	 */
	public Double getAmt3() {
		return amt3;
	}
	/**
	 * @param amt3 the amt3 to set
	 */
	public void setAmt3(Double amt3) {
		this.amt3 = amt3;
	}
	/**
	 * @return the amt4
	 */
	public Double getAmt4() {
		return amt4;
	}
	/**
	 * @param amt4 the amt4 to set
	 */
	public void setAmt4(Double amt4) {
		this.amt4 = amt4;
	}
	/**
	 * @return the amt5
	 */
	public Double getAmt5() {
		return amt5;
	}
	/**
	 * @param amt5 the amt5 to set
	 */
	public void setAmt5(Double amt5) {
		this.amt5 = amt5;
	}
	/**
	 * @return the trancode
	 */
	public String getTrancode() {
		return trancode;
	}
	/**
	 * @param trancode the trancode to set
	 */
	public void setTrancode(String trancode) {
		this.trancode = trancode;
	}
	/**
	 * @return the subtrancode
	 */
	public String getSubtrancode() {
		return subtrancode;
	}
	/**
	 * @param subtrancode the subtrancode to set
	 */
	public void setSubtrancode(String subtrancode) {
		this.subtrancode = subtrancode;
	}
	/**
	 * @return the bankchannel
	 */
	public String getBankchannel() {
		return bankchannel;
	}
	/**
	 * @param bankchannel the bankchannel to set
	 */
	public void setBankchannel(String bankchannel) {
		this.bankchannel = bankchannel;
	}
	/**
	 * @return the sapid
	 */
	public String getSapid() {
		return sapid;
	}
	/**
	 * @param sapid the sapid to set
	 */
	public void setSapid(String sapid) {
		this.sapid = sapid;
	}
	/**
	 * @return the paytypecode
	 */
	public String getPaytypecode() {
		return paytypecode;
	}
	/**
	 * @param paytypecode the paytypecode to set
	 */
	public void setPaytypecode(String paytypecode) {
		this.paytypecode = paytypecode;
	}
	/**
	 * @return the paychannelcode
	 */
	public String getPaychannelcode() {
		return paychannelcode;
	}
	/**
	 * @param paychannelcode the paychannelcode to set
	 */
	public void setPaychannelcode(String paychannelcode) {
		this.paychannelcode = paychannelcode;
	}
	/**
	 * @return the providercode
	 */
	public String getProvidercode() {
		return providercode;
	}
	/**
	 * @param providercode the providercode to set
	 */
	public void setProvidercode(String providercode) {
		this.providercode = providercode;
	}
	/**
	 * @return the otherfeetype
	 */
	public String getOtherfeetype() {
		return otherfeetype;
	}
	/**
	 * @param otherfeetype the otherfeetype to set
	 */
	public void setOtherfeetype(String otherfeetype) {
		this.otherfeetype = otherfeetype;
	}
	/**
	 * @return the chtype
	 */
	public String getChtype() {
		return chtype;
	}
	/**
	 * @param chtype the chtype to set
	 */
	public void setChtype(String chtype) {
		this.chtype = chtype;
	}
	/**
	 * @return the productcode
	 */
	public String getProductcode() {
		return productcode;
	}
	/**
	 * @param productcode the productcode to set
	 */
	public void setProductcode(String productcode) {
		this.productcode = productcode;
	}
	/**
	 * @return the tunnelData
	 */
	public String getTunnelData() {
		return tunnelData;
	}
	/**
	 * @param tunnelData the tunnelData to set
	 */
	public void setTunnelData(String tunnelData) {
		this.tunnelData = tunnelData;
	}
	/**
	 * @return the billno
	 */
	public String getBillno() {
		return billno;
	}
	/**
	 * @param billno the billno to set
	 */
	public void setBillno(String billno) {
		this.billno = billno;
	}
	/**
	 * @return the reserv1
	 */
	public String getReserv1() {
		return reserv1;
	}
	/**
	 * @param reserv1 the reserv1 to set
	 */
	public void setReserv1(String reserv1) {
		this.reserv1 = reserv1;
	}
	/**
	 * @return the reserv2
	 */
	public String getReserv2() {
		return reserv2;
	}
	/**
	 * @param reserv2 the reserv2 to set
	 */
	public void setReserv2(String reserv2) {
		this.reserv2 = reserv2;
	}
	/**
	 * @return the reserv3
	 */
	public String getReserv3() {
		return reserv3;
	}
	/**
	 * @param reserv3 the reserv3 to set
	 */
	public void setReserv3(String reserv3) {
		this.reserv3 = reserv3;
	}
	/**
	 * @return the reserv4
	 */
	public String getReserv4() {
		return reserv4;
	}
	/**
	 * @param reserv4 the reserv4 to set
	 */
	public void setReserv4(String reserv4) {
		this.reserv4 = reserv4;
	}
	/**
	 * @return the reserv5
	 */
	public String getReserv5() {
		return reserv5;
	}
	/**
	 * @param reserv5 the reserv5 to set
	 */
	public void setReserv5(String reserv5) {
		this.reserv5 = reserv5;
	}
	/**
	 * @return the reserv6
	 */
	public String getReserv6() {
		return reserv6;
	}
	/**
	 * @param reserv6 the reserv6 to set
	 */
	public void setReserv6(String reserv6) {
		this.reserv6 = reserv6;
	}
	/**
	 * @return the reserv7
	 */
	public String getReserv7() {
		return reserv7;
	}
	/**
	 * @param reserv7 the reserv7 to set
	 */
	public void setReserv7(String reserv7) {
		this.reserv7 = reserv7;
	}
	/**
	 * @return the reserv8
	 */
	public String getReserv8() {
		return reserv8;
	}
	/**
	 * @param reserv8 the reserv8 to set
	 */
	public void setReserv8(String reserv8) {
		this.reserv8 = reserv8;
	}
	/**
	 * @return the reserv9
	 */
	public String getReserv9() {
		return reserv9;
	}
	/**
	 * @param reserv9 the reserv9 to set
	 */
	public void setReserv9(String reserv9) {
		this.reserv9 = reserv9;
	}
	/**
	 * @return the reserv10
	 */
	public String getReserv10() {
		return reserv10;
	}
	/**
	 * @param reserv10 the reserv10 to set
	 */
	public void setReserv10(String reserv10) {
		this.reserv10 = reserv10;
	}
	/**
	 * @return the chserialNo
	 */
	public String getChserialNo() {
		return chserialNo;
	}
	/**
	 * @param chserialNo the chserialNo to set
	 */
	public void setChserialNo(String chserialNo) {
		this.chserialNo = chserialNo;
	}
	/**
	 * @return the chrelSerNo
	 */
	public String getChrelSerNo() {
		return chrelSerNo;
	}
	/**
	 * @param chrelSerNo the chrelSerNo to set
	 */
	public void setChrelSerNo(String chrelSerNo) {
		this.chrelSerNo = chrelSerNo;
	}
	
	
}
