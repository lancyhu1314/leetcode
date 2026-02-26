package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.account.LnsAmortizeplan;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：赔付户（赔付时开户并充值）/确认机构
 *
 * @Author 18049705 MYP
 * @Date Created in 11:33 2020/3/13
 * @see
 */
@Scope("prototype")
@Repository
public class Lns256  extends WorkUnit {

    String  repayAcctNo;//    贷款账号（平台借据号）
    String  channelType; //9-赔付开户（光大人保-数科赔付）10-融担赔付（光大人保-融担赔付）
    FabAmount amt;
    String outSerialNo;
    String fundChannel;
    String switchBrc;//转换后机构号
    String dealType;//1-充值（默认）2-转换充值机构
    String  receiptNo ;
    String depositType;
    @Autowired
    LoanEventOperateProvider eventProvider;

    @Override
    public void run() throws Exception {
        //借据号和预收账户一致
        if(VarChecker.isEmpty(repayAcctNo))
            repayAcctNo = receiptNo;

        //写幂等登记薄
        TblLnsinterface lnsinterface = new TblLnsinterface();
        //账务日期
        lnsinterface.setTrandate(tranctx.getTermDate());
        //幂等流水号
        lnsinterface.setSerialno(tranctx.getSerialNo());
        //交易码
        lnsinterface.setTrancode(tranctx.getTranCode());
        //自然日期
        lnsinterface.setAccdate(tranctx.getTranDate());
        //系统流水号
        lnsinterface.setSerseqno(tranctx.getSerSeqNo());
        //网点
        lnsinterface.setBrc(tranctx.getBrc());
        //预收账号
        lnsinterface.setUserno(repayAcctNo);
        //开户金额
        lnsinterface.setTranamt(amt.getVal());
        //时间戳
        lnsinterface.setTimestamp(tranctx.getTranTime());
        //银行流水号/易付宝单号/POS单号
        lnsinterface.setBankno(outSerialNo);
        //借据号
        lnsinterface.setMagacct(repayAcctNo);

        lnsinterface.setAcctno(repayAcctNo);
        lnsinterface.setReserv1(dealType);
        lnsinterface.setReserv2(depositType);
        lnsinterface.setReserv3(switchBrc);
        lnsinterface.setReserv5(channelType);
        lnsinterface.setBillno(fundChannel);
        try {
            DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
        }
        catch (FabSqlException e)
        {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
            } else {
                throw new FabException(e, "SPS100", "lnsinterface");
            }
        }

        //转换机构
        if(!VarChecker.isEmpty(dealType)&&"2".equals(dealType)){

            switchAccBrc();


        }else{
            //预收户充值
            recharge();

        }


        Map<String,Object> dyParam = new HashMap<String,Object>();
		dyParam.put("acctno", repayAcctNo);
		dyParam.put("brc", tranctx.getBrc());
		dyParam.put("trandate", tranctx.getTranDate());
		//更新动态表修改时间
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_dyninfo_603", dyParam);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsaccountdyninfo");
		}
    }

    private void recharge() throws FabException {
        //查询预收户
        Map<String,Object> param = new HashMap<>();
        param.put("brc", tranctx.getBrc());
        param.put("acctno", repayAcctNo);
        //2-赔付户（赔付时开户并充值）
        param.put("feetype",ConstantDeclare.ASSISTACCOUNT.PAYACCT);
        TblLnsassistdyninfo lnsassistdyninfo =  DbAccessUtil.queryForObject("Lnsassistdyninfo.selectByUk", param,TblLnsassistdyninfo.class);

        //新建预收户
        if(lnsassistdyninfo == null){
            //预收登记簿
        	lnsassistdyninfo = new TblLnsassistdyninfo();
        	lnsassistdyninfo.setAcctno(repayAcctNo);
        	lnsassistdyninfo.setBrc(tranctx.getBrc());	
        	lnsassistdyninfo.setCustomid(repayAcctNo);
        	lnsassistdyninfo.setCcy("01");
        	lnsassistdyninfo.setFeetype(ConstantDeclare.ASSISTACCOUNT.PAYACCT);
        	lnsassistdyninfo.setLastbal(0.00);
        	lnsassistdyninfo.setCurrbal(0.00);
        	lnsassistdyninfo.setStatus("N");
        	lnsassistdyninfo.setOrdnu(1);

            DbAccessUtil.execute("Lnsassistdyninfo.insert", lnsassistdyninfo);

            


            //查询摊销计划表里面的保费
            param.put("brc", tranctx.getBrc());
            param.put("acctno", repayAcctNo);
            param.put("amortizetype", ConstantDeclare.AMORTIZETYPE.AMORTIZERBBF);//保费
            param.put("status",  ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);//保费
            param.put("trancode",  "470023");
	        param.put("reserv2",  "1");
            

            LnsAmortizeplan lnsamortizeplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsamortizeplan", param,
                    LnsAmortizeplan.class);
            if( null == lnsamortizeplan )
            	throw new FabException("LNS013");


            //查询幂等登记簿
            List<TblLnsinterface>  lnsinterfaces;
            try{
            	lnsinterfaces= DbAccessUtil.queryForList("Lnsinterface.query_repay_fee",param,TblLnsinterface.class);
            }catch(FabException e){
    			throw new FabException(e, "SPS103", "LNSINTERFACE");
    		}
            
    		

             //用总摊销金额-幂等登记簿里面保费的还款拆分金额之和，得到剩余保费
            FabAmount feeblance = new FabAmount(lnsamortizeplan.getTotalamt());
            if( null != lnsinterfaces && lnsinterfaces.size()>0 )
            {
            	 for(TblLnsinterface lnsinterface:lnsinterfaces){
                     feeblance.selfSub(lnsinterface.getTranamt());
                 }
            }
           
            //更新状态
            try {
                DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan_status", param);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "lnsamortizeplan");
            }
            

            if(feeblance.isPositive()){
                //开预收户  保费户
            	TblLnsassistdyninfo tblLnsassistdyninfo = lnsassistdyninfo.clone();
            	tblLnsassistdyninfo.setFeetype(ConstantDeclare.ASSISTACCOUNT.PREMIUMSACCT);
                //51350000
            	tblLnsassistdyninfo.setBrc("51350000");
            	tblLnsassistdyninfo.setOrdnu(1);
//            	tblLnsassistdyninfo.setCurrbal(feeblance.getVal());
                
                try{
                	DbAccessUtil.execute("Lnsassistdyninfo.insert", tblLnsassistdyninfo);
        		}
        		catch (FabSqlException e)
        		{					
        			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
        				throw new FabException(e, "LNS080", receiptNo);
        			}
        		}
                
                //账户辅助明细表
                LoanAssistDynInfoUtil.saveLnsassistlist(tranctx, 2,repayAcctNo,repayAcctNo, ConstantDeclare.ASSISTACCOUNT.PREMIUMSACCT, feeblance, "add");
            }
        }else{

            //增加预收户
            param.put("balance", amt.getVal());
        }

        //登记辅助明细表
        LoanAssistDynInfoUtil.saveLnsassistlist(tranctx, 1,repayAcctNo,repayAcctNo, ConstantDeclare.ASSISTACCOUNT.PAYACCT, amt, "add");
       

        

    }

    private void switchAccBrc() throws FabException {
        if(VarChecker.isEmpty(switchBrc))
            throw new FabException("LNS055","switchBrc");

        //修改预收户机构号

        //更新预收户brc
        Map<String,Object> param = new HashMap<>();

        param.put("brc", tranctx.getBrc());
        param.put("acctno", repayAcctNo);
        param.put("feetype", ConstantDeclare.ASSISTACCOUNT.PAYACCT);
        param.put("switchbrc", switchBrc);

        TblLnsassistdyninfo lnsassistdyninfo =  DbAccessUtil.queryForObject("Lnsassistdyninfo.update_brc", param,TblLnsassistdyninfo.class);
        if(lnsassistdyninfo == null)
            throw new FabException("LNS013");
        
        
        
      //抛送事件
      LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(repayAcctNo,tranctx);

      loanAgreement.getFundInvest().setChannelType(channelType);
	  LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(repayAcctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
			  ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
	  
	  String briefcode = "";
      if("51340000".equals(switchBrc)){
	    	 
    	  //将赔付户余额抛送新事件赔付开户 COMPENSATE给会计系统
    	  eventProvider.createEvent(ConstantDeclare.EVENT.COMPENSATE, new FabAmount(lnsassistdyninfo.getCurrbal()), lnsOpAcctInfo
    			  , null, loanAgreement.getFundInvest(),ConstantDeclare.BRIEFCODE.RDPF ,tranctx, switchBrc );
    	  
    	  briefcode = ConstantDeclare.BRIEFCODE.RDPF;
      }
      else if("51350000".equals(switchBrc))
    	  briefcode = ConstantDeclare.BRIEFCODE.RBPF;
    	  
              
     
	  //查询幂等登记簿
	  param.put("brc", tranctx.getBrc());
      param.put("acctno", loanAgreement.getContract().getReceiptNo());
      param.put("trancode",  "176002");
      
      //查询幂等登记簿
      List<TblLnsinterface>  lnsinterfaces;
      try{
    	  lnsinterfaces= DbAccessUtil.queryForList("Lnsinterface.query_repay_fee",param,TblLnsinterface.class);
      }catch(FabException e){
    	  throw new FabException(e, "SPS103", "LNSINTERFACE");
      }
	  //累计总还款金额
      FabAmount totalAmt = new FabAmount(0.00);
      if( null != lnsinterfaces && lnsinterfaces.size()>0 )
      {
    	  for(TblLnsinterface lnsinterface:lnsinterfaces){
    		  totalAmt.selfAdd(lnsinterface.getTranamt());
    	  }
      }
  
      //判断初始赔付户金额（预收登记簿备用字段）与赔付户余额给到会计系统，不一致时将差额抛会计系统新事件赔付调整COMPENADJU。
      if( totalAmt.sub(lnsassistdyninfo.getCurrbal()).isPositive())
    	  eventProvider.createEvent(ConstantDeclare.EVENT.COMPENADJU, totalAmt.sub(lnsassistdyninfo.getCurrbal()), lnsOpAcctInfo
				  , null, loanAgreement.getFundInvest(),briefcode ,tranctx,switchBrc );
      else if( totalAmt.sub(lnsassistdyninfo.getCurrbal()).isNegative())
    	  eventProvider.createEvent(ConstantDeclare.EVENT.COMPENADJU, new FabAmount(lnsassistdyninfo.getCurrbal()).sub(totalAmt.getVal()), lnsOpAcctInfo
				  , null, loanAgreement.getFundInvest(),briefcode ,tranctx,switchBrc );
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
	 * @return the amt
	 */
	public FabAmount getAmt() {
		return amt;
	}

	/**
	 * @param amt the amt to set
	 */
	public void setAmt(FabAmount amt) {
		this.amt = amt;
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
	 * @return the switchBrc
	 */
	public String getSwitchBrc() {
		return switchBrc;
	}

	/**
	 * @param switchBrc the switchBrc to set
	 */
	public void setSwitchBrc(String switchBrc) {
		this.switchBrc = switchBrc;
	}

	/**
	 * @return the dealType
	 */
	public String getDealType() {
		return dealType;
	}

	/**
	 * @param dealType the dealType to set
	 */
	public void setDealType(String dealType) {
		this.dealType = dealType;
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
	 * @return the depositType
	 */
	public String getDepositType() {
		return depositType;
	}

	/**
	 * @param depositType the depositType to set
	 */
	public void setDepositType(String depositType) {
		this.depositType = depositType;
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

}
