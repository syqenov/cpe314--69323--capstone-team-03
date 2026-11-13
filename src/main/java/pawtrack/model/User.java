package pawtrack.model;

/**
 * Application user with role-based access.
 */
public final class User {
    private final Integer id;
    private final String username;
    private final String passwordHash;
    private final String passwordSalt;
    private final Role role;
    private final Integer adopterId;

    public User(Integer id, String username, String passwordHash, String passwordSalt, Role role, Integer adopterId) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.role = role;
        this.adopterId = adopterId;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public Role getRole() {
        return role;
    }

    public Integer getAdopterId() {
        return adopterId;
    }
}
