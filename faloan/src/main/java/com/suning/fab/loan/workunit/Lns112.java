package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.utils.AccountingModeChange;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsdiscountaccount;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	HY
 *
 * @version V1.1.1
 *
 * @see 	还款计划查询
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns112 extends WorkUnit {

	//账号
	String acctNo;
	//每页大小
	Integer pageSize;
	//当前页
	Integer currentPage;
	//期数
	Integer repayTerm;
	
	//总行数
	Integer totalLine;
	//json串
	String pkgList;
	
	Integer curTerm;
	
	String	endDate;
	String	bankSubject;
	String	serialNo;
	String	repayAcctNo;
	String	outSerialNo;
	String	repayChannel;
	FabAmount	dintAmt;
	FabAmount	repayAmt;
	FabAmount	nintAmt;
	FabAmount	prinAmt;
	String	endFlag;
	String	memo;
	FabAmount	intAmt;
	FabAmount	forfeitAmt;
	String		customName;
	String		customType;
	FabAmount	contractAmt;
	String		flags;
	
	String		customId;
	
	Map<Integer, String> acctList = new HashMap<Integer, String>();
	private Integer   txseqno = 0;  //预收明细登记簿  子序号

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		setCurTerm(0);
		//参数校验
		if(!VarChecker.isEmpty(repayChannel)){
			if("12".equals(repayChannel)){
				if(VarChecker.isEmpty(memo)){
					throw new FabException("LNS055","渠道号");
				}
			}
		}
		
		
		//定义查询map
		Map<String,Object> param = new HashMap<String,Object>();
		//账号
		param.put("acctno", acctNo);
		//机构
		param.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo = null;
		try {
			//取主文件信息
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			//异常
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo)
		{
			//异常
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		else //主文件表有数据 准备给list赋值 之后循环list调用415
		{
			setCustomId(lnsbasicinfo.getCustomid());
			setCustomName("");
			setCustomType(lnsbasicinfo.getCusttype());
			setFlags("0");
			acctList.put(Integer.valueOf(1), lnsbasicinfo.getAcctno());
		}	
		//判断账户余额是否充足
		if("10".equals(repayChannel)||"11".equals(repayChannel)||"13".equals(repayChannel))
		{
			//定义查询map
			Map<String,Object> discountParam = new HashMap<String,Object>();
			//账号
			discountParam.put("acctno", acctNo);
			//机构
			discountParam.put("brc", ctx.getBrc());
			//账号类型
			if("10".equals(repayChannel))
			{
				discountParam.put("accttype", "2");
			}
			if("11".equals(repayChannel))
			{
				discountParam.put("accttype", "3");
			}
			if("13".equals(repayChannel))
			{
				discountParam.put("accttype", "1");
			}
			TblLnsdiscountaccount lnsdiscountaccount = null;
			try {
				lnsdiscountaccount = DbAccessUtil.queryForObject("Lnsdiscountaccount.selectByUk", discountParam, TblLnsdiscountaccount.class);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS102", "lnsdiscountaccount");
			}
			if(null == lnsdiscountaccount){
				throw new FabException("LNS069");
			}
			//判断余额是否充足
			if(!VarChecker.isEmpty(lnsdiscountaccount.getBalance())){
				if(repayAmt.getVal()>lnsdiscountaccount.getBalance().doubleValue()){
					//账号类型
					if("10".equals(repayChannel))
					{
						throw new FabException("LNS070","贴息户");
					}
					if("11".equals(repayChannel))
					{
						throw new FabException("LNS070","业务保证金");
					}
					if("13".equals(repayChannel))
					{
						throw new FabException("LNS070","预收租金");
					}
					
				}else{
					discountParam.put("repayamt",repayAmt.getVal() );
					try {
						DbAccessUtil.execute("CUSTOMIZE.update_lnsdiscountaccount", discountParam);
					} catch (FabException e) {
						throw new FabException(e, "SPS102", "lnsbills");
					}
					AccountingModeChange.saveLnsprefundsch(ctx, ++txseqno, lnsdiscountaccount.getAcctno(),lnsdiscountaccount.getCustomid() ,
							lnsdiscountaccount.getAccttype(), lnsdiscountaccount.getCusttype(),lnsdiscountaccount.getName() , repayAmt.getVal(),
							"sub");

				}
			}
			
		}
		//取试算信息
		setAcctList(acctList);
		
		//加幂等处理 还款 冲销 事前利息
		if(VarChecker.asList("471012" ).contains(ctx.getTranCode()))
		{
			// 幂等登记薄
			TblLnsinterface lnsinterface = new TblLnsinterface();
			lnsinterface.setTrandate(ctx.getTermDate());
			lnsinterface.setSerialno(getSerialNo());
			lnsinterface.setAccdate(ctx.getTranDate());
			lnsinterface.setSerseqno(ctx.getSerSeqNo());
			lnsinterface.setTrancode(ctx.getTranCode());
			if (!VarChecker.isEmpty(getRepayAcctNo())) // 帐速融 银行直接还款
				lnsinterface.setUserno(getRepayAcctNo());
			lnsinterface.setAcctno(getAcctNo());
			lnsinterface.setBrc(ctx.getBrc());
			lnsinterface.setBankno(getOutSerialNo());
			lnsinterface.setTranamt(0.00); // 用还款总金额TODO
			lnsinterface.setSumrint(0.00);
			lnsinterface.setSumramt(0.00);
			lnsinterface.setSumrfint(0.00);
			//科目号存在billno
			if(!VarChecker.isEmpty(bankSubject))
				lnsinterface.setBillno(bankSubject);
			if (!VarChecker.isEmpty(repayChannel))
				lnsinterface.setReserv5(repayChannel);

			try {
				DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
			} catch (FabSqlException e) {
				if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put("serialno", getSerialNo());
					params.put("trancode", ctx.getTranCode());

					try {
						lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", params,
								TblLnsinterface.class);
					} catch (FabSqlException f) {
						throw new FabException(f, "SPS103", "lnsinterface");
					}
					dintAmt = new FabAmount(lnsinterface.getSumrfint());
					nintAmt = new FabAmount(lnsinterface.getSumrint());
					prinAmt = new FabAmount(lnsinterface.getSumramt());
					endFlag = lnsinterface.acctFlag2Endflag();

					throw new FabException(e, TRAN.IDEMPOTENCY);
				} else
					throw new FabException(e, "SPS100", "lnsinterface");
			}
		}	
	}
	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}


	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}


	/**
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
	}


	/**
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}


	/**
	 * @return the currentPage
	 */
	public Integer getCurrentPage() {
		return currentPage;
	}


	/**
	 * @param currentPage the currentPage to set
	 */
	public void setCurrentPage(Integer currentPage) {
		this.currentPage = currentPage;
	}


	/**
	 * @return the repayTerm
	 */
	public Integer getRepayTerm() {
		return repayTerm;
	}


	/**
	 * @param repayTerm the repayTerm to set
	 */
	public void setRepayTerm(Integer repayTerm) {
		this.repayTerm = repayTerm;
	}


	/**
	 * @return the totalLine
	 */
	public Integer getTotalLine() {
		return totalLine;
	}


	/**
	 * @param totalLine the totalLine to set
	 */
	public void setTotalLine(Integer totalLine) {
		this.totalLine = totalLine;
	}


	/**
	 * @return the pkgList
	 */
	public String getPkgList() {
		return pkgList;
	}


	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(String pkgList) {
		this.pkgList = pkgList;
	}

	public Map<Integer, String> getAcctList() {
		return acctList;
	}

	public void setAcctList(Map<Integer, String> acctList) {
		this.acctList = acctList;
	}

	public Integer getCurTerm() {
		return curTerm;
	}

	public void setCurTerm(Integer curTerm) {
		this.curTerm = curTerm;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getBankSubject() {
		return bankSubject;
	}

	public void setBankSubject(String bankSubject) {
		this.bankSubject = bankSubject;
	}

	public String getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	public String getRepayAcctNo() {
		return repayAcctNo;
	}

	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
	}

	public String getOutSerialNo() {
		return outSerialNo;
	}

	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}

	public String getRepayChannel() {
		return repayChannel;
	}

	public void setRepayChannel(String repayChannel) {
		this.repayChannel = repayChannel;
	}

	public FabAmount getDintAmt() {
		return dintAmt;
	}

	public void setDintAmt(FabAmount dintAmt) {
		this.dintAmt = dintAmt;
	}

	public FabAmount getNintAmt() {
		return nintAmt;
	}

	public void setNintAmt(FabAmount nintAmt) {
		this.nintAmt = nintAmt;
	}

	public FabAmount getPrinAmt() {
		return prinAmt;
	}

	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}

	public String getEndFlag() {
		return endFlag;
	}

	public void setEndFlag(String endFlag) {
		this.endFlag = endFlag;
	}

	public FabAmount getIntAmt() {
		return intAmt;
	}

	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}

	public FabAmount getForfeitAmt() {
		return forfeitAmt;
	}

	public void setForfeitAmt(FabAmount forfeitAmt) {
		this.forfeitAmt = forfeitAmt;
	}

	public String getCustomName() {
		return customName;
	}

	public void setCustomName(String customName) {
		this.customName = customName;
	}

	public FabAmount getContractAmt() {
		return contractAmt;
	}

	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	public String getFlags() {
		return flags;
	}

	public void setFlags(String flags) {
		this.flags = flags;
	}
	public FabAmount getRepayAmt() {
		return repayAmt;
	}
	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}
	public String getMemo() {
		return memo;
	}
	public void setMemo(String memo) {
		this.memo = memo;
	}
	public String getCustomId() {
		return customId;
	}
	public void setCustomId(String customId) {
		this.customId = customId;
	}
	public String getCustomType() {
		return customType;
	}
	public void setCustomType(String customType) {
		this.customType = customType;
	}

	public Integer getTxseqno() {
		return txseqno;
	}

	public void setTxseqno(Integer txseqno) {
		this.txseqno = txseqno;
	}
}
