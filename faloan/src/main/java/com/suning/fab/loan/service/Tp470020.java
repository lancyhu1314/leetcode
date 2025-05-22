package com.suning.fab.loan.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.bo.PreRepay;
import com.suning.fab.loan.utils.ProvisionRepayQuery;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉： 批量预约还款查询
 *
 * @Author 
 * @Date Created in 16:48 2019/04/17
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-batchProvisionRepayQuery")
public class Tp470020 extends ServiceTemplate {
	
    public Tp470020(){
        needSerSeqNo=false;
    }
    
    //出参
	String 		pkgList1 = "";
	Integer		totalLine=0;

	
    @Override
    protected void run() throws Exception {
    LoggerUtil.info("470020  start");
    //入参
	ListMap 	pkgList = ctx.getRequestDict("pkgList");
	List<PreRepay> redisList = new ArrayList<PreRepay>();

	List<Future<PreRepay>> futureList = new ArrayList<Future<PreRepay>>();
	if(pkgList != null && pkgList.size() == 1){
		
		for (final PubDict pkg:pkgList.getLoopmsg()) {

			if( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "acctNo")) ||
				VarChecker.isEmpty(PubDict.getRequestDict(pkg, "enCode")) )
			{
				throw new FabException("LNS021");
			}
			
			String acctNo = PubDict.getRequestDict(pkg, "acctNo").toString();
			String brc = PubDict.getRequestDict(pkg, "enCode").toString();
			
			//2020-05-29任性付融合提前结清手续费
			FabRate feeRate = new FabRate(0.00);
			if( !VarChecker.isEmpty(PubDict.getRequestDict(pkg, "cleanFeeRate")) )
			{
				feeRate = new FabRate(PubDict.getRequestDict(pkg, "cleanFeeRate").toString());
			}

			redisList.add(ProvisionRepayQuery.dealQueryList(acctNo, brc, feeRate, ctx.getTranDate(), ctx.getSerSeqNo()));
		}
		
		totalLine = redisList.size();
		
		if( totalLine == pkgList.size())
			pkgList1 = JsonTransfer.ToJson(redisList);
		else
			throw new FabException("LNS021");
	}
	
	
	if(pkgList != null && pkgList.size() > 1){
		
		for (final PubDict pkg:pkgList.getLoopmsg()) {

			if( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "acctNo")) ||
				VarChecker.isEmpty(PubDict.getRequestDict(pkg, "enCode")) )
			{
				throw new FabException("LNS021");
			}
			
			String acctNo = PubDict.getRequestDict(pkg, "acctNo").toString();
			String brc = PubDict.getRequestDict(pkg, "enCode").toString();
			
			//2020-05-29任性付融合提前结清手续费
			FabRate feeRate = new FabRate(0.00);
			if( !VarChecker.isEmpty(PubDict.getRequestDict(pkg, "cleanFeeRate")) )
			{
				feeRate = new FabRate(PubDict.getRequestDict(pkg, "cleanFeeRate").toString());
			}

			futureList.add(ProvisionRepayQuery.execute(acctNo, brc, feeRate, ctx.getTranDate(), ctx.getSerSeqNo()));
		}
		



		for(Future<PreRepay> future :futureList){
			try{
				redisList.add(future.get(4000, TimeUnit.MILLISECONDS));
			}catch(InterruptedException e){
				future.cancel(true);
				LoggerUtil.info("InterruptedException:{}",e);
				throw new FabException("LNS154");
			}catch(ExecutionException e){
				future.cancel(true);
				LoggerUtil.error("ExecutionException:{}",e);
				throw new FabException("LNS155");
			}catch(TimeoutException e){
				future.cancel(true);
				LoggerUtil.error("TimeoutException:{}",e);
				throw new FabException("LNS156");
			}
		}
    	
		
//		Thread.sleep(60000);
		
		totalLine = redisList.size();
		
		if( totalLine == pkgList.size())
			pkgList1 = JsonTransfer.ToJson(redisList);
		else
			throw new FabException("LNS021");
	}

    LoggerUtil.info("470020  end");
    }
    
	@Override
    protected void special() throws Exception {

    }
}
