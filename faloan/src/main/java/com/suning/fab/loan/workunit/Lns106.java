package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	HY
 *
 * @version V1.1.1
 *
 * @see 	非标准账号准备
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns106 extends WorkUnit {

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
	
	//结束日期
	String	endDate;
	//银行科目号
	String	bankSubject;
	//业务流水号
	String	serialNo;
	//预收账号
	String	repayAcctNo;
	//外部流水号
	String	outSerialNo;
	//还款渠道
	String	repayChannel;
	//罚息
	FabAmount	dintAmt;
	//利息
	FabAmount	nintAmt;
	//本金
	FabAmount	prinAmt;
	//结束标志
	String	endFlag;
	FabAmount	intAmt;
	FabAmount	forfeitAmt;
	String		customName;
	FabAmount	contractAmt;
	FabAmount incomeAmt;
	String		flags;
	
    FabAmount reduceIntAmt;				//利息减免金额
    FabAmount reduceFintAmt;			//罚息减免金额
	
	Map<Integer, String> acctList = new HashMap<Integer, String>();

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		int	i = 0;
		String nonDate = null;
		setCurTerm(0);
		
		if(VarChecker.isEmpty(endDate))
		{
			nonDate = ctx.getTranDate();
		}	
		else
		{
			nonDate = getEndDate();
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
		if (null == lnsbasicinfo){
			Map<String,Object> nonparam = new HashMap<String,Object>();
			//账号
			nonparam.put("acctno1", acctNo);
			//机构
			nonparam.put("openbrc", ctx.getBrc());
			List<TblLnsbasicinfo> lnsbasicinfolist;
			try {
				//取主文件信息
				lnsbasicinfolist = DbAccessUtil.queryForList("CUSTOMIZE.query_non_acctno", nonparam, TblLnsbasicinfo.class);
			}
			catch (FabSqlException e)
			{
				//异常
				throw new FabException(e, "SPS103", "Lnsnonstdplan");
			}
			
			if (null == lnsbasicinfolist){
				//lnsbasicinfo lnsnonstdplan都无数据
				throw new FabException("ACC108", acctNo);
			}
			else
			{	
				for( TblLnsbasicinfo lnsbasicinfonon : lnsbasicinfolist)
				{
					if("471009".equals(ctx.getTranCode()) && 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfonon.getLoanstat()))
					{
						i++;
						continue;
					}	
					if((Integer.valueOf(0)) == (getCurTerm()) &&	nonDate.compareTo(lnsbasicinfonon.getContduedate()) <= 0)
					{	
						setCurTerm(lnsbasicinfonon.getOldprinterm());
					}
					acctList.put(lnsbasicinfonon.getOldprinterm(), lnsbasicinfonon.getAcctno());
				}	
				if( Integer.valueOf(0) == (getCurTerm()))
					setCurTerm(lnsbasicinfolist.size());
				if(i == lnsbasicinfolist.size() && !"477017".equals(ctx.getTranCode()))
				{
					LoggerUtil.debug("该账户已销户或已核销");
					throw new FabException("ACC108", acctNo);
				}	
				setFlags("1");
			}	
		}
		else //主文件表有数据 准备给list赋值 之后循环list调用415
		{
			setFlags("0");
			acctList.put(Integer.valueOf(1), lnsbasicinfo.getAcctno());
		}	
		//取试算信息
		setAcctList(acctList);
		
		//加幂等处理 还款 冲销 事前利息
		if(VarChecker.asList("471009" ).contains(ctx.getTranCode()))
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
			//利息罚息减免登记 2019-03-18
			if(!VarChecker.isEmpty(reduceIntAmt)){
				lnsinterface.setSumdelint(reduceIntAmt.getVal());
			}
			if(!VarChecker.isEmpty(reduceFintAmt)){
				lnsinterface.setSumdelfint(reduceFintAmt.getVal());
			}
			
			if (!VarChecker.isEmpty(repayChannel))
				lnsinterface.setReserv5(repayChannel);

			
			//资金方信息存入reserv6  2019-05-31
			ListMap pkgList1 = ctx.getRequestDict("pkgList1");
			StringBuffer reserv6 = new StringBuffer();
			if( null != pkgList1  && pkgList1.size() > 0)
			{
				//暂不支持多资金方
				if(pkgList1.size() > 1)
				{
					throw new FabException("LNS133");
				}
				
				for(PubDict pkg:pkgList1.getLoopmsg()){
					if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "investeeId"))){
						throw new FabException("LNS055","资金方");
					}
					
					reserv6.append(PubDict.getRequestDict(pkg, "investeeId") + "|");
				}
				
				reserv6.deleteCharAt(reserv6.length() - 1);
				lnsinterface.setReserv6(reserv6.toString());
			}
			
			
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
		//加幂等处理 租赁退货
		if(VarChecker.asList("471010" ).contains(ctx.getTranCode()))
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
			if (!VarChecker.isEmpty(getBankSubject())) // 帐速融 银行直接还款
				lnsinterface.setBillno(getBankSubject());
			lnsinterface.setAcctno(getAcctNo());
			lnsinterface.setBrc(ctx.getBrc());
			lnsinterface.setBankno(getOutSerialNo());
			lnsinterface.setTranamt(0.00); // 用还款总金额TODO
			lnsinterface.setSumrint(0.00);
			lnsinterface.setSumramt(0.00);
			lnsinterface.setSumrfint(0.00);
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
					if(VarChecker.isEmpty(lnsinterface.getReserv4())){
						incomeAmt = new FabAmount();
					}else{
						try{						
							incomeAmt = new FabAmount(Double.valueOf(lnsinterface.getReserv4()));
						}catch(NumberFormatException exception){
							incomeAmt = new FabAmount();
						}						
					}
					
					endFlag = lnsinterface.acctFlag2Endflag();

					throw new FabException(e, TRAN.IDEMPOTENCY);
				} else
					throw new FabException(e, "SPS100", "lnsinterface");
			}
		}	
		//非标利息减免理
		if(VarChecker.asList("478003" ).contains(ctx.getTranCode()))
		{
			TblLnsinterface lnsinterface = new TblLnsinterface();
			lnsinterface.setTrandate(ctx.getTermDate());
			lnsinterface.setSerialno(serialNo);
			lnsinterface.setAccdate(ctx.getTranDate());
			lnsinterface.setSerseqno(ctx.getSerSeqNo());
			lnsinterface.setBrc(ctx.getBrc());
			lnsinterface.setTrancode(ctx.getTranCode());
			lnsinterface.setUserno(acctNo);
			lnsinterface.setAcctno(acctNo);
			if(!VarChecker.isEmpty(intAmt)){
				lnsinterface.setSumdelint(intAmt.getVal());
			}
			if(!VarChecker.isEmpty(forfeitAmt)){
				lnsinterface.setSumdelfint(forfeitAmt.getVal());
			}
			try{
				DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
			}catch (FabSqlException e){
				
				if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
					throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
				} else {
					throw new FabException(e, "SPS100", "lnsinterface");
				}
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
	/**
	 * 
	 * @return the incomeAmt
	 */
	public FabAmount getIncomeAmt() {
		return incomeAmt;
	}
	/**
	 * @param incomeAmt the incomeAmt to set
	 */
	public void setIncomeAmt(FabAmount incomeAmt) {
		this.incomeAmt = incomeAmt;
	}
	/**
	 * @return the reduceIntAmt
	 */
	public FabAmount getReduceIntAmt() {
		return reduceIntAmt;
	}
	/**
	 * @param reduceIntAmt the reduceIntAmt to set
	 */
	public void setReduceIntAmt(FabAmount reduceIntAmt) {
		this.reduceIntAmt = reduceIntAmt;
	}
	/**
	 * @return the reduceFintAmt
	 */
	public FabAmount getReduceFintAmt() {
		return reduceFintAmt;
	}
	/**
	 * @param reduceFintAmt the reduceFintAmt to set
	 */
	public void setReduceFintAmt(FabAmount reduceFintAmt) {
		this.reduceFintAmt = reduceFintAmt;
	}
	
}
