package com.bri.ui;

/**
 * Simple table payload passed from the DAO layer to the UI.
 */
public final class TableData {
    private final String[] columns;
    private final Object[][] rows;
    private final String note;

    public TableData(String[] columns, Object[][] rows) {
        this(columns, rows, null);
    }

    public TableData(String[] columns, Object[][] rows, String note) {
        this.columns = columns == null ? new String[0] : columns;
        this.rows = rows == null ? new Object[0][] : rows;
        this.note = note;
    }

    public String[] getColumns() {
        return columns;
    }

    public Object[][] getRows() {
        return rows;
    }

    public String getNote() {
        return note;
    }

    public static TableData empty(String... columns) {
        return new TableData(columns, new Object[0][]);
    }
}
