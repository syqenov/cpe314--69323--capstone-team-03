package pawtrack.gui;

import pawtrack.api.PetImageServiceFactory;
import pawtrack.db.AdopterDao;
import pawtrack.db.AdoptionRecordDao;
import pawtrack.db.AnimalDao;
import pawtrack.db.ClassificationDao;
import pawtrack.db.Database;
import pawtrack.db.WaitlistDao;
import pawtrack.model.Adopter;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.ClassificationNode;
import pawtrack.model.StatusChange;
import pawtrack.service.AnalyticsReport;
import pawtrack.service.AnalyticsService;
import pawtrack.service.ClassificationService;
import pawtrack.service.CsvImportResult;
import pawtrack.service.CsvImportService;
import pawtrack.service.ShelterSeeder;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.WaitlistService;

import pawtrack.security.Credentials;
import pawtrack.security.Credentials.Role;

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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Main application window (GUI). Connects to Main (entry point), Database, DAOs,
 * services, and workers. Contains toolbar, animal table, analytics, activity log.
 */
public final class MainFrame extends JFrame {

    private static final String ALL_CLASSIFICATIONS = "<All animals>";
    // Private helper for MainFrame GUI logic
    private static final Dimension IMAGE_SIZE = new Dimension(360, 360);

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

    // Private helper for MainFrame GUI logic
    private final AnimalTableModel tableModel = new AnimalTableModel(waitlist);
    // Private helper for MainFrame GUI logic
    private final JTable animalTable = new JTable(tableModel);
    // Private helper for MainFrame GUI logic
    private final JTextArea logArea = new JTextArea();
    private JTabbedPane tabs;
    private JPanel toolbar;
    private JPanel imagePanel;
    private boolean toolbarMinimized = false;
    private final Role role;
    private Runnable onLogout;
    // Private helper for MainFrame GUI logic
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    // Private helper for MainFrame GUI logic
    private final JLabel statusLabel = new JLabel("Ready");
    // Private helper for MainFrame GUI logic
    private final JComboBox<String> classificationFilter = new JComboBox<>();
    // Private helper for MainFrame GUI logic
    private final JLabel imageLabel = new JLabel();
    // Private helper for MainFrame GUI logic
    private final JLabel imageInfoLabel = new JLabel(" ");
    // Private helper for MainFrame GUI logic
    private final AnalyticsPanel analyticsPanel = new AnalyticsPanel();

    public MainFrame(Role role) throws SQLException {
        super("Pet Shelter & Adoption Tracker");
        this.role = role;
        String dbPath = System.getProperty("petshelter.db", "petshelter.db");
        this.database = Database.openFile(dbPath);
        this.animalDao = new AnimalDao(database);
        this.adopterDao = new AdopterDao(database);
        this.recordDao = new AdoptionRecordDao(database);
        this.classificationDao = new ClassificationDao(database);
        this.waitlistDao = new WaitlistDao(database);

        this.classification = ClassificationService.loadOrSeed(classificationDao);
        ShelterSeeder.seedIfEmpty(database, classification);
        loadWaitlists();

        buildUi();
        populateClassificationFilter();
        refreshAnimals();
        refreshAnalytics();

        log("Database ready: " + dbPath);
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
        setLayout(new BorderLayout(6, 6));
        setMinimumSize(new Dimension(900, 600));

        add(buildToolbar(), BorderLayout.WEST);

        tabs = new JTabbedPane();
        tabs.addTab("Animals", buildAnimalTab());
        if (role == Role.ADMIN) {
            tabs.addTab("Shelter Analytics", analyticsPanel);
        }
        if (role == Role.ADMIN) {
            JScrollPane activityScroll = new JScrollPane(logArea);
            tabs.addTab("Activity Log", activityScroll);
        }
        add(tabs, BorderLayout.CENTER);

        add(buildImagePanel(), BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);

        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setPreferredSize(new Dimension(160, 22));
        progressBar.setMinimumSize(new Dimension(120, 22));

        analyticsPanel.setRefreshAction(this::refreshAnalytics);

        logArea.setEditable(false);
        animalTable.setFillsViewportHeight(true);
        animalTable.setAutoCreateRowSorter(true);
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
                } catch (SQLException ignored) {
                    // nothing useful to do while shutting down
                }
            }
        });

        setSize(1200, 780);
        setLocationRelativeTo(null);
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildToolbar() {
        toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setAlignmentX(Component.CENTER_ALIGNMENT);
        toolbar.setBorder(BorderFactory.createTitledBorder("Controls"));

        JButton toggle = new JButton("◄");
        toggle.setFocusPainted(false);
        toggle.setPreferredSize(new Dimension(30, 30));
        toggle.setMinimumSize(new Dimension(30, 30));
        toggle.setMaximumSize(new Dimension(30, 30));
        toggle.setBorder(new RoundedBorder(15, new Color(0xCCCCCC), 1));
        toggle.setAlignmentX(Component.RIGHT_ALIGNMENT);
        toggle.addActionListener(event -> {
            toolbarMinimized = !toolbarMinimized;
            toggle.setText(toolbarMinimized ? "►" : "◄");
            applyToolbarMinimize();
        });
        toolbar.add(toggle);
        toolbar.add(Box.createVerticalStrut(8));

        JButton logoutButton = new JButton("Logout");
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setPreferredSize(new Dimension(240, 40));
        logoutButton.setMinimumSize(new Dimension(240, 40));
        logoutButton.setMaximumSize(new Dimension(240, 40));
        logoutButton.setBorder(new RoundedBorder(25, new Color(0xCCCCCC), 1));
        logoutButton.setBackground(new Color(0x8A2B2B));
        logoutButton.setForeground(Color.BLACK);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(event -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        toolbar.add(logoutButton);
        toolbar.add(Box.createVerticalStrut(16));

        toolbar.add(button("List / Refresh Animals", this::onRefreshAnimals));
        toolbar.add(Box.createVerticalStrut(16));
        toolbar.add(button("Filter by Classification", this::onFilterByClassification));
        toolbar.add(Box.createVerticalStrut(16));
        if (role == Role.ADMIN) {
            toolbar.add(button("Import Intake History (CSV)", this::onImportCsv));
            toolbar.add(Box.createVerticalStrut(16));
        }
        toolbar.add(button("Record Interest", this::onRecordInterest));
        toolbar.add(Box.createVerticalStrut(16));
        if (role == Role.ADMIN) {
            toolbar.add(button("Mark Adopted", this::onMarkAdopted));
            toolbar.add(Box.createVerticalStrut(16));
            toolbar.add(button("Undo Last Status Change", this::onUndo));
            toolbar.add(Box.createVerticalStrut(16));
        }
        toolbar.add(button("Fetch Sample Pet Image", this::onFetchImage));
        toolbar.add(Box.createVerticalStrut(16));
        classificationFilter.setPrototypeDisplayValue("Dog > Sporting Group > Retriever");
        classificationFilter.setAlignmentX(Component.CENTER_ALIGNMENT);
        classificationFilter.setPreferredSize(new Dimension(240, 40));
        classificationFilter.setMinimumSize(new Dimension(240, 40));
        classificationFilter.setMaximumSize(new Dimension(240, 40));
        classificationFilter.setBorder(new RoundedBorder(25, new Color(0xCCCCCC), 1));
        classificationFilter.setRenderer(new javax.swing.DefaultListCellRenderer() {
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                javax.swing.JLabel c = (javax.swing.JLabel) super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
                c.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
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
    }

    private String shorten(String text) {
        if (text == null) return text;
        if (text.startsWith("List")) return "List...";
        if (text.startsWith("Filter")) return "Filter...";
        if (text.startsWith("Import")) return "Import...";
        if (text.startsWith("Record")) return "Record...";
        if (text.startsWith("Mark")) return "Mark...";
        if (text.startsWith("Undo")) return "Undo...";
        if (text.startsWith("Fetch")) return "Fetch...";
        if (text.contains("All")) return "<all...";
        return text.length() > 6 ? text.substring(0, 6) + "..." : text;
    }

    private String restoreText(String text) {
        if (text.equals("List...")) return "List / Refresh Animals";
        if (text.equals("Filter...")) return "Filter by Classification";
        if (text.equals("Import...")) return "Import Intake History (CSV)";
        if (text.equals("Record...")) return "Record Interest";
        if (text.equals("Mark...")) return "Mark Adopted";
        if (text.equals("Undo...")) return "Undo Last Status Change";
        if (text.equals("Fetch...")) return "Fetch Sample Pet Image";
        if (text.equals("<all...")) return "<All animals>";
        return text;
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildAnimalTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(new JScrollPane(animalTable), BorderLayout.CENTER);
        return panel;
    }

    // Private helper for MainFrame GUI logic
    private JPanel buildImagePanel() {
        imagePanel = new JPanel();
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
        imagePanel.setBorder(BorderFactory.createTitledBorder("Sample Pet Image (live API)"));
        imageLabel.setPreferredSize(IMAGE_SIZE);
        imageLabel.setMinimumSize(new Dimension(200, 200));
        imageLabel.setMaximumSize(new Dimension(400, 400));
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setAlignmentX(CENTER_ALIGNMENT);
        imageLabel.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0xCCCCCC)));
        imageInfoLabel.setAlignmentX(CENTER_ALIGNMENT);
        imagePanel.add(Box.createVerticalStrut(6));
        imagePanel.add(imageLabel);
        imagePanel.add(Box.createVerticalStrut(6));
        imagePanel.add(imageInfoLabel);
        return imagePanel;
    }

    // Private helper for MainFrame GUI logic
    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(240, 40));
        button.setMinimumSize(new Dimension(240, 40));
        button.setMaximumSize(new Dimension(240, 40));
        button.setBorder(new RoundedBorder(25, new Color(0xCCCCCC), 1));
        button.setBackground(new Color(0x2B5B8A));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color normal = new Color(0x2B5B8A);
        Color hover = new Color(0x1F4366);
        Color pressed = new Color(0x142D4A);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(hover); }
            public void mouseExited(MouseEvent e) { button.setBackground(normal); }
            public void mousePressed(MouseEvent e) { button.setBackground(pressed); }
            public void mouseReleased(MouseEvent e) { button.setBackground(button.contains(e.getPoint()) ? hover : normal); }
        });
        button.addActionListener(event -> action.run());
        return button;
    }

    /** Simple rounded-rectangle border for Swing buttons. */
    private static final class RoundedBorder implements javax.swing.border.Border {
        private final int radius;
        private final Color color;
        private final int thickness;

        RoundedBorder(int radius, Color color, int thickness) {
            this.radius = radius;
            this.color = color;
            this.thickness = thickness;
        }

        public java.awt.Insets getBorderInsets(java.awt.Component c) {
            return new java.awt.Insets(thickness, thickness, thickness, thickness);
        }

        public boolean isBorderOpaque() {
            return false;
        }

        public void paintBorder(java.awt.Component c, java.awt.Graphics g, int x, int y, int w, int h) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setColor(color);
            g2.setStroke(new java.awt.BasicStroke(thickness));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }
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
        if (selected == null || ALL_CLASSIFICATIONS.equals(selected)) {
            refreshAnimals();
            statusLabel.setText("Showing all animals");
            return;
        }
        Optional<ClassificationNode> node = classification.findByPath(selected.toString());
        if (node.isEmpty()) {
            warn("Classification not found: " + selected);
            return;
        }
        Set<Integer> ids = new HashSet<>();
        for (ClassificationNode descendant : node.get().flatten()) {
            ids.add(descendant.getId());
        }
        try {
            List<Animal> matches = new ArrayList<>();
            for (Animal animal : animalDao.findAll()) {
                if (animal.getClassificationId() != null && ids.contains(animal.getClassificationId())) {
                    matches.add(animal);
                }
            }
            tableModel.setAnimals(matches);
            statusLabel.setText(String.format("Recursive search under '%s': %d animal(s) across %d classification node(s)",
                    node.get().getName(), matches.size(), ids.size()));
            log("Recursive filter: " + node.get().path());
        } catch (SQLException failure) {
            error("Filter failed", failure);
        }
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

        final ImportWorker worker = new ImportWorker(new CsvImportService(database, classification), path);
        worker.addPropertyChangeListener(event -> {
            if ("progress".equals(event.getPropertyName())) {
                int value = (int) event.getNewValue();
                progressBar.setValue(value);
                progressBar.setString(value + "%");
            } else if ("state".equals(event.getPropertyName())
                    && event.getNewValue() == SwingWorker.StateValue.DONE) {
                try {
                    CsvImportResult result = worker.get();
                    progressBar.setString("Done");
                    statusLabel.setText(result.summary());
                    log("Import finished: " + result.summary());
                    refreshAnimals();
                    refreshAnalytics();
                } catch (Exception failure) {
                    progressBar.setString("Failed");
                    error("Import failed", failure);
                }
            }
        });
        worker.execute();
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
            Adopter persisted = adopterDao.findOrCreate(adopter);
            int position = waitlist.addInterest(animal.getId(), persisted);
            waitlistDao.insert(animal.getId(), persisted.getId(), LocalDateTime.now(), position);
            if (animal.getStatus() != AnimalStatus.PENDING) {
                AnimalStatus previous = animal.getStatus();
                animal.setStatus(AnimalStatus.PENDING);
                animalDao.updateStatus(animal.getId(), AnimalStatus.PENDING);
                history.record(animal.getId(), previous, AnimalStatus.PENDING);
            }
            refreshAnimals();
            refreshAnalytics();
            log(String.format("Interest recorded: %s is #%d in the waitlist for %s",
                    persisted.getName(), position, animal.getName()));
            statusLabel.setText("Interest recorded for " + animal.getName());
        } catch (SQLException | IllegalArgumentException failure) {
            error("Could not record interest", failure);
        }
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
            Adopter persisted = adopterDao.findOrCreate(adopter);
            AdoptionRecord record = new AdoptionRecord(animal.getId(), persisted.getId(), LocalDate.now(),
                    animal.adoptionFee());
            recordDao.insert(record);
            AnimalStatus previous = animal.getStatus();
            animal.setStatus(AnimalStatus.ADOPTED);
            animalDao.updateStatus(animal.getId(), AnimalStatus.ADOPTED);
            history.record(animal.getId(), previous, AnimalStatus.ADOPTED);

            Optional<Adopter> nextInLine = waitlist.pollNext(animal.getId());
            waitlistDao.deleteByAnimal(animal.getId());

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
        Optional<StatusChange> undone = history.undo();
        if (undone.isEmpty()) {
            info("There is no status change to undo.");
            return;
        }
        StatusChange change = undone.get();
        try {
            animalDao.updateStatus(change.animalId(), change.from());
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
        try {
            tableModel.setAnimals(animalDao.findAll());
        } catch (SQLException failure) {
            error("Could not load animals", failure);
        }
    }

    // Private helper for MainFrame GUI logic
    private void refreshAnalytics() {
        try {
            List<Animal> animals = animalDao.findAll();
            List<AdoptionRecord> records = recordDao.findAll();
            AnalyticsReport report = analytics.analyze(animals, records);
            Map<Integer, List<Animal>> grouped = AnalyticsService.groupByClassification(animals);
            Map<String, Long> classCounts = analytics.adoptionCountByClassification(classification.root(), grouped);
            analyticsPanel.showReport(report, classCounts, waitlist.totalWaiters());
        } catch (SQLException failure) {
            log("Analytics failed: " + failure.getMessage());
        }
    }

    // Private helper for MainFrame GUI logic
    private void populateClassificationFilter() {
        classificationFilter.addItem(ALL_CLASSIFICATIONS);
        for (String path : classification.allPaths()) {
            classificationFilter.addItem(path);
        }
    }

    // Private helper for MainFrame GUI logic
    private void loadWaitlists() {
        try {
            for (WaitlistDao.Entry entry : waitlistDao.loadAll()) {
                adopterDao.findById(entry.adopterId())
                        .ifPresent(adopter -> waitlist.addInterest(entry.animalId(), adopter));
            }
            log("Restored " + waitlist.totalWaiters() + " waitlist entr(ies) from the database");
        } catch (SQLException failure) {
            log("Could not restore waitlists: " + failure.getMessage());
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
