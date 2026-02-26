package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.utils.JsonTransfer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsRentPlanCalculate;
import com.suning.fab.loan.domain.TblLnsrentreg;
import com.suning.fab.loan.domain.TblLnszlamortizeplan;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：登记摊销登记簿/租赁试算登记簿
 *
 * @author 
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype")
@Repository
public class Lns237 extends WorkUnit {


    /**最小差异*/  
    public static final double MINDIF=0.00000001;  
	
    
	String serialNo;	
	String receiptNo;	
	String startIntDate;	//起始日期
	String endDate;			//结束日期
	
	FabAmount contractAmt;	//确认金额
	FabAmount finalAmt;		//尾款本息
	FabAmount finalPrin;	//尾款本金
	FabAmount rentAmt;		//租金本息
	FabAmount rentPrin;		//租金本金
	String pkgList;
 
	List<LnsRentPlanCalculate> rentPlanpkgList = new ArrayList<LnsRentPlanCalculate>();

	
	/** 
     * @desc 使用方法参考main方法 
     * @param cashFlow  资金流 
     * @return 收益率 
     */  
    public static double getIrr(List<Double> cashFlow){  
    	/**迭代次数*/  
        int LOOPNUM=1000; 
        
        double flowOut=cashFlow.get(0);  
        double minValue=0d;  
        double maxValue=1d;  
        double testValue=0d;  
        while(LOOPNUM>0){  
            testValue=(minValue+maxValue)/2;  
            double npv=NPV(cashFlow,testValue);  
            if(Math.abs(flowOut+npv)<MINDIF){  
                break;  
            }else if(Math.abs(flowOut)>npv){  
                maxValue=testValue;  
            }else{  
                minValue=testValue;  
            }  
            LOOPNUM--;  
        }  
        return testValue;  
    }  
    
    public static double NPV(List<Double> flowInArr,double rate){  
        double npv=0;  
        for(int i=1;i<flowInArr.size();i++){  
            npv+=flowInArr.get(i)/Math.pow(1+rate, i);  
        }  
        return npv;  
    }  
    
    
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		
		//手机租赁、汽车租赁公司开户插摊销登记簿
		if( "51240000".equals(ctx.getBrc()) || "51240001".equals(ctx.getBrc()) )
		{
			FabAmount flowOut = contractAmt;  
			List<Double> flowInArr=new ArrayList<Double>();  
	        flowInArr.add(-flowOut.getVal()); 
	        for(LnsRentPlanCalculate rentList:rentPlanpkgList)
	        {
	        	flowInArr.add(rentList.getTermPrin().getVal());
	        }
	        
	        TblLnszlamortizeplan lnszlamortizeplan = new TblLnszlamortizeplan();
	        lnszlamortizeplan.setTrandate(Date.valueOf(ctx.getTranDate()));
	        lnszlamortizeplan.setSerseqno(ctx.getSerSeqNo());
	        lnszlamortizeplan.setBrc(ctx.getBrc());
	        lnszlamortizeplan.setAcctno(receiptNo);
	        lnszlamortizeplan.setCcy("01");
	        lnszlamortizeplan.setChargeamt(BigDecimal.valueOf(0.00));
	        lnszlamortizeplan.setContractamt(BigDecimal.valueOf(contractAmt.getVal()));
	        lnszlamortizeplan.setRentamt(BigDecimal.valueOf(rentAmt.getVal()));
	        lnszlamortizeplan.setMonrate(new FabRate(getIrr(flowInArr)).getVal());
	        lnszlamortizeplan.setTaxrate(BigDecimal.valueOf(0.06));
	        lnszlamortizeplan.setTotalamt(BigDecimal.valueOf(rentAmt.add(finalAmt).sub(contractAmt).getVal()));
	        lnszlamortizeplan.setAmortizeamt(BigDecimal.valueOf(0.00));
	        lnszlamortizeplan.setAmortizetax(BigDecimal.valueOf(0.00));
	        lnszlamortizeplan.setLastdate(startIntDate);
	        lnszlamortizeplan.setBegindate(startIntDate);
	        lnszlamortizeplan.setEnddate(endDate);
	        lnszlamortizeplan.setPeriod(0);
	        lnszlamortizeplan.setAmortizeformula("");
	        lnszlamortizeplan.setStatus("RUNNING");
	        
	        BigDecimal rate = lnszlamortizeplan.getTaxrate();
	        BigDecimal ratefactor = rate.add(new BigDecimal(1));
	        lnszlamortizeplan.setTotaltaxamt(BigDecimal.valueOf((lnszlamortizeplan.getTotalamt().divide(ratefactor, 9, BigDecimal.ROUND_HALF_UP).multiply(rate)).doubleValue()));
	        
	        try {
				DbAccessUtil.execute("Lnszlamortizeplan.insert", lnszlamortizeplan);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS100", "lnszlamortizeplan");
			}
		}

        
        //登记租赁试算登记簿
		String rsqMap="finalAmt:"+finalAmt+"|finalPrin:"+finalPrin+"|rentAmt:"+rentAmt+""+"|rentPrin:"+rentPrin;
		String tailAmt =""+(VarChecker.isEmpty(ctx.getRequestDict("tailAmt"))?"":ctx.getRequestDict("tailAmt"));
		String merchantCode =""+(VarChecker.isEmpty(ctx.getRequestDict("merchantCode"))?"":ctx.getRequestDict("merchantCode"));
		String downPayment =""+(VarChecker.isEmpty(ctx.getRequestDict("downPayment"))?"":ctx.getRequestDict("downPayment"));
		String reqMap="contractAmt:"+ctx.getRequestDict("contractAmt")+
					  "|startIntDate:"+ctx.getRequestDict("startIntDate")+
				  	  "|endDate:"+ctx.getRequestDict("endDate")+
					  "|repayDate:"+ctx.getRequestDict("repayDate")+
					  "|recoveryRate:"+ctx.getRequestDict("recoveryRate")+
					  "|finalRate:"+ctx.getRequestDict("finalRate")+
					  "|rentRate:"+ctx.getRequestDict("rentRate")+
				      "|floatRate:"+ctx.getRequestDict("floatRate")+
				      "|tailAmt:"+tailAmt+
				      "|merchantCode:"+merchantCode+
				      "|downPayment:"+downPayment;
		TblLnsrentreg lnsrentreg = new TblLnsrentreg();
		lnsrentreg.setTrandate(ctx.getTranDate());
		lnsrentreg.setSerialno(serialNo);
		lnsrentreg.setAccdate(ctx.getTranDate());
		lnsrentreg.setSerseqno(ctx.getSerSeqNo());
		lnsrentreg.setAcctno(receiptNo);
		lnsrentreg.setBrc(ctx.getBrc());
		lnsrentreg.setReqmap(reqMap);
		lnsrentreg.setRsqmap(rsqMap);

		try {
			DbAccessUtil.execute("Lnsrentreg.insert", lnsrentreg);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS100", "lnsrentreg");
		}
		Map<String,String> stringJson = new HashMap<>();
		stringJson.put("tailAmt", tailAmt);//留购价
		stringJson.put("downPayment", downPayment); //首付款
		stringJson.put("contractAmt",ctx.getRequestDict("contractAmt").toString());//融资额
		//存储开户时的多渠道数据   存储在json格式的value中
		AccountingModeChange.saveInterfaceEx(ctx, receiptNo, ConstantDeclare.KEYNAME.ZL, "租赁", JsonTransfer.ToJson(stringJson));

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
	 * @return the finalAmt
	 */
	public FabAmount getFinalAmt() {
		return finalAmt;
	}


	/**
	 * @param finalAmt the finalAmt to set
	 */
	public void setFinalAmt(FabAmount finalAmt) {
		this.finalAmt = finalAmt;
	}


	/**
	 * @return the finalPrin
	 */
	public FabAmount getFinalPrin() {
		return finalPrin;
	}


	/**
	 * @param finalPrin the finalPrin to set
	 */
	public void setFinalPrin(FabAmount finalPrin) {
		this.finalPrin = finalPrin;
	}


	/**
	 * @return the rentAmt
	 */
	public FabAmount getRentAmt() {
		return rentAmt;
	}


	/**
	 * @param rentAmt the rentAmt to set
	 */
	public void setRentAmt(FabAmount rentAmt) {
		this.rentAmt = rentAmt;
	}


	/**
	 * @return the rentPrin
	 */
	public FabAmount getRentPrin() {
		return rentPrin;
	}


	/**
	 * @param rentPrin the rentPrin to set
	 */
	public void setRentPrin(FabAmount rentPrin) {
		this.rentPrin = rentPrin;
	}


	/**
	 * @return the pkgList
	 */
	public String getPkgList() {
		return pkgList;
	}


	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(String pkgList) {
		this.pkgList = pkgList;
	}


	/**
	 * @return the rentPlanpkgList
	 */
	public List<LnsRentPlanCalculate> getRentPlanpkgList() {
		return rentPlanpkgList;
	}


	/**
	 * @param rentPlanpkgList the rentPlanpkgList to set
	 */
	public void setRentPlanpkgList(List<LnsRentPlanCalculate> rentPlanpkgList) {
		this.rentPlanpkgList = rentPlanpkgList;
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

}
