package com.bri.vo;

import java.math.BigDecimal;

/**
 * 企业排行（Q3-1）
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class RankRow {

    /** 名次 */
    private int rankNo;

    /** 企业 ID */
    private long enterpriseId;

    /** 企业名称 */
    private String enterpriseName;

    /** 企业性质 */
    private String natureName;

    /** 参与项目数 */
    private long projectCount;

    /** 作为总承包承担的投资额 */
    private BigDecimal zcAmount;


    public int getRankNo() {
        return rankNo;
    }

    public void setRankNo(int rankNo) {
        this.rankNo = rankNo;
    }

    public long getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(long enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getEnterpriseName() {
        return enterpriseName;
    }

    public void setEnterpriseName(String enterpriseName) {
        this.enterpriseName = enterpriseName;
    }

    public String getNatureName() {
        return natureName;
    }

    public void setNatureName(String natureName) {
        this.natureName = natureName;
    }

    public long getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(long projectCount) {
        this.projectCount = projectCount;
    }

    public BigDecimal getZcAmount() {
        return zcAmount;
    }

    public void setZcAmount(BigDecimal zcAmount) {
        this.zcAmount = zcAmount;
    }
}
