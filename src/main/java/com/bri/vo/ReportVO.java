package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 履约进度月报行（M6 · t_progress_report，联 t_project）。 @author B */
public class ReportVO {

    private long reportId;
    private long projectId;
    private String projectCode;
    private String projectName;
    private String periodYm;
    private BigDecimal progressPct;
    private int onSiteStaff;
    private int localStaff;
    private BigDecimal localHirePct;
    private BigDecimal outputValue;
    private LocalDate reportDate;
    private String reporter;
    private String remark;

    public long getReportId() {
        return reportId;
    }

    public void setReportId(long reportId) {
        this.reportId = reportId;
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

    public String getPeriodYm() {
        return periodYm;
    }

    public void setPeriodYm(String periodYm) {
        this.periodYm = periodYm;
    }

    public BigDecimal getProgressPct() {
        return progressPct;
    }

    public void setProgressPct(BigDecimal progressPct) {
        this.progressPct = progressPct;
    }

    public int getOnSiteStaff() {
        return onSiteStaff;
    }

    public void setOnSiteStaff(int onSiteStaff) {
        this.onSiteStaff = onSiteStaff;
    }

    public int getLocalStaff() {
        return localStaff;
    }

    public void setLocalStaff(int localStaff) {
        this.localStaff = localStaff;
    }

    public BigDecimal getLocalHirePct() {
        return localHirePct;
    }

    public void setLocalHirePct(BigDecimal localHirePct) {
        this.localHirePct = localHirePct;
    }

    public BigDecimal getOutputValue() {
        return outputValue;
    }

    public void setOutputValue(BigDecimal outputValue) {
        this.outputValue = outputValue;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "ReportVO{" + projectCode + " " + periodYm + ", progress=" + progressPct + "%}";
    }
}
