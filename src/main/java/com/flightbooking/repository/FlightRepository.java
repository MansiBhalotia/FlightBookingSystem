package com.flightbooking.repository;

import com.flightbooking.model.Flight;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FlightRepository {

    private final ConcurrentHashMap<String, Flight> store = new ConcurrentHashMap<>();

    public Flight save(Flight flight) {
        store.put(flight.getFlightNumber(), flight);
        return flight;
    }

    public Optional<Flight> findByFlightNumber(String flightNumber) {
        return Optional.ofNullable(store.get(flightNumber));
    }

    public boolean existsByFlightNumber(String flightNumber) {
        return store.containsKey(flightNumber);
    }

    public Collection<Flight> findAll() {
        return store.values();
    }
}

