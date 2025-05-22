package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanProvisionProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns450 extends WorkUnit {

    String acctNo;

    FabAmount lastIncome = new FabAmount();
    FabAmount totalIncome = new FabAmount();
    FabAmount totalInt = new FabAmount();//总未还利息
    FabAmount totalDint = new FabAmount();//总未还罚息
    FabAmount sumdelInt = new FabAmount();	//减免利息
    FabAmount sumdelFint = new FabAmount();	//减免罚息
    FabAmount sumPenalty = new FabAmount();	//违约金
    String vestingday;
    String settleFlag;

    @Override
    public void run() throws FabException, Exception {

    	TranCtx ctx = getTranctx();
    	

		//读取借据主文件
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		param.put("brc", ctx.getBrc());

		TblLnsbasicinfo lnsbasicinfo = null;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}

		if (null == lnsbasicinfo){
			throw new FabException("SPS104", "lnsbasicinfo");
		}

		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		//利息计提登记薄计算
//		TblLnsprovisionreg Lnsprovisionreg = null;
		TblLnsprovision lnsprovision;
		try {
//			Lnsprovisionreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_PPIncome", param, TblLnsprovisionreg.class);
			lnsprovision= LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,lnsbasicinfo.getAcctno(),ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.PROVISION, la);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "Lnsprovisionreg");
		}

		if (!lnsprovision.isExist()){
			vestingday = ctx.getTranDate();
		}else{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			this.setTotalIncome(new FabAmount(lnsprovision.getTotalinterest()));
			this.setLastIncome(new FabAmount(lnsprovision.getLastinterest()));
			this.setVestingday(sdf.format(lnsprovision.getLastbegindate()));
		}
		//罚息计提明细登记簿
        List<TblLnspenintprovregdtl> lnspenintprovregdtls;
        try{
        	lnspenintprovregdtls = DbAccessUtil.queryForList("CUSTOMIZE.query_lnspenintprovregdtl", param, TblLnspenintprovregdtl.class);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS103", "lnspenintprovregdtls");
        }

        if (VarChecker.isEmpty(lnspenintprovregdtls)){
            throw new FabException("SPS104", "lnspenintprovregdtls");
        }
        //期数
        int presentPeriod = 0;
        Map<Integer, List<TblLnspenintprovregdtl>> penintprovregdtlMap = new HashMap<Integer, List<TblLnspenintprovregdtl>>();
        //键值对 期数-罚息
        Map<Integer, List<TblLnspenintprovregdtl>> periodDintMap = new HashMap<Integer, List<TblLnspenintprovregdtl>>();
        //键值对 期数-复利
        Map<Integer, List<TblLnspenintprovregdtl>> periodCintMap = new HashMap<Integer, List<TblLnspenintprovregdtl>>();
        for(TblLnspenintprovregdtl lnspenintprovregdtl : lnspenintprovregdtls){
        	List<TblLnspenintprovregdtl> list = new ArrayList<TblLnspenintprovregdtl>();
        	List<TblLnspenintprovregdtl> clist = new ArrayList<TblLnspenintprovregdtl>();
        	List<TblLnspenintprovregdtl> dlist = new ArrayList<TblLnspenintprovregdtl>();
        	presentPeriod = lnspenintprovregdtl.getPeriod();
        	if(penintprovregdtlMap.containsKey(presentPeriod)){
        		list = penintprovregdtlMap.get(presentPeriod);
        	}
        	if(ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(lnspenintprovregdtl.getBilltype())){
        		if(periodDintMap.containsKey(presentPeriod)){
        			dlist = periodDintMap.get(presentPeriod);
        			dlist.add(lnspenintprovregdtl);
            		periodDintMap.put(presentPeriod, dlist);
	        	}else{
	        		dlist.add(lnspenintprovregdtl);
	        		periodDintMap.put(presentPeriod, dlist);
	        	}
        	}
        	
        	if(ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnspenintprovregdtl.getBilltype())){
	        	if(periodCintMap.containsKey(presentPeriod)){
	        		clist = periodCintMap.get(presentPeriod);
	    			clist.add(lnspenintprovregdtl);
	    			periodCintMap.put(presentPeriod, clist);
	        	}else{
	        		clist.add(lnspenintprovregdtl);
	    			periodCintMap.put(presentPeriod, clist);
	        	}
        	}
        	list.add(lnspenintprovregdtl);
    		penintprovregdtlMap.put(presentPeriod, list);
        }
        //上期罚息收入
        FabAmount lastDintIncome = new FabAmount();
        FabAmount totalDintIncome = new FabAmount();
        //上期复利收入
        FabAmount lastCintIncome = new FabAmount();
        FabAmount totalCintIncome = new FabAmount();
        //当前期生成的罚息收入条数
        int presentDintCount = 0;
        //当前期生成的复利收入条数
        int presentCintCount = 0;
        for( int i = 1;i<=presentPeriod;i++){
        	if(periodDintMap.get(i) != null){
            	presentDintCount = periodDintMap.get(i).size();
            	
            }
            if(periodCintMap.get(i) != null){
    			presentCintCount = periodCintMap.get(i).size();
            }
            if(presentDintCount == 1){
            	lastDintIncome.selfAdd(new FabAmount(periodDintMap.get(i).get(0).getInterest()));
            }else if(presentDintCount > 0){
            	if(periodDintMap.get(i).get(presentDintCount - 1).getEnddate().equals(periodDintMap.get(i).get(presentDintCount - 2).getEnddate())){
            		if(presentDintCount>=3){
            			lastDintIncome.selfAdd(new FabAmount(BigDecimal.valueOf(periodDintMap.get(i).get(presentDintCount - 2).getInterest())
                    			.subtract(BigDecimal.valueOf(periodDintMap.get(i).get(presentDintCount - 3).getInterest())).doubleValue()));
            		}else{
            			lastDintIncome.selfAdd(new FabAmount(periodDintMap.get(i).get(presentDintCount - 2).getInterest()));
            		}
            	}else{
            		lastDintIncome.selfAdd(new FabAmount(BigDecimal.valueOf(periodDintMap.get(i).get(presentDintCount - 1).getInterest())
                			.subtract(BigDecimal.valueOf(periodDintMap.get(i).get(presentDintCount - 2).getInterest())).doubleValue()));
            	}
            }
            if(presentCintCount == 1){
            	lastCintIncome.selfAdd(new FabAmount(periodCintMap.get(i).get(0).getInterest()));
            }else if(presentCintCount > 0){
            	if(periodCintMap.get(i).get(presentCintCount - 1).getEnddate().equals(periodCintMap.get(i).get(presentCintCount - 2).getEnddate())){
            		if(presentCintCount>=3){
            			lastCintIncome.selfAdd(new FabAmount(BigDecimal.valueOf(periodCintMap.get(i).get(presentCintCount - 2).getInterest())
                    			.subtract(BigDecimal.valueOf(periodCintMap.get(i).get(presentCintCount - 3).getInterest())).doubleValue()));
            		}else{
            			lastCintIncome.selfAdd(new FabAmount(periodCintMap.get(i).get(presentCintCount - 2).getInterest()));
            		}
            	}else{
	            	lastCintIncome.selfAdd(new FabAmount(BigDecimal.valueOf(periodCintMap.get(i).get(presentCintCount - 1).getInterest())
	            			.subtract(BigDecimal.valueOf(periodCintMap.get(i).get(presentCintCount - 2).getInterest())).doubleValue()));
            	}
        	}
        }
        
        
        //期数没0
        for( int i = 1;i<=presentPeriod;i++){
        	//罚息数量
        	if(periodDintMap.get(i)!=null){
        		int dintCount = periodDintMap.get(i).size();
	        	totalDintIncome.selfAdd(new FabAmount(periodDintMap.get(i).get(dintCount-1).getInterest()));
        	}
        }
    	for( int i = 1;i<=presentPeriod;i++){
        	//复利数量
    		if(periodCintMap.get(i)!=null){
    			int cintCount = periodCintMap.get(i).size();
            	totalCintIncome.selfAdd(new FabAmount(periodCintMap.get(i).get(cintCount-1).getInterest()));
    		}
        	
        }
        
        
        //查询rpyplan表算出减免罚息以及减免利息
        //定义查询map
  		Map<String,Object> billparam = new HashMap<String,Object>();
  		//按账号查询
  		billparam.put("acctno", acctNo);
  		billparam.put("brc", ctx.getBrc());
  		billparam.put("openbrc", ctx.getBrc());
  		List<TblLnsrpyplan> rpyplanlist = null;

  		try {
  			//取还款计划登记簿数据
  			rpyplanlist = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsrpyplan", billparam, TblLnsrpyplan.class);
  		}
  		catch (FabSqlException e)
  		{
  			throw new FabException(e, "SPS103", "lnsrpyplan");
  		}

  		if (null == rpyplanlist){
  			rpyplanlist = new ArrayList<TblLnsrpyplan>();
  		}
  		//结清标志赋值
		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())){
			this.setSettleFlag("CLOSE");
			for(TblLnsrpyplan lnsrpyplan : rpyplanlist){
				sumPenalty.selfAdd(lnsrpyplan.getReserve2());
	  		}
		}else{
			this.setSettleFlag("RUNNING");
		}
  		
  		for(TblLnsrpyplan lnsrpyplan : rpyplanlist){
  			sumdelInt.selfAdd(lnsrpyplan.getIntamt());
  			sumdelFint.selfAdd(lnsrpyplan.getSumrfint());
  		}
  		
  		//计算总未还利息
		List<TblLnsbill> billList;
		try {
			billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_nintall", billparam, TblLnsbill.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}
  		//totalInt = new FabAmount(Lnsprovisionreg.getTotalinterest());
		totalInt=new FabAmount(lnsprovision.getTotalinterest());
  		for(TblLnsbill bill : billList){
  			//已还利息
  			BigDecimal hisInt =  BigDecimal.valueOf(bill.getBillamt()).subtract(BigDecimal.valueOf(bill.getBillbal()));
  			totalInt.selfSub(hisInt.doubleValue());
  		}
  		//计算总未还罚息（动态表罚息账户余额）
  		//获取动态表信息
		List<Map<String, Object>> lnsaccountdyninfoList = null;
		try {
			lnsaccountdyninfoList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsaccountdyninfo", param);
		} catch (FabSqlException e) {
			LoggerUtil.error("查询lnsaccountdyninfo表错误{}", e);
		}
  		for(Map<String, Object> map :lnsaccountdyninfoList){
  			if(!VarChecker.isEmpty(map.get("acctstat")) && "DINT.N".equals(map.get("acctstat"))){
  				totalDint.selfAdd(new FabAmount(Double.valueOf(map.get("currbal").toString())));
  			}
  		}

		lastIncome.selfAdd(lastDintIncome).selfAdd(lastCintIncome);
		
		if("CLOSE".equals(settleFlag) && CalendarUtil.after(ctx.getTranDate(), lnsbasicinfo.getLastprindate())){
			this.setLastIncome(new FabAmount(0.0));
		}
		
        totalIncome.selfAdd(totalDintIncome).selfAdd(totalCintIncome).selfSub(sumdelInt).selfSub(sumdelFint).selfAdd(sumPenalty);
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
	 * @return the lastIncome
	 */
	public FabAmount getLastIncome() {
		return lastIncome;
	}

	/**
	 * @param lastIncome the lastIncome to set
	 */
	public void setLastIncome(FabAmount lastIncome) {
		this.lastIncome = lastIncome;
	}

	/**
	 * @return the totalIncome
	 */
	public FabAmount getTotalIncome() {
		return totalIncome;
	}

	/**
	 * @param totalIncome the totalIncome to set
	 */
	public void setTotalIncome(FabAmount totalIncome) {
		this.totalIncome = totalIncome;
	}

	/**
	 * @return the totalInt
	 */
	public FabAmount getTotalInt() {
		return totalInt;
	}

	/**
	 * @param totalInt the totalInt to set
	 */
	public void setTotalInt(FabAmount totalInt) {
		this.totalInt = totalInt;
	}

	/**
	 * @return the totalDint
	 */
	public FabAmount getTotalDint() {
		return totalDint;
	}

	/**
	 * @param totalDint the totalDint to set
	 */
	public void setTotalDint(FabAmount totalDint) {
		this.totalDint = totalDint;
	}

	/**
	 * @return the sumdelInt
	 */
	public FabAmount getSumdelInt() {
		return sumdelInt;
	}

	/**
	 * @param sumdelInt the sumdelInt to set
	 */
	public void setSumdelInt(FabAmount sumdelInt) {
		this.sumdelInt = sumdelInt;
	}

	/**
	 * @return the sumdelFint
	 */
	public FabAmount getSumdelFint() {
		return sumdelFint;
	}

	/**
	 * @param sumdelFint the sumdelFint to set
	 */
	public void setSumdelFint(FabAmount sumdelFint) {
		this.sumdelFint = sumdelFint;
	}

	/**
	 * @return the sumPenalty
	 */
	public FabAmount getSumPenalty() {
		return sumPenalty;
	}

	/**
	 * @param sumPenalty the sumPenalty to set
	 */
	public void setSumPenalty(FabAmount sumPenalty) {
		this.sumPenalty = sumPenalty;
	}

	/**
	 * @return the vestingday
	 */
	public String getVestingday() {
		return vestingday;
	}

	/**
	 * @param vestingday the vestingday to set
	 */
	public void setVestingday(String vestingday) {
		this.vestingday = vestingday;
	}

	/**
	 * @return the settleFlag
	 */
	public String getSettleFlag() {
		return settleFlag;
	}

	/**
	 * @param settleFlag the settleFlag to set
	 */
	public void setSettleFlag(String settleFlag) {
		this.settleFlag = settleFlag;
	}


}
