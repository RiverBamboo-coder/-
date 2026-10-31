package com.bri.vo;



/**
 * 数据字典 X4：字段清单
 *
 * <p>由 DAO 查询后填充，界面（D）直接绑定表格 / 表单。</p>
 *
 * @author A
 */
public class MetaColumnVO {

    /** 表名 */
    private String tableName;

    /** 字段名 */
    private String columnName;

    /** 字段类型 */
    private String columnType;

    /** 是否允许为空 */
    private boolean nullable;

    /** 键类型：PRI/UNI/MUL */
    private String columnKey;

    /** 额外属性：auto_increment / STORED GENERATED */
    private String extra;

    /** 字段注释 */
    private String columnComment;

    /** 字段序号 */
    private int ordinal;


    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getColumnKey() {
        return columnKey;
    }

    public void setColumnKey(String columnKey) {
        this.columnKey = columnKey;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getColumnComment() {
        return columnComment;
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }
}
