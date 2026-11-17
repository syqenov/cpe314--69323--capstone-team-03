package pawtrack.ui;

import pawtrack.controller.AuthController;
import pawtrack.model.Adopter;
import pawtrack.service.AccountPreferencesService;
import pawtrack.service.AccountSettingsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;

/**
 * Account-scoped profile, security and preference controls.
 *
 * <p>Layout and validation feedback belong here; persistence and authorization
 * continue to be handled by the injected services.</p>
 */
public final class AccountSettingsPanel extends JPanel {
    private static final Color NAVY = PawTrackTheme.NAVY;
    private static final Color BLUE = PawTrackTheme.BLUE;
    private static final Color MUTED = PawTrackTheme.MUTED;
    private static final Color CANVAS = PawTrackTheme.CANVAS;
    private static final Color LINE = PawTrackTheme.LINE;
    private static final Color INPUT_BACKGROUND = Color.WHITE;

    private final AuthController auth;
    private final AccountSettingsService accounts;
    private final AccountPreferencesService preferences;
    private final java.util.function.Consumer<Boolean> appearanceChanged;

    private final JTextField name = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextField phone = new JTextField();
    private final JTextArea address = new JTextArea(2, 24);
    private final JComboBox<String> contact = new JComboBox<>(new String[]{"Email", "Phone"});
    private final JPasswordField current = new JPasswordField();
    private final JPasswordField next = new JPasswordField();
    private final JPasswordField confirm = new JPasswordField();
    private final JComboBox<String> theme = new JComboBox<>(new String[]{"Light", "Dark"});
    private final JCheckBox notifications = new JCheckBox("Enable in-app notifications", true);
    private final JCheckBox applicationUpdates = new JCheckBox("Adoption application updates", true);
    private final JTabbedPane tabs = new JTabbedPane();

    public AccountSettingsPanel(AuthController auth, AccountSettingsService accounts) {
        this(auth, accounts, null, null);
    }

    public AccountSettingsPanel(AuthController auth, AccountSettingsService accounts,
                                AccountPreferencesService preferences) {
        this(auth, accounts, preferences, null);
    }

    public AccountSettingsPanel(AuthController auth, AccountSettingsService accounts,
                                AccountPreferencesService preferences,
                                java.util.function.Consumer<Boolean> appearanceChanged) {
        super(new BorderLayout(0, 17));
        this.auth = auth;
        this.accounts = accounts;
        this.preferences = preferences;
        this.appearanceChanged = appearanceChanged;
        setBackground(CANVAS);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel header = transparent(new BorderLayout(0, 7));
        header.add(PremiumUi.title("Account Settings", 27), BorderLayout.NORTH);
        header.add(PremiumUi.caption("Manage your profile, security, and preferences"), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        styleTabs();
        tabs.addTab("My Profile", scrollable(pageProfile()));
        tabs.addTab("Security", scrollable(pageSecurity()));
        tabs.addTab("Preferences", scrollable(pagePreferences()));
        tabs.addChangeListener(e -> {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.setForegroundAt(i, i == tabs.getSelectedIndex() ? Color.WHITE : PawTrackTheme.text());
            }
        });
        tabs.setSelectedIndex(0);
        // Initialize colors explicitly, even if the first tab never emits a change event.
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setForegroundAt(i, i == 0 ? Color.WHITE : PawTrackTheme.text());
        }
        add(tabs, BorderLayout.CENTER);
        load();
    }

    private void styleTabs() {
        tabs.setFont(PremiumUi.font(13, true));
        tabs.setBackground(CANVAS);
        tabs.setOpaque(false);
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(12, 22, 12, 22);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(0, 0, 0, 0);
            }

            @Override protected void paintTabBackground(Graphics graphics, int placement, int index,
                                                         int x, int y, int width, int height,
                                                         boolean selected) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(selected ? (PawTrackTheme.isDark() ? PawTrackTheme.DARK_SELECTED : BLUE) :
                        (PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xE1ECFC)));
                g.fillRoundRect(x + 2, y + 3, Math.max(0, width - 4),
                        Math.max(0, height - 6), 14, 14);
                g.dispose();
            }

            @Override protected void paintTabBorder(Graphics g, int placement, int index,
                                                    int x, int y, int width, int height,
                                                    boolean selected) { }
            @Override protected void paintContentBorder(Graphics g, int placement, int selectedIndex) { }
            @Override protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rectangles,
                                                          int index, Rectangle icon, Rectangle text,
                                                          boolean selected) { }
        });
    }

    private static JPanel transparent(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    private static JPanel page() {
        JPanel card = PremiumUi.card(new BorderLayout(0, 17));
        card.setBorder(new EmptyBorder(24, 30, 24, 34));
        return card;
    }

    /** Scroll the whole card, including its action button; nothing gets covered on resize. */
    private static JScrollPane scrollable(JPanel card) {
        JPanel canvas = transparent(new BorderLayout());
        canvas.setBorder(new EmptyBorder(9, 2, 14, 8));
        canvas.add(card, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(canvas,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private static JPanel sectionHeading(String title, String description) {
        JPanel p = transparent(new BorderLayout(0, 7));
        p.add(PremiumUi.title(title, 20), BorderLayout.NORTH);
        p.add(PremiumUi.caption(description), BorderLayout.SOUTH);
        return p;
    }

    private static JLabel formLabel(String text) {
        JLabel label = PremiumUi.title(text, 13);
        label.setLabelFor(null);
        return label;
    }

    private static void styleInput(JComponent component) {
        component.setFont(PremiumUi.font(13, false));
        component.setForeground(NAVY);
        component.setBackground(INPUT_BACKGROUND);
        component.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xBCCDE4), 1, true),
                new EmptyBorder(9, 12, 9, 12)));
        if (component instanceof JTextField text) {
            text.setCaretColor(BLUE);
            text.setPreferredSize(new Dimension(180, 43));
        } else if (component instanceof JComboBox<?> combo) {
            combo.setPreferredSize(new Dimension(180, 43));
            combo.setFocusable(true);
        }
    }

    private static JPanel field(String label, JComponent component) {
        JPanel field = transparent(new BorderLayout(0, 8));
        JLabel title = formLabel(label);
        title.setLabelFor(component);
        field.add(title, BorderLayout.NORTH);
        field.add(component, BorderLayout.CENTER);
        return field;
    }

    private static JPanel inputGrid() {
        return transparent(new GridBagLayout());
    }

    private static void addField(JPanel form, JPanel field, int row, int column, int span) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.gridwidth = span;
        c.weightx = span;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, column == 0 ? 0 : 10, 12, column == 0 && span == 1 ? 10 : 0);
        form.add(field, c);
    }

    private static JPanel readOnlyDetail(String label, String value) {
        JPanel detail = new JPanel(new BorderLayout(0, 7));
        detail.setBackground(new Color(0xF0F6FF));
        detail.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(LINE, 1, true), new EmptyBorder(13, 17, 14, 17)));
        JLabel heading = PremiumUi.caption(label.toUpperCase());
        heading.setFont(PremiumUi.font(11, true));
        JLabel content = PremiumUi.title(value, 14);
        detail.add(heading, BorderLayout.NORTH);
        detail.add(content, BorderLayout.CENTER);
        return detail;
    }

    private static JPanel footer(String hint, String action, Runnable handler) {
        JPanel footer = transparent(new BorderLayout(12, 0));
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));
        JLabel note = PremiumUi.caption(hint);
        footer.add(note, BorderLayout.CENTER);
        JButton button = PremiumUi.button(action, handler, true);
        button.setPreferredSize(new Dimension(174, 44));
        footer.add(button, BorderLayout.EAST);
        return footer;
    }

    private JPanel pageProfile() {
        JPanel card = page();
        JPanel content = transparent(new BorderLayout(0, 16));
        content.add(sectionHeading("My Profile",
                "Your contact details are used for your account and adoption requests."), BorderLayout.NORTH);

        JPanel contentBody = transparent(new BorderLayout(0, 16));
        JPanel accountInfo = inputGrid();
        addField(accountInfo, readOnlyDetail("Account role", auth.getCurrentUser().getRole().name()), 0, 0, 1);
        addField(accountInfo, readOnlyDetail("Username", auth.getCurrentUser().getUsername()), 0, 1, 1);

        JPanel profileForm = inputGrid();
        styleInput(name);
        styleInput(email);
        styleInput(phone);
        styleInput(contact);
        address.setLineWrap(true);
        address.setWrapStyleWord(true);
        address.setFont(PremiumUi.font(13, false));
        address.setForeground(NAVY);
        address.setBackground(Color.WHITE);
        address.setCaretColor(BLUE);
        address.setMargin(new Insets(10, 12, 10, 12));
        JScrollPane addressScroll = new JScrollPane(address,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        addressScroll.setPreferredSize(new Dimension(220, 73));
        addressScroll.setBorder(new LineBorder(new Color(0xBCCDE4), 1, true));

        addField(profileForm, field("Full Name *", name), 0, 0, 1);
        addField(profileForm, field("Email Address *", email), 0, 1, 1);
        addField(profileForm, field("Contact Number", phone), 1, 0, 1);
        addField(profileForm, field("Preferred Contact", contact), 1, 1, 1);
        addField(profileForm, field("Home Address", addressScroll), 2, 0, 2);

        JPanel formSection = transparent(new BorderLayout(0, 15));
        formSection.add(PremiumUi.title("Personal Information", 16), BorderLayout.NORTH);
        formSection.add(profileForm, BorderLayout.CENTER);
        contentBody.add(accountInfo, BorderLayout.NORTH);
        contentBody.add(formSection, BorderLayout.CENTER);
        content.add(contentBody, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        card.add(footer("* Required fields", "Save Changes", this::saveProfile), BorderLayout.SOUTH);
        return card;
    }

    private JPanel pageSecurity() {
        JPanel card = page();
        JPanel body = transparent(new BorderLayout(0, 16));
        body.add(sectionHeading("Password & Security",
                "Confirm your current password before setting a new one."), BorderLayout.NORTH);
        JPanel form = inputGrid();
        styleInput(current);
        styleInput(next);
        styleInput(confirm);
        addField(form, field("Current Password", current), 0, 0, 2);
        addField(form, field("New Password", next), 1, 0, 1);
        addField(form, field("Confirm New Password", confirm), 1, 1, 1);
        JPanel center = transparent(new BorderLayout(0, 7));
        center.add(form, BorderLayout.NORTH);
        JPanel options = transparent(new BorderLayout(0, 12));
        JCheckBox reveal = new JCheckBox("Show passwords");
        styleCheckbox(reveal);
        char echo = current.getEchoChar();
        reveal.addActionListener(e -> {
            char mask = reveal.isSelected() ? (char) 0 : echo;
            current.setEchoChar(mask);
            next.setEchoChar(mask);
            confirm.setEchoChar(mask);
        });
        options.add(reveal, BorderLayout.NORTH);
        options.add(PremiumUi.caption("Use 8 or more characters with a letter, a number, and a symbol."),
                BorderLayout.SOUTH);
        center.add(options, BorderLayout.SOUTH);
        body.add(center, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        card.add(footer("Your username and role cannot be changed here.",
                "Update Password", this::changePassword), BorderLayout.SOUTH);
        return card;
    }

    private static void styleCheckbox(JCheckBox box) {
        box.setOpaque(false);
        box.setForeground(NAVY);
        box.setFont(PremiumUi.font(13, false));
        box.setFocusPainted(false);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JPanel pagePreferences() {
        JPanel card = page();
        JPanel body = transparent(new BorderLayout(0, 16));
        body.add(sectionHeading("Preferences",
                "Customize how PawTrack looks and which updates you receive."), BorderLayout.NORTH);
        styleInput(theme);
        styleCheckbox(notifications);
        styleCheckbox(applicationUpdates);
        applicationUpdates.setText(auth.isAdopter() ?
                "Adoption application and waitlist alerts" : "New adoption request alerts");

        JPanel form = inputGrid();
        addField(form, field("Appearance", theme), 0, 0, 2);
        JPanel alerts = transparent(new GridLayout(0, 1, 0, 14));
        alerts.setBorder(BorderFactory.createCompoundBorder(new LineBorder(LINE, 1, true),
                new EmptyBorder(17, 18, 17, 18)));
        alerts.add(notifications);
        alerts.add(applicationUpdates);
        addField(form, field("Notifications", alerts), 1, 0, 2);
        body.add(form, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        card.add(footer("Your selected theme applies immediately and is remembered next time.",
                "Save Preferences", this::savePreferences), BorderLayout.SOUTH);
        return card;
    }

    private void load() {
        try {
            if (auth.isAdopter()) {
                Adopter a = accounts.myProfile();
                name.setText(a.getName());
                email.setText(a.getEmail().endsWith(".invalid") ? "" : a.getEmail());
                phone.setText(a.getPhone());
            }
            if (preferences != null) {
                Map<String, String> profile = preferences.profile();
                if (!auth.isAdopter()) {
                    name.setText(profile.getOrDefault("full_name", auth.getCurrentUser().getUsername()));
                    email.setText(profile.getOrDefault("email", ""));
                    phone.setText(profile.getOrDefault("phone", ""));
                }
                address.setText(profile.getOrDefault("address", ""));
                contact.setSelectedItem(profile.getOrDefault("preferred_contact", "Email"));
                theme.setSelectedItem(preferences.get("theme", "Light"));
                notifications.setSelected(Boolean.parseBoolean(preferences.get("notifications", "true")));
                applicationUpdates.setSelected(Boolean.parseBoolean(preferences.get("application_updates", "true")));
            }
        } catch (Exception ex) {
            message(ex.getMessage(), true);
        }
    }

    private void saveProfile() {
        try {
            if (preferences == null && !auth.isAdopter())
                throw new IllegalStateException("Profile storage is unavailable");
            if (auth.isAdopter()) accounts.updateMyContact(name.getText(), email.getText(), phone.getText());
            if (preferences != null) preferences.saveProfile(name.getText(), email.getText(),
                    phone.getText(), address.getText(), String.valueOf(contact.getSelectedItem()));
            message("Profile changes saved.", false);
        } catch (Exception e) {
            message(e.getMessage(), true);
        }
    }

    private void changePassword() {
        char[] old = current.getPassword();
        char[] newPass = next.getPassword();
        char[] check = confirm.getPassword();
        try {
            if (!Arrays.equals(newPass, check))
                throw new IllegalArgumentException("Passwords do not match");
            auth.changeMyPassword(new String(old), new String(newPass));
            current.setText("");
            next.setText("");
            confirm.setText("");
            message("Password updated.", false);
        } catch (Exception e) {
            message(e.getMessage(), true);
        } finally {
            Arrays.fill(old, '\0');
            Arrays.fill(newPass, '\0');
            Arrays.fill(check, '\0');
        }
    }

    /** Reflect changes made through the header or Tools & Settings switch. */
    public void setAppearanceSelection(boolean dark) {
        theme.setSelectedItem(dark ? "Dark" : "Light");
    }

    private void savePreferences() {
        try {
            if (preferences == null) throw new IllegalStateException("Preferences not available");
            preferences.put("theme", String.valueOf(theme.getSelectedItem()));
            preferences.put("notifications", Boolean.toString(notifications.isSelected()));
            preferences.put("application_updates", Boolean.toString(applicationUpdates.isSelected()));
            if (appearanceChanged != null) appearanceChanged.accept("Dark".equals(theme.getSelectedItem()));
            message("Preferences saved. Your theme is active now.", false);
        } catch (Exception e) {
            message(e.getMessage(), true);
        }
    }

    private void message(String value, boolean error) {
        JOptionPane.showMessageDialog(this, value,
                error ? "Settings Error" : "Account Settings",
                error ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }
}
