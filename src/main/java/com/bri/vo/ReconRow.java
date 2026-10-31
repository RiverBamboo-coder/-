package com.bri.vo;

import java.math.BigDecimal;

/**
 * 国别履约对账行（C 负责，对应 M7 的 Q7-4）。
 *
 * <p>按东道国汇总承诺数量与履约情况，形成对账表。</p>
 *
 * @author C
 */
public class ReconRow {

    /** 国家 ISO 三字母代码 */
    private String countryCode;

    /** 国家名称 */
    private String countryName;

    /** 承诺总数 */
    private long commitCnt;

    /** 已履约数 */
    private long fulfilledCnt;

    /** 未履约数 */
    private long breachedCnt;

    /** 待评估数 */
    private long pendingCnt;

    /** 履约率（百分比数值） */
    private BigDecimal ratePct;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public long getCommitCnt() {
        return commitCnt;
    }

    public void setCommitCnt(long commitCnt) {
        this.commitCnt = commitCnt;
    }

    public long getFulfilledCnt() {
        return fulfilledCnt;
    }

    public void setFulfilledCnt(long fulfilledCnt) {
        this.fulfilledCnt = fulfilledCnt;
    }

    public long getBreachedCnt() {
        return breachedCnt;
    }

    public void setBreachedCnt(long breachedCnt) {
        this.breachedCnt = breachedCnt;
    }

    public long getPendingCnt() {
        return pendingCnt;
    }

    public void setPendingCnt(long pendingCnt) {
        this.pendingCnt = pendingCnt;
    }

    public BigDecimal getRatePct() {
        return ratePct;
    }

    public void setRatePct(BigDecimal ratePct) {
        this.ratePct = ratePct;
    }
}
