package com.bri.ui.common;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

/**
 * Shared visual language for the D-side Swing prototype.
 */
public final class Theme {
    public static final Color BG = new Color(0xF4F7FB);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_ALT = new Color(0xF8FAFC);
    public static final Color SIDEBAR = new Color(0x0F172A);
    public static final Color SIDEBAR_SOFT = new Color(0x1E293B);
    public static final Color SIDEBAR_LINE = new Color(0x334155);
    public static final Color SIDEBAR_TEXT = new Color(0xCBD5E1);
    public static final Color PRIMARY = new Color(0x0D9488);
    public static final Color PRIMARY_DARK = new Color(0x0F766E);
    public static final Color PRIMARY_SOFT = new Color(0xE6FFFA);
    public static final Color BLUE = new Color(0x3B82F6);
    public static final Color BLUE_SOFT = new Color(0xEFF6FF);
    public static final Color ACCENT = new Color(0xF59E0B);
    public static final Color ACCENT_SOFT = new Color(0xFFFBEB);
    public static final Color PURPLE = new Color(0x8B5CF6);
    public static final Color PURPLE_SOFT = new Color(0xF5F3FF);
    public static final Color SUCCESS = new Color(0x22C55E);
    public static final Color SUCCESS_SOFT = new Color(0xF0FDF4);
    public static final Color DANGER = new Color(0xEF4444);
    public static final Color DANGER_SOFT = new Color(0xFEF2F2);
    public static final Color WARNING = new Color(0xF97316);
    public static final Color WARNING_SOFT = new Color(0xFFF7ED);
    public static final Color TEXT = new Color(0x0F172A);
    public static final Color TEXT_SUB = new Color(0x475569);
    public static final Color MUTED = new Color(0x64748B);
    public static final Color BORDER = new Color(0xE2E8F0);
    public static final Color BORDER_STRONG = new Color(0xCBD5E1);

    public static final String FONT_FAMILY = chooseFontFamily();

    private Theme() {
    }

    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, Math.round(size));
    }

    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color mix(Color a, Color b, float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        return new Color(
                Math.round(a.getRed() * (1 - r) + b.getRed() * r),
                Math.round(a.getGreen() * (1 - r) + b.getGreen() * r),
                Math.round(a.getBlue() * (1 - r) + b.getBlue() * r)
        );
    }

    public static Border cardBorder() {
        return BorderFactory.createLineBorder(BORDER, 1, true);
    }

    public static void styleTextField(JTextField field) {
        field.setFont(font(Font.PLAIN, 13));
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)
        ));
    }

    public static void styleComboBox(JComboBox<?> combo) {
        combo.setFont(font(Font.PLAIN, 13));
        combo.setForeground(TEXT);
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
    }

    public static void styleScrollBar(JScrollBar bar, boolean dark) {
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = dark ? new Color(0x475569) : BORDER_STRONG;
                trackColor = dark ? SIDEBAR : BG;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }
        });
        bar.setPreferredSize(new Dimension(dark ? 6 : 8, dark ? 6 : 8));
        bar.setUnitIncrement(18);
        bar.setOpaque(false);
    }

    public static void install() {
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", font(Font.PLAIN, 13));
        UIManager.put("Button.font", font(Font.BOLD, 13));
        UIManager.put("Table.font", font(Font.PLAIN, 13));
        UIManager.put("TableHeader.font", font(Font.BOLD, 12));
        UIManager.put("TextField.font", font(Font.PLAIN, 13));
        UIManager.put("ComboBox.font", font(Font.PLAIN, 13));
        UIManager.put("OptionPane.messageFont", font(Font.PLAIN, 13));
        UIManager.put("OptionPane.buttonFont", font(Font.BOLD, 13));
        UIManager.put("ToolTip.font", font(Font.PLAIN, 12));
        UIManager.put("ToolTip.background", SIDEBAR);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(7, 9, 7, 9));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumb", BORDER_STRONG);
        UIManager.put("ScrollBar.track", SURFACE_ALT);
    }

    private static String chooseFontFamily() {
        String[] preferred = {
                "Microsoft YaHei UI",
                "Microsoft YaHei",
                "PingFang SC",
                "Noto Sans CJK SC",
                "Source Han Sans SC",
                "SansSerif"
        };
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        return Arrays.stream(preferred)
                .filter(name -> Arrays.asList(available).contains(name))
                .findFirst()
                .orElse("SansSerif");
    }

    private static JButton zeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setBorder(BorderFactory.createEmptyBorder());
        return button;
    }
}
