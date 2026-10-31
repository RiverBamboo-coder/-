package com.bri.vo;

/**
 * 同一项目中既当总承包又存在分包的项目（Q3-3）。
 *
 * <p>用于核对“总包—分包”链条是否完整：有总包却没有任何分包的项目，
 * 要么是分包信息漏录，要么确实由总包自行施工，需要人工判断。</p>
 *
 * @author A
 */
public class DualRoleProjectRow {

    /** 项目主键 */
    private long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 担任总承包（ZC）的企业数 */
    private long zcCount;

    /** 担任分包（FB）的企业数 */
    private long fbCount;

    /** 参与该项目的企业总数（含联合体成员、设计、监理） */
    private long participantCount;


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

    public long getZcCount() {
        return zcCount;
    }

    public void setZcCount(long zcCount) {
        this.zcCount = zcCount;
    }

    public long getFbCount() {
        return fbCount;
    }

    public void setFbCount(long fbCount) {
        this.fbCount = fbCount;
    }

    public long getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(long participantCount) {
        this.participantCount = participantCount;
    }
}
