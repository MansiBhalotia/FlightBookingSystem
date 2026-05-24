package com.flightbooking.dto;

import com.flightbooking.model.Flight;
import java.time.LocalDateTime;

public class FlightResponse {

    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private int totalSeats;
    private int availableSeats;
    /**
     * Optimistic-concurrency version. Clients must echo this value back in
     * the {@code flightVersion} field of POST /api/bookings. If the version
     * has changed by the time the booking is processed, the request is rejected
     * with 409 Conflict.
     */
    private long version;

    public static FlightResponse from(Flight flight) {
        FlightResponse r = new FlightResponse();
        r.flightNumber = flight.getFlightNumber();
        r.origin = flight.getOrigin();
        r.destination = flight.getDestination();
        r.departureTime = flight.getDepartureTime();
        r.totalSeats = flight.getTotalSeats();
        r.availableSeats = flight.getAvailableSeats();
        r.version = flight.getVersion();
        return r;
    }

    public String getFlightNumber() { return flightNumber; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public int getTotalSeats() { return totalSeats; }
    public int getAvailableSeats() { return availableSeats; }
    public long getVersion() { return version; }
}

