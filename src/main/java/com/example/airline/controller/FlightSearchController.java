package com.example.airline.controller;

import com.example.airline.model.entity.Flight;
import com.example.airline.model.entity.User;
import com.example.airline.model.service.FlightService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FlightSearchController {

    // FXML Fields remain the same...
    @FXML private ComboBox<String> departureCityComboBox;
    @FXML private ComboBox<String> destinationCityComboBox;
    @FXML private DatePicker departureDatePicker;
    @FXML private Button searchButton;
    @FXML private TableView<Flight> flightsTableView;
    @FXML private TableColumn<Flight, String> flightNumberColumn;
    @FXML private TableColumn<Flight, String> departureCityColumn;
    @FXML private TableColumn<Flight, String> destinationCityColumn;
    @FXML private TableColumn<Flight, String> departureDateColumn;
    @FXML private TableColumn<Flight, String> departureTimeColumn;
    @FXML private TableColumn<Flight, String> arrivalTimeColumn;
    @FXML private TableColumn<Flight, Double> fareColumn;
    @FXML private TableColumn<Flight, Integer> totalSeatsColumn;
    @FXML private Button bookFlightButton;
    @FXML private Button viewHistoryButton;
    @FXML private Button logoutButton;
    @FXML private Label infoLabel; // Optional: Add a label to FXML for info messages
    @FXML private Button profileButton; // New Button


    private final FlightService flightService = new FlightService();
    private User loggedInUser;
    // Use ObservableList to hold the flights currently displayed in the table
    private ObservableList<Flight> displayedFlights = FXCollections.observableArrayList();

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        if (loggedInUser != null) {
            populateCityComboBoxes();
            initializeTable();
            // --- Load initial upcoming flights ---
            loadInitialFlights();
            // --- End of initial load ---
        } else {
            System.err.println("Error: Logged in user is null in FlightSearchController.");
            if (infoLabel != null) infoLabel.setText("Error: User not logged in.");
            // Consider disabling controls
        }
    }

    private void populateCityComboBoxes() {
        // (Same as before)
        List<String> cities = flightService.getAllAvailableFlights().stream()
                .flatMap(flight -> List.of(flight.getDepartureCity(), flight.getDestinationCity()).stream())
                .distinct()
                .sorted()
                .toList();
        departureCityComboBox.setItems(FXCollections.observableList(cities));
        destinationCityComboBox.setItems(FXCollections.observableList(cities));
        departureDatePicker.setValue(LocalDate.now());
    }

    private void initializeTable() {
        // (Cell value factories remain the same)
        flightNumberColumn.setCellValueFactory(new PropertyValueFactory<>("flightNumber"));
        departureCityColumn.setCellValueFactory(new PropertyValueFactory<>("departureCity"));
        destinationCityColumn.setCellValueFactory(new PropertyValueFactory<>("destinationCity"));
        departureDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDepartureDate"));
        departureTimeColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDepartureTime"));
        arrivalTimeColumn.setCellValueFactory(new PropertyValueFactory<>("formattedArrivalTime"));
        fareColumn.setCellValueFactory(new PropertyValueFactory<>("fare"));
        fareColumn.setCellFactory(tc -> new TableCell<Flight, Double>() { /* ... currency formatting ... */
            @Override protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("$%.2f", price));
            }
        });
        totalSeatsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSeats"));

        // Default sort order
        departureDateColumn.setSortType(TableColumn.SortType.ASCENDING);
        departureTimeColumn.setSortType(TableColumn.SortType.ASCENDING); // Add secondary sort default
        flightsTableView.getSortOrder().addAll(departureDateColumn, departureTimeColumn); // Apply defaults

        // Bind the sorted list to the TableView for dynamic sorting
        SortedList<Flight> sortedData = new SortedList<>(displayedFlights);
        sortedData.comparatorProperty().bind(flightsTableView.comparatorProperty());
        flightsTableView.setItems(sortedData);
    }

    /**
     * Loads and displays all upcoming flights, sorted by date/time, when the view first loads.
     */
    private void loadInitialFlights() {
        System.out.println("Loading initial upcoming flights...");
        List<Flight> upcomingFlights = flightService.getAllUpcomingFlightsSorted();
        displayedFlights.setAll(upcomingFlights); // Update the list bound to the table
        flightsTableView.sort(); // Apply default sort

        if (infoLabel != null) {
            if (upcomingFlights.isEmpty()) {
                infoLabel.setText("No upcoming flights found in the system.");
            } else {
                infoLabel.setText("Showing all upcoming flights. Use search to filter.");
            }
        }
        System.out.println("Displayed " + upcomingFlights.size() + " upcoming flights.");
    }


    /**
     * Handles the search button click. Filters flights based on user criteria
     * and updates the TableView. Only shows flights on or after today.
     */
    @FXML
    private void handleSearch() {
        String departureCity = departureCityComboBox.getValue();
        String destinationCity = destinationCityComboBox.getValue();
        LocalDate selectedDate = departureDatePicker.getValue();
        LocalDate today = LocalDate.now();

        // Validation
        if (departureCity == null || destinationCity == null || selectedDate == null) {
            showAlert(Alert.AlertType.WARNING, "Search Criteria Missing", "Please select departure city, destination city, and a date.");
            return;
        }
        if (departureCity.equalsIgnoreCase(destinationCity)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Route", "Departure and destination cities cannot be the same.");
            return;
        }
        // Prevent searching for dates in the past
        if (selectedDate.isBefore(today)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Date", "Cannot search for flights on a past date. Please select today or a future date.");
            departureDatePicker.setValue(today); // Reset date picker to today
            return;
        }

        System.out.println("Searching flights for: " + departureCity + " -> " + destinationCity + " on " + selectedDate);
        // Use the existing search method for the specific date
        List<Flight> searchResults = flightService.searchFlights(departureCity, destinationCity, selectedDate);

        // No need for extra filtering/sorting here as searchFlights targets a specific future/present date
        // and initializeTable sets the default sort order for the TableView.
        displayedFlights.setAll(searchResults); // Update the list bound to the table
        flightsTableView.sort(); // Re-apply table sort

        if (infoLabel != null) {
            if (searchResults.isEmpty()) {
                infoLabel.setText("No flights found matching your search criteria for " + selectedDate + ".");
            } else {
                infoLabel.setText("Showing search results for " + selectedDate + ".");
            }
        }
        System.out.println("Found " + searchResults.size() + " flights matching search.");
    }

    // handleBookFlight, handleViewHistory, handleLogout remain functionally the same
    // (ensure they use the correct button fx:ids for stage access)
    // ... (rest of the controller as before) ...

    @FXML
    private void handleBookFlight() {
        Flight selectedFlight = flightsTableView.getSelectionModel().getSelectedItem();
        if (selectedFlight == null) { /* ... show alert ... */
            showAlert(Alert.AlertType.WARNING, "No Flight Selected", "Please select a flight from the table to book.");
            return;
        }
        // Date check already done in handleSearch filtering, but good to double-check
        if (selectedFlight.getDepartureDate().isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.ERROR, "Flight Departed", "This flight has already departed.");
            loadInitialFlights(); // Refresh list to remove it
            return;
        }
        // Navigation logic (using bookFlightButton for stage)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/flight_booking.fxml"));
            Parent root = loader.load();
            FlightBookingController controller = loader.getController();
            if (controller != null) { controller.setFlight(selectedFlight); controller.setLoggedInUser(loggedInUser); }
            else { throw new IllegalStateException("Controller null"); }

            if (bookFlightButton != null && bookFlightButton.getScene() != null) {
                Stage stage = (Stage) bookFlightButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Book Seats for Flight " + selectedFlight.getFlightNumber());
            } else { throw new IllegalStateException("Stage null"); }
        } catch (IOException | IllegalStateException e) { /* ... handle error ... */
            System.err.println("Error navigating to booking: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not open the booking screen.");
        }
    }

    @FXML
    private void handleViewHistory() {
        if (loggedInUser == null) { /* ... show alert ... */ showAlert(Alert.AlertType.ERROR, "Error", "User information not available."); return; }
        // Navigation logic (using viewHistoryButton for stage)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/ticket_history.fxml"));
            Parent root = loader.load();
            TicketHistoryController controller = loader.getController();
            if (controller != null) { controller.setLoggedInUser(loggedInUser); controller.loadReservationHistory();}
            else { throw new IllegalStateException("Controller null"); }

            if (viewHistoryButton != null && viewHistoryButton.getScene() != null) {
                Stage stage = (Stage) viewHistoryButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Reservation History");
            } else { throw new IllegalStateException("Stage null"); }
        } catch (IOException | IllegalStateException e) { /* ... handle error ... */
            System.err.println("Error navigating to history: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not open the history screen.");
        }
    }

    @FXML
    private void handleLogout() {
        // Navigation logic (using logoutButton for stage)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/welcome.fxml"));
            Parent root = loader.load();
            if (logoutButton != null && logoutButton.getScene() != null) {
                Stage stage = (Stage) logoutButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Welcome to Airline Reservation System");
                stage.show();
            } else { throw new IllegalStateException("Stage null"); }
        } catch (IOException | IllegalStateException e) { /* ... handle error ... */
            System.err.println("Error on logout: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not return to the welcome screen.");
        }
    }

    // --- NEW HANDLER for Profile Button ---
    @FXML
    private void handleViewProfile() {
        if (loggedInUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "User information not available.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/passenger_profile.fxml"));
            Parent root = loader.load();

            PassengerProfileController controller = loader.getController();
            if (controller != null) {
                controller.setLoggedInUser(loggedInUser); // Pass user data
                controller.loadProfileData(); // Load data into the form
            } else {
                throw new IllegalStateException("PassengerProfileController was null after loading FXML.");
            }

            Stage profileStage = new Stage();
            profileStage.setTitle("My Profile - " + loggedInUser.getUsername());
            profileStage.setScene(new Scene(root));
            profileStage.initModality(Modality.WINDOW_MODAL); // Block main window

            // Set owner window
            Window owner = profileButton.getScene().getWindow();
            if(owner != null) profileStage.initOwner(owner);

            profileStage.showAndWait(); // Show and wait

            // Optional: Refresh user data if changes might affect display elsewhere
            // loggedInUser = userService.getUserByUsername(loggedInUser.getUsername());

        } catch (IOException | IllegalStateException e) {
            System.err.println("Error loading profile screen: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Could not open the profile screen.");
        }
    }
    // --- END NEW HANDLER ---
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}