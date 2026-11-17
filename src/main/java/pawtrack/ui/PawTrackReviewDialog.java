package pawtrack.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Modal, presentation-only action dialog. Existing MainFrame callbacks retain security and business logic. */
public final class PawTrackReviewDialog {
    private static final Color NAVY = new Color(0x1B3157);
    private static final Color BLUE = new Color(0x2B68BB);
    private static final Color GREEN = new Color(0x0F927D);

    private PawTrackReviewDialog() { }

    /** Returns the selected option index, or -1 if dismissed. */
    public static int show(Component owner, ApplicationDetailsPanel content, Object[] choices) {
        Window window = SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(window, "Review Adoption Application", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(content, BorderLayout.CENTER);

        final int[] selection = {-1};
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actions.setBackground(new Color(0xEDF4FF));
        actions.setBorder(new EmptyBorder(0, 14, 7, 14));
        for (int i = 0; i < choices.length; i++) {
            final int index = i;
            String caption = String.valueOf(choices[i]);
            boolean close = "Close".equalsIgnoreCase(caption);
            boolean destructive = caption.startsWith("Cancel");
            boolean positive = caption.startsWith("Reserve") || caption.startsWith("Complete");
            Color fill = close ? new Color(0xFFFFFF)
                    : destructive ? new Color(0xFCEAF0) : positive ? GREEN : BLUE;
            Color ink = close ? NAVY : destructive ? new Color(0x9B2346) : Color.WHITE;
            Color outline = close ? new Color(0xBDD0EC)
                    : destructive ? new Color(0xE9A9BB) : fill;
            JButton button = new ReviewActionButton(caption, fill, ink, outline);
            button.addActionListener(e -> {
                selection[0] = index;
                dialog.dispose();
            });
            actions.add(button);
            if (close) dialog.getRootPane().setDefaultButton(button);
        }
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        Dimension available = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setSize(Math.min(dialog.getWidth(), available.width - 40),
                       Math.min(dialog.getHeight(), available.height - 80));
        dialog.setMinimumSize(new Dimension(Math.min(650, dialog.getWidth()), Math.min(420, dialog.getHeight())));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return selection[0];
    }

    /** Draws its own background so Windows look-and-feel cannot wash out button text. */
    static final class ReviewActionButton extends JButton {
        private final Color fill;
        private final Color ink;
        private final Color outline;

        ReviewActionButton(String text, Color fill, Color ink, Color outline) {
            super(text);
            this.fill = fill;
            this.ink = ink;
            this.outline = outline;
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setForeground(ink);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(new EmptyBorder(11, 19, 11, 19));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color background = fill;
            if (getModel().isPressed()) background = blend(fill, Color.BLACK, 0.12f);
            else if (getModel().isRollover()) background = blend(fill, Color.WHITE, 0.08f);
            g2.setColor(background);
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 13, 13);
            g2.setColor(outline);
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 13, 13);
            g2.dispose();
            setForeground(ink);
            super.paintComponent(g);
        }

        private static Color blend(Color a, Color b, float mix) {
            return new Color(Math.round(a.getRed() * (1 - mix) + b.getRed() * mix),
                    Math.round(a.getGreen() * (1 - mix) + b.getGreen() * mix),
                    Math.round(a.getBlue() * (1 - mix) + b.getBlue() * mix));
        }
    }

}
