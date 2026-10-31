package com.bri.vo;

import java.math.BigDecimal;

/**
 * 大洲成效统计行（C 负责，对应思政查询 1“共商共建共享”）。
 *
 * <p>按大洲汇总共建国家的项目数、受益人口与就业岗位。</p>
 *
 * @author C
 */
public class RegionOutRow {

    /** 大洲名称 */
    private String regionName;

    /** 项目数（去重） */
    private long projCnt;

    /** 受益人口合计（千人） */
    private BigDecimal benefitK;

    /** 就业岗位合计 */
    private long jobs;

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public long getProjCnt() {
        return projCnt;
    }

    public void setProjCnt(long projCnt) {
        this.projCnt = projCnt;
    }

    public BigDecimal getBenefitK() {
        return benefitK;
    }

    public void setBenefitK(BigDecimal benefitK) {
        this.benefitK = benefitK;
    }

    public long getJobs() {
        return jobs;
    }

    public void setJobs(long jobs) {
        this.jobs = jobs;
    }
}
