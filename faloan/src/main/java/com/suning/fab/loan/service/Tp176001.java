package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns121;
import com.suning.fab.loan.workunit.Lns216;
import com.suning.fab.loan.workunit.Lns217;
import com.suning.fab.loan.workunit.Lns257;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*@see       预收账户充退
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-advanceAccountRefund")
public class Tp176001 extends ServiceTemplate {

	@Autowired
    Lns216 lns216;

    @Autowired
    Lns217 lns217;
    @Autowired
    Lns121 lns121;//考核数据校验 入库
    @Autowired
    Lns257 lns257;
    public Tp176001() {
        needSerSeqNo=true;
    }
	@Override
	protected void run() throws Exception {
		//预收账户冲退
        //2-赔付户（赔付时开户并充值）
        //3-保费户
        if(!VarChecker.isEmpty( ctx.getRequestDict("depositType"))
                &&VarChecker.asList("2","3").contains( ctx.getRequestDict("depositType").toString())) {
            trigger(lns257);
        }else {
            trigger(lns216);
        }
        //防止幂等报错被拦截
        trigger(lns121);
		FabAmount amt = ctx.getRequestDict("amt");
		ListMap pkgList = ctx.getRequestDict("pkgList");
		//用户累加循环金额
		FabAmount sumAmt = new FabAmount();
		//遍历循环报文
		if(pkgList != null && pkgList.size() > 0){
			for (PubDict pkg:pkgList.getLoopmsg()) {
				String debtCompany = PubDict.getRequestDict(pkg, "debtCompany");
				FabAmount debtAmt = PubDict.getRequestDict(pkg, "debtAmt");
				
				Map<String, Object> param = new HashMap<String,Object>();
				param.put("debtCompany", debtCompany);
				param.put("debtAmt", debtAmt);
				
				//债务公司预收充退
				trigger(lns217, "DEFAULT", param);
				//累加循环报文金额
				sumAmt.selfAdd(debtAmt);
			}
			//判断预收金额和保理总金额是否合法
			if(sumAmt.selfSub(amt).isPositive()){
				LoggerUtil.debug("充值充退累积金额与总金额校验非法:" + sumAmt + "|" + amt);
				throw new FabException("LNS028");
			}
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
