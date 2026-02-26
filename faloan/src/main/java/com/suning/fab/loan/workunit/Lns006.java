package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONArray;
import com.suning.fab.loan.account.LnsInterfaceQueryM;
import com.suning.fab.loan.bo.TopOffDetail;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author
 * @Date Created in 19:13 2019/10/11
 * @see
 */

@Scope("prototype")
@Repository
public class Lns006 extends WorkUnit {


    String acctNo;
    String brc;


    public void run() throws Exception {


        // 主文件
        TblLnsbasicinfo lnsbasicinfo;
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", acctNo);
        param.put("openbrc", brc);
        try {
            //取主文件信息
            lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbasicinfo");
        }

        if (null == lnsbasicinfo) {
            throw new FabException("SPS104", "lnsbasicinfo");
        }

        // 封顶值
        TblLnspenintprovreg lnspenintprovreg;
        param = new HashMap<>();
        param.put("billtype", ConstantDeclare.KEYNAME.DTFD);
        param.put("receiptno", acctNo);
        param.put("brc", brc);
        try {
            lnspenintprovreg = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "Lnspenintprovreg");
        }

        //若已经达到封顶 并且当前时间已经过了合同到期日
        if(lnspenintprovreg.getReserv1().contains("A")&&CalendarUtil.after(tranctx.getTranDate(),lnsbasicinfo.getContduedate())){
            DbAccessUtil.execute("Lnspenintprovreg.updateBDTFD", param);
        }
    }

    /**
     * @return the brc
     */
    public String getBrc() {
        return brc;
    }

    /**
     * @param brc to set
     */
    public void setBrc(String brc) {
        this.brc = brc;
    }


    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }
}
