package com.example.airline.model.entity;

public class Reservation {
    private String reservationId; // Unique identifier for each reservation
    private String flightNumber;
    private String passengerUsername;
    private String seatNumber;

    public Reservation() {
        // Default constructor
    }

    public Reservation(String reservationId, String flightNumber, String passengerUsername, String seatNumber) {
        this.reservationId = reservationId;
        this.flightNumber = flightNumber;
        this.passengerUsername = passengerUsername;
        this.seatNumber = seatNumber;
    }

    // Getters
    public String getReservationId() {
        return reservationId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getPassengerUsername() {
        return passengerUsername;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    // Setters
    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public void setPassengerUsername(String passengerUsername) {
        this.passengerUsername = passengerUsername;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId='" + reservationId + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", passengerUsername='" + passengerUsername + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                '}';
    }
}