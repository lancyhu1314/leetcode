package com.suning.fab.loan.utils;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.domain.*;
import com.suning.fab.tup4j.utils.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.suning.fab.loan.la.AbstractUserDefineAgreement;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.la.Product;
import com.suning.fab.loan.la.UserDefineAgreement;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.supporter.RepayWaySupporter;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.currency.FabCurrency;
import scala.collection.immutable.Stream;

@Component
@Scope("singleton")
public class LoanAgreementProvider {

	Map<String,Product> products;
	public static LoanAgreement genLoanAgreement(String prdId) throws FabException{
		LoanAgreement loanAgreement = new LoanAgreement();

		if (getProducts().get("pd"+prdId) == null)
		{
			throw new FabException("LNS006",prdId);
		}
		MapperUtil.map(getProducts().get("pd"+prdId), loanAgreement);

		return loanAgreement;
	}
	public static LoanAgreement genLoanAgreementFromDB(String acctNo, TranCtx ctx,LoanAgreement loanAgreement) throws FabException {
		if(VarChecker.isEmpty(loanAgreement))
			return  genLoanAgreementFromDB(acctNo,ctx);
		if(VarChecker.isEmpty(loanAgreement.getInterestAgreement()))
			return  genLoanAgreementFromDB(acctNo,ctx);
		return loanAgreement;
	}
	/**
	 * 根据数据库中对应账号的贷款信息给LoanAgreement赋值
	 * @param acctNo    贷款账号
	 * @param ctx    交易信息上下文
	 * @return
	 * @throws FabException
	 */
	public static LoanAgreement genLoanAgreementFromDB(String acctNo, TranCtx ctx) throws FabException {
		if (VarChecker.isEmpty(acctNo))
			throw new FabException("ACC103");

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo;
		try{
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		LoanAgreement loanagreement = LoanAgreementProvider.genLoanAgreement(lnsbasicinfo.getPrdcode());
		//借新还旧放款渠道存入la
		if( null != ctx.getRequestDict("channelType") )
			loanagreement.getBasicExtension().setChannelType(ctx.getRequestDict("channelType").toString());

		loanagreement.getContract().setContractAmt(new FabAmount(lnsbasicinfo.getContractamt())); //合同金额(取表里合同余额)
		loanagreement.getContract().setContractNo(lnsbasicinfo.getContrasctno()); //合同编号
		loanagreement.getContract().setReceiptNo(lnsbasicinfo.getAcctno()); //借据号
		loanagreement.getContract().setCcy(new FabCurrency(lnsbasicinfo.getCcy()));//币 种 号
		//6神要求起息日和合同开始日期一样
		loanagreement.getContract().setContractStartDate(lnsbasicinfo.getBeginintdate());
		loanagreement.getContract().setContractEndDate(lnsbasicinfo.getContduedate());
		loanagreement.getContract().setBalance(new FabAmount(lnsbasicinfo.getContractbal()));
		if(VarChecker.isEmpty(lnsbasicinfo.getLastprindate())){
			loanagreement.getContract().setRepayPrinDate(loanagreement.getContract().getContractStartDate());//合同开始日期
		}else{
			if("true".equals(loanagreement.getInterestAgreement().getIsCalTail()) && !lnsbasicinfo.getLastprindate().equals(loanagreement.getContract().getContractStartDate())){
				loanagreement.getContract().setRepayPrinDate(CalendarUtil.nDaysAfter(lnsbasicinfo.getLastprindate(), 1).toString("yyyy-MM-dd"));
			}else{
				loanagreement.getContract().setRepayPrinDate(lnsbasicinfo.getLastprindate());
			}
		}
		if(VarChecker.isEmpty(lnsbasicinfo.getLastintdate())){
			loanagreement.getContract().setRepayIntDate(lnsbasicinfo.getBeginintdate());//起息日期
		}else{
			if("true".equals(loanagreement.getInterestAgreement().getIsCalTail()) && !lnsbasicinfo.getLastintdate().equals(loanagreement.getContract().getContractStartDate())){
				loanagreement.getContract().setRepayIntDate(CalendarUtil.nDaysAfter(lnsbasicinfo.getLastintdate(), 1).toString("yyyy-MM-dd"));
			}else{
				loanagreement.getContract().setRepayIntDate(lnsbasicinfo.getLastintdate());
			}

		}
		loanagreement.getContract().setStartIntDate(lnsbasicinfo.getBeginintdate());
		loanagreement.getContract().setGraceDays(lnsbasicinfo.getGracedays());//宽限期
		loanagreement.getContract().setFlag1(lnsbasicinfo.getFlag1());//还款日期		!!!
		loanagreement.getContract().setDiscountAmt(new FabAmount(lnsbasicinfo.getDeductionamt()));//扣息金额

		/**
		loanagreement.getContract().setDiscountAmt(new FabAmount(lnsbasicinfo.getAmtres2()));//扣息金额
		loanagreement.getCustomer().setCustomId("");//客户号
		loanagreement.getCustomer().setMerchantNo("");//商户号		!!!
		loanagreement.getCustomer().setCustomType("");//客户类别		!!!
		 */
		loanagreement.getCustomer().setCustomName("");//客户名称		!!!
		//2020-09-27
		loanagreement.getCustomer().setCustomType(lnsbasicinfo.getCusttype());//客户类别		!!!


		loanagreement.getFundInvest().setInvestee(lnsbasicinfo.getInvestee()); //接收投资者
		loanagreement.getFundInvest().setInvestMode(lnsbasicinfo.getInvestmode()); //投放模式：保理、消费贷
		loanagreement.getFundInvest().setChannelType(lnsbasicinfo.getChanneltype());//放款渠道      1-银行   2-易付宝  3-任性贷
		loanagreement.getFundInvest().setFundChannel(lnsbasicinfo.getFundchannel());//资金通道    sap银行科目编号/易付宝总账科目
		loanagreement.getFundInvest().setOutSerialNo(lnsbasicinfo.getOutserialno());//外部流水单号：银行资金流水号/易付宝交易单号
		/**
		loanagreement.setAvailable()产品是否可用
		loanagreement.setPrdName("");
		*/
		loanagreement.setPrdId(lnsbasicinfo.getPrdcode());
		// RateAgreement
		loanagreement.getRateAgreement().setNormalRate(new FabRate(lnsbasicinfo.getNormalrate()));
		loanagreement.getRateAgreement().setOverdueRate(new FabRate(lnsbasicinfo.getOverduerate()));
		loanagreement.getRateAgreement().setCompoundRate(new FabRate(lnsbasicinfo.getOverduerate1()));

		//暂时处理，平台层升级后去掉
		loanagreement.getRateAgreement().getNormalRate().setRate(loanagreement.getRateAgreement().getNormalRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));
		loanagreement.getRateAgreement().getOverdueRate().setRate(loanagreement.getRateAgreement().getOverdueRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));
		loanagreement.getRateAgreement().getCompoundRate().setRate(loanagreement.getRateAgreement().getCompoundRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));
		/**
		 *private FeeAgreement feeAgreement
		 *private RangeLimit rangeLimit
		 *private GrantAgreement grantAgreement
		 *private OpenAgreement openAgreement
		 *private InterestAgreement interestAgreement
		 */
		loanagreement.getInterestAgreement().setIntFormula(lnsbasicinfo.getIntformula());// 利息公式 :1-等额本息公式   2-其他
		loanagreement.getWithdrawAgreement().setRepayAmtFormula(lnsbasicinfo.getPrinformula()); // 还款金额公式:1-等额本息  2-其他
		/**
		loanagreement.getInterestAgreement().setIntBase("ACT"); //计息基础   ACT-实际天数   YMD-对年对月对日(xml取)
		loanagreement.getInterestAgreement().setPeriodMinDays(15);(xml)
		loanagreement.getWithdrawAgreement().setRepayChannel("");// 还款渠道
		*/
		loanagreement.getInterestAgreement().setPeriodFormula(lnsbasicinfo.getIntperformula());
		loanagreement.getInterestAgreement().setIsCalInt(lnsbasicinfo.getIscalint());
		loanagreement.getInterestAgreement().setPeriodMinDays(lnsbasicinfo.getIntmindays());

		//private WithdrawAgreement withdrawAgreement
		loanagreement.getWithdrawAgreement().setPeriodFormula(lnsbasicinfo.getPrinperformula());
		loanagreement.getWithdrawAgreement().setPeriodMinDays(lnsbasicinfo.getPrinmindays());
		loanagreement.getWithdrawAgreement().setRepayWay(lnsbasicinfo.getRepayway());     //还款方式
		/**
		loanagreement.getWithdrawAgreement().setIsPrepay(false);// 是否提前还款
		loanagreement.getWithdrawAgreement().setRepayDateOpt("OPTDATE"); // 还款日期选择 MONTHHEAD-月初    MONTHEND-月末     OPENDATE-开户日  OPTDATE-指定日期
		loanagreement.getWithdrawAgreement().setPeriodMinDays(15);
		loanagreement.getWithdrawAgreement().setIsAgreePartRepay(""); // 是否同意部分还款
		loanagreement.getWithdrawAgreement().setGenCurrRepayPlanOpt("");  //生成还款计划时机
		loanagreement.getWithdrawAgreement().setIntIssueS("");	//扣息周期		定义到产品层
		loanagreement.getWithdrawAgreement().setIssueType("");//期限类型		定义到产品层
		*/
		//增加本金当前期和利息当前期
		loanagreement.getContract().setCurrIntPeriod(lnsbasicinfo.getCurintterm());
		loanagreement.getContract().setCurrPrinPeriod(lnsbasicinfo.getCurprinterm());
		//增加贷款状态  2020-07-02
		loanagreement.getContract().setLoanStat(lnsbasicinfo.getLoanstat());

		if(ConstantDeclare.REPAYWAY.REPAYWAY_XXHB.equals(loanagreement.getWithdrawAgreement().getRepayWay()) ||
		   ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(loanagreement.getWithdrawAgreement().getRepayWay())||
		   ConstantDeclare.REPAYWAY.REPAYWAY_YBYX.equals(loanagreement.getWithdrawAgreement().getRepayWay()) ||
		   ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(loanagreement.getWithdrawAgreement().getRepayWay())) //取非标自定义信息
		{
			loanagreement.setUserDefineAgreement(getUserDefineAgreementList(loanagreement,ctx));
		}

//		//展期判断做相关字段生成
//		if(!VarChecker.isEmpty(lnsbasicinfo.getFlag1()) && lnsbasicinfo.getFlag1().contains("A")){
//			param.put("key", "ZQ");
//			TblLnsbasicinfoex lnsbasicinfoex;
//			try{
//				lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsbasicinfoex");
//			}
//			if (null != lnsbasicinfoex) {
//				loanagreement.getBasicExtension().setBalBeforeEx(new FabAmount(lnsbasicinfoex.getValue2()));
//				loanagreement.getBasicExtension().setExtnums(lnsbasicinfo.getExtnums());
//				loanagreement.getBasicExtension().setLastPeriodStr(lnsbasicinfoex.getValue1());
//			}
//
//		}




//		//买方付息债务公司信息
//		if( Arrays.asList("3010013","3010015").contains(lnsbasicinfo.getPrdcode()) ){
//			param.put("key", "MFFX");
//			TblLnsbasicinfoex lnsbasicinfoex;
//			try{
//				lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsbasicinfoex");
//			}
//			if (null != lnsbasicinfoex) {
//				loanagreement.getBasicExtension().setDebtCompany(lnsbasicinfoex.getValue1());
//				loanagreement.getBasicExtension().setDebtAmt(new FabAmount(lnsbasicinfoex.getValue2()));
//				loanagreement.getCustomer().setMerchantNo(lnsbasicinfo.getCustomid());
//			}
//
//		}

//		//无追保理债务公司信息
//		if( Arrays.asList("3010006","3010014").contains(lnsbasicinfo.getPrdcode()) ){
//			param.put("key", "WZBL");
//			TblLnsbasicinfoex lnsbasicinfoex;
//			try{
//				lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsbasicinfoex");
//			}
//			if (null != lnsbasicinfoex) {
//				loanagreement.getBasicExtension().setJsonStr(lnsbasicinfoex.getTunneldata());
//				loanagreement.getCustomer().setMerchantNo(lnsbasicinfo.getCustomid());
//			}
//
//		}


//		//la加载汽车租赁款款方式
//		if( "2412617".equals(lnsbasicinfo.getPrdcode()) ){
//			param.put("key", "QCZL");
//			TblLnsbasicinfoex lnsbasicinfoex;
//			try{
//				lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsbasicinfoex");
//			}
//			if (null != lnsbasicinfoex) {
//				loanagreement.getBasicExtension().setCarRepayWay(lnsbasicinfoex.getValue1());
//			}
//
//		}

//		//封顶计息
		if(!VarChecker.isEmpty(loanagreement.getInterestAgreement().getCapRate()) ){
			/**
			 * 开户时插表
			 * lnsbasicinfoex.getValue1()收益截止日期十位(最近一次计算利息或者罚息的日期),该字段数据库结构32位,利息优先级>罚息
			 * 封顶计息收益截止日期一开始存在Lnsbasicinfoex表的value1,之后存在lnsbasicinfoDyn
			 * 累计利息,累计罚息(只到逾期),累计呆滞呆账罚息一直存存在lnsbasicinfoDyn		|14050183
			 */
            loanagreement.getBasicExtension().setCapAmt(DynamicCapUtil.calculateCappingVal(loanagreement));
//			loanagreement.getBasicExtension().setCapAmt(new FabAmount(BigDecimal.valueOf(loanagreement.getInterestAgreement().getCapRate())
//					.divide(new BigDecimal(366), 20 ,BigDecimal.ROUND_HALF_UP)
//					.multiply(BigDecimal.valueOf(loanagreement.getContract().getContractAmt().getVal()))
//					.multiply(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(loanagreement.getContract().getStartIntDate(), loanagreement.getContract().getContractEndDate())))
//					.setScale(2,BigDecimal.ROUND_HALF_UP ).doubleValue()));
			param.put("key", "FDJX");
			TblLnsbasicinfoex lnsbasicinfoex;
			try{
				lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsbasicinfoex");
			}
			if (null != lnsbasicinfoex) {
//				loanagreement.getBasicExtension().setCapAmt(new FabAmount(lnsbasicinfoex.getValue2()) );
				loanagreement.getBasicExtension().setComeinDate(lnsbasicinfoex.getValue1() );
			}

			//2019-05-06 lnsbasicinfoDyn目前仅供封顶计息使用
			/*
			 * sumint		DECIMAL(17,2)		累计利息
			 * sumdint		DECIMAL(17,2)		累计罚息(只到逾期)
			 * sumlbdcint	DECIMAL(17,2)		累计呆滞呆账罚息
			 * TRANDATE		DATE		上次更新日
			 */
			TblLnsbasicinfodyn lnsbasicinfoDyn;
			try{
				lnsbasicinfoDyn = DbAccessUtil.queryForObject("Lnsbasicinfodyn.selectByUk", param, TblLnsbasicinfodyn.class);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsbasicinfoDyn");
			}
			if (null != lnsbasicinfoDyn) {
				//sumcint目前都是0		2019-05-06|14050183
				loanagreement.getBasicExtension().setHisDintComein(new FabAmount(lnsbasicinfoDyn.getSumdint()+lnsbasicinfoDyn.getSumcint()) );
				loanagreement.getBasicExtension().setHisLBDintComein(new FabAmount(lnsbasicinfoDyn.getSumlbcdint()) );
				loanagreement.getBasicExtension().setComeinDate(new SimpleDateFormat("yyyy-MM-dd").format(lnsbasicinfoDyn.getTrandate()));
			}

			//老数据
			/**
			 * C:房抵贷封顶计息新数据
			 *
			 */
			//20190507|14050183
			if(!loanagreement.getContract().getFlag1().contains("C")){
				TblLnspenintprovreg lnspenintprovreg;
				param.put("billtype", ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
				param.put("receiptno", acctNo);
				param.put("brc", ctx.getBrc());
				try{
					//Lnspenintprovreg罚息计提登记簿
					lnspenintprovreg = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
				}catch (FabSqlException e){
					throw new FabException(e, "SPS103", "lnsbasicinfoex");
				}
				if (null != lnspenintprovreg) {
					loanagreement.getBasicExtension().setHisDintComein(new FabAmount(lnspenintprovreg.getTotalinterest().doubleValue()) );
					loanagreement.getBasicExtension().setComeinDate(lnspenintprovreg.getEnddate().toString());
				}else{
					loanagreement.getBasicExtension().setComeinDate(lnsbasicinfo.getBeginintdate());
				}
				//计提日对比
//				Map<String,Object> provision = new HashMap<String,Object>();
//				provision.put("receiptno", acctNo);
//				provision.put("intertype", ConstantDeclare.INTERTYPE.PROVISION);
//				provision.put("billtype",)

				TblLnsprovision lnsprovision;
				//Lnsprovisionreg利息计提登记簿
				try {
					/**
					 * SELECT RECEIPTNO, INTERTYPE, PERIOD, TRANDATE, SERSEQNO, TXNSEQ, BRC, TELLER, BILLTYPE,
			      	 *		  CCY, TOTALINTEREST, TOTALTAX, TAXRATE, INTEREST, TAX, BEGINDATE, ENDDATE, INTERFLAG,
			      	 *	  	  SENDFLAG, SENDNUM, CANCELFLAG, TIMESTAMP, RESERV1, RESERV2
			      	 * 	 FROM LNSPROVISIONREG
		     		 *	WHERE RECEIPTNO = :receiptno
		       		 *	  AND INTERTYPE = :intertype
		       		 *	  AND PERIOD = (SELECT max(PERIOD) FROM LNSPROVISIONREG
		       		 *	WHERE RECEIPTNO = :receiptno
	                 *	  AND INTERTYPE = :intertype)
					 */
					//lnsprovision = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_lastperiod", provision, TblLnsprovisionreg.class);
					lnsprovision=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,loanagreement.getContract().getReceiptNo(),ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.PROVISION,loanagreement);

				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS103", "lnsprovision");
				}
				//利息计提登记簿为空罚息计提登记簿也应该为空
				if (!lnsprovision.isExist()){
					loanagreement.getBasicExtension().setComeinDate(lnsbasicinfo.getBeginintdate());
				}
				//上次计提日期与上次罚息计提日对比 取 较晚一个
				else{
					loanagreement.getBasicExtension().setComeinDate(CalendarUtil.after(lnsprovision.getLastenddate().toString(),loanagreement.getBasicExtension().getComeinDate() )?lnsprovision.getLastenddate().toString():loanagreement.getBasicExtension().getComeinDate() );
				}
			}

		}

//		//非标自定义不规则总利息
//		if( ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(lnsbasicinfo.getRepayway() )){
//			param.put("key", "ZDY");
//			TblLnsbasicinfoex lnsbasicinfoex;
//			try{
//				lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsbasicinfoex");
//			}
//			if (null != lnsbasicinfoex) {
//				loanagreement.getBasicExtension().setTotalInt(new FabAmount(lnsbasicinfoex.getValue2()));
//			}
//		}

//		//现金免息2412611-现金贷
//		if( loanagreement.getContract().getFlag1().contains("F")){
//			param.put("key", "MX");
//			TblLnsbasicinfoex lnsbasicinfoex;
//			try{
//				lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsbasicinfoex");
//			}
//			if (null != lnsbasicinfoex) {
//				loanagreement.getBasicExtension().setFreeInterest(new FabAmount(lnsbasicinfoex.getValue2()));
//			}
//			//取主文件动态表剩余免息金额
//			TblLnsbasicinfodyn lnsbasicinfoDyn;
//			try{
//				lnsbasicinfoDyn = DbAccessUtil.queryForObject("Lnsbasicinfodyn.selectByUk", param, TblLnsbasicinfodyn.class);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsbasicinfoDyn");
//			}
//			if (null != lnsbasicinfoDyn) {
//				String json= lnsbasicinfoDyn.getTunneldata();
//				FaloanJson text = FaloanJson.parseObject(json);
//				loanagreement.getBasicExtension().setFreeInterest(new FabAmount(text.getDouble("MX")));
//			}
//		}



		//2019-10-12 现金贷提前还款违约金  --  移到后面了
		if( Arrays.asList("2412611","2412624","2512623","2512617","2512612").contains(lnsbasicinfo.getPrdcode()) ){
//			param.put("key", "TQHK");
//			TblLnsbasicinfoex lnsbasicinfoex;
//			try{
//				lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsbasicinfoex");
//			}
//			if (null != lnsbasicinfoex) {
//				loanagreement.getBasicExtension().setFeeRate(new FabRate(lnsbasicinfoex.getValue3()));
//			}
			//        if( Arrays.asList("2412611","2412624","2512612","2512617","2512623")){
            //临时校验
            //查询第一期还款计划辅助表  2020年1月老数据结息完成 可以删除
//            if(CalendarUtil.before(lnsbasicinfo.getBeginintdate(),"2019-12-17" )){
//                //查询还款计划辅助表
//                //定义查询map
//                Map<String,Object> billparam = new HashMap<String,Object>();
//                //按账号查询
//                billparam.put("acctno", acctNo);
//                billparam.put("brc", ctx.getBrc());
//				param.put("repayterm", 1);
//                TblLnsrpyplan rpyplan = null;
//
//                try {
//                    //取还款计划登记簿数据
//                    rpyplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrpyplan", billparam, TblLnsrpyplan.class);
//                }
//                catch (FabSqlException e)
//                {
//                    throw new FabException(e, "SPS103", "lnsrpyplan");
//                }
//				if(rpyplan!=null){
//                	if(CalendarUtil.monthsBetween(rpyplan.getRepayintbdate(), rpyplan.getRepayintedate())==1){
//                		loanagreement.getWithdrawAgreement().setFirstTermMonth(true);
//					}
//				}
//
//            }
		}

		//2020-03-26光大人保助贷 ，结清日期
		if( ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat()) ){
			loanagreement.getContract().setSettleDate(lnsbasicinfo.getModifydate());
		}

		List<TblLnsbasicinfoex> lnsbasicinfoexs ;




		//费用改造
		//查询所有的费用信息
		long start  = System.currentTimeMillis();
		param.put("acctno",acctNo );
		param.put("openbrc", ctx.getBrc());
		List<TblLnsfeeinfo> lnsfeeinfos;
		try{
			lnsfeeinfos = DbAccessUtil.queryForList("Lnsfeeinfo.select", param, TblLnsfeeinfo.class);
		}catch (FabSqlException e){
			throw  new FabException(e,"SPS103","Lnsfeeinfo");
		}
		loanagreement.getFeeAgreement().setLnsfeeinfos(lnsfeeinfos);
		LoggerUtil.info("query Lnsfeeinfo："+( System.currentTimeMillis()-start)+"ms");


		//动态封顶计息
		//结清了就不算封顶值了
		if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat()))
			DynamicCapUtil.CapInterestCalculation(loanagreement,ctx ,ctx.getTranDate());



		//以后拓展表新增的key，都在下面处理
		param.put("key", "");
		lnsbasicinfoexs = DbAccessUtil.queryForList("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
		if( null != lnsbasicinfoexs && lnsbasicinfoexs.size() > 0 )
		{
			for( TblLnsbasicinfoex lnsbasicinfoex : lnsbasicinfoexs )
			{
				//提前还款违约金
				if( ConstantDeclare.BASICINFOEXKEY.TQHK.equals(lnsbasicinfoex.getKey()))
					loanagreement.getBasicExtension().setFeeRate(new FabRate(lnsbasicinfoex.getValue3()));

				//延期+展期
				if( ConstantDeclare.BASICINFOEXKEY.ZQ.equals(lnsbasicinfoex.getKey()) )
				{
					if( !VarChecker.isEmpty(lnsbasicinfoex.getTunneldata()))
					{
						JSONObject tunneldata = JSONObject.parseObject(lnsbasicinfoex.getTunneldata());
						loanagreement.getBasicExtension().setTermEndDate(tunneldata.getString("termEndDate"));
			            loanagreement.getBasicExtension().setLastEnddates(tunneldata.getJSONObject("lastEndDate"));
					}
//					//展期判断做相关字段生成
					if(!VarChecker.isEmpty(lnsbasicinfo.getFlag1()) && lnsbasicinfo.getFlag1().contains("A")){
						loanagreement.getBasicExtension().setBalBeforeEx(new FabAmount(lnsbasicinfoex.getValue2()));
						loanagreement.getBasicExtension().setExtnums(lnsbasicinfo.getExtnums());
						loanagreement.getBasicExtension().setLastPeriodStr(lnsbasicinfoex.getValue1());
					}
				}

				//2019-09-26 气球贷取膨胀期数
				if( ConstantDeclare.BASICINFOEXKEY.PZQS.equals(lnsbasicinfoex.getKey()) )
					loanagreement.getBasicExtension().setExpandPeriod(Integer.valueOf(lnsbasicinfoex.getValue1()));

				//现金贷免息 2412611
				if( ConstantDeclare.BASICINFOEXKEY.MX.equals(lnsbasicinfoex.getKey()) )
				{
					loanagreement.getBasicExtension().setFreeInterest(new FabAmount(lnsbasicinfoex.getValue2()));
					//取主文件动态表剩余免息金额
					TblLnsbasicinfodyn lnsbasicinfoDyn;
					try{
						lnsbasicinfoDyn = DbAccessUtil.queryForObject("Lnsbasicinfodyn.selectByUk", param, TblLnsbasicinfodyn.class);
					}catch (FabSqlException e){
						throw new FabException(e, "SPS103", "lnsbasicinfoDyn");
					}
					if (null != lnsbasicinfoDyn) {
						String json= lnsbasicinfoDyn.getTunneldata();
						FaloanJson text = FaloanJson.parseObject(json);
						loanagreement.getBasicExtension().setFreeInterest(new FabAmount(text.getDouble("MX")));
					}
				}

				//添加免融担费隧道字段
				if(ConstantDeclare.PARACONFIG.EXTEND.equals(lnsbasicinfoex.getKey())){
					String tunnelData=lnsbasicinfoex.getTunneldata();
					Map<String,Object> tunnelMap=JSONObject.parseObject(tunnelData,Map.class);
					Map<String ,FabAmount> transferMap=new HashMap();
					for(Map.Entry<String,Object> temp:tunnelMap.entrySet()){
						//将隧道中的金额字段添加到减免金额的map中
						if(temp.getValue() instanceof  BigDecimal){
							transferMap.put(temp.getKey(),new FabAmount(((BigDecimal) temp.getValue()).doubleValue()));
						}
					}
					loanagreement.getBasicExtension().setFreeFee(transferMap);
				}
				//非标自定义不规则总利息
				if( ConstantDeclare.BASICINFOEXKEY.ZDY.equals(lnsbasicinfoex.getKey()) )
					loanagreement.getBasicExtension().setTotalInt(new FabAmount(lnsbasicinfoex.getValue2()));

				//汽车租赁款款方式
				if( ConstantDeclare.BASICINFOEXKEY.QCZL.equals(lnsbasicinfoex.getKey()) )
					loanagreement.getBasicExtension().setCarRepayWay(lnsbasicinfoex.getValue1());

				//无追保理债务公司信息
				if( ConstantDeclare.BASICINFOEXKEY.WZBL.equals(lnsbasicinfoex.getKey()) )
				{
					loanagreement.getBasicExtension().setJsonStr(lnsbasicinfoex.getTunneldata());
					loanagreement.getCustomer().setMerchantNo(lnsbasicinfo.getCustomid());
				}

				//买方付息债务公司信息
				if( ConstantDeclare.BASICINFOEXKEY.MFFX.equals(lnsbasicinfoex.getKey()) )
				{
					loanagreement.getBasicExtension().setDebtCompany(lnsbasicinfoex.getValue1());
					loanagreement.getBasicExtension().setDebtAmt(new FabAmount(lnsbasicinfoex.getValue2()));
					loanagreement.getCustomer().setMerchantNo(lnsbasicinfo.getCustomid());
				}

				//退完货后的使用金额
				if(ConstantDeclare.BASICINFOEXKEY.THJE.equals(lnsbasicinfoex.getKey())){
					//退货接口使用 添加退货后的余额 和 退货时的未来期第一期
					loanagreement.getBasicExtension().setSalesReturnAmt(new FabAmount(lnsbasicinfoex.getValue2()));
					loanagreement.getBasicExtension().setInitFuturePeriodNum(Integer.parseInt(lnsbasicinfoex.getValue1()));
				}

				if(ConstantDeclare.BASICINFOEXKEY.HXRQ.equals(lnsbasicinfoex.getKey())){
					loanagreement.getBasicExtension().setWriteOffDate(lnsbasicinfoex.getValue1());
				}

				if(ConstantDeclare.BASICINFOEXKEY.FRD.equals(lnsbasicinfoex.getKey())){
					loanagreement.getBasicExtension().setFirstRepayDate(lnsbasicinfoex.getValue1());
				}

			}
		}

		//还款计划规则适配
		//flag1是Z的特殊处理  狮桥sql更新最小天数，最后一期合并的98笔数据
		if(!VarChecker.isEmpty(lnsbasicinfo.getFlag1()) && lnsbasicinfo.getFlag1().contains("Z")){

			loanagreement.getWithdrawAgreement().setFirstTermMonth(true);
			loanagreement.getWithdrawAgreement().setMiddleTermMonth(false);
			loanagreement.getWithdrawAgreement().setLastTermMerge(false);
		}
		//光大人保助贷，最后一期同月合并，不同月拆分
		if( "2512627".equals(lnsbasicinfo.getPrdcode()) ){
			if( Integer.valueOf(lnsbasicinfo.getContduedate().substring(lnsbasicinfo.getContduedate().length()-2, lnsbasicinfo.getContduedate().length()).toString())
				< Integer.valueOf(lnsbasicinfo.getIntperformula().substring(lnsbasicinfo.getIntperformula().length()-2, lnsbasicinfo.getIntperformula().length()).toString()) )
				loanagreement.getWithdrawAgreement().setLastTowTermMerge(false);
			else
				loanagreement.getWithdrawAgreement().setLastTowTermMerge(true);
		}

		return loanagreement;
	}


	//仅用于按揭贷款还款计划试算repayPlanTentativeCalculation
	public static LoanAgreement genLoanAgreementForCalculation(LoanAgreement loanagreement, TranCtx ctx)
			throws FabException {

		Integer periodNum = Integer.valueOf(1);
		String periodUnit;
		String repayDate;

		if (!VarChecker.isEmpty(ctx.getRequestDict("intPerUnit"))) {
			periodUnit = ctx.getRequestDict("intPerUnit").toString();
		} else {
			periodUnit = "M";
		}
		
		if(!VarChecker.isEmpty(ctx.getRequestDict("repayDate"))|| ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(ctx.getRequestDict("repayWay"))){
			repayDate = ctx.getRequestDict("repayDate")!=null?ctx.getRequestDict("repayDate").toString():null;
		}else{
			throw new FabException("LNS008");
		}

		if (!VarChecker.isEmpty(ctx.getRequestDict("repayWay"))) {
			RepayWaySupporter loanRepayWaySupporter = LoanSupporterUtil
					.getRepayWaySupporter(ctx.getRequestDict("repayWay").toString());
			loanagreement.getInterestAgreement().setIntFormula(loanRepayWaySupporter.getIntFormula());// 利息公式:1-等额本息公式 2-其他
			loanagreement.getWithdrawAgreement().setRepayAmtFormula(loanRepayWaySupporter.getRepayAmtFormula()); // 还款金额公式:1-等额本息 2-其他

			loanagreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(
					periodNum, periodUnit, repayDate, ctx.getRequestDict("openDate").toString(),
					loanagreement.getContract().getContractEndDate()));
			loanagreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(
					periodNum, periodUnit, repayDate, ctx.getRequestDict("openDate").toString(),
					loanagreement.getContract().getContractEndDate()));
		}

		if (!VarChecker.isEmpty(ctx.getRequestDict("normalRate"))) {
			loanagreement.getRateAgreement().setNormalRate(new FabRate(Double.valueOf(ctx.getRequestDict("normalRate").toString())));
		}

		loanagreement.getWithdrawAgreement().setRepayWay(ctx.getRequestDict("repayWay").toString()); // 还款方式
		loanagreement.getContract().setBalance(loanagreement.getContract().getContractAmt());	//设置剩余本金
		loanagreement.getContract().setCurrIntPeriod(1);
		loanagreement.getContract().setCurrPrinPeriod(1);
		//封顶计息
		if(!VarChecker.isEmpty(loanagreement.getInterestAgreement().getCapRate())){
			loanagreement.getBasicExtension().setCapAmt(DynamicCapUtil.calculateCappingVal(loanagreement));

//			loanagreement.getBasicExtension().setCapAmt(new FabAmount(BigDecimal.valueOf(loanagreement.getInterestAgreement().getCapRate())
//					.divide(new BigDecimal(366), 20 ,BigDecimal.ROUND_HALF_UP)
//					.multiply(BigDecimal.valueOf(loanagreement.getContract().getContractAmt().getVal()))
//					.multiply(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(loanagreement.getContract().getStartIntDate(), loanagreement.getContract().getContractEndDate()))).doubleValue()));
			loanagreement.getBasicExtension().setComeinDate(ctx.getTranDate());

			loanagreement.getBasicExtension().setHisDintComein(new FabAmount(0.0) );
			loanagreement.getBasicExtension().setComeinDate(ctx.getTranDate());
		}
		//暂时处理，平台层升级后去掉
		loanagreement.getRateAgreement().getNormalRate().setRate(loanagreement.getRateAgreement().getNormalRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));
		if(!VarChecker.isEmpty(loanagreement.getRateAgreement().getOverdueRate()))
			loanagreement.getRateAgreement().getOverdueRate().setRate(loanagreement.getRateAgreement().getOverdueRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));
		if(!VarChecker.isEmpty(loanagreement.getRateAgreement().getCompoundRate()))
			loanagreement.getRateAgreement().getCompoundRate().setRate(loanagreement.getRateAgreement().getCompoundRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));
		//loanagreement.getFeeAgreement().getFeeRate().setRate(loanagreement.getFeeAgreement().getFeeRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));



		//光大人保助贷，最后一期同月合并，不同月拆分  2020-04-03
		if( null != ctx.getRequestDict("productCode") &&
			"2512627".equals( ctx.getRequestDict("productCode").toString()) ){
			if( Integer.valueOf(loanagreement.getContract().getContractEndDate().substring(loanagreement.getContract().getContractEndDate().length()-2, loanagreement.getContract().getContractEndDate().length()).toString())
				< Integer.valueOf(loanagreement.getInterestAgreement().getPeriodFormula().substring(loanagreement.getInterestAgreement().getPeriodFormula().length()-2, loanagreement.getInterestAgreement().getPeriodFormula().length()).toString()) )
				loanagreement.getWithdrawAgreement().setLastTowTermMerge(false);
			else
				loanagreement.getWithdrawAgreement().setLastTowTermMerge(true);

		}
		return loanagreement;
	}

	public static LoanAgreement genLoanAgreementFromRepayWay(LoanAgreement loanagreement, TranCtx ctx)
			throws FabException {

		Integer periodNum;
		String	periodUnit;
		String	repayDate;
		//现金贷免息
		String	endDate;
		if(!VarChecker.isEmpty(ctx.getRequestDict("periodNum"))){
			periodNum = Integer.valueOf(ctx.getRequestDict("periodNum").toString());
			}else{
			periodNum = Integer.valueOf(1);
		}
		if(!VarChecker.isEmpty(ctx.getRequestDict("intPerUnit"))){
			periodUnit = ctx.getRequestDict("intPerUnit").toString();
		}else{
			periodUnit = "M";
		}
		
		if(!VarChecker.isEmpty(ctx.getRequestDict("repayDate"))|| ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(ctx.getRequestDict("repayWay"))){
			repayDate = ctx.getRequestDict("repayDate")!=null?ctx.getRequestDict("repayDate").toString():null;
		}else{
			throw new FabException("LNS008");
		}

		//现金贷免息
		if(!VarChecker.isEmpty(ctx.getRequestDict("endDate"))){
			endDate = ctx.getRequestDict("endDate").toString();
		}else{
			endDate = loanagreement.getContract().getContractEndDate();
		}

		if(!VarChecker.isEmpty(ctx.getRequestDict("repayWay"))){
			RepayWaySupporter loanRepayWaySupporter =LoanSupporterUtil.getRepayWaySupporter(ctx.getRequestDict("repayWay").toString());
			loanagreement.getInterestAgreement().setIntFormula(loanRepayWaySupporter.getIntFormula());// 利息公式 :1-等额本息公式   2-其他
			loanagreement.getWithdrawAgreement().setRepayAmtFormula(loanRepayWaySupporter.getRepayAmtFormula()); // 还款金额公式:1-等额本息  2-其他
		/*	if(VarChecker.isEmpty(ctx.getRequestDict("startIntDate"))){
				loanagreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(periodNum, periodUnit, repayDate, ctx.getRequestDict("openDate").toString(), ctx.getRequestDict("endDate").toString()));
			}else{
				loanagreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(periodNum, periodUnit, repayDate, ctx.getRequestDict("startIntDate").toString(), ctx.getRequestDict("endDate").toString()));
			}
			loanagreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(periodNum, periodUnit, repayDate, ctx.getRequestDict("openDate").toString(), ctx.getRequestDict("endDate").toString()));
     */
			loanagreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(periodNum, periodUnit, repayDate, loanagreement.getContract().getStartIntDate(), endDate));
			loanagreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(periodNum, periodUnit, repayDate,loanagreement.getContract().getContractStartDate(), endDate));

		}
		// RateAgreement
		if(!VarChecker.isEmpty(ctx.getRequestDict("normalRate"))){
			loanagreement.getRateAgreement().setNormalRate(new FabRate(Double.valueOf(ctx.getRequestDict("normalRate").toString())));
		}
		if(!VarChecker.isEmpty(ctx.getRequestDict("overdueRate"))){
			loanagreement.getRateAgreement().setOverdueRate(new FabRate(Double.valueOf(ctx.getRequestDict("overdueRate").toString())));
		}
		if(!VarChecker.isEmpty(ctx.getRequestDict("compoundRate"))){
			loanagreement.getRateAgreement().setCompoundRate(new FabRate(Double.valueOf(ctx.getRequestDict("compoundRate").toString())));
		}

		if(("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no")))) {
			//暂时处理，平台层升级后去掉
			loanagreement.getRateAgreement().getNormalRate().setRate(loanagreement.getRateAgreement().getNormalRate().getRate().setScale(6, BigDecimal.ROUND_HALF_UP));
			loanagreement.getRateAgreement().getOverdueRate().setRate(loanagreement.getRateAgreement().getOverdueRate().getRate().setScale(6, BigDecimal.ROUND_HALF_UP));
			loanagreement.getRateAgreement().getCompoundRate().setRate(loanagreement.getRateAgreement().getCompoundRate().getRate().setScale(6, BigDecimal.ROUND_HALF_UP));
		}

		loanagreement.getWithdrawAgreement().setRepayWay(ctx.getRequestDict("repayWay").toString());     //还款方式
		//普通贷款不考虑最小天数
		if(VarChecker.asList("1").contains(ctx.getRequestDict("loanType"))){
			if(!loanagreement.getWithdrawAgreement().getFirstTermMonth()
					|| !loanagreement.getWithdrawAgreement().getMiddleTermMonth()
					|| !loanagreement.getWithdrawAgreement().getLastTermMerge()){
				if(!VarChecker.isEmpty(ctx.getRequestDict("prinMinDays")))
					loanagreement.getWithdrawAgreement().setPeriodMinDays(Integer.valueOf(ctx.getRequestDict("prinMinDays").toString()));
				if(!VarChecker.isEmpty(ctx.getRequestDict("intMinDays")))
					loanagreement.getInterestAgreement().setPeriodMinDays(Integer.valueOf(ctx.getRequestDict("intMinDays").toString()));
			}else{
				loanagreement.getWithdrawAgreement().setPeriodMinDays(Integer.valueOf(0));
				loanagreement.getInterestAgreement().setPeriodMinDays(Integer.valueOf(0));
			}
		} else{
			if(!VarChecker.isEmpty(ctx.getRequestDict("prinMinDays")))
				loanagreement.getWithdrawAgreement().setPeriodMinDays(Integer.valueOf(ctx.getRequestDict("prinMinDays").toString()));
			if(!VarChecker.isEmpty(ctx.getRequestDict("intMinDays")))
				loanagreement.getInterestAgreement().setPeriodMinDays(Integer.valueOf(ctx.getRequestDict("intMinDays").toString()));
		}

		if(!VarChecker.isEmpty(ctx.getRequestDict("isCalInt")))
			loanagreement.getInterestAgreement().setIsCalInt(ctx.getRequestDict("isCalInt").toString());
		/**从xml取
		loanagreement.getWithdrawAgreement().setRepayChannel("");// 还款渠道
		loanagreement.getWithdrawAgreement().setIsPrepay(false);// 是否提前还款
		loanagreement.getWithdrawAgreement().setPeriodMinDays(Integer.valueOf(15));//周期最小天数
		loanagreement.getWithdrawAgreement().setIsAgreePartRepay(""); // 是否同意部分还款
		loanagreement.getWithdrawAgreement().setGenCurrRepayPlanOpt("");  //生成还款计划时机
		loanagreement.getWithdrawAgreement().setIntIssueS("");	//扣息周期		定义到产品层
		loanagreement.getWithdrawAgreement().setIssueType("");//期限类型		定义到产品层
		loanagreement.getInterestAgreement().setIntBase("ACT"); //计息基础   ACT-实际天数   YMD-对年对月对日  从xml取
		loanagreement.getInterestAgreement().setPeriodMinDays(Integer.valueOf(15));//周期最小天数
		*/

		//增加本金当前期和利息当前期
		loanagreement.getContract().setCurrIntPeriod(1);
		loanagreement.getContract().setCurrPrinPeriod(1);




		return loanagreement;
	}


	private static List<AbstractUserDefineAgreement> getUserDefineAgreementList(LoanAgreement loanagreement,TranCtx ctx) throws FabException
	{
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", loanagreement.getContract().getReceiptNo());
		param.put("openbrc", ctx.getBrc());
		List<TblLnsnonstdplan> LnsnonstdplanList;

		try {
			LnsnonstdplanList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsnonstdplan_bill", param, TblLnsnonstdplan.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsnonstdplan");
		}
		if (null == LnsnonstdplanList) {
			throw new FabException("SPS104", "lnsnonstdplan");
		}


		List<AbstractUserDefineAgreement> list = new ArrayList<AbstractUserDefineAgreement>();


		for(TblLnsnonstdplan lnsnonstdplan : LnsnonstdplanList){
			UserDefineAgreement userdefine = new UserDefineAgreement();
			userdefine.setRepayTerm(lnsnonstdplan.getRepayterm());
			userdefine.setTermbDate(lnsnonstdplan.getIntbdate());
			userdefine.setTermeDate(lnsnonstdplan.getIntedate());
			userdefine.setTermPrin(new FabAmount(lnsnonstdplan.getTermprin()));
			userdefine.setTermInt(new FabAmount(lnsnonstdplan.getTermint()));
			userdefine.setDays(lnsnonstdplan.getDays());
			userdefine.setNormalRate(new FabRate(lnsnonstdplan.getNormalrate()));
			userdefine.setOverdueRate(new FabRate(lnsnonstdplan.getOverduerate()));
			userdefine.setCompoundRate(new FabRate(lnsnonstdplan.getOverduerate1()));
			list.add(userdefine);
		}

		return list;
	}


	public static List<LoanAgreement> genNonLoanAgreementFromDB(String acctNo, TranCtx ctx) throws FabException {
		if (VarChecker.isEmpty(acctNo))
			throw new FabException("ACC103");


		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo;
		List<TblLnsnonstdplan> LnsnonstdplanList;

		try{
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}

		try {
			LnsnonstdplanList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsnonstdplan_bill", param, TblLnsnonstdplan.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsnonstdplan");
		}
		if (null == LnsnonstdplanList) {
			throw new FabException("SPS104", "lnsnonstdplan");
		}


		List<LoanAgreement> loanagreementList = new ArrayList<LoanAgreement>();
		for( TblLnsnonstdplan nonBasicinfo : LnsnonstdplanList )
		{
			LoanAgreement nonLoanagreement = LoanAgreementProvider.genLoanAgreement(lnsbasicinfo.getPrdcode());
			nonLoanagreement.getContract().setContractAmt(new FabAmount(nonBasicinfo.getTermprin()));  //当期金额
			nonLoanagreement.getContract().setContractNo(lnsbasicinfo.getContrasctno());
			nonLoanagreement.getContract().setReceiptNo(lnsbasicinfo.getAcctno());
			nonLoanagreement.getContract().setCcy(new FabCurrency(lnsbasicinfo.getCcy()));
			nonLoanagreement.getContract().setContractStartDate(nonBasicinfo.getIntbdate());  //当期起息日
			nonLoanagreement.getContract().setContractEndDate(nonBasicinfo.getIntedate());    //当期到期日
			nonLoanagreement.getContract().setBalance(new FabAmount(lnsbasicinfo.getContractbal())); //当期余额

			if( CalendarUtil.before(nonBasicinfo.getTermbdate(),lnsbasicinfo.getLastprindate() ))
			{
				nonLoanagreement.getContract().setRepayPrinDate(lnsbasicinfo.getLastprindate());//上次结本日

			}else
			{
				nonLoanagreement.getContract().setRepayPrinDate(nonBasicinfo.getIntbdate());//上次结本日

			}

			if( CalendarUtil.before(nonBasicinfo.getIntbdate(),lnsbasicinfo.getLastintdate() ))
			{
				nonLoanagreement.getContract().setRepayIntDate(lnsbasicinfo.getLastintdate());//上次结息日
			}else
			{
				nonLoanagreement.getContract().setRepayIntDate(nonBasicinfo.getIntbdate());//上次结息日
			}



			nonLoanagreement.getContract().setStartIntDate(nonBasicinfo.getIntbdate());  //起息日期
			nonLoanagreement.getContract().setGraceDays(nonBasicinfo.getDays());//宽限期

			nonLoanagreement.getCustomer().setCustomName("");//客户名称
			nonLoanagreement.getFundInvest().setInvestee(lnsbasicinfo.getInvestee()); //接收投资者
			nonLoanagreement.getFundInvest().setInvestMode(lnsbasicinfo.getInvestmode()); //投放模式：保理、消费贷
			nonLoanagreement.getFundInvest().setChannelType(lnsbasicinfo.getChanneltype());//放款渠道      1-银行   2-易付宝  3-任性贷
			nonLoanagreement.getFundInvest().setFundChannel(lnsbasicinfo.getFundchannel());//资金通道    sap银行科目编号/易付宝总账科目
			nonLoanagreement.getFundInvest().setOutSerialNo(lnsbasicinfo.getOutserialno());//外部流水单号：银行资金流水号/易付宝交易单号

			nonLoanagreement.setPrdId(lnsbasicinfo.getPrdcode());

			// RateAgreement
			nonLoanagreement.getRateAgreement().setNormalRate(new FabRate(nonBasicinfo.getNormalrate()));
			nonLoanagreement.getRateAgreement().setOverdueRate(new FabRate(nonBasicinfo.getOverduerate()));
			nonLoanagreement.getRateAgreement().setCompoundRate(new FabRate(nonBasicinfo.getOverduerate1()));

			//暂时处理，平台层升级后去掉
			nonLoanagreement.getRateAgreement().getNormalRate().setRate(nonLoanagreement.getRateAgreement().getNormalRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));
			nonLoanagreement.getRateAgreement().getOverdueRate().setRate(nonLoanagreement.getRateAgreement().getOverdueRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));
			nonLoanagreement.getRateAgreement().getCompoundRate().setRate(nonLoanagreement.getRateAgreement().getCompoundRate().getRate().setScale(6,BigDecimal.ROUND_HALF_UP));


			nonLoanagreement.getInterestAgreement().setIntFormula(nonBasicinfo.getIntformula());// 利息公式 :1-等额本息公式   2-其他
			nonLoanagreement.getWithdrawAgreement().setRepayAmtFormula(nonBasicinfo.getPrinformula()); // 还款金额公式:1-等额本息  2-其他
			nonLoanagreement.getInterestAgreement().setPeriodFormula(nonBasicinfo.getIntperformula());
			nonLoanagreement.getInterestAgreement().setIsCalInt(nonBasicinfo.getIscalint());
			nonLoanagreement.getInterestAgreement().setPeriodMinDays(nonBasicinfo.getIntmindays());
			nonLoanagreement.getWithdrawAgreement().setPeriodFormula(nonBasicinfo.getPrinperformula());
			nonLoanagreement.getWithdrawAgreement().setPeriodMinDays(nonBasicinfo.getPrinmindays());
			nonLoanagreement.getWithdrawAgreement().setRepayWay(nonBasicinfo.getRepayway());     //还款方式

			//增加本金当前期和利息当前期
			nonLoanagreement.getContract().setCurrIntPeriod(lnsbasicinfo.getCurintterm());
			nonLoanagreement.getContract().setCurrPrinPeriod(lnsbasicinfo.getCurprinterm());

			loanagreementList.add(nonLoanagreement);
		}

		return loanagreementList;
	}

	public static LoanAgreement genNonLoanAgreementFromRepayWay(LoanAgreement loanagreement, TranCtx ctx, String repayDate,
			String intbdate, String intedate, String termbdate, String termedate)
			throws FabException {

		Integer periodNum;
		String	periodUnit;

		periodNum = Integer.valueOf(1);

		periodUnit = "D";

		if(!VarChecker.isEmpty(ctx.getRequestDict("repayWay"))){
			RepayWaySupporter loanRepayWaySupporter =LoanSupporterUtil.getRepayWaySupporter(ctx.getRequestDict("repayWay").toString());
			loanagreement.getInterestAgreement().setIntFormula("2");// 利息公式 :1-等额本息公式   2-其他 3-非标准
			loanagreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(periodNum, periodUnit, repayDate, intbdate, intedate));

			loanagreement.getWithdrawAgreement().setRepayAmtFormula("2"); // 还款金额公式:1-等额本息  2-其他 3-非标准
			loanagreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(periodNum, periodUnit, repayDate, termbdate, termedate));

			loanagreement.getWithdrawAgreement().setRepayWay(ctx.getRequestDict("repayWay").toString());     //还款方式
		}
//移到上面 20180424
//		loanagreement.getWithdrawAgreement().setRepayWay(ctx.getRequestDict("repayWay").toString());     //还款方式
		//普通贷款不考虑最小天数
		loanagreement.getWithdrawAgreement().setPeriodMinDays(Integer.valueOf(0));
		loanagreement.getInterestAgreement().setPeriodMinDays(Integer.valueOf(0));

		return loanagreement;
	}

	/*手机租赁*/
	public static LoanAgreement genLoanAgreementForRentCalculation(LoanAgreement loanagreement, TranCtx ctx)
			throws FabException {

		Integer periodNum = ctx.getRequestDict("periodNum");
		String periodUnit;
		String repayDate;

		if (!VarChecker.isEmpty(ctx.getRequestDict("intPerUnit"))) {
			periodUnit = ctx.getRequestDict("intPerUnit").toString();
		} else {
			periodUnit = "M";
		}
		
		if(!VarChecker.isEmpty(ctx.getRequestDict("repayDate"))|| ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(ctx.getRequestDict("repayWay"))){
			repayDate = ctx.getRequestDict("repayDate")!=null?ctx.getRequestDict("repayDate").toString():null;
		}else{
			throw new FabException("LNS008");
		}


		RepayWaySupporter loanRepayWaySupporter = LoanSupporterUtil
				.getRepayWaySupporter("0");
//			loanagreement.getInterestAgreement().setIntFormula(loanRepayWaySupporter.getIntFormula());// 利息公式:1-等额本息公式 2-其他
//			loanagreement.getWithdrawAgreement().setRepayAmtFormula(loanRepayWaySupporter.getRepayAmtFormula()); // 还款金额公式:1-等额本息 2-其他

//		loanagreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(
//				periodNum, periodUnit, repayDate, loanagreement.getContract().getContractStartDate(),
//				loanagreement.getContract().getContractEndDate()));
		loanagreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(
				periodNum, periodUnit, repayDate, loanagreement.getContract().getContractStartDate(),
				loanagreement.getContract().getContractEndDate()));


		if (!VarChecker.isEmpty(ctx.getRequestDict("normalRate"))) {
			loanagreement.getRateAgreement().setNormalRate(new FabRate(Double.valueOf(ctx.getRequestDict("normalRate").toString())));
		}
		loanagreement.getWithdrawAgreement().setRepayWay("0"); // 还款方式
		loanagreement.getContract().setBalance(loanagreement.getContract().getContractAmt());	//设置剩余本金
		loanagreement.getContract().setCurrIntPeriod(1);
		loanagreement.getContract().setCurrPrinPeriod(1);

		return loanagreement;
	}


	public static LoanAgreement genLoanAgreementFromP2P(LoanAgreement loanagreement, TranCtx ctx)
			throws FabException {

		Integer periodNum;
		String	periodUnit;
		String	repayDate;

		periodNum = Integer.valueOf(1);
		periodUnit = "M";
		
		if(!VarChecker.isEmpty(ctx.getRequestDict("repayDate"))|| ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(ctx.getRequestDict("repayWay"))){
			repayDate = ctx.getRequestDict("repayDate")!=null?ctx.getRequestDict("repayDate").toString():null;
		}else{
			throw new FabException("LNS008");
		}

		if(!VarChecker.isEmpty(ctx.getRequestDict("repayWay"))){
			RepayWaySupporter loanRepayWaySupporter =LoanSupporterUtil.getRepayWaySupporter(ctx.getRequestDict("repayWay").toString());
			loanagreement.getInterestAgreement().setIntFormula(loanRepayWaySupporter.getIntFormula());// 利息公式 :1-等额本息公式   2-其他
			loanagreement.getWithdrawAgreement().setRepayAmtFormula(loanRepayWaySupporter.getRepayAmtFormula()); // 还款金额公式:1-等额本息  2-其他
		/*	if(VarChecker.isEmpty(ctx.getRequestDict("startIntDate"))){
				loanagreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(periodNum, periodUnit, repayDate, ctx.getRequestDict("openDate").toString(), ctx.getRequestDict("endDate").toString()));
			}else{
				loanagreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(periodNum, periodUnit, repayDate, ctx.getRequestDict("startIntDate").toString(), ctx.getRequestDict("endDate").toString()));
			}
			loanagreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(periodNum, periodUnit, repayDate, ctx.getRequestDict("openDate").toString(), ctx.getRequestDict("endDate").toString()));
     */
			loanagreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(periodNum, periodUnit, repayDate, loanagreement.getContract().getStartIntDate(), ctx.getRequestDict("endDate").toString()));
			loanagreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(periodNum, periodUnit, repayDate,loanagreement.getContract().getContractStartDate(), ctx.getRequestDict("endDate").toString()));

		}
		// RateAgreement
		if(!VarChecker.isEmpty(ctx.getRequestDict("normalRate"))){
			loanagreement.getRateAgreement().setNormalRate(new FabRate(Double.valueOf(ctx.getRequestDict("normalRate").toString())));
		}
		if(!VarChecker.isEmpty(ctx.getRequestDict("overdueRate"))){
			loanagreement.getRateAgreement().setOverdueRate(new FabRate(Double.valueOf(ctx.getRequestDict("overdueRate").toString())));
		}
		if(!VarChecker.isEmpty(ctx.getRequestDict("compoundRate"))){
			loanagreement.getRateAgreement().setCompoundRate(new FabRate(Double.valueOf(ctx.getRequestDict("compoundRate").toString())));
		}

		loanagreement.getWithdrawAgreement().setRepayWay(ctx.getRequestDict("repayWay").toString());     //还款方式
		//普通贷款不考虑最小天数
		if(VarChecker.asList("8").contains(ctx.getRequestDict("repayWay"))){
			loanagreement.getWithdrawAgreement().setPeriodMinDays(Integer.valueOf(0));
			loanagreement.getInterestAgreement().setPeriodMinDays(Integer.valueOf(0));
		} else{
			if(!VarChecker.isEmpty(ctx.getRequestDict("prinMinDays")))
				loanagreement.getWithdrawAgreement().setPeriodMinDays(Integer.valueOf(ctx.getRequestDict("prinMinDays").toString()));
			if(!VarChecker.isEmpty(ctx.getRequestDict("intMinDays")))
				loanagreement.getInterestAgreement().setPeriodMinDays(Integer.valueOf(ctx.getRequestDict("intMinDays").toString()));
		}

//		if(!VarChecker.isEmpty(ctx.getRequestDict("isCalInt")))
//			loanagreement.getInterestAgreement().setIsCalInt(ctx.getRequestDict("isCalInt").toString());

		//增加本金当前期和利息当前期
		loanagreement.getContract().setCurrIntPeriod(1);
		loanagreement.getContract().setCurrPrinPeriod(1);



		loanagreement.getCustomer().setCustomName("");
		loanagreement.getCustomer().setMerchantNo(ctx.getRequestDict("merchantNo").toString());
		loanagreement.getCustomer().setCustomType(ctx.getRequestDict("customType").toString());
		loanagreement.getContract().setContractAmt(new FabAmount(Double.valueOf(ctx.getRequestDict("contractAmt").toString())));
		loanagreement.getContract().setContractNo(ctx.getRequestDict("contractNo").toString());
		loanagreement.getContract().setReceiptNo(ctx.getRequestDict("receiptNo").toString());
		loanagreement.getContract().setCcy(new FabCurrency("CNY"));
		loanagreement.getContract().setContractEndDate(ctx.getRequestDict("endDate").toString());
		loanagreement.getContract().setGraceDays(Integer.valueOf(ctx.getRequestDict("graceDays").toString()));
		loanagreement.getContract().setRepayDate(ctx.getRequestDict("repayDate").toString());
		loanagreement.getFundInvest().setChannelType(ctx.getRequestDict("channelType").toString());
		loanagreement.getFundInvest().setFundChannel(ctx.getRequestDict("fundChannel").toString());
		loanagreement.getFundInvest().setOutSerialNo(ctx.getRequestDict("outSerialNo").toString());

		return loanagreement;
	}

	@Resource(name="products")
	public void setProducts(Map<String, Product> products) {
		this.products = products;
	}
	public static Map<String, Product> getProducts() {
		return ServiceFactory.getBean(LoanAgreementProvider.class).products;
	}
}
