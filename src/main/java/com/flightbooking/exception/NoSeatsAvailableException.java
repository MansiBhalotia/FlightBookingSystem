package com.flightbooking.exception;

public class NoSeatsAvailableException extends RuntimeException {
    public NoSeatsAvailableException(String flightNumber, int requested, int available) {
        super(String.format(
            "Not enough seats on flight %s: requested %d, available %d",
            flightNumber, requested, available));
    }
}

