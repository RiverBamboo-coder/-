package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 里程碑超期/延迟统计行（C 负责，对应 M5 的 Q5-1 与 Q5-2）。
 *
 * <p>两类查询共用本行对象（接口契约里都返回 {@code List<DelayRow>}），字段按查询来源区分：</p>
 * <ul>
 *   <li><b>Q5-1 {@code statAvgDelayByMilestone}</b>（平均超期天数）：填
 *       {@code milestoneId / milestoneName / avgDelayDays / totalCnt / overdueCnt}；</li>
 *   <li><b>Q5-2 {@code findOverdueMilestones}</b>（已超期未完成）：填
 *       {@code pmId / projectCode / projectName / milestoneName / planDate / overdueDays}。</li>
 * </ul>
 *
 * @author C
 */
public class DelayRow {

    /** Q5-2：项目里程碑主键 pm_id */
    private long pmId;

    /** Q5-1：里程碑字典主键 milestone_id */
    private int milestoneId;

    /** 里程碑名称（两类查询都填） */
    private String milestoneName;

    /** Q5-2：项目编码 */
    private String projectCode;

    /** Q5-2：项目名称 */
    private String projectName;

    /** Q5-2：计划完成日期 */
    private LocalDate planDate;

    /** Q5-1：平均超期天数（天） */
    private BigDecimal avgDelayDays;

    /** Q5-2：已超期天数（天） */
    private long overdueDays;

    /** Q5-1：已完成样本数 */
    private int totalCnt;

    /** Q5-1：其中超期数 */
    private int overdueCnt;

    public long getPmId() {
        return pmId;
    }

    public void setPmId(long pmId) {
        this.pmId = pmId;
    }

    public int getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(int milestoneId) {
        this.milestoneId = milestoneId;
    }

    public String getMilestoneName() {
        return milestoneName;
    }

    public void setMilestoneName(String milestoneName) {
        this.milestoneName = milestoneName;
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

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public BigDecimal getAvgDelayDays() {
        return avgDelayDays;
    }

    public void setAvgDelayDays(BigDecimal avgDelayDays) {
        this.avgDelayDays = avgDelayDays;
    }

    public long getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(long overdueDays) {
        this.overdueDays = overdueDays;
    }

    public int getTotalCnt() {
        return totalCnt;
    }

    public void setTotalCnt(int totalCnt) {
        this.totalCnt = totalCnt;
    }

    public int getOverdueCnt() {
        return overdueCnt;
    }

    public void setOverdueCnt(int overdueCnt) {
        this.overdueCnt = overdueCnt;
    }
}
