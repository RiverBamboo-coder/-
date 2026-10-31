package com.bri.vo;

import java.math.BigDecimal;

/**
 * 联合体份额（Q3-4）
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class ShareRow {

    /** 联合体 ID */
    private long consortiumId;

    /** 联合体名称 */
    private String consortiumName;

    /** 牵头单位 */
    private String leaderName;

    /** 成员数 */
    private int memberCount;

    /** 成员份额合计（应等于 100） */
    private BigDecimal shareSum;

    /** 参与项目数 */
    private long projectCount;


    public long getConsortiumId() {
        return consortiumId;
    }

    public void setConsortiumId(long consortiumId) {
        this.consortiumId = consortiumId;
    }

    public String getConsortiumName() {
        return consortiumName;
    }

    public void setConsortiumName(String consortiumName) {
        this.consortiumName = consortiumName;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public BigDecimal getShareSum() {
        return shareSum;
    }

    public void setShareSum(BigDecimal shareSum) {
        this.shareSum = shareSum;
    }

    public long getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(long projectCount) {
        this.projectCount = projectCount;
    }
}
