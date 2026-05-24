package com.flightbooking.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Flight {

    /**
     * Result of an attempted seat reservation.
     */
    public enum ReservationResult {
        /** Seats reserved successfully. */
        SUCCESS,
        /**
         * The client's expected version does not match the current version.
         * Another booking (or cancellation) changed the flight state after the
         * client last read it — optimistic-concurrency conflict.
         */
        VERSION_CONFLICT,
        /** Not enough seats are available regardless of version. */
        NO_SEATS_AVAILABLE
    }

    private final String flightNumber;
    private final String origin;
    private final String destination;
    private final LocalDateTime departureTime;
    private final int totalSeats;
    private final AtomicInteger bookedSeats;

    /**
     * Monotonically-increasing version number. Incremented on every successful
     * seat reservation or release so clients can detect intervening changes.
     */
    private final AtomicLong version;

    /**
     * Fair (FCFS) lock: threads are granted access in the order they requested
     * it, so the first caller to arrive is the first to complete its booking.
     */
    private final ReentrantLock bookingLock = new ReentrantLock(true);

    public Flight(String flightNumber, String origin, String destination,
                  LocalDateTime departureTime, int totalSeats) {
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.totalSeats = totalSeats;
        this.bookedSeats = new AtomicInteger(0);
        this.version = new AtomicLong(0);
    }

    public String getFlightNumber() { return flightNumber; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public int getTotalSeats() { return totalSeats; }
    public int getBookedSeats() { return bookedSeats.get(); }
    public int getAvailableSeats() { return totalSeats - bookedSeats.get(); }
    public long getVersion() { return version.get(); }

    /**
     * Attempt to reserve {@code count} seats using optimistic concurrency control.
     *
     * <p>The caller must supply the {@code expectedVersion} it observed when it
     * last read the flight. If the version has advanced since then (meaning
     * another thread already booked or cancelled), the reservation is rejected
     * immediately with {@link ReservationResult#VERSION_CONFLICT} rather than
     * silently retrying — this prevents a user who was "just browsing" from
     * accidentally grabbing seats that a faster concurrent booking already claimed.
     *
     * <p>The fair {@link ReentrantLock} ensures threads are served in
     * first-come-first-served arrival order.
     *
     * @param count           number of seats to reserve
     * @param expectedVersion version the caller read before deciding to book
     * @return the reservation outcome
     */
    public ReservationResult reserveSeats(int count, long expectedVersion) {
        bookingLock.lock();
        try {
            // OCC check: reject if flight state changed since the client read it
            if (version.get() != expectedVersion) {
                return ReservationResult.VERSION_CONFLICT;
            }
            if (bookedSeats.get() + count > totalSeats) {
                return ReservationResult.NO_SEATS_AVAILABLE;
            }
            bookedSeats.addAndGet(count);
            version.incrementAndGet();
            return ReservationResult.SUCCESS;
        } finally {
            bookingLock.unlock();
        }
    }

    /**
     * Release {@code count} seats (called on cancellation).
     * Also bumps the version so any in-flight booking attempt that read the
     * pre-cancellation state will be detected as stale.
     */
    public void releaseSeats(int count) {
        bookingLock.lock();
        try {
            bookedSeats.updateAndGet(current -> Math.max(0, current - count));
            version.incrementAndGet();
        } finally {
            bookingLock.unlock();
        }
    }
}

