package com.suning.fab.loan.account;

import com.suning.fab.tup4j.amount.FabAmount;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉:接口幂等登记薄表查询类，用于返回报文使用
 *
 * @author 18049705
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsInterfaceQuery {
	private String trandate	;  // 交易日期
	private String serialno	;  // 幂等流水号
	private String accdate	;  // 账务日期
	private String serseqno	;  // 系统流水号
	private String brc	    ;  // 网点
	private String trancode	;  // 交易码
	private String acctname	;  // 户名
	private String userno	;  // 预收账号
	private String acctno	;  // 本金账号
	private FabAmount tranamt	;  // 开户/冲销金额
	private FabAmount sumrint	;  // 还款利息
	private FabAmount sumramt	;  // 还款本金
	private FabAmount sumrfint	;  // 还款罚息
	private FabAmount sumdelint;  // 减免利息
	private FabAmount sumdelfint; // 减免罚息
	private String acctflag	;  // 结清标志 3-结清
	private String timestamp;  // 时间戳
	private String reserv1	;  // 备用1
	private String reserv2	;  // 备用2
	private String reserv3	;  // 备用3
	private String reserv4	;  // 备用4
	private String reserv5	;  // 备用5  存放资金channel
	private String reserv6	;  // 备用6
	private String orgid	;  // 门店公司商户号
	private String billno	;  // 结算单号
	private String bankno	;  // 银行流水号/易付宝单号/POS单号
	private String magacct	;  // 借据号
	/**
	 * @return the trandate
	 */
	public String getTrandate() {
		return trandate;
	}
	/**
	 * @param trandate the trandate to set
	 */
	public void setTrandate(String trandate) {
		this.trandate = trandate;
	}
	/**
	 * @return the serialno
	 */
	public String getSerialno() {
		return serialno;
	}
	/**
	 * @param serialno the serialno to set
	 */
	public void setSerialno(String serialno) {
		this.serialno = serialno;
	}
	/**
	 * @return the accdate
	 */
	public String getAccdate() {
		return accdate;
	}
	/**
	 * @param accdate the accdate to set
	 */
	public void setAccdate(String accdate) {
		this.accdate = accdate;
	}
	/**
	 * @return the serseqno
	 */
	public String getSerseqno() {
		return serseqno;
	}
	/**
	 * @param serseqno the serseqno to set
	 */
	public void setSerseqno(String serseqno) {
		this.serseqno = serseqno;
	}
	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}
	/**
	 * @param brc the brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
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
	 * @return the acctname
	 */
	public String getAcctname() {
		return acctname;
	}
	/**
	 * @param acctname the acctname to set
	 */
	public void setAcctname(String acctname) {
		this.acctname = acctname;
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
	 * @return the acctno
	 */
	public String getAcctno() {
		return acctno;
	}
	/**
	 * @param acctno the acctno to set
	 */
	public void setAcctno(String acctno) {
		this.acctno = acctno;
	}
	/**
	 * @return the tranamt
	 */
	public FabAmount getTranamt() {
		return tranamt;
	}
	/**
	 * @param tranamt the tranamt to set
	 */
	public void setTranamt(FabAmount tranamt) {
		this.tranamt = tranamt;
	}
	/**
	 * @return the sumrint
	 */
	public FabAmount getSumrint() {
		return sumrint;
	}
	/**
	 * @param sumrint the sumrint to set
	 */
	public void setSumrint(FabAmount sumrint) {
		this.sumrint = sumrint;
	}
	/**
	 * @return the sumramt
	 */
	public FabAmount getSumramt() {
		return sumramt;
	}
	/**
	 * @param sumramt the sumramt to set
	 */
	public void setSumramt(FabAmount sumramt) {
		this.sumramt = sumramt;
	}
	/**
	 * @return the sumrfint
	 */
	public FabAmount getSumrfint() {
		return sumrfint;
	}
	/**
	 * @param sumrfint the sumrfint to set
	 */
	public void setSumrfint(FabAmount sumrfint) {
		this.sumrfint = sumrfint;
	}
	/**
	 * @return the sumdelint
	 */
	public FabAmount getSumdelint() {
		return sumdelint;
	}
	/**
	 * @param sumdelint the sumdelint to set
	 */
	public void setSumdelint(FabAmount sumdelint) {
		this.sumdelint = sumdelint;
	}
	/**
	 * @return the sumdelfint
	 */
	public FabAmount getSumdelfint() {
		return sumdelfint;
	}
	/**
	 * @param sumdelfint the sumdelfint to set
	 */
	public void setSumdelfint(FabAmount sumdelfint) {
		this.sumdelfint = sumdelfint;
	}
	/**
	 * @return the acctflag
	 */
	public String getAcctflag() {
		return acctflag;
	}
	/**
	 * @param acctflag the acctflag to set
	 */
	public void setAcctflag(String acctflag) {
		this.acctflag = acctflag;
	}
	/**
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
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
	 * @return the orgid
	 */
	public String getOrgid() {
		return orgid;
	}
	/**
	 * @param orgid the orgid to set
	 */
	public void setOrgid(String orgid) {
		this.orgid = orgid;
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
	 * @return the bankno
	 */
	public String getBankno() {
		return bankno;
	}
	/**
	 * @param bankno the bankno to set
	 */
	public void setBankno(String bankno) {
		this.bankno = bankno;
	}
	/**
	 * @return the magacct
	 */
	public String getMagacct() {
		return magacct;
	}
	/**
	 * @param magacct the magacct to set
	 */
	public void setMagacct(String magacct) {
		this.magacct = magacct;
	}
	

}
