package pawtrack.ui;

import javax.swing.*;
import java.awt.*;

/** Standalone table column-visibility dialog; no domain or database responsibilities. */
final class AnimalColumnVisibilityDialog {
    private AnimalColumnVisibilityDialog() { }

    static void show(JFrame parent, JTable animalTable) {
        JDialog dialog = new JDialog(parent, "Column Visibility", true);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        String[] columnNames = {"ID", "Name", "Species", "Breed", "Age", "Gender", "Status", "Fee", "Classification", "Intake"};
        for (int i = 0; i < columnNames.length && i < animalTable.getColumnCount(); i++) {
            JCheckBox check = new JCheckBox(columnNames[i]);
            check.setSelected(animalTable.getColumnModel().getColumn(i).getWidth() > 0);
            final int colIdx = i;
            check.addActionListener(e -> {
                javax.swing.table.TableColumn column = animalTable.getColumnModel().getColumn(colIdx);
                column.setMinWidth(check.isSelected() ? 50 : 0);
                column.setMaxWidth(check.isSelected() ? Integer.MAX_VALUE : 0);
                column.setWidth(check.isSelected() ? 100 : 0);
                column.setPreferredWidth(check.isSelected() ? 100 : 0);
            });
            panel.add(check);
        }

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(220, 300));
        dialog.add(scroll, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
