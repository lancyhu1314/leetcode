package com.suning.fab.loan.utils;


import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsassistlist;
import com.suning.fab.loan.domain.TblLnsprefundaccount;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.ServiceFactory;
import com.suning.fab.tup4j.utils.VarChecker;

import java.util.HashMap;
import java.util.Map;


/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705 MYP
 * @Date Created in 10:12 2019/5/15
 * @see
 */
public class LoanAssistDynInfoUtil {

    //辅助表其他信息获取
    public static TblLnsassistdyninfo saveLnsassistlist(TranCtx ctx, Integer txseqno, String acctno, String customid, String feetype, FabAmount amount, String cdflag) throws FabException {
        //如果金额为0，就不需要存了
        if (!amount.isPositive())
            return null;

        TblLnsassistlist lnsassistlist = new TblLnsassistlist();
        lnsassistlist.setTrandate(ctx.getTranDate());
        lnsassistlist.setSerseqno(ctx.getSerSeqNo());
        lnsassistlist.setTxnseq(GuidUtil.incrSubSeq(ConstantDeclare.SEQUENCE.TXNSUBSEQ));
        lnsassistlist.setAcctno(acctno);
        lnsassistlist.setBrc(ctx.getBrc());
        lnsassistlist.setCustomid(customid);
        lnsassistlist.setCcy("01");
        lnsassistlist.setFeetype(feetype);
        lnsassistlist.setCdflag(cdflag);
        lnsassistlist.setTranamt(amount.getVal());
        lnsassistlist.setAcctstat("N");
        lnsassistlist.setTrancode(ctx.getTranCode());


        //查询辅助动态表信息
        Map<String, Object> param = new HashMap<>();
        param.put("brc", ctx.getBrc());
        param.put("acctno", acctno);
        param.put("feetype", feetype);
        param.put("cdflag", lnsassistlist.getCdflag());
        param.put("currbal", amount.getVal());
        param.put("trandate", ctx.getTranDate());

        TblLnsassistdyninfo lnsassistdyninfo = DbAccessUtil.queryForObject("Lnsassistdyninfo.selectByUk", param, TblLnsassistdyninfo.class);
        if (null == lnsassistdyninfo) {
            lnsassistlist.setBal(amount.getVal());
        } else {
            if ("sub".equals(lnsassistlist.getCdflag())) {
                lnsassistlist.setBal(new FabAmount(lnsassistdyninfo.getCurrbal()).sub(amount).getVal());
            } else if ("add".equals(lnsassistlist.getCdflag())) {
                lnsassistlist.setBal(new FabAmount(lnsassistdyninfo.getCurrbal()).add(amount).getVal());
            } else
                throw new FabException("xxxx");   //TODO： 光大
        }

        if (new FabAmount(lnsassistlist.getBal()).isNegative()) {
            if (ConstantDeclare.ASSISTACCOUNT.PREMIUMSACCT.equals(feetype))
                throw new FabException("LNS070", "保费户");
            else if (ConstantDeclare.ASSISTACCOUNT.PAYACCT.equals(feetype))
                throw new FabException("LNS070", "赔付户");
        }


        int count;
        try {
            count = DbAccessUtil.execute("Lnsassistdyninfo.update_ordnu", param);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsassistdyninfo");
        }
        if (1 != count) {
            throw new FabException("SPS102", "lnsassistdyninfo");
        }


        lnsassistlist.setOrdnu(lnsassistdyninfo.getOrdnu());
        insertLnsassistlist(lnsassistlist);

        return lnsassistdyninfo;
    }


    /**
     * 辅助账户明细表插表
     *
     * @param lnsassistlist 辅助账户明细表
     * @throws FabException 插表异常
     */
    private static void insertLnsassistlist(TblLnsassistlist lnsassistlist) throws FabException {
        try {
            DbAccessUtil.execute("Lnsassistlist.insert", lnsassistlist);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100", "lnsassistlist");
        }
    }


    /**
     * 更新辅助动态表并且插入一条辅助账户明细表
     */
    public static TblLnsassistdyninfo updatePreaccountInfo(TranCtx ctx, String brc, String acctno, String feetype, FabAmount amount, String cdflag, String custtype) throws FabException {


        // ①更新辅助动态表数据
        TblLnsassistdyninfo dyninfo = getAndUpdateLnsassistdyninfo(ctx, brc, acctno, feetype, amount, cdflag);
        int iCount=0;
        // 如果辅助动态表中没有数据，查询贷款预收户登记簿
        while (null == dyninfo) {
//            //1、查询贷款预收登记簿
//            TblLnsprefundaccount fundaccount = queryPrefundaccount(brc, acctno, "", feetype);
        	iCount++;
        	if(iCount>100){
        		throw new FabException("LNS157", acctno);
        	}
            //2、将数据插入到辅助动态表中
            TblLnsassistdyninfo lnsassistdyninfo = new TblLnsassistdyninfo();
            lnsassistdyninfo.setBrc(brc);
            lnsassistdyninfo.setAcctno(acctno);
            lnsassistdyninfo.setCustomid(acctno);
            lnsassistdyninfo.setFeetype(feetype);
            lnsassistdyninfo.setOrdnu(0);
            lnsassistdyninfo.setStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
            lnsassistdyninfo.setCurrbal(0.00);
            lnsassistdyninfo.setLastbal(0.00);
            lnsassistdyninfo.setCcy(ConstantDeclare.CCY.CCY_CNY_S);

//            if (null != fundaccount) {
//                // 提取贷款预收户登记簿数据到辅助动态表中
//                lnsassistdyninfo.setCurrbal(fundaccount.getBalance());
//                lnsassistdyninfo.setAcctno(fundaccount.getAcctno());
//                lnsassistdyninfo.setCustomid(fundaccount.getCustomid());
//                lnsassistdyninfo.setLastbal(0.00);
//                lnsassistdyninfo.setCusttype(fundaccount.getCusttype());
//                lnsassistdyninfo.setCreatetime(fundaccount.getOpendate());
//                lnsassistdyninfo.setPretrandate(ctx.getTranDate());
//
//                // 将即将迁移的老数据做标记
//                discareDate(brc,feetype,fundaccount.getAcctno(),fundaccount.getCustomid());
//
//                LnsAcctInfo oppNAcctInfo = new LnsAcctInfo("", ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT, "", new FabCurrency());
//                if (new FabAmount(fundaccount.getBalance()).isPositive())
//                    ServiceFactory.getBean(LoanAcctChargeProvider.class).eventProvider.createEvent(ConstantDeclare.EVENT.LNMOPREACT,
//                            new FabAmount(fundaccount.getBalance()),
//                            null,
//                            oppNAcctInfo,
//                            new FundInvest(),
//                            ConstantDeclare.BRIEFCODE.YSQY,
//                            ctx, fundaccount.getCustomid(), "", fundaccount.getCusttype()
//                    );
//            } else {
                if (VarChecker.isEmpty(custtype))
                    throw new FabException("LNS227", feetype, acctno);

                lnsassistdyninfo.setCustomid(acctno);
                lnsassistdyninfo.setCusttype("2".equals(custtype)?"COMPANY":"PERSON");

//            }
              //判断客户号是否已经开立预收户
                TblLnsassistdyninfo preaccountInfo_O=queryLnsassistdynInfoByCustomid(ctx.getBrc(), acctno,feetype);
                if(null==preaccountInfo_O ){
                	LoggerUtil.info("预收户未开户" + acctno);
                	
                	try {
                		DbAccessUtil.execute("Lnsassistdyninfo.insert", lnsassistdyninfo);
                	} catch (FabSqlException e) {
                		if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                			LoggerUtil.info("预收户已开户" + acctno);
                		}else {
                			throw new FabException(e, "SPS100", "Lnsassistdyninfo");
                		}
                	}
                }

            // 重新执行更新方法
            dyninfo = getAndUpdateLnsassistdyninfo(ctx, brc, acctno, feetype, amount, cdflag);

        }

        // ②向辅助动态明细表中插入一条数据
        TblLnsassistlist assistlist = new TblLnsassistlist();
        assistlist.setAcctno(acctno);
        assistlist.setCustomid(dyninfo.getCustomid());
        assistlist.setBrc(brc);
        assistlist.setFeetype(feetype);
        assistlist.setBal(dyninfo.getCurrbal());
        assistlist.setAcctstat(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
        assistlist.setCcy(ConstantDeclare.CCY.CCY_CNY_S);
        assistlist.setCdflag(cdflag);
        assistlist.setSerseqno(ctx.getSerSeqNo());
        assistlist.setTrandate(ctx.getTranDate());
        assistlist.setOrdnu(dyninfo.getOrdnu());
        assistlist.setTrancode(ctx.getTranCode());
        assistlist.setTxnseq(GuidUtil.incrSubSeq(ConstantDeclare.SEQUENCE.TXNSUBSEQ));
        assistlist.setTranamt(amount.getVal());
        // 预收充值/冲退接口传的借据号存入reserv6
        if (VarChecker.asList("176001", "176002").contains(ctx.getRequestDict("tranCode"))) {
            // 因为充值接口存在批量，所以通过ctx的tunneldate将借据号传过来
            assistlist.setReserv6(ctx.getTunnelData());
        }

        if (new FabAmount(assistlist.getTranamt()).isPositive()) {
            try {
                DbAccessUtil.execute("Lnsassistlist.insert", assistlist);
            } catch (FabException e) {
                throw new FabException(e, "SPS100", "Lnsassistlist");
            }
        }


        return dyninfo;

    }

    /**
     * 查询贷款预收登记簿
     *
     * @param brc
     * @param acctno
     * @param feetype
     * @return
     * @throws FabException
     */
    private static TblLnsprefundaccount queryPrefundaccount(String brc, String acctno, String customid, String feetype) throws FabException {
        TblLnsprefundaccount fundaccount;
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("brc", brc);
            param.put("acctno", acctno);
            param.put("accsrccode", feetype);
            param.put("customid", customid);
            fundaccount = DbAccessUtil.queryForObject("Lnsprefundaccount.selectByUk", param, TblLnsprefundaccount.class);
        } catch (FabException e) {
            throw new FabException(e, "SPS102", "Lnsprefundaccount");
        }
        return fundaccount;
    }


    /**
     * 更新辅助动态表中的数据
     *
     * @param ctx
     * @param acctno
     * @param feetype
     * @param amount
     * @param cdflag
     * @return
     * @throws FabException
     */
    private static TblLnsassistdyninfo getAndUpdateLnsassistdyninfo(TranCtx ctx, String brc, String acctno, String feetype, FabAmount amount, String cdflag) throws FabException {
        Map<String, Object> param = new HashMap<>();
        param.put("brc", brc);
        param.put("acctno", acctno);
//        param.put("customid", customid);
        param.put("feetype", feetype);
        param.put("cdflag", cdflag);
        param.put("currbal", amount.getVal());
        param.put("trandate", ctx.getTranDate());

        TblLnsassistdyninfo dyninfo;
        try {
            dyninfo = DbAccessUtil.queryForObject("Lnsassistdyninfo.updateByExist", param, TblLnsassistdyninfo.class);
        } catch (FabException e) {
            throw new FabException(e, "SPS102", "lnsassistdyninfo");
        }
        return dyninfo;
    }

    /**
     * 预收户数据查询：先查辅助动态表，如果没有，再查贷款预收登记簿，最后将数据迁移到辅助动态表
     *
     * @param brc
     * @param acctno
     * @param feetype
     * @return
     * @throws FabException
     */
    public static TblLnsassistdyninfo queryPreaccountInfo(String brc, String acctno, String customid, String feetype, TranCtx ctx) throws FabException {

    	if( VarChecker.isEmpty(acctno) && VarChecker.isEmpty(customid) )
    		throw new FabException("LNS010");
    		
        Map<String, Object> param = new HashMap<>();
        param.put("brc", brc);
        param.put("acctno", acctno);
        param.put("feetype", feetype);
        param.put("customid", customid);
        TblLnsassistdyninfo lnsassistdyninfo;
        try {
            // 1、查询辅助动态表
            lnsassistdyninfo = DbAccessUtil.queryForObject("Lnsassistdyninfo.selectByUk", param, TblLnsassistdyninfo.class);
        } catch (FabException e) {
            throw new FabException(e, "SPS103", "lnsassistdyninfo");
        }

        if (null == lnsassistdyninfo) {
        	return null;
//            // 2、从贷款预收登记簿查找
//            TblLnsprefundaccount lnsprefundaccount = queryPrefundaccount(brc, acctno, customid, feetype);
//
//            if (null == lnsprefundaccount) {
//                return null;
//            }
//            ///3、插入到辅助账户表
//            lnsassistdyninfo = new TblLnsassistdyninfo();
//            lnsassistdyninfo.setBrc(brc);
//            lnsassistdyninfo.setAcctno(lnsprefundaccount.getAcctno());
//            lnsassistdyninfo.setFeetype(feetype);
//            lnsassistdyninfo.setOrdnu(0);
//            lnsassistdyninfo.setStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
//            lnsassistdyninfo.setCcy(ConstantDeclare.CCY.CCY_CNY_S);
//            lnsassistdyninfo.setCurrbal(lnsprefundaccount.getBalance());
//            lnsassistdyninfo.setCustomid(lnsprefundaccount.getCustomid());
//            lnsassistdyninfo.setLastbal(0.00);
//            lnsassistdyninfo.setPretrandate(ctx.getTranDate());
//            lnsassistdyninfo.setCusttype(lnsprefundaccount.getCusttype());
//            lnsassistdyninfo.setCreatetime(lnsprefundaccount.getOpendate());
//            
//            //无流水号不迁移
//            if( VarChecker.isEmpty(ctx.getSerSeqNo()))
//            	return lnsassistdyninfo;
//            try {
//                DbAccessUtil.execute("Lnsassistdyninfo.insert", lnsassistdyninfo);
//            } catch (FabException e) {
//                throw new FabException(e, "SPS100", "Lnsassistdyninfo");
//            }
//            // 将迁移的老数据做标记
//            discareDate(brc, feetype, lnsassistdyninfo.getAcctno(), lnsassistdyninfo.getCustomid());
//
//            LnsAcctInfo oppNAcctInfo = new LnsAcctInfo("", ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT, "", new FabCurrency());
//
//            if (new FabAmount(lnsprefundaccount.getBalance()).isPositive())
//                ServiceFactory.getBean(LoanAcctChargeProvider.class).eventProvider.createEvent(ConstantDeclare.EVENT.LNMOPREACT,
//                        new FabAmount(lnsprefundaccount.getBalance()),
//                        null,
//                        oppNAcctInfo,
//                        new FundInvest(),
//                        ConstantDeclare.BRIEFCODE.YSQY,
//                        ctx, lnsprefundaccount.getCustomid(), "", lnsprefundaccount.getCusttype()
//                );
        }
        // 4、返回
        return lnsassistdyninfo;
    }
    
    /**
     * 预收户数据查询
     *
     * @param brc
     * @param customid
     * @param feetype
     * @return TblLnsassistdyninfo
     * @throws FabException
     */
    public static TblLnsassistdyninfo queryLnsassistdynInfoByCustomid(String brc, String customid,String feetype) throws FabException {

    		
        Map<String, Object> param = new HashMap<>();
        param.put("brc", brc);
        param.put("feetype", feetype);
        param.put("customid", customid);
        TblLnsassistdyninfo lnsassistdyninfo;
        try {
            // 1、查询辅助动态表
            lnsassistdyninfo = DbAccessUtil.queryForObject("Lnsassistdyninfo.selectByUk", param, TblLnsassistdyninfo.class);
        } catch (FabException e) {
            throw new FabException(e, "SPS103", "lnsassistdyninfo");
        }

        if (null == lnsassistdyninfo) {
        	return null;
        }

        return lnsassistdyninfo;
    }

    /**
     * 辅助动态表预收户开户
     *
     * @param brc
     * @param acctno
     * @param feetype
     * @param customid
     * @param custtype
     * @throws FabException
     */
    public static TblLnsassistdyninfo addPreaccountinfo(TranCtx ctx, String brc, String acctno, String feetype, String customid, String custtype) throws FabException {
        // 开户抛事件标记
        TblLnsassistdyninfo dyninfo = new TblLnsassistdyninfo();
        dyninfo.setBrc(brc);
        dyninfo.setAcctno(acctno);
        dyninfo.setFeetype(feetype);
        dyninfo.setCustomid(customid);
        dyninfo.setOrdnu(0);
        dyninfo.setCurrbal(0.00);
        dyninfo.setLastbal(0.00);
        dyninfo.setStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
        dyninfo.setCcy(ConstantDeclare.CCY.CCY_CNY_S);
        dyninfo.setCusttype(custtype);
        
//        // 查询贷款预收户登记簿：查询出非已经废弃的数据
//        TblLnsprefundaccount lnsprefundaccount = queryPrefundaccount(brc, "", customid, feetype);
//
//        if (null != lnsprefundaccount) {
//            eventFlag = true;
//            LoggerUtil.info("预收户已开户" + acctno);
//            // 提取贷款预收户登记簿数据到辅助动态表中
//            dyninfo.setCurrbal(lnsprefundaccount.getBalance());
//            dyninfo.setAcctno(lnsprefundaccount.getAcctno());
//            dyninfo.setCustomid(lnsprefundaccount.getCustomid());
//            dyninfo.setLastbal(0.00);
//            dyninfo.setPretrandate(ctx.getTranDate());
//            dyninfo.setCusttype(lnsprefundaccount.getCusttype());
////            dyninfo.setReserv6(lnsprefundaccount.getName());
//            dyninfo.setCreatetime(lnsprefundaccount.getOpendate());
//            // 给即将迁移的老数据做标记
//            discareDate(brc,feetype,lnsprefundaccount.getAcctno(),lnsprefundaccount.getCustomid());
//
//        }
//        else {
            if (VarChecker.isEmpty(custtype))
                throw new FabException("LNS227", feetype, acctno);
//        }
            //判断客户号是否已经开立预收户
             TblLnsassistdyninfo preaccountInfo_O=queryLnsassistdynInfoByCustomid(ctx.getBrc(), customid,feetype);
    		if(null!=preaccountInfo_O ){
    			//已经开户
    			LoggerUtil.info("预收户已开户" + customid);
    			return dyninfo;	
    		}
        try {
            DbAccessUtil.execute("Lnsassistdyninfo.insert", dyninfo);
        } catch (FabSqlException e) {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                LoggerUtil.info("预收户已开户" + acctno);
            } else
                throw new FabException(e, "SPS100", "lnsassistdyninfo");
        }

//        if (eventFlag) {
//            //            LnsAcctInfo lnsAcctInfoPre = new LnsAcctInfo(lnsprefundaccount.getCustomid(), ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
//            LnsAcctInfo oppNAcctInfo = new LnsAcctInfo("", ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT, "", new FabCurrency());
//
//            if (new FabAmount(dyninfo.getCurrbal()).isPositive())
//                ServiceFactory.getBean(LoanAcctChargeProvider.class).eventProvider.createEvent(ConstantDeclare.EVENT.LNMOPREACT,
//                        new FabAmount(lnsprefundaccount.getBalance()),
//                        null,
//                        oppNAcctInfo,
//                        new FundInvest(),
//                        ConstantDeclare.BRIEFCODE.YSQY,
//                        ctx, lnsprefundaccount.getCustomid(), "", lnsprefundaccount.getCusttype()
//                );
//        }

        return dyninfo;
    }

    /**
     *将即将迁移到辅助账户动态表的原贷款预收登记簿的数据做废弃处理：status改成CANCEL
     */
    private static void discareDate(String brc, String accsrccode, String acctno, String customid) throws FabException {

        Map<String, Object> param = new HashMap<>();
        param.put("brc", brc);
        param.put("acctno", acctno);
        param.put("accsrccode", accsrccode);
        param.put("customid", customid);
        try {
            DbAccessUtil.execute("Lnsprefundaccount.updateStatusByUk", param);
        } catch (FabException e) {
            throw new FabException(e, "SPS102", "Lnsprefundaccount");
        }

    }
    
    /**
     * 更新辅助动态表并且插入一条辅助账户明细表
     */
    public static TblLnsassistdyninfo updateLnsAssistDynInfo(TranCtx ctx, String brc, String acctno, String feetype, FabAmount amount, String cdflag, String custtype,String customId,String feeFunCode,String acceptCourt, String lawFirm,String tunnelData) throws FabException {


        //更新辅助动态表数据
        TblLnsassistdyninfo dyninfo = getAndUpdateLnsassistdyninfoFC(ctx, brc, acctno, feetype, amount, cdflag,feeFunCode);

        while (null == dyninfo) {

            //2、将数据插入到辅助动态表中
            TblLnsassistdyninfo lnsassistdyninfo = new TblLnsassistdyninfo();
            lnsassistdyninfo.setBrc(brc);
            lnsassistdyninfo.setAcctno(acctno);
            lnsassistdyninfo.setCustomid(customId);
            lnsassistdyninfo.setFeetype(feetype);
            lnsassistdyninfo.setOrdnu(0);
            lnsassistdyninfo.setStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
            lnsassistdyninfo.setCurrbal(0.00);
            lnsassistdyninfo.setLastbal(0.00);
            lnsassistdyninfo.setCcy(ConstantDeclare.CCY.CCY_CNY_S);
            lnsassistdyninfo.setReserv1(feeFunCode);
            lnsassistdyninfo.setReserv3(acceptCourt);
            lnsassistdyninfo.setReserv2(lawFirm);
            lnsassistdyninfo.setTunneldata(tunnelData);
            lnsassistdyninfo.setReserv4(amount.toString());

            if (VarChecker.isEmpty(custtype))
            	throw new FabException("LNS227", feetype, acctno);

            lnsassistdyninfo.setCusttype("2".equals(custtype)?"COMPANY":"PERSON");

            try {
                DbAccessUtil.execute("Lnsassistdyninfo.insert", lnsassistdyninfo);
            } catch (FabSqlException e) {
                if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                    LoggerUtil.info("该法催户已开户" + acctno);
                }else {
                    throw new FabException(e, "SPS100", "Lnsassistdyninfo");
                }
            }

            // 重新执行更新方法
            dyninfo = getAndUpdateLnsassistdyninfoFC(ctx, brc, acctno, feetype, amount, cdflag,feeFunCode);

        }

        //向辅助动态明细表中插入一条数据
        TblLnsassistlist assistlist = new TblLnsassistlist();
        assistlist.setAcctno(acctno);
        assistlist.setCustomid(dyninfo.getCustomid());
        assistlist.setBrc(brc);
        assistlist.setFeetype(feetype);
        assistlist.setBal(dyninfo.getCurrbal());
        assistlist.setAcctstat(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
        assistlist.setCcy(ConstantDeclare.CCY.CCY_CNY_S);
        assistlist.setCdflag(cdflag);
        assistlist.setSerseqno(ctx.getSerSeqNo());
        assistlist.setTrandate(ctx.getTranDate());
        assistlist.setOrdnu(dyninfo.getOrdnu());
        assistlist.setTrancode(ctx.getTranCode());
        assistlist.setTxnseq(GuidUtil.incrSubSeq(ConstantDeclare.SEQUENCE.TXNSUBSEQ));
        assistlist.setTranamt(amount.getVal());

        //通过ctx的tunneldate将借据号传过来
        assistlist.setReserv6(ctx.getTunnelData());


        if (new FabAmount(assistlist.getTranamt()).isPositive()) {
        	try {
        		DbAccessUtil.execute("Lnsassistlist.insert", assistlist);
        	} catch (FabException e) {
        		throw new FabException(e, "SPS100", "Lnsassistlist");
        	}
        }


        return dyninfo;

    }
    
    /**
     * 更新辅助动态表中的数据(法催)
     *
     * @param ctx
     * @param acctno
     * @param feetype
     * @param amount
     * @param cdflag
     * @return
     * @throws FabException
     */
    private static TblLnsassistdyninfo getAndUpdateLnsassistdyninfoFC(TranCtx ctx, String brc, String acctno, String feetype, FabAmount amount, String cdflag,String feeFunCode) throws FabException {
        Map<String, Object> param = new HashMap<>();
        param.put("brc", brc);
        param.put("acctno", acctno);
        param.put("feetype", feetype);
        param.put("cdflag", cdflag);
        param.put("currbal", amount.getVal());
        param.put("trandate", ctx.getTranDate());
        param.put("feefuncode", feeFunCode);

        TblLnsassistdyninfo dyninfo;
        try {
            dyninfo = DbAccessUtil.queryForObject("Lnsassistdyninfo.updateByExistFC", param, TblLnsassistdyninfo.class);
        } catch (FabException e) {
            throw new FabException(e, "SPS102", "lnsassistdyninfo");
        }
        return dyninfo;
    }

}
