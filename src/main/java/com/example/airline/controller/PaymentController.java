package com.example.airline.controller;

import com.example.airline.model.entity.Flight;
import com.example.airline.model.entity.Reservation;
import com.example.airline.model.entity.User;
import com.example.airline.model.service.PaymentService;
import com.example.airline.model.service.ReservationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentController {

    @FXML private Label flightDetailsLabel;
    @FXML private Label seatsToBookLabel;   // Changed from seatNumberLabel
    @FXML private Label ticketPriceLabel;   // Shows price per seat AND total
    @FXML private TextField creditCardField;
    @FXML private TextField paymentAmountField;
    @FXML private Label paymentMessage;
    @FXML private Button payButton;
    @FXML private Button backButton;

    private Flight flight;
    private List<String> selectedSeats; // Changed to List<String>
    private User loggedInUser;
    private double totalRequiredFare;   // Store calculated total fare

    private final PaymentService paymentService = new PaymentService();
    private final ReservationService reservationService = new ReservationService();

    // Store the list of successful reservation IDs for potential navigation/display
    private List<String> successfulReservationIds = new ArrayList<>();


    /**
     * New method to receive all booking details together.
     * @param flight The selected flight.
     * @param selectedSeats The list of seat numbers selected by the user.
     * @param loggedInUser The currently logged-in user.
     */
    public void setBookingDetails(Flight flight, List<String> selectedSeats, User loggedInUser) {
        this.flight = flight;
        this.selectedSeats = selectedSeats;
        this.loggedInUser = loggedInUser;
        this.successfulReservationIds.clear(); // Clear previous results

        if (this.flight != null && this.selectedSeats != null) {
            this.totalRequiredFare = this.flight.getFare() * this.selectedSeats.size();
        } else {
            this.totalRequiredFare = 0.0;
        }
        updateLabels();
    }

    // *** Deprecated single-seat setters (use setBookingDetails instead) ***
    @Deprecated public void setFlight(Flight flight) { /* Use setBookingDetails */ }
    @Deprecated public void setSelectedSeat(String selectedSeat) { /* Use setBookingDetails */ }
    @Deprecated public void setLoggedInUser(User loggedInUser) { /* Use setBookingDetails */ }


    private void updateLabels() {
        if (flight != null && flightDetailsLabel != null) {
            flightDetailsLabel.setText(String.format("Flight: %s (%s → %s)",
                    flight.getFlightNumber(), flight.getDepartureCity(), flight.getDestinationCity()));
        }
        if (selectedSeats != null && !selectedSeats.isEmpty() && seatsToBookLabel != null) {
            seatsToBookLabel.setText("Selected Seats: " + String.join(", ", selectedSeats) + " (" + selectedSeats.size() + ")");
        } else if (seatsToBookLabel != null) {
            seatsToBookLabel.setText("Selected Seats: None");
        }
        if (ticketPriceLabel != null) {
            ticketPriceLabel.setText(String.format("Total Price: $%.2f (%.2f per seat)",
                    totalRequiredFare, flight != null ? flight.getFare() : 0.0));
        }
    }


    @FXML
    private void handlePay() {
        this.successfulReservationIds.clear(); // Clear previous results on new attempt

        // --- Basic Validation ---
        if (flight == null || selectedSeats == null || selectedSeats.isEmpty() || loggedInUser == null) {
            paymentMessage.setText("Error: Missing flight, seat, or user details.");
            showAlert(Alert.AlertType.ERROR, "Payment Error", "Cannot process payment. Missing necessary information.");
            return;
        }
        if (creditCardField == null || paymentAmountField == null || paymentMessage == null || payButton == null || backButton == null) {
            showAlert(Alert.AlertType.ERROR, "UI Error", "Payment screen components not loaded correctly.");
            return;
        }

        String creditCardInput = creditCardField.getText().trim();
        String amountText = paymentAmountField.getText().trim();
        double amountPaid;

        // --- Input Validation ---
        if (amountText.isEmpty()) { /* ... (same as before) ... */
            paymentMessage.setText("Please enter the payment amount.");
            showAlert(Alert.AlertType.WARNING, "Input Required", "Payment amount is required.");
            return;
        }
        try { amountPaid = Double.parseDouble(amountText); if (amountPaid < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { /* ... (same as before) ... */
            paymentMessage.setText("Invalid payment amount entered.");
            showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please enter a valid number for the payment amount.");
            return;
        }
        if (creditCardInput.isEmpty()) { /* ... (same as before) ... */
            paymentMessage.setText("Please enter credit card number.");
            showAlert(Alert.AlertType.WARNING, "Input Required", "Credit card number is required.");
            return;
        }
        if (!creditCardInput.matches("\\d{15,16}")) { /* ... (same as before) ... */
            paymentMessage.setText("Invalid credit card number format.");
            showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please enter a valid 15 or 16 digit credit card number (digits only).");
            return;
        }
        String creditCardIdentifier = "****-****-****-" + creditCardInput.substring(creditCardInput.length() - 4);
        // --- End Input Validation ---


        // --- Check Amount Paid vs Total Required ---
        if (amountPaid < totalRequiredFare) {
            paymentMessage.setText(String.format("Payment amount ($%.2f) is less than the required total ($%.2f).", amountPaid, totalRequiredFare));
            showAlert(Alert.AlertType.WARNING, "Insufficient Payment", String.format("The amount entered ($%.2f) is less than the total fare ($%.2f).", amountPaid, totalRequiredFare));
            return;
        }


        // --- Logical Flow: Book ALL Seats First, Then Pay ---
        paymentMessage.setText("Attempting to reserve seats: " + String.join(", ", selectedSeats));
        boolean allSeatsBooked = true;

        // Loop through each selected seat and try to book it
        for (String seat : selectedSeats) {
            String currentReservationId = reservationService.bookTicket(flight.getFlightNumber(), loggedInUser.getUsername(), seat);
            if (currentReservationId != null) {
                successfulReservationIds.add(currentReservationId); // Add to list if successful
            } else {
                // Booking failed for this seat!
                allSeatsBooked = false;
                paymentMessage.setText("Booking failed for seat " + seat + ". Rolling back any successful bookings...");
                showAlert(Alert.AlertType.ERROR, "Booking Failed", "Could not reserve seat " + seat + ". It might have been taken. Any other seats booked in this transaction will be cancelled.");
                break; // Stop trying to book more seats
            }
        }

        // --- Handle Booking Outcome ---
        if (allSeatsBooked) {
            // All seats booked successfully. Now process payment.
            // Use the FIRST reservation ID for linking the single payment record (simplification)
            String primaryReservationId = successfulReservationIds.get(0);
            paymentMessage.setText("All seats reserved successfully. Processing payment...");

            boolean paymentSuccessful = paymentService.processPayment(primaryReservationId, amountPaid, totalRequiredFare, creditCardIdentifier);

            if (paymentSuccessful) {
                // *** PAYMENT AND ALL BOOKINGS SUCCESSFUL ***
                paymentMessage.setText("Payment successful! Reservations Confirmed for seats: " + String.join(", ", selectedSeats));
                showAlert(Alert.AlertType.INFORMATION, "Success", "Your flight seats (" + String.join(", ", selectedSeats) + ") are booked and payment is confirmed!\nPrimary Reservation ID: " + primaryReservationId);

                payButton.setDisable(true);
                creditCardField.setDisable(true);
                paymentAmountField.setDisable(true);

                // Change 'Back' button to 'View History' or similar
                backButton.setText("View History");
                // Action will be handled in handleBackOrFinish

            } else {
                // Payment failed AFTER ALL bookings succeeded - Rollback ALL bookings!
                paymentMessage.setText("Payment denied (e.g., insufficient amount). Cancelling reservations for seats: " + String.join(", ", selectedSeats));
                showAlert(Alert.AlertType.ERROR, "Payment Failed", "Your payment was denied. The reservations for seats " + String.join(", ", selectedSeats) + " have been cancelled.");
                rollbackBookings(successfulReservationIds); // Rollback all successful bookings
                successfulReservationIds.clear(); // Clear the list after rollback
                // Keep buttons enabled to allow retry or going back
            }

        } else {
            // Booking failed for at least one seat. Rollback any successful ones.
            rollbackBookings(successfulReservationIds); // Rollback any that succeeded before the failure
            successfulReservationIds.clear(); // Clear the list
            // Message already shown. Keep buttons enabled.
        }
    }

    /** Helper method to cancel a list of reservations */
    private void rollbackBookings(List<String> reservationIdsToCancel) {
        System.out.println("Rolling back reservations: " + reservationIdsToCancel);
        int cancelledCount = 0;
        for (String resId : reservationIdsToCancel) {
            boolean cancelled = reservationService.cancelTicket(resId);
            if(cancelled) {
                cancelledCount++;
            } else {
                // Log critical error - manual intervention might be needed
                System.err.println("CRITICAL ERROR: Failed to rollback reservation ID: " + resId);
                // Show a persistent error to the user?
            }
        }
        System.out.println("Rollback complete. Cancelled " + cancelledCount + " of " + reservationIdsToCancel.size() + " reservations.");
    }


    /** Handles the action of the "Back" button, which might change function */
    @FXML
    private void handleBackOrFinish() { // Renamed from handleBackToBooking
        if (!this.successfulReservationIds.isEmpty()) {
            // Payment was successful, navigate to Ticket History
            navigateToHistoryScreen();
        } else {
            // Payment not successful or not attempted, navigate back to Booking screen
            navigateToBookingScreen();
        }
    }

    /** Navigates to the Ticket History screen */
    private void navigateToHistoryScreen() {
        if (loggedInUser == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "User information missing.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/ticket_history.fxml"));
            Parent root = loader.load();
            TicketHistoryController controller = loader.getController();
            if (controller != null) {
                controller.setLoggedInUser(loggedInUser);
                controller.loadReservationHistory(); // Refresh history
            } else { throw new IllegalStateException("TicketHistoryController null"); }

            if (backButton != null && backButton.getScene() != null) {
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Your Reservation History");
            } else { throw new IllegalStateException("Cannot get Stage from backButton"); }

        } catch (IOException | IllegalStateException e) {
            System.err.println("Error navigating to history: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the reservation history screen.");
        }
    }


    /** Navigates back to the Flight Booking screen */
    private void navigateToBookingScreen() {
        if (backButton == null || backButton.getScene() == null) { /* ... (handle error) ... */
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not determine the current window.");
            return;
        }
        if (flight == null || loggedInUser == null) { /* ... (handle error) ... */
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Missing flight or user data to return to booking.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/flight_booking.fxml"));
            Parent root = loader.load();
            FlightBookingController controller = loader.getController();
            if (controller != null) {
                controller.setFlight(flight); // Pass flight info back
                controller.setLoggedInUser(loggedInUser); // Pass user back
            } else { throw new IllegalStateException("FlightBookingController null"); }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Book Seats for Flight " + flight.getFlightNumber());
        } catch (IOException | IllegalStateException e) {
            System.err.println("Error navigating back to booking: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load the flight booking screen.");
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
