package com.bri.vo;

import java.time.LocalDate;

/**
 * 项目里程碑行对象（C 负责，对应 M5）。
 *
 * <p>对应 {@code t_project_milestone} 与 {@code t_milestone} 的联合查询结果，
 * 供 D 的“里程碑”面板列表绑定。字段名与 SQL 别名保持一致。</p>
 *
 * <p>{@link #isDelayed} 是数据库生成列读出的值：{@code true}=超期、{@code false}=准时、
 * {@code null}=未完成（{@code actual_date} 为空）。</p>
 *
 * @author C
 */
public class MilestoneVO {

    /** 项目里程碑主键 pm_id */
    private long pmId;

    /** 项目主键 project_id */
    private long projectId;

    /** 里程碑字典主键 milestone_id */
    private int milestoneId;

    /** 里程碑编码，如 START/STRUCT */
    private String milestoneCode;

    /** 里程碑名称，如 开工/主体结构 */
    private String milestoneName;

    /** 建议顺序 seq_no */
    private int seqNo;

    /** 计划完成日期 */
    private LocalDate planDate;

    /** 实际完成日期，未完成时为空 */
    private LocalDate actualDate;

    /** 是否超期：true=超期、false=准时、null=未完成 */
    private Boolean isDelayed;

    /** 备注 */
    private String remark;

    public long getPmId() {
        return pmId;
    }

    public void setPmId(long pmId) {
        this.pmId = pmId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public int getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(int milestoneId) {
        this.milestoneId = milestoneId;
    }

    public String getMilestoneCode() {
        return milestoneCode;
    }

    public void setMilestoneCode(String milestoneCode) {
        this.milestoneCode = milestoneCode;
    }

    public String getMilestoneName() {
        return milestoneName;
    }

    public void setMilestoneName(String milestoneName) {
        this.milestoneName = milestoneName;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(int seqNo) {
        this.seqNo = seqNo;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public LocalDate getActualDate() {
        return actualDate;
    }

    public void setActualDate(LocalDate actualDate) {
        this.actualDate = actualDate;
    }

    public Boolean getIsDelayed() {
        return isDelayed;
    }

    public void setIsDelayed(Boolean isDelayed) {
        this.isDelayed = isDelayed;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
