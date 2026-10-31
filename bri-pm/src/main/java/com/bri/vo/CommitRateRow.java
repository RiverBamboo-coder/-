package com.bri.vo;

import java.math.BigDecimal;

/**
 * 承诺类型履约率统计行（C 负责，对应 Q7-2 各承诺类型履约率）。
 *
 * <p>注意与 B 的 {@link RateRow}（项目级资金到位率）区分：本类是“承诺”维度的比率，
 * 维度列是承诺类型（工期/金额/属地用工/培训），而非项目。</p>
 *
 * <p>{@code ratePct} 为 {@code null} 表示该类型承诺数为 0（NULLIF 除零保护），
 * 与 0% 不是一回事，界面应显示“—”。</p>
 *
 * @author C
 */
public class CommitRateRow {

    /** 承诺类型：工期/金额/属地用工/培训 */
    private String commitType;

    /** 承诺总数 */
    private long totalCnt;

    /** 已履约数 */
    private long fulfilledCnt;

    /** 履约率（百分比数值，如 98.5 表示 98.5%） */
    private BigDecimal ratePct;

    public String getCommitType() {
        return commitType;
    }

    public void setCommitType(String commitType) {
        this.commitType = commitType;
    }

    public long getTotalCnt() {
        return totalCnt;
    }

    public void setTotalCnt(long totalCnt) {
        this.totalCnt = totalCnt;
    }

    public long getFulfilledCnt() {
        return fulfilledCnt;
    }

    public void setFulfilledCnt(long fulfilledCnt) {
        this.fulfilledCnt = fulfilledCnt;
    }

    public BigDecimal getRatePct() {
        return ratePct;
    }

    public void setRatePct(BigDecimal ratePct) {
        this.ratePct = ratePct;
    }
}
