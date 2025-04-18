package com.example.airline.model.dao;

import com.example.airline.data.DatabaseManager;
import com.example.airline.model.entity.Reservation;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ReservationDAO {

    /** Maps a row from the ResultSet to a Reservation object. */
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        // Use the simple constructor
        return new Reservation(
                rs.getString("reservation_id"),
                rs.getString("flight_number"),
                rs.getString("passenger_username"),
                rs.getString("seat_number")
        );
    }

    /** Retrieves a specific reservation by its ID. */
    public Reservation getReservationById(String reservationId) {
        String sql = "SELECT * FROM reservations WHERE reservation_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reservationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("DAO Error getting reservation by ID ("+reservationId+"): " + e.getMessage());
        }
        return null;
    }

    /** Retrieves all reservations for a specific passenger. */
    public List<Reservation> getReservationsByUsername(String username) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE passenger_username = ?"; // Case-sensitive usually
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("DAO Error getting reservations by username ("+username+"): " + e.getMessage());
        }
        return reservations;
    }

    /** Adds a new reservation record to the database. */
    public boolean addReservation(Reservation reservation) {
        String sql = "INSERT INTO reservations(reservation_id, flight_number, passenger_username, seat_number) VALUES(?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reservation.getReservationId());
            pstmt.setString(2, reservation.getFlightNumber());
            pstmt.setString(3, reservation.getPassengerUsername());
            pstmt.setString(4, reservation.getSeatNumber());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            if (e.getMessage() != null && (e.getMessage().contains("SQLITE_CONSTRAINT_PRIMARYKEY") || e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE"))) {
                System.err.println("DAO Error adding reservation: Constraint violation (Reservation ID '" + reservation.getReservationId() + "' or Flight/Seat combo '"+reservation.getFlightNumber()+"/"+reservation.getSeatNumber()+"' likely exists).");
            } else {
                System.err.println("DAO Error adding reservation (" + reservation.getReservationId() + "): " + e.getMessage());
            }
            return false;
        }
    }

    /** Deletes a reservation record by its ID. */
    public boolean deleteReservation(String reservationId) {
        String sql = "DELETE FROM reservations WHERE reservation_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reservationId);
            int affectedRows = pstmt.executeUpdate();
            if(affectedRows == 0) {
                System.out.println("DAO Warning: No reservation found with ID " + reservationId + " to delete.");
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("DAO Error deleting reservation (" + reservationId + "): " + e.getMessage());
            return false;
        }
    }


    /** Generates a unique reservation ID. */
    public String generateUniqueReservationId() {
        return UUID.randomUUID().toString();
    }
}