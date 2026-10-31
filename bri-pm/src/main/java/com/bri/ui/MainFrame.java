package com.bri.ui;

import com.bri.ui.common.RoundedPanel;
import com.bri.ui.common.Theme;
import com.bri.ui.common.UiIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application shell. All pages call DataService; the shell itself knows nothing about SQL.
 */
public class MainFrame extends JFrame {
    private final DataService dataService;
    private final List<ModuleSpec> modules;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final Map<String, SidebarButton> navButtons = new LinkedHashMap<>();
    private final Map<String, JComponent> pages = new LinkedHashMap<>();
    private final Map<String, NavSpec> navSpecs = new LinkedHashMap<>();
    private final JLabel breadcrumb = new JLabel("工作台");
    private final JLabel titleLabel = new JLabel("总览看板");
    private final JLabel statusDot = new JLabel("●");
    private final JLabel statusText = new JLabel();
    private final JLabel clock = new JLabel();

    public MainFrame(DataService dataService) {
        this.dataService = dataService;
        this.modules = ModuleCatalog.all(dataService);
        Theme.install();
        setTitle("BRI-PM · 一带一路基础设施合作项目管理");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        setIconImage(createIcon());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);

        showPage("dashboard");
        new Timer(1000, e -> {
            updateClock();
            updateStatus();
        }).start();
        updateStatus();
    }

    public void showPage(String key) {
        NavSpec spec = navSpecs.get(key);
        if (spec == null) {
            return;
        }
        cardLayout.show(cards, key);
        navButtons.forEach((k, button) -> button.setActive(k.equals(key)));
        breadcrumb.setText(spec.section);
        titleLabel.setText(spec.title);
        JComponent page = pages.get(key);
        if (page instanceof ModulePanel panel) {
            panel.refresh();
        }
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        top.setPreferredSize(new Dimension(0, 76));

        JPanel gradient = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), 0, Theme.BLUE));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } finally {
                    g2.dispose();
                }
            }
        };
        gradient.setPreferredSize(new Dimension(0, 3));
        top.add(gradient, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 26, 0, 26));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        breadcrumb.setFont(Theme.font(Font.PLAIN, 11));
        breadcrumb.setForeground(Theme.MUTED);
        left.add(breadcrumb);
        left.add(Box.createHorizontalStrut(8));
        left.add(new JLabel(UiIcons.of("chevron", 12, Theme.BORDER_STRONG)));
        left.add(Box.createHorizontalStrut(8));
        titleLabel.setFont(Theme.font(Font.BOLD, 18));
        titleLabel.setForeground(Theme.TEXT);
        left.add(titleLabel);
        content.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 11, 0));
        right.setOpaque(false);
        right.add(pill(dataService.isDemoMode() ? "演示模式" : "数据库模式",
                dataService.isDemoMode() ? Theme.WARNING : Theme.SUCCESS,
                dataService.isDemoMode() ? Theme.WARNING_SOFT : Theme.SUCCESS_SOFT));
        JButton bell = new JButton(UiIcons.of("bell", 18, Theme.TEXT_SUB));
        bell.setBorder(new EmptyBorder(8, 8, 8, 8));
        bell.setContentAreaFilled(false);
        bell.setFocusPainted(false);
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bell.setToolTipText("通知");
        bell.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "数据库查询、M5 补录、M6 预警刷新和 M7 履约计算均可在对应页面演示。",
                "答辩提示", JOptionPane.INFORMATION_MESSAGE));
        right.add(bell);

        JPanel avatar = avatar("D", Theme.PRIMARY);
        JPanel userText = new JPanel();
        userText.setOpaque(false);
        userText.setLayout(new BoxLayout(userText, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("D 同学");
        name.setFont(Theme.font(Font.BOLD, 12));
        name.setForeground(Theme.TEXT);
        JLabel role = new JLabel("界面开发");
        role.setFont(Theme.font(Font.PLAIN, 10));
        role.setForeground(Theme.MUTED);
        userText.add(name);
        userText.add(role);
        JPanel user = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        user.setOpaque(false);
        user.add(avatar);
        user.add(userText);
        right.add(user);
        content.add(right, BorderLayout.EAST);
        top.add(content, BorderLayout.CENTER);
        return top;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(252, 0));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 17));
        brand.setOpaque(false);
        brand.add(new LogoMark());
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("BRI-PM");
        name.setFont(Theme.font(Font.BOLD, 19));
        name.setForeground(Color.WHITE);
        JLabel sub = new JLabel("基础设施合作项目管理");
        sub.setFont(Theme.font(Font.PLAIN, 10));
        sub.setForeground(Theme.SIDEBAR_TEXT);
        brandText.add(name);
        brandText.add(Box.createVerticalStrut(2));
        brandText.add(sub);
        brand.add(brandText);
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(4, 12, 8, 12));
        addSection(nav, "工作台");
        addNav(nav, new NavSpec("dashboard", "总览看板", "dashboard", "工作台"));
        addSection(nav, "基础与项目");
        addModuleNav(nav, "m1", "基础与项目");
        addModuleNav(nav, "m2", "基础与项目");
        addModuleNav(nav, "m3", "基础与项目");
        addSection(nav, "资金与执行");
        addModuleNav(nav, "m4", "资金与执行");
        addModuleNav(nav, "m5", "资金与执行");
        addModuleNav(nav, "m7", "资金与执行");
        addSection(nav, "风控与成效");
        addModuleNav(nav, "m6", "风控与成效");
        addModuleNav(nav, "m8", "风控与成效");
        addModuleNav(nav, "m9", "风控与成效");
        addSection(nav, "设计数据");
        addModuleNav(nav, "x4", "设计数据");
        nav.add(Box.createVerticalGlue());

        JScrollPane navScroll = new JScrollPane(nav);
        navScroll.setBorder(BorderFactory.createEmptyBorder());
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Theme.styleScrollBar(navScroll.getVerticalScrollBar(), true);
        sidebar.add(navScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(8, 14, 14, 14));
        RoundedPanel quote = new RoundedPanel(new BorderLayout(), Theme.SIDEBAR_SOFT, 14,
                Theme.SIDEBAR_LINE, false);
        quote.setBorder(new EmptyBorder(12, 13, 12, 13));
        quote.setAlignmentX(LEFT_ALIGNMENT);
        quote.setMaximumSize(new Dimension(224, 82));
        JLabel quoteTitle = new JLabel("人类命运共同体");
        quoteTitle.setFont(Theme.font(Font.BOLD, 11));
        quoteTitle.setForeground(Theme.ACCENT);
        JLabel quoteText = new JLabel("<html><body style='width:190px'>"
                + "以数据连接国家、企业与项目，让合作成果可追踪。"
                + "</body></html>");
        quoteText.setFont(Theme.font(Font.PLAIN, 10));
        quoteText.setForeground(Theme.SIDEBAR_TEXT);
        quote.add(quoteTitle, BorderLayout.NORTH);
        quote.add(quoteText, BorderLayout.CENTER);
        bottom.add(quote);
        bottom.add(Box.createVerticalStrut(10));
        JPanel user = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        user.setOpaque(false);
        user.add(avatar("D", Theme.PRIMARY));
        JLabel userLabel = new JLabel("D 同学 · 界面开发");
        userLabel.setFont(Theme.font(Font.PLAIN, 11));
        userLabel.setForeground(Theme.SIDEBAR_TEXT);
        user.add(userLabel);
        bottom.add(user);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildContent() {
        addPage("dashboard", new DashboardPanel(dataService));
        for (ModuleSpec module : modules) {
            addPage(module.key, new ModulePage(dataService, module));
        }
        cards.setBackground(Theme.BG);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG);
        wrapper.add(cards, BorderLayout.CENTER);
        return wrapper;
    }

    private void addPage(String key, JComponent page) {
        page.setBorder(new EmptyBorder(24, 28, 28, 28));
        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Theme.styleScrollBar(scroll.getVerticalScrollBar(), false);
        cards.add(scroll, key);
        pages.put(key, page);
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        bar.setPreferredSize(new Dimension(0, 34));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(0, 20, 0, 0));
        statusDot.setFont(Theme.font(Font.PLAIN, 10));
        statusText.setFont(Theme.font(Font.PLAIN, 10));
        statusText.setForeground(Theme.MUTED);
        left.add(statusDot);
        left.add(statusText);
        bar.add(left, BorderLayout.WEST);
        JLabel center = new JLabel("人类命运共同体 · 互利共赢", SwingConstants.CENTER);
        center.setFont(Theme.font(Font.PLAIN, 10));
        center.setForeground(Theme.MUTED);
        bar.add(center, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 0, 0, 20));
        clock.setFont(Theme.font(Font.PLAIN, 10));
        clock.setForeground(Theme.MUTED);
        JLabel version = new JLabel("BRI-PM UI v1.0");
        version.setFont(Theme.font(Font.PLAIN, 10));
        version.setForeground(Theme.MUTED);
        right.add(clock);
        right.add(version);
        bar.add(right, BorderLayout.EAST);
        updateClock();
        return bar;
    }

    private void updateStatus() {
        if (dataService.isDemoMode()) {
            statusDot.setForeground(Theme.WARNING);
            statusText.setText("演示模式 · 修改 db.properties 后可切换真实数据库");
        } else if (dataService.isUsingFallback()) {
            statusDot.setForeground(Theme.WARNING);
            statusText.setText("数据库连接失败，当前显示离线演示数据");
        } else {
            statusDot.setForeground(Theme.SUCCESS);
            statusText.setText("数据库模式 · 查询通过 DAO 执行");
        }
    }

    private void updateClock() {
        clock.setText(DateTimeFormatter.ofPattern("MM-dd HH:mm").format(LocalDateTime.now()));
    }

    private void addSection(JPanel nav, String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 9));
        label.setForeground(new Color(0x64748B));
        label.setBorder(new EmptyBorder(10, 10, 4, 10));
        label.setAlignmentX(LEFT_ALIGNMENT);
        nav.add(label);
    }

    private void addModuleNav(JPanel nav, String key, String section) {
        ModuleSpec module = modules.stream().filter(m -> m.key.equals(key)).findFirst().orElse(null);
        if (module != null) {
            addNav(nav, new NavSpec(module.key, module.title, module.icon, section));
        }
    }

    private void addNav(JPanel nav, NavSpec spec) {
        SidebarButton button = new SidebarButton(spec);
        button.addActionListener(e -> showPage(spec.key));
        nav.add(button);
        nav.add(Box.createVerticalStrut(2));
        navButtons.put(spec.key, button);
        navSpecs.put(spec.key, spec);
    }

    private RoundedPanel pill(String text, Color fg, Color bg) {
        RoundedPanel pill = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 0, 0), bg, 12, null, false);
        pill.setBorder(new EmptyBorder(5, 10, 5, 10));
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 10));
        label.setForeground(fg);
        pill.add(label);
        return pill;
    }

    private JPanel avatar(String text, Color color) {
        RoundedPanel avatar = new RoundedPanel(new GridBagLayout(), color, 30, null, false);
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setMaximumSize(new Dimension(34, 34));
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.font(Font.BOLD, 13));
        label.setForeground(Color.WHITE);
        avatar.add(label);
        return avatar;
    }

    private Image createIcon() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, 64, 64, Theme.BLUE));
            g2.fillRoundRect(2, 2, 60, 60, 18, 18);
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.font(Font.BOLD, 30));
            g2.drawString("B", 20, 44);
        } finally {
            g2.dispose();
        }
        return image;
    }

    private static final class NavSpec {
        private final String key;
        private final String title;
        private final String icon;
        private final String section;

        private NavSpec(String key, String title, String icon, String section) {
            this.key = key;
            this.title = title;
            this.icon = icon;
            this.section = section;
        }
    }

    private final class SidebarButton extends JButton {
        private final NavSpec spec;
        private boolean active;
        private boolean hover;

        private SidebarButton(NavSpec spec) {
            this.spec = spec;
            setText("");
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(228, 38));
            setPreferredSize(new Dimension(228, 38));
            setToolTipText(spec.title);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        private void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active || hover) {
                    g2.setColor(new Color(255, 255, 255, active ? 18 : 9));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
                if (active) {
                    g2.setColor(Theme.PRIMARY);
                    g2.fillRoundRect(0, 9, 3, getHeight() - 18, 3, 3);
                }
                g2.setColor(active ? Color.WHITE : Theme.SIDEBAR_TEXT);
                g2.setFont(Theme.font(active ? Font.BOLD : Font.PLAIN, 12));
                int baseline = (getHeight() - g2.getFontMetrics().getHeight()) / 2
                        + g2.getFontMetrics().getAscent();
                g2.drawString(spec.title, 48, baseline);
                UiIcons.of(spec.icon, 18, active ? Color.WHITE : Theme.SIDEBAR_TEXT)
                        .paintIcon(this, g2, 18, (getHeight() - 18) / 2);
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class LogoMark extends JPanel {
        private LogoMark() {
            setOpaque(false);
            setPreferredSize(new Dimension(42, 42));
            setMaximumSize(new Dimension(42, 42));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, 42, 42, Theme.BLUE));
                g2.fill(new RoundRectangle2D.Float(0, 0, 42, 42, 14, 14));
                g2.setColor(Color.WHITE);
                g2.setFont(Theme.font(Font.BOLD, 18));
                g2.drawString("B", 14, 28);
            } finally {
                g2.dispose();
            }
        }
    }
}
