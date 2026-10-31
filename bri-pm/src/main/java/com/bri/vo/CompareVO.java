package com.bri.vo;

import java.math.BigDecimal;

/**
 * 承诺比对结果行对象（C 负责，对应 M7 的按结论筛选列表）。
 *
 * <p>对应 {@code t_commitment_result} + {@code t_commitment} + {@code t_project} 的联合查询，
 * 供 D 的“履约比对”面板按 未履约/部分履约/已履约 筛选展示。</p>
 *
 * @author C
 */
public class CompareVO {

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 承诺类型：工期/金额/属地用工/培训 */
    private String commitType;

    /** 目标值 */
    private BigDecimal targetValue;

    /** 比较方向：<= 或 >= */
    private String compareOp;

    /** 实际值 */
    private BigDecimal actualValue;

    /** 差距 = 实际值 - 目标值 */
    private BigDecimal gapValue;

    /** 结论：已履约/部分履约/未履约/待评估 */
    private String commitStatus;

    /** 承诺条款原文 */
    private String clauseText;

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

    public String getClauseText() {
        return clauseText;
    }

    public void setClauseText(String clauseText) {
        this.clauseText = clauseText;
    }
}
