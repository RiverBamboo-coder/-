package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资金拨付行（M4 · t_fund_disbursement，联 t_project / t_funding_source / t_fund_plan）。
 *
 * @author B
 */
public class DisbVO {

    /** 拨付主键 */
    private long disbId;

    /** 项目主键 */
    private long projectId;

    /** 项目编号（联表带出） */
    private String projectCode;

    /** 项目名称（联表带出） */
    private String projectName;

    /** 资金来源主键 */
    private long sourceId;

    /** 资金来源名称（联表带出） */
    private String sourceName;

    /** 对应年度计划主键，可为 null（计划外拨付） */
    private Long planId;

    /** 对应计划年度（联表带出，可为 null） */
    private Integer planYear;

    /** 拨付日期 */
    private LocalDate disbDate;

    /** 拨付金额（万元） */
    private BigDecimal disbAmount;

    /** 币种 */
    private String currencyCode;

    /** 用途 */
    private String purpose;

    public long getDisbId() {
        return disbId;
    }

    public void setDisbId(long disbId) {
        this.disbId = disbId;
    }

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

    public long getSourceId() {
        return sourceId;
    }

    public void setSourceId(long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Integer getPlanYear() {
        return planYear;
    }

    public void setPlanYear(Integer planYear) {
        this.planYear = planYear;
    }

    public LocalDate getDisbDate() {
        return disbDate;
    }

    public void setDisbDate(LocalDate disbDate) {
        this.disbDate = disbDate;
    }

    public BigDecimal getDisbAmount() {
        return disbAmount;
    }

    public void setDisbAmount(BigDecimal disbAmount) {
        this.disbAmount = disbAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    @Override
    public String toString() {
        return "DisbVO{disbId=" + disbId + ", projectId=" + projectId + ", disbDate=" + disbDate
                + ", disbAmount=" + disbAmount + "}";
    }
}
