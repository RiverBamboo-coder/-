package com.bri.vo;



/**
 * 数据字典 X4：表清单
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class MetaTableVO {

    /** 表名 */
    private String tableName;

    /** 表注释 */
    private String tableComment;

    /** 估算行数（information_schema.tables.table_rows） */
    private long rowCount;


    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long rowCount) {
        this.rowCount = rowCount;
    }
}
