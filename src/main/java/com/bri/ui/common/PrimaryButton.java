package com.bri.ui.common;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Rounded, hand-painted button with hover and pressed states.
 */
public class PrimaryButton extends JButton {
    public enum Variant {
        PRIMARY, SECONDARY, GHOST, DANGER
    }

    private final String label;
    private final Icon icon;
    private final Variant variant;
    private boolean hover;
    private boolean pressed;

    public PrimaryButton(String label, Icon icon, Variant variant) {
        this.label = label;
        this.icon = icon;
        this.variant = variant;
        setText("");
        setBorder(BorderFactory.createEmptyBorder());
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.font(Font.BOLD, 13));
        getAccessibleContext().setAccessibleName(label);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(label);
        int iconWidth = icon == null ? 0 : icon.getIconWidth() + 8;
        return new Dimension(Math.max(92, textWidth + iconWidth + 30), 38);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = 12;
            Color bg = null;
            Color fg;
            Color border = null;

            switch (variant) {
                case PRIMARY -> {
                    bg = Theme.PRIMARY;
                    fg = Color.WHITE;
                }
                case DANGER -> {
                    bg = Theme.DANGER;
                    fg = Color.WHITE;
                }
                case SECONDARY -> {
                    bg = Color.WHITE;
                    fg = Theme.TEXT;
                    border = Theme.BORDER_STRONG;
                }
                default -> {
                    fg = hover ? Theme.TEXT : Theme.MUTED;
                }
            }

            if (!isEnabled()) {
                bg = new Color(0xE2E8F0);
                fg = Theme.MUTED;
            } else if (pressed) {
                if (bg != null) {
                    bg = Theme.mix(bg, Color.BLACK, 0.10f);
                }
            } else if (hover) {
                if (bg != null) {
                    bg = Theme.mix(bg, Color.WHITE, 0.08f);
                } else {
                    bg = new Color(0xF1F5F9);
                }
            }

            if (bg != null) {
                if (variant == Variant.PRIMARY && isEnabled()) {
                    g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, w, h, Theme.PRIMARY_DARK));
                } else if (variant == Variant.DANGER && isEnabled()) {
                    g2.setPaint(new GradientPaint(0, 0, new Color(0xF87171), w, h, Theme.DANGER));
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }
            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            int iconWidth = icon == null ? 0 : icon.getIconWidth();
            int gap = icon == null ? 0 : 8;
            FontMetrics fm = g2.getFontMetrics(getFont());
            int contentWidth = iconWidth + gap + fm.stringWidth(label);
            int x = (w - contentWidth) / 2;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            if (icon != null) {
                icon.paintIcon(this, g2, x, (h - icon.getIconHeight()) / 2);
                x += iconWidth + gap;
            }
            g2.setFont(getFont());
            g2.setColor(fg);
            g2.drawString(label, x, y);
        } finally {
            g2.dispose();
        }
    }
}
