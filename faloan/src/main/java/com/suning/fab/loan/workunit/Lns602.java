package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.FundInvest;
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

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Scope("prototype")
@Repository
public class Lns602 extends WorkUnit {
	
	String receiptNo;  //借据号
	String openBrc; //开户机构
	String repayWay;//还款方式
	String startIntDate;//起息日
	int graceDays;//宽限期
	FabAmount contractAmt;//合同金额
	FabAmount contractBal;//合同余额
	FabRate normalRate;//贷款正常利率
	FabRate overdueRate;//贷款逾期利率
	FabRate compoundRate;//贷款复利率
	
	
	String		merchantNo;
	String		customName;//客户名称
	String		productCode;//产品编号
	ListMap		pkgList;//债务公司
	String		openDate;//开户日
	String		endDate;//合同结束日
	String		fundChannel;
	String		contractNo;
	String		outSerialNo;		//付款单号/易付宝流水号
	String  	customType;//客户类型
	String  	discountFlag;
	FabAmount	discountAmt;
	String		transferFlag;	//迁移标志 （1-普通 2-非标准）
	String		serialNo;		//业务流水号
	String		ccy;			//币种
	FabAmount	totalInt;	//合同余额
	String		channelType; //放款渠道
	String lastDate;
	FabAmount	noRetInt = new FabAmount(0.00);
	FabAmount	noRetPrin = new FabAmount(0.00);
	FabAmount	fundAmt;
	
	String		oldrepayacct;
	


	@Autowired LoanEventOperateProvider eventProvider;
	
	@Override
	public void run() throws Exception{
		//2020-07-15 涉敏字段
		customName="";
		
		TranCtx ctx = getTranctx();
		Integer num = 1;
		FabAmount total = new FabAmount(0.00);		//利息总额

		FabAmount sumPrin = new FabAmount(0.00);
		FabAmount noRetAmt = new FabAmount(0.00);
		FabAmount totalAmt = new FabAmount(0.00);
		FabAmount totalInterest = new FabAmount(0.00);
		ListMap pkgList = ctx.getRequestDict("pkgList");
		TblLnsnonstdplan lnsnonstdplan =  new TblLnsnonstdplan();
		TblLnsrpyplan lnsrpyplan =  new TblLnsrpyplan();
		
		List<Map> sortList =  pkgList.ToList();
		
		//2019-11-28  多期不支持扣息放款
		if( "2".equals(discountFlag) ){
			throw new FabException("LNS199");
		}
		if( "1".equals(discountFlag) ){
			if( !discountAmt.isZero() )
				throw new FabException("LNS198");
		}
		
		//判断是否已存在
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", receiptNo);
		param.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo basicinfo = null;
		try {
			basicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		
		if (null != basicinfo){
			//借据号已存在
			throw new FabException("LNS080", receiptNo);
		}
				
				
		//起息日期不可为空
		if( VarChecker.isEmpty(startIntDate))
			throw new FabException("LNS038");
		
		
		//起息日期不能大于等于到期日期
		if( CalendarUtil.afterAlsoEqual(startIntDate, endDate) )
			throw new FabException("LNS082",startIntDate,endDate);
		

		//非标自定义不能有扣息金额/债务公司
		//todo
		ListMap pkgList2 = ctx.getRequestDict("pkgList2");
		if( !VarChecker.isEmpty(pkgList2)  && pkgList2.size() != 0) 
		{
			//事件登记簿账户参数赋值
			FundInvest  fundInvest = new FundInvest();
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

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
			
			List<Map> sortList2 =  pkgList2.ToList();
			FabAmount totalDebtAmt = new FabAmount(0.00);
			int i = 0;
			//循环债务公司信息
			for (Map sortListTmp2:sortList2) {
				Map<String,Object> param2 = new HashMap<String,Object>();
				param2.put("brc", ctx.getBrc());
				param2.put("accsrccode", "D");
				param2.put("acctno", sortListTmp2.get("debtCompany").toString());
				TblLnsprefundaccount acctInfo = null;
				try {
					acctInfo = DbAccessUtil.queryForObject("Lnsprefundaccount.selectByUk", param2, TblLnsprefundaccount.class);
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
			//
			//2018-12-10 买方付息登记债务公司信息到主文件拓展表
			if( Arrays.asList("3010013","3010015").contains(productCode)  )
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
		
		
		
		
		if( new FabAmount(discountAmt.getVal()).isPositive() ||
			"2".equals(discountFlag) 
//			||(!VarChecker.isEmpty(pkgList2) && pkgList2.size()!=0 ) 
			)
			throw new FabException("LNS093");
		
		if( !sortList.get(0).get("repayintBdate").toString().equals(startIntDate) || 
			!sortList.get(sortList.size()-1).get("repayintEdate").toString().equals(endDate))
			throw new FabException("LNS097");
		
		//事件登记簿赋值
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		lnsAcctInfo.setMerchantNo(merchantNo);
		lnsAcctInfo.setCustType(customType);
		lnsAcctInfo.setCustName(customName);
		lnsAcctInfo.setPrdCode(productCode);
		
		
		
		//幂等
		TblLnsinterface	lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setSerialno(getSerialNo());
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode("479000");
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setAcctname(customName);
		lnsinterface.setUserno(merchantNo);
		lnsinterface.setAcctno(receiptNo);
		lnsinterface.setTranamt(contractBal.getVal());
		lnsinterface.setMagacct(receiptNo);
		lnsinterface.setBankno(outSerialNo);
		lnsinterface.setReserv5(channelType);
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
		
		
		
		//预收登记簿
//		TblLnsprefundaccount lnsprefundaccount = new TblLnsprefundaccount();
//		lnsprefundaccount.setBrc(ctx.getBrc());
//		lnsprefundaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
//		lnsprefundaccount.setOpendate(openDate);
//		lnsprefundaccount.setClosedate("");
//		lnsprefundaccount.setStatus(ConstantDeclare.STATUS.NORMAL);
//		lnsprefundaccount.setReverse1("");
//		lnsprefundaccount.setReverse2("");
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
//			LoggerUtil.info("lns602|query_oldrepayacctno:" + getMerchantNo());
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
//		TblLnsprovisionreg lnsprovisionreg = new TblLnsprovisionreg();
//		lnsprovisionreg.setTrandate(Date.valueOf(ctx.getTranDate()));
//		lnsprovisionreg.setSerseqno(ctx.getSerSeqNo());
//		lnsprovisionreg.setTxnseq(1);
//		lnsprovisionreg.setBrc(ctx.getBrc());
//		lnsprovisionreg.setTeller("479001");
//		lnsprovisionreg.setReceiptno(receiptNo);
//		lnsprovisionreg.setPeriod(1);
//		lnsprovisionreg.setBilltype("NINT");
//		lnsprovisionreg.setCcy("01");
//		lnsprovisionreg.setTotalinterest(totalInt.getVal());
//		lnsprovisionreg.setTotaltax(TaxUtil.calcVAT(totalInt).getVal());
//		lnsprovisionreg.setTaxrate(0.06);
//		lnsprovisionreg.setInterest(totalInt.getVal());
//		lnsprovisionreg.setTax(TaxUtil.calcVAT(totalInt).getVal());
//		lnsprovisionreg.setIntertype("PROVISION");
//		lnsprovisionreg.setBegindate(Date.valueOf(startIntDate));
//		lnsprovisionreg.setEnddate(Date.valueOf("2018-08-31"));
//		lnsprovisionreg.setInterflag("POSITIVE");
//		lnsprovisionreg.setSendflag("9");
//		lnsprovisionreg.setSendnum(0);
//		lnsprovisionreg.setCancelflag("NORMAL");
		


//		if( "2".equals(transferFlag))
//		{
//			try{
//				DbAccessUtil.execute("Lnsprovisionreg.insert", lnsprovisionreg);
//			}
//			catch (FabSqlException e)
//			{
//				throw new FabException(e, "SPS100", "lnsprovisionreg");
//			}
//		}
		
		
		
		
		
		
		
		
		
		
		
		
		FabAmount	contractAmtTmp = new FabAmount(getContractBal().getVal());
		//已生成账单本金18045158
		FabAmount existBillPrin = new FabAmount(0.0);
		for (Map sortListTmp:sortList) {
			
			
			//当期应还利息不能为0
			if( !"14".equals(repayWay) && !new FabAmount(Double.valueOf(sortListTmp.get("termInt").toString())).isPositive())
			{
				throw new FabException("LNS094");
			}

			
			
			if( "4".equals(transferFlag) )
			{
				//还款本金、利息、罚息不能大于0
				if( new FabAmount(Double.valueOf(sortListTmp.get("prinAmt").toString())).isPositive() ||
					new FabAmount(Double.valueOf(sortListTmp.get("intAmt").toString())).isPositive()||
					new FabAmount(Double.valueOf(sortListTmp.get("sumrFint").toString())).isPositive())
						throw new FabException("LNS099");
				
//				//未还款实际还款日和结清日期不为空  2018-11-27
//				!VarChecker.isEmpty(sortListTmp.get("settleDate").toString()) ||  
				if( 
					!VarChecker.isEmpty(sortListTmp.get("termretDate").toString()))
					throw new FabException("LNS114");
				
				//结束日期不能小于核心日期 2018-11-27
				if( CalendarUtil.after(ctx.getTranDate(), sortListTmp.get("repayintEdate").toString()) )
					throw new FabException("LNS071");
				
			}
			
			
			//有还款的期，实际还款日不能为空
			if( (new FabAmount(Double.valueOf(sortListTmp.get("prinAmt").toString())).isPositive() ||  
				 new FabAmount(Double.valueOf(sortListTmp.get("intAmt").toString())).isPositive()) &&
				 VarChecker.isEmpty(sortListTmp.get("termretDate").toString()))
				throw new FabException("LNS083");
			
			
			
			if( CalendarUtil.before(sortListTmp.get("repayintBdate").toString(), ctx.getTranDate()) &&
				CalendarUtil.beforeAlsoEqual(sortListTmp.get("repayintEdate").toString(), ctx.getTranDate()))
			{
				
				
				//当期应还金额减未还金额不等于已还金额
				if( !new FabAmount(Double.valueOf(sortListTmp.get("termPrin").toString())).sub(Double.valueOf(sortListTmp.get("noretPrin").toString())).sub(Double.valueOf(sortListTmp.get("prinAmt").toString())).isZero()   )
					throw new FabException("LNS096",sortListTmp.get("repayTerm").toString());
				//当期应还金额减未还金额不等于已还金额
				if( !new FabAmount(Double.valueOf(sortListTmp.get("termInt").toString())).sub(Double.valueOf(sortListTmp.get("noretInt").toString())).sub(Double.valueOf(sortListTmp.get("intAmt").toString())).isZero()   )
					throw new FabException("LNS096",sortListTmp.get("repayTerm").toString());
				
				lastDate =sortListTmp.get("repayintEdate").toString();
				noRetInt.selfAdd(Double.valueOf(sortListTmp.get("noretInt").toString()));
				noRetPrin.selfAdd(Double.valueOf(sortListTmp.get("noretPrin").toString()));
				//账单迁移
				TblLnsbill billList = new TblLnsbill();
				billList.setTrandate(Date.valueOf(ctx.getTranDate()));
				billList.setSerseqno(ctx.getSerSeqNo());

				billList.setAcctno(receiptNo);
				billList.setBrc(ctx.getBrc());
				billList.setPeriod(Integer.valueOf(sortListTmp.get("repayTerm").toString()));
				billList.setBillproperty("TRANS");
				billList.setLastbal(0.00);
				billList.setLastdate(sortListTmp.get("termretDate").toString());
				billList.setAccumulate(0.00);
				billList.setBegindate(sortListTmp.get("repayintBdate").toString());
				billList.setEnddate(sortListTmp.get("repayintEdate").toString());
				billList.setCurenddate(sortListTmp.get("repayintEdate").toString());
				billList.setRepayedate(CalendarUtil.nDaysAfter(sortListTmp.get("repayintEdate").toString(), graceDays).toString("yyyy-MM-dd"));
				billList.setSettledate(sortListTmp.get("repayintEdate").toString());
				billList.setIntedate(sortListTmp.get("repayintEdate").toString());
				billList.setFintedate("");
				billList.setCintedate("");
				billList.setRepayway("13");
				if( !VarChecker.isEmpty(repayWay) && "14".equals(repayWay)){
					billList.setRepayway(repayWay);
				}
				billList.setCcy("01");
				billList.setDertrandate(Date.valueOf("1970-01-01"));
				billList.setDerserseqno(0);
				billList.setDertxseq(0);
				billList.setBillstatus("N");
				billList.setStatusbdate(sortListTmp.get("repayintEdate").toString());
				billList.setBillproperty("TRANS");
				billList.setIntrecordflag("YES");
				billList.setCancelflag("NORMAL");
				billList.setSettleflag("RUNNING");
				
				if( new FabAmount( Double.valueOf(sortListTmp.get("termPrin").toString())).isPositive())
				{
					billList.setTxseq(num++);
					billList.setBilltype("PRIN");
					billList.setBillamt(Double.valueOf(sortListTmp.get("termPrin").toString()));
					//已生成账单本金18045158
					existBillPrin.selfAdd(Double.valueOf(sortListTmp.get("termPrin").toString()));
					billList.setBillbal(Double.valueOf(sortListTmp.get("noretPrin").toString()));
					billList.setPrinbal(Double.valueOf(sortListTmp.get("balance").toString()));
					billList.setBillrate(0.00);
					
					if( new FabAmount(billList.getBillbal()).isZero() )
						billList.setSettleflag("CLOSE");
					else{//18045158
						billList.setSettleflag("RUNNING");
					}
					insertBill(billList);
				}
				
			
				
				if( new FabAmount( Double.valueOf(sortListTmp.get("termInt").toString())).isPositive() )
				{
					
					billList.setTxseq(num++);
					billList.setBilltype("NINT");
					billList.setBillamt(Double.valueOf(sortListTmp.get("termInt").toString()));
					billList.setBillbal(Double.valueOf(sortListTmp.get("noretInt").toString()));
					billList.setPrinbal(Double.valueOf(sortListTmp.get("termPrin").toString()));
					billList.setBillrate(normalRate.getVal().doubleValue());
					
					if( new FabAmount(billList.getBillbal()).isZero() )
						billList.setSettleflag("CLOSE");
					else{//18045158
						billList.setSettleflag("RUNNING");
					}
					insertBill(billList);
				}
				
					//逾期借据无法迁移
//					throw new FabException("LNS079");
				
				
				
				
				
			}
			else{
				//当前期/未来期不允许提前还款
				if( new FabAmount(Double.valueOf(sortListTmp.get("prinAmt").toString())).isPositive() ||
					new FabAmount(Double.valueOf(sortListTmp.get("intAmt").toString())).isPositive()||
					new FabAmount(Double.valueOf(sortListTmp.get("sumrFint").toString())).isPositive())
						throw new FabException("LNS101");
				
				sumPrin.selfAdd(Double.valueOf(sortListTmp.get("termPrin").toString()));
					
			}
			
			totalAmt.selfAdd(Double.valueOf(sortListTmp.get("termPrin").toString()));
			totalInterest.selfAdd(Double.valueOf(sortListTmp.get("termInt").toString()));
			
			noRetAmt.selfAdd(Double.valueOf(sortListTmp.get("noretPrin").toString()));
			
			//判断总本金是否有问题18045158
//			if( BigDecimal.valueOf(contractBal.getVal()).add(BigDecimal.valueOf(existBillPrin.getVal())) == BigDecimal.valueOf(contractAmt.getVal())){
//				throw new FabException("LNS086",contractAmt,contractBal,existBillPrin);
//			}

			
			lnsrpyplan.setAcctno(receiptNo);
			lnsrpyplan.setBrc(openBrc);
			lnsrpyplan.setRepayterm(Integer.parseInt(sortListTmp.get("repayTerm").toString()));
			lnsrpyplan.setRepayway("13");
			if( !VarChecker.isEmpty(repayWay) && "14".equals(repayWay)){
				lnsrpyplan.setRepayway(repayWay);
			}
			lnsrpyplan.setRepayintbdate(sortListTmp.get("repayintBdate").toString());
			lnsrpyplan.setRepayintedate(sortListTmp.get("repayintEdate").toString());
			lnsrpyplan.setRepayownbdate(sortListTmp.get("repayintEdate").toString());
			lnsrpyplan.setRepayownedate(CalendarUtil.nDaysAfter(sortListTmp.get("repayintEdate").toString(), graceDays).toString("yyyy-MM-dd"));
			//本期应还本金
			FabAmount termPrin = new FabAmount(Double.parseDouble(sortListTmp.get("termPrin").toString()));
			lnsrpyplan.setTermretprin(termPrin.getVal());
			//本期应还利息
			total.selfAdd(Double.parseDouble(sortListTmp.get("termInt").toString()));
			FabAmount termInt = new FabAmount(Double.parseDouble(sortListTmp.get("termInt").toString()));
			lnsrpyplan.setTermretint(termInt.getVal());
			//扣息金额
			lnsrpyplan.setDeductionamt(0.0);
			//已还本金
			lnsrpyplan.setPrinamt(Double.parseDouble(sortListTmp.get("prinAmt").toString()));
			//已还利息
			lnsrpyplan.setIntamt(Double.parseDouble(sortListTmp.get("intAmt").toString()));
			//未还本金
			lnsrpyplan.setNoretamt(Double.parseDouble(sortListTmp.get("noretPrin").toString()));
			//未还利息
			lnsrpyplan.setNoretint(Double.parseDouble(sortListTmp.get("noretInt").toString()));
			//未还罚息
			lnsrpyplan.setTermfint(0.0);
			//未还复利
			lnsrpyplan.setTermcint(0.0);
			//累计罚息
			lnsrpyplan.setSumfint(0.0);
			//累计复利
			lnsrpyplan.setSumcint(0.0);
			//计息天数
			lnsrpyplan.setDays(0);
			//剩余结息利息
			lnsrpyplan.setTermcdint(Double.parseDouble(sortListTmp.get("termcdInt").toString()));
			//贷款剩余本金
			FabAmount balance = new FabAmount(Double.parseDouble(sortListTmp.get("balance").toString()));
			lnsrpyplan.setBalance(balance.getVal());
			//已还罚息
			lnsrpyplan.setSumrfint(0.0);
			//已还复利
			lnsrpyplan.setSumrcint(0.0);
			//本期实际止日
			lnsrpyplan.setActrepaydate(sortListTmp.get("repayintEdate").toString());
			lnsrpyplan.setReserve2(0.0);
			
			lnsnonstdplan.setAcctno(receiptNo);
			lnsnonstdplan.setOpenbrc(openBrc);
			lnsnonstdplan.setRepayterm(Integer.parseInt(sortListTmp.get("repayTerm").toString()));
			lnsnonstdplan.setPrinmindays(0);
			lnsnonstdplan.setIntmindays(0);
			lnsnonstdplan.setRepayway("13");
			if( !VarChecker.isEmpty(repayWay) && "14".equals(repayWay)){
				lnsnonstdplan.setRepayway(repayWay);
			}
			//prinperformula
			lnsnonstdplan.setPrinformula("3");
			lnsnonstdplan.setLastprindate(startIntDate);
			lnsnonstdplan.setIntformula("3");
			lnsnonstdplan.setLastintdate(startIntDate);
			lnsnonstdplan.setPrinterms(0);
			lnsnonstdplan.setIntterms(0);
			lnsnonstdplan.setCurprinterm(0);
			lnsnonstdplan.setCurintterm(0);
			lnsnonstdplan.setOldprinterm(0);
			lnsnonstdplan.setOldintterm(0);
			lnsnonstdplan.setContractbal(contractBal.getVal());
			lnsnonstdplan.setGracedays(graceDays);
			lnsnonstdplan.setExtnums(0);
			lnsnonstdplan.setBjtxbl(0.0);
			lnsnonstdplan.setContractamt(contractAmt.getVal());
			lnsnonstdplan.setDeductionamt(0.0);
			lnsnonstdplan.setNormalrate(normalRate.getVal().doubleValue());
			lnsnonstdplan.setOverduerate(overdueRate.getVal().doubleValue());
			lnsnonstdplan.setOverduerate1(compoundRate.getVal().doubleValue());
			lnsnonstdplan.setReservamt1(0.0);
			lnsnonstdplan.setTermbdate(sortListTmp.get("repayintBdate").toString());
			lnsnonstdplan.setTermedate(sortListTmp.get("repayintEdate").toString());
			lnsnonstdplan.setIntbdate(sortListTmp.get("repayintBdate").toString());
			lnsnonstdplan.setIntedate(sortListTmp.get("repayintEdate").toString());
			lnsnonstdplan.setTermprin(termPrin.getVal());
			if( new FabAmount(Double.parseDouble(sortListTmp.get("intAmt").toString())).isPositive() ||
				"14".equals(repayWay))
			{
				lnsnonstdplan.setTermint(termInt.getVal());
				lnsrpyplan.setTermretint(termInt.getVal());
			}
			else
			{
				int nDays = CalendarUtil.actualDaysBetween(lnsnonstdplan.getIntbdate(), lnsnonstdplan.getIntedate());
				LoggerUtil.debug("contractAmt"+contractAmt+"nDays"+nDays);
				BigDecimal intDec = new BigDecimal(nDays);
				intDec = (intDec.multiply(normalRate.getDayRate())
						.multiply(new BigDecimal(contractAmtTmp.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
				termInt = new FabAmount(intDec.doubleValue());
				contractAmtTmp.selfSub(termPrin);
				
				lnsnonstdplan.setTermint(termInt.getVal());
				lnsrpyplan.setTermretint(termInt.getVal());
			}
				
			lnsnonstdplan.setBalance(balance.getVal());
			lnsnonstdplan.setDays(pkgList.size());
			lnsnonstdplan.setCalcintflag1("1");
			lnsnonstdplan.setCalcintflag2("1");
			lnsnonstdplan.setRatetype("Y");
			
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
			
			try{
				DbAccessUtil.execute("Lnsnonstdplan.insert", lnsnonstdplan);
			}catch (FabSqlException e){
				//幂等处理
				if (e.getSqlCode().equals(ConstantDeclare.SQLCODE.DUPLICATIONKEY)) {
					throw new FabException(e,ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
				}
				//非幂等报错
				else
				{
					throw new FabException(e, "SPS100", "lnsnonstdplan");
				}
				
			}
		}
		//未还总本金不等于合同余额
		if( !noRetAmt.sub(contractBal).isZero() )
			throw new FabException("LNS084",noRetAmt,contractBal);
		
		//应还总本金不等于合同金额
		if( !totalAmt.sub(contractAmt).isZero() )
			throw new FabException("LNS085",totalAmt,contractAmt);
		
//		//合同金额-合同余额不等于已还本金
//		if( !new FabAmount(contractBal.getVal()).add(existBillPrin).sub(contractAmt).isZero() ){
//			throw new FabException("LNS102",contractAmt,contractBal,existBillPrin);
//		}
		//合同金额-合同余额不等于已还本金
		if( !new FabAmount(sumPrin.getVal()).add(existBillPrin).sub(contractAmt).isZero() ){
			throw new FabException("LNS102",contractAmt,contractBal,existBillPrin);
		}
		
		
		//14还款方式供计提使用
		if( "14".equals(repayWay)  )
		{
			//修改主文件拓展表
			Map<String,Object> exParam = new HashMap<>();
			exParam.put("acctno", receiptNo);
			exParam.put("brc", ctx.getBrc());
			exParam.put("key", "ZDY");
			exParam.put("value1", "");
			exParam.put("value2", total.getVal());
			exParam.put("value3", 0.00);
			exParam.put("tunneldata", "");

			try {
				DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsbasicinfoex");
			}
		}
		
		//动态表
		TblLnsaccountdyninfo lnsaccountdyninfo = new TblLnsaccountdyninfo();
		lnsaccountdyninfo.setAcctno(receiptNo);		
		lnsaccountdyninfo.setAcctstat("PRIN.N");
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
		lnsaccountdyninfo.setCtrlbal(noRetPrin.getVal());
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
		lnsaccountdyninfo.setCurrbal(noRetInt.getVal());
		lnsaccountdyninfo.setRatetype("");
		lnsaccountdyninfo.setAcctstat("NINT.N");
		if( "2".equals(transferFlag))
		{
			try{
				DbAccessUtil.execute("Lnsaccountdyninfo.insert", lnsaccountdyninfo);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS100", "lnsaccountdyninfo");
			}
		}

		
		//主文件
		TblLnsbasicinfo lnsbasicinfo = new TblLnsbasicinfo();
		lnsbasicinfo.setOpenbrc(ctx.getBrc());
		lnsbasicinfo.setAcctno(receiptNo);
		lnsbasicinfo.setAcctype("0");
		lnsbasicinfo.setCcy("01");
		
		if( "2".equals(customType) )
			lnsbasicinfo.setCusttype("COMPANY");
		else
			//账户类型只允许对公
			throw new FabException("LNS078");
		
		lnsbasicinfo.setCustomid(merchantNo);
		lnsbasicinfo.setName(customName);
		lnsbasicinfo.setPrdcode(productCode);
		if( "2".equals(transferFlag) ||
			"4".equals(transferFlag) )
		{
			if( !"14".equals(repayWay) )
				lnsbasicinfo.setFlag1("2");
			else
				lnsbasicinfo.setFlag1("");
		}

		else
			lnsbasicinfo.setFlag1("");
		if( !VarChecker.isEmpty(pkgList2)  && pkgList2.size() != 0) 
			lnsbasicinfo.setFlag1(lnsbasicinfo.getFlag1().concat("E"));

		lnsbasicinfo.setPrinmindays(0);
		lnsbasicinfo.setIntmindays(0);
		//迁移暂时都为未逾期的
		lnsbasicinfo.setLoanstat("N");
		lnsbasicinfo.setProfitbrc(ctx.getBrc());
		lnsbasicinfo.setAcctno1(receiptNo);
		lnsbasicinfo.setFirstprindate("");
		lnsbasicinfo.setFirstintdate("");
		lnsbasicinfo.setFirstintflag("1");
		lnsbasicinfo.setOldprinterm(0);
		lnsbasicinfo.setOldintterm(0);
		if( VarChecker.isEmpty(openDate) )
			lnsbasicinfo.setOpendate(startIntDate);
		else
			lnsbasicinfo.setOpendate(openDate);
		lnsbasicinfo.setBeginintdate(startIntDate);
		lnsbasicinfo.setGracedays(Integer.valueOf(graceDays));
		lnsbasicinfo.setContduedate(endDate);
		lnsbasicinfo.setIntenddate("");
		lnsbasicinfo.setExtnums(0);
		lnsbasicinfo.setIntrateplan("");
		lnsbasicinfo.setBjtxbl(0.00);
		lnsbasicinfo.setTxacctno("");
		lnsbasicinfo.setDiacct(receiptNo);
		lnsbasicinfo.setInvestee("");
		lnsbasicinfo.setInvestmode("");
		lnsbasicinfo.setChanneltype("1");
		lnsbasicinfo.setFundchannel(fundChannel);
		lnsbasicinfo.setOutserialno(outSerialNo);
		//2019-09-26 合同号超过20位存拓展表
		if( !VarChecker.isEmpty(getContractNo()) )
		{
			if( contractNo.length() <= 20 )
				lnsbasicinfo.setContrasctno(contractNo);
			else
			{
				//修改主文件拓展表
				Map<String,Object> exParam = new HashMap<>();
				exParam.put("acctno", lnsbasicinfo.getAcctno());
				exParam.put("brc", ctx.getBrc());
				exParam.put("key", "HTBM");
				exParam.put("value1", getContractNo());
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
		lnsbasicinfo.setContractamt(contractAmt.getVal());
		lnsbasicinfo.setOpenfee(0.00);
		lnsbasicinfo.setProvisionflag("RUNNING");
		lnsbasicinfo.setMemo("");
		lnsbasicinfo.setNormalrate(Double.valueOf(normalRate.toString()));
		lnsbasicinfo.setOverduerate(Double.valueOf(overdueRate.toString()));
		lnsbasicinfo.setOverduerate1(Double.valueOf(compoundRate.toString()));
		lnsbasicinfo.setModifydate(ctx.getTranDate());
		lnsbasicinfo.setModifytime(ctx.getTranTime());
		lnsbasicinfo.setTrandate(ctx.getTranDate());
		lnsbasicinfo.setReserv1("");
		lnsbasicinfo.setReservamt1(0.00);
		
		
		lnsbasicinfo.setIscalint("YES");
		lnsbasicinfo.setRepayway("13");
		if( !VarChecker.isEmpty(repayWay) && "14".equals(repayWay)){
			lnsbasicinfo.setRepayway(repayWay);
		}
		lnsbasicinfo.setLoantype1("3");
		
		lnsbasicinfo.setPrinperformula("");
		lnsbasicinfo.setPrinformula("3");
		lnsbasicinfo.setLastprindate(lastDate);
		
		lnsbasicinfo.setIntperformula("");
		lnsbasicinfo.setIntformula("3");
		lnsbasicinfo.setLastintdate(lastDate);
		
		lnsbasicinfo.setPrinterms(0);
		lnsbasicinfo.setIntterms(0);	
		lnsbasicinfo.setCurprinterm(pkgList.size());
		lnsbasicinfo.setCurintterm(pkgList.size());
		//2018-10-24
		lnsbasicinfo.setContractbal(sumPrin.getVal());
		lnsbasicinfo.setDeductionamt(0.00);
		
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
//			LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(receiptNo, ctx);
//			//查询未结清状态的本金利息账单
//			List<TblLnsbill> billLists;
//			Map<String, Object> param2 = new HashMap<String, Object>();
//			param2.put("acctno", receiptNo);
//			param2.put("brc", ctx.getBrc());
//			try{
//				billLists = DbAccessUtil.queryForList("CUSTOMIZE.query_runingprinintbills", param2, TblLnsbill.class);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsbill");
//			}
		if( "2".equals(transferFlag) )
		{
			FundInvest openFundInvest = new FundInvest("", "", getChannelType(), getFundChannel(), getOutSerialNo());	
			eventProvider.createEvent(ConstantDeclare.EVENT.LNDATRANSF, noRetAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx);
			
			
			if(getDiscountAmt() != null)
				fundAmt = new FabAmount(noRetAmt.sub(discountAmt).getVal());
			else
				fundAmt = new FabAmount(noRetAmt.getVal());
			eventProvider.createEvent(ConstantDeclare.EVENT.LNDACHANNL, fundAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.FKQD, ctx);
		}
		
		if( "4".equals(transferFlag) )
		{
			//新增数据迁移总计提金额不能大于0
			if( totalInt.isPositive() )
				throw new FabException("LNS095");
			
			
			
			
			FundInvest openFundInvest = new FundInvest("", "", getChannelType(), getFundChannel(), getOutSerialNo());	
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANMEBRGT, new FabAmount(0.00), lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.BJKH, ctx);
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANGRANTA, contractBal, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx);
						
			
			if(getDiscountAmt() != null)
				fundAmt = new FabAmount(contractBal.sub(discountAmt).getVal());
			else
				fundAmt = new FabAmount(contractBal.getVal());
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANCHANEL, fundAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.FKQD, ctx);
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

	public String getReceiptNo() {
		return receiptNo;
	}

	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	public String getOpenBrc() {
		return openBrc;
	}

	public void setOpenBrc(String openBrc) {
		this.openBrc = openBrc;
	}

	public String getRepayWay() {
		return repayWay;
	}

	public void setRepayWay(String repayWay) {
		this.repayWay = repayWay;
	}

	public int getGraceDays() {
		return graceDays;
	}

	public void setGraceDays(int graceDays) {
		this.graceDays = graceDays;
	}

	public FabAmount getContractAmt() {
		return contractAmt;
	}

	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	public FabAmount getContractBal() {
		return contractBal;
	}

	public void setContractBal(FabAmount contractBal) {
		this.contractBal = contractBal;
	}

	public FabRate getNormalRate() {
		return normalRate;
	}

	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}

	public FabRate getOverdueRate() {
		return overdueRate;
	}

	public void setOverdueRate(FabRate overdueRate) {
		this.overdueRate = overdueRate;
	}

	public FabRate getCompoundRate() {
		return compoundRate;
	}

	public void setCompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
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
	 * @return the lastDate
	 */
	public String getLastDate() {
		return lastDate;
	}


	/**
	 * @param lastDate the lastDate to set
	 */
	public void setLastDate(String lastDate) {
		this.lastDate = lastDate;
	}


	/**
	 * @return the noRetInt
	 */
	public FabAmount getNoRetInt() {
		return noRetInt;
	}


	/**
	 * @param noRetInt the noRetInt to set
	 */
	public void setNoRetInt(FabAmount noRetInt) {
		this.noRetInt = noRetInt;
	}


	/**
	 * @return the noRetPrin
	 */
	public FabAmount getNoRetPrin() {
		return noRetPrin;
	}


	/**
	 * @param noRetPrin the noRetPrin to set
	 */
	public void setNoRetPrin(FabAmount noRetPrin) {
		this.noRetPrin = noRetPrin;
	}


	/**
	 * @return the fundAmt
	 */
	public FabAmount getFundAmt() {
		return fundAmt;
	}


	/**
	 * @param fundAmt the fundAmt to set
	 */
	public void setFundAmt(FabAmount fundAmt) {
		this.fundAmt = fundAmt;
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
	 *
	 * @return oldrepayacct oldrepayacct
	 */
	public String getOldrepayacct() {
		return oldrepayacct;
	}

	/**
	 *
	 * @param oldrepayacct oldrepayacct
	 */
	public void setOldrepayacct(String oldrepayacct) {
		this.oldrepayacct = oldrepayacct;
	}

	
}
