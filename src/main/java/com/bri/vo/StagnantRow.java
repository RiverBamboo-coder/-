package com.bri.vo;

import java.math.BigDecimal;

/**
 * 停滞项目行（Q6-3，gaps-and-islands：连续 {@code months} 个月进度没有变化）。
 *
 * @author B
 */
public class StagnantRow {

    private long projectId;
    private String projectCode;
    private String projectName;
    private String startYm;
    private String endYm;
    private int months;
    private BigDecimal progressPct;

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

    public String getStartYm() {
        return startYm;
    }

    public void setStartYm(String startYm) {
        this.startYm = startYm;
    }

    public String getEndYm() {
        return endYm;
    }

    public void setEndYm(String endYm) {
        this.endYm = endYm;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public BigDecimal getProgressPct() {
        return progressPct;
    }

    public void setProgressPct(BigDecimal progressPct) {
        this.progressPct = progressPct;
    }

    @Override
    public String toString() {
        return "StagnantRow{" + projectCode + ", " + startYm + "~" + endYm + " 共 " + months
                + " 个月停在 " + progressPct + "%}";
    }
}
