/**
* @author 14050269 Howard
* @version 创建时间：2016年6月18日 下午6:18:43
* 类说明
*/
package com.suning.fab.tup4ml.elfin;

import com.suning.fab.model.common.AbstractDatagram;

public abstract class PlatConstant {
	private PlatConstant() {
		throw new IllegalStateException("PlatConstant class");
	}
	public static final class RSPVALUE{
		private RSPVALUE() {
			throw new IllegalStateException("RSPVALUE class");
		}
		public static final AbstractDatagram NOTSET = null;
	}
	public static final class RSPCODE	{
		private RSPCODE() {
			throw new IllegalStateException("RSPCODE class");
		}
		public static final String OK="000000";
		public static final String UNKNOWN="999999";
		public static final String DEFAULT=OK;
		public static final String DBERROR="TUP006";
		public static final String TRANSACTION_TIME_OUT="TUP007";
        public static final String TRANSACTION_RATE_LIMIT = "SPS141";

		/**
		 * DTF业务ID主键冲突
		 */
		public static final String DTF_PRIMARY_CONFILCT="TUP114";
		public static final String SERVICETYPEERROR="TUP104";
		public static final String VALIDATEERROR="TUP105";
		public static final String PROTOREGERROR="TUP106";
		public static final String THROWFAILPROTO="TUP108";
		public static final String IDEMPOTENCY="TUP109";
		public static final String APPLICATION_VALIDATE_ERROR="TUP110";
		public static final String TRANS_TIME_VALIDATE_ERROR="TUP111";
        /**
         * 公共考核字段校验不通过
         */
        public static final String CHECK_FIELD_VALIDATE_ERROR = "TUP115";
		public static final String PREFIX="TUP";
	}
	public static final class RSPMSG{
		private RSPMSG() {
			throw new IllegalStateException("RSPMSG class");
		}
		public static final String OK="交易成功";
		public static final String UNKNOWN="INTERVAL ERROR";
		public static final String DEFAULT=OK;
	}

    public static final class SCMFILENAME{
        private SCMFILENAME() {
            throw new IllegalStateException("SCMFILENAME class");
        }

        public static final String GLOBAL_SCM="GlobalScm.properties";
    }

    /**
     * 应用层properties配置文件
     */
    public static final class PROPERFILENAME {
        private PROPERFILENAME() {
            throw new IllegalStateException("PROPERFILENAME class");
        }

        public static final String CHECK_FIELD_TRANS_PROPER = "checkfieldtrans";
    }

    /**
     * 应用层properties配置文件
     */
    public static final class SCMFIELDNAME {
        private SCMFIELDNAME() {
            throw new IllegalStateException("SCMFIELDNAME class");
        }

        public static final String ASSESS_FIELD_FLAG = "AssessFieldFlag";
    }

	public static final class PLATCONST{
	    private PLATCONST() {
	        throw new IllegalStateException("PLATCONST class");
	    }
	    /**
	     * 存入ThreadLocal中的调用链常量key
	     */
	    public static final String TRANS_CALL_CHAIN = "PLAT_TRANSCALLCHAIN";
	    
	    /**
	     * 场景
	     */
	    public static final String TRANS_SCENE = "PLAT_TRANS_SCENE";

	    /**
	     * 场景中的流水
	     */
	    public static final String TRANS_SCENE_OUT_SERIAL_NUM = "PLAT_TRANS_SERIALNUM";
	}
}
