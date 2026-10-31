package com.bri.ui.common;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

/**
 * Hand-painted bar, donut and area chart card.
 */
public class ChartPanel extends JPanel {
    public enum Kind {
        BAR, DONUT, AREA
    }

    private final Kind kind;
    private final String title;
    private final String subtitle;
    private final String[] labels;
    private final double[] values;
    private final Color[] colors;

    public ChartPanel(Kind kind, String title, String subtitle,
                      String[] labels, double[] values, Color[] colors) {
        this.kind = kind;
        this.title = title;
        this.subtitle = subtitle;
        this.labels = labels;
        this.values = values;
        this.colors = colors;
        setOpaque(false);
        setPreferredSize(new Dimension(360, 270));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(15, 23, 42, 8));
            g2.fillRoundRect(3, 4, w - 6, h - 7, 20, 20);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 18, 18);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

            g2.setFont(Theme.font(Font.BOLD, 14));
            g2.setColor(Theme.TEXT);
            g2.drawString(title, 20, 29);
            g2.setFont(Theme.font(Font.PLAIN, 10.5f));
            g2.setColor(Theme.MUTED);
            g2.drawString(subtitle, 20, 48);

            int top = 76;
            int bottom = h - 34;
            int left = 24;
            int right = w - 22;
            switch (kind) {
                case BAR -> paintBar(g2, left, top, right, bottom);
                case DONUT -> paintDonut(g2, left, top, right, bottom);
                case AREA -> paintArea(g2, left, top, right, bottom);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintBar(Graphics2D g2, int left, int top, int right, int bottom) {
        double max = Math.max(1, Arrays.stream(values).max().orElse(1) * 1.18);
        paintGrid(g2, left, top, right, bottom);
        int chartHeight = bottom - top;
        double step = (right - left) / (double) values.length;
        int barWidth = (int) Math.min(44, step * 0.46);
        for (int i = 0; i < values.length; i++) {
            int barHeight = (int) Math.round(values[i] / max * chartHeight);
            int cx = (int) Math.round(left + step * i + step / 2.0);
            int x = cx - barWidth / 2;
            int y = bottom - barHeight;
            Color color = colors[i % colors.length];
            g2.setPaint(new GradientPaint(x, y, color, x, bottom, Theme.mix(color, Theme.SIDEBAR, 0.18f)));
            g2.fill(new RoundRectangle2D.Float(x, y, barWidth, barHeight, 10, 10));
            g2.setFont(Theme.font(Font.BOLD, 10));
            g2.setColor(Theme.TEXT_SUB);
            String value = formatValue(values[i]);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(value, cx - fm.stringWidth(value) / 2, y - 7);
            drawXLabel(g2, labels[i], cx, bottom + 18, (int) step);
        }
    }

    private void paintDonut(Graphics2D g2, int left, int top, int right, int bottom) {
        double total = Arrays.stream(values).sum();
        if (total <= 0) {
            return;
        }
        int chartHeight = bottom - top;
        int centerX = (int) (left + (right - left) * 0.30);
        int centerY = top + chartHeight / 2;
        int radius = (int) Math.min((right - left) * 0.18, chartHeight * 0.36);
        int ring = Math.max(18, (int) (radius * 0.44));

        g2.setColor(new Color(0xEEF2F7));
        g2.setStroke(new BasicStroke(ring, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Double(centerX - radius, centerY - radius,
                radius * 2.0, radius * 2.0, 0, 360, Arc2D.OPEN));

        double start = 90;
        for (int i = 0; i < values.length; i++) {
            double extent = -360.0 * values[i] / total;
            g2.setColor(colors[i % colors.length]);
            g2.draw(new Arc2D.Double(centerX - radius, centerY - radius,
                    radius * 2.0, radius * 2.0, start, extent, Arc2D.OPEN));
            start += extent;
        }

        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.font(Font.BOLD, 18));
        String center = "100%";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(center, centerX - fm.stringWidth(center) / 2, centerY + 2);
        g2.setColor(Theme.MUTED);
        g2.setFont(Theme.font(Font.PLAIN, 9.5f));
        String caption = "资金来源";
        fm = g2.getFontMetrics();
        g2.drawString(caption, centerX - fm.stringWidth(caption) / 2, centerY + 19);

        int legendX = (int) (left + (right - left) * 0.56);
        int legendY = top + 26;
        g2.setFont(Theme.font(Font.PLAIN, 10));
        for (int i = 0; i < labels.length; i++) {
            int y = legendY + i * 26;
            g2.setColor(colors[i % colors.length]);
            g2.fill(new Ellipse2D.Float(legendX, y - 8, 9, 9));
            g2.setColor(Theme.TEXT_SUB);
            g2.drawString(truncate(labels[i], 10), legendX + 16, y);
            String pct = Math.round(values[i] / total * 100) + "%";
            FontMetrics lfm = g2.getFontMetrics();
            g2.setColor(Theme.MUTED);
            g2.drawString(pct, right - lfm.stringWidth(pct), y);
        }
    }

    private void paintArea(Graphics2D g2, int left, int top, int right, int bottom) {
        double max = Math.max(1, Arrays.stream(values).max().orElse(1) * 1.18);
        paintGrid(g2, left, top, right, bottom);
        int chartHeight = bottom - top;
        int chartWidth = right - left;
        double step = chartWidth / (double) Math.max(1, values.length - 1);

        Path2D area = new Path2D.Double();
        area.moveTo(left, bottom);
        for (int i = 0; i < values.length; i++) {
            double x = left + step * i;
            double y = bottom - values[i] / max * chartHeight;
            if (i == 0) {
                area.lineTo(x, y);
            } else {
                area.lineTo(x, y);
            }
        }
        area.lineTo(right, bottom);
        area.closePath();
        Color color = colors[0];
        g2.setPaint(new GradientPaint(0, top, Theme.alpha(color, 95),
                0, bottom, Theme.alpha(color, 0)));
        g2.fill(area);

        Path2D line = new Path2D.Double();
        for (int i = 0; i < values.length; i++) {
            double x = left + step * i;
            double y = bottom - values[i] / max * chartHeight;
            if (i == 0) {
                line.moveTo(x, y);
            } else {
                line.lineTo(x, y);
            }
        }
        g2.setColor(color);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);

        for (int i = 0; i < values.length; i++) {
            double x = left + step * i;
            double y = bottom - values[i] / max * chartHeight;
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(x - 4, y - 4, 8, 8));
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Double(x - 4, y - 4, 8, 8));
            drawXLabel(g2, labels[i], (int) x, bottom + 18, (int) step);
        }
        String last = formatValue(values[values.length - 1]);
        g2.setFont(Theme.font(Font.BOLD, 11));
        g2.setColor(color);
        g2.drawString(last, right - 30, bottom - (int) (values[values.length - 1] / max * chartHeight) - 10);
    }

    private void paintGrid(Graphics2D g2, int left, int top, int right, int bottom) {
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 4; i++) {
            int y = top + (bottom - top) * i / 4;
            g2.setColor(i == 4 ? Theme.BORDER_STRONG : new Color(0xEDF2F7));
            g2.drawLine(left, y, right, y);
        }
    }

    private void drawXLabel(Graphics2D g2, String label, int centerX, int baseline, int step) {
        g2.setFont(Theme.font(Font.PLAIN, 10));
        g2.setColor(Theme.MUTED);
        String shortLabel = truncate(label, Math.max(4, step / 12));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(shortLabel, centerX - fm.stringWidth(shortLabel) / 2, baseline);
    }

    private static String formatValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, Math.max(1, max - 1)) + "…";
    }
}
