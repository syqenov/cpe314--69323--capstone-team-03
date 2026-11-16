package pawtrack.service;

import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.AdopterDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.AdoptionRecordDao;
import pawtrack.model.AdoptionRecord;
import pawtrack.model.Role;
import java.sql.SQLException;
import java.util.Objects;

/** Read-only receipts, scoped to the logged-in adopter or staff. */
public final class AdoptionReceiptService {
    private final AuthController auth;
    private final AdoptionRecordDao records;
    private final AnimalDao animals;
    private final AdopterDao adopters;
    public AdoptionReceiptService(AuthController auth, AdoptionRecordDao records,
                                  AnimalDao animals, AdopterDao adopters) {
        this.auth = Objects.requireNonNull(auth);
        this.records = Objects.requireNonNull(records);
        this.animals = Objects.requireNonNull(animals);
        this.adopters = Objects.requireNonNull(adopters);
    }
    public String receiptFor(int recordId) throws SQLException {
        var user = auth.getCurrentUser();
        if (user == null) throw new UnauthorizedException("Login required");
        // Limit adopter queries to the account's own records. Return the same
        // authorization error for foreign and unknown receipt IDs to avoid
        // revealing whether another adopter's receipt exists.
        if (user.getRole() != Role.STAFF && user.getAdopterId() == null) {
            throw new UnauthorizedException("You may view only your own adoption receipt");
        }
        var permittedRecords = user.getRole() == Role.STAFF
                ? records.findAll()
                : records.findByAdopterId(user.getAdopterId());
        AdoptionRecord record = permittedRecords.stream()
                .filter(r -> r.getId() == recordId)
                .findFirst()
                .orElseThrow(() -> {
                    if (user.getRole() != Role.STAFF) {
                        return new UnauthorizedException("You may view only your own adoption receipt");
                    }
                    return new IllegalArgumentException("Receipt not found");
                });
        String animal = animals.findById(record.getAnimalId()).map(a -> a.getName()).orElse("Animal #"+record.getAnimalId());
        String adopter = adopters.findById(record.getAdopterId()).map(a -> a.getName()).orElse("Adopter #"+record.getAdopterId());
        return "PAWTRACK — ADOPTION CONFIRMATION\n" +
                "Receipt #" + record.getId() + "\n" +
                "Animal: " + animal + "\n" +
                "Adopter: " + adopter + "\n" +
                "Date: " + record.getAdoptionDate() + "\n" +
                String.format(java.util.Locale.US, "Fee recorded: %.2f\n", record.getFeePaid()) +
                "\nFor shelter recordkeeping.\n";
    }
}
