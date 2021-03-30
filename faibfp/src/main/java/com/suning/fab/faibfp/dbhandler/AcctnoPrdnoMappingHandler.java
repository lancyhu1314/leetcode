package com.suning.fab.faibfp.dbhandler;

import com.suning.fab.faibfp.bean.AcctnoPrdnoMapping;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.mulssyn.db.AbstractBaseDao;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉贷款账号和产品映射关系表DB处理类
 *
 * @Author 19043955
 * @Date 2021/3/26
 * @Version 1.0
 */
public class AcctnoPrdnoMappingHandler extends AbstractBaseDao {


    public AcctnoPrdnoMapping load(String receiptNo) {
        Map<String, Object> param = new HashMap<>();
        param.put(ConstVar.PARAMETER.RECEIPTNO, receiptNo);
        return this.selectOne("ACCTNOPRDNOMAPPING.selectByKey", param);
    }

    /**
     * 保存一条数据
     *
     * @param receiptNo
     * @param productCode
     */
    public void save(String receiptNo, String productCode) {
        AcctnoPrdnoMapping mapping = new AcctnoPrdnoMapping();
        mapping.setreceiptNo(receiptNo);
        mapping.setProductCode(productCode);
        this.insert("ACCTNOPRDNOMAPPING.insert", mapping);
    }


}
