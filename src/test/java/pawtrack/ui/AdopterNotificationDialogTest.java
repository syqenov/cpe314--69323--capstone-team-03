package pawtrack.ui;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.time.LocalDateTime;
import pawtrack.service.AdopterNotificationService;
import static org.junit.jupiter.api.Assertions.*;

class AdopterNotificationDialogTest {
    @Test void emptyStateIsUseful() {
        assertTrue(AdopterNotificationDialog.render(List.of()).contains("No reservation updates"));
    }

    @Test void rendersOwnReservationUpdate() {
        String text = AdopterNotificationDialog.render(List.of(
            new AdopterNotificationService.Update("Milo", "RESERVED", LocalDateTime.of(2026,11,16,9,0), "Reserved until tomorrow")));
        assertTrue(text.contains("Milo"));
        assertTrue(text.contains("RESERVED"));
    }
}
