package com.example.airline.controller;

import com.example.airline.model.entity.Flight;
import com.example.airline.model.entity.Reservation;
import com.example.airline.model.entity.User; // Import User
import com.example.airline.model.service.FlightService;
// ReservationService IS needed now for cancellation
import com.example.airline.model.service.ReservationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button; // Import Button
import javafx.scene.control.ButtonType; // Import ButtonType for confirmation
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime; // Import LocalDateTime
import java.time.Duration; // Import Duration
import java.util.Optional; // Import Optional for confirmation dialog

public class TicketViewController {

    @FXML private Label reservationIdLabel;
    @FXML private Label flightNumberLabel;
    @FXML private Label seatNumberLabel;
    @FXML private Label departureCityLabel;
    @FXML private Label destinationCityLabel;
    @FXML private Label passengerUsernameLabel; // Added to display username
    @FXML private Label departureDateTimeLabel; // Combined Date and Time
    @FXML private Label arrivalDateTimeLabel; // Combined Date and Arrival Time (if applicable)
    @FXML private Label fareLabel; // Added to display fare
    @FXML private Button backButton; // Add a Button to get the stage reference
    @FXML private Button cancelButton; // Added Cancel button

    private Reservation reservation;
    private Flight flight; // Store the flight details for time check
    private User loggedInUser; // Add field for the logged-in user
    private final FlightService flightService = new FlightService();
    private final ReservationService reservationService = new ReservationService(); // Service for cancellation


    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        loadTicketDetails();
    }


    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        // Optionally update a label if you want to show the username
        if (passengerUsernameLabel != null && user != null) {
            // Keep showing passenger from reservation data for clarity
            // passengerUsernameLabel.setText("Passenger: " + user.getUsername());
        }
    }


    /**
     * Populates the labels with details from the Reservation and its corresponding Flight.
     */
    private void loadTicketDetails() {
        if (reservation != null) {
            // Basic Reservation Details
            if (reservationIdLabel != null) reservationIdLabel.setText("Reservation ID: " + reservation.getReservationId());
            if (flightNumberLabel != null) flightNumberLabel.setText("Flight Number: " + reservation.getFlightNumber());
            if (seatNumberLabel != null) seatNumberLabel.setText("Seat Number: " + reservation.getSeatNumber());
            if (passengerUsernameLabel != null) passengerUsernameLabel.setText("Passenger: " + reservation.getPassengerUsername());

            // Fetch Flight Details
            this.flight = flightService.getFlightByNumber(reservation.getFlightNumber()); // Store flight locally
            if (this.flight != null) {
                if (departureCityLabel != null) departureCityLabel.setText("Departure City: " + flight.getDepartureCity());
                if (destinationCityLabel != null) destinationCityLabel.setText("Destination City: " + flight.getDestinationCity());
                if (departureDateTimeLabel != null) departureDateTimeLabel.setText(String.format("Departure: %s %s", flight.getFormattedDepartureDate(), flight.getFormattedDepartureTime()));
                if (arrivalDateTimeLabel != null) arrivalDateTimeLabel.setText(String.format("Arrival: %s %s", flight.getFormattedDepartureDate(), // Using departure date
                        flight.getFormattedArrivalTime()));
                if (fareLabel != null) fareLabel.setText(String.format("Fare Paid: $%.2f", flight.getFare()));

                // Enable/Disable Cancel Button based on initial load time check
                updateCancelButtonState();

            } else {
                // Handle case where flight details couldn't be found
                System.err.println("Warning: Could not find flight details for flight number: " + reservation.getFlightNumber());
                if (departureCityLabel != null) departureCityLabel.setText("Departure City: N/A");
                if (destinationCityLabel != null) destinationCityLabel.setText("Destination City: N/A");
                if (departureDateTimeLabel != null) departureDateTimeLabel.setText("Departure: N/A");
                if (arrivalDateTimeLabel != null) arrivalDateTimeLabel.setText("Arrival: N/A");
                if (fareLabel != null) fareLabel.setText("Fare Paid: N/A");
                if (cancelButton != null) cancelButton.setDisable(true); // Disable if flight data missing
            }
        } else {
            System.err.println("Error: Reservation object is null in TicketViewController.");
            if (cancelButton != null) cancelButton.setDisable(true); // Disable if reservation data missing
        }
    }

    /**
     * Checks if cancellation is allowed based on time and updates the button state.
     */
    private void updateCancelButtonState() {
        if (cancelButton == null || flight == null || reservation == null) {
            if (cancelButton != null) cancelButton.setDisable(true);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureDateTime = flight.getDepartureDate().atTime(flight.getDepartureTime());
        Duration duration = Duration.between(now, departureDateTime);

        // Allow cancellation if departure is 6 hours or more away
        boolean canCancel = duration.toHours() >= 6;
        cancelButton.setDisable(!canCancel);

        if (!canCancel && departureDateTime.isAfter(now)) { // Add tooltip if disabled but flight is in future
            cancelButton.setTooltip(new javafx.scene.control.Tooltip("Cannot cancel less than 6 hours before departure."));
        } else if (!canCancel && !departureDateTime.isAfter(now)){ // Flight already departed
            cancelButton.setTooltip(new javafx.scene.control.Tooltip("Flight has departed."));
            cancelButton.setDisable(true); // Ensure disabled if departed
        } else {
            cancelButton.setTooltip(null); // Remove tooltip if enabled
        }
    }


    /**
     * Handles the action of the "Cancel Reservation" button.
     */
    @FXML
    private void handleCancelReservation() {
        // Double check conditions (already checked by button state, but safer)
        if (reservation == null || flight == null || loggedInUser == null || cancelButton.isDisabled()) {
            showAlert(Alert.AlertType.ERROR, "Cancellation Error", "Cannot process cancellation. Information missing or condition not met.");
            return;
        }

        // Verify ownership (although they should only see their own tickets)
        if (!loggedInUser.getUsername().equalsIgnoreCase(reservation.getPassengerUsername())) {
            showAlert(Alert.AlertType.ERROR, "Authorization Error", "You can only cancel your own reservations.");
            return;
        }

        // Confirm Cancellation
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Cancellation");
        confirmationAlert.setHeaderText("Cancel Reservation ID: " + reservation.getReservationId());
        confirmationAlert.setContentText("Are you sure you want to cancel this reservation for flight " + flight.getFlightNumber() + ", seat " + reservation.getSeatNumber() + "?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Proceed with cancellation
            boolean success = reservationService.cancelTicket(reservation.getReservationId());

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Cancellation Successful", "Reservation ID " + reservation.getReservationId() + " has been cancelled.");
                // Disable the button permanently after successful cancellation
                cancelButton.setDisable(true);
                cancelButton.setText("Cancelled");
                // Optionally navigate back immediately
                handleBackToHistory();
            } else {
                showAlert(Alert.AlertType.ERROR, "Cancellation Failed", "Could not cancel the reservation. It might have been already cancelled or a database error occurred.");
                // Refresh state in case it was already cancelled elsewhere
                updateCancelButtonState();
            }
        } else {
            // User chose Cancel in the confirmation dialog
            System.out.println("Cancellation aborted by user.");
        }
    }


    /**
     * Handles the action of the "Back" button, returning to the Ticket History screen.
     */
    @FXML
    private void handleBackToHistory() {
        if (loggedInUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "User information not available for navigation.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/ticket_history.fxml"));
            Parent root = loader.load();
            TicketHistoryController controller = loader.getController();
            if (controller != null) {
                controller.setLoggedInUser(loggedInUser); // Pass the user back
                controller.loadReservationHistory(); // Reload history
            } else {
                throw new IllegalStateException("TicketHistoryController null after loading FXML.");
            }


            if (backButton != null && backButton.getScene() != null) {
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Your Reservation History");
            } else {
                System.err.println("Error navigating back: Cannot get stage from backButton.");
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not return to the history screen.");
            }
        } catch (IOException e) {
            System.err.println("Error loading ticket_history.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load the reservation history screen.");
        } catch (IllegalStateException e) {
            System.err.println("Error: Controller or root is null after loading ticket_history.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to initialize the history screen components.");
        }
    }

    // loadSearchScreen() is removed as it's unused.

    /**
     * Helper method to show alerts.
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}