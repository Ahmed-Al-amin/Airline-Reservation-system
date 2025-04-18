package com.example.airline.controller;

import com.example.airline.model.entity.Flight;
import com.example.airline.model.entity.Seat;
import com.example.airline.model.entity.User;
// No longer using ReservationService directly here
// import com.example.airline.model.service.ReservationService;
import com.example.airline.model.service.SeatService;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode; // Import SelectionMode
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FlightBookingController {

    @FXML private Label flightDetailsLabel;
    @FXML private ListView<String> availableSeatsListView;
    @FXML private Button selectSeatsButton; // Changed from selectSeatButton
    @FXML private Label selectedSeatsLabel; // To show selected seats
    @FXML private Label totalFareLabel;     // To show total fare
    @FXML private Label instructionsLabel;  // User instructions
    @FXML private Button backButton;         // Added fx:id

    private Flight flight;
    private User loggedInUser;
    private final SeatService seatService = new SeatService();
    private ObservableList<String> currentlySelectedSeats = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // --- Configure ListView for Multiple Selections ---
        if (availableSeatsListView != null) {
            availableSeatsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // Listener to update our internal list and UI labels when selection changes
            availableSeatsListView.getSelectionModel().getSelectedItems().addListener(
                    (ListChangeListener.Change<? extends String> change) -> {
                        currentlySelectedSeats.setAll(change.getList()); // Update internal list
                        updateSelectionDisplay(); // Update UI labels
                    });
        } else {
            System.err.println("Error: availableSeatsListView is null during initialization.");
        }
        if(instructionsLabel != null) {
            instructionsLabel.setText("Select one or more available seats (use Ctrl/Cmd or Shift to select multiple).");
        }
        // Initialize labels
        updateSelectionDisplay();
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
        displayFlightDetails();
        loadAvailableSeats();
        updateSelectionDisplay(); // Reset fare/selection when flight changes
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    private void displayFlightDetails() {
        // (Same as before - display details in flightDetailsLabel)
        if (flightDetailsLabel == null) {
            System.err.println("Error: flightDetailsLabel is null. Check FXML connection.");
            return;
        }
        if (flight != null) {
            String details = String.format(
                    "Flight Number: %s\n" +
                            "Route: %s → %s\n" +
                            "Date: %s\n" +
                            "Departure Time: %s\n" +
                            "Arrival Time: %s\n" +
                            "Fare per Seat: $%.2f", // Clarify fare is per seat
                    flight.getFlightNumber(),
                    flight.getDepartureCity(),
                    flight.getDestinationCity(),
                    flight.getFormattedDepartureDate(),
                    flight.getFormattedDepartureTime(),
                    flight.getFormattedArrivalTime(),
                    flight.getFare()
            );
            flightDetailsLabel.setText(details);
        } else {
            flightDetailsLabel.setText("Flight details not available.");
        }
    }

    private void loadAvailableSeats() {
        if (availableSeatsListView == null) {
            System.err.println("Error: availableSeatsListView is null. Check FXML connection.");
            return;
        }
        availableSeatsListView.getSelectionModel().clearSelection(); // Clear previous selection
        currentlySelectedSeats.clear(); // Clear internal list too

        if (flight != null) {
            List<String> availableSeatNumbers = seatService.getAvailableSeats(flight.getFlightNumber()).stream()
                    .map(Seat::getSeatNumber)
                    .sorted(Comparator.comparingInt(s -> { // Sort numerically
                        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return Integer.MAX_VALUE; } // Put non-numeric last
                    }))
                    .collect(Collectors.toList());
            availableSeatsListView.setItems(FXCollections.observableList(availableSeatNumbers));
        } else {
            availableSeatsListView.getItems().clear();
        }
        updateSelectionDisplay(); // Update labels after loading/clearing
    }

    /** Updates the labels showing selected seats and total fare */
    private void updateSelectionDisplay() {
        if (selectedSeatsLabel != null) {
            if (currentlySelectedSeats.isEmpty()) {
                selectedSeatsLabel.setText("Selected Seats: None");
            } else {
                selectedSeatsLabel.setText("Selected Seats: " + String.join(", ", currentlySelectedSeats));
            }
        }

        if (totalFareLabel != null && flight != null) {
            double totalFare = flight.getFare() * currentlySelectedSeats.size();
            totalFareLabel.setText(String.format("Total Fare: $%.2f (%d seats)", totalFare, currentlySelectedSeats.size()));
        } else if (totalFareLabel != null) {
            totalFareLabel.setText("Total Fare: $0.00 (0 seats)");
        }

        // Enable/disable proceed button based on selection
        if (selectSeatsButton != null) {
            selectSeatsButton.setDisable(currentlySelectedSeats.isEmpty());
        }
    }


    /** Handles the action of proceeding to payment with the selected seats */
    @FXML
    private void handleProceedToPayment() {
        if (currentlySelectedSeats.isEmpty()) {
            showAlert(Alert.AlertType.WARNING,"Selection Required", "Please select at least one available seat from the list.");
            return;
        }
        if (flight == null || loggedInUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Missing flight or user information.");
            return;
        }

        // --- Pass the LIST of selected seats to the PaymentController ---
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/payment.fxml"));
            Parent root = loader.load();
            PaymentController controller = loader.getController();

            if (controller != null) {
                // Convert ObservableList to a regular List for passing
                List<String> seatsToBook = List.copyOf(currentlySelectedSeats);
                controller.setBookingDetails(flight, seatsToBook, loggedInUser); // New method in PaymentController
            } else {
                throw new IllegalStateException("PaymentController was null after loading FXML.");
            }


            // Use selectSeatsButton (ensure fx:id) to get the stage
            if (selectSeatsButton != null && selectSeatsButton.getScene() != null) {
                Stage stage = (Stage) selectSeatsButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Payment for Flight " + flight.getFlightNumber());
            } else {
                System.err.println("Error navigating to payment: Cannot get stage from selectSeatsButton.");
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not open the payment screen.");
            }

        } catch (IOException e) {
            System.err.println("Error loading payment.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load the payment screen.");
        } catch (IllegalStateException e) {
            System.err.println("Error navigating to payment: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to initialize the payment screen components.");
        }
    }


    @FXML
    private void handleBackToSearch() {
        if(backButton == null || backButton.getScene() == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Cannot determine current window.");
            return;
        }
        if (loggedInUser == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "User information missing.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/flight_search.fxml"));
            Parent root = loader.load();
            FlightSearchController controller = loader.getController();
            if (controller != null) {
                controller.setLoggedInUser(loggedInUser);
            } else {
                throw new IllegalStateException("FlightSearchController was null after loading FXML.");
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Flight Search");
        } catch (IOException e) {
            System.err.println("Error loading flight_search.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load the flight search screen.");
        } catch (IllegalStateException e) {
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to initialize the search screen components.");
        }
    }

    // Logout method can remain the same, using `backButton` for stage access
    @FXML
    private void handleLogout() {
        if(backButton == null || backButton.getScene() == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Cannot determine current window.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/welcome.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Welcome to Airline Reservation System");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading welcome.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load the welcome screen.");
        } catch (IllegalStateException e) {
            System.err.println("Error on logout: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to initialize the welcome screen components.");
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