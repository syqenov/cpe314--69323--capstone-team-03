package pawtrack.ui;

import javax.swing.*;
import java.awt.*;

/** Theme-aware modal dialogs, independent of Windows native JOptionPane button painting. */
public final class PawTrackSimpleDialog {
    private PawTrackSimpleDialog() { }

    public static boolean confirm(Component parent, String title, String question) {
        JLabel message = new JLabel(question);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return show(parent, title, message, "Yes", "No");
    }

    public static boolean showNotifications(Component parent, JComponent content) {
        return show(parent, "My Notifications", content, "Mark all read", "Close");
    }

    private static boolean show(Component parent, String title, JComponent content,
                                String acceptLabel, String cancelLabel) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = owner instanceof Frame frame ? new JDialog(frame, title, true)
                : owner instanceof Dialog ownerDialog ? new JDialog(ownerDialog, title, true)
                : new JDialog((Frame) null, title, true);
        final boolean[] accepted = {false};
        boolean dark = PawTrackTheme.isDark();
        Color bg = dark ? PawTrackTheme.DARK_SURFACE : Color.WHITE;
        Color fg = dark ? PawTrackTheme.DARK_TEXT : PawTrackTheme.NAVY;
        JPanel root = new JPanel(new BorderLayout(12, 16));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));
        root.setBackground(bg);
        content.setForeground(fg);
        if (!(content instanceof JScrollPane)) content.setBackground(bg);
        root.add(content, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton cancel = button(cancelLabel, false, dark);
        JButton accept = button(acceptLabel, true, dark);
        cancel.addActionListener(e -> dialog.dispose());
        accept.addActionListener(e -> { accepted[0] = true; dialog.dispose(); });
        actions.add(cancel);
        actions.add(accept);
        root.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(cancel);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(340, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return accepted[0];
    }

    private static JButton button(String title, boolean primary, boolean dark) {
        JButton button = new JButton(title);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(Math.max(105, title.length() * 9 + 25), 36));
        button.setBackground(primary ? new Color(0x315FAD)
                : dark ? PawTrackTheme.DARK_INPUT : new Color(0xEAF1FB));
        button.setForeground(primary ? Color.WHITE
                : dark ? PawTrackTheme.DARK_TEXT : PawTrackTheme.NAVY);
        button.setBorder(BorderFactory.createLineBorder(
                dark ? PawTrackTheme.DARK_BORDER : new Color(0xB9CBE3)));
        return button;
    }
}
