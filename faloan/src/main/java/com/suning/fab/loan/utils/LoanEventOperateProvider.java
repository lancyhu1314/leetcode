package com.suning.fab.loan.utils;

import java.lang.reflect.Method;
import java.util.*;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.tup4j.account.AcctOperationMemo;
import com.suning.fab.tup4j.utils.*;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Component;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.accounting.LnsEvent;
import com.suning.fab.loan.accounting.LnsEventMsg;
import com.suning.fab.loan.domain.TblLnseventreg;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.tup4j.account.AcctInfo;
import com.suning.fab.tup4j.accounting.Event;
import com.suning.fab.tup4j.amount.Amount;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;

@Component
public class LoanEventOperateProvider implements EventOperateProvider {


	public LoanEventOperateProvider() {
		/*目前方法保留为空*/
	}

	public void createEvent(String eventCode, Amount tranAmt,
			AcctInfo acctInfo, AcctInfo oppAcctInfo, FundInvest fundInvest,
			String briefCode, TranCtx ctx, String... reserves)
			throws FabException {
		//核销的账户进行事件转换
		boolean ignoreEvent=LoanAcctOperateProvider.changeEventLoanStat(eventCode,acctInfo,oppAcctInfo);
		if(ignoreEvent){
			return;
		}
		LnsEvent ev = new LnsEvent(eventCode, tranAmt, acctInfo, oppAcctInfo,
				fundInvest, briefCode);
		ev.setCtx(ctx);
		for (int i = 0; i < reserves.length; i++) {
			try {
				Method m = ev.getClass().getMethod("setReserv" + (i + 1),
						String.class);
				m.invoke(ev, reserves[i]);
			} catch (Exception e) {
				throw new FabException(e,"999999","创建事件错");
			}
		}
		// event seq从1开始
		ev.setTxnSeq(ctx.eventCount() + 1);
		ctx.addEvent(ev);

	}
	public void createEvent(String eventCode, Amount tranAmt,
			AcctInfo acctInfo, AcctInfo oppAcctInfo, FundInvest fundInvest,
			String briefCode, TranCtx ctx, List<FabAmount> amtList, String... reserves)
			throws FabException {
		//核销的账户进行事件转换
		boolean ignoreEvent=LoanAcctOperateProvider.changeEventLoanStat(eventCode,acctInfo,oppAcctInfo);
		if(ignoreEvent){
			return;
		}
		LnsEvent ev = new LnsEvent(eventCode, tranAmt, acctInfo, oppAcctInfo,
				fundInvest, briefCode,amtList);
		ev.setCtx(ctx);

		ev.setAmtList(amtList);
		
		for (int i = 0; i < reserves.length; i++) {
			try {
				Method m = ev.getClass().getMethod("setReserv" + (i + 1),
						String.class);
				m.invoke(ev, reserves[i]);
			} catch (Exception e) {
				throw new FabException(e,"999999","创建事件错");
			}
		}
		// event seq从1开始
		ev.setTxnSeq(ctx.eventCount() + 1);
		ctx.addEvent(ev);

	}
	public void createEvent(String eventCode, Amount tranAmt,
			AcctInfo acctInfo, AcctInfo oppAcctInfo, FundInvest fundInvest,
			String briefCode, TranCtx ctx, List<FabAmount> amtList,  String billTranDate,
			 Integer billSerSeqno,
			 Integer billTxSeq,String... reserves)
			throws FabException {
		//核销的账户进行事件转换
		boolean ignoreEvent=LoanAcctOperateProvider.changeEventLoanStat(eventCode,acctInfo,oppAcctInfo);
		if(ignoreEvent){
			return;
		}
		 Map<String,Object> param = new HashMap<String,Object>();
		//查询账单对应的核销核销，组装对应amtList队列，其中如果表外3类型，事件amt2赋值还款金额
		param.put("trandate", billTranDate);
        param.put("serseqno", billSerSeqno);
        param.put("txseq", billTxSeq);
        Map<String, Object> cancelFlag = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsbill_cancelflag", param);
		if (null != cancelFlag && "3".equals(cancelFlag.get("cancelflag").toString().trim())) {
			if (null != amtList && amtList.size() > 1) {
				amtList.set(1, (new FabAmount(tranAmt.getVal().doubleValue())));
			} else if (null != amtList && amtList.size() == 1) {
				amtList.add(new FabAmount(tranAmt.getVal().doubleValue()));
			} else if (null == amtList) {
				List<FabAmount> amtList1 = new ArrayList<FabAmount>();
				amtList1.add(TaxUtil.calcVAT(new FabAmount(tranAmt.getVal())));
				amtList1.add(new FabAmount(tranAmt.getVal().doubleValue()));
				amtList = amtList1;
			}
		}
		else if (null != cancelFlag && "2".equals(cancelFlag.get("cancelflag").toString().trim())) {
			if (null != amtList && amtList.size() > 1) {
				amtList.set(1, (new FabAmount(tranAmt.getVal().doubleValue())));
			} else if (null != amtList && amtList.size() == 1) {
				amtList.add(new FabAmount(0.00));
				amtList.add(new FabAmount(tranAmt.getVal().doubleValue()));
			} else if (null == amtList) {
				List<FabAmount> amtList1 = new ArrayList<FabAmount>();
				amtList1.add(TaxUtil.calcVAT(new FabAmount(tranAmt.getVal())));
				amtList1.add(new FabAmount(0.00));
				amtList1.add(new FabAmount(tranAmt.getVal().doubleValue()));
				amtList = amtList1;
			}
		}

//		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(ThreadLocalUtil.get("loanStat", ""))) {
//
//		}
//		else{
//			if (null != cancelFlag && "3".equals(cancelFlag.get("cancelflag").toString().trim())) {
//				if (null != amtList && amtList.size() > 1) {
//					amtList.set(1, (new FabAmount(tranAmt.getVal().doubleValue())));
//				} else if (null != amtList && amtList.size() == 1) {
//					amtList.add(new FabAmount(tranAmt.getVal().doubleValue()));
//				} else if (null == amtList) {
//					List<FabAmount> amtList1 = new ArrayList<FabAmount>();
//					amtList1.add(new FabAmount(0.00));
//					amtList1.add(new FabAmount(tranAmt.getVal().doubleValue()));
//					amtList = amtList1;
//				}
//			}
//		}
		LnsEvent ev = new LnsEvent(eventCode, tranAmt, acctInfo, oppAcctInfo,
				fundInvest, briefCode,amtList,billTranDate,billSerSeqno,billTxSeq);
		ev.setCtx(ctx);
		for (int i = 0; i < reserves.length; i++) {
			try {
				Method m = ev.getClass().getMethod("setReserv" + (i + 1),
						String.class);
				m.invoke(ev, reserves[i]);
			} catch (Exception e) {
				throw new FabException(e,"999999","创建事件错");
			}
		}
		// event seq从1开始
		ev.setTxnSeq(ctx.eventCount() + 1);
		ctx.addEvent(ev);
		if(!(null!=cancelFlag && VarChecker.asList("3","2").contains(cancelFlag.get("cancelflag").toString().trim()))){
			cloneEventInHx(ev);//核销还款进行 转回,"3"标识原核销后还款根据核销日期判断部分的逻辑去除 at 20220808
		}
	}

	@Override
	public void serialize(Event ev) throws FabException {
		TblLnseventreg eventReg = new TblLnseventreg();
		
		if(!(ev instanceof LnsEvent))
		{
			throw new FabException("999999","登记事件错");
		}
		LnsEvent event = (LnsEvent) ev;

		MapperUtil.map(event.getCtx(), eventReg);
		MapperUtil.map(event, eventReg);
		LnsAcctInfo acct = AcctInfoProvider
				.readAcctInfo(event.getAcctInfo(),event.getCtx().getBrc());
		LnsAcctInfo oppacct = AcctInfoProvider.readAcctInfo(event
				.getOppAcctInfo(),event.getCtx().getBrc());
		eventReg.setReceiptno(acct.getReceiptNo());
		eventReg.setAcctstat(acct.getAccSrcCod());
		eventReg.setCcy(event.getTranAmt().getCurrency().getCcy());
		//当是还款渠道事件  并且过预收时
		if(ConstantDeclare.EVENT.PAYCHANNEL.equals(event.getEventCode())&&"2".equals(event.getCtx().getRequestDict("repayChannel"))&&event.getOppAcctInfo()!=null&& VarChecker.asList(ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT).contains(((LnsAcctInfo)event.getOppAcctInfo()).getBillType())&&((LnsAcctInfo)event.getOppAcctInfo()).getCustType()!=null) {
			eventReg.setCusttype(((LnsAcctInfo)event.getOppAcctInfo()).getCustType());
			eventReg.setReceiptno(((LnsAcctInfo)event.getOppAcctInfo()).getReceiptNo());
		}else{
			eventReg.setCusttype(acct.getCustType());
		}
		eventReg.setMerchantno(acct.getMerchantNo().trim());
		eventReg.setPrdcode(acct.getPrdCode());
		eventReg.setBriefcode(event.getBriefCode());
		eventReg.setTranbrief(event.getBriefText());
		eventReg.setToreceiptno(oppacct.getReceiptNo());
		eventReg.setToacctstat(oppacct.getAccSrcCod());
		eventReg.setTocusttype(oppacct.getCustType());
		eventReg.setTomerchantno(oppacct.getMerchantNo().trim());
		eventReg.setToprdcode(oppacct.getPrdCode());
		
		/* FundChannel放入隧道字段 */
		Map<String,String> jsonStr = new HashMap<String,String>();
		
		
		if( null == event.getFundInvestInfo() )
		{
			eventReg.setTunneldata("");
		}
		else
		{
			if( null != event.getFundInvestInfo().getFundChannel() &&
				!event.getFundInvestInfo().getFundChannel().isEmpty() )
			{
				jsonStr.put("reserv5", event.getFundInvestInfo().getFundChannel());
				eventReg.setTunneldata(JsonTransfer.ToJson(jsonStr));
			}
			
		}
		//CustomName统一放至备用5方便发送事件时转换处理
		//转json存入Tunneldata字段 
		if(!VarChecker.isEmpty(eventReg.getReserv5())){
			if( Arrays.asList(ConstantDeclare.EVENT.NRECPREACT,ConstantDeclare.EVENT.NBACPREACT,ConstantDeclare.EVENT.PAYCHANNEL).contains(eventReg.getEventcode())){
//				jsonStr.put("reserv3", eventReg.getReserv5());
				addTunnelData(eventReg,eventReg.getReserv5());//隧道字段进行合并
				//eventReg.setTunneldata( eventReg.getReserv5() );
				eventReg.setReserv5("");
			}else if( Arrays.asList("161001","161002","161003").contains(eventReg.getTrancode())
					&& !"LNDKHKYSJK".equals(eventReg.getEventcode())){
				eventReg.setTunneldata(eventReg.getReserv5());
				eventReg.setCusttype(eventReg.getReserv3());
				eventReg.setMerchantno(eventReg.getReserv1());
				eventReg.setReceiptno(event.getAcctInfo().getAcctNo());
				FaloanJson tunnelMap = new FaloanJson(JSONObject.parseObject(eventReg.getReserv5()));
				if("161003".equals(eventReg.getTrancode())){
					if(null!=tunnelMap.getString("repayChannel"))
						eventReg.setChanneltype(tunnelMap.getString("repayChannel"));
					if(null!=tunnelMap.getString("outSerialNo"))
						eventReg.setOutserialno(tunnelMap.getString("outSerialNo"));

				}
				if("161001".equals(eventReg.getTrancode())){
					if(null!=tunnelMap.getString("payorderNo"))
						eventReg.setOutserialno(tunnelMap.getString("payorderNo"));
				}
				eventReg.setReserv5("");
				eventReg.setReserv1("");
				eventReg.setReserv3("");
				if(!VarChecker.isEmpty(eventReg.getAcctstat()) && eventReg.getAcctstat().length()>3 && "SSF".equals(eventReg.getAcctstat().substring(0,3))){
					eventReg.setPrdcode("5010001");
					eventReg.setProductcode("5010001");
				}else if (!VarChecker.isEmpty(eventReg.getAcctstat()) && eventReg.getAcctstat().length()>3 && "LSF".equals(eventReg.getAcctstat().substring(0,3))){
					eventReg.setPrdcode("5010002");
					eventReg.setProductcode("5010002");
				}
				if(!VarChecker.isEmpty(eventReg.getToacctstat()) && eventReg.getToacctstat().length()>3 && "SSF".equals(eventReg.getToacctstat().substring(0,3))){
					eventReg.setToprdcode("5010001");
					eventReg.setTocusttype(eventReg.getCusttype());
					eventReg.setTomerchantno(eventReg.getMerchantno());
					eventReg.setToreceiptno(event.getOppAcctInfo().getAcctNo());
				}else if (!VarChecker.isEmpty(eventReg.getToacctstat()) && eventReg.getToacctstat().length()>3 && "LSF".equals(eventReg.getToacctstat().substring(0,3))){
					eventReg.setToprdcode("5010002");
					eventReg.setTocusttype(eventReg.getCusttype());
					eventReg.setTomerchantno(eventReg.getMerchantno());
					eventReg.setToreceiptno(event.getOppAcctInfo().getAcctNo());
				}
			}
			else{
				jsonStr.put("reserv3", eventReg.getReserv5());
				eventReg.setReserv5("");
				eventReg.setTunneldata(JsonTransfer.ToJson(jsonStr));
			}
		}
		if("priorType".equals(eventReg.getReserv3())){
			eventReg.setReserv3("");
			eventReg.setSubtrancode(event.getCtx().getRequestDict("priorType")==null?"":event.getCtx().getRequestDict("priorType").toString());
		}
		// lns509 reserv3
		if("chtype".equals(eventReg.getReserv3())){
			eventReg.setReserv3("");
			eventReg.setChtype("03");
		}
		// lns212 lns211 lns213 lns522 reserv4
		if("chtype".equals(eventReg.getReserv4())){
			eventReg.setReserv4("");
			eventReg.setChtype("03");
		}
		if(!"161003".equals(eventReg.getTrancode()))
			eventReg.setFundchannel("");
		
	/*	if (!VarChecker.isEmpty(event.getFundInvestInfo()))
		{
			eventReg.setInvestee(event.getFundInvestInfo().getInvestee());   //接收投资者
			eventReg.setInvestmode(event.getFundInvestInfo().getInvestMode()); //投放模式：保理、消费贷
			eventReg.setChanneltype(event.getFundInvestInfo().getChannelType());//放款渠道      1-银行   2-易付宝  3-任性贷
			eventReg.setFundchannel(event.getFundInvestInfo().getFundChannel());//资金通道    sap银行科目编号/易付宝总账科目
			eventReg.setOutserialno(event.getFundInvestInfo().getOutSerialNo());//外部流水单号：银行资金流水号/易付宝交易单号
		}*/

		eventReg.setSendflag(ConstantDeclare.SENDFLAG.PENDIND);
		eventReg.setSendnum(0);
		

		
		
		if (event.getAmtList() != null)
		{
			for (int i = 0; i < event.getAmtList().size(); i++) {
				try {
					Method m = eventReg.getClass().getMethod("setAmt" + (i + 1),
							Double.class);
					m.invoke(eventReg, event.getAmtList().get(i).getVal());
				} catch (Exception e) {
					
					throw new FabException(e,"999999","写事件错");
				}
			}
		}
		//20190619 代客投保  brc 用子机构号
		if(VarChecker.asList(ConstantDeclare.EVENT.CINSURANCE,ConstantDeclare.EVENT.BINSURANCE).contains(eventReg.getEventcode())&&!VarChecker.isEmpty(eventReg.getReserv2())){
			eventReg.setBrc(eventReg.getReserv2());
			eventReg.setReserv2("");
		}

		DbAccessUtil.execute("Lnseventreg.insert", eventReg);
		LnsEventMsg x = LoanEventMsgHelper.genEventMsg(eventReg);
		String groupId = event.getCtx().getSerSeqNo().toString() + event.getCtx().getTranDate();
		HashMap<String, Object> common = new HashMap<String, Object>();
		common.put("tranDate", event.getCtx().getTranDate());
		common.put("serSeqNo", event.getCtx().getSerSeqNo());
		common.put("channelId", "65");
		KafkaHelper.pending(ConstantDeclare.KAFKA.TOPIC.EVENT, groupId, x, common);

	}

	/**
	 * 向事件登记簿隧道字段中添加隧道字段
	 * @param eventReg 原事件
	 * @param tunnelData 隧道字段
	 */
	public void addTunnelData(TblLnseventreg eventReg,String tunnelData){
		String oldTunnelData=eventReg.getTunneldata();
		//原有和添加的都是json格式
		if(!VarChecker.isEmpty(tunnelData)&&!VarChecker.isEmpty(oldTunnelData)&&oldTunnelData.contains("{")&&tunnelData.contains("{")){
			Map oldTunnelMap= (Map)JSONObject.parse(oldTunnelData);
			Map insertTunnelMap=(Map)JSONObject.parse(tunnelData);
			oldTunnelMap.putAll(insertTunnelMap);//合并json隧道字段
			eventReg.setTunneldata(JSONObject.toJSONString(oldTunnelMap));
		}else{
			//走原有的逻辑
			eventReg.setTunneldata(tunnelData);
		}
	}

	/**
	 * 核销根据还款事件生成对应的转回事件
	 * @param ev
	 */
	public void cloneEventInHx(LnsEvent ev){
		AcctInfo acctInfo=ev.getAcctInfo();
		String billType=acctInfo.getAccSrcCod().split("\\.")[0];
		//如果是贷款是核销状态  并且是 还款本息  罚息、利息减免 需要添加核销转回事件
		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION.equals(ThreadLocalUtil.get("loanStat", ""))&&
				!("true".equals(ThreadLocalUtil.get(ConstantDeclare.PARACONFIG.IGNOREOFFDINT, "")) &&
						ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(billType))&&
				Arrays.asList(ConstantDeclare.EVENT.CRDTREPYMT,ConstantDeclare.EVENT.REDUDEFINT,ConstantDeclare.EVENT.REDUCENINT).contains(ev.getEventCode())){
			List<FabAmount> amtList=ev.getAmtList();
			String billTranDate=ev.getBillTranDate();
			TranCtx ctx=ev.getCtx();
			if(billType.equals("PRIN")||
					CalendarUtil.beforeAlsoEqual(billTranDate,ThreadLocalUtil.get(ConstantDeclare.BASICINFOEXKEY.HXRQ,"1900-01-01"))){
				LnsEvent copyEvent= ev.clone();
				copyEvent.setEventCode(ConstantDeclare.EVENT.LNCNFINOFF);
				copyEvent.setBriefCode(ConstantDeclare.BRIEFCODE.HXZH);
				AcctOperationMemo memo = AccountOperationMemoBuilder.generic(ConstantDeclare.BRIEFCODE.HXZH);
				copyEvent.setBriefText(memo.getBreifText());
				copyEvent.setTxnSeq(ctx.eventCount()+1);
				ev.getCtx().addEvent(copyEvent);
			}
			if(Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(billType)&&
					CalendarUtil.after(billTranDate,ThreadLocalUtil.get(ConstantDeclare.BASICINFOEXKEY.HXRQ,"1900-01-01"))){
				if(amtList==null){
					//还款本息amt1为0.00
					amtList=new ArrayList();
					amtList.add(new FabAmount(0.00));
				}
				amtList.add((FabAmount)ev.getTranAmt());
				ev.setAmtList(amtList);
			}
		}
	}
}
