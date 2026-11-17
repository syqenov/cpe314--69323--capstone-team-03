
package pawtrack.ui;

import pawtrack.api.PetImageServiceFactory;
import pawtrack.controller.AdoptionController;
import pawtrack.controller.AnalyticsController;
import pawtrack.controller.AnimalController;
import pawtrack.controller.AuthController;
import pawtrack.controller.ClassificationController;
import pawtrack.controller.ImportController;
import pawtrack.controller.WaitlistController;
import pawtrack.dao.Database;
import pawtrack.dao.UserDao;
import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;
import pawtrack.model.StatusChange;
import pawtrack.service.AnalyticsService;
import pawtrack.service.ClassificationService;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.WaitlistService;


import pawtrack.model.Role;
import pawtrack.model.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.awt.Font;
import java.awt.FlowLayout;

/**
 * Main application window (GUI). Connects to Main (entry point), Database, DAOs,
 * services, and workers. Contains toolbar, animal table, analytics, activity log.
 */
public final class MainFrame extends JFrame {

    private static final String ALL_CLASSIFICATIONS = "<All animals>";
    // Private helper for MainFrame GUI logic
    private static final Dimension IMAGE_SIZE = new Dimension(260, 250);
    // PawTrack's existing blue identity, refined with softer surfaces and navy accents.
    private static final Color PANEL_BLUE = PawTrackTheme.CANVAS;
    private static final Color TABLE_BLUE = PawTrackTheme.SURFACE;
    private static final Color TOOLBAR_BLUE = new Color(0xA5C9E7);
    private static final Color PRIMARY_BLUE = PawTrackTheme.BLUE;
    private static final Color HOVER_BLUE = PawTrackTheme.BLUE_HOVER;
    private static final Color NAVY_BLUE = PawTrackTheme.NAVY;
    private static final Color BORDER_BLUE = PawTrackTheme.LINE;
    private static final Color ALT_ROW_BLUE = PawTrackTheme.ALT_ROW;
    private static final Color TAB_COLOR = PawTrackTheme.SIDEBAR_BUTTON;
    private static final Color TAB_SELECTED_COLOR = PawTrackTheme.BLUE;
    private static final java.awt.Font BODY_FONT = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13);
    private static final java.awt.Font LABEL_FONT = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13);
    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(MainFrame.class.getName());

    private final Database database;
    private final AuthController sessionAuth;

    private ClassificationService classification;
    // Private helper for MainFrame GUI logic
    private final WaitlistService waitlist = new WaitlistService();
    // Private helper for MainFrame GUI logic
    private final StatusHistoryService history = new StatusHistoryService();
    // Private helper for MainFrame GUI logic
    private final AnalyticsService analytics = new AnalyticsService();

    // Controllers hold the logic; the view only calls one method per click.
    private final AnimalController animalController;
    private final AdoptionController adoptionController;
    private final WaitlistController waitlistController;
    private final pawtrack.service.ReservationService reservationService;
    private final pawtrack.service.AdopterNotificationService notificationService;
    private final pawtrack.service.NotificationReadService notificationReadService;
    private final pawtrack.service.AccountSettingsService accountSettingsService;
    private final pawtrack.service.AdoptionApplicationService adoptionApplicationService;
    private final pawtrack.service.AccountPreferencesService accountPreferencesService;
    private final pawtrack.service.ApplicationReviewService applicationReviewService;
    private PawTrackHeader applicationHeader;
    private AccountSettingsPanel appearanceSettingsPanel;
    private final pawtrack.service.MyApplicationStatusService myApplicationStatusService;
    private final java.util.List<WaitlistController.WaitlistRequest> displayedWaitlist = new java.util.ArrayList<>();
    private final AnalyticsController analyticsController;
    private final ClassificationController classificationController;
    private final ImportController importController;
    private final pawtrack.service.AdoptionReceiptService adoptionReceiptService;

    // Private helper for MainFrame GUI logic
    private final AnimalTableModel tableModel = new AnimalTableModel(waitlist);
    // Private helper for MainFrame GUI logic
    private final JTable animalTable = new JTable(tableModel);
    // Private helper for MainFrame GUI logic
    private final JTextArea logArea = new JTextArea();
    private final PawTrackActivityLogPanel activityLogPanel = new PawTrackActivityLogPanel();
    private JTextArea myRecordsArea;
    private MyRecordsPanel myRecordsPanel;
    private javax.swing.table.DefaultTableModel waitlistModel;
    private JTable waitlistTable;
    private WaitlistPanel waitlistPanel;
    private CardLayout cardLayout;
    private String currentTab;
    private JPanel contentPanel;
    private PawTrackDashboard dashboardPanel;
    // UI refresh requests run in SwingWorker; Swing state is changed only from done() on the EDT.
    private boolean finishedInitializing;
    private int dashboardRefreshVersion;
    private int waitlistRefreshVersion;
    private SwingWorker<DashboardData, Void> dashboardRefreshWorker;
    private SwingWorker<List<WaitlistDisplayRow>, Void> waitlistRefreshWorker;
    private boolean dashboardRefreshPending;
    private final java.util.List<RoundedUi.RoundedButton> tabButtons = new java.util.ArrayList<>();
    private final java.util.List<String> tabNames = new java.util.ArrayList<>();
    private JPanel toolbar;
    private JPanel imagePanel;
    private JPanel samplePreviewContainer;
    private boolean toolbarMinimized = false;

    private final User user;
    private Runnable onLogout;
    // Private helper for MainFrame GUI logic
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    // Private helper for MainFrame GUI logic
    private final JLabel statusLabel = new JLabel("Ready");
    private final AnimalStatusFilter statusFilter = new AnimalStatusFilter(animalTable, statusLabel, PRIMARY_BLUE);
    // Private helper for MainFrame GUI logic
    private final JComboBox<String> classificationFilter = new RoundedUi.RoundedComboBox(18);
    // Private helper for MainFrame GUI logic
    private final JLabel imageLabel = new JLabel();
    // Private helper for MainFrame GUI logic
    private final JLabel imageInfoLabel = new JLabel(" ");
    // Private helper for MainFrame GUI logic
    private final AnalyticsPanel analyticsPanel = new AnalyticsPanel();
    // Private helper for MainFrame GUI logic
    private final RoundedUi.LoadingSpinner spinner = new RoundedUi.LoadingSpinner();
    // Private helper for MainFrame GUI logic
    private boolean rowStripingEnabled = true;
    // Private helper for MainFrame GUI logic
    private final JLabel connectionStatusLabel = new JLabel("● Connected");
    // Private helper for MainFrame GUI logic
    private boolean darkMode = false;
    private final java.util.concurrent.atomic.AtomicLong appearanceChangeId = new java.util.concurrent.atomic.AtomicLong();
    // Private helper for MainFrame GUI logic
    private JPanel statusBar;
    // Private helper for MainFrame GUI logic
    private JPanel previewCard;
    // Private helper for MainFrame GUI logic
    private JPanel userProfilePanel;
    // Private helper for MainFrame GUI logic
    private JLabel caption;
    // Private helper for MainFrame GUI logic
    private JLabel username;
    // Private helper for MainFrame GUI logic
    private JLabel role;

    /**
     * Compatibility constructor for legacy callers and tests. New application
     * entry points must pass the already authenticated AuthController.
     */
    @Deprecated
    public MainFrame(User user, Database database) throws SQLException {
        this(user, database, new AuthController(new UserDao(database)));
    }

    public MainFrame(User user, Database database, AuthController authController) throws SQLException {
        super("Pet Shelter & Adoption Tracker");
        if (authController == null) {
            throw new IllegalArgumentException("Authenticated controller is required");
        }
        if (authController.getCurrentUser() == null
                || !authController.getCurrentUser().getUsername().equals(user.getUsername())) {
            throw new IllegalArgumentException("Session user does not match window user");
        }
        this.user = user;
        this.sessionAuth = authController;
        this.database = database;
        pawtrack.service.PawTrackWiring wired = new pawtrack.service.PawTrackWiring(
                database, authController, waitlist, history, analytics);
        this.classification = wired.classification;
        this.animalController = wired.animals;
        this.adoptionController = wired.adoptions;
        this.waitlistController = wired.waitlists;
        this.reservationService = wired.reservations;
        this.notificationService = wired.notifications;
        this.notificationReadService = wired.notificationReads;
        this.accountSettingsService = wired.accountSettings;
        this.adoptionApplicationService = wired.adoptionApplications;
        this.accountPreferencesService = wired.accountPreferences;
        this.applicationReviewService = wired.applicationReviews;
        this.myApplicationStatusService = wired.myApplications;
        this.analyticsController = wired.analytics;
        this.classificationController = wired.classifications;
        this.importController = wired.imports;
        this.adoptionReceiptService = wired.receipts;

       
        animalTable.setAutoCreateRowSorter(true);

        // Restore the authenticated account's appearance; no global OS setting changes.
        try { darkMode = "Dark".equalsIgnoreCase(accountPreferencesService.get("theme", "Light")); }
        catch (SQLException e) { LOGGER.log(java.util.logging.Level.WARNING, "Could not load theme preference", e); }
        PawTrackTheme.setDark(darkMode);
        buildUi();
        PawTrackThemeManager.apply(this, darkMode);
        populateClassificationFilter();
        refreshAnimals();

        refreshAnalytics();
        refreshDashboard();
        finishedInitializing = true;

        log("Database ready");
        log(String.format("Classification tree loaded: %d nodes, depth %d",
                classification.nodeCount(), classification.depth()));
    }

    /** Sets the callback to run when the user logs out. */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    // ------------------------------------------------------------------ UI

// Private helper for MainFrame GUI logic
        private void buildUi() {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout(0, 0));
            setMinimumSize(new Dimension(1100, 680));
            java.awt.Image icon = Icons.appIcon();
            if (icon != null) {
                setIconImage(icon);
            }
            getContentPane().setBackground(PANEL_BLUE);
            javax.swing.UIManager.put("Label.font", BODY_FONT);
            javax.swing.UIManager.put("Button.font", BODY_FONT);
            javax.swing.UIManager.put("Table.font", BODY_FONT);
            javax.swing.UIManager.put("TableHeader.font", LABEL_FONT);
            javax.swing.UIManager.put("ComboBox.font", BODY_FONT);

            setupKeyboardShortcuts();
            getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T,
                            java.awt.event.InputEvent.CTRL_DOWN_MASK), "toggleWorkspaceTheme");
            getRootPane().getActionMap().put("toggleWorkspaceTheme", new javax.swing.AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggleTheme(); }
            });

        // Keep a small gap between the panel content and its scrollbar.
        JPanel toolbarHolder = new JPanel(new BorderLayout());
        toolbarHolder.setOpaque(false);
        toolbarHolder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        toolbarHolder.add(buildToolbar(), BorderLayout.CENTER);
        JScrollPane toolbarScroll = new JScrollPane(toolbarHolder,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        toolbarScroll.setBorder(null);
        toolbarScroll.getViewport().setBackground(Color.WHITE);
        styleScrollBar(toolbarScroll.getVerticalScrollBar());
        // M2A: actions are now hosted by the new sidebar.

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(PANEL_BLUE);
        dashboardPanel = new PawTrackDashboard(user.getRole() == Role.STAFF, user.getUsername(),
                () -> selectTab("Animals"), () -> selectTab("My Records"),
                () -> selectTab("Waitlist Requests"));
        dashboardPanel.setAnalyticsNavigation(() -> selectTab("Shelter Analytics"));
        addTabButton("Dashboard", dashboardPanel);
        addTabButton("Animals", buildAnimalTab());
        analyticsPanel.setBackground(PANEL_BLUE);
        analyticsPanel.setOpaque(true);
        if (user.getRole() == Role.STAFF) {
            addTabButton("Shelter Analytics", analyticsPanel);
        }
        if (user.getRole() == Role.STAFF) {
            // Dedicated staff Activity Log UI; legacy logArea remains for compatibility.
            addTabButton("Activity Log", activityLogPanel);
            addTabButton("Waitlist Requests", buildWaitlistTab());
            addTabButton("Application History", new ApplicationHistoryPanel(
                    new pawtrack.service.ApplicationHistoryService(sessionAuth, database)));
            refreshWaitlist();
        }
        if (user.getRole() == Role.ADOPTER) {
            addTabButton("My Records", buildMyRecordsTab());
            addTabButton("My Applications", new AdoptionApplicationsPanel(adoptionApplicationService, false));
            refreshMyRecords();
        }
        if (user.getRole() == Role.STAFF) {
            addTabButton("Import Records", new PawTrackImportPanel(this::onImportCsv, progressBar));
        }
        // Keep every legacy action accessible on its own workspace page instead of
        // squeezing the navigation sidebar. No controller behavior is removed.
        addTabButton("Tools & Settings", new PawTrackToolsPanel(toolbarScroll, () -> selectTab("Account Settings"), this::toggleTheme));
        appearanceSettingsPanel = new AccountSettingsPanel(sessionAuth, accountSettingsService,
                accountPreferencesService, this::changeAppearanceFromPreferences);
        addTabButton("Account Settings", appearanceSettingsPanel);
        add(contentPanel, BorderLayout.CENTER);
        add(new PawTrackSidebar(tabButtons, toolbarScroll, true, user.getRole() == Role.STAFF,
        () -> { if (onLogout != null) onLogout.run(); }), BorderLayout.WEST);
        applicationHeader = new PawTrackHeader(user.getRole().name(),
                () -> AdopterNotificationDialog.show(this, notificationReadService),
                () -> {
                    try { return notificationReadService.unreadCount(); }
                    catch (SQLException error) { return 0; }
                });
        applicationHeader.setAppearanceAction(this::toggleTheme, () -> darkMode);
        add(applicationHeader, BorderLayout.NORTH);
        if (!tabNames.isEmpty()) {
            selectTab(tabNames.get(0));
        }

        // The preview belongs to Animals, not every page. Its panel is created
        // by buildAnimalTab(), so the other pages use the full available width.
        this.statusBar = buildStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setPreferredSize(new Dimension(160, 22));
        progressBar.setMinimumSize(new Dimension(120, 22));

        analyticsPanel.setRefreshAction(this::refreshAnalytics);

        logArea.setEditable(false);
        logArea.setFont(BODY_FONT);
        animalTable.setFillsViewportHeight(true);
        animalTable.setBackground(TABLE_BLUE);
        animalTable.setForeground(NAVY_BLUE);
        animalTable.setFont(BODY_FONT);
        animalTable.setRowHeight(32);
        animalTable.setGridColor(new Color(0xD4E4F1));
        animalTable.setShowVerticalLines(false);
        animalTable.setShowHorizontalLines(true);
        animalTable.setIntercellSpacing(new Dimension(0, 1));
        animalTable.setSelectionBackground(new Color(0xC5DFF3));
        animalTable.setSelectionForeground(NAVY_BLUE);
        animalTable.setFillsViewportHeight(true);
        animalTable.getTableHeader().setOpaque(true);
        animalTable.getTableHeader().setPreferredSize(new Dimension(0, 38));
        animalTable.getTableHeader().setFont(LABEL_FONT);
        animalTable.getTableHeader().setBackground(PRIMARY_BLUE);
        animalTable.getTableHeader().setForeground(Color.WHITE);
        animalTable.setDefaultRenderer(Object.class, new BlueTableRenderer());
        animalTable.setDefaultRenderer(Number.class, new BlueTableRenderer());
        // Render status values as color-coded badges in the status column.
        animalTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        animalTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                javax.swing.JLabel cell = (javax.swing.JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                cell.setOpaque(true);
                cell.setFont(LABEL_FONT);
                cell.setBackground(PRIMARY_BLUE);
                cell.setForeground(Color.WHITE);
                cell.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                cell.setBorder(BorderFactory.createEmptyBorder(8, 7, 8, 7));
                return cell;
            }
        });

        configureStatusFilter();
        animalTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectedAnimalInfo();
            }
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                try {
                    database.close();
                } catch (SQLException failure) {
                    // nothing useful to do while shutting down
                    MainFrame.LOGGER.log(java.util.logging.Level.WARNING, "Failed to close database", failure);
                }
            }
        });

        setSize(1200, 780);
        setLocationRelativeTo(null);
    }

    // Private helper for MainFrame GUI logic
    private void addTabButton(String title, java.awt.Component content) {
        RoundedUi.RoundedButton btn = new RoundedUi.RoundedButton(title, 18, TAB_COLOR);
        btn.setPreferredSize(new Dimension(178, 44));
        btn.setMaximumSize(new Dimension(178, 44));
        btn.setFont(LABEL_FONT);
        btn.addActionListener(e -> selectTab(title));
        contentPanel.add(content, title);
        tabButtons.add(btn);
        tabNames.add(title);
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
        bar.setOpaque(false);
        for (RoundedUi.RoundedButton b : tabButtons) {
            bar.add(b);
        }
        return bar;
    }

    // Keep track of the active tab for theme switching; the navigation helper
    // owns the CardLayout, badge refresh and tab button state.
    private void selectTab(String title) {
        currentTab = title;
        PawTrackTabNavigator.select(title, cardLayout, contentPanel, tabButtons, tabNames,
                TAB_SELECTED_COLOR, TAB_COLOR,
                () -> {
                    if (applicationHeader != null) applicationHeader.refreshNotificationBadge();
                },
                this::refreshDashboardOnNavigation);
        if (darkMode) {
            for (int i = 0; i < tabButtons.size(); i++)
                tabButtons.get(i).setBaseColor(tabNames.get(i).equals(title)
                        ? PawTrackTheme.DARK_SELECTED : PawTrackTheme.DARK_HOVER);
        }
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildToolbar() {
        toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setAlignmentX(Component.CENTER_ALIGNMENT);
        toolbar.setBackground(Color.WHITE);
        toolbar.setOpaque(true);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        JButton toggle = new RoundedUi.RoundedButton("◄", 18, HOVER_BLUE);
        toggle.setPreferredSize(new Dimension(40, 40));
        toggle.setMinimumSize(new Dimension(40, 40));
        toggle.setMaximumSize(new Dimension(40, 40));
        toggle.setAlignmentX(Component.RIGHT_ALIGNMENT);
        toggle.addActionListener(event -> {
            toolbarMinimized = !toolbarMinimized;
            toggle.setText(toolbarMinimized ? "►" : "◄");
            applyToolbarMinimize();
        });
        // Sidebar collapse is controlled by PawTrackSidebar. Avoid an extra toggle
        // and the old blue user card inside Tools & Settings.
        toolbar.add(Box.createVerticalStrut(4));
        // Retain the hidden profile controls for the existing theme handler.
        userProfilePanel = buildUserProfilePanel();
        userProfilePanel.setVisible(false);
        toolbar.add(userProfilePanel);

        JButton logoutButton = new RoundedUi.RoundedButton("Logout", 18, new Color(0x8A2B2B));
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setPreferredSize(new Dimension(240, 40));
        logoutButton.setMinimumSize(new Dimension(240, 40));
        logoutButton.setMaximumSize(new Dimension(240, 40));
        logoutButton.addActionListener(event -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        toolbar.add(logoutButton);
        toolbar.add(Box.createVerticalStrut(16));

        toolbar.add(button("List / Refresh Animals", this::onRefreshAnimals, "Refresh the animal list (Ctrl+R)"));
        toolbar.add(Box.createVerticalStrut(16));
        toolbar.add(button("Filter by Classification", this::onFilterByClassification, "Focus the classification filter (Ctrl+F)"));
        toolbar.add(Box.createVerticalStrut(16));
        if (user.getRole() == Role.STAFF) {
            toolbar.add(button("Import Intake History (CSV)", this::onImportCsv, "Import animals from CSV file (Ctrl+I)"));
            toolbar.add(Box.createVerticalStrut(16));
            toolbar.add(button("Add Animal", this::onAddAnimal, "Add a new animal to the shelter"));
            toolbar.add(Box.createVerticalStrut(16));
            toolbar.add(button("Edit Animal", this::onEditAnimal, "Edit the selected animal"));
            toolbar.add(Box.createVerticalStrut(16));
            toolbar.add(button("Delete Animal", this::onDeleteAnimal, "Delete the selected animal"));
            toolbar.add(Box.createVerticalStrut(16));
        }
        toolbar.add(button("Record Interest", this::onRecordInterest, "Record adoption interest for selected animal"));
        toolbar.add(Box.createVerticalStrut(16));
        if (user.getRole() == Role.STAFF) {
            toolbar.add(button("Mark Adopted", this::onMarkAdopted, "Mark the selected animal as adopted"));
            toolbar.add(Box.createVerticalStrut(16));
            toolbar.add(button("Undo Last Status Change", this::onUndo, "Undo the last status change (Ctrl+U)"));
            toolbar.add(Box.createVerticalStrut(16));
        }

        // Quick search filter
        javax.swing.JTextField searchField = new javax.swing.JTextField();
        searchField.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchField.setPreferredSize(new Dimension(240, 36));
        searchField.setMinimumSize(new Dimension(240, 36));
        searchField.setMaximumSize(new Dimension(240, 36));
        searchField.setFont(BODY_FONT);
        searchField.setBorder(new RoundedUi.RoundedBorder(18, BORDER_BLUE, 1));
        searchField.putClientProperty("JTextField.placeholderText", "Search animals...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            private void filterTable() {
                String text = searchField.getText().trim().toLowerCase();
                javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter =
                        (javax.swing.table.TableRowSorter<javax.swing.table.TableModel>) animalTable.getRowSorter();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                }
            }
        });
        toolbar.add(searchField);
        toolbar.add(Box.createVerticalStrut(16));

        classificationFilter.setPrototypeDisplayValue("Dog > Sporting Group > Retriever");
        classificationFilter.setAlignmentX(Component.CENTER_ALIGNMENT);
        classificationFilter.setPreferredSize(new Dimension(240, 40));
        classificationFilter.setMinimumSize(new Dimension(240, 40));
        classificationFilter.setMaximumSize(new Dimension(240, 40));
        classificationFilter.setBackground(new Color(0xF7FBFF));
        classificationFilter.setForeground(NAVY_BLUE);
        classificationFilter.setFont(BODY_FONT);
        classificationFilter.setOpaque(true);
        classificationFilter.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
        classificationFilter.setBorder(new RoundedUi.RoundedBorder(18, BORDER_BLUE, 1));
        classificationFilter.setRenderer(new javax.swing.DefaultListCellRenderer() {
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                javax.swing.JLabel c = (javax.swing.JLabel) super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
                c.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                if (index < 0) {
                    // Closed (selected) state: transparent so the blue fill shows.
                    c.setOpaque(false);
                    c.setForeground(NAVY_BLUE);
                } else if (isSelected) {
                    // Hovered / selected item: vivid blue text on a light background.
                    c.setOpaque(true);
                    c.setBackground(Color.WHITE);
                    c.setForeground(PRIMARY_BLUE);
                } else {
                    c.setOpaque(true);
                    c.setBackground(Color.WHITE);
                    c.setForeground(NAVY_BLUE);
                }
                return c;
            }
        });
        classificationFilter.addActionListener(event -> {
            if (classificationFilter.getSelectedIndex() == 0) {
                refreshAnimals();
                statusLabel.setText("Showing all animals");
            } else {
                onFilterByClassification();
            }
        });
        toolbar.add(classificationFilter);

        // Loading spinner next to progress bar
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        toolbar.add(spinner);

        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        toolbar.add(progressBar);

        // Export to CSV button
        RoundedUi.RoundedButton exportButton = new RoundedUi.RoundedButton("Export CSV", 18, new Color(0x5D8AA8))
                .setTooltip("Export animals to CSV (Ctrl+E)");
        exportButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exportButton.setPreferredSize(new Dimension(120, 36));
        exportButton.setMinimumSize(new Dimension(100, 36));
        exportButton.setMaximumSize(new Dimension(140, 36));
        exportButton.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        exportButton.addActionListener(e -> exportToCsv());
        toolbar.add(exportButton);

        // Theme toggle button
        RoundedUi.RoundedButton themeButton = new RoundedUi.RoundedButton(darkMode ? "☀ Light" : "🌙 Dark", 18, new Color(0x5D8AA8))
                .setTooltip("Toggle dark/light theme (Ctrl+T)");
        themeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        themeButton.setPreferredSize(new Dimension(120, 36));
        themeButton.setMinimumSize(new Dimension(100, 36));
        themeButton.setMaximumSize(new Dimension(140, 36));
        themeButton.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        themeButton.addActionListener(e -> toggleTheme());
        toolbar.add(themeButton);

        // Column visibility toggle
        RoundedUi.RoundedButton columnsButton = new RoundedUi.RoundedButton("Columns", 18, new Color(0x5D8AA8))
                .setTooltip("Toggle column visibility");
        columnsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        columnsButton.setPreferredSize(new Dimension(120, 36));
        columnsButton.setMinimumSize(new Dimension(100, 36));
        columnsButton.setMaximumSize(new Dimension(140, 36));
        columnsButton.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        columnsButton.addActionListener(e -> showColumnVisibilityDialog());
        toolbar.add(columnsButton);

        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        toolbar.add(progressBar);
        return toolbar;
    }

    // Private helper for MainFrame GUI logic
    private void applyToolbarMinimize() {
        int fullW = 240;
        int miniW = (int) (fullW * 0.20);
        Component[] comps = toolbar.getComponents();
        for (Component c : comps) {
            if (c instanceof JButton && !((JButton) c).getText().equals("◄") && !((JButton) c).getText().equals("►")) {
                JButton b = (JButton) c;
                if (toolbarMinimized) {
                    b.setText(shorten(b.getText()));
                    b.setPreferredSize(new Dimension(miniW, 30));
                    b.setMinimumSize(new Dimension(miniW, 30));
                    b.setMaximumSize(new Dimension(miniW, 30));
                } else {
                    b.setText(restoreText(b.getText()));
                    b.setPreferredSize(new Dimension(fullW, 40));
                    b.setMinimumSize(new Dimension(fullW, 40));
                    b.setMaximumSize(new Dimension(fullW, 40));
                }
                b.revalidate();
            } else if (c instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox<?>) c;
                if (toolbarMinimized) {
                    cb.setPreferredSize(new Dimension(miniW, 30));
                    cb.setMinimumSize(new Dimension(miniW, 30));
                    cb.setMaximumSize(new Dimension(miniW, 30));
                } else {
                    cb.setPreferredSize(new Dimension(fullW, 40));
                    cb.setMinimumSize(new Dimension(fullW, 40));
                    cb.setMaximumSize(new Dimension(fullW, 40));
                }
                cb.revalidate();
            } else if (c instanceof JButton) {
                JButton tb = (JButton) c;
                if (tb.getText().equals("◄") || tb.getText().equals("►")) {
                    tb.setAlignmentX(toolbarMinimized ? Component.CENTER_ALIGNMENT : Component.RIGHT_ALIGNMENT);
                    tb.revalidate();
                }
            } else if (c instanceof JProgressBar) {
                JProgressBar pb = (JProgressBar) c;
                if (toolbarMinimized) {
                    pb.setPreferredSize(new Dimension(miniW, 20));
                    pb.setMinimumSize(new Dimension(miniW, 20));
                    pb.setMaximumSize(new Dimension(miniW, 20));
                } else {
                    pb.setPreferredSize(new Dimension(160, 22));
                    pb.setMinimumSize(new Dimension(120, 22));
                    pb.setMaximumSize(new Dimension(160, 22));
                }
                pb.revalidate();
            }
        }
        toolbar.revalidate();
        toolbar.repaint();
        // Re-layout the frame so the scroll pane / west region reflects the new width.
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private String shorten(String text) {
        if (text == null) {
            return text;
        }
        if (text.startsWith("List")) {
            return "List...";
        }
        if (text.startsWith("Filter")) {
            return "Filter...";
        }
        if (text.startsWith("Import")) {
            return "Import...";
        }
        if (text.startsWith("Record")) {
            return "Record...";
        }
        if (text.startsWith("Mark")) {
            return "Mark...";
        }
        if (text.startsWith("Undo")) {
            return "Undo...";
        }
        if (text.startsWith("Fetch")) {
            return "Fetch...";
        }
        if (text.startsWith("Add")) {
            return "Add...";
        }
        if (text.startsWith("Edit")) {
            return "Edit...";
        }
        if (text.startsWith("Delete")) {
            return "Delete...";
        }
        if (text.contains("All")) {
            return "<all...";
        }
        return text.length() > 6 ? text.substring(0, 6) + "..." : text;
    }

    private String restoreText(String text) {
        if (text.equals("List...")) {
            return "List / Refresh Animals";
        }
        if (text.equals("Filter...")) {
            return "Filter by Classification";
        }
        if (text.equals("Import...")) {
            return "Import Intake History (CSV)";
        }
        if (text.equals("Record...")) {
            return "Record Interest";
        }
        if (text.equals("Mark...")) {
            return "Mark Adopted";
        }
        if (text.equals("Undo...")) {
            return "Undo Last Status Change";
        }
        if (text.equals("Fetch...")) {
            return "Fetch Sample Pet Image";
        }
        if (text.equals("Add...")) {
            return "Add Animal";
        }
        if (text.equals("Edit...")) {
            return "Edit Animal";
        }
        if (text.equals("Delete...")) {
            return "Delete Animal";
        }
        if (text.equals("<all...")) {
            return "<All animals>";
        }
        return text;
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildAnimalTab() {
        JPanel panel = new AnimalBrowsePanel(animalTable,
                user.getRole() == Role.STAFF, this::onRefreshAnimals, this::onRecordInterest, this::onViewPetProfile);

        // Context menu for animal table
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem recordInterestItem = new JMenuItem("Record Interest");
        recordInterestItem.addActionListener(e -> onRecordInterest());
        contextMenu.add(recordInterestItem);

        if (user.getRole() == Role.STAFF) {
            JMenuItem markAdoptedItem = new JMenuItem("Mark Adopted");
            markAdoptedItem.addActionListener(e -> onMarkAdopted());
            contextMenu.add(markAdoptedItem);

            JMenuItem undoItem = new JMenuItem("Undo Last Status Change");
            undoItem.addActionListener(e -> onUndo());
            contextMenu.add(undoItem);
        }

        contextMenu.addSeparator();

        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> onRefreshAnimals());
        contextMenu.add(refreshItem);

        JCheckBoxMenuItem stripingItem = new JCheckBoxMenuItem("Alternating Row Colors", rowStripingEnabled);
        stripingItem.addActionListener(e -> {
            rowStripingEnabled = stripingItem.isSelected();
            animalTable.repaint();
        });
        contextMenu.add(stripingItem);

        contextMenu.add(buildColumnVisibilityMenu());

        animalTable.setComponentPopupMenu(contextMenu);

        JPanel workspace = new JPanel(new BorderLayout(12, 0));
        workspace.setBackground(PANEL_BLUE);
        workspace.add(panel, BorderLayout.CENTER);
        // STAFF actions belong beside the selected animal, not in Tools & Settings.
        // Reuse the existing secured MainFrame handlers (no controller changes).
        if (user.getRole() == Role.STAFF) {
            JPanel animalActions = new JPanel(new BorderLayout(12, 0));
            animalActions.setOpaque(false);
            animalActions.setBorder(BorderFactory.createEmptyBorder(10, 22, 0, 22));

            JLabel actionHeading = new JLabel("MANAGE ANIMALS");
            actionHeading.setFont(new Font("Segoe UI", Font.BOLD, 12));
            actionHeading.setForeground(NAVY_BLUE);
            animalActions.add(actionHeading, BorderLayout.WEST);

            JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
            actionButtons.setOpaque(false);
            JButton addAnimalAction = PremiumUi.button("Add Animal", this::onAddAnimal, true);
            JButton editAnimalAction = PremiumUi.button("Edit Selected", this::onEditAnimal, false);
            JButton deleteAnimalAction = PremiumUi.button("Delete Selected", this::onDeleteAnimal, false);
            addAnimalAction.setName("animalsAddButton");
            editAnimalAction.setName("animalsEditButton");
            deleteAnimalAction.setName("animalsDeleteButton");
            addAnimalAction.setToolTipText("Register a new animal");
            editAnimalAction.setToolTipText("Edit the animal selected in the table or gallery");
            deleteAnimalAction.setToolTipText("Delete the selected animal (confirmation required)");
            // Disable selection-dependent actions until an animal is selected.
            Runnable syncAnimalActions = () -> {
                boolean selected = animalTable.getSelectedRow() >= 0;
                editAnimalAction.setEnabled(selected);
                deleteAnimalAction.setEnabled(selected);
            };
            animalTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) syncAnimalActions.run();
            });
            syncAnimalActions.run();
            actionButtons.add(addAnimalAction);
            actionButtons.add(editAnimalAction);
            actionButtons.add(deleteAnimalAction);
            animalActions.add(actionButtons, BorderLayout.EAST);
            workspace.add(animalActions, BorderLayout.NORTH);
        }
        // Browse Pets and Pet Profile already show cached/uploaded/API photos.
        // Do not reserve space for the obsolete duplicate sample-image sidebar.
        return workspace;
    }

    // Private helper for MainFrame GUI logic
    private JMenu buildColumnVisibilityMenu() {
        JMenu columnMenu = new JMenu("Columns");
        for (int i = 0; i < animalTable.getColumnCount(); i++) {
            final int colIndex = i;
            final String colName = animalTable.getColumnName(i);
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(colName, true);
            item.addActionListener(e -> {
                javax.swing.table.TableColumn column = animalTable.getColumnModel().getColumn(colIndex);
                column.setMinWidth(item.isSelected() ? 50 : 0);
                column.setMaxWidth(item.isSelected() ? Integer.MAX_VALUE : 0);
                column.setPreferredWidth(item.isSelected() ? 100 : 0);
                column.setResizable(item.isSelected());
            });
            columnMenu.add(item);
        }
        return columnMenu;
    }

    // Private helper for MainFrame GUI logic
    private void rebuildColumnVisibilityMenu(JPopupMenu contextMenu) {
        // Remove existing column menu if present
        for (java.awt.Component comp : contextMenu.getComponents()) {
            if (comp instanceof JMenu && ((JMenu) comp).getText().equals("Columns")) {
                contextMenu.remove(comp);
                break;
            }
        }
        contextMenu.add(buildColumnVisibilityMenu());
    }

    // Private helper for MainFrame GUI logic
    private static void styleScrollBar(javax.swing.JScrollBar bar) {
        bar.setUI(new javax.swing.plaf.metal.MetalScrollBarUI());
        bar.setBackground(PANEL_BLUE);
        for (java.awt.Component comp : bar.getComponents()) {
            if (comp instanceof javax.swing.JButton button) {
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                button.setBackground(PANEL_BLUE);
            }
        }
    }

    /** Alternating blue-white rows and consistent readable table typography. */
    private final class BlueTableRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setFont(BODY_FONT);
            setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
            setForeground(darkMode ? PawTrackTheme.DARK_TEXT : NAVY_BLUE);
            setBackground(darkMode ? (isSelected ? PawTrackTheme.DARK_SELECTED
                    : (rowStripingEnabled && row % 2 == 0 ? PawTrackTheme.DARK_SURFACE : PawTrackTheme.DARK_INPUT))
                    : (isSelected ? new Color(0xC5DFF3)
                    : (rowStripingEnabled && row % 2 == 0 ? Color.WHITE : ALT_ROW_BLUE)));
            if (column == 0 || column >= 2) {
                setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            }
            return this;
        }
    }

    /** Cohesive, muted status palette with strong legibility and selection feedback. */
    private static final class StatusBadgeRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            String label = value == null ? "" : value.toString();
            String state = label.toUpperCase(java.util.Locale.ROOT);
            Color textColor;
            Color fillColor;
            if (state.contains("AVAILABLE")) {
                textColor = new Color(0x17665A); fillColor = new Color(0xDDF2ED);
            } else if (state.contains("ADOPTED")) {
                textColor = new Color(0x315F8B); fillColor = new Color(0xE0EDF9);
            } else if (state.contains("RESERV")) {
                textColor = new Color(0x175C79); fillColor = new Color(0xDCEFF4);
            } else if (state.contains("PEND") || state.contains("WAIT") || state.contains("INTAKE")) {
                textColor = new Color(0x97583B); fillColor = new Color(0xF9E9DF);
            } else if (state.contains("RETURN")) {
                textColor = new Color(0x53687E); fillColor = new Color(0xE9EFF5);
            } else {
                textColor = new Color(0x485F78); fillColor = new Color(0xE6EDF4);
            }
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            setFont(LABEL_FONT);
            setOpaque(true);
            setForeground(selected ? Color.WHITE : textColor);
            setBackground(selected ? new Color(0x315FAD) : fillColor);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(3, 6, 3, 6),
                    new RoundedUi.RoundedBorder(12,
                            selected ? new Color(0x244C8E) : fillColor, 1)));
            return this;
        }
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildImagePanel() {
        imagePanel = new JPanel();
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
        imagePanel.setBackground(PANEL_BLUE);
        imagePanel.setBorder(BorderFactory.createEmptyBorder(16, 12, 12, 14));

        JLabel imageTitle = new JLabel("SAMPLE PET IMAGE");
        imageTitle.setAlignmentX(CENTER_ALIGNMENT);
        imageTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        imageTitle.setForeground(NAVY_BLUE);
        imagePanel.add(imageTitle);
        JLabel imageSubtitle = new JLabel("Retrieved from Dog CEO / TheCatAPI");
        imageSubtitle.setAlignmentX(CENTER_ALIGNMENT);
        imageSubtitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        imageSubtitle.setForeground(new Color(0x456985));
        imagePanel.add(Box.createVerticalStrut(4));
        imagePanel.add(imageSubtitle);
        imagePanel.add(Box.createVerticalStrut(12));

        previewCard = new JPanel(new BorderLayout());
        previewCard.setBackground(new Color(0xF7FBFF));
        previewCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundedUi.RoundedBorder(20, BORDER_BLUE, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        previewCard.setPreferredSize(new Dimension(286, 290));
        previewCard.setMaximumSize(new Dimension(286, 290));
        imageLabel.setPreferredSize(IMAGE_SIZE);
        imageLabel.setMinimumSize(new Dimension(180, 180));
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setAlignmentX(CENTER_ALIGNMENT);
        imageLabel.setOpaque(true);
        imageLabel.setBackground(new Color(0xEAF3FA));
        imageLabel.setForeground(NAVY_BLUE);
        imageLabel.setFont(BODY_FONT);
        imageLabel.setBorder(new RoundedUi.RoundedBorder(14, new Color(0xD3E4F1), 1));
        previewCard.add(imageLabel, BorderLayout.CENTER);
        imagePanel.add(previewCard);

        imageInfoLabel.setAlignmentX(CENTER_ALIGNMENT);
        imageInfoLabel.setForeground(NAVY_BLUE);
        imageInfoLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        imageInfoLabel.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        imagePanel.add(imageInfoLabel);
        imagePanel.add(Box.createVerticalStrut(18));

        // Preserve the existing PawTrack logo and its original image source.
        java.awt.Image logo = LoginPanel.loadLogo();
        if (logo != null) {
            LoginPanel.LogoLabel logoLabel = new LoginPanel.LogoLabel(logo);
            logoLabel.setAlignmentX(CENTER_ALIGNMENT);
            int logoWidth = 180;
            int logoHeight = (int) (logoWidth * logo.getHeight(null) / (double) logo.getWidth(null));
            logoLabel.setPreferredSize(new Dimension(logoWidth, logoHeight));
            logoLabel.setMaximumSize(new Dimension(logoWidth, logoHeight));
            imagePanel.add(logoLabel);
        }
        return imagePanel;
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(PANEL_BLUE);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_BLUE),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));

        statusLabel.setForeground(NAVY_BLUE);
        statusLabel.setFont(BODY_FONT);
        statusBar.add(statusLabel, BorderLayout.WEST);

        connectionStatusLabel.setForeground(new Color(0x27AE60));
        connectionStatusLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        statusBar.add(connectionStatusLabel, BorderLayout.EAST);

        // Start connection monitoring timer
        javax.swing.Timer connectionTimer = new javax.swing.Timer(30000, e -> checkConnection());
        connectionTimer.start();
        checkConnection(); // Initial check

        return statusBar;
    }

    private void checkConnection() {
        try {
            database.connection().isValid(2);
            connectionStatusLabel.setText("● Connected");
            connectionStatusLabel.setForeground(new Color(0x27AE60));
        } catch (Exception e) {
            connectionStatusLabel.setText("● Disconnected");
            connectionStatusLabel.setForeground(new Color(0xE74C3C));
        }
    }

    /** Displays the identity and role of the account that completed login. */
    private JPanel buildUserProfilePanel() {
        JPanel profile = new JPanel();
        profile.setLayout(new BoxLayout(profile, BoxLayout.Y_AXIS));
        profile.setBackground(new Color(0xD8EAF7));
        profile.setBorder(BorderFactory.createCompoundBorder(
                new RoundedUi.RoundedBorder(16, BORDER_BLUE, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        profile.setAlignmentX(Component.CENTER_ALIGNMENT);
        profile.setMaximumSize(new Dimension(270, 76));

        caption = new JLabel("SIGNED IN AS");
        caption.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 10));
        caption.setForeground(new Color(0x52738D));
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);

        username = new JLabel(user.getUsername());
        username.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        username.setForeground(NAVY_BLUE);
        username.setAlignmentX(Component.LEFT_ALIGNMENT);

        String roleText = user.getRole() == Role.STAFF ? "STAFF ACCOUNT" : "ADOPTER ACCOUNT";
        role = new JLabel(roleText);
        role.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 10));
        role.setForeground(PRIMARY_BLUE);
        role.setAlignmentX(Component.LEFT_ALIGNMENT);

        profile.add(caption);
        profile.add(Box.createVerticalStrut(3));
        profile.add(username);
        profile.add(Box.createVerticalStrut(2));
        profile.add(role);
        return profile;
    }

    // Private helper for MainFrame GUI logic
    private JButton button(String text, Runnable action) {
        RoundedUi.RoundedButton button = new RoundedUi.RoundedButton(text, 18, PRIMARY_BLUE);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(250, 44));
        button.setMinimumSize(new Dimension(225, 44));
        button.setMaximumSize(new Dimension(270, 44));
        button.setFont(BODY_FONT);
        button.addActionListener(event -> action.run());
        return button;
    }

    /** Creates a toolbar button with a tooltip. */
    private JButton button(String text, Runnable action, String tooltip) {
        RoundedUi.RoundedButton button = new RoundedUi.RoundedButton(text, 18, PRIMARY_BLUE)
                .setTooltip(tooltip);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(250, 44));
        button.setMinimumSize(new Dimension(225, 44));
        button.setMaximumSize(new Dimension(270, 44));
        button.setFont(BODY_FONT);
        button.addActionListener(event -> action.run());
        return button;
    }

    /** All theme entry points share one reversible painter and one saved preference. */
    private void toggleTheme() { setAppearance(!darkMode, true); }

    private void changeAppearanceFromPreferences(Boolean useDark) {
        setAppearance(Boolean.TRUE.equals(useDark), false); // Already saved by settings page.
    }

    private void setAppearance(boolean useDark, boolean persist) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            javax.swing.SwingUtilities.invokeLater(() -> setAppearance(useDark, persist));
            return;
        }
        long change = appearanceChangeId.incrementAndGet();
        if (persist) {
            // The CSV worker may hold the same SQLite connection transaction lock;
            // never wait for that lock on the Swing Event Dispatch Thread.
            new javax.swing.SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    synchronized (database) {
                        if (change == appearanceChangeId.get())
                            accountPreferencesService.put("theme", useDark ? "Dark" : "Light");
                    }
                    return null;
                }
                @Override protected void done() {
                    try { get(); }
                    catch (Exception failure) {
                        java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(
                                java.util.logging.Level.WARNING, "Could not save theme preference", failure);
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Theme changed for this session, but could not be saved. Please try again.",
                                "Theme preference", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }.execute();
        }
        darkMode = useDark;
        statusFilter.closePopup();
        PawTrackThemeManager.apply(this, useDark);
        if (applicationHeader != null) applicationHeader.refreshAppearanceButton();
        if (appearanceSettingsPanel != null) appearanceSettingsPanel.setAppearanceSelection(useDark);
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).setBaseColor(tabNames.get(i).equals(currentTab)
                    ? (useDark ? PawTrackTheme.DARK_SELECTED : TAB_SELECTED_COLOR)
                    : (useDark ? PawTrackTheme.DARK_HOVER : TAB_COLOR));
        }
        animalTable.repaint();
        repaint();
    }

    // ------------------------------------------------------------- actions

    // Private helper for MainFrame GUI logic
    private void onRefreshAnimals() {
        classificationFilter.setSelectedIndex(0);
        refreshAnimals();
        statusLabel.setText("Loaded " + tableModel.getRowCount() + " animal(s)");
    }

    // Private helper for MainFrame GUI logic
    private void onFilterByClassification() {
        Object selected = classificationFilter.getSelectedItem();
        String path = selected == null ? ALL_CLASSIFICATIONS : selected.toString();
        List<Animal> matches = classificationController.filterByPath(path);
        tableModel.setAnimals(matches);
        statusLabel.setText("Filtered: " + path + " (" + matches.size() + " animal(s))");
    }

    // Private helper for MainFrame GUI logic
    private void onImportCsv() {
        if (user.getRole() != Role.STAFF) {
            warn("This action is available to shelter staff only.");
            return;
        }
        JFileChooser chooser = new JFileChooser(new File("data"));
        chooser.setDialogTitle("Select historical intake/adoption CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        Path sample = Paths.get("data", "intake_history.csv");
        if (Files.exists(sample)) {
            chooser.setSelectedFile(sample.toFile());
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final Path path = chooser.getSelectedFile().toPath();
        progressBar.setValue(0);
        progressBar.setString("Importing...");
        statusLabel.setText("Importing " + path.getFileName() + " on a background thread...");
        spinner.start();

        importController.importCsv(path,
                value -> {
                    progressBar.setValue(value);
                    progressBar.setString(value + "%");
                },
                result -> {
                    spinner.stop();
                    progressBar.setString("Done");
                    statusLabel.setText(result.summary());
                    log("Import finished: " + result.summary());
                    refreshAnimals();
                    refreshAnalytics();
                },
                failure -> {
                    spinner.stop();
                    progressBar.setString("Failed");
                    error("Import failed", failure);
                });
    }

    // Private helper for MainFrame GUI logic
    /** Show a profile using the selected animal already loaded in the table. */
    private void onViewPetProfile() {
        Animal animal = selectedAnimal();
        if (animal == null) {
            info("Select an animal from the table first.");
            return;
        }
        PetProfilePanel profile = new PetProfilePanel(animal, user.getRole() == Role.STAFF);
        if (user.getRole() == Role.ADOPTER && animal.getStatus() != AnimalStatus.ADOPTED) {
            Object[] options = {"Apply for Adoption", "Record Interest", "Close"};
            int choice = JOptionPane.showOptionDialog(this, profile, "Pet profile",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                try {
                    Adopter me = resolveMyAdopter();
                    if (me == null) { warn("Please sign in with a linked adopter account."); return; }
                    if (adoptionApplicationService.hasActive(animal.getId())) {
                        info("An active application already exists for this pet. Open My Applications to view it.");
                        selectTab("My Applications");
                        return;
                    }
                    AdoptionApplicationDialog.show(this, animal, me, adoptionApplicationService);
                } catch (Exception failure) { error("Could not open application", failure); }
            } else if (choice == 1) { onRecordInterest(); }
        } else {
            JOptionPane.showMessageDialog(this, profile, "Pet profile", JOptionPane.PLAIN_MESSAGE);
        }
    }
    private void onRecordInterest() {
        Animal animal = selectedAnimal();
        if (animal == null) {
            info("Select an animal in the table first.");
            return;
        }
        if (animal.getStatus() == AnimalStatus.ADOPTED) {
            warn(animal.getName() + " has already been adopted.");
            return;
        }
        try {
            Adopter adopter;
            if (user.getRole() == Role.ADOPTER) {
                adopter = resolveMyAdopter();
                if (adopter == null) {
                    warn("No linked adopter profile. Please sign in again.");
                    return;
                }
            } else {
                adopter = promptForAdopter("Record Adoption Interest");
                if (adopter == null) {
                    return;
                }
            }
            
            int position = waitlistController.recordInterest(animal, adopter);

            refreshAnimals();
            refreshAnalytics();

            if (user.getRole() == Role.STAFF) {
                refreshWaitlist();
            } else if (user.getRole() == Role.ADOPTER) {
                refreshMyRecords();
            }

            log(String.format("Interest recorded: %s is #%d in the waitlist for %s",
                    adopter.getName(), position, animal.getName()));

            statusLabel.setText(
                    "Interest recorded for " + animal.getName()
                    + " — Queue position #" + position
            );

        } catch (SQLException | IllegalArgumentException failure) {
            error("Could not record interest", failure);
        }
    }

    // Table status UI and filtering are owned by AnimalStatusFilter.
    private void configureStatusFilter() {
        statusFilter.install();
    }

    // Private helper for MainFrame GUI logic
    private void onAddAnimal() {
        if (!sessionAuth.isStaff()) {
            warn("This action is available to shelter staff only.");
            return;
        }
        Animal draft = showAnimalForm(null, "Add Animal");
        if (draft == null) {
            return;
        }
        try {
            animalController.addAnimal(draft);
            refreshAnimals();
            refreshAnalytics();
            log("Added animal: " + draft.getName());
            statusLabel.setText("Added " + draft.getName());
        } catch (SQLException | IllegalArgumentException | IllegalStateException failure) {
            error("Could not add animal", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void onEditAnimal() {
        if (!sessionAuth.isStaff()) {
            warn("This action is available to shelter staff only.");
            return;
        }
        Animal animal = selectedAnimal();
        if (animal == null) {
            info("Select an animal in the table first.");
            return;
        }
        Animal draft = showAnimalForm(animal, "Edit Animal");
        if (draft == null) {
            return;
        }
        try {
            animalController.updateAnimal(draft);
            refreshAnimals();
            refreshAnalytics();
            log("Edited animal: " + draft.getName());
            statusLabel.setText("Updated " + draft.getName());
        } catch (SQLException | IllegalArgumentException | IllegalStateException failure) {
            error("Could not edit animal", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void onDeleteAnimal() {
        if (!sessionAuth.isStaff()) {
            warn("This action is available to shelter staff only.");
            return;
        }
        Animal animal = selectedAnimal();
        if (animal == null) {
            info("Select an animal in the table first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete " + animal.getName() + "?",
                "Delete Animal", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            animalController.deleteAnimal(animal.getId());
            refreshAnimals();
            refreshAnalytics();
            log("Deleted animal: " + animal.getName());
            statusLabel.setText("Deleted " + animal.getName());
        } catch (SQLException | IllegalStateException failure) {
            error("Could not delete animal", failure);
        }
    }

    // Keep the existing MainFrame action routes; the dialog owns only form presentation.
    private Animal showAnimalForm(Animal existing, String title) {
        return AnimalFormDialog.showAnimalForm(this, existing, title);
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildWaitlistTab() {
        WaitlistPanel panel = new WaitlistPanel(PANEL_BLUE, PRIMARY_BLUE, BORDER_BLUE,
                button("Refresh Waitlist", this::refreshWaitlistAsync),
                button("Reserve First in Queue", this::onReserveFirst),
                button("Complete Selected Adoption", this::onCompleteReserved),
                button("Cancel Reservation", this::onCancelReservation),
                scroll -> styleScrollBar(scroll.getVerticalScrollBar()), this::onViewApplication);
        waitlistPanel = panel;
        waitlistModel = panel.getTableModel();
        waitlistTable = panel.getTable();
        javax.swing.JTabbedPane requestTabs = new javax.swing.JTabbedPane();
        requestTabs.addTab("FIFO Waitlist & Reservations", panel);
        requestTabs.addTab("Adoption Applications", new AdoptionApplicationsPanel(adoptionApplicationService, true));
        JPanel container = new JPanel(new BorderLayout());
        container.add(requestTabs, BorderLayout.CENTER);
        return container;
    }
    /** A detached row snapshot: database reads stay separate from Swing table mutations. */
    private record WaitlistDisplayRow(WaitlistController.WaitlistRequest request, Object[] values) { }

    private List<WaitlistDisplayRow> readWaitlistRows() throws SQLException {
        // Preserve a consistent snapshot while an import, reservation, or queue
        // position update owns the single SQLite connection.
        synchronized (database) {
            return readWaitlistRowsUnderDatabaseLock();
        }
    }

    private List<WaitlistDisplayRow> readWaitlistRowsUnderDatabaseLock() throws SQLException {
        List<WaitlistDisplayRow> rows = new ArrayList<>();
        for (WaitlistController.WaitlistRequest req : waitlistController.getRequests()) {
            var latest = reservationService.latestForApplicant(
                    req.animal().getId(), req.adopter().getId());
            String review = latest.isEmpty()
                    ? applicationReviewService.get(req.animal().getId(), req.adopter().getId())
                            .map(pawtrack.service.ApplicationReviewService.Review::status).orElse("WAITING")
                    : switch (latest.get().status()) {
                case "ACTIVE" -> "RESERVED";
                case "EXPIRED", "CANCELLED", "COMPLETED" -> latest.get().status();
                default -> "WAITING";
            };
            rows.add(new WaitlistDisplayRow(req, new Object[]{
                    req.animal().getName() + " (" + req.animal().species().label() + ")",
                    req.adopter().getName(),
                    req.adopter().getEmail(),
                    req.requestedAt().toLocalDate(),
                    req.position(),
                    review
            }));
        }
        return rows;
    }

    /** Must run on the Swing EDT: one atomic replacement preserves row-to-request mapping. */
    private void showWaitlistRows(List<WaitlistDisplayRow> rows) {
        if (waitlistModel == null) return;
        waitlistModel.setRowCount(0);
        displayedWaitlist.clear();
        for (WaitlistDisplayRow row : rows) {
            displayedWaitlist.add(row.request());
            waitlistModel.addRow(row.values());
            if (waitlistPanel != null) {
                waitlistPanel.registerAnimalForRow(waitlistModel.getRowCount() - 1,
                        row.request().animal());
            }
        }
        statusLabel.setText("Waitlist: " + rows.size() + " request(s)");
    }

    /** Immediate refresh retained for startup and staff actions that require current rows. */
    private void refreshWaitlist() {
        if (waitlistModel == null) return;
        ++waitlistRefreshVersion; // Invalidate any older asynchronous result.
        try {
            showWaitlistRows(readWaitlistRows());
        } catch (SQLException failure) {
            error("Could not load the waitlist", failure);
        }
    }

    /** User-triggered refresh: queries run off the EDT, Swing updates run in done(). */
    private void refreshWaitlistAsync() {
        if (waitlistModel == null) return;
        if (waitlistRefreshWorker != null && !waitlistRefreshWorker.isDone()) {
            statusLabel.setText("Waitlist refresh already in progress...");
            return;
        }
        final int version = ++waitlistRefreshVersion;
        statusLabel.setText("Refreshing waitlist...");
        waitlistRefreshWorker = new SwingWorker<>() {
            @Override protected List<WaitlistDisplayRow> doInBackground() throws Exception {
                return readWaitlistRows();
            }
            @Override protected void done() {
                if (version != waitlistRefreshVersion || !isDisplayable()) return;
                try {
                    showWaitlistRows(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Waitlist refresh interrupted");
                } catch (java.util.concurrent.ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    error("Could not load the waitlist", cause instanceof Exception
                            ? (Exception) cause : failure);
                } catch (java.util.concurrent.CancellationException cancelled) {
                    statusLabel.setText("Waitlist refresh cancelled");
                }
            }
        };
        waitlistRefreshWorker.execute();
    }

    /** Staff application details for the selected, currently loaded waitlist row. */
    private void onViewApplication() {
        if (user.getRole() != Role.STAFF) {
            warn("Only staff can review applications.");
            return;
        }
        int selected = waitlistTable == null ? -1 : waitlistTable.getSelectedRow();
        if (selected < 0) {
            info("Select an application in the Waitlist table first.");
            return;
        }
        int modelRow = waitlistTable.convertRowIndexToModel(selected);
        if (modelRow < 0 || modelRow >= displayedWaitlist.size()) {
            warn("Refresh Waitlist and select the application again.");
            return;
        }
        WaitlistController.WaitlistRequest request = displayedWaitlist.get(modelRow);
        try {
            var latest = reservationService.latestForApplicant(
                    request.animal().getId(), request.adopter().getId());
            var active = reservationService.activeForAnimal(request.animal().getId());
            ApplicationDetailsPanel details = new ApplicationDetailsPanel(request, latest);
            if (latest.isEmpty()) {
                applicationReviewService.get(request.animal().getId(), request.adopter().getId())
                        .ifPresent(review -> details.setReviewStatus(review.status()));
            }
            boolean selectedReserved = active.isPresent()
                    && active.get().adopterId() == request.adopter().getId();
            Object[] choices = selectedReserved
                    ? new Object[] { "Complete Adoption", "Cancel Reservation", "Close" }
                    : active.isPresent() || request.animal().getStatus() == AnimalStatus.ADOPTED
                        ? new Object[] { "Close" }
                        : new Object[] { "Start Review", "Reserve First in Queue", "Close" };
            int choice = PawTrackReviewDialog.show(this, details, choices);
            // Delegate all writes to existing secured actions with their own checks/confirmations.
            if (selectedReserved) {
                if (choice == 0) onCompleteReserved();
                else if (choice == 1) onCancelReservation();
            } else if (active.isEmpty() && request.animal().getStatus() != AnimalStatus.ADOPTED) {
                if (choice == 0) {
                    String note = JOptionPane.showInputDialog(this,
                            "Optional staff review note (up to 500 characters):", "");
                    if (note != null) {
                        applicationReviewService.startFirstReview(
                                request.animal().getId(), request.adopter().getId(), note);
                        refreshWaitlist();
                    }
                } else if (choice == 1) onReserveFirst();
            }
        } catch (SQLException | RuntimeException failure) {
            error("Could not open application review", failure);
        }
    }
    // Review the chosen animal's first applicant, never an arbitrary selected adopter.
    private void onReserveFirst() {
        if (user.getRole() != Role.STAFF) {
            warn("Only staff can reserve an animal.");
            return;
        }
        int selected = waitlistTable == null ? -1 : waitlistTable.getSelectedRow();
        if (selected < 0) {
            info("Select a waitlist entry first. The first applicant for that animal will be reserved.");
            return;
        }
        int modelRow = waitlistTable.convertRowIndexToModel(selected);
        if (modelRow < 0 || modelRow >= displayedWaitlist.size()) {
            warn("Reload the waitlist and select a request again.");
            return;
        }
        WaitlistController.WaitlistRequest request = displayedWaitlist.get(modelRow);
        try {
            pawtrack.dao.ReservationDao.Reservation reservation =
                    reservationService.reserveFirst(request.animal().getId());
            applicationReviewService.markApproved(request.animal().getId(), reservation.adopterId());
            refreshWaitlist();
            info("Reserved for first queued adopter #" + reservation.adopterId()
                    + " until " + reservation.expiresAt()
                    + ". This reservation expires automatically after its deadline.");
        } catch (SQLException | RuntimeException failure) {
            error("Could not reserve animal", failure);
        }
    }

    /** Staff cancellation preserves the waitlist and allows the next eligible applicant. */
    private void onCancelReservation() {
        if (user.getRole() != Role.STAFF) {
            warn("Only staff can cancel a reservation.");
            return;
        }
        int selected = waitlistTable == null ? -1 : waitlistTable.getSelectedRow();
        if (selected < 0) {
            info("Select the RESERVED application to cancel.");
            return;
        }
        int modelRow = waitlistTable.convertRowIndexToModel(selected);
        if (modelRow < 0 || modelRow >= displayedWaitlist.size()) {
            warn("Refresh the waitlist and select an application again.");
            return;
        }
        WaitlistController.WaitlistRequest request = displayedWaitlist.get(modelRow);
        try {
            var active = reservationService.activeForAnimal(request.animal().getId())
                    .orElseThrow(() -> new IllegalStateException("No active reservation for this animal"));
            if (active.adopterId() != request.adopter().getId()) {
                warn("Select the RESERVED applicant, not another applicant in the queue.");
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Cancel the reservation for " + request.adopter().getName() + "?\n"
                    + "The next eligible applicant can then be reserved. No waitlist entries will be deleted.",
                    "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
            reservationService.cancelReservation(request.animal().getId());
            refreshWaitlist();
            statusLabel.setText("Reservation cancelled for " + request.adopter().getName());
        } catch (SQLException | RuntimeException failure) {
            error("Could not cancel reservation", failure);
        }
    }

    /** Complete the selected reserved application without re-entering adopter details. */
    private void onCompleteReserved() {
        if (user.getRole() != Role.STAFF) {
            warn("Only staff can complete an adoption.");
            return;
        }
        int selected = waitlistTable == null ? -1 : waitlistTable.getSelectedRow();
        if (selected < 0) {
            info("Select a RESERVED waitlist application first.");
            return;
        }
        int modelRow = waitlistTable.convertRowIndexToModel(selected);
        if (modelRow < 0 || modelRow >= displayedWaitlist.size()) {
            warn("Refresh the waitlist and select an application again.");
            return;
        }
        WaitlistController.WaitlistRequest request = displayedWaitlist.get(modelRow);
        try {
            var reservation = reservationService.activeForAnimal(request.animal().getId())
                    .orElseThrow(() -> new IllegalStateException("This animal has no active reservation."));
            if (reservation.adopterId() != request.adopter().getId()) {
                warn("Select the applicant whose Review status is RESERVED.");
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Complete adoption for " + request.animal().getName() + "\n"
                    + "Reserved adopter: " + request.adopter().getName() + "\n"
                    + "Email: " + request.adopter().getEmail() + "\n\n"
                    + "The adoption record and animal status will be saved together.",
                    "Confirm Reserved Adoption", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            AdoptionRecord record = adoptionController.completeReserved(request.animal().getId());
            refreshAnimals();
            refreshAnalytics();
            refreshWaitlist();
            String message = "Adoption completed: " + request.animal().getName()
                    + " → " + request.adopter().getName() + " (record #" + record.getId() + ")";
            statusLabel.setText(message);
            log(message);
            info("Adoption completed successfully for " + request.adopter().getName() + ".");
        } catch (SQLException | RuntimeException failure) {
            error("Could not complete reserved adoption", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildMyRecordsTab() {
        MyRecordsPanel panel = new MyRecordsPanel(PANEL_BLUE);
        myRecordsPanel = panel;
        myRecordsArea = panel.getRecordsArea();
        JButton withdraw = PremiumUi.button("Withdraw Selected Interest", this::onWithdrawInterest, false);
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 8));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 22));
        actions.add(withdraw);
        JButton receipt = PremiumUi.button("View Adoption Receipt", this::onViewMyReceipt, true);
        actions.add(receipt);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void onViewMyReceipt() {
        try {
            Adopter me = resolveMyAdopter();
            if (me == null) throw new IllegalStateException("No linked adopter profile");
            List<AdoptionRecord> records = adoptionController.findAdoptionsByAdopterId(me.getId());
            if (records.isEmpty()) { info("No completed adoptions yet."); return; }
            Integer chosenReceiptId = myRecordsPanel == null ? null : myRecordsPanel.selectedReceiptId();
            String[] choices = records.stream().map(r -> "Receipt #" + r.getId() + " — " + r.getAdoptionDate())
                    .toArray(String[]::new);
            String selected = chosenReceiptId == null ? (String) JOptionPane.showInputDialog(this,
                    "Choose an adoption receipt:","My Receipts", JOptionPane.INFORMATION_MESSAGE,
                    null, choices, choices[0]) : null;
            if (chosenReceiptId == null && selected == null) return;
            int id = chosenReceiptId != null ? chosenReceiptId : Integer.parseInt(
                    selected.substring("Receipt #".length(), selected.indexOf(" — ")));
            String text = adoptionReceiptService.receiptFor(id);
            JTextArea display = new JTextArea(text, 12, 48);
            display.setEditable(false);
            display.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
            JOptionPane.showMessageDialog(this, new JScrollPane(display),
                    "Adoption Confirmation", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException | RuntimeException ex) { error("Could not open receipt", ex); }
    }

    /** Adopter may withdraw only their own pending application. */
    private void onWithdrawInterest() {
        if (user.getRole() != Role.ADOPTER) return;
        try {
            Adopter me = resolveMyAdopter();
            if (me == null) throw new IllegalStateException("No linked adopter account");
            List<WaitlistController.WaitlistRequest> requests = waitlistController.findByAdopterId(me.getId());
            if (requests.isEmpty()) { info("You have no pending interests to withdraw."); return; }
            String[] choices = requests.stream()
                    .map(r -> "#" + r.animal().getId() + " — " + r.animal().getName()).toArray(String[]::new);
            Integer selectedAnimalId = myRecordsPanel == null ? null : myRecordsPanel.selectedAnimalId();
            String selected = selectedAnimalId == null ? (String) JOptionPane.showInputDialog(this,
                    "Choose the application to withdraw. Active reservations require staff assistance.",
                    "Withdraw interest", JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]) : null;
            if (selectedAnimalId == null && selected == null) return;
            int id = selectedAnimalId != null ? selectedAnimalId : Integer.parseInt(selected.substring(1, selected.indexOf(" — ")));
            if (JOptionPane.showConfirmDialog(this, "Withdraw your interest in animal #" + id + "?",
                    "Confirm withdrawal", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            waitlistController.withdrawMyInterest(id);
            refreshMyRecords(); refreshAnimals();
            statusLabel.setText("Interest withdrawn");
        } catch (SQLException | RuntimeException failure) {
            error("Could not withdraw interest", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void refreshMyRecords() {
        if (myRecordsArea == null) {
            return;
        }
        try {
            Adopter me = resolveMyAdopter();
            if (me == null) {
                myRecordsArea.setText("No adopter profile is linked to this account.\n"
                        + "Please sign out and sign in again, or contact staff.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            java.util.List<MyRecordsPanel.ApplicationRow> applicationRows = new java.util.ArrayList<>();
            java.util.List<MyRecordsPanel.AdoptionRow> adoptionRows = new java.util.ArrayList<>();
            sb.append("My Waitlist\n");
            List<WaitlistController.WaitlistRequest> waiting = waitlistController.findByAdopterId(me.getId());
            if (waiting.isEmpty()) {
                sb.append("  (none)\n");
            } else {
                for (WaitlistController.WaitlistRequest req : waiting) {
                    var progress = myApplicationStatusService.statusForAnimal(req.animal().getId());
                    String visibleProgress = "WAITING".equals(progress.label())
                            ? applicationReviewService.myStage(req.animal().getId()).orElse(progress.display())
                            : progress.display();
                    applicationRows.add(new MyRecordsPanel.ApplicationRow(
                            req.animal().getId(), req.animal().getName(),
                            req.animal().species().label() + " / " + req.animal().getBreed(),
                            req.requestedAt().toLocalDate().toString(), req.position(), visibleProgress));
                    sb.append(String.format("  - %s  #%d  (requested %s)  [%s]%n",
                            req.animal().getName() + " (" + req.animal().species().label() + ")",
                            req.position(), req.requestedAt().toLocalDate(), visibleProgress));
                }
            }
            sb.append("\nMy Adoptions\n");
            List<AdoptionRecord> adopted = adoptionController.findAdoptionsByAdopterId(me.getId());
            if (adopted.isEmpty()) {
                sb.append("  (none)\n");
            } else {
                for (AdoptionRecord record : adopted) {
                    Animal animal = animalController.findById(record.getAnimalId()).orElse(null);
                    String label = animal != null
                            ? animal.getName() + " (" + animal.species().label() + ")"
                            : "Animal #" + record.getAnimalId();
                    adoptionRows.add(new MyRecordsPanel.AdoptionRow(record.getId(),
                            animal != null ? animal.getName() : "Animal #" + record.getAnimalId(),
                            animal != null ? animal.species().label() + " / " + animal.getBreed() : "Unknown breed",
                            record.getAdoptionDate().toString(), record.getFeePaid()));
                    sb.append(String.format("  - %s  adopted %s ($%.2f)%n",
                            label, record.getAdoptionDate(), record.getFeePaid()));
                }
            }
            myRecordsArea.setText(sb.toString());
            myRecordsArea.setCaretPosition(0);
            if (myRecordsPanel != null) myRecordsPanel.showRecords(applicationRows, adoptionRows);
        } catch (SQLException failure) {
            error("Could not load your records", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private Adopter resolveMyAdopter() throws SQLException {
        if (user.getAdopterId() != null) {
            return adoptionController.findMyAdopter().orElse(null);
        }
        // Never infer account ownership from a matching name or email.
        return null;
    }

    // Private helper for MainFrame GUI logic
    private void onMarkAdopted() {
        if (user.getRole() != Role.STAFF) {
            warn("This action is available to shelter staff only.");
            return;
        }
        Animal animal = selectedAnimal();
        if (animal == null) {
            info("Select an animal in the table first.");
            return;
        }
        if (animal.getStatus() == AnimalStatus.ADOPTED) {
            warn(animal.getName() + " has already been adopted.");
            return;
        }
        Adopter adopter = promptForAdopter("Complete Adoption");
        if (adopter == null) {
            return;
        }
        try {
            AdoptionRecord record = adoptionController.markAdopted(animal, adopter);
            Adopter persisted = adoptionController.findAdopterForStaff(record.getAdopterId()).orElse(adopter);
            // Adoption completion must not poll and remove another waitlist entry.
            // Application closure/reservation handling is a separate workflow.
            refreshAnimals();
            refreshAnalytics();
            String message = String.format("Adopted %s to %s for $%.2f", animal.getName(), persisted.getName(),
                    animal.adoptionFee());
            log(message);
            statusLabel.setText(message);
        } catch (SQLException | IllegalArgumentException | IllegalStateException failure) {
            error("Could not complete adoption", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void onUndo() {
        if (user.getRole() != Role.STAFF) {
            warn("This action is available to shelter staff only.");
            return;
        }
        try {
            Optional<StatusChange> undone = adoptionController.undoLastStatus();
            if (undone.isEmpty()) {
                info("There is no status change to undo.");
                return;
            }
            StatusChange change = undone.get();
            refreshAnimals();
            log("Undid status change -> " + change);
            statusLabel.setText("Reverted " + change);
        } catch (SQLException | IllegalStateException failure) {
            error("Undo failed", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void onFetchImage() {
        Animal animal = selectedAnimal();
        if (animal == null) {
            info("Select an animal to fetch a matching sample image.");
            return;
        }
        // Bring the image into view even when requested through Tools & Settings.
        selectTab("Animals");
        if (samplePreviewContainer != null && !samplePreviewContainer.isVisible()) {
            samplePreviewContainer.setVisible(true);
            samplePreviewContainer.revalidate();
        }
        var service = PetImageServiceFactory.forSpecies(animal.species());
        statusLabel.setText("Fetching image from " + service.providerName() + "...");
        imageLabel.setIcon(null);
        imageLabel.setText("Loading...");
        imageInfoLabel.setText(" ");
        spinner.start();

        String breed = animal.getBreed();
        ImageWorker worker = new ImageWorker(service, breed, (int) IMAGE_SIZE.getWidth());
        worker.addPropertyChangeListener(event -> {
            if ("state".equals(event.getPropertyName())
                    && event.getNewValue() == SwingWorker.StateValue.DONE) {
                spinner.stop();
                try {
                    ImageWorker.Result result = worker.get();
                    imageLabel.setText("");
                    imageLabel.setIcon(result.icon());
                    imageInfoLabel.setText(String.format("%s | %s", result.info().provider(),
                            result.info().breed() == null || result.info().breed().isBlank()
                                    ? "random" : result.info().breed()));
                    log("Fetched image from " + result.info().provider() + ": " + result.info().imageUrl());
                    statusLabel.setText("Image loaded for " + animal.getName());
                } catch (Exception failure) {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Image unavailable");
                    imageInfoLabel.setText(" ");
                    log("Image fetch failed (offline?): " + failure.getMessage());
                    statusLabel.setText("Image fetch failed - check your connection");
                }
            }
        });
        worker.execute();
    }

    // ------------------------------------------------------------- helpers

    /** Delegate global keyboard bindings to a focused view helper. */
    private void setupKeyboardShortcuts() {
        PawTrackKeyboardShortcuts.install(getRootPane(),
                this::onRefreshAnimals,
                () -> classificationFilter.requestFocusInWindow(),
                this::onImportCsv,
                this::onUndo,
                this::exportToCsv,
                this::refreshAnalytics,
                statusFilter::closePopup);
    }

    // ------------------------------------------------------------- helpers

    // Private helper for MainFrame GUI logic
    private Animal selectedAnimal() {
        int viewRow = animalTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return tableModel.getAnimalAt(animalTable.convertRowIndexToModel(viewRow));
    }

    // Private helper for MainFrame GUI logic
    private Adopter promptForAdopter(String title) {
        JTextField nameField = new JTextField(18);
        JTextField emailField = new JTextField(18);
        JTextField phoneField = new JTextField(18);
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Full name:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Phone (optional):"));
        panel.add(phoneField);

        int result = JOptionPane.showConfirmDialog(this, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        try {
            return new Adopter(nameField.getText(), emailField.getText(), phoneField.getText());
        } catch (IllegalArgumentException invalid) {
            warn(invalid.getMessage());
            return null;
        }
    }

    /** Immutable data used to populate the dashboard once a background read completes. */
    private record DashboardData(List<Animal> animals, int requests,
                                 AnalyticsController.AnalyticsSnapshot summary,
                                 List<AdoptionRecord> monthlyAdoptions,
                                 int adopterWaiting, int adopterAdopted) { }

    /** Queries only; never reads or writes Swing widgets. */
    private DashboardData readDashboardData(List<Animal> animals, int requests) {
        // Background dashboard refreshes must not read the shared connection
        // partway through a transaction that holds the same database lock.
        synchronized (database) {
            return readDashboardDataUnderDatabaseLock(animals, requests);
        }
    }

    private DashboardData readDashboardDataUnderDatabaseLock(List<Animal> animals, int requests) {
        if (user.getRole() == Role.STAFF) {
            AnalyticsController.AnalyticsSnapshot summary = analyticsController.compute();
            List<AdoptionRecord> monthly = null;
            try {
                monthly = adoptionController.allAdoptionsForStaff();
            } catch (SQLException failure) {
                LOGGER.log(java.util.logging.Level.WARNING,
                        "Could not load adoption trend", failure);
            }
            return new DashboardData(animals, requests, summary, monthly, 0, 0);
        }
        try {
            Adopter me = resolveMyAdopter();
            int waiting = me == null ? 0 : waitlistController.findByAdopterId(me.getId()).size();
            int adopted = me == null ? 0 : adoptionController.findAdoptionsByAdopterId(me.getId()).size();
            return new DashboardData(animals, requests, null, null, waiting, adopted);
        } catch (SQLException failure) {
            LOGGER.log(java.util.logging.Level.WARNING,
                    "Could not refresh adopter overview", failure);
            return new DashboardData(animals, requests, null, null, 0, 0);
        }
    }

    /** Swing-only rendering. Keeps existing KPI formulas and role boundaries unchanged. */
    private void showDashboardData(DashboardData data) {
        if (dashboardPanel == null) return;
        List<Animal> animals = data.animals();
        int total = animals.size();
        dashboardPanel.setAnimalSnapshot(animals);
        if (user.getRole() == Role.STAFF) {
            int available = (int) animals.stream()
                    .filter(a -> a.getStatus() == AnimalStatus.AVAILABLE).count();
            dashboardPanel.setStats(total, available, data.requests());
            AnalyticsController.AnalyticsSnapshot summary = data.summary();
            if (summary != null && summary.report() != null) {
                dashboardPanel.setStaffKpis(summary.report().totalAdoptions(),
                        summary.report().averageStayDaysOverall(), data.requests());
            } else {
                dashboardPanel.setStaffKpis((int) animals.stream()
                        .filter(a -> a.getStatus() == AnimalStatus.ADOPTED).count(),
                        Double.NaN, data.requests());
            }
            if (data.monthlyAdoptions() != null) {
                dashboardPanel.setMonthlyAdoptions(data.monthlyAdoptions());
            }
        } else {
            dashboardPanel.setStats(total, data.adopterWaiting(), data.adopterAdopted());
        }
    }

    /** Startup path: still synchronous, so the initial dashboard is populated immediately. */
    private void refreshDashboard() {
        if (dashboardPanel == null) return;
        List<Animal> animals = List.copyOf(animalController.getAnimals());
        int requests = waitlistModel == null ? 0 : waitlistModel.getRowCount();
        showDashboardData(readDashboardData(animals, requests));
    }

    /** Dashboard tab navigation no longer runs analytics SQL on the Swing EDT. */
    private void refreshDashboardOnNavigation() {
        // The constructor invokes selectTab() before the initial synchronous refresh.
        if (!finishedInitializing || dashboardPanel == null) return;
        if (dashboardRefreshWorker != null && !dashboardRefreshWorker.isDone()) {
            dashboardRefreshPending = true;
            return;
        }
        final int version = ++dashboardRefreshVersion;
        final List<Animal> animals = List.copyOf(animalController.getAnimals());
        final int requests = waitlistModel == null ? 0 : waitlistModel.getRowCount();
        dashboardRefreshWorker = new SwingWorker<>() {
            @Override protected DashboardData doInBackground() {
                return readDashboardData(animals, requests);
            }
            @Override protected void done() {
                try {
                    if (version == dashboardRefreshVersion && isDisplayable()) {
                        showDashboardData(get());
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    LOGGER.log(java.util.logging.Level.WARNING,
                            "Dashboard refresh interrupted", interrupted);
                } catch (java.util.concurrent.ExecutionException failure) {
                    LOGGER.log(java.util.logging.Level.WARNING,
                            "Dashboard refresh failed", failure.getCause());
                } catch (java.util.concurrent.CancellationException cancelled) {
                    LOGGER.fine("Dashboard refresh cancelled");
                } finally {
                    if (dashboardRefreshPending && isDisplayable()) {
                        dashboardRefreshPending = false;
                        refreshDashboardOnNavigation();
                    }
                }
            }
        };
        dashboardRefreshWorker.execute();
    }

    // Private helper for MainFrame GUI logic
    private void refreshAnimals() {
        animalController.refresh();
        tableModel.setAnimals(animalController.getAnimals());
    }

    // Private helper for MainFrame GUI logic
    public User getUser() {
        return user;
    }

    // Private helper for MainFrame GUI logic
    private void refreshAnalytics() {
        AnalyticsController.AnalyticsSnapshot snapshot = analyticsController.compute();
        analyticsPanel.showReport(snapshot.report(), snapshot.classCounts(), snapshot.totalWaiters());
    }

    // Private helper for MainFrame GUI logic
    private void populateClassificationFilter() {
        classificationFilter.addItem(ALL_CLASSIFICATIONS);
        for (String path : classificationController.allPaths()) {
            classificationFilter.addItem(path);
        }
    }

    // Private helper for MainFrame GUI logic
    private void updateSelectedAnimalInfo() {
        Animal animal = selectedAnimal();
        if (animal == null) {
            statusLabel.setText("Ready");
            return;
        }
        statusLabel.setText(String.format("Selected: %s | fee $%.2f | care: %s",
                animal, animal.adoptionFee(), animal.careSchedule()));
    }

    // Private helper for MainFrame GUI logic
    private void log(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
        activityLogPanel.appendEvent(message);
    }

    // Private helper for MainFrame GUI logic
    private void info(String message) {
        PawTrackMessageDialogs.info(this, message);
    }

    // Private helper for MainFrame GUI logic
    private void warn(String message) {
        PawTrackMessageDialogs.warn(this, message);
    }

    // Private helper for MainFrame GUI logic
    private void error(String title, Exception failure) {
        log(title + ": " + failure.getMessage());
        PawTrackMessageDialogs.error(this, title, failure);
    }

    // Private helper for MainFrame GUI logic
    private void exportToCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Animals to CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
        chooser.setSelectedFile(new File("animals_export_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }
        try {
            int exported = pawtrack.service.AnimalCsvExportService.export(
                    file.toPath(), animalController.getAnimals());
            statusLabel.setText("Exported " + exported + " animals to " + file.getName());
            info("Successfully exported " + exported + " animals to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            error("Export failed", ex);
        }
    }

    // Delegate column visibility presentation to a dedicated view.
    private void showColumnVisibilityDialog() {
        AnimalColumnVisibilityDialog.show(this, animalTable);
    }

}
