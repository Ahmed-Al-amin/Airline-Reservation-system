package com.example.airline.model.dao;

import com.example.airline.data.DatabaseManager;
import com.example.airline.model.entity.Flight;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException; // Import for specific exception
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlightDAO {

    // Use ISO standard formats which SQLite understands well for TEXT columns
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME; // HH:MM:SS or HH:MM

    /** Maps a row from the ResultSet to a Flight object. */
    private Flight mapResultSetToFlight(ResultSet rs) throws SQLException {
        try {
            return new Flight(
                    rs.getString("flight_number"),
                    rs.getString("departure_city"),
                    rs.getString("destination_city"),
                    rs.getDouble("fare"),
                    rs.getInt("total_seats"),
                    LocalDate.parse(rs.getString("departure_date"), DATE_FORMATTER),
                    LocalTime.parse(rs.getString("departure_time"), TIME_FORMATTER),
                    LocalTime.parse(rs.getString("arrival_time"), TIME_FORMATTER)
            );
        } catch (DateTimeParseException | NullPointerException | NumberFormatException e) {
            // Log mapping errors more specifically
            System.err.println("Error mapping ResultSet to Flight for flight_number "
                    + rs.getString("flight_number") + ": " + e.getMessage());
            return null; // Return null if data is corrupt/unparsable
        }
    }

    /** Retrieves all flights from the database. */
    public List<Flight> getAllFlights() {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT * FROM flights ORDER BY departure_date, departure_time"; // Add default order
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Flight flight = mapResultSetToFlight(rs);
                if (flight != null) { // Add only if mapping was successful
                    flights.add(flight);
                }
            }
        } catch (SQLException e) {
            System.err.println("DAO Error getting all flights: " + e.getMessage());
            // Return empty list, error handled by caller (Service)
        }
        return flights;
    }

    /** Retrieves a specific flight by its number. */
    public Flight getFlightByNumber(String flightNumber) {
        String sql = "SELECT * FROM flights WHERE flight_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, flightNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFlight(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("DAO Error getting flight by number (" + flightNumber + "): " + e.getMessage());
        }
        return null; // Not found or error
    }

    /** Adds a new flight record to the database. */
    public boolean addFlight(Flight flight) {
        String sql = "INSERT INTO flights(flight_number, departure_city, destination_city, fare, total_seats, " +
                "departure_date, departure_time, arrival_time) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, flight.getFlightNumber());
            pstmt.setString(2, flight.getDepartureCity());
            pstmt.setString(3, flight.getDestinationCity());
            pstmt.setDouble(4, flight.getFare());
            pstmt.setInt(5, flight.getTotalSeats());
            pstmt.setString(6, flight.getDepartureDate().format(DATE_FORMATTER));
            pstmt.setString(7, flight.getDepartureTime().format(TIME_FORMATTER));
            pstmt.setString(8, flight.getArrivalTime().format(TIME_FORMATTER));
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            // Check specific constraint errors
            if (e.getMessage() != null && e.getMessage().contains("SQLITE_CONSTRAINT_PRIMARYKEY")) {
                System.err.println("DAO Error adding flight: Flight number '" + flight.getFlightNumber() + "' already exists.");
            } else {
                System.err.println("DAO Error adding flight (" + flight.getFlightNumber() + "): " + e.getMessage());
            }
            return false;
        }
    }

    /** Updates an existing flight record in the database. */
    public boolean updateFlight(Flight flight) {
        String sql = "UPDATE flights SET departure_city = ?, destination_city = ?, fare = ?, total_seats = ?, " +
                "departure_date = ?, departure_time = ?, arrival_time = ? WHERE flight_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, flight.getDepartureCity());
            pstmt.setString(2, flight.getDestinationCity());
            pstmt.setDouble(3, flight.getFare());
            pstmt.setInt(4, flight.getTotalSeats());
            pstmt.setString(5, flight.getDepartureDate().format(DATE_FORMATTER));
            pstmt.setString(6, flight.getDepartureTime().format(TIME_FORMATTER));
            pstmt.setString(7, flight.getArrivalTime().format(TIME_FORMATTER));
            pstmt.setString(8, flight.getFlightNumber()); // WHERE clause parameter
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("DAO Warning: Update affected 0 rows for flight " + flight.getFlightNumber() + ". Flight might not exist.");
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("DAO Error updating flight (" + flight.getFlightNumber() + "): " + e.getMessage());
            return false;
        }
    }

    /** Overwrites all flights in the database with the provided list. Uses a transaction. */
    public boolean overwriteFlights(List<Flight> flights) {
        String deleteSql = "DELETE FROM flights";
        String insertSql = "INSERT INTO flights(flight_number, departure_city, destination_city, fare, total_seats, " +
                "departure_date, departure_time, arrival_time) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Delete existing
            try (Statement stmt = conn.createStatement()) { stmt.executeUpdate(deleteSql); }

            // Insert new using Batching
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (Flight flight : flights) {
                    // ... (set all 8 parameters from flight object) ...
                    pstmt.setString(1, flight.getFlightNumber());
                    pstmt.setString(2, flight.getDepartureCity());
                    pstmt.setString(3, flight.getDestinationCity());
                    pstmt.setDouble(4, flight.getFare());
                    pstmt.setInt(5, flight.getTotalSeats());
                    pstmt.setString(6, flight.getDepartureDate().format(DATE_FORMATTER));
                    pstmt.setString(7, flight.getDepartureTime().format(TIME_FORMATTER));
                    pstmt.setString(8, flight.getArrivalTime().format(TIME_FORMATTER));
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit(); // Commit transaction
            System.out.println("DAO: Successfully overwrote flights table.");
            return true;

        } catch (SQLException e) {
            System.err.println("DAO Error overwriting flights: " + e.getMessage());
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { /* log */ } }
            return false;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* log */ } }
        }
    }

    /** Initializes flights only if the table is empty. */
    public void initializeFlights(List<Flight> initialFlights) {
        String countSql = "SELECT COUNT(*) FROM flights";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("DAO: Flights table is empty. Initializing data...");
                overwriteFlights(initialFlights); // Call the overwrite method
            } else {
                System.out.println("DAO: Flights table already contains data. Skipping initialization.");
            }
        } catch (SQLException e) {
            System.err.println("DAO Error checking/initializing flights table: " + e.getMessage());
        }
    }

    /** Deletes a flight record from the database. */
    public boolean deleteFlight(String flightNumber) {
        String sql = "DELETE FROM flights WHERE flight_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, flightNumber);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("DAO Error deleting flight (" + flightNumber + "): " + e.getMessage());
            return false;
        }
    }
}