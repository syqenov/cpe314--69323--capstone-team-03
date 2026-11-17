package pawtrack.ui;

import pawtrack.controller.WaitlistController;
import pawtrack.dao.ReservationDao;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Presentation-only adoption review. No persistence or approval logic belongs here. */
public final class ApplicationDetailsPanel extends JPanel {
    private static final Color NAVY = new Color(0x17345E);
    private static final Color BLUE = new Color(0x2967BC);
    private static final Color SUB = new Color(0x627A9C);
    private static final Color CANVAS = new Color(0xEDF4FF);
    private static final Color BORDER = new Color(0xD9E6F8);
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("MMM d, uuuu  Ã¢â‚¬Â¢  h:mm a", Locale.ENGLISH);

    private final JLabel reviewStatus = new JLabel();
    private final StatusPill statusBadge = new StatusPill();
    private final JLabel petPhoto = new JLabel("PHOTO", SwingConstants.CENTER);

    public ApplicationDetailsPanel(WaitlistController.WaitlistRequest request,
                                   Optional<ReservationDao.Reservation> latest) {
        super(new BorderLayout(0, 14));
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(latest, "latest");
        setBackground(CANVAS);
        setBorder(new EmptyBorder(18, 18, 16, 18));
        setPreferredSize(new Dimension(790, 545));

        JPanel header = new GradientHeader();
        header.setLayout(new BorderLayout(16, 0));
        header.setBorder(new EmptyBorder(20, 24, 20, 24));
        JPanel headerText = vertical(6);
        headerText.add(label("STAFF WORKSPACE  /  ADOPTION REVIEW", 11, true, new Color(0xCDE2FF)));
        headerText.add(label("Review adoption application", 24, true, Color.WHITE));
        headerText.add(label("Confirm the applicant and pet details before taking action.", 12, false, new Color(0xE6F1FF)));
        header.add(headerText, BorderLayout.CENTER);
        header.add(label("PAWTRACK", 11, true, Color.WHITE), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        JPanel photoCard = card(new BorderLayout(0, 10), Color.WHITE);
        photoCard.setPreferredSize(new Dimension(230, 0));
        photoCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        petPhoto.setPreferredSize(new Dimension(200, 205));
        petPhoto.setOpaque(true);
        petPhoto.setBackground(new Color(0xE5EFFB));
        petPhoto.setForeground(BLUE);
        petPhoto.setFont(new Font("Segoe UI", Font.BOLD, 13));
        photoCard.add(petPhoto, BorderLayout.NORTH);
        // Center all text beneath the pet image, including the two-line caption.
        JPanel petInfo = new JPanel(new GridBagLayout());
        petInfo.setOpaque(false);
        GridBagConstraints petConstraints = new GridBagConstraints();
        petConstraints.gridx = 0;
        petConstraints.weightx = 1.0;
        petConstraints.fill = GridBagConstraints.HORIZONTAL;
        petConstraints.anchor = GridBagConstraints.PAGE_START;

        JLabel petName = label(request.animal().getName(), 19, true, NAVY);
        petName.setHorizontalAlignment(SwingConstants.CENTER);
        petConstraints.gridy = 0;
        petConstraints.insets = new Insets(0, 0, 4, 0);
        petInfo.add(petName, petConstraints);

        JLabel petBreed = label(
                request.animal().species().label() + "  Ã‚Â·  " + request.animal().getBreed(),
                12, false, SUB);
        petBreed.setHorizontalAlignment(SwingConstants.CENTER);
        petConstraints.gridy = 1;
        petConstraints.insets = new Insets(0, 0, 14, 0);
        petInfo.add(petBreed, petConstraints);

        JLabel photoNote = label("Loading photograph...", 11, false, SUB);
        photoNote.setHorizontalAlignment(SwingConstants.CENTER);
        petConstraints.gridy = 2;
        petConstraints.insets = new Insets(0, 0, 0, 0);
        petInfo.add(photoNote, petConstraints);

        photoCard.add(petInfo, BorderLayout.CENTER);
        body.add(photoCard, BorderLayout.WEST);
        PetImageCache.get().load(request.animal(), 200, 205, image -> {
            petPhoto.setText("");
            petPhoto.setIcon(image.icon());
            photoNote.setText(image.actualPhoto()
                    ? "Shelter photograph"
                    : "<html><div style='text-align:center;'>Sample photo Ã‚Â· not<br>this pet</div></html>");
        }, error -> photoNote.setText("Photo unavailable"));

        JPanel detailCard = card(new BorderLayout(0, 14), Color.WHITE);
        detailCard.setBorder(new EmptyBorder(18, 19, 16, 19));
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(label("Application information", 18, true, NAVY), BorderLayout.WEST);
        reviewStatus.setText(latest.map(r -> "ACTIVE".equals(r.status()) ? "RESERVED" : r.status())
                .orElse("WAITING"));
        styleStatus();
        top.add(statusBadge, BorderLayout.EAST);
        detailCard.add(top, BorderLayout.NORTH);

        JPanel sections = vertical(13);
        sections.setAlignmentX(Component.LEFT_ALIGNMENT);
        sections.add(sectionTitle("PET & APPLICANT"));
        JPanel people = pairGrid();
        addField(people, "Animal", request.animal().getName());
        addField(people, "Species / breed", request.animal().species().label() + " / " + request.animal().getBreed());
        addField(people, "Applicant", label(request.adopter().getName(), 12, false, NAVY));
        addField(people, "Email", request.adopter().getEmail());
        addField(people, "Phone", request.adopter().getPhone() == null || request.adopter().getPhone().isBlank()
                ? "Not supplied" : request.adopter().getPhone());
        addField(people, "Requested", formatDate(request.requestedAt()));
        sections.add(people);
        sections.add(new JSeparator());
        sections.add(sectionTitle("QUEUE & REVIEW"));
        JPanel queue = pairGrid();
        addField(queue, "FIFO position", label("#" + request.position(), 12, false, NAVY));
        addField(queue, "Review status", reviewStatus);
        sections.add(queue);

        JScrollPane detailsScroll = new JScrollPane(sections);
        detailsScroll.setBorder(null);
        detailsScroll.getViewport().setOpaque(false);
        detailsScroll.setOpaque(false);
        detailsScroll.getVerticalScrollBar().setUnitIncrement(14);
        detailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailCard.add(detailsScroll, BorderLayout.CENTER);
        body.add(detailCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JPanel footer = card(new BorderLayout(0, 5), new Color(0xDFECFF));
        footer.setBorder(new EmptyBorder(11, 15, 11, 15));
        footer.add(wrapped("Actions are verified against current shelter records. This review does not automatically complete an adoption.", 12, NAVY), BorderLayout.CENTER);
        latest.ifPresent(r -> {
            if ("ACTIVE".equals(r.status())) {
                footer.add(label("Reservation expires: " + formatDate(r.expiresAt()), 11, true, BLUE), BorderLayout.SOUTH);
            }
        });
        add(footer, BorderLayout.SOUTH);
    }

    private void styleStatus() {
        String status = reviewStatus.getText().trim().toUpperCase(Locale.ROOT);
        Color fg = switch (status) {
            case "COMPLETED", "APPROVED" -> new Color(0x087C58);
            case "CANCELLED", "EXPIRED" -> new Color(0xB33A52);
            case "RESERVED", "ACTIVE", "IN_REVIEW" -> new Color(0x7051B7);
            default -> BLUE;
        };
        Color bg = switch (status) {
            case "COMPLETED", "APPROVED" -> new Color(0xDCF7E8);
            case "CANCELLED", "EXPIRED" -> new Color(0xFCE6EB);
            case "RESERVED", "ACTIVE", "IN_REVIEW" -> new Color(0xEEE6FF);
            default -> new Color(0xE7F0FF);
        };
        statusBadge.update(status.isEmpty() ? "WAITING" : status.replace('_', ' '), fg, bg);
    }

    private static String formatDate(Object value) {
        if (value == null) return "Not available";
        String raw = value.toString();
        try { return LocalDateTime.parse(raw).format(DISPLAY_DATE); }
        catch (RuntimeException ignored) { return raw.replace('T', ' '); }
    }

    private static JPanel pairGrid() {
        JPanel p = new JPanel(new GridLayout(0, 2, 16, 15));
        p.setOpaque(false);
        return p;
    }

    private static JPanel vertical(int gap) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JLabel sectionTitle(String name) {
        JLabel title = label(name, 10, true, BLUE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        return title;
    }

    private static JLabel label(String value, int size, boolean bold, Color color) {
        JLabel l = new JLabel(value == null ? "" : value);
        l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    private static JTextArea wrapped(String text, int size, Color color) {
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, size));
        area.setForeground(color);
        area.setBorder(null);
        return area;
    }

    private static void addField(JPanel parent, String name, String value) {
        addField(parent, name, wrapped(value, 12, NAVY));
    }
    private static void addField(JPanel parent, String name, JComponent value) {
        JPanel p = vertical(4);
        JLabel caption = label(name, 11, true, SUB);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(caption);
        p.add(Box.createVerticalStrut(4));
        p.add(value);
        parent.add(p);
    }

    private static JPanel card(LayoutManager layout, Color color) {
        JPanel p = new JPanel(layout) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 19, 19));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        return p;
    }

    private static final class GradientHeader extends JPanel {
        GradientHeader() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(0x1B3C70), getWidth(), getHeight(), new Color(0x3F81DF)));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setColor(new Color(255, 255, 255, 17));
            g2.fillOval(getWidth() - 160, -105, 235, 235);
            g2.dispose();
        }
    }

    private static final class StatusPill extends JComponent {
        private String text = "WAITING";
        private Color foreground = BLUE;
        private Color background = new Color(0xE7F0FF);
        StatusPill() { setPreferredSize(new Dimension(124, 28)); }
        void update(String text, Color fg, Color bg) {
            this.text = text;
            foreground = fg;
            background = bg;
            repaint();
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics m = g2.getFontMetrics();
            g2.setColor(foreground);
            g2.drawString(text, Math.max(6, (getWidth() - m.stringWidth(text)) / 2), (getHeight() - m.getHeight()) / 2 + m.getAscent());
            g2.dispose();
        }
    }

    public void setReviewStatus(String status) {
        if (status != null && !status.isBlank()) {
            reviewStatus.setText(status);
            styleStatus();
        }
    }
    public String getReviewStatus() { return reviewStatus.getText(); }
}
