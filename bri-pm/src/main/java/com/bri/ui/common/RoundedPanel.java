package com.bri.ui.common;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Base card surface used by the whole prototype.
 */
public class RoundedPanel extends JPanel {
    private Color fill;
    private Color borderColor;
    private int arc;
    private boolean shadow;

    public RoundedPanel() {
        this(null, Color.WHITE, 18, null, true);
    }

    public RoundedPanel(LayoutManager layout) {
        this(layout, Color.WHITE, 18, null, true);
    }

    public RoundedPanel(Color fill, int arc) {
        this(null, fill, arc, null, true);
    }

    public RoundedPanel(LayoutManager layout, Color fill, int arc) {
        this(layout, fill, arc, null, true);
    }

    public RoundedPanel(LayoutManager layout, Color fill, int arc, Color borderColor, boolean shadow) {
        super(layout);
        this.fill = fill;
        this.arc = arc;
        this.borderColor = borderColor;
        this.shadow = shadow;
        setOpaque(false);
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setArc(int arc) {
        this.arc = arc;
        repaint();
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            if (shadow) {
                for (int i = 5; i >= 1; i--) {
                    g2.setColor(new Color(15, 23, 42, 5 + (5 - i) * 2));
                    g2.fillRoundRect(i, i + 2, width - i * 2, height - i * 2, arc, arc);
                }
            }
            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, width - 1, height - 1, arc, arc);
            }
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
