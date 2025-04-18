package com.example.airline.controller;

import com.example.airline.model.entity.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert; // Import Alert
import javafx.scene.control.Button; // Import Button
import javafx.stage.Stage;

import java.io.IOException;

public class AdminDashboardController {

    // Removed FXML fields for the deleted buttons
    @FXML private Button handleAddFlightButton; // Still needed for scene access and Add Flight functionality
    // @FXML private Button handleViewBookingsButton; // REMOVED
    // @FXML private Button handleViewSeatAvailabilityButton; // REMOVED
    // @FXML private Button handleViewDeleteFlightsButton; // REMOVED
    @FXML private Button handleLogoutButton; // Still needed for logout


    private User loggedInUser;

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        // You might want to display a welcome message with the admin's username
    }

    @FXML
    private void handleAddFlight() {
        // Simplified call, as loadScene now only handles one case
        loadScene("add_flight.fxml", "Add New Flight");
    }

    // REMOVED handleViewBookings()
    // REMOVED handleViewSeatAvailability()
    // REMOVED handleViewDeleteFlights()


    @FXML
    private void handleLogout() {
        loadWelcomeScreen();
    }

    // Simplified loadScene method as it only handles AddFlight now
    private void loadScene(String fxmlFile, String title) {
        if (!"add_flight.fxml".equals(fxmlFile)) {
            System.err.println("Attempted to load unexpected scene: " + fxmlFile);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Invalid navigation target.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/" + fxmlFile));
            Parent root = loader.load();

            // Pass user if needed by the destination controller
            Object controller = loader.getController();
            if (controller instanceof AddFlightController) {
                // ((AddFlightController) controller).setLoggedInUser(loggedInUser); // Pass user if AddFlightController needs it
            } else {
                // This condition should ideally not be reached if only AddFlight is loaded
                System.err.println("Warning: Loaded FXML ("+fxmlFile+") has unexpected controller type: " + (controller != null ? controller.getClass().getName() : "null"));
            }

            // Use any button guaranteed to be on the dashboard for stage access
            Stage stage = (Stage) handleAddFlightButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Error loading scene " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Could not load the requested screen: " + title);
        } catch (IllegalStateException e) {
            System.err.println("Error processing scene " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Could not initialize the requested screen: " + title);
        } catch (NullPointerException e) {
            System.err.println("Error: Could not find FXML file: /com/example/airline/view/" + fxmlFile);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Could not find the view file for: " + title);
        }
    }


    private void loadWelcomeScreen() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/airline/view/welcome.fxml"));
            // Use any button guaranteed to be on the dashboard for stage access
            Stage stage = (Stage) handleAddFlightButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Welcome");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading welcome screen: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Logout Error", "Could not load the welcome screen.");
        } catch (IllegalStateException e) {
            System.err.println("Error processing welcome screen: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Logout Error", "Could not initialize the welcome screen.");
        } catch (NullPointerException e) {
            System.err.println("Error: Could not find welcome.fxml. Check path.");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Logout Error", "Could not find the welcome screen view file.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}