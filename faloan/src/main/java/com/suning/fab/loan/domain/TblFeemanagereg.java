package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.math.BigDecimal;

@Entity(name = "FEEMANAGEREG")
public class TblFeemanagereg {
    //公司代码
    private String brc = "";
    //账号
    private String acctno = "";
    //还款期数
    private Integer repayterm = 0;
    //    还款起日
    private String repayownbdate = "";
    //   还款止日
    private String repayownedate = "";
    //   本期起日
    private String repayintbdate = "";
    //    本期止日
    private String repayintedate = "";
    //    本期应还费用
    private BigDecimal termretfee = BigDecimal.valueOf(0.00);
    //  	已还费用
    private BigDecimal feeamt = BigDecimal.valueOf(0.00);
    //  	未还费用
    private BigDecimal noretfee = BigDecimal.valueOf(0.00);
    //   	天数
    private Integer days = 0;
    //    c逾期状态
    private String currentstat = "";
    //    结清状态
    private String settleflag = "";
    //    	本期还款日期
    private String termretdate = "";
    //    	修改日期
    private String modifydate = "";
    //    	修改时间
    private String modifytime = "";
    //   	备用字段1
    private String reserve1 = "";
    //   	备用字段2
    private BigDecimal reserve2 = BigDecimal.valueOf(0.00);

    public TblFeemanagereg() {

    }

    /**
     *
     * @return brc get
     */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /**
     *
     * @param brc set
     */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    /**
     *
     * @return acctno get
     */
    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    /**
     *
     * @param acctno  set
     */
    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    /**
     *
     * @return  repayterm get
     */
    @Column(name = "Repayterm")
    public Integer getRepayterm() {
        return repayterm;
    }

    /**
     *
     * @param repayterm  set
     */
    public void setRepayterm(Integer repayterm) {
        if (repayterm == null) {
            this.repayterm = 0;
        } else {
            this.repayterm = repayterm;
        }
    }

    /**
     * @return repayownbdate get
     */
    @Column(name = "Repayownbdate")
    public String getRepayownbdate() {
        return repayownbdate;
    }

    /**
     *
     * @param repayownbdate get
     */
    public void setRepayownbdate(String repayownbdate) {
        this.repayownbdate = repayownbdate == null ? "" : repayownbdate.trim();
    }

    /**
     *
     * @return repayownedate get
     */
    @Column(name = "Repayownedate")
    public String getRepayownedate() {
        return repayownedate;
    }

    /**
     *
     * @param repayownedate set
     */
    public void setRepayownedate(String repayownedate) {
        this.repayownedate = repayownedate == null ? "" : repayownedate.trim();
    }

    /**
     *
     * @return repayintbdate get
     */
    @Column(name = "Repayintbdate")
    public String getRepayintbdate() {
        return repayintbdate;
    }
    /**
     *
     * @param  repayintbdate 设置
     */
    public void setRepayintbdate(String repayintbdate) {
        this.repayintbdate = repayintbdate == null ? "" : repayintbdate.trim();
    }

    /**
     *
     * @return repayintedate get
     */
    @Column(name = "Repayintedate")
    public String getRepayintedate() {
        return repayintedate;
    }

    /**
     *
     * @param repayintedate set
     */
    public void setRepayintedate(String repayintedate) {
        this.repayintedate = repayintedate == null ? "" : repayintedate.trim();
    }

    /**
     *
     * @return  termretfee get
     */
    @Column(name = "Termretfee")
    public BigDecimal getTermretfee() {
        return termretfee;
    }

    /**
     *
     * @param termretfee set
     */
    public void setTermretfee(BigDecimal termretfee) {
        if (termretfee == null) {
            this.termretfee = BigDecimal.valueOf(0.00);
        } else {
            this.termretfee = termretfee;
        }
    }

    /**
     *
     * @return feeamt get
     */
    @Column(name = "Feeamt")
    public BigDecimal getFeeamt() {
        return feeamt;
    }

    /**
     *
     * @param feeamt set
     */
    public void setFeeamt(BigDecimal feeamt) {
        if (feeamt == null) {
            this.feeamt = BigDecimal.valueOf(0.00);
        } else {
            this.feeamt = feeamt;
        }
    }

    /**
     *
     * @return  BigDecimal  noretfee
     */
    @Column(name = "Noretfee")
    public BigDecimal getNoretfee() {
        return noretfee;
    }

    /**
     *
     * @param noretfee  set
     */
    public void setNoretfee(BigDecimal noretfee) {
        if (noretfee == null) {
            this.noretfee = BigDecimal.valueOf(0.00);
        } else {
            this.noretfee = noretfee;
        }
    }

    /**
     *
     * @return  days get
     */
    @Column(name = "Days")
    public Integer getDays() {
        return days;
    }

    /**
     *
     * @param days  set
     */
    public void setDays(Integer days) {
        if (days == null) {
            this.days = 0;
        } else {
            this.days = days;
        }
    }

    /**
     *
     * @return  currentstat get
     */
    @Column(name = "Currentstat")
    public String getCurrentstat() {
        return currentstat;
    }

    /**
     *
     * @param currentstat  set
     */
    public void setCurrentstat(String currentstat) {
        this.currentstat = currentstat == null ? "" : currentstat.trim();
    }

    /**
     *
     * @return settleflag get
     */
    @Column(name = "Settleflag")
    public String getSettleflag() {
        return settleflag;
    }

    /**
     *
     * @param settleflag  set
     */
    public void setSettleflag(String settleflag) {
        this.settleflag = settleflag == null ? "" : settleflag.trim();
    }

    /**
     *
     * @return termretdate get
     */
    @Column(name = "Termretdate")
    public String getTermretdate() {
        return termretdate;
    }

    /**
     *
     * @param termretdate set
     */
    public void setTermretdate(String termretdate) {
        this.termretdate = termretdate == null ? "" : termretdate.trim();
    }

    /**
     *
     * @return modifydate get
     */
    @Column(name = "Modifydate")
    public String getModifydate() {
        return modifydate;
    }

    /**
     *
     * @param modifydate set
     */
    public void setModifydate(String modifydate) {
        this.modifydate = modifydate == null ? "" : modifydate.trim();
    }

    /**
     *
     * @return modifytime
     */
    @Column(name = "Modifytime")
    public String getModifytime() {
        return modifytime;
    }

    /**
     *
     * @param modifytime set
     */
    public void setModifytime(String modifytime) {
        this.modifytime = modifytime == null ? "" : modifytime.trim();
    }

    /**
     *
     * @return reserve1 get
     */
    @Column(name = "Reserve1")
    public String getReserve1() {
        return reserve1;
    }

    /**
     *
     * @param reserve1 set
     */
    public void setReserve1(String reserve1) {
        this.reserve1 = reserve1 == null ? "" : reserve1.trim();
    }

    /**
     *
     * @return reserve2
     */
    @Column(name = "Reserve2")
    public BigDecimal getReserve2() {
        return reserve2;
    }

    /**
     *
     * @param reserve2 get
     */
    public void setReserve2(BigDecimal reserve2) {
        if (reserve2 == null) {
            this.reserve2 = BigDecimal.valueOf(0.00);
        } else {
            this.reserve2 = reserve2;
        }
    }
}