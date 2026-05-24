package com.flightbooking.exception;

public class StaleFlightDataException extends RuntimeException {
    public StaleFlightDataException(String flightNumber) {
        super("Flight data has changed since you last viewed it. " +
              "Please refresh the flight details for '" + flightNumber +
              "' and try again.");
    }
}

