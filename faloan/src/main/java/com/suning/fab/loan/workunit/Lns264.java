package com.suning.fab.loan.workunit;

import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

/**
 * @author LH
 * @version V1.0.1
 * <p>
 * 跨库预收开户
 * @return
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns264 extends WorkUnit {

    String merchantNo;
    String customType;

    @Override
    public void run() throws Exception {
        TranCtx ctx = getTranctx();

        // 登记预收
        LoanAssistDynInfoUtil.addPreaccountinfo(ctx,
                ctx.getBrc(),
                merchantNo,
                ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,
                merchantNo,
                "2".equals(customType) ? ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY : ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);
    }

    /**
     * @return the merchantNo
     */
    public String getMerchantNo() {
        return merchantNo;
    }

    /**
     * @param merchantNo to set
     */
    public void setMerchantNo(String merchantNo) {
        this.merchantNo = merchantNo;
    }

    /**
     * @return the customType
     */
    public String getCustomType() {
        return customType;
    }

    /**
     * @param customType to set
     */
    public void setCustomType(String customType) {
        this.customType = customType;
    }
}