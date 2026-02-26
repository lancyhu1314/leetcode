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
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.amount.FabAmount;
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
 * 〈功能详细描述〉：放款冲销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-loanWriteOff")
public class Tp472002 extends ServiceTemplate {

	@Autowired
	Lns103 lns103; // 本金户销户
	@Autowired
	Lns205 lns205; // 扣息税金冲销
	@Autowired
	Lns206 lns206; // 放款渠道冲销
	@Autowired
	Lns207 lns207; // 扣息冲销
	@Autowired
	Lns208 lns208; // 放款冲销
	@Autowired
	Lns209 lns209; // 利息计提冲销
	@Autowired
	Lns219 lns219; // 摊销冲销
	@Autowired
	Lns223 lns223; // 结息冲销
	@Autowired
	Lns224 lns224; // 结息税金冲销
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	public Tp472002() {
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
		
		// 新系统数据冲销
		// 开始调用放款冲销子交易
		trigger(lns219);// 摊销冲销
		lns121.setAcctNo(lns219.getTblLnsinterface().getAcctno());
		trigger(lns121) ;
		if("473007".equals(lns219.getTblLnsinterface().getTrancode()))
		{
			throw new FabException("LNS181");
		}
		lns209.setTxseqno(lns219.getTxseqno()); //子序号
		lns209.setLnsinterface(lns219.getTblLnsinterface());
		lns209.setLoanAgreement(lns219.getLoanAgreement());
		trigger(lns209);// 利息计提冲销

		lns223.setTxseqno(lns209.getTxseqno());//子序号
		lns223.setLoanAgreement(lns209.getLoanAgreement());
		lns223.setAcctNo(lns219.getTblLnsinterface().getAcctno());
		trigger(lns223); // 结息冲销
		trigger(lns224, "map224", lns223); // 结息税金冲销

		lns208.setLnsinterface(lns219.getTblLnsinterface());
		lns208.setTxseqno(lns224.getTxseqno());//子序号
		lns208.setLoanAgreement(lns223.getLoanAgreement());
		lns208.setLnsbasicinfo(lns223.getLnsbasicinfo());
		trigger(lns208);// 放款冲销

		lns207.setLnsbasicinfo(lns223.getLnsbasicinfo());
		lns207.setTxseqno(lns208.getTxseqno());//子序号
		lns207.setLoanAgreement(lns208.getLoanAgreement());
		trigger(lns207);// 扣息冲销

		lns206.setLnsbasicinfo(lns207.getLnsbasicinfo());
		lns206.setTxseqno(lns207.getTxseqno());//子序号
		lns206.setLoanAgreement(lns207.getLoanAgreement());
		trigger(lns206);// 放款渠道冲销

		lns205.setLoanAgreement(lns206.getLoanAgreement());
		lns205.setTxseqno(lns206.getTxseqno());
		trigger(lns205);// 扣息税金冲销

		lns103.setLoanAgreement(lns205.getLoanAgreement());
		lns103.setLnsinterface(lns219.getTblLnsinterface());
		lns103.setTxseqno(lns205.getTxseqno());
		trigger(lns103);// 本金户冲销

	}

	@Override
	protected void special() throws Exception {

		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS).contains(ctx.getRspCode())) {

			Map<String, Object> param = new HashMap<String, Object>();
			param.put("acctno", lns103.getAcctNo());
			param.put("brc", ctx.getBrc());
			
			//删除该借据号部分数据，动态表和主文件表因为写事件的时候需要使用，所以此处在特殊流程中对这两张表进行删除
			try {// 删除开户对应的表信息，为防止在特殊流程是出现删除数据时异常，可以通过幂等的方式多次删除如下表数据
				param.put("serseqno", ctx.getRequestDict("errSerSeq"));
				param.put("trandate", ctx.getRequestDict("errDate"));
				
				//删除摊销计划表
				DbAccessUtil.execute("CUSTOMIZE.delete_lnsamortizeplan", param);
				//删除计提登记薄
				DbAccessUtil.execute("CUSTOMIZE.delete_lnsprovision", param);
				DbAccessUtil.execute("Lnsprovision.deleteByAcctno",param);
				DbAccessUtil.execute("Lnsprovisiondtl.deleteByAcctno",param);
				//罚息计提登记簿
				DbAccessUtil.execute("CUSTOMIZE.delete_lnspenintprovreg", param);
				//罚息计提明细登记簿
				DbAccessUtil.execute("CUSTOMIZE.delete_lnspenintprovregdtl", param);
				/*删除幂等登记薄中该借据号交易码(trancode)为473004的记录*/
				//DbAccessUtil.execute("CUSTOMIZE.delete_lnsinterface", param);
//				/*删除交易明细表中该借据号的数据(lnsaccountlist)*/
//				DbAccessUtil.execute("CUSTOMIZE.delete_lnsaccountlist", param);
				/*删除交易明细表中该借据号的数据(lnsaccountlist)*/
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
				DbAccessUtil.execute("Lnsfeeinfo.delete", param);
				
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

		} else if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY).contains(ctx.getRspCode())) {
			// 如果幂等，则去幂等登记薄中查询customName(acctname), acctNo,
			// contractAmt(tranamt)三个字段对应的值，并返回给外围
			TblLnsinterface lnsinterface = new TblLnsinterface();
			lnsinterface.setTrancode(ctx.getTranCode());
			lnsinterface.setSerialno(ctx.getSerialNo());

			try {
				lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", lnsinterface,
						TblLnsinterface.class);
				if (null == lnsinterface) {
					throw new FabException("SPS103", "lnsinterface");
				}
				lns103.setCustomName(lnsinterface.getAcctname());
				lns103.setAcctNo(lnsinterface.getAcctno());
				lns103.setContractAmt(new FabAmount(lnsinterface.getTranamt()));

				/* 此处继续进行删除操作 *//* 冲销优化（相同流水幂等不做任何操作） 2019-07-31
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("acctno", lnsinterface.getAcctno());
				param.put("brc", lnsinterface.getBrc());

				*//* 删除动态表中该借据号对应的余额信息记录 *//*
				DbAccessUtil.execute("CUSTOMIZE.delete_lnsaccountdyninfo", param);

				*//* 删除主文件表中该借据号的数据 *//*
				DbAccessUtil.execute("CUSTOMIZE.delete_lnsbasicinfo", param);*/
			} catch (FabSqlException se) {
				throw new FabException(se, "SPS100", "lnsinterface");
			}

			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		}
	}

}
