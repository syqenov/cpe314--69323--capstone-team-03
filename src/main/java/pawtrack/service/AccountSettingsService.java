package pawtrack.service;

import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.AdopterDao;
import pawtrack.model.Adopter;
import pawtrack.model.Role;
import pawtrack.model.User;
import java.sql.SQLException;
import java.util.Objects;

/** Account-scoped profile settings; never accepts an arbitrary adopter ID. */
public final class AccountSettingsService {
    private final AuthController auth;
    private final AdopterDao adopterDao;

    public AccountSettingsService(AuthController auth, AdopterDao adopterDao) {
        this.auth = Objects.requireNonNull(auth);
        this.adopterDao = Objects.requireNonNull(adopterDao);
    }

    public Adopter myProfile() throws SQLException {
        User u = auth.getCurrentUser();
        if (u == null || u.getRole() != Role.ADOPTER || u.getAdopterId() == null)
            throw new UnauthorizedException("An adopter account is required");
        return adopterDao.findById(u.getAdopterId())
                .orElseThrow(() -> new SQLException("Profile not found"));
    }

    public void updateMyContact(String name, String email, String phone) throws SQLException {
        Adopter own = myProfile();
        if (name == null || name.isBlank() || name.length() > 100)
            throw new IllegalArgumentException("Enter a name of up to 100 characters");
        if (email == null || email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Enter a valid email address");
        if (phone != null && phone.length() > 40)
            throw new IllegalArgumentException("Phone number is too long");
        adopterDao.updateContact(own.getId(), name.trim(), email.trim(), phone == null ? "" : phone.trim());
    }
}
