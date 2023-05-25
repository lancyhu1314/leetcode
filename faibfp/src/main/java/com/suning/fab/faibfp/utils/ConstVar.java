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

        public static final String REFUSED_CODES = "refusedCodes";
        public static final String PRODUCT_CODES = "productCodes";
        public static final String REALTIME_PRD = "realTimePrd";

        private KEYNAME() {
        }
    }

    public static final class PARAMETER {

        public static final String PRODUCTCODE = "productCode";
        public static final String SYSPRDCODE = "sysPrdCode";

        public static final String RECEIPTNO = "receiptNo";

        public static final String SYSRECEIPTNO = "sysReceiptNo";

        public static final String ACCTNO = "acctNo";

        public static final String OLD_ACCTO = "oldAcctNo";

        public static final String ROUTEID = "routeId";

        public static final String COUNTS = "counts";

        public static final String STATUS = "status";

        public static final String REPAYACCTNO = "repayAcctNo";

        public static final String CUSTOMID = "customId";

        public static final String REPAYCHANNEL = "repayChannel";

        public static final String CHANNELTYPE = "channelType";

        public static final String PKGLIST = "pkgList";

        public static final String ERRSERSEQ = "errSerSeq";

        public static final String ERRDATE = "errDate";

        public static final String TRANDATE = "tranDate";

        public static final String MERCHANTNO = "merchantNo";

        private PARAMETER() {
        }
    }

    public static final class ROUTETYPE {

        /**
         * 贷款账号
         */
        public static final String RECEIPTNO = "0";
        /**
         * 放款返回的流水号+trandate
         */
        public static final String SERSEQNO = "1";

        private ROUTETYPE() {
        }
    }

    public static final class TRANSFERSTATUS {

        /**
         * 未迁移
         */
        public static final String NOT_TRANSFER = "1";
        /**
         * 老系统处理中
         */
        public static final String OLD_PROCESSING = "2";
        /**
         * 迁移中
         */
        public static final String TRANSFERING = "3";
        /**
         * 迁移完成
         */
        public static final String END_TRANSFER = "4";

        private TRANSFERSTATUS() {
        }
    }

    private ConstVar() {
    }

}
