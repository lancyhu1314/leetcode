/*
 * Copyright (C), 2002-2019, 苏宁易购电子商务有限公司
 * FileName: IValidator
 * Author:   17060915
 * Date:     2019/8/17 14:23
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名     修改时间     版本号        描述
 */
package com.suning.fab.tup4ml.service;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.exception.FabException;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 *
 * @author 17060915
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public interface IValidator {

    boolean validate(AbstractDatagram param) throws FabException;

    boolean validate(AbstractDatagram param, Class<? extends ServiceTemplate> cls) throws FabException;

}
