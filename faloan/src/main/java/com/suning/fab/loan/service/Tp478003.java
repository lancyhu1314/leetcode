package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanBasicInfoProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
 * @author TT.Y
 * 
 * @version V1.0.0
 *
 * @see -非标准贷款事前利息减免
 *
 * @param -serialNo 业务流水号
 * @param -acctNo 贷款账号
 * @param -intAmt 利息减免金额
 * @param -forfeitAmt 罚息减免金额
 *
 * @return
 *
 * @exception
 */
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-nonIntterestDeduction")
public class Tp478003 extends ServiceTemplate {
	@Autowired Lns106 lns106;
	@Autowired Lns501 lns501;
	@Autowired Lns507 lns507; //生成到最近一期所有账单
	@Autowired Lns503 lns503; //转列
	@Autowired Lns504 lns504; //呆滞呆账罚息复利补记
	@Autowired Lns230 lns230; //利息减免
	@Autowired Lns218 lns218; //罚息减免
	@Autowired Lns235 lns235; //利息减免
	@Autowired Lns236 lns236; //罚息减免
	@Autowired Lns506 lns506; //更新贷款结清标志
	@Autowired Lns512 lns512; 
	@Autowired Lns513 lns513;
	//@Autowired Lns421 lns421;
	@Autowired Lns121 lns121;//考核数据校验 入库

	public Tp478003() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		FabAmount	intAmt = ctx.getRequestDict("intAmt");
		FabAmount	forfeitAmt = ctx.getRequestDict("forfeitAmt");
		
/*		FabAmount	intAmtTmp = new FabAmount(0.00);
		FabAmount	forfeitAmtTmp = new FabAmount(0.00);
		
		if(!VarChecker.isEmpty(intAmt))
			intAmtTmp.setVal(intAmt.getVal());
		
		if(!VarChecker.isEmpty(forfeitAmt))
			forfeitAmtTmp.setVal(forfeitAmt.getVal());
		*/
		
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("acctNo").toString(), ctx);
		prepareWriteOff(la);//转非应计变量预备
		//还款更新主文件时间戳 2019-04-15
		LoanBasicInfoProvider.basicInfoUptForRepay(la, ctx);
		trigger(lns106);
		Map<Integer,String> acctMap = lns106.getAcctList();
		int	curTerm = lns106.getCurTerm();

		if(curTerm == 0)
		{
			Map<String, Object> param = new HashMap<>();
			param.put("repayDate", ctx.getTranDate());

			//trigger(lns421);
			trigger(lns507, "map507_01", param);
			//核销的贷款 不能事前利息减免
			if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(lns507.getLnsbasicinfo().getLoanstat()))
				throw new FabException("LNS120",lns507.getLnsbasicinfo().getAcctno());//核销贷款 事前利息减免报错
			//罚息计提
			lns512.setRepayDate(ctx.getTranDate());
			lns512.setLnssatistics(lns507.getLnsBillStatistics());
			lns512.setLnsbasicinfo(lns507.getLnsbasicinfo());
			trigger(lns512);
			
			//罚息落表
			lns513.setRepayDate(ctx.getTranDate());
			lns513.setLnsBillStatistics(lns507.getLnsBillStatistics());
			lns513.setLnsbasicinfo(lns507.getLnsbasicinfo());
			trigger(lns513);
			
			trigger(lns503, "map503_02", param);
			/*del 2018-06-28 罚息结息时已记呆滞呆账*/
//			trigger(lns504, "map504_02", lns507); //呆滞结息 507传递subno给504
//			lns507.getLnsBillStatistics().setBillNo(lns504.getSubNo());
			//上面一行507重新取得504新生成subno
			//之后把试算结构体和subno重新传给230,230可能要试算当期的利息
			trigger(lns230, "map2301", lns507); //利息减免比lns220少了幂等在106处理
			trigger(lns218); //罚息减免
			//trigger(lns506);
		}
		else
		{
			for (Map.Entry<Integer, String> entry : acctMap.entrySet())
			{
				Map<String, Object> param = new HashMap<>();
				param.put("repayDate", ctx.getTranDate());
				param.put("acctNo", entry.getValue());				
				//trigger(lns421);
				trigger(lns501, "DEFAULT", param);
				//核销的贷款 不能事前利息减免
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(lns501.getLnsbasicinfo().getLoanstat()))
					throw new FabException("LNS120", entry.getValue());//核销贷款 事前利息减免报错
				trigger(lns503, "DEFAULT", param);
				trigger(lns504, "map501_02", lns501); //呆滞结息
				lns501.getLnsBillStatistics().setBillNo(lns504.getSubNo());

				lns235.setBillStatistics(lns501.getLnsBillStatistics());
				lns235.setSubNo(lns501.getLnsBillStatistics().getBillNo());
				
				Map<String, Object> param2356 = new HashMap<String, Object>();
				param2356.put("repayDate", ctx.getTranDate());
				param2356.put("acctNo", entry.getValue());
				
				param2356.put("intAmt", intAmt);
				param2356.put("forfeitAmt", forfeitAmt);

				trigger(lns235, "DEFAULT", param2356);
				trigger(lns236, "DEFAULT", param2356);//利息减免比lns220少了幂等在106处理
				
				//intAmtTmp.setVal(lns235.getIntAmtTmp().getVal() );
				//forfeitAmtTmp.setVal(lns236.getForfeitAmtTmp().getVal());

				//trigger(lns506, "DEFAULT", param2356);
			}
			
			if( VarChecker.isEmpty(intAmt) )
				intAmt = new FabAmount(0.00);
			if(VarChecker.isEmpty(forfeitAmt) )
				forfeitAmt = new FabAmount(0.00);
			
			if(!intAmt.isZero() || !forfeitAmt.isZero())
			{
				throw new FabException("LNS033");
			}

		}
		//防止幂等报错被拦截
		trigger(lns121);
	}

	/**
	 * 转非应计变量预备
	 *
	 * @param la
	 */
	public void prepareWriteOff(LoanAgreement la) {
		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(la.getContract().getLoanStat())){
			//将账户的状态放入上下文中  只有还款/减免服务才需要存放此字段
			ThreadLocalUtil.set("loanStat", la.getContract().getLoanStat());
		}
	}

	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		} 	
	}
}
