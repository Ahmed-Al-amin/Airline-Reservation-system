package com.example.airline.model.service;

import com.example.airline.model.dao.ReservationDAO;
import com.example.airline.model.entity.Reservation;
import com.example.airline.model.entity.Seat;
import java.sql.SQLException; // Can catch specifically if needed
import java.util.Collections;
import java.util.List;

public class ReservationService {
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final SeatService seatService = new SeatService(); // Uses updated SeatService

    /** Books a SINGLE seat, interacting with SQLite DAOs. */
    public String bookTicket(String flightNumber, String passengerUsername, String seatNumber) {
        Seat seat = seatService.getSeat(flightNumber, seatNumber);
        if (seat == null) {
            System.err.println("Service: Seat " + seatNumber + " not found for flight " + flightNumber);
            return null;
        }
        if (seat.isReserved()) {
            System.err.println("Service: Seat " + seatNumber + " already reserved on flight " + flightNumber);
            return null;
        }

        String reservationId = reservationDAO.generateUniqueReservationId();
        Reservation reservation = new Reservation(reservationId, flightNumber, passengerUsername, seatNumber);

        // --- Transaction-like flow ---
        // 1. Add Reservation Record
        boolean reservationAdded = reservationDAO.addReservation(reservation);
        if (!reservationAdded) {
            System.err.println("Service: Failed to add reservation record for ID " + reservationId);
            // Error likely logged in DAO (e.g., unique constraint violation)
            return null; // Stop if we can't even save the reservation
        }

        // 2. Reserve Seat
        boolean seatReserved = seatService.reserveSeat(flightNumber, seatNumber);
        if (!seatReserved) {
            System.err.println("Service CRITICAL: Failed to reserve seat " + seatNumber + " AFTER adding reservation record " + reservationId + ". Attempting rollback.");
            // Attempt to delete the reservation record we just added
            boolean rollbackSuccess = reservationDAO.deleteReservation(reservationId);
            if (!rollbackSuccess) {
                System.err.println("Service CRITICAL: Rollback FAILED for reservation " + reservationId + ". Data inconsistent!");
                // This requires manual intervention
            }
            return null; // Booking ultimately failed
        }

        // Success!
        System.out.println("Service: Successfully booked ticket: ID " + reservationId + " for Seat " + seatNumber);
        return reservationId;
    }

    /** Cancels a SINGLE reservation (deletes record) and unreserves seat. */
    public boolean cancelTicket(String reservationId) {
        Reservation reservation = getReservationDetails(reservationId); // Use service method
        if (reservation == null) {
            System.err.println("Service: Cancellation failed: Reservation ID " + reservationId + " not found.");
            return false;
        }

        // 1. Delete Reservation Record
        boolean recordDeleted = reservationDAO.deleteReservation(reservationId);

        if (recordDeleted) {
            System.out.println("Service: Deleted reservation record: " + reservationId);
            // 2. Unreserve Seat (Best effort)
            boolean seatUnreserved = seatService.unreserveSeat(reservation.getFlightNumber(), reservation.getSeatNumber());
            if (!seatUnreserved) {
                System.err.println("Service Warning: Deleted reservation " + reservationId + ", but failed to unreserve seat " + reservation.getSeatNumber() + ". Seat status may be inconsistent.");
                // Still return true, as the reservation itself is gone
            } else {
                System.out.println("Service: Unreserved seat: " + reservation.getSeatNumber());
            }
            return true; // Reservation deleted successfully
        } else {
            System.err.println("Service: Failed to delete reservation record " + reservationId + " (already deleted or DB error).");
            return false; // Failed to delete
        }
    }

    /** Gets reservation history for a user. */
    public List<Reservation> getReservationHistory(String username) {
        try {
            return reservationDAO.getReservationsByUsername(username);
        } catch (Exception e) {
            System.err.println("Service Error getting reservation history for " + username + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Gets details for a specific reservation. */
    public Reservation getReservationDetails(String reservationId) {
        try {
            return reservationDAO.getReservationById(reservationId);
        } catch (Exception e) {
            System.err.println("Service Error getting reservation details for " + reservationId + ": " + e.getMessage());
            return null;
        }
    }

}