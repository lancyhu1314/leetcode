package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsaccountdyninfo;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈贷款核销子交易〉
 * 〈功能详细描述〉：通知核销金额添加C账户
 *
 * @Author 18049705
 * @Date Created in 14:48 2018/11/9
 * @see
 */
@Scope("prototype")
@Repository
public class Lns468 extends WorkUnit {

    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    String acctNo;

    TblLnsbasicinfo lnsbasicinfo;

    public String getWriteOffDate() {
        return writeOffDate;
    }

    public void setWriteOffDate(String writeOffDate) {
        this.writeOffDate = writeOffDate;
    }

    String writeOffDate;


    @Autowired
    @Qualifier("accountAdder")
    AccountOperator add;
    @Override
    public void run() throws Exception {

        TranCtx ctx = getTranctx();

        //查询主文件  --结息到当前日期 可能更改主文件的合同余额
        Map<String,Object> param = new HashMap<String,Object>();
        //账号
        param.put("acctno", acctNo);
        //机构
        param.put("openbrc", ctx.getBrc());

        try {
            //取主文件信息
            lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
        }catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_non_acctno");
        }

        //如果是新核销数据或者是已经迁移完成的数据不需要再次迁移
        if(lnsbasicinfo==null || lnsbasicinfo.getFlag1().contains(ConstantDeclare.FLAG1.H)){
            return ;
        }

        LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);

        //取计提登记簿信息
        TblLnspenintprovreg penintprovisionDint ;
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
            if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
                    .contains(lnsBill.getBilltype())
            )
            {
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
        //抛多次事件，一个账户类型一个金额一个事件
        for(Map.Entry<String,FabAmount> tzhx :tzhxMap.entrySet()) {
            if(Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,ConstantDeclare.BILLTYPE.BILLTYPE_GINT,ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(tzhx.getKey().split("\\.")[0])){
                //核销后罚息不需要通知的 过滤掉
                if("true".equals(la.getInterestAgreement().getIgnoreOffDint())&&tzhx.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)){
                    continue;
                }
                if (tzhx.getValue().isPositive()) {
                    //添加核销户
                    LnsAcctInfo lnsAcctInfo1 = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                            tzhx.getKey().split("\\.")[0], "C", new FabCurrency());
                    add.operate(lnsAcctInfo1, null, tzhx.getValue(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.TZHXOLD,
                            ctx);
                }
            }
        }
        //查询冲销日期
        Map interfaceParm=new HashMap();
        interfaceParm.put("brc",ctx.getBrc());
        interfaceParm.put("acctno",lnsbasicinfo.getAcctno());
        interfaceParm.put("trancode","472006");
        Map resultWriteOffInterface=null;
        try {
            resultWriteOffInterface= DbAccessUtil.queryForMap("Lnsinterface.query_repay_fee", interfaceParm);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "Lnsinterface");
        }
        //登记核销日期
        //主文件拓展表添加核销日期
        writeOffDate=(resultWriteOffInterface==null?ctx.getTranDate(): StringUtil.parseString(resultWriteOffInterface.get("ACCDATE")));
        Map<String,Object> exParam = new HashMap<>();
        exParam.put("acctno", lnsbasicinfo.getAcctno());
        exParam.put("brc", ctx.getBrc());
        exParam.put("key", "HXRQ");
        exParam.put("value1",writeOffDate);
        exParam.put("value2", 0.00);
        exParam.put("value3", 0.00);
        exParam.put("tunneldata", "");
        try {
            DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsbasicinfoex");
        }


        //更新主文件表的迁移状态
        try{
            param.put("acctno", lnsbasicinfo.getAcctno());
            param.put("brc", ctx.getBrc());
            param.put("flag1",lnsbasicinfo.getFlag1()+ConstantDeclare.FLAG1.HX);
            /*更新主文件表中该数据的贷款状态CA,以及利息计提标志CLOSE*/
            //核销不更改计提标志  在计提交易用贷款状态判断2019-10-31
            DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_flag", param);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "update_lnsbasicinfo_flag");
        }

    }


    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }


}
