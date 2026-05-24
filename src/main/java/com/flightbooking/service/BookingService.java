package com.flightbooking.service;

import com.flightbooking.dto.CreateBookingRequest;
import com.flightbooking.exception.*;
import com.flightbooking.model.Booking;
import com.flightbooking.model.Flight;
import com.flightbooking.model.Passenger;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    public BookingService(BookingRepository bookingRepository, FlightRepository flightRepository) {
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
    }

    /**
     * Single entry point for the user's "Book" action.
     * Internally runs three steps in sequence:
     *   1. OCC + fair-lock seat reservation   → PENDING
     *   2. Immediate CAS confirmation          → CONFIRMED
     * If step 2 fails (should never happen in normal flow since no other thread
     * knows this booking id yet), the seats are released and the booking is
     * cancelled to leave the system in a consistent state.
     * The caller receives a fully CONFIRMED booking in one HTTP round-trip.
     */
    public Booking book(CreateBookingRequest request) {
        // ── Step 1: reserve seats (PENDING) ──────────────────────────────────
        Flight flight = flightRepository.findByFlightNumber(request.getFlightNumber())
                .orElseThrow(() -> new FlightNotFoundException(request.getFlightNumber()));

        int seats = request.getPassengers().size();
        Flight.ReservationResult result = flight.reserveSeats(seats, request.getFlightVersion());

        if (result == Flight.ReservationResult.VERSION_CONFLICT) {
            throw new StaleFlightDataException(flight.getFlightNumber());
        }
        if (result == Flight.ReservationResult.NO_SEATS_AVAILABLE) {
            throw new NoSeatsAvailableException(flight.getFlightNumber(), seats, flight.getAvailableSeats());
        }

        List<Passenger> passengers = request.getPassengers().stream()
                .map(p -> new Passenger(p.getFirstName(), p.getLastName(), p.getPassportNumber()))
                .toList();

        Booking booking = bookingRepository.save(new Booking(
                UUID.randomUUID().toString(),
                flight.getFlightNumber(),
                flight.getOrigin(),
                flight.getDestination(),
                flight.getDepartureTime(),
                passengers,
                LocalDateTime.now()
        ));

        // ── Step 2: immediately confirm (PENDING → CONFIRMED) ────────────────
        if (!confirmBooking(booking)) {
            // Safety net: CAS failed unexpectedly — roll back seat reservation
            flight.releaseSeats(seats);
            booking.cancel();
            throw new IllegalStateException(
                    "Booking could not be confirmed after reservation. Please try again.");
        }

        return booking;
    }

    /**
     * Internally transitions a PENDING booking to CONFIRMED via CAS.
     * Private — not exposed as an API endpoint.
     */
    private boolean confirmBooking(Booking booking) {
        return booking.confirm();
    }

    /**
     * Cancel a CONFIRMED booking — releases seats back to the flight.
     *
     * booking.cancel() is a CAS: CONFIRMED → CANCELLED.
     * If two threads race to cancel, exactly one CAS wins —
     * the other gets false and throws, preventing double seat release.
     */
    public Booking cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.cancel()) {
            throw new BookingAlreadyCancelledException(bookingId);
        }

        flightRepository.findByFlightNumber(booking.getFlightNumber())
                .orElseThrow(() -> new FlightNotFoundException(booking.getFlightNumber()))
                .releaseSeats(booking.getSeatCount());

        return booking;
    }
}
