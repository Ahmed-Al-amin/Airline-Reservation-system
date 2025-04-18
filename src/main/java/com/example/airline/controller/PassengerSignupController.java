package com.example.airline.controller;

import com.example.airline.model.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button; // Import Button
import javafx.stage.Stage;

import java.io.IOException;

public class PassengerSignupController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField; // New Field
    @FXML private TextField phoneField; // New Field
    @FXML private Label signupMessage;
    @FXML private Button signupButton; // Add fx:id for stage access if needed later
    @FXML private Button backButton;   // Add fx:id

    private final UserService userService = new UserService();

    @FXML
    private void handleSignup() { // Removed throws IOException
        String username = usernameField.getText();
        String password = passwordField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();

        // Basic Validation
        if (username.trim().isEmpty() || password.isEmpty()) {
            signupMessage.setText("Username and Password cannot be empty.");
            signupMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        // Very basic email validation (optional)
        if (!email.trim().isEmpty() && !email.contains("@")) {
            signupMessage.setText("Please enter a valid email address.");
            signupMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        // Basic password length (optional)
        if (password.length() < 4) {
            signupMessage.setText("Password must be at least 4 characters.");
            signupMessage.setStyle("-fx-text-fill: red;");
            return;
        }


        // Call the updated service method
        boolean success = userService.signup(
                username,
                password,
                "passenger", // Role
                email.trim().isEmpty() ? null : email.trim(), // Pass null if empty, else trimmed value
                phone.trim().isEmpty() ? null : phone.trim()   // Pass null if empty, else trimmed value
        );

        if (success) {
            signupMessage.setText("Signup successful! You can now log in.");
            signupMessage.setStyle("-fx-text-fill: green;");
            // Optionally clear fields or disable button
            usernameField.clear();
            passwordField.clear();
            emailField.clear();
            phoneField.clear();
            // signupButton.setDisable(true); // Prevent immediate re-signup
        } else {
            // Service layer already logs details, provide user-friendly message
            signupMessage.setText("Signup failed. Username might already exist.");
            signupMessage.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleBack() throws IOException {
        loadWelcomeScreen();
    }

    private void loadWelcomeScreen() throws IOException {
        // Use backButton to get the stage reliably
        Stage stage = (Stage) backButton.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/airline/view/welcome.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Welcome");
        // No need to show() again if it's the same stage
    }
}