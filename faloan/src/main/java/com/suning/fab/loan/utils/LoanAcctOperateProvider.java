package com.suning.fab.loan.utils;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsacclist;
import com.suning.fab.loan.domain.TblLnsaccountdyninfo;
import com.suning.fab.tup4j.account.AcctChange;
import com.suning.fab.tup4j.account.AcctInfo;
import com.suning.fab.tup4j.account.AcctOperationInfo;
import com.suning.fab.tup4j.account.AssetAcctOperationInfo;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
@Component
public class LoanAcctOperateProvider implements AcctOperateProvider {

	public LoanAcctOperateProvider() {
		//nothing to do
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.suning.fab.tup4j.utils.AcctOperateProvider#generic(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public AssetAcctOperationInfo generic(AcctInfo acctInfo)
			throws FabException {

		return new AssetAcctOperationInfo(acctInfo);
	}

	private TblLnsacclist genNewAcctList(TblLnsaccountdyninfo acctdyninfo,AcctOperationInfo aoi,AcctChange change, TranCtx ctx) throws FabException
//	private TblLnsaccountlist genNewAcctList(TblLnsaccountdyninfo acctdyninfo,AcctOperationInfo aoi,AcctChange change, TranCtx ctx) throws FabException
	{
		FabAmount amount = new FabAmount(acctdyninfo.getCurrbal());
		if (ConstantDeclare.ACCOUNTCHANGE.CDFLAG.DEBIT.equals(change.getCdFlag()))
			acctdyninfo.setCurrbal(amount.add(change.getTranAmt()).getVal());

		if (ConstantDeclare.ACCOUNTCHANGE.CDFLAG.CREDIT.equals(change.getCdFlag()))
			acctdyninfo.setCurrbal(amount.sub(change.getTranAmt()).getVal());

		/*
		 * 开始填充旧账户明细 因为个人和对公的明细格式一样，所以复用单一格式插表
		 */
		calcNextPageNum(acctdyninfo);

		/* 开始填充新账户明细 */
//		TblLnsaccountlist al = MapperUtil.map(aoi.getAcct(), TblLnsaccountlist.class);
		TblLnsacclist al = MapperUtil.map(aoi.getAcct(), TblLnsacclist.class);
		MapperUtil.map(ctx, al);
		MapperUtil.map(change, al);

		MapperUtil.map(acctdyninfo, al);
		if (change.getOppAcctNo() != null)
		{
			al.setOppacctno(change.getOppAcctNo().getAcctNo());
		}
		al.setBal(acctdyninfo.getCurrbal());
		al.setOrdnu((acctdyninfo.getOrdnu()));
//		KafkaHelper.pending(ConstantDeclare.KAFKA.TOPIC.ACCTLIST, JSON.toJSONString(al))

		return al;
	}



	private TblLnsaccountdyninfo updateSubacct(LnsAcctInfo acctinfo,AssetAcctOperationInfo aoi, TranCtx ctx) throws FabException
	{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("acctno", acctinfo.getAcctNo());
		params.put("brc", ctx.getBrc());
		params.put("acctstat", acctinfo.getAccSrcCod());
		params.put("trandate", ctx.getTranDate());
		params.put("listcount", aoi.size());
		params.put("tranamt", aoi.getSummary()==null?Double.valueOf(0.00):aoi.getSummary().getVal());
		params.put("profitbrc", ctx.getBrc());
		if(!VarChecker.isEmpty(acctinfo.getChildBrc()))
			params.put("profitbrc", acctinfo.getChildBrc());
		TblLnsaccountdyninfo acctdyninfo = DbAccessUtil.queryForObject("CUSTOMIZE.query_update_dyninfo", params, TblLnsaccountdyninfo.class);

		if (acctdyninfo == null )
		{
			throw new FabException("ACC100", acctinfo.getAcctNo(), acctinfo.getAccSrcCod());
		}
		/**
		 *  系统日切
		 */
		if (!ctx.getTranDate().equals(SpsSysInfoHelper.getTrandate()))
		{
			throw new FabException("SPS111", ctx.getTranDate(),SpsSysInfoHelper.getTrandate());
		}
		if (!validBal(acctdyninfo))
		{
			LoggerUtil.error("账户可用余额不足inacctno={},currbal={}", acctdyninfo.getInacctno(),
					acctdyninfo.getCurrbal());
			throw new FabException("ACC101", acctdyninfo.getAcctno(), acctdyninfo.getAcctstat());
		}

		return acctdyninfo;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public void writeAccountRecordAtom(AcctOperationInfo acctoperationinfo, TranCtx ctx) throws FabException
	{

		AssetAcctOperationInfo aoi = (AssetAcctOperationInfo)acctoperationinfo;
		LnsAcctInfo acctinfo = AcctInfoProvider.readAcctInfo( aoi.getAcct(),ctx.getBrc());
		//正常本金账户不存在报错
		if (VarChecker.isEmpty(acctinfo.getPrdCode()) &&
				ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(acctinfo.getBillType()) &&
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL.equals(acctinfo.getLoanForm()))
		{
			throw new FabException("ACC100", acctinfo.getAcctNo(),ConstantDeclare.BILLTYPE.BILLTYPE_PRIN+"."+ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
		}
		//非正常本金账户则开立账户
		if (VarChecker.isEmpty(acctinfo.getPrdCode())  &&
				!((ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(acctinfo.getBillType()) &&
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL.equals(acctinfo.getLoanForm()))))
		{
			//检查本金账户是否存在
			 if (!prinAcctIsExist(acctinfo,ctx.getBrc()))
			 {
				 throw new FabException("ACC100", acctinfo.getAcctNo(), ConstantDeclare.BILLTYPE.BILLTYPE_PRIN+"."+ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
			 }

			//开立形态账户
			LoanFormAcctCreater.loanFormAcctCreater(acctinfo, ctx);
		}

		TblLnsaccountdyninfo acctdyninfo = this.updateSubacct(acctinfo, aoi, ctx);
		if (aoi.size()<=0)
			return ;
		FabAmount bal = new FabAmount(acctdyninfo.getCurrbal());
		acctdyninfo.setCurrbal(bal.sub(aoi.getSummary()).getVal());

		calcOldOrdNu(acctdyninfo,aoi.size());

		ArrayList<Map<String, Object>> newListes = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < aoi.size(); i++)
		{
			AcctChange change = aoi.get(i);
//			TblLnsaccountlist al = genNewAcctList(acctdyninfo, aoi, change, ctx);
			TblLnsacclist al = genNewAcctList(acctdyninfo, aoi, change, ctx);
			if(!VarChecker.isEmpty(acctinfo.getChildBrc()))
				al.setBrc(acctinfo.getChildBrc());
			newListes.add(MapperUtil.map(al, Map.class));
		}
//		DbAccessUtil.batchUpdate("Lnsaccountlist.insert",newListes.toArray(new Map[newListes.size()]));
		DbAccessUtil.batchUpdate("Lnsacclist.insert",newListes.toArray(new Map[newListes.size()]));
	}

	private Boolean prinAcctIsExist(LnsAcctInfo acct,String brc) throws FabException
	{
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acct.getAcctNo());
		param.put("ccy", acct.getCcy().getCcy());
		param.put("acctstat", ConstantDeclare.BILLTYPE.BILLTYPE_PRIN+"."+ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
		param.put("brc",brc);
		//本金暂时没有多核算机构
		param.put("profitbrc", brc);
		Map<String, Object> prdcode = DbAccessUtil.queryForMap("CUSTOMIZE.query_prdcode", param);
		if (prdcode != null)
		{
			acct.setPrdCode(prdcode.get("prdcode").toString());
			return true;
		}
		return false;
	}

	/**
	 * 根据当前账户的状态进行状态转换
	 * @param acctInfo
	 */
	public static void changeLoanStat(LnsAcctInfo acctInfo){
		//新的核销状态时 使用核销状态户
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get("loanStat",""))){
			if(acctInfo!=null){
				String billtype=acctInfo.getBillType();
				//只对本金产生的罚息 利息 复利 进行转换
				if(Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,ConstantDeclare.BILLTYPE.BILLTYPE_GINT,ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(billtype)&&!("true".equals(ThreadLocalUtil.get(ConstantDeclare.PARACONFIG.IGNOREOFFDINT,""))&&ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(billtype)))
				{
					acctInfo.setLoanForm(ThreadLocalUtil.get("loanStat",""));
					acctInfo.setAccSrcCod(billtype+"."+ThreadLocalUtil.get("loanStat",""));
				}
			}
		}
	}

	/**
	 * 根据当前账户的状态进行状态转换
	 * @param acctInfo
	 */
	public static void changeEventLoanStat(AcctInfo acctInfo){


		//当账户信息存在继承关系时
		if(acctInfo instanceof LnsAcctInfo){
			if( !("1".equals(((LnsAcctInfo) acctInfo).getCancelFlag()) &&
					(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get("loanStat",""))))
				changeLoanStat((LnsAcctInfo)acctInfo);
		}else {
			if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get("loanStat",""))) {
				if (acctInfo != null) {
					String accSrcCod = acctInfo.getAccSrcCod();
					if (accSrcCod != null) {
						String[] status = accSrcCod.split("\\.");
						if (status.length > 0) {
							String billType = status[0];
							//只对本金产生的罚息 利息 复利 进行转换
							if (Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, ConstantDeclare.BILLTYPE.BILLTYPE_NINT, ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, ConstantDeclare.BILLTYPE.BILLTYPE_GINT, ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(billType) && !("true".equals(ThreadLocalUtil.get(ConstantDeclare.PARACONFIG.IGNOREOFFDINT, "")) && ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(billType))) {
								acctInfo.setAccSrcCod(billType + "." + ThreadLocalUtil.get("loanStat",""));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 核销账户进行账户转换
	 * @param eventCode
	 * @param acctInfo
	 * @param oppAcctInfo
	 * @return true转换成功向下运行
	 */
	public static boolean changeEventLoanStat(String eventCode,AcctInfo acctInfo,AcctInfo oppAcctInfo){
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get("loanStat", ""))){
			if( !("1".equals(((LnsAcctInfo) acctInfo).getCancelFlag()) &&
					VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get("loanStat", "")))) {
				//核销状态 转列事件不需要抛送
				if (Arrays.asList(ConstantDeclare.EVENT.LNTRANSFER, ConstantDeclare.EVENT.INTTRNSFER, ConstantDeclare.EVENT.FEETRNSFER, ConstantDeclare.EVENT.LOANRETURN, ConstantDeclare.EVENT.NINTRETURN, ConstantDeclare.EVENT.ACCRUEDINT).contains(eventCode)) {
					return true;
				} else {
					//事件进行状态转换
					changeEventLoanStat(acctInfo);
					changeEventLoanStat(oppAcctInfo);
					return false;
				}
			}
		}else{
			return false;
		}
		return false;
	}



	private void calcOldOrdNu(TblLnsaccountdyninfo acctdyninfo, int delta)
	{
		acctdyninfo.setOrdnu(acctdyninfo.getOrdnu() - delta);
	}

	private void calcNextPageNum(TblLnsaccountdyninfo acctdyninfo)
	{
		acctdyninfo.setOrdnu(acctdyninfo.getOrdnu() + 1);
	}

	private boolean validBal(TblLnsaccountdyninfo acctdyninfo)
	{
		if (acctdyninfo.getCurrbal()  < -0.005)
			return false;
		return true;
	}

}
