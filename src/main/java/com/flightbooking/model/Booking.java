package com.flightbooking.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Booking {

    public enum Status { PENDING, CONFIRMED, CANCELLED }

    private final String bookingId;
    private final String flightNumber;
    private final String origin;
    private final String destination;
    private final LocalDateTime departureTime;
    private final List<Passenger> passengers;
    private final LocalDateTime bookedAt;

    /**
     * AtomicReference guarantees that status transitions are a single
     * compare-and-swap (CAS) CPU instruction — no two threads can
     * simultaneously read the same status and both succeed in changing it.
     */
    private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);

    public Booking(String bookingId, String flightNumber,
                   String origin, String destination, LocalDateTime departureTime,
                   List<Passenger> passengers, LocalDateTime bookedAt) {
        this.bookingId     = bookingId;
        this.flightNumber  = flightNumber;
        this.origin        = origin;
        this.destination   = destination;
        this.departureTime = departureTime;
        this.passengers    = List.copyOf(passengers);
        this.bookedAt      = bookedAt;
    }

    /**
     * Atomically transition PENDING → CONFIRMED.
     * Returns false if the booking is not currently PENDING
     * (already CONFIRMED or CANCELLED), so callers can detect the conflict.
     */
    public boolean confirm() {
        return status.compareAndSet(Status.PENDING, Status.CONFIRMED);
    }

    /**
     * Atomically transition PENDING → CANCELLED or CONFIRMED → CANCELLED.
     * Returns false only if the booking is already CANCELLED.
     */
    public boolean cancel() {
        return status.compareAndSet(Status.PENDING,   Status.CANCELLED)
            || status.compareAndSet(Status.CONFIRMED, Status.CANCELLED);
    }

    public int getSeatCount()               { return passengers.size(); }
    public String getBookingId()            { return bookingId; }
    public String getFlightNumber()         { return flightNumber; }
    public String getOrigin()               { return origin; }
    public String getDestination()          { return destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public List<Passenger> getPassengers()  { return passengers; }
    public LocalDateTime getBookedAt()      { return bookedAt; }
    public Status getStatus()               { return status.get(); }
}
