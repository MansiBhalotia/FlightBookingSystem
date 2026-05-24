package com.flightbooking.model;

import java.time.LocalDateTime;
import java.util.List;

public class Booking {

    public enum Status { CONFIRMED, CANCELLED }

    private final String bookingId;
    private final String flightNumber;
    private final List<Passenger> passengers;
    private final LocalDateTime bookedAt;
    private Status status;

    public Booking(String bookingId, String flightNumber,
                   List<Passenger> passengers, LocalDateTime bookedAt) {
        this.bookingId = bookingId;
        this.flightNumber = flightNumber;
        this.passengers = passengers;
        this.bookedAt = bookedAt;
        this.status = Status.CONFIRMED;
    }

    public String getBookingId() { return bookingId; }
    public String getFlightNumber() { return flightNumber; }
    public List<Passenger> getPassengers() { return passengers; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getSeatCount() { return passengers.size(); }
}

