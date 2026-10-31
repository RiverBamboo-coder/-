package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目成效行对象（C 负责，对应 M9）。
 *
 * <p>对应 {@code t_project_outcome}，与项目 1:1。供 D 的“成果展示”面板与三条思政查询使用。</p>
 *
 * @author C
 */
public class OutcomeVO {

    /** 项目主键（主键即外键，保证 1:1） */
    private long projectId;

    /** 受益人口（千人） */
    private BigDecimal beneficiaryK;

    /** 直接创造就业岗位数 */
    private Integer jobsCreated;

    /** 培训人次 */
    private Integer trainingPpl;

    /** 绿色装机容量（MW） */
    private BigDecimal greenMw;

    /** 输出标准数量 */
    private Integer standardsOut;

    /** 数据更新日期 */
    private LocalDate updateDate;

    /** 备注 */
    private String remark;

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public BigDecimal getBeneficiaryK() {
        return beneficiaryK;
    }

    public void setBeneficiaryK(BigDecimal beneficiaryK) {
        this.beneficiaryK = beneficiaryK;
    }

    public Integer getJobsCreated() {
        return jobsCreated;
    }

    public void setJobsCreated(Integer jobsCreated) {
        this.jobsCreated = jobsCreated;
    }

    public Integer getTrainingPpl() {
        return trainingPpl;
    }

    public void setTrainingPpl(Integer trainingPpl) {
        this.trainingPpl = trainingPpl;
    }

    public BigDecimal getGreenMw() {
        return greenMw;
    }

    public void setGreenMw(BigDecimal greenMw) {
        this.greenMw = greenMw;
    }

    public Integer getStandardsOut() {
        return standardsOut;
    }

    public void setStandardsOut(Integer standardsOut) {
        this.standardsOut = standardsOut;
    }

    public LocalDate getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDate updateDate) {
        this.updateDate = updateDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
