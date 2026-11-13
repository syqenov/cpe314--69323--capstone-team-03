package pawtrack.ui;

import pawtrack.api.PetImageServiceFactory;
import pawtrack.controller.AdoptionController;
import pawtrack.controller.AnalyticsController;
import pawtrack.controller.AnimalController;
import pawtrack.controller.AuthController;
import pawtrack.controller.ClassificationController;
import pawtrack.controller.ImportController;
import pawtrack.controller.WaitlistController;
import pawtrack.dao.AdopterDao;
import pawtrack.dao.AdoptionRecordDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.ClassificationDao;
import pawtrack.dao.Database;
import pawtrack.dao.WaitlistDao;
import pawtrack.dao.UserDao;
import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;
import pawtrack.model.StatusChange;
import pawtrack.service.AnalyticsService;
import pawtrack.service.ClassificationService;
import pawtrack.service.ShelterSeeder;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.WaitlistService;


import pawtrack.model.Role;
import pawtrack.model.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import java.awt.CardLayout;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Main application window (GUI). Connects to Main (entry point), Database, DAOs,
 * services, and workers. Contains toolbar, animal table, analytics, activity log.
 */
public final class MainFrame extends JFrame {

    private static final String ALL_CLASSIFICATIONS = "<All animals>";
    // Private helper for MainFrame GUI logic
    private static final Dimension IMAGE_SIZE = new Dimension(260, 250);
    // PawTrack's existing blue identity, refined with softer surfaces and navy accents.
    private static final Color PANEL_BLUE = new Color(0xB7D7F0);
    private static final Color TABLE_BLUE = new Color(0xF7FBFF);
    private static final Color TOOLBAR_BLUE = new Color(0xA5C9E7);
    private static final Color PRIMARY_BLUE = new Color(0x2E608C);
    private static final Color HOVER_BLUE = new Color(0x3E79A8);
    private static final Color NAVY_BLUE = new Color(0x1D3F5C);
    private static final Color BORDER_BLUE = new Color(0x8FB8DA);
    private static final Color ALT_ROW_BLUE = new Color(0xE8F2FA);
    private static final Color TAB_COLOR = new Color(0x315F86);
    private static final Color TAB_SELECTED_COLOR = PRIMARY_BLUE;
    private static final java.awt.Font BODY_FONT = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13);
    private static final java.awt.Font LABEL_FONT = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13);
    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(MainFrame.class.getName());

    private final Database database;
    private final AnimalDao animalDao;
    private final AdopterDao adopterDao;
    private final AdoptionRecordDao recordDao;
    private final ClassificationDao classificationDao;
    private final WaitlistDao waitlistDao;

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
    private final AnalyticsController analyticsController;
    private final ClassificationController classificationController;
    private final ImportController importController;

    // Private helper for MainFrame GUI logic
    private final AnimalTableModel tableModel = new AnimalTableModel(waitlist);
    // Private helper for MainFrame GUI logic
    private final JTable animalTable = new JTable(tableModel);
    // Private helper for MainFrame GUI logic
    private final JTextArea logArea = new JTextArea();
    private JTextArea myRecordsArea;
    private javax.swing.table.DefaultTableModel waitlistModel;
    private JTable waitlistTable;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private final java.util.List<RoundedUi.RoundedButton> tabButtons = new java.util.ArrayList<>();
    private final java.util.List<String> tabNames = new java.util.ArrayList<>();
    private JPanel toolbar;
    private JPanel imagePanel;
    private boolean toolbarMinimized = false;
    private javax.swing.JDialog statusPopup;
    private final User user;
    private Runnable onLogout;
    // Private helper for MainFrame GUI logic
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    // Private helper for MainFrame GUI logic
    private final JLabel statusLabel = new JLabel("Ready");
    // Private helper for MainFrame GUI logic
    private final JComboBox<String> classificationFilter = new RoundedUi.RoundedComboBox(18);
    // Private helper for MainFrame GUI logic
    private final JLabel imageLabel = new JLabel();
    // Private helper for MainFrame GUI logic
    private final JLabel imageInfoLabel = new JLabel(" ");
    // Private helper for MainFrame GUI logic
    private final AnalyticsPanel analyticsPanel = new AnalyticsPanel();

    public MainFrame(User user, Database database) throws SQLException {
        super("Pet Shelter & Adoption Tracker");
        this.user = user;
        this.database = database;
        this.animalDao = new AnimalDao(database);
        this.adopterDao = new AdopterDao(database);
        this.recordDao = new AdoptionRecordDao(database);
        this.classificationDao = new ClassificationDao(database);
        this.waitlistDao = new WaitlistDao(database);

        this.classification = ClassificationService.loadOrSeed(classificationDao);
        ShelterSeeder.seedIfEmpty(database, classification);

        AuthController authController = new AuthController(new UserDao(database));
        this.animalController = new AnimalController(animalDao);
        this.adoptionController = new AdoptionController(animalDao, adopterDao, recordDao, history, authController);
        this.waitlistController = new WaitlistController(waitlist, waitlistDao, adopterDao, animalDao, history);
        this.analyticsController = new AnalyticsController(animalDao, recordDao, analytics, waitlist, classification);
        this.classificationController = new ClassificationController(classification, animalDao);
        this.importController = new ImportController(database, classification, authController);
        this.waitlistController.loadFromDb();

        buildUi();
        populateClassificationFilter();
        refreshAnimals();
        refreshAnalytics();

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
            setLayout(new BorderLayout(8, 8));
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

        // Keep a small gap between the panel content and its scrollbar.
        JPanel toolbarHolder = new JPanel(new BorderLayout());
        toolbarHolder.setOpaque(false);
        toolbarHolder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        toolbarHolder.add(buildToolbar(), BorderLayout.CENTER);
        JScrollPane toolbarScroll = new JScrollPane(toolbarHolder,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        toolbarScroll.setBorder(null);
        toolbarScroll.getViewport().setBackground(TOOLBAR_BLUE);
        styleScrollBar(toolbarScroll.getVerticalScrollBar());
        add(toolbarScroll, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(PANEL_BLUE);
        addTabButton("Animals", buildAnimalTab());
        analyticsPanel.setBackground(PANEL_BLUE);
        analyticsPanel.setOpaque(true);
        if (user.getRole() == Role.STAFF) {
            addTabButton("Shelter Analytics", analyticsPanel);
        }
        if (user.getRole() == Role.STAFF) {
            logArea.setBackground(PANEL_BLUE);
            logArea.setOpaque(true);
            JScrollPane activityScroll = new JScrollPane(logArea);
            activityScroll.getViewport().setBackground(PANEL_BLUE);
            styleScrollBar(activityScroll.getVerticalScrollBar());
            addTabButton("Activity Log", activityScroll);
            addTabButton("Waitlist Requests", buildWaitlistTab());
            refreshWaitlist();
        }
        if (user.getRole() == Role.ADOPTER) {
            addTabButton("My Records", buildMyRecordsTab());
            refreshMyRecords();
        }
        add(contentPanel, BorderLayout.CENTER);
        add(buildTabBar(), BorderLayout.NORTH);
        if (!tabNames.isEmpty()) {
            selectTab(tabNames.get(0));
        }

        add(buildImagePanel(), BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);

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
        animalTable.setAutoCreateRowSorter(true);
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

    // Private helper for MainFrame GUI logic
    private void selectTab(String title) {
        cardLayout.show(contentPanel, title);
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).setBaseColor(tabNames.get(i).equals(title) ? TAB_SELECTED_COLOR : TAB_COLOR);
        }
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildToolbar() {
        toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setAlignmentX(Component.CENTER_ALIGNMENT);
        toolbar.setBackground(TOOLBAR_BLUE);
        toolbar.setOpaque(true);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                new RoundedUi.RoundedBorder(22, BORDER_BLUE, 1),
                BorderFactory.createEmptyBorder(10, 9, 10, 9)));

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
        toolbar.add(toggle);
        toolbar.add(Box.createVerticalStrut(10));
        toolbar.add(buildUserProfilePanel());
        toolbar.add(Box.createVerticalStrut(12));

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
        toolbar.add(button("Fetch Sample Pet Image", this::onFetchImage, "Fetch a sample image for the selected animal"));
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BLUE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_BLUE),
                BorderFactory.createEmptyBorder(6, 20, 6, 6)));
        JScrollPane scroll = new JScrollPane(animalTable);
        scroll.setBorder(BorderFactory.createLineBorder(PANEL_BLUE));
        scroll.getViewport().setBackground(TABLE_BLUE);
        styleScrollBar(scroll.getVerticalScrollBar());
        styleScrollBar(scroll.getHorizontalScrollBar());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
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
    private static final class BlueTableRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setFont(BODY_FONT);
            setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
            setForeground(NAVY_BLUE);
            setBackground(isSelected ? new Color(0xC5DFF3)
                    : (row % 2 == 0 ? Color.WHITE : ALT_ROW_BLUE));
            if (column == 0 || column >= 2) {
                setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            }
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

        JPanel previewCard = new JPanel(new BorderLayout());
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

        JLabel caption = new JLabel("SIGNED IN AS");
        caption.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 10));
        caption.setForeground(new Color(0x52738D));
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel username = new JLabel(user.getUsername());
        username.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        username.setForeground(NAVY_BLUE);
        username.setAlignmentX(Component.LEFT_ALIGNMENT);

        String roleText = user.getRole() == Role.STAFF ? "STAFF ACCOUNT" : "ADOPTER ACCOUNT";
        JLabel role = new JLabel(roleText);
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

        importController.importCsv(path,
                value -> {
                    progressBar.setValue(value);
                    progressBar.setString(value + "%");
                },
                result -> {
                    progressBar.setString("Done");
                    statusLabel.setText(result.summary());
                    log("Import finished: " + result.summary());
                    refreshAnimals();
                    refreshAnalytics();
                },
                failure -> {
                    progressBar.setString("Failed");
                    error("Import failed", failure);
                });
    }

    // Private helper for MainFrame GUI logic
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
        Adopter adopter = promptForAdopter("Record Adoption Interest");
        if (adopter == null) {
            return;
        }
        try {
            int position = waitlistController.recordInterest(animal, adopter);
            refreshAnimals();
            refreshAnalytics();
            refreshWaitlist();
            log(String.format("Interest recorded: %s is #%d in the waitlist for %s",
                    adopter.getName(), position, animal.getName()));
            statusLabel.setText("Interest recorded for " + animal.getName());
        } catch (SQLException | IllegalArgumentException failure) {
            error("Could not record interest", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void configureStatusFilter() {
        javax.swing.table.JTableHeader header = animalTable.getTableHeader();
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int column = header.columnAtPoint(e.getPoint());
                if (column == 6) {
                    showStatusFilterMenu(header, e.getX(), e.getY());
                }
            }
        });
        javax.swing.table.TableColumn statusColumn = animalTable.getColumnModel().getColumn(6);
        statusColumn.setHeaderRenderer(new StatusHeaderRenderer());
    }

    /** Header cell renderer for the Status column that shows a down-arrow icon. */
    private static final class StatusHeaderRenderer extends javax.swing.table.DefaultTableCellRenderer {
        StatusHeaderRenderer() {
            setOpaque(true);
            setBackground(PRIMARY_BLUE);
            setForeground(Color.WHITE);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value + " \u25BC");
            return this;
        }
    }

    // Private helper for MainFrame GUI logic
    private void showStatusFilterMenu(java.awt.Component invoker, int x, int y) {
        // Close any previously opened status panel first (prevents stacking).
        if (statusPopup != null) {
            statusPopup.setVisible(false);
            statusPopup.dispose();
            statusPopup = null;
        }

        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(invoker);
        javax.swing.JDialog dialog = new javax.swing.JDialog(owner, java.awt.Dialog.ModalityType.MODELESS);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        ((javax.swing.JComponent) dialog.getContentPane()).setOpaque(false);

        StatusPopupPanel panel = new StatusPopupPanel();
        final javax.swing.JDialog d = dialog;
        Runnable close = () -> {
            if (statusPopup == d) {
                statusPopup = null;
            }
        };
        panel.addOption("All", () -> applyStatusFilter(null), dialog, close);
        for (AnimalStatus status : AnimalStatus.values()) {
            panel.addOption(status.label(), () -> applyStatusFilter(status.label()), dialog, close);
        }
        dialog.getContentPane().add(panel);
        dialog.pack();
        java.awt.Point location = invoker.getLocationOnScreen();
        dialog.setLocation(location.x + x, location.y + y);

        final javax.swing.JDialog current = dialog;
        dialog.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
            }

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                current.setVisible(false);
                current.dispose();
                if (statusPopup == current) {
                    statusPopup = null;
                }
            }
        });

        statusPopup = dialog;
        dialog.setVisible(true);
    }

    /** Rounded blue drop-down panel with a soft shadow (custom-painted). */
    private static final class StatusPopupPanel extends JPanel {
        StatusPopupPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        }

        void addOption(String text, Runnable action, javax.swing.JDialog dialog, Runnable onClose) {
            JLabel label = new JLabel(text);
            label.setForeground(Color.WHITE);
            label.setOpaque(false);
            label.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            label.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    label.setForeground(new Color(0xBFD7F2));
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    label.setForeground(Color.WHITE);
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    action.run();
                    dialog.setVisible(false);
                    dialog.dispose();
                    onClose.run();
                }
            });
            add(label);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int s = 3;
            int w = getWidth() - s;
            int h = getHeight() - s;
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(s, s, w, h, 22, 22);
            g2.setColor(PRIMARY_BLUE);
            g2.fillRoundRect(0, 0, w, h, 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Private helper for MainFrame GUI logic
    private void applyStatusFilter(String label) {
        javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter =
                (javax.swing.table.TableRowSorter<javax.swing.table.TableModel>) animalTable.getRowSorter();
        if (label == null) {
            sorter.setRowFilter(null);
            statusLabel.setText("Showing all animals");
        } else {
            sorter.setRowFilter(javax.swing.RowFilter.regexFilter(java.util.regex.Pattern.quote(label), 6));
            statusLabel.setText("Filtered by status: " + label);
        }
    }

    // Private helper for MainFrame GUI logic
    private void onAddAnimal() {
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
        } catch (SQLException | IllegalArgumentException failure) {
            error("Could not add animal", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void onEditAnimal() {
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
        } catch (SQLException | IllegalArgumentException failure) {
            error("Could not edit animal", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void onDeleteAnimal() {
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
        } catch (SQLException failure) {
            error("Could not delete animal", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private Animal showAnimalForm(Animal existing, String title) {
        JTextField name = new JTextField(existing != null ? existing.getName() : "", 16);
        JComboBox<Species> species = new JComboBox<>(Species.values());
        if (existing != null) {
            species.setSelectedItem(existing.species());
        }
        JTextField breed = new JTextField(existing != null ? existing.getBreed() : "", 16);
        JTextField age = new JTextField(existing != null ? String.valueOf(existing.getAgeMonths()) : "0", 6);
        JTextField intake = new JTextField(
                existing != null ? existing.getIntakeDate().toString() : LocalDate.now().toString(), 12);
        JComboBox<AnimalStatus> status = new JComboBox<>(AnimalStatus.values());
        status.setSelectedItem(existing != null ? existing.getStatus() : AnimalStatus.INTAKE);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Name:"));
        form.add(name);
        form.add(new JLabel("Species:"));
        form.add(species);
        form.add(new JLabel("Breed:"));
        form.add(breed);
        form.add(new JLabel("Age (months):"));
        form.add(age);
        form.add(new JLabel("Intake date (yyyy-mm-dd):"));
        form.add(intake);
        form.add(new JLabel("Status:"));
        form.add(status);

        int result = JOptionPane.showConfirmDialog(this, form, title, JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        try {
            int ageMonths = Integer.parseInt(age.getText().trim());
            LocalDate intakeDate = LocalDate.parse(intake.getText().trim());
            int id = existing != null ? existing.getId() : 0;
            return Animal.create((Species) species.getSelectedItem(), id, name.getText().trim(),
                    breed.getText().trim(), ageMonths, intakeDate, (AnimalStatus) status.getSelectedItem());
        } catch (RuntimeException failure) {
            error("Invalid animal details", failure);
            return null;
        }
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildWaitlistTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BLUE);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        waitlistModel = new javax.swing.table.DefaultTableModel(
                new String[]{"Animal", "Adopter", "Email", "Requested", "Position"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        waitlistTable = new JTable(waitlistModel);
        waitlistTable.setBackground(PANEL_BLUE);
        waitlistTable.setGridColor(BORDER_BLUE);
        waitlistTable.setFillsViewportHeight(true);
        waitlistTable.getTableHeader().setBackground(PRIMARY_BLUE);
        waitlistTable.getTableHeader().setForeground(Color.WHITE);
        waitlistTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                javax.swing.JLabel c = (javax.swing.JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                c.setOpaque(true);
                c.setBackground(PRIMARY_BLUE);
                c.setForeground(Color.WHITE);
                return c;
            }
        });

        JPanel top = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(button("Refresh Waitlist", this::refreshWaitlist));
        panel.add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(waitlistTable);
        scroll.getViewport().setBackground(PANEL_BLUE);
        styleScrollBar(scroll.getVerticalScrollBar());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // Private helper for MainFrame GUI logic
    private void refreshWaitlist() {
        if (waitlistModel == null) {
            return;
        }
        waitlistModel.setRowCount(0);
        try {
            for (WaitlistController.WaitlistRequest req : waitlistController.getRequests()) {
                waitlistModel.addRow(new Object[]{
                        req.animal().getName() + " (" + req.animal().species().label() + ")",
                        req.adopter().getName(),
                        req.adopter().getEmail(),
                        req.requestedAt().toLocalDate(),
                        req.position()
                });
            }
            statusLabel.setText("Waitlist: " + waitlistModel.getRowCount() + " request(s)");
        } catch (SQLException failure) {
            error("Could not load the waitlist", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildMyRecordsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BLUE);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        myRecordsArea = new JTextArea();
        myRecordsArea.setEditable(false);
        myRecordsArea.setBackground(PANEL_BLUE);
        myRecordsArea.setForeground(Color.BLACK);
        JScrollPane scroll = new JScrollPane(myRecordsArea);
        scroll.getViewport().setBackground(PANEL_BLUE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
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
                        + "Ask staff to link your account, or record interest using your name/email.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("My Waitlist\n");
            List<WaitlistController.WaitlistRequest> waiting = waitlistController.findByAdopterId(me.getId());
            if (waiting.isEmpty()) {
                sb.append("  (none)\n");
            } else {
                for (WaitlistController.WaitlistRequest req : waiting) {
                    sb.append(String.format("  - %s  #%d  (requested %s)%n",
                            req.animal().getName() + " (" + req.animal().species().label() + ")",
                            req.position(), req.requestedAt().toLocalDate()));
                }
            }
            sb.append("\nMy Adoptions\n");
            List<AdoptionRecord> adopted = adoptionController.findAdoptionsByAdopterId(me.getId());
            if (adopted.isEmpty()) {
                sb.append("  (none)\n");
            } else {
                for (AdoptionRecord record : adopted) {
                    Animal animal = animalDao.findById(record.getAnimalId()).orElse(null);
                    String label = animal != null
                            ? animal.getName() + " (" + animal.species().label() + ")"
                            : "Animal #" + record.getAnimalId();
                    sb.append(String.format("  - %s  adopted %s ($%.2f)%n",
                            label, record.getAdoptionDate(), record.getFeePaid()));
                }
            }
            myRecordsArea.setText(sb.toString());
            myRecordsArea.setCaretPosition(0);
        } catch (SQLException failure) {
            error("Could not load your records", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private Adopter resolveMyAdopter() throws SQLException {
        if (user.getAdopterId() != null) {
            return adopterDao.findById(user.getAdopterId()).orElse(null);
        }
        return adopterDao.findByNameOrEmail(user.getUsername()).orElse(null);
    }

    // Private helper for MainFrame GUI logic
    private void onMarkAdopted() {
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
            Adopter persisted = adopterDao.findById(record.getAdopterId()).orElse(adopter);
            Optional<Adopter> nextInLine = waitlistController.pollNext(animal.getId());

            refreshAnimals();
            refreshAnalytics();
            String message = String.format("Adopted %s to %s for $%.2f", animal.getName(), persisted.getName(),
                    animal.adoptionFee());
            if (nextInLine.isPresent()) {
                message += " (next on waitlist: " + nextInLine.get().getName() + ")";
            }
            log(message);
            statusLabel.setText(message);
        } catch (SQLException | IllegalArgumentException failure) {
            error("Could not complete adoption", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void onUndo() {
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
        } catch (SQLException failure) {
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
        var service = PetImageServiceFactory.forSpecies(animal.species());
        statusLabel.setText("Fetching image from " + service.providerName() + "...");
        imageLabel.setIcon(null);
        imageLabel.setText("Loading...");
        imageInfoLabel.setText(" ");

        String breed = animal.getBreed();
        ImageWorker worker = new ImageWorker(service, breed, (int) IMAGE_SIZE.getWidth());
        worker.addPropertyChangeListener(event -> {
            if ("state".equals(event.getPropertyName())
                    && event.getNewValue() == SwingWorker.StateValue.DONE) {
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

    /** Sets up keyboard shortcuts and mnemonics for the main window. */
    private void setupKeyboardShortcuts() {
        javax.swing.InputMap im = getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = getRootPane().getActionMap();

        // Ctrl+R: Refresh animals
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK), "refreshAnimals");
        am.put("refreshAnimals", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onRefreshAnimals();
            }
        });

        // Ctrl+F: Focus classification filter
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK), "focusFilter");
        am.put("focusFilter", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                classificationFilter.requestFocusInWindow();
            }
        });

        // Ctrl+I: Import CSV
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK), "importCsv");
        am.put("importCsv", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onImportCsv();
            }
        });

        // Ctrl+U: Undo last status change
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_DOWN_MASK), "undo");
        am.put("undo", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onUndo();
            }
        });

        // F5: Refresh analytics
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "refreshAnalytics");
        am.put("refreshAnalytics", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refreshAnalytics();
            }
        });

        // Escape: Close any open dialogs/popups
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "escape");
        am.put("escape", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (statusPopup != null) {
                    statusPopup.setVisible(false);
                    statusPopup.dispose();
                    statusPopup = null;
                }
            }
        });
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

    // Private helper for MainFrame GUI logic
    private void refreshAnimals() {
        animalController.refresh();
        tableModel.setAnimals(animalController.getAnimals());
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
    }

    // Private helper for MainFrame GUI logic
    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    // Private helper for MainFrame GUI logic
    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    // Private helper for MainFrame GUI logic
    private void error(String title, Exception failure) {
        log(title + ": " + failure.getMessage());
        JOptionPane.showMessageDialog(this, title + "\n" + failure.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
