package com.example.airline.controller;

import com.example.airline.model.service.FlightService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button; // Import Button
import javafx.scene.control.DatePicker; // Import DatePicker
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate; // Import LocalDate
import java.time.LocalTime; // Import LocalTime
import java.time.format.DateTimeFormatter; // Import DateTimeFormatter
import java.time.format.DateTimeParseException; // Import DateTimeParseException

public class AddFlightController {

    // Existing Fields
    @FXML private TextField flightNumberField;
    @FXML private TextField departureCityField;
    @FXML private TextField destinationCityField;
    @FXML private TextField fareField;
    @FXML private TextField totalSeatsField;
    @FXML private Label addFlightMessage;
    @FXML private Button backButton; // Added fx:id for stage access
    @FXML private Button addButton;  // Added fx:id for stage access

    // New Fields for Date/Time
    @FXML private DatePicker departureDatePicker;
    @FXML private TextField departureTimeField;
    @FXML private TextField arrivalTimeField;

    private final FlightService flightService = new FlightService();
    // Define the expected time format
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        // Set default date to today or tomorrow
        departureDatePicker.setValue(LocalDate.now().plusDays(1));
        addFlightMessage.setText(""); // Clear message on init
    }

    @FXML
    private void handleAddFlight() {
        // Clear previous message
        addFlightMessage.setText("");
        addFlightMessage.setStyle("-fx-text-fill: red;"); // Default to error color

        // Get values from fields
        String flightNumber = flightNumberField.getText().trim().toUpperCase(); // Standardize flight number
        String departureCity = departureCityField.getText().trim();
        String destinationCity = destinationCityField.getText().trim();
        String fareText = fareField.getText().trim();
        String totalSeatsText = totalSeatsField.getText().trim();
        LocalDate departureDate = departureDatePicker.getValue();
        String departureTimeStr = departureTimeField.getText().trim();
        String arrivalTimeStr = arrivalTimeField.getText().trim();

        // --- Input Validation ---
        if (flightNumber.isEmpty() || departureCity.isEmpty() || destinationCity.isEmpty() ||
                fareText.isEmpty() || totalSeatsText.isEmpty() || departureDate == null ||
                departureTimeStr.isEmpty() || arrivalTimeStr.isEmpty()) {
            addFlightMessage.setText("Error: All fields must be filled.");
            return;
        }

        if (departureCity.equalsIgnoreCase(destinationCity)) {
            addFlightMessage.setText("Error: Departure and Destination cities cannot be the same.");
            return;
        }

        double fare;
        int totalSeats;
        LocalTime departureTime;
        LocalTime arrivalTime;

        try {
            fare = Double.parseDouble(fareText);
            if (fare <= 0) throw new NumberFormatException("Fare must be positive.");
        } catch (NumberFormatException e) {
            addFlightMessage.setText("Error: Invalid fare. Please enter a positive number.");
            return;
        }

        try {
            totalSeats = Integer.parseInt(totalSeatsText);
            if (totalSeats <= 0) throw new NumberFormatException("Seats must be positive.");
        } catch (NumberFormatException e) {
            addFlightMessage.setText("Error: Invalid total seats. Please enter a positive whole number.");
            return;
        }

        // Prevent adding flights for past dates
        if (departureDate.isBefore(LocalDate.now())) {
            addFlightMessage.setText("Error: Cannot add a flight for a past date.");
            return;
        }


        try {
            departureTime = LocalTime.parse(departureTimeStr, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            addFlightMessage.setText("Error: Invalid Departure Time format. Use HH:MM (e.g., 09:30).");
            return;
        }

        try {
            arrivalTime = LocalTime.parse(arrivalTimeStr, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            addFlightMessage.setText("Error: Invalid Arrival Time format. Use HH:MM (e.g., 17:00).");
            return;
        }

        // Basic time logic check (can be more complex for overnight)
        // if (departureDate.isEqual(arrivalDate) && !arrivalTime.isAfter(departureTime)) {
        //    addFlightMessage.setText("Error: Arrival time must be after departure time on the same day.");
        //    return;
        // }
        // --- End Validation ---


        // --- Call the DETAILED FlightService method ---
        boolean success = flightService.addFlight(
                flightNumber,
                departureCity,
                destinationCity,
                fare,
                totalSeats,
                departureDate,
                departureTime,
                arrivalTime
        );

        if (success) {
            addFlightMessage.setStyle("-fx-text-fill: green;"); // Success color
            addFlightMessage.setText("Flight " + flightNumber + " added successfully!");
            // Optionally clear the fields after successful addition
            clearFields();
        } else {
            // Error message likely logged by service/DAO (e.g., duplicate flight number)
            addFlightMessage.setText("Error: Could not add flight. Flight number might already exist or database error occurred.");
        }
    }

    /** Helper method to clear input fields */
    private void clearFields() {
        flightNumberField.clear();
        departureCityField.clear();
        destinationCityField.clear();
        fareField.clear();
        totalSeatsField.clear();
        departureDatePicker.setValue(LocalDate.now().plusDays(1)); // Reset date
        departureTimeField.clear();
        arrivalTimeField.clear();
    }


    @FXML
    private void handleBackToAdminDashboard() {
        // Use one of the buttons guaranteed to be present
        if (backButton == null || backButton.getScene() == null) {
            System.err.println("Error navigating back: Cannot get current stage.");
            // Optionally show alert
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/airline/view/admin_dashboard.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Dashboard");
        } catch (IOException e) {
            System.err.println("Error loading Admin Dashboard: " + e.getMessage());
            e.printStackTrace();
            // Show error alert
            addFlightMessage.setText("Error loading dashboard.");
        } catch (NullPointerException e) {
            System.err.println("Error: Could not find admin_dashboard.fxml. Check path.");
            e.printStackTrace();
            addFlightMessage.setText("Error: Dashboard view not found.");
        }
    }
}