package pawtrack.ui;

import javax.swing.*;
import java.awt.Component;

/** Standard application message dialogs. Logging stays with the caller. */
final class PawTrackMessageDialogs {
    private PawTrackMessageDialogs() { }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /** The calling layer logs the exception; users see an actionable, nontechnical message. */
    static String userFacingError(String title) {
        String subject = title == null || title.isBlank() ? "The action could not be completed" : title.trim();
        return subject + ".\nPlease try again. If the problem continues, contact shelter staff.";
    }

    static void error(Component parent, String title, Exception failure) {
        JOptionPane.showMessageDialog(parent, userFacingError(title), "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
