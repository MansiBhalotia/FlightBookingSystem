package com.flightbooking.controller;

import com.flightbooking.dto.BookingResponse;
import com.flightbooking.dto.CreateBookingRequest;
import com.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /api/bookings
     * Book seats on a flight for one or more passengers.
     * Returns 201 Created with the booking details.
     * Returns 404 if the flight is unknown.
     * Returns 409 if there are not enough seats.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = BookingResponse.from(bookingService.createBooking(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/bookings/{bookingId}
     * Cancel an existing booking and release its seats.
     * Returns 200 OK with the updated booking.
     * Returns 404 if booking does not exist.
     * Returns 409 if the booking is already cancelled.
     */
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable String bookingId) {
        BookingResponse response = BookingResponse.from(bookingService.cancelBooking(bookingId));
        return ResponseEntity.ok(response);
    }
}

