package com.example.airline.model.service;

import com.example.airline.model.dao.SeatDAO;
import com.example.airline.model.entity.Seat;
import java.util.Collections;
import java.util.List;

public class SeatService {
    private final SeatDAO seatDAO = new SeatDAO();

    /** Gets available seats for a flight. */
    public List<Seat> getAvailableSeats(String flightNumber) {
        try {
            return seatDAO.getAvailableSeats(flightNumber);
        } catch (Exception e) {
            System.err.println("Service Error getting available seats for " + flightNumber + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }


    /** Gets a specific seat. */
    public Seat getSeat(String flightNumber, String seatNumber) {
        try {
            return seatDAO.getSeat(flightNumber, seatNumber);
        } catch (Exception e) {
            System.err.println("Service Error getting seat " + seatNumber + " for " + flightNumber + ": " + e.getMessage());
            return null;
        }
    }

    /** Attempts to reserve a seat. */
    public boolean reserveSeat(String flightNumber, String seatNumber) {
        try {
            return seatDAO.reserveSeat(flightNumber, seatNumber);
        } catch (Exception e) {
            System.err.println("Service Error reserving seat " + seatNumber + " for " + flightNumber + ": " + e.getMessage());
            return false;
        }
    }

    /** Attempts to unreserve a seat. */
    public boolean unreserveSeat(String flightNumber, String seatNumber) {
        try {
            return seatDAO.unreserveSeat(flightNumber, seatNumber);
        } catch (Exception e) {
            System.err.println("Service Error unreserving seat " + seatNumber + " for " + flightNumber + ": " + e.getMessage());
            return false;
        }
    }

    /** Initializes seat records in the database for a flight if they don't exist. */
    public boolean initializeSeatsForFlight(String flightNumber, int totalSeats) {
        try {
            // DAO method now handles check for existence and initialization
            return seatDAO.initializeSeatsForFlight(flightNumber, totalSeats);
        } catch (Exception e) {
            System.err.println("Service Error initializing seats for " + flightNumber + ": " + e.getMessage());
            return false;
        }
    }

    /** Checks if seat records exist for a flight. */
    public boolean seatsExistForFlight(String flightNumber) {
        try {
            return seatDAO.seatsExistForFlight(flightNumber);
        } catch (Exception e) {
            System.err.println("Service Error checking seat existence for " + flightNumber + ": " + e.getMessage());
            return false; // Assume not exists on error
        }
    }

}