package pawtrack.gui;

import pawtrack.security.Credentials;
import pawtrack.security.LoginManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Login panel shown at application startup.
 * Supports admin and user login with role-based access after sign-in.
 */
public final class LoginPanel extends JPanel {

    private final LoginManager loginManager = new LoginManager();
    private final LoginListener listener;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton showHideButton;
    private JButton signInButton;
    private JLabel statusLabel;

    public interface LoginListener {
        void onLoginSuccess(Credentials.Role role);
    }

    public LoginPanel(LoginListener listener) {
        this.listener = listener;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buildUi();
    }

    private void buildUi() {
        JPanel form = new JPanel();
        form.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 8));

        JLabel userLabel = new JLabel("Username or email:");
        userLabel.setLabelFor(usernameField);
        usernameField = new JTextField(20);
        usernameField.setPreferredSize(new Dimension(220, 30));

        JLabel passLabel = new JLabel("Password:");
        passLabel.setLabelFor(passwordField);
        passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new Dimension(220, 30));
        passwordField.setEchoChar('•');

        showHideButton = new JButton("👁");
        showHideButton.setFocusPainted(false);
        showHideButton.setPreferredSize(new Dimension(30, 30));
        showHideButton.addActionListener(e -> togglePasswordVisibility());

        signInButton = new JButton("Sign In");
        signInButton.setPreferredSize(new Dimension(120, 35));
        signInButton.addActionListener(e -> attemptLogin());

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);

        form.add(userLabel);
        form.add(usernameField);
        form.add(passLabel);
        form.add(passwordField);
        form.add(showHideButton);
        form.add(signInButton);

        add(form, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Allow pressing Enter to sign in
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> attemptLogin());
    }

    private void togglePasswordVisibility() {
        boolean showing = passwordField.getEchoChar() == (char) 0;
        passwordField.setEchoChar(showing ? '•' : (char) 0);
        showHideButton.setText(showing ? "👁" : "🙈");
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        try {
            Credentials.Role role = loginManager.login(username, password);
            if (role != null) {
                statusLabel.setText("Signed in as " + username + " (" + role + ")...");
                SwingUtilities.invokeLater(() -> listener.onLoginSuccess(role));
            } else {
                int remaining = loginManager.getFailedAttempts();
                statusLabel.setText("Invalid credentials. Attempt " + remaining + "/3.");
            }
        } catch (LoginManager.LockedException e) {
            statusLabel.setText("Account locked. Try again in " + e.getSeconds() + "s.");
        }
    }
}
