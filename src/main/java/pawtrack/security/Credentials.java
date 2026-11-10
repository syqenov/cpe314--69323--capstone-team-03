package pawtrack.security;

import java.util.Map;
import java.util.HashMap;

/**
 * Stores user credentials and roles.
 *
 * <p>Edit this file to add or change users. Format:
 * {@code put("username", new UserCredentials("password", Role.ADMIN));}
 * </p>
 */
public final class Credentials {

    public enum Role { ADMIN, USER }

    public static final class UserCredentials {
        public final String password;
        public final Role role;
        public UserCredentials(String password, Role role) {
            this.password = password;
            this.role = role;
        }
    }

    private static final Map<String, UserCredentials> USERS = new HashMap<>();

    static {
        // Default admin account - change the password
        USERS.put("admin", new UserCredentials("admin123", Role.ADMIN));
        // Default user account - change the password
        USERS.put("user", new UserCredentials("user123", Role.USER));
    }

    public static UserCredentials get(String username) {
        return USERS.get(username);
    }

    public static boolean contains(String username) {
        return USERS.containsKey(username);
    }
}
