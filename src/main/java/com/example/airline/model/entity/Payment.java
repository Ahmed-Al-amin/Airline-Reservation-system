package com.example.airline.model.entity;

public class Payment {
    private String paymentId;       // Unique identifier for each payment
    private String reservationId;   // ID of the reservation this payment is for
    private double amount;          // Amount paid
    // Changed from creditCard to reflect storing only an identifier (e.g., last 4 or masked)
    private String creditCardIdentifier;

    public Payment() {
        // Default constructor
    }


    public Payment(String paymentId, String reservationId, double amount, String creditCardIdentifier) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.creditCardIdentifier = creditCardIdentifier; // Assign to the new field name
    }

    // Getters
    public String getPaymentId() {
        return paymentId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public double getAmount() {
        return amount;
    }

    // Updated getter name
    public String getCreditCardIdentifier() {
        return creditCardIdentifier;
    }

    // Updated setter name
    public void setCreditCardIdentifier(String creditCardIdentifier) {
        this.creditCardIdentifier = creditCardIdentifier;
    }

    @Override
    public String toString() {
        // Updated toString to use the new field name and reflect it's an identifier
        return "Payment{" +
                "paymentId='" + paymentId + '\'' +
                ", reservationId='" + reservationId + '\'' +
                ", amount=" + String.format("%.2f", amount) +
                ", creditCardIdentifier='" + creditCardIdentifier + '\'' +
                '}';
    }
}