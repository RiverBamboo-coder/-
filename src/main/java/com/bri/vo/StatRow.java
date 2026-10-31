package com.bri.vo;

import java.math.BigDecimal;

/**
 * 通用统计行（Q1-1 按洲统计 / Q1-3 覆盖率 / Q2-1 领域×状态）
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class StatRow {

    /** 维度编码或状态 */
    private String code;

    /** 维度显示名 */
    private String label;

    /** 计数（项目数 / 国家数 / 覆盖国家数） */
    private long cnt;

    /** 金额合计，可为 null */
    private BigDecimal total;

    /** 比率（如覆盖率 %），可为 null */
    private BigDecimal pct;

    /** 覆盖数（仅 Q1-3 使用：已有项目的国家数） */
    private long covered;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCnt() {
        return cnt;
    }

    public void setCnt(long cnt) {
        this.cnt = cnt;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getPct() {
        return pct;
    }

    public void setPct(BigDecimal pct) {
        this.pct = pct;
    }

    public long getCovered() {
        return covered;
    }

    public void setCovered(long covered) {
        this.covered = covered;
    }
}
