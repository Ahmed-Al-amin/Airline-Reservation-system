package com.example.airline.controller;

import com.example.airline.model.entity.Reservation;
import com.example.airline.model.entity.User;
import com.example.airline.model.service.ReservationService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert; // Import Alert
import javafx.scene.control.Button; // Import Button
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;

public class TicketHistoryController {

    @FXML
    private ListView<String> reservationListView;

    @FXML
    private Button viewDetailsButton; // Added button for stage access

    @FXML
    private Button backToSearchButton; // Added button for stage access

    @FXML
    private Button logoutButton; // Added button for stage access


    private User loggedInUser;
    private final ReservationService reservationService = new ReservationService();

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    public void loadReservationHistory() {
        // Ensure list view and user are not null
        if (reservationListView == null) {
            System.err.println("Error: reservationListView is null in TicketHistoryController.");
            return;
        }
        if (loggedInUser != null) {
            reservationListView.setItems(FXCollections.observableList(
                    reservationService.getReservationHistory(loggedInUser.getUsername()).stream()
                            .map(reservation -> String.format("ID: %s, Flight: %s, Seat: %s",
                                    reservation.getReservationId(), reservation.getFlightNumber(), reservation.getSeatNumber()))
                            .toList()
            ));
        } else {
            System.err.println("Error loading history: loggedInUser is null.");
            reservationListView.getItems().clear(); // Clear list if user is null
            showAlert(Alert.AlertType.WARNING, "User Error", "Could not load history because user data is missing.");
        }
    }

    @FXML
    private void handleViewDetails() { // Removed throws IOException
        if (reservationListView == null || reservationListView.getSelectionModel() == null) {
            showAlert(Alert.AlertType.ERROR, "UI Error", "Reservation list component not available.");
            return;
        }
        String selectedReservation = reservationListView.getSelectionModel().getSelectedItem();

        if (selectedReservation != null && loggedInUser != null) { // Also check loggedInUser
            try {
                String reservationId = selectedReservation.split(",")[0].substring(4).trim(); // Extract ID
                Reservation reservation = reservationService.getReservationDetails(reservationId);

                if (reservation != null) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/ticket_view.fxml"));
                    Parent root = loader.load(); // Can throw IOException
                    TicketViewController controller = loader.getController();

                    if (controller != null) {
                        controller.setReservation(reservation);
                        controller.setLoggedInUser(loggedInUser); // *** PASS USER HERE ***

                        // Use viewDetailsButton to get the stage
                        if (viewDetailsButton != null && viewDetailsButton.getScene() != null) {
                            Stage stage = (Stage) viewDetailsButton.getScene().getWindow();
                            stage.setScene(new Scene(root)); // Use javafx.scene.Scene
                            stage.setTitle("Ticket Details - " + reservationId);
                        } else {
                            System.err.println("Error navigating to details: Cannot get stage from viewDetailsButton.");
                            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not open the ticket details screen.");
                        }

                    } else {
                        System.err.println("Error: TicketViewController is null after loading FXML.");
                        showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to initialize the ticket view screen.");
                    }
                } else {
                    System.err.println("Error: Could not find reservation details for ID: " + reservationId);
                    showAlert(Alert.AlertType.ERROR, "Data Error", "Could not retrieve details for the selected reservation.");
                }
            } catch (IOException e) {
                System.err.println("Error loading ticket_view.fxml: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load the ticket details screen.");
            } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
                System.err.println("Error parsing selected reservation string: " + selectedReservation + " - " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Selection Error", "Could not read the selected reservation ID.");
            } catch (IllegalStateException e) {
                System.err.println("Error: Controller or root is null after loading ticket_view.fxml: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to initialize the ticket view components.");
            }
        } else if (selectedReservation == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a reservation to view details.");
        } else { // loggedInUser is null
            showAlert(Alert.AlertType.ERROR, "User Error", "Cannot view details because user information is missing.");
        }
    }

    @FXML
    private void handleBackToSearch() { // Removed throws IOException
        if (loggedInUser == null) {
            showAlert(Alert.AlertType.ERROR, "User Error", "Cannot go back to search because user information is missing.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/flight_search.fxml"));
            Parent root = loader.load(); // Use Parent
            FlightSearchController controller = loader.getController();
            controller.setLoggedInUser(loggedInUser); // Pass user back

            // Use backToSearchButton to get the stage
            if (backToSearchButton != null && backToSearchButton.getScene() != null) {
                Stage stage = (Stage) backToSearchButton.getScene().getWindow();
                stage.setScene(new Scene(root)); // Use javafx.scene.Scene
                stage.setTitle("Flight Search");
            } else {
                System.err.println("Error navigating back to search: Cannot get stage from backToSearchButton.");
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not go back to the search screen.");
            }

        } catch (IOException e) {
            System.err.println("Error loading flight_search.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load the flight search screen.");
        } catch (IllegalStateException e) {
            System.err.println("Error: Controller or root is null after loading flight_search.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to initialize the search screen components.");
        }
    }

    @FXML
    private void handleLogout() { // Removed throws IOException
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/welcome.fxml"));
            Parent root = loader.load(); // Use Parent

            // Use logoutButton to get the stage
            if (logoutButton != null && logoutButton.getScene() != null) {
                Stage stage = (Stage) logoutButton.getScene().getWindow();
                stage.setScene(new Scene(root)); // Use javafx.scene.Scene
                stage.setTitle("Welcome to Airline Reservation System");
                stage.show();
            } else {
                System.err.println("Error logging out: Cannot get stage from logoutButton.");
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not return to the welcome screen.");
            }
        } catch (IOException e) {
            System.err.println("Error loading welcome.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load the welcome screen.");
        } catch (IllegalStateException e) {
            System.err.println("Error: Controller or root is null after loading welcome.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to initialize the welcome screen components.");
        }
    }

    /**
     * Helper method to display alerts.
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}