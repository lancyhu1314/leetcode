package com.suning.fab.loan.workunit;


import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.supporter.RepayWaySupporter;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.*;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 *  数据迁移
 *
 * acctNo 贷款账号
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository

public class Lns601 extends WorkUnit {

	String		receiptNo;		//借据号
	String		merchantNo;		//商户号
	String		customName;		//户名
	String		productCode;	//产品代码
	String		openDate;		//开户日期
	String		startIntDate;	//起息日期
	Integer		graceDays;		//宽限期
	String		endDate;		//合同到期日
	String		fundChannel;	//发放科目
	String		contractNo;		//合同编号
	String		outSerialNo;	//付款单号/易付宝流水号
	String  	customType;		//客户类型
	String  	discountFlag;	//扣息标志
	String  	repayWay;		//还款方式
	FabAmount	contractAmt;	//合同金额
	FabAmount	contractBal;	//合同余额
	FabRate		normalRate;		//正常利率
	FabRate		overdueRate;	//罚息利率
	FabRate		compoundRate;	//复利利率
	FabAmount	discountAmt;	//扣息金额
	String		transferFlag;	//迁移标志 （1-普通 2-非标准）
	String		serialNo;		//业务流水号
	String		ccy;			//币种
	FabAmount	totalInt;		//已计提/摊销金额
	String		channelType;	//放款渠道
	String		intDays; 		//上次摊销日期
	String      entrustedPaybrc;//受托支付商户号
	ListMap		pkgList;		//还款计划list
	
	String		oldrepayacct;	//老预收账号

	@Autowired LoanEventOperateProvider eventProvider;
	
	@Override
	public void run() throws Exception {
		//2020-07-15 涉敏字段
		customName="";
		
		Integer num = 1;
		TranCtx ctx = getTranctx();
		RepayWaySupporter loanRepayWaySupporter = LoanSupporterUtil.getRepayWaySupporter("8");
		TblLnsbasicinfo lnsbasicinfo = new TblLnsbasicinfo();
		TblLnsrpyplan lnsrpyplan =  new TblLnsrpyplan();
		
		
		ListMap pkgList = ctx.getRequestDict("pkgList");
		List<Map> sortList =  pkgList.ToList();
		
		//2019-11-28 扣息放款扣息金额要大于0，非扣息放款扣息金额要等于0
		if( "2".equals(discountFlag) ){
			if( !discountAmt.isPositive() )
				throw new FabException("LNS197");
		}
		if( "1".equals(discountFlag) ){
			if( !discountAmt.isZero() )
				throw new FabException("LNS198");
		}
			
		
		
		
		//判断该笔借据主文件表是否已存在
		Map<String,Object> Param = new HashMap<String,Object>();
		Param.put("acctno", receiptNo);
		Param.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo basicinfo = null;
		try {
			basicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", Param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		
		if (null != basicinfo){
			//借据号已存在
			throw new FabException("LNS080", receiptNo);
		}
				
		//产品代码新增3010017 非标-无追受托支付，校验：新增放款渠道C受托支付，必输受托支付商户号entrustedPaybrc 20191112
		if("3010017".equals(productCode)&&"C".equals(channelType)&&VarChecker.isEmpty(entrustedPaybrc))
			throw  new FabException("LNS055","受托支付商户号");

		//随借随还只有一期
		if( pkgList.size() > 1 )
			throw new FabException("LNS088");
		
		//起息日期不能大于等于到期日期
		if( CalendarUtil.afterAlsoEqual(startIntDate, endDate) )
			throw new FabException("LNS082",startIntDate,endDate);
		
		//起息日期不可为空
		if( VarChecker.isEmpty(startIntDate))
			throw new FabException("LNS038");
		
		//开始日期不等于放款日、结束日期不等于到期日期
		if( !startIntDate.equals(sortList.get(0).get("repayintBdate").toString()) ||
			!endDate.equals(sortList.get(0).get("repayintEdate").toString()))
			throw new FabException("LNS092");
		
		
		//事件登记簿账户参数赋值
		FundInvest  fundInvest = new FundInvest();
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		//商户号
		lnsAcctInfo.setMerchantNo(merchantNo);
		//客户类型
		lnsAcctInfo.setCustType(customType);
		//户名
		lnsAcctInfo.setCustName(customName);
		//产品代码
		lnsAcctInfo.setPrdCode(productCode);

		
		//幂等登记簿赋值
		TblLnsinterface	lnsinterface = new TblLnsinterface();
		//交易日期
		lnsinterface.setTrandate(ctx.getTermDate());
		//交易流水号
		lnsinterface.setSerialno(getSerialNo());
		//账务日期
		lnsinterface.setAccdate(ctx.getTranDate());
		//账务流水号
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		//交易码
		lnsinterface.setTrancode("479000");
		//机构号
		lnsinterface.setBrc(ctx.getBrc());
		//户名
		lnsinterface.setAcctname(customName);
		//商户号
		lnsinterface.setUserno(merchantNo);
		//借据号
		lnsinterface.setAcctno(receiptNo);
		//合同余额
		lnsinterface.setTranamt(contractBal.getVal());
		//借据号
		lnsinterface.setMagacct(receiptNo);
		//外部单号
		lnsinterface.setBankno(outSerialNo);
		//放款渠道
		lnsinterface.setReserv5(channelType);
		//科目号
		if(!VarChecker.isEmpty(fundChannel))
			lnsinterface.setBillno(fundChannel);


		try{
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
		} catch(FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				throw new FabException(e,TRAN.IDEMPOTENCY);
			}
			else
				throw new FabException(e, "SPS100", "lnsinterface");
		}
		
		
		
		//债务公司开户
		TblLnsprefundaccount lnsprefundaccount = new TblLnsprefundaccount();
		//机构号
		lnsprefundaccount.setBrc(ctx.getBrc());
		//客户类型
		lnsprefundaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
		//开户日期
		lnsprefundaccount.setOpendate(openDate);
		lnsprefundaccount.setClosedate("");
		//账户状态
		lnsprefundaccount.setStatus(ConstantDeclare.STATUS.NORMAL);
		lnsprefundaccount.setReverse1("");
		lnsprefundaccount.setReverse2("");
		

		ListMap pkgList2 = ctx.getRequestDict("pkgList2");
		if( !VarChecker.isEmpty(pkgList2)  && pkgList2.size() != 0) 
		{
			List<Map> sortList2 =  pkgList2.ToList();
			FabAmount totalDebtAmt = new FabAmount(0.00);
			int i =0;
			//循环债务公司信息
			for (Map sortListTmp2:sortList2) {

				Map<String,Object> param = new HashMap<String,Object>();
				param.put("brc", ctx.getBrc());
				param.put("accsrccode", "D");
				param.put("acctno", sortListTmp2.get("debtCompany").toString());
				TblLnsprefundaccount acctInfo = null;
				try {
					acctInfo = DbAccessUtil.queryForObject("Lnsprefundaccount.selectByUk", param, TblLnsprefundaccount.class);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS103", "lnsprefundaccount");
				}


				totalDebtAmt.selfAdd(Double.valueOf(sortListTmp2.get("debtAmt").toString()));
				lnsprefundaccount.setCustomid(sortListTmp2.get("debtCompany").toString());
				lnsprefundaccount.setAcctno(sortListTmp2.get("debtCompany").toString());
				lnsprefundaccount.setAccsrccode("D");
				lnsprefundaccount.setName(sortListTmp2.get("debtCompany").toString());



				if (null == acctInfo){
					lnsprefundaccount.setBalance(Double.valueOf(sortListTmp2.get("debtAmt").toString()));
					//债务公司不存在，插入债务公司信息
					try{
						DbAccessUtil.execute("Lnsprefundaccount.insert", lnsprefundaccount);
					}
					catch (FabSqlException e)
					{
						if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
							LoggerUtil.info("该商户已开户"+getMerchantNo());
						}
						else
							throw new FabException(e, "SPS100", "lnsprefundaccount");
					}
				}
				else
				{
					//债务公司已存在，累加账务公司金额
					lnsprefundaccount.setBalance(new FabAmount(acctInfo.getBalance()).add(Double.valueOf(sortListTmp2.get("debtAmt").toString())).getVal()   );
					//更新债务公司信息
					 try{
                        DbAccessUtil.execute("Lnsprefundaccount.updateByUk", lnsprefundaccount);
                    }catch (FabSqlException e){
                        throw new FabException(e,"SPS102", "lnsprefundaccount");
                    }
				}

				
				
				
				//债务公司 贷款开户
				AccountingModeChange.saveLnsprefundsch(ctx, ++i, lnsprefundaccount.getAcctno(), lnsprefundaccount.getCustomid(), "D",lnsprefundaccount.getCusttype() ,
						lnsprefundaccount.getName() ,Double.valueOf(sortListTmp2.get("debtAmt").toString()) ,"add" );
				//放款本金(多债务公司传多次事件)
				eventProvider.createEvent(ConstantDeclare.EVENT.RECGDEBTCO, new FabAmount(Double.valueOf(sortListTmp2.get("debtAmt").toString())), lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.ZWCZ, ctx, getMerchantNo(), lnsprefundaccount.getCustomid(), ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
			}
			
			//债务公司校验（累加金额是否等于合同余额）
			if(!totalDebtAmt.sub(contractBal).isZero())
				throw new FabException("LNS090");
			
			
			
			//债务公司登记主文件拓展表 2019-030-06
			String		debtTotalCompany = "";
			FabAmount	debtTotalAmt = new FabAmount(0.00);
			Map<String,String> jsonStr = new HashMap<>();
			for (PubDict pkg:pkgList2.getLoopmsg()) {
				String debtCompany = PubDict.getRequestDict(pkg, "debtCompany");
				FabAmount debtAmt = PubDict.getRequestDict(pkg, "debtAmt");

				if( Arrays.asList("3010013","3010015").contains(productCode)  )
				{
					if( pkgList2.size() > 1 &&
						!VarChecker.isEmpty(debtTotalCompany) &&
						!debtCompany.equals(debtTotalCompany))
							throw new FabException("LNS122");
				}
				debtTotalCompany= debtCompany;
				debtTotalAmt.selfAdd(debtAmt);
				if(jsonStr.containsKey(debtCompany)){
					debtAmt.selfAdd(new FabAmount(Double.valueOf(jsonStr.get(debtCompany)))); 
				}
				jsonStr.put(debtCompany, debtAmt.toString());
			}
			
			
			//普通开户拓展表增加
			//2018-12-10 买方付息登记债务公司信息到主文件拓展表
			if( Arrays.asList("3010013","3010015").contains(productCode) )
			{
				
				//修改主文件拓展表
				Map<String,Object> exParam = new HashMap<>();
				exParam.put("acctno", receiptNo);
				exParam.put("brc", ctx.getBrc());
				exParam.put("key", "MFFX");
				exParam.put("value1", debtTotalCompany);
				exParam.put("value2", debtTotalAmt.getVal());
				exParam.put("value3", 0.00);
				exParam.put("tunneldata", "");
				
				try {
					DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS102", "lnsbasicinfoex");
				}
			}
			
			
			if( Arrays.asList("3010006","3010014").contains(productCode)  )
			{
				//修改主文件拓展表
				Map<String,Object> exParam = new HashMap<>();
				exParam.put("acctno", receiptNo);
				exParam.put("brc", ctx.getBrc());
				exParam.put("key", "WZBL");
				exParam.put("value1", "");
				exParam.put("value2", 0.00);
				exParam.put("value3", 0.00);
				exParam.put("tunneldata", JsonTransfer.ToJson(jsonStr));
				
				try {
					DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS102", "lnsbasicinfoex");
				}
			}
		}
		else	//2019-06-07
		{
			//无追保理和买方付息产品的债务公司必输
			if( Arrays.asList("3010006","3010013","3010014","3010015").contains(productCode))
			{
				throw new FabException("LNS055","债务公司");
			}
		}
		

		//主文件赋值
		//机构号
		lnsbasicinfo.setOpenbrc(ctx.getBrc());
		//账号
		lnsbasicinfo.setAcctno(receiptNo);
		//账户类型
		lnsbasicinfo.setAcctype("0");
		//币种
		lnsbasicinfo.setCcy("01");
		//客户类型
		if( "2".equals(customType) )
			lnsbasicinfo.setCusttype("COMPANY");
		else
			//账户类型只允许对公
			throw new FabException("LNS078");
		//商户号
		lnsbasicinfo.setCustomid(merchantNo);
		//客户名称
		lnsbasicinfo.setName(customName);
		//产品代码
		lnsbasicinfo.setPrdcode(productCode);
		//特殊标志
		if( !VarChecker.isEmpty(pkgList2)  && pkgList2.size() != 0)
			lnsbasicinfo.setFlag1("E");
		else
			lnsbasicinfo.setFlag1("");
		//无追保理-账速融迁移，flag1存D
		if( "1".equals(transferFlag) )
		{
			lnsbasicinfo.setFlag1("D");
		}
		
		lnsbasicinfo.setPrinmindays(0);
		lnsbasicinfo.setIntmindays(0);
		//迁移暂时都为未逾期的
		lnsbasicinfo.setLoanstat("N");
		lnsbasicinfo.setProfitbrc(ctx.getBrc());
		//借据号
		lnsbasicinfo.setAcctno1(receiptNo);
		lnsbasicinfo.setFirstprindate("");
		lnsbasicinfo.setFirstintdate("");
		lnsbasicinfo.setFirstintflag("1");
		lnsbasicinfo.setOldprinterm(0);
		lnsbasicinfo.setOldintterm(0);
		//开户日期空取起息日期
		if( VarChecker.isEmpty(openDate) )
			lnsbasicinfo.setOpendate(startIntDate);
		else
			lnsbasicinfo.setOpendate(openDate);
		//起息日期
		lnsbasicinfo.setBeginintdate(startIntDate);
		//宽限期
		lnsbasicinfo.setGracedays(Integer.valueOf(graceDays));
		//合同到期日
		lnsbasicinfo.setContduedate(endDate);
		lnsbasicinfo.setIntenddate("");
		lnsbasicinfo.setExtnums(0);
		lnsbasicinfo.setIntrateplan("");
		lnsbasicinfo.setBjtxbl(0.00);
		lnsbasicinfo.setTxacctno("");
		lnsbasicinfo.setDiacct(receiptNo);
		lnsbasicinfo.setInvestee("");
		lnsbasicinfo.setInvestmode("");
		//渠道类型
		lnsbasicinfo.setChanneltype(channelType);
		//发放科目
		lnsbasicinfo.setFundchannel(fundChannel);
		//外部流水号
		lnsbasicinfo.setOutserialno(outSerialNo);
		//合同编码
		//2019-09-26 合同号超过20位存拓展表
		if( !VarChecker.isEmpty(getContractNo()) )
		{
			if( contractNo.length() <= 20 )
				lnsbasicinfo.setContrasctno(contractNo);
			else
			{
				//修改主文件拓展表
				Map<String,Object> exParam = new HashMap<>();
				exParam.put("acctno", receiptNo);
				exParam.put("brc", ctx.getBrc());
				exParam.put("key", "HTBM");
				exParam.put("value1", contractNo);
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
		//合同金额
		lnsbasicinfo.setContractamt(contractAmt.getVal());
		lnsbasicinfo.setOpenfee(0.00);
		lnsbasicinfo.setProvisionflag("RUNNING");
		lnsbasicinfo.setMemo("");
		//正常利率
		lnsbasicinfo.setNormalrate(Double.valueOf(normalRate.toString()));
		//罚息利率
		lnsbasicinfo.setOverduerate(Double.valueOf(overdueRate.toString()));
		//复利利率
		lnsbasicinfo.setOverduerate1(Double.valueOf(compoundRate.toString()));
		lnsbasicinfo.setModifydate(ctx.getTranDate());
		lnsbasicinfo.setModifytime(ctx.getTranTime());
		lnsbasicinfo.setTrandate(ctx.getTranDate());
		lnsbasicinfo.setReserv1("");
		lnsbasicinfo.setReservamt1(0.00);
		//非扣息放款
		if( "1".equals(discountFlag) )
			lnsbasicinfo.setIscalint("YES");
		//扣息放款
		else
			lnsbasicinfo.setIscalint("NO");
		//还款方式
		if( "8".equals(repayWay) )
			lnsbasicinfo.setRepayway(repayWay);
		else
			//还款方式只允许为8
			throw new FabException("LNS089");
		
		lnsbasicinfo.setLoantype1("1");

		//拼周期公式
		lnsbasicinfo.setPrinperformula(loanRepayWaySupporter.getIntPeriodFormula(1, "", "", startIntDate, endDate));
		lnsbasicinfo.setPrinformula("2");
		lnsbasicinfo.setLastprindate(sortList.get(0).get("termretDate").toString());
		lnsbasicinfo.setIntperformula(loanRepayWaySupporter.getIntPeriodFormula(1, "", "", startIntDate, endDate));
		lnsbasicinfo.setIntformula("2");
//		lnsbasicinfo.setLastintdate(sortList.get(0).get("termretDate").toString());
		lnsbasicinfo.setLastintdate(sortList.get(0).get("termretDate").toString());
		
		lnsbasicinfo.setPrinterms(0);
		lnsbasicinfo.setIntterms(0);
		lnsbasicinfo.setCurprinterm(1);
		lnsbasicinfo.setCurintterm(1);
		
		//合同金额-合同余额不等于已还本金
		if( !contractAmt.sub(Double.valueOf(sortList.get(0).get("noretPrin").toString())).sub(Double.valueOf(sortList.get(0).get("prinAmt").toString())).isZero())
			throw new FabException("LNS086",Double.valueOf(sortList.get(0).get("prinAmt").toString()),Double.valueOf(sortList.get(0).get("noretPrin").toString()),contractAmt);
		//合同余额
		lnsbasicinfo.setContractbal(contractBal.getVal());
		//扣息金额
		lnsbasicinfo.setDeductionamt(discountAmt.getVal());
		
		try{
			DbAccessUtil.execute("Lnsbasicinfo.insert", lnsbasicinfo);
		}catch (FabSqlException e){
			//幂等处理
			if (e.getSqlCode().equals(ConstantDeclare.SQLCODE.DUPLICATIONKEY)) {
				throw new FabException(e,ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
			}
			//非幂等报错
			else
			{
				throw new FabException(e, "SPS100", "lnsbasicinfo");
			}
		}

		
		
//		//预收登记簿
//		lnsprefundaccount.setCustomid(merchantNo);
//		lnsprefundaccount.setAcctno(merchantNo);
//		lnsprefundaccount.setAccsrccode("N");
//		lnsprefundaccount.setBalance(0.00);
//		lnsprefundaccount.setName(customName);
//
//		try{
//		DbAccessUtil.execute("Lnsprefundaccount.insert", lnsprefundaccount);
//		}
//		catch (FabSqlException e)
//		{
//			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
//				LoggerUtil.info("该商户已开户"+getMerchantNo());
//			}
//			else
//				throw new FabException(e, "SPS100", "lnsprefundaccount");
//		}
//
//		Map<String, Object> prefundaccountparam = new HashMap<String, Object>();
//		prefundaccountparam.put("brc", ctx.getBrc());
//		prefundaccountparam.put("accsrccode", "N");
//		prefundaccountparam.put("customid",merchantNo);
//
//		Map<String, Object> oldrepayacctno;
//
//		oldrepayacctno = DbAccessUtil.queryForMap("CUSTOMIZE.query_oldrepayacctno", prefundaccountparam );
//		if (null == oldrepayacctno) {
//			LoggerUtil.info("lns601|query_oldrepayacctno:" + getMerchantNo());
//			throw new FabException("SPS103", "Lnsprefundaccount");
//		}
//		oldrepayacct  = oldrepayacctno.get("acctno").toString();

		//预收户开户:有可能本商户已经开过户了
		LoanAssistDynInfoUtil.addPreaccountinfo(ctx,
			ctx.getBrc(),
			getMerchantNo(),
			ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,
			getMerchantNo(),
			ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);

		//查询brc,feetype,custmid为条件的acctno
		TblLnsassistdyninfo preaccountInfo = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), "", getMerchantNo(), ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,ctx);
		if (null == preaccountInfo) {
			throw new FabException("SPS104", "lnsassistdyninfo/lnsprefundaccount");
		}
		oldrepayacct = preaccountInfo.getAcctno();


		//计提
//		if( discountAmt.sub(totalInt).isNegative() ||
//			new FabAmount(Double.valueOf(sortList.get(0).get("termcdInt").toString())).sub(totalInt).isNegative()	)
//			throw new FabException("LNS081");

//		TblLnsprovisionreg lnsprovisionreg = new TblLnsprovisionreg();
		TblLnsprovisiondtl lnsprovisiondtl = getLnsprovisiondtl(ctx);
//		lnsprovisiondtl.setTrandate(Date.valueOf(ctx.getTranDate()));
//		lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
//		lnsprovisiondtl.setTxnseq(1);
//		lnsprovisiondtl.setBrc(ctx.getBrc());
////		lnsprovisionreg.setTeller("479001");
//		lnsprovisiondtl.setAcctno(receiptNo);
////		lnsprovisionreg.setReceiptno(receiptNo);
//		lnsprovisiondtl.setPeriod(la.getContract().getCurrIntPeriod());
//		lnsprovisiondtl.setListno(1);
//		lnsprovisiondtl.setBilltype("NINT");
//		lnsprovisiondtl.setCcy("01");
//		lnsprovisiondtl.setTotalinterest(totalInt.getVal());
//		lnsprovisiondtl.setTotaltax(TaxUtil.calcVAT(totalInt).getVal());
//		lnsprovisiondtl.setTaxrate(0.06);
//		lnsprovisiondtl.setInterest(totalInt.getVal());
//		lnsprovisiondtl.setTax(TaxUtil.calcVAT(totalInt).getVal());
//		lnsprovisiondtl.setIntertype("PROVISION");
//		lnsprovisiondtl.setBegindate(Date.valueOf(startIntDate));
//		if( "1".equals(transferFlag))
//		{
//			lnsprovisiondtl.setEnddate(Date.valueOf(intDays));
//		}
//		else
//			lnsprovisiondtl.setEnddate(Date.valueOf(startIntDate));
//		lnsprovisiondtl.setInterflag("POSITIVE");
//		lnsprovisiondtl.setCancelflag("NORMAL");
//


		if( "2".equals(discountFlag) )
		{
			TblLnsamortizeplan lnsamortizeplan = new TblLnsamortizeplan();
			lnsamortizeplan.setTrandate(Date.valueOf(ctx.getTranDate()));
			lnsamortizeplan.setSerseqno(ctx.getSerSeqNo());
			lnsamortizeplan.setBrc(ctx.getBrc());
			lnsamortizeplan.setAcctno(receiptNo);
			lnsamortizeplan.setAmortizetype("1");
			lnsamortizeplan.setCcy("01");
			lnsamortizeplan.setTaxrate(0.06);
			lnsamortizeplan.setTotalamt(discountAmt.getVal());
			lnsamortizeplan.setAmortizeamt(totalInt.getVal());
			lnsamortizeplan.setTotaltaxamt(TaxUtil.calcVAT(discountAmt).getVal());
			lnsamortizeplan.setAmortizetax(TaxUtil.calcVAT(totalInt).getVal());
			if( "1".equals(transferFlag))
			{
				lnsamortizeplan.setLastdate(intDays);
			}
			else
				lnsamortizeplan.setLastdate(startIntDate);
			
			lnsamortizeplan.setBegindate(startIntDate);
			lnsamortizeplan.setEnddate(endDate);
			lnsamortizeplan.setPeriod(1);
			lnsamortizeplan.setStatus("RUNNING");
			
			
				
			try{
				DbAccessUtil.execute("Lnsamortizeplan.insert", lnsamortizeplan);
			}
			catch (FabSqlException e)
			{
				
				//幂等处理
				if (e.getSqlCode().equals(ConstantDeclare.SQLCODE.DUPLICATIONKEY)) {
					throw new FabException(e,ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
				}
				//非幂等报错
				else
				{
					throw new FabException(e, "SPS100", "lnsamortizeplan");
				}
			}

			lnsprovisiondtl.setIntertype("AMORTIZE");
//			lnsprovisionreg.setTeller("479010");
		}
		
		
		if( "1".equals(transferFlag))
		{
//			try{
//				DbAccessUtil.execute("Lnsprovisionreg.insert", lnsprovisionreg);
//			}
//			catch (FabSqlException e)
//			{
//				throw new FabException(e, "SPS100", "lnsprovisionreg");
//			}
			LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
			try {
				DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS100", "lnsprovisiondtl");
			}

		}
		
		
		
		
		
		
		//动态表
		TblLnsaccountdyninfo lnsaccountdyninfo = new TblLnsaccountdyninfo();
		lnsaccountdyninfo.setAcctno(receiptNo);		
		lnsaccountdyninfo.setAcctstat("PRIN.N");
		//PRIN.B1.4.9
//		if((lnsbasicinfo.getBeginintdate()).equals("2017-11-16")||(lnsbasicinfo.getBeginintdate()).equals("2016-06-22")){
//			lnsaccountdyninfo.setAcctstat("PRIN.B");
//		}
		lnsaccountdyninfo.setStatus("0");
		lnsaccountdyninfo.setPrdcode(productCode);
		lnsaccountdyninfo.setInacctno("");
		lnsaccountdyninfo.setProfitbrc(ctx.getBrc());
		lnsaccountdyninfo.setOpenacctbrc(ctx.getBrc());
		lnsaccountdyninfo.setAcctype("0");
		lnsaccountdyninfo.setSubctrlcode("");
		lnsaccountdyninfo.setCcy("01");
		lnsaccountdyninfo.setFlag("");
		lnsaccountdyninfo.setFlag1("");
		lnsaccountdyninfo.setRepayorder("");
		lnsaccountdyninfo.setRatetype("Y");
		lnsaccountdyninfo.setBald("D");
		lnsaccountdyninfo.setIntrate(normalRate.getVal().doubleValue());
		lnsaccountdyninfo.setLastbal(contractAmt.getVal());
		lnsaccountdyninfo.setCurrbal(contractBal.getVal());
		lnsaccountdyninfo.setCtrlbal(0.00);
		lnsaccountdyninfo.setAccum(0.00);
		lnsaccountdyninfo.setBegindate("");
		lnsaccountdyninfo.setPrechgratedate("");
		lnsaccountdyninfo.setPrecalcintdate("");
		lnsaccountdyninfo.setNextcalcdate("");
		lnsaccountdyninfo.setPretrandate(sortList.get(0).get("termretDate").toString());
		lnsaccountdyninfo.setDac("");
		lnsaccountdyninfo.setBegincacudate("");
		lnsaccountdyninfo.setEndcacudate("");
		lnsaccountdyninfo.setPrecacudays(1);
		lnsaccountdyninfo.setCacudays(1);
		lnsaccountdyninfo.setYearstartbal(0.00);
		lnsaccountdyninfo.setFlagres11("");
		lnsaccountdyninfo.setDateres01("");
		lnsaccountdyninfo.setDateres02("");
		lnsaccountdyninfo.setAmountres1(0.00);
		lnsaccountdyninfo.setAmountres2(0.00);
		lnsaccountdyninfo.setAccumres(0.00);
		lnsaccountdyninfo.setAcctnores01("");
		lnsaccountdyninfo.setAcctnores02("");
		lnsaccountdyninfo.setOrdnu(1);
		lnsaccountdyninfo.setModifydate(ctx.getTranDate());
		lnsaccountdyninfo.setModifytime(ctx.getTranDate());
		
		try{
			DbAccessUtil.execute("Lnsaccountdyninfo.insert", lnsaccountdyninfo);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS100", "lnsaccountdyninfo");
		}
		
		
		
		lnsaccountdyninfo.setIntrate(0.00);
		lnsaccountdyninfo.setLastbal(0.00);
		
		
		
		if( "3".equals(transferFlag) )
		{
			//还款本金、利息、罚息不能大于0
			if( new FabAmount(Double.valueOf(sortList.get(0).get("prinAmt").toString())).isPositive() ||
				new FabAmount(Double.valueOf(sortList.get(0).get("intAmt").toString())).isPositive()||
				new FabAmount(Double.valueOf(sortList.get(0).get("sumrFint").toString())).isPositive())
					throw new FabException("LNS099");
			
			//未还款实际还款日和结清日期不为空 2018-11-27
//			!VarChecker.isEmpty(sortList.get(0).get("settleDate").toString()) ||  
			if( 
				!VarChecker.isEmpty(sortList.get(0).get("termretDate").toString()))
				throw new FabException("LNS114");
			
		}
		//账单迁移
		if( (new FabAmount(Double.valueOf(sortList.get(0).get("prinAmt").toString())).isPositive()  ||
			new FabAmount(Double.valueOf(sortList.get(0).get("intAmt").toString())).isPositive()   ||
			new FabAmount(Double.valueOf(sortList.get(0).get("sumrFint").toString())).isPositive()) &&
			VarChecker.isEmpty(sortList.get(0).get("termretDate").toString()))
		{
			//有还款的期，实际还款日不能为空
			throw new FabException("LNS083");
		}
		
		TblLnsbill billList = new TblLnsbill();
		billList.setTrandate(Date.valueOf(ctx.getTranDate()));
		billList.setSerseqno(ctx.getSerSeqNo());

		billList.setAcctno(receiptNo);
		billList.setBrc(ctx.getBrc());
		billList.setPeriod(Integer.valueOf(sortList.get(0).get("repayTerm").toString()));
		billList.setBillproperty("TRANS");
		billList.setLastbal(0.00);
		billList.setLastdate(sortList.get(0).get("termretDate").toString());
		billList.setAccumulate(0.00);
		billList.setBegindate(sortList.get(0).get("repayintBdate").toString());
		billList.setEnddate(sortList.get(0).get("termretDate").toString());
		billList.setCurenddate(sortList.get(0).get("repayintEdate").toString());
		billList.setRepayedate(CalendarUtil.nDaysAfter(sortList.get(0).get("repayintEdate").toString(), graceDays).toString("yyyy-MM-dd"));
		billList.setSettledate(sortList.get(0).get("termretDate").toString());
		billList.setIntedate(sortList.get(0).get("termretDate").toString());
		billList.setFintedate("");
		billList.setCintedate("");
		billList.setRepayway(repayWay);
		billList.setCcy("01");
		billList.setDertrandate(Date.valueOf("1970-01-01"));
		billList.setDerserseqno(0);
		billList.setDertxseq(0);
		billList.setBillstatus("N");
		billList.setStatusbdate(sortList.get(0).get("termretDate").toString());
		billList.setBillproperty("TRANS");
		billList.setIntrecordflag("YES");
		billList.setCancelflag("NORMAL");
		billList.setSettleflag("RUNNING");
		
		if( new FabAmount( Double.valueOf(sortList.get(0).get("prinAmt").toString())).isPositive())
		{
			//不允许只还本
			if( !"2".equals(discountFlag) &&
				!new FabAmount( Double.valueOf(sortList.get(0).get("intAmt").toString())).isPositive() )
				throw new FabException("LNS087");
				
			billList.setTxseq(num++);
			billList.setBilltype("PRIN");
			billList.setBillamt(Double.valueOf(sortList.get(0).get("prinAmt").toString()));
			billList.setBillbal(0.00);
			billList.setPrinbal(Double.valueOf(sortList.get(0).get("noretPrin").toString()));
			billList.setBillrate(0.00);
			if( new FabAmount(billList.getBillbal()).isZero() )
				billList.setSettleflag("CLOSE");
			else{//18045158
				billList.setSettleflag("RUNNING");
			}
			insertBill(billList);
		}
		
		
			
		if( new FabAmount( Double.valueOf(sortList.get(0).get("intAmt").toString())).isPositive() )
		{
			billList.setTxseq(num++);
			billList.setBilltype("NINT");
			billList.setBillamt(0.00);
			billList.setBillbal(Double.valueOf(sortList.get(0).get("noretInt").toString()));
			billList.setPrinbal(Double.valueOf(sortList.get(0).get("termPrin").toString()));
			billList.setBillrate(normalRate.getVal().doubleValue());
			
			billList.setIntedate(sortList.get(0).get("repayintEdate").toString());
			billList.setSettledate("");
			billList.setStatusbdate(sortList.get(0).get("termretDate").toString());
			
			//当日应还利息-本期未还利息不等已还利息
			if(!new FabAmount(billList.getBillamt()).sub(billList.getBillbal()).sub(Double.valueOf(sortList.get(0).get("intAmt").toString())).isZero() )
				throw new FabException("LNS091",billList.getBillamt(),billList.getBillbal(),Double.valueOf(sortList.get(0).get("intAmt").toString()));
			
			if( new FabAmount(billList.getBillbal()).isZero() ){
				billList.setSettleflag("CLOSE");
			}else{//18045158
				billList.setSettleflag("RUNNING");
			}
				
			insertBill(billList);
		}
		
				//逾期借据无法迁移
//				throw new FabException("LNS079");
			//1019//20181025  1.4.9
			
		
		lnsrpyplan.setBrc(ctx.getBrc());
		lnsrpyplan.setAcctno(receiptNo);
		lnsrpyplan.setRepayterm(1);
		lnsrpyplan.setAcctflag("");
		lnsrpyplan.setRepayway("TRANS");
		
		
		lnsrpyplan.setRepayintbdate(startIntDate);
		lnsrpyplan.setRepayintedate(endDate);
		lnsrpyplan.setRepayownbdate(endDate);
		lnsrpyplan.setRepayownedate(CalendarUtil.nDaysAfter(endDate, graceDays).toString("yyyy-MM-dd"));
		lnsrpyplan.setTermretprin(contractAmt.getVal());
		lnsrpyplan.setTermretint(Double.valueOf(sortList.get(0).get("termInt").toString()));
		lnsrpyplan.setDeductionamt(0.0);
		lnsrpyplan.setPrinamt(0.00);
		lnsrpyplan.setIntamt(0.00);
		lnsrpyplan.setNoretamt(0.00);
		lnsrpyplan.setNoretint(0.00);
		lnsrpyplan.setTermfint(0.0);
		lnsrpyplan.setTermcint(0.0);
		lnsrpyplan.setSumfint(0.0);
		lnsrpyplan.setSumcint(0.0);
		lnsrpyplan.setDays(0);
		lnsrpyplan.setTermcdint(0.00);
		lnsrpyplan.setBalance(0.00);
		lnsrpyplan.setSumrfint(0.0);
		lnsrpyplan.setSumrcint(0.0);
		lnsrpyplan.setActrepaydate(endDate);
		lnsrpyplan.setReserve2(0.0);
		
		try{
			DbAccessUtil.execute("Lnsrpyplan.insert", lnsrpyplan);
		}catch (FabSqlException e){
			//幂等处理
			if (e.getSqlCode().equals(ConstantDeclare.SQLCODE.DUPLICATIONKEY)) {
				throw new FabException(e,ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
			}
			//非幂等报错
			else
			{
				throw new FabException(e, "SPS100", "lnsrpyplan");
			}
			
		}
		
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(receiptNo, ctx);

		
		if( "1".equals(transferFlag) )
		{
			FabAmount noRetAmt = new FabAmount(Double.valueOf(sortList.get(0).get("noretPrin").toString()));
			eventProvider.createEvent(ConstantDeclare.EVENT.LNDATRANSF, noRetAmt, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx);


			
	
			FabAmount fundAmt = new FabAmount(0.00);
			if( discountAmt.isPositive() )
			{
				//扣息税金
				FabAmount	taxAmt = TaxUtil.calcVAT(discountAmt);
				eventProvider.createEvent(ConstantDeclare.EVENT.LNDAINTTAX, taxAmt, lnsAcctInfo, null, fundInvest,ConstantDeclare.BRIEFCODE.KXSJ, ctx);
				//非标迁移扣息税金
				AccountingModeChange.saveDiscountTax(ctx,receiptNo , getDiscountAmt().getVal(), taxAmt.getVal(), ConstantDeclare.BILLTYPE.BILLTYPE_NINT);

				fundAmt = new FabAmount(noRetAmt.sub(discountAmt).getVal());
				//扣息放款的扣息金额
				eventProvider.createEvent(ConstantDeclare.EVENT.LNDADISCNT, discountAmt, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.KXJE, ctx);
			}
			else
				fundAmt = new FabAmount(noRetAmt.getVal());
			
			//放款渠道
			eventProvider.createEvent(ConstantDeclare.EVENT.LNDACHANNL, fundAmt, lnsAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.FKQD, ctx);
			
		}
		
		
		if( "3".equals(transferFlag) )
		{			
			//结束日期不能小于核心日期 2018-11-27
			if( CalendarUtil.after(ctx.getTranDate(), endDate) )
				throw new FabException("LNS071");
			
			
			//新增数据迁移总计提金额不能大于0
			if( totalInt.isPositive() )
				throw new FabException("LNS095");
				
			//本金放款
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANGRANTA, contractBal, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx);
			if(pkgList2 == null || pkgList2.size() == 0) //无追保理开户本金户表外不抛会计系统
			{
				//生成贷款本金账户
				eventProvider.createEvent(ConstantDeclare.EVENT.LOANMEBRGT, contractBal, lnsAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.BJKH, ctx);
			}
			else if( discountAmt.isPositive() )
			{
				//扣息税金
				FabAmount	taxAmt = TaxUtil.calcVAT(discountAmt);
				eventProvider.createEvent(ConstantDeclare.EVENT.DISCONTTAX, taxAmt, lnsAcctInfo, null, fundInvest,ConstantDeclare.BRIEFCODE.KXSJ, ctx);
				//扣息税金
				AccountingModeChange.saveDiscountTax(ctx,receiptNo , getDiscountAmt().getVal(), taxAmt.getVal(), ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
			}
			
			
	
			FabAmount fundAmt = new FabAmount(0.00);
			if( discountAmt.isPositive() )
			{
				fundAmt = new FabAmount(contractBal.sub(discountAmt).getVal());
				//扣息放款的扣息金额
				eventProvider.createEvent(ConstantDeclare.EVENT.LNDISCOUNT, discountAmt, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.KXJE, ctx);
			}
			else
				fundAmt = new FabAmount(contractBal.getVal());

			if(!VarChecker.isEmpty(entrustedPaybrc)) {
				List<Map<String,String>> jsonArray= new ArrayList<>();//存储成jsonArray的格式
				Map<String,String> channelInfo = new HashMap<>();
				channelInfo.put("entrustedPaybrc", entrustedPaybrc);
				channelInfo.put("code", channelType);

				jsonArray.add(channelInfo);
				Map<String,Object> stringJson = new HashMap<>();
				stringJson.put("QD", jsonArray);
				AccountingModeChange.saveInterfaceEx(ctx, receiptNo, ConstantDeclare.KEYNAME.QD,"单渠道" , JsonTransfer.ToJson(stringJson) );
			}
			else {
				entrustedPaybrc = "";
			}

				//放款渠道
			//LOANCHANEL事件登记entrustedPaybrc到reserv4 20191112
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANCHANEL, fundAmt, lnsAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.FKQD, ctx,"","","",entrustedPaybrc);


		}
		
	}

	public void insertBill(TblLnsbill lnsbill) throws FabException
	{
		try{
			DbAccessUtil.execute("Lnsbill.insert", lnsbill);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS100", "lnsbill");
		}

	}

	/**
	 * 获取明细数据
	 * @param ctx
	 * @return
	 */
	public TblLnsprovisiondtl getLnsprovisiondtl(TranCtx ctx){
		TblLnsprovisiondtl lnsprovisiondtl = new TblLnsprovisiondtl();
		lnsprovisiondtl.setTrandate(Date.valueOf(ctx.getTranDate()));
		lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
		lnsprovisiondtl.setTxnseq(1);
		lnsprovisiondtl.setBrc(ctx.getBrc());
//		lnsprovisionreg.setTeller("479001");
		lnsprovisiondtl.setAcctno(receiptNo);
//		lnsprovisionreg.setReceiptno(receiptNo);
		lnsprovisiondtl.setPeriod(0);
		lnsprovisiondtl.setListno(1);
		lnsprovisiondtl.setBilltype("NINT");
		lnsprovisiondtl.setCcy("01");
		lnsprovisiondtl.setTotalinterest(totalInt.getVal());
		lnsprovisiondtl.setTotaltax(TaxUtil.calcVAT(totalInt).getVal());
		lnsprovisiondtl.setTaxrate(0.06);
		lnsprovisiondtl.setInterest(totalInt.getVal());
		lnsprovisiondtl.setTax(TaxUtil.calcVAT(totalInt).getVal());
		lnsprovisiondtl.setIntertype("PROVISION");
		lnsprovisiondtl.setBegindate(Date.valueOf(startIntDate));
		if( "1".equals(transferFlag))
		{
			lnsprovisiondtl.setEnddate(Date.valueOf(intDays));
		}
		else
			lnsprovisiondtl.setEnddate(Date.valueOf(startIntDate));
		lnsprovisiondtl.setInterflag("POSITIVE");
		lnsprovisiondtl.setCancelflag("NORMAL");
		return lnsprovisiondtl;

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
	 * @return the merchantNo
	 */
	public String getMerchantNo() {
		return merchantNo;
	}


	/**
	 * @param merchantNo the merchantNo to set
	 */
	public void setMerchantNo(String merchantNo) {
		this.merchantNo = merchantNo;
	}


	/**
	 * @return the customName
	 */
	public String getCustomName() {
		return customName;
	}


	/**
	 * @param customName the customName to set
	 */
	public void setCustomName(String customName) {
		this.customName = customName;
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
	 * @return the endDate
	 */
	public String getEndDate() {
		return endDate;
	}


	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}


	/**
	 * @return the fundChannel
	 */
	public String getFundChannel() {
		return fundChannel;
	}


	/**
	 * @param fundChannel the fundChannel to set
	 */
	public void setFundChannel(String fundChannel) {
		this.fundChannel = fundChannel;
	}


	/**
	 * @return the contractNo
	 */
	public String getContractNo() {
		return contractNo;
	}


	/**
	 * @param contractNo the contractNo to set
	 */
	public void setContractNo(String contractNo) {
		this.contractNo = contractNo;
	}


	/**
	 * @return the outSerialNo
	 */
	public String getOutSerialNo() {
		return outSerialNo;
	}


	/**
	 * @param outSerialNo the outSerialNo to set
	 */
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}


	/**
	 * @return the contractAmt
	 */
	public FabAmount getContractAmt() {
		return contractAmt;
	}


	/**
	 * @param contractAmt the contractAmt to set
	 */
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}


	/**
	 * @return the contractBal
	 */
	public FabAmount getContractBal() {
		return contractBal;
	}


	/**
	 * @param contractBal the contractBal to set
	 */
	public void setContractBal(FabAmount contractBal) {
		this.contractBal = contractBal;
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
	 * @return the transferFlag
	 */
	public String getTransferFlag() {
		return transferFlag;
	}


	/**
	 * @param transferFlag the transferFlag to set
	 */
	public void setTransferFlag(String transferFlag) {
		this.transferFlag = transferFlag;
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
	 * @return the ccy
	 */
	public String getCcy() {
		return ccy;
	}


	/**
	 * @param ccy the ccy to set
	 */
	public void setCcy(String ccy) {
		this.ccy = ccy;
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
	 * @return the customType
	 */
	public String getCustomType() {
		return customType;
	}


	/**
	 * @param customType the customType to set
	 */
	public void setCustomType(String customType) {
		this.customType = customType;
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
	 * @return the graceDays
	 */
	public Integer getGraceDays() {
		return graceDays;
	}

	/**
	 * @param graceDays the graceDays to set
	 */
	public void setGraceDays(Integer graceDays) {
		this.graceDays = graceDays;
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
	 * @return the channelType
	 */
	public String getChannelType() {
		return channelType;
	}

	/**
	 * @param channelType the channelType to set
	 */
	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	/**
	 *
	 * @return oldrepayacct get
	 */
	public String getOldrepayacct() {
		return oldrepayacct;
	}

	/**
	 *
	 * @param oldrepayacct set
	 */
	public void setOldrepayacct(String oldrepayacct) {
		this.oldrepayacct = oldrepayacct;
	}

	/**
	 * @return the intDays
	 */
	public String getIntDays() {
		return intDays;
	}

	/**
	 * @param intDays the intDays to set
	 */
	public void setIntDays(String intDays) {
		this.intDays = intDays;
	}


	/**
	 * Gets the value of entrustedPaybrc.
	 *
	 * @return the value of entrustedPaybrc
	 */
	public String getEntrustedPaybrc() {
		return entrustedPaybrc;
	}

	/**
	 * Sets the entrustedPaybrc.
	 *
	 * @param entrustedPaybrc entrustedPaybrc
	 */
	public void setEntrustedPaybrc(String entrustedPaybrc) {
		this.entrustedPaybrc = entrustedPaybrc;

	}
}
