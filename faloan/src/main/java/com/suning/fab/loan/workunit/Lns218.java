package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.domain.TblLnsinterfaceex;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanDbUtil;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanRepayOrderHelper;
import com.suning.fab.loan.utils.LoanRpyInfoUtil;
import com.suning.fab.loan.utils.TaxUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 罚息减免
 *
 * @param serialNo 业务流水号
 * @param acctNo 贷款账号 
 * @param forfeitAmt 罚息减免金额
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns218 extends WorkUnit { 

	String	acctNo;
	FabAmount forfeitAmt;
	String serialNo;
	FabAmount intAmt;
	String  reduceList;  //减免明细2020-08-03
	FabAmount reduceIntAmt = new FabAmount();
	FabAmount reduceFintAmt = new FabAmount();
	Map<String, Map<String,FabAmount>> repayAmtList = new HashMap<String, Map<String,FabAmount>>();
	
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
	
		
		if( !"478003".equals(ctx.getTranCode()) )
		{
			//插幂等登记簿
			TblLnsinterface lnsinterface = new TblLnsinterface();
			lnsinterface.setTrandate(ctx.getTermDate());
			lnsinterface.setSerialno(serialNo);
			lnsinterface.setAccdate(ctx.getTranDate());
			lnsinterface.setSerseqno(ctx.getSerSeqNo());
			lnsinterface.setBrc(ctx.getBrc());
			lnsinterface.setTrancode(ctx.getTranCode());
			lnsinterface.setUserno(acctNo);
			lnsinterface.setAcctno(acctNo);
			if(!VarChecker.isEmpty(reduceIntAmt)){
				lnsinterface.setSumdelint(reduceIntAmt.getVal());
			}
			if(!VarChecker.isEmpty(reduceFintAmt)){
				lnsinterface.setSumdelfint(reduceFintAmt.getVal());
	
			}
			try{
				DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
			}catch (FabSqlException e){
				
				if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
					
					 Map<String, Object> params = new HashMap<>();
	                params.put("serialno", tranctx.getSerialNo());
	                params.put("trancode", tranctx.getTranCode());
	                //幂等登记薄 幂等的话返回数据要从表里面取
	                lnsinterface = LoanDbUtil.queryForObject("Lnsinterface.selectByUk", params,
	                            TblLnsinterface.class);
	
	//	                forfeetAmt = new FabAmount(lnsinterface.getTranamt());
	//                forfeetAmt = new FabAmount(lnsinterface.getSumrint());
	//                damagesAmt = new FabAmount(lnsinterface.getSumrfint());
		                
		                
					//展示减免明细准备数据 2020-08-03
	                if( "true".equals(la.getInterestAgreement().getShowRepayList()) )
	                {
	                	Map<String, Object> param = new HashMap<>();
	//                    param.put("acctno", acctNo);
	//                    param.put("brc", getTranctx().getBrc());
	                    param.put("trandate", lnsinterface.getAccdate());
	                    param.put("serseqno", lnsinterface.getSerseqno());
	                    param.put("key", ConstantDeclare.KEYNAME.JMMX);
	                    TblLnsinterfaceex interfaceex;
	                    try {
	                        interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex_hkmx", param, TblLnsinterfaceex.class);
	                    } catch (FabSqlException e1) {
	                        throw new FabException(e1, "SPS103", "query_lnsinterfaceex");
	                    }
	                    if (interfaceex != null)
	                    	reduceList = interfaceex.getValue();
	                    else
	                    	reduceList = null;
	                }
					throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
				} else {
					throw new FabException(e, "SPS100", "lnsinterface");
				}
			}
		}
				
				
		if(VarChecker.isEmpty(forfeitAmt)||forfeitAmt.isZero()){
			//展示还款明细准备数据 2020-06-08
		    if( "true".equals(la.getInterestAgreement().getShowRepayList()) )
		    {
				reduceList = LoanRpyInfoUtil.getRepayList(acctNo, ctx, repayAmtList,la);
				AccountingModeChange.saveInterfaceEx(tranctx, acctNo, ConstantDeclare.KEYNAME.JMMX, "减免明细", reduceList.length() > 2000 ? reduceList.substring(0, 2000) : reduceList);
		    } else {
		    	reduceList = null;
		    }
		    
			return;
		}
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());
		List<TblLnsbill> billList;
		try{
			billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_dint", param, TblLnsbill.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbill");
		}
		
		if(null == billList){
			throw new FabException("LNS032");
		}
		
		// 小本小息
		billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
		Map<String,Object> upbillmap = new HashMap<String, Object>();
		for(TblLnsbill lnsbill : billList)
		{
            LoggerUtil.debug("LNSBILL:"+lnsbill.getAcctno()+"|"+lnsbill.getPeriod()+"|"+lnsbill.getBillstatus()+"."+lnsbill.getBilltype()+"|"+lnsbill.getBillbal());

            upbillmap.put("actrandate", ctx.getTranDate());
        	upbillmap.put("tranamt", forfeitAmt.getVal());
        	
        	upbillmap.put("trandate", lnsbill.getTrandate());
    		upbillmap.put("serseqno", lnsbill.getSerseqno());
    		upbillmap.put("txseq", lnsbill.getTxseq());
			
    		Map<String, Object> repaymap;
    		try{
    			repaymap = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsbill_repay", upbillmap);
    		}catch(FabSqlException e){
    			throw new FabException(e, "SPS103", "lnsbill");
    		}
			if(repaymap == null)
			{
				throw new FabException("SPS104", "lnsbill");
			}
			FabAmount minAmt = new FabAmount(Double.valueOf(repaymap.get("minamt").toString()));
			LoggerUtil.debug("minAmt:" + minAmt.getVal());
			if(minAmt.isZero())
			{
				LoggerUtil.debug("该账单金额已经为零");
				continue;
			}
    		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),lnsbill.getBilltype(), lnsbill.getBillstatus(), new FabCurrency());
			lnsAcctInfo.setCancelFlag(lnsbill.getCancelflag());
    		
    		List<FabAmount> amtList = new ArrayList<FabAmount>();
			amtList.add(TaxUtil.calcVAT(minAmt));
    		sub.operate(lnsAcctInfo, null, minAmt, la.getFundInvest(), ConstantDeclare.BRIEFCODE.FXJM, lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), ctx);
   			eventProvider.createEvent(ConstantDeclare.EVENT.REDUDEFINT, minAmt, lnsAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.FXJM, ctx, amtList, lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
			//罚息减免
			AccountingModeChange.saveReduceTax( ctx ,acctNo , minAmt ,lnsbill);
   			forfeitAmt.selfSub(minAmt);
        	
	        //展示减免明细准备数据 2020-08-03
	        if( "true".equals(la.getInterestAgreement().getShowRepayList()) )
	            	LoanRpyInfoUtil.addRpyList(repayAmtList, lnsbill.getBilltype(), lnsbill.getPeriod().toString(), minAmt,lnsbill.getBillstatus());
	        
        	if(!forfeitAmt.isPositive())
	        {	
	        	break;
	        }   
		}
		
		//展示还款明细准备数据 2020-06-08
	    if( "true".equals(la.getInterestAgreement().getShowRepayList()) )
	    {
			reduceList = LoanRpyInfoUtil.getRepayList(acctNo, ctx, repayAmtList,la);
	        AccountingModeChange.saveInterfaceEx(tranctx, acctNo, ConstantDeclare.KEYNAME.JMMX, "减免明细", reduceList.length() > 2000 ? reduceList.substring(0, 2000) : reduceList);
	    } else {
	    	reduceList = null;
	    }
	    
		if(forfeitAmt.isPositive()){
			throw new FabException("LNS032");
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
	 * @return the forfeitAmt
	 */
	public FabAmount getForfeitAmt() {
		return forfeitAmt;
	}
	/**
	 * @param forfeitAmt the forfeitAmt to set
	 */
	public void setForfeitAmt(FabAmount forfeitAmt) {
		this.forfeitAmt = forfeitAmt;
	}
	/**
	 * @return the sub
	 */
	public AccountOperator getSub() {
		return sub;
	}
	/**
	 * @param sub the sub to set
	 */
	public void setSub(AccountOperator sub) {
		this.sub = sub;
	}
	/**
	 * @return the eventProvider
	 */
	public LoanEventOperateProvider getEventProvider() {
		return eventProvider;
	}
	/**
	 * @param eventProvider the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}
	/**
	 * @return the serialNo
	 */
	public String getSerialNo() {
		return serialNo;
	}
	/**
	 * @param serialNo the serialNo to set
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}
	/**
	 * @return the intAmt
	 */
	public FabAmount getIntAmt() {
		return intAmt;
	}
	/**
	 * @param intAmt the intAmt to set
	 */
	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}
	/**
	 * @return the reduceList
	 */
	public String getReduceList() {
		return reduceList;
	}
	/**
	 * @param reduceList the reduceList to set
	 */
	public void setReduceList(String reduceList) {
		this.reduceList = reduceList;
	}
	/**
	 * @return the repayAmtList
	 */
	public Map<String, Map<String, FabAmount>> getRepayAmtList() {
		return repayAmtList;
	}
	/**
	 * @param repayAmtList the repayAmtList to set
	 */
	public void setRepayAmtList(Map<String, Map<String, FabAmount>> repayAmtList) {
		this.repayAmtList = repayAmtList;
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

