package com.bri.vo;

import java.math.BigDecimal;

/** 资金来源占比行（Q4-3，窗口函数算占比与排名）。 @author B */
public class SourceRow {

    private long sourceId;
    private String sourceName;
    private String sourceType;
    private BigDecimal actualTotal;
    private BigDecimal sharePct;
    private int rankNo;

    public long getSourceId() {
        return sourceId;
    }

    public void setSourceId(long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public BigDecimal getActualTotal() {
        return actualTotal;
    }

    public void setActualTotal(BigDecimal actualTotal) {
        this.actualTotal = actualTotal;
    }

    public BigDecimal getSharePct() {
        return sharePct;
    }

    public void setSharePct(BigDecimal sharePct) {
        this.sharePct = sharePct;
    }

    public int getRankNo() {
        return rankNo;
    }

    public void setRankNo(int rankNo) {
        this.rankNo = rankNo;
    }

    @Override
    public String toString() {
        return "SourceRow{" + sourceName + ", actual=" + actualTotal + ", share=" + sharePct + "%}";
    }
}
