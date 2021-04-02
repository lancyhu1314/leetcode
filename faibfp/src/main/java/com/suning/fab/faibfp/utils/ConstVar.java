package com.suning.fab.faibfp.utils;

public class ConstVar {

    public static final class SEQUENCE {
        /**
         * 交易子序号
         */
        public static final String TXNSUBSEQ = "TXNSUBSEQ";

        private SEQUENCE() {
        }
    }

    public static final class SCMFILENAME {

        /**
         * 已迁移产品登记文件
         */
        public static final String MIGRATED_PRODUCTS = "MigratedProducts.properties";

        private SCMFILENAME() {
        }
    }

    public static final class CONFIGFILENAME {

        public static final String RSF_ELEMENTS_OLD = "rsf_elements_old";


        private CONFIGFILENAME() {
        }
    }

    public static final class KEYNAME {

        public static final String PRODUCT_CODES = "productCodes";

        private KEYNAME() {
        }
    }

    public static final class PARAMETER{

        public static final String PRODUCTCODE = "productCode";

        public static final String RECEIPTNO = "receiptNo";

        public static final String ACCTNO = "acctNo";

        public static final String ROUTEID = "routeId";

        public static final String REPAYACCTNO = "repayAcctNo";

        public static final String CUSTOMID = "customId";

        public static final String REPAYCHANNEL = "repayChannel";
        private PARAMETER(){}
    }


    private ConstVar() {
    }

}
