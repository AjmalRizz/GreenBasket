package model;

import java.io.Serializable;
import java.util.Date;

/**
 * Base class for every staff account.
 *
 * It is abstract because a plain "User" is never created - every account is
 * either a StoreManager or a SalesAssistant. Each subclass answers the
 * permission questions (canManageUsers, canDeleteProducts, ...) for its role,
 * so the rest of the app never has to check "if role == ..." (polymorphism).
 */
public abstract class User implements Serializable {

    private static final long serialVersionUID = 2L;

    protected String username;
    protected String passwordHash;
    protected String fullName;
    protected Date createdAt;

    public User(String username, String passwordHash, String fullName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.createdAt = new Date();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    // --- Polymorphic Permission Methods ---
    public abstract String getRoleName();

    public abstract boolean canManageUsers();

    public abstract boolean canDeleteProducts();

    public abstract boolean canEditProducts();

    public abstract boolean canAddProducts();

    public abstract boolean canRestock();

    public abstract boolean canPerformSales();

    @Override
    public String toString() {
        return fullName + " (" + getRoleName() + ") [" + username + "]";
    }
}
