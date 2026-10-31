package com.bri.vo;

import java.time.LocalDateTime;

/**
 * 预警实例行（M8 · t_warning，联 t_warning_rule / t_project）。
 *
 * <p>{@code warnLevelName} 与 {@code ruleName} 是刷新时的快照：规则改名、改级别之后，
 * 历史预警仍能解释“当初是哪条规则、按什么阈值触发的”。</p>
 *
 * @author B
 */
public class WarningVO {

    private long warnId;
    private long ruleId;
    private String ruleCode;
    private String ruleName;
    private String levelCode;
    private long projectId;
    private String projectCode;
    private String projectName;
    private String countryName;
    private String periodYm;
    private String triggerSnapshot;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime ackAt;
    private String ackBy;

    public long getWarnId() {
        return warnId;
    }

    public void setWarnId(long warnId) {
        this.warnId = warnId;
    }

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getLevelCode() {
        return levelCode;
    }

    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
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

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getPeriodYm() {
        return periodYm;
    }

    public void setPeriodYm(String periodYm) {
        this.periodYm = periodYm;
    }

    public String getTriggerSnapshot() {
        return triggerSnapshot;
    }

    public void setTriggerSnapshot(String triggerSnapshot) {
        this.triggerSnapshot = triggerSnapshot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAckAt() {
        return ackAt;
    }

    public void setAckAt(LocalDateTime ackAt) {
        this.ackAt = ackAt;
    }

    public String getAckBy() {
        return ackBy;
    }

    public void setAckBy(String ackBy) {
        this.ackBy = ackBy;
    }

    /** 级别中文名：红/黄/蓝。 */
    public String getLevelName() {
        return WarningRuleVO.levelNameOf(levelCode);
    }

    /** 状态中文名：未处理/已确认/已解除。 */
    public String getStatusName() {
        if ("OPEN".equals(status)) {
            return "未处理";
        }
        if ("ACK".equals(status)) {
            return "已确认";
        }
        if ("CLOSED".equals(status)) {
            return "已解除";
        }
        return String.valueOf(status);
    }

    @Override
    public String toString() {
        return "WarningVO{" + getLevelName() + " " + ruleName + " / " + projectCode + " "
                + periodYm + " / " + triggerSnapshot + " / " + getStatusName() + "}";
    }
}
