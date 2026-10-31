package com.bri.vo;



/**
 * 企业（M3）：列表行，含性质、国别与参与项目数
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class EnterpriseVO {

    /** 企业 ID */
    private long enterpriseId;

    /** 企业名称 */
    private String enterpriseName;

    /** 英文名称 */
    private String enterpriseNameEn;

    /** 企业性质编码 */
    private String natureCode;

    /** 企业性质名称 */
    private String natureName;

    /** 注册地代码 */
    private String countryCode;

    /** 注册地国别 */
    private String countryName;

    /** 上级企业 ID（递归） */
    private Long parentEnterpriseId;

    /** 上级企业名称 */
    private String parentName;

    /** 参与项目数 */
    private long projectCount;

    /** 备注 */
    private String remark;


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

    public String getEnterpriseNameEn() {
        return enterpriseNameEn;
    }

    public void setEnterpriseNameEn(String enterpriseNameEn) {
        this.enterpriseNameEn = enterpriseNameEn;
    }

    public String getNatureCode() {
        return natureCode;
    }

    public void setNatureCode(String natureCode) {
        this.natureCode = natureCode;
    }

    public String getNatureName() {
        return natureName;
    }

    public void setNatureName(String natureName) {
        this.natureName = natureName;
    }

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

    public Long getParentEnterpriseId() {
        return parentEnterpriseId;
    }

    public void setParentEnterpriseId(Long parentEnterpriseId) {
        this.parentEnterpriseId = parentEnterpriseId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public long getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(long projectCount) {
        this.projectCount = projectCount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
