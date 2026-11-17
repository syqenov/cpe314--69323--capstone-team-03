package pawtrack.ui;

import pawtrack.controller.AuthController;
import pawtrack.model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * PawTrack's actual animated login screen. Illustrations are supplied by the
 * user's approved GIF, while all inputs are native, accessible Swing controls.
 * Authentication is delegated to the existing AuthController.
 */
public final class LoginPanel extends JPanel {
    private static final Color NAVY = new Color(0x172F58);
    private static final Color TEXT = new Color(0x142D55);
    private static final Color MUTED = new Color(0x58729F);
    private static final Color OUTLINE = new Color(0xBDD2FA);
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCKOUT_SECONDS = 60;

    private final AuthController authController;
    private final LoginListener listener;
    private final Preferences preferences = Preferences.userNodeForPackage(LoginPanel.class);
    private int failedAttempts;
    private long lockoutUntilMillis;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton signInButton;
    private JLabel statusLabel;
    private JCheckBox remember;
    private final AnimatedBackdrop backdrop = new AnimatedBackdrop();
    private final boolean formOnly;

    public interface LoginListener { void onLoginSuccess(User user); }

    public LoginPanel(AuthController authController, LoginListener listener) {
        this(authController, listener, false);
    }

    /** Standalone login form for the one-window video/login screen. */
    public LoginPanel(AuthController authController, LoginListener listener, boolean formOnly) {
        this.formOnly = formOnly;
        this.authController = Objects.requireNonNull(authController);
        this.listener = Objects.requireNonNull(listener);
        buildUi();
    }

    private void buildUi() {
        if (formOnly) {
            setLayout(new BorderLayout());
            setMinimumSize(new Dimension(390, 530));
            JPanel lightSurface = new JPanel(new GridBagLayout());
            lightSurface.setOpaque(false);
            lightSurface.add(buildForm());
            add(lightSurface, BorderLayout.CENTER);
            return;
        }
        setLayout(new BorderLayout());
        setBackground(new Color(0xC8DDFF));
        setMinimumSize(new Dimension(800, 545));
        backdrop.setLayout(new BorderLayout());
        add(backdrop, BorderLayout.CENTER);

        // This is a real app header, not a painted copy of the GIF's controls.
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(13, 23, 4, 15));
        header.setPreferredSize(new Dimension(0, 57));
        JLabel brand = new JLabel("PawTrack", new PawIcon(25, NAVY), SwingConstants.LEFT);
        brand.setFont(font(20, true));
        brand.setForeground(NAVY);
        brand.setIconTextGap(9);
        header.add(brand, BorderLayout.WEST);
        backdrop.add(header, BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 0, 0));
        columns.setOpaque(false);
        JPanel illustrationSpace = new JPanel();
        illustrationSpace.setOpaque(false);
        columns.add(illustrationSpace);
        columns.add(buildForm());
        backdrop.add(columns, BorderLayout.CENTER);

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
                backdrop.setAnimating(isShowing());
        });
    }

    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 24, 14, 31));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(400, 476));
        form.setMaximumSize(new Dimension(435, 510));

        JLabel title = new JLabel("Welcome Back!");
        title.setFont(font(36, true));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(title);
        form.add(Box.createVerticalStrut(5));
        JLabel subtitle = new JLabel("Sign in to continue to PawTrack");
        subtitle.setFont(font(16, false));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(subtitle);
        form.add(Box.createVerticalStrut(29));

        String savedName = preferences.get("savedUsername", "");
        usernameField = new PromptTextField("Username or Email");
        usernameField.setText(savedName);
        usernameField.setFont(font(16, false));
        usernameField.setForeground(TEXT);
        form.add(new InputShell(usernameField, false));
        form.add(Box.createVerticalStrut(13));

        passwordField = new PromptPasswordField("Password");
        passwordField.setFont(font(16, false));
        passwordField.setForeground(TEXT);
        passwordField.setEchoChar('\u2022');
        InputShell passShell = new InputShell(passwordField, true);
        JButton eye = new JButton(new EyeIcon(false));
        eye.setToolTipText("Show password");
        eye.setBorderPainted(false);
        eye.setFocusPainted(false);
        eye.setContentAreaFilled(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.setPreferredSize(new Dimension(34, 32));
        eye.addActionListener(e -> {
            boolean showing = passwordField.getEchoChar() != 0;
            passwordField.setEchoChar(showing ? (char) 0 : '\u2022');
            eye.setIcon(new EyeIcon(showing));
            eye.setToolTipText(showing ? "Hide password" : "Show password");
        });
        passShell.addAccessory(eye);
        form.add(passShell);
        form.add(Box.createVerticalStrut(15));

        JPanel choices = new JPanel(new BorderLayout());
        choices.setOpaque(false);
        choices.setAlignmentX(Component.LEFT_ALIGNMENT);
        choices.setPreferredSize(new Dimension(400, 30));
        choices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        remember = new JCheckBox("Remember me");
        remember.setFont(font(13, false));
        remember.setForeground(NAVY);
        remember.setOpaque(false);
        remember.setFocusPainted(false);
        remember.setSelected(!savedName.isBlank());
        choices.add(remember, BorderLayout.WEST);
        JButton forgot = new JButton("Forgot password?");
        forgot.setFont(font(13, false));
        forgot.setForeground(new Color(0x205FC4));
        forgot.setBorder(null);
        forgot.setOpaque(false);
        forgot.setContentAreaFilled(false);
        forgot.setFocusPainted(false);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Password recovery is not available in this offline version. Contact shelter staff for account assistance.",
            "Account assistance", JOptionPane.INFORMATION_MESSAGE));
        choices.add(forgot, BorderLayout.EAST);
        form.add(choices);
        form.add(Box.createVerticalStrut(21));

        signInButton = new PillButton("Login", true);
        signInButton.addActionListener(e -> attemptLogin());
        form.add(signInButton);
        form.add(Box.createVerticalStrut(17));
        form.add(new Divider());
        form.add(Box.createVerticalStrut(17));
        JButton create = new PillButton("Create an Account", false);
        create.addActionListener(e -> openCreateAccount());
        form.add(create);
        form.add(Box.createVerticalStrut(10));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(font(12, false));
        statusLabel.setForeground(new Color(0xAD3B49));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(statusLabel);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        wrapper.add(form, constraints);

        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> attemptLogin());
        return wrapper;
    }

    private static Font font(int size, boolean bold) {
        return new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size);
    }

    private void attemptLogin() {
        if (System.currentTimeMillis() < lockoutUntilMillis) {
            status("Too many attempts. Please try again shortly.", true);
            return;
        }
        String username = usernameField.getText().trim();
        char[] secret = passwordField.getPassword();
        try {
            if (username.isEmpty() || secret.length == 0) {
                status("Enter your username and password.", true);
                return;
            }
            Optional<User> user = authController.login(username, new String(secret));
            if (user.isPresent()) {
                failedAttempts = 0;
                lockoutUntilMillis = 0;
                if (remember.isSelected()) preferences.put("savedUsername", username);
                else preferences.remove("savedUsername");
                status("Signed in successfully.", false);
                SwingUtilities.invokeLater(() -> listener.onLoginSuccess(user.get()));
            } else {
                failedAttempts++;
                if (failedAttempts >= MAX_FAILED_ATTEMPTS)
                    lockoutUntilMillis = System.currentTimeMillis() + LOCKOUT_SECONDS * 1000;
                status("Invalid credentials (" + failedAttempts + "/3).", true);
            }
        } finally {
            Arrays.fill(secret, '\0');
            passwordField.setText("");
        }
    }

    private void status(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setForeground(error ? new Color(0xB43A47) : new Color(0x157B52));
    }

    private void openCreateAccount() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Create an Account", Dialog.ModalityType.APPLICATION_MODAL);
        CreateAccountPanel panel = new CreateAccountPanel(authController, username -> {
            status("Account created. Please sign in.", false);
            usernameField.setText(username);
            passwordField.setText("");
            dialog.dispose();
        }, dialog::dispose);
        dialog.setContentPane(panel);
        dialog.setSize(480, 620);
        dialog.setMinimumSize(new Dimension(420, 540));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    // Retained for MainFrame / existing UI code compatibility.
    static Image loadLogo() {
        try (InputStream in = LoginPanel.class.getResourceAsStream("/PawTrack.png")) {
            return in == null ? null : ImageIO.read(in);
        } catch (Exception ex) { return null; }
    }

    static final class LogoLabel extends JLabel {
        private final Image image;
        LogoLabel(Image image) { this.image = image; setOpaque(false); }
        Image image() { return image; }
        @Override protected void paintComponent(Graphics graphics) {
            if (image == null) { super.paintComponent(graphics); return; }
            int iw = image.getWidth(null), ih = image.getHeight(null);
            if (iw <= 0 || ih <= 0) return;
            double scale = Math.min(getWidth()/(double) iw, getHeight()/(double) ih);
            int w = (int) (iw * scale), h = (int) (ih * scale);
            graphics.drawImage(image, (getWidth()-w)/2, (getHeight()-h)/2, w, h, null);
        }
    }

    private static class PromptTextField extends JTextField {
        private final String hint;
        PromptTextField(String hint) { this.hint = hint; setOpaque(false); setBorder(null); setCaretColor(NAVY); }
        @Override protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);
            if (getText().isEmpty()) drawHint(gr, hint);
        }
        protected void drawHint(Graphics gr, String hint) {
            Graphics2D g = (Graphics2D) gr.create();
            g.setFont(getFont());
            g.setColor(new Color(0x7489AF));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(hint, 0, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    private static final class PromptPasswordField extends JPasswordField {
        private final String hint;
        PromptPasswordField(String hint) { this.hint=hint; setOpaque(false); setBorder(null); setCaretColor(NAVY); }
        @Override protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);
            if (getPassword().length == 0) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setFont(getFont());
                g.setColor(new Color(0x7489AF));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(hint, 0, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g.dispose();
            }
        }
    }

    private static final class InputShell extends JPanel {
        InputShell(JTextField field, boolean password) {
            super(new BorderLayout(9, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(6, 20, 6, 13));
            setPreferredSize(new Dimension(400, 54));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            add(new JLabel(password ? new LockIcon() : new UserIcon()), BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
        }
        void addAccessory(JComponent component) { add(component, BorderLayout.EAST); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xF6F9FF));
            g.fillRoundRect(1, 1, getWidth()-3, getHeight()-3, 52, 52);
            g.setStroke(new BasicStroke(1.3f));
            g.setColor(OUTLINE);
            g.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 52, 52);
            g.dispose();
        }
    }

    private static final class Divider extends JComponent {
        Divider() {
            setPreferredSize(new Dimension(400, 21));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 21));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            int cy=getHeight()/2;
            g.setColor(new Color(0xABC4EC));
            g.setStroke(new BasicStroke(1.1f));
            g.drawLine(0,cy,getWidth()/2-25,cy);
            g.drawLine(getWidth()/2+25,cy,getWidth(),cy);
            g.setColor(TEXT);
            g.setFont(font(14,false));
            FontMetrics fm=g.getFontMetrics();
            g.drawString("or",(getWidth()-fm.stringWidth("or"))/2,cy+(fm.getAscent()-fm.getDescent())/2);
            g.dispose();
        }
    }

    private static final class PillButton extends JButton {
        private final boolean primary;
        PillButton(String label, boolean primary) {
            super(label);
            this.primary = primary;
            setFont(font(16, true));
            setForeground(primary ? Color.WHITE : NAVY);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setBorder(new EmptyBorder(8, 18, 8, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(400, 56));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            g.setColor(new Color(0x8D,0xA4,0xC7,55));
            g.fillRoundRect(2, 4, w-4, h-4, h, h);
            if (primary) {
                g.setPaint(new GradientPaint(0, 0, new Color(0x2F4C7E), 0, h, new Color(0x172F58)));
                g.fillRoundRect(1, 1, w-3, h-5, h, h);
            } else {
                g.setColor(new Color(0xFAFCFF));
                g.fillRoundRect(1,1,w-3,h-5,h,h);
                g.setColor(new Color(0x4379C9));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(1,1,w-3,h-5,h,h);
            }
            g.dispose();
            super.paintComponent(graphics);
            Graphics2D paw=(Graphics2D)graphics.create();
            paw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = paw.getFontMetrics(getFont());
            int iconX = (getWidth()-fm.stringWidth(getText()))/2 - (primary ? 39 : 36);
            int cy = (getHeight()-4)/2;
            if(primary) PawIcon.paintPaw(paw,iconX,cy-11,22,Color.WHITE);
            else UserIcon.paintUser(paw,iconX,cy-10,new Color(0x3B71C4));
            paw.dispose();
        }
    }

    private static final class UserIcon implements Icon {
        @Override public int getIconWidth(){return 23;}
        @Override public int getIconHeight(){return 26;}
        @Override public void paintIcon(Component c, Graphics g,int x,int y){
            Graphics2D p=(Graphics2D)g.create();paintUser(p,x,y+2,NAVY);p.dispose();
        }
        static void paintUser(Graphics2D g,int x,int y,Color color){
            g.setColor(color);
            g.fillOval(x+7,y,10,10);
            g.fillRoundRect(x+3,y+12,18,11,7,7);
        }
    }

    private static final class LockIcon implements Icon {
        @Override public int getIconWidth(){return 23;}
        @Override public int getIconHeight(){return 26;}
        @Override public void paintIcon(Component c,Graphics graphics,int x,int y){
            Graphics2D g=(Graphics2D)graphics.create();
            g.setColor(NAVY);g.setStroke(new BasicStroke(2.4f));
            g.drawArc(x+5,y+1,13,17,0,180);
            g.fillRoundRect(x+4,y+11,16,14,4,4);
            g.setColor(Color.WHITE);g.fillOval(x+11,y+16,3,3);
            g.dispose();
        }
    }

    private static final class EyeIcon implements Icon {
        private final boolean visible;
        EyeIcon(boolean visible){this.visible=visible;}
        @Override public int getIconWidth(){return 25;}
        @Override public int getIconHeight(){return 22;}
        @Override public void paintIcon(Component c,Graphics gr,int x,int y){
            Graphics2D g=(Graphics2D)gr.create();
            g.setColor(new Color(0x5779A9));g.setStroke(new BasicStroke(1.8f));
            Path2D eye=new Path2D.Double();
            eye.moveTo(x+2,y+11);eye.curveTo(x+8,y+3,x+17,y+3,x+23,y+11);
            eye.curveTo(x+17,y+19,x+8,y+19,x+2,y+11);
            g.draw(eye);g.fillOval(x+10,y+8,5,5);
            if(!visible)g.drawLine(x+4,y+20,x+21,y+2);
            g.dispose();
        }
    }

    private static final class PawIcon implements Icon {
        private final int size;
        private final Color color;
        PawIcon(int size,Color color){this.size=size;this.color=color;}
        @Override public int getIconWidth(){return size;}
        @Override public int getIconHeight(){return size;}
        @Override public void paintIcon(Component c, Graphics gr,int x,int y){
            Graphics2D g=(Graphics2D)gr.create();
            paintPaw(g,x,y,size,color);
            g.dispose();
        }
        static void paintPaw(Graphics2D g,int x,int y,int size,Color color){
            Object old=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            double scale=size/24.0;
            Graphics2D p=(Graphics2D)g.create();p.translate(x,y);p.scale(scale,scale);
            p.fillOval(2,3,5,7);p.fillOval(8,0,5,7);
            p.fillOval(15,1,5,7);p.fillOval(20,7,4,6);
            p.fillRoundRect(4,11,17,12,10,10);p.dispose();
            if(old != null)g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,old);
        }
    }

    private static final class AnimatedBackdrop extends JPanel {
        private final BufferedImage[] frames=new BufferedImage[6];
        private final Timer timer;
        private int index;
        AnimatedBackdrop(){
            setOpaque(false);
            for(int i=0;i<frames.length;i++){
                String name=String.format("/pawtrack/login-frames/login-pets-%02d.png",i);
                try(InputStream in=getClass().getResourceAsStream(name)){
                    if(in!=null)frames[i]=ImageIO.read(in);
                }catch(Exception ignored){}
            }
            timer=new Timer(180,e->{index=(index+1)%frames.length;repaint();});
            timer.setCoalesce(true);
        }
        void setAnimating(boolean enabled){if(enabled)timer.start();else timer.stop();}
        @Override protected void paintComponent(Graphics gr){
            Graphics2D g=(Graphics2D)gr.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();
            g.setPaint(new GradientPaint(0,0,new Color(0xBFD6FE),w,h,new Color(0xE4EDFF)));
            g.fillRect(0,0,w,h);
            BufferedImage pic=frames[index];
            if(pic!=null){
                Shape clip=g.getClip();
                int artW=(int)(w*.525);
                g.clipRect(0,57,artW,h-57);
                // The supplied GIF was 490x526. Only its illustrated LEFT HALF
                // is used; all login controls on the right are native Swing.
                g.drawImage(pic,0,57,artW,h-57,null);
                g.setClip(clip);
            }
            Path2D curve=new Path2D.Double();
            curve.moveTo(w*.595,0);
            curve.curveTo(w*.43,h*.18,w*.49,h*.38,w*.535,h*.55);
            curve.curveTo(w*.59,h*.76,w*.47,h*.88,w*.60,h);
            curve.lineTo(w,h);curve.lineTo(w,0);curve.closePath();
            g.setPaint(new GradientPaint(w*.53f,0,new Color(0xFAFCFF),w,h,new Color(0xE6EEFF)));
            g.fill(curve);
            g.setColor(new Color(0xC6D8F7));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(curve);
            // Decorative paw is painted, not a Unicode glyph, so it works on Windows.
            PawIcon.paintPaw(g,w-78,87,44,new Color(0x9D,0xBE,0xEE,150));
            g.dispose();
        }
    }
}
