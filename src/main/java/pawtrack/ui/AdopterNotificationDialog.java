package pawtrack.ui;

import pawtrack.service.AdopterNotificationService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** On-demand, account-scoped notification viewer; does not poll in the background. */
public final class AdopterNotificationDialog {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private AdopterNotificationDialog() { }

    public static void show(Component parent, AdopterNotificationService service) {
        Objects.requireNonNull(service);
        final List<AdopterNotificationService.Update> updates;
        try {
            updates = service.myUpdates(30);
        } catch (SQLException error) {
            JOptionPane.showMessageDialog(parent, "Could not load application updates: " + error.getMessage(),
                    "Updates unavailable", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JTextArea text = new JTextArea(render(updates));
        text.setEditable(false);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setBackground(new Color(0xF5F8FD));
        text.setForeground(new Color(0x1B3157));
        text.setMargin(new Insets(14, 16, 14, 16));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(520, 340));
        JOptionPane.showMessageDialog(parent, scroll, "My Application Updates", JOptionPane.INFORMATION_MESSAGE);
    }

    /** New read/unread view backed by SQLite receipts; no background polling. */
    public static void show(Component parent, pawtrack.service.NotificationReadService readService) {
        try {
            var notices = readService.myNotices();
            StringBuilder content = new StringBuilder();
            var applications = readService.applicationNotices();
            if (notices.isEmpty() && applications.isEmpty()) content.append("No updates yet.");
            for (var n : applications) content.append(n.read()?"[Read]  ":"[New]  ")
                    .append(n.animal()).append(" - Application ").append(n.status().replace('_',' ')).append("\n");
            for (var n : notices) {
                content.append(n.read() ? "[Read]  " : "[New]  ").append(n.animal()).append(" - ")
                        .append(n.status()).append("\n");
            }
            JTextArea text = new JTextArea(content.toString());
            text.setEditable(false); text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            text.setBackground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xF5F8FD));
            text.setForeground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_TEXT : new Color(0x1B3157));
            text.setCaretColor(text.getForeground());
            text.setMargin(new Insets(18, 18, 18, 18));
            JScrollPane pane = new JScrollPane(text);
            pane.setPreferredSize(new Dimension(480, 300));
            if (PawTrackSimpleDialog.showNotifications(parent, pane)) readService.markAllRead();
        } catch (SQLException err) {
            JOptionPane.showMessageDialog(parent, "Notifications unavailable: " + err.getMessage(),
                    "Notifications", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String render(List<AdopterNotificationService.Update> updates) {
        if (updates.isEmpty()) return "No reservation updates yet. Your waiting applications are in My Records.";
        StringBuilder out = new StringBuilder();
        for (var update : updates) {
            out.append(update.animalName()).append("  •  ").append(update.status()).append('\n')
                    .append(update.message()).append('\n')
                    .append("Reserved on: ").append(update.occurredAt().format(DATE))
                    .append("\n\n");
        }
        return out.toString().stripTrailing();
    }
}
