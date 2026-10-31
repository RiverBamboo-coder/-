package com.bri.ui;

import com.bri.common.Result;
import com.bri.dao.WarningDao;
import com.bri.ui.common.ModernTable;
import com.bri.ui.common.PrimaryButton;
import com.bri.ui.common.RoundedPanel;
import com.bri.ui.common.Theme;
import com.bri.ui.common.UiIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

/**
 * Generic query page. Each of the 9 modules is one instance of this class,
 * configured by ModuleCatalog. The page only talks to DataService, never SQL.
 */
public class ModulePage extends JPanel implements ModulePanel {
    private final DataService dataService;
    private final ModuleSpec spec;
    private final JComboBox<QueryAction> queryBox = new JComboBox<>();
    private final JPanel inputHost = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JTextField textInput = new JTextField();
    private final JComboBox<String> optionInput = new JComboBox<>();
    private final JTextField searchField = new SearchField("在当前结果中筛选");
    private final JLabel descriptionLabel = new JLabel();
    private final JLabel resultCount = new JLabel("共 0 条记录");
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel tableHost = new JPanel(new BorderLayout());
    private PrimaryButton queryButton;
    private ModernTable table;
    private QueryAction currentAction;

    public ModulePage(DataService dataService, ModuleSpec spec) {
        this.dataService = dataService;
        this.spec = spec;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUi();
        if (!spec.actions.isEmpty()) {
            currentAction = spec.actions.get(0);
            queryBox.setSelectedIndex(0);
            applyActionInput(currentAction);
            runQuery();
            queryBox.addActionListener(e -> {
                QueryAction action = (QueryAction) queryBox.getSelectedItem();
                if (action != null) {
                    applyActionInput(action);
                    runQuery();
                }
            });
        }
    }

    private void buildUi() {
        add(buildHeader());
        add(Box.createVerticalStrut(16));
        add(buildQueryBar());
        add(Box.createVerticalStrut(6));
        descriptionLabel.setFont(Theme.font(Font.PLAIN, 11));
        descriptionLabel.setForeground(Theme.MUTED);
        descriptionLabel.setAlignmentX(LEFT_ALIGNMENT);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        add(descriptionLabel);
        add(Box.createVerticalStrut(12));
        add(buildMain());
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(spec.title);
        title.setFont(Theme.font(Font.BOLD, 24));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel(spec.subtitle);
        subtitle.setFont(Theme.font(Font.PLAIN, 12));
        subtitle.setForeground(Theme.MUTED);
        titles.add(title);
        titles.add(Box.createVerticalStrut(5));
        titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        actions.setOpaque(false);
        actions.add(pill(spec.badge, spec.accent));
        if ("m5".equals(spec.key)) {
            PrimaryButton record = new PrimaryButton("补录实际日期",
                    UiIcons.of("check", 14, Color.WHITE), PrimaryButton.Variant.PRIMARY);
            record.addActionListener(e -> showRecordActualDialog());
            actions.add(record);
        } else if ("m6".equals(spec.key)) {
            PrimaryButton refresh = new PrimaryButton("刷新预警",
                    UiIcons.of("refresh", 14, Color.WHITE), PrimaryButton.Variant.PRIMARY);
            refresh.addActionListener(e -> refreshWarnings());
            PrimaryButton ack = new PrimaryButton("处理选中",
                    UiIcons.of("check", 14, Theme.TEXT_SUB), PrimaryButton.Variant.SECONDARY);
            ack.addActionListener(e -> ackSelectedWarning());
            actions.add(ack);
            actions.add(refresh);
        } else if ("m7".equals(spec.key)) {
            PrimaryButton calc = new PrimaryButton("重新计算履约",
                    UiIcons.of("refresh", 14, Color.WHITE), PrimaryButton.Variant.PRIMARY);
            calc.addActionListener(e -> calculateCommitments());
            actions.add(calc);
        }
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildQueryBar() {
        RoundedPanel bar = new RoundedPanel(new BorderLayout(10, 0), Color.WHITE, 15,
                Theme.BORDER, false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        bar.setAlignmentX(LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        for (QueryAction action : spec.actions) {
            queryBox.addItem(action);
        }
        queryBox.setPreferredSize(new Dimension(190, 36));
        Theme.styleComboBox(queryBox);
        left.add(queryBox);

        inputHost.setOpaque(false);
        left.add(inputHost);

        searchField.setPreferredSize(new Dimension(190, 36));
        Theme.styleTextField(searchField);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterCurrentTable();
            }
        });
        left.add(searchField);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        queryButton = new PrimaryButton("执行查询",
                UiIcons.of("search", 14, Color.WHITE), PrimaryButton.Variant.PRIMARY);
        queryButton.setPreferredSize(new Dimension(104, 36));
        queryButton.addActionListener(e -> runQuery());
        PrimaryButton refresh = new PrimaryButton("刷新",
                UiIcons.of("refresh", 14, Theme.TEXT_SUB), PrimaryButton.Variant.GHOST);
        refresh.setPreferredSize(new Dimension(82, 36));
        refresh.addActionListener(e -> runQuery());
        right.add(queryButton);
        right.add(refresh);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);
        main.setAlignmentX(LEFT_ALIGNMENT);
        main.setPreferredSize(new Dimension(1040, 520));
        main.setMaximumSize(new Dimension(Integer.MAX_VALUE, 700));

        RoundedPanel tableCard = new RoundedPanel(new BorderLayout(), Color.WHITE, 18,
                Theme.BORDER, true);
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        tableHeader.setBorder(BorderFactory.createEmptyBorder(15, 18, 10, 18));
        JLabel title = new JLabel("查询结果");
        title.setFont(Theme.font(Font.BOLD, 15));
        title.setForeground(Theme.TEXT);
        resultCount.setFont(Theme.font(Font.PLAIN, 11));
        resultCount.setForeground(Theme.MUTED);
        tableHeader.add(title, BorderLayout.WEST);
        tableHeader.add(resultCount, BorderLayout.EAST);
        tableCard.add(tableHeader, BorderLayout.NORTH);

        tableHost.setOpaque(true);
        tableHost.setBackground(Color.WHITE);
        tableHost.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        tableHost.setPreferredSize(new Dimension(760, 420));
        tableHost.setMinimumSize(new Dimension(420, 220));
        tableCard.add(tableHost, BorderLayout.CENTER);

        statusLabel.setFont(Theme.font(Font.PLAIN, 10));
        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 18, 10, 18));
        tableCard.add(statusLabel, BorderLayout.SOUTH);

        GridBagConstraints tableGbc = new GridBagConstraints();
        tableGbc.gridx = 0;
        tableGbc.gridy = 0;
        tableGbc.weightx = 1;
        tableGbc.weighty = 1;
        tableGbc.fill = GridBagConstraints.BOTH;
        tableGbc.insets = new Insets(0, 0, 0, 16);
        main.add(tableCard, tableGbc);

        RoundedPanel insight = buildInsightCard();
        GridBagConstraints insightGbc = new GridBagConstraints();
        insightGbc.gridx = 1;
        insightGbc.gridy = 0;
        insightGbc.weightx = 0;
        insightGbc.weighty = 1;
        insightGbc.fill = GridBagConstraints.VERTICAL;
        main.add(insight, insightGbc);
        return main;
    }

    private RoundedPanel buildInsightCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), Color.WHITE, 18,
                Theme.BORDER, true);
        card.setPreferredSize(new Dimension(300, 520));
        card.setMaximumSize(new Dimension(300, 700));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("答辩提示");
        eyebrow.setFont(Theme.font(Font.BOLD, 11));
        eyebrow.setForeground(spec.accent);
        eyebrow.setAlignmentX(LEFT_ALIGNMENT);
        body.add(eyebrow);
        body.add(Box.createVerticalStrut(10));
        JLabel summary = new JLabel(wrap(spec.defenseSummary, 240));
        summary.setFont(Theme.font(Font.BOLD, 15));
        summary.setForeground(Theme.TEXT);
        summary.setAlignmentX(LEFT_ALIGNMENT);
        summary.setMaximumSize(new Dimension(264, 86));
        body.add(summary);
        body.add(Box.createVerticalStrut(14));
        for (String point : spec.defensePoints) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(264, 58));
            JLabel dot = new JLabel("●");
            dot.setFont(Theme.font(Font.PLAIN, 10));
            dot.setForeground(spec.accent);
            JLabel text = new JLabel(wrap(point, 205));
            text.setFont(Theme.font(Font.PLAIN, 12));
            text.setForeground(Theme.TEXT_SUB);
            row.add(dot, BorderLayout.WEST);
            row.add(text, BorderLayout.CENTER);
            body.add(row);
            body.add(Box.createVerticalStrut(8));
        }
        body.add(Box.createVerticalGlue());
        RoundedPanel quote = new RoundedPanel(new BorderLayout(),
                Theme.alpha(spec.accent, 18), 14, null, false);
        quote.setBorder(BorderFactory.createEmptyBorder(12, 13, 12, 13));
        quote.setAlignmentX(LEFT_ALIGNMENT);
        quote.setMaximumSize(new Dimension(264, 84));
        JLabel quoteText = new JLabel(wrap(
                "界面只调用 DAO，不写 SQL；查询失败时自动切换为离线演示数据。", 220));
        quoteText.setFont(Theme.font(Font.PLAIN, 11));
        quoteText.setForeground(Theme.TEXT_SUB);
        quote.add(quoteText, BorderLayout.CENTER);
        body.add(quote);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void applyActionInput(QueryAction action) {
        currentAction = action;
        inputHost.removeAll();
        if (action.needsInput()) {
            JLabel label = new JLabel(action.inputLabel + "：");
            label.setFont(Theme.font(Font.PLAIN, 11));
            label.setForeground(Theme.MUTED);
            inputHost.add(label);
            if (action.hasOptions()) {
                optionInput.removeAllItems();
                for (String option : action.inputOptions) {
                    optionInput.addItem(option);
                }
                optionInput.setSelectedItem(action.inputDefault);
                optionInput.setPreferredSize(new Dimension(120, 36));
                Theme.styleComboBox(optionInput);
                inputHost.add(optionInput);
            } else {
                textInput.setText(action.inputDefault);
                textInput.setPreferredSize(new Dimension(110, 36));
                Theme.styleTextField(textInput);
                inputHost.add(textInput);
            }
        }
        descriptionLabel.setText(action.description);
        inputHost.revalidate();
        inputHost.repaint();
    }

    private String currentInput() {
        if (currentAction == null || !currentAction.needsInput()) {
            return "";
        }
        if (currentAction.hasOptions()) {
            return String.valueOf(optionInput.getSelectedItem());
        }
        return textInput.getText();
    }

    private void runQuery() {
        if (currentAction == null) {
            return;
        }
        queryButton.setEnabled(false);
        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setText("查询中...");
        String input = currentInput();
        QueryAction action = currentAction;
        new SwingWorker<TableData, Void>() {
            @Override
            protected TableData doInBackground() {
                return action.load(input);
            }

            @Override
            protected void done() {
                queryButton.setEnabled(true);
                try {
                    setTable(get());
                    updateStatusAfterQuery();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showStatusError("查询被中断");
                } catch (ExecutionException e) {
                    showStatusError("查询失败：" + rootMessage(e));
                }
            }
        }.execute();
    }

    private void setTable(TableData data) {
        tableHost.removeAll();
        if (data.getColumns().length == 0) {
            JLabel empty = new JLabel("暂无数据", JLabel.CENTER);
            empty.setFont(Theme.font(Font.PLAIN, 13));
            empty.setForeground(Theme.MUTED);
            tableHost.add(empty, BorderLayout.CENTER);
            table = null;
        } else {
            table = new ModernTable(data.getColumns(), data.getRows());
            table.setOpaque(true);
            tableHost.add(table, BorderLayout.CENTER);
            filterCurrentTable();
        }
        resultCount.setText("共 " + data.getRows().length + " 条记录");
        tableHost.revalidate();
        tableHost.validate();
        tableHost.repaint();
    }

    private void updateStatusAfterQuery() {
        if (dataService.isUsingFallback()) {
            String reason = dataService.getLastError();
            statusLabel.setForeground(Theme.WARNING);
            statusLabel.setText("离线演示数据" + (reason == null ? "" : "：" + shorten(reason, 90)));
        } else {
            statusLabel.setForeground(Theme.SUCCESS);
            statusLabel.setText("真实数据库查询完成");
        }
    }

    private void filterCurrentTable() {
        if (table != null) {
            table.filter(searchField.getText(), -1, null);
            resultCount.setText("共 " + table.visibleRowCount() + " 条记录");
        }
    }

    private void showStatusError(String message) {
        statusLabel.setForeground(Theme.DANGER);
        statusLabel.setText(message);
    }

    private void showRecordActualDialog() {
        JTextField projectId = new JTextField("1");
        JTextField milestoneId = new JTextField("3");
        JTextField date = new JTextField(LocalDate.now().toString());
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("项目ID："));
        form.add(projectId);
        form.add(new JLabel("里程碑ID（1-6）："));
        form.add(milestoneId);
        form.add(new JLabel("实际日期："));
        form.add(date);
        int choice = JOptionPane.showConfirmDialog(this, form, "补录里程碑实际日期",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        Result<Void> result = dataService.recordActual(projectId.getText(), milestoneId.getText(), date.getText());
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, result.getMessage());
            runQuery();
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(), "补录失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshWarnings() {
        try {
            WarningDao.RefreshResult result = dataService.refreshWarnings();
            String text = "活跃预警：" + result.getActive()
                    + "\n本次命中：" + result.getMatched()
                    + "\n自动关闭：" + result.getClosed()
                    + (result.getSkippedMetrics().isEmpty() ? ""
                    : "\n跳过指标：" + String.join("、", result.getSkippedMetrics()));
            JOptionPane.showMessageDialog(this, text, "刷新预警", JOptionPane.INFORMATION_MESSAGE);
            runQuery();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "刷新失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ackSelectedWarning() {
        if (table == null || table.getTable().getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "请先在表格中选中一条预警。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int viewRow = table.getTable().getSelectedRow();
        Object value = table.getTable().getValueAt(viewRow, 0);
        long warnId;
        try {
            warnId = Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "当前查询结果第一列不是预警编号。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Result<Integer> result = dataService.ackWarning(warnId);
        JOptionPane.showMessageDialog(this, result.getMessage(),
                result.isSuccess() ? "处理成功" : "处理失败",
                result.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (result.isSuccess()) {
            runQuery();
        }
    }

    private void calculateCommitments() {
        try {
            int count = dataService.calcCommitments();
            JOptionPane.showMessageDialog(this, "履约结果已重新计算，共 " + count + " 条。",
                    "M7 履约计算", JOptionPane.INFORMATION_MESSAGE);
            runQuery();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "计算失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private static String wrap(String text, int widthPx) {
        return "<html><body style='width:" + widthPx + "px'>" + text + "</body></html>";
    }

    private static RoundedPanel pill(String text, Color color) {
        RoundedPanel panel = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 0, 0),
                Theme.alpha(color, 24), 12, null, false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 10));
        label.setForeground(color);
        panel.add(label);
        return panel;
    }

    @Override
    public void refresh() {
        runQuery();
    }

    private static final class SearchField extends JTextField {
        private final String placeholder;

        private SearchField(String placeholder) {
            this.placeholder = placeholder;
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus()) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                try {
                    g2.setColor(Theme.MUTED);
                    g2.setFont(Theme.font(Font.PLAIN, 11));
                    int y = (getHeight() - g2.getFontMetrics().getHeight()) / 2
                            + g2.getFontMetrics().getAscent();
                    g2.drawString(placeholder, 11, y);
                } finally {
                    g2.dispose();
                }
            }
        }
    }
}
