package com.suning.fab.loan.workunit;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanDbUtil;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author 15041590
 * @Date 2021/8/06 11:12
 * @Version 1.0
 */
@Scope("prototype")
@Repository
public class Lns124 extends WorkUnit {

	//----入参
	//借据号
	String acctNo;
	//机构号
	String brc;
	@Override
	public void run() throws Exception {

		TblLnsbasicinfo lnsinfo;
		Map<String, Object> param = new HashMap<>();
		param.put("acctno", acctNo);
		param.put("openbrc", brc);
		//表更新标识
		Boolean updFlag=false;
		try {
			//取主文件信息
			lnsinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}

		if (null == lnsinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		//贷款非结清状态下
		if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsinfo.getLoanstat())){

			//查询 结息登记簿lnspennintprovreg
			TblLnspenintprovreg lnspenintprovreg;
			param.put("billtype", ConstantDeclare.KEYNAME.DTFD);
			param.put("receiptno", acctNo);
			param.put("brc", brc);
			try {
				lnspenintprovreg = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "Lnspenintprovreg");
			}
			//封顶状态更新
			if ("B".equals(lnspenintprovreg.getReserv2())){
				param.put("reserv2", "");
				updFlag=true;
			}else if("lns609B".equals(lnspenintprovreg.getReserv2())){
				param.put("reserv2","lns609");
				updFlag=true;
			}
			// 结息登记簿更新操作
			if(updFlag){
				try {
					DbAccessUtil.execute("Lnspenintprovreg.updateTSFD", param);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS102", "Lnspenintprovreg");
				}
				//执行更新本金账本计止日期处理
				lnsBillUpdate(lnspenintprovreg.getEnddate(),lnsinfo);
			}
			//封顶日期大于合同到期日，特定的封顶日期或产品下补封顶值处理
			if ( CalendarUtil.before(lnsinfo.getContduedate(),lnspenintprovreg.getEnddate().toString()) &&
					"lns609B".equals(lnspenintprovreg.getReserv2().trim()) ){			
				if (CalendarUtil.equalDate("2019-11-06", lnspenintprovreg.getEnddate().toString())  ||
						CalendarUtil.equalDate("2019-11-07", lnspenintprovreg.getEnddate().toString()) ||
						(CalendarUtil.equalDate("2020-01-18", lnspenintprovreg.getEnddate().toString()) && "2512612".equals(lnsinfo.getPrdcode()))){
					//累加未还本金
					Map<String, Object> param1 = new HashMap<>();
			        param1.put("acctno", acctNo);
			        param1.put("brc", brc);
			        BigDecimal sumPrinBal;
			        try {
			        	sumPrinBal = DbAccessUtil.queryForObject("CUSTOMIZE.query_sumunrpyprin", param1, BigDecimal.class);
			        } catch (FabSqlException e) {
			            throw new FabException(e, "SPS103", "lnsbill");
			        }
			        //封顶日期-合同到期日间隔天数
			        int nTotalDays = CalendarUtil.actualDaysBetween(lnsinfo.getContduedate(), lnspenintprovreg.getEnddate().toString());
			       //计算封顶值，更新封顶金额
			        BigDecimal fdAddAmt= BigDecimal.valueOf(lnspenintprovreg.getTaxrate().doubleValue()).divide(new BigDecimal("360"), 20, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(nTotalDays))
                            .multiply(BigDecimal.valueOf(sumPrinBal.doubleValue())).setScale(2, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP);
			        param.put("fdaddamt", fdAddAmt);
			        try {
						DbAccessUtil.execute("Lnspenintprovreg.updateFDJE", param);
					} catch (FabSqlException e) {
						throw new FabException(e, "SPS102", "Lnspenintprovreg");
					}

				}
				
			}
		}
	}

	/**
	 * 更新本金账本计止日期
	 * @param endDate  达到封顶日期
	 * @param lnsinfo  主文件信息
	 *
	 * @throws FabException
	 */
	private void lnsBillUpdate(Date endDate, TblLnsbasicinfo lnsinfo) throws FabException {

		boolean ifOverdue = false;  //罚息是否大于0
		boolean ifOverdue1 = false;	//复利是否大于0
		boolean updBillFlag = false; //账本更新标志
		String  newInteDate=""; //计止日期
		//判断主文件表逾期利率是否大于0
		if (rateIsPositive(new FabRate(lnsinfo.getOverduerate()))){
			ifOverdue=true;
		}
		//判断主文件表复利利率是否大于0
		if (rateIsPositive(new FabRate(lnsinfo.getOverduerate1()))){
			ifOverdue1=true;
		}
		//查询历史期账本
		// 读取贷款账单
		Map<String, Object> param = new HashMap<String, Object>();

		param.put("acctno", acctNo);
		param.put("brc", brc);
		List<TblLnsbill> billList = LoanDbUtil.queryForList("Lnsbill.query_bill_adjust", param, TblLnsbill.class);
        
		//遍历本金账本和利息账本，若对应逾期利率和复利利率大于0且计止日期大于达到封顶日期，则将计止日期更新为达到封顶日期
		for(TblLnsbill lnsbill :billList) {
			if ("PRIN".equals(lnsbill.getBilltype()) && ifOverdue ||
				"NINT".equals(lnsbill.getBilltype()) && ifOverdue1){
				if(CalendarUtil.before(endDate.toString(),lnsbill.getIntedate())){
					updBillFlag=true;
					newInteDate=endDate.toString();
				}
			}
			if(updBillFlag){
				//根据唯一索引，更新账本表
				Map<String, Object> param1 = new HashMap<String, Object>();
				param1.put("trandate", lnsbill.getTrandate().toString());
				param1.put("serseqno", lnsbill.getSerseqno());
				param1.put("txseq", lnsbill.getTxseq());
				param1.put("intedate", newInteDate);
				try {
					DbAccessUtil.execute("CUSTOMIZE.update_lnsbill_intedate", param1);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS102", "Lnsbill");
				}
			}
		}

	}
	//利率大于0判断
	public  static  Boolean rateIsPositive(FabRate rate){
		return !VarChecker.isEmpty(rate)&&rate.getRate().compareTo(BigDecimal.valueOf(0.00))>0;
	}

	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}

	/**
	 * @param acctNo to set
	 */

	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	/**
	 * @return the brc
	 */

	public String getBrc() {
		return brc;
	}

	/**
	 * @param brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
	}

}
