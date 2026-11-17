package pawtrack.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

/** Staff import workspace. The existing MainFrame chooser and SwingWorker remain the source of truth. */
public final class PawTrackImportPanel extends JPanel {
    private static final Color NAVY = new Color(0x19365F);
    private static final Color BLUE = new Color(0x2C69C3);
    private static final Color MUTED = new Color(0x61799D);
    private static final Color CANVAS = new Color(0xEFF5FF);
    private static final Color BORDER = new Color(0xD5E4FA);
    private static final Color SOFT = new Color(0xE2EEFF);
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 12);

    private final JProgressBar displayProgress = new JProgressBar(0, 100);
    private final JLabel state = text("Ready to import", 18, true, NAVY);
    private final JLabel detail = text("Select an intake CSV to begin a background import.", 12, false, MUTED);
    private final JLabel percent = text("0%", 14, true, BLUE);
    private final StepTrack steps = new StepTrack();

    /** Keeps the signature expected by MainFrame. */
    public PawTrackImportPanel(Runnable openChooser, JProgressBar applicationProgress) {
        super(new BorderLayout());
        Objects.requireNonNull(openChooser, "openChooser");
        Objects.requireNonNull(applicationProgress, "applicationProgress");
        setBackground(CANVAS);
        setBorder(new EmptyBorder(13, 17, 13, 17));

        JPanel column = transparent(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 13, 0);
        column.add(makeHeader(), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 13, 0);
        column.add(makeChooser(openChooser), gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        column.add(makeBottom(), gbc);

        // Keep the page scrollable when PawTrack is running in a non-maximized window.
        JScrollPane pageScroll = new JScrollPane(column,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.setOpaque(false);
        pageScroll.getViewport().setOpaque(false);
        pageScroll.setBorder(null);
        pageScroll.getVerticalScrollBar().setUnitIncrement(22);
        add(pageScroll, BorderLayout.CENTER);

        applicationProgress.addChangeListener(e -> updateProgress(applicationProgress));
        // String changes ("Done"/"Failed") do not always change the numeric progress value.
        applicationProgress.addPropertyChangeListener("string", e -> updateProgress(applicationProgress));
        updateProgress(applicationProgress);
    }

    private JComponent makeHeader() {
        JPanel banner = new JPanel(new BorderLayout(18, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xD5E7FF), getWidth(), getHeight(), PawTrackTheme.isDark() ? PawTrackTheme.DARK_SURFACE : new Color(0xEAF3FF)));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
                g2.setColor(new Color(0xBDD5F7));
                Path2D.Double wave = new Path2D.Double();
                int w = getWidth(), h = getHeight();
                wave.moveTo(w * .56, h);
                wave.curveTo(w * .70, h * .37, w * .79, h * 1.06, w, h * .45);
                wave.lineTo(w, h);
                wave.closePath();
                g2.fill(wave);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 26, 26);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        banner.setPreferredSize(new Dimension(600, 125));
        banner.setBorder(new EmptyBorder(17, 20, 17, 20));

        JPanel icon = new JPanel(new BorderLayout());
        icon.setOpaque(false);
        IconCanvas uploadIcon = new IconCanvas("upload", BLUE);
        uploadIcon.setPreferredSize(new Dimension(58, 58));
        icon.add(uploadIcon);
        banner.add(icon, BorderLayout.WEST);

        JPanel heading = transparent(new GridLayout(2, 1, 0, 3));
        JLabel title = text("Import Records", 27, true, NAVY);
        JLabel subtitle = text("Bring historical intake and adoption data into PawTrack.", 12, false, new Color(0x4B6D9B));
        heading.add(title);
        heading.add(subtitle);
        banner.add(heading, BorderLayout.CENTER);

        IconCanvas pets = new IconCanvas("pets", new Color(0x6193D7));
        pets.setPreferredSize(new Dimension(132, 78));
        banner.add(pets, BorderLayout.EAST);
        return banner;
    }

    private JComponent makeChooser(Runnable openChooser) {
        Card card = new Card(new BorderLayout(0, 9));
        card.setBorder(new EmptyBorder(15, 20, 15, 20));
        JPanel titles = transparent(new BorderLayout(12, 0));
        JPanel labels = transparent(new GridLayout(2, 1, 0, 3));
        labels.add(text("Historical Intake CSV", 19, true, NAVY));
        labels.add(text("Select a UTF-8 CSV with animal intake details and optional adoption records.", 11, false, MUTED));
        titles.add(labels, BorderLayout.CENTER);
        card.add(titles, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PawTrackTheme.isDark() ? PawTrackTheme.DARK_INPUT : new Color(0xF0F6FF));
                g2.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, 22, 22);
                Stroke previous = g2.getStroke();
                g2.setColor(new Color(0xA8C9F5));
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                        0, new float[]{7, 6}, 0));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 22, 22);
                g2.setStroke(previous);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(14, 18, 14, 18));
        content.setPreferredSize(new Dimension(600, 185));
        IconCanvas graphic = new IconCanvas("csv", BLUE);
        graphic.setPreferredSize(new Dimension(145, 135));
        content.add(graphic, BorderLayout.WEST);

        JPanel action = transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx=0; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 4, 9, 4);
        c.gridy=0;
        action.add(text("CSV  ·  ANIMAL INTAKE", 22, true, NAVY), c);
        c.gridy=1;
        action.add(text("Imports run in the background, so the interface remains responsive.", 11, false, MUTED), c);
        c.gridy=2;
        c.insets = new Insets(1, 4, 0, 4);
        JButton choose = new PrimaryButton("Choose CSV File and Start Import");
        choose.setName("chooseCsvImport");
        choose.addActionListener(e -> openChooser.run());
        action.add(choose, c);
        content.add(action, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JComponent makeBottom() {
        JPanel bottom = transparent(new GridLayout(1, 2, 14, 0));
        Card progress = new Card(new BorderLayout(0, 13));
        progress.setBorder(new EmptyBorder(15, 17, 17, 17));
        progress.add(sectionTitle("progress", "Import progress"), BorderLayout.NORTH);

        JPanel area = transparent(new BorderLayout(0, 12));
        steps.setPreferredSize(new Dimension(300, 68));
        area.add(steps, BorderLayout.NORTH);
        JPanel status = new JPanel(new BorderLayout(10, 0));
        status.setBackground(SOFT);
        status.setBorder(new EmptyBorder(13, 12, 13, 12));
        JLabel statusIcon = text("✓", 21, true, new Color(0x168B68));
        status.add(statusIcon, BorderLayout.WEST);
        JPanel statusText = transparent(new GridLayout(2, 1, 0, 4));
        statusText.add(state);
        statusText.add(detail);
        status.add(statusText, BorderLayout.CENTER);
        area.add(status, BorderLayout.CENTER);
        JPanel gauge = transparent(new BorderLayout(8, 0));
        displayProgress.setStringPainted(false);
        displayProgress.setForeground(BLUE);
        displayProgress.setBackground(new Color(0xD8E4F3));
        displayProgress.setBorderPainted(false);
        displayProgress.setPreferredSize(new Dimension(10, 12));
        gauge.add(displayProgress, BorderLayout.CENTER);
        gauge.add(percent, BorderLayout.EAST);
        area.add(gauge, BorderLayout.SOUTH);
        progress.add(area, BorderLayout.CENTER);
        bottom.add(progress);

        Card schema = new Card(new BorderLayout(0, 10));
        schema.setBorder(new EmptyBorder(15, 17, 17, 17));
        schema.add(sectionTitle("document", "Supported CSV Format"), BorderLayout.NORTH);
        JPanel schemaBody = transparent(new GridLayout(3, 1, 0, 10));
        schemaBody.add(schemaGroup("REQUIRED HEADERS", "name, species, breed, age_months, intake_date, status",
                new Color(0xE9F2FF), BLUE));
        schemaBody.add(schemaGroup("OPTIONAL HEADERS", "adoption_date, adopter_name, adopter_email",
                new Color(0xEBF8F3), new Color(0x147F60)));
        JPanel formatNote = new JPanel(new BorderLayout(8, 0));
        formatNote.setBackground(new Color(0xF0F5FF));
        formatNote.setBorder(new EmptyBorder(8, 10, 8, 10));
        formatNote.add(text("i", 17, true, BLUE), BorderLayout.WEST);
        formatNote.add(text("Dates: YYYY-MM-DD  ·  Species: Dog or Cat", 11, false, NAVY), BorderLayout.CENTER);
        schemaBody.add(formatNote);
        schema.add(schemaBody, BorderLayout.CENTER);
        bottom.add(schema);
        return bottom;
    }

    private static JPanel schemaGroup(String heading, String fields, Color background, Color accent) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 4));
        card.setBackground(background);
        card.setBorder(new EmptyBorder(8, 11, 8, 11));
        card.add(text(heading, 10, true, accent));
        JLabel list = text(fields, 11, false, NAVY);
        list.setToolTipText(fields);
        card.add(list);
        return card;
    }

    private static JComponent sectionTitle(String icon, String title) {
        JPanel p = transparent(new BorderLayout(9, 0));
        IconCanvas mark = new IconCanvas(icon, BLUE);
        mark.setPreferredSize(new Dimension(35, 32));
        p.add(mark, BorderLayout.WEST);
        p.add(text(title, 18, true, NAVY), BorderLayout.CENTER);
        return p;
    }

    private void updateProgress(JProgressBar source) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateProgress(source));
            return;
        }
        int value = Math.max(0, Math.min(100, source.getValue()));
        displayProgress.setValue(value);
        percent.setText(value + "%");
        String label = source.getString();
        boolean done = label != null && "Done".equalsIgnoreCase(label.trim());
        boolean failed = label != null && "Failed".equalsIgnoreCase(label.trim());
        if (done) {
            state.setText("Import complete");
            detail.setText("Refresh Animals or Analytics to inspect imported records.");
            steps.setState(100, true, false);
        } else if (failed) {
            state.setText("Import failed");
            detail.setText("Review the import error and required CSV headers.");
            steps.setState(value, false, true);
        } else if (value > 0 || (label != null && !label.isBlank()
                && !"Ready".equalsIgnoreCase(label.trim())
                && !label.trim().matches("[0-9]+%"))) {
            state.setText("Importing records...");
            detail.setText("Your file is being processed in the background.");
            steps.setState(value, false, false);
        } else {
            state.setText("Ready to import");
            detail.setText("Select an intake CSV to begin a background import.");
            steps.setState(0, false, false);
        }
    }

    private static JLabel text(String value, int size, boolean bold, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }
    private static JPanel transparent(LayoutManager manager) {
        JPanel panel = new JPanel(manager);
        panel.setOpaque(false);
        return panel;
    }

    private static final class Card extends JPanel {
        Card(LayoutManager manager) { super(manager); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D x = (Graphics2D) g.create();
            x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            x.setColor(new Color(0xD7E5F9));
            x.fillRoundRect(1, 4, getWidth()-3, getHeight()-6, 22, 22);
            x.setColor(PawTrackTheme.surface());
            x.fillRoundRect(0, 0, getWidth()-3, getHeight()-7, 22, 22);
            x.setColor(PawTrackTheme.line());
            x.drawRoundRect(0, 0, getWidth()-3, getHeight()-7, 22, 22);
            x.dispose();
            super.paintComponent(g);
        }
    }

    private static final class PrimaryButton extends JButton {
        PrimaryButton(String title) {
            super(title);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setOpaque(false);
            setBorder(new EmptyBorder(11, 20, 13, 20));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(350, 42));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D x = (Graphics2D) g.create();
            x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            x.setColor(new Color(0xC2D1E5));
            x.fillRoundRect(1, 4, getWidth()-2, getHeight()-5, 17, 17);
            Color c1 = getModel().isRollover() ? new Color(0x285BA4) : NAVY;
            Color c2 = getModel().isRollover() ? new Color(0x4588DD) : new Color(0x295CA8);
            x.setPaint(new GradientPaint(0, 0, c1, getWidth(), 0, c2));
            x.fillRoundRect(0, 0, getWidth()-2, getHeight()-6, 17, 17);
            x.dispose();
            super.paintComponent(g);
        }
    }

    /** Visual milestones indicate progress, not separate verified import phases. */
    private static final class StepTrack extends JComponent {
        private final String[] names = { "Ready", "Processing", "Finishing", "Complete" };
        private int amount;
        private boolean complete, failed;
        void setState(int amount, boolean complete, boolean failed) {
            this.amount = amount; this.complete = complete; this.failed = failed; repaint();
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), y=18;
            int left=32, right=Math.max(left+1,w-36);
            int dx=(right-left)/3;
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(0xC9D9EF));
            g.drawLine(left,y,right,y);
            for(int i=0;i<4;i++) {
                int x=left+i*dx;
                boolean active = i==0 || (!failed && (complete || (i==1 && amount>0) || (i==2 && amount>=70) || (i==3 && amount>=100)));
                g.setColor(active ? BLUE : Color.WHITE);
                g.fillOval(x-8,y-8,16,16);
                g.setColor(active ? BLUE : new Color(0xA7BEDC));
                g.drawOval(x-8,y-8,16,16);
                String label=names[i];
                g.setColor(active ? NAVY : MUTED);
                g.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 10));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label,x-fm.stringWidth(label)/2,y+29);
            }
            g.dispose();
        }
    }

    /** Draw icons with Java2D, avoiding unsupported Unicode glyphs on Windows. */
    private static final class IconCanvas extends JComponent {
        private final String kind;
        private final Color accent;
        IconCanvas(String kind, Color accent) {this.kind=kind;this.accent=accent;setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();
            g.setColor(new Color(255,255,255,175));
            int iconSize=Math.min(w-6,h-6);
            if(!"pets".equals(kind))g.fillRoundRect((w-iconSize)/2,(h-iconSize)/2,iconSize,iconSize,19,19);
            g.setColor(accent);
            g.setStroke(new BasicStroke(3.3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            if("upload".equals(kind)) {
                int cx=w/2, cy=h/2;
                g.drawLine(cx,cy-17,cx,cy+7);
                g.drawLine(cx-9,cy-1,cx,cy+8);
                g.drawLine(cx+9,cy-1,cx,cy+8);
                g.drawLine(cx-13,cy+11,cx-13,cy+19);
                g.drawLine(cx-13,cy+19,cx+13,cy+19);
                g.drawLine(cx+13,cy+19,cx+13,cy+11);
            } else if("csv".equals(kind)) {
                int cx=w/2,cy=h/2;
                g.setStroke(new BasicStroke(3));
                Path2D page=new Path2D.Double();
                page.moveTo(cx-20,cy-32);page.lineTo(cx+8,cy-32);
                page.lineTo(cx+22,cy-17);page.lineTo(cx+22,cy+29);
                page.lineTo(cx-20,cy+29);page.closePath();
                g.draw(page);
                g.drawLine(cx+8,cy-32,cx+8,cy-17);
                g.drawLine(cx+8,cy-17,cx+22,cy-17);
                g.setFont(new Font("Segoe UI",Font.BOLD,18));
                g.drawString("CSV",cx-18,cy+10);
            } else if("progress".equals(kind)) {
                g.drawRoundRect(w/2-10,h/2-10,20,20,5,5);
                g.drawLine(w/2-5,h/2+1,w/2,h/2+5);
                g.drawLine(w/2,h/2+5,w/2+7,h/2-4);
            } else if("document".equals(kind)) {
                g.drawRoundRect(w/2-10,h/2-12,20,24,3,3);
                for(int i=0;i<3;i++)g.drawLine(w/2-5,h/2-5+i*6,w/2+6,h/2-5+i*6);
            } else if("pets".equals(kind)) {
                g.setColor(new Color(0x80A8DF));
                // A simple paired paw illustration, intentionally independent of image files.
                paintPaw(g,w/2-28,h/2+10,1.05);
                g.setColor(new Color(0x3F7FC6));
                paintPaw(g,w/2+23,h/2-10,.78);
            }
            g.dispose();
        }
        private static void paintPaw(Graphics2D g,double x,double y,double s) {
            g.fill(new RoundRectangle2D.Double(x-13*s,y-3*s,26*s,23*s,11*s,11*s));
            g.fillOval((int)(x-23*s),(int)(y-14*s),(int)(10*s),(int)(13*s));
            g.fillOval((int)(x-11*s),(int)(y-24*s),(int)(10*s),(int)(13*s));
            g.fillOval((int)(x+2*s),(int)(y-24*s),(int)(10*s),(int)(13*s));
            g.fillOval((int)(x+14*s),(int)(y-14*s),(int)(10*s),(int)(13*s));
        }
    }
}
