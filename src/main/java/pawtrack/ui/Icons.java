package pawtrack.ui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.InputStream;

/** Loads application icons bundled in {@code src/main/resources}. */
public final class Icons {

    private Icons() {
    }

    /** The PawTrack window/taskbar icon (square 512px, rasterised from pawtrack-logo.svg). */
    public static Image appIcon() {
        try (InputStream in = Icons.class.getResourceAsStream("/PawTrack-app-icon.png")) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {
            // icon is optional
        }
        return null;
    }
}