package pawtrack.controller;

import org.junit.jupiter.api.Test;
import pawtrack.dao.*;
import pawtrack.service.*;
import static org.junit.jupiter.api.Assertions.*;

class PawTrackWiringTest {
    @Test
    void wiringRequiresLoginAndConstructsSecuredControllers() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            assertThrows(IllegalArgumentException.class, () -> new PawTrackWiring(db, auth,
                    new WaitlistService(), new StatusHistoryService(), new AnalyticsService()));
            auth.login("admin", "admin123").orElseThrow();
            PawTrackWiring wiring = new PawTrackWiring(db, auth,
                    new WaitlistService(), new StatusHistoryService(), new AnalyticsService());
            assertNotNull(wiring.animals);
            assertNotNull(wiring.adoptions);
            assertNotNull(wiring.receipts);
        }
    }
}
