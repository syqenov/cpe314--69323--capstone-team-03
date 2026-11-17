package pawtrack.service;

import pawtrack.controller.*;
import pawtrack.dao.*;

import java.sql.SQLException;

/**
 * Application composition root. Owns dependency construction, independent of Swing UI.
 * MainFrame receives the already-wired controllers and services.
 */
public final class PawTrackWiring {
    public final ClassificationService classification;
    public final AnimalController animals;
    public final AdoptionController adoptions;
    public final WaitlistController waitlists;
    public final ReservationService reservations;
    public final AdopterNotificationService notifications;
    public final NotificationReadService notificationReads;
    public final AccountSettingsService accountSettings;
    public final AccountPreferencesService accountPreferences;
    public final AdoptionApplicationService adoptionApplications;
    public final ApplicationReviewService applicationReviews;
    public final MyApplicationStatusService myApplications;
    public final AnalyticsController analytics;
    public final ClassificationController classifications;
    public final ImportController imports;
    public final AdoptionReceiptService receipts;

    public PawTrackWiring(Database database, AuthController auth,
                          WaitlistService waitlist, StatusHistoryService history,
                          AnalyticsService analyticsService) throws SQLException {
        if (database == null || auth == null || auth.getCurrentUser() == null) {
            throw new IllegalArgumentException("Authenticated application session required");
        }
        AnimalDao animalDao = new AnimalDao(database);
        AdopterDao adopterDao = new AdopterDao(database);
        AdoptionRecordDao recordDao = new AdoptionRecordDao(database);
        ClassificationDao classificationDao = new ClassificationDao(database);
        WaitlistDao waitlistDao = new WaitlistDao(database);
        ReservationDao reservationDao = new ReservationDao(database);
        classification = ClassificationService.loadOrSeed(classificationDao);
        ShelterSeeder.seedIfEmpty(database, classification);

        animals = new AnimalController(animalDao, auth);
        adoptions = new AdoptionController(database, animalDao, adopterDao, recordDao, history, auth);
        reservations = new ReservationService(database, reservationDao, animalDao, auth);
        myApplications = new MyApplicationStatusService(auth, reservationDao);
        notifications = new AdopterNotificationService(auth, reservationDao);
        notificationReads = new NotificationReadService(database, auth, reservationDao);
        accountSettings = new AccountSettingsService(auth, adopterDao);
        accountPreferences = new AccountPreferencesService(database, auth);
        adoptionApplications = new AdoptionApplicationService(database, auth);
        applicationReviews = new ApplicationReviewService(auth, database, reservationDao);
        waitlists = new WaitlistController(waitlist, waitlistDao, adopterDao, animalDao, history, auth, reservationDao);
        analytics = new AnalyticsController(animalDao, recordDao, analyticsService, waitlist, classification);
        classifications = new ClassificationController(classification, animalDao);
        imports = new ImportController(database, classification, auth);
        receipts = new AdoptionReceiptService(auth, recordDao, animalDao, adopterDao);
        waitlists.loadFromDb();
    }
}
