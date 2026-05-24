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
     * Reserve Seats - Internally handle status transitions From Pending to Confirmed
     * Returns 201 CONFIRMED in a single round-trip.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> book(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookingResponse.from(bookingService.book(request)));
    }

    /**
     * DELETE /api/bookings/{bookingId}
     * Cancel a CONFIRMED booking — releases seats back to the flight.
     * Returns 409 if already cancelled.
     */
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(BookingResponse.from(bookingService.cancelBooking(bookingId)));
    }
}
