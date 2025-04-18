package com.example.airline.model.dao;

import com.example.airline.data.DatabaseManager;
import com.example.airline.model.entity.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaymentDAO {

    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        return new Payment(
                rs.getString("payment_id"),
                rs.getString("reservation_id"),
                rs.getDouble("amount"),
                rs.getString("credit_card_identifier")
                // Could also retrieve and parse rs.getString("payment_timestamp") if needed
        );
    }

    public List<Payment> getAllPayments() {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM payments ORDER BY payment_timestamp DESC"; // Order recent first
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                payments.add(mapResultSetToPayment(rs));
            }
        } catch (SQLException e) { /* handle */ }
        return payments;
    }

    public boolean addPayment(Payment payment) {
        String sql = "INSERT INTO payments(payment_id, reservation_id, amount, credit_card_identifier) VALUES(?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, payment.getPaymentId());
            pstmt.setString(2, payment.getReservationId());
            pstmt.setDouble(3, payment.getAmount());
            pstmt.setString(4, payment.getCreditCardIdentifier());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("SQLITE_CONSTRAINT_PRIMARYKEY")) {
                System.err.println("Error adding payment: Payment ID '" + payment.getPaymentId() + "' already exists.");
            } else {
                System.err.println("Error adding payment (" + payment.getPaymentId() + "): " + e.getMessage());
            }
            return false;
        }
    }

    // generateUniquePaymentId remains the same
    public String generateUniquePaymentId() {
        return UUID.randomUUID().toString();
    }
}