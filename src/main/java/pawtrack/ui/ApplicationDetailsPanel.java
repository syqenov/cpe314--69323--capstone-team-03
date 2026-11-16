package pawtrack.ui;

import pawtrack.controller.WaitlistController;
import pawtrack.dao.ReservationDao;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.Optional;

/** Read-only application details; decisions are handled by secured MainFrame callbacks. */
public final class ApplicationDetailsPanel extends JPanel {
    private static final Color NAVY = new Color(0x17345E);
    private static final Color BLUE = new Color(0x286BC7);
    private static final Color SUB = new Color(0x667C9A);
    private final JLabel reviewStatus = new JLabel();
    private final JLabel statusBadge = new JLabel();
    private final JLabel petPhoto = new JLabel("PHOTO", SwingConstants.CENTER);

    public ApplicationDetailsPanel(WaitlistController.WaitlistRequest request,
                                   Optional<ReservationDao.Reservation> latest) {
        super(new BorderLayout(0, 14));
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(latest, "latest");
        setBackground(new Color(0xF4F8FE));
        setBorder(new EmptyBorder(16, 18, 16, 18));
        setPreferredSize(new Dimension(670, 455));

        JPanel headingCard = card(new BorderLayout(14, 0), new Color(0x1D4B8B));
        headingCard.setBorder(new EmptyBorder(15, 18, 15, 18));
        JPanel title = new JPanel(new GridLayout(2, 1, 0, 4));
        title.setOpaque(false);
        JLabel caption = label("STAFF WORKSPACE  /  ADOPTION REVIEW", 10, true, new Color(0xCBE0FF));
        JLabel heading = label("Adoption application", 23, true, Color.WHITE);
        title.add(caption);
        title.add(heading);
        headingCard.add(title, BorderLayout.CENTER);
        JLabel badge = label("PAWTRACK", 11, true, Color.WHITE);
        headingCard.add(badge, BorderLayout.EAST);
        add(headingCard, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);

        JPanel photoCard = card(new BorderLayout(0, 8), Color.WHITE);
        photoCard.setPreferredSize(new Dimension(215, 0));
        photoCard.setBorder(new EmptyBorder(14, 13, 14, 13));
        petPhoto.setPreferredSize(new Dimension(188, 188));
        petPhoto.setOpaque(true);
        petPhoto.setBackground(new Color(0xE3EFFD));
        petPhoto.setForeground(BLUE);
        petPhoto.setFont(new Font("Segoe UI", Font.BOLD, 13));
        photoCard.add(petPhoto, BorderLayout.NORTH);
        JPanel petInfo = new JPanel(new GridLayout(3, 1, 0, 4));
        petInfo.setOpaque(false);
        petInfo.add(label(request.animal().getName(), 18, true, NAVY));
        petInfo.add(label(request.animal().species().label() + " · " + request.animal().getBreed(), 12, false, SUB));
        JLabel photoNote = label("Photo loading...", 10, false, SUB);
        petInfo.add(photoNote);
        photoCard.add(petInfo, BorderLayout.CENTER);
        body.add(photoCard, BorderLayout.WEST);

        PetImageCache.get().load(request.animal(), 188, 188,
                image -> {
                    petPhoto.setText("");
                    petPhoto.setIcon(image.icon());
                    photoNote.setText(image.actualPhoto() ? "Shelter photograph" : "Sample pet image");
                }, error -> photoNote.setText("Photo unavailable"));

        JPanel details = card(new BorderLayout(0, 13), Color.WHITE);
        details.setBorder(new EmptyBorder(16, 18, 15, 18));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("Application information", 16, true, NAVY), BorderLayout.WEST);
        reviewStatus.setText(latest.map(r -> "ACTIVE".equals(r.status()) ? "RESERVED" : r.status())
                .orElse("WAITING"));
        styleStatus();
        top.add(statusBadge, BorderLayout.EAST);
        details.add(top, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(0, 2, 13, 13));
        fields.setOpaque(false);
        addField(fields, "Animal", request.animal().getName());
        addField(fields, "Species / breed", request.animal().species().label() + " / " + request.animal().getBreed());
        addField(fields, "Applicant", request.adopter().getName());
        addField(fields, "Email", request.adopter().getEmail());
        addField(fields, "Phone", request.adopter().getPhone().isBlank() ? "Not supplied" : request.adopter().getPhone());
        addField(fields, "Requested", request.requestedAt().toString().replace('T', ' '));
        addField(fields, "FIFO position", "#" + request.position());
        addField(fields, "Review status", reviewStatus);
        details.add(fields, BorderLayout.CENTER);
        body.add(details, BorderLayout.CENTER);

        JPanel footer = card(new BorderLayout(0, 5), new Color(0xE7F1FF));
        footer.setBorder(new EmptyBorder(10, 14, 10, 14));
        footer.add(label("Actions are checked against current shelter records.", 11, false, NAVY), BorderLayout.NORTH);
        latest.ifPresent(r -> {
            if ("ACTIVE".equals(r.status())) {
                footer.add(label("Reservation expires: " + r.expiresAt(), 11, true, BLUE), BorderLayout.SOUTH);
            }
        });
        add(footer, BorderLayout.SOUTH);
    }

    private void styleStatus() {
        String status = reviewStatus.getText().toUpperCase(java.util.Locale.ROOT);
        Color color = switch (status) {
            case "COMPLETED", "APPROVED" -> new Color(0x08764B);
            case "CANCELLED", "EXPIRED" -> new Color(0xB33A52);
            case "RESERVED", "IN_REVIEW" -> new Color(0x7051B7);
            default -> new Color(0x285FA8);
        };
        statusBadge.setText(status);
        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusBadge.setForeground(color);
        statusBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
    }

    private static JPanel card(LayoutManager layout, Color color) {
        JPanel p = new JPanel(layout) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D x = (Graphics2D) g.create();
                x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                x.setColor(color);
                x.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 18, 18));
                x.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        return p;
    }

    private static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    private static void addField(JPanel panel, String title, String value) {
        addField(panel, title, label(value, 12, false, NAVY));
    }
    private static void addField(JPanel panel, String title, JLabel value) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        JLabel label = label(title, 10, true, SUB);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(4));
        group.add(value);
        panel.add(group);
    }

    public void setReviewStatus(String status) {
        if (status != null && !status.isBlank()) {
            reviewStatus.setText(status);
            styleStatus();
        }
    }
    public String getReviewStatus() { return reviewStatus.getText(); }
}
