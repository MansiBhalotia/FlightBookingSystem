package com.flightbooking.exception;

public class BookingNotPendingException extends RuntimeException {
    public BookingNotPendingException(String bookingId) {
        super("Booking " + bookingId + " cannot be confirmed — it is not in PENDING state.");
    }
}

