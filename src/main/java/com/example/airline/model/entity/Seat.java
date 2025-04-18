package com.example.airline.model.entity;

public class Seat {
    // Flight number is still useful context, even if not stored in the per-flight CSV row
    private String flightNumber;
    private String seatNumber;
    private boolean isReserved;

    public Seat() {
        // Default constructor
    }

    // Constructor used when creating seats initially or reading from file
    public Seat(String flightNumber, String seatNumber, boolean isReserved) {
        this.flightNumber = flightNumber;
        this.seatNumber = seatNumber;
        this.isReserved = isReserved;
    }

    // Convenience constructor assuming initially not reserved
    public Seat(String flightNumber, String seatNumber) {
        this(flightNumber, seatNumber, false);
    }


    // Getters
    public String getFlightNumber() {
        return flightNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public boolean isReserved() {
        return isReserved;
    }

    // Setters
    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setReserved(boolean reserved) {
        isReserved = reserved;
    }

    @Override
    public String toString() {
        return "Seat{" +
                "flightNumber='" + flightNumber + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", isReserved=" + isReserved +
                '}';
    }

    // equals and hashCode are good practice if storing Seats in Sets/Maps
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return java.util.Objects.equals(flightNumber, seat.flightNumber) &&
                java.util.Objects.equals(seatNumber, seat.seatNumber);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(flightNumber, seatNumber);
    }
}