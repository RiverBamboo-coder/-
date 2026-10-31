package com.bri.vo;

import java.math.BigDecimal;

/**
 * 合同-投资一致性差异（Q2-2）
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class GapRow {

    /** 项目 ID */
    private long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 总投资 */
    private BigDecimal investAmount;

    /** 合同金额合计 */
    private BigDecimal contractSum;

    /** 差额 = 投资 - 合同合计 */
    private BigDecimal gap;

    /** 差额占投资比例 % */
    private BigDecimal gapPct;


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

    public BigDecimal getInvestAmount() {
        return investAmount;
    }

    public void setInvestAmount(BigDecimal investAmount) {
        this.investAmount = investAmount;
    }

    public BigDecimal getContractSum() {
        return contractSum;
    }

    public void setContractSum(BigDecimal contractSum) {
        this.contractSum = contractSum;
    }

    public BigDecimal getGap() {
        return gap;
    }

    public void setGap(BigDecimal gap) {
        this.gap = gap;
    }

    public BigDecimal getGapPct() {
        return gapPct;
    }

    public void setGapPct(BigDecimal gapPct) {
        this.gapPct = gapPct;
    }
}
