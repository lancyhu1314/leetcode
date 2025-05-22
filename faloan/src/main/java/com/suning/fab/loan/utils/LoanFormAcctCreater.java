/**
 * @author 14050269 Toward
 * @version 创建时间：2016年6月13日 下午3:02:59
 * 类说明
 */
package com.suning.fab.loan.utils;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsaccountdyninfo;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

public class LoanFormAcctCreater {
	private LoanFormAcctCreater(){
		//nothing to do
	}
	
	public static void loanFormAcctCreater(LnsAcctInfo acct, TranCtx ctx)
			throws FabException {
		// 写动态表
		TblLnsaccountdyninfo lnsaccountdyninfo = new TblLnsaccountdyninfo();
		lnsaccountdyninfo.setAcctno(acct.getAcctNo());// 贷款账号
		lnsaccountdyninfo.setAcctstat(acct.getAccSrcCod());// 帐户形态
		lnsaccountdyninfo.setStatus(ConstantDeclare.STATUS.NORMAL);// 状态
		lnsaccountdyninfo.setPrdcode(acct.getPrdCode());
		// TODO:未使用
		lnsaccountdyninfo.setInacctno("");// 贷款内部账号

		if( !VarChecker.isEmpty(acct.getChildBrc()) )
			lnsaccountdyninfo.setProfitbrc(acct.getChildBrc());// 核算机构代码
		else
			lnsaccountdyninfo.setProfitbrc(ctx.getBrc());// 核算机构代码
		lnsaccountdyninfo.setOpenacctbrc(ctx.getBrc());// 开户机构代码
		lnsaccountdyninfo.setAcctype(ConstantDeclare.ACCTYPE.DEFAULT);// 帐别
		// TODO:科目控制字赋值待确定
		lnsaccountdyninfo.setSubctrlcode("");// 科目控制字
		// lnsacctdyninfo.setCcy(loanAgreement.getContract().getCcy().getCcy());//币种
		lnsaccountdyninfo.setCcy("01");
		// TODO:未使用开始
		lnsaccountdyninfo.setFlag("");// 静态控制标志
		lnsaccountdyninfo.setFlag1("");// 动态控制标志
		lnsaccountdyninfo.setRepayorder("");// 贷款还款顺序
		// TODO:未使用结束
		lnsaccountdyninfo.setRatetype("");// 利率类型
		lnsaccountdyninfo.setBald(ConstantDeclare.BALCTRLDIR.DEBIT);// 余额方向
		lnsaccountdyninfo.setIntrate(0.00);// 执行利率
		lnsaccountdyninfo.setLastbal(0.00);// 昨日余额
		lnsaccountdyninfo.setCurrbal(0.00);// 当前余额
		lnsaccountdyninfo.setCtrlbal(0.00);// 控制余额
		lnsaccountdyninfo.setAccum(0.00);// 积数
		// TODO:使用静态表起息日期，动态表起息日期不使用
		lnsaccountdyninfo.setBegindate("");// 起息日期
		// TODO:未使用开始
		lnsaccountdyninfo.setPrechgratedate("");// 上次利率调整日
		lnsaccountdyninfo.setPrecalcintdate("");// 上次结息日
		lnsaccountdyninfo.setNextcalcdate("");// 下次结息日
		// TODO:未使用结束
		lnsaccountdyninfo.setPretrandate(ctx.getTranDate());// 上笔交易日
		// TODO:未使用开始
		lnsaccountdyninfo.setDac("");// 校验位
		lnsaccountdyninfo.setBegincacudate("");// 累计积数起日
		lnsaccountdyninfo.setEndcacudate("");// 累计积数止日
		lnsaccountdyninfo.setPrecacudays(1);// 应计积数天数
		lnsaccountdyninfo.setCacudays(1);// 已计积数天数
		lnsaccountdyninfo.setYearstartbal(0.00);// 年初余额
		lnsaccountdyninfo.setFlagres11("");// 标志备用
		lnsaccountdyninfo.setDateres01("");// 日期备用
		lnsaccountdyninfo.setDateres02("");// 日期备用
		lnsaccountdyninfo.setAmountres1(0.00);// 金额备用
		lnsaccountdyninfo.setAmountres2(0.00);// 金额备用
		lnsaccountdyninfo.setAccumres(0.00);// 积数备用
		lnsaccountdyninfo.setAcctnores01("");// 账号备用
		lnsaccountdyninfo.setAcctnores02("");// 账号备用
		// TODO:未使用结束
		lnsaccountdyninfo.setOrdnu(1);// 明细帐序号
		lnsaccountdyninfo.setModifydate(ctx.getTranDate());// 交易日期
		// lnsacctdyninfo.setModifytime("");//交易时间
		DbAccessUtil.execute("Lnsaccountdyninfo.insert", lnsaccountdyninfo);
	}
}
