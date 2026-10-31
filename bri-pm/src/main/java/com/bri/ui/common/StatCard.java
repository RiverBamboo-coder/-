package com.bri.ui.common;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Compact KPI card for dashboards and module headers.
 */
public class StatCard extends RoundedPanel {
    public StatCard(String title, String value, String subtitle, String trend,
                    Color trendColor, Icon icon, Color accent) {
        super(new BorderLayout(14, 0), Color.WHITE, 18, Theme.BORDER, true);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setPreferredSize(new Dimension(250, 126));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        setAlignmentX(LEFT_ALIGNMENT);

        RoundedPanel iconBadge = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 0, 0),
                Theme.alpha(accent, 24), 16, null, false);
        iconBadge.setPreferredSize(new Dimension(48, 48));
        iconBadge.setMaximumSize(new Dimension(48, 48));
        JLabel iconLabel = new JLabel(icon);
        iconBadge.add(iconLabel);
        add(iconBadge, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.font(Font.PLAIN, 12));
        titleLabel.setForeground(Theme.MUTED);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        textPanel.add(titleLabel);

        JPanel valueRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        valueRow.setOpaque(false);
        valueRow.setAlignmentX(LEFT_ALIGNMENT);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(Theme.font(Font.BOLD, 24));
        valueLabel.setForeground(Theme.TEXT);
        valueRow.add(valueLabel);
        if (trend != null && !trend.isBlank()) {
            RoundedPanel trendPill = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 0, 0),
                    Theme.alpha(trendColor, 26), 10, null, false);
            trendPill.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            JLabel trendLabel = new JLabel(trend);
            trendLabel.setFont(Theme.font(Font.BOLD, 10));
            trendLabel.setForeground(trendColor);
            trendPill.add(trendLabel);
            valueRow.add(trendPill);
        }
        textPanel.add(valueRow);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(Theme.font(Font.PLAIN, 11));
        subtitleLabel.setForeground(Theme.MUTED);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        textPanel.add(subtitleLabel);

        add(textPanel, BorderLayout.CENTER);
    }
}
