package pawtrack.ui;

import pawtrack.controller.AuthController;
import pawtrack.model.User;
import pawtrack.service.PasswordPolicy;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;

/**
 * Standalone "Create an Account" panel styled to match the login panel
 * (same pastel background, padding, rounded shadowed fields/buttons, logo).
 * Layout is responsive: the logo rescales when the panel/window resizes.
 */
public final class CreateAccountPanel extends JPanel {

    private static final int CORNER_RADIUS = 18;
    private static final Color PANEL_BACKGROUND = new Color(0xAFD3F2);
    private static final Color FIELD_COLOR = Color.WHITE;
    private static final Color CREATE_ACCOUNT_COLOR = new Color(0x5B8AB0);
    private static final Color CANCEL_COLOR = new Color(0x8A2B2B);
    private static final int FIELD_WIDTH = 240;
    private static final int MIN_WIDTH = 420;

    private final AuthController authController;
    private final Consumer<String> onSuccess;
    private final Runnable onCancel;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JLabel statusLabel;
    private LoginPanel.LogoLabel logoLabel;

    public CreateAccountPanel(AuthController authController, Consumer<String> onSuccess, Runnable onCancel) {
        this.authController = authController;
        this.onSuccess = onSuccess;
        this.onCancel = onCancel;
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

        Image logo = LoginPanel.loadLogo();
        if (logo != null) {
            logoLabel = new LoginPanel.LogoLabel(logo);
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

        JLabel confirmLabel = new JLabel("Confirm Password:");
        alignCenter(confirmLabel);

        confirmField = new RoundedUi.RoundedPasswordField(20, CORNER_RADIUS);
        confirmField.setBackground(FIELD_COLOR);
        confirmField.setPreferredSize(new Dimension(FIELD_WIDTH, 36));
        confirmField.setMinimumSize(new Dimension(FIELD_WIDTH, 36));
        confirmField.setMaximumSize(new Dimension(FIELD_WIDTH, 36));
        confirmField.setEchoChar('•');
        alignCenter(confirmField);

        JButton createButton =
                new RoundedUi.RoundedButton("Create Account", CORNER_RADIUS, CREATE_ACCOUNT_COLOR);
        createButton.setPreferredSize(new Dimension(180, 50));
        createButton.setMaximumSize(new Dimension(180, 50));
        createButton.addActionListener(e -> createAccount());
        alignCenter(createButton);

        JButton cancelButton =
                new RoundedUi.RoundedButton("Cancel", CORNER_RADIUS, CANCEL_COLOR);
        cancelButton.setPreferredSize(new Dimension(160, 50));
        cancelButton.setMaximumSize(new Dimension(160, 50));
        cancelButton.addActionListener(e -> {
            if (onCancel != null) {
                onCancel.run();
            }
        });
        alignCenter(cancelButton);

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
        form.add(Box.createVerticalStrut(10));
        form.add(confirmLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(RoundedUi.passwordRow(confirmField, FIELD_WIDTH));
        form.add(Box.createVerticalStrut(14));
        form.add(createButton);
        form.add(Box.createVerticalStrut(8));
        form.add(cancelButton);

        add(form, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeLogo();
            }
        });

        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> confirmField.requestFocusInWindow());
        confirmField.addActionListener(e -> createAccount());
    }

    private void resizeLogo() {
        if (logoLabel == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Image image = logoLabel.image();
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

    private void setStatus(String message, boolean error) {
        statusLabel.setForeground(error ? Color.RED : new Color(0x1F6F3A));
        statusLabel.setText(message);
    }

    private void createAccount() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (username.isEmpty()) {
            setStatus("Username is required.", true);
            return;
        }
        if (!password.equals(confirm)) {
            setStatus("Passwords do not match.", true);
            return;
        }
        try {
            User created = authController.createAccount(username, password);
            setStatus("Account created for '" + created.getUsername() + "'.", false);
            if (onSuccess != null) {
                onSuccess.accept(created.getUsername());
            }
        } catch (PasswordPolicy.WeakPasswordException weak) {
            setStatus(weak.getMessage(), true);
        } catch (IllegalArgumentException invalid) {
            setStatus(invalid.getMessage(), true);
        }
    }

    private static void alignCenter(javax.swing.JComponent component) {
        component.setAlignmentX(javax.swing.JComponent.CENTER_ALIGNMENT);
    }
}