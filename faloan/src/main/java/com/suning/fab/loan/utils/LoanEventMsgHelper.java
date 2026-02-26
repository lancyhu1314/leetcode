/**
* @author 14050269 Howard
* @version 创建时间：2016年8月10日 下午3:04:22
* 类说明
*/
package com.suning.fab.loan.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.accounting.LnsEventMsg;
import com.suning.fab.loan.domain.TblLnseventreg;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.fab.tup4j.utils.VarChecker;

public class LoanEventMsgHelper {
	private LoanEventMsgHelper(){
		//nothing to do
	}

	public static LnsEventMsg genEventMsg(TblLnseventreg reg)
	{
		LnsEventMsg msg = MapperUtil.map(reg,LnsEventMsg.class);
		//TODO增加其它变量赋值
		if (reg.getBrc().length()<8) {//防只送四位机构号(5103)的情况
			msg.setS_Brc(reg.getBrc() + "0000");
		} else {
			msg.setS_Brc(reg.getBrc());
		}
		msg.setS_ChannelId("65");
		msg.setS_TermDate(new SimpleDateFormat("yyyy-MM-dd").format(reg.getTrandate()));
		msg.setS_TermTime(new SimpleDateFormat("HH:mm:ss").format(new Date()));
		msg.setS_SerialNo(reg.getSerseqno());
		msg.setS_LineNo(reg.getTxnseq());
		msg.setS_Ccy(reg.getCcy());
		msg.setS_TranCode("876001");
		msg.setS_EvtCode(reg.getEventcode());
		msg.setS_Teller(reg.getTeller());
		msg.setS_BriefCode(reg.getBriefcode());
		msg.setS_TranBrief(reg.getTranbrief());
		
		msg.setUserno(reg.getReceiptno());		
		msg.setAccsrccod(reg.getAcctstat());
		msg.setTouserno(reg.getToreceiptno());
		msg.setToaccsrccod(reg.getToacctstat());
		msg.setReserv3(reg.getInvestee());
		msg.setPaychannelcode(reg.getChanneltype());
		msg.setBankchannel(reg.getFundchannel());
		msg.setBillno(reg.getOutserialno());
		
		//如果是预收充值充退事件， 则将reserv3中的数据给custtype字段
		if (VarChecker.asList(ConstantDeclare.EVENT.LNMOPREACT,
							  ConstantDeclare.EVENT.NRECPREACT, 
							  ConstantDeclare.EVENT.TRECPREACT,
							  ConstantDeclare.EVENT.RECGDEBTCO,
							  ConstantDeclare.EVENT.NBACPREACT, 
							  ConstantDeclare.EVENT.TBACPREACT,
							  ConstantDeclare.EVENT.BACKDEBTCO,
							  ConstantDeclare.EVENT.PACTADJUST).contains(reg.getEventcode())) {
			//客户名称 从Reserv3转存到Tunneldata字段上了  需要从Tunneldata获取  兼容未曾转存数据
			if(VarChecker.isEmpty(reg.getReserv3())){
				Map<String,String> jsonStr = new HashMap<String,String>();
				jsonStr = JSONObject.parseObject(reg.getTunneldata().trim(),jsonStr.getClass());
				if(null != jsonStr.get("reserv3"))
					reg.setReserv3(jsonStr.get("reserv3"));
								
			}
			msg.setCusttype(reg.getReserv3());
			msg.setReserv3("");//此时备用字段三存放的是客户类型字段，抛事件时应该将其置空
		} else {
			msg.setCusttype(reg.getCusttype());
		}
		
		//客户类型 会计系统中该字段只支持1，所以此处进行转换一下
		if (!VarChecker.isEmpty(msg.getCusttype())) {//如果custtype为空的话，不处理
			if (ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON.equals(msg.getCusttype())) {//1表示对私Person
				msg.setCusttype("1");
			} else {//2表示对公Company
				msg.setCusttype("2");
			}
		}
		
		// 收款方客户类型
		if (!VarChecker.isEmpty(reg.getTocusttype())) {
			if (ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON.equals(reg.getTocusttype())) {// 1表示对私Person
				msg.setTocusttype("1");
			} else {// 2表示对公Company
				msg.setTocusttype("2");
			}
		}
		
		// lnsassistdyninfo账户类型转换
		if (!VarChecker.isEmpty(reg.getToacctstat())) {
			if ("N.N".equals(reg.getToacctstat())) {//预收户
				msg.setToaccsrccod(ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT);
			}
			else if("L.N".equals(reg.getToacctstat())){//长款户
				msg.setToaccsrccod(ConstantDeclare.ASSISTACCOUNT.SURPLUSACCT);
			}
		}
		
		// lnsassistdyninfo账户类型转换
		if (!VarChecker.isEmpty(reg.getTocusttype())) {
			if ("N.N".equals(reg.getTocusttype())) {//预收户
				msg.setTocusttype(ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT);
			}
			else if("L.N".equals(reg.getTocusttype())){//长款户
				msg.setTocusttype(ConstantDeclare.ASSISTACCOUNT.SURPLUSACCT);
			}
		}
		
		return msg;
	}
	
}
