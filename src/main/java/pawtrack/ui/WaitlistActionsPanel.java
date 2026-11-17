package pawtrack.ui;

import java.awt.FlowLayout;
import java.util.Objects;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Displays staff waitlist actions in two rows without embedding layout rules in MainFrame.
 * Button creation and authorization remain with MainFrame/controllers.
 */
public final class WaitlistActionsPanel extends JPanel {

    public WaitlistActionsPanel(JButton refresh, JButton reserve,
                                JButton complete, JButton cancel) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel firstRow = createRow();
        firstRow.add(Objects.requireNonNull(refresh, "refresh"));
        firstRow.add(Objects.requireNonNull(reserve, "reserve"));
        JPanel secondRow = createRow();
        secondRow.add(Objects.requireNonNull(complete, "complete"));
        secondRow.add(Objects.requireNonNull(cancel, "cancel"));
        add(firstRow);
        add(secondRow);
    }

    private static JPanel createRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.setOpaque(false);
        return row;
    }
}
