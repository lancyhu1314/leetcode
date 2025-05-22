/**
 * @author 14050269 Howard
 * @version 创建时间：2016年5月31日 下午9:37:31
 * 类说明
 */
package com.suning.fab.loan.utils;

import com.suning.fab.tup4j.utils.PlatConstant;
import com.suning.fab.tup4j.utils.VarChecker;

import java.util.Arrays;

public class ConstantDeclare extends PlatConstant {

	public static final class ACCOUNT {
		private ACCOUNT(){
			//nothing to do
		}
		public static final class CUSTOMERTYPE {
			//个人
			public static final String PERSON = "PERSON";
			//分公司
			public static final String COMPANY = "COMPANY";
			//商户
			public static final String FINANCIAL = "FINANCIAL";
			private CUSTOMERTYPE(){
				//nothing to do
			}
		}

		public static final class ACCSRCCOD {
			public static final String EPP = "01";
			public static final String DEFAULT = "01";
			private ACCSRCCOD(){
				//nothing to do
			}

		}

	}
	public static final class RSPCODE {
		public static final String INNERERR = "999999";
		private RSPCODE(){
			//nothing to do
		}
		/* 交易层*/
		public static final class TRAN {
			//幂等
			public static final String IDEMPOTENCY = "LNS023";
			//成功
			public static final String SUCCESS = "000000";
			private TRAN(){
				//nothing to do
			}
		}
	}
	public static final class ACCOUNTCHANGE {
		private ACCOUNTCHANGE(){
			//nothing to do
		}
		public static final class CDFLAG {
			public static final String CREDIT = "C";
			public static final String DEBIT = "D";
			private CDFLAG(){
				//nothing to do
			}
		}
	}
	public static final class ACCTYPE {
		public static final String DEFAULT = "0";
		private ACCTYPE(){
			//nothing to do
		}
	}

	/* 贷款币种 */
	public static final class CCY {
		public static final String CCY_CNY_S = "01"; /* 人民币 */
		private CCY(){
			//nothing to do
		}
	}
	
	public static final class PREPAYFLAG {
		public static final String	PREPAYFLAG_ONLYINT = "ONLYINT";
		public static final String	PREPAYFLAG_ONLYPRIN = "ONLYPRIN";
		public static final String	PREPAYFLAG_INTPRIN = "INTPRIN";
		
		public static final String	PREPAYSPEC_ONLYINT = "SPECINT";
		public static final String	PREPAYSPEC_ONLYPRIN = "SPECPRIN";
		private PREPAYFLAG(){
			//nothing to do
		}
	}
	
	/* 账户状态 */
	public static final class STATUS {
		public static final String NORMAL = "0";
		public static final String CANCEL = "1";
		private STATUS(){
			//nothing to do
		}
	}
	
	/*priorType指定还款类型   1-还罚息 2-还利息 3-还本金 4-罚息利息*/
	public static final class PRIORTYPE {
		public static final String GDCINT = "1";
		public static final String NINT = "2";
		public static final String PRIN = "3";
		public static final String INTDINT = "4";
		private PRIORTYPE(){
			//nothing to do
		}
	}
	
	/*priorType指定还款类型   1-还罚息 2-还利息 3-还本金*/
	public static final class ENDFLAG {
		public static final String CURRCLR = "1";
		public static final String ALLCLR = "3";
		private ENDFLAG(){
			//nothing to do
		}
	}
	
	public static final class BALCTRLDIR{
		public static final String DEBIT = "D";   /* 借方 */
		public static final String CREDIT = "C";  /* 贷方 */
		private BALCTRLDIR(){
			//nothing to do
		}
	}

	public static final class SENDFLAG {
		//发送成功
		public static final String ACCESS = "0";
		//发送失败
		public static final String FAIL = "1";
		//发送中
		public static final String DEALING = "5";
		//发送成功
		public static final String DEALED = "6";
		//待发送
		public static final String PENDIND = "9";
		private SENDFLAG(){
			//nothing to do
		}
	}
	public static final class KAFKA {
		private KAFKA(){
			//nothing to do
		}
		public static final class TOPIC {
			//明细TOPIC
			public static final String ACCTLIST = "faepp_acct_list";
			//会计事件TOPIC
			public static final String EVENT = "faacc_uniform_account";
			//对账平台抛数TOPIC
			public static final String EPPSCAP = "faepp_data_transmission";
			private TOPIC(){
				//nothing to do
			}
		}
	}
	
	/*----------------------------------------------------------------------------*/
	/* 期数单位 */
	public static final class PERIODUNIT {
		public static final String PERIODUNIT_Y = "Y"; //年 
		public static final String PERIODUNIT_M = "M"; //月
		public static final String PERIODUNIT_D = "D"; //日
		public static final String PERIODUNIT_H = "H"; //半年
		public static final String PERIODUNIT_X = "X"; //旬
		public static final String PERIODUNIT_A = "A"; //一期
		private PERIODUNIT(){
			//nothing to do
		}
	}
	/* 还款公式*/
	public static final class FORMULA {
		public static final String FORMULA_EQPRINEQINT = "0"; //等本等息
		public static final String FORMULA_EQPRININT = "1"; //等额本息
		public static final String FORMULA_OTHER = "2"; //等额本金
		public static final String FORMULA_USERDEFINE = "3"; //自定义
		public static final String FORMULA_BOLLOON = "4"; //气球贷
		public static final String RORMULA_WAYWARD="5";//任性付等本等息计算公式  user:chenchao
		public static final String FORMULA_EQPRININTFEE="6";//等额本息费



		private FORMULA(){
			//nothing to do
		}
	}
	/* 计息基础*/
	public static final class INTBASE {
		 //实际天数
		public static final String INTBASE_ACT = "ACT"; 
		 //对年对月对日
		public static final String INTBASE_YMD  = "YMD";
		 //周期利率
		public static final String INTBASE_PER  = "PER";
		private INTBASE(){
			//nothing to do
		}
	}
	
	/* 还款方式定义*/
	public static final class REPAYWAY {
		public static final String REPAYWAY_DBDX   = "0"; /* 等本等息 */
		public static final String REPAYWAY_DEBX   = "1"; /* 等额本息 */
		public static final String REPAYWAY_DEBJ   = "2"; /* 等额本金 */
		public static final String REPAYWAY_HDE    = "3"; /* 先还息后本息 */
		public static final String REPAYWAY_BDQBDE = "4"; /* 不定期不等额 */
		public static final String REPAYWAY_LSBQ   = "5"; /* 利随本清/年还本,月还息 */
		public static final String REPAYWAY_LZHK   = "6"; /* 按2周还款/半年本月还息 */
		public static final String REPAYWAY_JYHK   = "7"; /* 季年本月还息 */
		public static final String REPAYWAY_DQHBHX = "8"; /* 到期还本还息*/
		public static final String REPAYWAY_QQD    = "9"; /* 气球贷*/
		public static final String REPAYWAY_WILLFUL="10";/*任性付 等本等息 author:chenchao*/

		public static final String REPAYWAY_XXHB   = "11"; /* 先息后本*/
		public static final String REPAYWAY_XBHX   = "12"; /* 先本后息*/
		public static final String REPAYWAY_YBYX   = "13"; /* 有本有息*/
		public static final String REPAYWAY_ZDY    = "14"; /* 自定义*/
		public static final String REPAYWAY_DEBXF  ="15" ;/*等额本息费*/
		private REPAYWAY(){
			//nothing to do
		}

		/**
		 * 校验还款方式是不是等本等息 author:chenchao
		 * @param repayWay
		 * @return
		 */
		public static boolean isEqualInterest(String repayWay){
			//判断是否是等本等系
			return Arrays.asList(REPAYWAY.REPAYWAY_DBDX,REPAYWAY_WILLFUL).contains(repayWay);
		}
	}
	
	/* 账户类型 */
	public static final class ACCOUNTTYPE {
		//本金
		public static final String ACCOUNTTYPE_PRIN   = "PRIN"; /* 本金 */
		//利息
		public static final String ACCOUNTTYPE_NINT   = "NINT"; /* 利息 */
		//罚息+复利
		public static final String ACCOUNTTYPE_DINT   = "DINT"; /* 罚息+复利 */
		//费用
		public static final String ACCOUNTTYPE_FEEA  = "FEEA"; /* 费用 */
		//费用违约金
		public static final String ACCOUNTTYPE_FEED   = "FEED"; /* 费用违约金 */
		
		//资金方提前结清手续费
		public static final String ACCOUNTTYPE_ADFE   = "ADFE"; /* 汽车分期提前结清手续费 */
	}
	
	/* 账单类型 */
	public static final class BILLTYPE {
		//本金
		public static final String BILLTYPE_PRIN   = "PRIN"; /* 本金 */
		//利息
		public static final String BILLTYPE_NINT   = "NINT"; /* 利息 */
		//宽限期利息
		public static final String BILLTYPE_GINT   = "GINT"; /* 宽限期利息 */
		//罚息
		public static final String BILLTYPE_DINT   = "DINT"; /* 罚息 */
		//复利
		public static final String BILLTYPE_CINT   = "CINT"; /* 复利 */
		//管理费
		public static final String BILLTYPE_MINT   = "MINT"; /* 管理费 */
		//信息服务费
		public static final String BILLTYPE_AFEE   = "FEEA"; /* 信息服务费 */
		//违约金
		public static final String BILLTYPE_PNLA   = "PNLA"; /* 违约金 */
		//提前结清手续费
		public static final String BILLTYPE_BFEE   = "BFEE"; /* 提前结清手续费 */
		//资金方提前结清手续费
		public static final String BILLTYPE_ADFE   = "ADFE"; /* 提前结清手续费 */
		
		public static final String BILLTYPE_FEEA   = "FEEA"; /* 管理费/保费       预扣融担费 */

		
		public static final String BILLTYPE_RMFE   = "RMFE"; /* 风险管理费*/
		public static final String BILLTYPE_SQFE   = "SQFE"; /* 担保费 */
		public static final String BILLTYPE_ISFE   = "ISFE"; /* 保费*/
		public static final String BILLTYPE_FWFE   = "FWFE"; /* 联通沃易融服务费*/
		public static final String BILLTYPE_WBRD   = "WBRD"; /* 外部融担费 */
		public static final String BILLTYPE_RBBF   = "RBBF"; /* 人保保费*/
		public static final String BILLTYPE_GDBF   = "GDBF"; /* 光大保费*/


		public static final String BILLTYPE_DBWY   = "DBWY"; /*担保费违约金*/
		public static final String BILLTYPE_FWWY   = "FWWY"; /*平台服务费违约金*/


		public static final String SQFE = "SQFE";
		//趸交保费	一次性/分期	ISFE 保费
		public static final String ISFE = "ISFE";
		//风险管理费	分期	RMFE风险管理费
		public static final String RMFE = "RMFE";
		
		private BILLTYPE(){
			//nothing to do
		}
	}
	
	
	/* 账户形态*/
	public static final class LOANSTATUS {
		//正常
		public static final String LOANSTATUS_NORMAL       = "N"; 
		//宽限期
		public static final String LOANSTATUS_GRACE        = "G"; 
		//逾期
		public static final String LOANSTATUS_OVERDU       = "O"; 
		//呆滞
		public static final String LOANSTATUS_LANGUISHMENT = "L"; 
		//呆帐
		public static final String LOANSTATUS_BADDEBTS     = "B"; 
		//核销
		public static final String LOANSTATUS_CERTIFICATION = "C";
		//销户 
		public static final String LOANSTATUS_CLOSE        = "CA"; 
		//部分逾期
		public static final String LOANSTATUS_PARTOVR      = "PO";
		//债转
		public static final String LOANSTATUS_DEBTTRANS      = "S";
		// 非应计
		public static final String LOANSTATUS_NONACCURAL      = "F";
		private LOANSTATUS(){
			//nothing to do 
		} 
	}
	
	/* 账单属性*/
	public static final class BILLPROPERTY{
		
		//正常结息
		public static final String BILLPROPERTY_INTSET = "INTSET";
		//还款
		public static final String BILLPROPERTY_REPAY  = "REPAY";
		//代偿
		public static final String BILLPROPERTY_COMPEN  = "COMPEN";
		//借新还旧
		public static final String BILLPROPERTY_SWITCH  = "SWITCH";
		//退货
		public static final String BILLPROPERTY_RETURN="RETURN";
		//债权转让
		public static final String BILLPROPERTY_SELL="SELL";

		private BILLPROPERTY(){
			//nothing to do
		}
	}
	/* 利息入账标志 */
	public static final class INTRECORDFLAG{
		 //未入账
		public static final String INTRECORDFLAG_NO = "NO";
		 //已入账
		public static final String INTRECORDFLAG_YES = "YES";
		private INTRECORDFLAG(){
			//nothing to do
		}
	}
	/* 账单作废标识*/
	public static final class CANCELFLAG{
		 //正常
		public static final String CANCELFLAG_NORMAL = "NORMAL";
		 //作废
		public static final String CANCELFLAG_CANCEL = "CANCEL";
		private CANCELFLAG(){
			//nothing to do
		}
	}
	/* 账单结清标志*/
	public static final class SETTLEFLAG{
		 //结清
		public static final String SETTLEFLAG_CLOSE   = "CLOSE";
		 //未结清
		public static final String SETTLEFLAG_RUNNING = "RUNNING";
		private SETTLEFLAG(){
			//nothing to do
		}
	}
	
	/* 逾期天数*/
	public static final Integer OVERDUEDAYS = 90;
	
	
	/* 是否计息标志*/
	public static final class ISCALINT{
		 //计息
		public static final String ISCALINT_YES = "YES";
		 //不计息
		public static final String ISCALINT_NO = "NO";
		private ISCALINT(){
			//nothing to do
		}
	}
	
	public static final class EVENT{
		//本金转回
		public static final String LOANRETURN = "LOANRETURN";
		//利息转回
		public static final String NINTRETURN = "NINTRETURN";
		//生成贷款本金账户
		public static final String LOANMEBRGT = "LOANMEBRGT"; 	
		//扣息放款对应扣息的税金金额
		public static final String DISCONTTAX = "DISCONTTAX"; 	
		//放款本金(多债务公司传多次事件)
		public static final String LOANGRANTA = "LOANGRANTA"; 	
		//不同渠道或通渠道多次金额则传多次事件
		public static final String LOANCHANEL = "LOANCHANEL";
		//借新还旧事件
		public static final String SWITCHLOAN = "SWITCHLOAN";
		//扣息放款的扣息金额
		public static final String LNDISCOUNT = "LNDISCOUNT"; 	
		//本金账户注销
		public static final String LOANMEBCAL = "LOANMEBCAL"; 	
		//扣息税金反向冲销
		public static final String DISCTAXOFF = "DISCTAXOFF"; 	
		//放款渠道方向冲销
		public static final String LOANCNLOFF = "LOANCNLOFF"; 	
		//扣息方向冲销
		public static final String DISCONTOFF = "DISCONTOFF"; 	
		//放款本金冲销（债务公司冲销）
		public static final String LOANWRTOFF = "LOANWRTOFF"; 	
		//已计提利息放款冲销时需同步反向冲销
		public static final String WRITOFFINT = "WRITOFFINT";
		//贷款核销（退用户）
		public static final String GOODRETURN = "GOODRETURN"; 
		//还复利、罚息、利息、本金、债务公司1、债务公司2
		public static final String CRDTREPYMT = "CRDTREPYMT"; 	
		/**利息结转*/
		public static final String INTSETLEMT = "INTSETLEMT"; 
		/**利息结转对应税金*/
//		public static final String INTSETMTAX = "INTSETMTAX";
		//还款金额的渠道来源（预收、云商、易付宝等）
		public static final String PAYCHANNEL = "PAYCHANNEL"; 	
		//还款时收取的手续费
		public static final String LNFEECOLCT = "LNFEECOLCT"; 	
		//预收账户充值
		public static final String RECGPREACT = "RECGPREACT"; 	
		//新预收账户充值 2020-07-16
		public static final String NRECPREACT = "NRECPREACT"; 
		//预收账户充值转付 2020-08-18
		public static final String TRECPREACT = "TRECPREACT"; 
		//预收贴息充值
		public static final String PREDNTBOND = "PREDNTBOND"; 	
		//有债务公司时需传输该事件
		public static final String RECGDEBTCO = "RECGDEBTCO"; 	
		//预收账户充退
		public static final String BACKPREACT = "BACKPREACT"; 	
		//新预收账户充退  2020-07-16
		public static final String NBACPREACT = "NBACPREACT"; 	
		//预收账户冲退转付  2020-08-18
		public static final String TBACPREACT = "TBACPREACT"; 	
		//预收贴息充退
		public static final String PREDNTBOFF = "PREDNTBOFF"; 	
		//债务公司预收的反向充退
		public static final String BACKDEBTCO = "BACKDEBTCO"; 	
		//罚息减免、税金
		public static final String REDUDEFINT = "REDUDEFINT";  
		//利息减免、税金
		public static final String REDUCENINT = "REDUCENINT"; 	
		//利息计提（含税金）
		public static final String ACCRUEDINT = "ACCRUEDINT"; 	
		//含税金，分录同罚息计提分录
		public static final String COMPONDINT = "COMPONDINT"; 	
		//含税金
		public static final String DEFAULTINT = "DEFAULTINT"; 	
		//本金形态转列
		public static final String LNTRANSFER = "LNTRANSFER"; 	
		//利息形态转列
		public static final String INTTRNSFER = "INTTRNSFER"; 
		//摊销冲销
		public static final String AMORTIZOFF = "AMORTIZOFF";   
		//摊销
		public static final String LNAMORTIZE = "LNAMORTIZE";   
		//罚息还本
		public static final String DINTADJUST = "DINTADJUST";
		//利息还本 
		public static final String NINTADJUST = "NINTADJUST";
		//客户帐调整
		public static final String CACTADJUST = "CACTADJUST";
		//预收户调整
		public static final String PACTADJUST = "PACTADJUST";
		/**结息冲销*/
		public static final String INTSETMOFF = "INTSETMOFF";
		/**结息税金冲销*/
		public static final String INTSTAXOFF = "INTSTAXOFF";
		/**资金方还款*/
		public static final String LBRPYINVES = "LBRPYINVES";
		/**收入结转（含税金）*/
		public static final String INCMCARRYO = "INCMCARRYO";
		/**费用收入结转**/
		public static final String FEEINCMCYO="FEEINCMCYO";
		/**收益冲销*/
		public static final String AMORTIZEAD = "AMORTIZEAD";
		/**管理费计提*/
		public static final String ACCRUEDFEE = "ACCRUEDFEE";
		/**风险管理费还款*/
		public static final String LNSFEEPYMT = "LNSFEEPYMT";
		/**管理费冲销*/
		public static  final  String WRITOFFFEE = "WRITOFFFEE";
		/**服务费计提*/
		public static  final  String ACCRUEAFEE = "ACCRUEAFEE";
		/**服务费冲销*/
		public static  final  String ACCRFEEOFF = "ACCRFEEOFF";
		/**违约金收取*/
		public static  final  String LNPENALSUM = "LNPENALSUM";
		/** 非标迁移放款 **/
		public static  final  String LNDATRANSF = "LNDATRANSF";
		/** 非标迁移扣息税金 **/
		public static  final  String LNDAINTTAX = "LNDAINTTAX";
		/** 非标迁移扣息 **/
		public static  final  String LNDADISCNT = "LNDADISCNT";
		/** 非标迁移放款渠道 **/
		public static  final  String LNDACHANNL = "LNDACHANNL";
		/** 非标迁移利息结转 **/
		public static  final  String LNTRINTSET = "LNTRINTSET";
		/** 非标迁移结息税金**/
		public static  final  String LNTRINTTAX = "LNTRINTTAX";
		/**通知核销**/
		public static  final  String LNCNLVRFIN = "LNCNLVRFIN";
		/**转非应计**/
		public static  final  String BATRANSFER = "BATRANSFER";
		/**核销转回**/
		public static  final  String LNCNFINOFF =  "LNCNFINOFF";
		/**保费开户**/
		public static  final  String BINSURANCE =  "BINSURANCE";
		/**退保**/
		public static  final  String CINSURANCE =  "CINSURANCE";
		/**手续费结转**/
		public static  final  String INCADFRRYO =  "INCADFRRYO";

		/**费用减免**/
		public static  final  String FEEREDUCEM =  "FEEREDUCEM";
		
		
		/**费用结转**/
		public static  final  String FEESETLEMT =  "FEESETLEMT";
		/**费用结转对应税金*/
		public static final String FEESETMTAX = "FEESETMTAX";
		/**结费冲销**/
		public static  final  String WFFEESETLE = "WFFEESETLE";
		//费用转列
		public static final String FEETRNSFER = "FEETRNSFER";
		//复利罚息冲销
		public static final String DEFAULTOFF = "DEFAULTOFF";
		//还款拆分
		public static final String REPAYSPLIT = "REPAYSPLIT";


		/** 融担赔付 */
		public static final String COMPENSATE = "COMPENSATE";
		/** 赔付开户 */
		public static final String COMPENADJU = "COMPENADJU";
		/** 违约金计提 */
		public static final String DEFUALPROV = "DEFUALPROV";
		/**违约金结转*/
		public static final String DEFSETLEMT="DEFSETLEMT";
		/** 债权转让 */
		public static final String LNTRANSCRE = "LNTRANSCRE";

	
		/** 预收迁移 */
		public static final String LNMOPREACT = "LNMOPREACT";
		/** 法催开户 */
		public static final String LNFCFYKHJT = "LNFCFYKHJT";
	
		/** 法催费用转列 */
		public static final String LNFCFYZTJZ = "LNFCFYZTJZ";
		/** 法催费用还款 */
		public static final String LNFCFYRPAY = "LNFCFYRPAY";
		
		private EVENT(){
			//nothing to do
		}

	}
	public static final class BRIEFCODE{
		//利息转回
		public static final String LXZH ="LXZH";
		//本金转回
		public static final String BJZH ="BJZH";
		//本金开户
		public static final String BJKH ="BJKH";
		//扣息税金
		public static final String KXSJ ="KXSJ";
		//本金放款
		public static final String BJFK ="BJFK";
		//放款渠道
		public static final String FKQD ="FKQD";
		//扣息金额
		public static final String KXJE ="KXJE";
		//本金销户
		public static final String BJXH ="BJXH";
		//扣息税金冲销
		public static final String KSCX ="KSCX";
		//放款渠道冲销
		public static final String FDCX ="FDCX";
		//扣息冲销
		public static final String KXCX ="KXCX";
		//放款冲销
		public static final String FKCX ="FKCX";
		//利息冲销
		public static final String LXCX ="LXCX";
		//退货渠道
		public static final String THQD ="THQD";
		//还款本息
		public static final String HKBX ="HKBX";
		//还款渠道
		public static final String HKQD ="HKQD";
		//收手续费
		public static final String SSXF ="SSXF";
		//预收充值
		public static final String YSCZ ="YSCZ";
		//债务公司预收充值
		public static final String ZWCZ ="ZWCZ";
		//预收充退
		public static final String YSCT ="YSCT";
		//债务公司预收充退
		public static final String ZWCT ="ZWCT";
		//罚息减免
		public static final String FXJM ="FXJM";
		//利息结转
		public static final String LXJZ ="LXJZ";
		//结息税金
		public static final String JXSJ ="JXSJ";
		//利息减免
		public static final String LXJM ="LXJM";
		//利息计提
		public static final String LXJT ="LXJT";
		//复利计提
		public static final String FLJT ="FLJT";
		//罚息计提
		public static final String FXJT ="FXJT";
		//本金转列
		public static final String BJZL ="BJZL";
		//利息转列
		public static final String LXZL ="LXZL";
		//利息摊销
		public static final String LXTX ="LXTX";
		//摊销冲销
		public static final String TXCX ="TXCX";
		//罚息还本
		public static final String FXHB ="FXHB";
		//利息还本 
		public static final String LXHB ="LXHB";
		//客户帐加 
		public static final String KHZJ ="KHZJ";
		//客户帐扣减
		public static final String KHZK ="KHZK";
		//客户帐转
		public static final String KHZZ ="KHZZ";
		//预收增加
		public static final String YSZJ ="YSZJ";
		//预收减少
		public static final String YSJS ="YSJS";
		//长款增加
		public static final String CKZJ ="CKZJ";
		//长款减少
		public static final String CKJS ="CKJS";
		/**结息冲销*/
		public static final String JXCX ="JXCX";
		/**结息税金冲销*/
		public static final String JSCX ="JSCX";
		/**资金方还款*/
		public static final String ZJXQ ="ZJXQ";
		/**收入结转*/
		public static final String SRJZ ="SRJZ";
		/**费用收入结转**/
		public static final String TRDF="TRDF";
		/**收益冲销*/
		public static final String SYCX ="SYCX";
		/**摊销调整*/
		public static final String TXTZ ="TXTZ";
		/**费用计提*/
		public static final String FYJT ="FYJT";
		/**费用还款*/
		public static final String FYHK ="FYHK";
		/**费用冲销*/
		public  static final String FYCX = "FYCX";
		/**服务费计提*/
		public static final String XFJT ="XFJT";
		/**服务费计提冲销*/
		public static final String FJTC ="FJTC";
		/**任性担保*/
		public static final String RXDB ="RXDB";
		/**收取违约金*/
		public static final String SWYJ ="SWYJ";
		/**数据迁移*/
		public static final String SJQY ="SJQY";
		/**通知核销**/
		public static final String TZHX ="TZHX";
		/**转非应计**/
		public static final String ZFYJ ="ZFYJ";
		/**通知核销老的数据**/
		public static final String TZHXOLD="TZOL";
		/**核销还款**/
        public static final String HXZH ="HXZH";
		/**代客投保**/
		public static final String DKTB ="DKTB";
		/**提前结清**/
		public static final String TQJQ ="TQJQ";
		/**逾期退保**/
		public static final String YQTB ="YQTB";
		/**冲销退保**/
		public static final String CXTB ="CXTB";
		/**保费还款**/
		public static final String BFHK ="BFHK";
		
		//债务公司冲销充退
		public static final String CXCT ="CXCT";
		//债务公司预还款充退
		public static final String HKCT ="HKCT";
		//固定担保
		public static final String GDDB ="GDDB";
		//担保计提
		public static final String DBJT = "DBJT";
		//固担还款
		public static final String GDHK = "GDHK";
		//担保冲销
		public static final String DBCX = "DBCX";
		//资金方提前还款手续费
		public static final String TQJZ = "TQJZ";
		//有追还款
		public static final String YZHK = "YZHK";
		//期缴担保
		public static final String QJDB = "QJDB";
		//风险管理
		public static final String FXGL = "FXGL";
		//违约金结转
		public static final String WYJZ="WYJZ";

		// 固定担保费结转
		public static final String OFJZ = "OFJZ";
		//担保费结转
		public static final String SFJZ = "SFJZ";
		//保费结转
		public static final String IFJZ = "IFJZ";
		//风险管理费结转
		public static final String RFJZ = "RFJZ";
		//费用结转 2020-06-29
		public static final String FYJZ = "FYJZ";
		//结费税金
		public static final String JFSJ ="JFSJ";
		
		
		
		//每月担保费转列
		public static final String SFZL = "SFZL";
		//固定担保费转列
		public static final String OFZL = "OFZL";
		//保费转列
		public static final String IFZL = "IFZL";
		//风险管理费转列
		public static final String RFZL = "RFZL";
		//费用转列
		public static final String FYZL = "FYZL";


		//SFJC担保费结转冲销
		public static final String SFJC = "SFJC";

		//OFJC 固定担保费结转冲销
		public static final String OFJC = "OFJC";

		//IFJC保费结转冲销
		public static final String IFJC = "IFJC";

		//RFJC风险管理费结转冲销
		public static final String RFJC = "RFJC";
		//FYJC 费用结转冲销
		public static final String FYJC = "FYJC";

		//期缴担保费减免
		public static final String SFJM = "SFJM";
		//风险管理费减免
		public static final String RFJM = "RFJM";
		//第三方服务费减免
		public static final String FYJM = "FYJM";

		//担保费还款
		public static final String SFHK = "SFHK";
		//保费还款
		public static final String IFHK = "IFHK";
		//风险管理费还款
		public static final String RFHK = "RFHK";
		//代偿结清
		public static final String DCJQ = "DCJQ";
		//罚息冲销
		public static final String FXCX = "FXCX";
		//复利冲销
		public static final String FLCX = "FLCX";
		//人保保费
		public static final String RBBF = "RBBF";
		//光大保费
		public static final String GDBF = "GDBF";
		/** 赔付本息 */
		public static final String PFBX = "PFBX";
		/** 赔付调整 */
		public static final String PFTZ = "PFTZ";
		/** 融担赔付 */
		public static final String RDPF = "RDPF";
		/** 人保赔付 */
		public static final String RBPF = "RBPF";
        /** 光大计提 */
        public static final String GDJT = "GDJT";
		/** 光大冲销 */
		public static final String GDCX = "GDCX";
        /** 光大项目担保费结转 */
        public static final String GDJZ = "GDJZ";
    	/** 光大项目担保费结转冲销*/
		public static final String GDJC = "GDJC";
		/** 服务费违约金计提 */
		public static final String FWJT = "FWJT";
		/** 赔付待定 **/
		public static final String PFDD = "PFDD";
		/** 担保还款 **/
		public static final String DBHK = "DBHK";
		/** 借新还旧 **/
		public static final String JXHJ = "JXHJ";
		/** 退货本息 **/
		public static final String THBX = "THBX";
		/** 债权转让 **/
		public static final String ZQZR = "ZQZR";
		/** 核销开户 **/
		public static final String HXKH = "HXKH";
		/** 预收迁移 **/
		public static final String YSQY = "YSQY";
		
		/**法催计提**/
		public static final String FCJT ="FCJT";
		/**法催转列**/
		public static final String FCZL ="FCZL";
		/**法催还款**/
		public static final String FCHK ="FCHK";

		
		private BRIEFCODE(){
			//nothing to do
		}
		
		
		//还款本息摘要码透传枚举值
		public static boolean isBriefCode(String memo){
			return Arrays.asList(BRIEFCODE.JXHJ,BRIEFCODE.THBX,BRIEFCODE.ZQZR).contains(memo);
		}


	}
	
	public static final class SQLCODE {
		/** 重复数据 */
		public static final Integer DUPLICATIONKEY = -803;
		
		private SQLCODE(){
			//nothing to do 
		}
	}
	
	
	public static final class INTERTYPE {
		//PROVISION计提
		public static final String PROVISION = "PROVISION";
		//AMORTIZE摊销（预扣融担费）
		public static final String AMORTIZE = "AMORTIZE";
		//MANAGEFEE管理费
		public static final String MANAGEFEE = "MANAGEFEE";
		//SERVERFEE信息服务费
		public static final String SERVERFEE = "SERVERFEE";
		//SECURITYFEE房抵贷固定担保
		public static final String SECURITFEE = "SECURITFEE";
		
		
		//人保保费
		public static final String RBBFAMOR = "RBBFAMOR";
		//光大保费
		public static final String GDBFAMOR = "GDBFAMOR";
		private INTERTYPE(){
			//nothing to do
		}
	}
	
	public static final class  PAYCHANNEL{
		//云商
		public static final String ONE = "1";
		//预收
		public static final String TWO = "2";
		//易付宝
		public static final String THREE = "3";
		//票据
		public static final String FOUR = "4";
		//任性贷退款
		public static final String FIVE = "5";
		//任性贷支付退货
		public static final String SIX = "6";
		//退货渠道  author:chenchao
		public static final String SALES_RETURN="E";

		private PAYCHANNEL(){
			//nothing to do
		}
	}
	
	public static final class INTERFLAG {
		//POSITIVE正向
		public static final String POSITIVE = "POSITIVE";
		//NEGATIVE反向
		public static final String NEGATIVE = "NEGATIVE";
		private INTERFLAG(){
			//nothing to do
		}
	}

	public static final class FLAG1{
		//区分是否是新老核销数据
		public static final String H="H";
		//老帐户迁移C账户后变更
		public static final String HX="HX";
	}
	
	public static final class HBASE {
		/**表名 */
		public static final String TABLENAME = "tablename";
		/**分区数的长度*/
		public static final String PARTITIONLENGTH = "partitionlength";
		/**分区数的个数*/
		public static final String PARTITIONCOUNT = "partitioncount";
		/**表分区存储依赖字段*/
		public static final String PARTITIONFIELD = "partitionfield";
		/**rowkey依赖字段*/
		public static final String ROWKEYFIELD = "rowkeyfield";
		/**列簇名*/
		public static final String COLUMN = "cf";
		/**scm节点名称*/
		public static final String HBASENODENAME = "Hbase.properties";
		
		private HBASE(){
		}
	}

	public static final class KEYNAME {

		//幂等拓展表的key值  lnsinterfaceex
		/**渠道*/
		public static final String  QD = "QD";
		/**投保*/
		public static final String  BI = "BI";
		/**租赁*/
		public static final String  ZL = "ZL";
		/**退保*/
		public static final String  CI = "CI";
		/**代偿*/
		public static final String  DC = "DC";
		/**借新还旧-借新*/
		public static final String JXHJ_JX ="JXHJ";
		/**借新还旧-还旧*/
		public static final String JXHJ_HJ ="HJJX";
		/**展期*/
		public static final String  ZQ = "ZQ";
		/**自动减免*/
		public static final String  ZDJM = "ZDJM";
		/**自动减费*/
		public static final String  ZDJF = "ZDJF";
		//主文件扩展表的key值
		public static final String GDDB = "GDDB";
        /**原罚息利率和对应时间 Original penalty interest rate*/
        public static final String  OPIR = "OPIR";
		//taxtype
        //费用预提
        public static final String FYYT = "FYYT";
		//动态封顶
		public static final String DTFD = "DTFD";
		//预收渠道
		public static final String  YCQD = "YCQD";
		//还款明细
		public static final String  HKMX = "HKMX";
		//预收明细
		public static final String YSMX = "YSMX";
		//减免明细
		public static final String JMMX = "JMMX";
		//还费明细
		public static final String HFMX = "HFMX";
		//幂等流水
		public static final String MDLS = "MDLS";

		private KEYNAME(){
		}
	}
	
	//资金方登记簿trantype
	public static final class TRANTYPE{
		
		//开户
		public static final String KH = "KH";
		//还款
		public static final String HK = "HK";
		//转换
		public static final String ZH = "ZH";
		
		private TRANTYPE(){
			
		}
	}

	//放款渠道
	public static final class CHANNELCODE{

		//预收费
		public static final String A = "A";


		private CHANNELCODE(){

		}
	}
	/* 还款状态 */
	public static final class REPAYFLAG{
		//正常还款
		public static final String REPAY_NORMAL   = "1";
		//逾期还款
		public static final String REPAY_EXCEED   = "2";
		//正常结清
		public static final String SETTLE_NORMAL   = "3";
		//逾期结清
		public static final String SETTLE_EXCEED   = "4";
	}
	/* 到期日按月对日规则 */
	public static final class DTAEAGAINST{
		//起息日对日
		public static final String STARTINTDATE  = "1";
		//固定还款日
		public static final String REPAYDATE   = "2";
		//起息日对日-1，供个贷业务试算到期日使用
		public static final String DATE_BEFORE_STARTINT   = "3";
	}
	/* 利率类型 */
	public static final class RATETYPE{
		//正常还款
		public static final String NORMAL   = "1";
		//逾期还款
		public static final String PENALTY   = "2";
		//正常结清
		public static final String COMPOUND   = "3";
		//逾期结清
		public static final String FEE   = "4";
		//逾期结清
		public static final String ONCE   = "5";
	}

	//2019-12-03 费用计息方式
	public static final class PROVISIONRULE{
		//按日计提
		public static final String BYDAY   = "0";
		// 月底计提
		public static final String BYTERM   = "1";
		// 2-（预扣摊销）
		public static final String ADVDEDUCT   = "2";

	}

	//2019-12-03 费用计息方式
	public static final class CALCULATRULE{
		//按日计费）
		public static final String BYDAY   = "DAY";
		// 按期计费
		public static final String BYTERM   = "TERM";
		//等额本息费计费方式
		public static final String BYDAYTERM="DTM";
	}
	//2019-12-03 费用还款方式
	public static final class FEEREPAYWAY{
		//A- 分期
		public static final String STAGING   = "A";
		// B 一次性
		public static final String ONETIME   = "B";
		// C- 预扣
		public static final String ADVDEDUCT   = "C";
		//D-非标模式
		public static final String NONESTATIC="D";

	}
	//2019-12-03 费用提前结清收取方式
	public static final class EARLYSETTLRFLAG{

		// 1- 收到当期
		public static final String CURRCHARGE   = "1";
		// 2- 全收
		public static final String FULLCHARGE   = "2";
		// 3- 收到当日
		public static final String DATECHARGE   = "3";

	}
	//2019-12-03 费用计费基数
	public static final class FEEBASE{

		// 剩余本金
		public static final String BAL   = "0";
		// 合同金额
		public static final String ALL   = "1";

	}
	
	//费用账本类型
	public static final class FEETYPE{
		//任性贷固定担保费	预扣	不涉及
		//		房抵贷固定担保费	一次性/一次性+分期	SQFE担保费
		//		期缴担保费	分期/一次性+分期	SQFE担保费
		public static final String SQFE = "SQFE";
		//趸交保费	一次性/分期	ISFE 保费
		public static final String ISFE = "ISFE";
		//风险管理费	分期	RMFE风险管理费
		public static final String RMFE = "RMFE";
		//平台服务费
		public static final String FWFE = "FWFE";
		//外部融担费
		public static final String WBRD = "WBRD";
		private FEETYPE(){
			//nothing to do
		}
	}
	//预扣费用账本类型
	public static final class ADVANCEFEE{
		//光大保费
		public static final String GDBF = "GDBF";
		//人保保费
		public static final String RBBF = "RBBF";
		//预扣担保费
		public static final String SQFE = "SQFE";

		private ADVANCEFEE(){
			//nothing to do
		}
	}

	//第三方服务费
	public static final class OTHERFEE{
		//平台服务费
		public static final String FWFE = "FWFE";
		//外部融担费
		public static final String WBRD = "WBRD";

		private OTHERFEE(){
			//nothing to do
		}
	}

	
	//摊销类型
	public static final class AMORTIZETYPE{

		// 利息
		public static final String AMORTIZEINT   = "1";
		// 担保费
		public static final String AMORTIZEFEE   = "2";
		// 人保保费
		public static final String AMORTIZERBBF   = "3";
		// 光大保费
		public static final String AMORTIZEGDBF   = "4";


	}

	/* 日期类型 */
	public static final class DATETYPE{
		//工作日
		public static final String WORKDAY   = "WORKDAY";
		//假日
		public static final String HOLIDAY   = "HOLIDAY";

	}
	
	/** 辅助户 类型*/
	public static final class ASSISTACCOUNT {
		
		/** 保费户*/
		public static final String PREMIUMSACCT = "B";
		/** 赔付户*/
		public static final String PAYACCT = "C";
		/** 预收户*/
		public static final String PREFUNDACCT = "N";
		/** 长款户*/
		public static final String SURPLUSACCT = "L";

		private ASSISTACCOUNT(){
			//nothing to do
		}
	}
	
	/** 预收表账户类型*/
	public static final class ASSISTACCOUNTINFO {
		
		/** 预收户*/
		public static final String PREFUNDACCT = "PREFUNDACCT";
		/** 长款户*/
		public static final String SURPLUSACCT = "SURPLUSACCT";

		private ASSISTACCOUNTINFO(){
			//nothing to do
		}
	}
	
	
	/** 主文件拓展表key值 */
	public static final class BASICINFOEXKEY {
		
		/**  展期*/
		public static final String ZQ = "ZQ";
		/** 买方付息*/
		public static final String MFFX = "MFFX";
		/** 封顶计息*/
		public static final String FDJX = "FDJX";
		/** 无追保理*/
		public static final String WZBL = "WZBL";
		/** 非标自定义不规则*/
		public static final String ZDY = "ZDY";
		/** 固定担保*/
		public static final String GDDB = "GDDB";
		/** 合同编码*/
		public static final String HTBM = "HTBM";
		/** 膨胀期数*/
		public static final String PZQS = "PZQS";
		/** 提前还款违约金*/
		public static final String TQHK = "TQHK";
		/** 免息*/
		public static final String MX = "MX";
		/** 汽车租赁还款方式 */
		public static final String QCZL = "QCZL";
		/*退货接口推完后合同 所剩金额*/
		public static final String THJE="THJE";
		/*核销日期*/
		public static final String HXRQ="HXRQ";
        /*借据封顶日期*/
		public static final String FDRQ="FDRQ";
		/*首日还款日*/
		public static final String FRD="FRD";

		private BASICINFOEXKEY(){
			//nothing to do
		}
	}
	
	
	
	/** 幂等拓展表key值 */
	public static final class INTERFACEEXKEY {
		
		/**  展期*/
		public static final String QD = "QD";
		/** 买方付息*/
		public static final String BI = "BI";
		/** 封顶计息*/
		public static final String ZL = "ZL";
		/** 无追保理*/
		public static final String CI = "CI";
		/** 非标自定义不规则*/
		public static final String DC = "DC";
		/** 固定担保*/
		public static final String ZQ = "ZQ";
		
		private INTERFACEEXKEY(){
			//nothing to do
		}
	}

	/**
	 * 费用收取方式 author:chenchao
	 */
	public static final class ADVANCEFEETYPE{
		//收到当前期
		public static final String CURRENT="1";
		//收到未来期
		public static final String FUTURE="2";
		//固定期
		public static final String FIXED="3";
	}
	
	/**
	 * 借新还旧类型
	 */
	public static final class SWITCHLOANTYPE{
		//1-房抵贷债务重组
		public static final String DEBTRESTRUCTURING="1";
		//2-任性付账单分期
		public static final String BILLDIVIDE="2";
		//3-任性付最低还款额
		public static final String MUNIMUM="3";
		
	    public static boolean  isSwitchType(String element){
	        return VarChecker.isValidConstOption(element, ConstantDeclare.SWITCHLOANTYPE.class);
	    }
	}


	public static final class COMPENSATEFLAG{
		//1-用户自偿
		public static final String SELFPAY="1";
		//2-融担代偿
		public static final String OTHERPAY="2";
		//4-债权转让
		public static final String SELL="4";

	}

	//参数配置
	public static final class PARACONFIG{
		//核销后是否需要转罚息
		public static final String IGNOREOFFDINT="ignoreOffDint";
		//贷款状态
		public static final String LOANSTAT="loanStat";
		//折前利率
		public static final String ZQLL="ZQLL";
		//账户扩展表统一存放参数配置
		public static final String EXTEND="EXTEND";
		//免费金额 _1 第一期总的  :1 当次
		public static final String FREEFEE="FF";
		//费用折前利率  feeDiscount 缩写 FD,费用因有多种类型需要FEETYPE+REPAYWAY+FD 这种方式进行区分
		public static final String FEEDISCOUNT="FD";
		//贴费标志 discountFeeFlag
		public static final String DISCOUNTFEEFLAG="DFF";
		//贴息标志 discountIntFlag
		public static final String DISCOUNTINTFLAG="DIF";
	}

    public static final class SEQUENCE {
        /** 交易子序号 */
        public static final String TXNSUBSEQ = "TXNSUBSEQ";

        private SEQUENCE() {
        }
    }

}
		
