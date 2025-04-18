package com.example.airline.data; // Changed package to data

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DATABASE_NAME = "airline.db";
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_NAME;

    // Ensure the database and tables are ready on class loading
    static {
        initializeDatabase();
    }

    /**
     * Establishes a connection to the SQLite database.
     * @return A Connection object.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Optional: Explicitly load the driver (usually not needed with modern JDBC)
            // Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            System.err.println("Error loading SQLite JDBC driver: " + e.getMessage());
            // Handle driver loading error appropriately
        }
        return DriverManager.getConnection(DATABASE_URL);
    }

    /**
     * Creates the necessary tables in the database if they don't already exist.
     * Should be called once on application startup.
     */
    public static void initializeDatabase() {
        // Use try-with-resources for Connection and Statement
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // --- Users Table ---
            // !! IMPORTANT: Adding columns to an existing table with data in SQLite
            // requires more complex ALTER TABLE statements or a data migration strategy.
            // This CREATE TABLE statement assumes a new database or manual alteration.
            String createUserTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY NOT NULL," +
                    "password TEXT NOT NULL," + // Store HASHED passwords in production!
                    "role TEXT NOT NULL CHECK(role IN ('passenger', 'admin'))," +
                    "approved INTEGER NOT NULL DEFAULT 0," + // 0=false, 1=true
                    "email TEXT," +                      // New Column (nullable)
                    "phone_number TEXT" +                // New Column (nullable)
                    ");";
            stmt.execute(createUserTable);
            System.out.println("Users table checked/created (with email/phone).");

            // --- Flights Table (Simplified version) ---
            String createFlightsTable = "CREATE TABLE IF NOT EXISTS flights (" +
                    "flight_number TEXT PRIMARY KEY NOT NULL," +
                    "departure_city TEXT NOT NULL," +
                    "destination_city TEXT NOT NULL," +
                    "fare REAL NOT NULL," +
                    "total_seats INTEGER NOT NULL," +
                    "departure_date TEXT NOT NULL," + // Store as ISO YYYY-MM-DD
                    "departure_time TEXT NOT NULL," + // Store as HH:MM
                    "arrival_time TEXT NOT NULL" +   // Store as HH:MM
                    ");";
            stmt.execute(createFlightsTable);
            System.out.println("Flights table checked/created.");

            // --- Seats Table (Simplified version) ---
            String createSeatsTable = "CREATE TABLE IF NOT EXISTS seats (" +
                    "flight_number TEXT NOT NULL," +
                    "seat_number TEXT NOT NULL," +
                    "is_reserved INTEGER NOT NULL DEFAULT 0," + // 0=false, 1=true
                    "PRIMARY KEY (flight_number, seat_number)," + // Composite key
                    "FOREIGN KEY (flight_number) REFERENCES flights(flight_number) ON DELETE CASCADE" + // Cascade delete if flight deleted
                    ");";
            stmt.execute(createSeatsTable);
            System.out.println("Seats table checked/created.");

            // --- Reservations Table (Simplified version) ---
            // Note: If a user is deleted, their reservations remain unless a trigger or manual cleanup is done.
            // Consider adding ON DELETE SET NULL or ON DELETE CASCADE if user deletion should affect reservations.
            String createReservationsTable = "CREATE TABLE IF NOT EXISTS reservations (" +
                    "reservation_id TEXT PRIMARY KEY NOT NULL," +
                    "flight_number TEXT NOT NULL," +
                    "passenger_username TEXT NOT NULL," +
                    "seat_number TEXT NOT NULL," +
                    // Unique constraint to prevent double booking same seat on same flight
                    "UNIQUE (flight_number, seat_number)," +
                    "FOREIGN KEY (flight_number) REFERENCES flights(flight_number) ON DELETE CASCADE," +
                    "FOREIGN KEY (passenger_username) REFERENCES users(username) ON DELETE SET NULL" + // Set username to NULL if user is deleted
                    // Optional: Link seat via FK (makes reuse complex), sticking to simple text link for now
                    // "FOREIGN KEY (flight_number, seat_number) REFERENCES seats(flight_number, seat_number)"
                    ");";
            stmt.execute(createReservationsTable);
            System.out.println("Reservations table checked/created.");

            // --- Payments Table ---
            String createPaymentsTable = "CREATE TABLE IF NOT EXISTS payments (" +
                    "payment_id TEXT PRIMARY KEY NOT NULL," +
                    "reservation_id TEXT NOT NULL," + // Can link to the primary reservation
                    "amount REAL NOT NULL," +
                    "credit_card_identifier TEXT NOT NULL," + // Masked / Last 4
                    "payment_timestamp TEXT DEFAULT CURRENT_TIMESTAMP," + // Record payment time
                    "FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE" + // Delete payment if reservation is deleted
                    ");";
            stmt.execute(createPaymentsTable);
            System.out.println("Payments table checked/created.");

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
            // Application might not be usable, consider exiting or more robust error handling
        }
    }
}