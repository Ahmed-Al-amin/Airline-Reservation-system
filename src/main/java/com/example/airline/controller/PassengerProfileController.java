package com.example.airline.controller;

import com.example.airline.model.entity.User;
import com.example.airline.model.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PassengerProfileController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField currentPasswordField; // Optional for password change validation
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button saveProfileButton;
    @FXML private Button changePasswordButton;
    @FXML private Button backButton;
    @FXML private Label messageLabel; // For feedback

    private User loggedInUser;
    private final UserService userService = new UserService();

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    /** Loads current user data into the form fields */
    public void loadProfileData() {
        if (loggedInUser != null) {
            usernameField.setText(loggedInUser.getUsername());
            usernameField.setEditable(false); // Username typically not editable
            emailField.setText(loggedInUser.getEmail() != null ? loggedInUser.getEmail() : "");
            phoneField.setText(loggedInUser.getPhoneNumber() != null ? loggedInUser.getPhoneNumber() : "");
            messageLabel.setText(""); // Clear message
        } else {
            messageLabel.setText("Error: User data not loaded.");
            // Disable fields if no user data
            emailField.setDisable(true);
            phoneField.setDisable(true);
            currentPasswordField.setDisable(true);
            newPasswordField.setDisable(true);
            confirmPasswordField.setDisable(true);
            saveProfileButton.setDisable(true);
            changePasswordButton.setDisable(true);
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (loggedInUser == null) return;

        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        // Basic validation (add more robust email/phone validation as needed)
        if (email.isEmpty() && !phone.isEmpty()){
            // Allow saving phone even if email is empty
        } else if (!email.isEmpty() && !email.contains("@")){ // very basic email check
            setMessage("Invalid email format.", true);
            return;
        }

        boolean success = userService.updateUserProfile(
                loggedInUser.getUsername(),
                null, // Not changing password here
                email.isEmpty() ? null : email, // Pass null if empty
                phone.isEmpty() ? null : phone   // Pass null if empty
        );

        if (success) {
            setMessage("Profile updated successfully.", false);
            // Update the local user object in case the window stays open
            loggedInUser.setEmail(email.isEmpty() ? null : email);
            loggedInUser.setPhoneNumber(phone.isEmpty() ? null : phone);
        } else {
            setMessage("Failed to update profile.", true);
        }
    }

    @FXML
    private void handleChangePassword() {
        if (loggedInUser == null) return;

        String currentPassword = currentPasswordField.getText(); // Optional validation step
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // --- Validation ---
        // 1. Check if new passwords match
        if (newPassword.isEmpty() || !newPassword.equals(confirmPassword)) {
            setMessage("New passwords do not match or are empty.", true);
            return;
        }
        // 2. Basic length check
        if (newPassword.length() < 4) {
            setMessage("New password must be at least 4 characters.", true);
            return;
        }

        // 3. **IMPORTANT**: Optional - Verify Current Password (Requires hashing to be implemented correctly)
        //    If implementing hashing, uncomment and adapt this:
        /*
        if (currentPassword.isEmpty() || !HashUtil.verifyPassword(currentPassword, loggedInUser.getPassword())) {
             setMessage("Incorrect current password.", true);
             return;
        }
        */
        // --- USING INSECURE PLAIN TEXT CHECK (REMOVE IN PRODUCTION) ---
        if (currentPassword.isEmpty() || !currentPassword.equals(loggedInUser.getPassword())) {
            setMessage("Incorrect current password.", true);
            return;
        }
        // --- END INSECURE CHECK ---


        // --- Proceed with update ---
        // **IMPORTANT**: Pass the RAW newPassword. The service layer should handle hashing.
        boolean success = userService.updateUserProfile(
                loggedInUser.getUsername(),
                newPassword, // Pass the new raw password
                loggedInUser.getEmail(), // Keep existing email
                loggedInUser.getPhoneNumber() // Keep existing phone
        );

        if (success) {
            setMessage("Password changed successfully.", false);
            // Clear password fields
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            // Update local user object's password (in production, you wouldn't store raw pass)
            loggedInUser.setPassword(newPassword); // INSECURE
        } else {
            setMessage("Failed to change password.", true);
        }
    }

    @FXML
    private void handleBack() {
        // Close the profile window
        Stage stage = (Stage) backButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    /** Helper to set feedback message */
    private void setMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}