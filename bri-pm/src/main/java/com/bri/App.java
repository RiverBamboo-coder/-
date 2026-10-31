package com.bri;

import com.bri.ui.DataService;
import com.bri.ui.MainFrame;
import com.bri.ui.common.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Swing entry point.
 *
 * <p>Normal run: {@code java -jar target/bri-pm.jar}</p>
 * <p>Offline demo: {@code java -jar target/bri-pm.jar --demo}</p>
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) {
        boolean demo = args != null && args.length > 0 && "--demo".equalsIgnoreCase(args[0]);
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null,
                        "界面异常：" + throwable.getMessage(),
                        "运行提示",
                        JOptionPane.ERROR_MESSAGE
                )));
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Custom components provide the visual identity.
            }
            Theme.install();
            MainFrame frame = new MainFrame(new DataService(demo));
            frame.setVisible(true);
        });
    }
}
