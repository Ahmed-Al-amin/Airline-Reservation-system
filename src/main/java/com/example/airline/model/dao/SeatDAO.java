package com.example.airline.model.dao;

import com.example.airline.data.DatabaseManager;
import com.example.airline.model.entity.Seat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeatDAO {

    private Seat mapResultSetToSeat(ResultSet rs) throws SQLException {
        // Assumes flight_number is also selected or provided externally
        return new Seat(
                rs.getString("flight_number"),
                rs.getString("seat_number"),
                rs.getInt("is_reserved") == 1 // Convert int to boolean
        );
    }

    // Gets all seats for a specific flight
    public List<Seat> getSeatsForFlight(String flightNumber) {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT * FROM seats WHERE flight_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, flightNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    seats.add(mapResultSetToSeat(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting seats for flight " + flightNumber + ": " + e.getMessage());
        }
        return seats;
    }

    public List<Seat> getAvailableSeats(String flightNumber) {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT * FROM seats WHERE flight_number = ? AND is_reserved = 0 ORDER BY CAST(seat_number AS INTEGER)"; // Order numerically
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, flightNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    seats.add(mapResultSetToSeat(rs));
                }
            }
        } catch (SQLException e) { /* handle */ }
        return seats;
    }



    public Seat getSeat(String flightNumber, String seatNumber) {
        String sql = "SELECT * FROM seats WHERE flight_number = ? AND seat_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, flightNumber);
            pstmt.setString(2, seatNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSeat(rs);
                }
            }
        } catch (SQLException e) { /* handle */ }
        return null;
    }

    private boolean updateSeatReservationStatus(String flightNumber, String seatNumber, boolean reserve) {
        String sql = "UPDATE seats SET is_reserved = ? WHERE flight_number = ? AND seat_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reserve ? 1 : 0); // Set 1 for true, 0 for false
            pstmt.setString(2, flightNumber);
            pstmt.setString(3, seatNumber);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating seat reservation status for " + flightNumber + "/" + seatNumber + ": " + e.getMessage());
            return false;
        }
    }

    public boolean reserveSeat(String flightNumber, String seatNumber) {
        return updateSeatReservationStatus(flightNumber, seatNumber, true);
    }

    public boolean unreserveSeat(String flightNumber, String seatNumber) {
        return updateSeatReservationStatus(flightNumber, seatNumber, false);
    }

    /** Creates seat records only if they don't exist for the flight */
    public boolean initializeSeatsForFlight(String flightNumber, int totalSeats) {
        if (seatsExistForFlight(flightNumber)) {
            System.out.println("Seats already exist for flight " + flightNumber + ". Skipping initialization.");
            // Optional: Add verification/update logic here if needed
            return true; // Indicate they exist or verification passed
        }
        if (totalSeats <= 0) {
            System.err.println("Cannot initialize seats for flight " + flightNumber + " with zero or negative total seats.");
            return false;
        }

        System.out.println("Initializing " + totalSeats + " seats for flight " + flightNumber + "...");
        String sql = "INSERT INTO seats (flight_number, seat_number, is_reserved) VALUES (?, ?, 0)";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Use transaction for batch insert

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 1; i <= totalSeats; i++) {
                    pstmt.setString(1, flightNumber);
                    pstmt.setString(2, String.valueOf(i)); // Store seat number as string
                    pstmt.addBatch();
                    // Execute batch periodically for very large numbers of seats (optional)
                    // if (i % 100 == 0) { pstmt.executeBatch();}
                }
                pstmt.executeBatch(); // Execute remaining batch
            }
            conn.commit();
            System.out.println("Successfully initialized seats for flight " + flightNumber);
            return true;

        } catch (SQLException e) {
            System.err.println("Error initializing seats for flight " + flightNumber + ": " + e.getMessage());
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { /* ignore rollback error */ } }
            return false;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignore */ } }
        }
    }

    public boolean seatsExistForFlight(String flightNumber) {
        String sql = "SELECT COUNT(*) FROM seats WHERE flight_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, flightNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking if seats exist for flight " + flightNumber + ": " + e.getMessage());
            return false; // Assume they don't exist on error
        }
    }
}