package pawtrack.ui;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.geom.Path2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * A single-window, two-stage video-backed login layout.
 *
 * Stage 1 (5 seconds): full-window intro, shown once per app launch.
 * Stage 2: left video loops only the latter half of the team's MP4 while
 * the existing Swing login form is visible on the right. Skip/Escape
 * jumps straight to stage 2 without creating or disposing another window.
 *
 * The MP4 was sampled to bundled JPEG frames at build time, so Windows users
 * do not need video codecs, JavaFX, FFmpeg, or an external player at runtime.
 * Frames are decoded on a background thread; all Swing updates run on the EDT.
 */
public final class PawTrackStartupPanel extends JLayeredPane {
    public static final int INTRO_DURATION_MS = 5_000;
    public static final int FPS = 8;
    public static final int INTRO_FRAME_COUNT = 40;
    public static final int LOOP_FRAME_COUNT = 40;
    private static final int FRAME_MS = 1000 / FPS;
    private static final String RESOURCE_BASE = "/pawtrack/intro-loop/";

    private final BufferedImage[] introFrames = new BufferedImage[INTRO_FRAME_COUNT];
    private final BufferedImage[] loopFrames = new BufferedImage[LOOP_FRAME_COUNT];
    private final VideoCanvas video = new VideoCanvas();
    private final WaveOverlay wave = new WaveOverlay();
    private final LoginFadeLayer fadingLogin = new LoginFadeLayer();
    private final JComponent loginForm;
    private final JButton skipButton = new SkipButton();
    private final Timer timer;
    private final boolean playIntro;
    private SwingWorker<Void, Void> frameLoader;
    private long phaseStartedNanos;
    private long fadeStartedNanos;
    private static final long FADE_DURATION_NANOS = 1_400_000_000L;
    private boolean inLoop;
    private boolean running;
    private boolean stopped;

    public PawTrackStartupPanel(JComponent loginForm, boolean playIntro) {
        if (loginForm == null) throw new IllegalArgumentException("Login form is required");
        this.loginForm = loginForm;
        this.playIntro = playIntro && !Boolean.getBoolean("pawtrack.skipIntro");
        this.inLoop = !this.playIntro;
        setOpaque(true);
        setBackground(new Color(0xE6EFFF));
        setPreferredSize(new Dimension(1080, 690));
        setMinimumSize(new Dimension(850, 550));
        setDoubleBuffered(true);

        // A frame from each segment is available before playback starts,
        // even if background decoding takes a moment on a slower machine.
        try { introFrames[0] = readFrame("intro", 1); }
        catch (IOException ex) { System.err.println("PawTrack intro frame unavailable: " + ex.getMessage()); }
        try { loopFrames[0] = readFrame("loop", 1); }
        catch (IOException ex) { System.err.println("PawTrack loop frame unavailable: " + ex.getMessage()); }

        // The login form and its grid container have no painted rectangle:
        // the continuous curved surface is painted once by WaveOverlay.
        loginForm.setOpaque(false);
        fadingLogin.setLayout(null);
        fadingLogin.setOpaque(false);
        fadingLogin.add(loginForm);

        video.setFrame(inLoop ? loopFrames[0] : introFrames[0]);
        add(video, Integer.valueOf(0));
        add(wave, Integer.valueOf(100));
        add(fadingLogin, Integer.valueOf(150));
        add(skipButton, Integer.valueOf(200));
        wave.setVisible(inLoop);
        fadingLogin.setVisible(inLoop);
        fadingLogin.setOpacity(inLoop ? 1f : 0f);
        skipButton.setVisible(!inLoop);
        skipButton.addActionListener(e -> skipIntro());

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "skipOpening");
        getActionMap().put("skipOpening", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!inLoop) skipIntro();
            }
        });

        timer = new Timer(40, this::onTimer);
        timer.setCoalesce(true);
    }

    /** Call after the JFrame becomes visible. Safe to call more than once. */
    public void start() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::start);
            return;
        }
        if (running || stopped) return;
        running = true;
        phaseStartedNanos = System.nanoTime();
        preloadFrames();
        timer.start();
    }

    /** Stops animation and background frame loading when leaving Login. */
    public void stop() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::stop);
            return;
        }
        if (stopped) return;
        stopped = true;
        running = false;
        timer.stop();
        if (frameLoader != null) frameLoader.cancel(true);
    }

    /** Switches to the looping left-hand video and reveals login in-place. */
    public void skipIntro() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::skipIntro);
            return;
        }
        if (stopped || inLoop) return;
        enterLoop();
    }

    public boolean isLooping() { return inLoop; }

    private void enterLoop() {
        inLoop = true;
        phaseStartedNanos = System.nanoTime();
        video.setFrame(loopFrames[0] != null ? loopFrames[0] : introFrames[INTRO_FRAME_COUNT - 1]);
        fadeStartedNanos = System.nanoTime();
        wave.setVisible(true);
        fadingLogin.setOpacity(0f);
        fadingLogin.setVisible(true);
        skipButton.setVisible(false);
        revalidate();
        doLayout();
        repaint();
    }

    
    private static int splitAt(int width) {
        // Give the video space while reserving enough room for Login.
        return Math.max(
                240,
                Math.min(Math.round(width * 0.58f), width - 545)
        );
}


@Override
public void doLayout() {
    int width = Math.max(1, getWidth());
    int height = Math.max(1, getHeight());

    if (inLoop) {
        int split = splitAt(width);

        // Reserve space for the login panel.
        int formStart = split + 40;

        // Extend the video behind the curve.
        video.setBounds(
                0, 0,
                Math.min(width, split + 65),
                height
        );

        // Draw the curve across the entire window.
        wave.setBounds(0, 0, width, height);

        // Display the login panel on the right.
        fadingLogin.setBounds(
                formStart, 0,
                Math.max(0, width - formStart),
                height
        );

        fadingLogin.doLayout();

    } else {
        // Intro occupies the entire window.
        video.setBounds(0, 0, width, height);
        wave.setBounds(0, 0, width, height);
        fadingLogin.setBounds(width, 0, 0, height);
    }

    skipButton.setBounds(
            Math.max(12, width - 148),
            Math.max(12, height - 63),
            126, 40
    );
}


    private void onTimer(ActionEvent ignored) {
        if (stopped || !running) return;
        long now = System.nanoTime();
        long elapsedMs = Math.max(0, (now - phaseStartedNanos) / 1_000_000L);
        if (inLoop && fadingLogin.opacity() < 1f) {
            double p = Math.min(1.0, Math.max(0.0,
                    (now - fadeStartedNanos) / (double) FADE_DURATION_NANOS));
            // Smoothstep easing: gentle start and finish, no abrupt flash.
            fadingLogin.setOpacity((float) (p * p * (3.0 - 2.0 * p)));
        }
        if (!inLoop && elapsedMs >= INTRO_DURATION_MS) {
            enterLoop();
            return;
        }
        BufferedImage[] frames = inLoop ? loopFrames : introFrames;
        int pos = (int) (elapsedMs / FRAME_MS);
        if (inLoop) pos %= LOOP_FRAME_COUNT;
        else pos = Math.min(pos, INTRO_FRAME_COUNT - 1);
        if (frames[pos] != null) video.setFrame(frames[pos]);
    }

    private void preloadFrames() {
        frameLoader = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                // Interleave the two segments so immediate Skip never waits
                // for the entire opening to be decoded before loop playback.
                for (int i = 1; i < Math.max(INTRO_FRAME_COUNT, LOOP_FRAME_COUNT) && !isCancelled(); i++) {
                    if (i < INTRO_FRAME_COUNT) {
                        try { introFrames[i] = readFrame("intro", i + 1); }
                        catch (IOException e) { System.err.println("Skipping intro frame: " + e.getMessage()); }
                    }
                    if (i < LOOP_FRAME_COUNT) {
                        try { loopFrames[i] = readFrame("loop", i + 1); }
                        catch (IOException e) { System.err.println("Skipping loop frame: " + e.getMessage()); }
                    }
                }
                return null;
            }
            @Override protected void done() {
                if (stopped) return;
                try { get(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (CancellationException ignored) { }
                catch (ExecutionException e) {
                    System.err.println("PawTrack video frame preload failed: " + e.getCause());
                }
            }
        };
        frameLoader.execute();
    }

    private static BufferedImage readFrame(String stage, int oneBasedIndex) throws IOException {
        String resource = RESOURCE_BASE + stage + "-" + String.format("%03d", oneBasedIndex) + ".jpg";
        try (InputStream in = PawTrackStartupPanel.class.getResourceAsStream(resource)) {
            if (in == null) throw new IOException("Missing " + resource);
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IOException("Invalid image " + resource);
            return image;
        }
    }

    /** A single continuous soft-blue surface with the curved/wave edge. */
    private final class WaveOverlay extends JComponent {
        WaveOverlay() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            if (!inLoop) return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
        
                double s = splitAt(w);

                Path2D.Double surface = new Path2D.Double();

                // Start near the top
                surface.moveTo(s + 8, 0);

                // Upper inward curve
                surface.curveTo(
                        s - 45, h * 0.10,
                        s - 86, h * 0.24,
                        s - 66, h * 0.34
                );

                // Gentle outward curve through the middle
                surface.curveTo(
                        s - 42, h * 0.45,
                        s + 8,  h * 0.52,
                        s - 24, h * 0.63
                );

                // Lower inward curve
                surface.curveTo(
                        s - 47, h * 0.72,
                        s - 70, h * 0.78,
                        s - 66, h * 0.85
                );

                // Smooth outward finish
                surface.curveTo(
                        s - 58, h * 0.92,
                        s - 9,  h * 0.98,
                        s + 8,  h
                );



                surface.lineTo(w, h);
                surface.lineTo(w, 0);
                surface.closePath();
                g.setPaint(new GradientPaint((float) s, 0,
                        new Color(0xF8FBFF), w, h, new Color(0xE8F0FF)));
                g.fill(surface);
            } finally { g.dispose(); }
        }
    }

    /** The login is native Swing; only painting fades, not its layout. */
    private static final class LoginFadeLayer extends JComponent {
        private float opacity = 1f;
        LoginFadeLayer() { setOpaque(false); }
        void setOpacity(float value) {
            float next = Math.max(0f, Math.min(1f, value));
            if (Math.abs(next - opacity) > .001f) { opacity = next; repaint(); }
        }
        float opacity() { return opacity; }
        @Override public void doLayout() {
            for (java.awt.Component child : getComponents()) {
                child.setBounds(0, 0, getWidth(), getHeight());
            }
        }
        @Override protected void paintChildren(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setComposite(AlphaComposite.SrcOver.derive(opacity));
                super.paintChildren(g);
            } finally { g.dispose(); }
        }
    }

    public boolean isLoginFullyRevealed() {
        return inLoop && fadingLogin.opacity() >= .999f;
    }

    private static final class VideoCanvas extends JComponent {
        private BufferedImage current;
        private void setFrame(BufferedImage frame) {
            if (frame != null && current != frame) {
                current = frame;
                repaint();
            }
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                int w = getWidth(), h = getHeight();
                g.setPaint(new GradientPaint(0, 0, new Color(0xB6D0F6), w, h, new Color(0xE9F3FF)));
                g.fillRect(0, 0, w, h);
                if (current != null) {
                    double scale = Math.max(w / (double) current.getWidth(), h / (double) current.getHeight());
                    int dw = (int) Math.ceil(current.getWidth() * scale);
                    int dh = (int) Math.ceil(current.getHeight() * scale);
                    int x = (w - dw) / 2, y = (h - dh) / 2;
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(current, x, y, dw, dh, null);
                } else {
                    g.setFont(new Font("Segoe UI", Font.BOLD, 32));
                    g.setColor(new Color(0x1B3157));
                    g.drawString("PawTrack", Math.max(25, w/2 - 80), h/2);
                }
            } finally {
                g.dispose();
            }
        }
    }

    private static final class SkipButton extends JButton {
        private SkipButton() {
            super("Skip Intro  >");
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Go straight to Login (Esc)");
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setComposite(AlphaComposite.SrcOver.derive(getModel().isRollover() ? .88f : .67f));
                g.setColor(new Color(17, 39, 80));
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth()-1, getHeight()-1, 24, 24));
            } finally { g.dispose(); }
            super.paintComponent(graphics);
        }
    }
}
