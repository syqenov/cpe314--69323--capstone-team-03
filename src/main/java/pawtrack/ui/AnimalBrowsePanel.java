package pawtrack.ui;

import pawtrack.model.Species;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Animal browser with a Gallery view and a Table view over the same sortable JTable.
 * All record modifications stay in the existing controller callbacks in MainFrame.
 *
 * Redesign notes:
 * - Pictures come from PetImageCache (uploaded photo first, API sample second).
 * - Adopters start on "Available"; staff start on "All statuses".
 * - Species filter, page-jump box, and a Gallery/Table toggle that shows the active view.
 * - The Pet Profile box only appears once a pet is selected.
 * - Status pills: green = available, amber = pending, grey = adopted.
 */
public final class AnimalBrowsePanel extends JPanel {
    @Override protected void paintComponent(java.awt.Graphics graphics) {
        super.paintComponent(graphics);
        PawTrackBackdrop.paint(graphics, this);
    }

    private static final int PAGE_SIZE = 9;           // 3 x 3 grid
    private static final int CARD_IMG_W = 300, CARD_IMG_H = 150;
    private static final int PROFILE_IMG_W = 210, PROFILE_IMG_H = 190;
    private static final String ALL_STATUSES = "All statuses";
    private static final String ALL_SPECIES = "All species";

    private final JTable table;
    private final JTextField search = new JTextField(16);
    private final JComboBox<String> status = new JComboBox<>(new String[]{
            ALL_STATUSES, "Available", "Pending Interest", "Intake", "Adopted", "Returned"});
    private final JComboBox<String> speciesFilter = new JComboBox<>(new String[]{ALL_SPECIES});
    private final JToggleButton galleryToggle = viewToggle("Gallery");
    private final JToggleButton tableToggle = viewToggle("Table");
    private final JLabel resultCount = PremiumUi.caption("0 pets");
    private final TableRowSorter<TableModel> sorter;
    private final CardLayout viewsLayout = new CardLayout();
    private final JPanel views = PremiumUi.clear(viewsLayout);
    private final JPanel galleryGrid = PremiumUi.clear(new GridLayout(0, 2, 13, 13));
    private final JScrollPane galleryScroll;
    private int galleryColumns = 2;
    private final JLabel pageOf = PremiumUi.caption("");
    private final JTextField pageField = new JTextField(3);
    private final JButton previousPage = PremiumUi.button("Previous", this::previousPage, false);
    private final JButton nextPage = PremiumUi.button("Next", this::nextPage, false);
    private final JPanel preview;
    private final JLabel profileName = PremiumUi.title("Choose a pet", 20);
    private final JLabel profileBreed = PremiumUi.caption("");
    private final JLabel profileStatus = PremiumUi.caption("");
    private final JLabel profileDetails = PremiumUi.caption("");
    private final JLabel profilePhoto = new JLabel("PET", SwingConstants.CENTER);
    private final JLabel profilePhotoNote = PremiumUi.caption(" ");
    private int galleryPage = 0;
    private boolean galleryView;
    private boolean defaultAvailableFilter;
    private boolean automaticStatusAdjustment;

    public AnimalBrowsePanel(JTable table, boolean staff, Runnable refresh, Runnable recordInterest) {
        this(table, staff, refresh, recordInterest, () -> {});
    }

    public AnimalBrowsePanel(JTable table, boolean staff, Runnable refresh, Runnable recordInterest,
                             Runnable viewProfile) {
        super(new BorderLayout(0, 13));
        this.table = Objects.requireNonNull(table, "table");
        Objects.requireNonNull(refresh);
        Objects.requireNonNull(recordInterest);
        Objects.requireNonNull(viewProfile);
        setBackground(PremiumUi.CANVAS);
        setBorder(new EmptyBorder(19, 21, 18, 21));
        if (table.getRowSorter() instanceof TableRowSorter<?>) {
            @SuppressWarnings("unchecked") TableRowSorter<TableModel> existing =
                    (TableRowSorter<TableModel>) table.getRowSorter();
            sorter = existing;
        } else {
            sorter = new TableRowSorter<>(table.getModel());
            table.setRowSorter(sorter);
        }

        // ---------------------------------------------------------------- header + filters
        JPanel top = PremiumUi.clear(new BorderLayout(0, 12));
        JPanel heading = PremiumUi.clear(new BorderLayout(0, 3));
        heading.add(PremiumUi.title(staff ? "Animal Records" : "Browse Pets", 27), BorderLayout.NORTH);
        heading.add(PremiumUi.caption(staff ? "Manage your shelter's animal records in one place."
                : "Find a companion, explore profiles and follow adoption availability."), BorderLayout.SOUTH);
        top.add(heading, BorderLayout.NORTH);

        JPanel tools = PremiumUi.clear(new BorderLayout(0, 6));
        JPanel filters = PremiumUi.clear(new FlowLayout(FlowLayout.LEFT, 9, 2));
        JPanel commands = PremiumUi.clear(new FlowLayout(FlowLayout.LEFT, 9, 2));
        search.setName("animalBrowseSearch");
        search.setToolTipText("Search name, species, breed and table details");
        search.setPreferredSize(new Dimension(175, 37));
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PremiumUi.LINE),
                new EmptyBorder(6, 10, 6, 10)));
        status.setName("animalBrowseStatus");
        status.setPreferredSize(new Dimension(150, 37));
        speciesFilter.setName("animalBrowseSpecies");
        speciesFilter.setPreferredSize(new Dimension(130, 37));
        filters.add(search);
        filters.add(status);
        filters.add(speciesFilter);
        commands.add(PremiumUi.button("Refresh", refresh, false));
        ButtonGroup group = new ButtonGroup();
        group.add(galleryToggle);
        group.add(tableToggle);
        galleryToggle.addActionListener(e -> setGalleryView(true));
        tableToggle.addActionListener(e -> setGalleryView(false));
        commands.add(galleryToggle);
        commands.add(tableToggle);
        tools.add(filters, BorderLayout.NORTH);
        tools.add(commands, BorderLayout.SOUTH);
        top.add(tools, BorderLayout.CENTER);
        top.add(resultCount, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // ---------------------------------------------------------------- table view
        PremiumUi.styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {62, 135, 90, 205, 85, 125, 155, 100, 80};
        for (int i = 0; i < Math.min(widths.length, table.getColumnCount()); i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(PremiumUi.LINE));
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(20);
        views.add(tableScroll, "table");

        // ---------------------------------------------------------------- gallery view
        JPanel gallery = PremiumUi.clear(new BorderLayout(0, 9));
        galleryScroll = new JScrollPane(galleryGrid);
        galleryScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        galleryScroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                updateGalleryColumns();
            }
        });
        galleryScroll.setBorder(null);
        galleryScroll.getViewport().setBackground(PremiumUi.CANVAS);
        galleryScroll.getVerticalScrollBar().setUnitIncrement(22);
        gallery.add(galleryScroll, BorderLayout.CENTER);
        pageField.setHorizontalAlignment(SwingConstants.CENTER);
        pageField.setToolTipText("Type a page number and press Enter");
        pageField.addActionListener(e -> jumpToTypedPage());
        JPanel footer = PremiumUi.clear(new FlowLayout(FlowLayout.CENTER, 8, 2));
        footer.add(previousPage);
        footer.add(PremiumUi.caption("Page"));
        footer.add(pageField);
        footer.add(pageOf);
        footer.add(nextPage);
        gallery.add(footer, BorderLayout.SOUTH);
        views.add(gallery, "gallery");

        // ---------------------------------------------------------------- profile box (hidden until a pet is selected)
        preview = PremiumUi.card(new BorderLayout(0, 12));
        preview.setPreferredSize(new Dimension(240, 0));
        preview.setMinimumSize(new Dimension(200, 0));
        preview.add(PremiumUi.title("Pet Profile", 18), BorderLayout.NORTH);
        JPanel description = PremiumUi.clear(new BorderLayout(0, 9));
        profilePhoto.setOpaque(true);
        profilePhoto.setBackground(new Color(0xE4EFFB));
        profilePhoto.setForeground(PremiumUi.BLUE);
        profilePhoto.setFont(PremiumUi.font(22, true));
        profilePhoto.setPreferredSize(new Dimension(175, 145));
        JPanel photoBox = PremiumUi.clear(new BorderLayout(0, 3));
        photoBox.add(profilePhoto, BorderLayout.CENTER);
        photoBox.add(profilePhotoNote, BorderLayout.SOUTH);
        description.add(photoBox, BorderLayout.NORTH);
        JPanel details = PremiumUi.clear(new GridLayout(4, 1, 0, 5));
        details.add(profileName);
        details.add(profileBreed);
        details.add(profileStatus);
        details.add(profileDetails);
        description.add(details, BorderLayout.CENTER);
        JScrollPane previewScroll = new JScrollPane(description);
        previewScroll.setBorder(null);
        previewScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        previewScroll.getViewport().setOpaque(false);
        previewScroll.setOpaque(false);
        previewScroll.getVerticalScrollBar().setUnitIncrement(16);
        preview.add(previewScroll, BorderLayout.CENTER);
        JPanel actions = PremiumUi.clear(new GridLayout(staff ? 1 : 2, 1, 0, 7));
        JButton profileButton = PremiumUi.button("View pet profile", viewProfile, false);
        profileButton.setName("animalBrowseProfile");
        actions.add(profileButton);
        if (!staff) {
            JButton interest = PremiumUi.button("Record interest", recordInterest, true);
            interest.setName("animalBrowseInterest");
            actions.add(interest);
        }
        preview.add(actions, BorderLayout.SOUTH);
        preview.setVisible(false);

        JPanel body = PremiumUi.clear(new BorderLayout(13, 0));
        body.add(views, BorderLayout.CENTER);
        body.add(preview, BorderLayout.EAST);
        add(body, BorderLayout.CENTER);

        // ---------------------------------------------------------------- listeners
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        status.addActionListener(e -> {
            // Explicit filter choices take precedence over the adopter's
            // automatic Available-only initial view.
            if (!automaticStatusAdjustment) defaultAvailableFilter = false;
            applyFilters();
        });
        speciesFilter.addActionListener(e -> applyFilters());
        sorter.addRowSorterListener(e -> SwingUtilities.invokeLater(this::updateResultCount));
        table.getModel().addTableModelListener(e -> SwingUtilities.invokeLater(() -> {
            refreshSpeciesChoices();
            updateResultCount();
            if (galleryView) renderGallery();
            updateProfile();
        }));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateProfile();
                // Paint the selected card immediately, without rebuilding or reloading images.
                galleryGrid.repaint();
            }
        });

        // Show adoptable pets initially. Until the user selects a status,
        // a search may include all statuses (including adopted records).
        if (!staff) {
            defaultAvailableFilter = true;
            automaticStatusAdjustment = true;
            try { status.setSelectedItem("Available"); }
            finally { automaticStatusAdjustment = false; }
        }
        refreshSpeciesChoices();
        updateResultCount();
        setGalleryView(!staff);
    }

    // ==================================================================== views / paging

    private static JToggleButton viewToggle(String text) {
        JToggleButton button = new JToggleButton(text);
        button.setFont(PremiumUi.font(12, true));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PremiumUi.LINE, 1, true), new EmptyBorder(8, 14, 8, 14)));
        Runnable paint = () -> {
            button.setBackground(PawTrackTheme.isDark()
                    ? (button.isSelected() ? PawTrackTheme.DARK_SELECTED : PawTrackTheme.DARK_INPUT)
                    : (button.isSelected() ? PremiumUi.BLUE : Color.WHITE));
            button.setForeground(button.isSelected() ? Color.WHITE : PawTrackTheme.accent());
        };
        button.addItemListener(e -> paint.run());
        paint.run();
        return button;
    }

    private void setGalleryView(boolean enabled) {
        galleryView = enabled;
        galleryToggle.setSelected(enabled);
        tableToggle.setSelected(!enabled);
        viewsLayout.show(views, enabled ? "gallery" : "table");
        if (enabled) {
            updateGalleryColumns();
            renderGallery();
        }
    }

    private int pageCount() {
        return Math.max(1, (table.getRowCount() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void previousPage() {
        if (galleryPage > 0) { galleryPage--; renderGallery(); }
    }

    private void nextPage() {
        if (galleryPage + 1 < pageCount()) { galleryPage++; renderGallery(); }
    }

    private void jumpToTypedPage() {
        try {
            int wanted = Integer.parseInt(pageField.getText().trim());
            galleryPage = Math.max(0, Math.min(pageCount() - 1, wanted - 1));
        } catch (NumberFormatException ignored) {
            // keep the current page; renderGallery restores the box text
        }
        renderGallery();
    }

    /** Adjust the gallery to the actual visible width instead of forcing three wide cards. */
    private void updateGalleryColumns() {
        int width = galleryScroll.getViewport().getWidth();
        if (width <= 0) return;
        int wanted = width >= 890 ? 3 : width >= 510 ? 2 : 1;
        if (wanted == galleryColumns) return;
        galleryColumns = wanted;
        galleryGrid.setLayout(new GridLayout(0, wanted, 13, 13));
        renderGallery();
    }

    private void renderGallery() {
        galleryGrid.removeAll();
        int count = table.getRowCount();
        galleryPage = Math.min(galleryPage, pageCount() - 1);
        int start = galleryPage * PAGE_SIZE;
        int end = Math.min(count, start + PAGE_SIZE);
        for (int viewRow = start; viewRow < end; viewRow++) {
            galleryGrid.add(buildCard(viewRow));
        }
        if (start == end) galleryGrid.add(PremiumUi.caption("No animals match your search."));
        pageField.setText(String.valueOf(galleryPage + 1));
        pageOf.setText("of " + pageCount() + "  ·  " + (count == 0 ? "no results"
                : (start + 1) + "–" + end + " of " + count));
        previousPage.setEnabled(galleryPage > 0);
        nextPage.setEnabled(galleryPage + 1 < pageCount());
        galleryGrid.revalidate();
        galleryGrid.repaint();
    }

    private JPanel buildCard(int row) {
        int id = table.getValueAt(row, 0) instanceof Number n ? n.intValue() : -1;
        String name = Objects.toString(table.getValueAt(row, 1), "Pet");
        String species = Objects.toString(table.getValueAt(row, 2), "");
        String breed = Objects.toString(table.getValueAt(row, 3), "");
        String state = Objects.toString(table.getValueAt(row, 6), "");

        // Selected-card appearance is derived from JTable selection, so the
        // gallery and table always agree without maintaining a second selection.
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean selected = isGalleryCardSelected(id);
                g.setColor(new Color(36, 82, 150, 18));
                g.fillRoundRect(3, 5, Math.max(0, getWidth() - 7), Math.max(0, getHeight() - 8), 25, 25);
                g.setColor(PawTrackTheme.isDark()
                        ? (selected ? PawTrackTheme.DARK_INPUT : PawTrackTheme.DARK_SURFACE)
                        : (selected ? new Color(0xE8F2FF) : Color.WHITE));
                g.fillRoundRect(1, 1, Math.max(0, getWidth() - 7), Math.max(0, getHeight() - 9), 24, 24);
                g.setColor(PawTrackTheme.isDark()
                        ? (selected ? PawTrackTheme.DARK_ACCENT : PawTrackTheme.DARK_BORDER)
                        : (selected ? new Color(0x2867C5) : PremiumUi.LINE));
                g.setStroke(new BasicStroke(selected ? 3f : 1f));
                g.drawRoundRect(2, 2, Math.max(0, getWidth() - 9), Math.max(0, getHeight() - 11), 23, 23);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 10, 12, 10));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel image = new JLabel(species.toUpperCase(Locale.ROOT), SwingConstants.CENTER);
        image.setOpaque(true);
        image.setBackground(new Color(0xE7F0FC));
        image.setForeground(PremiumUi.BLUE);
        if (PawTrackTheme.isDark()) PawTrackThemeManager.applyCurrent(image);
        image.setFont(PremiumUi.font(20, true));
        image.setPreferredSize(new Dimension(135, CARD_IMG_H));
        loadInto(image, id, species, breed, CARD_IMG_W, CARD_IMG_H);
        card.add(image, BorderLayout.CENTER);

        JPanel words = PremiumUi.clear(new BorderLayout(0, 2));
        words.add(PremiumUi.title(name, 15), BorderLayout.NORTH);
        words.add(PremiumUi.caption(species + " · " + breed), BorderLayout.CENTER);
        JPanel pillRow = PremiumUi.clear(new FlowLayout(FlowLayout.LEFT, 0, 4));
        pillRow.add(statusPill(state));
        words.add(pillRow, BorderLayout.SOUTH);
        card.add(words, BorderLayout.SOUTH);

        java.awt.event.MouseAdapter select = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                table.setRowSelectionInterval(row, row);
            }
        };
        attachSelection(card, select);
        return card;
    }

    private boolean isGalleryCardSelected(int animalId) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= table.getRowCount()) return false;
        Object selectedId = table.getValueAt(selectedRow, 0);
        return selectedId instanceof Number number && number.intValue() == animalId;
    }

    private static void attachSelection(Component component, java.awt.event.MouseListener listener) {
        component.addMouseListener(listener);
        if (component instanceof Container container)
            for (Component child : container.getComponents()) attachSelection(child, listener);
    }

    // ==================================================================== status pill

    private static JLabel statusPill(String state) {
        JLabel pill = PremiumUi.caption(state);
        pill.setOpaque(true);
        pill.setForeground(pillText(state));
        pill.setBackground(pillFill(state));
        pill.setBorder(new EmptyBorder(4, 10, 4, 10));
        return pill;
    }

    private static Color pillFill(String state) {
        String s = state.toLowerCase(Locale.ROOT);
        if (s.contains("available")) return new Color(0xDCF5E7);   // green
        if (s.contains("pending")) return new Color(0xFFF0D9);     // amber
        if (s.contains("adopt")) return new Color(0xE9ECF1);       // grey
        return new Color(0xE4EFFB);                                // blue: intake / returned / other
    }

    private static Color pillText(String state) {
        String s = state.toLowerCase(Locale.ROOT);
        if (s.contains("available")) return new Color(0x168653);
        if (s.contains("pending")) return new Color(0xAB7414);
        if (s.contains("adopt")) return new Color(0x5B6675);
        return new Color(0x2D5DA8);
    }

    // ==================================================================== pictures

    private static Species toSpecies(String label) {
        if ("dog".equalsIgnoreCase(label)) return Species.DOG;
        if ("cat".equalsIgnoreCase(label)) return Species.CAT;
        return null;
    }

    /** Shows the species text now, swaps in the picture when it arrives (same picture everywhere). */
    private void loadInto(JLabel target, int id, String speciesLabel, String breed, int w, int h) {
        target.putClientProperty("petId", id);
        if (id <= 0) return;
        PetImageCache.get().load(id, toSpecies(speciesLabel), breed, w, h, loaded -> {
            if (!Objects.equals(target.getClientProperty("petId"), id)) return; // card/selection changed
            target.setText("");
            target.setIcon(loaded.icon());
            if (target == profilePhoto) {
                profilePhotoNote.setText(loaded.actualPhoto() ? "Shelter photo" : "Sample image, not this pet");
            }
        }, failure -> { /* keep the species text placeholder */ });
    }

    // ==================================================================== profile box

    private void updateProfile() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= table.getRowCount()) {
            profilePhoto.putClientProperty("petId", null);
            profilePhoto.setIcon(null);
            profilePhoto.setText("PET");
            preview.setVisible(false);
            revalidate();
            return;
        }
        int id = ((Number) table.getValueAt(row, 0)).intValue();
        String species = Objects.toString(table.getValueAt(row, 2), "Pet");
        String state = Objects.toString(table.getValueAt(row, 6), "");
        profileName.setText(Objects.toString(table.getValueAt(row, 1), "Pet"));
        profileBreed.setText(species + " · " + table.getValueAt(row, 3));
        profileStatus.setText("Status: " + state);
        profileStatus.setForeground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_ACCENT : pillText(state));
        profileDetails.setText("Age: " + table.getValueAt(row, 4) + " months · Queue: " + table.getValueAt(row, 8));
        profilePhoto.setIcon(null);
        profilePhoto.setText(species.toUpperCase(Locale.ROOT));
        profilePhotoNote.setText(" ");
        preview.setVisible(true);
        loadInto(profilePhoto, id, species, Objects.toString(table.getValueAt(row, 3), ""),
                PROFILE_IMG_W, PROFILE_IMG_H);
        revalidate();
    }

    // ==================================================================== filters

    /** Fills the species box from the data so new species appear without code changes. */
    private void refreshSpeciesChoices() {
        Set<String> found = new LinkedHashSet<>();
        TableModel model = table.getModel();
        for (int r = 0; r < model.getRowCount(); r++) {
            String s = Objects.toString(model.getValueAt(r, 2), "").trim();
            if (!s.isEmpty()) found.add(s);
        }
        List<String> wanted = new ArrayList<>();
        wanted.add(ALL_SPECIES);
        wanted.addAll(found);
        int existing = speciesFilter.getItemCount();
        boolean same = existing == wanted.size();
        for (int i = 0; same && i < existing; i++) same = wanted.get(i).equals(speciesFilter.getItemAt(i));
        if (same) return;
        Object selected = speciesFilter.getSelectedItem();
        ActionListener[] listeners = speciesFilter.getActionListeners();
        for (ActionListener l : listeners) speciesFilter.removeActionListener(l);
        speciesFilter.removeAllItems();
        for (String s : wanted) speciesFilter.addItem(s);
        speciesFilter.setSelectedItem(wanted.contains(selected) ? selected : ALL_SPECIES);
        for (ActionListener l : listeners) speciesFilter.addActionListener(l);
    }

    private void applyFilters() {
        if (defaultAvailableFilter && !automaticStatusAdjustment) {
            String desired = search.getText().trim().isEmpty() ? "Available" : ALL_STATUSES;
            if (!desired.equals(status.getSelectedItem())) {
                automaticStatusAdjustment = true;
                try { status.setSelectedItem(desired); }
                finally { automaticStatusAdjustment = false; }
            }
        }
        List<RowFilter<TableModel, Integer>> all = new ArrayList<>();
        String query = search.getText().trim();
        if (!query.isEmpty()) all.add(RowFilter.regexFilter("(?i)" + Pattern.quote(query)));
        String selectedStatus = Objects.toString(status.getSelectedItem(), ALL_STATUSES);
        if (!ALL_STATUSES.equals(selectedStatus)) {
            all.add(new RowFilter<>() {
                @Override public boolean include(Entry<? extends TableModel, ? extends Integer> row) {
                    return row.getStringValue(6).equalsIgnoreCase(selectedStatus);
                }
            });
        }
        String selectedSpecies = Objects.toString(speciesFilter.getSelectedItem(), ALL_SPECIES);
        if (!ALL_SPECIES.equals(selectedSpecies)) {
            all.add(new RowFilter<>() {
                @Override public boolean include(Entry<? extends TableModel, ? extends Integer> row) {
                    return row.getStringValue(2).equalsIgnoreCase(selectedSpecies);
                }
            });
        }
        sorter.setRowFilter(all.isEmpty() ? null : RowFilter.andFilter(all));
        galleryPage = 0;
        updateResultCount();
        if (galleryView) renderGallery();
    }

    private void updateResultCount() { resultCount.setText(table.getRowCount() + " pet(s) shown"); }

    public void setSearchText(String value) { search.setText(value == null ? "" : value); }
    public void setStatusFilter(String value) { status.setSelectedItem(value); }
    public int visibleAnimalCount() { return table.getRowCount(); }
    public JTable getAnimalTable() { return table; }
}
