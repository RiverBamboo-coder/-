package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目组合筛选条件（四维 + 关键字 + 日期 + 金额区间）
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class ProjectQuery {

    /** 项目名/编码关键字 */
    private String keyword;

    /** 东道国 */
    private String countryCode;

    /** 领域 */
    private String fieldCode;

    /** 合作模式 */
    private String modeCode;

    /** 状态 */
    private String status;

    /** 大洲编码（AS/AF/EU/LA/OC/NA） */
    private String continentCode;

    /** 投资下限（元） */
    private BigDecimal investMin;

    /** 投资上限（元） */
    private BigDecimal investMax;

    /** 签约日期起 */
    private LocalDate signFrom;

    /** 签约日期止 */
    private LocalDate signTo;


    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getModeCode() {
        return modeCode;
    }

    public void setModeCode(String modeCode) {
        this.modeCode = modeCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContinentCode() {
        return continentCode;
    }

    public void setContinentCode(String continentCode) {
        this.continentCode = continentCode;
    }

    public BigDecimal getInvestMin() {
        return investMin;
    }

    public void setInvestMin(BigDecimal investMin) {
        this.investMin = investMin;
    }

    public BigDecimal getInvestMax() {
        return investMax;
    }

    public void setInvestMax(BigDecimal investMax) {
        this.investMax = investMax;
    }

    public LocalDate getSignFrom() {
        return signFrom;
    }

    public void setSignFrom(LocalDate signFrom) {
        this.signFrom = signFrom;
    }

    public LocalDate getSignTo() {
        return signTo;
    }

    public void setSignTo(LocalDate signTo) {
        this.signTo = signTo;
    }
}
