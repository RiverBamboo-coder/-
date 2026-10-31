package com.bri.vo;

import java.math.BigDecimal;

/**
 * 某领域承接项目最多的企业（Q3-2）。
 *
 * <p>每个领域只返回一行（该领域的冠军企业）。SQL 用窗口函数
 * {@code ROW_NUMBER() OVER (PARTITION BY field_code ORDER BY 项目数 DESC)} 取 rn = 1，
 * 比“先查最大值再回查”少一次往返，也不会在并列时返回多行。</p>
 *
 * @author A
 */
public class FieldTopEnterpriseRow {

    /** 领域编码（字典 FIELD） */
    private String fieldCode;

    /** 领域名称 */
    private String fieldName;

    /** 企业主键 */
    private long enterpriseId;

    /** 企业名称 */
    private String enterpriseName;

    /** 该企业在这个领域承接的项目数 */
    private long projectCount;

    /** 这些项目的投资额合计 */
    private BigDecimal totalInvest;


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

    public long getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(long projectCount) {
        this.projectCount = projectCount;
    }

    public BigDecimal getTotalInvest() {
        return totalInvest;
    }

    public void setTotalInvest(BigDecimal totalInvest) {
        this.totalInvest = totalInvest;
    }
}
