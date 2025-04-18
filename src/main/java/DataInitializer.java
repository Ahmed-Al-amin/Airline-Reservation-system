import com.example.airline.data.DatabaseManager;
import com.example.airline.model.entity.Flight; // Use your Flight entity path
import com.example.airline.model.service.FlightService; // Needed for check
import com.example.airline.model.service.SeatService; // Needed for seat init

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataInitializer { // Or place method inside Main.java

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME; // HH:MM or HH:MM:SS


    public static void insertFlightDataFromTextFile(String filePath) {
        List<Flight> flightsToInsert = new ArrayList<>();
        System.out.println("Reading flight data from: " + filePath);

        // 1. Read data from the text file
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header row
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",", -1); // Use -1 to keep trailing empty fields if needed
                if (data.length == 8) { // Ensure correct number of columns
                    try {
                        // Create Flight object using the correct constructor
                        Flight flight = new Flight(
                                data[0].trim(), // flightNumber
                                data[1].trim(), // departureCity
                                data[2].trim(), // destinationCity
                                Double.parseDouble(data[3].trim()), // fare
                                Integer.parseInt(data[4].trim()), // totalSeats
                                LocalDate.parse(data[5].trim(), DATE_FORMATTER), // departureDate
                                LocalTime.parse(data[6].trim(), TIME_FORMATTER), // departureTime
                                LocalTime.parse(data[7].trim(), TIME_FORMATTER)  // arrivalTime
                        );
                        flightsToInsert.add(flight);
                    } catch (Exception e) {
                        System.err.println("Skipping malformed line: " + line + " - Error: " + e.getMessage());
                    }
                } else {
                    System.err.println("Skipping line with incorrect column count: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading flight data file: " + e.getMessage());
            e.printStackTrace();
            return; // Stop if file reading fails
        }

        System.out.println("Read " + flightsToInsert.size() + " potential flights from file.");
        if (flightsToInsert.isEmpty()) {
            System.out.println("No valid flight data found in file to insert.");
            return;
        }

        // 2. Insert into database using INSERT OR IGNORE
        String sql = "INSERT OR IGNORE INTO flights(flight_number, departure_city, destination_city, fare, total_seats, " +
                "departure_date, departure_time, arrival_time) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        int insertedCount = 0;
        int batchSize = 100; // Process in batches
        List<Flight> successfullyInsertedFlights = new ArrayList<>(); // Track for seat init

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Use transaction

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int count = 0;
                for (Flight flight : flightsToInsert) {
                    pstmt.setString(1, flight.getFlightNumber());
                    pstmt.setString(2, flight.getDepartureCity());
                    pstmt.setString(3, flight.getDestinationCity());
                    pstmt.setDouble(4, flight.getFare());
                    pstmt.setInt(5, flight.getTotalSeats());
                    pstmt.setString(6, flight.getDepartureDate().format(DATE_FORMATTER));
                    pstmt.setString(7, flight.getDepartureTime().format(TIME_FORMATTER));
                    pstmt.setString(8, flight.getArrivalTime().format(TIME_FORMATTER));
                    pstmt.addBatch();
                    count++;

                    if (count % batchSize == 0 || count == flightsToInsert.size()) {
                        int[] results = pstmt.executeBatch();
                        // Check results to see which ones were actually inserted (result > 0 or == 1)
                        // For simplicity here, we assume executeBatch works or throws exception
                        // To track precisely, you'd need to iterate results array.
                        // For now, we'll try to initialize seats for all, assuming INSERT OR IGNORE worked.
                        // A more robust way would be to SELECT flight_number after insertion attempt.
                        // For this one-time load, trying to init seats for all is acceptable.
                        insertedCount += results.length; // Approximation, includes ignored ones
                        System.out.println("Executed batch of " + results.length);
                    }
                }
                // Add all flights from the list read from file to the list for seat initialization attempt
                successfullyInsertedFlights.addAll(flightsToInsert);
            }
            conn.commit(); // Commit transaction
            System.out.println("Finished inserting/ignoring " + flightsToInsert.size() + " flights.");

        } catch (SQLException e) {
            System.err.println("Database error during flight insertion: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage()); }
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignore */ }
            }
        }

        // 3. Initialize Seats for the inserted/existing flights from the file list
        if (!successfullyInsertedFlights.isEmpty()) {
            System.out.println("Initializing seats for " + successfullyInsertedFlights.size() + " flights from the data file...");
            SeatService seatService = new SeatService(); // Instantiate SeatService
            int seatFailures = 0;
            for (Flight flight : successfullyInsertedFlights) {
                // SeatService initialize method already checks if seats exist
                if (!seatService.initializeSeatsForFlight(flight.getFlightNumber(), flight.getTotalSeats())) {
                    seatFailures++;
                    System.err.println("Failed to initialize seats for flight: " + flight.getFlightNumber());
                }
            }
            System.out.println("Seat initialization attempt complete. Failures: " + seatFailures);
        }
    }

    // Example of how to call this method ONCE
    public static void main(String[] args) {
        // --- IMPORTANT ---
        // --- RUN THIS MAIN METHOD ONLY ONCE ---
        // --- THEN DELETE OR COMMENT OUT THE CALL ---
        System.out.println("Attempting ONE-TIME data insertion...");
        DatabaseManager.initializeDatabase(); // Make sure DB and tables exist
        insertFlightDataFromTextFile("flights_data.txt"); // Provide correct path
        System.out.println("ONE-TIME data insertion attempt finished.");
        // --- DELETE OR COMMENT OUT THE CALL ABOVE AFTER RUNNING ---
    }
}