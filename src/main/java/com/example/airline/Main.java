package com.example.airline;

import com.example.airline.data.DatabaseManager; // Import DB Manager
import com.example.airline.model.entity.Flight;
import com.example.airline.model.service.FlightService;
import com.example.airline.model.service.SeatService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Removed java.io imports
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.airline.model.dao.FlightDAO.DATE_FORMATTER;
import static com.example.airline.model.entity.Flight.TIME_FORMATTER;

public class Main extends Application {

    // Constants for directory names are no longer strictly needed here,
    // as the DAOs and DatabaseManager handle the db file location.
    // public static final String DATA_DIR = "data"; // Can be removed
    // public static final String SEATS_SUBDIR = "seats"; // Can be removed

    @Override
    public void start(Stage primaryStage) throws IOException {
        // --- Initialize Database FIRST ---
        System.out.println("Initializing Database Schema...");
        DatabaseManager.initializeDatabase(); // Creates DB and tables if they don't exist
        System.out.println("Database Schema Initialized.");
        // --- End DB Init ---

        // --- Trigger Flight Date Update ONCE on Startup ---
        updatePastFlightDatesOnStartup();
        // --- End of Update Trigger ---

        // --- Initialize Initial Data if Needed (AFTER DB init and date update) ---
        initializeInitialDataIfNeeded();
        // --- End Initial Data ---

        // --- Load JavaFX UI ---
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/welcome.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        // Ensure CSS path is correct relative to the resources folder
        if (getClass().getResource("/com/example/airline/view/style.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/example/airline/view/style.css").toExternalForm());
        } else {
            System.err.println("Warning: style.css not found.");
        }

        primaryStage.setTitle("Airline Reservation System");
        primaryStage.setScene(scene);
        primaryStage.show();
        // --- End UI Load ---
    }

    /**
     * Populates the database with initial flight data if it's currently empty.
     * Also ensures corresponding seat records are created.
     */
    private void initializeInitialDataIfNeeded() {
        FlightService flightService = new FlightService();
        // Check if the database is empty by trying to get flights
        if (flightService.getAllAvailableFlights().isEmpty()) {
            System.out.println("Database appears empty. Initializing initial flight data...");
            // Using single 'fare' field constructor for the simpler Flight entity
            List<Flight> initialFlights = List.of(
                    new Flight("AI101", "New York", "London", 500.00, 100, LocalDate.of(2025, 4, 15), LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    new Flight("JL205", "Tokyo", "Paris", 750.00, 80, LocalDate.of(2025, 4, 16), LocalTime.of(12, 30), LocalTime.of(18, 45)),
                    new Flight("EK318", "Dubai", "Sydney", 1200.00, 150, LocalDate.of(2025, 4, 17), LocalTime.of(22, 0), LocalTime.of(10, 30)),
                    new Flight("LH400", "Frankfurt", "New York", 550.00, 90, LocalDate.of(2025, 4, 18), LocalTime.of(14, 0), LocalTime.of(16, 30)),
                    new Flight("BA249", "London", "Cairo", 400.00, 120, LocalDate.now().minusDays(5), LocalTime.of(10, 0), LocalTime.of(14, 0)), // Past flight for testing
                    new Flight("AF102", "Paris", "Rome", 250.00, 70, LocalDate.of(2025, 4, 13), LocalTime.of(16, 0), LocalTime.of(18, 0)),
                    new Flight("SA321", "Sydney", "Auckland", 300.00, 95, LocalDate.of(2025, 4, 19), LocalTime.of(7, 0), LocalTime.of(10, 0)),
                    new Flight("TK001", "Istanbul", "New York", 600.00, 110, LocalDate.of(2025, 4, 20), LocalTime.of(11, 0), LocalTime.of(18, 0))
            );
            // Call the service's initialize method (uses DAO's overwriteFlights)
            boolean initSuccess = flightService.initializeFlights(initialFlights);
            if (initSuccess) {
                System.out.println("Initial flight data loaded into database.");
                // Optional: Re-run the date update check immediately after initializing
                // updatePastFlightDatesOnStartup();
            } else {
                System.err.println("Failed to initialize flight data.");
            }
        } else {
            System.out.println("Flight data already exists in database. Skipping data initialization.");
            // Ensure seat records exist even if not initializing flights table
            flightService.ensureSeatRecordsForAllFlights();
        }
    }

    /**
     * Calls the FlightService to check and update departure dates of past flights in the database.
     */
    private void updatePastFlightDatesOnStartup() {
        System.out.println("Checking for past flight dates to update in database...");
        FlightService flightService = new FlightService();
        int updatedCount = flightService.updatePastFlightDatesToNextMonth();
        if (updatedCount > 0) {
            System.out.println("Updated " + updatedCount + " past flight dates to the next month in database.");
        } else {
            System.out.println("No past flight dates needed updating in database.");
        }
    }

    public static void main(String[] args) {
        // Ensure driver is loaded before launching JavaFX if explicit loading is needed
        // try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException e) { e.printStackTrace(); return; }
        launch(args);

    }
}