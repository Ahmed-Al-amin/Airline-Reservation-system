package com.example.airline.controller;

import com.example.airline.model.entity.User;
import com.example.airline.model.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminLoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorMessage;

    private final UserService userService = new UserService();

    @FXML
    private void handleLogin() throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        User loggedInUser = userService.login(username, password);

        if (loggedInUser != null && loggedInUser.getRole().equals("admin")) {
            if (loggedInUser.getUsername().equals("admin")) {
                // Load root admin approval page
                loadScene("admin_approval.fxml", "Admin Approval", loggedInUser);
            } else if (loggedInUser.isApproved()) {
                // Load admin dashboard
                loadScene("admin_dashboard.fxml", "Admin Dashboard", loggedInUser);
            } else {
                errorMessage.setText("Your admin account is awaiting approval.");
            }
        } else {
            errorMessage.setText("Invalid username or password for admin.");
        }
    }

    @FXML
    private void handleBack() throws IOException {
        loadWelcomeScreen();
    }

    private void loadScene(String fxmlFile, String title, User loggedInUser) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/" + fxmlFile));
        Parent root = loader.load();
        // You might need to pass the loggedInUser to the next controller
        if (fxmlFile.equals("admin_dashboard.fxml")) {
            AdminDashboardController controller = loader.getController();
            controller.setLoggedInUser(loggedInUser);
        } else if (fxmlFile.equals("admin_approval.fxml")) {
            AdminApprovalController controller = loader.getController();
            controller.setLoggedInUser(loggedInUser);
            controller.loadPendingAdmins();
        }
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }

    private void loadWelcomeScreen() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/airline/view/welcome.fxml"));
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Welcome");
    }
}