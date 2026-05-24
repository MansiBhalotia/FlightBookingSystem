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
     * Reserve seats for a flight — booking starts as PENDING.
     *
     * The fair ReentrantLock inside Flight.reserveSeats() ensures FCFS ordering.
     * The OCC version check ensures the client's view of the flight is still current.
     * Together they prevent double-booking even under heavy concurrency.
     */
    public Booking createBooking(CreateBookingRequest request) {
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
        // result == SUCCESS — seats are now reserved
        List<Passenger> passengers = request.getPassengers().stream()
                .map(p -> new Passenger(p.getFirstName(), p.getLastName(), p.getPassportNumber()))
                .toList();

        Booking booking = new Booking(
                UUID.randomUUID().toString(),
                flight.getFlightNumber(),
                flight.getOrigin(),
                flight.getDestination(),
                flight.getDepartureTime(),
                passengers,
                LocalDateTime.now()
        );

        return bookingRepository.save(booking);
    }

    /**
     * Confirm a PENDING booking (payment completed).
     *
     * booking.confirm() is a CAS: PENDING → CONFIRMED.
     * If two threads call this simultaneously, exactly one CAS wins —
     * the other gets false and throws, so double-confirm is impossible.
     */
    public Booking confirmBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new BookingAlreadyCancelledException(bookingId);
        }

        if (!booking.confirm()) {
            // CAS failed — status was not PENDING when we tried (already CONFIRMED)
            throw new BookingAlreadyConfirmedException(bookingId);
        }

        return booking;
    }

    /**
     * Cancel a PENDING or CONFIRMED booking — releases seats back to the flight.
     *
     * booking.cancel() is a CAS: PENDING → CANCELLED or CONFIRMED → CANCELLED.
     * If two threads race to cancel, exactly one CAS wins —
     * the other gets false and throws, preventing double seat release.
     */
    public Booking cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.cancel()) {
            // CAS failed — booking was already CANCELLED
            throw new BookingAlreadyCancelledException(bookingId);
        }

        // orElseThrow instead of ifPresent — seat release must not silently fail
        flightRepository.findByFlightNumber(booking.getFlightNumber())
                .orElseThrow(() -> new FlightNotFoundException(booking.getFlightNumber()))
                .releaseSeats(booking.getSeatCount());

        return booking;
    }
}
