package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnseventreg;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	HY
 *
 * @version V1.1.1
 *
 * @see 	-冲销幂等
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns108 extends WorkUnit {

	//账号
	String acctNo;
	//借据号
	String receiptNo;
	//期数
	Integer repayTerm;
	
	private Integer curTerm;

	String	serialNo;
	String		customName;
	FabAmount	contractAmt;
	private TblLnsinterface lnsinterface;
	private Map<Integer, String> acctList = new HashMap<>();

	@Override
	public void run() throws Exception {
		//2020-07-15 涉敏字段
		customName="";
		
		TranCtx ctx = getTranctx();
		int	i = 0;
		setCurTerm(0);
		
		contractAmt = new FabAmount(0.00);
		
		//非标冲销幂等处理
		if(VarChecker.asList("472004" ).contains(ctx.getTranCode()))
		{
			TblLnsinterface lnsinterface = new TblLnsinterface();
			lnsinterface.setTrandate(ctx.getTermDate());
			lnsinterface.setSerialno(serialNo);
			lnsinterface.setAccdate(ctx.getTranDate());
			lnsinterface.setSerseqno(ctx.getSerSeqNo());
			lnsinterface.setBrc(ctx.getBrc());
			lnsinterface.setTrancode(ctx.getTranCode());
			//lnsinterface.setAcctno();
			//lnsinterface.setCustomname();
			//lnsinterface.setTranamt();

			try{
				DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
			}catch (FabSqlException e){
				
				if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
					// 如果幂等，则去幂等登记薄中查询customName(acctname), acctNo,
					// contractAmt(tranamt)三个字段对应的值，并返回给外围
					lnsinterface.setTrancode(ctx.getTranCode());
					lnsinterface.setSerialno(ctx.getSerialNo());

					try {
						lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", lnsinterface,
								TblLnsinterface.class);
						if (null == lnsinterface) {
							throw new FabException("SPS103", "lnsinterface");
						}
						setCustomName("");
						setAcctNo(lnsinterface.getAcctno());
						setContractAmt(new FabAmount(lnsinterface.getTranamt()));
					} catch (FabSqlException se) {
						throw new FabException(se, "SPS100", "lnsinterface");
					}
					
					throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
				} else {
					throw new FabException(e, "SPS100", "lnsinterface");
				}
			}
		}	
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", ctx.getRequestDict("errDate"));
		param.put("serseqno", ctx.getRequestDict("errSerSeq"));


		try {
			lnsinterface = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsinterface_108", param, TblLnsinterface.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnseventreg");
		}
		if(lnsinterface==null){
			throw new FabException("LNS003");
		}
		setAcctNo(lnsinterface.getAcctno());
		setReceiptNo(lnsinterface.getAcctno());
		
		//定义查询map
		param.clear();
		param.put("acctno", getAcctNo());
		//机构
		param.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo ;
		try {
			//取主文件信息
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			//异常
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo){
			Map<String,Object> nonparam = new HashMap<String,Object>();
			//账号
			nonparam.put("acctno1", acctNo);
			//机构
			nonparam.put("openbrc", ctx.getBrc());
			
			List<TblLnsbasicinfo> lnsbasicinfolist;
			try {
				//取主文件信息
				lnsbasicinfolist = DbAccessUtil.queryForList("CUSTOMIZE.query_non_acctno", nonparam, TblLnsbasicinfo.class);
			}
			catch (FabSqlException e)
			{
				//异常
				throw new FabException(e, "SPS103", "Lnsnonstdplan");
			}
			
			if (null == lnsbasicinfolist){
				//lnsbasicinfo lnsnonstdplan都无数据
				throw new FabException("ACC108", acctNo);
			}
			else
			{	
				for( TblLnsbasicinfo lnsbasicinfonon : lnsbasicinfolist)
				{	
					i++;
					acctList.put(lnsbasicinfonon.getOldprinterm(), lnsbasicinfonon.getAcctno());
					contractAmt.selfAdd(new  FabAmount(lnsbasicinfonon.getContractamt()));
					//上面金额是否需要set一下 配置接口返回映射用
					setCustomName("");
				}
				setCurTerm(i);
			}	
		}
		else //主文件表有数据 准备给list赋值 之后循环list调用415
		{
			acctList.put(1, lnsbasicinfo.getAcctno());
			setCustomName("");
			setContractAmt(new FabAmount(lnsbasicinfo.getContractamt()));
		}	
		//取试算信息
		setAcctList(acctList);
		
		//更新幂等表
		Map<String, Object> lns108map = new HashMap<String, Object>();
		lns108map.put("acctno", getAcctNo());
		lns108map.put("acctname", "");
		lns108map.put("tranamt", getContractAmt().getVal());
		lns108map.put("reserv5", " ");
		lns108map.put("memo", " ");
		lns108map.put("serialno", getSerialNo());
		lns108map.put("trancode", ctx.getTranCode());

		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_off", lns108map);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsinterface");
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
	 * @return the repayTerm
	 */
	public Integer getRepayTerm() {
		return repayTerm;
	}


	/**
	 * @param repayTerm the repayTerm to set
	 */
	public void setRepayTerm(Integer repayTerm) {
		this.repayTerm = repayTerm;
	}

	public Map<Integer, String> getAcctList() {
		return acctList;
	}

	public void setAcctList(Map<Integer, String> acctList) {
		this.acctList = acctList;
	}

	public Integer getCurTerm() {
		return curTerm;
	}

	public void setCurTerm(Integer curTerm) {
		this.curTerm = curTerm;
	}

	public String getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	public String getCustomName() {
		return customName;
	}

	public void setCustomName(String customName) {
		this.customName = customName;
	}

	public FabAmount getContractAmt() {
		return contractAmt;
	}

	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	public String getReceiptNo() {
		return receiptNo;
	}

	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	public TblLnsinterface getLnsinterface() {
		return lnsinterface;
	}

	public void setLnsinterface(TblLnsinterface lnsinterface) {
		this.lnsinterface = lnsinterface;
	}
}
