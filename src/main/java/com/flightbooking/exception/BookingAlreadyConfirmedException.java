package com.flightbooking.exception;

public class BookingAlreadyConfirmedException extends RuntimeException {
    public BookingAlreadyConfirmedException(String bookingId) {
        super("Booking " + bookingId + " is already confirmed.");
    }
}

