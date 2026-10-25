package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警规则行（M8 · t_warning_rule）。
 *
 * <p>规则是数据，不是代码：两个条件按 AND 组合，{@code cond2Metric} 为 null 表示单条件规则。
 * 指标取值见 {@link #conditionDesc()}，比较符含义见 {@link #opOf(String)}。</p>
 *
 * @author B
 */
public class WarningRuleVO {

    private long ruleId;
    private String ruleCode;
    private String ruleName;
    private String levelCode;
    private String cond1Metric;
    private String cond1Op;
    private BigDecimal cond1Value;
    private String cond2Metric;
    private String cond2Op;
    private BigDecimal cond2Value;
    private boolean enabled;
    private String messageTpl;
    private LocalDateTime updatedAt;

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getLevelCode() {
        return levelCode;
    }

    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
    }

    public String getCond1Metric() {
        return cond1Metric;
    }

    public void setCond1Metric(String cond1Metric) {
        this.cond1Metric = cond1Metric;
    }

    public String getCond1Op() {
        return cond1Op;
    }

    public void setCond1Op(String cond1Op) {
        this.cond1Op = cond1Op;
    }

    public BigDecimal getCond1Value() {
        return cond1Value;
    }

    public void setCond1Value(BigDecimal cond1Value) {
        this.cond1Value = cond1Value;
    }

    public String getCond2Metric() {
        return cond2Metric;
    }

    public void setCond2Metric(String cond2Metric) {
        this.cond2Metric = cond2Metric;
    }

    public String getCond2Op() {
        return cond2Op;
    }

    public void setCond2Op(String cond2Op) {
        this.cond2Op = cond2Op;
    }

    public BigDecimal getCond2Value() {
        return cond2Value;
    }

    public void setCond2Value(BigDecimal cond2Value) {
        this.cond2Value = cond2Value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMessageTpl() {
        return messageTpl;
    }

    public void setMessageTpl(String messageTpl) {
        this.messageTpl = messageTpl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** 级别中文名：R 红 / Y 黄 / B 蓝。 */
    public String getLevelName() {
        return levelNameOf(levelCode);
    }

    public static String levelNameOf(String levelCode) {
        if ("R".equals(levelCode)) {
            return "红灯";
        }
        if ("Y".equals(levelCode)) {
            return "黄灯";
        }
        if ("B".equals(levelCode)) {
            return "蓝灯";
        }
        return String.valueOf(levelCode);
    }

    /** 比较符 → 中文（界面显示用）。 */
    public static String opOf(String op) {
        if (op == null) {
            return "";
        }
        switch (op) {
            case "LT":
                return "<";
            case "LE":
                return "<=";
            case "GT":
                return ">";
            case "GE":
                return ">=";
            case "EQ":
                return "=";
            default:
                return op;
        }
    }

    /** 指标中文名：Java 侧只需要认识它，不需要认识表。 */
    public static String metricNameOf(String metric) {
        if (metric == null) {
            return "";
        }
        switch (metric) {
            case "FUND_RATE":
                return "资金到位率";
            case "PROGRESS_PCT":
                return "形象进度";
            case "OVERDUE_COUNT":
                return "超期里程碑数";
            case "LOCAL_HIRE_RATE":
                return "属地用工占比";
            case "REPORT_GAP_MONTH":
                return "未报月数";
            default:
                return metric;
        }
    }

    /** 条件的中文描述，例如 “资金到位率<60 且 形象进度<50”。 */
    public String conditionDesc() {
        StringBuilder sb = new StringBuilder();
        sb.append(metricNameOf(cond1Metric)).append(opOf(cond1Op)).append(plain(cond1Value));
        if (cond2Metric != null) {
            sb.append(" 且 ").append(metricNameOf(cond2Metric)).append(opOf(cond2Op)).append(plain(cond2Value));
        }
        return sb.toString();
    }

    private static String plain(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

    @Override
    public String toString() {
        return "WarningRuleVO{" + ruleCode + " " + ruleName + " [" + getLevelName() + "] "
                + conditionDesc() + (enabled ? "" : "（停用）") + "}";
    }
}
