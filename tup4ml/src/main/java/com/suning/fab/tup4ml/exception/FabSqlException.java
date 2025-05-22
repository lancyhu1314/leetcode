
package com.suning.fab.tup4ml.exception;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import com.suning.fab.tup4ml.elfin.Pair;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.utils.LoggerUtil;


/**
 * @author 14050269 Howard
 * @since 2016年7月5日 下午9:47:20
 * 数据库操作异常类
 */
public class FabSqlException extends FabRuntimeException {
	private static final long serialVersionUID = 1L;

	//xml里sql的id
	private final String sqlId;

	//数据库错误码
	private final String sqlCode;

	//数据库错误状态
	private final String sqlState;

	public FabSqlException(Exception e, String sqlId) {
		super(e, PlatConstant.RSPCODE.DBERROR);
		
		LoggerUtil.info("异常sqlId:{},堆栈异常:{}",sqlId,e);

		this.sqlId = sqlId;
		if(e instanceof DataAccessException) {
			Pair<String, String> x = parseMessage((DataAccessException)e);
			this.sqlCode = x.getFirst();
			this.sqlState = x.getSecond();
		}else {
			this.sqlCode = "-999";
			this.sqlState = "-999";
		}
	}

	public String getSqlId() {
		return sqlId;
	}

	public String getSqlCode() {
		return sqlCode;
	}

	public String getSqlState() {
		return sqlState;
	}

	/**
	 * Insert或Update数据时违反了完整性，例如违反了惟一性限制
	 * @return true -- 违反了惟一性限制异常；false -- 其他异常；
	 */
	public boolean isIntegrityViolationException(){
		return this.getCause() instanceof DataIntegrityViolationException;
	}

	protected Pair<String, String> parseMessage(DataAccessException dae) {
		try {
			if (dae.getCause() instanceof SQLException) {
				SQLException sqlExp = (SQLException)dae.getCause();
				Integer sqlCode = sqlExp.getErrorCode();
				return new Pair<>(sqlCode.toString(), sqlExp.getSQLState());
			} 
		} catch (NumberFormatException e) {
		}
		return new Pair<>("-999", "-999");
	}
	
    @Override
	public String getErrMsg() {
	    String msg = super.getErrMsg();
        String message;
        try {
            message = String.format(msg, sqlCode);
        }catch(Exception e){
            message = msg;
        }
        return message;
	}
	
    @Override
    public String getMessage() {  
        
        String msg = super.getMessage();
        String message;
        try {
            message = String.format(msg, sqlCode);
        }catch(Exception e){
            message = msg;
        }
        return message;
    }

}
