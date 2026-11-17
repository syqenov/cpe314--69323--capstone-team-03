package pawtrack.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PawTrackMessageDialogsTest {
    @Test
    void errorMessageIsNontechnicalAndActionable() {
        String message = PawTrackMessageDialogs.userFacingError("Could not complete adoption");
        assertTrue(message.contains("Could not complete adoption"));
        assertTrue(message.contains("try again"));
        assertFalse(message.contains("SQLException"));
    }

    @Test
    void blankTitleUsesSafeFallback() {
        String message = PawTrackMessageDialogs.userFacingError("  ");
        assertTrue(message.contains("could not be completed"));
    }
}
