package pawtrack.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ImportControllerCancelTest {

    @Test
    @DisplayName("Cancel does nothing when no import is running (edge case)")
    void cancelWhenIdleReturnsFalse() {
        ImportController controller = new ImportController(null, null, null);

        assertFalse(controller.isBusy());
        assertFalse(controller.cancel());
    }
}
