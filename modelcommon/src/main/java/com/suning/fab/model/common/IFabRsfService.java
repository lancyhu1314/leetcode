package com.suning.fab.model.common;

import java.util.Map;

import com.suning.rsf.provider.annotation.Contract;
import com.suning.rsf.provider.annotation.Method;

@Contract(name = "com.suning.fab.model.common.fabrsfcommonservice", internal = false, description = "this is a suning api of remote fab service", warningPhones="18512582690")
public interface IFabRsfService {

	//rsf路由策略：根据会员编号路由，即从第0个入参里取出相关的【会员编号】值，其Spring EL表达式：#arg.getRouteId()
	@Method(idempotent = false, timeout = 5000, retryTimes = 0, priority = "H", description = "execute service")
	public AbstractDatagram execute(AbstractDatagram arg);
	
	//rsf路由策略：根据会员编号路由，即从第0个入参里取出相关的【会员编号】值，其Spring EL表达式：#reqMsg[routeId]
	@Method(idempotent = false, timeout = 5000, retryTimes = 0, priority = "H", description = "execute service")
    Map<String, Object> execute(Map<String, Object> reqMsg);
}
