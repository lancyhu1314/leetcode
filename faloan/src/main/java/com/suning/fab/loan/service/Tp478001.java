package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanBasicInfoProvider;
import com.suning.fab.loan.workunit.Lns107;
import com.suning.fab.loan.workunit.Lns121;
import com.suning.fab.loan.workunit.Lns218;
import com.suning.fab.loan.workunit.Lns220;
import com.suning.fab.loan.workunit.Lns501;
import com.suning.fab.loan.workunit.Lns503;
import com.suning.fab.loan.workunit.Lns504;
import com.suning.fab.loan.workunit.Lns506;
import com.suning.fab.loan.workunit.Lns512;
import com.suning.fab.loan.workunit.Lns513;
import com.suning.fab.loan.workunit.Lns517;
import com.suning.fab.loan.workunit.Lns519;
import com.suning.fab.loan.workunit.Lns520;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
 * @author TT.Y
 * 
 * @version V1.0.0
 *
 * @see -贷款事前利息减免
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
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-exanteIntterestDeduction")
public class Tp478001 extends ServiceTemplate {

	@Autowired Lns501 lns501; //生成到最近一期所有账单
	@Autowired Lns503 lns503; //转列
	@Autowired Lns504 lns504; //呆滞呆账罚息复利补记
	@Autowired Lns220 lns220; //利息减免
	@Autowired Lns218 lns218; //罚息减免
	@Autowired Lns506 lns506; //更新贷款结清标志
	@Autowired Lns512 lns512; 
	@Autowired Lns513 lns513;
	@Autowired Lns519 lns519;
	@Autowired Lns520 lns520;
	@Autowired Lns107 lns107;
	@Autowired Lns121 lns121;//考核数据校验 入库
	@Autowired Lns517 lns517;//核销刷新贷款状态

	public Tp478001() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("acctNo").toString(), ctx);
		prepareWriteOff(la);//转非应计变量预备
		//还款更新主文件时间戳 2019-04-15
		LoanBasicInfoProvider.basicInfoUptForRepay(la, ctx);
		//核销状态借据减免罚息计提处理，贷款主文件状态更新
		lns517.setLoanAgreement(la);
        trigger(lns517);
        //赋值更新后的主文件信息
        lns501.setLnsbasicinfo(lns517.getLnsbasicinfo());
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("repayDate", ctx.getTranDate());
		trigger(lns501, "map501_01", param);

		//核销的贷款 不能  事前利息减免 
		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION.equals(lns501.getLnsbasicinfo().getLoanstat()))
			throw new FabException("LNS120",lns501.getLnsbasicinfo().getAcctno());//核销贷款 事前利息减免报错

		//撤销贷款不允许交易	20190626|14050183
		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lns501.getLnsbasicinfo().getLoanstat()))
			throw new FabException("ACC108",lns501.getLnsbasicinfo().getAcctno());
		
		//罚息计提
		lns512.setRepayDate(ctx.getTranDate());
		lns512.setLnssatistics(lns501.getLnsBillStatistics());
		lns512.setLnsbasicinfo(lns501.getLnsbasicinfo());
		trigger(lns512);
		
		//罚息落表
		if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())){
			//罚息落表
			lns520.setRepayDate(ctx.getTranDate());
			lns520.setLnsBillStatistics(lns501.getLnsBillStatistics());
			lns520.setLnsbasicinfo(lns501.getLnsbasicinfo());
			lns520.setLa(lns512.getLa());
			trigger(lns520);
		}else{
			lns513.setRepayDate(ctx.getTranDate());
			lns513.setLnsBillStatistics(lns501.getLnsBillStatistics());
			lns513.setLnsbasicinfo(lns501.getLnsbasicinfo());
			trigger(lns513);
		}
		
				
				
		trigger(lns503, "map503_01", param);
		/*del 2018-06-28 罚息结息时已记呆滞呆账*/
//		trigger(lns504, "map504_jm", lns501); //呆滞结息
//		lns501.getLnsBillStatistics().setBillNo(lns504.getSubNo());
		trigger(lns220, "map220", lns501);
		lns218.setRepayAmtList(lns220.getRepayAmtList());
		lns218.setReduceIntAmt(lns220.getReduceIntAmt());
		lns218.setReduceFintAmt(lns220.getReduceFintAmt());

		trigger(lns218);
		trigger(lns506);

		//防止幂等报错被拦截
		trigger(lns121);



		//房抵贷罚息落表封顶复习特殊处理20190127
		if("2412615".equals(la.getPrdId()) && la.getContract().getFlag1().contains("C")){
//			if(!VarChecker.isEmpty(lns513.getThisSerseqno()) && !VarChecker.isEmpty(lns513.getThisTranDate()) && !VarChecker.isEmpty(lns513.getThisTxSeq())){
				//修改intenddate
				lns519.setRepayDate(ctx.getTranDate());
				trigger(lns519);
//			}
		}
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
