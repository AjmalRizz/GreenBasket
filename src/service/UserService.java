package service;

import model.SalesAssistant;
import model.StoreManager;
import model.User;
import util.FileHandler;
import util.SecurityUtil;
import util.ValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Login, account creation and account deletion.
 *
 * There are NO default accounts or passwords in the code. On first run the
 * user list is empty, and the app asks for the first Store Manager account
 * (see createFirstManager). After that, only a Store Manager can add staff.
 */
public class UserService {

    public static final String DEFAULT_FILE = "users.dat";
    static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 15 * 60 * 1000L; // 15 minutes

    private final String fileName;
    private final List<User> userList;

    // Failed-login tracking is kept in memory. It resets when the app restarts,
    // which is acceptable for a single-PC desktop app.
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, Long> lockedUntil = new HashMap<>();

    public UserService() {
        this(DEFAULT_FILE);
    }

    /** @param fileName where users are stored (tests pass a temp file) */
    public UserService(String fileName) {
        this.fileName = fileName;
        this.userList = FileHandler.loadFromFile(fileName);
    }

    /** True on first run, before any account exists. */
    public synchronized boolean hasNoUsers() {
        return userList.isEmpty();
    }

    /** Creates the very first account. Only allowed while there are no users yet. */
    public synchronized User createFirstManager(String username, String fullName, String password)
            throws ValidationException {
        if (!userList.isEmpty()) {
            throw new ValidationException("Setup has already been completed.");
        }
        return createAndSave(username, fullName, password, true);
    }

    /** Adds a staff account. Only a Store Manager may do this. */
    public synchronized User addUser(User operator, String username, String fullName,
                                     String password, boolean isManager) throws ValidationException {
        if (operator == null || !operator.canManageUsers()) {
            throw new ValidationException("Access denied: only a Store Manager can create accounts.");
        }
        return createAndSave(username, fullName, password, isManager);
    }

    /**
     * Checks the username and password.
     *
     * @return the logged-in user
     * @throws ValidationException with a message to show if login fails
     */
    public synchronized User login(String username, String password) throws ValidationException {
        if (isBlank(username) || password == null || password.isEmpty()) {
            throw new ValidationException("Please enter both username and password.");
        }
        String key = username.trim().toLowerCase();

        Long until = lockedUntil.get(key);
        if (until != null && until > System.currentTimeMillis()) {
            long minutesLeft = (until - System.currentTimeMillis()) / 60_000 + 1;
            throw new ValidationException("Account locked after " + MAX_FAILED_ATTEMPTS
                    + " failed attempts. Try again in " + minutesLeft + " minute(s).");
        }

        User user = findUser(key);
        if (user != null && SecurityUtil.verifyPassword(password, user.getPasswordHash())) {
            failedAttempts.remove(key);
            lockedUntil.remove(key);
            return user;
        }

        int attempts = failedAttempts.merge(key, 1, Integer::sum);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil.put(key, System.currentTimeMillis() + LOCKOUT_MS);
            failedAttempts.remove(key);
            throw new ValidationException("Too many failed attempts. Account locked for 15 minutes.");
        }
        // Same message whether the username or the password was wrong,
        // so an attacker cannot find out which usernames exist.
        throw new ValidationException("Invalid username or password. "
                + (MAX_FAILED_ATTEMPTS - attempts) + " attempt(s) left.");
    }

    /** Deletes an account. Only a Store Manager may do this, and never the last manager. */
    public synchronized void deleteUser(String username, User operator) throws ValidationException {
        if (operator == null || !operator.canManageUsers()) {
            throw new ValidationException("Access denied: only a Store Manager can delete accounts.");
        }
        if (username == null || username.equalsIgnoreCase(operator.getUsername())) {
            throw new ValidationException("You cannot delete your own account.");
        }
        User target = findUser(username);
        if (target == null) {
            throw new ValidationException("User not found: " + username);
        }
        if (target instanceof StoreManager && countManagers() <= 1) {
            throw new ValidationException("Cannot delete the only Store Manager.");
        }
        userList.remove(target);
        FileHandler.saveToFile(userList, fileName);
    }

    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(userList);
    }

    // ---------------------------------------------------------------- helpers

    private User createAndSave(String username, String fullName, String password, boolean isManager)
            throws ValidationException {
        if (isBlank(username)) throw new ValidationException("Username cannot be empty.");
        if (isBlank(fullName)) throw new ValidationException("Full name cannot be empty.");
        SecurityUtil.validatePasswordStrength(password);

        String cleanUsername = username.trim();
        if (findUser(cleanUsername) != null) {
            throw new ValidationException("Username '" + cleanUsername + "' is already taken.");
        }

        String hash = SecurityUtil.hashPassword(password);
        User user = isManager
                ? new StoreManager(cleanUsername, hash, fullName.trim())
                : new SalesAssistant(cleanUsername, hash, fullName.trim());

        userList.add(user);
        FileHandler.saveToFile(userList, fileName);
        return user;
    }

    private User findUser(String username) {
        for (User u : userList) {
            if (u.getUsername().equalsIgnoreCase(username.trim())) {
                return u;
            }
        }
        return null;
    }

    private int countManagers() {
        int count = 0;
        for (User u : userList) {
            if (u instanceof StoreManager) count++;
        }
        return count;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
