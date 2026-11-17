package pawtrack.ui;

import pawtrack.api.HttpJson;
import pawtrack.api.PetImageResult;
import pawtrack.api.PetImageService;
import pawtrack.api.PetImageServiceFactory;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dashboard presentation only. All counts and lists come from MainFrame's
 * existing snapshot/role-specific callbacks. No database operations or
 * authorization decisions are performed by this view.
 *
 * The team-approved blue/white pet aesthetic is implemented using Java2D;
 * there are no external icon-font or look-and-feel dependencies.
 *
 * Motion (all cheap, all stop when the page is not showing): metric numbers
 * count up, chart bars grow, cards respond to hover, and the hero artwork
 * floats a few hearts.
 */
public final class PawTrackDashboard extends JPanel {
    private static final Color NAVY = new Color(0x172E55);
    private static final Color BLUE = new Color(0x2F6CD4);
    private static final Color MUTED = new Color(0x5B7092);
    private static final Color CANVAS = new Color(0xF2F6FE);
    private static final Color LINE = new Color(0xDBE5F5);
    private static final Color LINE_HOVER = new Color(0xB5CEF5);
    private static final Color PINK = new Color(0xE95281);
    private static final Color PURPLE = new Color(0x7971D4);
    private static final Color GREEN = new Color(0x2FA981);
    private static final Color AMBER = new Color(0xB07518);

    private final boolean staff;
    private final JLabel[] values = new JLabel[4];
    private final String[] targets = {"\u2014", "\u2014", "\u2014", "\u2014"};
    private final String[] metricSuffixes = {"", "", "", ""};
    private final int[] targetNumbers = new int[4];
    private final boolean[] metricLoaded = new boolean[4];
    private final int[] shown = new int[4];
    private final Timer[] counters = new Timer[4];
    private final JLabel heroHeading = text("Find your next best friend", 20, true, NAVY);
    private final JLabel heroSubheading = text("Every pet deserves a loving home.", 12, false, MUTED);
    private final Pill heroChipA = new Pill("", BLUE);
    private final Pill heroChipB = new Pill("", PINK);
    private final JLabel chartFooter = text("", 12, true, BLUE);
    private final JPanel recentRows = transparent(new GridLayout(4, 1, 0, 5));
    private final StatusChart statusChart = new StatusChart();
    private final MonthlyTrendChart monthlyTrend = new MonthlyTrendChart();
    private final JComponent chart;
    private final JLabel recentCount = text("", 12, false, MUTED);
    private final JLabel overviewHint = text("Live shelter statistics", 12, false, MUTED);
    private final JLabel[] metricCaptions = new JLabel[4];
    private final JLabel[] metricNotes = new JLabel[4];
    private final JourneyStepper stepper;
    private final JScrollPane scroll;
    private Runnable showAnalytics;

    /** Original constructor, kept so existing callers and tests keep working. */
    public PawTrackDashboard(boolean staff, String username, Runnable showAnimals,
                             Runnable showRecords, Runnable showWaitlist) {
        this(staff, username, showAnimals, showRecords, showWaitlist, null);
    }

    /**
     * @param showThird optional third shortcut: Shelter Analytics for staff, Account Settings
     *                  for adopters. When null the old (duplicate-prone) third shortcut is used.
     */
    public PawTrackDashboard(boolean staff, String username, Runnable showAnimals,
                             Runnable showRecords, Runnable showWaitlist, Runnable showThird) {
        super(new BorderLayout());
        if (showAnimals == null || showRecords == null || showWaitlist == null)
            throw new IllegalArgumentException("Navigation callbacks are required");
        this.staff = staff;
        this.chart = staff ? monthlyTrend : statusChart;
        this.stepper = new JourneyStepper(staff);
        setOpaque(true);
        setBackground(CANVAS);

        BackgroundPage page = new BackgroundPage();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(22, 28, 28, 28));
        page.add(buildHeader(username));
        page.add(Box.createVerticalStrut(16));
        page.add(buildMetrics());
        page.add(Box.createVerticalStrut(16));
        page.add(buildMainRow(showAnimals, showRecords, showWaitlist, showThird));
        page.add(Box.createVerticalStrut(16));
        page.add(buildInsights(showAnimals));
        page.add(Box.createVerticalStrut(16));
        page.add(buildJourneyStrip());

        scroll = new JScrollPane(page,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CANVAS);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        add(scroll, BorderLayout.CENTER);

        // Always open at the top: a leftover scroll offset used to cut the welcome heading in half.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing())
                SwingUtilities.invokeLater(() -> {
                    scroll.getViewport().setViewPosition(new Point(0, 0));
                    replayEntrance();
                });
        });
        renderRecent(List.of());
    }

    // ------------------------------------------------------------------ sections

    private JComponent buildHeader(String username) {
        JPanel wrapper = transparent(new BorderLayout(12, 0));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        String name = displayName(username);
        int hour = LocalDateTime.now().getHour();
        String salutation = hour < 12 ? "Good morning" : (hour < 17 ? "Good afternoon" : "Good evening");

        JPanel left = transparent(null);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel eyebrow = text(staff ? "SHELTER CONTROL CENTER" : "YOUR ADOPTION JOURNEY", 11, true, BLUE);
        JLabel title = text(staff ? salutation + ", " + name + "!" : "Welcome back, " + name + "!", 29, true, NAVY);
        JLabel sub = text(staff ? "Let's see how your shelter is doing today."
                : "A little love can make a big difference. Let's find your companion!", 13, false, MUTED);
        for (JLabel l : new JLabel[]{eyebrow, title, sub}) l.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(eyebrow);
        left.add(Box.createVerticalStrut(2));
        left.add(title);
        left.add(Box.createVerticalStrut(1));
        left.add(sub);
        wrapper.add(left, BorderLayout.CENTER);

        Pill date = new Pill(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), BLUE);
        JPanel east = transparent(new GridBagLayout());
        east.add(date);
        wrapper.add(east, BorderLayout.EAST);
        capHeight(wrapper);
        return wrapper;
    }

    private JComponent buildMetrics() {
        JPanel grid = transparent(new GridLayout(1, 4, 14, 0));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] names = staff ?
                new String[]{"Total Animals", "Total Adoptions", "Average Stay", "Pending Waitlist"} :
                new String[]{"Shelter Animals", "My Applications", "My Adoptions", "Available Pets"};
        String[] notes = staff ?
                new String[]{"In your shelter", "Completed adoption records", "Days in shelter", "Requests in queue"} :
                new String[]{"Pets in the shelter", "Your active requests", "Your happy endings", "Ready for adoption"};
        String[] icons = {"paw", staff ? "check" : "heart", "people", staff ? "heart" : "check"};
        Color[] colors = {BLUE, staff ? GREEN : PINK, PURPLE, staff ? PINK : GREEN};
        for (int i = 0; i < 4; i++) {
            SoftCard card = new SoftCard(Color.WHITE, 20).hoverable();
            card.setLayout(new BorderLayout());
            card.setBorder(new EmptyBorder(16, 16, 17, 10));
            JPanel content = transparent(new BorderLayout(9, 0));
            content.add(new PetIcon(icons[i], colors[i], 44), BorderLayout.WEST);
            JPanel info = transparent(new GridLayout(3, 1, 0, 2));
            metricCaptions[i] = text(names[i], 12, true, MUTED);
            metricCaptions[i].setToolTipText(names[i]);
            values[i] = text("\u2014", 28, true, NAVY);
            values[i].setName("dashboardMetric" + i);
            metricNotes[i] = text(notes[i], 11, false, colors[i]);
            info.add(metricCaptions[i]);
            info.add(values[i]);
            info.add(metricNotes[i]);
            content.add(info, BorderLayout.CENTER);
            card.add(content, BorderLayout.CENTER);
            grid.add(card);
        }
        grid.setPreferredSize(new Dimension(920, 121));
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 121));
        return grid;
    }

    private JComponent buildMainRow(Runnable showAnimals, Runnable showRecords, Runnable showWaitlist, Runnable showThird) {
        JPanel row = transparent(new GridBagLayout());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.fill = GridBagConstraints.BOTH;
        c.weighty = 1; c.weightx = 1.45; c.gridx = 0;
        c.insets = new Insets(0, 0, 0, 12);
        row.add(buildHero(showAnimals), c);
        c.gridx = 1; c.weightx = 1; c.insets = new Insets(0, 0, 0, 0);
        row.add(buildQuickActions(showAnimals, showRecords, showWaitlist, showThird), c);
        row.setPreferredSize(new Dimension(920, 268));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 268));
        return row;
    }

    private JComponent buildHero(Runnable showAnimals) {
        JPanel top = transparent(null);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel tag = text(staff ? "SHELTER HIGHLIGHT" : "YOUR NEXT CHAPTER AWAITS", 11, true, BLUE);
        tag.setIcon(new PetIcon(staff ? "spark" : "heart", staff ? new Color(0xF0A53A) : PINK, 18));
        tag.setIconTextGap(7);
        JPanel chips = transparent(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setBorder(new EmptyBorder(0, 0, 0, 0));
        chips.add(heroChipA);
        chips.add(heroChipB);
        for (JComponent c : new JComponent[]{tag, heroHeading, heroSubheading, chips}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(tag);
        top.add(Box.createVerticalStrut(10));
        top.add(heroHeading);
        top.add(Box.createVerticalStrut(3));
        top.add(heroSubheading);
        top.add(Box.createVerticalStrut(11));
        chips.setPreferredSize(new Dimension(10, 28));
        chips.setMinimumSize(new Dimension(10, 28));
        chips.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        top.add(chips);

        ActionButton cta = new ActionButton(staff ? "Manage shelter pets" : "Explore available pets", "paw", BLUE, true, showAnimals);
        cta.setPreferredSize(new Dimension(212, 42));
        cta.setMinimumSize(new Dimension(190, 42));
        JPanel bottom = transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.add(cta);

        JPanel textStack = transparent(new BorderLayout(0, 8));
        textStack.add(top, BorderLayout.NORTH);
        textStack.add(bottom, BorderLayout.SOUTH);
        return new HeroCard(textStack);
    }

    private JComponent buildQuickActions(Runnable showAnimals, Runnable showRecords,
                                         Runnable showWaitlist, Runnable showThird) {
        SoftCard card = new SoftCard(Color.WHITE, 20);
        card.setLayout(new BorderLayout(0, 11));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        JPanel heading = transparent(new GridLayout(2, 1, 0, 1));
        heading.add(text("Quick Actions", 18, true, NAVY));
        heading.add(text(staff ? "Your most-used shelter tasks" : "Jump straight to what matters", 12, false, MUTED));
        card.add(heading, BorderLayout.NORTH);
        JPanel actions = transparent(new GridLayout(3, 1, 0, 8));
        actions.add(new ActionButton(staff ? "Manage animals" : "Browse available pets", "paw", BLUE, false, showAnimals));
        actions.add(new ActionButton(staff ? "Review applications" : "My applications", "heart", PINK, false,
                staff ? showWaitlist : showRecords));
        if (showThird != null) {
            actions.add(new ActionButton(staff ? "Shelter analytics" : "Account settings",
                    staff ? "chart" : "gear", PURPLE, false, showThird));
        } else {
            actions.add(new ActionButton(staff ? "Open waitlist" : "Browse animals", "people", PURPLE, false,
                    staff ? showWaitlist : showAnimals));
        }
        card.add(actions, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildInsights(Runnable showAnimals) {
        JPanel row = transparent(new GridBagLayout());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.gridx = 0; c.weightx = 1.1; c.insets = new Insets(0, 0, 0, 12);
        row.add(buildRecent(showAnimals), c);
        c.gridx = 1; c.weightx = 1; c.insets = new Insets(0, 0, 0, 0);
        row.add(buildOverview(), c);
        row.setPreferredSize(new Dimension(920, 289));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 289));
        return row;
    }

    private JComponent buildRecent(Runnable showAnimals) {
        SoftCard card = new SoftCard(Color.WHITE, 20);
        card.setBorder(new EmptyBorder(18, 22, 16, 22));
        card.setLayout(new BorderLayout(0, 10));
        JPanel heading = transparent(new BorderLayout());
        JPanel caption = transparent(new GridLayout(2, 1, 0, 2));
        caption.add(text(staff ? "Recent shelter intake" : "Fresh faces at the shelter", 17, true, NAVY));
        caption.add(recentCount);
        heading.add(caption, BorderLayout.WEST);
        heading.add(linkButton("View animals", showAnimals), BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);
        card.add(recentRows, BorderLayout.CENTER);
        return card;
    }

    /** Staff-only navigation from the adoption trend to the full Analytics tab. */
    public void setAnalyticsNavigation(Runnable action) {
        this.showAnalytics = action;
    }

    private JComponent buildOverview() {
        SoftCard card = new SoftCard(Color.WHITE, 20);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(18, 22, 15, 22));
        JPanel header = transparent(new BorderLayout());
        JPanel captions = transparent(new GridLayout(2, 1, 0, 2));
        captions.add(text(staff ? "Adoption trend" : "Shelter overview", 17, true, NAVY));
        captions.add(overviewHint);
        header.add(captions, BorderLayout.WEST);
        header.add(new PetIcon("chart", BLUE, 33), BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);
        chart.setPreferredSize(new Dimension(200, 172));
        card.add(chart, BorderLayout.CENTER);
        JPanel footer = transparent(new BorderLayout());
        footer.add(text(staff ? "LAST 6 MONTHS" : "CURRENT ANIMAL STATUS", 11, true, MUTED), BorderLayout.WEST);
        footer.add(chartFooter, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        if (staff) {
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            java.awt.event.MouseAdapter openAnalytics = new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (showAnalytics != null) showAnalytics.run();
                }
            };
            // Forward clicks from child chart and labels as well as the card itself.
            attachClick(card, openAnalytics);
        }
        return card;
    }

    private static void attachClick(Component component, java.awt.event.MouseAdapter listener) {
        component.addMouseListener(listener);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) attachClick(child, listener);
        }
    }

    private JComponent buildJourneyStrip() {
        SoftCard card = new SoftCard(new Color(0xE9F1FF), 20);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(14, 22, 14, 22));
        JPanel copy = transparent(new GridLayout(2, 1, 0, 3));
        copy.add(text(staff ? "Make every adoption count" : "Your adoption journey, made simple", 16, true, NAVY));
        copy.add(text(staff ? "Keep records current and help every pet find their home."
                : "Meet a pet, join the waitlist, and follow your application.", 12, false, MUTED));
        card.add(copy, BorderLayout.CENTER);
        card.add(stepper, BorderLayout.EAST);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setPreferredSize(new Dimension(920, 86));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        return card;
    }

    // ------------------------------------------------------------------ public API (unchanged contract)

    public boolean isStaffDashboard() { return staff; }

    /** MainFrame currently calls this with (total, available, requests) for staff. */
    public void setStats(int first, int second, int third) {
        // Keep the original three-int interface for existing callers and tests.
        // For staff, these are provisional until setStaffKpis supplies the
        // authoritative adoption/average-stay/waitlist values.
        if (staff) metricSuffixes[2] = "";
        setMetric(0, first);
        setMetric(1, second);
        setMetric(2, third);
        if (!staff) stepper.setProgress(Math.max(0, third) > 0 ? 3 : (Math.max(0, second) > 0 ? 2 : 0));
    }

    public String metricText(int index) {
        if (index < 0 || index >= values.length) throw new IllegalArgumentException("Invalid metric index");
        return targets[index];
    }

    /** Works with existing DAO-backed animal snapshots (no extra DB calls). */
    public void setAnimalSnapshot(List<Animal> animals) {
        if (animals == null) animals = List.of();
        int available = 0, adopted = 0, other = 0, availDogs = 0, availCats = 0;
        Map<String, Integer> adoptedBreeds = new LinkedHashMap<>();
        for (Animal a : animals) {
            if (a.getStatus() == AnimalStatus.AVAILABLE) {
                available++;
                if (a.species() == Species.CAT) availCats++; else availDogs++;
            } else if (a.getStatus() == AnimalStatus.ADOPTED) {
                adopted++;
                adoptedBreeds.merge(a.getBreed(), 1, Integer::sum);
            } else other++;
        }
        setMetric(3, staff ? adopted : available);
        if (staff) {
            if (adoptedBreeds.isEmpty()) {
                heroHeading.setText("Celebrate every pet");
                heroSubheading.setText("Adoption insights appear as records grow.");
            } else {
                var top = adoptedBreeds.entrySet().stream()
                        .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                                .thenComparing(Map.Entry::getKey)).orElseThrow();
                heroHeading.setText(top.getKey());
                heroHeading.setToolTipText(top.getKey());
                heroSubheading.setText(top.getValue() + " adopted of this breed");
            }
            heroChipA.set(available + " available", GREEN);
            heroChipB.set(other + " in process", AMBER);
        } else {
            heroHeading.setText(available + " friends await you");
            heroSubheading.setText("Their next chapter starts with you.");
            heroChipA.set(availDogs + (availDogs == 1 ? " dog" : " dogs"), BLUE);
            heroChipB.set(availCats + (availCats == 1 ? " cat" : " cats"), PINK);
        }
        chartFooter.setText(adopted + (staff ? " adopted animals" : " adopted"));
        statusChart.setCounts(available, adopted, other);
        List<Animal> latest = new ArrayList<>(animals);
        latest.sort(Comparator.comparing(Animal::getIntakeDate).reversed()
                .thenComparing(Animal::getId, Comparator.reverseOrder()));
        renderRecent(latest.subList(0, Math.min(4, latest.size())));
    }

    /** Optional richer KPIs when analytics are wired in by MainFrame. */
    public void setStaffKpis(int adoptions, double averageStayDays, int pendingWaitlist) {
        if (!staff) return;
        // MainFrame provides real adoption records and average shelter stay here.
        // The primary KPI cards must display those values, not the provisional
        // 'available animals' and 'pending requests' integers from setStats.
        setMetric(1, adoptions);
        metricSuffixes[2] = " days";
        setMetric(2, Double.isFinite(averageStayDays)
                ? (int) Math.round(Math.max(0, averageStayDays)) : 0);
        metricNotes[2].setText(Double.isFinite(averageStayDays)
                ? "Average shelter stay" : "Stay data unavailable");
        setMetric(3, pendingWaitlist);
        overviewHint.setText("Average stay " + (Double.isFinite(averageStayDays)
                ? Math.round(Math.max(0, averageStayDays)) + " days" : "not available"));
    }

    public void setMonthlyAdoptions(List<AdoptionRecord> records) {
        if (!staff) return;
        List<AdoptionRecord> safe = records == null ? List.of() : records;
        monthlyTrend.setRecords(safe);
        YearMonth now = YearMonth.now();
        int current = 0, previous = 0;
        for (AdoptionRecord r : safe) {
            if (r.getAdoptionDate() == null) continue;
            long d = java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(r.getAdoptionDate()), now);
            if (d >= 0 && d < 6) current++;
            else if (d >= 6 && d < 12) previous++;
        }
        if (previous > 0) {
            long growth = Math.round((current - previous) * 100.0 / previous);
            chartFooter.setText(String.format("%+d%% vs previous", growth));
        } else chartFooter.setText(current + " in last 6 months");
    }

    // ------------------------------------------------------------------ internals

    /** Short entrance animation each time the Dashboard page is shown: numbers count up, bars grow. */
    private void replayEntrance() {
        if (GraphicsEnvironment.isHeadless() || !isShowing()) return;
        for (int i = 0; i < values.length; i++) {
            if (!metricLoaded[i]) continue;
            int target = targetNumbers[i];
            shown[i] = 0;
            values[i].setText("0" + metricSuffixes[i]);
            setMetric(i, target);
        }
        statusChart.replay();
    }

    /** Sets the label immediately when hidden/headless, otherwise counts up with an ease-out curve. */
    private void setMetric(int index, int rawTarget) {
        final int target = Math.max(0, rawTarget);
        metricLoaded[index] = true;
        targetNumbers[index] = target;
        targets[index] = target + metricSuffixes[index];
        if (counters[index] != null) counters[index].stop();
        final int from = shown[index];
        if (from == target || !isShowing() || GraphicsEnvironment.isHeadless()) {
            shown[index] = target;
            values[index].setText(targets[index]);
            return;
        }
        final long start = System.nanoTime();
        final int durationMs = 650;
        Timer timer = new Timer(16, null);
        timer.addActionListener(e -> {
            double t = Math.min(1.0, (System.nanoTime() - start) / 1e6 / durationMs);
            double eased = 1 - Math.pow(1 - t, 3);
            shown[index] = (int) Math.round(from + (target - from) * eased);
            values[index].setText(shown[index] + metricSuffixes[index]);
            if (t >= 1.0) timer.stop();
        });
        counters[index] = timer;
        timer.start();
    }

    private void renderRecent(List<Animal> latest) {
        recentRows.removeAll();
        recentCount.setText(latest.isEmpty() ? "No animals to show yet" : "The latest arrivals and updates");
        int i = 0;
        for (Animal animal : latest) {
            JPanel row = transparent(new BorderLayout(12, 0));
            row.setBorder(new EmptyBorder(2, 0, 2, 2));
            row.add(new PetThumb(animal), BorderLayout.WEST);
            JPanel details = transparent(new GridLayout(2, 1, 0, 2));
            String firstLine = animal.getName() + "  \u00B7  " + animal.getBreed();
            JLabel title = text(firstLine, 13, true, NAVY);
            title.setToolTipText(firstLine);
            details.add(title);
            details.add(text(animal.species().label() + "  \u00B7  Intake "
                    + animal.getIntakeDate().format(DateTimeFormatter.ISO_LOCAL_DATE), 11, false, MUTED));
            row.add(details, BorderLayout.CENTER);
            JPanel chipHolder = transparent(new GridBagLayout());
            chipHolder.add(statusChip(animal.getStatus()));
            row.add(chipHolder, BorderLayout.EAST);
            recentRows.add(row);
            i++;
        }
        while (i++ < 4) {
            JPanel empty = transparent(new BorderLayout());
            empty.add(text(latest.isEmpty() && i == 2 ? "New arrivals will appear here" : "", 12, false, MUTED), BorderLayout.CENTER);
            recentRows.add(empty);
        }
        // Refresh results can arrive after the original theme traversal.
        // Capture each new label's light colors before theming it, so Light
        // Mode restores correctly rather than retaining navy-on-navy text.
        if (PawTrackTheme.isDark()) PawTrackThemeManager.applyCurrent(recentRows);
        recentRows.revalidate();
        recentRows.repaint();
    }

    private static Pill statusChip(AnimalStatus status) {
        String title = status.name().replace('_', ' ');
        Color color = switch (status) {
            case AVAILABLE -> GREEN;
            case ADOPTED -> BLUE;
            case PENDING -> AMBER;
            case INTAKE -> PURPLE;
            default -> MUTED;
        };
        Pill chip = new Pill(title, color);
        chip.setFont(font(10, true));
        chip.setToolTipText(status.label());
        return chip;
    }

    static String displayName(String username) {
        if (username == null || username.isBlank()) return "there";
        String[] words = username.trim().replace('_', ' ').replace('.', ' ').split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return out.length() == 0 ? "there" : out.toString();
    }

    private static void capHeight(JComponent c) {
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
    }
    private static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }
    private static Font font(int size, boolean bold) {
        return new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size);
    }
    private static JLabel text(String value, int size, boolean bold, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(font(size, bold));
        label.setForeground(color);
        return label;
    }
    private static JButton linkButton(String title, Runnable action) {
        JButton button = new JButton(title + "  \u203A");
        button.setFont(font(12, true));
        button.setForeground(BLUE);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }
    private static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
    private static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
    private static float easeOut(float t) { float u = 1 - Math.min(1f, Math.max(0f, t)); return 1 - u * u * u; }

    // ------------------------------------------------------------------ components

    /** Page background that tracks the viewport width, so a narrow window never shows a horizontal scrollbar. */
    private static final class BackgroundPage extends JPanel implements Scrollable {
        BackgroundPage() { setOpaque(true); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            gg.setPaint(new GradientPaint(0, 0, PawTrackTheme.isDark() ? PawTrackTheme.DARK_CANVAS : new Color(0xF2F7FF), getWidth(), getHeight(), PawTrackTheme.isDark() ? new Color(0x172A42) : new Color(0xEEF3FF)));
            gg.fillRect(0, 0, getWidth(), getHeight());
            gg.setColor(new Color(109, 160, 241, 22));
            gg.fillOval(getWidth() - 240, -110, 280, 240);
            gg.fillOval(-160, getHeight() - 80, 300, 160);
            gg.dispose();
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 22; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(40, r.height - 60); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport vp && vp.getHeight() > getPreferredSize().height;
        }
    }

    /** Rounded card with a soft, even shadow. Optional hover glow. */
    private static class SoftCard extends JPanel {
        private final Color fill;
        private final int radius;
        private float hover;
        private Timer hoverTimer;
        SoftCard(Color color, int radius) { this.fill = color; this.radius = radius; setOpaque(false); }

        SoftCard hoverable() {
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { animateHover(1f); }
                @Override public void mouseExited(MouseEvent e) {
                    if (getMousePosition(true) == null) animateHover(0f);
                }
            });
            return this;
        }
        private void animateHover(float goal) {
            if (hoverTimer != null) hoverTimer.stop();
            hoverTimer = new Timer(16, null);
            hoverTimer.addActionListener(e -> {
                hover += (goal - hover) * 0.28f;
                if (Math.abs(goal - hover) < 0.02f) { hover = goal; hoverTimer.stop(); }
                repaint();
            });
            hoverTimer.start();
        }
        int bodyW() { return Math.max(3, getWidth() - 6); }
        int bodyH() { return Math.max(3, getHeight() - 8); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            int bw = bodyW(), bh = bodyH();
            int alpha = Math.round(5 + 5 * hover);
            for (int k = 3; k >= 0; k--) {
                gg.setColor(new Color(32, 63, 133, alpha));
                gg.fillRoundRect(3 - k, 4 - k, bw + 2 * k, bh + 2 * k, radius + 2 * k, radius + 2 * k);
            }
            gg.setColor(PawTrackTheme.fill(fill));
            gg.fillRoundRect(3, 2, bw, bh, radius, radius);
            gg.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_BORDER : mix(LINE, LINE_HOVER, hover));
            gg.drawRoundRect(3, 2, bw - 1, bh - 1, radius, radius);
            gg.dispose();
            super.paintComponent(g);
        }
    }

    /** Hero: copy on the left, dog and cat on the right with a few gently floating hearts. */
    private static final class HeroCard extends SoftCard {
        private static BufferedImage image;
        private final JComponent content;
        private final Timer animation;
        private final long born = System.nanoTime();

        HeroCard(JComponent content) {
            super(new Color(0xE8F1FF), 22);
            this.content = content;
            setLayout(null);
            setPreferredSize(new Dimension(300, 200));
            add(content);
            animation = new Timer(40, e -> repaint(artBounds()));
            addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) animation.start(); else animation.stop();
                }
            });
        }
        private static synchronized BufferedImage load() {
            if (image != null) return image;
            try (InputStream in = HeroCard.class.getResourceAsStream("/pawtrack-login-art.jpg")) {
                if (in != null) image = ImageIO.read(in);
            } catch (Exception ignored) { }
            return image;
        }
        @Override public void doLayout() {
            int pad = 22;
            int textW = (int) (bodyW() * 0.53) - pad;
            content.setBounds(3 + pad, 2 + 20, Math.max(80, textW), bodyH() - 38);
        }
        private Rectangle artBounds() {
            int bw = bodyW(), bh = bodyH();
            int dw = Math.min(330, Math.max(110, (int) (bw * 0.42f)));
            int dh = (int) (bh * 0.80);
            return new Rectangle(3 + bw - dw - 10, 2 + bh - dh - 8, dw, dh);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            int bw = bodyW(), bh = bodyH();
            Shape old = gg.getClip();
            gg.clip(new RoundRectangle2D.Float(3, 2, bw, bh, 22, 22));
            gg.setPaint(new GradientPaint(3 + bw * .46f, 2, PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xDDEAFF), 3 + bw, 2 + bh, PawTrackTheme.isDark() ? new Color(0x294869) : new Color(0xBFD9FF)));
            gg.fill(new Ellipse2D.Float(3 + bw * .52f, 2 - bh * .25f, bw * .77f, bh * 1.5f));
            gg.setColor(new Color(255, 255, 255, 65));
            gg.fillOval((int) (3 + bw * .45), (int) (2 + bh * .38), (int) (bw * .75), (int) (bh * .88));

            BufferedImage art = load();
            Rectangle r = artBounds();
            if (art != null) {
                // Crop to the destination's aspect ratio so the pets are never squashed.
                double aspect = r.width / (double) r.height;
                int bandTop = (int) (art.getHeight() * 0.40), bandH = (int) (art.getHeight() * 0.48);
                int cropW = (int) Math.min(art.getWidth(), bandH * aspect);
                int cropH = (int) Math.min(bandH, cropW / aspect);
                int cx = (int) (art.getWidth() * 0.52);
                int sx1 = Math.max(0, Math.min(art.getWidth() - cropW, cx - cropW / 2));
                int sy1 = bandTop + (bandH - cropH) / 2;
                gg.clip(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 20, 20));
                gg.drawImage(art, r.x, r.y, r.x + r.width, r.y + r.height, sx1, sy1, sx1 + cropW, sy1 + cropH, null);
                gg.setClip(old);
                gg.clip(new RoundRectangle2D.Float(3, 2, bw, bh, 22, 22));
            }
            double t = (System.nanoTime() - born) / 1e9;
            drawFloatingHeart(gg, r.x + r.width * 0.60, r.y + r.height * 0.36, t, 0.0, PINK, 8);
            drawFloatingHeart(gg, r.x + r.width * 0.90, r.y + r.height * 0.32, t, 1.7, new Color(0x4C82E8), 7);
            gg.setClip(old);
            gg.dispose();
        }
        private static void drawFloatingHeart(Graphics2D g, double x, double y, double t, double offset, Color c, double size) {
            double cycle = 3.6;
            double p = (((t + offset) % cycle) + cycle) % cycle / cycle;
            float fade = (float) Math.sin(Math.PI * p);
            if (fade <= 0.02f) return;
            double hx = x + Math.sin((t + offset) * 1.7) * 4;
            double hy = y - p * 34;
            double s = size * (0.8 + 0.25 * Math.sin(p * Math.PI));
            Path2D.Double heart = new Path2D.Double();
            heart.moveTo(0, s * 0.9);
            heart.curveTo(-s * 1.3, -s * 0.1, -s * 0.5, -s * 1.1, 0, -s * 0.35);
            heart.curveTo(s * 0.5, -s * 1.1, s * 1.3, -s * 0.1, 0, s * 0.9);
            AffineTransform at = AffineTransform.getTranslateInstance(hx, hy);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(210 * fade)));
            g.fill(at.createTransformedShape(heart));
        }
    }

    /** Native JButton (keyboard, accessibility, tests) with a custom painted pill look. */
    private static final class ActionButton extends JButton {
        private final String icon;
        private final Color accent;
        private final boolean primary;
        ActionButton(String name, String icon, Color accent, boolean primary, Runnable callback) {
            super(name);
            this.icon = icon; this.accent = accent; this.primary = primary;
            setOpaque(false); setContentAreaFilled(false); setFocusPainted(false);
            setBorderPainted(false); setBorder(new EmptyBorder(0, 0, 0, 0));
            setForeground(primary ? Color.WHITE : NAVY);
            setFont(font(primary ? 12 : 13, true)); setHorizontalAlignment(SwingConstants.LEFT);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> callback.run());
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            boolean over = getModel().isRollover(), down = getModel().isPressed();
            int w = getWidth() - 3, h = getHeight() - 4;
            int shift = down ? 1 : 0;
            gg.setColor(new Color(30, 54, 109, over ? 34 : 20));
            gg.fillRoundRect(2, 3, w, h, 17, 17);
            Color fill = primary ? (over ? new Color(0x25498A) : new Color(0x1C3D73))
                    : (over ? new Color(0xE8F1FF) : new Color(0xF7FAFF));
            gg.setColor(PawTrackTheme.fill(fill));
            gg.fillRoundRect(0, shift, w, h, 17, 17);
            if (!primary) {
                gg.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_BORDER : (over ? LINE_HOVER : new Color(0xD8E5FA)));
                gg.drawRoundRect(0, shift, w - 1, h - 1, 17, 17);
            }
            new PetIcon(icon, accent, 23).paintSimple(gg, 13, shift + Math.max(8, (h - 23) / 2), 23, primary);
            gg.setFont(getFont());
            gg.setColor(primary ? Color.WHITE : PawTrackTheme.text());
            FontMetrics fm = gg.getFontMetrics();
            gg.drawString(getText(), 48, shift + (h - fm.getHeight()) / 2 + fm.getAscent());
            gg.setColor(primary ? new Color(0xC7D9FF) : BLUE);
            gg.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int arrowX = w - 17 + (over ? 3 : 0), cy = shift + h / 2;
            gg.drawLine(arrowX - 4, cy - 5, arrowX + 1, cy);
            gg.drawLine(arrowX + 1, cy, arrowX - 4, cy + 5);
            gg.dispose();
        }
    }

    /** Small rounded label: status chips, date, hero chips. */
    private static final class Pill extends JLabel {
        private Color tint;
        Pill(String text, Color fg) {
            super(text);
            setFont(font(11, true));
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(5, 11, 5, 11));
            set(text, fg);
        }
        void set(String text, Color fg) {
            setText(text);
            setForeground(fg);
            tint = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 26);
            setVisible(text != null && !text.isEmpty());
            repaint();
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            gg.setColor(PawTrackTheme.fill(tint));
            gg.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            gg.dispose();
            super.paintComponent(g);
        }
    }

    /** Three-step adoption journey. For adopters the steps reflect their real applications and adoptions. */
    private static final class JourneyStepper extends JComponent {
        private final boolean decorative;
        private final String[] labels;
        private final Color[] colors = {BLUE, PINK, GREEN};
        private int progress;

        JourneyStepper(boolean staff) {
            this.decorative = staff;
            this.labels = staff ? new String[]{"Intake", "Review", "Place"} : new String[]{"Explore", "Apply", "Adopt"};
            setPreferredSize(new Dimension(366, 48));
            setOpaque(false);
        }
        void setProgress(int p) { progress = Math.max(0, Math.min(3, p)); repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            int stepW = getWidth() / 3, d = 30, cy = getHeight() / 2;
            for (int i = 0; i < 3; i++) {
                int x0 = i * stepW + 2;
                boolean done = decorative || i < progress;
                boolean current = !decorative && i == progress;
                Color c = colors[i];
                gg.setFont(font(12, true));
                FontMetrics fm = gg.getFontMetrics();
                int labelX = x0 + d + 9, labelW = fm.stringWidth(labels[i]);
                if (i < 2) {
                    gg.setColor(done && !decorative ? c : new Color(0xC9D8F0));
                    gg.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int lx1 = labelX + labelW + 8, lx2 = (i + 1) * stepW - 6;
                    if (lx2 > lx1) gg.drawLine(lx1, cy, lx2, cy);
                }
                if (done) {
                    gg.setColor(c);
                    gg.fillOval(x0, cy - d / 2, d, d);
                    gg.setColor(Color.WHITE);
                    if (decorative) {
                        gg.setFont(font(12, true));
                        String n = Integer.toString(i + 1);
                        FontMetrics f2 = gg.getFontMetrics();
                        gg.drawString(n, x0 + d / 2 - f2.stringWidth(n) / 2, cy + f2.getAscent() / 2 - 1);
                    } else {
                        gg.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        gg.drawLine(x0 + 9, cy, x0 + 13, cy + 4);
                        gg.drawLine(x0 + 13, cy + 4, x0 + 21, cy - 5);
                    }
                } else {
                    gg.setColor(current ? Color.WHITE : new Color(0xE3ECFA));
                    gg.fillOval(x0, cy - d / 2, d, d);
                    if (current) {
                        gg.setColor(c);
                        gg.setStroke(new BasicStroke(2.6f));
                        gg.drawOval(x0 + 1, cy - d / 2 + 1, d - 2, d - 2);
                    }
                    gg.setColor(current ? c : MUTED);
                    gg.setFont(font(12, true));
                    String n = Integer.toString(i + 1);
                    FontMetrics f2 = gg.getFontMetrics();
                    gg.drawString(n, x0 + d / 2 - f2.stringWidth(n) / 2, cy + f2.getAscent() / 2 - 1);
                }
                gg.setFont(font(12, true));
                gg.setColor(done || current ? PawTrackTheme.text() : PawTrackTheme.muted());
                gg.drawString(labels[i], labelX, cy + fm.getAscent() / 2 - 1);
            }
            gg.dispose();
        }
    }

    private static final class PetIcon extends JComponent implements Icon {
        private final String type;
        private final Color color;
        private final int size;
        PetIcon(String type, Color color, int size) {
            this.type = type; this.color = color; this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setOpaque(false);
        }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
        @Override protected void paintComponent(Graphics g) { paintIcon(this, g, 0, 0); }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            gg.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 26));
            gg.fillRoundRect(x, y, size, size, size / 3, size / 3);
            gg.dispose();
            paintSimple((Graphics2D) g, x, y, size, false);
        }
        void paintSimple(Graphics2D g, int x, int y, int size, boolean white) {
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            gg.translate(x, y);
            gg.scale(size / 48.0, size / 48.0);
            gg.setColor(white ? Color.WHITE : color);
            if ("heart".equals(type)) {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(24, 39);
                p.curveTo(8, 27, 6, 19, 13, 13); p.curveTo(19, 8, 24, 13, 24, 18);
                p.curveTo(25, 11, 34, 8, 39, 14); p.curveTo(44, 20, 41, 27, 24, 39);
                gg.fill(p);
            } else if ("people".equals(type)) {
                gg.fillOval(17, 9, 14, 14); gg.fillRoundRect(12, 26, 24, 14, 12, 12);
                gg.fillOval(4, 17, 9, 9); gg.fillOval(35, 17, 9, 9);
                gg.fillRoundRect(2, 29, 10, 8, 5, 5); gg.fillRoundRect(36, 29, 10, 8, 5, 5);
            } else if ("check".equals(type)) {
                gg.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gg.drawLine(11, 25, 20, 34); gg.drawLine(20, 34, 40, 13);
            } else if ("spark".equals(type)) {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(25, 2); p.lineTo(29, 18); p.lineTo(45, 23); p.lineTo(29, 28); p.lineTo(25, 45);
                p.lineTo(20, 29); p.lineTo(3, 23); p.lineTo(20, 18); p.closePath();
                gg.fill(p);
            } else if ("chart".equals(type)) {
                gg.fillRoundRect(10, 23, 7, 17, 4, 4); gg.fillRoundRect(21, 14, 7, 26, 4, 4);
                gg.fillRoundRect(32, 6, 7, 34, 4, 4);
            } else if ("gear".equals(type)) {
                for (int k = 0; k < 8; k++) {
                    AffineTransform keep = gg.getTransform();
                    gg.rotate(Math.toRadians(k * 45), 24, 24);
                    gg.fillRoundRect(20, 5, 8, 10, 3, 3);
                    gg.setTransform(keep);
                }
                gg.setStroke(new BasicStroke(6f));
                gg.drawOval(14, 14, 20, 20);
            } else {
                gg.fillOval(13, 21, 22, 17); gg.fillOval(6, 12, 10, 12);
                gg.fillOval(18, 5, 10, 12); gg.fillOval(31, 10, 10, 12); gg.fillOval(38, 19, 7, 9);
            }
            gg.dispose();
        }
    }

    /**
     * Pet thumbnail. Order of preference: a photo uploaded to the local photo store,
     * then a sample picture from the Dog CEO / TheCatAPI service the app already uses
     * (cached per animal, fetched off the Event Dispatch Thread), then a painted paw.
     * API pictures are samples, not photographs of the actual shelter animal.
     */
    private static final class PetThumb extends JComponent {
        private static final Map<Integer, BufferedImage> SAMPLE_CACHE = new ConcurrentHashMap<>();
        private static final Map<Integer, Long> SAMPLE_FAILED = new ConcurrentHashMap<>();
        private static final long RETRY_AFTER_MS = 5 * 60_000L;
        private static final int THUMB = 44;

        private final Animal animal;
        private BufferedImage photo;
        private boolean sample;

        PetThumb(Animal animal) {
            this.animal = animal;
            setPreferredSize(new Dimension(THUMB, THUMB));
            setMinimumSize(new Dimension(THUMB, THUMB));
            setToolTipText(animal.getName() + " \u2014 " + animal.getBreed());
            setOpaque(false);
            BufferedImage cached = SAMPLE_CACHE.get(animal.getId());
            if (cached != null) { photo = cached; sample = true; }
            // Never decode or download photos on Swing's Event Dispatch Thread.
            new SwingWorker<BufferedImage, Void>() {
                private boolean fromSample;
                @Override protected BufferedImage doInBackground() {
                    try {
                        List<Path> photos = pawtrack.service.PetPhotoStore.defaultStore().list(animal.getId());
                        if (!photos.isEmpty()) return ImageIO.read(photos.get(0).toFile());
                    } catch (Exception ignored) { }
                    BufferedImage have = SAMPLE_CACHE.get(animal.getId());
                    if (have != null) { fromSample = true; return have; }
                    BufferedImage fetched = fetchSample();
                    fromSample = fetched != null;
                    return fetched;
                }
                @Override protected void done() {
                    try {
                        BufferedImage result = get();
                        if (result != null) { photo = result; sample = fromSample; repaint(); }
                    } catch (Exception ignored) { }
                }
            }.execute();
        }

        private BufferedImage fetchSample() {
            if (GraphicsEnvironment.isHeadless() || Boolean.getBoolean("pawtrack.offline")) return null;
            Long failedAt = SAMPLE_FAILED.get(animal.getId());
            if (failedAt != null && System.currentTimeMillis() - failedAt < RETRY_AFTER_MS) return null;
            try {
                PetImageService service = PetImageServiceFactory.forSpecies(animal.species());
                PetImageResult info;
                try {
                    info = service.fetchRandomImage(animal.getBreed());
                } catch (java.io.IOException unknownBreed) {
                    info = service.fetchRandomImage(null);
                }
                BufferedImage full = ImageIO.read(new ByteArrayInputStream(HttpJson.getBytes(info.imageUrl())));
                if (full == null) throw new java.io.IOException("Unsupported image format");
                BufferedImage small = coverScale(full, THUMB * 2);
                SAMPLE_CACHE.put(animal.getId(), small);
                return small;
            } catch (Exception offlineOrBadImage) {
                SAMPLE_FAILED.put(animal.getId(), System.currentTimeMillis());
                return null;
            }
        }

        private static BufferedImage coverScale(BufferedImage src, int side) {
            double scale = Math.max(side / (double) src.getWidth(), side / (double) src.getHeight());
            int dw = (int) Math.ceil(src.getWidth() * scale), dh = (int) Math.ceil(src.getHeight() * scale);
            BufferedImage out = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(src, (side - dw) / 2, (side - dh) / 2, dw, dh, null);
            g.dispose();
            return out;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            int w = getWidth(), h = getHeight();
            Shape shape = new RoundRectangle2D.Float(0, 0, w, h, 14, 14);
            gg.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xE3EDFC));
            gg.fill(shape);
            gg.setClip(shape);
            if (photo != null) {
                double scale = Math.max(w / (double) photo.getWidth(), h / (double) photo.getHeight());
                int dw = (int) Math.ceil(photo.getWidth() * scale), dh = (int) Math.ceil(photo.getHeight() * scale);
                gg.drawImage(photo, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            } else {
                new PetIcon("paw", animal.species() == Species.CAT ? PINK : BLUE, 36).paintSimple(gg, (w - 36) / 2, (h - 36) / 2, 36, false);
            }
            gg.dispose();
        }
        @Override public String getToolTipText() {
            return sample ? animal.getName() + " \u2014 sample photo, not this animal"
                    : animal.getName() + " \u2014 " + animal.getBreed();
        }
    }

    /** Status counts from the user's SQLite-backed animal list. Bars grow in when the page is shown. */
    private static final class StatusChart extends JComponent {
        private final int[] counts = new int[3];
        private float progress = 1f;
        private Timer grow;
        void setCounts(int a, int b, int c) {
            boolean changed = counts[0] != a || counts[1] != b || counts[2] != c;
            counts[0] = a; counts[1] = b; counts[2] = c;
            if (!changed || !isShowing() || GraphicsEnvironment.isHeadless()) { progress = 1f; repaint(); return; }
            startGrow();
        }
        void replay() {
            if (counts[0] + counts[1] + counts[2] == 0 || !isShowing() || GraphicsEnvironment.isHeadless()) return;
            startGrow();
        }
        private void startGrow() {
            if (grow != null) grow.stop();
            progress = 0f;
            final long start = System.nanoTime();
            grow = new Timer(16, null);
            grow.addActionListener(e -> {
                progress = Math.min(1f, (System.nanoTime() - start) / 1e6f / 750f);
                repaint();
                if (progress >= 1f) grow.stop();
            });
            grow.start();
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D gg = (Graphics2D) g.create();
            antialias(gg);
            int w = getWidth(), h = getHeight();
            int max = Math.max(1, Math.max(counts[0], Math.max(counts[1], counts[2])));
            String[] titles = {"Available", "Adopted", "In process"};
            Color[] colors = {new Color(0x4B83E5), new Color(0x4DB596), new Color(0x9382E0)};
            int baseline = h - 30, top = 24, trackH = baseline - top;
            float ease = easeOut(progress);
            for (int i = 0; i < 3; i++) {
                int x = (2 * i + 1) * w / 6;
                int bh = Math.max(4, Math.round(trackH * (counts[i] / (float) max) * ease));
                gg.setColor((PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xEDF2FB)));
                gg.fillRoundRect(x - 25, top, 50, trackH, 14, 14);
                gg.setPaint(new GradientPaint(0, baseline - bh, mix(colors[i], Color.WHITE, .28f), 0, baseline, colors[i]));
                gg.fillRoundRect(x - 25, baseline - bh, 50, bh, 14, 14);
                gg.setFont(font(12, true));
                gg.setColor(PawTrackTheme.text());
                String number = Integer.toString(Math.round(counts[i] * ease));
                FontMetrics fm = gg.getFontMetrics();
                gg.drawString(number, x - fm.stringWidth(number) / 2, Math.max(15, baseline - bh - 6));
                gg.setFont(font(11, false));
                gg.setColor(PawTrackTheme.muted());
                fm = gg.getFontMetrics();
                gg.drawString(titles[i], x - fm.stringWidth(titles[i]) / 2, h - 9);
            }
            gg.dispose();
        }
    }

    private static final class MonthlyTrendChart extends JComponent {
        private final int[] values = new int[6];
        private final String[] months = new String[6];
        MonthlyTrendChart() { setRecords(List.of()); }
        void setRecords(List<AdoptionRecord> records) {
            YearMonth end = YearMonth.now();
            for (int i = 0; i < 6; i++) {
                months[i] = end.minusMonths(5 - i).format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH));
                values[i] = 0;
            }
            for (AdoptionRecord r : records) {
                if (r.getAdoptionDate() == null) continue;
                int x = (int) java.time.temporal.ChronoUnit.MONTHS.between(end.minusMonths(5), YearMonth.from(r.getAdoptionDate()));
                if (x >= 0 && x < 6) values[x]++;
            }
            repaint();
        }
        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            antialias(g);
            int l = 34, r = getWidth() - 17, t = 14, b = getHeight() - 25;
            if (r <= l + 30 || b <= t + 30) { g.dispose(); return; }
            int max = 4;
            for (int v : values) max = Math.max(max, v);
            max = Math.max(4, (int) Math.ceil(max / 4.0) * 4);
            for (int k = 0; k < 5; k++) {
                int y = b - (b - t) * k / 4;
                g.setColor(new Color(0xE3EBF7)); g.drawLine(l, y, r, y);
                g.setColor(PawTrackTheme.muted()); g.setFont(font(11, false)); g.drawString(Integer.toString(max * k / 4), 4, y + 4);
            }
            Path2D.Double path = new Path2D.Double(), area = new Path2D.Double();
            for (int i = 0; i < 6; i++) {
                int x = l + (r - l) * i / 5, y = b - (int) (values[i] / (double) max * (b - t));
                if (i == 0) { path.moveTo(x, y); area.moveTo(x, b); area.lineTo(x, y); }
                else { path.lineTo(x, y); area.lineTo(x, y); }
                g.setColor(PawTrackTheme.muted()); g.setFont(font(11, false)); g.drawString(months[i], x - 10, getHeight() - 5);
            }
            area.lineTo(r, b); area.closePath();
            g.setPaint(new GradientPaint(0, t, PawTrackTheme.isDark()?PawTrackTheme.DARK_HOVER:new Color(0xCFE2FF), 0, b, PawTrackTheme.isDark()?PawTrackTheme.DARK_INPUT:new Color(0xF3F8FF)));
            g.fill(area);
            g.setColor(BLUE); g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(path);
            for (int i = 0; i < 6; i++) {
                int x = l + (r - l) * i / 5, y = b - (int) (values[i] / (double) max * (b - t));
                g.setColor(Color.WHITE); g.fillOval(x - 5, y - 5, 10, 10);
                g.setColor(BLUE); g.drawOval(x - 5, y - 5, 10, 10);
            }
            g.dispose();
        }
    }
}