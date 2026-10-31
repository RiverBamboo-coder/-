package com.bri.vo;

/**
 * 全球化布局企业：在多少个“洲”有参与项目（Q3-5）。
 *
 * <p>“洲”指 {@code t_region.level_no = 1} 的一级区域；国家的 region 可能挂在二级区域下，
 * 所以 SQL 用 {@code COALESCE(二级.parent_id, 二级.region_id)} 上卷到洲，
 * 再用 {@code COUNT(DISTINCT 洲)} 判“跨几个洲”，避免把同一洲的多个国家重复计数。</p>
 *
 * @author A
 */
public class ContinentSpreadRow {

    /** 企业主键 */
    private long enterpriseId;

    /** 企业名称 */
    private String enterpriseName;

    /** 企业性质（央企/地方国企/民营…） */
    private String natureName;

    /** 覆盖的洲数（去重） */
    private long continentCount;

    /** 覆盖的国家数（去重） */
    private long countryCount;

    /** 参与的项目数（去重） */
    private long projectCount;

    /** 洲名列表，如“亚洲、非洲、欧洲”，便于界面直接显示 */
    private String continentNames;


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

    public long getContinentCount() {
        return continentCount;
    }

    public void setContinentCount(long continentCount) {
        this.continentCount = continentCount;
    }

    public long getCountryCount() {
        return countryCount;
    }

    public void setCountryCount(long countryCount) {
        this.countryCount = countryCount;
    }

    public long getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(long projectCount) {
        this.projectCount = projectCount;
    }

    public String getContinentNames() {
        return continentNames;
    }

    public void setContinentNames(String continentNames) {
        this.continentNames = continentNames;
    }
}
