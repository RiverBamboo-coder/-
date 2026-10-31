package com.bri.vo;

import java.time.LocalDateTime;

/**
 * 项目状态变更轨迹（Q2-5），数据源 {@code t_project_status_log}。
 *
 * <p>项目主表只存“当前状态”，历史变更全部落在这张日志表里，
 * 这样既满足 3NF（不把历史塞进主表），又能回答“这个项目是什么时候从在建变成竣工的”。</p>
 *
 * @author A
 */
public class StatusLogRow {

    /** 日志主键 */
    private long logId;

    /** 项目主键 */
    private long projectId;

    /** 项目编码（联查 t_project 得到，便于界面直接显示） */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 变更前状态，首次记录时为 null */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 变更时间（库中为 DATETIME，保留时分，便于时间线排序） */
    private LocalDateTime changeDate;

    /** 操作人 */
    private String operator;

    /** 备注（变更原因） */
    private String remark;


    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
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

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(LocalDateTime changeDate) {
        this.changeDate = changeDate;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
