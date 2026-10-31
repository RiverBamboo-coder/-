package com.bri.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 企业参与项目（M3）：一行 = 某企业在某项目中的某个角色
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class ProjectRoleVO {

    /** 项目 ID */
    private long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 东道国 */
    private String countryName;

    /** 项目状态 */
    private String status;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 工作份额 */
    private BigDecimal sharePct;

    /** 参与日期 */
    private LocalDate joinDate;

    /** 联合体 ID */
    private Long consortiumId;

    /** 联合体名称 */
    private String consortiumName;


    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public BigDecimal getSharePct() {
        return sharePct;
    }

    public void setSharePct(BigDecimal sharePct) {
        this.sharePct = sharePct;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public Long getConsortiumId() {
        return consortiumId;
    }

    public void setConsortiumId(Long consortiumId) {
        this.consortiumId = consortiumId;
    }

    public String getConsortiumName() {
        return consortiumName;
    }

    public void setConsortiumName(String consortiumName) {
        this.consortiumName = consortiumName;
    }
}
