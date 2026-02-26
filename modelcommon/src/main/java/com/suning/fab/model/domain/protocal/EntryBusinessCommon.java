package com.suning.fab.model.domain.protocal;

import java.io.Serializable;
import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.common.CommonCheckedField;
import com.suning.fab.model.constant.CommonConstant;
import com.suning.fab.model.utils.VarChecker;

/**
 * 请求报文的公共字段；
 * @author 16030888
 *
 */
public abstract class EntryBusinessCommon extends AbstractDatagram implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主机交易码；
	 */
	protected String tranCode;
	
	/**
	 * 渠道日期，格式：yyyy-MM-dd；
	 */
	@JSONField(format="yyyy-MM-dd")
	protected Date termDate;
	
	/**
	 * 渠道代码；
	 */
	protected String channelId;
	
	/**
	 * 交易机构（常量：51000000）；
	 */
	protected String brc;
	
	/**
	 * 柜员号；
	 */
	protected String teller;

    /**
     * 公共检查字段
     */
    protected CommonCheckedField commonCheckedField;

	public EntryBusinessCommon(){
		//不做任何操作
	}

	/**
	 * 创建报文协议公共字段域；
	 * @param tranCode 主机交易码；
	 * @param termDate 交易日期，格式：YYYY-MM-DD；
	 * @param channelId 渠道代码；
	 * @param brc 交易机构（常量：51000000）；
	 * @param teller 柜员号；
	 */
	public EntryBusinessCommon(String tranCode, Date termDate,String channelId, String brc, String teller) {
		if(VarChecker.isEmpty(tranCode) 
				|| VarChecker.isEmpty(termDate) 
				|| VarChecker.isEmpty(channelId) 
				|| VarChecker.isEmpty(brc) 
				|| VarChecker.isEmpty(teller)){
			throw new IllegalArgumentException(CommonConstant.ExceptionConstant.ARGUMENTISNULL);
		}
		this.tranCode = tranCode;
		this.termDate = termDate;
		this.channelId = channelId;
		this.brc = brc;
		this.teller = teller;
	}

	/**
	 * 获取交易代码；
	 * @return 返回交易代码；
	 */
	public String getTranCode() {
		return tranCode;
	}

	/**
	 * 获取渠道请求日期；
	 * @return 返回渠道请求日期；
	 */
	public Date getTermDate() {
		return termDate;
	}

	/**
	 * 获取渠道代码；
	 * @return 返回渠道代码；
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * 获取机构代码；
	 * @return 返回机构代码；
	 */
	public String getBrc() {
		return brc;
	}

	/**
	 * 获取柜员代码；
	 * @return 返回柜员代码；
	 */
    public String getTeller() {
        return teller;
    }

    /**
     * 获取公共检查字段
     *
     * @return 返回公共检查字段；
     */
    public CommonCheckedField getCommonCheckedField() {
        return commonCheckedField;
    }

    /**
     * 设置公共检查字段
     *
     * @return 返回公共检查字段；
     */
    public void setCommonCheckedField(CommonCheckedField commonCheckedField) {
        this.commonCheckedField = commonCheckedField;
    }

	@Override
	public boolean validate() {
		return !(VarChecker.isEmpty(tranCode) 
				|| VarChecker.isEmpty(termDate) 
				|| VarChecker.isEmpty(channelId)
                || VarChecker.isEmpty(brc)
                || VarChecker.isEmpty(teller)
		);
	}

}
