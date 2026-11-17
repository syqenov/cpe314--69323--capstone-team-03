package pawtrack.controller;

import org.junit.jupiter.api.Test;
import pawtrack.dao.AdopterDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.dao.UserDao;
import pawtrack.dao.WaitlistDao;
import pawtrack.model.Adopter;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;
import pawtrack.model.User;
import pawtrack.service.AdoptionApplicationService;
import pawtrack.service.StatusHistoryService;
import pawtrack.service.WaitlistService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 1: authenticated adopter ID is the only identity; registration cannot duplicate accounts. */
class AdopterIdentityTest {

    private static WaitlistController queue(Database db, AuthController auth) {
        return new WaitlistController(new WaitlistService(), new WaitlistDao(db), new AdopterDao(db),
                new AnimalDao(db), new StatusHistoryService(), auth);
    }

    private static Animal availableDog(Database db) throws Exception {
        Animal dog = new Dog(0, "Buddy", "Retriever", 24, LocalDate.now().minusDays(3), AnimalStatus.AVAILABLE);
        new AnimalDao(db).insert(dog);
        return dog;
    }

    @Test
    void waitlistUsesAuthenticatedAdopterIdNotTypedNameOrEmail() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            User alice = auth.createAccount("alice", "Abcdef1!");
            Animal dog = availableDog(db);
            auth.login("alice", "Abcdef1!");
            AdopterDao adopters = new AdopterDao(db);
            int before = adopters.count();

            Adopter forged = new Adopter(alice.getAdopterId(), "Mallory", "mallory@example.com", "000");
            assertEquals(1, queue(db, auth).recordInterest(dog, forged));

            assertEquals(before, adopters.count());                                   // no new adopter row
            assertTrue(adopters.findByEmail("mallory@example.com").isEmpty());
            assertEquals("alice", adopters.findById(alice.getAdopterId()).orElseThrow().getName());
            assertEquals(1, new WaitlistDao(db).findByAdopterId(alice.getAdopterId()).size());
        }
    }

    @Test
    void adopterCannotRecordInterestForAnotherOrUnsavedIdentity() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            auth.createAccount("alice", "Abcdef1!");
            User bob = auth.createAccount("bob", "Abcdef1!");
            Animal dog = availableDog(db);
            auth.login("alice", "Abcdef1!");
            AdopterDao adopters = new AdopterDao(db);
            int before = adopters.count();
            WaitlistController controller = queue(db, auth);

            Adopter bobsProfile = adopters.findById(bob.getAdopterId()).orElseThrow();
            assertThrows(UnauthorizedException.class, () -> controller.recordInterest(dog, bobsProfile));
            assertThrows(UnauthorizedException.class,
                    () -> controller.recordInterest(dog, new Adopter("Bob", "bob@example.com", "")));

            assertEquals(before, adopters.count());
            assertTrue(new WaitlistDao(db).findByAdopterId(bob.getAdopterId()).isEmpty());
        }
    }

    private static Map<String, String> answers(String email) {
        Map<String, String> a = new HashMap<>();
        for (String key : new String[] {"phone", "address", "residence", "ownership", "pets_allowed",
                "pet_location", "secure_space", "household_agreement", "pets_before", "pets_now",
                "shelter_before", "experience", "reason", "caregiver", "expenses", "alone", "vet_care",
                "fallback"}) {
            a.put(key, "yes");
        }
        a.put("full_name", "Typed Name");
        a.put("email", email);
        a.put("household", "2");
        a.put("adult", "true");
        a.put("agreement", "true");
        return a;
    }

    @Test
    void applicationsBelongToTheAuthenticatedAdopterOnly() throws Exception {
        try (Database db = Database.openInMemory()) {
            AuthController auth = new AuthController(new UserDao(db));
            User alice = auth.createAccount("alice", "Abcdef1!");
            auth.createAccount("bob", "Abcdef1!");
            Animal dog = availableDog(db);
            AdoptionApplicationService apps = new AdoptionApplicationService(db, auth);

            auth.login("alice", "Abcdef1!");
            int appId = apps.submit(dog.getId(), answers("someone.else@example.com"));
            assertEquals(1, apps.mine().size());
            assertEquals(alice.getAdopterId().intValue(), apps.mine().get(0).adopterId());

            auth.login("bob", "Abcdef1!");
            assertTrue(apps.mine().isEmpty());
            assertThrows(IllegalStateException.class, () -> apps.cancel(appId));
            assertThrows(UnauthorizedException.class, apps::staffList);

            auth.login("alice", "Abcdef1!");
            assertEquals("PENDING_REVIEW", apps.mine().get(0).status());               // untouched by bob

            auth.login("admin", "admin123");
            assertEquals(1, apps.staffList().size());
            assertThrows(UnauthorizedException.class, () -> apps.submit(dog.getId(), answers("s@example.com")));
        }
    }

    @Test
    void registrationNeverCreatesDuplicateAccountsOrOrphans() throws Exception {
        try (Database db = Database.openInMemory()) {
            UserDao users = new UserDao(db);
            AuthController auth = new AuthController(users);
            AdopterDao adopters = new AdopterDao(db);
            int before = adopters.count();

            User alice = auth.createAccount("Alice", "Abcdef1!");
            assertNotNull(alice.getAdopterId());
            assertEquals(before + 1, adopters.count());

            for (String duplicate : new String[] {"Alice", "alice", " ALICE  "}) {
                assertThrows(IllegalArgumentException.class, () -> auth.createAccount(duplicate, "Abcdef1!"));
            }
            assertEquals(before + 1, adopters.count());                                // no extra adopter rows

            User carol = auth.createAccount("  carol  ", "Abcdef1!");
            assertEquals("carol", carol.getUsername());
            assertTrue(users.findByUsername("carol").isPresent());
            assertEquals(before + 2, adopters.count());
            assertFalse(alice.getAdopterId().equals(carol.getAdopterId()));
            assertEquals(0, users.countInvalidAdopterLinks());

            Adopter profile = adopters.findById(carol.getAdopterId()).orElseThrow();
            assertNotNull(profile.getCreatedAt());
            assertFalse(profile.getCreatedAt().isAfter(LocalDate.now()));
        }
    }
}
