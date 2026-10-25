package com.bri.vo;

import java.math.BigDecimal;

/**
 * 超计划拨付行（Q4-5）。
 *
 * <p>口径：同一项目同一计划年度，实际拨付合计超过年度计划金额的比例。
 * 合同金额口径由 A 的 t_contract 提供，需要时把 SQL 的 plan_total 换成合同金额即可（见 docs/B-7）。</p>
 *
 * @author B
 */
public class OverrunRow {

    private long projectId;
    private String projectCode;
    private String projectName;
    private int planYear;
    private BigDecimal planAmount;
    private BigDecimal actualAmount;
    private BigDecimal overrunPct;

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

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public BigDecimal getOverrunPct() {
        return overrunPct;
    }

    public void setOverrunPct(BigDecimal overrunPct) {
        this.overrunPct = overrunPct;
    }

    @Override
    public String toString() {
        return "OverrunRow{" + projectCode + ", " + planYear + ", plan=" + planAmount
                + ", actual=" + actualAmount + ", over=" + overrunPct + "%}";
    }
}
