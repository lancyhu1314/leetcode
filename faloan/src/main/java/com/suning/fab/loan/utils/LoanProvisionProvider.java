package com.suning.fab.loan.utils;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsprovision;
import com.suning.fab.loan.domain.TblLnsprovisiondtl;
import com.suning.fab.loan.domain.TblLnsprovisionreg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *利息计提工具类
 * author:16090227
 */
@Component
public class LoanProvisionProvider {


    /**
     *
     * @param brc 机构
     * @param childBrc 子机构
     * @param acctno 账户
     * @param billtype 账单类型
     * @param intertype 计提/摊销类型
     * @param la 协议
     * @return
     * @throws FabException
     */
    public static TblLnsprovision getLnsprovisionBasic(String brc,String childBrc,String acctno,String billtype,String intertype, LoanAgreement la) throws FabException {
        //构造查询明细的参数  老登记簿明细查询数据
        Map provisionOld=new HashMap();
        provisionOld.put("brc",brc);
        provisionOld.put("receiptno", acctno);
        provisionOld.put("billtype",billtype);
        provisionOld.put("intertype", intertype);
        provisionOld.put("childbrc",childBrc);
        //将老的明细参数进行转换
        Map provisionNew=new HashMap();
        provisionNew.putAll(provisionOld);
        exchangeProvision(provisionNew);
        //从新表中查询计提相关信息
        TblLnsprovision lnsprovisionBasic;
        try {
             lnsprovisionBasic = DbAccessUtil.queryForObject("Lnsprovision.selectByUk", provisionNew, TblLnsprovision.class);
        }catch (FabSqlException e){
            LoggerUtil.info("查询计提明细异常：",e);
            throw new FabException(e, "SPS100", "Lnsprovision");
        }
        if(lnsprovisionBasic==null){
            lnsprovisionBasic=new TblLnsprovision();
            //数据库中不存在
            lnsprovisionBasic.setSaveFlag(false);
            //若计提总表查询为空 则进入计提老的登记簿进行查询判断
            lnsprovisionBasic.setAcctno(la.getContract().getReceiptNo());
            lnsprovisionBasic.setBrc(brc);
            lnsprovisionBasic.setChildbrc(childBrc);
            lnsprovisionBasic.setCcy(la.getContract().getCcy().getCcy());
            lnsprovisionBasic.setBilltype(provisionNew.get("billtype")==null?"":provisionNew.get("billtype").toString());
            lnsprovisionBasic.setIntertype(provisionNew.get("intertype")==null?"":provisionNew.get("intertype").toString());
            lnsprovisionBasic.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
            lnsprovisionBasic.setBegindate(Date.valueOf(la.getContract().getContractStartDate()));
            lnsprovisionBasic.setEnddate(Date.valueOf(la.getContract().getContractEndDate()));
            lnsprovisionBasic.setLastenddate(Date.valueOf(la.getContract().getContractStartDate()));
            TblLnsprovisionreg lnsprovisionOld;
            //新的明细数据
            TblLnsprovisiondtl lnsprovisiondtl_new;
            try {
                //查询新计提登记簿
                LoggerUtil.info("search lnsprovision:{}",acctno+":"+billtype+":"+intertype);
                lnsprovisiondtl_new=DbAccessUtil.queryForObject("Lnsprovisiondtl.query_lnsprovision_lastperiod", provisionNew, TblLnsprovisiondtl.class);
            }catch (FabSqlException e){
                LoggerUtil.info("查询计提明细异常：",e);
                throw new FabException(e, "SPS100", "Lnsprovision");
            }

            if(lnsprovisiondtl_new!=null){
                //新的明细表进行赋值
                MapperUtil.map(lnsprovisiondtl_new, lnsprovisionBasic, "map500_01");
            }else{
                //查询计提老登记簿
                LoggerUtil.info("search lnsprovisionreg:{}",acctno+":"+billtype+":"+intertype);
                lnsprovisionOld = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_lastperiod", provisionOld, TblLnsprovisionreg.class);
                //新的明细表也没有记录
                if(lnsprovisionOld!=null){
                    //老登记簿查询有值则进行 赋值老的
                    MapperUtil.map(lnsprovisionOld, lnsprovisionBasic, "map511_01");
                }else{
                    //判断数据加载标志
                    lnsprovisionBasic.setExist(false);
                }
            }
        }else{
            //数据库中已存在
            lnsprovisionBasic.setSaveFlag(true);
        }
        return lnsprovisionBasic;
    }

    /**
     * 对明细数据的属性进行转换  并进行特殊处理
     *
     * @param provision
     */
    public static void exchangeProvision(Object provision)throws FabException{
        if(provision instanceof TblLnsprovisiondtl){
            try {
                Integer maxLine = DbAccessUtil.queryForObject("Lnsprovisiondtl.selectCurrentMaxNumber", provision, Integer.class);
                BeanUtils.setProperty(provision, "txnseq",maxLine == null ? 1 : maxLine + 1);
            }catch (FabSqlException e)
            {
                throw new FabException(e, "SPS103", "lnsprovisiondtl");
            }catch (Exception e){
                throw new FabException(e,"LoanProvisonProvider","属性转换异常");
            }
        }
        try {
            String billtype = BeanUtils.getProperty(provision, "billtype") == null ? "" : BeanUtils.getProperty(provision, "billtype").toString();
            String intertype = BeanUtils.getProperty(provision, "intertype") == null ? "" : BeanUtils.getProperty(provision, "intertype").toString();
            String childbrc = BeanUtils.getProperty(provision, "childbrc") == null ? "" : BeanUtils.getProperty(provision, "childbrc").toString();
            if ("MINT".equals(billtype) && "MANAGEFEE".equals(intertype)) {
                if("51340000".equals(childbrc)){
                    BeanUtils.setProperty(provision, "billtype", "SQFE");
                }else{
                    BeanUtils.setProperty(provision, "billtype", "RMFE");
                }
                BeanUtils.setProperty(provision, "intertype", "PROVISION");
            } else if ("MINT".equals(billtype) && "SECURITFEE".equals(intertype)) {
                BeanUtils.setProperty(provision, "billtype", "SQFE");
                BeanUtils.setProperty(provision, "intertype", "ONETIME");
            }  else if ("SQFE".equals(billtype) && "MANAGEFEE".equals(intertype)) {
                BeanUtils.setProperty(provision, "billtype", "SQFE");
                BeanUtils.setProperty(provision, "intertype", "PROVISION");
            } else if ("SQFE".equals(billtype) && "SECURITFEE".equals(intertype)) {
                BeanUtils.setProperty(provision, "billtype", "SQFE");
                BeanUtils.setProperty(provision, "intertype", "ONETIME");
            } else if ("RMFE".equals(billtype) && "MANAGEFEE".equals(intertype)) {
                BeanUtils.setProperty(provision, "billtype", "RMFE");
                BeanUtils.setProperty(provision, "intertype", "PROVISION");
            } else if ("GDBF".equals(billtype) && "GDBFAMOR".equals(intertype)) {
                BeanUtils.setProperty(provision, "billtype", "GDBF");
                BeanUtils.setProperty(provision, "intertype", "AMORTIZE");
            } else if ("RBBF".equals(billtype) && "RBBFAMOR".equals(intertype)) {
                BeanUtils.setProperty(provision, "billtype", "RBBF");
                BeanUtils.setProperty(provision, "intertype", "AMORTIZE");
            } else if ("FEEA".equals(billtype) && "AMORTIZE".equals(intertype)) {
                BeanUtils.setProperty(provision, "billtype", "SQFE");
                BeanUtils.setProperty(provision, "intertype", "AMORTIZE");
            }
        }catch (Exception e){
            throw new FabException(e,"LoanProvisonProvider","属性转换异常");
        }
    }

    /**
     * 获取计提明细初始化数据
     * @param ctx 交易上下文
     * @param lnsprovisionBasic 计提明细
     * @param totaltax 总税金
     * @param totalnint 总利息
     *
     * @return
     */
    public static TblLnsprovisiondtl getInitTblLnsprovisiondtl(TranCtx ctx, TblLnsprovision lnsprovisionBasic,FabAmount totaltax , FabAmount totalnint,int txnseq,int period){
        TblLnsprovisiondtl lnsprovisiondtl=new TblLnsprovisiondtl();
        lnsprovisiondtl.setTrandate(Date.valueOf(ctx.getTranDate()));
        //账务流水号
        lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
        //序号
        lnsprovisiondtl.setTxnseq(txnseq);
        //机构
        lnsprovisiondtl.setBrc(lnsprovisionBasic.getBrc());
        //子机构
        lnsprovisiondtl.setChildbrc(lnsprovisionBasic.getChildbrc());
        //借据号
        lnsprovisiondtl.setAcctno(lnsprovisionBasic.getAcctno());
        //笔数
        lnsprovisiondtl.setListno(lnsprovisionBasic.getTotallist() + 1);
        //账单类型 NINT利息 DINT罚息 CINT复利
        lnsprovisiondtl.setBilltype(lnsprovisionBasic.getBilltype());
        //币种
        lnsprovisiondtl.setCcy(lnsprovisionBasic.getCcy());
        //计提利息总金额
        lnsprovisiondtl.setTotalinterest(totalnint.getVal());
        //计提税金总金额
        lnsprovisiondtl.setTotaltax(totaltax.getVal());
        //期数
        lnsprovisiondtl.setPeriod(period);
        //税率
        lnsprovisiondtl.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
        //计提，摊销类型 取值：PROVISION计提，AMORTIZE摊销
        lnsprovisiondtl.setIntertype(lnsprovisionBasic.getIntertype());
        //开始日期
        lnsprovisiondtl.setBegindate(lnsprovisionBasic.getLastenddate());
        //结束日期
        lnsprovisiondtl.setEnddate(Date.valueOf(ctx.getTranDate()));
        //是否作废标志 取值：NORMAL正常，CANCEL作废
        lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
        return lnsprovisiondtl;
    }
}
