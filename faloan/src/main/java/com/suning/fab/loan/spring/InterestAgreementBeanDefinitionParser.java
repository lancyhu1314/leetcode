package com.suning.fab.loan.spring;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.la.InterestAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class InterestAgreementBeanDefinitionParser implements BeanDefinitionParser {
	private static final String ATTRIBUTE_INTBASE = "intBase";
	private static final String ATTRIBUTE_PERIODMINDAYS = "periodMinDays";
	private static final String ATTRIBUTE_PERIODFORMULA = "periodFormula";
	private static final String ATTRIBUTE_INTFORMULA = "intFormula";
	private static final String ATTRIBUTE_ISCALINT = "isCalInt";
	private static final String ATTRIBUTE_COLLECTDEFAULTINTERESTFLAG = "collectDefaultInterestFlag";
	private static final String ATTRIBUTE_COLLECTDEFAULTINTERESTPRIN = "collectDefaultInterestPrin";
	private static final String ATTRIBUTE_COLLECTDEFAULTINTERESTINT = "collectDefaultInterestInt";
	private static final String ATTRIBUTE_NEEDRISKSCLASSIFICATIONFLAG = "needRisksClassificationFlag";
	private static final String ATTRIBUTE_NEEDRISKSCLASSIFICATIONPRIN = "needRisksClassificationPrin";
	private static final String ATTRIBUTE_NEEDRISKSCLASSIFICATIONINT = "needRisksClassificationInt";
	private static final String ATTRIBUTE_DINTSOURCE = "dintSource";
	private static final String ATTRIBUTE_CAPRATE = "capRate";
	private static final String ATTRIBUTE_DYNAMICCAPRATE = "dynamicCapRate";

	private static final String ATTRIBUTE_ISADVANCEREPAY = "isAdvanceRepay";
	private static final String ATTRIBUTE_ISCALTAIL = "isCalTail";
	private static final String ATTRIBUTE_ISCOMPENSATORY = "isCompensatory";
	
	private static final String ATTRIBUTE_ISCALGRACE = "isCalGrace";
	private static final String ATTRIBUTE_NEEDDUEDATE = "needDueDate";
	private static final String ATTRIBUTE_SHOWREPAYLIST = "showRepayList";
	private static final String ATTRIBUTE_ISPENALTY = "isPenalty";


	//提前结清收取手续费
	private static final String ADVANCE_FEETYPE="advanceFeeType";
	//最低还款额
	private static final String ATTRIBUTE_MINIMUMPAYMENT="minimumPayment";
	
	@Override
	public BeanDefinition parse(Element element, ParserContext context) {

		BeanDefinitionBuilder def = BeanDefinitionBuilder
				.rootBeanDefinition(InterestAgreement.class);

		String intBase = element.getAttribute(ATTRIBUTE_INTBASE);
		if (!VarChecker.isEmpty(intBase))
		{
		def.addPropertyValue(ATTRIBUTE_INTBASE, intBase);
		}
		
		String periodFormula = element.getAttribute(ATTRIBUTE_PERIODFORMULA);
		if (!VarChecker.isEmpty(periodFormula))
		{
			def.addPropertyValue(ATTRIBUTE_PERIODFORMULA, periodFormula);
		}


		String intFormula = element.getAttribute(ATTRIBUTE_INTFORMULA);
		if (!VarChecker.isEmpty(intFormula))
		{
			def.addPropertyValue(ATTRIBUTE_INTFORMULA, intFormula);
		}
		String periodMinDays = element.getAttribute(ATTRIBUTE_PERIODMINDAYS);
		if (!VarChecker.isEmpty(periodMinDays))
		{
			def.addPropertyValue(ATTRIBUTE_PERIODMINDAYS, periodMinDays);
		}
		
		
		String isCalInt = element.getAttribute(ATTRIBUTE_ISCALINT);
		if (!VarChecker.isEmpty(isCalInt)) {
			def.addPropertyValue(ATTRIBUTE_ISCALINT,isCalInt.toUpperCase());
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_ISCALINT,"YES");
		}
		
		//是否记罚息标志
		String collectDefaultInterestFlag = element.getAttribute(ATTRIBUTE_COLLECTDEFAULTINTERESTFLAG);
		if (!VarChecker.isEmpty(collectDefaultInterestFlag)) {
			def.addPropertyValue(ATTRIBUTE_COLLECTDEFAULTINTERESTFLAG,Boolean.parseBoolean(collectDefaultInterestFlag));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_COLLECTDEFAULTINTERESTFLAG,Boolean.TRUE);
		}
		
		//本金是否记罚息标志
		String collectDefaultInterestPrin = element.getAttribute(ATTRIBUTE_COLLECTDEFAULTINTERESTPRIN);
		if (!VarChecker.isEmpty(collectDefaultInterestPrin)) {
			def.addPropertyValue(ATTRIBUTE_COLLECTDEFAULTINTERESTPRIN,Boolean.parseBoolean(collectDefaultInterestPrin));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_COLLECTDEFAULTINTERESTPRIN,Boolean.TRUE);
		}
		
		//利息是否记复利标志
		String collectDefaultInterestInt = element.getAttribute(ATTRIBUTE_COLLECTDEFAULTINTERESTINT);
		if (!VarChecker.isEmpty(collectDefaultInterestInt)) {
			def.addPropertyValue(ATTRIBUTE_COLLECTDEFAULTINTERESTINT,Boolean.parseBoolean(collectDefaultInterestInt));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_COLLECTDEFAULTINTERESTINT,Boolean.TRUE);
		}
		
		
		//是否转列
		String needRisksClassificationFlag = element.getAttribute(ATTRIBUTE_NEEDRISKSCLASSIFICATIONFLAG);
		if (!VarChecker.isEmpty(needRisksClassificationFlag)) {
			def.addPropertyValue(ATTRIBUTE_NEEDRISKSCLASSIFICATIONFLAG,Boolean.parseBoolean(needRisksClassificationFlag));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_NEEDRISKSCLASSIFICATIONFLAG,Boolean.TRUE);
		}
		
		//本金是否转列
		String needRisksClassificationPrin = element.getAttribute(ATTRIBUTE_NEEDRISKSCLASSIFICATIONPRIN);
		if (!VarChecker.isEmpty(needRisksClassificationPrin)) {
			def.addPropertyValue(ATTRIBUTE_NEEDRISKSCLASSIFICATIONPRIN,Boolean.parseBoolean(needRisksClassificationPrin));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_NEEDRISKSCLASSIFICATIONPRIN,Boolean.TRUE);
		}
		
		//利息是否转列
		String needRisksClassificationInt = element.getAttribute(ATTRIBUTE_NEEDRISKSCLASSIFICATIONINT);
		if (!VarChecker.isEmpty(needRisksClassificationInt)) {
			def.addPropertyValue(ATTRIBUTE_NEEDRISKSCLASSIFICATIONINT,Boolean.parseBoolean(needRisksClassificationInt));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_NEEDRISKSCLASSIFICATIONINT,Boolean.TRUE);
		}
		
		//罚息来源
		String dintSource = element.getAttribute(ATTRIBUTE_DINTSOURCE);
		if (!VarChecker.isEmpty(dintSource)) 
		{
			def.addPropertyValue(ATTRIBUTE_DINTSOURCE,dintSource);
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_DINTSOURCE,"1");
		}

		//新封顶计息
		String dynamicCapRate = element.getAttribute(ATTRIBUTE_DYNAMICCAPRATE);
		if (!VarChecker.isEmpty(dynamicCapRate))
		{
			def.addPropertyValue(ATTRIBUTE_DYNAMICCAPRATE, JSONObject.parseObject(dynamicCapRate));
		}else{
			def.addPropertyValue(ATTRIBUTE_DYNAMICCAPRATE,new JSONObject());

		}
		//封顶计息
		String capRate = element.getAttribute(ATTRIBUTE_CAPRATE);
		if (!VarChecker.isEmpty(capRate))
		{
			def.addPropertyValue(ATTRIBUTE_CAPRATE,capRate);
		}
		//是否允许提前还款
		String isAdvanceRepay = element.getAttribute(ATTRIBUTE_ISADVANCEREPAY);
		if (!VarChecker.isEmpty(isAdvanceRepay)) {
			def.addPropertyValue(ATTRIBUTE_ISADVANCEREPAY,Boolean.parseBoolean(isAdvanceRepay));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_ISADVANCEREPAY,Boolean.TRUE);
		}
		
		//是否算头算尾
		String isCalTail = element.getAttribute(ATTRIBUTE_ISCALTAIL);
		if (!VarChecker.isEmpty(isCalTail)) {
			def.addPropertyValue(ATTRIBUTE_ISCALTAIL,Boolean.parseBoolean(isCalTail));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_ISCALTAIL,Boolean.FALSE);
		}
		//是否代偿开户
		String isCompensatory = element.getAttribute(ATTRIBUTE_ISCOMPENSATORY);
		if (!VarChecker.isEmpty(isCompensatory)) {
			def.addPropertyValue(ATTRIBUTE_ISCOMPENSATORY,Boolean.parseBoolean(isCompensatory));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_ISCOMPENSATORY,Boolean.FALSE);
		}
		//宽限期是否计息
		String isCalGrace = element.getAttribute(ATTRIBUTE_ISCALGRACE);
		if (!VarChecker.isEmpty(isCalGrace)) {
			def.addPropertyValue(ATTRIBUTE_ISCALGRACE,Boolean.parseBoolean(isCalGrace));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_ISCALGRACE,Boolean.TRUE);
		}
		//是否记违约金
		String isPenalty = element.getAttribute(ATTRIBUTE_ISPENALTY);
		if (!VarChecker.isEmpty(isPenalty)) {
			def.addPropertyValue(ATTRIBUTE_ISPENALTY,Boolean.parseBoolean(isPenalty));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_ISPENALTY,Boolean.FALSE);
		}
		//是否展示还款明细
		String showRepayList = element.getAttribute(ATTRIBUTE_SHOWREPAYLIST);
		if (!VarChecker.isEmpty(showRepayList)) {
			def.addPropertyValue(ATTRIBUTE_SHOWREPAYLIST,Boolean.parseBoolean(showRepayList));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_SHOWREPAYLIST,Boolean.FALSE);
		}
		//是否有到期日
		String needDueDate = element.getAttribute(ATTRIBUTE_NEEDDUEDATE);
		if (!VarChecker.isEmpty(needDueDate)) {
			def.addPropertyValue(ATTRIBUTE_NEEDDUEDATE,needDueDate.toUpperCase());
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_NEEDDUEDATE,"YES");
		}

		String advanceFeeType=element.getAttribute(ADVANCE_FEETYPE);
		if(!VarChecker.isEmpty(advanceFeeType)){
			def.addPropertyValue(ADVANCE_FEETYPE,advanceFeeType);
		}else{
			def.addPropertyValue(ADVANCE_FEETYPE,"2");
		}

		String  ignoreOffDint=element.getAttribute(ConstantDeclare.PARACONFIG.IGNOREOFFDINT);
		if(!VarChecker.isEmpty(ignoreOffDint)){
			def.addPropertyValue(ConstantDeclare.PARACONFIG.IGNOREOFFDINT,ignoreOffDint);
		}else{
			def.addPropertyValue(ConstantDeclare.PARACONFIG.IGNOREOFFDINT,"false");
		}
		
		
		//最低还款额
		String minimumPayment = element.getAttribute(ATTRIBUTE_MINIMUMPAYMENT);
		if (!VarChecker.isEmpty(minimumPayment)) {
			def.addPropertyValue(ATTRIBUTE_MINIMUMPAYMENT,Boolean.parseBoolean(minimumPayment));
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_MINIMUMPAYMENT,Boolean.FALSE);
		}

		return def.getBeanDefinition();
	}
}
