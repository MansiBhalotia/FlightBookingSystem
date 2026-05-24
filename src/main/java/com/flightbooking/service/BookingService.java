package com.flightbooking.service;

import com.flightbooking.dto.CreateBookingRequest;
import com.flightbooking.exception.BookingAlreadyCancelledException;
import com.flightbooking.exception.BookingNotFoundException;
import com.flightbooking.exception.FlightNotFoundException;
import com.flightbooking.exception.NoSeatsAvailableException;
import com.flightbooking.exception.StaleFlightDataException;
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

    public Booking createBooking(CreateBookingRequest request) {
        Flight flight = flightRepository.findByFlightNumber(request.getFlightNumber())
                .orElseThrow(() -> new FlightNotFoundException(request.getFlightNumber()));

        int seats = request.getPassengers().size();

        // Attempt reservation under the fair (FCFS) lock with OCC version check.
        // The client must echo back the version it read from GET /api/flights/{flightNumber}.
        // If another booking or cancellation happened between the client's read and now,
        // the version will have advanced and we reject with a 409 so the client can refresh.
        Flight.ReservationResult result = flight.reserveSeats(seats, request.getFlightVersion());

        switch (result) {
            case VERSION_CONFLICT -> throw new StaleFlightDataException(flight.getFlightNumber());
            case NO_SEATS_AVAILABLE -> throw new NoSeatsAvailableException(
                    flight.getFlightNumber(), seats, flight.getAvailableSeats());
            case SUCCESS -> { /* fall through to create the booking */ }
        }

        List<Passenger> passengers = request.getPassengers().stream()
                .map(p -> new Passenger(p.getFirstName(), p.getLastName(), p.getPassportNumber()))
                .toList();

        Booking booking = new Booking(
                UUID.randomUUID().toString(),
                flight.getFlightNumber(),
                passengers,
                LocalDateTime.now()
        );

        return bookingRepository.save(booking);
    }

    public Booking cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new BookingAlreadyCancelledException(bookingId);
        }

        booking.setStatus(Booking.Status.CANCELLED);

        // Release the seats back to the flight (also bumps the version)
        flightRepository.findByFlightNumber(booking.getFlightNumber())
                .ifPresent(f -> f.releaseSeats(booking.getSeatCount()));

        return booking;
    }
}

