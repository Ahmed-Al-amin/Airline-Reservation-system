package com.example.airline.model.entity;

public class User {
    private String username;
    private String password; // INSECURE: Should be hashed password
    private String role; // "passenger" or "admin"
    private boolean approved; // For admin users
    private String email; // New field
    private String phoneNumber; // New field

    public User() {
        // Default constructor
    }

    // Constructor primarily for adding new users initially (maybe from signup)
    public User(String username, String password, String role) {
        this(username, password, role, false, null, null); // Default approved=false, null email/phone
    }

    // Constructor for loading from DB or full creation
    public User(String username, String password, String role, boolean approved, String email, String phoneNumber) {
        this.username = username;
        this.password = password; // Store HASHED password here in production
        this.role = role;
        this.approved = approved;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; } // Should return hash
    public String getRole() { return role; }
    public boolean isApproved() { return approved; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; } // Should take raw pass and service should hash it
    public void setRole(String role) { this.role = role; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='********'" + // Don't print password/hash
                ", role='" + role + '\'' +
                ", approved=" + approved +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}