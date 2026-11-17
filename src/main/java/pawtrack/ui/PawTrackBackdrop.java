package pawtrack.ui;

import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * Paint-only light-theme wallpaper. No photos, fonts, controllers or persistence.
 * Draws at runtime so decorations scale with the available window size.
 */
public final class PawTrackBackdrop {
    private PawTrackBackdrop() { }

    public static void paint(Graphics source, JComponent panel) {
        int w = panel.getWidth();
        int h = panel.getHeight();
        if (w <= 0 || h <= 0) return;
        Graphics2D g = (Graphics2D) source.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, PawTrackTheme.isDark() ? PawTrackTheme.DARK_CANVAS : new Color(0xEDF5FF), w, h, PawTrackTheme.isDark() ? new Color(0x172A42) : new Color(0xFAFCFF)));
            g.fillRect(0, 0, w, h);

            // Large overlapping curves stay at the edges, outside the content focus.
            g.setColor((PawTrackTheme.isDark() ? new Color(0x37577E) : new Color(0xA8CAFA)));
            g.setComposite(AlphaComposite.SrcOver.derive(0.19f));
            g.fill(new Ellipse2D.Double(w - 275, -185, 445, 380));
            g.setColor((PawTrackTheme.isDark() ? new Color(0x36537A) : new Color(0x78A5E6)));
            g.setComposite(AlphaComposite.SrcOver.derive(0.09f));
            g.fill(new Ellipse2D.Double(w - 125, -110, 270, 265));

            Path2D.Double wave = new Path2D.Double();
            wave.moveTo(0, h);
            wave.lineTo(0, Math.max(0, h - 180));
            wave.curveTo(w * 0.26, h - 240, w * 0.40, h + 12, w * 0.75, h - 80);
            wave.curveTo(w * 0.90, h - 115, w, h - 95, w, h - 95);
            wave.lineTo(w, h);
            wave.closePath();
            g.setComposite(AlphaComposite.SrcOver.derive(0.14f));
            g.setColor((PawTrackTheme.isDark() ? new Color(0x294767) : new Color(0xC3DAF9)));
            g.fill(wave);

            // Tiny paw watermarks: barely visible, away from central tables/cards.
            g.setComposite(AlphaComposite.SrcOver.derive(0.12f));
            g.setColor((PawTrackTheme.isDark() ? new Color(0x4A6387) : new Color(0x6B9DDE)));
            paw(g, w - 74, Math.max(90, h / 3), 0.85);
            paw(g, Math.max(45, w - 175), Math.max(130, h / 3 + 95), 0.55);
            paw(g, 48, Math.max(60, h - 96), 0.65);
            if (h > 450) paw(g, 135, h - 54, 0.39);
        } finally {
            g.dispose();
        }
    }

    private static void paw(Graphics2D g, double cx, double cy, double scale) {
        oval(g, cx - 17*scale, cy + 2*scale, 34*scale, 26*scale);
        oval(g, cx - 27*scale, cy - 16*scale, 11*scale, 15*scale);
        oval(g, cx - 12*scale, cy - 26*scale, 11*scale, 15*scale);
        oval(g, cx + 5*scale, cy - 25*scale, 11*scale, 15*scale);
        oval(g, cx + 20*scale, cy - 13*scale, 11*scale, 15*scale);
    }

    private static void oval(Graphics2D g, double x, double y, double width, double height) {
        g.fill(new Ellipse2D.Double(x, y, width, height));
    }
}
