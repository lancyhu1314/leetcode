package com.suning.fab.faibfp.service;

import com.suning.fab.faibfp.intf.FaloanMapService;
import com.suning.fab.faibfp.service.template.RsfQuerServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉<br>
 * 预收账户余额查询
 *
 * @author 19043955
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
@Implement(contract = FaloanMapService.class, implCode = "faloan-queryAdvanceAccountBlanace")
public class Rsf176003 extends RsfQuerServiceTemplate {

    /**
     * 预收户不迁移，暂时只调用老系统
     *
     * @param productCode
     * @return
     */
    @Override
    protected boolean isCallOldSystem(String productCode) {
        return true;
    }

    @Override
    public String getTranCode() {
        return "176003";
    }
}
