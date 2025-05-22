package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.domain.TblLnsdiscountaccount;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	18049687
 *
 * @version V1.0.1
 *
 * @see 	汽车租赁账户查询
 *
 * @param 	acctNo		贷款账号
 * 			ccy			币种
 *
 * @return	discountAmt	预收贴息户
 *			businessAmt	业务保证金
 *			channelAmt	渠道保证金
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns435 extends WorkUnit {

	String		acctNo;		//贷款账号
	String		ccy;		//币种
	
	FabAmount	discountAmt;//预收贴息户
	FabAmount	businessAmt;//业务保证金
	FabAmount	channelAmt;	//渠道保证金

	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		if(VarChecker.isEmpty(acctNo)){
			throw new FabException("ACC103");
		}
		if(VarChecker.isEmpty(ccy)){
			throw new FabException("ACC107");
		}
		
		
		//查询贷款贴息户登记薄
		List<TblLnsdiscountaccount> lnsdiscountaccountList;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("brc", ctx.getBrc());
		params.put("acctno", acctNo);
		
		try{
			lnsdiscountaccountList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsdiscountaccount", params, TblLnsdiscountaccount.class);
		}catch(FabSqlException e){
			throw new FabException(e, "SPS103", "lnsdiscountaccount");
		}
		if(null == lnsdiscountaccountList || 0 == lnsdiscountaccountList.size()){
			throw new FabException("SPS104", "lnsdiscountaccount");
		}

		//返回字段赋值
		for (TblLnsdiscountaccount tblLnsdiscountaccount : lnsdiscountaccountList) {
			if (tblLnsdiscountaccount.getAccttype().equals("2")) {
				discountAmt = new FabAmount(tblLnsdiscountaccount.getBalance().doubleValue());
			}
			if (tblLnsdiscountaccount.getAccttype().equals("3")) {
				businessAmt = new FabAmount(tblLnsdiscountaccount.getBalance().doubleValue());
			}
			if (tblLnsdiscountaccount.getAccttype().equals("1")) {
				channelAmt = new FabAmount(tblLnsdiscountaccount.getBalance().doubleValue());
			}
		}
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
	 * @return the businessAmt
	 */
	public FabAmount getBusinessAmt() {
		return businessAmt;
	}

	/**
	 * @param businessAmt the businessAmt to set
	 */
	public void setBusinessAmt(FabAmount businessAmt) {
		this.businessAmt = businessAmt;
	}

	/**
	 * @return the channelAmt
	 */
	public FabAmount getChannelAmt() {
		return channelAmt;
	}

	/**
	 * @param channelAmt the channelAmt to set
	 */
	public void setChannelAmt(FabAmount channelAmt) {
		this.channelAmt = channelAmt;
	}

}
