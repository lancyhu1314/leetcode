package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsamortizeplan;
import com.suning.fab.loan.domain.TblLnsbasicinfoex;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.FaChecker;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.TaxUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 *  〈一句话功能简述〉
 *  〈功能详细描述〉： 费用lsit入库
 * @Author 
 * @Date  2020-03-10
 */

@Scope("prototype")
@Repository
public class Lns255 extends WorkUnit{
	String 		openBrc;
	String 		receiptNo;
	String 		startIntDate;
	ListMap		pkgList;
	LoanAgreement loanAgreement;	
	
	String		feerepayWay;
	String		feeBrc;
	String		feeType;
	String		calCulatrule;
	String		feeBase;
	String		advanceSettle;
	String		productCode;
	FabAmount	deducetionAmt;
	FabAmount	feeRate;
	FabAmount	overRate;
	String		repayWay;
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		ListMap pkgList = ctx.getRequestDict("pkgList3");
		
		FaChecker.checkFeeList(productCode,pkgList);

		if(pkgList != null && pkgList.size() > 0){
			for (PubDict pkg:pkgList.getLoopmsg()) {
				if ( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feerepayWay")) ||
					 VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeBrc")) ||
					 VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeType"))  ) {
					throw new FabException("LNS055","费用入参");
				}
				

				//费用信息表
				TblLnsfeeinfo lnsfeeinfo = new TblLnsfeeinfo();
				lnsfeeinfo.setOpenbrc(openBrc);
				lnsfeeinfo.setAcctno(receiptNo);
 				lnsfeeinfo.setCcy(loanAgreement.getContract().getCcy().getCcy());
		        lnsfeeinfo.setTrandate(ctx.getTranDate());
		        lnsfeeinfo.setTrantime(ctx.getTranDate());
		        lnsfeeinfo.setOpendate(ctx.getTranDate());
		        lnsfeeinfo.setFeeperiod(1);
				
				if( !VarChecker.isEmpty(startIntDate))
					lnsfeeinfo.setLastfeedate(startIntDate);
				else
					lnsfeeinfo.setLastfeedate(ctx.getTranDate());
				
		        if( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeRate")) )
		        	lnsfeeinfo.setFeerate(0.00);
		        else
		        	lnsfeeinfo.setFeerate(Double.valueOf(PubDict.getRequestDict(pkg, "feeRate").toString()));
		        
		        if( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "overRate")) )
		        	lnsfeeinfo.setOverrate(0.00);
		        else
		        	lnsfeeinfo.setOverrate(Double.valueOf(PubDict.getRequestDict(pkg, "overRate").toString()));
				
				if(!VarChecker.isEmpty(PubDict.getRequestDict(pkg, "deducetionAmt")))
				    lnsfeeinfo.setDeducetionamt(  Double.valueOf(PubDict.getRequestDict(pkg, "deducetionAmt").toString())  );
		        lnsfeeinfo.setFeeformula(loanAgreement.getInterestAgreement().getPeriodFormula());
 				
		        
 				//费用List入参：
				lnsfeeinfo.setRepayway(PubDict.getRequestDict(pkg, "feerepayWay"));
				lnsfeeinfo.setFeebrc(PubDict.getRequestDict(pkg, "feeBrc"));
				lnsfeeinfo.setFeetype(PubDict.getRequestDict(pkg, "feeType"));

				if( !LoanFeeUtils.isFeeRepayway(lnsfeeinfo.getRepayway()) )
					throw new FabException("LNS169","费用交取模式",lnsfeeinfo.getRepayway());
				
				//计费方式：1-按日计费（DAY） 2-按期计费（TERM）    默认按日计费
				if( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "calCulatrule")) )
					lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYDAY);
				else if( "1".equals(PubDict.getRequestDict(pkg, "calCulatrule").toString()) )
					lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYDAY);
				else if( "2".equals(PubDict.getRequestDict(pkg, "calCulatrule").toString()) )
					lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYTERM);
				else if("3".equals(PubDict.getRequestDict(pkg, "calCulatrule").toString()))
					lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYDAYTERM);
				
				
				//计提：正常按日计提， 预扣费摊销
				if( ConstantDeclare.FEEREPAYWAY.ADVDEDUCT.equals(lnsfeeinfo.getRepayway())){
					lnsfeeinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					lnsfeeinfo.setProvisionrule(ConstantDeclare.PROVISIONRULE.ADVDEDUCT);
		        	lnsfeeinfo.setFeestat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
					if( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "deducetionAmt")) ){
						throw new FabException("LNS055","扣费金额");
					}
					//预扣费用类型枚举值校验
					if( !LoanFeeUtils.isAdvanceFee(lnsfeeinfo.getFeetype()) )
						throw new FabException("LNS169","预扣费用类型",lnsfeeinfo.getFeetype());
				}
				else{
					lnsfeeinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
					lnsfeeinfo.setProvisionrule(ConstantDeclare.PROVISIONRULE.BYDAY);
					lnsfeeinfo.setFeestat(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
					//费用类型枚举值校验
					if( !LoanFeeUtils.isFeeType(lnsfeeinfo.getFeetype()) )
						throw new FabException("LNS169","费用类型",lnsfeeinfo.getFeetype());
				}
				//风险管理费按期计提
		        if( "2412615".equals( productCode ) &&
		            "51230004".equals( PubDict.getRequestDict(pkg, "feeBrc").toString()) ) {
		            lnsfeeinfo.setFeetype(ConstantDeclare.FEETYPE.RMFE);
		            lnsfeeinfo.setProvisionrule(ConstantDeclare.PROVISIONRULE.BYTERM);
		        }
		        //保费不计提,抛送投保事件
		        if( VarChecker.asList("2512617","2512619").contains( productCode ) ) {
		        	lnsfeeinfo.setFeetype(ConstantDeclare.FEETYPE.ISFE);
		        	lnsfeeinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
		     		discounts(lnsfeeinfo.getFeebrc(), lnsfeeinfo.getRepayway(),lnsfeeinfo.getFeerate());
		        }
		        
		        
				//费用计息基数： 0-剩余本金  1-合同金额 
				if( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeBase")) )
				{
					if( ConstantDeclare.CALCULATRULE.BYTERM.equals(lnsfeeinfo.getCalculatrule()) )
						lnsfeeinfo.setFeebase(ConstantDeclare.FEEBASE.ALL);
					else if( ConstantDeclare.CALCULATRULE.BYDAY.equals(lnsfeeinfo.getCalculatrule()) )
						lnsfeeinfo.setFeebase(ConstantDeclare.FEEBASE.BAL);
				}
				else
					lnsfeeinfo.setFeebase(PubDict.getRequestDict(pkg, "feeBase").toString());
		        
				
				/*提前结清收取方式：
				 *calCulatrule=1时，不传默认收到当日
				 *calCulatrule=2时，不传默认收到当期
				 *calCulatrule=1时，支持传1和3
				 *calCulatrule=2时，支持传1
				 */
				if( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "advanceSettle")) )
				{
					if( ConstantDeclare.CALCULATRULE.BYTERM.equals(lnsfeeinfo.getCalculatrule()) )
						lnsfeeinfo.setAdvancesettle(ConstantDeclare.EARLYSETTLRFLAG.CURRCHARGE);
					else if( ConstantDeclare.CALCULATRULE.BYDAY.equals(lnsfeeinfo.getCalculatrule()) )
						lnsfeeinfo.setAdvancesettle(ConstantDeclare.EARLYSETTLRFLAG.DATECHARGE);
				}
				else
				{
					lnsfeeinfo.setAdvancesettle(PubDict.getRequestDict(pkg, "advanceSettle").toString());
					if(  (   ConstantDeclare.CALCULATRULE.BYTERM.equals(lnsfeeinfo.getCalculatrule()) && 
						     "3".equals(PubDict.getRequestDict(pkg, "advanceSettle").toString())    )  ||
						 ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(PubDict.getRequestDict(pkg, "advanceSettle").toString() ) 
					)
						throw new FabException("LNS169","提前结清收取方式",PubDict.getRequestDict(pkg, "advanceSettle").toString());
				}
				
				
				//一次性费率默认值 2020-07-02
				if( ConstantDeclare.FEEREPAYWAY.ONETIME.equals( lnsfeeinfo.getRepayway()) )
				{
//					lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYTERM);
					if( !ConstantDeclare.CALCULATRULE.BYTERM.equals(lnsfeeinfo.getCalculatrule()) )
						throw new FabException("LNS231");
					lnsfeeinfo.setFeebase(ConstantDeclare.FEEBASE.ALL);//计费方式
				}
		        
		        try {
					DbAccessUtil.execute("Lnsfeeinfo.insert", lnsfeeinfo);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "TblLnsfeeinfo");
				}

		        
		        //登记摊销计划表用于每日费用摊销
				if(	lnsfeeinfo.getRepayway().equals(ConstantDeclare.FEEREPAYWAY.ADVDEDUCT)){
					// 摊销计划表
					TblLnsamortizeplan lnsamortizeplan = new TblLnsamortizeplan();
					lnsamortizeplan.setTrandate(Date.valueOf(ctx.getTranDate()));
					lnsamortizeplan.setSerseqno(ctx.getSerSeqNo());
					lnsamortizeplan.setBrc(ctx.getBrc());
					lnsamortizeplan.setAcctno(getReceiptNo());
					//扣费
					if( ConstantDeclare.BILLTYPE.BILLTYPE_RBBF.equals(lnsfeeinfo.getFeetype()) )
						lnsamortizeplan.setAmortizetype(  ConstantDeclare.AMORTIZETYPE.AMORTIZERBBF );
					else if( ConstantDeclare.BILLTYPE.BILLTYPE_GDBF.equals(lnsfeeinfo.getFeetype()) )
						lnsamortizeplan.setAmortizetype( ConstantDeclare.AMORTIZETYPE.AMORTIZEGDBF );
					else
						lnsamortizeplan.setAmortizetype( ConstantDeclare.AMORTIZETYPE.AMORTIZEFEE );
					
					lnsamortizeplan.setCcy( lnsfeeinfo.getCcy() );
					//不需要税金了
					lnsamortizeplan.setTaxrate(Double.valueOf(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")));
					lnsamortizeplan.setTotalamt(lnsfeeinfo.getDeducetionamt());
					lnsamortizeplan.setAmortizeamt(0.00);
					lnsamortizeplan.setTotaltaxamt(TaxUtil.calcVAT(new FabAmount(lnsfeeinfo.getDeducetionamt())).getVal());
					lnsamortizeplan.setAmortizetax(0.00);
					//MOD by TT.Y考虑日期为空逻辑
					lnsamortizeplan.setLastdate(loanAgreement.getContract().getStartIntDate());
					lnsamortizeplan.setBegindate(loanAgreement.getContract().getStartIntDate());

					lnsamortizeplan.setEnddate(loanAgreement.getContract().getContractEndDate());
					lnsamortizeplan.setAmortizeformula("");
					lnsamortizeplan.setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);

					try {
						DbAccessUtil.execute("Lnsamortizeplan.insert", lnsamortizeplan);
					} catch (FabSqlException e) {
						throw new FabException(e, "SPS100", "lnsamortizeplan");
					}
				}
				
				
		        //保费不计提,抛送投保事件
		        if( !VarChecker.asList("2512617","2512619").contains( productCode )  &&
		        	!VarChecker.asList("RBBF","GDBF").contains( lnsfeeinfo.getFeetype()) ) {
			        	//处理固定担保费
		        		if( !ConstantDeclare.FEEREPAYWAY.STAGING.equals(lnsfeeinfo.getRepayway()))
		        			dealGuaranteeFee(lnsfeeinfo,ctx);
		        }
		       
			}

//			//等本等息不支持加挂费用
//			if (VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_DBDX, ConstantDeclare.REPAYWAY.REPAYWAY_WILLFUL).contains(repayWay))
//				throw new FabException("TUP105");
		}
	}
	

	//保费的事件
	private void discounts(String childBrc,String feerepayWay,Double feeRate) throws FabException {
  		if(LoanFeeUtils.isPremium(productCode)) {
  			//数据不全，需要重新查询
  			LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(receiptNo,tranctx );
  			//求总期数
  			LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, tranctx.getTranDate(),tranctx);
  			//统计  费用的期数
//  			periodNum = LoanFeeUtils.countPeriod(loanAgreement, lnsBillStatistics,tranctx);//总期数
  			//分期的总金额
  			FabAmount sumFee  = new FabAmount();
  			List<LnsBill> feeBills = new ArrayList<>();
              feeBills.addAll(lnsBillStatistics.getFutureBillInfoList());
              feeBills.addAll(lnsBillStatistics.getBillInfoList());

              for( LnsBill lnsBill:feeBills){
  			    if(ConstantDeclare.FEETYPE.ISFE.equals(lnsBill.getBillType()))
                      sumFee.selfAdd(lnsBill.getBillAmt());
              }
  			Map<String,Object> map = new HashMap<>();
  			map.put("sumfee", sumFee.getVal());
  			if("A".equals(feerepayWay))
  				map.put("flag", "2");
  			else if("B".equals(feerepayWay))
  			{
  				map.put("flag", "1");
  				map.put("onetimeRate",feeRate.toString() );
  			}
  			map.put("childBrc", childBrc);
  				
  			AccountingModeChange.saveInterfaceEx(tranctx, receiptNo, ConstantDeclare.KEYNAME.BI, "投保金额",  JsonTransfer.ToJson(map));

  			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(receiptNo,  ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,
  					new FabCurrency());
  			List<FabAmount> amts = new ArrayList<>();
  			amts.add(TaxUtil.calcVAT(sumFee));
  			//代客投保事件
  			eventProvider.createEvent(ConstantDeclare.EVENT.BINSURANCE, sumFee, lnsAcctInfo,null, la.getFundInvest(),
  					ConstantDeclare.BRIEFCODE.DKTB, tranctx ,amts,"",childBrc);
              AccountingModeChange.saveFeeTax(tranctx, receiptNo, sumFee.getVal(),
                      TaxUtil.calcVAT(sumFee).getVal(), ConstantDeclare.FEETYPE.ISFE);
  		}
  	}
	
	
	
	//处理固定担保费
	private void dealGuaranteeFee(TblLnsfeeinfo lnsfeeinfo, TranCtx ctx) throws FabException {
		FabAmount oneTimeFee = new FabAmount();
		if( ConstantDeclare.FEEREPAYWAY.ADVDEDUCT.equals(lnsfeeinfo.getRepayway()) )
			oneTimeFee.selfAdd(lnsfeeinfo.getDeducetionamt());
		else if( ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsfeeinfo.getRepayway()) )
			oneTimeFee.selfAdd(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()).multiply(BigDecimal.valueOf(lnsfeeinfo.getFeerate())).setScale(2, RoundingMode.HALF_UP).doubleValue());
		else
			return;
		
		List<FabAmount> amts = new ArrayList<>();
		amts.add(TaxUtil.calcVAT(oneTimeFee));

		Map<String,Object> runneldata = new HashMap<>();
		runneldata.put("termretfee",oneTimeFee.getVal());//应还
		runneldata.put("feeamt", 0.00);//已还
		if(LoanFeeUtils.isAdvanceDeduce(productCode)) runneldata.put("feeamt", oneTimeFee.getVal());//已还
		TblLnsbasicinfoex lnsbasicinfoex = new TblLnsbasicinfoex();
		lnsbasicinfoex.setAcctno(receiptNo);
		lnsbasicinfoex.setValue1(lnsfeeinfo.getFeerate().toString());
		lnsbasicinfoex.setOpenbrc(ctx.getBrc());
		lnsbasicinfoex.setKey(ConstantDeclare.KEYNAME.GDDB);
		lnsbasicinfoex.setTunneldata( JsonTransfer.ToJson(runneldata));
		try {
			DbAccessUtil.execute("Lnsbasicinfoex.insert", lnsbasicinfoex);
		} catch (FabSqlException e) {
			throw new FabException("SPS100","lnsbasicinfoex",e);
		}
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		//2412624 2412626任性贷搭融担费，假设放款机构是51030000.子机构是51340000，那么，
		//事件的reserve3传R5103（截取51030000的前四位加R前缀）
		String reserv = "";
		if(LoanFeeUtils.isAdvanceDeduce(productCode))
			reserv = "R" + ctx.getBrc().substring(0, 4);
		else{
			//固定担保费需要结息
			LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(receiptNo,tranctx );
			//求总期数
			LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, tranctx.getTranDate(),tranctx);
			List<LnsBill> feeBills = new ArrayList<>();
			feeBills.addAll(lnsBillStatistics.getFutureBillInfoList());
			feeBills.addAll(lnsBillStatistics.getBillInfoList());

			for( LnsBill lnsBill:feeBills){
				if(ConstantDeclare.FEETYPE.SQFE.equals(lnsBill.getBillType())
						&&ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getRepayWay()))
				{
					LoanFeeUtils.settleFee(ctx,lnsBill ,la);
					return ;
				}
			}

		}
		//只有预扣的抛债务公司投保事件
		AccountingModeChange.saveFeeTax(tranctx, receiptNo, oneTimeFee.getVal(),
				TaxUtil.calcVAT(oneTimeFee).getVal(), lnsfeeinfo.getFeetype(),"C");
		//固定担保事件
		eventProvider.createEvent(ConstantDeclare.EVENT.BINSURANCE, oneTimeFee
				//一次性费用
				, lnsAcctInfo, null, loanAgreement.getFundInvest(),
				ConstantDeclare.BRIEFCODE.GDDB, ctx,amts ,"", lnsfeeinfo.getFeebrc(), "", reserv);
		
  	}
	/**
	 * @return the openBrc
	 */
	public String getOpenBrc() {
		return openBrc;
	}

	/**
	 * @param openBrc the openBrc to set
	 */
	public void setOpenBrc(String openBrc) {
		this.openBrc = openBrc;
	}

	/**
	 * @return the receiptNo
	 */
	public String getReceiptNo() {
		return receiptNo;
	}

	/**
	 * @param receiptNo the receiptNo to set
	 */
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	/**
	 * @return the startIntDate
	 */
	public String getStartIntDate() {
		return startIntDate;
	}

	/**
	 * @param startIntDate the startIntDate to set
	 */
	public void setStartIntDate(String startIntDate) {
		this.startIntDate = startIntDate;
	}

	/**
	 * @return the pkgList
	 */
	public ListMap getPkgList() {
		return pkgList;
	}

	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(ListMap pkgList) {
		this.pkgList = pkgList;
	}

	/**
	 * @return the loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	/**
	 * @param loanAgreement the loanAgreement to set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
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
	 * @return the feerepayWay
	 */
	public String getFeerepayWay() {
		return feerepayWay;
	}

	/**
	 * @param feerepayWay the feerepayWay to set
	 */
	public void setFeerepayWay(String feerepayWay) {
		this.feerepayWay = feerepayWay;
	}

	/**
	 * @return the feeBrc
	 */
	public String getFeeBrc() {
		return feeBrc;
	}

	/**
	 * @param feeBrc the feeBrc to set
	 */
	public void setFeeBrc(String feeBrc) {
		this.feeBrc = feeBrc;
	}

	/**
	 * @return the feeType
	 */
	public String getFeeType() {
		return feeType;
	}

	/**
	 * @param feeType the feeType to set
	 */
	public void setFeeType(String feeType) {
		this.feeType = feeType;
	}

	/**
	 * @return the calCulatrule
	 */
	public String getCalCulatrule() {
		return calCulatrule;
	}

	/**
	 * @param calCulatrule the calCulatrule to set
	 */
	public void setCalCulatrule(String calCulatrule) {
		this.calCulatrule = calCulatrule;
	}

	/**
	 * @return the feeBase
	 */
	public String getFeeBase() {
		return feeBase;
	}

	/**
	 * @param feeBase the feeBase to set
	 */
	public void setFeeBase(String feeBase) {
		this.feeBase = feeBase;
	}

	/**
	 * @return the advanceSettle
	 */
	public String getAdvanceSettle() {
		return advanceSettle;
	}

	/**
	 * @param advanceSettle the advanceSettle to set
	 */
	public void setAdvanceSettle(String advanceSettle) {
		this.advanceSettle = advanceSettle;
	}

	/**
	 * @return the deducetionAmt
	 */
	public FabAmount getDeducetionAmt() {
		return deducetionAmt;
	}

	/**
	 * @param deducetionAmt the deducetionAmt to set
	 */
	public void setDeducetionAmt(FabAmount deducetionAmt) {
		this.deducetionAmt = deducetionAmt;
	}

	/**
	 * @return the feeRate
	 */
	public FabAmount getFeeRate() {
		return feeRate;
	}

	/**
	 * @param feeRate the feeRate to set
	 */
	public void setFeeRate(FabAmount feeRate) {
		this.feeRate = feeRate;
	}

	/**
	 * @return the overRate
	 */
	public FabAmount getOverRate() {
		return overRate;
	}

	/**
	 * @param overRate the overRate to set
	 */
	public void setOverRate(FabAmount overRate) {
		this.overRate = overRate;
	}


	/**
	 * @return the productCode
	 */
	public String getProductCode() {
		return productCode;
	}


	/**
	 * @param productCode the productCode to set
	 */
	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	/**
	 * @return the repayWay
	 */
	public String getRepayWay() {
		return repayWay;
	}

	/**
	 * @param repayWay to set
	 */
	public void setRepayWay(String repayWay) {
		this.repayWay = repayWay;
	}
}
