package com.example.airline.model.service;

import com.example.airline.model.dao.UserDAO;
import com.example.airline.model.entity.User;
// Hypothetical Hashing Utility (Replace with actual implementation)
// import com.example.airline.util.HashUtil;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UserService {
    private final UserDAO userDAO = new UserDAO();
    // Root admin credentials (consider moving to config)
    private static final String ROOT_ADMIN_USERNAME = "admin";
    private static final String ROOT_ADMIN_PASSWORD = "adminpass"; // HASH THIS in production!

    public User login(String username, String password) {
        // Check for root admin first
        // IMPORTANT: Compare against HASHED root password in production
        if (ROOT_ADMIN_USERNAME.equalsIgnoreCase(username) && ROOT_ADMIN_PASSWORD.equals(password)) {
            // Return a transient User object for root admin (not stored in DB)
            // Provide nulls for email/phone as they aren't stored for root
            return new User(ROOT_ADMIN_USERNAME, ROOT_ADMIN_PASSWORD, "admin", true, null, null);
        }

        // Check regular users in DB
        User user = userDAO.getUserByUsername(username);
        if (user != null) {
            // --- PRODUCTION: HASH COMPARISON ---
            // if (HashUtil.verifyPassword(password, user.getPassword())) { return user; }
            // --- SIMPLIFIED PLAIN TEXT CHECK (INSECURE) ---
            if (user.getPassword().equals(password)) {
                // Don't allow login if admin is not approved (except root)
                if ("admin".equals(user.getRole()) && !user.isApproved()) {
                    System.out.println("Login failed for admin " + username + ": Account not approved.");
                    return null;
                }
                return user; // Login successful
            }
            // --- END INSECURE CHECK ---
            else {
                System.out.println("Login failed for user " + username + ": Incorrect password.");
            }
        } else {
            System.out.println("Login failed: User " + username + " not found.");
        }
        return null; // Login failed
    }


    public boolean signup(String username, String password, String role, String email, String phoneNumber) {
        // Basic validation
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() || role == null) {
            System.err.println("Signup failed: Username, password, and role cannot be empty.");
            return false;
        }
        // Add more validation for password complexity if needed


        if (userDAO.getUserByUsername(username) != null) {
            System.err.println("Signup failed: Username '" + username + "' already exists.");
            return false; // Username already exists
        }

        // --- PRODUCTION: HASH the password ---
        // String hashedPassword = HashUtil.hashPassword(password.trim());

        boolean isApproved;
        if ("admin".equalsIgnoreCase(role)) {
            isApproved = false; // Admins need approval
        } else {
            isApproved = true; // Passengers are auto-approved
            role = "passenger"; // Normalize role
        }

        // Create the User object with all details
        // --- Using Hashed Password (Production) ---
        // User newUser = new User(username.trim(), hashedPassword, role, isApproved, email, phoneNumber);
        // --- SIMPLIFIED PLAIN TEXT (INSECURE) ---
        User newUser = new User(username.trim(), password.trim(), role, isApproved, email, phoneNumber);
        // --- END INSECURE ---

        boolean added = userDAO.addUser(newUser);
        if (added) {
            System.out.println("Signup successful for user: " + username + (newUser.isApproved() ? "" : ". Awaiting admin approval."));
        } else {
            System.err.println("Signup failed for user " + username + " (database error).");
        }
        return added;
    }

    public boolean updateUserProfile(String username, String newPassword, String email, String phoneNumber) {
        User user = userDAO.getUserByUsername(username);
        if (user == null) {
            System.err.println("Service Error: Cannot update profile. User '" + username + "' not found.");
            return false;
        }

        // Update fields in the user object
        // Trim input, treat empty strings as null for storage consistency
        String finalEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : null;
        String finalPhone = (phoneNumber != null && !phoneNumber.trim().isEmpty()) ? phoneNumber.trim() : null;

        user.setEmail(finalEmail);
        user.setPhoneNumber(finalPhone);

        // Handle password change
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            // --- PRODUCTION: HASH the new password ---
            // String hashedPassword = HashUtil.hashPassword(newPassword.trim());
            // user.setPassword(hashedPassword);
            // --- SIMPLIFIED PLAIN TEXT (INSECURE) ---
            if (newPassword.length() < 4) { // Example minimal validation
                System.err.println("Service Error: New password too short.");
                return false;
            }
            user.setPassword(newPassword.trim());
            System.out.println("Service: Updating password for user " + username);
            // --- END INSECURE ---
        } else {
            System.out.println("Service: Updating profile (no password change) for user " + username);
        }

        // Call DAO to persist changes
        boolean updated = userDAO.updateUser(user);
        if (updated) {
            System.out.println("Profile updated successfully for user: " + username);
        } else {
            System.err.println("Failed to persist profile update for user " + username);
        }
        return updated;
    }


    public boolean approveAdmin(String username) {
        User user = userDAO.getUserByUsername(username);
        // Cannot approve root admin or non-existent users or non-admins or already approved
        if (user == null || ROOT_ADMIN_USERNAME.equalsIgnoreCase(username) ||
                !"admin".equals(user.getRole()) || user.isApproved()) {
            System.err.println("Cannot approve user: " + username + " (Not found, root, not admin, or already approved).");
            return false;
        }
        user.setApproved(true);
        boolean updated = userDAO.updateUser(user);
        if (updated) {
            System.out.println("Admin user " + username + " approved.");
        } else {
            System.err.println("Failed to update approval status for admin " + username);
        }
        return updated;
    }

    public boolean disapproveAdmin(String username) {
        User user = userDAO.getUserByUsername(username);
        // Cannot disapprove root admin, non-existent users, non-admins, or already disapproved
        if (user == null || ROOT_ADMIN_USERNAME.equalsIgnoreCase(username) ||
                !"admin".equals(user.getRole()) || !user.isApproved()) {
            System.err.println("Cannot disapprove user: " + username + " (Not found, root, not admin, or already disapproved).");
            return false;
        }
        user.setApproved(false); // Set approved to false
        boolean updated = userDAO.updateUser(user);
        if (updated) {
            System.out.println("Admin user " + username + " disapproved (approval revoked).");
        } else {
            System.err.println("Failed to update disapproval status for admin " + username);
        }
        return updated;
    }

    public List<User> getAllPendingAdmins() {
        try {
            return userDAO.getAllUsers().stream()
                    .filter(user -> user != null && "admin".equals(user.getRole()) && !user.isApproved())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Service Error getting pending admins: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<User> getAllUsers() {
        try {
            return userDAO.getAllUsers();
        } catch (Exception e) {
            System.err.println("Service Error getting all users: " + e.getMessage());
            return Collections.emptyList();
        }
    }


    public boolean deleteUser(String username, String loggedInAdminUsername) {
        // Prevent deleting oneself (especially the root admin)
        if (username == null || username.equalsIgnoreCase(loggedInAdminUsername)) {
            System.err.println("Service Error: Cannot delete the currently logged-in user account (" + username + ").");
            return false;
        }
        // Specifically prevent deleting the root admin, even if logged in as someone else (shouldn't happen)
        if (ROOT_ADMIN_USERNAME.equalsIgnoreCase(username)) {
            System.err.println("Service Error: The root admin account ('" + ROOT_ADMIN_USERNAME + "') cannot be deleted.");
            return false;
        }

        User userToDelete = userDAO.getUserByUsername(username);
        if (userToDelete == null) {
            System.err.println("Service Error: Cannot delete user '" + username + "'. User not found.");
            return false;
        }

        System.out.println("Attempting to delete user: " + username + " by admin: " + loggedInAdminUsername);
        boolean deleted = userDAO.deleteUser(username);

        if (deleted) {
            System.out.println("Successfully deleted user: " + username);
            // Add warnings about potential orphaned data if necessary
            System.out.println("Warning: Ensure related data (e.g., reservations set to NULL) is handled as expected.");
        } else {
            System.err.println("Failed to delete user: " + username + " (DAO error or constraints).");
        }
        return deleted;
    }

    public User getUserByUsername(String username) {
        User user = userDAO.getUserByUsername(username);
        return user;
    }
}