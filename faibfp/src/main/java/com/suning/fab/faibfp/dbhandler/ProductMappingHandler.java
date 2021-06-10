package com.suning.fab.faibfp.dbhandler;

import com.suning.fab.faibfp.bean.ProductMapping;
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
public class ProductMappingHandler extends AbstractBaseDao {


    /**
     * 借据号或者开户成功返回的serseqno
     *
     * @param routeId
     * @return
     */
    public ProductMapping load(String routeId) {
        Map<String, Object> param = new HashMap<>();
        param.put(ConstVar.PARAMETER.ROUTEID, routeId);
        return this.selectOne("PRODUCTMAPPING.selectByKey", param);
    }

    /**
     * 保存一条数据
     *
     * @param receiptNo
     * @param productCode
     */
    public void save(String routeId, String receiptNo, String productCode, String routeType) {
        ProductMapping mapping = new ProductMapping();
        mapping.setRouteId(routeId);
        mapping.setReceiptNo(receiptNo);
        mapping.setProductCode(productCode);
        mapping.setRouteType(routeType);
        this.insert("PRODUCTMAPPING.insert", mapping);
    }

    /**
     * 更新源数据
     *
     * @param routeId
     * @param receiptNo
     * @param productCode
     */
    public void update(String routeId, String receiptNo, String productCode, String routeType) {
        Map<String, Object> param = new HashMap<>();
        param.put("routeId", routeId);
        param.put("receiptNo", receiptNo);
        param.put("productCode", productCode);
        param.put("routeType", routeType);
        this.update("PRODUCTMAPPING.update", param);
    }


}
