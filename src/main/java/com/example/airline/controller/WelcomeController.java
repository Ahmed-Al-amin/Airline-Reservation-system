package com.example.airline.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class WelcomeController {

    @FXML
    private Node welcomeRoot; // Inject the root node of the welcome screen

    @FXML
    private void handlePassengerLogin() throws IOException {
        loadScene("passenger_login.fxml", "Passenger Login");
    }

    @FXML
    private void handlePassengerSignup() throws IOException {
        loadScene("passenger_signup.fxml", "Passenger Signup");
    }

    @FXML
    private void handleAdminLogin() throws IOException {
        loadScene("admin_login.fxml", "Admin Login");
    }

    @FXML
    private void handleAdminSignup() throws IOException {
        loadScene("admin_signup.fxml", "Admin Signup");
    }

    @FXML
    private javafx.scene.control.Button handlePassengerLoginButton; // Added to access the stage

    private void loadScene(String fxmlFile, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/airline/view/" + fxmlFile));
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.show();
        // Close the welcome screen
        if (welcomeRoot != null) {
            ((Stage) welcomeRoot.getScene().getWindow()).close();
        }

    }
}