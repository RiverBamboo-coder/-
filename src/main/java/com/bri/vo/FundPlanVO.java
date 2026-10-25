package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 年度资金计划行（M4 · t_fund_plan）。
 *
 * <p>字段即表字段，界面直接绑表格；金额一律 {@link BigDecimal}，日期一律 {@link LocalDate}，
 * 主键用 {@code long}（项目约定：不用 double 存金额、不用 int 存主键）。</p>
 *
 * @author B
 */
public class FundPlanVO {

    /** 计划主键 */
    private long planId;

    /** 项目主键 */
    private long projectId;

    /** 项目编号（联表带出，界面显示用） */
    private String projectCode;

    /** 项目名称（联表带出） */
    private String projectName;

    /** 计划年度 */
    private int planYear;

    /** 年度计划金额（万元） */
    private BigDecimal planAmount;

    /** 币种 */
    private String currencyCode;

    /** 批复日期 */
    private LocalDate approvedDate;

    /** 备注 */
    private String remark;

    public long getPlanId() {
        return planId;
    }

    public void setPlanId(long planId) {
        this.planId = planId;
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

    public int getPlanYear() {
        return planYear;
    }

    public void setPlanYear(int planYear) {
        this.planYear = planYear;
    }

    public BigDecimal getPlanAmount() {
        return planAmount;
    }

    public void setPlanAmount(BigDecimal planAmount) {
        this.planAmount = planAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public LocalDate getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDate approvedDate) {
        this.approvedDate = approvedDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "FundPlanVO{planId=" + planId + ", projectId=" + projectId + ", planYear=" + planYear
                + ", planAmount=" + planAmount + "}";
    }
}
