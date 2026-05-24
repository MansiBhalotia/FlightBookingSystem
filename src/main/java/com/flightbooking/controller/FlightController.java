package com.flightbooking.controller;

import com.flightbooking.dto.CreateFlightRequest;
import com.flightbooking.dto.FlightResponse;
import com.flightbooking.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    /**
     * POST /api/flights
     * Register a new flight. Returns 201 Created.
     */
    @PostMapping
    public ResponseEntity<FlightResponse> createFlight(@Valid @RequestBody CreateFlightRequest request) {
        FlightResponse response = FlightResponse.from(flightService.createFlight(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/flights
     * List all registered flights.
     */
    @GetMapping
    public ResponseEntity<List<FlightResponse>> getAllFlights() {
        List<FlightResponse> flights = flightService.getAllFlights().stream()
                .map(FlightResponse::from)
                .toList();
        return ResponseEntity.ok(flights);
    }

    /**
     * GET /api/flights/{flightNumber}
     * Get a specific flight by its flight number.
     */
    @GetMapping("/{flightNumber}")
    public ResponseEntity<FlightResponse> getFlight(@PathVariable String flightNumber) {
        return ResponseEntity.ok(FlightResponse.from(flightService.getFlightByNumber(flightNumber)));
    }
}

