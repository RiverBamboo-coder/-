package com.bri.vo;

import java.math.BigDecimal;

/**
 * 绿色丝绸之路统计行（C 负责，对应思政查询 3“绿色丝绸之路”）。
 *
 * <p>按领域汇总绿色装机容量与输出标准数量。</p>
 *
 * @author C
 */
public class GreenRow {

    /** 领域编码 */
    private String fieldCode;

    /** 领域名称（字典项，便于界面显示） */
    private String fieldName;

    /** 绿色装机容量合计（MW） */
    private BigDecimal greenMw;

    /** 输出标准数量合计 */
    private long standards;

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public BigDecimal getGreenMw() {
        return greenMw;
    }

    public void setGreenMw(BigDecimal greenMw) {
        this.greenMw = greenMw;
    }

    public long getStandards() {
        return standards;
    }

    public void setStandards(long standards) {
        this.standards = standards;
    }
}
