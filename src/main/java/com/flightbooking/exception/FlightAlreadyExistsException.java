package com.flightbooking.exception;

public class FlightAlreadyExistsException extends RuntimeException {
    public FlightAlreadyExistsException(String flightNumber) {
        super("Flight already exists: " + flightNumber);
    }
}

