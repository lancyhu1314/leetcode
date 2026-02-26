package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsaccountdyninfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.scmconf.GlobalScmConf;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * @author LH
 *
 * @version V1.0.1
 * 保存扩展表
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns101 extends WorkUnit { 

	LoanAgreement loanAgreement;	//贷款协议 包含合同 客户 渠道等信息
	String		cashFlag;			//支付方式 1-现金 2-转账 			
	FabRate		compoundRate;		//复利利率
	String		compoundRateType;	//复利利率类型
	ListMap		debtCompany;		//账务公司列表
	FabAmount	discountAmt;		//扣息放款金额
	String		discountFlag;		//扣息放款标志
	FabAmount	feeAmt;				//放款手续费
	String		intPerUnit;			//扣息周期
	String		loanType;			//贷款类型 1-普通 2-按揭
	FabRate		normalRate;			//正常利率
	String		normalRateType;		//正常利率类型
	String		openBrc;			//开户机构
	String		openDate;			//开户日期
	FabRate		overdueRate;		//逾期利率
	String		overdueRateType;	//逾期利率类型
	Integer		periodNum;			//期限数量
	String		repayWay;			//还款方式
	String		serialNo;			//幂等流水号
	String		periodType;			//目前小贷接口使用该字段标志周期类型
	String		repayDate;			//目前小贷接口使用该字段标志每期还款日
	String		termDate;			//交易日期
	String		tranCode;			//交易代码
	String		merchantNo;			//商户号
	String		customName;			//客户名称
	String		customType;			//客户类型
	String		oldrepayacct;		//历史预收户
	String		fundChannel;		//渠道编码
	FabRate		penaltyFeeRate;		//管理费率

	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
			
		//P2P放款la注入
		if( "4010001".equals(loanAgreement.getPrdId()))
			loanAgreement = LoanAgreementProvider.genLoanAgreementFromP2P(loanAgreement, ctx);
		else
			loanAgreement = LoanAgreementProvider.genLoanAgreementFromRepayWay(loanAgreement, ctx);

		//等本等费校验
		if(ConstantDeclare.REPAYWAY.REPAYWAY_DEBXF.equals(loanAgreement.getWithdrawAgreement().getRepayWay())){
			ListMap pkgList = ctx.getRequestDict("pkgList3");
			if(VarChecker.isEmpty(pkgList)||pkgList.size()==0){
				throw new FabException("LNS240","等额本息费,费用必传");
			}else{
				if( pkgList.size() > 1 )
					throw new FabException("LNS240","等额本息费,不支持多费用");
				if(!"3".equals(PubDict.getRequestDict(pkgList.get(0), "calCulatrule").toString())){
					throw new FabException("LNS240","费用计算类型不支持");
				}

				if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().queryDynamicCapRate(ctx.getBrc()))){
					FabRate dynamicCapRate=new FabRate(loanAgreement.getInterestAgreement().queryDynamicCapRate(ctx.getBrc()));
					FabRate feeRate=new FabRate(PubDict.getRequestDict(pkgList.get(0), "feeRate").toString());//费率
					FabRate normalRate=new FabRate(ctx.getRequestDict("normalRate").toString());
					if(normalRate.add(feeRate).sub(dynamicCapRate).isPositive()){
						throw new FabException("LNS240","费率加利率超过封顶利率");
					}
				}


			}
		}
				
				
//		//预收登记簿
//		TblLnsprefundaccount lnsprefundaccount = new TblLnsprefundaccount();
//		lnsprefundaccount.setBrc(ctx.getBrc());
//		lnsprefundaccount.setCustomid(loanAgreement.getCustomer().getMerchantNo());
//		lnsprefundaccount.setAcctno(loanAgreement.getCustomer().getMerchantNo());
//		lnsprefundaccount.setName(loanAgreement.getCustomer().getCustomName());
//		lnsprefundaccount.setAccsrccode("N");
//		if( "2".equals(loanAgreement.getCustomer().getCustomType()) )
//			lnsprefundaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
//		else
//			lnsprefundaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);
//
//		lnsprefundaccount.setOpendate(ctx.getTranDate());
//		lnsprefundaccount.setClosedate("");
//		lnsprefundaccount.setBalance(0.00);
//		lnsprefundaccount.setStatus(ConstantDeclare.STATUS.NORMAL);
//		lnsprefundaccount.setReverse1("");
//		lnsprefundaccount.setReverse2("");
//
//		try{
//		DbAccessUtil.execute("Lnsprefundaccount.insert", lnsprefundaccount);
//		}
//		catch (FabSqlException e)
//		{
//			LoggerUtil.info("Lnsprefundaccount.insert.SQLCODE"+e.getSqlCode());
//
//			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
//				LoggerUtil.info("该商户已开户"+getMerchantNo());
//			}
//			else
//				throw new FabException(e, "SPS100", "lnsprefundaccount");
//		}
		// 登记预收
		LoanAssistDynInfoUtil.addPreaccountinfo(ctx,
			ctx.getBrc(),
			loanAgreement.getCustomer().getMerchantNo(),
			ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,
			loanAgreement.getCustomer().getMerchantNo(),
			"2".equals(loanAgreement.getCustomer().getCustomType())?ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY:ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);


//		Map<String, Object> param = new HashMap<String, Object>();
//		param.put("brc", ctx.getBrc());
//		param.put("accsrccode", "N");
//		param.put("customid", loanAgreement.getCustomer().getMerchantNo());
//
//		Map<String, Object> oldrepayacctno;
//
//		oldrepayacctno = DbAccessUtil.queryForMap("CUSTOMIZE.query_oldrepayacctno", param );
//		if (null == oldrepayacctno) {
//			LoggerUtil.info("lns102|query_oldrepayacctno:" + getMerchantNo());
//			throw new FabException("SPS103", "Lnsprefundaccount");
//		}
//		/**
//		 * C系统老数据预收登记簿中acctno为预收账号,customid为商户号
//		 * JAVA系统预收登记簿acctno和customid都是商户号
//		 * C系统迁移数据的acctno和customid不一样
//		 * 此处取oldrepayacct不知道什么用途
//		 * 20190527|14050183
//		 */
//		oldrepayacct  = oldrepayacctno.get("acctno").toString();


		TblLnsassistdyninfo preaccountInfo = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), "", loanAgreement.getCustomer().getMerchantNo(), ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,ctx);
		if (null == preaccountInfo) {
			throw new FabException("SPS104", "lnsassistdyninfo/lnsprefundaccount");
		}
		oldrepayacct = preaccountInfo.getAcctno();
			
		
		//幂等登记薄 幂等的话返回数据要从表里面取  现在是刚好借用了请求包数据
		//如果以后不直接用借据生成账号 幂等插表还是要放最后
		TblLnsinterface	lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setSerialno(getSerialNo());
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode(ctx.getTranCode());
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setAcctname("");
		lnsinterface.setUserno(oldrepayacct);
		lnsinterface.setAcctno(loanAgreement.getContract().getReceiptNo());
		lnsinterface.setTranamt(loanAgreement.getContract().getContractAmt().getVal());
		lnsinterface.setMagacct(loanAgreement.getContract().getReceiptNo());
		lnsinterface.setOrgid(loanAgreement.getFundInvest().getInvestee());
		lnsinterface.setBankno(loanAgreement.getFundInvest().getOutSerialNo());
		if(loanAgreement.getFundInvest().getChannelType() != null)
			lnsinterface.setReserv5(loanAgreement.getFundInvest().getChannelType());

		//科目号存入  2019-01-10 
		lnsinterface.setBillno(loanAgreement.getFundInvest().getFundChannel());
		
		//多渠道放款，放款渠道存入reserv6  2019-01-10
		ListMap pkgList2 = ctx.getRequestDict("pkgList2");
		/**
		 * channelType为0代表多渠道,pkgList2渠道详情
		 * 20190527|14050183
		 */
		if( "0".equals(loanAgreement.getFundInvest().getChannelType()) &&
			pkgList2 != null && pkgList2.size() != 0)
		{

			StringBuilder reserv6 = new StringBuilder();
			for(PubDict pkg:pkgList2.getLoopmsg()){
				if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "channelCode")) || VarChecker.isEmpty(PubDict.getRequestDict(pkg, "channelAmt"))){
					throw new FabException("LNS055","放款渠道");
				}
				
				reserv6.append(PubDict.getRequestDict(pkg, "channelCode").toString())
						.append( ":") .append( PubDict.getRequestDict(pkg, "channelAmt").toString()).append( "|");
			}
			
			reserv6.deleteCharAt(reserv6.length() - 1);
			lnsinterface.setReserv6(reserv6.toString());

		}
		
		//资金方信息存入reserv4  2019-02-15
		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		//2512617任性贷搭保险（平台开户）时必传

		if( null != pkgList1  && pkgList1.size() > 0)
		{
			StringBuilder reserv4 = new StringBuilder();

			//暂不支持多资金方
			if(pkgList1.size() > 1)
			{
				throw new FabException("LNS133");
			}
			
			for(PubDict pkg:pkgList1.getLoopmsg()){
				if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "investeeId"))){
					throw new FabException("LNS055","资金方");
				}
				
				reserv4.append(PubDict.getRequestDict(pkg, "investeeId").toString()).append( "|");
			}
			
			reserv4.deleteCharAt(reserv4.length() - 1);
			lnsinterface.setReserv4(reserv4.toString());
			//机构号为51230004 必传资金方
		}else if("51230004".equals(ctx.getBrc())){
			throw  new FabException("LNS167","开户机构号",ctx.getBrc());
		}
		
		
		
		try{
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
		} catch(FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				LoggerUtil.info("交易幂等ACCTNO["+loanAgreement.getContract().getReceiptNo()+"]");
				throw new FabException(e,TRAN.IDEMPOTENCY);
			}
			else
				throw new FabException(e, "SPS100", "lnsinterface");
		}

		//禁止一笔借据多机构开户
		LoanBasicInfoProvider.checkReceiptNo(loanAgreement.getContract().getReceiptNo());
/* 7*24版本不再做这种无聊的校验		
		if( !ctx.getRequestDict("openDate").toString().equals(ctx.getTranDate()) )
		{
			LoggerUtil.error("上送开户日期与核心账务日期不相等:"+"TRANDATE["+ctx.getTranDate()
			+"]"+"OPENDATE["+loanAgreement.getContract().getContractStartDate());
			throw new FabException("LNS024");
		}
*/		
		//为了7*24 下面的比较 改opendate为trandate 2017-11-29
		if( !"true".equals(loanAgreement.getInterestAgreement().getIsCompensatory()) && //代偿产品支持倒起息开户
			!Date.valueOf(loanAgreement.getContract().getContractEndDate()).after(Date.valueOf(ctx.getTranDate())))
		{
			LoggerUtil.error("合同结束日期必须大于起始日期:"+"startdate["+ctx.getTranDate()
			+"]"+"enddate["+loanAgreement.getContract().getContractEndDate());
			throw new FabException("LNS036");
		}
		
		//为了7*24 下面的比较 改opendate为trandate 2017-11-29
		if(!VarChecker.isEmpty(loanAgreement.getContract().getStartIntDate()) && Date.valueOf(loanAgreement.getContract().getStartIntDate()).after(Date.valueOf(ctx.getTranDate())))
		{
			LoggerUtil.error("起息日期不能大于合同开户日期:"+"STARTINTDATE["+loanAgreement.getContract().getStartIntDate()
			+"]"+"OPENDATE["+ctx.getTranDate()+"]");
			throw new FabException("LNS025");
		}
		
		//写静态表
		TblLnsbasicinfo lnsbasicinfo = new TblLnsbasicinfo();
		lnsbasicinfo.setOpenbrc(getOpenBrc());	//开户机构代码
		lnsbasicinfo.setAcctno(loanAgreement.getContract().getReceiptNo());	//账号
		lnsbasicinfo.setAcctype(ConstantDeclare.ACCTYPE.DEFAULT);	//帐别
		lnsbasicinfo.setCcy(new FabCurrency().getCcy());
		if( "2".equals(loanAgreement.getCustomer().getCustomType()) )
			lnsbasicinfo.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
		else
			lnsbasicinfo.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);
		
		lnsbasicinfo.setCustomid(loanAgreement.getCustomer().getMerchantNo());
		lnsbasicinfo.setName("");	//户名
		lnsbasicinfo.setPrdcode(loanAgreement.getPrdId());	//产品代码
		ListMap pkgList = ctx.getRequestDict("pkgList");
		//2512617任性贷搭保险（平台开户）时必传

		if( null != pkgList  && pkgList.size() > 0 && "473004".equals(ctx.getTranCode()))
		//无追保理-债务公司事件优化新开户数据	20190529|14050183
			lnsbasicinfo.setFlag1("E");
		else
			lnsbasicinfo.setFlag1("");	//静态标志1 目前第一位标志扣息放款
		if(ctx.getRequestDict("periodMinDays")!=null){
            if(Arrays.asList(GlobalScmConfUtil.getProperty("periodMinDays","").split(",")).contains(ctx.getRequestDict("periodMinDays").toString())&&Arrays.asList(GlobalScmConfUtil.getProperty("periodMinDays_PrdCode","").split(",")).contains(loanAgreement.getPrdId())){
            	//前端送最小天数
				lnsbasicinfo.setPrinmindays(ctx.getRequestDict("periodMinDays"));
				lnsbasicinfo.setIntmindays(ctx.getRequestDict("periodMinDays"));
			}else{
				throw new FabException("LNS237");
			}

		}else{
			lnsbasicinfo.setPrinmindays(loanAgreement.getWithdrawAgreement().getPeriodMinDays());
			lnsbasicinfo.setIntmindays(loanAgreement.getInterestAgreement().getPeriodMinDays());
		}

		if(getDiscountFlag() == null)
		{
			throw new FabException("LNS101");
		}	
		if("1".equals(discountFlag))
			lnsbasicinfo.setIscalint("YES");
		else //2-扣息 3-提前收息 这种模式以后必改
			lnsbasicinfo.setIscalint("NO");
		lnsbasicinfo.setRepayway(getRepayWay());	//担保方式    被用为还款方式
		lnsbasicinfo.setLoantype1(getLoanType());	//贷款种类1   2-按揭 、1-普通
		lnsbasicinfo.setLoanstat(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);	//贷款状态
		lnsbasicinfo.setProfitbrc(ctx.getBrc());	//核算机构代码
		lnsbasicinfo.setAcctno1(loanAgreement.getContract().getReceiptNo());	//借据号
		//lnsbasicinfo.setFirstprindate(billInfoAll.getFirstRepayPrinDate());  //首次还本日期  char(10)
		//lnsbasicinfo.setFirstintdate(billInfoAll.getFirstRepayIntDate());   //首次付息日期  char(10)
		lnsbasicinfo.setFirstintflag("1");   //首次结息日标志    char(1)
		lnsbasicinfo.setPrinperformula(loanAgreement.getWithdrawAgreement().getPeriodFormula());
		lnsbasicinfo.setPrinformula(loanAgreement.getWithdrawAgreement().getRepayAmtFormula());  

		//lnsbasicinfo.setLastprindate("");   //下次结本日    char(10)
		//lnsbasicinfo.setLastintdate("");    //下次结息日    char(10)
		if(VarChecker.isEmpty(loanAgreement.getContract().getStartIntDate()))
		{	
			lnsbasicinfo.setLastprindate(loanAgreement.getContract().getContractStartDate()); 
			lnsbasicinfo.setLastintdate(loanAgreement.getContract().getContractStartDate()); 
		}
		else
		{	
			lnsbasicinfo.setLastprindate(loanAgreement.getContract().getStartIntDate()); 
			lnsbasicinfo.setLastintdate(loanAgreement.getContract().getStartIntDate());	//起息日期
		}
		
		lnsbasicinfo.setIntperformula(loanAgreement.getInterestAgreement().getPeriodFormula());
		lnsbasicinfo.setIntformula(loanAgreement.getInterestAgreement().getIntFormula());

		//lnsbasicinfo.setPrinterms(billInfoAll.getPrinTotalPeriod());   //还本总期数    integer
		//lnsbasicinfo.setIntterms(billInfoAll.getIntTotalPeriod());    //还息总期数    integer
		lnsbasicinfo.setCurprinterm(1); //还本当期期数  integer
		lnsbasicinfo.setCurintterm(1);  //还息当期期数  integer
		lnsbasicinfo.setOldprinterm(0); //本金已还期数  integer
		lnsbasicinfo.setOldintterm(0);  //利息已还期数  integer
		lnsbasicinfo.setContractbal(loanAgreement.getContract().getContractAmt().getVal());  //合同余额  decimal

		/*if(VarChecker.isEmpty(loanAgreement.getContract().getStartIntDate()))
			lnsbasicinfo.setBeginintdate(loanAgreement.getContract().getContractStartDate());	//起息日期
		else
			lnsbasicinfo.setBeginintdate(loanAgreement.getContract().getStartIntDate());	//起息日期
			
		lnsbasicinfo.setOpendate(loanAgreement.getContract().getContractStartDate());*/
		lnsbasicinfo.setBeginintdate(loanAgreement.getContract().getStartIntDate());
		//为了7*24 下面的比较 改opendate为trandate 2017-11-29
		lnsbasicinfo.setOpendate(ctx.getTranDate());
		
		lnsbasicinfo.setGracedays(loanAgreement.getContract().getGraceDays());	//贷款宽限期
		lnsbasicinfo.setContduedate(loanAgreement.getContract().getContractEndDate());	//合同到期日
		lnsbasicinfo.setExtnums(0);	//展期次数
		lnsbasicinfo.setIntrateplan("");	//利率计划
		lnsbasicinfo.setBjtxbl(0.00);	//本金贴息比例
		lnsbasicinfo.setTxacctno("");	//贴息帐号
		lnsbasicinfo.setDiacct(oldrepayacct);	//扣款帐号
		if(loanAgreement.getFundInvest().getInvestee() != null)
			lnsbasicinfo.setInvestee(loanAgreement.getFundInvest().getInvestee());   //接收投资者 门店商户号
		if(loanAgreement.getFundInvest().getInvestMode() != null)
			lnsbasicinfo.setInvestmode(loanAgreement.getFundInvest().getInvestMode()); //投放模式：保理、消费贷
		lnsbasicinfo.setChanneltype(loanAgreement.getFundInvest().getChannelType());    //资金来源 放款渠道 1-银行 2-易付宝char
		lnsbasicinfo.setFundchannel(loanAgreement.getFundInvest().getFundChannel());    //资金来源帐号 资金通道 sap银行科目编号/易付宝总
		lnsbasicinfo.setOutserialno(loanAgreement.getFundInvest().getOutSerialNo());    //外部流水单号：银行资金流水号/易付宝交易单号
		//2019-09-26 合同号超过20位存拓展表
		if( !VarChecker.isEmpty(loanAgreement.getContract().getContractNo()) )
		{
			if( loanAgreement.getContract().getContractNo().length() <= 20 )
				lnsbasicinfo.setContrasctno(loanAgreement.getContract().getContractNo());	//合同编号
			else
			{
				//修改主文件拓展表
				Map<String,Object> exParam = new HashMap<>();
				exParam.put("acctno", loanAgreement.getContract().getReceiptNo());
				exParam.put("brc", ctx.getBrc());
				exParam.put("key", "HTBM");
				exParam.put("value1", loanAgreement.getContract().getContractNo());
				exParam.put("value2", 0.00);
				exParam.put("value3", 0.00);
				exParam.put("tunneldata", "");
				
				try {
					DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS102", "lnsbasicinfoex");
				}
			}
		}
		
		lnsbasicinfo.setContractamt(loanAgreement.getContract().getContractAmt().getVal());	//合同金额
		if(!VarChecker.isEmpty(getFeeAmt()))
			lnsbasicinfo.setOpenfee(getFeeAmt().getVal());
		if(!VarChecker.isEmpty(loanAgreement.getContract().getDiscountAmt()))
			lnsbasicinfo.setDeductionamt(loanAgreement.getContract().getDiscountAmt().getVal());
		lnsbasicinfo.setProvisionflag("RUNNING");
		lnsbasicinfo.setMemo("");	//备注
		lnsbasicinfo.setNormalrate(loanAgreement.getRateAgreement().getNormalRate().getYearRate().doubleValue());	//正常利率
		lnsbasicinfo.setOverduerate(loanAgreement.getRateAgreement().getOverdueRate().getYearRate().doubleValue());	//逾期利率
		lnsbasicinfo.setOverduerate1(loanAgreement.getRateAgreement().getCompoundRate().getYearRate().doubleValue());	//复利利率
		if(!VarChecker.isEmpty(getPenaltyFeeRate()))
			lnsbasicinfo.setBjtxbl(getPenaltyFeeRate().getVal().doubleValue());
		lnsbasicinfo.setModifydate(ctx.getTranDate());	//交易日期
		lnsbasicinfo.setTrandate(ctx.getTranDate());
		lnsbasicinfo.setModifytime("");	//交易时间

		//无是否有到期日时 合同到期日取起息日期，合同开始取起息日期前一天
		if("NO".equals(loanAgreement.getInterestAgreement().getNeedDueDate()))
		{

			lnsbasicinfo.setBeginintdate(CalendarUtil.nDaysBefore(loanAgreement.getContract().getStartIntDate(),1 ).toString("yyyy-MM-dd"));
			lnsbasicinfo.setContduedate(loanAgreement.getContract().getStartIntDate());	//合同到期日
			lnsbasicinfo.setLastprindate(lnsbasicinfo.getBeginintdate());
			lnsbasicinfo.setLastintdate(lnsbasicinfo.getContduedate());	//起息日期
		}

		try {
			DbAccessUtil.execute("Lnsbasicinfo.insert",lnsbasicinfo );
		} catch(FabSqlException e) {
			LoggerUtil.error("Lnsbasicinfo.insert.ERROR[{}]",e.getSqlCode());
			throw new FabException(e, "SPS100", "lnsbasicinfo");
		}

		//首期还款日存放扩展表
		if(!VarChecker.isEmpty(ctx.getRequestDict("firstRepayDate"))){
			insertFirstRepayDate(ctx.getRequestDict("firstRepayDate"));
		}

		//写动态表
		TblLnsaccountdyninfo lnsaccountdyninfo =  new TblLnsaccountdyninfo();
		lnsaccountdyninfo.setAcctno(loanAgreement.getContract().getReceiptNo());//贷款账号
		lnsaccountdyninfo.setAcctstat(ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN+"."+ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);//帐户形态
		lnsaccountdyninfo.setStatus(ConstantDeclare.STATUS.NORMAL);//状态
		lnsaccountdyninfo.setInacctno("");//贷款内部账号
		
		lnsaccountdyninfo.setProfitbrc(ctx.getBrc());//核算机构代码
		lnsaccountdyninfo.setOpenacctbrc(getOpenBrc());//开户机构代码
		lnsaccountdyninfo.setAcctype(ConstantDeclare.ACCTYPE.DEFAULT);//帐别
		lnsaccountdyninfo.setSubctrlcode("");//科目控制字
		lnsaccountdyninfo.setCcy(new FabCurrency().getCcy());
		lnsaccountdyninfo.setFlag("");//静态控制标志
		lnsaccountdyninfo.setFlag1("");//动态控制标志
		lnsaccountdyninfo.setRepayorder("");//贷款还款顺序
		lnsaccountdyninfo.setRatetype(loanAgreement.getRateAgreement().getNormalRate().getRateUnit());//利率类型
		lnsaccountdyninfo.setBald(ConstantDeclare.BALCTRLDIR.DEBIT);//余额方向
		lnsaccountdyninfo.setIntrate(loanAgreement.getRateAgreement().getNormalRate().getYearRate().doubleValue());//执行利率
		lnsaccountdyninfo.setLastbal(0.00);//昨日余额
		lnsaccountdyninfo.setCurrbal(0.00);//当前余额
		lnsaccountdyninfo.setCtrlbal(0.00);//控制余额
		lnsaccountdyninfo.setAccum(0.00);//积数
		lnsaccountdyninfo.setBegindate("");//起息日期
		lnsaccountdyninfo.setPrechgratedate("");//上次利率调整日
		lnsaccountdyninfo.setPrecalcintdate("");//上次结息日
		lnsaccountdyninfo.setNextcalcdate("");//下次结息日
		lnsaccountdyninfo.setPretrandate(ctx.getTranDate());//上笔交易日
		lnsaccountdyninfo.setDac("");//校验位
		lnsaccountdyninfo.setBegincacudate("");//累计积数起日
		lnsaccountdyninfo.setEndcacudate("");//累计积数止日
		lnsaccountdyninfo.setPrecacudays(1);//应计积数天数
		lnsaccountdyninfo.setCacudays(1);//已计积数天数
		lnsaccountdyninfo.setYearstartbal(0.00);//年初余额
		lnsaccountdyninfo.setFlagres11("");//标志备用
		lnsaccountdyninfo.setDateres01("");//日期备用
		lnsaccountdyninfo.setDateres02("");//日期备用
		lnsaccountdyninfo.setAmountres1(0.00);//金额备用
		lnsaccountdyninfo.setAmountres2(0.00);//金额备用
		lnsaccountdyninfo.setAccumres(0.00);//积数备用
		lnsaccountdyninfo.setAcctnores01("");//账号备用
		lnsaccountdyninfo.setAcctnores02("");//账号备用
		lnsaccountdyninfo.setOrdnu(1);//明细帐序号
		lnsaccountdyninfo.setModifydate(ctx.getTranDate());//交易日期
		lnsaccountdyninfo.setPrdcode(loanAgreement.getPrdId());
		try{
			DbAccessUtil.execute("Lnsaccountdyninfo.insert", lnsaccountdyninfo);
		}catch (FabSqlException e){
			LoggerUtil.error("Lnsbasicinfo.insert.ERROR");
			throw new FabException(e, "SPS100", "lnsaccountdyninfo");
		}
		
		//开本金户抛会计系统事件准备
		FabAmount	amount = new FabAmount(0.00);
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		lnsAcctInfo.setMerchantNo(loanAgreement.getCustomer().getMerchantNo());
		lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
		lnsAcctInfo.setCustName("");
		lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());

		if(pkgList == null || pkgList.size() == 0) //无追保理开户本金户表外不抛会计系统
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANMEBRGT, amount, lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.BJKH, ctx);
	}

	/**
	 * 后期还款日存扩展表
	 * @param firstRepayDate
	 */
	private void insertFirstRepayDate(String firstRepayDate)throws FabException{
		PubDictUtils.checkFirstRepayDate(firstRepayDate,tranctx.getRequestDict("repayDate"),loanAgreement.getContract().getContractEndDate(),loanAgreement.getContract().getStartIntDate(),loanAgreement.getWithdrawAgreement().getRepayWay());

		//新增主文件拓展表
		Map<String, Object> exParam = new HashMap<String, Object>();
		exParam.put("acctno", loanAgreement.getContract().getReceiptNo());
		exParam.put("openbrc", getOpenBrc());
		exParam.put("brc", getOpenBrc());

		//现金贷主文件静态拓展表
		exParam.put("key", "FRD");
		exParam.put("value1", firstRepayDate);
		exParam.put("value2", null);
		exParam.put("value3", null);
		try {
			DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_118", exParam);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS100", "lnsbasicinfoex");
		}

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
	 * @return the cashFlag
	 */
	public String getCashFlag() {
		return cashFlag;
	}
	/**
	 * @param cashFlag the cashFlag to set
	 */
	public void setCashFlag(String cashFlag) {
		this.cashFlag = cashFlag;
	}
	/**
	 * @return the compoundRate
	 */
	public FabRate getCompoundRate() {
		return compoundRate;
	}
	/**
	 * @param compoundRate the compoundRate to set
	 */
	public void setCompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
	}
	/**
	 * @return the compoundRateType
	 */
	public String getCompoundRateType() {
		return compoundRateType;
	}
	/**
	 * @param compoundRateType the compoundRateType to set
	 */
	public void setCompoundRateType(String compoundRateType) {
		this.compoundRateType = compoundRateType;
	}
	/**
	 * @return the debtCompany
	 */
	public ListMap getDebtCompany() {
		return debtCompany;
	}
	/**
	 * @param debtCompany the debtCompany to set
	 */
	public void setDebtCompany(ListMap debtCompany) {
		this.debtCompany = debtCompany;
	}
	/**
	 * @return the discountAmt
	 */
	public FabAmount getDiscountAmt() {
		return discountAmt;
	}
	/**
	 * @param discountAmt the discountAmt to set
	 */
	public void setDiscountAmt(FabAmount discountAmt) {
		this.discountAmt = discountAmt;
	}
	/**
	 * @return the discountFlag
	 */
	public String getDiscountFlag() {
		return discountFlag;
	}
	/**
	 * @param discountFlag the discountFlag to set
	 */
	public void setDiscountFlag(String discountFlag) {
		this.discountFlag = discountFlag;
	}
	/**
	 * @return the feeAmt
	 */
	public FabAmount getFeeAmt() {
		return feeAmt;
	}
	/**
	 * @param feeAmt the feeAmt to set
	 */
	public void setFeeAmt(FabAmount feeAmt) {
		this.feeAmt = feeAmt;
	}
	/**
	 * @return the intPerUnit
	 */
	public String getIntPerUnit() {
		return intPerUnit;
	}
	/**
	 * @param intPerUnit the intPerUnit to set
	 */
	public void setIntPerUnit(String intPerUnit) {
		this.intPerUnit = intPerUnit;
	}
	/**
	 * @return the loanType
	 */
	public String getLoanType() {
		return loanType;
	}
	/**
	 * @param loanType the loanType to set
	 */
	public void setLoanType(String loanType) {
		this.loanType = loanType;
	}
	/**
	 * @return the normalRate
	 */
	public FabRate getNormalRate() {
		return normalRate;
	}
	/**
	 * @param normalRate the normalRate to set
	 */
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}
	/**
	 * @return the normalRateType
	 */
	public String getNormalRateType() {
		return normalRateType;
	}
	/**
	 * @param normalRateType the normalRateType to set
	 */
	public void setNormalRateType(String normalRateType) {
		this.normalRateType = normalRateType;
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
	 * @return the openDate
	 */
	public String getOpenDate() {
		return openDate;
	}
	/**
	 * @param openDate the openDate to set
	 */
	public void setOpenDate(String openDate) {
		this.openDate = openDate;
	}
	/**
	 * @return the overdueRate
	 */
	public FabRate getOverdueRate() {
		return overdueRate;
	}
	/**
	 * @param overdueRate the overdueRate to set
	 */
	public void setOverdueRate(FabRate overdueRate) {
		this.overdueRate = overdueRate;
	}
	/**
	 * @return the overdueRateType
	 */
	public String getOverdueRateType() {
		return overdueRateType;
	}
	/**
	 * @param overdueRateType the overdueRateType to set
	 */
	public void setOverdueRateType(String overdueRateType) {
		this.overdueRateType = overdueRateType;
	}
	/**
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
	 * @return the repayWay
	 */
	public String getRepayWay() {
		return repayWay;
	}
	/**
	 * @param repayWay the repayWay to set
	 */
	public void setRepayWay(String repayWay) {
		this.repayWay = repayWay;
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
	 * @return the periodType
	 */
	public String getPeriodType() {
		return periodType;
	}
	/**
	 * @param periodType the periodType to set
	 */
	public void setPeriodType(String periodType) {
		this.periodType = periodType;
	}
	/**
	 * @return the repayDate
	 */
	public String getRepayDate() {
		return repayDate;
	}
	/**
	 * @param repayDate the repayDate to set
	 */
	public void setRepayDate(String repayDate) {
		this.repayDate = repayDate;
	}
	/**
	 * @return the termDate
	 */
	public String getTermDate() {
		return termDate;
	}
	/**
	 * @param termDate the termDate to set
	 */
	public void setTermDate(String termDate) {
		this.termDate = termDate;
	}
	/**
	 * @return the tranCode
	 */
	public String getTranCode() {
		return tranCode;
	}
	/**
	 * @param tranCode the tranCode to set
	 */
	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
	}
	/**
	 * @return the add
	 */
	public AccountOperator getAdd() {
		return add;
	}
	/**
	 * @param add the add to set
	 */
	public void setAdd(AccountOperator add) {
		this.add = add;
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
	public String getMerchantNo() {
		return merchantNo;
	}
	public void setMerchantNo(String merchantNo) {
		this.merchantNo = merchantNo;
	}
	public String getCustomName() {
		return customName;
	}
	public void setCustomName(String customName) {
		this.customName = customName;
	}
	public String getCustomType() {
		return customType;
	}
	public void setCustomType(String customType) {
		this.customType = customType;
	}
	public String getOldrepayacct() {
		return oldrepayacct;
	}
	public void setOldrepayacct(String oldrepayacct) {
		this.oldrepayacct = oldrepayacct;
	}
	/**
	 * @return the penaltyFeeRate
	 */
	public FabRate getPenaltyFeeRate() {
		return penaltyFeeRate;
	}
	/**
	 * @param penaltyFeeRate the penaltyFeeRate to set
	 */
	public void setPenaltyFeeRate(FabRate penaltyFeeRate) {
		this.penaltyFeeRate = penaltyFeeRate;
	}


}
