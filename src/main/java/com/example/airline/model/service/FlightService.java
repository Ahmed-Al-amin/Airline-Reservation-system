package com.example.airline.model.service;

import com.example.airline.model.dao.FlightDAO;
import com.example.airline.model.dao.ReservationDAO; // Import ReservationDAO
import com.example.airline.model.entity.Flight;
// Removed java.io.File import
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections; // Import Collections for emptyList
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FlightService {
    private final FlightDAO flightDAO = new FlightDAO();
    private final SeatService seatService = new SeatService(); // SeatService uses SeatDAO (SQLite version)
    private final ReservationDAO reservationDAO = new ReservationDAO(); // Needed for checking reservations

    /** Retrieves all flights from the database. */
    public List<Flight> getAllAvailableFlights() {
        try {
            // This now gets ALL flights, including past ones, needed for admin view
            return flightDAO.getAllFlights();
        } catch (Exception e) {
            System.err.println("Error retrieving all flights from service: " + e.getMessage());
            return Collections.emptyList(); // Return empty list on error
        }
    }

    /** Retrieves all flights scheduled for today or later, sorted by date and time. */
    public List<Flight> getAllUpcomingFlightsSorted() {
        LocalDate today = LocalDate.now();
        try {
            // Filter for upcoming flights here in the service layer
            return flightDAO.getAllFlights().stream()
                    .filter(flight -> flight != null && !flight.getDepartureDate().isBefore(today))
                    .sorted(Comparator.comparing(Flight::getDepartureDate)
                            .thenComparing(Flight::getDepartureTime))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error retrieving upcoming flights from service: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Gets a specific flight by its number. */
    public Flight getFlightByNumber(String flightNumber) {
        try {
            return flightDAO.getFlightByNumber(flightNumber);
        } catch (Exception e) {
            System.err.println("Error retrieving flight by number ("+flightNumber+") from service: " + e.getMessage());
            return null;
        }
    }

    /** Searches for flights matching criteria ON a specific date. */
    public List<Flight> searchFlights(String departureCity, String destinationCity, LocalDate searchDate) {
        // Only return future/present flights matching criteria
        LocalDate today = LocalDate.now();
        if (searchDate.isBefore(today)) {
            System.out.println("Service: Search date is in the past. Returning empty list.");
            return Collections.emptyList();
        }
        try {
            return flightDAO.getAllFlights().stream()
                    .filter(flight -> flight != null &&
                            flight.getDepartureCity().equalsIgnoreCase(departureCity) &&
                            flight.getDestinationCity().equalsIgnoreCase(destinationCity) &&
                            flight.getDepartureDate().isEqual(searchDate))
                    // No need to sort here, TableView handles it
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error searching flights from service: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Adds a new flight and initializes its seats. */
    public boolean addFlight(String flightNumber, String departureCity, String destinationCity, double fare, int totalSeats, LocalDate departureDate, LocalTime departureTime, LocalTime arrivalTime) {
        if (getFlightByNumber(flightNumber) != null) {
            System.err.println("Service Error: Flight with number " + flightNumber + " already exists.");
            return false;
        }
        // Basic validation can be added here (e.g., departure date not in past)
        if (departureDate.isBefore(LocalDate.now())) {
            System.err.println("Service Error: Cannot add flight for a past date.");
            return false;
        }

        Flight newFlight = new Flight(flightNumber, departureCity, destinationCity, fare, totalSeats, departureDate, departureTime, arrivalTime);
        boolean flightAdded = flightDAO.addFlight(newFlight);
        if (flightAdded) {
            System.out.println("Added flight: " + flightNumber);
            // Initialize seats after successful flight addition
            boolean seatsInitialized = seatService.initializeSeatsForFlight(flightNumber, totalSeats);
            if (!seatsInitialized) {
                System.err.println("Warning: Flight " + flightNumber + " added, but failed to initialize seats.");
                // Consider if flight should be removed if seats fail - depends on requirements
                // For now, we report the warning but consider the flight added.
            }
            return true;
        } else {
            System.err.println("Service Error: Failed to add flight " + flightNumber + " to database.");
            return false;
        }
    }



    @Deprecated
    public void addFlight(String flightNumber, String departureCity, String destinationCity, double fare, int totalSeats) {
        // This version internally calls the detailed one with default dates/times
        LocalDate defaultDate = LocalDate.now().plusDays(1);
        LocalTime defaultDeparture = LocalTime.of(10, 0);
        LocalTime defaultArrival = defaultDeparture.plusHours(3); // Example duration
        System.out.println("Warning: Using deprecated addFlight. Use the detailed version.");
        addFlight(flightNumber, departureCity, destinationCity, fare, totalSeats, defaultDate, defaultDeparture, defaultArrival);
    }

    public int updatePastFlightDatesToNextMonth() {
        List<Flight> allFlights = getAllAvailableFlights(); // Use service method to get all
        LocalDate today = LocalDate.now();
        int updatedCount = 0;
        List<Flight> flightsToUpdate = new ArrayList<>(); // Collect flights needing update

        for (Flight flight : allFlights) {
            // Check only the date part for being in the past
            if (flight != null && flight.getDepartureDate().isBefore(today)) {
                LocalDate oldDate = flight.getDepartureDate();
                LocalDate newDate = oldDate.plusMonths(1);
                // Ensure the new date is not in the past either (edge case if run very late in month)
                if(newDate.isBefore(today)) {
                    newDate = today; // Or set to today + 1 day, depending on policy
                }
                flight.setDepartureDate(newDate); // Modify the object
                flightsToUpdate.add(flight); // Add to list for bulk update
                System.out.println("Marking flight " + flight.getFlightNumber() + " (Date: "+oldDate+") for date update to: " + newDate);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            System.out.println("Attempting to persist date updates for " + updatedCount + " flights...");
            // Update each modified flight individually using DAO update
            int successfullyUpdated = 0;
            for (Flight flight : flightsToUpdate) {
                if (flightDAO.updateFlight(flight)) { // Use the single update method
                    successfullyUpdated++;
                } else {
                    System.err.println("Failed to persist date update for flight: " + flight.getFlightNumber());
                    // Log this error properly
                }
            }
            System.out.println("Successfully persisted date updates for " + successfullyUpdated + " flights.");
            // Return the count of flights whose update was *attempted*
            return updatedCount;
        }
        return 0; // No updates needed
    }


    /** Initializes flights in the database if empty and ensures seats are created. */
    public boolean initializeFlights(List<Flight> initialFlights) {
        // Check is done in Main, here we just perform the action
        System.out.println("Attempting to initialize flights in database...");
        // The DAO's overwriteFlights handles the transaction
        boolean flightsWritten = flightDAO.overwriteFlights(initialFlights);
        if (flightsWritten) {
            System.out.println("Base flight data written. Initializing seats...");
            int seatInitFailures = 0;
            for (Flight flight : initialFlights) {
                if (!seatService.initializeSeatsForFlight(flight.getFlightNumber(), flight.getTotalSeats())) {
                    seatInitFailures++;
                    System.err.println("Failed to initialize seats for initial flight: " + flight.getFlightNumber());
                }
            }
            if (seatInitFailures > 0) {
                System.err.println("Warning: " + seatInitFailures + " flights failed seat initialization.");
                return false; // Indicate partial failure
            }
            return true; // All succeeded
        } else {
            System.err.println("Failed to write initial flight data to database.");
            return false;
        }
    }

    /** Ensures seat records exist for all flights currently in the database. */
    public void ensureSeatRecordsForAllFlights() {
        System.out.println("Verifying seat records for all existing flights in database...");
        List<Flight> allFlights = getAllAvailableFlights();
        if (allFlights.isEmpty()) {
            System.out.println("No flights found in database to verify seats for.");
            return;
        }
        int createdCount = 0;
        int verifiedCount = 0;
        for (Flight flight : allFlights) {
            if (flight == null) continue; // Skip if there was an error retrieving a flight
            // Use SeatService's check which now uses the SeatDAO (SQLite)
            if (!seatService.seatsExistForFlight(flight.getFlightNumber())) {
                System.out.println("Seat records missing for flight: " + flight.getFlightNumber() + ". Creating...");
                if (seatService.initializeSeatsForFlight(flight.getFlightNumber(), flight.getTotalSeats())) {
                    createdCount++;
                } else {
                    System.err.println("Failed to create missing seat records for " + flight.getFlightNumber());
                }
            } else {
                verifiedCount++;
            }
        }
        System.out.println("Seat record verification complete. Verified: " + verifiedCount + ", Created: " + createdCount);
    }

}