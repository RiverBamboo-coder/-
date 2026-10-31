package com.bri.ui;

import com.bri.ui.common.ChartPanel;
import com.bri.ui.common.ModernTable;
import com.bri.ui.common.RoundedPanel;
import com.bri.ui.common.StatCard;
import com.bri.ui.common.Theme;
import com.bri.ui.common.UiIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/**
 * Overview page with live counts and two DAO-backed charts.
 */
public class DashboardPanel extends JPanel implements ModulePanel {
    private final DataService dataService;
    private final JPanel statsHost = new JPanel(new BorderLayout());
    private final JPanel chartsHost = new JPanel(new BorderLayout());
    private final JPanel tableHost = new JPanel(new BorderLayout());
    private final JLabel note = new JLabel("正在加载数据库数据...");

    public DashboardPanel(DataService dataService) {
        this.dataService = dataService;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(buildHeader());
        add(Box.createVerticalStrut(16));
        add(statsHost);
        add(Box.createVerticalStrut(14));
        add(chartsHost);
        add(Box.createVerticalStrut(14));
        add(tableHost);
        add(Box.createVerticalStrut(8));
        note.setFont(Theme.font(Font.PLAIN, 10));
        note.setForeground(Theme.MUTED);
        note.setAlignmentX(LEFT_ALIGNMENT);
        add(note);
        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("合作项目总览");
        title.setFont(Theme.font(Font.BOLD, 26));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("国家—项目—企业—资金—进度全要素跟踪");
        subtitle.setFont(Theme.font(Font.PLAIN, 12));
        subtitle.setForeground(Theme.MUTED);
        titles.add(title);
        titles.add(Box.createVerticalStrut(5));
        titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        actions.setOpaque(false);
        actions.add(pill("人类命运共同体", Theme.ACCENT, Theme.ACCENT_SOFT));
        actions.add(pill("互利共赢", Theme.PRIMARY, Theme.PRIMARY_SOFT));
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private void render(DataService.DashboardData data) {
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.add(new StatCard("共建国家", String.valueOf(data.countryCount),
                "覆盖多个大洲", "数据库", Theme.PRIMARY,
                UiIcons.of("globe", 24, Theme.PRIMARY), Theme.PRIMARY));
        stats.add(new StatCard("合作项目", String.valueOf(data.projectCount),
                "全生命周期项目", "数据库", Theme.BLUE,
                UiIcons.of("project", 24, Theme.BLUE), Theme.BLUE));
        stats.add(new StatCard("参与企业", String.valueOf(data.enterpriseCount),
                "中方 / 外方 / 金融机构", "数据库", Theme.PURPLE,
                UiIcons.of("enterprise", 24, Theme.PURPLE), Theme.PURPLE));
        stats.add(new StatCard("未处理预警", String.valueOf(data.openWarningCount),
                "资金、进度与履约风险", "关注", Theme.WARNING,
                UiIcons.of("warning", 24, Theme.WARNING), Theme.WARNING));
        statsHost.removeAll();
        statsHost.setOpaque(false);
        statsHost.add(stats, BorderLayout.CENTER);

        ChartPanel regions = new ChartPanel(ChartPanel.Kind.BAR,
                "区域项目分布", "按大洲统计项目数",
                data.regionLabels.length == 0 ? new String[]{"暂无"} : data.regionLabels,
                data.regionValues.length == 0 ? new double[]{0} : data.regionValues,
                new Color[]{Theme.PRIMARY, Theme.BLUE, Theme.PURPLE, Theme.ACCENT, Theme.SUCCESS});
        ChartPanel sources = new ChartPanel(ChartPanel.Kind.DONUT,
                "资金来源结构", "实际拨付占比",
                data.sourceLabels.length == 0 ? new String[]{"暂无"} : data.sourceLabels,
                data.sourceValues.length == 0 ? new double[]{0} : data.sourceValues,
                new Color[]{Theme.PRIMARY, Theme.BLUE, Theme.PURPLE, Theme.ACCENT, Theme.SUCCESS});
        JPanel charts = new JPanel(new GridBagLayout());
        charts.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.weightx = 1.1;
        gbc.insets = new Insets(0, 0, 0, 14);
        charts.add(regions, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.9;
        gbc.insets = new Insets(0, 0, 0, 0);
        charts.add(sources, gbc);
        chartsHost.removeAll();
        chartsHost.setOpaque(false);
        chartsHost.setPreferredSize(new Dimension(1040, 280));
        chartsHost.add(charts, BorderLayout.CENTER);

        RoundedPanel card = new RoundedPanel(new BorderLayout(), Color.WHITE, 18,
                Theme.BORDER, true);
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        tableHeader.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));
        JLabel title = new JLabel("重点项目");
        title.setFont(Theme.font(Font.BOLD, 15));
        title.setForeground(Theme.TEXT);
        JLabel count = new JLabel("最近 " + data.projectRows.length + " 条");
        count.setFont(Theme.font(Font.PLAIN, 11));
        count.setForeground(Theme.MUTED);
        tableHeader.add(title, BorderLayout.WEST);
        tableHeader.add(count, BorderLayout.EAST);
        card.add(tableHeader, BorderLayout.NORTH);
        card.add(new ModernTable(data.projectColumns, data.projectRows), BorderLayout.CENTER);
        tableHost.removeAll();
        tableHost.setOpaque(false);
        tableHost.setPreferredSize(new Dimension(1040, 330));
        tableHost.add(card, BorderLayout.CENTER);

        note.setText(data.fallback
                ? "离线演示数据" + (data.note == null ? "" : "：" + data.note)
                : "数据来源：MySQL bri_pm，查询全部通过现有 DAO");
        note.setForeground(data.fallback ? Theme.WARNING : Theme.SUCCESS);
        revalidate();
        repaint();
    }

    @Override
    public void refresh() {
        note.setForeground(Theme.MUTED);
        note.setText("正在加载数据库数据...");
        new SwingWorker<DataService.DashboardData, Void>() {
            @Override
            protected DataService.DashboardData doInBackground() {
                return dataService.dashboard();
            }

            @Override
            protected void done() {
                try {
                    render(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    note.setForeground(Theme.DANGER);
                    note.setText("加载失败：" + e.getMessage());
                }
            }
        }.execute();
    }

    private static RoundedPanel pill(String text, Color fg, Color bg) {
        RoundedPanel pill = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 0, 0), bg, 12, null, false);
        pill.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 10));
        label.setForeground(fg);
        pill.add(label);
        return pill;
    }
}
