package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;

import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;

/**
 *  〈一句话功能简述〉
 *  〈功能详细描述〉： 房抵贷融担代偿开户汇总费用
 * @Author 
 * @Date  2019-10-24
 */

@Scope("prototype")
@Repository
public class Lns254 extends WorkUnit{

	String acctNo;
	FabRate yearFeeRate;    //年费率
	String childBrc;   //子机构
	Integer periodNum; //期限数量

	String		receiptNo;//贷款账号
	String productCode; // 产品代码  2512617任性贷搭保险（平台开户）---新增   2412619个贷-外部平台贷（资产方开户）

	FabRate	 onetimeRate;//一次性费率
	String earlysettleFlag;

	

	String		exAcctno;		//代偿原小贷借据号
	String		exAcctno1;		//代偿原平台借据号
	String		exBrc;			//代偿原小贷机构
	String		exBrc1;			//代偿原平台机构
	String		flag;  			//代偿转换 
	Integer 	exPeriod; 		//代偿期数
	FabAmount 	switchFee;		//转换费用金额
	FabAmount	contractAmt;	//放款金额
	String		exinvesteeId; 	//代偿资金方
	LoanAgreement loanAgreement;
	
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		FabAmount 	totalNoFee= new FabAmount(0.00);	//汇总已转换未还总费用
//		FabAmount 	totalFee= new FabAmount(0.00);	//汇总已转换已还总费用
//		FabAmount 	totalTermFee= new FabAmount(0.00);	//汇总已转换应还总费用
		FabAmount	exPrinBal = new FabAmount(0.00);
		FabAmount	exBillRate = new FabAmount(0.00);

		

		
		//融担代偿费用交取模式为4
 		if( !"4".equals(flag) )
			throw new FabException("SPS106","费用交取模式");
		
		//代偿期数0为整笔代偿
		if( exPeriod != 0 )
			throw new FabException("SPS106","代偿期数");
		if(!Arrays.asList("2412638","2412639","2412641","2412640").contains(productCode)) {
			//代偿期数0为整笔代偿
			if (VarChecker.isEmpty(exinvesteeId) || VarChecker.isEmpty(exAcctno))
				throw new FabException("SPS107", "代偿信息");
		}
		//校验本息罚和费用账号处理
		String		checkAcctNo = exAcctno;
		String		checkAcctBrc = exBrc;
		
		String		checkFeeNo = exAcctno1;
		String		checkFeeBrc = exBrc1;


		if(  VarChecker.isEmpty(exAcctno1) )
		{
			checkFeeNo = exAcctno;
			checkFeeBrc = exinvesteeId.substring(1, 5)+"0000";
			checkAcctBrc = exinvesteeId.substring(1, 5)+"0000";
		}

		//原代偿借据号是否结清
		TblLnsbasicinfo lnsbasicinfo;
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", checkAcctNo);
		param.put("openbrc", checkAcctBrc);
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
        //融担代偿上海农商行 不需要校验 小贷
		if(!Arrays.asList("2412638","2412639","2412641","2412640").contains(productCode)){
			if (null == lnsbasicinfo){
				throw new FabException("ACC108", acctNo);
			}
			if( !"CA".equals(lnsbasicinfo.getLoanstat()))
				throw new FabException("LNS177");
		}

		
		

			
		if(  !VarChecker.isEmpty(exAcctno1) )
		{
			param.put("acctno", checkFeeNo);
			param.put("openbrc", checkFeeBrc);
			try {
				lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS103", "lnsbasicinfo");
			}
			if (null == lnsbasicinfo){
				throw new FabException("ACC108", acctNo);
			}
			if( !"CA".equals(lnsbasicinfo.getLoanstat()))
				throw new FabException("LNS177");

		}
		
			
		//取代偿原借据号转换总费用
		Double sumTranAmt = 0.00;
		Double sumFeeAmt = 0.00;
		Double sumDbwyAmt=0.00;//融担费违约金
		Map<String,Object> compenBillMap = new HashMap<>();
		
		if(  !VarChecker.isEmpty(exAcctno1) )
		{
			compenBillMap.put("acctno", checkAcctNo);
			compenBillMap.put("brc", checkAcctBrc);
			try {
				sumTranAmt = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrpyinfo_254", compenBillMap, Double.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsbill");
			}
		}
		
		compenBillMap.put("acctno", checkFeeNo);
		compenBillMap.put("brc", checkFeeBrc);
		compenBillMap.put("feetypes", LoanFeeUtils.getFeetypes());
		try {
			sumFeeAmt = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrpyinfofee_254", compenBillMap, Double.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}
		if(Arrays.asList("2412638","2412639","2412641","2412640").contains(productCode)){
			//代偿增加违约金
			compenBillMap.put("feetypes","'"+ConstantDeclare.BILLTYPE.BILLTYPE_DBWY+"'");
			try {
				sumDbwyAmt = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrpyinfofee_254", compenBillMap, Double.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsbill");
			}
		}
		if(  !VarChecker.isEmpty(exAcctno1) && !Arrays.asList("2412638","2412639","2412641","2412640").contains(productCode))
		{
			if( !contractAmt.sub(sumTranAmt).isZero() ) 
				throw new FabException("LNS211",contractAmt,sumTranAmt);
		}
		if (sumFeeAmt == null ) {
			sumFeeAmt = 0.00;
		}
		if(sumDbwyAmt==null){
			sumDbwyAmt=0.00;
		}
		//违约金和费用总额
		Double totalAmt=new FabAmount(sumFeeAmt).selfAdd(sumDbwyAmt).getVal();

		if( "YES".equals(GlobalScmConfUtil.getProperty("checkFeeAmt", "YES"))&&!switchFee.sub(totalAmt).isZero()){
			throw new FabException("LNS211",switchFee,totalAmt);
		}
        //费用和违约金总和
		totalNoFee.selfAdd(sumFeeAmt);


//		if( "NO".equals(GlobalScmConfUtil.getProperty("checkFeeAmt", "YES")) )
//		{
//			totalNoFee.selfAdd(switchFee);
////	 		totalTermFee.selfAdd(switchFee);
//
//		}
//		else
//		{
//			if( !switchFee.sub(sumFeeAmt).isZero() )
//				throw new FabException("LNS211",switchFee,sumFeeAmt);
//
//			totalNoFee.selfAdd(sumFeeAmt);
////	 		totalTermFee.selfAdd(sumFeeAmt);
//		}

// 		exPrinBal.setVal(compenbill.getPrinbal());
// 		exBillRate.setVal(compenbill.getBillrate());
// 		exBrc = compenbill.getBrc();
		 
		 
//		List<TblLnsbill> compenbillList;
//		Map<String,Object> compenBillMap = new HashMap<>();
//
//		compenBillMap.put("acctno", exAcctno);
//		compenBillMap.put("brc", exinvesteeId.substring(1, 5)+"0000");
//		try {
//			compenbillList = DbAccessUtil.queryForList("CUSTOMIZE.query_compenbill_254", compenBillMap, TblLnsbill.class);
//		} catch (FabSqlException e) {
//			throw new FabException(e, "SPS103", "lnsbill");
//		}
//
//		if (null == compenbillList) {
//			throw new FabException("ACC108",exAcctno);
//		}
//		
//        for(TblLnsbill compenbill :compenbillList){
//        	if( "COMPEN".equals(compenbill.getBillproperty()) )
//        	{
//				if("COMPEN".equals(compenbill.getSettleflag()))
//        		    totalNoFee.selfAdd( compenbill.getLastbal()  );
//        		totalTermFee.selfAdd( compenbill.getBillamt() );
//        		exPrinBal.setVal(compenbill.getPrinbal());
//        		exBillRate.setVal(compenbill.getBillrate());
//        		exBrc = compenbill.getBrc();
//        	}
//		}
		
//		totalFee.selfAdd( totalTermFee.sub(totalNoFee));

        //查询拓展表剔除固定担保费
        Map<String, Object> queryparam = new HashMap<>();
        queryparam.put("acctno", checkFeeNo);
        queryparam.put("openbrc", checkFeeBrc);
        queryparam.put("key", ConstantDeclare.KEYNAME.GDDB);

//        TblLnsbasicinfoex lnsbasicinfoex;
//
//        try {
//            lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", queryparam, TblLnsbasicinfoex.class);
//        } catch (FabSqlException e) {
//            throw new FabException("SPS100", "lnsbasicinfoex", e);
//        }

        //2020-05-28
//        if (lnsbasicinfoex != null) {
//            JSONObject tunneldata = JSONObject.parseObject(lnsbasicinfoex.getTunneldata());
//            //计算已还的固定担保费
////            totalFee.selfSub(tunneldata.getDouble("feeamt"));
//            totalTermFee.selfSub(tunneldata.getDouble("termretfee"));
//        }

//        if(totalNoFee.isZero())
//        {
//        	totalTermFee.setVal(0.00);
////        	totalFee.setVal(0.00);
//        }

        //校验费用登记簿中转换费用和接口所传转换费用是否一致
//        if( !totalNoFee.sub(switchFee.getVal()).isZero() )
//			throw new FabException("SPS106","转换费用金额");
		//查询代偿前费用信息
		param.put("acctno",checkFeeNo );
		param.put("openbrc", checkFeeBrc);
		List<TblLnsfeeinfo> lnsfeeinfos;
		try{
			lnsfeeinfos = DbAccessUtil.queryForList("Lnsfeeinfo.select", param, TblLnsfeeinfo.class);
		}catch (FabSqlException e){
			throw  new FabException(e,"SPS103","Lnsfeeinfo");
		}

		if( lnsfeeinfos.size() == 0 )
		{
			param.put("acctno",checkAcctNo );
			param.put("openbrc", checkAcctBrc);
			try{
				lnsfeeinfos = DbAccessUtil.queryForList("Lnsfeeinfo.select", param, TblLnsfeeinfo.class);
			}catch (FabSqlException e){
				throw  new FabException(e,"SPS103","Lnsfeeinfo");
			}

			if( lnsfeeinfos.size() == 0 )
				throw  new FabException("LNS219");

			lnsfeeinfos.get(0).setDeducetionamt(0.00);
		}

		TblLnsbill lnsbill = new TblLnsbill();
        lnsbill.setTrandate(Date.valueOf(ctx.getTranDate()));
        lnsbill.setSerseqno(ctx.getSerSeqNo());
        lnsbill.setTxseq(1);
        lnsbill.setAcctno(receiptNo);
        lnsbill.setBrc(ctx.getBrc());
        lnsbill.setBilltype(lnsfeeinfos.get(0).getFeetype());
        lnsbill.setPeriod(1);
        lnsbill.setBillamt(totalNoFee.getVal());
        lnsbill.setBillbal(totalNoFee.getVal());
        lnsbill.setLastbal(0.00);
        lnsbill.setLastdate("");
        lnsbill.setPrinbal(exPrinBal.getVal());
        lnsbill.setBillrate(exBillRate.getVal());
        lnsbill.setAccumulate(0.00);
        lnsbill.setBegindate(CalendarUtil.nDaysBefore(loanAgreement.getContract().getStartIntDate(),1 ).toString("yyyy-MM-dd"));
        lnsbill.setEnddate(loanAgreement.getContract().getStartIntDate());
        lnsbill.setCurenddate(loanAgreement.getContract().getStartIntDate());
        lnsbill.setRepayedate(loanAgreement.getContract().getStartIntDate());
        lnsbill.setSettledate(loanAgreement.getContract().getStartIntDate());
        lnsbill.setIntedate(loanAgreement.getContract().getStartIntDate());
        lnsbill.setRepayway(lnsfeeinfos.get(0).getRepayway());//获取第一个还款方式
        lnsbill.setCcy("01");
        lnsbill.setDertrandate(Date.valueOf("1970-01-01"));
        lnsbill.setDerserseqno(0);
        lnsbill.setDertxseq(0);
        lnsbill.setBillstatus("N");
        lnsbill.setBillproperty("INTSET");
        lnsbill.setCancelflag("NORMAL");
        lnsbill.setSettleflag("RUNNING");
        if( totalNoFee.isZero())
        	 lnsbill.setSettleflag("CLOSE");

		try{
			DbAccessUtil.execute("Lnsbill.insert", lnsbill );
		} catch(FabSqlException e) {
			LoggerUtil.error("lnsbill.insert.ERROR");
			throw new FabException(e, "SPS100", "lnsbill");
		}
        //增加违约金部分保存
		TblLnsbill dbwyBill =new TblLnsbill();
		if(new FabAmount(sumDbwyAmt).isPositive()){
			 dbwyBill=getDbwyBill(ctx,new FabAmount(sumDbwyAmt),2,"DBWY",new FabAmount(lnsbill.getBillamt()),exBillRate,lnsfeeinfos.get(0).getRepayway());
			try{
				//插入违约金账本
				DbAccessUtil.execute("Lnsbill.insert", dbwyBill );
			} catch(FabSqlException e) {
				LoggerUtil.error("lnsbill.insert.ERROR");
				throw new FabException(e, "SPS100", "lnsbill");
			}
		}


		for(TblLnsfeeinfo lnsfeeinfo : lnsfeeinfos)
		{
			lnsfeeinfo.setAcctno(receiptNo);
			lnsfeeinfo.setFeestat(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsfeeinfo.getRepayway())?"CA":"N");
			lnsfeeinfo.setLastfeedate(lnsbill.getEnddate());
			//机构号不同
			lnsfeeinfo.setOpenbrc(ctx.getBrc());
			//  代偿开户  计提标志改为close
			lnsfeeinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
			lnsfeeinfo.setReserv5("DC");
			//农商行融担代偿 费用不收违约金
			if(Arrays.asList("2412638","2412641","2412640").contains(productCode)){
				lnsfeeinfo.setOverrate(0.00);
			}else{
				if(ctx.getRequestDict("overRate")!=null){
					lnsfeeinfo.setOverrate(((FabRate)ctx.getRequestDict("overRate")).getVal().doubleValue());
				}
			}
			try {
				DbAccessUtil.execute("Lnsfeeinfo.insert", lnsfeeinfo);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS100", "TblLnsfeeinfo");
			}
		}
		
		loanAgreement.getFeeAgreement().setLnsfeeinfos(lnsfeeinfos);
		//动态表添加余额
		lnsbill.setBillamt(lnsbill.getBillbal());
		if( new FabAmount(lnsbill.getBillamt()).isPositive() )
		{
			LoanAcctChargeProvider.add(BillTransformHelper.convertToLnsBill(lnsbill), ctx, loanAgreement, ConstantDeclare.EVENT.FEESETLEMT, ConstantDeclare.BRIEFCODE.SFJZ);
	        AccountingModeChange.saveIntSetmTax(ctx, loanAgreement.getContract().getReceiptNo(), BillTransformHelper.convertToLnsBill(lnsbill));
		}
		//违约金计提
		if(new FabAmount(dbwyBill.getBillamt()).isPositive()){
			LoanAcctChargeProvider.add(BillTransformHelper.convertToLnsBill(dbwyBill), ctx, loanAgreement, ConstantDeclare.EVENT.DEFSETLEMT, ConstantDeclare.BRIEFCODE.WYJZ);
			AccountingModeChange.saveIntSetmTax(ctx, loanAgreement.getContract().getReceiptNo(), BillTransformHelper.convertToLnsBill(dbwyBill));
			provision(new FabAmount(dbwyBill.getBillamt()), dbwyBill,ctx);
		}

	}

	/**
	 * 获取违约金账本
	 * @return
	 */
	public TblLnsbill getDbwyBill(TranCtx ctx,FabAmount billAmt,int txseq,String billType,FabAmount prinBal,FabAmount billRate,String repayWay){
		TblLnsbill lnsbill = new TblLnsbill();
		lnsbill.setTrandate(Date.valueOf(ctx.getTranDate()));
		lnsbill.setSerseqno(ctx.getSerSeqNo());
		lnsbill.setTxseq(txseq);
		lnsbill.setAcctno(receiptNo);
		lnsbill.setBrc(ctx.getBrc());
		lnsbill.setBilltype(billType);
		lnsbill.setPeriod(1);
		lnsbill.setBillamt(billAmt.getVal());
		lnsbill.setBillbal(billAmt.getVal());
		lnsbill.setLastbal(0.00);
		lnsbill.setLastdate("");
		lnsbill.setPrinbal(prinBal.getVal());
		lnsbill.setBillrate(billRate.getVal());
		lnsbill.setAccumulate(0.00);
		lnsbill.setBegindate(CalendarUtil.nDaysBefore(loanAgreement.getContract().getStartIntDate(),1 ).toString("yyyy-MM-dd"));
		lnsbill.setEnddate(loanAgreement.getContract().getStartIntDate());
		lnsbill.setCurenddate(loanAgreement.getContract().getStartIntDate());
		lnsbill.setRepayedate(loanAgreement.getContract().getStartIntDate());
		lnsbill.setSettledate(loanAgreement.getContract().getStartIntDate());
		lnsbill.setIntedate(loanAgreement.getContract().getStartIntDate());
		lnsbill.setRepayway(repayWay);//上海农商行所用费用为D
		lnsbill.setCcy("01");
		lnsbill.setDertrandate(Date.valueOf(ctx.getTranDate()));
		lnsbill.setDerserseqno(ctx.getSerSeqNo());
		lnsbill.setDertxseq(1);
		lnsbill.setBillstatus("N");
		lnsbill.setBillproperty("INTSET");
		lnsbill.setCancelflag("NORMAL");
		lnsbill.setSettleflag("RUNNING");
		if( billAmt.isZero())
			lnsbill.setSettleflag("CLOSE");
		return lnsbill;
	}

	/**
	 *  统计期数
	 * @param loanAgreement 贷款数据
	 * @param listBill 试算账本list
	 * @param feeMap 合并一期本金利息账单生成还款计划入map
	 */
	private void countPeriod(LoanAgreement loanAgreement, List<LnsBill> listBill, Map<Integer, TblLnsrpyplan> feeMap) {
		for (LnsBill lnsBill:listBill)
		{
			Integer key = lnsBill.getPeriod();
			TblLnsrpyplan repayPlan = feeMap.get(key);

			//如果是空，说明是新的一期计划，清空原计划repayPlan，利息信息存入repayPlan
			//如果不是空，说明已经有了利息信息，将同期本金信息合并到repayPlan生成一整期还款计划
			if (null == repayPlan){
				repayPlan = new TblLnsrpyplan();
			}
			TblLnsbasicinfo lnsbasicinfo = new TblLnsbasicinfo();
			if(!VarChecker.isEmpty(loanAgreement.getContract().getDiscountAmt()))
				lnsbasicinfo.setDeductionamt(loanAgreement.getContract().getDiscountAmt().getVal());
			LoanRepayPlanProvider.dealRepayPlan(loanAgreement, getTranctx(), "FUTURE", lnsbasicinfo, lnsBill, repayPlan);

			//合并一期本金利息账单生成还款计划入map
			feeMap.put(key, repayPlan);
		}
	}

	//保费的事件
	private void discounts(TranCtx ctx, LoanAgreement loanAgreement, FabAmount sumFee) throws FabException {
		if(VarChecker.asList("2512617","2512619").contains(productCode)) {

			Map<String,Object> map = new HashMap<>();
			map.put("sumfee", sumFee.getVal());
			map.put("flag", flag);
			map.put("childBrc", childBrc);
			if(null !=onetimeRate)
				map.put("onetimeRate",onetimeRate.toString() );
			AccountingModeChange.saveInterfaceEx(ctx, receiptNo, ConstantDeclare.KEYNAME.BI, "投保金额",  JsonTransfer.ToJson(map));
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(receiptNo,  ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,
					new FabCurrency());
			//代客投保事件
			eventProvider.createEvent(ConstantDeclare.EVENT.BINSURANCE, sumFee, lnsAcctInfo,null, loanAgreement.getFundInvest(),
					ConstantDeclare.BRIEFCODE.DKTB, ctx ,"",childBrc);
		}
	}


//	private  TblFeemanagereg  bill2FeeMgreg(TblLnsrpyplan repayPlan, TranCtx ctx,Integer graceDays){
//		TblFeemanagereg feemanagereg = new TblFeemanagereg();
//		feemanagereg.setBrc(childBrc); //公司代码
//		feemanagereg.setAcctno(receiptNo);//账号
//		feemanagereg.setRepayterm(repayPlan.getRepayterm());  //还款期数
//
//
//		feemanagereg.setFeeamt(BigDecimal.valueOf(0.00));//已还款金额
//
//		feemanagereg.setModifydate(ctx.getTranDate()); //修改日期
//		feemanagereg.setModifytime(ctx.getTranTime()); //修改时间
//
//		feemanagereg.setRepayownbdate(repayPlan.getRepayownbdate());	//还款起日
//		feemanagereg.setRepayownedate(repayPlan.getRepayownedate());	//还款止日
//		feemanagereg.setRepayintbdate(repayPlan.getRepayintbdate());	//本期起日
//		feemanagereg.setRepayintedate(repayPlan.getRepayintedate());	//本期止日
//
//
//		feemanagereg.setDays(CalendarUtil.actualDaysBetween(repayPlan.getRepayintbdate(), repayPlan.getRepayintedate()));//天数
//		//计算宽限期到第几天
//		String transDate = CalendarUtil.nDaysAfter(feemanagereg.getRepayintedate(), graceDays).toString("yyyy-MM-dd");
//		//倒起息的会存在逾期
//		if (CalendarUtil.before(transDate, ctx.getTranDate())) {
//			feemanagereg.setCurrentstat("02");//逾期状态
//		}else{
//			feemanagereg.setCurrentstat("01");//逾期状态
//
//		}
//		feemanagereg.setSettleflag("01");//结清状态
//
//		feemanagereg.setTermretdate("");//本期还款日期
//
//		//存提前结清收费模式earlysettleFlag于费用登记簿feemanagereg的reserve1字段，拼接成1：10%---意思为提前结清收到当期，分期费率10%
//		feemanagereg.setReserve1(
//				(VarChecker.isEmpty(earlysettleFlag)?"":earlysettleFlag+":" )+(null ==yearFeeRate?"":yearFeeRate.toString()));
//		return feemanagereg;
//	}

	/**
	 *  入参校验
	 * @throws FabException  //校验
	 */
	private void checkInput() throws  FabException{
		//保费
		//产品代码为2512617  2512619，即为保费
		if(VarChecker.asList("2512617","2512619").contains(productCode)){
			//flag为1.一次性收取  yearfeeRate 为0  oneTimeRate 不为0
			if("1".equals(flag)){
				if(!rateIsPositive(onetimeRate)||rateIsPositive(yearFeeRate))
					throw new FabException("LNS168",getFlag());
			}
			//2.分期收取(不传也默认2)
			else if (VarChecker.isEmpty(flag)||"2".equals(flag)){
				if(rateIsPositive(onetimeRate)||!rateIsPositive(yearFeeRate))
					throw new FabException("LNS168","2");
//			}
//			//3.分期+一次性
//			else if("3".equals(flag)){
//				if(!rateIsPositive(onetimeRate)||!rateIsPositive(yearFeeRate))
//					throw new FabException("LNS168","分期+一次性保费时，年利率大于0，一次性利率大于0");
			}else{
				throw new FabException("LNS169","费用交取模式",getFlag());
			}

		}else {

			//flag为1.一次性收取  yearfeeRate 为0  oneTimeRate 不为0
			if("1".equals(flag)){
				if(!rateIsPositive(onetimeRate)||rateIsPositive(yearFeeRate))
					throw new FabException("LNS168",getFlag());
				if("2512623".equals(productCode))
					throw new FabException("LNS169","任性贷期缴融担费","费用交取模式");
			}
			//2.分期收取(不传也默认2)
			else if (VarChecker.isEmpty(flag)||"2".equals(flag)){
				if(rateIsPositive(onetimeRate))
					throw new FabException("LNS168","2");
			}
			//3.分期+一次性
			else if("3".equals(flag)){
				if(!rateIsPositive(onetimeRate)||!rateIsPositive(yearFeeRate))
					throw new FabException("LNS168","3");
				if("2512623".equals(productCode))
					throw new FabException("LNS169","任性贷期缴融担费","费用交取模式");
			}else{
				throw new FabException("LNS169","费用交取模式",getFlag());
			}
		}
	}
	//利率大于0.00
	private Boolean rateIsPositive(FabRate rate){
		return !VarChecker.isEmpty(rate)&&rate.getRate().compareTo(BigDecimal.valueOf(0.00))>0;
	}

	/**
	 * 登记违约金计提明细
	 * @param totalPint
	 *
	 * @throws FabException
	 */
	private void provision(FabAmount totalPint, TblLnsbill bill,TranCtx ctx) throws FabException {
		//取计提登记簿信息
		TblLnspenintprovreg penintprovisionPint = null;
		Map<String, Object> param = new HashMap<>();
		param.put("receiptno", bill.getAcctno());
		param.put("brc", ctx.getBrc());
		param.put("billtype", bill.getBilltype());
		penintprovisionPint = LoanDbUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);

		if (null == penintprovisionPint){
			penintprovisionPint = new TblLnspenintprovreg();
			penintprovisionPint.setBrc(ctx.getBrc());
			penintprovisionPint.setReceiptno(bill.getAcctno());
			penintprovisionPint.setCcy(loanAgreement.getContract().getCcy().getCcy());
			penintprovisionPint.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")));
			penintprovisionPint.setTotalinterest(BigDecimal.valueOf(0.00));
			penintprovisionPint.setTotaltax(BigDecimal.valueOf(0.00));
			penintprovisionPint.setBegindate(java.sql.Date.valueOf(bill.getBegindate()));
			penintprovisionPint.setTotallist(0);
			penintprovisionPint.setTimestamp(new java.util.Date().toString());
			penintprovisionPint.setBilltype(bill.getBilltype());
			//直接插入表中后面直接更新
			try{
				DbAccessUtil.execute("Lnspenintprovreg.insert", penintprovisionPint);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS100", "Lnspenintprovreg");
			}
		}
		//需要计提事件
		if(totalPint.sub(penintprovisionPint.getTotalinterest().doubleValue()).isPositive()){
			//插入明细
			insertDtl(penintprovisionPint, bill);
			//本次应计提的金额=罚息账单的账单金额（数据库表里面的）+试算出来的罚息账单金额-已计提出来的总金额
			List<FabAmount> amtpintList = new ArrayList<>();
			amtpintList.add(new FabAmount(TaxUtil.calcVAT(totalPint).sub(penintprovisionPint.getTotaltax().doubleValue()).getVal()));
			penintprovisionPint.setTotalinterest( BigDecimal.valueOf(totalPint.getVal()) );
			penintprovisionPint.setTotaltax( BigDecimal.valueOf(TaxUtil.calcVAT(totalPint).getVal()) );
			penintprovisionPint.setEnddate(java.sql.Date.valueOf(bill.getEnddate()));
			penintprovisionPint.setBilltype(bill.getBilltype());
			LoanDbUtil.update("Lnspenintprovreg.updateByUk", penintprovisionPint);
		}
	}

	private void insertDtl(  TblLnspenintprovreg penintprovision, TblLnsbill billDetail) throws FabException {
		TblLnspenintprovregdtl penintprovregdtl = new TblLnspenintprovregdtl();
		//登记计提明细登记簿
		penintprovregdtl.setReceiptno(acctNo);
		penintprovregdtl.setBrc(tranctx.getBrc());
		penintprovregdtl.setCcy(loanAgreement.getContract().getCcy().getCcy());
		//总表的条数
		penintprovision.setTotallist(penintprovision.getTotallist() + 1);
		//计提登记详细表
		penintprovregdtl.setTrandate(java.sql.Date.valueOf(tranctx.getTranDate()));
		penintprovregdtl.setSerseqno(tranctx.getSerSeqNo());
		penintprovregdtl.setTxnseq(0);
		penintprovregdtl.setPeriod(billDetail.getPeriod());
		penintprovregdtl.setListno(penintprovision.getTotallist());
		penintprovregdtl.setBilltype(billDetail.getBilltype());
		penintprovregdtl.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
		penintprovregdtl.setBegindate(java.sql.Date.valueOf(billDetail.getBegindate()));
		penintprovregdtl.setEnddate(java.sql.Date.valueOf(billDetail.getEnddate()));
		penintprovregdtl.setTimestamp(new java.util.Date().toString());
		penintprovregdtl.setInterest(billDetail.getBillamt());
		penintprovregdtl.setTax(TaxUtil.calcVAT(new FabAmount(penintprovregdtl.getInterest())).getVal());

		//登记详表
		if( new FabAmount(penintprovregdtl.getInterest()).isPositive() )
		{
			LoanDbUtil.insert("Lnspenintprovregdtl.insert", penintprovregdtl);
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
	 * @return the exAcctno
	 */
	public String getExAcctno() {
		return exAcctno;
	}

	/**
	 * @param exAcctno the exAcctno to set
	 */
	public void setExAcctno(String exAcctno) {
		this.exAcctno = exAcctno;
	}

	/**
	 * @return the exPeriod
	 */
	public Integer getExPeriod() {
		return exPeriod;
	}

	/**
	 * @param exPeriod the exPeriod to set
	 */
	public void setExPeriod(Integer exPeriod) {
		this.exPeriod = exPeriod;
	}

	/**
	 * @return the switchFee
	 */
	public FabAmount getSwitchFee() {
		return switchFee;
	}

	/**
	 * @param switchFee the switchFee to set
	 */
	public void setSwitchFee(FabAmount switchFee) {
		this.switchFee = switchFee;
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
	 * @return the exinvesteeId
	 */
	public String getExinvesteeId() {
		return exinvesteeId;
	}

	/**
	 * @param exinvesteeId the exinvesteeId to set
	 */
	public void setExinvesteeId(String exinvesteeId) {
		this.exinvesteeId = exinvesteeId;
	}

	/**
	 * @return the exAcctno1
	 */
	public String getExAcctno1() {
		return exAcctno1;
	}

	/**
	 * @param exAcctno1 the exAcctno1 to set
	 */
	public void setExAcctno1(String exAcctno1) {
		this.exAcctno1 = exAcctno1;
	}

	/**
	 * @return the exBrc
	 */
	public String getExBrc() {
		return exBrc;
	}

	/**
	 * @param exBrc the exBrc to set
	 */
	public void setExBrc(String exBrc) {
		this.exBrc = exBrc;
	}

	/**
	 * @return the exBrc1
	 */
	public String getExBrc1() {
		return exBrc1;
	}

	/**
	 * @param exBrc1 the exBrc1 to set
	 */
	public void setExBrc1(String exBrc1) {
		this.exBrc1 = exBrc1;
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



}
