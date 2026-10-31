package com.bri.vo;

import java.time.LocalDate;

/**
 * 已过计划竣工期但仍未移交的项目（Q2-4）。
 *
 * <p>逾期天数由 SQL 的 {@code DATEDIFF(CURDATE(), planned_end_date)} 直接算出，
 * 不在 Java 里重复计算，避免“界面显示的天数”与“数据库口径”不一致。</p>
 *
 * @author A
 */
public class OverdueProjectRow {

    /** 项目主键 */
    private long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 东道国 */
    private String countryName;

    /** 当前状态（前期/在建/竣工/已移交/暂停） */
    private String status;

    /** 计划竣工日期，可能为空（前期项目尚未排期） */
    private LocalDate plannedEndDate;

    /** 逾期天数 = 今天 − 计划竣工日，未逾期为 0 */
    private long overdueDays;


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

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public void setPlannedEndDate(LocalDate plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    public long getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(long overdueDays) {
        this.overdueDays = overdueDays;
    }
}
