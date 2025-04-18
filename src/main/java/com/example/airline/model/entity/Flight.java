package com.example.airline.model.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Flight {
    private String flightNumber;
    private String departureCity;
    private String destinationCity;
    private double fare;
    private int totalSeats;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public Flight(String flightNumber, String departureCity, String destinationCity, double fare, int totalSeats, LocalDate departureDate, LocalTime departureTime, LocalTime arrivalTime) {
        this.flightNumber = flightNumber;
        this.departureCity = departureCity;
        this.destinationCity = destinationCity;
        this.fare = fare;
        this.totalSeats = totalSeats;
        this.departureDate = departureDate;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    // Constructor for reading from CSV (assuming String format for date and time)
    public Flight(String flightNumber, String departureCity, String destinationCity, String fare, String totalSeats, String departureDate, String departureTime, String arrivalTime) {
        this(flightNumber, departureCity, destinationCity, Double.parseDouble(fare), Integer.parseInt(totalSeats),
                LocalDate.parse(departureDate, DATE_FORMATTER),
                LocalTime.parse(departureTime, TIME_FORMATTER),
                LocalTime.parse(arrivalTime, TIME_FORMATTER));
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getDepartureCity() {
        return departureCity;
    }

    public void setDepartureCity(String departureCity) {
        this.departureCity = departureCity;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public double getFare() {
        return fare;
    }

    public void setFare(double fare) {
        this.fare = fare;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    // Method to format date for display
    public String getFormattedDepartureDate() {
        return departureDate.format(DATE_FORMATTER);
    }

    // Method to format time for display
    public String getFormattedDepartureTime() {
        return departureTime.format(TIME_FORMATTER);
    }

    // Method to format time for display
    public String getFormattedArrivalTime() {
        return arrivalTime.format(TIME_FORMATTER);
    }
}