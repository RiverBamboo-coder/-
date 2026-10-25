package com.bri.vo;

import java.math.BigDecimal;

/** 进度环比行（Q6-1，LAG 窗口函数）。{@code momDelta} 为 null 表示是该项目第一期。 @author B */
public class MoMRow {

    private long reportId;
    private long projectId;
    private String projectCode;
    private String periodYm;
    private BigDecimal progressPct;
    private BigDecimal prevPct;
    private BigDecimal momDelta;

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

    public BigDecimal getPrevPct() {
        return prevPct;
    }

    public void setPrevPct(BigDecimal prevPct) {
        this.prevPct = prevPct;
    }

    public BigDecimal getMomDelta() {
        return momDelta;
    }

    public void setMomDelta(BigDecimal momDelta) {
        this.momDelta = momDelta;
    }

    @Override
    public String toString() {
        return "MoMRow{" + projectCode + " " + periodYm + ", " + progressPct + "% (上一期 "
                + prevPct + "%, 环比 " + momDelta + ")}";
    }
}
