package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblFeemanagereg;
import com.suning.fab.loan.domain.TblLnsamortizeplan;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.TaxUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉：费用迁移
 *
 * @author 18043620
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns666 extends WorkUnit {

	String acctNo; // 借据号
	String childBrc;
	
	
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator suber;

	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		setChildBrc(ctx.getBrc().trim());
		
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("acctno", acctNo);
		Map<String, Object> basicInfo;
		try{
			basicInfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsbasicinfo_521", paramMap);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (basicInfo == null)
		{
			throw new FabException("SPS104", acctNo);
		}
		ctx.setBrc(basicInfo.get("OPENBRC").toString());
		List<TblFeemanagereg> manageregList;
		Map<String, Object> manageParam = new HashMap<>();
		manageParam.put("acctno", acctNo);
		manageParam.put("openbrc", childBrc);
		try{
			manageregList = DbAccessUtil.queryForList("CUSTOMIZE.query_feemanagereg_666", manageParam, TblFeemanagereg.class);
		}catch (FabSqlException e){
            throw new FabException(e, "SPS103", "feemanagereg");
        }

        if (CollectionUtils.isEmpty(manageregList)){
           throw new FabException("SPS104", "feemanagereg");
        }
		
		insertLnsFeeInfo(ctx, acctNo, basicInfo, manageregList, childBrc);
	}
	
	public void insertLnsFeeInfo(TranCtx ctx, String acctNo, Map<String, Object> basicInfo, List<TblFeemanagereg> manageregList, String childBrc) throws FabException {
		TblLnsfeeinfo lnsfeeinfo = new TblLnsfeeinfo();
		lnsfeeinfo.setOpenbrc(basicInfo.get("OPENBRC").toString());     // 开户机构代码
		lnsfeeinfo.setAcctno(acctNo);            // 帐号
		lnsfeeinfo.setCcy(basicInfo.get("CCY").toString());            // 币种
		lnsfeeinfo.setFeebrc(childBrc);	           // 费用机构代码
		lnsfeeinfo.setCalculatrule("TERM");
		lnsfeeinfo.setTrandate(ctx.getTranDate());	       // 交易日期(登记迁移时间)
		lnsfeeinfo.setTrantime(ctx.getTranTime());	       // 交易时间(登记迁移时间)
		lnsfeeinfo.setLastfeedate(manageregList.get(0).getRepayintbdate());	   // 上次结费日
		lnsfeeinfo.setOpendate(manageregList.get(0).getRepayintbdate());	       // 开户日工作日期(第一条开始时间)
		lnsfeeinfo.setFeestat("CA");	       // 费用状态
		lnsfeeinfo.setFeeformula(basicInfo.get("INTPERFORMULA").toString());
		lnsfeeinfo.setProvisionflag(basicInfo.get("PROVISIONFLAG").toString());

		
		FabAmount totalTermRetFee = new FabAmount();
		for(TblFeemanagereg feeManageReg : manageregList){
			if(new FabAmount(feeManageReg.getNoretfee().doubleValue()).isPositive())
				lnsfeeinfo.setFeestat("N");	       // 费用状态
			FabAmount termRetFee = new FabAmount(feeManageReg.getTermretfee().doubleValue());
			FabAmount feeAmt = new FabAmount(feeManageReg.getFeeamt().doubleValue());
			if(feeAmt.isPositive()||CalendarUtil.afterAlsoEqual(ctx.getTranDate(), feeManageReg.getRepayintedate()))
				lnsfeeinfo.setLastfeedate(feeManageReg.getRepayintedate());	   // 上次结费日
			totalTermRetFee.selfAdd(termRetFee);

		}
		// 融担-代偿开户
		if("2412623".equals(basicInfo.get("PRDCODE").toString().trim())){
			BigDecimal feeRate = BigDecimal.valueOf(totalTermRetFee.getVal()).divide(new BigDecimal(basicInfo.get("CONTRACTAMT").toString()), 6, BigDecimal.ROUND_HALF_UP);
			insertLnsFeeInfo2412623(acctNo, lnsfeeinfo, feeRate.doubleValue());
			inserLnsBill(ctx, acctNo, basicInfo, manageregList, childBrc, feeRate.doubleValue(), "SFJZ", new FabAmount(), new FabAmount(), lnsfeeinfo);
		}
		// 个贷-外部平台贷-搭固定融担
		// 个贷-任性贷固定融担费
		if("2412626".equals(basicInfo.get("PRDCODE").toString().trim())||"2412624".equals(basicInfo.get("PRDCODE").toString().trim())){
			insertLnsFeeInfo2412626(acctNo, lnsfeeinfo, totalTermRetFee);
			insertLnsamortizeplan(ctx, acctNo, basicInfo, totalTermRetFee);

		}
		// 个贷-任性贷期缴融担费
		else if("2512623".equals(basicInfo.get("PRDCODE").toString().trim())){
			String adFlag;
			if(manageregList.get(0).getReserve1().contains(":"))
				adFlag = manageregList.get(0).getReserve1().split(":")[0];
			else
				adFlag = "1";
			Double feeRate;
			if(manageregList.get(0).getReserve1().contains(":"))
				feeRate = Double.valueOf(manageregList.get(0).getReserve1().split(":")[1]);
			else
				feeRate = Double.valueOf(manageregList.get(0).getReserve1());
			insertLnsFeeInfo2512623(acctNo, lnsfeeinfo, adFlag, feeRate, "SQFE");
			inserLnsBill(ctx, acctNo, basicInfo, manageregList, childBrc, feeRate, "SFJZ", new FabAmount(), new FabAmount(), lnsfeeinfo);

		}
			
		// 个贷-任性贷搭保费
		else if("2512617".equals(basicInfo.get("PRDCODE").toString().trim()) || "2512619".equals(basicInfo.get("PRDCODE").toString().trim())){
			String repayWay;
			Double feeRate ;
			if(!VarChecker.isEmpty(manageregList.get(0).getReserve1())){
				repayWay = "A";
				if(manageregList.get(0).getReserve1().contains(":"))
					feeRate = Double.valueOf(manageregList.get(0).getReserve1().split(":")[1]);
				else
					feeRate = Double.valueOf(manageregList.get(0).getReserve1());
			}
			else{
				repayWay = "B";
				
				Map<String, Object> paramMap = new HashMap<String, Object>();
				paramMap.put("acctNo", acctNo);
				paramMap.put("key", "BI");
				Map<String, Object> lnsinterfaceEX;
				try {
					lnsinterfaceEX = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsinterfaceex_666", paramMap);
				} catch (FabException e) {
					throw new FabException(e, "SPS103", acctNo);
				}
				if (lnsinterfaceEX == null)
				{
					throw new FabException("SPS104", acctNo);
				}
				JSONObject jsonObject = JSONObject.parseObject(lnsinterfaceEX.get("VALUE").toString());
		        String flag = jsonObject.getString("flag");
				if(!"1".equals(flag.trim()))
					throw new FabException("LNS169", "flag", flag);
				feeRate = Double.valueOf(jsonObject.getString("onetimeRate"));
			}
				
			insertLnsFeeInfo2512617(acctNo, lnsfeeinfo, repayWay, feeRate);
			inserLnsBill(ctx, acctNo, basicInfo, manageregList, childBrc, feeRate, "IFJZ", new FabAmount(), new FabAmount(), lnsfeeinfo);

		}
		// 乐业贷-房速融
		else if("2412615".equals(basicInfo.get("PRDCODE").toString().trim())){
			//一次性
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("acctNo", acctNo);
			paramMap.put("key", "GDDB");
			Map<String, Object> basicInfoEX;
			try {
				basicInfoEX = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsbasicinfoex_666", paramMap);
			} catch (FabException e) {
				throw new FabException(e, "SPS103", acctNo);
			}
			FabAmount termRetFeeOT = new FabAmount(0.00);
			FabAmount feeAmtOT = new FabAmount(0.00);
			if (basicInfoEX != null)
			{
				TblLnsfeeinfo feeInfoOneTime = new TblLnsfeeinfo();
				feeInfoOneTime.setOpenbrc(lnsfeeinfo.getOpenbrc());          // 开户机构代码
				feeInfoOneTime.setAcctno(lnsfeeinfo.getAcctno());   // 帐号
			    feeInfoOneTime.setCcy(lnsfeeinfo.getCcy());              // 币种
			    feeInfoOneTime.setFeebrc(lnsfeeinfo.getFeebrc());	           // 费用机构代码
			    feeInfoOneTime.setCalculatrule("TERM");
			   // feeInfoOneTime.setfeeformula="";	   // 还费周期公式
			    feeInfoOneTime.setFeerate(Double.valueOf(basicInfoEX.get("VALUE1").toString()));	       // 费率利率
			    feeInfoOneTime.setTrandate(lnsfeeinfo.getTrandate());	       // 交易日期
			    feeInfoOneTime.setTrantime(lnsfeeinfo.getTrantime());	       // 交易时间
			    feeInfoOneTime.setOpendate(lnsfeeinfo.getOpendate());	       // 开户日工作日期
			    feeInfoOneTime.setFeeformula(lnsfeeinfo.getFeeformula());
			    feeInfoOneTime.setProvisionflag(basicInfo.get("PROVISIONFLAG").toString());
			    
			    
			    JSONObject jsonObject = JSONObject.parseObject(basicInfoEX.get("TUNNELDATA").toString());
			    termRetFeeOT = new FabAmount(jsonObject.getDouble("termretfee"));
			    feeAmtOT = new FabAmount(jsonObject.getDouble("feeamt"));
			    if(feeAmtOT.isPositive()||CalendarUtil.afterAlsoEqual(ctx.getTranDate(), manageregList.get(0).getRepayintedate()))
			    	feeInfoOneTime.setLastfeedate(manageregList.get(0).getRepayintedate());
			    else
			    	feeInfoOneTime.setLastfeedate(manageregList.get(0).getRepayintbdate());
			    if(termRetFeeOT.sub(feeAmtOT).isZero())
			    	feeInfoOneTime.setFeestat("CA");
			    else
			    	feeInfoOneTime.setFeestat("N");
				insertLnsFeeInfo2412615(acctNo, feeInfoOneTime);
				try{
					DbAccessUtil.execute("Lnsfeeinfo.insert", feeInfoOneTime);
				}catch (FabSqlException e){
					throw new FabException(e, "SPS100", "lnsfeeinfo");
				}
				
				if(CalendarUtil.before(ctx.getTranDate(), manageregList.get(0).getRepayintedate())){
					//分期已还大于0，赋值第一期repayintedate
					if(new FabAmount(manageregList.get(0).getFeeamt().doubleValue()).sub(feeAmtOT).isPositive()){
						lnsfeeinfo.setLastfeedate(manageregList.get(0).getRepayintedate());
					}else{
						lnsfeeinfo.setLastfeedate(manageregList.get(0).getRepayintbdate());
					}
				}
				inserLnsBillOneTime(ctx, acctNo, basicInfo, manageregList, childBrc, feeInfoOneTime.getFeerate(), "OFJZ", termRetFeeOT, new FabAmount(termRetFeeOT.sub(feeAmtOT).getVal()));
				
			}
			//分期
			BigDecimal feeRate;
			String feeType = "SQFE";
			//分期
			
			if(!VarChecker.isEmpty(manageregList.get(0).getReserve1())){
				if(manageregList.get(0).getReserve1().contains(":"))
					feeRate = new BigDecimal(manageregList.get(0).getReserve1().split(":")[1]);
				else
					feeRate = new BigDecimal(manageregList.get(0).getReserve1());
				if("51230004".equals(childBrc)){
					feeType = "RMFE";
					insertLnsFeeInfo2512623(acctNo, lnsfeeinfo, "1", feeRate.doubleValue(), feeType);
					inserLnsBill(ctx, acctNo, basicInfo, manageregList, childBrc, feeRate.doubleValue(), "RFJZ", new FabAmount(), new FabAmount(), lnsfeeinfo);
				}else{
					insertLnsFeeInfo2512623(acctNo, lnsfeeinfo, "1", feeRate.doubleValue(), feeType);
					inserLnsBill(ctx, acctNo, basicInfo, manageregList, childBrc, feeRate.doubleValue(), "SFJZ", termRetFeeOT, feeAmtOT, lnsfeeinfo);
				}
			}else if("51230004".equals(childBrc)){
				feeType = "RMFE";
				feeRate = BigDecimal.valueOf(12).multiply(manageregList.get(0).getTermretfee()).divide(new BigDecimal(basicInfo.get("CONTRACTAMT").toString()), 6, BigDecimal.ROUND_HALF_UP);
				insertLnsFeeInfo2512623(acctNo, lnsfeeinfo, "1", feeRate.doubleValue(), feeType);
				inserLnsBill(ctx, acctNo, basicInfo, manageregList, childBrc, feeRate.doubleValue(), "RFJZ", new FabAmount(), new FabAmount(), lnsfeeinfo);
			}

		}

		if("RMFE".equals(lnsfeeinfo.getFeetype()))
			lnsfeeinfo.setProvisionrule("1");
		try{
			DbAccessUtil.execute("Lnsfeeinfo.insert", lnsfeeinfo);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS100", "lnsfeeinfo");
		}

	}

	public void insertLnsFeeInfo2412623(String acctNo, TblLnsfeeinfo lnsfeeinfo, Double feeRate) {	
		lnsfeeinfo.setRepayway("A");	       // 费用交取模式
		lnsfeeinfo.setFeetype("SQFE");	       // 费用账本类型
		lnsfeeinfo.setProvisionrule("0");   // 费用计息方式
	    lnsfeeinfo.setAdvancesettle("1");	   // 提前结清收取方式
	    lnsfeeinfo.setFeebase("1");         //费用计息基数
	   // private String feeformula="";	   // 还费周期公式(保持和缪总开户逻辑一致)
	    lnsfeeinfo.setProvisionflag("CLOSE");   // 计提结束标志
	    lnsfeeinfo.setFeerate(feeRate);	       // 费率利率
	}
	public void insertLnsFeeInfo2412626(String acctNo, TblLnsfeeinfo lnsfeeinfo, FabAmount totalTermRetFee) throws FabException {
		
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("acctNo", acctNo);
		paramMap.put("key", "GDDB");
		Map<String, Object> basicInfoEX;
		try {
			basicInfoEX = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsbasicinfoex_666", paramMap);
		} catch (FabException e) {
			throw new FabException(e, "SPS103", acctNo);
		}
		if (basicInfoEX == null)
		{
			throw new FabException("SPS104", acctNo);
		}
		lnsfeeinfo.setRepayway("C");	       // 费用交取模式
		lnsfeeinfo.setProvisionrule("2");   // 费用计息方式
		lnsfeeinfo.setFeeperiod(1);
		lnsfeeinfo.setFeebase("1");         //费用计息基数
	    lnsfeeinfo.setDeducetionamt(totalTermRetFee.getVal());;	   // 扣费金额
	    //private String feeformula="";	   // 还费周期公式(保持和缪总开户逻辑一致)
		lnsfeeinfo.setProvisionflag("CLOSE");   // 计提结束标志
		lnsfeeinfo.setFeerate(Double.valueOf(basicInfoEX.get("VALUE1").toString()));      // 费率利率
		lnsfeeinfo.setFeestat("CA");	       // 费用状态
	}
	public void insertLnsFeeInfo2512623(String acctNo, TblLnsfeeinfo lnsfeeinfo, String adFlag, Double feeRate, String feeType) {
		lnsfeeinfo.setRepayway("A");	       // 费用交取模式
		lnsfeeinfo.setFeetype(feeType);	       // 费用账本类型
		lnsfeeinfo.setProvisionrule("0");   // 费用计息方式
		lnsfeeinfo.setAdvancesettle(adFlag);   // 提前结清收取方式
		lnsfeeinfo.setFeebase("1");         //费用计息基数
	   //private String feeformula="";	   // 还费周期公式(保持和缪总开户逻辑一致)
		//lnsfeeinfo.setProvisionflag("RUNNING");   // 计提结束标志
		lnsfeeinfo.setFeerate(feeRate);	       // 费率利率
	}
	public void insertLnsFeeInfo2512617(String acctNo, TblLnsfeeinfo lnsfeeinfo, String repayWay, Double feeRate) {
		lnsfeeinfo.setRepayway(repayWay);	       // 费用交取模式
		lnsfeeinfo.setFeetype("ISFE");	       // 费用账本类型
		lnsfeeinfo.setProvisionrule("0");  // 费用计息方式
	    lnsfeeinfo.setAdvancesettle("0".equals(repayWay.trim())?"1":"");	   // 提前结清收取方式
		lnsfeeinfo.setFeebase("1");         //费用计息基数
	    //private String feeformula="";	   // 还费周期公式(保持和缪总开户逻辑一致)
		lnsfeeinfo.setProvisionflag("CLOSE");   // 计提结束标志
	    lnsfeeinfo.setFeerate(feeRate);	       // 费率利率
	}
	public void insertLnsFeeInfo2412615(String acctNo, TblLnsfeeinfo lnsfeeinfo) {
		lnsfeeinfo.setRepayway("B");	       // 费用交取模式
		lnsfeeinfo.setFeetype("SQFE");	       // 费用账本类型
	    lnsfeeinfo.setProvisionrule("0");   // 费用计息方式
	    lnsfeeinfo.setFeebase("1");         //费用计息基数
	    lnsfeeinfo.setFeeperiod(1);
	    //private String feeformula="";	   // 还费周期公式(保持和缪总开户逻辑一致)
	    lnsfeeinfo.setProvisionflag("RUNNING");   // 计提结束标志
	}


	public void inserLnsBill(TranCtx ctx, String acctNo, Map<String, Object> basicInfo,  List<TblFeemanagereg> manageregList, String childBrc, Double billRate, String briefCode, FabAmount termRetFeeOT, FabAmount feeOT, TblLnsfeeinfo lnsFeeInfo) throws FabException{
	    	
	    Integer i = 1;
	    for(TblFeemanagereg feeManageReg : manageregList){
	    	lnsFeeInfo.setFeeperiod(i);
	    	FabAmount termRetFee;
	    	FabAmount feeAmt;
	    	FabAmount noretFee;
	    	if(1==i){
	    		termRetFee = new FabAmount(new FabAmount(feeManageReg.getTermretfee().doubleValue()).sub(termRetFeeOT).getVal());
	    		feeAmt = new FabAmount(new FabAmount(feeManageReg.getFeeamt().doubleValue()).sub(feeOT).getVal());
	    		noretFee = new FabAmount(termRetFee.sub(feeAmt).getVal());
	    	}else{
	    		termRetFee = new FabAmount(feeManageReg.getTermretfee().doubleValue());
	    		feeAmt = new FabAmount(feeManageReg.getFeeamt().doubleValue());
	    		noretFee = new FabAmount(feeManageReg.getNoretfee().doubleValue());
	    	}
	    	if(feeAmt.isPositive()||CalendarUtil.afterAlsoEqual(ctx.getTranDate(), feeManageReg.getRepayintedate())){
	    		TblLnsbill lnsbill = new TblLnsbill();
	    		lnsbill.setTrandate(Date.valueOf(ctx.getTranDate()));
	    		lnsbill.setSerseqno(ctx.getSerSeqNo());
	    		lnsbill.setAcctno(acctNo);
	    		lnsbill.setBrc(basicInfo.get("OPENBRC").toString());
	    		if(VarChecker.asList("2412623","2512623","2412615").contains(basicInfo.get("PRDCODE").toString().trim())){
	    			if("51230004".equals(childBrc))
	    				lnsbill.setBilltype("RMFE");
	    			else 
	    				lnsbill.setBilltype("SQFE");
	    		}else if(VarChecker.asList("2512617","2512619").contains(basicInfo.get("PRDCODE").toString().trim()))
	    			lnsbill.setBilltype("ISFE");
	    		lnsbill.setTxseq(i);
	    		lnsbill.setPeriod(i);
	    		lnsbill.setBillamt(termRetFee.getVal());
	    		lnsbill.setBillbal(noretFee.getVal());
	    		lnsbill.setBegindate(feeManageReg.getRepayintbdate());
	    		lnsbill.setEnddate(feeManageReg.getRepayintedate());
	    		lnsbill.setCurenddate(feeManageReg.getRepayintedate());
	    		lnsbill.setRepayedate(feeManageReg.getRepayownedate());
	    		lnsbill.setSettledate(feeManageReg.getRepayintedate());
	    		lnsbill.setIntedate(feeManageReg.getRepayintedate());
	    		lnsbill.setStatusbdate(feeManageReg.getRepayintedate());

	    		lnsbill.setLastbal(0.00);
	    		lnsbill.setLastdate(feeManageReg.getTermretdate());
	    		lnsbill.setPrinbal(Double.valueOf(basicInfo.get("CONTRACTAMT").toString()));
	    		lnsbill.setBillrate(billRate);
	    		lnsbill.setRepayway(lnsFeeInfo.getRepayway());
	    		lnsbill.setCcy(basicInfo.get("CCY").toString());
	    		if(VarChecker.asList("2","02").contains(feeManageReg.getCurrentstat()))
	    			lnsbill.setBillstatus("O");
	    		else if(VarChecker.asList("1","01").contains(feeManageReg.getCurrentstat()))
	    			lnsbill.setBillstatus("N");
	    		lnsbill.setBillproperty("INTSET");
	    		lnsbill.setIntrecordflag("YES");
	    		lnsbill.setCancelflag("NORMAL");
	    		if(noretFee.isPositive()){
	    			lnsbill.setSettleflag("RUNNING");
	    			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, "FEEA", lnsbill.getBillstatus(), new FabCurrency(), childBrc);
	    			LnsAcctInfo lnsAcctInfoN = new LnsAcctInfo(acctNo, "FEEA", "N", new FabCurrency(), childBrc);
	    			
	    			
	    			add.operate(lnsAcctInfoN, null, new FabAmount(lnsbill.getBillbal()), null, "FYQY", ctx);
	    			List<FabAmount> amtList = new ArrayList<FabAmount>();
	    			amtList.add(TaxUtil.calcVAT(new FabAmount(lnsbill.getBillbal())));
	    			eventProvider.createEvent("FEESETLEMT", new FabAmount(lnsbill.getBillbal()), lnsAcctInfoN, null, null, briefCode, ctx, amtList, feeManageReg.getBrc());
	    			if("O".equals(lnsbill.getBillstatus().trim())){
	    				suber.operate(lnsAcctInfoN, null, new FabAmount(lnsbill.getBillbal()), null, "FYQY", ctx);
	    				add.operate(lnsAcctInfo, null, new FabAmount(lnsbill.getBillbal()), null, "FYQY", ctx);
	    				eventProvider.createEvent("FEETRNSFER", new FabAmount(lnsbill.getBillbal()), lnsAcctInfoN, lnsAcctInfo, null, briefCode.substring(0, 2)+"ZL", ctx, feeManageReg.getBrc());
	    			}
	    		}
	    		else if(noretFee.isZero())
	    			lnsbill.setSettleflag("CLOSE");
	    		i++;
	    		if(lnsbill.getBillamt()>0.005){
	    			try {
	    				DbAccessUtil.execute("Lnsbill.insert", lnsbill);
	    			} catch (FabSqlException e) {
	    				throw new FabException(e, "SPS100", "lnsbill");
	    			}
	    		}
	    	}

	    }
	}
	public void inserLnsBillOneTime(TranCtx ctx, String acctNo, Map<String, Object> basicInfo,  List<TblFeemanagereg> manageregList, String childBrc, Double billRate, String briefCode, FabAmount billAmt, FabAmount billBal) throws FabException{

		TblFeemanagereg feeManageReg = manageregList.get(0);
		TblLnsbill lnsbill = new TblLnsbill();
		lnsbill.setTrandate(Date.valueOf(ctx.getTranDate()));
		lnsbill.setSerseqno(ctx.getSerSeqNo());
		lnsbill.setAcctno(acctNo);
		lnsbill.setBrc(basicInfo.get("OPENBRC").toString());
		lnsbill.setBilltype("SQFE");
		lnsbill.setTxseq(0);
		lnsbill.setPeriod(1);
		lnsbill.setBillamt(billAmt.getVal());
		lnsbill.setBillbal(billBal.getVal());
		lnsbill.setBegindate(feeManageReg.getRepayintbdate());
		lnsbill.setEnddate(feeManageReg.getRepayintedate());
		lnsbill.setCurenddate(feeManageReg.getRepayintedate());
		lnsbill.setRepayedate(feeManageReg.getRepayownedate());
		lnsbill.setSettledate(feeManageReg.getRepayintedate());
		lnsbill.setIntedate(feeManageReg.getRepayintedate());
		lnsbill.setStatusbdate(feeManageReg.getRepayintedate());

		lnsbill.setLastbal(0.00);
		lnsbill.setLastdate(feeManageReg.getTermretdate());
		lnsbill.setPrinbal(Double.valueOf(basicInfo.get("CONTRACTAMT").toString()));
		lnsbill.setBillrate(billRate);
		lnsbill.setRepayway("B");
		lnsbill.setCcy(basicInfo.get("CCY").toString());
		if(VarChecker.asList("2","02").contains(feeManageReg.getCurrentstat()))
			lnsbill.setBillstatus("O");
		else if(VarChecker.asList("1","01").contains(feeManageReg.getCurrentstat()))
			lnsbill.setBillstatus("N");
		lnsbill.setBillproperty("INTSET");
		lnsbill.setIntrecordflag("YES");
		lnsbill.setCancelflag("NORMAL");
		if(billBal.isPositive()){
			lnsbill.setSettleflag("RUNNING");
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, "FEEA", lnsbill.getBillstatus(), new FabCurrency(), childBrc);
			LnsAcctInfo lnsAcctInfoN = new LnsAcctInfo(acctNo, "FEEA", "N", new FabCurrency(), childBrc);


			add.operate(lnsAcctInfoN, null, new FabAmount(lnsbill.getBillbal()), null, "FYQY", ctx);
			List<FabAmount> amtList = new ArrayList<FabAmount>();
			amtList.add(TaxUtil.calcVAT(new FabAmount(lnsbill.getBillbal())));
			eventProvider.createEvent("FEESETLEMT", new FabAmount(lnsbill.getBillbal()), lnsAcctInfoN, null, null, briefCode, ctx, amtList, feeManageReg.getBrc());
			if("O".equals(lnsbill.getBillstatus().trim())){
				suber.operate(lnsAcctInfoN, null, new FabAmount(lnsbill.getBillbal()), null, "FYQY", ctx);
				add.operate(lnsAcctInfo, null, new FabAmount(lnsbill.getBillbal()), null, "FYQY", ctx);
				eventProvider.createEvent("FEETRNSFER", new FabAmount(lnsbill.getBillbal()), lnsAcctInfoN, lnsAcctInfo, null, briefCode.substring(0, 2)+"ZL", ctx, feeManageReg.getBrc());
			}
		}
		else if(billBal.isZero())
			lnsbill.setSettleflag("CLOSE");
		if(lnsbill.getBillamt()>0.005){
			try {
				DbAccessUtil.execute("Lnsbill.insert", lnsbill);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS100", "lnsbill");
			}
		}
	}
	public void insertLnsamortizeplan(TranCtx ctx, String acctNo, Map<String, Object> basicInfo, FabAmount totalTermRetFee) throws FabException{
		TblLnsamortizeplan lnsamortizeplan = new TblLnsamortizeplan();
		lnsamortizeplan.setTrandate(Date.valueOf(ctx.getTranDate()));
	    lnsamortizeplan.setSerseqno(ctx.getSerSeqNo());
	    lnsamortizeplan.setBrc(basicInfo.get("OPENBRC").toString());
	    lnsamortizeplan.setAcctno(acctNo);;
	    lnsamortizeplan.setAmortizetype("2");;
	    lnsamortizeplan.setCcy(basicInfo.get("CCY").toString());;
	    lnsamortizeplan.setTaxrate(new Double(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")));;
	    lnsamortizeplan.setTotalamt(totalTermRetFee.getVal());
	    lnsamortizeplan.setAmortizeamt(0.00);;
	    lnsamortizeplan.setTotaltaxamt(TaxUtil.calcVAT(totalTermRetFee).getVal());;
	    lnsamortizeplan.setAmortizetax(0.00);;
	    lnsamortizeplan.setLastdate(basicInfo.get("BEGININTDATE").toString());;
	    lnsamortizeplan.setBegindate(basicInfo.get("BEGININTDATE").toString());;
	    lnsamortizeplan.setEnddate(basicInfo.get("CONTDUEDATE").toString());;
	    lnsamortizeplan.setPeriod(1);;
	    //lnsamortizeplan.setamortizeformula = ""
	    lnsamortizeplan.setStatus(basicInfo.get("PROVISIONFLAG").toString());;
	    //lnsamortizeplan.settimestamp = ""
	    //累积已摊销金额，税金
	    if("CA".equals(basicInfo.get("LOANSTAT").toString().trim()) && "CLOSE".equals(basicInfo.get("PROVISIONFLAG").toString().trim())){
		    lnsamortizeplan.setAmortizeamt(lnsamortizeplan.getTotalamt());
		    lnsamortizeplan.setAmortizetax(lnsamortizeplan.getTotaltaxamt());
		    lnsamortizeplan.setLastdate(basicInfo.get("MODIFYDATE").toString());
		    lnsamortizeplan.setPeriod(1);

	    }else{
			Map<String, Object> provMap = new HashMap<String, Object>();
			provMap.put("acctNo", acctNo);
			Map<String, Object> provInfo;
			try {
				provInfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsprovisionreg_666", provMap);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsprovisionreg");
			}
			if (null != provInfo && Double.valueOf(provInfo.get("TOTALINT").toString())>0.005)
			{
				lnsamortizeplan.setAmortizeamt(Double.valueOf(provInfo.get("TOTALINT").toString()));
			    lnsamortizeplan.setAmortizetax(Double.valueOf(provInfo.get("TOTALTAX").toString()));
			    lnsamortizeplan.setLastdate(provInfo.get("LASTDATE").toString());
			    lnsamortizeplan.setPeriod(Integer.valueOf(provInfo.get("PERIOD").toString()));
			}
	    	
	    }
	    try {
			DbAccessUtil.execute("Lnsamortizeplan.insert", lnsamortizeplan);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS100", "lnsamortizeplan");
		}
	}
	




	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}

	/**
	 * @param acctNo
	 *            the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
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



}
