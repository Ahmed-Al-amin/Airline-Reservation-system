package com.example.airline.model.service;

import com.example.airline.model.dao.PaymentDAO;
import com.example.airline.model.entity.Payment;

public class PaymentService {
    private final PaymentDAO paymentDAO = new PaymentDAO();


    public boolean processPayment(String reservationId, double amount, double ticketPrice, String creditCardIdentifier) {
        // Check if reservationId is provided (it should be after the fix)
        if (reservationId == null || reservationId.trim().isEmpty()) {
            System.err.println("Error processing payment: Reservation ID is missing.");
            // Depending on policy, you might still record the payment attempt,
            // but linking it is impossible. Let's deny it for now.
            return false;
        }

        // Simple check: Amount paid must be >= ticket price
        if (amount >= ticketPrice) {
            String paymentId = paymentDAO.generateUniquePaymentId();
            // Use creditCardIdentifier when creating the Payment object
            Payment payment = new Payment(paymentId, reservationId, amount, creditCardIdentifier);
            try {
                paymentDAO.addPayment(payment); // Save the payment record
                System.out.println("Payment record created: ID " + paymentId + " for Reservation " + reservationId);
                return true; // Payment successful
            } catch (Exception e) {
                System.err.println("Error saving payment record for Reservation " + reservationId + ": " + e.getMessage());
                // Payment processed logically, but saving failed. This is an issue.
                // For simplicity here, we return false, but real system needs robust error handling.
                return false;
            }

        } else {
            System.out.println("Payment denied for Reservation " + reservationId + ": Insufficient amount paid (" + amount + " < " + ticketPrice + ")");
            return false; // Transaction denied due to insufficient amount
        }
    }
}