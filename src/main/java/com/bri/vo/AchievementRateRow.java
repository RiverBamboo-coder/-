package com.bri.vo;

import java.math.BigDecimal;

/**
 * 各领域里程碑达成率统计行（C 负责，对应 Q5-4 各领域里程碑达成率）。
 *
 * <p>注意与 B 的 {@link RateRow}（项目级资金到位率）区分：本类的维度是“领域”，
 * 指标是“里程碑达成率”（已完成里程碑数 / 计划里程碑总数）。</p>
 *
 * <p>{@code ratePct} 为 {@code null} 表示该领域计划里程碑数为 0（NULLIF 除零保护），
 * 与 0% 不是一回事，界面应显示“—”。</p>
 *
 * @author C
 */
public class AchievementRateRow {

    /** 领域编码（字典 FIELD） */
    private String fieldCode;

    /** 领域名称（字典项，便于界面显示） */
    private String fieldName;

    /** 计划里程碑总数 */
    private long totalCnt;

    /** 已达成里程碑数（actual_date 非空） */
    private long achievedCnt;

    /** 达成率（百分比数值，如 96.7 表示 96.7%） */
    private BigDecimal ratePct;

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public long getTotalCnt() {
        return totalCnt;
    }

    public void setTotalCnt(long totalCnt) {
        this.totalCnt = totalCnt;
    }

    public long getAchievedCnt() {
        return achievedCnt;
    }

    public void setAchievedCnt(long achievedCnt) {
        this.achievedCnt = achievedCnt;
    }

    public BigDecimal getRatePct() {
        return ratePct;
    }

    public void setRatePct(BigDecimal ratePct) {
        this.ratePct = ratePct;
    }
}
