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
     * Reserve seats → booking starts as PENDING.
     * Client must supply flightVersion from GET /api/flights/{flightNumber}.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookingResponse.from(bookingService.createBooking(request)));
    }

    /**
     * POST /api/bookings/{bookingId}/confirm
     * Mark a PENDING booking as CONFIRMED (payment completed).
     * Returns 409 if booking is not PENDING.
     */
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(BookingResponse.from(bookingService.confirmBooking(bookingId)));
    }

    /**
     * DELETE /api/bookings/{bookingId}
     * Cancel a PENDING or CONFIRMED booking — releases seats back to the flight.
     * Returns 409 if already cancelled.
     */
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(BookingResponse.from(bookingService.cancelBooking(bookingId)));
    }
}
