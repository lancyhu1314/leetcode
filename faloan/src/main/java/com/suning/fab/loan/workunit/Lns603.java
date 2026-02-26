
package com.suning.fab.loan.workunit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * @author 	
 *
 * @version V1.0.1
 *
 * @see 	修改还款日
 *
 * @param	acctNo			借据号
 * 			repayDate		还款日期
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns603 extends WorkUnit {

	String acctNo;  //账号
	String repayDate; //还款日
	String serialNo; //流水号


	@Override
	public void run() throws Exception{
		
		TranCtx ctx = getTranctx();
		
		//参数为空判断
		if (VarChecker.isEmpty(repayDate) || !"15".equals(repayDate.trim())){
			throw new FabException("LNS005");
		}
		
		String repayDateStr = String.format("%02d", Integer.parseInt(repayDate));
		
		//查询主文件信息
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		param.put("brc", ctx.getBrc());

		TblLnsbasicinfo lnsbasicinfo;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}

		if (null == lnsbasicinfo){
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		//判断是否是每月一期的计算公式
		if(!lnsbasicinfo.getPrinperformula().trim().startsWith("1MA") || !lnsbasicinfo.getIntperformula().trim().startsWith("1MA")){
			throw new FabException("LNS045");
		}
		
		if(!Arrays.asList("06,11,16,21".split(",")).contains(lnsbasicinfo.getPrinperformula().trim().substring(3))){
			throw new FabException("LNS045");
		}
		
		//判断当前系统日期是否在合同结束日之前   判断还款方式是否是等本等息
		if(CalendarUtil.after(ctx.getTranDate(), lnsbasicinfo.getContduedate())
				|| !"0".equals(lnsbasicinfo.getRepayway())){
			throw new FabException("LNS045");
		}
		
		//查询还款计划
		Map<String,Object> reqParam = new HashMap<String,Object>();
		reqParam.put("acctno", acctNo);
		reqParam.put("openbrc", ctx.getBrc());
		reqParam.put("prinPerformula", "1MA"+repayDateStr);
		reqParam.put("intPerformula", "1MA"+repayDateStr);
		
		//拼接合同结束日字段
		//结束日方法
		String contduedate = getEndDate(lnsbasicinfo.getContduedate().trim(),repayDateStr,lnsbasicinfo.getPrinperformula().trim().substring(3)); 
		
		List<TblLnsrpyplan> rpyplanlist = null;

		try {
			//取还款计划登记簿数据
			rpyplanlist = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsrpyplan", param, TblLnsrpyplan.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsrpyplan");
		}

		if (null == rpyplanlist){
			rpyplanlist = new ArrayList<TblLnsrpyplan>();
		}
		
		//查询当前期还款计划
		TblLnsrpyplan lnsrpyplan = null;
		//当前期还款计划处于哪一期
		int termNo = 0;
		//取当前系统时间区间
		for(int i = 0 ; i < rpyplanlist.size() ; i++){
			TblLnsrpyplan tblLnsrpyplan = rpyplanlist.get(i);
			if(CalendarUtil.before(ctx.getTranDate(),tblLnsrpyplan.getRepayintedate())){
				lnsrpyplan = tblLnsrpyplan;
				termNo = i;
				break;
			}
		}
		if(lnsrpyplan == null ){
			throw new FabException("SPS103", "lnsrpyplan");
		}
//		lnsrpyplan = rpyplanlist.get(rpyplanlist.size()-1);
//		try {
//			lnsrpyplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrpyplan_603", param, TblLnsrpyplan.class);
//		}
//		catch (FabSqlException e)
//		{
//			throw new FabException(e, "SPS103", "lnsrpyplan");
//		}
//
//		if (null == lnsrpyplan){
//			throw new FabException("SPS104", "lnsrpyplan");
//		}
		
		//未来期标识
		boolean futureFlag = false;
		String repayOwnBDate = null;
		//判断还款计划是当期还是未来期
		if(ctx.getTranDate().equals(lnsrpyplan.getRepayintedate())){
			futureFlag = true;
		}
		Map<String,Object> rpyParam = new HashMap<String,Object>();
		//如果是未来期或者原来的还款日在修改还款日之前
		if(futureFlag || Integer.parseInt(lnsrpyplan.getRepayownbdate().substring(8)) < Integer.parseInt(repayDate)){
			repayOwnBDate = lnsrpyplan.getRepayownbdate().substring(0,8)+repayDateStr;
		//如果原来还款日在修改后还款日之后并且系统日期在还款日之后并且系统日期在原来还款日之前
		//原来还款日 > 系统日期 > 修改后还款日
		}else if(Integer.parseInt(lnsrpyplan.getRepayownbdate().substring(8)) > Integer.parseInt(repayDate)
				&& Integer.parseInt(ctx.getTranDate().substring(8)) > Integer.parseInt(repayDate)
				&& ctx.getTranDate().compareTo(lnsrpyplan.getRepayownbdate()) < 0){
			//如果系统日期 >修改后还款日
			if(ctx.getTranDate().compareTo(lnsrpyplan.getRepayownbdate().substring(0,8)+repayDateStr) > 0){
				//当前期还款日往后延一个月
				repayOwnBDate = CalendarUtil.nMonthsAfter(lnsrpyplan.getRepayownbdate(), 1).toString("yyyy-MM-dd").substring(0,8)+repayDateStr;
				contduedate = CalendarUtil.nMonthsAfter(contduedate,1).toString("yyyy-MM-dd");
			}else if(CalendarUtil.actualDaysBetween(lnsrpyplan.getRepayintbdate(), lnsrpyplan.getRepayownbdate().substring(0,8)+repayDateStr)>15){
				repayOwnBDate = lnsrpyplan.getRepayownbdate().substring(0,8)+repayDateStr;
			}else{
				repayOwnBDate = lnsrpyplan.getRepayownbdate().substring(0,8)+repayDateStr;
			}
		}else{
			repayOwnBDate = lnsrpyplan.getRepayownbdate().substring(0,8)+repayDateStr;
		}
		//跨月标志
		String flag1 = lnsbasicinfo.getFlag1();
		if( CalendarUtil.monthDifference(rpyplanlist.get(0).getRepayintbdate()
				, rpyplanlist.get(0).getRepayownbdate())== 2){
			flag1 = "1";
		}
		
		//跨两月标志(如果放款日在月底,还款日在15号之后)
		if(CalendarUtil.isMonthEnd(rpyplanlist.get(0).getRepayintbdate()) 
				&& Arrays.asList("16,21".split(",")).contains(lnsbasicinfo.getPrinperformula().trim().substring(3))){
			flag1 = "8";
		}
		
		reqParam.put("contduedate", contduedate);
		if(!VarChecker.isEmpty(flag1)){
			reqParam.put("flag1", flag1);
		}
		
		//是否已结清
		boolean isSettle = true;
		//结清账单特殊处理
		if("CA".equals(lnsbasicinfo.getLoanstat()) 
				&& !lnsbasicinfo.getCurprinterm().toString().equals(String.valueOf(rpyplanlist.size()))){
			rpyParam.put("period", lnsbasicinfo.getCurprinterm());
			rpyParam.put("enddate", contduedate);
			rpyParam.put("curenddate", contduedate);
			rpyParam.put("intedate", contduedate);
			rpyParam.put("statusbdate", contduedate);
			rpyParam.put("billtype", "PRIN");
			rpyParam.put("curenddate", CalendarUtil.nDaysAfter(contduedate, lnsbasicinfo.getGracedays()).toString("yyyy-MM-dd"));
			try {
				//账单
				DbAccessUtil.execute(
						"CUSTOMIZE.update_lnsbill_603", rpyParam);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS103", "update_lnsbill_603");
				}
			isSettle = false;
		}

		//修改还款计划表
		rpyParam.put("acctno", acctNo);
		rpyParam.put("brc", ctx.getBrc());
		for(int i = termNo ; i < rpyplanlist.size() ; i++){
			int a = 0;
			if(i != termNo){
				//还款计划
				rpyParam.put("repayintbdate", CalendarUtil.nMonthsAfter(repayOwnBDate, a).toString("yyyy-MM-dd"));
				//账单
				rpyParam.put("begindate", rpyParam.get("repayintbdate"));
				a++;
			}
			//还款计划
			repayOwnBDate = CalendarUtil.nMonthsAfter(repayOwnBDate, a).toString("yyyy-MM-dd");
			rpyParam.put("repayterm", rpyplanlist.get(i).getRepayterm());
			rpyParam.put("repayownbdate", repayOwnBDate);
			rpyParam.put("repayownedate", CalendarUtil.nDaysAfter(repayOwnBDate, lnsbasicinfo.getGracedays()).toString("yyyy-MM-dd"));
			rpyParam.put("repayintedate", repayOwnBDate);
			rpyParam.put("actrepaydate", repayOwnBDate);
			
			//账单
			rpyParam.put("period", rpyParam.get("repayterm"));
			rpyParam.put("enddate", repayOwnBDate);
			rpyParam.put("curenddate", repayOwnBDate);
			rpyParam.put("repayedate", rpyParam.get("repayownedate"));
			rpyParam.put("intedate", repayOwnBDate);
			rpyParam.put("statusbdate", repayOwnBDate);
			
			try {
				//还款计划
				DbAccessUtil.execute(
						"CUSTOMIZE.update_lnsrpyplan_603", rpyParam);
				
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "update_lnsrpyplan_603");
			}
			if(isSettle){
				try {
					//账单
					DbAccessUtil.execute(
							"CUSTOMIZE.update_lnsbill_603", rpyParam);
					} catch (FabSqlException e) {
						throw new FabException(e, "SPS103", "update_lnsbill_603");
					}
			}
			
		}
		
		//上次还款日更新
		
		if(!lnsbasicinfo.getLastprindate().equals(lnsbasicinfo.getOpendate())){
			reqParam.put("lastprindate", lnsbasicinfo.getLastprindate().substring(0,8)+repayDateStr);
			reqParam.put("lastintdate", lnsbasicinfo.getLastintdate().substring(0,8)+repayDateStr);
		}
		
		
		//更新主文件表中信息
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_repaydate", reqParam);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsbasicinfo");
		}
		
		Map<String,Object> dyParam = new HashMap<String,Object>();
		dyParam.put("acctno", acctNo);
		dyParam.put("brc", ctx.getBrc());
		dyParam.put("trandate", ctx.getTranDate());
		//更新动态表修改时间
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_dyninfo_603", dyParam);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsbasicinfo");
		}
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		//幂等
		TblLnsinterface	lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setSerialno("GD"+formatter.format(new Date())+ctx.getSerSeqNo());
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode("470015");
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setAcctname(lnsbasicinfo.getName());
		lnsinterface.setUserno(lnsbasicinfo.getCustomid());
		lnsinterface.setAcctno(acctNo);
		lnsinterface.setTranamt(0.0);
		lnsinterface.setMagacct(acctNo);
			
		
		try{
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
		} catch(FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				throw new FabException(e,TRAN.IDEMPOTENCY);
			}
			else
				throw new FabException(e, "SPS100", "lnsinterface");
		}
		
	}

	private String getEndDate(String endDate,String repayDateStr,String originalRepayDate){
		//最后一期的本期起日
		String lastStartDate = null;
		if(CalendarUtil.before(endDate.substring(0,8)+originalRepayDate, endDate)){
			if(CalendarUtil.actualDaysBetween(endDate.substring(0,8)+originalRepayDate, endDate)>15){
				lastStartDate = endDate.substring(0,8)+originalRepayDate;
			}else if(CalendarUtil.actualDaysBetween(endDate.substring(0,8)+originalRepayDate, endDate)<=15){
				lastStartDate = CalendarUtil.nMonthsAfter(endDate, -1).toString("yyyy-MM-dd").substring(0,8)+originalRepayDate;
			}
		}else if(CalendarUtil.after(endDate.substring(0,8)+originalRepayDate, endDate)){
			if(CalendarUtil.actualDaysBetween(CalendarUtil.nMonthsAfter(endDate, -1).toString("yyyy-MM-dd").substring(0,8)+originalRepayDate, endDate)>15){
				lastStartDate = CalendarUtil.nMonthsAfter(endDate, -1).toString("yyyy-MM-dd").substring(0,8)+originalRepayDate;
			}else if(CalendarUtil.actualDaysBetween(CalendarUtil.nMonthsAfter(endDate, -1).toString("yyyy-MM-dd").substring(0,8)+originalRepayDate, endDate)<=15){
				lastStartDate = CalendarUtil.nMonthsAfter(endDate, -2).toString("yyyy-MM-dd").substring(0,8)+originalRepayDate;
			}
		}else{
			lastStartDate = CalendarUtil.nMonthsAfter(endDate, -1).toString("yyyy-MM-dd");
		}
		
		//结束日期就是还款日
		if(endDate.substring(8).equals(repayDateStr)){
			return endDate.substring(0,8)+repayDateStr;
		}
		//原来日期在修改的还款日之后
		if(CalendarUtil.after(endDate, endDate.substring(0,8)+repayDateStr)){
			if(CalendarUtil.actualDaysBetween(lastStartDate, endDate.substring(0,8)+repayDateStr)<=15){
				return CalendarUtil.nMonthsAfter(endDate, 1).toString("yyyy-MM-dd").substring(0,8)+repayDateStr;
			}else if(CalendarUtil.actualDaysBetween(endDate, endDate.substring(0,8)+repayDateStr)>15){
				return CalendarUtil.nMonthsAfter(endDate, -1).toString("yyyy-MM-dd").substring(0,8)+repayDateStr;
			}else{
				return endDate.substring(0,8)+repayDateStr;
			}
		}
		//原来日期在修改的还款日之前
		if(CalendarUtil.before(endDate, endDate.substring(0,8)+repayDateStr)){
			if(CalendarUtil.actualDaysBetween(endDate, endDate.substring(0,8)+repayDateStr)>15){
				return CalendarUtil.nMonthsAfter(endDate, -1).toString("yyyy-MM-dd").substring(0,8)+repayDateStr;
			}else if(CalendarUtil.actualDaysBetween(CalendarUtil.nMonthsAfter(lastStartDate, 1).toString("yyyy-MM-dd"), endDate.substring(0,8)+repayDateStr)>15){
				return CalendarUtil.nMonthsAfter(endDate, -1).toString("yyyy-MM-dd").substring(0,8)+repayDateStr;
			}else{
				return endDate.substring(0,8)+repayDateStr;
			}
		}
		return null;
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
	 * @return the repayDate
	 */
	public String getRepayDate() {
		return repayDate;
	}

	/**
	 * @param repayDate the repayDate to set
	 */
	public void setRepayDate(String repayDate) {
		this.repayDate = repayDate;
	}

	/**
	 *
	 * @return serialNo 流水号
	 */
	public String getSerialNo() {
		return serialNo;
	}

	/**
	 *
	 * @param serialNo 流水号
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	
}
