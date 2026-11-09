package pawtrack;

import pawtrack.gui.LoginPanel;
import pawtrack.gui.MainFrame;
import pawtrack.security.Credentials.Role;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point. Shows the login panel first, then launches the
 * Swing GUI with role-based access on the Event Dispatch Thread.
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
        SwingUtilities.invokeLater(Main::showLogin);
    }

    private static void showLogin() {
        JFrame loginFrame = new JFrame("Pet Shelter - Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(420, 320);
        loginFrame.setLocationRelativeTo(null);

        LoginPanel loginPanel = new LoginPanel(role -> {
            loginFrame.dispose();
            try {
                MainFrame frame = new MainFrame(role);
                frame.setOnLogout(() -> {
                    frame.dispose();
                    showLogin();
                });
                frame.setVisible(true);
            } catch (Exception failure) {
                failure.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to start the application:\n" + failure.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        loginFrame.add(loginPanel);
        loginFrame.setVisible(true);
    }
}
