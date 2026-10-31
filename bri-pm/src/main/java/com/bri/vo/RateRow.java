package com.bri.vo;

import java.math.BigDecimal;

/**
 * 项目资金到位率行（Q4-1 / Q4-2，来自视图 v_fund_rate）。
 *
 * <p>{@code ratePct} 为 {@code null} 表示“没有年度计划，无从计算”，
 * 与 0% 不是一回事，界面应显示“—”。</p>
 *
 * @author B
 */
public class RateRow {

    private long projectId;
    private String projectCode;
    private String projectName;
    private String status;
    private BigDecimal planTotal;
    private BigDecimal actualTotal;
    private BigDecimal ratePct;

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    @Override
    public String toString() {
        return "RateRow{" + projectCode + ", plan=" + planTotal + ", actual=" + actualTotal
                + ", rate=" + ratePct + "%}";
    }
}
