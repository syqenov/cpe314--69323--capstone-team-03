package pawtrack.ui;

import pawtrack.model.Species;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * An account-scoped visual overview. MainFrame supplies only the current adopter's
 * records, and continues to own authorization and all write actions.
 */
public final class MyRecordsPanel extends JPanel {
    public record ApplicationRow(int animalId, String pet, String breed, String requested,
                                 int queue, String status) {}
    public record AdoptionRow(int recordId, String pet, String breed, String adopted, double fee) {}

    private static final Color NAVY = new Color(0x162C52);
    private static final Color BLUE = new Color(0x315FAD);
    private static final Color MUTED = new Color(0x637694);
    private static final Color LINE = new Color(0xD6E3F8);
    private static final Color GREEN = new Color(0x18764A);
    private static final Color AMBER = new Color(0x9B660E);

    // Retain the legacy API for MainFrame and tests; this raw text is never visible.
    private final JTextArea recordsArea = new JTextArea();
    private final DefaultTableModel applications = readOnly("Pet", "Species / Breed", "Requested", "Queue", "Status");
    private final DefaultTableModel adoptions = readOnly("Pet", "Adopted", "Fee", "Receipt");
    private final EmptyAwareTable applicationsTable = new EmptyAwareTable(applications);
    private final EmptyAwareTable adoptionsTable = new EmptyAwareTable(adoptions);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(applications);
    private final JLabel pendingCount = valueLabel();
    private final JLabel reservedCount = valueLabel();
    private final JLabel adoptedCount = valueLabel();
    private final JLabel applicationCount = caption("0 applications");
    private final JLabel adoptionCount = caption("0 completed adoptions");
    private final JLabel selectedName = title("Select an application", 18);
    private final JLabel selectedBreed = caption("Choose a row from My Waitlist to see its details.");
    private final JLabel selectedRequested = text("Request date: —");
    private final JLabel selectedQueue = text("Queue position: —");
    private final JLabel selectedStatus = pillLabel("NOT SELECTED", MUTED, new Color(0xEAF0FB));
    private final JTextArea selectedDescription = new JTextArea();
    private final JLabel selectionPhoto = new JLabel("", SwingConstants.CENTER);
    private final JTextField search = new JTextField() {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(0x8295B2));
            graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = getHeight() / 2;
            graphics.drawOval(12, cy - 7, 9, 9);
            graphics.drawLine(19, cy + 1, 24, cy + 6);
            if (getText().isEmpty() && !isFocusOwner()) {
                graphics.setFont(font(12, false));
                FontMetrics metrics = graphics.getFontMetrics();
                int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                graphics.drawString("Search pet, breed, or status...", 34, y);
            }
            graphics.dispose();
        }
    };
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"All statuses", "Waiting", "Under review", "Reserved", "Completed", "Other"});
    /** Uses the same photo and API cache as Browse Pets and staff Waitlist. */
    @FunctionalInterface
    interface PhotoLoader {
        void load(int id, Species species, String breed, int width, int height,
                  Consumer<PetImageCache.Loaded> onSuccess, Consumer<Exception> onFailure);
    }

    private final PhotoLoader photoLoader;
    private final Map<Integer, PetImageCache.Loaded> photoCache = new HashMap<>();
    private final Set<Integer> loadingPhotoIds = new HashSet<>();
    private final Map<Integer, Long> failedPhotoAt = new HashMap<>();
    private static final long RETRY_AFTER_MS = 60_000;
    private List<ApplicationRow> currentApplications = List.of();
    private List<AdoptionRow> currentAdoptions = List.of();
    public MyRecordsPanel(Color surfaceColor) {
        this(surfaceColor, PetImageCache.get()::load);
    }

    /** Package-private loader injection keeps photo tests fully offline. */
    MyRecordsPanel(Color surfaceColor, PhotoLoader photoLoader) {
        super(new BorderLayout());
        this.photoLoader = Objects.requireNonNull(photoLoader, "photoLoader");
        Color background = Objects.requireNonNull(surfaceColor, "surfaceColor");
        setBackground(background);
        recordsArea.setEditable(false);
        recordsArea.setLineWrap(true);
        recordsArea.setWrapStyleWord(true);
        recordsArea.setBackground(background);
        recordsArea.setForeground(NAVY);
        JScrollPane legacy = new JScrollPane(recordsArea);
        legacy.setVisible(false);
        legacy.setPreferredSize(new Dimension(0, 0));
        add(legacy, BorderLayout.WEST); // Preserve existing tests and MainFrame API.

        JPanel page = new PremiumUi.Background(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(20, 24, 15, 24));
        page.add(buildHeader(), BorderLayout.NORTH);
        page.add(buildMainContent(), BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);

        configureTable(applicationsTable, 58);
        configureTable(adoptionsTable, 54);
        applicationsTable.setRowSorter(sorter);
        applicationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        adoptionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        applicationsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateSelection();
        });
        applicationsTable.setDefaultRenderer(Object.class, new ApplicationCell());
        applicationsTable.getColumnModel().getColumn(4).setCellRenderer(new StatusCell());
        applicationsTable.setEmpty("No applications yet", "Record interest in an animal to join its waitlist.");
        adoptionsTable.setEmpty("No adoptions yet", "Completed adoptions and receipts appear here.");
        adoptionsTable.setDefaultRenderer(Object.class, new AdoptionCell());
        applicationsTable.getColumnModel().getColumn(0).setPreferredWidth(155);
        applicationsTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        applicationsTable.getColumnModel().getColumn(2).setPreferredWidth(115);
        applicationsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        applicationsTable.getColumnModel().getColumn(4).setPreferredWidth(165);
        adoptionsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        adoptionsTable.getColumnModel().getColumn(1).setPreferredWidth(96);
        adoptionsTable.getColumnModel().getColumn(2).setPreferredWidth(75);
        adoptionsTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        selectionPhoto.setOpaque(true);
        selectionPhoto.setBackground(new Color(0xE8F1FF));
        selectionPhoto.setForeground(BLUE);
        updateSelection();
        setFilterListeners();
    }

    private JPanel buildHeader() {
        JPanel header = transparent(new BorderLayout(0, 12));
        JPanel heading = transparent(new GridLayout(2, 1, 0, 3));
        heading.add(title("My Applications & Adoptions", 27));
        heading.add(caption("Track requests, queue positions, and successful adoptions in one place."));
        header.add(heading, BorderLayout.NORTH);
        JPanel metrics = transparent(new GridLayout(1, 3, 14, 0));
        metrics.add(stat("PENDING APPLICATIONS", pendingCount, "Requests currently in your queue",
                new Color(0xE9F1FF), BLUE, 0));
        metrics.add(stat("RESERVED", reservedCount, "Pets temporarily reserved for you",
                new Color(0xEFEAFF), new Color(0x6A4FC2), 1));
        metrics.add(stat("MY ADOPTIONS", adoptedCount, "Successfully adopted pets",
                new Color(0xE7F7ED), GREEN, 2));
        header.add(metrics, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildMainContent() {
        JPanel body = transparent(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = .63;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 9);
        body.add(buildApplicationsCard(), constraints);

        JPanel right = transparent(new GridBagLayout());
        GridBagConstraints detail = new GridBagConstraints();
        detail.gridx = 0;
        detail.gridy = 0;
        detail.weightx = 1;
        detail.weighty = .52;
        detail.fill = GridBagConstraints.BOTH;
        detail.insets = new Insets(0, 0, 8, 0);
        right.add(buildSelectedCard(), detail);
        detail.gridy = 1;
        detail.weighty = .48;
        detail.insets = new Insets(8, 0, 0, 0);
        right.add(buildAdoptionsCard(), detail);

        constraints.gridx = 1;
        constraints.weightx = .37;
        constraints.insets = new Insets(0, 9, 0, 0);
        body.add(right, constraints);
        return body;
    }

    private JPanel buildApplicationsCard() {
        JPanel card = PremiumUi.card(new BorderLayout(0, 13));
        JPanel header = transparent(new BorderLayout(0, 4));
        header.add(title("My Waitlist", 21), BorderLayout.NORTH);
        header.add(caption("All your own applications, including completed requests."), BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        JPanel center = transparent(new BorderLayout(0, 10));
        JPanel tools = transparent(new BorderLayout(10, 0));
        search.setFont(font(12, false));
        search.setToolTipText("Search by pet name, breed, date, or status");
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(9, 34, 9, 11)));
        tools.add(search, BorderLayout.CENTER);
        statusFilter.setFont(font(12, false));
        statusFilter.setPreferredSize(new Dimension(136, 37));
        tools.add(statusFilter, BorderLayout.EAST);
        center.add(tools, BorderLayout.NORTH);

        JScrollPane scroll = scroll(applicationsTable);
        scroll.setMinimumSize(new Dimension(0, 120));
        center.add(scroll, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);
        JPanel footer = transparent(new BorderLayout());
        footer.add(applicationCount, BorderLayout.WEST);
        JLabel instruction = caption("Select a row to see details");
        footer.add(instruction, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        card.setMinimumSize(new Dimension(320, 230));
        return card;
    }

    private JPanel buildSelectedCard() {
        JPanel card = PremiumUi.card(new BorderLayout(0, 9));
        card.add(title("Selected Application", 19), BorderLayout.NORTH);
        JPanel main = transparent(new BorderLayout(10, 12));
        selectionPhoto.setPreferredSize(new Dimension(78, 82));
        selectionPhoto.setBorder(BorderFactory.createLineBorder(LINE));
        main.add(selectionPhoto, BorderLayout.WEST);
        JPanel summary = transparent(new GridLayout(3, 1, 0, 4));
        summary.add(selectedName);
        summary.add(selectedBreed);
        JPanel pillRow = transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pillRow.add(selectedStatus);
        summary.add(pillRow);
        main.add(summary, BorderLayout.CENTER);
        JPanel details = transparent(new GridLayout(2, 1, 0, 6));
        details.add(selectedRequested);
        details.add(selectedQueue);
        JPanel info = transparent(new BorderLayout(0, 9));
        info.add(main, BorderLayout.NORTH);
        info.add(details, BorderLayout.CENTER);
        card.add(info, BorderLayout.CENTER);
        selectedDescription.setEditable(false);
        selectedDescription.setLineWrap(true);
        selectedDescription.setWrapStyleWord(true);
        selectedDescription.setOpaque(false);
        selectedDescription.setForeground(MUTED);
        selectedDescription.setFont(font(12, false));
        selectedDescription.setRows(3);
        selectedDescription.setText("Select an application to view its progress.");
        card.add(selectedDescription, BorderLayout.SOUTH);
        card.setMinimumSize(new Dimension(230, 195));
        return card;
    }

    private JPanel buildAdoptionsCard() {
        JPanel card = PremiumUi.card(new BorderLayout(0, 10));
        JPanel header = transparent(new BorderLayout(0, 4));
        header.add(title("My Adoptions", 19), BorderLayout.NORTH);
        header.add(caption("Select a receipt below and choose View Adoption Receipt."), BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);
        card.add(scroll(adoptionsTable), BorderLayout.CENTER);
        card.add(adoptionCount, BorderLayout.SOUTH);
        card.setMinimumSize(new Dimension(230, 195));
        return card;
    }

    private void setFilterListeners() {
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { filterApplications(); }
            @Override public void removeUpdate(DocumentEvent event) { filterApplications(); }
            @Override public void changedUpdate(DocumentEvent event) { filterApplications(); }
        });
        statusFilter.addActionListener(event -> filterApplications());
    }

    private void filterApplications() {
        String q = search.getText().strip().toLowerCase(java.util.Locale.ROOT);
        String status = (String) statusFilter.getSelectedItem();
        sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                int model = entry.getIdentifier();
                if (model < 0 || model >= currentApplications.size()) return false;
                ApplicationRow r = currentApplications.get(model);
                String all = (r.pet() + " " + r.breed() + " " + r.requested() + " " + r.status())
                        .toLowerCase(java.util.Locale.ROOT);
                if (!q.isEmpty() && !all.contains(q)) return false;
                String state = r.status().toUpperCase(java.util.Locale.ROOT);
                if ("Waiting".equals(status)) return state.contains("WAITING") || state.contains("PENDING");
                if ("Under review".equals(status)) return state.contains("REVIEW");
                if ("Reserved".equals(status)) return state.contains("RESERVED");
                if ("Completed".equals(status)) return state.contains("COMPLETED");
                if ("Other".equals(status)) return !(state.contains("WAITING") || state.contains("PENDING") ||
                        state.contains("REVIEW") || state.contains("RESERVED") || state.contains("COMPLETED"));
                return true;
            }
        });
        if (currentApplications.isEmpty()) {
            applicationsTable.setEmpty("No applications yet", "Record interest in an animal to join its waitlist.");
        } else {
            applicationsTable.setEmpty("No matching applications", "Try a different search or status filter.");
        }
        applicationCount.setText(applicationsTable.getRowCount() + " of " + currentApplications.size() + " applications");
        updateSelection();
    }

    public void showRecords(List<ApplicationRow> requestRows, List<AdoptionRow> adoptionRows) {
        Objects.requireNonNull(requestRows, "requestRows");
        Objects.requireNonNull(adoptionRows, "adoptionRows");
        Integer selectedId = selectedAnimalId();
        currentApplications = List.copyOf(requestRows);
        currentAdoptions = List.copyOf(adoptionRows);
        // A shelter photo might have been added since the last refresh.
        // The shared cache preserves API sample consistency across screens.
        photoCache.clear();
        failedPhotoAt.clear();
        applications.setRowCount(0);
        adoptions.setRowCount(0);
        int pending = 0;
        int reserved = 0;
        for (ApplicationRow r : currentApplications) {
            String state = r.status().toUpperCase(java.util.Locale.ROOT);
            if (state.contains("WAITING") || state.contains("REVIEW") || state.contains("PENDING")) pending++;
            if (state.contains("RESERVED")) reserved++;
            applications.addRow(new Object[]{r.pet(), r.breed(), r.requested(), "#" + r.queue(), r.status()});
        }
        for (AdoptionRow r : currentAdoptions) {
            adoptions.addRow(new Object[]{r.pet(), r.adopted(), String.format(java.util.Locale.US, "$%.2f", r.fee()),
                    "#" + r.recordId()});
        }
        pendingCount.setText(Integer.toString(pending));
        reservedCount.setText(Integer.toString(reserved));
        adoptedCount.setText(Integer.toString(currentAdoptions.size()));
        adoptionCount.setText(currentAdoptions.size() + " completed adoptions");
        filterApplications();
        if (selectedId != null) {
            for (int i = 0; i < currentApplications.size(); i++) {
                if (currentApplications.get(i).animalId() == selectedId) {
                    int visible = applicationsTable.convertRowIndexToView(i);
                    if (visible >= 0) applicationsTable.setRowSelectionInterval(visible, visible);
                    break;
                }
            }
        }
        updateSelection();
    }

    private void updateSelection() {
        int view = applicationsTable.getSelectedRow();
        int index = view < 0 ? -1 : applicationsTable.convertRowIndexToModel(view);
        if (index < 0 || index >= currentApplications.size()) {
            selectedName.setText("Select an application");
            selectedBreed.setText("Choose a pet from the waitlist table");
            selectedRequested.setText("Request date: —");
            selectedQueue.setText("Queue position: —");
            selectedStatus.setText("NOT SELECTED");
            selectedDescription.setText("Your application details and current progress appear here.");
            selectionPhoto.setText("");
            selectionPhoto.setToolTipText(null);
            selectionPhoto.setIcon(new PlaceholderIcon(76, 80, 1.25, ""));
            return;
        }
        ApplicationRow row = currentApplications.get(index);
        selectedName.setText(row.pet());
        selectedBreed.setText(row.breed());
        selectedRequested.setText("Requested: " + row.requested());
        selectedQueue.setText("Queue position: #" + row.queue());
        String state = classifyState(row.status());
        selectedStatus.setText(state);
        selectedStatus.setForeground(stateColor(state));
        selectedStatus.setBackground(stateBackground(state));
        selectedDescription.setText(statusExplanation(row.status()));
        Species species = speciesFor(row.breed());
        PetImageCache.Loaded photo = photoCache.get(row.animalId());
        selectionPhoto.setText("");
        selectionPhoto.setIcon(photo != null ? photo.icon() :
                new PlaceholderIcon(76, 80, 1.25, species == null ? "PET" : species.name()));
        selectionPhoto.setToolTipText(photo == null ? "Photo unavailable" : photoTooltip(photo));
        if (photo != null && !photo.actualPhoto()) {
            selectedDescription.setText("Sample image from " + photo.provider() +
                    " (not this specific pet).\n" + statusExplanation(row.status()));
        }
        if (photo == null) loadPhoto(row);
    }

    /**
     * Starts a single asynchronous load for this pet. Local uploaded photos take
     * priority; the existing shared cache fetches a dog/cat API sample otherwise.
     * The callbacks are delivered on Swing's EDT and never block table painting.
     */
    private void loadPhoto(ApplicationRow row) {
        int id = row.animalId();
        if (id <= 0 || photoCache.containsKey(id) || loadingPhotoIds.contains(id)) return;
        Long lastFailure = failedPhotoAt.get(id);
        if (lastFailure != null && System.currentTimeMillis() - lastFailure < RETRY_AFTER_MS) return;
        loadingPhotoIds.add(id);
        try {
            photoLoader.load(id, speciesFor(row.breed()), breedForApi(row.breed()), 76, 80,
                    loaded -> {
                        loadingPhotoIds.remove(id);
                        failedPhotoAt.remove(id);
                        if (loaded != null && loaded.icon() != null) photoCache.put(id, loaded);
                        if (Objects.equals(selectedAnimalId(), id)) updateSelection();
                        applicationsTable.repaint();
                    }, failure -> {
                        loadingPhotoIds.remove(id);
                        failedPhotoAt.put(id, System.currentTimeMillis());
                        applicationsTable.repaint(); // Retain a paw placeholder offline.
                    });
        } catch (RuntimeException failure) {
            loadingPhotoIds.remove(id);
            failedPhotoAt.put(id, System.currentTimeMillis());
        }
    }

    /** MainFrame formats these values as "Dog / Golden Retriever" or "Cat / Siamese". */
    static Species speciesFor(String speciesAndBreed) {
        if (speciesAndBreed == null) return null;
        String prefix = speciesAndBreed.split(" / ", 2)[0].strip();
        if ("Dog".equalsIgnoreCase(prefix)) return Species.DOG;
        if ("Cat".equalsIgnoreCase(prefix)) return Species.CAT;
        return null;
    }

    static String breedForApi(String speciesAndBreed) {
        if (speciesAndBreed == null) return "";
        int divider = speciesAndBreed.indexOf(" / ");
        return divider < 0 ? "" : speciesAndBreed.substring(divider + 3).strip();
    }

    private static String photoTooltip(PetImageCache.Loaded photo) {
        return photo.actualPhoto() ? "Photo uploaded by the shelter" :
                "Sample image from " + photo.provider() + "; not this specific pet";
    }

    public Integer selectedAnimalId() {
        int view = applicationsTable.getSelectedRow();
        if (view < 0) return null;
        int row = applicationsTable.convertRowIndexToModel(view);
        return row >= 0 && row < currentApplications.size() ? currentApplications.get(row).animalId() : null;
    }

    public Integer selectedReceiptId() {
        int view = adoptionsTable.getSelectedRow();
        if (view < 0) return null;
        int row = adoptionsTable.convertRowIndexToModel(view);
        return row >= 0 && row < currentAdoptions.size() ? currentAdoptions.get(row).recordId() : null;
    }

    /** Source compatibility only; the raw JTextArea isn't the visible records UI. */
    public JTextArea getRecordsArea() { return recordsArea; }

    private static String classifyState(String full) {
        String u = full.toUpperCase(java.util.Locale.ROOT);
        if (u.contains("COMPLETED")) return "COMPLETED";
        if (u.contains("RESERVED")) return "RESERVED";
        if (u.contains("REVIEW")) return "IN REVIEW";
        if (u.contains("WAITING") || u.contains("PENDING")) return "WAITING";
        if (u.contains("EXPIRED")) return "EXPIRED";
        if (u.contains("CANCELLED")) return "CANCELLED";
        return u;
    }

    private static String statusExplanation(String full) {
        String s = classifyState(full);
        return switch (s) {
            case "COMPLETED" -> "Adoption finalized. You can view the corresponding adoption receipt below.";
            case "RESERVED" -> "A reservation is active. Follow your shelter's instructions before its deadline.";
            case "IN REVIEW" -> "Your application is being reviewed by shelter staff.";
            case "WAITING" -> "Your interest is in the first-in, first-out waitlist. Staff will review eligible requests.";
            case "EXPIRED" -> "The reservation deadline has passed. Contact the shelter for assistance.";
            case "CANCELLED" -> "This reservation was cancelled. Contact the shelter if you need more information.";
            default -> full;
        };
    }

    private static Color stateBackground(String s) {
        return switch (s) {
            case "COMPLETED" -> new Color(0xDDF6E8);
            case "RESERVED" -> new Color(0xE0EBFF);
            case "WAITING", "IN REVIEW" -> new Color(0xFFF2D8);
            default -> new Color(0xEEF2F9);
        };
    }

    private static Color stateColor(String s) {
        return switch (s) {
            case "COMPLETED" -> GREEN;
            case "WAITING", "IN REVIEW" -> AMBER;
            case "RESERVED" -> BLUE;
            default -> MUTED;
        };
    }

    private static DefaultTableModel readOnly(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    private static JPanel transparent(LayoutManager manager) {
        JPanel panel = new JPanel(manager);
        panel.setOpaque(false);
        return panel;
    }

    private static Font font(int size, boolean bold) { return new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size); }
    private static JLabel title(String s, int size) {
        JLabel l = new JLabel(s);
        l.setForeground(NAVY);
        l.setFont(font(size, true));
        return l;
    }
    private static JLabel caption(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(MUTED);
        l.setFont(font(12, false));
        return l;
    }
    private static JLabel text(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(NAVY);
        l.setFont(font(12, false));
        return l;
    }
    private static JLabel valueLabel() { return title("0", 32); }
    private static JLabel pillLabel(String s, Color fg, Color bg) {
        Pill l = new Pill(s);
        l.setForeground(fg);
        l.setBackground(bg);
        return l;
    }
    private static JPanel stat(String label, JLabel value, String explanation, Color accent, Color fg, int kind) {
        JPanel card = PremiumUi.card(new BorderLayout(14, 0));
        card.setBorder(new EmptyBorder(14, 18, 16, 18));
        card.add(new StatBadge(kind, fg, accent), BorderLayout.WEST);
        JPanel text = transparent(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel heading = title(label, 11);
        heading.setForeground(MUTED);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel note = caption(explanation);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(Box.createVerticalGlue());
        text.add(heading);
        text.add(value);
        text.add(note);
        text.add(Box.createVerticalGlue());
        card.add(text, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(150, 112));
        return card;
    }

    private static JScrollPane scroll(JTable table) {
        JScrollPane pane = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setBorder(BorderFactory.createLineBorder(LINE));
        pane.getViewport().setBackground(Color.WHITE);
        return pane;
    }

    private static void configureTable(JTable table, int height) {
        PremiumUi.styleTable(table);
        table.setRowHeight(height);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String formatBreed(String breed) {
        String[] two = breed.split(" / ", 2);
        if (two.length == 2) return two[0] + " · " + two[1];
        return breed;
    }

    private abstract class Cell extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object val, boolean selected,
                                                                   boolean focus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, val, selected, focus, row, column);
            l.setIcon(null); // renderers are reused; clear any icon left from another column
            l.setFont(font(12, false));
            l.setForeground(PawTrackTheme.text());
            l.setBackground(PawTrackTheme.isDark() ? (selected ? PawTrackTheme.DARK_SELECTED :
                    (row % 2 == 0 ? PawTrackTheme.DARK_SURFACE : PawTrackTheme.DARK_INPUT)) :
                    (selected ? new Color(0xDCEAFE) : row % 2 == 0 ? Color.WHITE : new Color(0xF4F8FF)));
            l.setBorder(new EmptyBorder(6, 10, 6, 10));
            l.setOpaque(true);
            l.setVerticalAlignment(SwingConstants.CENTER);
            l.setToolTipText(val == null ? null : val.toString());
            return l;
        }
    }

    private final class ApplicationCell extends Cell {
        @Override public Component getTableCellRendererComponent(JTable table, Object val, boolean sel,
                                                                   boolean focus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, val, sel, focus, row, column);
            int index = table.convertRowIndexToModel(row);
            if (index >= 0 && index < currentApplications.size()) {
                ApplicationRow r = currentApplications.get(index);
                if (column == 0) {
                    l.setText("<html><b>" + escapeHtml(r.pet()) + "</b></html>");
                    PetImageCache.Loaded image = photoCache.get(r.animalId());
                    if (image != null) {
                        Image thumb = image.icon().getImage().getScaledInstance(38, 40, Image.SCALE_SMOOTH);
                        l.setIcon(new ImageIcon(thumb));
                        l.setToolTipText(photoTooltip(image));
                        if (!image.actualPhoto()) {
                            l.setText("<html><b>" + escapeHtml(r.pet()) +
                                    "</b><br><font color='#A9BCD3'><small>Sample image</small></font></html>");
                        }
                    } else {
                        l.setIcon(new PlaceholderIcon(38, 40, 0.62, ""));
                        loadPhoto(r);
                    }
                    l.setIconTextGap(10);
                } else if (column == 1) {
                    l.setText(formatBreed(r.breed()));
                } else if (column == 3) {
                    l.setFont(font(12, true));
                    l.setForeground(PawTrackTheme.accent());
                }
            }
            return l;
        }
    }

    /** Status shown as a rounded colored pill instead of plain colored text. */
    private final class StatusCell extends JPanel implements TableCellRenderer {
        private String state = "";
        private Color rowBackground = Color.WHITE;

        StatusCell() { setOpaque(true); }

        @Override public Component getTableCellRendererComponent(JTable table, Object val, boolean selected,
                                                                   boolean focus, int row, int column) {
            rowBackground = selected ? new Color(0xDCEAFE) : row % 2 == 0 ? Color.WHITE : new Color(0xF4F8FF);
            state = "";
            int index = table.convertRowIndexToModel(row);
            if (index >= 0 && index < currentApplications.size()) {
                ApplicationRow r = currentApplications.get(index);
                state = classifyState(r.status());
                setToolTipText(r.status());
            }
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gg.setColor(PawTrackTheme.fill(rowBackground));
            gg.fillRect(0, 0, getWidth(), getHeight());
            if (!state.isEmpty()) {
                gg.setFont(font(11, true));
                FontMetrics metrics = gg.getFontMetrics();
                int h = 26;
                int w = metrics.stringWidth(state) + 38;
                int x = 10;
                int y = (getHeight() - h) / 2;
                gg.setColor(stateBackground(state));
                gg.fillRoundRect(x, y, w, h, h, h);
                gg.setColor(stateColor(state));
                gg.fillOval(x + 12, y + h / 2 - 3, 6, 6);
                gg.drawString(state, x + 24, y + (h - metrics.getHeight()) / 2 + metrics.getAscent());
            }
            gg.dispose();
        }
    }

    private final class AdoptionCell extends Cell {
        @Override public Component getTableCellRendererComponent(JTable table, Object val, boolean sel,
                                                                   boolean focus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, val, sel, focus, row, column);
            if (column == 0) l.setFont(font(12, true));
            if (column == 3) {
                l.setForeground(PawTrackTheme.accent());
                l.setHorizontalAlignment(SwingConstants.CENTER);
            } else l.setHorizontalAlignment(SwingConstants.LEFT);
            return l;
        }
    }

    // ------------------------------------------------------------ visual helpers

    /** Same paw shape as the header logo (a 37x37 box, scaled). */
    private static void drawPaw(Graphics2D g, int x, int y, double scale) {
        Graphics2D p = (Graphics2D) g.create();
        p.translate(x, y);
        p.scale(scale, scale);
        p.fillOval(13, 18, 13, 11);
        p.fillOval(8, 10, 6, 8);
        p.fillOval(16, 6, 6, 8);
        p.fillOval(25, 10, 6, 8);
        p.dispose();
    }

    /** Scales to fill w x h (centered crop, rounded corners) so photos are never stretched. */
    private static BufferedImage cover(BufferedImage src, int w, int h) {
        double scale = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
        int sw = (int) Math.ceil(src.getWidth() * scale);
        int sh = (int) Math.ceil(src.getHeight() * scale);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new RoundRectangle2D.Double(0, 0, w, h, 14, 14));
        g.drawImage(src, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
        g.dispose();
        return out;
    }

    /** Rounded status pill (the background is painted, so it never stretches like a bar). */
    private static final class Pill extends JLabel {
        Pill(String text) {
            super(text, SwingConstants.CENTER);
            setOpaque(false);
            setFont(font(11, true));
            setBorder(new EmptyBorder(5, 14, 5, 14));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setColor(getBackground());
            gg.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            gg.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded tinted square with a small vector icon: 0 clock, 1 bookmark, 2 heart. */
    private static final class StatBadge extends JComponent {
        private final int kind;
        private final Color fg;
        private final Color bg;

        StatBadge(int kind, Color fg, Color bg) {
            this.kind = kind;
            this.fg = fg;
            this.bg = bg;
            setPreferredSize(new Dimension(52, 52));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = Math.max(0, (getHeight() - 52) / 2);
            gg.setColor(PawTrackTheme.fill(bg));
            gg.fillRoundRect(0, y, 52, 52, 18, 18);
            gg.translate(14, y + 14);
            gg.setColor(fg);
            gg.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case 0 -> {
                    gg.drawOval(3, 3, 18, 18);
                    gg.drawPolyline(new int[]{12, 12, 16}, new int[]{7, 12, 14}, 3);
                }
                case 1 -> {
                    Path2D mark = new Path2D.Double();
                    mark.moveTo(7, 3);
                    mark.lineTo(17, 3);
                    mark.lineTo(17, 21);
                    mark.lineTo(12, 17);
                    mark.lineTo(7, 21);
                    mark.closePath();
                    gg.draw(mark);
                }
                default -> {
                    Path2D heart = new Path2D.Double();
                    heart.moveTo(12, 21);
                    heart.curveTo(4, 15, 2, 9, 6, 6);
                    heart.curveTo(9, 4, 11.5, 6, 12, 8);
                    heart.curveTo(12.5, 6, 15, 4, 18, 6);
                    heart.curveTo(22, 9, 20, 15, 12, 21);
                    heart.closePath();
                    gg.fill(heart);
                }
            }
            gg.dispose();
        }
    }

    /** Soft paw placeholder shown when an animal has no photo yet. */
    private static final class PlaceholderIcon implements Icon {
        private final int w;
        private final int h;
        private final double scale;
        private final String label;

        PlaceholderIcon(int w, int h, double scale, String label) {
            this.w = w;
            this.h = h;
            this.scale = scale;
            this.label = label;
        }

        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int arc = Math.min(w, h) / 4;
            gg.setColor(new Color(0xE4EEFF));
            gg.fillRoundRect(x, y, w, h, arc, arc);
            gg.setColor(new Color(0xA9C3EE));
            int box = (int) Math.round(37 * scale);
            int px = x + (w - box) / 2;
            int py = y + (h - box) / 2 - (label.isEmpty() ? 0 : 7);
            drawPaw(gg, px, py, scale);
            if (!label.isEmpty()) {
                gg.setFont(font(9, true));
                FontMetrics metrics = gg.getFontMetrics();
                gg.drawString(label, x + (w - metrics.stringWidth(label)) / 2, y + h - 9);
            }
            gg.dispose();
        }
    }

    /** JTable that paints a friendly message when it has no rows. */
    private static final class EmptyAwareTable extends JTable {
        private String emptyTitle = "";
        private String emptyHint = "";

        EmptyAwareTable(TableModel model) { super(model); }

        void setEmpty(String title, String hint) {
            emptyTitle = title;
            emptyHint = hint;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() > 0 || emptyTitle.isEmpty()) return;
            Graphics2D gg = (Graphics2D) g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean roomForPaw = h > 170;
            int blockHeight = (roomForPaw ? 62 : 0) + 22 + (emptyHint.isEmpty() ? 0 : 18);
            int top = Math.max(8, (h - blockHeight) / 2);
            if (roomForPaw) {
                gg.setColor(new Color(0xD5E3FA));
                drawPaw(gg, (w - 48) / 2, top, 1.3);
                top += 62;
            }
            gg.setFont(font(14, true));
            FontMetrics bold = gg.getFontMetrics();
            gg.setColor(new Color(0x4A6491));
            gg.drawString(emptyTitle, (w - bold.stringWidth(emptyTitle)) / 2, top + bold.getAscent());
            if (!emptyHint.isEmpty()) {
                gg.setFont(font(12, false));
                FontMetrics plain = gg.getFontMetrics();
                gg.setColor(new Color(0x8295B2));
                gg.drawString(emptyHint, (w - plain.stringWidth(emptyHint)) / 2, top + 22 + plain.getAscent());
            }
            gg.dispose();
        }
    }
}
