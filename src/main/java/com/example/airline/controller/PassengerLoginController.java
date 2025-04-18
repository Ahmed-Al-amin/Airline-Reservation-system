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

public class PassengerLoginController {

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

        if (loggedInUser != null && loggedInUser.getRole().equals("passenger")) {
            // Load passenger dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/flight_search.fxml"));
            Parent root = loader.load();
            FlightSearchController controller = loader.getController();
            controller.setLoggedInUser(loggedInUser); // Pass user information
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Flight Search");
        } else {
            errorMessage.setText("Invalid username or password for passenger.");
        }
    }

    @FXML
    private void handleBack() throws IOException {
        loadWelcomeScreen();
    }

    private void loadWelcomeScreen() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/airline/view/welcome.fxml"));
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Welcome");
    }
}