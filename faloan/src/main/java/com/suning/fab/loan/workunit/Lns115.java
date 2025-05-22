package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanInterestSettlementUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈贷款核销子交易〉
 * 〈功能详细描述〉：贷款核销
 *
 * @Author 18049705
 * @Date Created in 14:48 2018/11/9
 * @see
 */
@Scope("prototype")
@Repository
public class Lns115 extends WorkUnit {

    String receiptNo;
    //渠道流水
    String serialNo;

    @Autowired
    LoanEventOperateProvider eventProvider;
    TblLnsbasicinfo lnsbasicinfo;
    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;
    @Autowired
    @Qualifier("accountAdder")
    AccountOperator add;
    @Override
    public void run() throws Exception {

        TranCtx ctx = getTranctx();



        LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);

        // 部分逾期和正常贷款  结息到当前日期  --类似于未来期的还款
       if( VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,
               ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR).contains(lnsbasicinfo.getLoanstat()))
       {
//           if(VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_YBYX,
//                   ConstantDeclare.REPAYWAY.REPAYWAY_XXHB,
//                   ConstantDeclare.REPAYWAY.REPAYWAY_XBHX)
//                   .contains( lnsbasicinfo.getRepayway()))
//           {
//               LoanInterestSettlementUtil.inerestBillByDate(ctx.getTranDate(), la, ctx);
//           }
//           else
//           {
  //               LoanInterestSettlementUtil.inerestBillByDate(ctx.getTranDate(), la, ctx);
                 LoanInterestSettlementUtil.inerestBillByDate(ctx.getTranDate(), la, ctx);

//           }
       }

        //查询主文件  --结息到当前日期 可能更改主文件的合同余额
        Map<String,Object> param = new HashMap<String,Object>();
        //账号
        param.put("acctno1", receiptNo);
        //机构
        param.put("openbrc", ctx.getBrc());

        try {
            //取主文件信息
            lnsbasicinfo = DbAccessUtil.queryForObject("CUSTOMIZE.query_non_acctno", param, TblLnsbasicinfo.class);

        }catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_non_acctno");
        }

        //取计提登记簿信息
        TblLnspenintprovreg penintprovisionDint = null;
        param.put("receiptno", lnsbasicinfo.getAcctno());
        param.put("brc", tranctx.getBrc());
        param.put("billtype", "DINT");
        try {
            penintprovisionDint = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS103", "罚息计提登记簿Lnspenintprovreg");
        }
        //罚息处理  计提总罚息 - 已还的罚息
        FabAmount  hxhkDint = new FabAmount();
        FabAmount  hxhkDint_3 = new FabAmount();
        if(penintprovisionDint!=null){
            hxhkDint.selfAdd(penintprovisionDint.getTotalinterest().doubleValue());
        }
        param.put("billtype", "CINT");
        try {
            penintprovisionDint = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS103", "罚息计提登记簿Lnspenintprovreg");
        }
        if(penintprovisionDint!=null){
            hxhkDint.selfAdd(penintprovisionDint.getTotalinterest().doubleValue());
    }
        /*
         *  因为结息 是在交易结束时插动态表  所以需要手动求和
         */
        //统计各个类型的账单   正常本金账单还需要加上主文件表的合同余额  用map储存
        Map<String,FabAmount> tzhxMap = new HashMap<>();//键值为账户类型 例：PRIN.N
        //查询已有的账单表
        param.put("acctno", lnsbasicinfo.getAcctno());
        param.put("brc", ctx.getBrc());
        List<TblLnsbill> tblLnsBills;
        try {
            tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbills", param, TblLnsbill.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_hisbills");
        }
        //主文件表的合同余额 作为正常本金
        if( new FabAmount(lnsbasicinfo.getContractbal()).isPositive())
            tzhxMap.put("PRIN.N",new FabAmount(lnsbasicinfo.getContractbal()));
        String key;
        for(TblLnsbill lnsBill:tblLnsBills)
        {
        	//根据罚息复利账本结束日是否超出原交易的账本到期结束日+90天，赋值对应表外cancelflag："3"（超90天）和表内cancelflag："1"（90天内）账本核销属性
        	 String cancelflag="1";
        	if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
                    .contains(lnsBill.getBilltype())
                    )
            {
        		param.clear();
        		param.put("trandate", lnsBill.getDertrandate());
                param.put("serseqno", lnsBill.getDerserseqno());
                param.put("txseq", lnsBill.getDertxseq());
                
                
                Map<String, Object> endDate = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsbill_enddate", param);
               
                
                if (null!=endDate)
        		{
                	String overStartDate = CalendarUtil.nDaysAfter(endDate.get("enddate").toString(), 90).toString("yyyy-MM-dd");

                	if (CalendarUtil.beforeAlsoEqual(lnsBill.getEnddate(), overStartDate)){
                		cancelflag="1";
                	}else{
                		cancelflag="3";
                	}
                	param.clear();
                	param.put("cancelflag",cancelflag);
            		param.put("trandate", lnsBill.getTrandate());
                    param.put("serseqno", lnsBill.getSerseqno());
                    param.put("txseq", lnsBill.getTxseq());
                    try {
                        DbAccessUtil.execute("CUSTOMIZE.update_lnsbill_cancelflag", param);
                    } catch (FabSqlException e) {
                        throw new FabException(e, "SPS102", "lnsbill");
                    }
        		}
            }
        	//循环更新利息账本cancelflag为“1”
        	if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
                    .contains(lnsBill.getBilltype())
                    )
            {
        		param.clear();
        		cancelflag="1";
            	param.put("cancelflag",cancelflag);
        		param.put("trandate", lnsBill.getTrandate());
                param.put("serseqno", lnsBill.getSerseqno());
                param.put("txseq", lnsBill.getTxseq());
                try {
                    DbAccessUtil.execute("CUSTOMIZE.update_lnsbill_cancelflag", param);
                } catch (FabSqlException e) {
                    throw new FabException(e, "SPS102", "lnsbill");
                }
            }
        	//罚息金额汇总处理，含总金额和表外金额
            if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
                    .contains(lnsBill.getBilltype())
                    )
            {
            	if("3".equals(cancelflag)){
            		hxhkDint_3.selfAdd(new FabAmount(lnsBill.getBillbal()));
            	}
                hxhkDint.selfSub(new FabAmount(lnsBill.getBillamt()).sub(lnsBill.getBillbal()));
                continue;
            }
            else{
                key = lnsBill.getBilltype()+"."+lnsBill.getBillstatus();
            }
            if(null!=tzhxMap.get(key))
            {
                tzhxMap.get(key).selfAdd(lnsBill.getBillbal());
            }else{
                tzhxMap.put(key, new FabAmount(lnsBill.getBillbal()));
            }
        }
        //需要核销通知的罚息 计算公式   = 计提总罚息 - 已还的罚息
        tzhxMap.put("DINT.N", hxhkDint);

        //更新主文件表的 状态为核销  利息计提标志为close
        try{

            param.put("acctno", lnsbasicinfo.getAcctno());
            param.put("brc", ctx.getBrc());
            param.put("loanstat", ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION);
            param.put("flag1",lnsbasicinfo.getFlag1()+ConstantDeclare.FLAG1.H);
            /*更新主文件表中该数据的贷款状态CA,以及利息计提标志CLOSE*/
            //核销不更改计提标志  在计提交易用贷款状态判断2019-10-31
            DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstatEx_115", param);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "update_lnsbasicinfo_loanstatEx_115");
        }

        //主文件拓展表添加核销日期
        Map<String,Object> exParam = new HashMap<>();
        exParam.put("acctno", receiptNo);
        exParam.put("brc", ctx.getBrc());
        exParam.put("key", "HXRQ");
        exParam.put("value1", ctx.getTranDate());
        exParam.put("value2", 0.00);
        exParam.put("value3", 0.00);
        exParam.put("tunneldata", "");
        try {
            DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsbasicinfoex");
        }

        //扣息放款  摊销结束
        if (new FabAmount( lnsbasicinfo.getDeductionamt()).isPositive())
        {
            param.put("brc", ctx.getBrc());
            param.put("acctno", lnsbasicinfo.getAcctno());
            param.put("status", ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);

            try {
                DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan_status", param);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "lnsamortizeplan");
            }

        }

        //抛多次事件，一个账户类型一个金额一个事件
        for(Map.Entry<String,FabAmount> tzhx :tzhxMap.entrySet()) {
            if(!Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,ConstantDeclare.BILLTYPE.BILLTYPE_GINT,ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(tzhx.getKey().split("\\.")[0])){
                continue;
            }
            //核销后罚息不需要通知的 过滤掉
            if("true".equals(la.getInterestAgreement().getIgnoreOffDint())&&tzhx.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)){
                continue;
            }
            if (tzhx.getValue().isPositive()) {
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                        tzhx.getKey().split("\\.")[0], tzhx.getKey().split("\\.")[1], new FabCurrency());

                sub.operate(lnsAcctInfo, null, tzhx.getValue(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.TZHX,
                        ctx);
                //添加核销户
                LnsAcctInfo lnsAcctInfo1 = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                        tzhx.getKey().split("\\.")[0], "C", new FabCurrency());

                add.operate(lnsAcctInfo1, null, tzhx.getValue(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.TZHX,
                        ctx);
                //登记本金户销户事件
                LnsAcctInfo opAcctInfo = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                        tzhx.getKey().split("\\.")[0], ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION, new FabCurrency());
                //罚息表外记账类型“3”金额汇总登记事件表amt2
                if("DINT".equals(tzhx.getKey().split("\\.")[0])){
                	List<FabAmount> amtList = new ArrayList<FabAmount>();
                	amtList.add(new FabAmount(0.00));
        			if(!VarChecker.isEmpty(hxhkDint_3)){
        				amtList.add(hxhkDint_3);
        			}
        			eventProvider.createEvent(ConstantDeclare.EVENT.LNCNLVRFIN, tzhx.getValue(),
                            lnsAcctInfo, opAcctInfo, la.getFundInvest(), ConstantDeclare.BRIEFCODE.TZHX, ctx, amtList,la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
                	
                }else{
                eventProvider.createEvent(ConstantDeclare.EVENT.LNCNLVRFIN, tzhx.getValue(),
                        lnsAcctInfo, opAcctInfo, la.getFundInvest(), ConstantDeclare.BRIEFCODE.TZHX, ctx, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
                }
            }
        }

    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }



    public LoanEventOperateProvider getEventProvider() {
        return eventProvider;
    }

    public void setEventProvider(LoanEventOperateProvider eventProvider) {
        this.eventProvider = eventProvider;
    }

    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }

    public AccountOperator getSub() {
        return sub;
    }

    public void setSub(AccountOperator sub) {
        this.sub = sub;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }
}
