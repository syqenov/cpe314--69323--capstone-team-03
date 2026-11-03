
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import pawtrack.gui.MainFrame;
/**
 * Application entry point. Launches the Swing GUI on the Event Dispatch Thread.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the cross-platform look and feel
        }
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception failure) {
                failure.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to start the application:\n" + failure.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}