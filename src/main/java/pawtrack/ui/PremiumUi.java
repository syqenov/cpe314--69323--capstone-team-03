package pawtrack.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Reusable painting and table primitives for the PawTrack desktop design system. */
public final class PremiumUi {
    public static final Color NAVY = PawTrackTheme.NAVY;
    public static final Color BLUE = PawTrackTheme.BLUE;
    public static final Color MUTED = PawTrackTheme.MUTED;
    public static final Color CANVAS = PawTrackTheme.CANVAS;
    public static final Color LINE = PawTrackTheme.LINE;
    private PremiumUi() {}

    public static Font font(int size, boolean bold) {
        return new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size);
    }
    public static JLabel title(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, true)); label.setForeground(NAVY);
        // Register light-mode colors first, even if the label is added while dark.
        if (PawTrackTheme.isDark()) PawTrackThemeManager.applyCurrent(label);
        return label;
    }
    public static JLabel caption(String text) {
        JLabel label = new JLabel(text); label.setForeground(MUTED); label.setFont(font(12, false));
        if (PawTrackTheme.isDark()) PawTrackThemeManager.applyCurrent(label);
        return label;
    }
    public static JPanel clear(LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setOpaque(false); return panel;
    }
    public static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g=(Graphics2D)graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(65, 110, 188, 18));
                g.fillRoundRect(3, 5, Math.max(0,getWidth()-7), Math.max(0,getHeight()-8), 25,25);
                g.setColor(PawTrackTheme.surface());
                g.fillRoundRect(1,1,Math.max(0,getWidth()-7),Math.max(0,getHeight()-9),24,24);
                g.setColor(PawTrackTheme.line());g.drawRoundRect(1,1,Math.max(0,getWidth()-7),Math.max(0,getHeight()-9),24,24);
                g.dispose();super.paintComponent(graphics);
            }
        };
        panel.setOpaque(false); panel.setBorder(new EmptyBorder(20,22,22,26)); return panel;
    }
    public static JButton button(String text, Runnable action, boolean primary) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g=(Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(primary ? (PawTrackTheme.isDark() ? PawTrackTheme.DARK_SELECTED : NAVY) : PawTrackTheme.surface());
                g.fillRoundRect(1,1,getWidth()-3,getHeight()-3,24,24);
                if(!primary) {g.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_BORDER : new Color(0xAFC8F2));g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,24,24);}
                g.dispose();super.paintComponent(graphics);
            }
        };
        button.setContentAreaFilled(false);button.setFocusPainted(false);button.setBorder(new EmptyBorder(10,15,10,15));
        button.setForeground(primary ? Color.WHITE : NAVY);button.setFont(font(13,true));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e->action.run());
        if (PawTrackTheme.isDark()) PawTrackThemeManager.applyCurrent(button);
        return button;
    }
    public static JPanel kpi(String icon,String label,String value) {
        JPanel p=card(new BorderLayout(10,5));
        JLabel mark=title(icon,26);mark.setForeground(BLUE);p.add(mark,BorderLayout.WEST);
        JPanel text=clear(new GridLayout(2,1,0,1));text.add(caption(label));text.add(title(value,23));p.add(text,BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(175,94));return p;
    }
    public static void styleTable(JTable table) {
        table.setFont(font(12,false));table.setRowHeight(46);table.setGridColor(LINE);
        table.setShowVerticalLines(false);table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(0xDBEAFF));table.setSelectionForeground(NAVY);
        table.setBackground(new Color(0xFBFDFF));table.setForeground(NAVY);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setPreferredSize(new Dimension(1,39));
        table.getTableHeader().setBackground(new Color(0xE3EDFF));
        table.getTableHeader().setForeground(NAVY);table.getTableHeader().setFont(font(12,true));
        // A JTableHeader's default renderer may ignore setBackground/setForeground,
        // especially on Windows Look & Feel. Paint both colors in the renderer.
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                setBackground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_SIDEBAR : new Color(0xE3EDFF));
                setForeground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_TEXT : NAVY);
                setFont(font(12, true));
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 10, 0, 8));
                setOpaque(true);
                return this;
            }
        });
        DefaultTableCellRenderer rowRenderer = new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col){
                JLabel label=(JLabel)super.getTableCellRendererComponent(t,value,selected,focus,row,col);
                label.setBorder(new EmptyBorder(5,10,5,10));
                label.setForeground(PawTrackTheme.isDark() ? PawTrackTheme.DARK_TEXT : NAVY);
                label.setBackground(PawTrackTheme.isDark() ? (selected ? PawTrackTheme.DARK_SELECTED : (row%2==0 ? PawTrackTheme.DARK_SURFACE : PawTrackTheme.DARK_INPUT)) : (selected?new Color(0xDBEAFF):row%2==0?Color.WHITE:new Color(0xF2F7FF)));
                label.setOpaque(true);
                if(value!=null && (value.toString().startsWith("WAITING")||value.toString().startsWith("COMPLETED"))) {
                    label.setForeground(value.toString().startsWith("COMPLETED")
                            ? (PawTrackTheme.isDark() ? new Color(0x9AE6B4) : new Color(0x187848))
                            : (PawTrackTheme.isDark() ? new Color(0xF7D08A) : new Color(0x975D10)));
                }
                return label;
            }
        };
        table.setDefaultRenderer(Object.class, rowRenderer);
        table.setDefaultRenderer(Number.class, rowRenderer);
        table.setDefaultRenderer(java.util.Date.class, rowRenderer);
        if (PawTrackTheme.isDark()) PawTrackThemeManager.applyCurrent(table);
    }
    public static class Background extends JPanel {
        public Background(LayoutManager layout){super(layout);setOpaque(true);}
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);
            Graphics2D g=(Graphics2D)graphics.create();
            g.setPaint(new GradientPaint(0,0,PawTrackTheme.isDark()?PawTrackTheme.DARK_CANVAS:new Color(0xF7FAFF),getWidth(),getHeight(),PawTrackTheme.isDark()?new Color(0x172A42):new Color(0xE2ECFF)));
            g.fillRect(0,0,getWidth(),getHeight());g.dispose();
        }
    }
}
