package com.bri.vo;

import java.math.BigDecimal;

/** 国别累计产值行（Q6-2）。 @author B */
public class OutRow {

    private String countryCode;
    private String countryName;
    private int projectCnt;
    private BigDecimal outputTotal;
    private BigDecimal avgProgress;

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

    public int getProjectCnt() {
        return projectCnt;
    }

    public void setProjectCnt(int projectCnt) {
        this.projectCnt = projectCnt;
    }

    public BigDecimal getOutputTotal() {
        return outputTotal;
    }

    public void setOutputTotal(BigDecimal outputTotal) {
        this.outputTotal = outputTotal;
    }

    public BigDecimal getAvgProgress() {
        return avgProgress;
    }

    public void setAvgProgress(BigDecimal avgProgress) {
        this.avgProgress = avgProgress;
    }

    @Override
    public String toString() {
        return "OutRow{" + countryName + ", projects=" + projectCnt + ", output=" + outputTotal + "}";
    }
}
