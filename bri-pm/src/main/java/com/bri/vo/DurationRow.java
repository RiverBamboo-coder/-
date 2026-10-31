package com.bri.vo;

import java.time.LocalDate;

/**
 * 项目计划工期与实际工期对比行（C 负责，对应 M5 的 Q5-3）。
 *
 * @author C
 */
public class DurationRow {

    /** 项目主键 */
    private long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 计划开工日期 */
    private LocalDate plannedStartDate;

    /** 计划竣工日期 */
    private LocalDate plannedEndDate;

    /** 实际开工日期 */
    private LocalDate actualStartDate;

    /** 实际竣工日期 */
    private LocalDate actualEndDate;

    /** 计划工期（天） */
    private long plannedDays;

    /** 实际工期（天） */
    private long actualDays;

    /** 工期差（天）= 实际工期 - 计划工期，正数表示延期 */
    private long diffDays;

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

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(LocalDate plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public void setPlannedEndDate(LocalDate plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    public LocalDate getActualStartDate() {
        return actualStartDate;
    }

    public void setActualStartDate(LocalDate actualStartDate) {
        this.actualStartDate = actualStartDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public long getPlannedDays() {
        return plannedDays;
    }

    public void setPlannedDays(long plannedDays) {
        this.plannedDays = plannedDays;
    }

    public long getActualDays() {
        return actualDays;
    }

    public void setActualDays(long actualDays) {
        this.actualDays = actualDays;
    }

    public long getDiffDays() {
        return diffDays;
    }

    public void setDiffDays(long diffDays) {
        this.diffDays = diffDays;
    }
}
