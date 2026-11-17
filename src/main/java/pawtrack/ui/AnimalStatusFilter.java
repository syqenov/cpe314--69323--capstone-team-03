package pawtrack.ui;

import pawtrack.model.AnimalStatus;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Objects;

/** Controls the Animals table status filter popup and row filtering. */
final class AnimalStatusFilter {
    private final JTable animalTable;
    private final JLabel statusLabel;
    private final Color primaryBlue;
    private JDialog popup;

    AnimalStatusFilter(JTable animalTable, JLabel statusLabel, Color primaryBlue) {
        this.animalTable = Objects.requireNonNull(animalTable);
        this.statusLabel = Objects.requireNonNull(statusLabel);
        this.primaryBlue = Objects.requireNonNull(primaryBlue);
    }

    /** Dispose a status popup when Esc or the theme switch is used. */
    void closePopup() {
        if (popup != null) {
            popup.setVisible(false);
            popup.dispose();
            popup = null;
        }
    }

    void install() {
        javax.swing.table.JTableHeader header = animalTable.getTableHeader();
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int column = header.columnAtPoint(e.getPoint());
                if (column == 6) {
                    showStatusFilterMenu(header, e.getX(), e.getY());
                }
            }
        });
        javax.swing.table.TableColumn statusColumn = animalTable.getColumnModel().getColumn(6);
        statusColumn.setHeaderRenderer(new StatusHeaderRenderer());
    }

    /** Header cell renderer for the Status column that shows a down-arrow icon. */
    private final class StatusHeaderRenderer extends javax.swing.table.DefaultTableCellRenderer {
        StatusHeaderRenderer() {
            setOpaque(true);
            setBackground(primaryBlue);
            setForeground(Color.WHITE);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value + " \u25BC");
            return this;
        }
    }

    // Private helper for MainFrame GUI logic
    private void showStatusFilterMenu(java.awt.Component invoker, int x, int y) {
        // Close any previously opened status panel first (prevents stacking).
        closePopup();

        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(invoker);
        javax.swing.JDialog dialog = new javax.swing.JDialog(owner, java.awt.Dialog.ModalityType.MODELESS);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        ((javax.swing.JComponent) dialog.getContentPane()).setOpaque(false);

        StatusPopupPanel panel = new StatusPopupPanel();
        final javax.swing.JDialog d = dialog;
        Runnable close = () -> {
            if (popup == d) {
                popup = null;
            }
        };
        panel.addOption("All", () -> applyStatusFilter(null), dialog, close);
        for (AnimalStatus status : AnimalStatus.values()) {
            panel.addOption(status.label(), () -> applyStatusFilter(status.label()), dialog, close);
        }
        dialog.getContentPane().add(panel);
        dialog.pack();
        java.awt.Point location = invoker.getLocationOnScreen();
        dialog.setLocation(location.x + x, location.y + y);

        final javax.swing.JDialog current = dialog;
        dialog.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
            }

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                current.setVisible(false);
                current.dispose();
                if (popup == current) {
                    popup = null;
                }
            }
        });

        popup = dialog;
        dialog.setVisible(true);
    }

    /** Rounded blue drop-down panel with a soft shadow (custom-painted). */
    private final class StatusPopupPanel extends JPanel {
        StatusPopupPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        }

        void addOption(String text, Runnable action, javax.swing.JDialog dialog, Runnable onClose) {
            JLabel label = new JLabel(text);
            label.setForeground(Color.WHITE);
            label.setOpaque(false);
            label.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            label.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    label.setForeground(new Color(0xBFD7F2));
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    label.setForeground(Color.WHITE);
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    action.run();
                    dialog.setVisible(false);
                    dialog.dispose();
                    onClose.run();
                }
            });
            add(label);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int s = 3;
            int w = getWidth() - s;
            int h = getHeight() - s;
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(s, s, w, h, 22, 22);
            g2.setColor(primaryBlue);
            g2.fillRoundRect(0, 0, w, h, 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Private helper for MainFrame GUI logic
    void applyStatusFilter(String label) {
        javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter =
                (javax.swing.table.TableRowSorter<javax.swing.table.TableModel>) animalTable.getRowSorter();
        if (label == null) {
            sorter.setRowFilter(null);
            statusLabel.setText("Showing all animals");
        } else {
            sorter.setRowFilter(javax.swing.RowFilter.regexFilter(java.util.regex.Pattern.quote(label), 6));
            statusLabel.setText("Filtered by status: " + label);
        }
    }

}
