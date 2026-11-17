package pawtrack.controller;

import org.junit.jupiter.api.Test;
import pawtrack.dao.*;
import pawtrack.model.*;
import pawtrack.service.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class WaitlistAuthorizationTest {
    @Test
    void signedInAdopterCannotPollStaffWaitlist() throws Exception {
        try (Database db = Database.openInMemory()) {
            AnimalDao animalDao = new AnimalDao(db);
            AdopterDao adopterDao = new AdopterDao(db);
            WaitlistDao waitlistDao = new WaitlistDao(db);
            AuthController auth = new AuthController(new UserDao(db));
            auth.login("user", "user123").orElseThrow();
            WaitlistController controller = new WaitlistController(new WaitlistService(),
                    waitlistDao, adopterDao, animalDao, new StatusHistoryService(), auth);
            assertThrows(UnauthorizedException.class, () -> controller.pollNext(1));
        }
    }
}
