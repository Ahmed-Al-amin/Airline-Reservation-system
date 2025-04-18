package com.example.airline.model.dao;

import com.example.airline.data.DatabaseManager; // Use DatabaseManager
import com.example.airline.model.entity.User;

import java.sql.*; // Use java.sql
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Helper to map ResultSet to User object
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("username"),
                rs.getString("password"), // Retrieve plain password (Hashing needed!)
                rs.getString("role"),
                rs.getInt("approved") == 1, // Convert integer to boolean
                rs.getString("email"),      // Map new field
                rs.getString("phone_number") // Map new field
        );
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username"; // Add ordering
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
        }
        return users;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)"; // Case-insensitive lookup
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.toLowerCase()); // Use lowercase for query
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by username (" + username + "): " + e.getMessage());
        }
        return null;
    }

    public boolean addUser(User user) {
        // IMPORTANT: Hash the password before storing! Using plain text here for simplicity.
        // In a real app: String hashedPassword = HashUtil.hashPassword(user.getPassword());
        String sql = "INSERT INTO users(username, password, role, approved, email, phone_number) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword()); // Store HASHED password here
            pstmt.setString(3, user.getRole());
            pstmt.setInt(4, user.isApproved() ? 1 : 0); // Convert boolean to integer
            pstmt.setString(5, user.getEmail());       // Add new field
            pstmt.setString(6, user.getPhoneNumber()); // Add new field

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("SQLITE_CONSTRAINT_PRIMARYKEY")) {
                System.err.println("Error adding user: Username '" + user.getUsername() + "' already exists.");
            } else {
                System.err.println("Error adding user (" + user.getUsername() + "): " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Updates user information. Assumes password provided is already hashed if changed.
     * Role change is not typically done here, but included based on existing fields.
     */
    public boolean updateUser(User user) {
        // IMPORTANT: Hash the password in the service layer IF it's being changed!
        String sql = "UPDATE users SET password = ?, role = ?, approved = ?, email = ?, phone_number = ? WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getPassword()); // Store HASHED password if changed
            pstmt.setString(2, user.getRole());
            pstmt.setInt(3, user.isApproved() ? 1 : 0);
            pstmt.setString(4, user.getEmail());       // Update new field
            pstmt.setString(5, user.getPhoneNumber()); // Update new field
            pstmt.setString(6, user.getUsername());    // WHERE clause

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("DAO Warning: Update affected 0 rows for user " + user.getUsername() + ". User might not exist.");
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user (" + user.getUsername() + "): " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String username) {
        // Basic check to prevent deleting the root admin via DAO directly (should also be in service)
        if ("admin".equalsIgnoreCase(username)) {
            System.err.println("DAO Error: Attempted to delete root admin user directly.");
            return false;
        }
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("DAO Warning: Delete affected 0 rows for user " + username + ". User might not exist.");
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            // Foreign key constraints might prevent deletion if not handled properly (e.g., ON DELETE SET NULL for reservations)
            System.err.println("Error deleting user (" + username + "): " + e.getMessage());
            return false;
        }
    }
}