package com.bri.vo;

import java.math.BigDecimal;

/** 属地用工偏低行（Q6-4，取每个项目最新一期月报）。 @author B */
public class HireRow {

    private long projectId;
    private String projectCode;
    private String projectName;
    private String periodYm;
    private int localStaff;
    private int onSiteStaff;
    private BigDecimal localHirePct;

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

    public String getPeriodYm() {
        return periodYm;
    }

    public void setPeriodYm(String periodYm) {
        this.periodYm = periodYm;
    }

    public int getLocalStaff() {
        return localStaff;
    }

    public void setLocalStaff(int localStaff) {
        this.localStaff = localStaff;
    }

    public int getOnSiteStaff() {
        return onSiteStaff;
    }

    public void setOnSiteStaff(int onSiteStaff) {
        this.onSiteStaff = onSiteStaff;
    }

    public BigDecimal getLocalHirePct() {
        return localHirePct;
    }

    public void setLocalHirePct(BigDecimal localHirePct) {
        this.localHirePct = localHirePct;
    }

    @Override
    public String toString() {
        return "HireRow{" + projectCode + " " + periodYm + ", 属地 " + localStaff + "/" + onSiteStaff
                + " = " + localHirePct + "%}";
    }
}
