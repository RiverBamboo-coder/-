package com.bri.vo;

import java.math.BigDecimal;

/** 年度计划 vs 实际行（Q4-4）。 @author B */
public class YearRow {

    private int planYear;
    private BigDecimal planTotal;
    private BigDecimal actualTotal;
    private BigDecimal ratePct;
    private int projectCnt;

    public int getPlanYear() {
        return planYear;
    }

    public void setPlanYear(int planYear) {
        this.planYear = planYear;
    }

    public BigDecimal getPlanTotal() {
        return planTotal;
    }

    public void setPlanTotal(BigDecimal planTotal) {
        this.planTotal = planTotal;
    }

    public BigDecimal getActualTotal() {
        return actualTotal;
    }

    public void setActualTotal(BigDecimal actualTotal) {
        this.actualTotal = actualTotal;
    }

    public BigDecimal getRatePct() {
        return ratePct;
    }

    public void setRatePct(BigDecimal ratePct) {
        this.ratePct = ratePct;
    }

    public int getProjectCnt() {
        return projectCnt;
    }

    public void setProjectCnt(int projectCnt) {
        this.projectCnt = projectCnt;
    }

    @Override
    public String toString() {
        return "YearRow{" + planYear + ", plan=" + planTotal + ", actual=" + actualTotal
                + ", rate=" + ratePct + "%}";
    }
}
