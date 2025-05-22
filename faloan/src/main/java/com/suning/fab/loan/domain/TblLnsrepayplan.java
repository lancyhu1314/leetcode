package com.suning.fab.loan.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSREPAYPLAN")
public class TblLnsrepayplan implements Serializable{
    private String acctno = "";

    private String brc = "";

    private Integer repayterm = 0;

    private String acctflag = "";

    private String repayway = "";

    private String repayownbdate = "";

    private String repayownedate = "";

    private String repayintbdate = "";

    private String repayintedate = "";

    private Double termretprin = 0.00;

    private Double termretint = 0.00;

    private Double deductionamt = 0.00;

    private Double prinamt = 0.00;

    private Double intamt = 0.00;

    private Double noretamt = 0.00;

    private Double noretint = 0.00;

    private Double termfint = 0.00;

    private Double termcint = 0.00;

    private Double sumfint = 0.00;

    private Double sumcint = 0.00;

    private Integer days = 0;

    private Double termcdint = 0.00;

    private Double balance = 0.00;

    private Double sumrfint = 0.00;

    private Double sumrcint = 0.00;

    private String actrepaydate = "";

    private String termretdate = "";

    private String modifydate = "";

    private String modifytime = "";

    private String reserve1 = "";

    private Double reserve2 = 0.00;

    public TblLnsrepayplan() {
         
    }

    /** 账号 */
    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    /** 账号 */
    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    /** 公司代码 */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /** 公司代码 */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    @Column(name = "Repayterm")
    public Integer getRepayterm() {
        return repayterm;
    }

    public void setRepayterm(Integer repayterm) {
        if(repayterm == null){
            this.repayterm = 0;
        }else{
            this.repayterm = repayterm;
        }
    }

    /** 账户状态 */
    @Column(name = "Acctflag")
    public String getAcctflag() {
        return acctflag;
    }

    /** 账户状态 */
    public void setAcctflag(String acctflag) {
        this.acctflag = acctflag == null ? "" : acctflag.trim();
    }

    /** 本期方式 */
    @Column(name = "Repayway")
    public String getRepayway() {
        return repayway;
    }

    /** 本期方式 */
    public void setRepayway(String repayway) {
        this.repayway = repayway == null ? "" : repayway.trim();
    }

    /** 还款起日 */
    @Column(name = "Repayownbdate")
    public String getRepayownbdate() {
        return repayownbdate;
    }

    /** 还款起日 */
    public void setRepayownbdate(String repayownbdate) {
        this.repayownbdate = repayownbdate == null ? "" : repayownbdate.trim();
    }

    /** 还款止日 */
    @Column(name = "Repayownedate")
    public String getRepayownedate() {
        return repayownedate;
    }

    /** 还款止日 */
    public void setRepayownedate(String repayownedate) {
        this.repayownedate = repayownedate == null ? "" : repayownedate.trim();
    }

    /** 本期起日 */
    @Column(name = "Repayintbdate")
    public String getRepayintbdate() {
        return repayintbdate;
    }

    /** 本期起日 */
    public void setRepayintbdate(String repayintbdate) {
        this.repayintbdate = repayintbdate == null ? "" : repayintbdate.trim();
    }

    /** 本期止日 */
    @Column(name = "Repayintedate")
    public String getRepayintedate() {
        return repayintedate;
    }

    /** 本期止日 */
    public void setRepayintedate(String repayintedate) {
        this.repayintedate = repayintedate == null ? "" : repayintedate.trim();
    }

    /** 本期应还本金 */
    @Column(name = "Termretprin")
    public Double getTermretprin() {
        return termretprin;
    }

    /** 本期应还本金 */
    public void setTermretprin(Double termretprin) {
        if(termretprin == null){
            this.termretprin = 0.00;
        }else{
            this.termretprin = termretprin;
        }
    }

    /** 本期应还利息 */
    @Column(name = "Termretint")
    public Double getTermretint() {
        return termretint;
    }

    /** 本期应还利息 */
    public void setTermretint(Double termretint) {
        if(termretint == null){
            this.termretint = 0.00;
        }else{
            this.termretint = termretint;
        }
    }

    /** 扣息金额 */
    @Column(name = "Deductionamt")
    public Double getDeductionamt() {
        return deductionamt;
    }

    /** 扣息金额 */
    public void setDeductionamt(Double deductionamt) {
        if(deductionamt == null){
            this.deductionamt = 0.00;
        }else{
            this.deductionamt = deductionamt;
        }
    }

    /** 已还本金 */
    @Column(name = "Prinamt")
    public Double getPrinamt() {
        return prinamt;
    }

    /** 已还本金 */
    public void setPrinamt(Double prinamt) {
        if(prinamt == null){
            this.prinamt = 0.00;
        }else{
            this.prinamt = prinamt;
        }
    }

    /** 已还利息 */
    @Column(name = "Intamt")
    public Double getIntamt() {
        return intamt;
    }

    /** 已还利息 */
    public void setIntamt(Double intamt) {
        if(intamt == null){
            this.intamt = 0.00;
        }else{
            this.intamt = intamt;
        }
    }

    /** 未还本金 */
    @Column(name = "Noretamt")
    public Double getNoretamt() {
        return noretamt;
    }

    /** 未还本金 */
    public void setNoretamt(Double noretamt) {
        if(noretamt == null){
            this.noretamt = 0.00;
        }else{
            this.noretamt = noretamt;
        }
    }

    /** 未还利息 */
    @Column(name = "Noretint")
    public Double getNoretint() {
        return noretint;
    }

    /** 未还利息 */
    public void setNoretint(Double noretint) {
        if(noretint == null){
            this.noretint = 0.00;
        }else{
            this.noretint = noretint;
        }
    }

    /** 未还罚息 */
    @Column(name = "Termfint")
    public Double getTermfint() {
        return termfint;
    }

    /** 未还罚息 */
    public void setTermfint(Double termfint) {
        if(termfint == null){
            this.termfint = 0.00;
        }else{
            this.termfint = termfint;
        }
    }

    /** 未还复利 */
    @Column(name = "Termcint")
    public Double getTermcint() {
        return termcint;
    }

    /** 未还复利 */
    public void setTermcint(Double termcint) {
        if(termcint == null){
            this.termcint = 0.00;
        }else{
            this.termcint = termcint;
        }
    }

    /** 累计罚息 */
    @Column(name = "Sumfint")
    public Double getSumfint() {
        return sumfint;
    }

    /** 累计罚息 */
    public void setSumfint(Double sumfint) {
        if(sumfint == null){
            this.sumfint = 0.00;
        }else{
            this.sumfint = sumfint;
        }
    }

    /** 累计复利 */
    @Column(name = "Sumcint")
    public Double getSumcint() {
        return sumcint;
    }

    /** 累计复利 */
    public void setSumcint(Double sumcint) {
        if(sumcint == null){
            this.sumcint = 0.00;
        }else{
            this.sumcint = sumcint;
        }
    }

    /** 记息天数 */
    @Column(name = "Days")
    public Integer getDays() {
        return days;
    }

    /** 记息天数 */
    public void setDays(Integer days) {
        if(days == null){
            this.days = 0;
        }else{
            this.days = days;
        }
    }

    /** 剩余结息利息 */
    @Column(name = "Termcdint")
    public Double getTermcdint() {
        return termcdint;
    }

    /** 剩余结息利息 */
    public void setTermcdint(Double termcdint) {
        if(termcdint == null){
            this.termcdint = 0.00;
        }else{
            this.termcdint = termcdint;
        }
    }

    /** 贷款剩余本金 */
    @Column(name = "Balance")
    public Double getBalance() {
        return balance;
    }

    /** 贷款剩余本金 */
    public void setBalance(Double balance) {
        if(balance == null){
            this.balance = 0.00;
        }else{
            this.balance = balance;
        }
    }

    /** 已还罚息 */
    @Column(name = "Sumrfint")
    public Double getSumrfint() {
        return sumrfint;
    }

    /** 已还罚息 */
    public void setSumrfint(Double sumrfint) {
        if(sumrfint == null){
            this.sumrfint = 0.00;
        }else{
            this.sumrfint = sumrfint;
        }
    }

    /** 已还复利 */
    @Column(name = "Sumrcint")
    public Double getSumrcint() {
        return sumrcint;
    }

    /** 已还复利 */
    public void setSumrcint(Double sumrcint) {
        if(sumrcint == null){
            this.sumrcint = 0.00;
        }else{
            this.sumrcint = sumrcint;
        }
    }

    /** 本期实际止日 */
    @Column(name = "Actrepaydate")
    public String getActrepaydate() {
        return actrepaydate;
    }

    /** 本期实际止日 */
    public void setActrepaydate(String actrepaydate) {
        this.actrepaydate = actrepaydate == null ? "" : actrepaydate.trim();
    }

    /** 本期还款日期 */
    @Column(name = "Termretdate")
    public String getTermretdate() {
        return termretdate;
    }

    /** 本期还款日期 */
    public void setTermretdate(String termretdate) {
        this.termretdate = termretdate == null ? "" : termretdate.trim();
    }

    /** 修改日期 */
    @Column(name = "Modifydate")
    public String getModifydate() {
        return modifydate;
    }

    /** 修改日期 */
    public void setModifydate(String modifydate) {
        this.modifydate = modifydate == null ? "" : modifydate.trim();
    }

    /** 修改时间 */
    @Column(name = "Modifytime")
    public String getModifytime() {
        return modifytime;
    }

    /** 修改时间 */
    public void setModifytime(String modifytime) {
        this.modifytime = modifytime == null ? "" : modifytime.trim();
    }

    /** 备用字段1 */
    @Column(name = "Reserve1")
    public String getReserve1() {
        return reserve1;
    }

    /** 备用字段1 */
    public void setReserve1(String reserve1) {
        this.reserve1 = reserve1 == null ? "" : reserve1.trim();
    }

    /** 备用字段2 */
    @Column(name = "Reserve2")
    public Double getReserve2() {
        return reserve2;
    }

    /** 备用字段2 */
    public void setReserve2(Double reserve2) {
        if(reserve2 == null){
            this.reserve2 = 0.00;
        }else{
            this.reserve2 = reserve2;
        }
    }
}