package pawtrack.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Small helpers for rounded Swing components. Swing has no native
 * border-radius, so these paint a rounded background/outline themselves.
 */
public final class RoundedUi {

    private RoundedUi() {
    }

    /** Offset (px) of the drop shadow drawn behind each rounded box. */
    private static final int SHADOW_OFFSET = 3;
    /** Translucent black used for the drop shadow. */
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 45);

    /** Margin kept around a button so the hover scale never clips. */
    private static final int BUTTON_MARGIN = 3;
    /** How much a button grows when the pointer is over it. */
    private static final float HOVER_SCALE = 1.05f;

    private static void paintShadowedRoundRect(Graphics2D g2, int w, int h, int radius, Color fill) {
        g2.setColor(SHADOW_COLOR);
        g2.fillRoundRect(SHADOW_OFFSET, SHADOW_OFFSET, w, h, radius, radius);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w, h, radius, radius);
    }

    /**
     * Prepares a password field for display. The eye icon is painted inside the
     * field itself (see {@link RoundedPasswordField}), so this only fixes its size.
     */
    public static JPasswordField passwordRow(JPasswordField field, int fieldWidth) {
        field.setPreferredSize(new Dimension(fieldWidth, 36));
        field.setMinimumSize(new Dimension(fieldWidth, 36));
        field.setMaximumSize(new Dimension(fieldWidth, 36));
        return field;
    }

    /** Rounded outline border (like CSS border-radius + border). */
    public static final class RoundedBorder implements javax.swing.border.Border {
        private final int radius;
        private final Color color;
        private final int thickness;

        public RoundedBorder(int radius, Color color, int thickness) {
            this.radius = radius;
            this.color = color;
            this.thickness = thickness;
        }

        @Override
        public java.awt.Insets getBorderInsets(java.awt.Component c) {
            return new java.awt.Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(java.awt.Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }
    }

    /** Text field with rounded corners. */
    public static class RoundedTextField extends JTextField {
        private final int radius;

        public RoundedTextField(int columns, int radius) {
            super(columns);
            this.radius = radius;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - SHADOW_OFFSET;
            int h = getHeight() - SHADOW_OFFSET;
            paintShadowedRoundRect(g2, w, h, radius, getBackground());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Password field with rounded corners and an in-field show/hide eye icon. */
    public static class RoundedPasswordField extends JPasswordField {
        private static final int EYE_SIZE = 20;
        private static final int EYE_MARGIN = 8;
        private final int radius;

        public RoundedPasswordField(int columns, int radius) {
            super(columns);
            this.radius = radius;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, EYE_SIZE + EYE_MARGIN + 6));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isOverEye(e.getX())) {
                        boolean visible = getEchoChar() == (char) 0;
                        setEchoChar(visible ? '•' : (char) 0);
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.TEXT_CURSOR));
                }
            });
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(isOverEye(e.getX())
                            ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                            : java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.TEXT_CURSOR));
                }
            });
        }

        private boolean isOverEye(double x) {
            int eyeX = getWidth() - EYE_SIZE - EYE_MARGIN;
            return x >= eyeX && x <= eyeX + EYE_SIZE;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - SHADOW_OFFSET;
            int h = getHeight() - SHADOW_OFFSET;
            paintShadowedRoundRect(g2, w, h, radius, getBackground());
            g2.dispose();
            super.paintComponent(g);
            paintEye(g);
        }

        private void paintEye(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = getWidth() - EYE_SIZE - EYE_MARGIN;
            int y = (getHeight() - EYE_SIZE) / 2;
            g2.setColor(new Color(0x5B8AB0));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawArc(x, y, EYE_SIZE, EYE_SIZE, 20, 140);
            g2.drawArc(x, y, EYE_SIZE, EYE_SIZE, 200, 140);
            g2.fillOval(x + EYE_SIZE / 2 - 2, y + EYE_SIZE / 2 - 2, 4, 4);
            if (getEchoChar() != (char) 0) {
                g2.drawLine(x + 2, y + EYE_SIZE - 2, x + EYE_SIZE - 2, y + 2);
            }
            g2.dispose();
        }
    }

    /** Button with rounded corners, animated ease-in-out hover scale, and press feedback. */
    public static class RoundedButton extends JButton {
        /** Duration of one grow/shrink animation (ms). */
        private static final int ANIM_MS = 150;
        /** Wait before the grow animation starts after the pointer enters (ms). */
        private static final int HOVER_DELAY_MS = 120;
        /** Repaint interval of the animation (ms). */
        private static final int TICK_MS = 15;

        private final int radius;
        private Color baseColor;
        private final javax.swing.Timer animTimer;
        private javax.swing.Timer delayTimer;

        private boolean pressed;
        private float scale = 1.0f;
        private float fromScale = 1.0f;
        private float targetScale = 1.0f;
        private long animStart = 0L;
        private String tooltipText;

        public RoundedButton(String text, int radius, Color baseColor) {
            super(text);
            this.radius = radius;
            this.baseColor = baseColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            setForeground(Color.WHITE);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

            animTimer = new javax.swing.Timer(TICK_MS, e -> stepAnimation());
            animTimer.setRepeats(true);
            delayTimer = new javax.swing.Timer(HOVER_DELAY_MS, e -> {
                delayTimer.stop();
                animateTo(HOVER_SCALE);
            });
            delayTimer.setRepeats(false);

            MouseAdapter listener = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    delayTimer.restart();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    delayTimer.stop();
                    pressed = false;
                    animateTo(1.0f);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                }
            };
            addMouseListener(listener);
        }

        /** Sets the fill color (used for the selected tab state). */
        public void setBaseColor(Color color) {
            this.baseColor = color;
            repaint();
        }

        /** Sets a tooltip that appears after a short delay. */
        public RoundedButton setTooltip(String text) {
            this.tooltipText = text;
            if (text != null && !text.isEmpty()) {
                setToolTipText(text);
            }
            return this;
        }

        private void animateTo(float target) {
            fromScale = scale;
            targetScale = target;
            animStart = System.currentTimeMillis();
            if (!animTimer.isRunning()) {
                animTimer.start();
            }
        }

        private void stepAnimation() {
            long elapsed = System.currentTimeMillis() - animStart;
            double t = Math.min(1.0, elapsed / (double) ANIM_MS);
            double eased = t * t * (3 - 2 * t); // smoothstep: ease-in-out
            scale = (float) (fromScale + (targetScale - fromScale) * eased);
            if (t >= 1.0) {
                scale = targetScale;
                animTimer.stop();
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (scale != 1.0f) {
                double cx = getWidth() / 2.0;
                double cy = getHeight() / 2.0;
                g2.translate(cx, cy);
                g2.scale(scale, scale);
                g2.translate(-cx, -cy);
            }
            Color fill = pressed ? baseColor.darker() : scale > 1.0f ? baseColor.brighter() : baseColor;
            int m = BUTTON_MARGIN;
            int w = getWidth() - m * 2 - SHADOW_OFFSET;
            int h = getHeight() - m * 2 - SHADOW_OFFSET;
            g2.setColor(SHADOW_COLOR);
            g2.fillRoundRect(m + SHADOW_OFFSET, m + SHADOW_OFFSET, w, h, radius, radius);
            g2.setColor(fill);
            g2.fillRoundRect(m, m, w, h, radius, radius);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    /**
     * Combo box that paints its own rounded blue background and drop shadow,
     * matching the button design (JComboBox ignores setBackground under most L&Fs).
     */
    public static class RoundedComboBox extends JComboBox<String> {
        private final int radius;
        private boolean hover;

        public RoundedComboBox(int radius) {
            this.radius = radius;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            setBackground(new Color(0x2B5B8A));
            setForeground(Color.BLACK);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - SHADOW_OFFSET;
            int h = getHeight() - SHADOW_OFFSET;
            g2.setColor(SHADOW_COLOR);
            g2.fillRoundRect(SHADOW_OFFSET, SHADOW_OFFSET, w, h, radius, radius);
            g2.setColor(hover ? getBackground().brighter() : getBackground());
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}