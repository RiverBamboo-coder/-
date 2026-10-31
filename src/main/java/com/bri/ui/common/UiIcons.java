package com.bri.ui.common;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Lightweight vector icons drawn with Java2D, so the UI has no icon-font dependency.
 */
public final class UiIcons {
    private UiIcons() {
    }

    public static Icon of(String name, int size, Color color) {
        return new VectorIcon(name, size, color);
    }

    private static final class VectorIcon implements Icon {
        private final String name;
        private final int size;
        private final Color color;

        private VectorIcon(String name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(Math.max(1.4f, size / 11f),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                float s = size;
                switch (name) {
                    case "dashboard" -> drawDashboard(g2, s);
                    case "globe" -> drawGlobe(g2, s);
                    case "enterprise" -> drawEnterprise(g2, s);
                    case "project" -> drawProject(g2, s);
                    case "fund" -> drawFund(g2, s);
                    case "progress" -> drawProgress(g2, s);
                    case "commitment" -> drawCommitment(g2, s);
                    case "warning" -> drawWarning(g2, s);
                    case "report" -> drawReport(g2, s);
                    case "outcome" -> drawOutcome(g2, s);
                    case "dictionary" -> drawDictionary(g2, s);
                    case "search" -> drawSearch(g2, s);
                    case "bell" -> drawBell(g2, s);
                    case "plus" -> drawPlus(g2, s);
                    case "export" -> drawExport(g2, s);
                    case "refresh" -> drawRefresh(g2, s);
                    case "filter" -> drawFilter(g2, s);
                    case "chevron" -> drawChevron(g2, s);
                    case "user" -> drawUser(g2, s);
                    case "check" -> drawCheck(g2, s);
                    case "clock" -> drawClock(g2, s);
                    case "spark" -> drawSpark(g2, s);
                    default -> g2.fill(new Ellipse2D.Double(s * 0.22, s * 0.22, s * 0.56, s * 0.56));
                }
            } finally {
                g2.dispose();
            }
        }

        private static void drawDashboard(Graphics2D g2, float s) {
            float r = s * 0.12f;
            float gap = s * 0.10f;
            float w = (s - gap * 3) / 2f;
            g2.fill(new RoundRectangle2D.Float(s * 0.14f, s * 0.14f, w, w, r, r));
            g2.fill(new RoundRectangle2D.Float(s * 0.14f + w + gap, s * 0.14f, w, w, r, r));
            g2.fill(new RoundRectangle2D.Float(s * 0.14f, s * 0.14f + w + gap, w, w, r, r));
            g2.fill(new RoundRectangle2D.Float(s * 0.14f + w + gap, s * 0.14f + w + gap, w, w, r, r));
        }

        private static void drawGlobe(Graphics2D g2, float s) {
            g2.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.72f, s * 0.72f));
            g2.draw(new Ellipse2D.Float(s * 0.33f, s * 0.14f, s * 0.34f, s * 0.72f));
            g2.draw(new Line2D.Float(s * 0.14f, s * 0.50f, s * 0.86f, s * 0.50f));
            g2.draw(new Arc2D.Float(s * 0.18f, s * 0.22f, s * 0.64f, s * 0.40f, 20f, 140f, Arc2D.OPEN));
        }

        private static void drawEnterprise(Graphics2D g2, float s) {
            g2.draw(new RoundRectangle2D.Float(s * 0.20f, s * 0.18f, s * 0.48f, s * 0.70f, s * 0.08f, s * 0.08f));
            g2.draw(new Line2D.Float(s * 0.68f, s * 0.34f, s * 0.82f, s * 0.34f));
            g2.draw(new Line2D.Float(s * 0.68f, s * 0.56f, s * 0.82f, s * 0.56f));
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 2; col++) {
                    g2.fill(new RoundRectangle2D.Float(
                            s * (0.31f + col * 0.16f),
                            s * (0.31f + row * 0.16f),
                            s * 0.07f, s * 0.07f, 2, 2));
                }
            }
        }

        private static void drawProject(Graphics2D g2, float s) {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.50f, s * 0.14f);
            p.lineTo(s * 0.86f, s * 0.34f);
            p.lineTo(s * 0.50f, s * 0.54f);
            p.lineTo(s * 0.14f, s * 0.34f);
            p.closePath();
            g2.draw(p);
            g2.draw(new Line2D.Float(s * 0.14f, s * 0.50f, s * 0.50f, s * 0.70f));
            g2.draw(new Line2D.Float(s * 0.86f, s * 0.50f, s * 0.50f, s * 0.70f));
            g2.draw(new Line2D.Float(s * 0.14f, s * 0.66f, s * 0.50f, s * 0.86f));
            g2.draw(new Line2D.Float(s * 0.86f, s * 0.66f, s * 0.50f, s * 0.86f));
        }

        private static void drawFund(Graphics2D g2, float s) {
            g2.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.72f, s * 0.72f));
            g2.setFont(Theme.font(Font.BOLD, s * 0.48f));
            String yen = "¥";
            int tw = g2.getFontMetrics().stringWidth(yen);
            g2.drawString(yen, (s - tw) / 2f, s * 0.63f);
        }

        private static void drawProgress(Graphics2D g2, float s) {
            g2.draw(new Line2D.Float(s * 0.15f, s * 0.82f, s * 0.85f, s * 0.82f));
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.18f, s * 0.66f);
            p.lineTo(s * 0.38f, s * 0.50f);
            p.lineTo(s * 0.56f, s * 0.58f);
            p.lineTo(s * 0.82f, s * 0.25f);
            g2.draw(p);
            g2.fill(new Ellipse2D.Float(s * 0.15f, s * 0.63f, s * 0.06f, s * 0.06f));
            g2.fill(new Ellipse2D.Float(s * 0.35f, s * 0.47f, s * 0.06f, s * 0.06f));
            g2.fill(new Ellipse2D.Float(s * 0.53f, s * 0.55f, s * 0.06f, s * 0.06f));
            g2.fill(new Ellipse2D.Float(s * 0.79f, s * 0.22f, s * 0.06f, s * 0.06f));
        }

        private static void drawCommitment(Graphics2D g2, float s) {
            Path2D shield = new Path2D.Float();
            shield.moveTo(s * 0.50f, s * 0.12f);
            shield.lineTo(s * 0.82f, s * 0.24f);
            shield.lineTo(s * 0.77f, s * 0.62f);
            shield.quadTo(s * 0.70f, s * 0.79f, s * 0.50f, s * 0.88f);
            shield.quadTo(s * 0.30f, s * 0.79f, s * 0.23f, s * 0.62f);
            shield.lineTo(s * 0.18f, s * 0.24f);
            shield.closePath();
            g2.draw(shield);
            Path2D check = new Path2D.Float();
            check.moveTo(s * 0.34f, s * 0.50f);
            check.lineTo(s * 0.46f, s * 0.62f);
            check.lineTo(s * 0.68f, s * 0.37f);
            g2.draw(check);
        }

        private static void drawWarning(Graphics2D g2, float s) {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.50f, s * 0.12f);
            p.lineTo(s * 0.88f, s * 0.82f);
            p.lineTo(s * 0.12f, s * 0.82f);
            p.closePath();
            g2.draw(p);
            g2.draw(new Line2D.Float(s * 0.50f, s * 0.35f, s * 0.50f, s * 0.60f));
            g2.fill(new Ellipse2D.Float(s * 0.47f, s * 0.69f, s * 0.06f, s * 0.06f));
        }

        private static void drawReport(Graphics2D g2, float s) {
            g2.draw(new Line2D.Float(s * 0.14f, s * 0.84f, s * 0.86f, s * 0.84f));
            g2.fill(new RoundRectangle2D.Float(s * 0.22f, s * 0.56f, s * 0.13f, s * 0.25f, 3, 3));
            g2.fill(new RoundRectangle2D.Float(s * 0.44f, s * 0.38f, s * 0.13f, s * 0.43f, 3, 3));
            g2.fill(new RoundRectangle2D.Float(s * 0.66f, s * 0.20f, s * 0.13f, s * 0.61f, 3, 3));
        }

        private static void drawOutcome(Graphics2D g2, float s) {
            Path2D leaf = new Path2D.Float();
            leaf.moveTo(s * 0.18f, s * 0.66f);
            leaf.quadTo(s * 0.20f, s * 0.24f, s * 0.78f, s * 0.18f);
            leaf.quadTo(s * 0.76f, s * 0.64f, s * 0.40f, s * 0.78f);
            leaf.quadTo(s * 0.26f, s * 0.82f, s * 0.18f, s * 0.66f);
            leaf.closePath();
            g2.draw(leaf);
            g2.draw(new Line2D.Float(s * 0.33f, s * 0.65f, s * 0.68f, s * 0.34f));
        }

        private static void drawDictionary(Graphics2D g2, float s) {
            g2.draw(new RoundRectangle2D.Float(s * 0.16f, s * 0.16f, s * 0.68f, s * 0.68f, s * 0.08f, s * 0.08f));
            g2.draw(new Line2D.Float(s * 0.36f, s * 0.22f, s * 0.36f, s * 0.78f));
            g2.draw(new Line2D.Float(s * 0.48f, s * 0.34f, s * 0.72f, s * 0.34f));
            g2.draw(new Line2D.Float(s * 0.48f, s * 0.50f, s * 0.72f, s * 0.50f));
            g2.draw(new Line2D.Float(s * 0.48f, s * 0.66f, s * 0.65f, s * 0.66f));
        }

        private static void drawSearch(Graphics2D g2, float s) {
            g2.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.52f, s * 0.52f));
            g2.draw(new Line2D.Float(s * 0.61f, s * 0.61f, s * 0.86f, s * 0.86f));
        }

        private static void drawBell(Graphics2D g2, float s) {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.22f, s * 0.66f);
            p.quadTo(s * 0.30f, s * 0.50f, s * 0.30f, s * 0.38f);
            p.quadTo(s * 0.30f, s * 0.18f, s * 0.50f, s * 0.18f);
            p.quadTo(s * 0.70f, s * 0.18f, s * 0.70f, s * 0.38f);
            p.quadTo(s * 0.70f, s * 0.50f, s * 0.78f, s * 0.66f);
            p.closePath();
            g2.draw(p);
            g2.draw(new Arc2D.Float(s * 0.42f, s * 0.69f, s * 0.16f, s * 0.14f, 180f, 180f, Arc2D.OPEN));
        }

        private static void drawPlus(Graphics2D g2, float s) {
            g2.draw(new Line2D.Float(s * 0.50f, s * 0.18f, s * 0.50f, s * 0.82f));
            g2.draw(new Line2D.Float(s * 0.18f, s * 0.50f, s * 0.82f, s * 0.50f));
        }

        private static void drawExport(Graphics2D g2, float s) {
            g2.draw(new RoundRectangle2D.Float(s * 0.18f, s * 0.44f, s * 0.64f, s * 0.38f, s * 0.08f, s * 0.08f));
            g2.draw(new Line2D.Float(s * 0.50f, s * 0.12f, s * 0.50f, s * 0.58f));
            g2.draw(new Line2D.Float(s * 0.34f, s * 0.28f, s * 0.50f, s * 0.12f));
            g2.draw(new Line2D.Float(s * 0.66f, s * 0.28f, s * 0.50f, s * 0.12f));
        }

        private static void drawRefresh(Graphics2D g2, float s) {
            g2.draw(new Arc2D.Float(s * 0.18f, s * 0.18f, s * 0.64f, s * 0.64f, 35f, 285f, Arc2D.OPEN));
            g2.draw(new Line2D.Float(s * 0.70f, s * 0.18f, s * 0.82f, s * 0.31f));
            g2.draw(new Line2D.Float(s * 0.82f, s * 0.31f, s * 0.66f, s * 0.34f));
        }

        private static void drawFilter(Graphics2D g2, float s) {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.14f, s * 0.20f);
            p.lineTo(s * 0.86f, s * 0.20f);
            p.lineTo(s * 0.58f, s * 0.50f);
            p.lineTo(s * 0.58f, s * 0.80f);
            p.lineTo(s * 0.42f, s * 0.88f);
            p.lineTo(s * 0.42f, s * 0.50f);
            p.closePath();
            g2.draw(p);
        }

        private static void drawChevron(Graphics2D g2, float s) {
            g2.draw(new Line2D.Float(s * 0.34f, s * 0.22f, s * 0.62f, s * 0.50f));
            g2.draw(new Line2D.Float(s * 0.62f, s * 0.50f, s * 0.34f, s * 0.78f));
        }

        private static void drawUser(Graphics2D g2, float s) {
            g2.draw(new Ellipse2D.Float(s * 0.36f, s * 0.14f, s * 0.28f, s * 0.28f));
            g2.draw(new Arc2D.Float(s * 0.20f, s * 0.48f, s * 0.60f, s * 0.38f, 0f, 180f, Arc2D.OPEN));
        }

        private static void drawCheck(Graphics2D g2, float s) {
            g2.draw(new Line2D.Float(s * 0.18f, s * 0.52f, s * 0.40f, s * 0.72f));
            g2.draw(new Line2D.Float(s * 0.40f, s * 0.72f, s * 0.82f, s * 0.28f));
        }

        private static void drawClock(Graphics2D g2, float s) {
            g2.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.72f, s * 0.72f));
            g2.draw(new Line2D.Float(s * 0.50f, s * 0.50f, s * 0.50f, s * 0.28f));
            g2.draw(new Line2D.Float(s * 0.50f, s * 0.50f, s * 0.66f, s * 0.60f));
        }

        private static void drawSpark(Graphics2D g2, float s) {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.50f, s * 0.10f);
            p.lineTo(s * 0.60f, s * 0.40f);
            p.lineTo(s * 0.90f, s * 0.50f);
            p.lineTo(s * 0.60f, s * 0.60f);
            p.lineTo(s * 0.50f, s * 0.90f);
            p.lineTo(s * 0.40f, s * 0.60f);
            p.lineTo(s * 0.10f, s * 0.50f);
            p.lineTo(s * 0.40f, s * 0.40f);
            p.closePath();
            g2.draw(p);
        }
    }
}
