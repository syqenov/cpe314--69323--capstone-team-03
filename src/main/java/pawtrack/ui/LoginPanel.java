package pawtrack.ui;

import pawtrack.controller.AuthController;
import pawtrack.model.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.InputStream;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * Login panel shown at application startup.
 * Supports STAFF and ADOPTER login with role-based access after sign-in,
 * and lets new users create an ADOPTER account.
 */
public final class LoginPanel extends JPanel {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCKOUT_SECONDS = 60;
    private static final int FIELD_WIDTH = 240;
    private static final int CORNER_RADIUS = 18;
    private static final Color PANEL_BACKGROUND = new Color(0xAFD3F2);
    private static final Color FIELD_COLOR = Color.WHITE;
    private static final Color SIGN_IN_COLOR = new Color(0x2B5B8A);
    private static final Color CREATE_ACCOUNT_COLOR = new Color(0x5B8AB0);
    private static final int MIN_WIDTH = 420;

    private final AuthController authController;
    private final LoginListener listener;

    private int failedAttempts = 0;
    private long lockoutUntilMillis = 0;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton signInButton;
    private JLabel statusLabel;
    private LogoLabel logoLabel;

    public interface LoginListener {
        void onLoginSuccess(User user);
    }

    public LoginPanel(AuthController authController, LoginListener listener) {
        this.authController = authController;
        this.listener = listener;
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        setBackground(PANEL_BACKGROUND);
        setOpaque(true);
        setMinimumSize(new Dimension(MIN_WIDTH, 0));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        Image logo = loadLogo();
        if (logo != null) {
            logoLabel = new LogoLabel(logo);
            alignCenter(logoLabel);
            form.add(logoLabel);
            form.add(Box.createVerticalStrut(12));
        }

        JLabel userLabel = new JLabel("Username:");
        alignCenter(userLabel);

        usernameField = new RoundedUi.RoundedTextField(20, CORNER_RADIUS);
        usernameField.setBackground(FIELD_COLOR);
        usernameField.setPreferredSize(new Dimension(FIELD_WIDTH, 36));
        usernameField.setMinimumSize(new Dimension(FIELD_WIDTH, 36));
        usernameField.setMaximumSize(new Dimension(FIELD_WIDTH, 36));
        alignCenter(usernameField);

        JLabel passLabel = new JLabel("Password:");
        alignCenter(passLabel);

        passwordField = new RoundedUi.RoundedPasswordField(20, CORNER_RADIUS);
        passwordField.setBackground(FIELD_COLOR);
        passwordField.setPreferredSize(new Dimension(FIELD_WIDTH, 36));
        passwordField.setMinimumSize(new Dimension(FIELD_WIDTH, 36));
        passwordField.setMaximumSize(new Dimension(FIELD_WIDTH, 36));
        passwordField.setEchoChar('•');
        alignCenter(passwordField);

        signInButton = new RoundedUi.RoundedButton("Sign In", CORNER_RADIUS, SIGN_IN_COLOR);
        signInButton.setPreferredSize(new Dimension(160, 50));
        signInButton.setMaximumSize(new Dimension(160, 50));
        signInButton.addActionListener(e -> attemptLogin());
        alignCenter(signInButton);

        JButton createAccountButton =
                new RoundedUi.RoundedButton("Create an Account", CORNER_RADIUS, CREATE_ACCOUNT_COLOR);
        createAccountButton.setPreferredSize(new Dimension(180, 50));
        createAccountButton.setMaximumSize(new Dimension(180, 50));
        createAccountButton.addActionListener(e -> openCreateAccount());
        alignCenter(createAccountButton);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        alignCenter(statusLabel);

        form.add(userLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(10));
        form.add(passLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(RoundedUi.passwordRow(passwordField, FIELD_WIDTH));
        form.add(Box.createVerticalStrut(14));
        form.add(signInButton);
        form.add(Box.createVerticalStrut(8));
        form.add(createAccountButton);

        add(form, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeLogo();
            }
        });

        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> attemptLogin());
    }

    private void resizeLogo() {
        if (logoLabel == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Image image = logoLabel.image;
        int maxWidth = (int) (getWidth() * 0.82);
        int maxHeight = (int) (getHeight() * 0.48);
        double scale = Math.min(maxWidth / (double) image.getWidth(null),
                maxHeight / (double) image.getHeight(null));
        int w = Math.max(1, (int) (image.getWidth(null) * scale));
        int h = Math.max(1, (int) (image.getHeight(null) * scale));
        logoLabel.setPreferredSize(new Dimension(w, h));
        logoLabel.setMaximumSize(new Dimension(w, h));
        logoLabel.revalidate();
    }

    static Image loadLogo() {
        try (InputStream in = LoginPanel.class.getResourceAsStream("/PawTrack.png")) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {
            // logo is optional; fall back to no logo
        }
        return null;
    }

    /** Label that draws the logo scaled to fit its bounds, preserving aspect ratio. */
    static final class LogoLabel extends JLabel {
        private final Image image;

        LogoLabel(Image image) {
            this.image = image;
            setOpaque(false);
        }

        Image image() {
            return image;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (image == null) {
                super.paintComponent(g);
                return;
            }
            int w = getWidth();
            int h = getHeight();
            double scale = Math.min(w / (double) image.getWidth(null), h / (double) image.getHeight(null));
            int dw = (int) (image.getWidth(null) * scale);
            int dh = (int) (image.getHeight(null) * scale);
            int x = (w - dw) / 2;
            int y = (h - dh) / 2;
            g.drawImage(image, x, y, dw, dh, null);
        }
    }

    private static void alignCenter(javax.swing.JComponent component) {
        component.setAlignmentX(javax.swing.JComponent.CENTER_ALIGNMENT);
    }

    private void openCreateAccount() {
        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        javax.swing.JDialog dialog = new javax.swing.JDialog(owner,
                "Create an Account", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        CreateAccountPanel panel = new CreateAccountPanel(authController, username -> {
            setStatus("Account created for '" + username + "'. Please sign in.", false);
            usernameField.setText(username);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
            dialog.dispose();
        }, dialog::dispose);

        dialog.setContentPane(panel);
        dialog.setSize(460, 600);
        dialog.setMinimumSize(new Dimension(420, 540));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setForeground(error ? Color.RED : new Color(0x1F6F3A));
        statusLabel.setText(message);
    }

    private void attemptLogin() {
        if (isLocked()) {
            setStatus("Account locked. Try again in " + remainingLockoutSeconds() + "s.", true);
            return;
        }
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            setStatus("Please enter both username and password.", true);
            return;
        }

        Optional<User> user = authController.login(username, password);
        if (user.isPresent()) {
            failedAttempts = 0;
            lockoutUntilMillis = 0;
            setStatus("Signed in as " + username + " (" + user.get().getRole() + ")...", false);
            SwingUtilities.invokeLater(() -> listener.onLoginSuccess(user.get()));
        } else {
            failedAttempts++;
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                lockoutUntilMillis = System.currentTimeMillis() + LOCKOUT_SECONDS * 1000;
            }
            setStatus("Invalid credentials. Attempt " + failedAttempts + "/3.", true);
        }
    }

    private boolean isLocked() {
        if (lockoutUntilMillis == 0) {
            return false;
        }
        if (System.currentTimeMillis() > lockoutUntilMillis) {
            lockoutUntilMillis = 0;
            failedAttempts = 0;
            return false;
        }
        return true;
    }

    private long remainingLockoutSeconds() {
        if (lockoutUntilMillis == 0) {
            return 0;
        }
        long remaining = (lockoutUntilMillis - System.currentTimeMillis()) / 1000 + 1;
        return Math.max(0, remaining);
    }
}