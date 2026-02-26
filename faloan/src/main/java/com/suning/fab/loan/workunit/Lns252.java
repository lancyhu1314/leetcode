package com.suning.fab.loan.workunit;


import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.framework.dal.client.support.DefaultDalClient;
import com.suning.framework.dal.client.support.PaginationDalClient;
import org.jets3t.service.multi.event.UpdateACLEvent;

import org.omg.CORBA.CTX_RESTRICT_SCOPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：罚息复利账单同期合并
 *
 * @Author 18049705
 * @Date Created in 16:38 2018/11/12
 * @see
 */
@Scope("prototype")
@Repository
public class Lns252 extends WorkUnit {

    String acctNo;
    String brc;
    String flag;
    String trancode;


    @Override
    public void run() throws Exception {
        setBrc(tranctx.getBrc());
        setTrancode(tranctx.getTranCode());

        if("1".equals(flag))
            //合并罚息账本
            mergeBill();
        else{
            //将备份账本表的本金余额 同步到  账本表
            updatePrinBal();
        }

    }

    //合并罚息账本
    private void mergeBill()throws FabException{
        Map<String,String> param = new HashMap<>();
        param.put("acctno", acctNo);
        param.put("brc", tranctx.getBrc());

        //更新主文件 memo为1
        param.put("memo", "1");
        param.put("modifyDate", tranctx.getTranDate());
        try {
            DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_252", param);
        } catch (FabException e) {
            throw new FabException(e, "SPS102", "update_lnsbasicinfo_252");
        }

        //在备份表已经有的，不再备份
        List<Map<String, Object>> lnsbillListbak;

        try {
            lnsbillListbak = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbillbaks", param);
        } catch (FabSqlException e) {
            LoggerUtil.error("查询lnsbill表错误{}", e);
            throw new FabException("SPS103","query_hisbills");
        }
        if(lnsbillListbak!=null&&!lnsbillListbak.isEmpty()){
            return;
        }
        //先查询出来 插入备份表

        List<Map<String, Object>> lnsbillList;

        try {
            lnsbillList = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbills", param);
        } catch (FabSqlException e) {
            LoggerUtil.error("查询lnsbill表错误{}", e);
            throw new FabException("SPS102","query_hisbills");
        }

        //备份所有账本
        try {
            DbAccessUtil.batchUpdate("Lnsbill.bill_bak_insert", lnsbillList.toArray(new Map[lnsbillList.size()]));
        } catch (FabException e) {
            throw new FabException(e, "SPS100", "bill_bak_insert");
        }

        //计算罚息整期账单金额，update
        try {
            DbAccessUtil.execute("Lnsbill.bill_Consolidated_update", param);
        } catch (FabException e) {
            throw new FabException(e, "SPS102", "bill_Consolidated_update");
        }
        //删除该期的其他账单，保留endate最大（本期最后一条）
        try {
            DbAccessUtil.execute("Lnsbill.bill_Consolidated_delete", param);
        } catch (FabException e) {
            throw new FabException(e, "SPS101", "bill_Consolidated_delete");
        }
    }
    //将备份账本表的本金余额 同步到  账本表
    private void updatePrinBal()throws FabException{
        Map<String,String> param = new HashMap<>();
        param.put("acctno", acctNo);
        param.put("brc", brc );

        try {
            DbAccessUtil.execute("Lnsbill.bill_prinbal_update", param);
        } catch (FabException e) {
            throw new FabException(e, "SPS102", "bill_prinbal_update");
        }
        //更新主文件 memo为0
        param.put("memo", "0");
        param.put("modifyDate", tranctx.getTranDate());
        try {
            DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_252", param);
        } catch (FabException e) {
            throw new FabException(e, "SPS102", "update_lnsbasicinfo_252");
        }
    }

	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}

	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}

	/**
	 * @param brc the brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
	}

	/**
	 * @return the flag
	 */
	public String getFlag() {
		return flag;
	}

	/**
	 * @param flag the flag to set
	 */
	public void setFlag(String flag) {
		this.flag = flag;
	}

	/**
	 * @return the trancode
	 */
	public String getTrancode() {
		return trancode;
	}

	/**
	 * @param trancode the trancode to set
	 */
	public void setTrancode(String trancode) {
		this.trancode = trancode;
	}


    
}
