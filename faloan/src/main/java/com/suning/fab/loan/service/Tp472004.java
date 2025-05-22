/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Tp472002.java
 * Author:   16071579
 * Date:     2017年5月25日 下午3:31:44
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉：非标放款撤销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-nonWriteOff")
public class Tp472004 extends ServiceTemplate {
	@Autowired Lns103 lns103; // 本金户销户
	@Autowired Lns108 lns108;
	@Autowired Lns110 lns110; // 本金户销户
	@Autowired Lns205 lns205; // 扣息税金冲销
	@Autowired Lns206 lns206; // 放款渠道冲销
	@Autowired Lns207 lns207; // 扣息冲销
	@Autowired Lns208 lns208; // 放款冲销
	@Autowired Lns209 lns209; // 利息计提冲销
	@Autowired Lns232 lns232; // 非标利息计提冲销
	@Autowired Lns223 lns223; // 结息冲销
	@Autowired Lns224 lns224; // 结息税金冲销
	@Autowired Lns109 lns109; //逾期和还款校验
	@Autowired Lns233 lns233;
	@Autowired Lns234 lns234;
	@Autowired Lns240 lns240;  //租赁摊销冲销
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	public Tp472004() {
		needSerSeqNo = true;
	}

	@Override
	protected void run() throws Exception {

		// 1、查看事件登记薄中是否有数据，如果有则直接执行原来的逻辑，如果无则执行第二步
		// 2、查询幂等登记薄中是否有数据，如果有则判断是否满足冲销的条件，将步骤三
		// 3、查看是否有过还款记录以及有木器账单，如果都没有则进行以下步骤进行冲销事件
		// 4、 查看有没有计提事件，有则冲销，并登记事件登记薄
		// 5、 反向操作开户，放款，放款渠道冲销三个事件，放款和放款渠道需要传相应的金额
		if(VarChecker.isEmpty(ctx.getRequestDict("errDate"))){
			throw new FabException("LNS055","errDate");
		}
		//半年前的不能冲销，冲销走手工调账
		if( CalendarUtil.actualDaysBetween(ctx.getRequestDict("errDate").toString(),ctx.getTranDate()) > 60)
			throw new FabException("LNS230");

		trigger(lns108); //lns108做幂等处理
		lns121.setAcctNo(lns108.getLnsinterface().getAcctno());
		trigger(lns121);
		Map<Integer, String> acctMap = lns108.getAcctList();
		int	curTerm = lns108.getCurTerm();

		if(0 == curTerm)
		{	
			Map<String, Object> param1 = new HashMap<String, Object>();
			param1.put("acctNo", lns108.getReceiptNo());//新系统借据号和贷款帐号是一样的，所以此处直接使用借据号

			trigger(lns109, "DEFAULT", param1);//做冲销合法性校验
			trigger(lns240);// 租赁摊销冲销

			lns209.setTxseqno(lns240.getTxseqno());
			lns209.setLnsinterface(lns108.getLnsinterface());
			lns209.setLoanAgreement(lns240.getLoanAgreement());
			trigger(lns209);// 利息计提冲销

			lns223.setTxseqno(lns209.getTxseqno());//子序号
			lns223.setLoanAgreement(lns240.getLoanAgreement());
			lns223.setAcctNo(lns108.getLnsinterface().getAcctno());

			trigger(lns223, "DEFAULT", param1); // 结息冲销
			lns224.setAcctNo(lns223.getAcctNo());
			lns224.setOrgId(lns223.getOrgId());
			lns224.setChannelType(lns223.getChannelType());
			lns224.setOutSerialNo(lns223.getOutSerialNo());
			lns224.setReceiptNo(lns223.getReceiptNo());
			lns224.setTxseqno(lns223.getTxseqno());
			lns224.setLoanAgreement(lns223.getLoanAgreement());
			lns224.setLnstaxdetail(lns223.getLnstaxdetail());
			trigger(lns224);  // 结息税金冲销

			lns208.setLnsinterface(lns209.getLnsinterface());
			lns208.setTxseqno(lns224.getTxseqno());//子序号
			lns208.setLoanAgreement(lns240.getLoanAgreement());
			lns208.setLnsbasicinfo(lns223.getLnsbasicinfo());
			trigger(lns208);// 放款冲销

			lns206.setLnsbasicinfo(lns223.getLnsbasicinfo());
			lns206.setTxseqno(lns208.getTxseqno());//子序号
			lns206.setLoanAgreement(lns240.getLoanAgreement());
			trigger(lns206);// 放款渠道冲销

			lns103.setLnsinterface(lns209.getLnsinterface());
			lns103.setLoanAgreement(lns240.getLoanAgreement());
			lns103.setTxseqno(lns206.getTxseqno());//子序号
			trigger(lns103);// 本金户冲销
		}
		else
		{
			Map<String, Object> param232s = new HashMap<>();

			for (Map.Entry<Integer, String> entry : acctMap.entrySet())
			{

				param232s.put("acctNo", entry.getValue());//新系统借据号和贷款帐号是一样的，所以此处直接使用借据号
				param232s.put("receiptNo", lns108.getReceiptNo());//新系统借据号和贷款帐号是一样的，所以此处直接使用借据号
				param232s.put("curKey", entry.getKey());
				param232s.put("loanAgreement", null);
				trigger(lns109, "DEFAULT", param232s);//做冲销合法性校验
				
				trigger(lns232, "DEFAULT", param232s);// 利息计提冲销
				param232s.put("loanAgreement",lns232.getLoanAgreement());
 				param232s.put("txseqno",lns232.getTxseqno() );
				if(1 == entry.getKey())
				{	
					trigger(lns223, "DEFAULT", param232s); // 结息冲销
					param232s.put("loanAgreement",lns223.getLoanAgreement());
					param232s.put("txseqno",lns223.getTxseqno() );

					trigger(lns224, "DEFAULT", param232s);  // 结息税金冲销
					param232s.put("loanAgreement",lns224.getLoanAgreement());
					param232s.put("txseqno",lns224.getTxseqno() );
					param232s.put("lnsinterface",lns108.getLnsinterface() );

					trigger(lns234, "DEFAULT", param232s);// 放款渠道冲销
					param232s.put("loanAgreement",lns234.getLoanAgreement());
					param232s.put("txseqno",lns234.getTxseqno() );

				}
				trigger(lns233, "DEFAULT", param232s);// 放款冲销
				param232s.put("txseqno",lns233.getTxseqno() );
				param232s.put("loanAgreement",lns233.getLoanAgreement());

				trigger(lns110, "DEFAULT", param232s);// 本金户冲销
				param232s.put("txseqno",lns110.getTxseqno() );

			}
		}	
		//返回接口赋值
	}

	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS).contains(ctx.getRspCode())) {

			int	curTerm = lns108.getCurTerm();
			if(0 == curTerm)
			{	
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("acctno", lns108.getAcctNo());
				param.put("brc", ctx.getBrc());
				
				//删除该借据号部分数据，动态表和主文件表因为写事件的时候需要使用，所以此处在特殊流程中对这两张表进行删除
				try {// 删除开户对应的表信息，为防止在特殊流程是出现删除数据时异常，可以通过幂等的方式多次删除如下表数据
					param.put("serseqno", ctx.getRequestDict("errSerSeq"));
					param.put("trandate", ctx.getRequestDict("errDate"));
					
					//删除计提登记薄
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsprovision", param);
					DbAccessUtil.execute("Lnsprovision.deleteByAcctno",param);
					DbAccessUtil.execute("Lnsprovisiondtl.deleteByAcctno",param);
					/*删除幂等登记薄中该借据号交易码(trancode)为473004的记录*/
					//DbAccessUtil.execute("CUSTOMIZE.delete_lnsinterface", param);
//					/*删除交易明细表中该借据号的数据(lnsaccountlist)*/
//					DbAccessUtil.execute("CUSTOMIZE.delete_lnsaccountlist", param);
					/*删除交易明细表中该借据号的数据(lnsacclist)*/
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsacclist", param);
					/* 删除动态表中该借据号对应的余额信息记录 */
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsaccountdyninfo", param);
					/*删除还款计划表*/
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsrpyplan_off", param);
					/*删除账单表*/
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsbill_off", param);
					/* 删除主文件表中该借据号的数据 */
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsbasicinfo", param);
					/* 删除非标辅助表中该借据号的数据 */
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsnonstdplan", param);
					/* 删除试算登记簿中该借据号的数据 */
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsrentreg", param);
					//删除租赁摊销计划表
					DbAccessUtil.execute("CUSTOMIZE.delete_lnszlamortizeplan", param);
					/* 删除主文件拓展表中该借据号的数据 */
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsbasicinfoex", param);
					/* 删除主文件备份表中该借据号的数据 */
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsbasicinfoback", param);
					/* 删除税金表中该借据号的数据 */
					//DELETE FROM lnstaxdetail WHERE ACCTNO = :acctno AND brc = :brc
					DbAccessUtil.execute("AccountingMode.delete_lnstaxdetail", param);

					/* 删除幂等明细表中该借据号的数据 */
					DbAccessUtil.execute("AccountingMode.delete_lnsinterfaceex", param);

					/* 删除资金方登记簿中该借据号的数据*/
					//DELETE FROM lnsinvesteedetail WHERE ACCTNO = :acctno AND brc = :brc
					DbAccessUtil.execute("AccountingMode.delete_lnsinvesteedetail", param);
					/*删除主文件动态表LNSBASICINFODYN*/
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsbasicinfodyn", param);
				} catch (FabSqlException e) {
					/* 通过测试发现，此处异常并不会导致事件回滚，为防止上述删除异常，暂时现在幂等的时候在删除一下 */
					throw new FabException(e, "SPS101", "开户涉及");
					
				}
			}
			else
			{
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("acctno", lns108.getReceiptNo());
				param.put("brc", ctx.getBrc());
				param.put("serseqno", ctx.getRequestDict("errSerSeq"));
				param.put("trandate", ctx.getRequestDict("errDate"));
				/*删除幂等登记薄中该借据号交易码(trancode)为473004的记录*/
				//DbAccessUtil.execute("CUSTOMIZE.delete_lnsinterface", param);
				/* 删除非标辅助表中该借据号的数据 */
				DbAccessUtil.execute("CUSTOMIZE.delete_lnsnonstdplan", param);
				/* 删除试算登记簿中该借据号的数据 */
				DbAccessUtil.execute("CUSTOMIZE.delete_lnsrentreg", param);
				/* 删除试算登记簿中该借据号的数据 */
				DbAccessUtil.execute("CUSTOMIZE.delete_lnsrentreg", param);
				
				Map<Integer, String> acctMap = lns108.getAcctList();
				for(Map.Entry<Integer, String> entry : acctMap.entrySet())
				{	
					param.put("acctno", entry.getValue());
					param.put("brc", ctx.getBrc());
					
					//删除该借据号部分数据，动态表和主文件表因为写事件的时候需要使用，所以此处在特殊流程中对这两张表进行删除
					try {// 删除开户对应的表信息，为防止在特殊流程是出现删除数据时异常，可以通过幂等的方式多次删除如下表数据
						//删除计提登记薄
						DbAccessUtil.execute("CUSTOMIZE.delete_lnsprovision", param);
						DbAccessUtil.execute("Lnsprovision.deleteByAcctno",param);
						DbAccessUtil.execute("Lnsprovisiondtl.deleteByAcctno",param);
//						/*删除交易明细表中该借据号的数据(lnsaccountlist)*/
//						DbAccessUtil.execute("CUSTOMIZE.delete_lnsaccountlist", param);
						/*删除交易明细表中该借据号的数据(lnsacclist)*/
						DbAccessUtil.execute("CUSTOMIZE.delete_lnsacclist", param);
						/* 删除动态表中该借据号对应的余额信息记录 */
						DbAccessUtil.execute("CUSTOMIZE.delete_lnsaccountdyninfo", param);
						/*删除还款计划表*/
						DbAccessUtil.execute("CUSTOMIZE.delete_lnsrpyplan_off", param);
						/*删除账单表*/
						DbAccessUtil.execute("CUSTOMIZE.delete_lnsbill_off", param);
						/* 删除主文件表中该借据号的数据 */
						DbAccessUtil.execute("CUSTOMIZE.delete_lnsbasicinfo", param);
						/* 删除主文件拓展表中该借据号的数据 */
						DbAccessUtil.execute("CUSTOMIZE.delete_lnsbasicinfoex", param);
						/* 删除主文件备份表中该借据号的数据 */
						DbAccessUtil.execute("CUSTOMIZE.delete_lnsbasicinfoback", param);
						/* 删除税金表中该借据号的数据 */
						//DELETE FROM lnstaxdetail WHERE ACCTNO = :acctno AND brc = :brc
						DbAccessUtil.execute("AccountingMode.delete_lnstaxdetail", param);

						/* 删除幂等明细表中该借据号的数据 */
						DbAccessUtil.execute("AccountingMode.delete_lnsinterfaceex", param);

						/* 删除幂等明细表中该借据号的数据 根据trandate serseqno*/
						DbAccessUtil.execute("AccountingMode.delete_lnsinterfaceexk", param);

						/* 删除资金方登记簿中该借据号的数据*/
						//DELETE FROM lnsinvesteedetail WHERE ACCTNO = :acctno AND brc = :brc
						DbAccessUtil.execute("AccountingMode.delete_lnsinvesteedetail", param);
						/*删除主文件动态表LNSBASICINFODYN*/
						DbAccessUtil.execute("CUSTOMIZE.delete_lnsbasicinfodyn", param);
					} catch (FabSqlException e) {
						/* 通过测试发现，此处异常并不会导致事件回滚，为防止上述删除异常，暂时现在幂等的时候在删除一下 */
						throw new FabException(e, "SPS101", "开户涉及");
					}
				}
			}	
				
		} else if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY).contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		}
	}	
}
