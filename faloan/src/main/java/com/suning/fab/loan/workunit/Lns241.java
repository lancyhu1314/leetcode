package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsamortizeplan;
import com.suning.fab.loan.domain.TblLnsbasicinfoex;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  〈一句话功能简述〉
 *  〈功能详细描述〉：费用开户落费用静态表
 * @Author 18049705
 * @Date changed in 10:15 2019/5/29
 */

@Scope("prototype")
@Repository
public class Lns241 extends WorkUnit{

	String acctNo;
	private FabRate yearFeeRate;    //年费率
	private String childBrc;   //子机构
	private Integer periodNum; //期限数量

	String		receiptNo;//贷款账号
	String productCode; // 产品代码  2512617任性贷搭保险（平台开户）---新增   2412619个贷-外部平台贷（资产方开户）
    private String flag;  //1.一次性收取2.分期收取(默认)3.分期+一次性
	private FabRate	 onetimeRate;//一次性费率
	private String earlysettleFlag;
	private String periodUnit;
	private LoanAgreement loanAgreement;
	String repayWay;
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		FaChecker.checkFeeInput(productCode,flag ,onetimeRate ,yearFeeRate );
		TranCtx ctx = getTranctx();

		//分期费率
		if(LoanFeeUtils.rateIsPositive(yearFeeRate)){
//			//等本等息不支持加挂费用
//			if (VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_DBDX, ConstantDeclare.REPAYWAY.REPAYWAY_WILLFUL).contains(repayWay))
//				throw new FabException("TUP105");

			TblLnsfeeinfo lnsfeeinfo = LoanFeeUtils.prepareFeeinfo(loanAgreement,ctx,ConstantDeclare.FEEREPAYWAY.STAGING,yearFeeRate);
			saveFeeInfo(lnsfeeinfo);
		}

		//一次性费率
		if(LoanFeeUtils.rateIsPositive(onetimeRate)){
            TblLnsfeeinfo lnsfeeinfo = LoanFeeUtils.prepareFeeinfo(loanAgreement,ctx,ConstantDeclare.FEEREPAYWAY.ONETIME,onetimeRate);

            //登记摊销表
            if(LoanFeeUtils.isAdvanceDeduce(productCode))  registeredAmortization(ctx, lnsfeeinfo);
            saveFeeInfo(lnsfeeinfo);

            //固定担保费 抛固定担保费事件   除了保费的一次性是固定担保费
			if(!LoanFeeUtils.isPremium(productCode)) {
				//预扣费的
				FabAmount oneTimeFee=  new FabAmount(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()).multiply(onetimeRate.getVal()).setScale(2, RoundingMode.HALF_UP).doubleValue());
				List<FabAmount> amts = new ArrayList<>();
				amts.add(TaxUtil.calcVAT(oneTimeFee));

				Map<String,Object> runneldata = new HashMap<>();
				runneldata.put("termretfee",oneTimeFee.getVal());//应还
				runneldata.put("feeamt", 0.00);//已还
				if(LoanFeeUtils.isAdvanceDeduce(productCode)) runneldata.put("feeamt", oneTimeFee.getVal());//已还
				TblLnsbasicinfoex lnsbasicinfoex = new TblLnsbasicinfoex();
				lnsbasicinfoex.setAcctno(receiptNo);
				lnsbasicinfoex.setValue1(onetimeRate.toString());
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
						TaxUtil.calcVAT(oneTimeFee).getVal(), lnsfeeinfo.getFeetype());
				//固定担保事件
				eventProvider.createEvent(ConstantDeclare.EVENT.BINSURANCE, oneTimeFee
						//一次性费用
						, lnsAcctInfo, null, loanAgreement.getFundInvest(),
						ConstantDeclare.BRIEFCODE.GDDB, ctx,amts ,"", childBrc, "", reserv);

			}
			//0费率插表
		}else if(!LoanFeeUtils.rateIsPositive(yearFeeRate)){
			TblLnsfeeinfo lnsfeeinfo = LoanFeeUtils.prepareFeeinfo(loanAgreement,ctx,ConstantDeclare.FEEREPAYWAY.STAGING,yearFeeRate);
			lnsfeeinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
			lnsfeeinfo.setFeestat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
			saveFeeInfo(lnsfeeinfo);
		}

		//保费的事件
		discounts();
	}





    private void registeredAmortization(TranCtx ctx, TblLnsfeeinfo  lnsfeeinfo) throws FabException {
		// 摊销计划表
		TblLnsamortizeplan lnsamortizeplan = new TblLnsamortizeplan();
		lnsamortizeplan.setTrandate(Date.valueOf(ctx.getTranDate()));
		lnsamortizeplan.setSerseqno(ctx.getSerSeqNo());
		lnsamortizeplan.setBrc(ctx.getBrc());
		lnsamortizeplan.setAcctno(getReceiptNo());
		//扣费
		lnsamortizeplan.setAmortizetype("2");
		lnsamortizeplan.setCcy("01");
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

	private void saveFeeInfo(TblLnsfeeinfo lnsfeeinfo) throws FabException {
		try {
			DbAccessUtil.execute("Lnsfeeinfo.insert", lnsfeeinfo);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS100", "TblLnsfeeinfo");
		}
	}


	//保费的事件
	private void discounts() throws FabException {
		if(LoanFeeUtils.isPremium(productCode)) {
			//数据不全，需要重新查询
			LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(receiptNo,tranctx );
			//求总期数
			LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, tranctx.getTranDate(),tranctx);
			//统计  费用的期数
//			periodNum = LoanFeeUtils.countPeriod(loanAgreement, lnsBillStatistics,tranctx);//总期数
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
			map.put("flag", flag);
			map.put("childBrc", childBrc);
			if(null !=onetimeRate)
				map.put("onetimeRate",onetimeRate.toString() );
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





	/**
	 * Gets the value of earlysettleFlag.
	 *
	 * @return the value of earlysettleFlag
	 */
	public String getEarlysettleFlag() {
		return earlysettleFlag;
	}

	/**
	 * Sets the earlysettleFlag.
	 *
	 * @param earlysettleFlag earlysettleFlag
	 */
	public void setEarlysettleFlag(String earlysettleFlag) {
		this.earlysettleFlag = earlysettleFlag;

	}

	/**
	 *
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
	 * @return the yearFeeRate
	 */
	public FabRate getYearFeeRate() {
		return yearFeeRate;
	}


	/**
	 * @param yearFeeRate the yearFeeRate to set
	 */
	public void setYearFeeRate(FabRate yearFeeRate) {
		this.yearFeeRate = yearFeeRate;
	}


	/**
	 *
	 * @return the childBrc
	 */
	public String getChildBrc() {
		return childBrc;
	}


	/**
	 * @param childBrc the childBrc to set
	 */
	public void setChildBrc(String childBrc) {
		this.childBrc = childBrc;
	}


	/**
	 *
	 * @return the periodNum
	 */
	public Integer getPeriodNum() {
		return periodNum;
	}


	/**
	 * @param periodNum the periodNum to set
	 */
	public void setPeriodNum(Integer periodNum) {
		this.periodNum = periodNum;
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


	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public FabRate getOnetimeRate() {
		return onetimeRate;
	}

	public void setOnetimeRate(FabRate onetimeRate) {
		this.onetimeRate = onetimeRate;
	}

	/**
	 * Gets the value of periodUnit.
	 *
	 * @return the value of periodUnit
	 */
	public String getPeriodUnit() {
		return periodUnit;
	}

	/**
	 * Sets the periodUnit.
	 *
	 * @param periodUnit periodUnit
	 */
	public void setPeriodUnit(String periodUnit) {
		this.periodUnit = periodUnit;

	}

	/**
	 * Gets the value of loanAgreement.
	 *
	 * @return the value of loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	/**
	 * Sets the loanAgreement.
	 *
	 * @param loanAgreement loanAgreement
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;

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
