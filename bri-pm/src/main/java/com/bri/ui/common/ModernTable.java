package com.bri.ui.common;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Locale;

/**
 * Styled table with search filtering, pill statuses and progress cells.
 */
public class ModernTable extends JPanel {
    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;

    public ModernTable(String[] columns, Object[][] rows) {
        super(new BorderLayout());
        setOpaque(false);
        model = new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setFont(Theme.font(Font.PLAIN, 12));
        table.setForeground(Theme.TEXT);
        table.setBackground(Color.WHITE);
        table.setRowHeight(44);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(Theme.PRIMARY_SOFT);
        table.setSelectionForeground(Theme.TEXT);
        table.setAutoCreateRowSorter(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setPreferredScrollableViewportSize(new Dimension(800, 420));

        for (int i = 0; i < columns.length; i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            int width = columnWidth(columns[i]);
            column.setPreferredWidth(width);
            if (isStatusColumn(columns[i])) {
                column.setCellRenderer(new BadgeRenderer());
            } else if (isProgressColumn(columns[i])) {
                column.setCellRenderer(new ProgressRenderer());
            } else {
                column.setCellRenderer(new TextRenderer());
            }
        }

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));
        header.setDefaultRenderer(new HeaderRenderer());
        header.setBackground(new Color(0xF1F5F9));

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        Theme.styleScrollBar(scrollPane.getVerticalScrollBar(), false);
        Theme.styleScrollBar(scrollPane.getHorizontalScrollBar(), false);
        add(scrollPane, BorderLayout.CENTER);
    }

    public JTable getTable() {
        return table;
    }

    public void setRows(Object[][] rows) {
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
        sorter.setRowFilter(null);
    }

    public void filter(String text, int column, String value) {
        String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        boolean hasStatus = value != null && !value.isBlank() && !value.startsWith("全部");
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                if (hasStatus && column >= 0 && column < entry.getValueCount()) {
                    Object cell = entry.getValue(column);
                    if (cell == null || !String.valueOf(cell).equals(value)) {
                        return false;
                    }
                }
                if (query.isEmpty()) {
                    return true;
                }
                for (int i = 0; i < entry.getValueCount(); i++) {
                    Object cell = entry.getValue(i);
                    if (cell != null && String.valueOf(cell).toLowerCase(Locale.ROOT).contains(query)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public int visibleRowCount() {
        return table.getRowCount();
    }

    private static boolean isStatusColumn(String name) {
        return name.contains("状态") || name.contains("级别") || name.contains("结果")
                || name.contains("模式") || name.equals("共建国家");
    }

    private static boolean isProgressColumn(String name) {
        return name.contains("进度") || name.contains("到位率") || name.contains("达成率");
    }

    private static boolean isRightAligned(String name) {
        return name.contains("金额") || name.contains("投资") || name.contains("合同")
                || name.contains("拨付") || name.contains("目标") || name.contains("实际")
                || name.contains("人口") || name.contains("就业") || name.contains("培训")
                || name.contains("装机") || name.contains("评分") || name.contains("数")
                || name.contains("率") || name.contains("进度") || name.contains("偏差")
                || name.contains("阈值") || name.contains("触发值");
    }

    private static int columnWidth(String name) {
        if (name.contains("项目名称") || name.contains("企业名称") || name.contains("报表名称")
                || name.equals("名称")) {
            return 240;
        }
        if (name.contains("项目") || name.contains("规则") || name.contains("指标")) {
            return 180;
        }
        if (name.contains("时间") || name.contains("日期")) {
            return 112;
        }
        if (name.contains("说明") || name.contains("责任") || name.contains("联合体")) {
            return 150;
        }
        return Math.max(76, Math.min(160, 42 + name.length() * 16));
    }

    private final class TextRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            label.setFont(Theme.font(Font.PLAIN, 12));
            label.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            label.setOpaque(true);
            label.setBackground(selected ? Theme.PRIMARY_SOFT
                    : (row % 2 == 0 ? Color.WHITE : Theme.SURFACE_ALT));
            label.setForeground(selected ? Theme.TEXT : Theme.TEXT_SUB);
            String name = table.getColumnName(column);
            label.setHorizontalAlignment(isRightAligned(name) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return label;
        }
    }

    private final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            label.setOpaque(true);
            label.setBackground(new Color(0xF1F5F9));
            label.setForeground(Theme.TEXT_SUB);
            label.setFont(Theme.font(Font.BOLD, 11));
            label.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            String name = table.getColumnName(column);
            label.setHorizontalAlignment(isRightAligned(name) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return label;
        }
    }

    private final class BadgeRenderer implements TableCellRenderer {
        private final BadgeCell cell = new BadgeCell();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            cell.update(value == null ? "" : String.valueOf(value), selected, row);
            return cell;
        }
    }

    private final class BadgeCell extends JPanel {
        private final PillLabel pill = new PillLabel();

        private BadgeCell() {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
            add(pill);
        }

        private void update(String text, boolean selected, int row) {
            setBackground(selected ? Theme.PRIMARY_SOFT
                    : (row % 2 == 0 ? Color.WHITE : Theme.SURFACE_ALT));
            pill.setText(text);
            Color[] colors = badgeColors(text);
            pill.setColors(colors[0], colors[1]);
        }
    }

    private final class ProgressRenderer implements TableCellRenderer {
        private final ProgressCell cell = new ProgressCell();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            cell.update(value == null ? "" : String.valueOf(value), selected, row);
            return cell;
        }
    }

    private final class ProgressCell extends JPanel {
        private String text = "";
        private boolean selected;
        private int row;

        private ProgressCell() {
            setOpaque(true);
        }

        private void update(String text, boolean selected, int row) {
            this.text = text;
            this.selected = selected;
            this.row = row;
            setToolTipText(text);
            setBackground(selected ? Theme.PRIMARY_SOFT
                    : (row % 2 == 0 ? Color.WHITE : Theme.SURFACE_ALT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());

                double percent = parsePercent(text);
                if (Double.isNaN(percent)) {
                    g2.setFont(Theme.font(Font.PLAIN, 12));
                    g2.setColor(Theme.TEXT_SUB);
                    g2.drawString(text, 14, getHeight() / 2 + 4);
                    return;
                }
                percent = Math.max(0, Math.min(100, percent));
                int x = 14;
                int trackWidth = Math.max(30, getWidth() - 28);
                int barHeight = 18;
                int y = (getHeight() - barHeight) / 2;
                g2.setColor(new Color(0xEDF2F7));
                g2.fillRoundRect(x, y, trackWidth, barHeight, barHeight, barHeight);

                Color accent = percent >= 80 ? Theme.PRIMARY
                        : percent >= 50 ? Theme.ACCENT : Theme.DANGER;
                int fillWidth = Math.max(0, (int) Math.round(trackWidth * percent / 100.0));
                if (fillWidth > 0) {
                    g2.setPaint(new GradientPaint(x, y, accent,
                            x + fillWidth, y, Theme.mix(accent, Theme.SIDEBAR, 0.15f)));
                    g2.fillRoundRect(x, y, fillWidth, barHeight, barHeight, barHeight);
                }
                g2.setFont(Theme.font(Font.BOLD, 10));
                String label = String.format(Locale.ROOT, "%.0f%%", percent);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (trackWidth - fm.stringWidth(label)) / 2;
                int ty = y + (barHeight - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(percent >= 50 ? Color.WHITE : Theme.TEXT);
                g2.drawString(label, tx, ty);
            } finally {
                g2.dispose();
            }
        }

        private double parsePercent(String value) {
            if (value == null) {
                return Double.NaN;
            }
            String cleaned = value.replace("%", "").trim();
            try {
                return Double.parseDouble(cleaned);
            } catch (NumberFormatException ex) {
                return Double.NaN;
            }
        }
    }

    private static final class PillLabel extends JLabel {
        private Color background;
        private Color foreground;

        private PillLabel() {
            setOpaque(false);
            setFont(Theme.font(Font.BOLD, 10));
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        private void setColors(Color background, Color foreground) {
            this.background = background;
            this.foreground = foreground;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(background == null ? Theme.SURFACE_ALT : background);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            } finally {
                g2.dispose();
            }
            setForeground(foreground == null ? Theme.TEXT_SUB : foreground);
            super.paintComponent(g);
        }
    }

    private static Color[] badgeColors(String text) {
        if (text.contains("高") || text.contains("预警") || text.contains("延期")
                || text.contains("滞后") || text.contains("风险") || text.contains("失败")) {
            return new Color[]{Theme.DANGER_SOFT, Theme.DANGER};
        }
        if (text.contains("中") || text.contains("待") || text.contains("执行")
                || text.contains("更新")) {
            return new Color[]{Theme.WARNING_SOFT, Theme.WARNING};
        }
        if (text.contains("在建") || text.contains("正常") || text.contains("达成")
                || text.contains("完成") || text.contains("是")) {
            return new Color[]{Theme.SUCCESS_SOFT, Theme.PRIMARY_DARK};
        }
        return new Color[]{Theme.BLUE_SOFT, Theme.BLUE};
    }
}
