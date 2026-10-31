package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 履约承诺 + 比对结果行对象（C 负责，对应 M7）。
 *
 * <p>对应 {@code t_commitment} 与 {@code t_commitment_result} 的左连接结果，
 * 供 D 的“履约比对”面板按项目展示。{@link #actualValue} 为 {@code null} 时结论应为“待评估”。</p>
 *
 * @author C
 */
public class CommitVO {

    /** 承诺主键 commit_id */
    private long commitId;

    /** 项目主键 project_id */
    private long projectId;

    /** 承诺类型：工期/金额/属地用工/培训 */
    private String commitType;

    /** 目标值（工期为天数、金额为元、用工为百分比、培训为人次） */
    private BigDecimal targetValue;

    /** 比较方向：<= 或 >= */
    private String compareOp;

    /** 基准日（工期承诺从签约日/开工日算） */
    private LocalDate baseDate;

    /** 承诺条款原文 */
    private String clauseText;

    /** 承诺签署日期 */
    private LocalDate signDate;

    /** 备注 */
    private String remark;

    /** 实际值，无法计算时为 null */
    private BigDecimal actualValue;

    /** 差距 = 实际值 - 目标值 */
    private BigDecimal gapValue;

    /** 结论：已履约/部分履约/未履约/待评估 */
    private String commitStatus;

    public long getCommitId() {
        return commitId;
    }

    public void setCommitId(long commitId) {
        this.commitId = commitId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getCommitType() {
        return commitType;
    }

    public void setCommitType(String commitType) {
        this.commitType = commitType;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public String getCompareOp() {
        return compareOp;
    }

    public void setCompareOp(String compareOp) {
        this.compareOp = compareOp;
    }

    public LocalDate getBaseDate() {
        return baseDate;
    }

    public void setBaseDate(LocalDate baseDate) {
        this.baseDate = baseDate;
    }

    public String getClauseText() {
        return clauseText;
    }

    public void setClauseText(String clauseText) {
        this.clauseText = clauseText;
    }

    public LocalDate getSignDate() {
        return signDate;
    }

    public void setSignDate(LocalDate signDate) {
        this.signDate = signDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public BigDecimal getActualValue() {
        return actualValue;
    }

    public void setActualValue(BigDecimal actualValue) {
        this.actualValue = actualValue;
    }

    public BigDecimal getGapValue() {
        return gapValue;
    }

    public void setGapValue(BigDecimal gapValue) {
        this.gapValue = gapValue;
    }

    public String getCommitStatus() {
        return commitStatus;
    }

    public void setCommitStatus(String commitStatus) {
        this.commitStatus = commitStatus;
    }
}
