package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.suning.fab.loan.domain.TblLnsaccountdyninfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBasicInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RpyList;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.BillTransformHelper;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanBasicInfoProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.loan.utils.LoanRepayPlanProvider;
import com.suning.fab.loan.utils.LoanRpyInfoUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：债权转让
 *
 * @Author 
 * @Date Created in 16:57 2020/6/24
 * @see
 */
@Scope("prototype")
@Repository
public class Lns261 extends WorkUnit {

    String 		repayChannel; 	//还款方式 F-债权转让
    String 		memo;			//ZQZR-债权转让
    String 		compensateFlag; //代偿标志 4-债权转让
    String 		acctNo; 		//贷款账号
    FabAmount 	repayAmt;		//还款金额
    String 		serialNo; 		// 幂等流水号
    
    
    
    
   
    String repayAcctNo;
    String ccy;
    String cashFlag;
    String termDate;
    String tranCode;
    String bankSubject; // 帐速融 银行直接还款科目
    FabAmount feeAmt;
    FabAmount refundAmt;
    String brc;
    Map<String, FabAmount> repayAmtMap;
    Integer subNo;
    String outSerialNo;
    String settleFlag;
    String platformId; //平台方代码
    ListMap pkgList;//债务公司明细
    ListMap pkgList1;//资金方明细
	String  repayList;  //还款明细2020-06-08


    LnsBillStatistics billStatistics;
    FabAmount prinAmt;
    FabAmount nintAmt;
    FabAmount dintAmt;
    String endFlag;
    String priorType;
    String prdId;
    String custType;
    FabRate cleanFeeRate;
    FabAmount reduceIntAmt;             //利息减免金额
    FabAmount reduceFintAmt;            //罚息减免金额
    FabAmount nintRed;                  //总罚息减免金额
    FabAmount fnintRed;                 //总利息减免金额

    String realDate;                    //实际扣款日
    String switchloanType;				//借新还旧类型 1-房抵贷债务重组  2-任性付账单分期  3-任性付最低还款额
    
    private LnsBillStatistics lnsBillStatistics;
    @Autowired
    LoanEventOperateProvider eventProvider;
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;
    @Override
    public void run() throws Exception {
    	TranCtx ctx = getTranctx();
        Map<String, FabAmount> repayAmtMap = new HashMap<String, FabAmount>();
    	LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
    	
    	//条件限制
    	if( !ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION.equals(la.getContract().getLoanStat()) )
    		throw new FabException("LNS117", acctNo);
    	if( !"ZQZR".equals(memo) )
    		throw new FabException("SPS106", "memo");
    	if( !"F".equals(repayChannel) )
    		throw new FabException("SPS106", "repayChannel");
    	if( !repayAmt.isZero() )
    		throw new FabException("SPS106", "repayAmt");
    	
    	
    	// 幂等登记薄
        TblLnsinterface lnsinterface = new TblLnsinterface();
        lnsinterface.setTrandate(ctx.getTermDate());
        lnsinterface.setSerialno(serialNo);
        lnsinterface.setAccdate(ctx.getTranDate());
        lnsinterface.setSerseqno(ctx.getSerSeqNo());
        lnsinterface.setTrancode(ctx.getTranCode());
        if (!VarChecker.isEmpty(repayAcctNo))
            lnsinterface.setUserno(repayAcctNo);
        lnsinterface.setAcctno(getAcctNo());
        lnsinterface.setBrc(ctx.getBrc());
        lnsinterface.setBankno(outSerialNo);
        lnsinterface.setTranamt(0.00);
        lnsinterface.setSumrint(0.00);
        lnsinterface.setSumramt(0.00);
        lnsinterface.setSumrfint(0.00);
        lnsinterface.setSumdelfint(0.00);
        lnsinterface.setSumdelint(0.00);

        if (!VarChecker.isEmpty(repayChannel))
            lnsinterface.setReserv5(repayChannel);
        if (!VarChecker.isEmpty(bankSubject))
            lnsinterface.setBillno(bankSubject);

        try {
            DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
        } catch (FabSqlException e) {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("serialno", serialNo);
                params.put("trancode", ctx.getTranCode());
                try {
                    lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", params,
                            TblLnsinterface.class);
                } catch (FabSqlException f) {
                    throw new FabException(f, "SPS103", "lnsinterface");
                }
                dintAmt = new FabAmount(lnsinterface.getSumrfint());
                nintAmt = new FabAmount(lnsinterface.getSumrint());
                prinAmt = new FabAmount(lnsinterface.getSumramt());
                endFlag = lnsinterface.acctFlag2Endflag();
                
                throw new FabException(e, TRAN.IDEMPOTENCY);
            } else
                throw new FabException(e, "SPS100", "lnsinterface");
        }
        
        if (outSerialNo != null)
            la.getFundInvest().setOutSerialNo(outSerialNo);
        if (bankSubject != null)
        	la.getFundInvest().setFundChannel(bankSubject);
        if (getRepayChannel() != null)
        	la.getFundInvest().setChannelType(getRepayChannel());
        
        
        
        setPrdId(la.getPrdId());
        setCustType(la.getCustomer().getCustomType());
        
        
        
        //读取贷款账单
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", getAcctNo());
        param.put("brc", ctx.getBrc());
        List<TblLnsbill> billList = new ArrayList<TblLnsbill>();
        try {
            billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_repay", param, TblLnsbill.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbill");
        }
        
        //筛选账本  非费用账本
        billList = LoanFeeUtils.filtFee(billList);
        
        Map<String, Object> upbillmap = new HashMap<String, Object>();
        
        for (int i = 0; i < billList.size(); i++) {
        	TblLnsbill lnsbill = billList.get(i);
        	
        	 upbillmap.put("actrandate", ctx.getTranDate());
             upbillmap.put("tranamt", lnsbill.getBillbal());

             upbillmap.put("trandate", lnsbill.getTrandate());
             upbillmap.put("serseqno", lnsbill.getSerseqno());
             upbillmap.put("txseq", lnsbill.getTxseq());

             Map<String, Object> repaymap;
             try {
            	 upbillmap.put("billproperty", ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL);
                 repaymap = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsbill_dcrepay", upbillmap);
             } catch (FabSqlException e) {
                 throw new FabException(e, "SPS103", "lnsbill");
             }
             if (repaymap == null) {
                 throw new FabException("SPS104", "lnsbill");
             }
             
             Double minAmt = Double.parseDouble(repaymap.get("minamt").toString());
             FabAmount amount = new FabAmount(minAmt);
             //债权转让 罚息 复利进行合并
			if (lnsbill.getBilltype().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
					|| lnsbill.getBilltype().contains(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
					|| lnsbill.getBilltype().contains(ConstantDeclare.BILLTYPE.BILLTYPE_GINT)) {
				lnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
			}
             LoanRpyInfoUtil.addRpyInfo(repayAmtMap, lnsbill.getBilltype(), ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, amount);
             
        }

        //获取账户动态表余额
		Map<String,FabAmount> dyninfoMap=getBasicDynInfoByAcctStat(getAcctNo(),ctx.getBrc());
		for (Map.Entry<String, FabAmount> entry : dyninfoMap.entrySet()) {
        	FabAmount amount = new FabAmount();
        	String billType="";
        	amount.selfAdd(entry.getValue());
        	
            if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)){
            	//本金单独获取金额
            	billType=ConstantDeclare.BILLTYPE.BILLTYPE_PRIN;
//            	amount=repayAmtMap.get(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN+"."+ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
            }
                
            if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)){
            	billType=ConstantDeclare.BILLTYPE.BILLTYPE_NINT;
            }
            if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
                    || entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
                    || entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_GINT)) {
            	billType=ConstantDeclare.BILLTYPE.BILLTYPE_DINT;
            }

            LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), billType, ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
                    new FabCurrency());
            lnsAcctInfo.setMerchantNo(getRepayAcctNo());
            lnsAcctInfo.setCustType(la.getCustomer().getCustomType());
            lnsAcctInfo.setCustName(la.getCustomer().getCustomName());
            lnsAcctInfo.setPrdCode(la.getPrdId());
            if( amount.isPositive())
            {
           	 	//add.operate(lnsAcctInfo, null, amount, la.getFundInvest(), ConstantDeclare.BRIEFCODE.HXKH, ctx);
                eventProvider.createEvent(ConstantDeclare.EVENT.LNCNFINOFF, amount, lnsAcctInfo, lnsAcctInfo,
                        la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZQZR, ctx);
                
                sub.operate(lnsAcctInfo, null, amount, la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZQZR, ctx);
//                eventProvider.createEvent(ConstantDeclare.EVENT.LNTRANSCRE, amount, lnsAcctInfo, lnsAcctInfo,
//                        la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZQZR, ctx);
            }
        }

        
       
        LnsBasicInfo basicinfo = new LnsBasicInfo();
        if( la.getContract().getBalance().isPositive() )
        {
        	TblLnsbill lnsPrinBill = new TblLnsbill();
            String endDate = CalendarUtil.after(ctx.getTranDate(), la.getContract().getContractEndDate())?la.getContract().getContractEndDate():ctx.getTranDate();
    		//账单金额为合同余额
    		lnsPrinBill.setBillamt( la.getContract().getBalance().getVal() );
    		//主文件状态结清
    		//basicinfo.setLoanStat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
    		//结清标志3-结清
    		settleFlag = "3";
    		//还款计划登记簿登记未来期所有计划
    		LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"ADVANCE");
    		
    		
    		//本金账单赋值
    		//账单余额0
    		lnsPrinBill.setBillbal( 0.00 );
    		//账务日期
    		lnsPrinBill.setTrandate(Date.valueOf(ctx.getTranDate()));			
    		//流水号
    		lnsPrinBill.setSerseqno(ctx.getSerSeqNo());							
    		//子序号
    		lnsPrinBill.setTxseq(1);										
    		//账号
    		lnsPrinBill.setAcctno(la.getContract().getReceiptNo());				
    		//机构
    		lnsPrinBill.setBrc(ctx.getBrc());
    		//账单类型（本金）
    		lnsPrinBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN); 	
    		//账单金额
    		lnsPrinBill.setBillamt(lnsPrinBill.getBillamt()); 					
    		//账单余额
    		lnsPrinBill.setBillbal(lnsPrinBill.getBillbal()); 					
    		//上日余额
    		//lnsPrinBill.setLastbal(0.00);  									
    		//上笔日期
    		lnsPrinBill.setLastdate(ctx.getTranDate());						 	
    		//账单对应剩余本金金额（合同余额-账单金额）
    		lnsPrinBill.setPrinbal(la.getContract().getBalance().sub(new FabAmount(lnsPrinBill.getBillamt())).getVal());
    		//账单利率（正常利率）
    		lnsPrinBill.setBillrate(0.00);  									
    		//首次还款账单开始日为开户日
    		if( la.getContract().getRepayPrinDate().isEmpty() )
    		{
    			lnsPrinBill.setBegindate(la.getContract().getContractStartDate());
    		}
    		//非首次账单开始日为上次结本日
    		else
    		{
    			lnsPrinBill.setBegindate(la.getContract().getRepayPrinDate()); 		
    		}
    		
    		//账单结束日期
    		lnsPrinBill.setEnddate(endDate); 							
    		//期数（利息期数）
    		lnsPrinBill.setPeriod(la.getContract().getCurrPrinPeriod());     							
    		//账单应还款日期
    		lnsPrinBill.setRepayedate(endDate);    					
    		//账单结清日期
    		lnsPrinBill.setSettledate(endDate);    					
    		//利息记至日期
    		lnsPrinBill.setIntedate(endDate);
    		//当期到期日
    		lnsPrinBill.setCurenddate(endDate);
    		//还款方式
    		lnsPrinBill.setRepayway(la.getWithdrawAgreement().getRepayWay());   
    		//币种
    		lnsPrinBill.setCcy(la.getContract().getCcy().getCcy());
    		//账单状态（正常）
    		lnsPrinBill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
    		//账单状态开始日期
    		lnsPrinBill.setStatusbdate(endDate);  		 			
    		//账单属性(还款)
    		lnsPrinBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);	
    		if( VarChecker.asList("2","3","4").contains(compensateFlag) )
    			lnsPrinBill.setBillproperty("2".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN : ("3".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH : ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL));	//账单属性(还款)
    		//利息入账标志
    		lnsPrinBill.setIntrecordflag("YES");   								
    		//账单是否作废标志 
    		lnsPrinBill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);  	
    		if( new FabAmount(lnsPrinBill.getBillbal()).isZero() )
    		{
    			//账单结清标志（结清）
    			lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);	
    		}
    		else
    		{
    			//账单结清标志（未结清）
    			lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING); 	
    		}
    		
    		//本金账单金额大于0
    		if( lnsPrinBill.getBillamt() > 0.00 )
    		{
    			//插入账单表
    			try {
    				DbAccessUtil.execute("Lnsbill.insert", lnsPrinBill);
    			} catch (FabSqlException e) {
    				throw new FabException(e, "SPS100", "lnsbill");
    			}
    			

    			//修改主文件上次结本日
    			basicinfo.setLastPrinDate(endDate);
    			//修改主文件合同余额
    			basicinfo.setTranAmt(new FabAmount(lnsPrinBill.getBillamt()));
    			
    			
    			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), lnsPrinBill.getBilltype(), ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
    	                new FabCurrency());
    	        lnsAcctInfo.setMerchantNo(getRepayAcctNo());
    	        lnsAcctInfo.setCustType(la.getCustomer().getCustomType());
    	        lnsAcctInfo.setCustName(la.getCustomer().getCustomName());
    	        lnsAcctInfo.setPrdCode(la.getPrdId());
//    	        if( new FabAmount(lnsPrinBill.getBillamt()).isPositive())
//    	        {
////    	        	add.operate(lnsAcctInfo, null, new FabAmount(lnsPrinBill.getBillamt()), la.getFundInvest(), ConstantDeclare.BRIEFCODE.HXKH,
////    	    				lnsPrinBill.getTrandate().toString(), lnsPrinBill.getSerseqno(), lnsPrinBill.getTxseq(), ctx);
//    	    		eventProvider.createEvent(ConstantDeclare.EVENT.LNCNFINOFF, new FabAmount(lnsPrinBill.getBillamt()), lnsAcctInfo, lnsAcctInfo,
//    	                    la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZQZR, ctx);
//
//    	    		sub.operate(lnsAcctInfo, null, new FabAmount(lnsPrinBill.getBillamt()), la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZQZR,
//    	    				lnsPrinBill.getTrandate().toString(), lnsPrinBill.getSerseqno(), lnsPrinBill.getTxseq(), ctx);
////    	    		eventProvider.createEvent(ConstantDeclare.EVENT.LNTRANSCRE, new FabAmount(lnsPrinBill.getBillamt()), lnsAcctInfo, lnsAcctInfo,
////    	    	                la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZQZR, ctx);
//    	        }
    		}
        	
        }
        
		
		
		
		
		
		
		
       
        
        basicinfo.setLoanStat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
        basicinfo.setTranAmt(la.getContract().getBalance());
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);
    }


	/**
	 * 根据统计的账户类型 查询是否产生了C账户
	 * @param acctno 借据号
	 * @param brc 机构号
	 * @param tzhxMap
	 * @return
	 */
	public Map getBasicDynInfoByAcctStat(String acctno,String brc) throws FabException{
		String[] bills=new String []{ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.BILLTYPE.BILLTYPE_DINT};
		Map<String,FabAmount> acctResult=new HashMap();
		Map param =new HashMap();
		StringBuffer stringBuffer =new StringBuffer();
		for(String temp:bills){
			stringBuffer.append("'"+temp+".C',");
		}
		if(stringBuffer.length()>0){
			//查询对应的C账户是否金额
			param.put("acctno",acctno);
			param.put("profitbrc",brc);
			param.put("acctstat",stringBuffer.substring(0,stringBuffer.length()-1));
			List<TblLnsaccountdyninfo> accountList=DbAccessUtil.queryForList("Lnsaccountdyninfo.queryByAcctNo", param, TblLnsaccountdyninfo.class);
			for(TblLnsaccountdyninfo accountDynInfo:accountList){
				acctResult.put(accountDynInfo.getAcctstat(),new FabAmount(accountDynInfo.getCurrbal()));
			}
		}
		return acctResult;
	}
   



    /**
     * Gets the value of repayChannel.
     *
     * @return the value of repayChannel
     */
    public String getRepayChannel() {
        return repayChannel;
    }

    /**
     * Sets the repayChannel.
     *
     * @param repayChannel repayChannel
     */
    public void setRepayChannel(String repayChannel) {
        this.repayChannel = repayChannel;

    }



    /**
     * Gets the value of lnsBillStatistics.
     *
     * @return the value of lnsBillStatistics
     */
    public LnsBillStatistics getLnsBillStatistics() {
        return lnsBillStatistics;
    }

    /**
     * Sets the lnsBillStatistics.
     *
     * @param lnsBillStatistics lnsBillStatistics
     */
    public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
        this.lnsBillStatistics = lnsBillStatistics;

    }





	/**
	 * @return the memo
	 */
	public String getMemo() {
		return memo;
	}





	/**
	 * @param memo the memo to set
	 */
	public void setMemo(String memo) {
		this.memo = memo;
	}





	/**
	 * @return the compensateFlag
	 */
	public String getCompensateFlag() {
		return compensateFlag;
	}





	/**
	 * @param compensateFlag the compensateFlag to set
	 */
	public void setCompensateFlag(String compensateFlag) {
		this.compensateFlag = compensateFlag;
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
	 * @return the sub
	 */
	public AccountOperator getSub() {
		return sub;
	}





	/**
	 * @param sub the sub to set
	 */
	public void setSub(AccountOperator sub) {
		this.sub = sub;
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
	 * @return the repayAmt
	 */
	public FabAmount getRepayAmt() {
		return repayAmt;
	}





	/**
	 * @param repayAmt the repayAmt to set
	 */
	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
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
	 * @return the repayAcctNo
	 */
	public String getRepayAcctNo() {
		return repayAcctNo;
	}





	/**
	 * @param repayAcctNo the repayAcctNo to set
	 */
	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
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
	 * @return the bankSubject
	 */
	public String getBankSubject() {
		return bankSubject;
	}





	/**
	 * @param bankSubject the bankSubject to set
	 */
	public void setBankSubject(String bankSubject) {
		this.bankSubject = bankSubject;
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
	 * @return the refundAmt
	 */
	public FabAmount getRefundAmt() {
		return refundAmt;
	}





	/**
	 * @param refundAmt the refundAmt to set
	 */
	public void setRefundAmt(FabAmount refundAmt) {
		this.refundAmt = refundAmt;
	}





	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}





	/**
	 * @param brc the brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
	}





	/**
	 * @return the repayAmtMap
	 */
	public Map<String, FabAmount> getRepayAmtMap() {
		return repayAmtMap;
	}





	/**
	 * @param repayAmtMap the repayAmtMap to set
	 */
	public void setRepayAmtMap(Map<String, FabAmount> repayAmtMap) {
		this.repayAmtMap = repayAmtMap;
	}





	/**
	 * @return the subNo
	 */
	public Integer getSubNo() {
		return subNo;
	}





	/**
	 * @param subNo the subNo to set
	 */
	public void setSubNo(Integer subNo) {
		this.subNo = subNo;
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





	/**
	 * @return the platformId
	 */
	public String getPlatformId() {
		return platformId;
	}





	/**
	 * @param platformId the platformId to set
	 */
	public void setPlatformId(String platformId) {
		this.platformId = platformId;
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
	 * @return the pkgList1
	 */
	public ListMap getPkgList1() {
		return pkgList1;
	}





	/**
	 * @param pkgList1 the pkgList1 to set
	 */
	public void setPkgList1(ListMap pkgList1) {
		this.pkgList1 = pkgList1;
	}





	/**
	 * @return the repayList
	 */
	public String getRepayList() {
		return repayList;
	}





	/**
	 * @param repayList the repayList to set
	 */
	public void setRepayList(String repayList) {
		this.repayList = repayList;
	}





	/**
	 * @return the billStatistics
	 */
	public LnsBillStatistics getBillStatistics() {
		return billStatistics;
	}





	/**
	 * @param billStatistics the billStatistics to set
	 */
	public void setBillStatistics(LnsBillStatistics billStatistics) {
		this.billStatistics = billStatistics;
	}





	/**
	 * @return the prinAmt
	 */
	public FabAmount getPrinAmt() {
		return prinAmt;
	}





	/**
	 * @param prinAmt the prinAmt to set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}





	/**
	 * @return the nintAmt
	 */
	public FabAmount getNintAmt() {
		return nintAmt;
	}





	/**
	 * @param nintAmt the nintAmt to set
	 */
	public void setNintAmt(FabAmount nintAmt) {
		this.nintAmt = nintAmt;
	}





	/**
	 * @return the dintAmt
	 */
	public FabAmount getDintAmt() {
		return dintAmt;
	}





	/**
	 * @param dintAmt the dintAmt to set
	 */
	public void setDintAmt(FabAmount dintAmt) {
		this.dintAmt = dintAmt;
	}





	/**
	 * @return the endFlag
	 */
	public String getEndFlag() {
		return endFlag;
	}





	/**
	 * @param endFlag the endFlag to set
	 */
	public void setEndFlag(String endFlag) {
		this.endFlag = endFlag;
	}





	/**
	 * @return the priorType
	 */
	public String getPriorType() {
		return priorType;
	}





	/**
	 * @param priorType the priorType to set
	 */
	public void setPriorType(String priorType) {
		this.priorType = priorType;
	}





	/**
	 * @return the prdId
	 */
	public String getPrdId() {
		return prdId;
	}





	/**
	 * @param prdId the prdId to set
	 */
	public void setPrdId(String prdId) {
		this.prdId = prdId;
	}





	/**
	 * @return the custType
	 */
	public String getCustType() {
		return custType;
	}





	/**
	 * @param custType the custType to set
	 */
	public void setCustType(String custType) {
		this.custType = custType;
	}





	/**
	 * @return the cleanFeeRate
	 */
	public FabRate getCleanFeeRate() {
		return cleanFeeRate;
	}





	/**
	 * @param cleanFeeRate the cleanFeeRate to set
	 */
	public void setCleanFeeRate(FabRate cleanFeeRate) {
		this.cleanFeeRate = cleanFeeRate;
	}





	/**
	 * @return the reduceIntAmt
	 */
	public FabAmount getReduceIntAmt() {
		return reduceIntAmt;
	}





	/**
	 * @param reduceIntAmt the reduceIntAmt to set
	 */
	public void setReduceIntAmt(FabAmount reduceIntAmt) {
		this.reduceIntAmt = reduceIntAmt;
	}





	/**
	 * @return the reduceFintAmt
	 */
	public FabAmount getReduceFintAmt() {
		return reduceFintAmt;
	}





	/**
	 * @param reduceFintAmt the reduceFintAmt to set
	 */
	public void setReduceFintAmt(FabAmount reduceFintAmt) {
		this.reduceFintAmt = reduceFintAmt;
	}





	/**
	 * @return the nintRed
	 */
	public FabAmount getNintRed() {
		return nintRed;
	}





	/**
	 * @param nintRed the nintRed to set
	 */
	public void setNintRed(FabAmount nintRed) {
		this.nintRed = nintRed;
	}





	/**
	 * @return the fnintRed
	 */
	public FabAmount getFnintRed() {
		return fnintRed;
	}





	/**
	 * @param fnintRed the fnintRed to set
	 */
	public void setFnintRed(FabAmount fnintRed) {
		this.fnintRed = fnintRed;
	}





	/**
	 * @return the realDate
	 */
	public String getRealDate() {
		return realDate;
	}





	/**
	 * @param realDate the realDate to set
	 */
	public void setRealDate(String realDate) {
		this.realDate = realDate;
	}





	/**
	 * @return the switchloanType
	 */
	public String getSwitchloanType() {
		return switchloanType;
	}





	/**
	 * @param switchloanType the switchloanType to set
	 */
	public void setSwitchloanType(String switchloanType) {
		this.switchloanType = switchloanType;
	}

}
