package com.flightbooking;

import com.flightbooking.model.Flight;
import com.flightbooking.repository.FlightRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds a few sample flights on startup so the API is immediately usable.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final FlightRepository flightRepository;

    public DataSeeder(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public void run(String... args) {
        flightRepository.save(new Flight("AA101", "New York (JFK)", "Los Angeles (LAX)",
                LocalDateTime.now().plusDays(1), 150));
        flightRepository.save(new Flight("BA202", "London (LHR)", "Paris (CDG)",
                LocalDateTime.now().plusDays(2), 80));
        flightRepository.save(new Flight("LH303", "Frankfurt (FRA)", "Tokyo (NRT)",
                LocalDateTime.now().plusDays(3), 200));
    }
}

