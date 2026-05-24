package com.flightbooking.service;

import com.flightbooking.dto.CreateFlightRequest;
import com.flightbooking.exception.FlightAlreadyExistsException;
import com.flightbooking.exception.FlightNotFoundException;
import com.flightbooking.model.Flight;
import com.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public Flight createFlight(CreateFlightRequest request) {
        if (flightRepository.existsByFlightNumber(request.getFlightNumber())) {
            throw new FlightAlreadyExistsException(request.getFlightNumber());
        }
        Flight flight = new Flight(
                request.getFlightNumber(),
                request.getOrigin(),
                request.getDestination(),
                request.getDepartureTime(),
                request.getTotalSeats()
        );
        return flightRepository.save(flight);
    }

    public Flight getFlightByNumber(String flightNumber) {
        return flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> new FlightNotFoundException(flightNumber));
    }

    public Collection<Flight> getAllFlights() {
        return flightRepository.findAll();
    }
}

