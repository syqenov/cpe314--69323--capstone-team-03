package pawtrack.ui;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Four-second PawTrack startup splash, sampled from the team's own opening video.
 * Uses Java Swing only (no VLC, codecs, ffmpeg, or external media player at runtime).
 *
 * All Swing operations run on the EDT; frame decoding happens in a background worker.
 * The skip button and Escape key immediately open the existing login panel.
 * On a missing/corrupt asset, the application safely continues to login.
 */
public final class PawTrackIntroScreen {
    private static final int DURATION_MS = 4_000;
    private static final int FRAMES_PER_SECOND = 10;
    private static final int FRAME_COUNT = 40;
    private static final int TIMER_DELAY_MS = 1000 / FRAMES_PER_SECOND;

    private PawTrackIntroScreen() { }

    public static void showThen(Runnable showLogin) {
        if (showLogin == null) {
            throw new IllegalArgumentException("showLogin is required");
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showThen(showLogin));
            return;
        }
        if (GraphicsEnvironment.isHeadless() || Boolean.getBoolean("pawtrack.skipIntro")) {
            showLogin.run();
            return;
        }
        try {
            new Player(showLogin).start();
        } catch (RuntimeException startupFailure) {
            System.err.println("PawTrack intro unavailable: " + startupFailure.getMessage());
            showLogin.run();
        }
    }

    private static BufferedImage readFrame(int frameIndex) throws IOException {
        String resourcePath = String.format("/pawtrack/intro/frame-%02d.jpg", frameIndex);
        try (InputStream in = PawTrackIntroScreen.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Missing intro resource " + resourcePath);
            }
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("Unreadable intro frame " + resourcePath);
            }
            return image;
        }
    }

    private static final class Player {
        private final Runnable showLogin;
        private final JWindow window = new JWindow();
        private final FrameCanvas canvas = new FrameCanvas();
        private final BufferedImage[] frames = new BufferedImage[FRAME_COUNT];
        private final Timer playback;
        private SwingWorker<BufferedImage[], Void> loader;
        private long playbackStartedAt;
        private boolean closed;

        private Player(Runnable showLogin) {
            this.showLogin = showLogin;
            playback = new Timer(TIMER_DELAY_MS, this::advance);
            playback.setCoalesce(true);
        }

        private void start() {
            try {
                frames[0] = readFrame(1);
            } catch (IOException missingIntro) {
                System.err.println("PawTrack intro skipped: " + missingIntro.getMessage());
                finish();
                return;
            }
            canvas.setFrame(frames[0]);
            configureWindow();
            preloadFrames();
            window.setVisible(true);
            playbackStartedAt = System.nanoTime();
            playback.start();
        }

        private void configureWindow() {
            Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            int width = Math.min(960, Math.max(480, bounds.width - 64));
            int height = Math.round(width * 9f / 16f);
            if (height > bounds.height - 64) {
                height = Math.max(270, bounds.height - 64);
                width = Math.round(height * 16f / 9f);
            }
            window.setSize(new Dimension(width, height));
            window.setLocationRelativeTo(null);

            JButton skip = new JButton("Skip Intro  >");
            skip.setFont(new Font("SansSerif", Font.BOLD, 13));
            skip.setForeground(Color.WHITE);
            skip.setBackground(new Color(27, 49, 87));
            skip.setOpaque(true);
            skip.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(new Color(169, 198, 241), 1, true),
                    javax.swing.BorderFactory.createEmptyBorder(4, 12, 4, 12)));
            skip.setFocusPainted(false);
            skip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            skip.setToolTipText("Skip the opening animation and go to Login");
            skip.addActionListener(event -> finish());

            JLayeredPane layers = new JLayeredPane() {
                @Override public void doLayout() {
                    int w = getWidth(), h = getHeight();
                    canvas.setBounds(0, 0, w, h);
                    skip.setBounds(Math.max(4, w - 148), Math.max(4, h - 56), 132, 36);
                }
            };
            layers.add(canvas, Integer.valueOf(0));
            layers.add(skip, Integer.valueOf(100));
            layers.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke("ESCAPE"), "skipIntro");
            layers.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke("SPACE"), "skipIntro");
            layers.getActionMap().put("skipIntro", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { finish(); }
            });
            window.setContentPane(layers);
        }

        private void preloadFrames() {
            loader = new SwingWorker<>() {
                @Override protected BufferedImage[] doInBackground() throws Exception {
                    BufferedImage[] loaded = new BufferedImage[FRAME_COUNT];
                    loaded[0] = frames[0];
                    for (int i = 1; i < FRAME_COUNT; i++) {
                        if (isCancelled()) break;
                        loaded[i] = readFrame(i + 1);
                    }
                    return loaded;
                }

                @Override protected void done() {
                    if (closed || isCancelled()) return;
                    try {
                        BufferedImage[] loaded = get();
                        System.arraycopy(loaded, 0, frames, 0, FRAME_COUNT);
                        // Display the current frame as soon as decoding completes.
                        int position = framePosition();
                        if (position < FRAME_COUNT && frames[position] != null) {
                            canvas.setFrame(frames[position]);
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } catch (CancellationException ignored) {
                        // User skipped the splash while it was loading.
                    } catch (ExecutionException ex) {
                        System.err.println("PawTrack intro frames unavailable: " + ex.getCause());
                        finish();
                    }
                }
            };
            loader.execute();
        }

        private int framePosition() {
            long elapsedMs = (System.nanoTime() - playbackStartedAt) / 1_000_000L;
            return Math.max(0, Math.min(FRAME_COUNT, (int) (elapsedMs / TIMER_DELAY_MS)));
        }

        private void advance(ActionEvent e) {
            if (closed) return;
            int position = framePosition();
            if (position >= FRAME_COUNT ||
                (System.nanoTime() - playbackStartedAt) / 1_000_000L >= DURATION_MS) {
                finish();
                return;
            }
            if (frames[position] != null) {
                canvas.setFrame(frames[position]);
            }
        }

        private void finish() {
            if (closed) return;
            closed = true;
            playback.stop();
            if (loader != null) loader.cancel(true);
            window.dispose();
            showLogin.run();
        }
    }

    private static final class FrameCanvas extends JComponent {
        private BufferedImage frame;
        private void setFrame(BufferedImage frame) { this.frame = frame; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(new Color(233, 240, 255));
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (frame != null) {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
                }
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, h - 90, new Color(11, 28, 57, 0),
                        0, h, new Color(11, 28, 57, 155)));
                g2.fillRect(0, h - 90, getWidth(), 90);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("Welcome to PawTrack", 22, h - 28);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(178, 200, 232, 170));
                g2.drawLine(22, h - 12, getWidth() - 22, h - 12);
            } finally {
                g2.dispose();
            }
        }
    }
}
