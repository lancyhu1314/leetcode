package com.suning.fab.faibfp.service;

import com.suning.fab.faibfp.service.template.RsfServiceTemplate;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉预收户开户
 *
 * @Author 19043955
 * @Date 2021/7/20
 * @Version 1.0
 */
public class Rsf176013 extends RsfServiceTemplate {

    @Override
    public String getTranCode() {
        return "176013";
    }


    /**
     * 预收户不迁移，暂时只调用老系统
     *
     * @param productCode
     * @return
     */
    @Override
    public boolean isCallOldSystem(String productCode) {
        return true;
    }
}
