package com.bri.vo;

import java.math.BigDecimal;

/** 报告指标行（M6 EAV · t_report_indicator）。 @author B */
public class IndicatorRow {

    private long reportId;
    private String periodYm;
    private String indicatorCode;
    private String indicatorName;
    private BigDecimal indicatorVal;
    private String unit;

    public long getReportId() {
        return reportId;
    }

    public void setReportId(long reportId) {
        this.reportId = reportId;
    }

    public String getPeriodYm() {
        return periodYm;
    }

    public void setPeriodYm(String periodYm) {
        this.periodYm = periodYm;
    }

    public String getIndicatorCode() {
        return indicatorCode;
    }

    public void setIndicatorCode(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }

    public String getIndicatorName() {
        return indicatorName;
    }

    public void setIndicatorName(String indicatorName) {
        this.indicatorName = indicatorName;
    }

    public BigDecimal getIndicatorVal() {
        return indicatorVal;
    }

    public void setIndicatorVal(BigDecimal indicatorVal) {
        this.indicatorVal = indicatorVal;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    /** 指标中文名：码表放在 VO 里，避免为 2 个指标建字典表。 */
    public static String nameOf(String code) {
        if (code == null) {
            return "";
        }
        switch (code) {
            case "TRAINING":
                return "本期培训人次";
            case "LOCAL_SUPPLY":
                return "本地采购额";
            default:
                return code;
        }
    }

    @Override
    public String toString() {
        return "IndicatorRow{" + indicatorCode + "=" + indicatorVal + unit + "}";
    }
}
