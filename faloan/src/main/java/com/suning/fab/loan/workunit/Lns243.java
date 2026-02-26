package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.FeeManageregM;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.account.LnsInterfaceQueryM;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉：费用冲销
 *
 * @author
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns243 extends WorkUnit{


	String acctNo;
	String errDate;		// 错误日期
	String errSerSeq;	// 错误流水号
//	String childBrc;
	TblLnseventreg lnseventreg;
	List<FeeManageregM> feeList;
	@Autowired
	LoanEventOperateProvider eventProvider;
	private LoanAgreement loanAgreement;
	private Integer txseqno = 0;
	private String customID;
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator suber;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();

 		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx,loanAgreement);
		if(loanAgreement.getFeeAgreement().isEmpty()){
			throw new FabException("LNS003");
		}

		//管理费统计
		//判断有没有重复子机构 是否已经还款
		Map<String, Object> param = new HashMap<String, Object>();

		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());
		param.put("feetypes", LoanFeeUtils.getFeetypes());
		List<TblLnsbill> feeList;
		String billType="";
		try {
			feeList = DbAccessUtil.queryForList("Lnsfeeinfo.queryFeeBills", param, TblLnsbill.class);
		} catch (FabException e) {
			throw new FabException(e, "SPS103", "queryFeeBills");
		}
//		String childBrc = loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getFeebrc();

		//结费冲销事件
		for(TblLnsbill lnsbill:feeList){
			billType=lnsbill.getBilltype().trim();
			if(!lnsbill.getBillstatus().equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL)){
				throw new FabException("LNS003");
			}

			String  childBrc = LoanFeeUtils.matchFeeInfo(loanAgreement, lnsbill.getBilltype().trim(), lnsbill.getRepayway().trim()).getFeebrc();
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA,
					"N", new FabCurrency(),childBrc);
			List<FabAmount> amtList = new ArrayList<>();
			amtList.add(TaxUtil.calcVAT(new FabAmount(lnsbill.getBillbal())));
			suber.operate(lnsAcctInfo, null, new FabAmount(lnsbill.getBillbal()), loanAgreement.getFundInvest(),
					LoanFeeUtils.feeWriteOffBrief(lnsbill), ctx);

			eventProvider.createEvent(ConstantDeclare.EVENT.WFFEESETLE,
					new FabAmount(lnsbill.getBillbal()),
					lnsAcctInfo,
					null,
					loanAgreement.getFundInvest(),
					LoanFeeUtils.feeWriteOffBrief(lnsbill),
					ctx,
					amtList,
					lnsbill.getTrandate().toString(),
					lnsbill.getSerseqno(),
					lnsbill.getTxseq(),childBrc
					);
			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, getAcctNo(), ConstantDeclare.EVENT.WFFEESETLE, LoanFeeUtils.feeWriteOffBrief(lnsbill),
					lnsbill.getBillbal(), "", loanAgreement.getFundInvest(), amtList.get(0).getVal(), childBrc);

			//允许固定担保费还款后可冲销 20190731
			if(lnsbill.getRepayway().equals(ConstantDeclare.FEEREPAYWAY.ONETIME)  && lnsbill.getBilltype().equals(ConstantDeclare.FEETYPE.SQFE)){

//				FabAmount tranamt = new FabAmount(lnsbill.getBillamt());
//				tranamt.selfSub(lnsbill.getBillbal());
//				if(tranamt.isPositive()) {
//					eventProvider.createEvent(ConstantDeclare.EVENT.CINSURANCE,tranamt , lnsAcctInfo, null,
//							loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.DBCX, ctx, "", childBrc);
//
//					AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, getAcctNo(), ConstantDeclare.EVENT.CINSURANCE, ConstantDeclare.BRIEFCODE.DBCX,
//							tranamt.getVal(), "", loanAgreement.getFundInvest(), 0.0, childBrc);
//				}
//				if(!new FabAmount(lnsbill.getBillamt()).sub(lnsbill.getBillbal()).isZero())
//				{
//					eventProvider.createEvent(ConstantDeclare.EVENT.WFFEESETLE,
//							new FabAmount(lnsbill.getBillbal()),
//							acctinfo,
//							null,
//							loanAgreement.getFundInvest(),
//							LoanFeeUtils.feeWriteOffBrief(lnsbill),
//							ctx,
//							amtList,
//							lnsbill.getTrandate().toString(),
//							lnsbill.getSerseqno(),
//							lnsbill.getTxseq(),
//							loanAgreement.getCustomer().getMerchantNo(),
//							loanAgreement.getBasicExtension().getDebtCompany());
//					AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, getAcctNo(), ConstantDeclare.EVENT.WFFEESETLE, LoanFeeUtils.feeWriteOffBrief(lnsbill),
//							lnsbill.getBillbal(), "", loanAgreement.getFundInvest(), amtList.get(0).getVal(), childBrc);
//				}
				continue;
			}

			if(!new FabAmount(lnsbill.getBillamt()).sub(lnsbill.getBillbal()).isZero()){
			    queryRpy();
			}

		}

		param.put("taxtype", ConstantDeclare.KEYNAME.FYYT);
		//查询税金相关表   select * from lnstaxdetail where acctno = :acctno  and brc = :brc and taxtype = 'JX'
		List<TblLnstaxdetail> lnstaxdetails;
		try {
			lnstaxdetails = DbAccessUtil.queryForList("AccountingMode.query_lnstaxdetail", param, TblLnstaxdetail.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsaccountdyninfo");
		}



		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());



		//保费 没有计提
		if(LoanFeeUtils.isPremium(loanAgreement.getPrdId())) {
			if(VarChecker.isEmpty(customID))
				throw  new FabException("LNS055","保险公司商户号");
			FabAmount feeAmount = new FabAmount();
			List<FabAmount> amts = new ArrayList<>();

			for(TblLnstaxdetail lnstaxdetail:lnstaxdetails){
				if(ConstantDeclare.FEETYPE.ISFE.equals(lnstaxdetail.getBilltype())) {
					feeAmount.selfAdd(lnstaxdetail.getTranamt());
					amts.add(new FabAmount(lnstaxdetail.getTax()));
				}

			}
			String childBrc = "";
			for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
				if(ConstantDeclare.FEETYPE.ISFE.equals(lnsfeeinfo.getFeetype()))
					childBrc = lnsfeeinfo.getFeebrc();
			}
			//兼容老数据
			if(!feeAmount.isPositive()){
				Map<String,Object> queryparam = new HashMap<>();
				queryparam.put("acctno", acctNo);
				queryparam.put("brc", ctx.getBrc());
				queryparam.put("key", ConstantDeclare.KEYNAME.BI);
				TblLnsinterfaceex interfaceex;
				try {
					interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex", queryparam, TblLnsinterfaceex.class);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS103", "query_lnsinterfaceex");
				}
				feeAmount.selfAdd(JSONObject.parseObject(interfaceex.getValue()).getDouble("sumfee"));
				amts.add(TaxUtil.calcVAT(feeAmount));

			}
			// 登记费用退保
			eventProvider.createEvent(ConstantDeclare.EVENT.CINSURANCE, feeAmount, lnsAcctInfo, null,
					loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.CXTB, ctx,amts,customID,childBrc);
			Map<String,Object> map = new HashMap<>();
			map.put("customID", customID);
			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, getAcctNo(), ConstantDeclare.EVENT.CINSURANCE, ConstantDeclare.BRIEFCODE.CXTB,
					feeAmount.getVal(), "", loanAgreement.getFundInvest(),amts.get(0).getVal(), childBrc,JsonTransfer.ToJson(map));
		}else {


//			TblLnsprovisionreg mfprovision;
			TblLnsprovision lnsprovision;
			Map<String,Object> provparam = new HashMap<>();
			provparam.put("receiptno", getAcctNo());
			provparam.put("intertype", ConstantDeclare.INTERTYPE.MANAGEFEE);
			billType = loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getFeetype();
			try {
			//	mfprovision = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_lastperiod", provparam, TblLnsprovisionreg.class);
				lnsprovision=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,getAcctNo(),billType, ConstantDeclare.INTERTYPE.MANAGEFEE, loanAgreement);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsprovision");
			}
			if(lnsprovision.isExist()){
				// 登记费用冲销事件
				List<FabAmount> amtList = new ArrayList<>();
				amtList.add(new FabAmount(lnsprovision.getTotaltax()));
//				lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFFEE, new FabAmount(lnsprovision.getTotalinterest()), lnsAcctInfo, null,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FYCX, ctx,amtList, lnsprovision.getChildbrc());
				FabAmount theInt = new FabAmount();
				FabAmount theTax = new FabAmount();
				if(CalendarUtil.equalDate(lnsprovision.getLastenddate().toString() ,ctx.getTranDate()) ){
					if(ConstantDeclare.INTERFLAG.POSITIVE.equals(lnsprovision.getInterflag())){
//						theInt.selfAdd(mfprovision.getInterest());
//						theTax.selfAdd(mfprovision.getTax());
						theInt.selfAdd(lnsprovision.getLastinterest());
						theTax.selfAdd(lnsprovision.getLasttax());
					}else{
//						theInt.selfSub(mfprovision.getInterest());
//						theTax.selfSub(mfprovision.getTax());
						theInt.selfSub(lnsprovision.getLastinterest());
						theTax.selfSub(lnsprovision.getLasttax());
					}

				}
				AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, getAcctNo(), ConstantDeclare.EVENT.WRITOFFFEE, ConstantDeclare.BRIEFCODE.FYCX,
						lnsprovision.getTotalinterest(), "", loanAgreement.getFundInvest(),lnsprovision.getTotaltax(),theInt.getVal(),theTax.getVal(), lnsprovision.getChildbrc());

			}
			//固定担保费用冲销
			/*


			需将固定担保费计提冲销（WRITOFFFEE原管理费冲销事件，区分摘要码为DBCX担保冲销），
			将固定担保预收账务冲销用（CINSURANCE退保事件，区分摘要码为DBCX担保冲销）
			 */
//			TblLnsprovisionreg mfprovisionreg;
			TblLnsprovision lnsprovisionReg;
			provparam.put("intertype", ConstantDeclare.INTERTYPE.SECURITFEE);
			try {
		//		mfprovisionreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_lastperiod", provparam, TblLnsprovisionreg.class);
				lnsprovisionReg=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,getAcctNo(),billType,  ConstantDeclare.INTERTYPE.SECURITFEE, loanAgreement);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsprovision");
			}
			if(lnsprovisionReg.isExist()){
				//  固定担保费用计提冲销事件
				List<FabAmount> amtList = new ArrayList<>();
				amtList.add(new FabAmount(lnsprovisionReg.getTotaltax()));
//				lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFFEE, new FabAmount(lnsprovisionReg.getTotalinterest()), lnsAcctInfo, null,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.DBCX, ctx,amtList, lnsprovisionReg.getChildbrc());
				FabAmount theInt = new FabAmount();
				FabAmount theTax = new FabAmount();
				if(CalendarUtil.equalDate(lnsprovisionReg.getLastenddate().toString() ,ctx.getTranDate()) ){
					if(ConstantDeclare.INTERFLAG.POSITIVE.equals(lnsprovisionReg.getInterflag())){
//						theInt.selfAdd(mfprovisionreg.getInterest());
//						theTax.selfAdd(mfprovisionreg.getTax());
						theInt.selfAdd(lnsprovisionReg.getLastinterest());
						theTax.selfAdd(lnsprovisionReg.getLasttax());
					}else{
//						theInt.selfSub(mfprovisionreg.getInterest());
//						theTax.selfSub(mfprovisionreg.getTax());
						theInt.selfSub(lnsprovisionReg.getLastinterest());
						theTax.selfSub(lnsprovisionReg.getLasttax());
					}

				}
				AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, getAcctNo(), ConstantDeclare.EVENT.WRITOFFFEE, ConstantDeclare.BRIEFCODE.DBCX,
						lnsprovisionReg.getTotalinterest(), "", loanAgreement.getFundInvest(),lnsprovisionReg.getTotaltax(),theInt.getVal(),theTax.getVal(), lnsprovisionReg.getChildbrc());
			}
			//如果存在固定担保 预扣的
			if(lnstaxdetails!=null) {
				for (TblLnstaxdetail lnstaxdetail : lnstaxdetails) {
					//预扣费
					if ("".equals(lnstaxdetail.getBilltype()) || "C".equals(lnstaxdetail.getReserv1())) {
						FabAmount termretfee =  new FabAmount(lnstaxdetail.getTranamt());
//						List<FabAmount> amts = new ArrayList<>();
//
//						amts.add(TaxUtil.calcVAT(new FabAmount(lnstaxdetail.getTax())));

//						 lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

						//如果已还大于0
						String childBrc = "";
						for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
							if("".equals(lnsfeeinfo.getFeetype()) || ConstantDeclare.FEEREPAYWAY.ADVDEDUCT.equals(lnsfeeinfo.getRepayway()))
								childBrc = lnsfeeinfo.getFeebrc();
						}
						if(termretfee.isPositive()){
							//2412624任性贷搭融担费，假设放款机构是51030000.子机构是51340000，那么，
							//事件的reserve3传R5103（截取51030000的前四位加R前缀）
							String reserv = "";
							if(VarChecker.asList("2412624","2412626").contains(loanAgreement.getPrdId()))
							{
								reserv = "R"+ctx.getBrc().substring(0,4 );
							}
							List<FabAmount> amts = new ArrayList<>();
							amts.add(TaxUtil.calcVAT(termretfee));
							eventProvider.createEvent(ConstantDeclare.EVENT.CINSURANCE, termretfee, lnsAcctInfo, null,
									loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.DBCX, ctx,amts, "", childBrc,"",reserv);


							AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, getAcctNo(), ConstantDeclare.EVENT.CINSURANCE, ConstantDeclare.BRIEFCODE.DBCX,
									termretfee.getVal(), "", loanAgreement.getFundInvest(), amts.get(0).getVal(), childBrc);
						}
					}
				}
			}else {
				//兼容老数据

				for (TblLnsfeeinfo lnsfeeinfo : loanAgreement.getFeeAgreement().getLnsfeeinfos()) {
					if (lnsfeeinfo.getRepayway().equals(ConstantDeclare.FEEREPAYWAY.ADVDEDUCT)) {

						//退保费用
						FabAmount termretfee = new FabAmount(lnsfeeinfo.getDeducetionamt());

						//当天固定担保费还款费用
//						FabAmount repayToday = new FabAmount();
//						lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

						if (termretfee.isPositive()) {
							//2412624任性贷搭融担费，假设放款机构是51030000.子机构是51340000，那么，
							//事件的reserve3传R5103（截取51030000的前四位加R前缀）
							String reserv = "";
							if(VarChecker.asList("2412624","2412626").contains(loanAgreement.getPrdId())) {
								reserv = "R" + ctx.getBrc().substring(0, 4);
							}
							List<FabAmount> amts = new ArrayList<>();
							amts.add(TaxUtil.calcVAT(termretfee));
							eventProvider.createEvent(ConstantDeclare.EVENT.CINSURANCE, termretfee, lnsAcctInfo, null,
									loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.DBCX, ctx,amts, "", lnsfeeinfo.getFeebrc(), "", reserv);

							AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, getAcctNo(), ConstantDeclare.EVENT.CINSURANCE, ConstantDeclare.BRIEFCODE.DBCX,
									termretfee.getVal(), "", loanAgreement.getFundInvest(), amts.get(0).getVal(), lnsfeeinfo.getFeebrc());
						}
					}
				}
			}

		}
		//冲销完成后，忘lnsbasicinfocal中插入该冲销记录，同时更新幂等登记薄中acctname,acctno,tranamt
		try{
			param.clear();
			param.put("acctno", acctNo);
			param.put("brc", ctx.getBrc());
			param.put("feestat", ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
			param.put("provisionflag", ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);


			/*更新费用信息表该数据的贷款状态CA,以及利息计提标志CLOSE*/
			DbAccessUtil.execute("Lnsfeeinfo.update_feeinfo_loanstatEx", param);

			/*冲销时将该条记录插入到lnsbasicinfocal表中*/
			DbAccessUtil.execute("Lnsfeeinfo.insert_feeinfocal", param);


		} catch (FabSqlException e) {
			throw new FabException(e, "SPS101", "update_feeinfo_loanstatEx");
		}
	}

	//查询一下有没有过还款  融担代偿的
	private void  queryRpy() throws FabException {
		//查询幂等表  当天的幂等表管理费还款  固定担保brc不同
		Map<String,Object> query = new HashMap<>();
		query.put("acctno", acctNo);
		query.put("trancode", "471011");
		List<LnsInterfaceQueryM> interfaces;
		try{
			interfaces = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsinterface_243",query , LnsInterfaceQueryM.class);
		}catch(FabException e){
			throw new FabException(e, "SPS103", "LNSINTERFACE");
		}

		if(interfaces!=null && !interfaces.isEmpty())
		    throw new FabException("LNS003");
	}


	/**
	 *
	 * @return the feeList
	 */
	public List<FeeManageregM> getFeeList() {
		return feeList;
	}



	/**
	 * @param feeList the feeList to set
	 */
	public void setFeeList(List<FeeManageregM> feeList) {
		this.feeList = feeList;
	}



	public String getAcctNo() {
		return acctNo;
	}

	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	public String getErrDate() {
		return errDate;
	}

	/**
	 *
	 * @return the lnseventreg
	 */
	public TblLnseventreg getLnseventreg() {
		return lnseventreg;
	}

	/**
	 * @param lnseventreg the lnseventreg to set
	 */
	public void setLnseventreg(TblLnseventreg lnseventreg) {
		this.lnseventreg = lnseventreg;
	}

	public void setErrDate(String errDate) {
		this.errDate = errDate;
	}

	public String getErrSerSeq() {
		return errSerSeq;
	}

	public void setErrSerSeq(String errSerSeq) {
		this.errSerSeq = errSerSeq;
	}

	public LoanEventOperateProvider getEventProvider() {
		return eventProvider;
	}

	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}


	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}

	public Integer getTxseqno() {
		return txseqno;
	}

	public void setTxseqno(Integer txseqno) {
		this.txseqno = txseqno;
	}

	public String getCustomID() {
		return customID;
	}

	public void setCustomID(String customID) {
		this.customID = customID;
	}
}
