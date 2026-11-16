package pawtrack.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Reusable painting and table primitives for the PawTrack desktop design system. */
public final class PremiumUi {
    public static final Color NAVY = new Color(0x1B3157);
    public static final Color BLUE = new Color(0x315FAD);
    public static final Color MUTED = new Color(0x617697);
    public static final Color CANVAS = new Color(0xEFF5FF);
    public static final Color LINE = new Color(0xD9E5FA);
    private PremiumUi() {}

    public static Font font(int size, boolean bold) {
        return new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size);
    }
    public static JLabel title(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, true)); label.setForeground(NAVY);
        return label;
    }
    public static JLabel caption(String text) {
        JLabel label = new JLabel(text); label.setForeground(MUTED); label.setFont(font(12, false)); return label;
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
                g.setColor(Color.WHITE);
                g.fillRoundRect(1,1,Math.max(0,getWidth()-7),Math.max(0,getHeight()-9),24,24);
                g.setColor(LINE);g.drawRoundRect(1,1,Math.max(0,getWidth()-7),Math.max(0,getHeight()-9),24,24);
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
                g.setColor(primary ? NAVY : new Color(0xF6F9FF));
                g.fillRoundRect(1,1,getWidth()-3,getHeight()-3,24,24);
                if(!primary) {g.setColor(new Color(0xAFC8F2));g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,24,24);}
                g.dispose();super.paintComponent(graphics);
            }
        };
        button.setContentAreaFilled(false);button.setFocusPainted(false);button.setBorder(new EmptyBorder(10,15,10,15));
        button.setForeground(primary?Color.WHITE:NAVY);button.setFont(font(13,true));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e->action.run());return button;
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
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col){
                JLabel label=(JLabel)super.getTableCellRendererComponent(t,value,selected,focus,row,col);
                label.setBorder(new EmptyBorder(5,10,5,10));
                label.setForeground(selected?NAVY:PremiumUi.NAVY);
                label.setBackground(selected?new Color(0xDBEAFF):row%2==0?Color.WHITE:new Color(0xF2F7FF));
                label.setOpaque(true);
                if(value!=null && (value.toString().startsWith("WAITING")||value.toString().startsWith("COMPLETED"))) {
                    label.setForeground(value.toString().startsWith("COMPLETED")?new Color(0x187848):new Color(0x975D10));
                }
                return label;
            }
        });
    }
    public static class Background extends JPanel {
        public Background(LayoutManager layout){super(layout);setOpaque(true);}
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);
            Graphics2D g=(Graphics2D)graphics.create();
            g.setPaint(new GradientPaint(0,0,new Color(0xF7FAFF),getWidth(),getHeight(),new Color(0xE2ECFF)));
            g.fillRect(0,0,getWidth(),getHeight());g.dispose();
        }
    }
}
