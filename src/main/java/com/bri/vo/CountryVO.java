package com.bri.vo;

import java.math.BigDecimal;

/**
 * 国家（M1）：列表行，含所属区域/大洲与项目、投资聚合
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class CountryVO {

    /** ISO 三字母代码 */
    private String countryCode;

    /** 中文国名 */
    private String countryName;

    /** 英文国名 */
    private String countryNameEn;

    /** 所属二级区域 ID */
    private Integer regionId;

    /** 二级区域名称 */
    private String regionName;

    /** 所属大洲名称 */
    private String continentName;

    /** 是否共建国家 */
    private boolean beltRoad;

    /** 人口（万人） */
    private BigDecimal populationK;

    /** 项目数 */
    private long projectCount;

    /** 项目投资合计 */
    private BigDecimal investTotal;


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

    public String getCountryNameEn() {
        return countryNameEn;
    }

    public void setCountryNameEn(String countryNameEn) {
        this.countryNameEn = countryNameEn;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getContinentName() {
        return continentName;
    }

    public void setContinentName(String continentName) {
        this.continentName = continentName;
    }

    public boolean isBeltRoad() {
        return beltRoad;
    }

    public void setBeltRoad(boolean beltRoad) {
        this.beltRoad = beltRoad;
    }

    public BigDecimal getPopulationK() {
        return populationK;
    }

    public void setPopulationK(BigDecimal populationK) {
        this.populationK = populationK;
    }

    public long getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(long projectCount) {
        this.projectCount = projectCount;
    }

    public BigDecimal getInvestTotal() {
        return investTotal;
    }

    public void setInvestTotal(BigDecimal investTotal) {
        this.investTotal = investTotal;
    }
}
