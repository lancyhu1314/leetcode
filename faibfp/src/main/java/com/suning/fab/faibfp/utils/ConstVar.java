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


    public static final class CCY {
        /**
         * 零钱宝标准账户
         */
        public static final String CNY = "CNY";

        private CCY() {
        }
    }


    public static final class RSPCODE {
        /**
         * 交易层
         */
        public static final class TRAN {
            /**
             * 成功
             */
            public static final String SUCCESS = "000000";

            private TRAN() {
            }
        }

        private RSPCODE() {
        }
    }

    public static final class BRIEFCODE {

        /**
         * 还款冲销
         */
        public static final String HKCX = "还款冲销";

        private BRIEFCODE() {
        }
    }

    public static final class SQLCODE {
        /**
         * 重复数据
         */
        public static final Integer DUPLICATIONKEY = -803;
        /**
         * check条件不满足
         */
        public static final Integer VIOLATED = -545;

        private SQLCODE() {
        }
    }

    public static final class OPERATOR {
        /**
         * 创建
         */
        public static final String CREATE = "CRT";
        /**
         * 加款
         */
        public static final String INCREASE = "ADD";
        /**
         * 减款
         */
        public static final String REDUCTION = "SUB";
        /**
         * 冲销
         */
        public static final String BAK = "BAK";
        /**
         * 更新
         */
        public static final String UPDATE = "UPD";

        /**
         * 结息
         */
        public static final String INTEREST_SET = "SET";

        private OPERATOR() {
        }

    }

    public static final class DATASTATUS {

        /**
         * 正常
         */
        public static final String NORMAL = "NORMAL";
        /**
         * 作废
         */
        public static final String CANCEL = "CANCEL";

        /**
         * 冲销
         */
        public static final String WRTOFF = "WRTOFF";

        private DATASTATUS() {
        }
    }

    public static final class BILL {

        public static final class BILLSTATUS {

            /**
             * 正常
             */
            public static final String NORMAL = "N";
            /**
             * 逾期
             */
            public static final String OVERDUE = "O";
            /**
             * 呆滞
             */
            public static final String L = "L";
            /**
             * 呆账
             */
            public static final String BAD = "B";
            /**
             * 宽限期
             */
            public static final String GRACE = "G";


            private BILLSTATUS() {
            }
        }

        public static final class BILLTYPE {
            /**
             * 本金
             */
            public static final String PRIN = "PRIN";
            /**
             * 利息
             */
            public static final String NINT = "NINT";
            /**
             * 费用
             */
            public static final String NFEE = "NFEE";
            /**
             * 罚息
             */
            public static final String DINT = "DINT";
            /**
             * 复利
             */
            public static final String CINT = "CINT";
            /**
             * 违约金
             */
            public static final String DBWY = "DBWY";


            private BILLTYPE() {
            }
        }

        public static final class BILLPROPERTY {

            /**
             * 正常结息
             */
            public static final String INTSET = "INTSET";
            /**
             * 还款
             */
            public static final String REPAY = "REPAY";
            /**
             * 退货
             */
            public static final String RETURN = "RETURN";
            /**
             * 债转
             */
            public static final String SWITCH = "SWITCH";
            /**
             * 代偿
             */
            public static final String COMPEN = "COMPEN";
            /**
             * 迁移
             */
            public static final String TRANS = "TRANS";


            private BILLPROPERTY() {
            }
        }

        public static final class SETTLEFLAG {

            /**
             * 未结
             */
            public static final String RUNNING = "RUNNING";
            /**
             * 已结
             */
            public static final String CLOSE = "CLOSE";


            private SETTLEFLAG() {
            }
        }


        private BILL() {
        }
    }

    public static final class LOANSTAT {
        /**
         * 正常
         */
        public static final String NORMAL = "N";
        /**
         * 核销
         */
        public static final String CANCEL = "C";
        /**
         * 结清
         */
        public static final String SETTING = "CA";


        private LOANSTAT() {
        }
    }

    /**
     * 五级分类装填
     */
    public static final class CANSTAT {

        /**
         * 正常
         */
        public static final String NORMAL = "N";
        /**
         * 部分逾期
         */
        public static final String PO = "PO";
        /**
         * 部分呆滞
         */
        public static final String PL = "PL";
        /**
         * 部分呆账
         */
        public static final String PB = "PB";
        /**
         * 整笔逾期
         */
        public static final String O = "O";
        /**
         * 整笔呆滞
         */
        public static final String L = "L";
        /**
         * 整笔呆账
         */
        public static final String B = "B";


        private CANSTAT() {
        }
    }

    public static final class COLUMN {

        public static final String BILLAMT = "billAmt";
        public static final String BILLBAL = "billBal";
        public static final String INTEDATE = "inteDate";
        public static final String SETTLEFLAG = "settleFlag";
        public static final String BILLSTATUS = "billStatus";
        public static final String CONTRACTBAL = "contractBal";
        public static final String CONTRACTAMT = "contractamt";
        public static final String PRINPERFORMULA = "prinperformula";
        public static final String CONTDUEDATE = "contduedate";
        public static final String REPAYMENTDATE = "repaymentDate";
        public static final String LASTPRINDATE = "lastPrinDate";
        public static final String LASTINTFEEDATE = "lastIntfeeDate";
        public static final String CANSTAT = "canStat";
        public static final String LOANSTAT = "loanstat";
        public static final String INTFEESTAT = "intfeestat";
        public static final String ASSISTACCBAL = "bal";
        public static final String REPAYMENT = "repayment";
        public static final String RESERV1 = "reserv1";
        public static final String CURRBAL = "currbal";
        public static final String LASTBAL = "lastbal";
        public static final String PRETRANDATE = "pretrandate";
        public static final String CUSTTYPE = "custtype";
        public static final String STATUS = "status";
        public static final String INTFEEFORMULA = "intfeeformula";
        public static final String VALUE1 = "value1";
        public static final String VALUE2 = "value2";
        public static final String VALUE3 = "value3";
        public static final String OVERDUERATE = "overduerate";
        public static final String INTFEERATE = "intfeerate";
        public static final String OVERRATE = "overrate";
        public static final String BILLPROPERTY = "billproperty";




        private COLUMN() {
        }
    }

    /**
     * 产品配置key值
     */
    public static final class PRODUCT_KEY {

        //是否固定封顶
        public static final String ISFIXEDCAP = "isFixedCap";
        //是否动态封顶
        public static final String ISDYNAMICCAP = "isDynamicCap";
        //是否允许提前还款
        public static final String ISADVANCEREPAY = "isAdvanceRepay";
        //宽限期计息
        public static final String ISCALGRACE = "isCalGrace";
        //是否计利息
        public static final String ISCALINT = "isCalInt";
        //是否计罚息
        public static final String ISCALDINT = "isCalDInt";
        //是否计复利
        public static final String ISCALCINT = "isCalCInt";
        //是否计违约金
        public static final String ISCALDBWY = "isCalDBWY";
        //是否计本息转列
        public static final String ISPRINTRANS = "isPrinTrans";
        //是否计本金转列
        public static final String ISNINTTRANS = "isNintTrans";
        //罚息来源：1-剩余本金 4-未还本金
        public static final String SOURCEOFDINT = "sourceOfDInt";
        //是否有到期日
        public static final String NEEDDUEDATE = "needDueDate";
        //最小天数
        public static final String MINDAYS = "minDays";
        //是否首期跨月
        public static final String FIRSTACRMON = "firstAcrMon";
        //是否中间期合并
        public static final String MERGEMIDDMON = "mergeMiddMon";
        //是否最后一期合并
        public static final String MERGELASTPERD = "mergeLastPerd";
        //还款方式限制
        public static final String REPAYWAYLIMIT = "repayWayLimit";
        //提前结清手续费
        public static final String ADVANCESETFEETYPE = "advanceSetFeeType";
        //还款/减免明细展示
        public static final String ISSHOWREPAYLIST = "isShowRepayList";


        private PRODUCT_KEY() {
        }
    }

    public static final class EVN {

        /**
         * 生产环境
         */
        public static final String PROD = "PROD";

        private EVN() {
        }
    }

    /**
     * properties文件中的key
     */
    public static final class PROPERTY_KEY {

        /**
         * 环境名称
         */
        public static final String EVN = "Evn";

        private PROPERTY_KEY() {
        }
    }

    public static final class COMMON {

        /**
         * 初始日期
         */
        public static final String INIT_DATE = "1970-01-01";

        private COMMON() {
        }
    }

    public static final class CONTRACT_EX_KEY {

        /**
         * 冲销日期
         */
        public static final String CXRQ = "CXRQ";

        private CONTRACT_EX_KEY() {
        }
    }

    public static final class TRANSFER {

        /**
         * 费用
         */
        public static final String FEE = "fee";
        /**
         * 本金、利息
         */
        public static final String COM = "com";

        private TRANSFER() {
        }
    }

    private ConstVar() {
    }

}
